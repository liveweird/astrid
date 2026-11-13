@file:Suppress("ThrowsCount") // Route validation maps each independent client fault to a safe problem.

package app.astrid.server.exercises

import app.astrid.engine.Answer
import app.astrid.engine.Evaluation
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.GenerationRequest
import app.astrid.engine.MatchingColumns
import app.astrid.engine.MatchingItem
import app.astrid.engine.PilotEngine
import app.astrid.server.authz.Authenticator
import app.astrid.server.authz.BadRequest
import app.astrid.server.authz.Conflict
import app.astrid.server.authz.EvaluationUnavailable
import app.astrid.server.authz.NotFound
import app.astrid.server.authz.caller
import app.astrid.server.authz.parseStrictUuid
import app.astrid.server.authz.requireInspectorKey
import app.astrid.server.authz.requireMutationHeaders
import app.astrid.server.authz.uuidParameter
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.nio.charset.StandardCharsets
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString

private const val MAX_IDENTIFIER_LENGTH = 32
private const val MAX_REVISION_LENGTH = 64
private const val MAX_GENERATION_INDEX = 1_000_000
private const val MAX_TEXT_LENGTH = 512
private const val MAX_BLOCK_COUNT = 16

/** Catalog, playground exercises, attempts and the development inspector. */
internal fun Route.exerciseRoutes(
    config: ServerConfig,
    exercises: ExerciseStore,
    authenticator: Authenticator,
    engine: PilotEngine,
    evaluator: suspend (GeneratedExercise, Answer) -> Evaluation,
) {
    catalogRoute(authenticator, engine)
    playgroundExerciseRoutes(exercises, authenticator, engine)
    attemptRoutes(exercises, authenticator, engine, evaluator)
    inspectorRoute(config, exercises, authenticator, engine)
}

private fun Route.catalogRoute(authenticator: Authenticator, engine: PilotEngine) {
    get("/catalog") {
        call.caller(authenticator)
        val catalog = engine.catalog()
        call.respond(
            CatalogResponse(
                catalog.contentRevision,
                catalog.generatorRevision,
                checkNotNull(engine.evaluationRevision(catalog.contentRevision, catalog.generatorRevision)) {
                    "Catalog exposed an unsupported revision pair"
                },
                catalog.skills.map { SkillDto(it.id, it.title, it.explanation, it.prerequisiteIds) },
                catalog.formats.map { format ->
                    FormatDto(
                        format.id,
                        format.title,
                        format.targetSkillId,
                        format.lexicalTask?.let { LexicalTaskDto(it.facet, it.direction) },
                        format.readingTask?.let { ReadingTaskDto(it.facet) },
                    )
                },
                catalog.sentenceCount,
            ),
        )
    }
}

private fun Route.playgroundExerciseRoutes(exercises: ExerciseStore, authenticator: Authenticator, engine: PilotEngine) {
    post("/exercises") {
        requireMutationHeaders(call)
        val session = call.caller(authenticator)
        val request = call.receive<ExerciseRequest>().validated()
        val generated = generate(engine, request)
        val id = UUID.randomUUID()
        val presentation = generated.toResponse(id.toString())
        exercises.insertExercise(request.toStored(id, session.id, presentation, generated.reproductionFingerprint()))
        call.response.headers.append(HttpHeaders.Location, "/api/v1/exercises/$id")
        call.respond(HttpStatusCode.Created, presentation)
    }
    get("/exercises/{id}") {
        val session = call.caller(authenticator)
        val stored = exercises.findExercise(session.id, call.uuidParameter("id")) ?: throw NotFound()
        call.respond(stored.presentation)
    }
}

private fun Route.attemptRoutes(
    exercises: ExerciseStore,
    authenticator: Authenticator,
    engine: PilotEngine,
    evaluator: suspend (GeneratedExercise, Answer) -> Evaluation,
) {
    post("/attempts") {
        requireMutationHeaders(call)
        val session = call.caller(authenticator)
        val request = call.receive<AttemptRequest>().validated()
        val result = exercises.writeAttemptWithPractice(
            session.id,
            request,
            wireJson.encodeToString(request),
        ) { stored, retry, time, practice ->
            val context = practice?.toAttemptContext()
            CreatedAttempt(evaluateAttempt(engine, evaluator, stored, request, retry, time), context)
        }
        when (result) {
            is AttemptWriteResult.Created -> call.respond(result.response)
            is AttemptWriteResult.Existing -> call.respond(result.response)
            AttemptWriteResult.PayloadConflict -> throw Conflict("Attempt id was already used with a different payload")
            AttemptWriteResult.ExerciseMissing -> throw NotFound("Exercise not found")
            AttemptWriteResult.PracticePositionConflict -> throw Conflict("Practice item is no longer current")
            AttemptWriteResult.PracticeIntroductionRequired -> throw Conflict("Open the practice introduction before attempting this item")
        }
    }
    get("/attempts/{id}") {
        val session = call.caller(authenticator)
        call.respond(exercises.findAttempt(session.id, call.uuidParameter("id")) ?: throw NotFound())
    }
}

