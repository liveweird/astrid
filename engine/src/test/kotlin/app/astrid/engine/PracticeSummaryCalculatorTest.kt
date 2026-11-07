package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeSummaryCalculatorTest {
    private val plan = PracticePlanner().plan("reading-basics", "practice-1", 0)
    private val emptyContext = PracticeAttemptContext(false, false, false, emptyList())

    @Test
    fun `summary keeps first and latest outcomes separate across retries`() {
        val observations = emptyObservations().toMutableList()
        observations[0] = observations[0].copy(
            advanced = true,
            attempts = listOf(
                attempt(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE),
                attempt(TargetStatus.MET, LinguisticStatus.ACCEPTABLE),
            ),
        )
        val summary = PracticeSummaryCalculator.calculate(plan, false, observations)
        val row = summary.rows.single { it.lexicalTarget?.senseId == "L014-S01" && it.lexicalTarget.direction == "da-en" }

        assertEquals(1, summary.attemptedItems)
        assertEquals(1, summary.completedItems)
        assertEquals(2, summary.attemptCount)
        assertEquals(1, summary.retryCount)
        assertEquals(1, row.first.targetNotMet)
        assertEquals(0, row.first.fullyMet)
        assertEquals(1, row.latest.targetMet)
        assertEquals(1, row.latest.fullyMet)
    }

    @Test
    fun `summary reports unknown language and failed response constraints without upgrading them`() {
        val observations = emptyObservations().toMutableList()
        observations[3] = observations[3].copy(
            lessonOpened = true,
            hintOpened = true,
            attempts = listOf(
                attempt(
                    target = TargetStatus.MET,
                    language = LinguisticStatus.UNKNOWN,
                    constraint = ResponseConstraintStatus.NOT_MET,
                    context = PracticeAttemptContext(
                        introductionOpened = true,
                        lessonOpened = true,
                        hintOpened = false,
                        priorExposure = listOf(
                            PracticePriorExposure(0, PracticeExposureSource.PROMPT, listOf("headword:L014-S01")),
                        ),
                    ),
                ),
            ),
        )
        val summary = PracticeSummaryCalculator.calculate(plan, true, observations)
        val row = summary.rows.single { it.lexicalTarget?.senseId == "L014-S01" && it.lexicalTarget.direction == "en-da" }

        assertTrue(summary.introductionOpened)
        assertEquals(1, row.first.targetMet)
        assertEquals(1, row.first.languageUnknown)
        assertEquals(1, row.first.responseConstraintNotMet)
        assertEquals(0, row.first.fullyMet)
        assertEquals(1, row.firstWithSupport)
        assertEquals(1, row.firstWithPriorExposure)
        assertEquals(1, row.lessonOpenedItems)
        assertEquals(1, row.hintOpenedItems)
    }

    @Test
    fun `all attempted outcomes may advance without a grading gate`() {
        val observations = emptyObservations().map { observation ->
            observation.copy(
                advanced = true,
                attempts = listOf(attempt(TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE)),
            )
        }
        val summary = PracticeSummaryCalculator.calculate(plan, false, observations)

        assertEquals(8, summary.attemptedItems)
        assertEquals(8, summary.completedItems)
        assertEquals(8, summary.attemptCount)
        assertEquals(0, summary.retryCount)
        assertTrue(summary.rows.all { it.first.targetNotMet == it.attemptedItems })
        assertTrue(summary.rows.all { it.first.languageKnownIssue == it.attemptedItems })
        assertFalse(summary.rows.any { it.first.fullyMet > 0 })
    }

    @Test
    fun `submitted hint use counts as support without rewriting context`() {
        val observations = emptyObservations().toMutableList()
        observations[0] = observations[0].copy(
            attempts = listOf(
                PracticeSummaryAttemptObservation(
                    targetStatus = TargetStatus.MET,
                    linguisticStatus = LinguisticStatus.ACCEPTABLE,
                    responseConstraintStatus = null,
                    context = emptyContext,
                    hintUsed = true,
                ),
            ),
        )

        val row = PracticeSummaryCalculator.calculate(plan, false, observations).rows.first()

        assertEquals(1, row.firstWithSupport)
        assertFalse(observations[0].attempts.single().context.hintOpened)
    }

    @Test
    fun `cross run exposure counts separately as possible earlier exposure`() {
        val observations = emptyObservations().toMutableList()
        observations[0] = observations[0].copy(
            attempts = listOf(
                attempt(
                    TargetStatus.MET,
                    LinguisticStatus.ACCEPTABLE,
                    context = emptyContext.copy(
                        sourceExposure = listOf(
                            PracticeCrossRunExposure(
                                "source-session", 0, PracticeExposureSource.FEEDBACK, listOf("gloss:L014-S01"),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val row = PracticeSummaryCalculator.calculate(plan, false, observations).rows.first()

        assertEquals(1, row.firstWithPriorExposure)
        assertTrue(observations[0].attempts.single().context.priorExposure.isEmpty())
    }

    @Test
    fun `persisted target descriptors retain frozen labels and first latest outcomes`() {
        val descriptor = PracticeTargetDescriptor("Frozen custom label", "G999", null)
        val items = listOf(
            PracticeSummarySnapshotItemObservation(
                position = 0,
                targetDescriptor = descriptor,
                advanced = true,
                lessonOpened = false,
                hintOpened = false,
                attempts = listOf(
                    attempt(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE),
                    attempt(TargetStatus.MET, LinguisticStatus.ACCEPTABLE),
                ),
            ),
            PracticeSummarySnapshotItemObservation(
                position = 1,
                targetDescriptor = descriptor,
                advanced = false,
                lessonOpened = true,
                hintOpened = false,
                attempts = emptyList(),
            ),
        )

        val summary = PracticeSummaryCalculator.calculate(2, true, items)
        val row = summary.rows.single()

        assertEquals("Frozen custom label", row.label)
        assertEquals("G999", row.targetSkillId)
        assertEquals(2, row.plannedItems)
        assertEquals(1, row.first.targetNotMet)
        assertEquals(1, row.latest.targetMet)
        assertEquals(1, row.latest.fullyMet)
    }

    @Test
    fun `present verb operations produce separate summary rows`() {
        val presentPlan = PracticePlanner().plan("present-verbs", "present-verbs-1", 0)
        val observations = presentPlan.items.map { item ->
            PracticeSummaryItemObservation(
                item = item,
                advanced = true,
                lessonOpened = false,
                hintOpened = false,
                attempts = listOf(attempt(TargetStatus.MET, LinguisticStatus.ACCEPTABLE)),
            )
        }

        val rows = PracticeSummaryCalculator.calculate(presentPlan, false, observations).rows

        assertEquals(listOf("selection", "recall", "repair"), rows.map { it.assessmentMode })
        assertEquals(listOf(2, 2, 2), rows.map { it.plannedItems })
        assertEquals(
            setOf("grammar:G004:selection", "grammar:G004:recall", "grammar:G004:repair"),
            presentPlan.items.map { it.targetDescriptor.targetKey() }.toSet(),
        )
    }

    @Test
    fun `plan summaries reject a changed target descriptor even with the same exercise identity`() {
        val observations = emptyObservations().toMutableList()
        observations[0] = observations[0].copy(
            item = observations[0].item.copy(
                targetDescriptor = PracticeTargetDescriptor("Unplanned label", "G999", null),
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            PracticeSummaryCalculator.calculate(plan, false, observations)
        }
    }

    private fun emptyObservations(): List<PracticeSummaryItemObservation> =
        plan.items.map { PracticeSummaryItemObservation(it, false, false, false, emptyList()) }

    private fun attempt(
        target: TargetStatus,
        language: LinguisticStatus,
        constraint: ResponseConstraintStatus? = null,
        context: PracticeAttemptContext = emptyContext,
    ) = PracticeSummaryAttemptObservation(target, language, constraint, context)
}
