@file:Suppress("ThrowsCount") // Route validation maps each independent client fault to a safe problem.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.engine.Answer
import app.astrid.engine.Evaluation
import app.astrid.engine.GeneratedExercise
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.CancellationException

private const val SESSION_COOKIE = "astrid_session"
private const val CLIENT_HEADER = "X-Astrid-Client"
private const val INSPECTOR_HEADER = "X-Astrid-Inspector-Key"
private const val TOKEN_BYTES = 32
private const val MAX_IDENTIFIER_LENGTH = 32
private const val MAX_REVISION_LENGTH = 64
private const val MAX_GENERATION_INDEX = 1_000_000
private const val MAX_TEXT_LENGTH = 512
private const val MAX_BLOCK_COUNT = 16
private val secureRandom = SecureRandom()

internal fun Route.astridRoutes(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    evaluator: suspend (GeneratedExercise, Answer) -> Evaluation,
    clock: () -> Long,
) {
    route("/api/v1") {
        healthRoutes(repository)
        sessionRoutes(config, repository, clock)
        catalogRoute(config, repository, engine, clock)
        exerciseRoutes(config, repository, engine, clock)
        attemptRoutes(config, repository, engine, evaluator, clock)
        inspectorRoute(config, repository, engine, clock)
    }
    staticRoutes(config)
}

private fun Route.healthRoutes(repository: AstridRepository) {
    get("/health") { call.respond(HealthResponse("ok")) }
    get("/ready") {
        if (repository.ready()) call.respond(HealthResponse("ready"))
        else call.respond(HttpStatusCode.ServiceUnavailable, HealthResponse("unavailable"))
    }
}

private fun Route.sessionRoutes(config: ServerConfig, repository: AstridRepository, clock: () -> Long) {
    post("/development-session") {
        if (!config.developmentMode) throw NotFound()
        requireMutationHeaders(call)
        call.receive<DevelopmentSessionRequest>()
        val existing = call.request.cookies[SESSION_COOKIE]
            ?.takeIf(::validTokenShape)
            ?.let { repository.findSession(hashToken(it), clock()) }
        val session = existing ?: newSession(repository, config, call, clock)
        if (session.expiresAt <= clock()) throw Unauthorized()
        call.respond(DevelopmentSessionResponse(inspectorEnabled = config.inspectorEnabled))
    }
}

private fun Route.catalogRoute(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    clock: () -> Long,
) {
    get("/catalog") {
        requireSession(call, config, repository, clock)
        val catalog = engine.catalog()
        call.respond(
            CatalogResponse(
                catalog.contentRevision,
                catalog.generatorRevision,
                EVALUATION_REVISION,
                catalog.skills.map { SkillDto(it.id, it.title, it.explanation, it.prerequisiteIds) },
                catalog.formats.map { FormatDto(it.id, it.title, it.targetSkillId) },
                catalog.sentenceCount,
            ),
        )
    }
}

private fun Route.exerciseRoutes(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    clock: () -> Long,
) {
    post("/exercises") {
        requireMutationHeaders(call)
        val session = requireSession(call, config, repository, clock)
        val request = call.receive<ExerciseRequest>().validated()
        val generated = generate(engine, request)
        val id = UUID.randomUUID()
        val presentation = generated.toResponse(id.toString())
        repository.insertExercise(request.toStored(id, session.id, presentation, generated.reproductionFingerprint()))
        call.respond(HttpStatusCode.Created, presentation)
    }
    get("/exercises/{id}") {
        val session = requireSession(call, config, repository, clock)
        val stored = repository.findExercise(session.id, call.uuidParameter("id")) ?: throw NotFound()
        call.respond(stored.presentation)
    }
}

private fun Route.attemptRoutes(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    evaluator: suspend (GeneratedExercise, Answer) -> Evaluation,
    clock: () -> Long,
) {
    post("/attempts") {
        requireMutationHeaders(call)
        val session = requireSession(call, config, repository, clock)
        val request = call.receive<AttemptRequest>().validated()
        val result = repository.writeAttempt(session.id, request, wireJson.encodeToString(request)) { stored, retry, time ->
            evaluateAttempt(engine, evaluator, stored, request, retry, time)
        }
        when (result) {
            is AttemptWriteResult.Created -> call.respond(result.response)
            is AttemptWriteResult.Existing -> call.respond(result.response)
            AttemptWriteResult.PayloadConflict -> throw Conflict("Attempt id was already used with a different payload")
            AttemptWriteResult.ExerciseMissing -> throw NotFound("Exercise not found")
        }
    }
    get("/attempts/{id}") {
        val session = requireSession(call, config, repository, clock)
        call.respond(repository.findAttempt(session.id, call.uuidParameter("id")) ?: throw NotFound())
    }
}

