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
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal object VocabularyP1RevisionTwoPracticeWireCompatibilityFixture {
    const val RELEASE_SOURCE_COMMIT = "9eefcfed708670ce45dbdd9b11adc41f9fe17d25"
    const val RELEASE_VERSION = "0.20.1"
    const val PROJECTION_SCHEMA = "p1-revision-two-server-wire-v1"
    const val CAPTURE_DATE = "2026-11-12"
    private val ownerId = uuid(1_800)
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    private val profiles = (1..5).map { number ->
        "vocabulary-p1-s$number" to "vocabulary-p1-s$number-2"
    }

    fun rows(): List<String> = buildList {
        add(row("PROFILES", hash(wireJson.encodeToString(profileProjection()))))
        profiles.forEachIndexed { profileIndex, (focus, revision) ->
            listOf(0, 1, 47, Int.MAX_VALUE).forEachIndexed { seedIndex, seed ->
                val sessionId = uuid(1_810 + profileIndex * 100 + seedIndex)
                val request = PracticeSessionRequest(sessionId.toString(), focus, revision, seed)
                val plan = planner.plan(focus, revision, seed)
                val created = deterministic(plan.toNewPracticeSession(ownerId), 2_000 + profileIndex * 100 + seedIndex * 10)
                val initial = stored(sessionId, focus, revision, seed, created)
                val taught = stored(sessionId, focus, revision, seed, created, introductionOpened = true)
                val supported = stored(
                    sessionId, focus, revision, seed, created, introductionOpened = true, lessonOpened = 0, hintOpened = 0,
                )
                add(
                    row(
                        "CREATE", focus, seed.toString(),
                        hash(wireJson.encodeToString(request), wireJson.encodeToString(created.toPayload())),
                    ),
                )
                add(
                    row(
                        "PLAN_TASK_ORDER", focus, revision, seed.toString(),
                        plan.items.joinToString(",") { requireNotNull(it.exercise.inspector.vocabularySource).taskId },
                    ),
                )
                val payloadJson = wireJson.encodeToString(created.toPayload())
                val restored = wireJson.decodeFromString<PracticePlanPayload>(payloadJson)
                add(row("PAYLOAD_ROUND_TRIP", focus, seed.toString(), hash(payloadJson, wireJson.encodeToString(restored))))
                add(row("INITIAL", focus, seed.toString(), hash(wireJson.encodeToString(initial.toResponse()))))
                add(
                    row(
                        "BEFORE_TEACHING", focus, seed.toString(),
                        hash(
                            "409", ProblemJson.withCharset(Charsets.UTF_8).toString(),
                            wireJson.encodeToString(
                                ProblemDetail(
                                    title = "Conflict", status = 409,
                                    detail = "Open the practice introduction before attempting this item",
                                    instance = "/api/v1/attempts", code = "conflict",
                                ),
                            ),
                        ),
                    ),
                )
                add(row("TAUGHT", focus, seed.toString(), hash(wireJson.encodeToString(taught.toResponse()))))
                add(row("SUPPORTED", focus, seed.toString(), hash(wireJson.encodeToString(supported.toResponse()))))

                val completedAttempts = completedAttempts(plan, created)
                val attempted = stored(
                    sessionId, focus, revision, seed, created, introductionOpened = true,
                    attempts = mapOf(0 to completedAttempts.getValue(0).take(1)),
                )
                val completed = stored(
                    sessionId, focus, revision, seed, created, cursor = plan.items.size,
                    introductionOpened = true, attempts = completedAttempts,
                )
                add(row("ATTEMPTED", focus, seed.toString(), hash(wireJson.encodeToString(attempted.toResponse()))))
                add(row("COMPLETED", focus, seed.toString(), hash(wireJson.encodeToString(completed.toResponse()))))

                val firstAttempt = completedAttempts.getValue(0).first()
                val savedRequest = AttemptRequest(
                    firstAttempt.response.attemptId, created.items[0].exercise.id.toString(), firstAttempt.answer, false,
                )
                add(
                    row(
                        "SAVED_ATTEMPT", focus, seed.toString(),
                        hash(
                            wireJson.encodeToString(savedRequest), wireJson.encodeToString(firstAttempt.response),
                            wireJson.encodeToString(firstAttempt.context),
                        ),
                    ),
                )
                if (seed == 47) addAll(boundaryRows(sessionId, focus, revision, seed, plan, created))
            }
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=$RELEASE_VERSION")
        appendLine("captureDate=$CAPTURE_DATE")
        appendLine("projectionSchema=$PROJECTION_SCHEMA")
        appendLine("planPairs=${profiles.joinToString(",") { "${it.second}:pilot-18/generator-18/evaluation-18" }}")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=Five literal P1 profiles with 4/4/5/5/5 items at four boundary seeds; selected profiles, requests, " +
                "private plans, payload round-trips, saved contexts, required teaching, support, actual confirmed-only feedback " +
                "exposure, first/latest outcomes, meaning counts, reading roles, matching targets/results and eligibility.",
        )
    }

    fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    fun samples(): List<String> = profiles.flatMapIndexed { index, (focus, revision) ->
        val plan = planner.plan(focus, revision, 47)
        val created = deterministic(plan.toNewPracticeSession(ownerId), 4_000 + index * 100)
        val initial = stored(uuid(4_900 + index), focus, revision, 47, created)
        val completed = stored(
            uuid(4_900 + index), focus, revision, 47, created, plan.items.size, true,
            attempts = completedAttempts(plan, created),
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
            ownerId.toString(), 1_800_000,
            selected.map {
                PracticeProfileDto(
                    it.focusId, it.planRevision, it.title, it.description, it.itemCount, it.prerequisiteSkillIds,
                )
            },
        )
    }

    private fun boundaryRows(
        id: UUID,
        focus: String,
        revision: String,
        seed: Int,
        plan: PracticePlan,
        session: NewPracticeSession,
    ): List<String> = buildList {
        val canonical = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        addAttempt(plan, session, 0, AnswerCase.CORRECT, canonical)
        add(row("ELIGIBILITY", focus, "correct", hash(wireJson.encodeToString(
            stored(id, focus, revision, seed, session, introductionOpened = true, attempts = canonical).toResponse(),
        ))))

        val wrong = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        addAttempt(plan, session, 0, AnswerCase.KNOWN_WRONG, wrong)
        add(row("ELIGIBILITY", focus, "known-wrong", hash(wireJson.encodeToString(
            stored(id, focus, revision, seed, session, introductionOpened = true, attempts = wrong).toResponse(),
        ))))

        val matchingPosition = plan.items.indexOfFirst { it.exercise.formatId == "E033" }
        if (matchingPosition >= 0) {
            val swapped = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
            addAttempt(plan, session, matchingPosition, AnswerCase.KNOWN_WRONG, swapped)
            add(row("MATCHING_BOUNDARY", focus, "complete-swapped", hash(wireJson.encodeToString(
                stored(
                    id, focus, revision, seed, session, cursor = matchingPosition,
                    introductionOpened = true, attempts = swapped,
                ).toResponse(),
            ))))
            val partial = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
            addAttempt(plan, session, matchingPosition, AnswerCase.PARTIAL, partial)
            add(row("MATCHING_BOUNDARY", focus, "partial", hash(wireJson.encodeToString(
                stored(
                    id, focus, revision, seed, session, cursor = matchingPosition,
                    introductionOpened = true, attempts = partial,
                ).toResponse(),
            ))))
        }

        val typedPosition = plan.items.indexOfFirst { it.exercise.formatId in setOf("E034", "E035", "E036") }
        if (typedPosition >= 0) {
            val latestUnknown = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
            addAttempt(plan, session, typedPosition, AnswerCase.CORRECT, latestUnknown)
            addAttempt(plan, session, typedPosition, AnswerCase.UNKNOWN, latestUnknown)
            add(row("LATEST_BOUNDARY", focus, "unknown-after-eligible", hash(wireJson.encodeToString(
                stored(
                    id, focus, revision, seed, session, cursor = typedPosition,
                    introductionOpened = true, lessonOpened = typedPosition, hintOpened = typedPosition,
                    attempts = latestUnknown,
                ).toResponse(),
            ))))
        }
    }

    private fun completedAttempts(
        plan: PracticePlan,
        session: NewPracticeSession,
    ): Map<Int, List<StoredPracticeAttempt>> {
        val attempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        plan.items.forEachIndexed { position, _ ->
            addAttempt(plan, session, position, AnswerCase.KNOWN_WRONG, attempts)
            addAttempt(plan, session, position, AnswerCase.CORRECT, attempts)
        }
        return attempts
    }

    private fun addAttempt(
        plan: PracticePlan,
        session: NewPracticeSession,
        position: Int,
        answerCase: AnswerCase,
        attempts: MutableMap<Int, MutableList<StoredPracticeAttempt>>,
    ) {
        val planned = plan.items.single { it.position == position }
        val item = session.items.single { it.position == position }
        val answer = answer(planned.exercise, answerCase)
        val evaluation = engine.evaluate(planned.exercise, answer)
        val context = PracticeAttemptSource(
            plan.focusId, plan.planRevision, plan.seed, position, true,
            plan.items.map { observation ->
                val responses = attempts[observation.position].orEmpty().map { it.response }
                val filteredFeedback = frozenFeedbackExposureKeys(
                    observation.feedbackExposureKeys,
                    observation.assessments.map { assessment ->
                        app.astrid.server.practice.PracticeAssessmentPayload(
                            assessment.slotId,
                            assessment.targetDescriptor.label,
                            assessment.targetDescriptor.targetSkillId,
                            assessment.targetDescriptor.assessmentMode,
                            assessment.assessmentRole,
                            assessment.answerExposureKeys,
                            assessment.targetDescriptor.targetScopeId,
                            assessment.targetDescriptor.lexicalTarget?.let {
                                LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction)
                            },
                            assessment.targetDescriptor.readingTarget?.let {
                                app.astrid.server.exercises.ReadingTaskDto(it.facet)
                            },
                            assessment.targetDescriptor.readingRole,
                            assessment.targetDescriptor.matchingTarget?.let {
                                app.astrid.server.exercises.LexicalTaskDto(it.facet, it.direction)
                            },
                        ).toEngineAssessment()
                    },
                    responses,
                    plan.exposurePolicyId,
                )
                PracticeExposureState(
                    observation.position, observation.answerExposureKeys, observation.promptExposureKeys,
                    observation.lessonExposureKeys, observation.hintExposureKeys, filteredFeedback,
                    lessonOpened = observation.position == position, hintOpened = observation.position == position,
                    feedbackReleased = responses.isNotEmpty(),
                )
            },
            introductionExposureKeys = plan.introductionExposureKeys,
            exposurePolicyId = plan.exposurePolicyId,
        ).toAttemptContext()
        val ordinal = attempts.values.sumOf { it.size } + 1
        val response = response(
            evaluation,
            uuid(5_000 + ordinal).toString(),
            item.exercise.id.toString(),
            plan,
            attempts[position].orEmpty().size + 1,
        )
        attempts.getOrPut(position, ::mutableListOf).add(StoredPracticeAttempt(response, context, answer.toDto()))
    }

    private fun response(
        evaluation: Evaluation,
        attemptId: String,
        exerciseId: String,
        plan: PracticePlan,
        retry: Int,
    ) = AttemptResponse(
        attemptId, exerciseId, evaluation.targetStatus.wireValue, evaluation.linguisticStatus.wireValue,
        evaluation.issues.map { IssueDto(it.code, it.message) }, evaluation.feedback, evaluation.completedSentence.orEmpty(),
        plan.evaluationRevision, false, retry, 1_800_000L + retry,
        evaluation.linguisticScope?.wireValue, evaluation.responseConstraintStatus?.wireValue,
        evaluation.meaningSatisfaction?.wireValue, evaluation.matchingResults?.map { result ->
            MatchingPairResultDto(
                result.leftId, result.rightId,
                LexicalTargetDto(
                    result.lexicalTarget.entryId, result.lexicalTarget.senseId,
                    result.lexicalTarget.facet, result.lexicalTarget.direction,
                ),
                result.targetStatus.wireValue, result.meaningSatisfaction.wireValue,
                result.responseConstraintStatus.wireValue, result.linguisticStatus.wireValue, result.feedback,
            )
        },
    )

    private fun answer(exercise: app.astrid.engine.GeneratedExercise, answerCase: AnswerCase): Answer {
        val source = requireNotNull(exercise.inspector.vocabularySource)
        return when (answerCase) {
            AnswerCase.CORRECT -> when (exercise.formatId) {
                "E033" -> Answer.Matching(source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) })
                "E037", "E038" -> Answer.Order(source.acceptedOrders.single())
                "E039" -> Answer.Text(source.supportFacts.single { it.factId == source.targetSupportFactId }.valueText)
                else -> Answer.Text(requireNotNull(source.expectedAnswer))
            }
            AnswerCase.KNOWN_WRONG -> knownWrongAnswer(exercise.formatId, source)
            AnswerCase.PARTIAL -> {
                val pairs = source.matchingPairs.dropLast(1).map { MatchingPair(it.leftId, it.rightId) }
                Answer.Matching(pairs)
            }
            AnswerCase.UNKNOWN -> Answer.Text("ukendt svar")
        }
    }

    private fun knownWrongAnswer(
        formatId: String,
        source: app.astrid.engine.VocabularyExerciseSourceDetails,
    ): Answer = when (formatId) {
        "E033" -> {
            val pairs = source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) }.toMutableList()
            pairs[0] = pairs[0].copy(rightId = pairs[1].rightId)
            pairs[1] = pairs[1].copy(rightId = source.matchingPairs[0].rightId)
            Answer.Matching(pairs)
        }
        "E037", "E038" -> source.reviewedOrders.firstOrNull { it.issueCode != null }?.words?.let { words ->
            val occurrences = source.tileOccurrences.groupBy { it.form }.mapValues { it.value.toMutableList() }
            Answer.Order(words.map { word -> requireNotNull(occurrences.getValue(word).removeFirstOrNull()).blockId })
        } ?: Answer.Order(source.acceptedOrders.single().reversed())
        "E039" -> Answer.Text(
            source.suppliedChoices.first {
                it != source.supportFacts.single { fact -> fact.factId == source.targetSupportFactId }.valueText
            },
        )
        "E031", "E032" -> Answer.Text(source.suppliedChoices.first { it != source.expectedAnswer })
        else -> Answer.Text(source.knownAlternatives.first().text)
    }

    private fun Answer.toDto(): AttemptAnswerDto = when (this) {
        is Answer.Text -> AttemptAnswerDto.Text(text)
        is Answer.Order -> AttemptAnswerDto.Order(blockIds)
        is Answer.Matching -> AttemptAnswerDto.Matching(pairs.map { MatchingPairDto(it.leftId, it.rightId) })
        is Answer.Gaps -> AttemptAnswerDto.Gaps(
            selections.map { app.astrid.server.exercises.GapSelectionDto(it.slotId, it.choiceId) },
        )
    }

    private fun deterministic(session: NewPracticeSession, base: Int) = session.copy(
        items = session.items.mapIndexed { index, item ->
            val id = uuid(base + index)
            item.copy(exercise = item.exercise.copy(id = id, presentation = item.exercise.presentation.copy(id = id.toString())))
        },
    )

    private fun stored(
        id: UUID,
        focus: String,
        revision: String,
        seed: Int,
        session: NewPracticeSession,
        cursor: Int = 0,
        introductionOpened: Boolean = false,
        lessonOpened: Int? = null,
        hintOpened: Int? = null,
        attempts: Map<Int, List<StoredPracticeAttempt>> = emptyMap(),
    ) = StoredPracticeSession(
        id, ownerId, 1_800_000, focus, session.title, revision, seed, session.contentRevision, session.generatorRevision,
        session.evaluationRevision, cursor, introductionOpened, session.prerequisiteSkillIds, session.introduction,
        session.items.map { item ->
            StoredPracticeItem(
                item.position, item.exercise, item.targetLabel, item.answerExposureKeys, item.promptExposureKeys,
                item.lessonExposureKeys, item.hintExposureKeys, item.feedbackExposureKeys,
                item.position == lessonOpened, item.position == hintOpened, attempts[item.position].orEmpty(),
                item.assessmentMode, item.assessments, item.targetScopeId, item.readingRole, item.matchingTarget,
            )
        },
        session.followUp, session.sourceExposure, session.introductionExposureKeys, session.introductionRequired,
        session.completionRuleId, session.exposurePolicyId,
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
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun uuid(value: Int) = UUID.fromString("00000000-0000-0000-0000-${value.toString().padStart(12, '0')}")
    private enum class AnswerCase { CORRECT, KNOWN_WRONG, PARTIAL, UNKNOWN }
}
