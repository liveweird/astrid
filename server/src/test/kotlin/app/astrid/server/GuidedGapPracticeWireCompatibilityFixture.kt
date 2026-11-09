package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.GapSelection
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticeFollowUpPlanner
import app.astrid.engine.PracticeFollowUpSourceItem
import app.astrid.engine.PracticePlan
import app.astrid.engine.PracticePlanner
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.GapResultDto
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.PracticeAttemptSource
import app.astrid.server.exercises.PracticeExposureState
import app.astrid.server.exercises.frozenFeedbackExposureKeys
import app.astrid.server.exercises.toAttemptContext
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.NewPracticeItem
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticeFollowUpRequest
import app.astrid.server.practice.PracticeProfileDto
import app.astrid.server.practice.PracticeProfilesResponse
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.PracticeSupportRequest
import app.astrid.server.practice.StoredPracticeItem
import app.astrid.server.practice.StoredPracticeAttempt
import app.astrid.server.practice.StoredPracticeSession
import app.astrid.server.practice.toFollowUp
import app.astrid.server.practice.toNewPracticeSession
import app.astrid.server.practice.toPayload
import app.astrid.server.practice.toResponse
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.encodeToString

internal object GuidedGapPracticeWireCompatibilityFixture {
    const val BASELINE_COMMIT = "0dfc00afdc2bd21f5f06899ff47554689f0ae0c9"
    const val RELEASE_SOURCE_COMMIT = "cdb6e066288550fb958efab1e103f538c03824a5"
    const val RELEASE_VERSION = "0.8.0"
    const val PLAN_REVISION = "verb-article-gaps-1"
    const val FOLLOW_UP_REVISION = "follow-up-3"

    private val ownerId = UUID.fromString("00000000-0000-0000-0000-000000000800")
    private val sourceId = UUID.fromString("00000000-0000-0000-0000-000000000801")
    private val childId = UUID.fromString("00000000-0000-0000-0000-000000000802")
    private val grandchildId = UUID.fromString("00000000-0000-0000-0000-000000000803")
    private val planner = PracticePlanner()
    private val engine = PilotEngine()

    fun rows(): List<String> = buildList {
        add(row("PROFILES", hash(wireJson.encodeToString(profiles()))))
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val request = PracticeSessionRequest(sourceId.toString(), "verb-article-gaps", PLAN_REVISION, seed)
            add(row("CREATE", seed.toString(), hash(wireJson.encodeToString(request))))
            val plan = planner.plan("verb-article-gaps", PLAN_REVISION, seed)
            val newSession = deterministic(plan.toNewPracticeSession(ownerId))
            val initial = stored(sourceId, "verb-article-gaps", PLAN_REVISION, seed, newSession)
            val supported = stored(
                sourceId,
                "verb-article-gaps",
                PLAN_REVISION,
                seed,
                newSession,
                introductionOpened = true,
                supportedPosition = 0,
            )
            add(
                row(
                    "INITIAL_WIRE",
                    seed.toString(),
                    hash(wireJson.encodeToString(newSession.toPayload()), wireJson.encodeToString(initial.toResponse())),
                ),
            )
            add(row("SUPPORTED_WIRE", seed.toString(), hash(wireJson.encodeToString(supported.toResponse()))))
            if (seed == 47) {
                val attempts = attempts(plan, newSession)
                val attempted = stored(
                    sourceId,
                    "verb-article-gaps",
                    PLAN_REVISION,
                    seed,
                    newSession,
                    introductionOpened = true,
                    supportedPosition = 0,
                    attemptsByPosition = mapOf(0 to attempts.getValue(0)),
                )
                val completed = stored(
                    sourceId,
                    "verb-article-gaps",
                    PLAN_REVISION,
                    seed,
                    newSession,
                    cursor = newSession.items.size,
                    introductionOpened = true,
                    supportedPosition = 0,
                    attemptsByPosition = attempts,
                )
                add(row("ATTEMPTED_WIRE", hash(wireJson.encodeToString(attempted.toResponse()))))
                add(row("COMPLETED_WIRE", hash(wireJson.encodeToString(completed.toResponse()))))
            }
        }
        listOf(
            PracticeSupportRequest("introduction"),
            PracticeSupportRequest("lesson", 0),
            PracticeSupportRequest("hint", 0),
        ).forEach { request ->
            add(
                row(
                    "SUPPORT",
                    request.kind,
                    request.position?.toString().orEmpty(),
                    hash(wireJson.encodeToString(request)),
                ),
            )
        }

