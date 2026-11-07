package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PracticePlannerTest {
    private val planner = PracticePlanner()

    @Test
    fun `seed zero produces the exact pinned eight item plan`() {
        val plan = planner.plan("reading-basics", "practice-1", 0)

        assertEquals("Words and simple statements", plan.title)
        assertEquals(listOf("G001", "G002", "G003", "G004"), plan.prerequisiteSkillIds)
        assertEquals(listOf("pilot-3", "generator-3", "evaluation-3"),
            listOf(plan.contentRevision, plan.generatorRevision, plan.evaluationRevision))
        assertEquals(
            listOf(
                Triple("E007", null, 0),
                Triple("E003", "G005", 1),
                Triple("E007", null, 1),
                Triple("E008", null, 0),
                Triple("E007", null, 2),
                Triple("E008", null, 1),
                Triple("E006", "G006", 3),
                Triple("E008", null, 2),
            ),
            plan.items.map { Triple(it.exercise.formatId, it.exercise.targetSkillId, it.exercise.reproduction.index) },
        )
        assertEquals(
            listOf("L014-S01", null, "L018-S01", "L014-S01", "L015-S01", "L018-S01", null, "L015-S01"),
            plan.items.map { it.exercise.lexicalTarget?.senseId },
        )
        assertEquals("L015", grammarNoun(plan.items[1]))
        assertEquals("L014", grammarNoun(plan.items[6]))
        assertEquals("Simple subject-first statements", plan.items[1].targetDescriptor.label)
        assertEquals(plan.items[0].exercise.lexicalTarget, plan.items[0].targetDescriptor.lexicalTarget)
        assertEquals(plan.introduction.distinct(), plan.introduction)
    }

    @Test
    fun `every seed covers each sense in both directions without adjacent nouns`() {
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val plan = planner.plan("reading-basics", "practice-1", seed)
            val nouns = plan.items.map(::itemNoun)
            val lexical = plan.items.mapNotNull { it.exercise.lexicalTarget }

            assertEquals((0..7).toList(), plan.items.map { it.position })
            assertTrue(nouns.zipWithNext().all { (first, second) -> first != second }, "seed=$seed nouns=$nouns")
            assertEquals(PilotContent.vocabularySenses.map { it.senseId }.toSet(),
                lexical.filter { it.direction == "da-en" }.map { it.senseId }.toSet())
            assertEquals(PilotContent.vocabularySenses.map { it.senseId }.toSet(),
                lexical.filter { it.direction == "en-da" }.map { it.senseId }.toSet())
            lexical.filter { it.direction == "en-da" }.forEach { recall ->
                val recallPosition = plan.items.single { it.exercise.lexicalTarget == recall }.position
                val recognitionPosition = plan.items.single {
                    val target = it.exercise.lexicalTarget
                    target != null && target.senseId == recall.senseId && target.direction == "da-en"
                }.position
                assertTrue(recognitionPosition < recallPosition)
            }
        }
    }

    @Test
    fun `plan is deterministic and rejects unsupported identities`() {
        val first = planner.plan("reading-basics", "practice-1", 73)
        val repeated = planner.plan("reading-basics", "practice-1", 73)
        val otherSeed = planner.plan("reading-basics", "practice-1", 74)

        assertEquals(first.items.map { it.exercise.id }, repeated.items.map { it.exercise.id })
        assertEquals(first.items.map { it.exercise.presentation }, repeated.items.map { it.exercise.presentation })
        assertNotEquals(first.items.map { it.exercise.id }, otherSeed.items.map { it.exercise.id })
        assertFailsWith<IllegalArgumentException> { planner.plan("other", "practice-1", 0) }
        assertFailsWith<IllegalArgumentException> { planner.plan("reading-basics", "practice-2", 0) }
        assertFailsWith<IllegalArgumentException> { planner.plan("reading-basics", "practice-1", -1) }
    }

    @Test
    fun `exposure metadata covers answers choices cues and grammar context`() {
        val plan = planner.plan("reading-basics", "practice-1", 0)
        val recognition = plan.items[0]
        val ordering = plan.items[1]
        val recall = plan.items[3]
        val repair = plan.items[6]

        assertEquals(listOf("gloss:L014-S01"), recognition.answerExposureKeys)
        assertEquals(
            setOf("gloss:L014-S01", "gloss:L015-S01", "gloss:L018-S01", "headword:L014-S01"),
            recognition.promptExposureKeys.toSet(),
        )
        assertEquals(listOf("headword:L014-S01"), recall.answerExposureKeys)
        assertEquals(listOf("gloss:L014-S01"), recall.promptExposureKeys)
        assertEquals(
            setOf("headword:L014-S01", "article:L014", "gloss:L014-S01"),
            recognition.lessonExposureKeys.toSet(),
        )
        assertEquals(
            setOf("headword:L015-S01", "article:L015", "gloss:L015-S01"),
            ordering.promptExposureKeys.toSet(),
        )
        assertEquals(listOf("grammar:G005"), ordering.answerExposureKeys)
        assertEquals(listOf("article:L014"), repair.answerExposureKeys)
        assertTrue(repair.lessonExposureKeys.containsAll(repair.answerExposureKeys))
        assertTrue(repair.hintExposureKeys.isEmpty())
        assertTrue(repair.feedbackExposureKeys.containsAll(repair.promptExposureKeys.filterNot { it.startsWith("gloss:") }))
        assertTrue(repair.feedbackExposureKeys.none { it.startsWith("gloss:") })
        assertTrue(repair.feedbackExposureKeys.containsAll(repair.answerExposureKeys))
        assertTrue(recognition.hintExposureKeys.isEmpty())
        assertTrue(recall.hintExposureKeys.isEmpty())
        assertEquals(ordering.answerExposureKeys, ordering.hintExposureKeys)
    }

    private fun itemNoun(item: PracticePlanItem): String =
        item.exercise.lexicalTarget?.entryId ?: grammarNoun(item)

    private fun grammarNoun(item: PracticePlanItem): String =
        item.exercise.inspector.lexicalIds.single { id -> PilotContent.vocabularySenses.any { it.entryId == id } }
}
