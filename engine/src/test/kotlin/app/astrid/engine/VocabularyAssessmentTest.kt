package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyAssessmentTest {
    private val engine = PilotEngine()

    @Test
    fun `catalog and revision dispatch retain vocabulary while current advances`() {
        assertEquals(
            listOf(
                RevisionPair("pilot-1", "generator-1"),
                RevisionPair("pilot-2", "generator-2"),
                RevisionPair("pilot-3", "generator-3"),
                RevisionPair("pilot-4", "generator-4"),
                RevisionPair("pilot-5", "generator-5"),
                RevisionPair("pilot-6", "generator-6"),
                RevisionPair("pilot-7", "generator-7"),
                RevisionPair("pilot-8", "generator-8"),
                RevisionPair("pilot-9", "generator-9"),
                RevisionPair("pilot-10", "generator-10"),
                RevisionPair("pilot-11", "generator-11"),
            ),
            engine.supportedRevisionPairs(),
        )
        assertEquals("evaluation-3", engine.evaluationRevision("pilot-3", "generator-3"))
        assertEquals((1..25).map { "E${it.toString().padStart(3, '0')}" }, engine.catalog().formats.map { it.id })
        assertEquals(LexicalTask("meaning", "da-en"), engine.catalog().formats.single { it.id == "E007" }.lexicalTask)
        assertEquals(LexicalTask("singular-headword", "en-da"), engine.catalog().formats.single { it.id == "E008" }.lexicalTask)
    }

    @Test
    fun `lexical pool cycles without repeats and carries only bounded metadata`() {
        listOf("E007", "E008").forEach { format ->
            val cycle = (0..2).map { generate(format, seed = 47, index = it) }
            assertEquals(setOf("L014-S01", "L015-S01", "L018-S01"), cycle.map { it.lexicalTarget?.senseId }.toSet())
            assertEquals(cycle.first().lexicalTarget, generate(format, seed = 47, index = 3).lexicalTarget)
            cycle.forEach { exercise ->
                assertNull(exercise.targetSkillId)
                assertTrue(exercise.supportSkillIds.isEmpty())
                assertNull(exercise.inspector.canonicalSentenceId)
                assertNull(exercise.inspector.constructionId)
                assertNull(exercise.inspector.canonicalSentence)
                assertEquals(listOf(exercise.lexicalTarget?.entryId), exercise.inspector.lexicalIds)
                assertTrue(exercise.inspector.clauseRoles.isEmpty())
            }
        }
    }

    @Test
    fun `recognition choices are complete deterministic shuffled and position varied`() {
        val expected = setOf("book", "newspaper", "letter (written message)")
        val first = generate("E007", seed = 9, index = 2)
        assertEquals(first.prompt, generate("E007", seed = 9, index = 2).prompt)
        assertEquals(expected, first.prompt.choices.toSet())
        assertEquals(3, first.prompt.choices.size)
        val positions = mutableMapOf<String, MutableSet<Int>>()
        (0..40).forEach { seed ->
            (0..2).forEach { index ->
                val exercise = generate("E007", seed, index)
                val target = requireNotNull(exercise.inspector.lexicalSource).gloss
                positions.getOrPut(target) { mutableSetOf() } += exercise.prompt.choices.indexOf(target)
            }
        }
        assertTrue(positions.values.all { it == setOf(0, 1, 2) }, positions.toString())
    }

    @Test
    fun `prompts have exact languages empty fragments and no ordinary support leakage`() {
        val recognition = find("E007", "L015")
        assertEquals("avis", recognition.prompt.cue)
        assertEquals("da", recognition.prompt.cueLanguage)
        assertEquals("en", recognition.prompt.answerLanguage)
        assertEquals("", recognition.prompt.punctuation)
        assertTrue("newspaper" !in recognition.instruction + recognition.context + recognition.hint)

        val recall = find("E008", "L015")
        assertEquals("newspaper", recall.prompt.cue)
        assertEquals("en", recall.prompt.cueLanguage)
        assertEquals("da", recall.prompt.answerLanguage)
        assertEquals("Danish singular headword", recall.prompt.inputLabel)
        assertEquals("", recall.prompt.before)
        assertEquals("", recall.prompt.after)
        assertTrue("avis" !in recall.instruction + recall.context + recall.hint)
        assertTrue(recall.introduction.any { "en avis" in it.body })
    }

    @Test
    fun `recognition grading distinguishes supplied choices from other text case insensitively`() {
        val exercise = find("E007", "L015")
        assertVocabularyResult(exercise, " NEWSPAPER ", TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        assertVocabularyResult(exercise, "book", TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        val unknown = assertVocabularyResult(
            exercise,
            "journal",
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
        )
        assertEquals("UNRECOGNIZED_MEANING_CHOICE", unknown.issues.single().code)
        assertEquals(NormalizedAnswer.Text("journal"), unknown.normalizedAnswer)
        assertNull(unknown.completedSentence)

        (0..2).forEach { index ->
            val targetExercise = generate("E007", index = index)
            val expected = requireNotNull(targetExercise.inspector.lexicalSource).gloss
            assertEquals(TargetStatus.MET, engine.evaluate(targetExercise, Answer.Text(expected)).targetStatus)
        }
    }

    @Test
    fun `headword grading keeps target language and one word constraint independent`() {
        val exercise = find("E008", "L015")
        assertVocabularyResult(exercise, " Avis ", TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        assertVocabularyResult(exercise, "bog", TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        assertVocabularyResult(exercise, "en avis", TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.NOT_MET)
        assertVocabularyResult(exercise, "et avis", TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET)
        assertVocabularyResult(exercise, "en bog", TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.NOT_MET)
        assertVocabularyResult(exercise, "et bog", TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET)
        assertVocabularyResult(exercise, "tidende", TargetStatus.NOT_MET, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET)
        assertVocabularyResult(exercise, "   ", TargetStatus.NOT_MET, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET)
        assertVocabularyResult(
            exercise,
            "avis\u00a0bog",
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
        )
        assertVocabularyResult(
            exercise,
            "alternativ avis",
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
        )

        (0..2).forEach { index ->
            val targetExercise = generate("E008", index = index)
            val expected = requireNotNull(targetExercise.inspector.lexicalSource).headword
            assertEquals(TargetStatus.MET, engine.evaluate(targetExercise, Answer.Text(expected)).targetStatus)
        }
        PilotContent.nouns.filterNot { it.id == "L015" }.forEach { noun ->
            val result = engine.evaluate(exercise, Answer.Text(noun.text))
            assertEquals(TargetStatus.NOT_MET, result.targetStatus, noun.id)
            assertEquals(LinguisticStatus.ACCEPTABLE, result.linguisticStatus, noun.id)
            assertEquals("INTRODUCED_HEADWORD_REQUIRED", result.issues.single().code, noun.id)
        }
    }

    @Test
    fun `vocabulary normalization preserves case and NFC while comparison remains exact`() {
        val recall = find("E008", "L015")
        val uppercase = engine.evaluate(recall, Answer.Text("AVIS"))
        assertEquals(TargetStatus.MET, uppercase.targetStatus)
        assertEquals(NormalizedAnswer.Text("AVIS"), uppercase.normalizedAnswer)
        assertNotEquals(normalizeText("æ"), normalizeText("ae"))
        val decomposed = engine.evaluate(recall, Answer.Text("a\u030Avis"))
        assertEquals(NormalizedAnswer.Text("åvis"), decomposed.normalizedAnswer)
        assertEquals(LinguisticStatus.UNKNOWN, decomposed.linguisticStatus)
    }

    @Test
    fun `revision and target boundaries reject mixed or caller selected lexical targets`() {
        assertFailsWith<IllegalArgumentException> { generate("E007", target = "G004") }
        assertFailsWith<IllegalArgumentException> { generate("E008", content = "pilot-2", generator = "generator-2") }
        assertFailsWith<IllegalArgumentException> { generate("E006", target = "G006", content = "pilot-1", generator = "generator-1") }
        assertFailsWith<IllegalArgumentException> { generate("E001") }
        assertFailsWith<IllegalArgumentException> { generate("E001", target = "G004", content = "pilot-3", generator = "generator-2") }
    }

    private fun assertVocabularyResult(
        exercise: GeneratedExercise,
        answer: String,
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus,
    ): Evaluation {
        val result = engine.evaluate(exercise, Answer.Text(answer))
        assertEquals(target, result.targetStatus, answer)
        assertEquals(linguistic, result.linguisticStatus, answer)
        assertEquals(constraint, result.responseConstraintStatus, answer)
        val expectedScope =
            if (exercise.formatId == "E007") LinguisticScope.ENGLISH_CHOICE else LinguisticScope.DANISH_HEADWORD
        assertEquals(expectedScope, result.linguisticScope)
        assertNull(result.completedSentence)
        return result
    }

    private fun find(format: String, entryId: String): GeneratedExercise =
        (0..2).map { generate(format, index = it) }.single { it.lexicalTarget?.entryId == entryId }

    private fun generate(
        format: String,
        seed: Int = 0,
        index: Int = 0,
        target: String? = null,
        content: String = "pilot-3",
        generator: String = "generator-3",
    ) = engine.generate(GenerationRequest(format, target, seed, index, content, generator))
}
