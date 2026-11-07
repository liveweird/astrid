@file:Suppress("ThrowsCount")

package app.astrid.server

import app.astrid.engine.GeneratedExercise
import app.astrid.engine.LinguisticStatus
import app.astrid.engine.PracticeAttemptContext
import app.astrid.engine.PracticeFollowUpPlanner
import app.astrid.engine.PracticeExposureSource
import app.astrid.engine.PracticePlanner
import app.astrid.engine.PracticePriorExposure
import app.astrid.engine.PracticeSummaryAttemptObservation
import app.astrid.engine.PracticeSummaryCalculator
import app.astrid.engine.PracticeSummarySnapshotItemObservation
import app.astrid.engine.PracticeTargetDescriptor
import app.astrid.engine.ResponseConstraintStatus
import app.astrid.engine.TargetStatus
import app.astrid.engine.PilotEngine
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable

private const val PRACTICE_OWNER_HEADER = "X-Astrid-Practice-Owner"
private const val PRACTICE_FOCUS = "reading-basics"
private const val PRACTICE_REVISION = "practice-1"
private const val PRACTICE_ITEM_COUNT = 8
private const val FOLLOW_UP_REVISION = "follow-up-1"

internal fun Route.practiceRoutes(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    clock: () -> Long,
) {
    val planner = PracticePlanner(engine)
    get("/practice-context") {
        val owner = requirePracticeSession(call, config, repository, clock)
        val plan = planner.plan(PRACTICE_FOCUS, PRACTICE_REVISION, 0)
        call.respond(
            PracticeContextResponse(
                owner.id.toString(), owner.expiresAt, plan.focusId, plan.title, plan.planRevision, plan.items.size,
            ),
        )
    }
    post("/practice-sessions") {
        requireMutationHeaders(call)
        val owner = requirePracticeSession(call, config, repository, clock)
        requirePracticeOwner(call, owner.id)
        val request = call.receive<PracticeSessionRequest>().validated()
        val result = repository.createPracticeSession(owner, request, wireJson.encodeToString(request)) {
            val plan = try {
                planner.plan(request.focusId, request.planRevision, request.seed)
            } catch (cause: IllegalArgumentException) {
                throw Conflict(cause.message ?: "Unsupported practice plan").apply { initCause(cause) }
            }
            NewPracticeSession(
                plan.title,
                plan.contentRevision,
                plan.generatorRevision,
                plan.evaluationRevision,
                plan.prerequisiteSkillIds,
                plan.introduction.map { IntroductionDto(it.title, it.body) },
                plan.items.map { item ->
                    val id = UUID.randomUUID()
                    NewPracticeItem(
                        item.position,
                        item.exercise.toStoredPractice(id, owner.id),
                        item.targetDescriptor.label,
                        item.answerExposureKeys,
                        item.promptExposureKeys,
                        item.lessonExposureKeys,
                        item.hintExposureKeys,
                        item.feedbackExposureKeys,
                    )
                },
            )
        }
        when (result) {
            is PracticeCreateResult.Created -> call.respond(HttpStatusCode.Created, result.session.toResponse())
            is PracticeCreateResult.Existing -> call.respond(result.session.toResponse())
            PracticeCreateResult.RequestConflict -> throw Conflict("Practice session id was already used with a different request")
            PracticeCreateResult.SourceMissing, PracticeCreateResult.SourceActive -> error("Initial practice has no source")
        }
    }
    followUpRoute(config, repository, engine, clock)
    get("/practice-sessions/{id}") {
        val owner = requirePracticeSession(call, config, repository, clock)
        val stored = repository.findPracticeSession(owner.id, call.practiceId()) ?: throw NotFound()
        call.respond(stored.toResponse())
    }
    post("/practice-sessions/{id}/support") {
        requireMutationHeaders(call)
        val owner = requirePracticeSession(call, config, repository, clock)
        requirePracticeOwner(call, owner.id)
        val request = call.receive<PracticeSupportRequest>().validated()
        call.respond(repository.openPracticeSupport(owner.id, call.practiceId(), request).response())
    }
    post("/practice-sessions/{id}/advance") {
        requireMutationHeaders(call)
        val owner = requirePracticeSession(call, config, repository, clock)
        requirePracticeOwner(call, owner.id)
        val request = call.receive<PracticeAdvanceRequest>()
        if (request.position !in 0 until PRACTICE_ITEM_COUNT) throw BadRequest("Invalid practice position")
        call.respond(repository.advancePracticeSession(owner.id, call.practiceId(), request.position).response())
    }
}

