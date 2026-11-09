package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PracticePresentVerbPlannerTest {
    private val planner = PracticePlanner()

    @Test
    fun `profiles publish mixed and present verb choices without changing the legacy plan title`() {
        assertEquals(
            listOf(
                "reading-basics/practice-1",
                "present-verbs/present-verbs-1",
                "verb-article-gaps/verb-article-gaps-1",
                "module-one-singular/module-one-singular-1",
                "module-one-groups/module-one-groups-1",
                "module-one-er/module-one-er-1",
            ),
            planner.profiles().map { "${it.focusId}/${it.planRevision}" },
        )
        assertEquals(listOf(
            "Mixed practice",
            "Present verb forms",
            "Verbs and articles together",
            "Talking about one person",
            "Talking to and about groups",
            "Using the verb to be",
        ),
            planner.profiles().map { it.title })
        assertEquals(listOf(8, 6, 4, 6, 6, 6), planner.profiles().map { it.itemCount })
        assertEquals("Words and simple statements", planner.plan("reading-basics", "practice-1", 0).title)
    }

    @Test
    fun `present verb plan rotates all verbs through three assessment operations`() {
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val plan = planner.plan("present-verbs", "present-verbs-1", seed)
            val verbs = plan.items.map(::verbId)

            assertEquals("Present verb forms", plan.title)
            assertEquals(listOf("G001", "G002", "G003"), plan.prerequisiteSkillIds)
            assertEquals(listOf("E001", "E004", "E005", "E001", "E004", "E005"),
                plan.items.map { it.exercise.formatId })
            assertEquals(PilotContent.verbs.map { it.lexicalId }.toSet(), verbs.toSet(), "seed=$seed")
            assertEquals(verbs.first(), verbs.last(), "seed=$seed")
            assertTrue(verbs.zipWithNext().none { (left, right) -> left == right }, "seed=$seed")
            assertEquals(6, plan.items.map { it.exercise.inspector.canonicalSentenceId }.distinct().size)
            assertEquals(listOf("selection", "recall", "repair", "selection", "recall", "repair"),
                plan.items.map { it.targetDescriptor.assessmentMode })
            assertEquals(listOf("pilot-3", "generator-3", "evaluation-3"),
                listOf(plan.contentRevision, plan.generatorRevision, plan.evaluationRevision))
        }
    }

    @Test
    fun `present verb plan is deterministic and exact profile pairs are required`() {
        val first = planner.plan("present-verbs", "present-verbs-1", 73)
        val repeated = planner.plan("present-verbs", "present-verbs-1", 73)
        val otherSeed = planner.plan("present-verbs", "present-verbs-1", 74)

        assertEquals(first.items.map { it.exercise.id }, repeated.items.map { it.exercise.id })
        assertNotEquals(first.items.map { it.exercise.id }, otherSeed.items.map { it.exercise.id })
        assertFailsWith<IllegalArgumentException> { planner.plan("present-verbs", "practice-1", 0) }
        assertFailsWith<IllegalArgumentException> { planner.plan("reading-basics", "present-verbs-1", 0) }
        assertFailsWith<IllegalArgumentException> { PracticeTargetKeys.key("G005", null, "selection") }
        assertFailsWith<IllegalArgumentException> { PracticeTargetKeys.key("G004", null, "unknown") }
        assertEquals("grammar:G006:selection", PracticeTargetKeys.key("G006", null, "selection"))
        assertEquals("grammar:G006:recall", PracticeTargetKeys.key("G006", null, "recall"))
        assertFailsWith<IllegalArgumentException> { PracticeTargetKeys.key("G006", null, "repair") }
    }

    @Test
    fun `present verb exposure distinguishes displayed infinitives from correct forms and concept help`() {
        val items = planner.plan("present-verbs", "present-verbs-1", 0).items

        items.forEach { item ->
            val verbId = verbId(item)
            val presentKey = "verb-present:$verbId"
            val infinitiveKey = "verb-infinitive:$verbId"
            assertEquals(listOf(presentKey), item.answerExposureKeys)
            assertEquals(listOf(infinitiveKey, presentKey, "grammar:G004"), item.lessonExposureKeys)
            assertEquals(listOf("grammar:G004"), item.hintExposureKeys)
            assertEquals(listOf(presentKey, "grammar:G004"), item.feedbackExposureKeys)
            if (item.exercise.formatId == "E001") {
                assertEquals(listOf(infinitiveKey, presentKey), item.promptExposureKeys)
            } else {
                assertEquals(listOf(infinitiveKey), item.promptExposureKeys)
                assertTrue(presentKey !in item.promptExposureKeys)
            }
        }
    }

    private fun verbId(item: PracticePlanItem): String =
        PilotContent.verbs.single { it.lexicalId in item.exercise.inspector.lexicalIds }.lexicalId
}