private fun Route.inspectorRoute(
    config: ServerConfig,
    exercises: ExerciseStore,
    authenticator: Authenticator,
    engine: PilotEngine,
) {
    get("/dev/exercises/{id}") {
        if (!config.developmentMode || !config.inspectorEnabled) throw NotFound()
        requireInspectorKey(call, config.inspectorKey!!)
        val session = call.caller(authenticator)
        val stored = exercises.findExercise(session.id, call.uuidParameter("id")) ?: throw NotFound()
        call.respond(regenerateVerified(engine, stored).toInspector(stored.id.toString()))
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
    request.answer.validateFor(engine, generated)
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
        checkNotNull(engine.evaluationRevision(generated.contentRevision, generated.generatorRevision)) {
            "Regenerated exercise has an unsupported revision pair"
        },
        request.hintUsed,
        retry,
        createdAt,
        evaluation.linguisticScope?.wireValue,
        evaluation.responseConstraintStatus?.wireValue,
        evaluation.meaningSatisfaction?.wireValue,
        evaluation.matchingResults?.map { result ->
            MatchingPairResultDto(
                leftId = result.leftId,
                rightId = result.rightId,
                lexicalTarget = result.lexicalTarget.let {
                    LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction)
                },
                targetSatisfaction = result.targetStatus.wireValue,
                meaningSatisfaction = result.meaningSatisfaction.wireValue,
                responseConstraintStatus = result.responseConstraintStatus.wireValue,
                linguisticStatus = result.linguisticStatus.wireValue,
                feedback = result.feedback,
            )
        },
        evaluation.gapResults?.map { result ->
            GapResultDto(
                slotId = result.slotId,
                choiceId = result.choiceId,
                targetSkillId = result.targetSkillId,
                assessmentRole = result.assessmentRole.wireValue,
                targetSatisfaction = result.targetStatus.wireValue,
                responseConstraintStatus = result.responseConstraintStatus.wireValue,
                linguisticStatus = result.linguisticStatus.wireValue,
                feedback = result.feedback,
            )
        },
    )
}

private fun ExerciseRequest.validated(): ExerciseRequest {
    if (formatId.length !in 1..MAX_IDENTIFIER_LENGTH ||
        (targetSkillId != null && targetSkillId.length !in 1..MAX_IDENTIFIER_LENGTH)) {
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
        is AttemptAnswerDto.Text -> {
            if (!StandardCharsets.UTF_8.newEncoder().canEncode(value.text)) {
                throw BadRequest("Text answer must contain well-formed Unicode")
            }
            if (value.text.codePointCount(0, value.text.length) > MAX_TEXT_LENGTH) {
                throw BadRequest("Text answer is too long")
            }
        }
        is AttemptAnswerDto.Order -> if (value.blockIds.size > MAX_BLOCK_COUNT ||
            value.blockIds.any { it.length !in 1..MAX_REVISION_LENGTH }) throw BadRequest("Invalid block answer")
        is AttemptAnswerDto.Matching -> Unit
        is AttemptAnswerDto.Gaps -> Unit
    }
    return this
}

private fun AttemptAnswerDto.validateFor(engine: PilotEngine, exercise: GeneratedExercise) {
    val suppliedBlocks = exercise.prompt.blocks.map { it.id }.toSet()
    when (exercise.formatId) {
        "E003", "E024", "E029", "E030", "E037", "E043" -> validateCompleteOrderAnswer(this, suppliedBlocks)
        "E012", "E013", "E038" -> validateTileOrderAnswer(this, suppliedBlocks)
        "E025" -> validateTileOrderAnswer(this, suppliedBlocks, requireNonempty = true)
        "E011", "E016", "E018", "E020", "E022", "E026", "E028", "E031", "E032", "E039",
        "E040", "E041", "E044",
        ->
            validateSuppliedChoiceAnswer(this, engine, exercise)
        "E014", "E033", "E042" -> validateMatchingAnswer(this, exercise)
        "E015" -> validateGapAnswer(this, exercise)
        else -> validateTextAnswer(this)
    }
}

private fun validateCompleteOrderAnswer(answer: AttemptAnswerDto, suppliedBlocks: Set<String>) {
    val order = answer as? AttemptAnswerDto.Order ?: throw BadRequest("This exercise requires an order answer")
    val valid = listOf(order.blockIds.size == suppliedBlocks.size, order.blockIds.toSet() == suppliedBlocks).all { it }
    if (!valid) throw BadRequest("The answer must contain every supplied block exactly once")
}

private fun validateTileOrderAnswer(answer: AttemptAnswerDto, suppliedBlocks: Set<String>, requireNonempty: Boolean = false) {
    val order = answer as? AttemptAnswerDto.Order ?: throw BadRequest("This exercise requires an order answer")
    val valid = listOf(
        !requireNonempty || order.blockIds.isNotEmpty(),
        order.blockIds.size == order.blockIds.toSet().size,
        suppliedBlocks.containsAll(order.blockIds),
    ).all { it }
    if (!valid) throw BadRequest("The answer must use distinct supplied block ids")
}