private fun Route.followUpRoute(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    clock: () -> Long,
) {
    post("/practice-sessions/{id}/follow-ups") {
        requireMutationHeaders(call)
        val owner = requirePracticeSession(call, config, repository, clock)
        requirePracticeOwner(call, owner.id)
        val sourceId = call.practiceId()
        val request = call.receive<PracticeFollowUpRequest>().validated(sourceId)
        val identity = wireJson.encodeToString(PracticeFollowUpCreationIdentity(sourceId.toString(), request))
        val result = repository.createPracticeFollowUp(owner, sourceId, request, identity) { source ->
            source.toFollowUp(request, sourceId, owner.id, engine)
        }
        when (result) {
            is PracticeCreateResult.Created -> call.respond(HttpStatusCode.Created, result.session.toResponse())
            is PracticeCreateResult.Existing -> call.respond(result.session.toResponse())
            PracticeCreateResult.RequestConflict -> throw Conflict("Practice session id was already used with a different request")
            PracticeCreateResult.SourceMissing -> throw NotFound("Source practice session not found")
            PracticeCreateResult.SourceActive -> throw Conflict("Complete the source practice before starting a review")
        }
    }
}

private fun StoredPracticeSession.toFollowUp(
    request: PracticeFollowUpRequest,
    sourceId: UUID,
    ownerId: UUID,
    engine: PilotEngine,
): NewPracticeSession {
    val sourceExercises = items.map { regenerateVerified(engine, it.exercise) }
    val plan = try {
        PracticeFollowUpPlanner(engine).plan(request.planRevision, request.seed, request.targetKeys, sourceExercises)
    } catch (cause: IllegalArgumentException) {
        throw Conflict(cause.message ?: "Unsupported follow-up practice plan").apply { initCause(cause) }
    }
    return NewPracticeSession(
        plan.title,
        plan.contentRevision,
        plan.generatorRevision,
        plan.evaluationRevision,
        plan.prerequisiteSkillIds,
        plan.introduction.map { IntroductionDto(it.title, it.body) },
        plan.items.map { item ->
            NewPracticeItem(
                item.position,
                item.exercise.toStoredPractice(UUID.randomUUID(), ownerId),
                item.targetDescriptor.label,
                item.answerExposureKeys,
                item.promptExposureKeys,
                item.lessonExposureKeys,
                item.hintExposureKeys,
                item.feedbackExposureKeys,
            )
        },
        PracticeFollowUpLineage(sourceId, request.targetKeys),
        sourceExposureSnapshot(),
    )
}

private suspend fun requirePracticeSession(
    call: ApplicationCall,
    config: ServerConfig,
    repository: AstridRepository,
    clock: () -> Long,
): SessionRecord = requireSession(call, config, repository, clock)

private fun requirePracticeOwner(call: ApplicationCall, expected: UUID) {
    val raw = call.request.header(PRACTICE_OWNER_HEADER) ?: throw BadRequest("Practice owner header is required")
    val supplied = parseStrictUuid(raw, "practice owner")
    if (supplied != expected) throw Conflict("Practice owner does not match the authenticated session")
}

