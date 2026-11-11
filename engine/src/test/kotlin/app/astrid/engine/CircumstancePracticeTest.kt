package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CircumstancePracticeTest {
    @Test
    fun `profile pins four required-teaching pair-fifteen items`() {
        val profile = CircumstancePracticePlanner.profile()
        assertEquals("module-one-circumstances", profile.focusId)
        assertEquals("module-one-circumstances-1", profile.planRevision)
        assertEquals("Adding when and where", profile.title)
        assertEquals(4, profile.itemCount)
        assertEquals(listOf("G001", "G002", "G003", "G004", "G005"), profile.prerequisiteSkillIds)

        assertTrue(PracticePlanner().profiles().any { it == profile })

        val plan = PracticePlanner().plan(profile.focusId, profile.planRevision, 47)
        assertEquals("pilot-15", plan.contentRevision)
        assertEquals("generator-15", plan.generatorRevision)
        assertEquals("evaluation-15", plan.evaluationRevision)
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-order-outcome-1", plan.completionRuleId)
        assertEquals("module-one-order-exposure-1", plan.exposurePolicyId)
        assertEquals(FinalCircumstanceContent.introduction(), plan.introduction)
        assertEquals(FinalCircumstanceContent.LESSON_EXPOSURE_KEYS, plan.introductionExposureKeys)
        assertEquals(FinalCircumstanceContent.tasks.map(FinalCircumstanceTask::id), plan.items.map {
            (it.exercise.source as CircumstanceExerciseSource).task.id
        })
        assertTrue(plan.items.all { it.exercise.formatId == "E029" })
        assertTrue(plan.items.all { it.targetDescriptor.targetSkillId == "G016" })
        assertTrue(plan.items.all { it.targetDescriptor.assessmentMode == "constituent-order" })
        assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == "one-final-circumstance" })
    }

    @Test
    fun `required teaching uses separate examples and explicitly bounds the final pattern`() {
        val sections = FinalCircumstanceContent.introduction()
        assertTrue(sections.any { it.body == "Du arbejder nu." && it.bodyLanguage == "da" })
        assertTrue(sections.any { it.body == "Han læser en bog i dag." && it.bodyLanguage == "da" })
        assertTrue(sections.any { it.body == "Jeg arbejder i Aarhus." && it.bodyLanguage == "da" })
        assertTrue(sections.any { it.body == "i dag" && it.bodyLanguage == "da" })
        assertTrue(sections.any { "meaning today contains two written words" in it.body })
        assertTrue(sections.any { "does not claim" in it.body })
        val assessed = FinalCircumstanceContent.tasks.map { task ->
            task.parts.joinToString(" ") { it.text }.replaceFirstChar(Char::titlecase) + "."
        }
        assertFalse(sections.any { it.body in assessed })
    }

    @Test
    fun `existing order policies advance and disclose canonical and bounded order failures`() {
        val known = PracticeCompletionAttemptObservation(
            TargetStatus.MET,
            LinguisticStatus.ACCEPTABLE,
            ResponseConstraintStatus.MET,
            emptyList(),
        )
        val wrongOrder = PracticeCompletionAttemptObservation(
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            listOf("REQUESTED_ORDER_CONSTRAINT"),
        )
        val malformed = PracticeCompletionAttemptObservation(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            listOf("ANSWER_TYPE_MISMATCH"),
        )
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", known))
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", wrongOrder))
        assertFalse(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", malformed))
    }

    @Test
    fun `review inventory exposes every literal card source and boundary`() {
        val review = buildString {
            CircumstanceReviewRenderer.appendTo(this, PilotContent.pilotFifteenSnapshot, 47)
        }
        FinalCircumstanceContent.tasks.forEach { task ->
            assertTrue(task.id in review)
            assertTrue(task.frameVariantId in review)
            assertTrue(FinalCircumstanceContent.exactOrderKey(task.id) in review)
        }
        assertTrue("L037 / L037-S01" in review)
        assertTrue("L038 / L038-S01" in review)
        assertTrue("sv00006990" in review)
        assertTrue("no arbitrary placement" in review)
        assertTrue("Du arbejder nu." in review)
        assertTrue("Han læser en bog i dag." in review)
        assertTrue("Jeg arbejder i Aarhus." in review)
    }
}
