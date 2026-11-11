package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AbilityPracticeTest {
    private val planner = PracticePlanner()

    @Test
    fun `ability profile pins two selections followed by two recalls`() {
        val profile = planner.profiles().single { it.focusId == "module-one-ability" }
        assertEquals("module-one-ability-1", profile.planRevision)
        assertEquals("Using a verb after kan", profile.title)
        assertEquals("Choose and type the bare infinitive læse after kan.", profile.description)
        assertEquals(4, profile.itemCount)
        assertEquals(listOf("G001", "G002", "G003", "G004", "G005"), profile.prerequisiteSkillIds)

        val plan = planner.plan(profile.focusId, profile.planRevision, 47)
        assertEquals("pilot-13", plan.contentRevision)
        assertEquals("generator-13", plan.generatorRevision)
        assertEquals("evaluation-13", plan.evaluationRevision)
        assertEquals(
            listOf("ability-select-letter", "ability-select-danish", "ability-recall-letter", "ability-recall-danish"),
            plan.items.map { requireNotNull(it.exercise.inspector.moduleOneSource).taskId },
        )
        assertEquals(listOf("E026", "E026", "E027", "E027"), plan.items.map { it.exercise.formatId })
        assertEquals(listOf("selection", "selection", "recall", "recall"), plan.items.map {
            it.targetDescriptor.assessmentMode
        })
        assertTrue(plan.items.all { it.targetDescriptor.targetSkillId == "G030" })
        assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == "kan-bare-infinitive" })
        assertTrue(plan.items.all { it.assessments.single().targetDescriptor == it.targetDescriptor })
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-known-outcome-1", plan.completionRuleId)
        assertEquals("module-one-exposure-1", plan.exposurePolicyId)
    }

    @Test
    fun `guided selections vary within each run and cover every correct position across seeds`() {
        val positionsByTask = mutableMapOf<String, MutableSet<Int>>()
        (0..2).forEach { seed ->
            val selections = planner.plan("module-one-ability", "module-one-ability-1", seed).items.take(2)
            val positions = selections.map { item ->
                val taskId = requireNotNull(item.exercise.inspector.moduleOneSource).taskId
                item.exercise.prompt.choices.indexOf("læse").also { position ->
                    positionsByTask.getOrPut(taskId, ::mutableSetOf).add(position)
                }
            }
            assertEquals(2, positions.toSet().size, "seed=$seed")
        }
        assertEquals(setOf("ability-select-letter", "ability-select-danish"), positionsByTask.keys)
        assertTrue(positionsByTask.values.all { it == setOf(0, 1, 2) })
    }

    @Test
    fun `required teaching separates kan from the following bare infinitive`() {
        val plan = planner.plan("module-one-ability", "module-one-ability-1", 47)
        assertEquals("at kunne → kan", plan.introduction.single { it.title == "Ability modal" }.body)
        assertEquals("at læse → læse efter kan", plan.introduction.single { it.title == "Reading verb" }.body)
        assertEquals(
            "Jeg kan læse et brev.",
            plan.introduction.single { it.title == "Worked example — letter" }.body,
        )
        assertEquals(
            "Han kan læse dansk.",
            plan.introduction.single { it.title == "Worked example — Danish" }.body,
        )
        val meanings = plan.introduction.single { it.title == "Meanings" }.body
        assertTrue("expresses ability" in meanings)
        assertTrue("reading written language" in meanings)
        assertTrue(plan.introduction.filter { it.bodyLanguage == "da" }.none { " after " in it.body })
        val rule = plan.introduction.single { it.title == "Requested pattern" }.body
        assertTrue("no at" in rule)
        assertTrue("no present form læser" in rule)
        assertFalse(plan.introduction.joinToString { it.body }.contains("all modal verbs"))
        assertEquals(AbilityContent.introductionExposureKeys(), plan.introductionExposureKeys)
    }

    @Test
    fun `ability exposure distinguishes answer pattern supplied context hint and feedback`() {
        val plan = planner.plan("module-one-ability", "module-one-ability-1", 47)
        plan.items.forEach { item ->
            assertEquals(
                listOf(
                    AbilityContent.BARE_INFINITIVE_KEY, AbilityContent.AT_INFINITIVE_KEY,
                    AbilityContent.FINITE_PRESENT_KEY, AbilityContent.PATTERN_KEY,
                ),
                item.answerExposureKeys,
            )
            assertEquals(AbilityContent.introductionExposureKeys(), item.lessonExposureKeys)
            assertFalse(AbilityContent.MODAL_PRESENT_KEY in item.answerExposureKeys)
            assertTrue(AbilityContent.MODAL_PRESENT_KEY in item.promptExposureKeys)
            assertEquals(
                listOf(AbilityContent.PATTERN_KEY, AbilityContent.BARE_INFINITIVE_KEY),
                item.hintExposureKeys,
            )
            assertEquals(
                listOf(AbilityContent.PATTERN_KEY, AbilityContent.BARE_INFINITIVE_KEY),
                item.feedbackExposureKeys,
            )
        }
        assertTrue(AbilityContent.AT_INFINITIVE_KEY in plan.items[0].promptExposureKeys)
        assertTrue(AbilityContent.FINITE_PRESENT_KEY in plan.items[0].promptExposureKeys)
        assertTrue(PresentOrderContent.componentKey("L018") in plan.items[0].promptExposureKeys)
        assertTrue(PresentOrderContent.componentKey("L031") in plan.items[1].promptExposureKeys)
        assertFalse(AbilityContent.FINITE_PRESENT_KEY in plan.items[2].promptExposureKeys)
        assertTrue(AbilityContent.BARE_INFINITIVE_KEY in plan.items[2].promptExposureKeys)
    }
}