        val sourcePlan = planner.plan("verb-article-gaps", PLAN_REVISION, 47)
        val sourceNew = deterministic(sourcePlan.toNewPracticeSession(ownerId))
        val sourceStored = stored(
            sourceId,
            "verb-article-gaps",
            PLAN_REVISION,
            47,
            sourceNew,
            cursor = sourceNew.items.size,
            introductionOpened = true,
            supportedPosition = 0,
            attemptsByPosition = attempts(sourcePlan, sourceNew),
        )
        val selections = listOf(
            listOf("grammar:G004:selection"),
            listOf("grammar:G006:selection"),
            listOf("grammar:G004:selection", "grammar:G006:selection"),
        )
        var recursiveSource: StoredPracticeSession? = null
        val engineSource = sourcePlan.items.map {
            PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments)
        }
        selections.forEachIndexed { index, selected ->
            val request = PracticeFollowUpRequest(childId.toString(), FOLLOW_UP_REVISION, Int.MAX_VALUE, selected)
            val newChild = deterministic(
                sourceStored.toFollowUp(request, sourceId, ownerId, app.astrid.engine.PilotEngine()),
                idBase = 850 + index * 10,
            )
            val storedChild = stored(childId, "targeted-review", FOLLOW_UP_REVISION, Int.MAX_VALUE, newChild)
            add(
                row(
                    "FOLLOW_UP_WIRE",
                    selected.joinToString(","),
                    hash(
                        wireJson.encodeToString(request),
                        wireJson.encodeToString(newChild.toPayload()),
                        wireJson.encodeToString(storedChild.toResponse()),
                    ),
                ),
            )
            if (selected.size == 2) {
                val childPlan = PracticeFollowUpPlanner(engine).planGuidedGaps(
                    FOLLOW_UP_REVISION,
                    Int.MAX_VALUE,
                    selected,
                    engineSource,
                )
                recursiveSource = stored(
                    childId,
                    "targeted-review",
                    FOLLOW_UP_REVISION,
                    Int.MAX_VALUE,
                    newChild,
                    cursor = newChild.items.size,
                    introductionOpened = true,
                    attemptsByPosition = attempts(childPlan, newChild),
                )
            }
        }
        val recursiveRequest = PracticeFollowUpRequest(
            grandchildId.toString(),
            FOLLOW_UP_REVISION,
            0,
            listOf("grammar:G004:selection", "grammar:G006:recall"),
        )
        val recursiveChild = deterministic(
            requireNotNull(recursiveSource).toFollowUp(recursiveRequest, childId, ownerId, engine),
            idBase = 890,
        )
        add(
            row(
                "RECURSIVE_FOLLOW_UP_WIRE",
                hash(
                    wireJson.encodeToString(recursiveRequest),
                    wireJson.encodeToString(recursiveChild.toPayload()),
                    wireJson.encodeToString(
                        stored(
                            grandchildId,
                            "targeted-review",
                            FOLLOW_UP_REVISION,
                            0,
                            recursiveChild,
                        ).toResponse(),
                    ),
                ),
            ),
        )
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int): String = buildString {
        appendLine("baselineCommit=$BASELINE_COMMIT")
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=$RELEASE_VERSION")
        appendLine("captureDate=2026-11-09")
        appendLine("planRevision=$PLAN_REVISION")
        appendLine("followUpRevision=$FOLLOW_UP_REVISION")
        appendLine("contentRevision=pilot-7")
        appendLine("generatorRevision=generator-7")
        appendLine("evaluationRevision=evaluation-7")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=Published 0.8.0 guided-gap profile, creation/support serialization, private plan payloads, " +
                "supported/attempted/completed public wire, and initial/recursive follow-up-3 payloads before new fields.",
        )
    }

    fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    fun frozenWireSamples(): List<String> {
        val plan = planner.plan("verb-article-gaps", PLAN_REVISION, 47)
        val newSession = deterministic(plan.toNewPracticeSession(ownerId))
        val attemptRows = attempts(plan, newSession)
        val supported = stored(
            sourceId,
            "verb-article-gaps",
            PLAN_REVISION,
            47,
            newSession,
            introductionOpened = true,
            supportedPosition = 0,
        )
        val attempted = stored(
            sourceId,
            "verb-article-gaps",
            PLAN_REVISION,
            47,
            newSession,
            introductionOpened = true,
            supportedPosition = 0,
            attemptsByPosition = mapOf(0 to attemptRows.getValue(0)),
        )
        val completed = stored(
            sourceId,
            "verb-article-gaps",
            PLAN_REVISION,
            47,
            newSession,
            cursor = newSession.items.size,
            introductionOpened = true,
            supportedPosition = 0,
            attemptsByPosition = attemptRows,
        )
        return listOf(
            wireJson.encodeToString(newSession.toPayload()),
            wireJson.encodeToString(supported.toResponse()),
            wireJson.encodeToString(attempted.toResponse()),
            wireJson.encodeToString(completed.toResponse()),
        )
    }

    private fun profiles() = PracticeProfilesResponse(
        ownerId.toString(),
        1_800_000,
        listOf(planner.profiles().single { it.focusId == "verb-article-gaps" }).map {
            PracticeProfileDto(
                it.focusId,
                it.planRevision,
                it.title,
                it.description,
                it.itemCount,
                it.prerequisiteSkillIds,
            )
        },
    )

    private fun deterministic(session: NewPracticeSession, idBase: Int = 810): NewPracticeSession = session.copy(
        items = session.items.mapIndexed { index, item -> deterministic(item, idBase + index) },
    )

    private fun deterministic(item: NewPracticeItem, suffix: Int): NewPracticeItem {
        val id = UUID.fromString("00000000-0000-0000-0000-${suffix.toString().padStart(12, '0')}")
        return item.copy(
            exercise = item.exercise.copy(
                id = id,
                presentation = item.exercise.presentation.copy(id = id.toString()),
            ),
        )
    }

    private fun stored(
        id: UUID,
        focusId: String,
        planRevision: String,
        seed: Int,
        session: NewPracticeSession,
        cursor: Int = 0,
        introductionOpened: Boolean = false,
        supportedPosition: Int? = null,
        attemptsByPosition: Map<Int, List<StoredPracticeAttempt>> = emptyMap(),
    ) = StoredPracticeSession(
        id,
        ownerId,
        1_800_000,
        focusId,
        session.title,
        planRevision,
        seed,
        session.contentRevision,
        session.generatorRevision,
        session.evaluationRevision,
        cursor,
        introductionOpened,
        session.prerequisiteSkillIds,
        session.introduction,
        session.items.map { item ->
            StoredPracticeItem(
                item.position,
                item.exercise,
                item.targetLabel,
                item.answerExposureKeys,
                item.promptExposureKeys,
                item.lessonExposureKeys,
                item.hintExposureKeys,
                item.feedbackExposureKeys,
                lessonOpened = item.position == supportedPosition,
                hintOpened = item.position == supportedPosition,
                attempts = attemptsByPosition[item.position].orEmpty(),
                item.assessmentMode,
                item.assessments,
            )
        },
        session.followUp,
        session.sourceExposure,
        session.introductionExposureKeys,
    )

    private fun attempts(plan: PracticePlan, session: NewPracticeSession): Map<Int, List<StoredPracticeAttempt>> {
        val storedAttempts = mutableMapOf<Int, MutableList<StoredPracticeAttempt>>()
        if (plan.items.first().exercise.formatId == "E015") {
            val firstStates = listOf("correct", "wrong", "partial", "unknown")
            firstStates.forEach { state -> addAttempt(plan, session, 0, state, storedAttempts) }
            plan.items.drop(1).forEach { item -> addAttempt(plan, session, item.position, "correct", storedAttempts) }
        } else {
            plan.items.forEach { item -> addAttempt(plan, session, item.position, "correct", storedAttempts) }
        }
        return storedAttempts
    }

    private fun addAttempt(
        plan: PracticePlan,
        session: NewPracticeSession,
        position: Int,
        state: String,
        attempts: MutableMap<Int, MutableList<StoredPracticeAttempt>>,
    ) {
        val planned = plan.items.single { it.position == position }
        val storedItem = session.items.single { it.position == position }
        val (engineAnswer, answer) = answer(planned.exercise, state)
        val context = PracticeAttemptSource(
            plan.focusId,
            plan.planRevision,
            plan.seed,
            position,
            introductionOpened = true,
            items = plan.items.map { item ->
                val responses = attempts[item.position].orEmpty().map { it.response }
                PracticeExposureState(
                    item.position,
                    item.answerExposureKeys,
                    item.promptExposureKeys,
                    item.lessonExposureKeys,
                    item.hintExposureKeys,
                    frozenFeedbackExposureKeys(item.feedbackExposureKeys, item.assessments, responses),
                    lessonOpened = item.position == 0,
                    hintOpened = item.position == 0,
                    feedbackReleased = responses.isNotEmpty(),
                )
            },
        ).toAttemptContext()
        val retry = attempts[position].orEmpty().size + 1
        val response = attemptResponse(
            planned.exercise,
            engineAnswer,
            storedItem.exercise.id,
            attemptId = 900 + attempts.values.sumOf { it.size },
            retry = retry,
        )
        attempts.getOrPut(position, ::mutableListOf).add(StoredPracticeAttempt(response, context, answer))
    }

    private fun answer(exercise: GeneratedExercise, state: String): Pair<Answer, AttemptAnswerDto> {
        if (exercise.formatId == "E015") {
            val selections = gapSelections(exercise, state)
            return Answer.Gaps(selections) to AttemptAnswerDto.Gaps(
                selections.map { app.astrid.server.exercises.GapSelectionDto(it.slotId, it.choiceId) },
            )
        }
        require(state == "correct") { "Only correct retained follow-up answers are frozen" }
        val candidates = (exercise.prompt.choices + listOf("en", "et")).distinct()
        val text = candidates.single { engine.evaluate(exercise, Answer.Text(it)).targetStatus.wireValue == "met" }
        return Answer.Text(text) to AttemptAnswerDto.Text(text)
    }

    private fun gapSelections(exercise: GeneratedExercise, state: String): List<GapSelection> {
        val source = requireNotNull(exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val prompt = requireNotNull(exercise.prompt.gapPrompt).slots.associateBy { it.id }
        fun selected(slotId: String, correct: Boolean): GapSelection {
            val expected = source.getValue(slotId).expectedChoiceId
            val choice = if (correct) expected else prompt.getValue(slotId).choices.single { it.id != expected }.id
            return GapSelection(slotId, choice)
        }
        return when (state) {
            "correct" -> listOf(selected("g0", true), selected("g1", true))
            "wrong" -> listOf(selected("g0", false), selected("g1", false))
            "partial" -> listOf(selected("g0", true))
            "unknown" -> emptyList()
            else -> error("Unsupported attempt state $state")
        }
    }

    private fun attemptResponse(
        exercise: GeneratedExercise,
        engineAnswer: Answer,
        exerciseId: UUID,
        attemptId: Int,
        retry: Int,
    ): AttemptResponse {
        val evaluation = engine.evaluate(exercise, engineAnswer)
        return AttemptResponse(
            attemptId = UUID.fromString("00000000-0000-0000-0000-${attemptId.toString().padStart(12, '0')}").toString(),
            exerciseId = exerciseId.toString(),
            targetSatisfaction = evaluation.targetStatus.wireValue,
            linguisticStatus = evaluation.linguisticStatus.wireValue,
            issues = evaluation.issues.map { IssueDto(it.code, it.message) },
            feedback = evaluation.feedback,
            completedSentence = evaluation.completedSentence.orEmpty(),
            evaluationRevision = "evaluation-7",
            hintUsed = false,
            retryNumber = retry,
            createdAt = 1_000L + attemptId,
            linguisticScope = evaluation.linguisticScope?.wireValue,
            responseConstraintStatus = evaluation.responseConstraintStatus?.wireValue,
            meaningSatisfaction = evaluation.meaningSatisfaction?.wireValue,
            gapResults = evaluation.gapResults?.map { result ->
                GapResultDto(
                    result.slotId,
                    result.choiceId,
                    result.targetSkillId,
                    result.assessmentRole.wireValue,
                    result.targetStatus.wireValue,
                    result.responseConstraintStatus.wireValue,
                    result.linguisticStatus.wireValue,
                    result.feedback,
                )
            },
        )
    }

    private fun hash(vararg fields: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
            digest.update(bytes)
        }
        return digest.digest().toHex()
    }

    private fun row(vararg fields: String) = fields.joinToString("\t")

    private fun ByteArray.toHex() = joinToString("") { byte -> "%02x".format(byte) }
}
