package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.GeneratedExercise
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

internal object AbilityPracticeWireCompatibilityFixture {
    const val RELEASE_SOURCE_COMMIT = "43e9726b079f4483103badbd87d2209da943adb8"
    const val RELEASE_VERSION = "0.15.0"
    private val ownerId = uuid(900)
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    private val profiles = listOf(
        "module-one-ability" to "module-one-ability-1",
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
        appendLine("planPairs=module-one-ability-1:pilot-13/generator-13/evaluation-13")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=The four-item S10 ability plan at four seeds; exact profile/request/private-plan and public " +
                "lifecycle wire, two selection then two recall items, full teaching and exposure keys, selected-vs-typed " +
                "marker constraints, scoped eligibility, frozen context and request/response replay. No follow-up fallback.",
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

    fun markerConstraintStatuses(): Map<String, String?> {
        val plan = planner.plan("module-one-ability", "module-one-ability-1", 47)
        return mapOf(
            "selection" to engine.evaluate(plan.items[0].exercise, Answer.Text("at læse"))
                .responseConstraintStatus?.wireValue,
            "recall" to engine.evaluate(plan.items[2].exercise, Answer.Text("at læse"))
                .responseConstraintStatus?.wireValue,
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

    private fun eligibilityRows(
        id: UUID,
        focus: String,
        revision: String,
        seed: Int,
        plan: PracticePlan,
        session: NewPracticeSession,
    ): List<String> {
        val cases = listOf(
            Triple("correct", 0, "correct"),
            Triple("at-marker-selected", 0, "at-marker"),
            Triple("finite", 0, "finite"),
            Triple("case", 0, "case"),
            Triple("unknown", 0, "unknown"),
            Triple("at-marker-typed", 2, "at-marker"),
            Triple("wrong-kind", 2, "wrong-kind"),
        )
        return cases.map { (case, position, state) ->
            val attempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
            if (case == "unknown") addAttempt(plan, session, position, "correct", attempts)
            addAttempt(plan, session, position, state, attempts)
            val response = stored(
                id, focus, revision, seed, session, cursor = position, introductionOpened = true, attempts = attempts,
            )
            row("ELIGIBILITY", focus, case, hash(wireJson.encodeToString(response.toResponse())))
        }
    }

    private fun completedAttempts(plan: PracticePlan, session: NewPracticeSession): Map<Int, List<StoredPracticeAttempt>> {
        val attempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        plan.items.forEachIndexed { position, _ ->
            val first = listOf("unknown", "finite", "at-marker", "case")[position]
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
        val answer = answerForState(planned.exercise, state)
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

    private fun answerForState(exercise: GeneratedExercise, state: String): Answer {
        val source = exercise.inspector.moduleOneSource
        val lexical = exercise.inspector.lexicalSource
        val lexicalExpected = lexical?.let { "${it.article} ${it.headword}" }
        return when (state) {
            "correct" -> Answer.Text(lexicalExpected ?: requireNotNull(source).expectedForm)
            "known-wrong" -> Answer.Text(
                if (lexicalExpected != null) exercise.prompt.choices.first { it != lexicalExpected }
                else requireNotNull(source).knownAlternativeForms.first(),
            )
            "at-marker" -> Answer.Text("at læse")
            "finite" -> Answer.Text("læser")
            "case" -> Answer.Text(requireNotNull(source).expectedForm.replaceFirstChar(Char::uppercase))
            "shape" -> Answer.Text("vi sover")
            "unknown" -> Answer.Text("ukendt")
            "wrong-kind" -> Answer.Order(emptyList())
            else -> error("Unsupported state $state")
        }
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
