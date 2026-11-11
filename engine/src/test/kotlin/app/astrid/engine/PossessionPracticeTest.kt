package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PossessionPracticeTest {
    private val planner = PracticePlanner()

    @Test
    fun `possession profile pins exact four-card plan and separate teaching`() {
        val profile = planner.profiles().single { it.focusId == "module-one-possession" }
        assertEquals("module-one-possession-1", profile.planRevision)
        assertEquals("Saying what someone has", profile.title)
        assertEquals("Choose and type the Danish present form har for possession.", profile.description)
        assertEquals(4, profile.itemCount)
        assertEquals(listOf("G001", "G002", "G003", "G004", "G005"), profile.prerequisiteSkillIds)

        val plan = planner.plan(profile.focusId, profile.planRevision, 47)
        assertEquals("pilot-11", plan.contentRevision)
        assertEquals("generator-11", plan.generatorRevision)
        assertEquals("evaluation-11", plan.evaluationRevision)
        assertEquals(
            listOf(
                "possession-select-letter", "possession-select-newspaper", "possession-recall-book", "possession-recall-letter",
            ),
            plan.items.map { requireNotNull(it.exercise.inspector.moduleOneSource).taskId },
        )
        assertEquals(listOf("E022", "E022", "E023", "E023"), plan.items.map { it.exercise.formatId })
        assertEquals(listOf("selection", "selection", "recall", "recall"), plan.items.map { it.targetDescriptor.assessmentMode })
        assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == "have-possession-present" })
        assertTrue(plan.items.all { it.assessments.single().targetDescriptor == it.targetDescriptor })
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-known-outcome-1", plan.completionRuleId)
        assertEquals("module-one-exposure-1", plan.exposurePolicyId)
        assertTrue(plan.introduction.any { it.body == "at have → har" })
        assertTrue(plan.introduction.any { it.body == "Jeg har en bog." })
        assertTrue(plan.introduction.any { "same present form with each subject" in it.body })
        assertFalse(plan.introduction.any { "ending in -r" in it.body || "adds -r" in it.body })
        assertTrue(PresentOrderContent.POSSESSION_PATTERN_KEY in requireNotNull(plan.introductionExposureKeys))
        assertTrue(plan.items.all { PresentOrderContent.POSSESSION_PATTERN_KEY in it.answerExposureKeys })
        assertTrue(plan.items.all { it.hintExposureKeys == listOf(PresentOrderContent.POSSESSION_PATTERN_KEY) })
    }

    @Test
    fun `retained pair eleven review includes saved teaching through possession`() {
        val teaching = PilotReview.renderPilotElevenMarkdown().substringAfter("## S05–S07 required teaching", "")
        assertTrue("`module-one-present-1`" in teaching)
        assertTrue("`module-one-order-1`" in teaching)
        assertTrue("`module-one-possession-1`" in teaching)
        assertTrue("Worked example [da]: Jeg har en bog." in teaching)
        assertTrue("same present form with each subject" in teaching)
    }

    @Test
    fun `possession plan keeps both selection choices variable and authored items stable`() {
        val plans = (0..3).map { planner.plan("module-one-possession", "module-one-possession-1", it) }
        val taskIds = plans.map { plan -> plan.items.map { requireNotNull(it.exercise.inspector.moduleOneSource).taskId } }
        assertEquals(1, taskIds.toSet().size)
        listOf(0, 1).forEach { position ->
            assertEquals(setOf(0, 1), plans.map { it.items[position].exercise.prompt.choices.indexOf("har") }.toSet())
        }
    }

    @Test
    fun `existing present and order plans remain pinned to pair ten`() {
        listOf(
            "module-one-present" to "module-one-present-1",
            "module-one-order" to "module-one-order-1",
        ).forEach { (focusId, revision) ->
            val plan = planner.plan(focusId, revision, 47)
            assertEquals("pilot-10", plan.contentRevision)
            assertEquals("generator-10", plan.generatorRevision)
            assertEquals("evaluation-10", plan.evaluationRevision)
        }
    }
}