private fun Route.inspectorRoute(
    config: ServerConfig,
    repository: AstridRepository,
    engine: PilotEngine,
    clock: () -> Long,
) {
    get("/dev/exercises/{id}") {
        if (!config.developmentMode || !config.inspectorEnabled) throw NotFound()
        requireInspectorKey(call, config.inspectorKey!!)
        val session = requireSession(call, config, repository, clock)
        val stored = repository.findExercise(session.id, call.uuidParameter("id")) ?: throw NotFound()
        call.respond(regenerateVerified(engine, stored).toInspector(stored.id.toString()))
    }
}

private fun Route.staticRoutes(config: ServerConfig) {
    val root = config.webStaticDir?.let(::File)?.takeIf(File::isDirectory) ?: return
    get("/") { call.serveStaticOrSpa(root) }
    get("/{...}") { call.serveStaticOrSpa(root) }
}

private suspend fun ApplicationCall.serveStaticOrSpa(root: File) {
    val path = request.path()
    if (path == "/api" || path.startsWith("/api/")) throw NotFound()
    response.headers.append(HttpHeaders.CacheControl, "no-cache")
    val canonicalRoot = root.canonicalFile
    val requested = File(canonicalRoot, path.removePrefix("/")).canonicalFile
    val insideRoot = requested.toPath().startsWith(canonicalRoot.toPath())
    if (insideRoot && requested.isFile) {
        respondFile(requested)
    } else if (path == "/assets" || path.startsWith("/assets/")) {
        throw NotFound()
    } else {
        respondFile(File(canonicalRoot, "index.html"))
    }
}

@Suppress("TooGenericExceptionCaught") // Any engine failure is an unavailable grade; coroutine cancellation still propagates.
private suspend fun evaluateAttempt(
    engine: PilotEngine,
    evaluator: suspend (GeneratedExercise, Answer) -> Evaluation,
    stored: StoredExercise,
    request: AttemptRequest,
    retry: Int,
    createdAt: Long,
): AttemptResponse {
    val generated = regenerateVerified(engine, stored)
    request.answer.validateFor(generated.formatId, generated.prompt.blocks.map { it.id }.toSet())
    val evaluation = try {
        evaluator(generated, request.answer.toEngineAnswer())
    } catch (cause: CancellationException) {
        throw cause
    } catch (cause: Exception) {
        throw EvaluationUnavailable().apply { initCause(cause) }
    }
    return AttemptResponse(
        request.attemptId,
        request.exerciseId,
        evaluation.targetStatus.wireValue,
        evaluation.linguisticStatus.wireValue,
        evaluation.issues.map { IssueDto(it.code, it.message) },
        evaluation.feedback,
        evaluation.completedSentence.orEmpty(),
        EVALUATION_REVISION,
        request.hintUsed,
        retry,
        createdAt,
    )
}