private fun PracticeSessionRequest.validated(): PracticeSessionRequest {
    parseStrictUuid(practiceSessionId, "practiceSessionId")
    if (focusId != PRACTICE_FOCUS || planRevision != PRACTICE_REVISION) throw Conflict("Unsupported practice plan")
    if (seed < 0) throw BadRequest("Invalid practice seed")
    return this
}

private fun PracticeFollowUpRequest.validated(sourceId: UUID): PracticeFollowUpRequest {
    val id = parseStrictUuid(practiceSessionId, "practiceSessionId")
    if (id == sourceId) throw Conflict("Follow-up practice must use a new session id")
    if (planRevision != FOLLOW_UP_REVISION) throw Conflict("Unsupported follow-up practice plan")
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

private fun ApplicationCall.practiceId(): UUID =
    parameters["id"]?.let { parseStrictUuid(it, "id") } ?: throw BadRequest("Invalid id")

private fun PracticeMutationResult.response() = when (this) {
    is PracticeMutationResult.Success -> session.toResponse()
    PracticeMutationResult.Missing -> throw NotFound()
    PracticeMutationResult.Conflict -> throw Conflict("Practice operation conflicts with current progress")
}

private fun GeneratedExercise.toStoredPractice(id: UUID, ownerSessionId: UUID) = StoredExercise(
    id,
    ownerSessionId,
    formatId,
    targetSkillId,
    reproduction.seed,
    reproduction.index,
    contentRevision,
    generatorRevision,
    toResponse(id.toString()),
    reproductionFingerprint(),
)

private fun StoredPracticeSession.toResponse(): PracticeSessionResponse {
    val summary = PracticeSummaryCalculator.calculate(
        items.size,
        introductionOpened,
        items.map { stored ->
            PracticeSummarySnapshotItemObservation(
                stored.position,
                PracticeTargetDescriptor(
                    stored.targetLabel,
                    stored.exercise.targetSkillId,
                    stored.exercise.presentation.lexicalTarget?.let {
                        app.astrid.engine.LexicalTarget(it.entryId, it.senseId, it.facet, it.direction)
                    },
                ),
                advanced = stored.position < cursor,
                lessonOpened = stored.lessonOpened,
                hintOpened = stored.hintOpened,
                attempts = stored.attempts.map { attempt ->
                    PracticeSummaryAttemptObservation(
                        enumByWire<TargetStatus>(attempt.response.targetSatisfaction),
                        enumByWire<LinguisticStatus>(attempt.response.linguisticStatus),
                        attempt.response.responseConstraintStatus?.let { enumByWire<ResponseConstraintStatus>(it) },
                        attempt.context.toEngine(),
                        attempt.response.hintUsed,
                    )
                },
            )
        },
    )
    return PracticeSessionResponse(
        id.toString(), ownerSessionId.toString(), focusId, title, planRevision, seed,
        contentRevision, generatorRevision, evaluationRevision, sessionExpiresAt,
        if (cursor == items.size) "completed" else "active", cursor, items.size,
        prerequisiteSkillIds, introduction, introductionOpened,
        items.map { it.toDto() }, items.singleOrNull { it.position == cursor }?.exercise?.presentation,
        summary.toDto(includeRows = cursor == items.size),
        followUp?.let { PracticeFollowUpDto(it.sourcePracticeSessionId.toString(), it.selectedTargetKeys) },
    )
}

private fun StoredPracticeItem.toDto(): PracticeItemDto = PracticeItemDto(
    position,
    exercise.id.toString(),
    exercise.formatId,
    exercise.targetSkillId,
    exercise.presentation.lexicalTarget,
    lessonOpened,
    hintOpened,
    attempts.map { it.response.attemptId },
    attempts.size,
    attempts.firstOrNull()?.response,
    attempts.lastOrNull()?.response,
    attempts.firstOrNull()?.answer,
    attempts.lastOrNull()?.answer,
    attempts.firstOrNull()?.context,
    attempts.lastOrNull()?.context,
)

private fun PracticeAttemptContextDto.toEngine() = PracticeAttemptContext(
    introductionOpened,
    lessonOpened,
    hintOpened,
    priorExposure.map { exposure ->
        PracticePriorExposure(
            exposure.position,
            PracticeExposureSource.entries.single { it.wireValue == exposure.source },
            exposure.keys,
        )
    },
    sourceExposure.map { exposure ->
        app.astrid.engine.PracticeCrossRunExposure(
            exposure.practiceSessionId,
            exposure.position,
            PracticeExposureSource.entries.single { it.wireValue == exposure.source },
            exposure.keys,
        )
    },
)

private fun StoredPracticeSession.sourceExposureSnapshot(): List<PracticeCrossRunExposureDto> {
    val events = buildList {
        addAll(sourceExposure)
        if (introductionOpened) {
            add(
                PracticeCrossRunExposureDto(
                    id.toString(),
                    INTRODUCTION_POSITION,
                    PracticeExposureSource.INTRODUCTION.wireValue,
                    items.flatMap { it.lessonExposureKeys }.distinct(),
                ),
            )
        }
        items.sortedBy { it.position }.forEach { item ->
            add(PracticeCrossRunExposureDto(id.toString(), item.position, "prompt", item.promptExposureKeys))
            if (item.lessonOpened) add(PracticeCrossRunExposureDto(id.toString(), item.position, "lesson", item.lessonExposureKeys))
            if (item.hintOpened) add(PracticeCrossRunExposureDto(id.toString(), item.position, "hint", item.hintExposureKeys))
            if (item.attempts.isNotEmpty()) {
                add(PracticeCrossRunExposureDto(id.toString(), item.position, "feedback", item.feedbackExposureKeys))
            }
        }
    }
    val witnessByKey = linkedMapOf<String, PracticeCrossRunExposureDto>()
    events.forEach { event -> event.keys.distinct().forEach { key -> witnessByKey.putIfAbsent(key, event.copy(keys = listOf(key))) } }
    return witnessByKey.values.groupBy { Triple(it.practiceSessionId, it.position, it.source) }.map { (origin, witnesses) ->
        PracticeCrossRunExposureDto(origin.first, origin.second, origin.third, witnesses.flatMap { it.keys })
    }
}

@Serializable
private data class PracticeFollowUpCreationIdentity(
    val sourcePracticeSessionId: String,
    val request: PracticeFollowUpRequest,
)

private const val MAX_TARGET_KEY_LENGTH = 160
private const val INTRODUCTION_POSITION = -1

private fun app.astrid.engine.PracticeSummary.toDto(includeRows: Boolean) = PracticeSummaryDto(
    plannedItems, attemptedItems, completedItems, attemptCount, retryCount, introductionOpened,
    rows.takeIf { includeRows }.orEmpty().map { row ->
        PracticeSummaryRowDto(
            row.label,
            row.targetSkillId,
            row.lexicalTarget?.let { LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction) },
            row.plannedItems,
            row.attemptedItems,
            row.advancedItems,
            row.attemptCount,
            row.retryCount,
            row.first.toDto(),
            row.latest.toDto(),
            row.firstWithSupport,
            row.firstWithPriorExposure,
            row.lessonOpenedItems,
            row.hintOpenedItems,
        )
    },
)

private fun app.astrid.engine.PracticeOutcomeCounts.toDto() = PracticeOutcomeCountsDto(
    fullyMet, targetMet, targetNotMet, unevaluable, languageUnknown, languageKnownIssue, responseConstraintNotMet,
)

private inline fun <reified T : Enum<T>> enumByWire(value: String): T = enumValues<T>().single { entry ->
    when (entry) {
        is TargetStatus -> entry.wireValue
        is LinguisticStatus -> entry.wireValue
        is ResponseConstraintStatus -> entry.wireValue
        else -> error("Unsupported wire enum")
    } == value
}
