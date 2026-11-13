package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.Evaluation
import app.astrid.engine.MatchingPair
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlan
import app.astrid.engine.PracticePlanner
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.MatchingPairDto
import app.astrid.server.exercises.MatchingPairResultDto
import app.astrid.server.exercises.PracticeAttemptSource
import app.astrid.server.exercises.PracticeExposureState
import app.astrid.server.exercises.frozenFeedbackExposureKeys
import app.astrid.server.exercises.toAttemptContext
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticeAssessmentPayload
import app.astrid.server.practice.PracticePlanPayload
import app.astrid.server.practice.PracticeProfileDto
import app.astrid.server.practice.PracticeProfilesResponse
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.StoredPracticeAttempt
import app.astrid.server.practice.StoredPracticeItem
import app.astrid.server.practice.StoredPracticeSession
import app.astrid.server.practice.toEngineAssessment
import app.astrid.server.practice.toNewPracticeSession
import app.astrid.server.practice.toPayload
import app.astrid.server.practice.toResponse
import app.astrid.server.plugins.ProblemDetail
import app.astrid.server.plugins.ProblemJson
import io.ktor.http.withCharset
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal object VocabularyP2PracticeWireCompatibilityFixture {
    private val ownerId = uuid(19_000)
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    val profiles = (1..6).map { "vocabulary-p2-s$it" to "vocabulary-p2-s$it-1" }

    fun rows(): List<String> = buildList {
        add(row("PROFILES", hash(wireJson.encodeToString(profileProjection()))))
        profiles.forEachIndexed { profileIndex, (focus, revision) ->
            SEEDS.forEachIndexed seedLoop@{ seedIndex, seed ->
                val sessionId = uuid(19_100 + profileIndex * 100 + seedIndex)
                val request = PracticeSessionRequest(sessionId.toString(), focus, revision, seed)
                val plan = try {
                    planner.plan(focus, revision, seed)
                } catch (failure: NoSuchElementException) {
                    add(row(
                        "CREATE_FAILURE", focus, seed.toString(),
                        hash(wireJson.encodeToString(request), failure::class.qualifiedName.orEmpty(), failure.message.orEmpty()),
                    ))
                    return@seedLoop
                }
                val created = plan.toNewPracticeSession(ownerId).deterministic(20_000 + profileIndex * 100 + seedIndex * 10)
                val initial = stored(sessionId, focus, revision, seed, created)
                val taught = initial.copy(introductionOpened = true)
                val supported = taught.copy(items = taught.items.mapIndexed { index, item ->
                    if (index == 0) item.copy(lessonOpened = true, hintOpened = true) else item
                })
                val attempts = completedAttempts(plan, created)
                val attempted = taught.copy(items = taught.items.map { item ->
                    if (item.position == 0) item.copy(attempts = attempts.getValue(0).take(1)) else item
                })
                val completed = taught.copy(cursor = plan.items.size, items = taught.items.map { item ->
                    item.copy(attempts = attempts.getValue(item.position))
                })
                val payload = wireJson.encodeToString(created.toPayload())
                val restored = wireJson.decodeFromString<PracticePlanPayload>(payload)
                add(row("CREATE", focus, seed.toString(), hash(wireJson.encodeToString(request), payload)))
                add(row("PLAN_TASK_ORDER", focus, revision, seed.toString(), plan.items.joinToString(",") {
                    requireNotNull(it.exercise.inspector.vocabularySource).taskId
                }))
                add(row("PAYLOAD_ROUND_TRIP", focus, seed.toString(), hash(payload, wireJson.encodeToString(restored))))
                add(row("INITIAL", focus, seed.toString(), hash(wireJson.encodeToString(initial.toResponse()))))
                add(row("BEFORE_TEACHING", focus, seed.toString(), hash(
                    "409", ProblemJson.withCharset(Charsets.UTF_8).toString(),
                    wireJson.encodeToString(ProblemDetail(
                        title = "Conflict",
                        status = 409,
                        detail = "Open the practice introduction before attempting this item",
                        instance = "/api/v1/attempts",
                        code = "conflict",
                    )),
                )))
                add(row("TAUGHT", focus, seed.toString(), hash(wireJson.encodeToString(taught.toResponse()))))
                add(row("SUPPORTED", focus, seed.toString(), hash(wireJson.encodeToString(supported.toResponse()))))
                add(row("ATTEMPTED", focus, seed.toString(), hash(wireJson.encodeToString(attempted.toResponse()))))
                add(row("COMPLETED", focus, seed.toString(), hash(wireJson.encodeToString(completed.toResponse()))))
                val saved = attempts.getValue(0).first()
                add(row("SAVED_GRADE", focus, seed.toString(), hash(
                    wireJson.encodeToString(AttemptRequest(
                        saved.response.attemptId, created.items[0].exercise.id.toString(), saved.answer, false,
                    )),
                    wireJson.encodeToString(saved.response), wireJson.encodeToString(saved.context),
                )))
            }
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=0.22.0")
        appendLine("captureDate=$CAPTURE_DATE")
        appendLine("projectionSchema=p2-practice-server-wire-v1")
        appendLine("planPairs=${profiles.joinToString(",") { "${it.second}:pilot-19/generator-19/evaluation-19" }}")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=Six literal P2 profiles with 3/4/4/3/3/4 items at boundary seeds; saved requests and private plans, " +
                "support, actual grades and contexts, completion, exposure, summaries and exact lifecycle wire.",
        )
    }

    fun samples(): List<String> = profiles.flatMapIndexed { index, (focus, revision) ->
        val plan = planner.plan(focus, revision, 47)
        val created = plan.toNewPracticeSession(ownerId).deterministic(25_000 + index * 100)
        val initial = stored(uuid(26_000 + index), focus, revision, 47, created)
        val attempts = completedAttempts(plan, created)
        val completed = initial.copy(
            cursor = plan.items.size,
            introductionOpened = true,
            items = initial.items.map { it.copy(attempts = attempts.getValue(it.position)) },
        )
        listOf(
            wireJson.encodeToString(created.toPayload()),
            wireJson.encodeToString(initial.toResponse()),
            wireJson.encodeToString(completed.toResponse()),
        )
    }

    private fun profileProjection(): PracticeProfilesResponse {
        val selected = planner.profiles().filter { candidate -> profiles.any { it.first == candidate.focusId } }
        return PracticeProfilesResponse(
            ownerId.toString(), 1_900_000,
            selected.map { PracticeProfileDto(
                it.focusId, it.planRevision, it.title, it.description, it.itemCount, it.prerequisiteSkillIds,
            ) },
        )
    }

    private fun completedAttempts(plan: PracticePlan, session: NewPracticeSession): Map<Int, List<StoredPracticeAttempt>> {
        val attempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        plan.items.forEachIndexed { position, item ->
            addAttempt(plan, session, position, wrongAnswer(item.exercise), attempts)
            addAttempt(plan, session, position, canonicalAnswer(item.exercise), attempts)
        }
        return attempts
    }

    private fun addAttempt(
        plan: PracticePlan,
        session: NewPracticeSession,
        position: Int,
        answer: Answer,
        attempts: MutableMap<Int, MutableList<StoredPracticeAttempt>>,
    ) {
        val planned = plan.items.single { it.position == position }
        val item = session.items.single { it.position == position }
        val evaluation = engine.evaluate(planned.exercise, answer)
        val context = PracticeAttemptSource(
            plan.focusId, plan.planRevision, plan.seed, position, true,
            plan.items.map { observation ->
                val priorResponses = attempts[observation.position].orEmpty().map { it.response }
                val assessments = observation.assessments.map { assessment -> assessment.payload().toEngineAssessment() }
                PracticeExposureState(
                    observation.position, observation.answerExposureKeys, observation.promptExposureKeys,
                    observation.lessonExposureKeys, observation.hintExposureKeys,
                    frozenFeedbackExposureKeys(
                        observation.feedbackExposureKeys, assessments, priorResponses, plan.exposurePolicyId,
                    ),
                    observation.position == position, observation.position == position, priorResponses.isNotEmpty(),
                )
            },
            introductionExposureKeys = plan.introductionExposureKeys,
            exposurePolicyId = plan.exposurePolicyId,
        ).toAttemptContext()
        val ordinal = attempts.values.sumOf { it.size } + 1
        val retry = attempts[position].orEmpty().size + 1
        val response = evaluation.response(
            uuid(27_000 + ordinal).toString(), item.exercise.id.toString(), plan.evaluationRevision, retry,
        )
        attempts.getOrPut(position, ::mutableListOf).add(StoredPracticeAttempt(response, context, answer.toDto()))
    }

    private fun app.astrid.engine.PracticeAssessment.payload() = PracticeAssessmentPayload(
        slotId, targetDescriptor.label, targetDescriptor.targetSkillId, targetDescriptor.assessmentMode,
        assessmentRole, answerExposureKeys, targetDescriptor.targetScopeId,
        targetDescriptor.lexicalTarget?.let { LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction) },
        targetDescriptor.readingTarget?.let { app.astrid.server.exercises.ReadingTaskDto(it.facet) },
        targetDescriptor.readingRole,
        targetDescriptor.matchingTarget?.let { app.astrid.server.exercises.LexicalTaskDto(it.facet, it.direction) },
    )

    private fun Evaluation.response(attemptId: String, exerciseId: String, revision: String, retry: Int) = AttemptResponse(
        attemptId, exerciseId, targetStatus.wireValue, linguisticStatus.wireValue,
        issues.map { IssueDto(it.code, it.message) }, feedback, completedSentence.orEmpty(), revision, false, retry,
        1_900_000L + retry, linguisticScope?.wireValue, responseConstraintStatus?.wireValue,
        meaningSatisfaction?.wireValue, matchingResults?.map { result -> MatchingPairResultDto(
            result.leftId, result.rightId,
            LexicalTargetDto(
                result.lexicalTarget.entryId, result.lexicalTarget.senseId,
                result.lexicalTarget.facet, result.lexicalTarget.direction,
            ),
            result.targetStatus.wireValue, result.meaningSatisfaction.wireValue,
            result.responseConstraintStatus.wireValue, result.linguisticStatus.wireValue, result.feedback,
        ) },
    )

    private fun wrongAnswer(exercise: app.astrid.engine.GeneratedExercise): Answer {
        val source = requireNotNull(exercise.inspector.vocabularySource)
        return when (exercise.formatId) {
            "E042" -> {
                val pairs = source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) }.toMutableList()
                val first = pairs[0].rightId
                pairs[0] = pairs[0].copy(rightId = pairs[1].rightId)
                pairs[1] = pairs[1].copy(rightId = first)
                Answer.Matching(pairs)
            }
            "E043" -> Answer.Order(source.acceptedOrders.single().reversed())
            else -> Answer.Text(source.suppliedChoices.firstOrNull { it != source.expectedAnswer } ?: "ukendt svar")
        }
    }

    private fun canonicalAnswer(exercise: app.astrid.engine.GeneratedExercise): Answer {
        val source = requireNotNull(exercise.inspector.vocabularySource)
        return when (exercise.formatId) {
            "E042" -> Answer.Matching(source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) })
            "E043" -> Answer.Order(source.acceptedOrders.single())
            else -> Answer.Text(requireNotNull(source.expectedAnswer))
        }
    }

    private fun Answer.toDto(): AttemptAnswerDto = when (this) {
        is Answer.Text -> AttemptAnswerDto.Text(text)
        is Answer.Order -> AttemptAnswerDto.Order(blockIds)
        is Answer.Matching -> AttemptAnswerDto.Matching(pairs.map { MatchingPairDto(it.leftId, it.rightId) })
        is Answer.Gaps -> AttemptAnswerDto.Gaps(
            selections.map { app.astrid.server.exercises.GapSelectionDto(it.slotId, it.choiceId) },
        )
    }

    private fun NewPracticeSession.deterministic(base: Int) = copy(items = items.mapIndexed { index, item ->
        val id = uuid(base + index)
        item.copy(exercise = item.exercise.copy(id = id, presentation = item.exercise.presentation.copy(id = id.toString())))
    })

    private fun stored(id: UUID, focus: String, revision: String, seed: Int, session: NewPracticeSession) = StoredPracticeSession(
        id, ownerId, 1_900_000, focus, session.title, revision, seed, session.contentRevision, session.generatorRevision,
        session.evaluationRevision, 0, false, session.prerequisiteSkillIds, session.introduction,
        session.items.map { item -> StoredPracticeItem(
            item.position, item.exercise, item.targetLabel, item.answerExposureKeys, item.promptExposureKeys,
            item.lessonExposureKeys, item.hintExposureKeys, item.feedbackExposureKeys, false, false, emptyList(),
            item.assessmentMode, item.assessments, item.targetScopeId, item.readingRole, item.matchingTarget, item.policy,
        ) },
        session.followUp, session.sourceExposure, session.introductionExposureKeys, session.introductionRequired,
        session.completionRuleId, session.exposurePolicyId, session.cumulativeReview,
    )

    private fun hash(vararg fields: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray()
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        return digest.digest().toHex()
    }
    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private fun uuid(value: Int) = UUID.fromString("00000000-0000-0000-0000-${value.toString().padStart(12, '0')}")

    const val RELEASE_SOURCE_COMMIT = "844b88af33cc729e095d443adb72fcd72cd17534"
    const val CAPTURE_DATE = "2026-11-13"
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}
