package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PracticeGapPlannerTest {
    private val planner = PracticePlanner()

    @Test
    fun `guided gap profile pins four distinct pair seven exercises and explicit introduction exposure`() {
        val profile = planner.profiles().single { it.focusId == "verb-article-gaps" }
        assertEquals("verb-article-gaps-1", profile.planRevision)
        assertEquals("Verbs and articles together", profile.title)
        assertEquals(4, profile.itemCount)

        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val plan = planner.plan(profile.focusId, profile.planRevision, seed)
            assertEquals(listOf("pilot-7", "generator-7", "evaluation-7"),
                listOf(plan.contentRevision, plan.generatorRevision, plan.evaluationRevision))
            assertEquals(4, plan.items.size)
            assertTrue(plan.items.all { it.exercise.formatId == "E015" })
            assertEquals(4, plan.items.map { it.exercise.inspector.canonicalSentenceId }.distinct().size)
            assertEquals(
                setOf("L009", "L010", "L011", "L012"),
                plan.items.map(::verbId).toSet(),
            )
            assertEquals(listOf("g0", "g1"), plan.items.first().assessments.map { it.slotId })
            assertEquals(listOf("primary", "support"), plan.items.first().assessments.map { it.assessmentRole })
            assertEquals(
                listOf("grammar:G004:selection", "grammar:G006:selection"),
                plan.items.first().assessments.map { it.targetDescriptor.targetKey() },
            )
            assertEquals(
                listOf(
                    "verb-infinitive:L011", "verb-present:L011", "article:L014",
                    "headword:L014-S01", "grammar:G004", "grammar:G006",
                ),
                plan.introductionExposureKeys,
            )
            assertEquals("Jeg læser en bog.", plan.introduction.single { it.title == "Worked example" }.body)
            assertEquals("da", plan.introduction.single { it.title == "Worked example" }.bodyLanguage)
        }
    }

    @Test
    fun `guided gap plan is deterministic across boundaries and rejects invalid pairs`() {
        val first = planner.plan("verb-article-gaps", "verb-article-gaps-1", 73)
        val repeated = planner.plan("verb-article-gaps", "verb-article-gaps-1", 73)
        val other = planner.plan("verb-article-gaps", "verb-article-gaps-1", 74)
        assertEquals(first.items.map { it.exercise.id }, repeated.items.map { it.exercise.id })
        assertNotEquals(first.items.map { it.exercise.id }, other.items.map { it.exercise.id })
        assertFailsWith<IllegalArgumentException> { planner.plan("verb-article-gaps", "practice-1", 0) }
        assertFailsWith<IllegalArgumentException> { planner.plan("reading-basics", "verb-article-gaps-1", 0) }
        assertFailsWith<IllegalArgumentException> { planner.plan("verb-article-gaps", "verb-article-gaps-1", -1) }
    }

    private fun verbId(item: PracticePlanItem): String =
        PilotContent.verbs.single { it.lexicalId in item.exercise.inspector.lexicalIds }.lexicalId
}
