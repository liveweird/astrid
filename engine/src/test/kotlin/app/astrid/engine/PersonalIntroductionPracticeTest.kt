package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalIntroductionPracticeTest {
    private val planner = PracticePlanner()

    @Test
    fun `personal profile pins alternating selection and recall for each verb`() {
        val profile = planner.profiles().single { it.focusId == "module-one-personal" }
        assertEquals("module-one-personal-1", profile.planRevision)
        assertEquals("Names, homes and languages", profile.title)
        assertEquals("Choose and type Danish present forms for names, places and languages.", profile.description)
        assertEquals(6, profile.itemCount)
        assertEquals(listOf("G001", "G002", "G003", "G004", "G005"), profile.prerequisiteSkillIds)

        val plan = planner.plan(profile.focusId, profile.planRevision, 47)
        assertEquals("pilot-12", plan.contentRevision)
        assertEquals("generator-12", plan.generatorRevision)
        assertEquals("evaluation-12", plan.evaluationRevision)
        assertEquals(
            listOf(
                "personal-select-name", "personal-recall-name",
                "personal-select-residence", "personal-recall-residence",
                "personal-select-language", "personal-recall-language",
            ),
            plan.items.map { requireNotNull(it.exercise.inspector.moduleOneSource).taskId },
        )
        assertEquals(listOf("E022", "E023", "E022", "E023", "E022", "E023"), plan.items.map { it.exercise.formatId })
        assertEquals(listOf("selection", "recall", "selection", "recall", "selection", "recall"), plan.items.map {
            it.targetDescriptor.assessmentMode
        })
        assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == "personal-introduction-present" })
        assertTrue(plan.items.all { it.assessments.single().targetDescriptor == it.targetDescriptor })
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-known-outcome-1", plan.completionRuleId)
        assertEquals("module-one-exposure-1", plan.exposurePolicyId)
    }

    @Test
    fun `personal teaching and exposure state the three exact frame families`() {
        val plan = planner.plan("module-one-personal", "module-one-personal-1", 47)
        assertEquals(
            listOf("Hun hedder Nora.", "Jeg bor i Odense.", "Du taler dansk."),
            plan.introduction.filter { it.title == "Worked example" }.map { it.body },
        )
        val frames = plan.introduction.single { it.title == "Personal statement frames" }.body
        assertTrue("name" in frames)
        assertTrue("i with a city" in frames)
        assertTrue("language" in frames)
        assertTrue(plan.introduction.single { it.title == "Subject agreement" }.body.contains("same stored present form"))
        assertFalse(plan.introduction.joinToString { it.body }.contains("adds -r"))
        assertEquals(
            listOf(
                PresentOrderContent.PERSONAL_PATTERN_KEY,
                PresentOrderContent.infinitiveKey("L028"), PresentOrderContent.presentKey("L028"),
                PresentOrderContent.infinitiveKey("L029"), PresentOrderContent.presentKey("L029"),
                PresentOrderContent.infinitiveKey("L030"), PresentOrderContent.presentKey("L030"),
                PresentOrderContent.componentKey("L031"), PresentOrderContent.componentKey("L032"),
            ),
            plan.introductionExposureKeys,
        )
        plan.items.forEach { item ->
            assertTrue(PresentOrderContent.PERSONAL_PATTERN_KEY in item.answerExposureKeys)
            val verbId = requireNotNull(item.exercise.inspector.moduleOneSource).targetLexicalId
            assertEquals(
                listOf(
                    PresentOrderContent.PERSONAL_PATTERN_KEY,
                    PresentOrderContent.infinitiveKey(verbId), PresentOrderContent.presentKey(verbId),
                ),
                item.hintExposureKeys,
            )
        }
        assertTrue(PresentOrderContent.componentKey("L032") in plan.items[2].promptExposureKeys)
        assertTrue(PresentOrderContent.componentKey("L031") in plan.items[4].promptExposureKeys)
    }

    @Test
    fun `personal guided choices vary correct positions within each run`() {
        for (seed in listOf(0, 1, 47, Int.MAX_VALUE)) {
            val selections = planner.plan("module-one-personal", "module-one-personal-1", seed)
                .items.filter { it.exercise.formatId == "E022" }
            assertEquals(
                setOf(0, 1),
                selections.map { item ->
                    item.exercise.prompt.choices.indexOf(requireNotNull(item.exercise.inspector.moduleOneSource).expectedForm)
                }.toSet(),
                "seed $seed",
            )
        }
    }

    @Test
    fun `personal authored order remains stable while all three selection choices vary`() {
        val plans = (0..3).map { planner.plan("module-one-personal", "module-one-personal-1", it) }
        assertEquals(1, plans.map { plan -> plan.items.map { it.exercise.inspector.moduleOneSource?.taskId } }.toSet().size)
        listOf(0, 2, 4).forEach { position ->
            assertEquals(setOf(0, 1), plans.map { it.items[position].exercise.prompt.choices.indexOf(
                requireNotNull(it.items[position].exercise.inspector.moduleOneSource).expectedForm,
            ) }.toSet())
        }
    }
}
