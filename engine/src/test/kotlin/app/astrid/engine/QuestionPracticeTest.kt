package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionPracticeTest {
    @Test
    fun `profile pins four required-teaching pair-sixteen question activities`() {
        val profile = QuestionPracticePlanner.profile()
        assertEquals("module-one-questions", profile.focusId)
        assertEquals("module-one-questions-1", profile.planRevision)
        assertEquals("Asking yes or no questions", profile.title)
        assertEquals(4, profile.itemCount)
        assertEquals(listOf("G001", "G002", "G003", "G004", "G005"), profile.prerequisiteSkillIds)

        val plan = PracticePlanner().plan(profile.focusId, profile.planRevision, 47)
        assertEquals("pilot-16", plan.contentRevision)
        assertEquals("generator-16", plan.generatorRevision)
        assertEquals("evaluation-16", plan.evaluationRevision)
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-order-outcome-1", plan.completionRuleId)
        assertEquals("module-one-order-exposure-1", plan.exposurePolicyId)
        assertEquals(NeutralQuestionContent.introduction(), plan.introduction)
        assertEquals(NeutralQuestionContent.LESSON_EXPOSURE_KEYS, plan.introductionExposureKeys)
        assertEquals(NeutralQuestionContent.tasks.map(NeutralQuestionTask::id), plan.items.map {
            (it.exercise.source as QuestionExerciseSource).task.id
        })
        assertTrue(plan.items.all { it.exercise.formatId == "E030" })
        assertTrue(plan.items.all { it.targetDescriptor.targetSkillId == "G018" })
        assertTrue(plan.items.all { it.targetDescriptor.assessmentMode == "constituent-order" })
        assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == "neutral-positive-yes-no" })
        assertTrue(plan.items.all { it.assessments.size == 1 && it.assessments.single().assessmentRole == "primary" })
    }

    @Test
    fun `required teaching uses separate transformations and distinguishes uppercase I`() {
        val sections = NeutralQuestionContent.introduction()
        assertSection(sections, "Han læser en bog.", "da")
        assertSection(sections, "He reads a book.", null)
        assertSection(sections, "Læser han en bog?", "da")
        assertSection(sections, "Does he read a book?", null)
        assertSection(sections, "I arbejder.", "da")
        assertSection(sections, "You all work.", null)
        assertSection(sections, "Arbejder I?", "da")
        assertSection(sections, "Do you all work?", null)
        assertTrue(sections.any { "no English do auxiliary" in it.body })
        assertSection(sections, "I", "da")
        assertSection(sections, "you all", null)
        assertSection(sections, "i", "da")
        assertSection(sections, "in", null)
        assertSection(sections, "i Aarhus", "da")
        assertSection(sections, "in Aarhus", null)
        assertSection(sections, "ja", "da")
        assertSection(sections, "yes", null)
        assertSection(sections, "nej", "da")
        assertSection(sections, "no", null)
        val assessed = NeutralQuestionContent.tasks.flatMap { listOf(it.positiveProposition, question(it)) }
        assertFalse(sections.any { it.body in assessed })
    }

    @Test
    fun `existing order policies advance only known or requested-order outcomes`() {
        val canonical = PracticeCompletionAttemptObservation(
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
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", canonical))
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", wrongOrder))
        assertFalse(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", malformed))
    }

    @Test
    fun `review inventory contains every card source support key and boundary`() {
        val review = buildString {
            QuestionReviewRenderer.appendTo(this, PilotContent.currentSnapshot, 47)
        }
        NeutralQuestionContent.tasks.forEach { task ->
            assertTrue(task.id in review)
            assertTrue(task.frameVariantId in review)
            assertTrue(NeutralQuestionContent.exactOrderKey(task.id) in review)
        }
        assertTrue("L039 / L039-S01" in review)
        assertTrue("L040 / L040-S01" in review)
        assertTrue("ledstilling-i-ja-nej-spoergsmaal" in review)
        assertTrue("neither is an assessed lexical target" in review)
        assertTrue("Læser han en bog?" in review)
        assertTrue("Arbejder I?" in review)
    }

    private fun question(task: NeutralQuestionTask) =
        task.parts.joinToString(" ") { it.text }.replaceFirstChar(Char::titlecase) + "?"

    private fun assertSection(sections: List<IntroductionSection>, body: String, language: String?) {
        assertTrue(sections.any { it.body == body && it.bodyLanguage == language })
    }
}