private suspend fun newSession(
    repository: AstridRepository,
    config: ServerConfig,
    call: ApplicationCall,
    clock: () -> Long,
): SessionRecord {
    val token = ByteArray(TOKEN_BYTES).also(secureRandom::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    val session = repository.createSession(hashToken(token), clock() + config.sessionTtlMillis)
    call.response.cookies.append(
        Cookie(SESSION_COOKIE, token, maxAge = (config.sessionTtlMillis / 1000).toInt(), path = "/", secure = config.secureCookie,
            httpOnly = true, extensions = mapOf("SameSite" to "Strict")),
    )
    return session
}

private suspend fun requireSession(
    call: ApplicationCall,
    config: ServerConfig,
    repository: AstridRepository,
    clock: () -> Long,
): SessionRecord {
    if (!config.developmentMode) throw NotFound()
    val token = call.request.cookies[SESSION_COOKIE]?.takeIf(::validTokenShape) ?: throw Unauthorized()
    return repository.findSession(hashToken(token), clock()) ?: throw Unauthorized()
}

private fun requireMutationHeaders(call: ApplicationCall) {
    if (call.request.header(CLIENT_HEADER) != "playground") throw Forbidden("Missing required playground client header")
    val media = call.request.headers[HttpHeaders.ContentType] ?: throw BadRequest("Request body is missing or not JSON")
    if (media.isBlank() || call.request.contentType().withoutParameters() != ContentType.Application.Json) {
        throw UnsupportedMediaType()
    }
}

private fun requireInspectorKey(call: ApplicationCall, expected: String) {
    val provided = call.request.header(INSPECTOR_HEADER) ?: throw Forbidden("Inspector key is required")
    if (!MessageDigest.isEqual(provided.toByteArray(), expected.toByteArray())) throw Forbidden("Inspector key is invalid")
}

private fun ApplicationCall.uuidParameter(name: String): UUID =
    parameters[name]?.let { parseStrictUuid(it, name) } ?: throw BadRequest("Invalid $name")

private fun ExerciseRequest.validated(): ExerciseRequest {
    if (formatId.length !in 1..MAX_IDENTIFIER_LENGTH || targetSkillId.length !in 1..MAX_IDENTIFIER_LENGTH) {
        throw BadRequest("Invalid exercise identifiers")
    }
    if (contentRevision.length !in 1..MAX_REVISION_LENGTH || generatorRevision.length !in 1..MAX_REVISION_LENGTH) {
        throw BadRequest("Invalid revision")
    }
    if (seed < 0 || index !in 0..MAX_GENERATION_INDEX) throw BadRequest("Invalid generation coordinates")
    return this
}

private fun AttemptRequest.validated(): AttemptRequest {
    parseStrictUuid(attemptId, "attemptId")
    parseStrictUuid(exerciseId, "exerciseId")
    when (val value = answer) {
        is AttemptAnswerDto.Text -> if (value.text.length > MAX_TEXT_LENGTH) throw BadRequest("Text answer is too long")
        is AttemptAnswerDto.Order -> if (value.blockIds.size > MAX_BLOCK_COUNT ||
            value.blockIds.any { it.length !in 1..MAX_REVISION_LENGTH }) throw BadRequest("Invalid block answer")
    }
    return this
}

private fun parseStrictUuid(value: String, field: String): UUID {
    val parsed = runCatching { UUID.fromString(value) }.getOrElse { throw BadRequest("Invalid $field") }
    if (!value.equals(parsed.toString(), ignoreCase = true)) throw BadRequest("Invalid $field")
    return parsed
}

private fun AttemptAnswerDto.validateFor(formatId: String, expectedBlocks: Set<String>) {
    if (formatId == "E003" && this !is AttemptAnswerDto.Order) throw BadRequest("This exercise requires an order answer")
    if (formatId != "E003" && this !is AttemptAnswerDto.Text) throw BadRequest("This exercise requires a text answer")
    if (this is AttemptAnswerDto.Order && (blockIds.size != expectedBlocks.size || blockIds.toSet() != expectedBlocks)) {
        throw BadRequest("The answer must contain every supplied block exactly once")
    }
}

private fun generate(engine: PilotEngine, request: ExerciseRequest) =
    if (request.contentRevision != CONTENT_REVISION || request.generatorRevision != GENERATOR_REVISION) {
        throw Conflict("Unsupported content or generator revision")
    } else try {
        engine.generate(request.toGenerationRequest())
    } catch (cause: IllegalArgumentException) {
        throw BadRequest(cause.message ?: "Invalid generation request").apply { initCause(cause) }
    }

private fun regenerate(engine: PilotEngine, stored: StoredExercise) = try {
    engine.generate(stored.toGenerationRequest())
} catch (cause: IllegalArgumentException) {
    throw Conflict("Exercise revision is no longer supported").apply { initCause(cause) }
}

private fun regenerateVerified(engine: PilotEngine, stored: StoredExercise) = regenerate(engine, stored).also { generated ->
    if (generated.reproductionFingerprint() != stored.reproductionFingerprint) {
        throw Conflict("Exercise reproduction no longer matches its stored presentation")
    }
}

private fun ExerciseRequest.toGenerationRequest() =
    GenerationRequest(formatId, targetSkillId, seed, index, contentRevision, generatorRevision)

private fun StoredExercise.toGenerationRequest() =
    GenerationRequest(formatId, targetSkillId, seed, index, contentRevision, generatorRevision)

private fun ExerciseRequest.toStored(
    id: UUID,
    sessionId: UUID,
    presentation: ExerciseResponse,
    fingerprint: String,
) = StoredExercise(id, sessionId, formatId, targetSkillId, seed, index, contentRevision, generatorRevision, presentation, fingerprint)

private fun validTokenShape(value: String) = value.matches(Regex("[A-Za-z0-9_-]{43}"))

private fun hashToken(token: String): String = MessageDigest.getInstance("SHA-256")
    .digest(token.toByteArray(Charsets.US_ASCII)).joinToString("") { "%02x".format(it) }
