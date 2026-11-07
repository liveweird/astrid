@file:Suppress("ThrowsCount") // Route validation maps each independent client fault to a safe problem.

package app.astrid.server.practice

import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlanner
import app.astrid.server.authz.Authenticator
import app.astrid.server.authz.BadRequest
import app.astrid.server.authz.Conflict
import app.astrid.server.authz.NotFound
import app.astrid.server.authz.caller
import app.astrid.server.authz.parseStrictUuid
import app.astrid.server.authz.requireMutationHeaders
import app.astrid.server.authz.requirePracticeOwner
import app.astrid.server.authz.uuidParameter
import app.astrid.server.infra.json.wireJson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

private const val MAX_TARGET_KEY_LENGTH = 160

internal fun Route.practiceRoutes(
    practice: PracticeStore,
    authenticator: Authenticator,
    engine: PilotEngine,
) {
    val planner = PracticePlanner(engine)
    practiceProfilesRoute(authenticator, planner)
    get("/practice-context") {
        val owner = call.caller(authenticator)
        val plan = planner.plan(PRACTICE_FOCUS, PRACTICE_REVISION, 0)
        call.respond(
            PracticeContextResponse(
                owner.id.toString(), owner.expiresAt, plan.focusId, plan.title, plan.planRevision, plan.items.size,
            ),
        )
    }
    post("/practice-sessions") {
        requireMutationHeaders(call)
        val owner = call.caller(authenticator)
        requirePracticeOwner(call, owner.id)
        val request = call.receive<PracticeSessionRequest>().validated()
        val result = practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) {
            val plan = try {
                planner.plan(request.focusId, request.planRevision, request.seed)
            } catch (cause: IllegalArgumentException) {
                throw Conflict(cause.message ?: "Unsupported practice plan").apply { initCause(cause) }
            }
            plan.toNewPracticeSession(owner.id)
        }
        when (result) {
            is PracticeCreateResult.Created -> {
                call.response.headers.append(HttpHeaders.Location, "/api/v1/practice-sessions/${result.session.id}")
                call.respond(HttpStatusCode.Created, result.session.toResponse())
            }
            is PracticeCreateResult.Existing -> call.respond(result.session.toResponse())
            PracticeCreateResult.RequestConflict -> throw Conflict("Practice session id was already used with a different request")
            PracticeCreateResult.SourceMissing, PracticeCreateResult.SourceActive -> error("Initial practice has no source")
        }
    }
    followUpRoute(practice, authenticator, engine)
    get("/practice-sessions/{id}") {
        val owner = call.caller(authenticator)
        val stored = practice.findPracticeSession(owner.id, call.practiceId()) ?: throw NotFound()
        call.respond(stored.toResponse())
    }
    post("/practice-sessions/{id}/support") {
        requireMutationHeaders(call)
        val owner = call.caller(authenticator)
        requirePracticeOwner(call, owner.id)
        val request = call.receive<PracticeSupportRequest>().validated()
        call.respond(practice.openPracticeSupport(owner.id, call.practiceId(), request).response())
    }
    post("/practice-sessions/{id}/advance") {
        requireMutationHeaders(call)
        val owner = call.caller(authenticator)
        requirePracticeOwner(call, owner.id)
        val request = call.receive<PracticeAdvanceRequest>()
        if (request.position !in 0 until PRACTICE_ITEM_COUNT) throw BadRequest("Invalid practice position")
        call.respond(practice.advancePracticeSession(owner.id, call.practiceId(), request.position).response())
    }
}

private fun Route.practiceProfilesRoute(authenticator: Authenticator, planner: PracticePlanner) {
    get("/practice-profiles") {
        val owner = call.caller(authenticator)
        call.respond(
            PracticeProfilesResponse(
                owner.id.toString(),
                owner.expiresAt,
                planner.profiles().map { profile ->
                    PracticeProfileDto(
                        profile.focusId,
                        profile.planRevision,
                        profile.title,
                        profile.description,
                        profile.itemCount,
                        profile.prerequisiteSkillIds,
                    )
                },
            ),
        )
    }
}

private fun Route.followUpRoute(
    practice: PracticeStore,
    authenticator: Authenticator,
    engine: PilotEngine,
) {
    post("/practice-sessions/{id}/follow-ups") {
        requireMutationHeaders(call)
        val owner = call.caller(authenticator)
        requirePracticeOwner(call, owner.id)
        val sourceId = call.practiceId()
        val request = call.receive<PracticeFollowUpRequest>().validated(sourceId)
        val identity = wireJson.encodeToString(PracticeFollowUpCreationIdentity(sourceId.toString(), request))
        val result = practice.createPracticeFollowUp(owner, sourceId, request, identity) { source ->
            source.toFollowUp(request, sourceId, owner.id, engine)
        }
        when (result) {
            is PracticeCreateResult.Created -> {
                call.response.headers.append(HttpHeaders.Location, "/api/v1/practice-sessions/${result.session.id}")
                call.respond(HttpStatusCode.Created, result.session.toResponse())
            }
            is PracticeCreateResult.Existing -> call.respond(result.session.toResponse())
            PracticeCreateResult.RequestConflict -> throw Conflict("Practice session id was already used with a different request")
            PracticeCreateResult.SourceMissing -> throw NotFound("Source practice session not found")
            PracticeCreateResult.SourceActive -> throw Conflict("Complete the source practice before starting a review")
        }
    }
}

private fun PracticeSessionRequest.validated(): PracticeSessionRequest {
    parseStrictUuid(practiceSessionId, "practiceSessionId")
    val supported =
        (focusId == PRACTICE_FOCUS && planRevision == PRACTICE_REVISION) ||
            (focusId == PRESENT_VERBS_FOCUS && planRevision == PRESENT_VERBS_REVISION)
    if (!supported) throw Conflict("Unsupported practice plan")
    if (seed < 0) throw BadRequest("Invalid practice seed")
    return this
}

private fun PracticeFollowUpRequest.validated(sourceId: UUID): PracticeFollowUpRequest {
    val id = parseStrictUuid(practiceSessionId, "practiceSessionId")
    if (id == sourceId) throw Conflict("Follow-up practice must use a new session id")
    if (planRevision != FOLLOW_UP_REVISION && planRevision != PRESENT_VERBS_FOLLOW_UP_REVISION) {
        throw Conflict("Unsupported follow-up practice plan")
    }
    if (seed < 0) throw BadRequest("Invalid practice seed")
    if (targetKeys.isEmpty() || targetKeys.size > PRACTICE_ITEM_COUNT || targetKeys.toSet().size != targetKeys.size) {
        throw BadRequest("Follow-up target keys must contain 1 to 8 distinct values")
    }
    if (targetKeys.any { it.isBlank() || it.length > MAX_TARGET_KEY_LENGTH }) {
        throw BadRequest("Invalid follow-up target key")
    }
    return this
}

private fun PracticeSupportRequest.validated(): PracticeSupportRequest {
    when (kind) {
        "introduction" -> if (position != null) throw BadRequest("Introduction support must not include a position")
        "lesson", "hint" -> if (position !in 0 until PRACTICE_ITEM_COUNT) {
            throw BadRequest("Item support requires a valid position")
        }
        else -> throw BadRequest("Invalid practice support kind")
    }
    return this
}

private fun ApplicationCall.practiceId(): UUID = uuidParameter("id")

@Serializable
private data class PracticeFollowUpCreationIdentity(
    val sourcePracticeSessionId: String,
    val request: PracticeFollowUpRequest,
)
