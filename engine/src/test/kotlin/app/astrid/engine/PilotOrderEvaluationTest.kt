package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PilotOrderEvaluationTest {
    private val engine = PilotEngine()

    @Test
    fun `every ordering exercise accepts its canonical role order`() {
        (0 until 99).forEach { index ->
            val exercise = engine.generate(GenerationRequest("E003", "G005", seed = 23, index = index))
            val roleToBlock = exercise.source.blockRoles.entries.associate { (block, role) -> role to block }
            val key = exercise.source.answerKey as AnswerKey.SubjectFirstOrder
            val answer = Answer.Order(key.canonicalRoles.map(roleToBlock::getValue))
            val result = engine.evaluate(exercise, answer)

            assertEquals(TargetStatus.MET, result.targetStatus)
            assertEquals(exercise.inspector.canonicalSentence, result.completedSentence)
        }
    }

    @Test
    fun `fronted object is acceptable Danish but misses subject-first target`() {
        val exercise = find("Jeg har en bog.")
        val roleToBlock = exercise.source.blockRoles.entries.associate { (block, role) -> role to block }
        val answer = Answer.Order(listOf("OBJECT", "FINITE_VERB", "SUBJECT").map(roleToBlock::getValue))
        val result = engine.evaluate(exercise, answer)

        assertEquals(TargetStatus.NOT_MET, result.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, result.linguisticStatus)
        assertEquals("VALID_FRONTING_OUTSIDE_TARGET", result.issues.single().code)
        assertEquals("En bog har jeg.", result.completedSentence)
    }

    @Test
    fun `role-aware rendering preserves uppercase plural I outside first position`() {
        val exercise = find("I har en bog.")
        val roleToBlock = exercise.source.blockRoles.entries.associate { (block, role) -> role to block }
        val answer = Answer.Order(listOf("OBJECT", "FINITE_VERB", "SUBJECT").map(roleToBlock::getValue))

        assertEquals("En bog har I.", engine.evaluate(exercise, answer).completedSentence)
    }

    @Test
    fun `missing duplicate and foreign block IDs are unevaluable`() {
        val exercise = find("Jeg har en bog.")
        val ids = exercise.prompt.blocks.map { it.id }
        val invalidAnswers =
            listOf(
                Answer.Order(ids.dropLast(1)),
                Answer.Order(List(ids.size) { ids.first() }),
                Answer.Order(ids.dropLast(1) + "not-a-block"),
            )

        invalidAnswers.forEach { answer ->
            val result = engine.evaluate(exercise, answer)
            assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
            assertEquals("INVALID_BLOCK_SET", result.issues.single().code)
            assertNull(result.completedSentence)
        }
    }

    @Test
    fun `unsupported permutation is not declared invalid Danish`() {
        val exercise = find("Jeg har en bog.")
        val roleToBlock = exercise.source.blockRoles.entries.associate { (block, role) -> role to block }
        val answer = Answer.Order(listOf("FINITE_VERB", "SUBJECT", "OBJECT").map(roleToBlock::getValue))
        val result = engine.evaluate(exercise, answer)

        assertEquals(TargetStatus.NOT_MET, result.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
        assertEquals("ORDER_OUTSIDE_SUPPORTED_PATTERN", result.issues.single().code)
    }

    private fun find(sentence: String): GeneratedExercise =
        (0 until 99).asSequence()
            .map { engine.generate(GenerationRequest("E003", "G005", seed = 0, index = it)) }
            .first { it.inspector.canonicalSentence == sentence }
}
