package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PracticeAttemptContextCalculatorTest {
    private val plan = PracticePlanner().plan("reading-basics", "practice-1", 0)

    @Test
    fun `first attempt excludes its current prompt but includes earlier released choices`() {
        val context = PracticeAttemptContextCalculator.calculate(
            currentItem = plan.items[3],
            introductionOpened = false,
            observations = observations(cursor = 3),
        )

        assertFalse(context.lessonOpened)
        assertFalse(context.hintOpened)
        assertEquals(
            listOf(PracticePriorExposure(0, PracticeExposureSource.PROMPT, listOf("headword:L014-S01"))),
            context.priorExposure,
        )
    }

    @Test
    fun `retry includes current support and feedback without duplicating retries`() {
        val observations = observations(cursor = 3).map {
            if (it.item.position == 3) it.copy(lessonOpened = true, hintOpened = true, feedbackReleased = true) else it
        }
        val context = PracticeAttemptContextCalculator.calculate(plan.items[3], true, observations)

        assertTrue(context.introductionOpened)
        assertTrue(context.lessonOpened)
        assertTrue(context.hintOpened)
        assertEquals(
            listOf(
                PracticePriorExposure(0, PracticeExposureSource.PROMPT, listOf("headword:L014-S01")),
                PracticePriorExposure(3, PracticeExposureSource.LESSON, listOf("headword:L014-S01")),
                PracticePriorExposure(3, PracticeExposureSource.FEEDBACK, listOf("headword:L014-S01")),
            ),
            context.priorExposure,
        )
    }

    @Test
    fun `support opened later does not mutate a frozen attempt context`() {
        val before = PracticeAttemptContextCalculator.calculate(plan.items[3], false, observations(cursor = 3))
        val afterObservations = observations(cursor = 3).map {
            if (it.item.position == 0) it.copy(lessonOpened = true) else it
        }
        val after = PracticeAttemptContextCalculator.calculate(plan.items[3], false, afterObservations)

        assertFalse(before.priorExposure.any { it.source == PracticeExposureSource.LESSON })
        assertTrue(after.priorExposure.any { it.position == 0 && it.source == PracticeExposureSource.LESSON })
        assertEquals(
            listOf(PracticePriorExposure(0, PracticeExposureSource.PROMPT, listOf("headword:L014-S01"))),
            before.priorExposure,
        )
    }

    @Test
    fun `persisted exposure snapshots work without generated exercises`() {
        val earlier = exposureSnapshot(position = 0, prompt = listOf("frozen:key"))
        val current = exposureSnapshot(position = 1, answer = listOf("frozen:key"))
        val observations = listOf(
            PracticeItemExposureObservation(earlier, true, false, false, false),
            PracticeItemExposureObservation(current, true, false, false, false),
        )

        val context = PracticeAttemptContextCalculator.calculate(current, false, observations)

        assertEquals(
            listOf(PracticePriorExposure(0, PracticeExposureSource.PROMPT, listOf("frozen:key"))),
            context.priorExposure,
        )
        assertFailsWith<IllegalArgumentException> {
            PracticeAttemptContextCalculator.calculate(
                current.copy(answerExposureKeys = listOf("changed:key")),
                false,
                observations,
            )
        }
    }

    @Test
    fun `generic lexical hints record support without inventing answer exposure`() {
        listOf(0, 3).forEach { position ->
            val context = PracticeAttemptContextCalculator.calculate(
                plan.items[position], false,
                observations(position).map {
                    if (it.item.position == position) it.copy(hintOpened = true) else it
                },
            )
            assertTrue(context.hintOpened)
            assertFalse(context.priorExposure.any { it.source == PracticeExposureSource.HINT })
        }
    }

    @Test
    fun `headword feedback can expose its article before later article repair`() {
        val evaluation = PilotEngine().evaluate(plan.items[3].exercise, Answer.Text("et bog"))
        assertTrue(evaluation.feedback.contains("article en"))
        val context = PracticeAttemptContextCalculator.calculate(plan.items[6], false, observations(6))
        assertEquals(
            listOf(PracticePriorExposure(3, PracticeExposureSource.FEEDBACK, listOf("article:L014"))),
            context.priorExposure,
        )
    }

    @Test
    fun `ordered sentence feedback preserves its separate headword exposure source`() {
        val context = PracticeAttemptContextCalculator.calculate(plan.items[7], false, observations(7))
        assertTrue(context.priorExposure.contains(
            PracticePriorExposure(1, PracticeExposureSource.PROMPT, listOf("headword:L015-S01")),
        ))
        assertTrue(context.priorExposure.contains(
            PracticePriorExposure(1, PracticeExposureSource.FEEDBACK, listOf("headword:L015-S01")),
        ))
    }

    @Test
    fun `Danish sentence feedback does not invent a second English gloss exposure`() {
        val context = PracticeAttemptContextCalculator.calculate(plan.items[4], false, observations(4))
        assertTrue(context.priorExposure.contains(
            PracticePriorExposure(1, PracticeExposureSource.PROMPT, listOf("gloss:L015-S01")),
        ))
        assertFalse(context.priorExposure.any { it.position == 1 && it.source == PracticeExposureSource.FEEDBACK })
    }

    private fun observations(cursor: Int): List<PracticeItemExposureObservation> =
        plan.items.map {
            PracticeItemExposureObservation(
                item = it,
                promptReleased = it.position <= cursor,
                lessonOpened = false,
                hintOpened = false,
                feedbackReleased = it.position < cursor,
            )
        }

    private fun exposureSnapshot(
        position: Int,
        answer: List<String> = emptyList(),
        prompt: List<String> = emptyList(),
    ) = PracticeExposureSnapshot(position, answer, prompt, emptyList(), emptyList(), emptyList())
}
