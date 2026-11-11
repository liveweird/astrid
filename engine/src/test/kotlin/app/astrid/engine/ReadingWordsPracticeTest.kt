package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingWordsPracticeTest {
    @Test
    fun `reading words visit the four authored meanings on the retained choice pair`() {
        val engine = PilotEngine()
        val planner = PracticePlanner(engine)
        val bundles = listOf("et magasin", "en bog", "en avis", "et brev")
        val senses = listOf("L024-S01", "L014-S01", "L015-S01", "L018-S01")
        for (seed in listOf(0, 1, 47, Int.MAX_VALUE)) {
            val plan = planner.plan("module-one-reading-words", "module-one-reading-words-1", seed)
            assertEquals(
                Triple("pilot-5", "generator-5", "evaluation-5"),
                Triple(plan.contentRevision, plan.generatorRevision, plan.evaluationRevision),
            )
            assertEquals(senses, plan.items.map { it.exercise.lexicalTarget?.senseId })
            assertTrue(plan.introductionRequired)
            assertEquals(bundles, plan.introduction.filter { it.bodyLanguage == "da" }.map { it.body })
            for ((position, item) in plan.items.withIndex()) {
                assertEquals("E011", item.exercise.formatId)
                assertEquals(bundles.toSet(), item.exercise.prompt.choices.toSet())
                assertEquals("meaning", item.targetDescriptor.lexicalTarget?.facet)
                assertEquals("en-da", item.targetDescriptor.lexicalTarget?.direction)
                assertNull(item.targetDescriptor.targetScopeId)
                assertTrue(item.assessments.isEmpty())
                assertEquals(TargetStatus.MET, engine.evaluate(item.exercise, Answer.Text(bundles[position])).targetStatus)
                assertFalse(engine.isSuppliedChoice(item.exercise, "magasin"))
            }
        }
    }

    @Test
    fun `wrong reading word feedback never records confirmation of the expected meaning`() {
        val engine = PilotEngine()
        val plan = PracticePlanner(engine).plan("module-one-reading-words", "module-one-reading-words-1", 47)
        val item = plan.items.first()
        val wrong = engine.evaluate(item.exercise, Answer.Text("en bog"))
        val right = engine.evaluate(item.exercise, Answer.Text("et magasin"))
        val policy = plan.exposurePolicyId
        assertEquals("module-one-reading-word-exposure-1", policy)
        val wrongObservation = wrong.feedbackObservation()
        val rightObservation = right.feedbackObservation()
        assertEquals(
            emptyList(),
            PracticeFeedbackExposureCalculator.releasedKeys(item.feedbackExposureKeys, item.assessments, listOf(wrongObservation), policy),
        )
        assertEquals(
            item.feedbackExposureKeys,
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys, item.assessments, listOf(wrongObservation, rightObservation), policy,
            ),
        )
        assertEquals(TargetStatus.NOT_MET, wrong.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, wrong.linguisticStatus)
        assertNull(wrong.completedSentence)
        assertTrue(PracticeCompletionPolicy.canAdvance(plan.completionRuleId, PracticeCompletionAttemptObservation(
            wrong.targetStatus, wrong.linguisticStatus, wrong.responseConstraintStatus, wrong.issues.map { it.code },
        )))
    }

    private fun Evaluation.feedbackObservation() = PracticeFeedbackAttemptObservation(
        targetStatus, gapResults.orEmpty(), linguisticStatus, responseConstraintStatus, issues.map { it.code },
    )
}
