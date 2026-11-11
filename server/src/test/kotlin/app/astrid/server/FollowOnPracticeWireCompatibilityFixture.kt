package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlan
import app.astrid.engine.PracticePlanner
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.PracticeAttemptSource
import app.astrid.server.exercises.PracticeExposureState
import app.astrid.server.exercises.toAttemptContext
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticeProfileDto
import app.astrid.server.practice.PracticeProfilesResponse
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.StoredPracticeAttempt
import app.astrid.server.practice.StoredPracticeItem
import app.astrid.server.practice.StoredPracticeSession
import app.astrid.server.practice.toNewPracticeSession
import app.astrid.server.practice.toPayload
import app.astrid.server.practice.toResponse
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.encodeToString

internal object FollowOnPracticeWireCompatibilityFixture {
    const val RELEASE_SOURCE_COMMIT = "b82ad3615b74de932ae903e7d479d454debc0740"
    const val RELEASE_VERSION = "0.13.0"
    private val ownerId = uuid(900)
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    private val profiles = listOf(
        "module-one-possession" to "module-one-possession-1",
        "module-one-reading-words" to "module-one-reading-words-1",
    )

    fun rows(): List<String> = buildList {
        add(row("PROFILES", hash(wireJson.encodeToString(profileProjection()))))
        profiles.forEachIndexed { profileIndex, (focus, revision) ->
            listOf(0, 1, 47, Int.MAX_VALUE).forEachIndexed { seedIndex, seed ->
                val sessionId = uuid(910 + profileIndex * 100 + seedIndex)
                val request = PracticeSessionRequest(sessionId.toString(), focus, revision, seed)
                val plan = planner.plan(focus, revision, seed)
                val created = deterministic(plan.toNewPracticeSession(ownerId), 1_000 + profileIndex * 100 + seedIndex * 10)
                val initial = stored(sessionId, focus, revision, seed, created)
                val introduced = stored(sessionId, focus, revision, seed, created, introductionOpened = true)
                val supported = stored(sessionId, focus, revision, seed, created, introductionOpened = true, supported = 0)
                add(
                    row(
                        "CREATE", focus, seed.toString(),
                        hash(wireJson.encodeToString(request), wireJson.encodeToString(created.toPayload())),
                    ),
                )
                add(row("INITIAL", focus, seed.toString(), hash(wireJson.encodeToString(initial.toResponse()))))
                add(row("INTRODUCED", focus, seed.toString(), hash(wireJson.encodeToString(introduced.toResponse()))))
                add(row("SUPPORTED", focus, seed.toString(), hash(wireJson.encodeToString(supported.toResponse()))))

                val completedAttempts = completedAttempts(plan, created)
                val attempted = stored(
                    sessionId, focus, revision, seed, created, introductionOpened = true, supported = 0,
                    attempts = mapOf(0 to completedAttempts.getValue(0).take(1)),
                )
                val completed = stored(
                    sessionId, focus, revision, seed, created, cursor = 4, introductionOpened = true, supported = 0,
                    attempts = completedAttempts,
                )
                add(row("ATTEMPTED", focus, seed.toString(), hash(wireJson.encodeToString(attempted.toResponse()))))
                add(row("COMPLETED", focus, seed.toString(), hash(wireJson.encodeToString(completed.toResponse()))))

                if (seed == 47) addAll(eligibilityRows(sessionId, focus, revision, seed, plan, created))
                val firstAttempt = completedAttempts.getValue(0).first()
                val replayRequest = AttemptRequest(
                    firstAttempt.response.attemptId,
                    created.items[0].exercise.id.toString(),
                    firstAttempt.answer,
                    false,
                )
                add(
                    row(
                        "ATTEMPT_REPLAY", focus, seed.toString(),
                        hash(
                            wireJson.encodeToString(replayRequest), wireJson.encodeToString(replayRequest),
                            wireJson.encodeToString(firstAttempt.response), wireJson.encodeToString(firstAttempt.response),
                            wireJson.encodeToString(firstAttempt.context),
                        ),
                    ),
                )
            }
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=$RELEASE_VERSION")
        appendLine("captureDate=2026-11-11")
        appendLine("planPairs=module-one-possession-1:pilot-11/generator-11/evaluation-11," +
            "module-one-reading-words-1:pilot-5/generator-5/evaluation-5")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=The four-item S07 possession and S08 reading-meaning plans at four seeds; exact profile/request/" +
                "private-plan and public lifecycle wire, scoped eligibility, correct-only reading feedback exposure, " +
                "frozen context, and request/response replay. No follow-up fallback.",
        )
    }
    fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    fun samples(): List<String> {
        return profiles.flatMapIndexed { index, (focus, revision) ->
            val plan = planner.plan(focus, revision, 47)
            val created = deterministic(plan.toNewPracticeSession(ownerId), 1_500 + index * 100)
            val initial = stored(uuid(999 + index), focus, revision, 47, created)
            val completed = stored(
                uuid(999 + index), focus, revision, 47, created, 4, true, 0, completedAttempts(plan, created),
            )
            listOf(
                wireJson.encodeToString(created.toPayload()),
                wireJson.encodeToString(initial.toResponse()),
                wireJson.encodeToString(completed.toResponse()),
            )
        }
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

    private fun eligibilityRows(
        id: UUID,
        focus: String,
        revision: String,
        seed: Int,
        plan: PracticePlan,
        session: NewPracticeSession,
    ): List<String> {
        val cases = if (focus == "module-one-reading-words") {
            listOf("correct", "known-wrong", "unknown", "wrong-kind", "wrong-then-correct")
        } else {
            listOf("correct", "known-wrong", "case", "shape", "unknown")
        }
        return cases.map { case ->
            val attempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
            val position = 0
            if (case == "wrong-then-correct") {
                addAttempt(plan, session, position, "known-wrong", attempts)
                addAttempt(plan, session, position, "correct", attempts)
            } else {
                if (case == "unknown") addAttempt(plan, session, position, "correct", attempts)
                addAttempt(plan, session, position, case, attempts)
            }
            val response = stored(id, focus, revision, seed, session, introductionOpened = true, attempts = attempts)
            row("ELIGIBILITY", focus, case, hash(wireJson.encodeToString(response.toResponse())))
        }
    }

    private fun completedAttempts(plan: PracticePlan, session: NewPracticeSession): Map<Int, List<StoredPracticeAttempt>> {
        val attempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        plan.items.forEachIndexed { position, item ->
            val first = if (item.exercise.formatId == "E011") {
                listOf("unknown", "known-wrong", "correct", "known-wrong")[position % 4]
            } else {
                listOf("unknown", "known-wrong", "case", "correct")[position % 4]
            }
            addAttempt(plan, session, position, first, attempts)
            if (first != "correct") addAttempt(plan, session, position, "correct", attempts)
        }
        return attempts
    }

    private fun addAttempt(
        plan: PracticePlan,
        session: NewPracticeSession,
        position: Int,
        state: String,
        attempts: MutableMap<Int, MutableList<StoredPracticeAttempt>>,
    ) {
        val planned = plan.items.single { it.position == position }
        val item = session.items.single { it.position == position }
        val source = planned.exercise.inspector.moduleOneSource
        val lexical = planned.exercise.inspector.lexicalSource
        val lexicalExpected = lexical?.let { "${it.article} ${it.headword}" }
        val answer = when (state) {
            "correct" -> Answer.Text(lexicalExpected ?: requireNotNull(source).expectedForm)
            "known-wrong" -> Answer.Text(
                if (lexicalExpected != null) planned.exercise.prompt.choices.first { it != lexicalExpected }
                else requireNotNull(source).knownAlternativeForms.first(),
            )
            "case" -> Answer.Text(requireNotNull(source).expectedForm.replaceFirstChar(Char::uppercase))
            "shape" -> Answer.Text("vi sover")
            "unknown" -> Answer.Text("ukendt")
            "wrong-kind" -> Answer.Order(emptyList())
            else -> error("Unsupported state $state")
        }
        val evaluation = engine.evaluate(planned.exercise, answer)
        val context = PracticeAttemptSource(
            plan.focusId, plan.planRevision, plan.seed, position, true,
            plan.items.map { observation ->
                val responses = attempts[observation.position].orEmpty().map { it.response }
                val feedbackReleased = if (plan.exposurePolicyId == "module-one-reading-word-exposure-1") {
                    responses.any {
                        it.targetSatisfaction == "met" && it.linguisticStatus == "acceptable" &&
                            it.responseConstraintStatus == "met"
                    }
                } else {
                    responses.isNotEmpty()
                }
                PracticeExposureState(
                    observation.position, observation.answerExposureKeys, observation.promptExposureKeys,
                    observation.lessonExposureKeys, observation.hintExposureKeys, observation.feedbackExposureKeys,
                    lessonOpened = observation.position == 0, hintOpened = observation.position == 0,
                    feedbackReleased = feedbackReleased,
                )
            },
            introductionExposureKeys = plan.introductionExposureKeys,
            exposurePolicyId = plan.exposurePolicyId,
        ).toAttemptContext()
        val ordinal = attempts.values.sumOf { it.size } + 1
        val response = AttemptResponse(
            uuid(2_000 + ordinal).toString(), item.exercise.id.toString(), evaluation.targetStatus.wireValue,
            evaluation.linguisticStatus.wireValue, evaluation.issues.map { IssueDto(it.code, it.message) },
            evaluation.feedback, evaluation.completedSentence.orEmpty(), plan.evaluationRevision, false,
            attempts[position].orEmpty().size + 1, 1_800_000L + ordinal, evaluation.linguisticScope?.wireValue,
            evaluation.responseConstraintStatus?.wireValue, evaluation.meaningSatisfaction?.wireValue,
        )
        attempts.getOrPut(position, ::mutableListOf).add(StoredPracticeAttempt(response, context, answer.toDto()))
    }

    private fun Answer.toDto(): AttemptAnswerDto = when (this) {
        is Answer.Text -> AttemptAnswerDto.Text(text)
        is Answer.Order -> AttemptAnswerDto.Order(blockIds)
        is Answer.Matching -> AttemptAnswerDto.Matching(
            pairs.map { app.astrid.server.exercises.MatchingPairDto(it.leftId, it.rightId) },
        )
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
        supported: Int? = null,
        attempts: Map<Int, List<StoredPracticeAttempt>> = emptyMap(),
    ) = StoredPracticeSession(
        id, ownerId, 1_800_000, focus, session.title, revision, seed, session.contentRevision, session.generatorRevision,
        session.evaluationRevision, cursor, introductionOpened, session.prerequisiteSkillIds, session.introduction,
        session.items.map { item ->
            StoredPracticeItem(
                item.position, item.exercise, item.targetLabel, item.answerExposureKeys, item.promptExposureKeys,
                item.lessonExposureKeys, item.hintExposureKeys, item.feedbackExposureKeys,
                item.position == supported, item.position == supported, attempts[item.position].orEmpty(),
                item.assessmentMode, item.assessments, item.targetScopeId,
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
}
