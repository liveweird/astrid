package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PracticeFollowUpPlannerTest {
    private val source = PracticePlanner().plan("reading-basics", "practice-1", 47)
    private val sourceExercises = source.items.map { it.exercise }
    private val planner = PracticeFollowUpPlanner()

    @Test
    fun `all selected source targets stay distinct and retain their assessment direction`() {
        val selected = source.items.map { it.targetDescriptor.targetKey() }.distinct()

        val plan = planner.plan("follow-up-1", 73, selected, sourceExercises)

        assertEquals("targeted-review", plan.focusId)
        assertEquals("Review selected targets", plan.title)
        assertEquals(listOf("pilot-3", "generator-3", "evaluation-3"),
            listOf(plan.contentRevision, plan.generatorRevision, plan.evaluationRevision))
        assertEquals(8, plan.items.size)
        assertEquals(selected.toSet(), plan.items.map { it.targetDescriptor.targetKey() }.toSet())
        assertEquals((0..7).toList(), plan.items.map { it.position })
        assertTrue("E003" in plan.items.map { it.exercise.formatId })
        assertTrue(plan.items.any { it.exercise.formatId in setOf("E002", "E006") })
        assertTrue(plan.items.map { it.exercise.formatId }.containsAll(listOf("E007", "E008")))
        plan.items.filter { it.exercise.lexicalTarget != null }.forEach { item ->
            assertEquals(item.targetDescriptor.lexicalTarget, item.exercise.lexicalTarget)
        }
    }

    @Test
    fun `one lexical target produces one truthful item while grammar fills a short varied run`() {
        val lexicalKey = source.items.first { it.exercise.formatId == "E008" }.targetDescriptor.targetKey()
        val lexical = planner.plan("follow-up-1", 5, listOf(lexicalKey), sourceExercises)
        assertEquals(1, lexical.items.size)
        assertEquals(listOf(lexicalKey), lexical.items.map { it.targetDescriptor.targetKey() })

        val grammarKey = "grammar:G005"
        val grammar = planner.plan("follow-up-1", 5, listOf(grammarKey), sourceExercises)
        val sourceSentence = source.items.single { it.targetDescriptor.targetKey() == grammarKey }
            .exercise.inspector.canonicalSentenceId

        assertEquals(4, grammar.items.size)
        assertTrue(grammar.items.all { it.exercise.formatId == "E003" })
        assertEquals(4, grammar.items.map { it.exercise.inspector.canonicalSentenceId }.distinct().size)
        assertTrue(grammar.items.none { it.exercise.inspector.canonicalSentenceId == sourceSentence })
        assertTrue(grammar.items.all { "L011" in it.exercise.inspector.lexicalIds })
    }

    @Test
    fun `six lexical directions produce exactly six nonrepeated tasks`() {
        val selected = source.items.mapNotNull { item ->
            item.targetDescriptor.targetKey().takeIf { it.startsWith("lexical:") }
        }.distinct()

        val plan = planner.plan("follow-up-1", 29, selected, sourceExercises)

        assertEquals(6, selected.size)
        assertEquals(6, plan.items.size)
        assertEquals(selected.toSet(), plan.items.map { it.targetDescriptor.targetKey() }.toSet())
        assertEquals(6, plan.items.map { it.targetDescriptor.targetKey() }.distinct().size)
    }

    @Test
    fun `article review alternates supply and repair over distinct licensed sentences`() {
        val plan = planner.plan("follow-up-1", 6, listOf("grammar:G006"), sourceExercises)
        val sourceSentence = source.items.single { it.targetDescriptor.targetKey() == "grammar:G006" }
            .exercise.inspector.canonicalSentenceId

        assertEquals(listOf("E002", "E006", "E002", "E006"), plan.items.map { it.exercise.formatId })
        assertEquals(4, plan.items.map { it.exercise.inspector.canonicalSentenceId }.distinct().size)
        assertTrue(plan.items.none { it.exercise.inspector.canonicalSentenceId == sourceSentence })
        assertTrue(plan.items.all { item ->
            item.exercise.inspector.lexicalIds.any { id -> id in setOf("L014", "L015", "L018") }
        })
        assertTrue(plan.items.all { it.answerExposureKeys.single().startsWith("article:") })
    }

    @Test
    fun `mixed targets avoid adjacent repetition when another selected target remains`() {
        val selected = listOf("grammar:G005", "grammar:G006")
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val keys = planner.plan("follow-up-1", seed, selected, sourceExercises)
                .items.map { it.targetDescriptor.targetKey() }
            assertTrue(keys.zipWithNext().none { (left, right) -> left == right }, "seed=$seed keys=$keys")
        }
    }

    @Test
    fun `seeded scheduling retains alternatives until repeated grammar targets are separated`() {
        val lexical = source.items.filter { it.exercise.formatId == "E007" }
            .take(2).map { it.targetDescriptor.targetKey() }
        val selections = listOf(
            listOf("grammar:G005") + lexical,
            listOf("grammar:G005", "grammar:G006", lexical.first()),
        )

        selections.forEach { selected ->
            repeat(64) { seed ->
                val keys = planner.plan("follow-up-1", seed, selected, sourceExercises)
                    .items.map { it.targetDescriptor.targetKey() }
                assertEquals(minimumAdjacentRepeats(keys), adjacentRepeats(keys), "seed=$seed keys=$keys")
            }
        }
    }

    @Test
    fun `same request is deterministic while another seed changes generated evidence`() {
        val selected = listOf("grammar:G005", "grammar:G006")
        val first = planner.plan("follow-up-1", 73, selected, sourceExercises)
        val repeated = planner.plan("follow-up-1", 73, selected, sourceExercises)
        val otherSeed = planner.plan("follow-up-1", 74, selected, sourceExercises)

        assertEquals(first.items.map { it.exercise.id }, repeated.items.map { it.exercise.id })
        assertEquals(first.items.map { it.targetDescriptor }, repeated.items.map { it.targetDescriptor })
        assertNotEquals(first.items.map { it.exercise.id }, otherSeed.items.map { it.exercise.id })
    }

    @Test
    fun `invalid revision selection and source evidence are rejected`() {
        val lexicalKey = source.items.first { it.exercise.lexicalTarget != null }.targetDescriptor.targetKey()
        val recognition = source.items.first { it.exercise.formatId == "E007" }.exercise
        val oppositeDirection = requireNotNull(recognition.lexicalTarget).copy(
            facet = "singular-headword",
            direction = "en-da",
        )
        val oppositeKey = PracticeTargetKeys.key(null, oppositeDirection)
        val legacy = PilotEngine().generate(
            GenerationRequest("E003", "G005", 0, 0, "pilot-1", "generator-1"),
        )

        assertFailsWith<IllegalArgumentException> {
            planner.plan("practice-1", 0, listOf(lexicalKey), sourceExercises)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", -1, listOf(lexicalKey), sourceExercises)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, emptyList(), sourceExercises)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, listOf(lexicalKey), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, List(9) { "grammar:G005" }, sourceExercises)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, listOf(lexicalKey, lexicalKey), sourceExercises)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, listOf("grammar:G006"),
                sourceExercises.filter { PracticeTargetKeys.key(it.targetSkillId, it.lexicalTarget) == "grammar:G005" })
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, listOf(oppositeKey), listOf(recognition))
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, listOf("grammar:G004"), sourceExercises)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan("follow-up-1", 0, listOf("grammar:G005"), listOf(legacy))
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key("G005", source.items.first { it.exercise.lexicalTarget != null }.exercise.lexicalTarget)
        }
    }

    private fun adjacentRepeats(keys: List<String>): Int = keys.zipWithNext().count { (left, right) -> left == right }

    private fun minimumAdjacentRepeats(keys: List<String>): Int {
        val largestGroup = keys.groupingBy { it }.eachCount().values.max()
        return maxOf(0, 2 * largestGroup - keys.size - 1)
    }
}
