package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PracticeGapSummaryTest {
    private val engine = PilotEngine()
    private val plan = PracticePlanner().plan("verb-article-gaps", "verb-article-gaps-1", 0)
    private val item = plan.items.first()
    private val emptyContext = PracticeAttemptContext(false, false, false, emptyList())

    @Test
    fun `all nine gap states project exact per-slot outcomes while session attempts count once`() {
        val source = requireNotNull(item.exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val prompt = requireNotNull(item.exercise.prompt.gapPrompt).slots.associateBy { it.id }
        listOf<String?>(null, "correct", "wrong").forEach { verbState ->
            listOf<String?>(null, "correct", "wrong").forEach { articleState ->
                val selections = listOfNotNull(
                    selection("g0", verbState, source, prompt),
                    selection("g1", articleState, source, prompt),
                )
                val evaluation = engine.evaluate(item.exercise, Answer.Gaps(selections))
                val summary = summary(evaluation)
                val verb = summary.rows.single { it.targetSkillId == "G004" }
                val article = summary.rows.single { it.targetSkillId == "G006" }
                val results = requireNotNull(evaluation.gapResults).associateBy { it.slotId }

                assertEquals(1, summary.attemptCount)
                assertEquals(0, summary.retryCount)
                assertCounts(results.getValue("g0"), verb.first)
                assertCounts(results.getValue("g1"), article.first)
                assertEquals("primary", verb.assessmentRole)
                assertEquals("support", article.assessmentRole)
            }
        }
    }

    @Test
    fun `first and latest stay distinct and prior exposure is filtered to each assessment key`() {
        val source = requireNotNull(item.exercise.inspector.gapSource).slots
        val correct = source.map { GapSelection(it.slotId, it.expectedChoiceId) }
        val wrong = requireNotNull(item.exercise.prompt.gapPrompt).slots.map { slot ->
            GapSelection(slot.id, slot.choices.single { it.id != source.single { row -> row.slotId == slot.id }.expectedChoiceId }.id)
        }
        val first = engine.evaluate(item.exercise, Answer.Gaps(wrong))
        val latest = engine.evaluate(item.exercise, Answer.Gaps(correct))
        val articleKey = item.assessments.single { it.slotId == "g1" }.answerExposureKeys.single()
        val context = emptyContext.copy(
            priorExposure = listOf(PracticePriorExposure(0, PracticeExposureSource.FEEDBACK, listOf(articleKey))),
        )
        val summary = summary(first to context, latest to emptyContext)
        val verb = summary.rows.single { it.targetSkillId == "G004" }
        val article = summary.rows.single { it.targetSkillId == "G006" }

        assertEquals(2, summary.attemptCount)
        assertEquals(1, summary.retryCount)
        assertEquals(1, verb.first.targetNotMet)
        assertEquals(1, verb.latest.targetMet)
        assertEquals(0, verb.firstWithPriorExposure)
        assertEquals(1, article.firstWithPriorExposure)
    }

    @Test
    fun `missing duplicate or wrong-role gap rows are rejected`() {
        val evaluation = engine.evaluate(item.exercise, Answer.Gaps(emptyList()))
        val rows = requireNotNull(evaluation.gapResults)
        listOf(
            rows.dropLast(1),
            listOf(rows.first(), rows.first()),
            rows.map { if (it.slotId == "g0") it.copy(assessmentRole = GapAssessmentRole.SUPPORT) else it },
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { summary(evaluation.copy(gapResults = invalid)) }
        }
    }

    @Test
    fun `plan summary rejects duplicate observed positions before projecting assessments`() {
        val observations = plan.items.map { planned ->
            PracticeSummaryItemObservation(planned, false, false, false, emptyList())
        }.toMutableList()
        observations[1] = observations[1].copy(item = observations[1].item.copy(position = 0))
        assertFailsWith<IllegalArgumentException> {
            PracticeSummaryCalculator.calculate(plan, false, observations)
        }
    }

    @Test
    fun `a frozen plan with duplicate assessment slots cannot manufacture summary rows`() {
        val malformed = plan.copy(items = plan.items.map { planned ->
            if (planned.position == 0) {
                planned.copy(assessments = listOf(planned.assessments.first(), planned.assessments.first()))
            } else planned
        })
        val failure = assertFailsWith<IllegalArgumentException> {
            PracticeSummaryCalculator.calculate(
                malformed,
                false,
                malformed.items.map { PracticeSummaryItemObservation(it, false, false, false, emptyList()) },
            )
        }
        assertEquals("Practice assessment slots must be unique", failure.message)
    }

    @Test
    fun `single target follow up assessments use whole outcomes without gap rows`() {
        val sources = plan.items.map { PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments) }
        val child = PracticeFollowUpPlanner().planGuidedGaps(
            "follow-up-3", 0, listOf("grammar:G006:selection"), sources,
        )
        val observations = child.items.map { planned ->
            PracticeSummaryItemObservation(
                planned,
                advanced = true,
                lessonOpened = false,
                hintOpened = false,
                attempts = listOf(
                    PracticeSummaryAttemptObservation(
                        TargetStatus.MET,
                        LinguisticStatus.ACCEPTABLE,
                        ResponseConstraintStatus.MET,
                        emptyContext,
                    ),
                ),
            )
        }
        val summary = PracticeSummaryCalculator.calculate(child, false, observations)
        assertEquals(4, summary.attemptCount)
        assertEquals(4, summary.rows.single().first.fullyMet)
        assertEquals("recall", summary.rows.single().assessmentMode)
        assertEquals("primary", summary.rows.single().assessmentRole)
    }

    private fun summary(vararg attempts: Pair<Evaluation, PracticeAttemptContext>): PracticeSummary =
        PracticeSummaryCalculator.calculate(
            plan,
            false,
            plan.items.map { planned ->
                PracticeSummaryItemObservation(
                    planned,
                    advanced = false,
                    lessonOpened = false,
                    hintOpened = false,
                    attempts = if (planned.position == item.position) attempts.map { (evaluation, context) ->
                        evaluation.toObservation(context)
                    } else emptyList(),
                )
            },
        )

    private fun summary(evaluation: Evaluation): PracticeSummary = summary(evaluation to emptyContext)

    private fun Evaluation.toObservation(context: PracticeAttemptContext) = PracticeSummaryAttemptObservation(
        targetStatus,
        linguisticStatus,
        responseConstraintStatus,
        context,
        gapResults = requireNotNull(gapResults),
    )

    private fun selection(
        slotId: String,
        state: String?,
        source: Map<String, GapSlotSourceDetails>,
        prompt: Map<String, GapSlot>,
    ): GapSelection? = state?.let {
        val expected = source.getValue(slotId).expectedChoiceId
        val choiceId = if (it == "correct") {
            expected
        } else {
            prompt.getValue(slotId).choices.single { choice -> choice.id != expected }.id
        }
        GapSelection(slotId, choiceId)
    }

    private fun assertCounts(result: GapResult, counts: PracticeOutcomeCounts) {
        assertEquals(count(result.targetStatus == TargetStatus.MET), counts.targetMet)
        assertEquals(count(result.targetStatus == TargetStatus.NOT_MET), counts.targetNotMet)
        assertEquals(count(result.targetStatus == TargetStatus.UNEVALUABLE), counts.unevaluable)
        assertEquals(count(result.linguisticStatus == LinguisticStatus.UNKNOWN), counts.languageUnknown)
        assertEquals(count(result.linguisticStatus == LinguisticStatus.KNOWN_ISSUE), counts.languageKnownIssue)
        assertEquals(count(result.responseConstraintStatus == ResponseConstraintStatus.NOT_MET), counts.responseConstraintNotMet)
    }

    private fun count(value: Boolean): Int = if (value) 1 else 0
}