private fun validateSuppliedChoiceAnswer(answer: AttemptAnswerDto, engine: PilotEngine, exercise: GeneratedExercise) {
    val text = answer as? AttemptAnswerDto.Text ?: throw BadRequest("This exercise requires a text answer")
    if (!engine.isSuppliedChoice(exercise, text.text)) throw BadRequest("The answer must be one of the supplied choices")
}

private fun validateTextAnswer(answer: AttemptAnswerDto) {
    if (answer !is AttemptAnswerDto.Text) throw BadRequest("This exercise requires a text answer")
}

private fun validateMatchingAnswer(answer: AttemptAnswerDto, exercise: GeneratedExercise) {
    val matching = answer as? AttemptAnswerDto.Matching
        ?: throw BadRequest("This exercise requires a matching answer")
    val columns = checkNotNull(exercise.prompt.matchingColumns) { "Matching exercise has no matching columns" }
    val leftIds = columns.left.map { it.id }.toSet()
    val rightIds = columns.right.map { it.id }.toSet()
    val suppliedLeft = matching.pairs.map { it.leftId }
    val suppliedRight = matching.pairs.map { it.rightId }
    val valid = listOf(
        matching.pairs.size <= leftIds.size,
        suppliedLeft.all(::isValidMatchingId),
        suppliedRight.all(::isValidMatchingId),
        suppliedLeft.size == suppliedLeft.toSet().size,
        suppliedRight.size == suppliedRight.toSet().size,
        leftIds.containsAll(suppliedLeft),
        rightIds.containsAll(suppliedRight),
    ).all { it }
    if (!valid) throw BadRequest("The answer must pair distinct supplied matching ids")
}

private fun isValidMatchingId(value: String) = value.length in 1..MAX_REVISION_LENGTH

private fun validateGapAnswer(answer: AttemptAnswerDto, exercise: GeneratedExercise) {
    val gaps = answer as? AttemptAnswerDto.Gaps
        ?: throw BadRequest("This exercise requires a gap answer")
    val slots = checkNotNull(exercise.prompt.gapPrompt) { "Gap exercise has no gap prompt" }.slots
    val slotsById = slots.associateBy { it.id }
    val suppliedSlotIds = gaps.selections.map { it.slotId }
    val valid = listOf(
        gaps.selections.size <= slots.size,
        suppliedSlotIds.size == suppliedSlotIds.toSet().size,
        gaps.selections.all { selection ->
            isValidMatchingId(selection.slotId) &&
                isValidMatchingId(selection.choiceId) &&
                slotsById[selection.slotId]?.choices?.any { it.id == selection.choiceId } == true
        },
    ).all { it }
    if (!valid) throw BadRequest("The answer must select at most one supplied choice for each gap")
}

private fun generate(engine: PilotEngine, request: ExerciseRequest) =
    if (engine.evaluationRevision(request.contentRevision, request.generatorRevision) == null) {
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

internal fun regenerateVerified(engine: PilotEngine, stored: StoredExercise): GeneratedExercise {
    val generated = regenerate(engine, stored)
    if (generated.reproductionFingerprint() == stored.reproductionFingerprint) return generated
    if (stored.contentRevision != LEGACY_VOCABULARY_CONTENT_REVISION ||
        stored.generatorRevision != LEGACY_VOCABULARY_GENERATOR_REVISION ||
        stored.formatId != LEGACY_VOCABULARY_MATCHING_FORMAT
    ) {
        throw reproductionConflict()
    }
    val columns = stored.presentation.prompt.matchingColumns ?: throw reproductionConflict()
    val restored = try {
        engine.restoreLegacyVocabularyMatching(
            stored.toGenerationRequest(),
            MatchingColumns(
                columns.left.map { MatchingItem(it.id, it.text) },
                columns.right.map { MatchingItem(it.id, it.text) },
            ),
        )
    } catch (cause: IllegalArgumentException) {
        throw reproductionConflict().apply { initCause(cause) }
    }
    if (restored.reproductionFingerprint() != stored.reproductionFingerprint ||
        restored.toResponse(stored.id.toString()) != stored.presentation
    ) {
        throw reproductionConflict()
    }
    return restored
}

private fun reproductionConflict() = Conflict("Exercise reproduction no longer matches its stored presentation")

private fun ExerciseRequest.toGenerationRequest() =
    GenerationRequest(formatId, targetSkillId, seed, index, contentRevision, generatorRevision)

private fun StoredExercise.toGenerationRequest() =
    GenerationRequest(formatId, targetSkillId, seed, index, contentRevision, generatorRevision)

private const val LEGACY_VOCABULARY_CONTENT_REVISION = "pilot-17"
private const val LEGACY_VOCABULARY_GENERATOR_REVISION = "generator-17"
private const val LEGACY_VOCABULARY_MATCHING_FORMAT = "E033"

private fun ExerciseRequest.toStored(
    id: UUID,
    sessionId: UUID,
    presentation: ExerciseResponse,
    fingerprint: String,
) = StoredExercise(id, sessionId, formatId, targetSkillId, seed, index, contentRevision, generatorRevision, presentation, fingerprint)
