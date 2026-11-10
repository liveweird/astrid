package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecallRepairTest {
    private val engine = PilotEngine()

    @Test
    fun `current catalog and revision APIs expose exact supported pairs`() {
        val catalog = engine.catalog()
        assertEquals((1..25).map { "E${it.toString().padStart(3, '0')}" }, catalog.formats.map { it.id })
        assertEquals("pilot-10", catalog.contentRevision)
        assertEquals("generator-10", catalog.generatorRevision)
        assertEquals(109, catalog.sentenceCount)
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
            ),
            engine.supportedRevisionPairs(),
        )
        assertEquals("evaluation-1", engine.evaluationRevision("pilot-1", "generator-1"))
        assertEquals("evaluation-2", engine.evaluationRevision("pilot-2", "generator-2"))
        assertEquals("evaluation-3", engine.evaluationRevision("pilot-3", "generator-3"))
        assertEquals("evaluation-4", engine.evaluationRevision("pilot-4", "generator-4"))
        assertEquals("evaluation-5", engine.evaluationRevision("pilot-5", "generator-5"))
        assertEquals("evaluation-6", engine.evaluationRevision("pilot-6", "generator-6"))
        assertEquals("evaluation-7", engine.evaluationRevision("pilot-7", "generator-7"))
        assertEquals("evaluation-8", engine.evaluationRevision("pilot-8", "generator-8"))
        assertEquals("evaluation-9", engine.evaluationRevision("pilot-9", "generator-9"))
        assertNull(engine.evaluationRevision("pilot-1", "generator-2"))
        assertNull(engine.evaluationRevision("pilot-2", "generator-1"))
    }

    @Test
    fun `legacy and mixed revisions reject new formats`() {
        assertFailsWith<IllegalArgumentException> { engine.generate(GenerationRequest("E004", "G004", 0, 0)) }
        assertFailsWith<IllegalArgumentException> {
            engine.generate(current("E004", "G004").copy(contentRevision = "pilot-1"))
        }
        assertFailsWith<IllegalArgumentException> {
            engine.generate(current("E004", "G004").copy(generatorRevision = "generator-1"))
        }
    }

    @Test
    fun `new formats enumerate exact deterministic pools`() {
        listOf(Triple("E004", "G004", 99), Triple("E005", "G004", 99), Triple("E006", "G006", 92))
            .forEach { (format, target, size) ->
                val cycle = (0 until size).map { engine.generate(current(format, target, 47, it)) }
                assertEquals(size, cycle.map { it.inspector.canonicalSentenceId }.toSet().size)
                assertEquals(
                    cycle.map { it.id },
                    (0 until size).map { engine.generate(current(format, target, 47, it)).id },
                )
                assertEquals(
                    cycle.first().inspector.canonicalSentenceId,
                    engine.generate(current(format, target, 47, size)).inspector.canonicalSentenceId,
                )
                assertTrue(cycle.all { "pilot-2-generator-2" in it.id })
            }
    }

    @Test
    fun `recall and repair prompts delimit exactly one answer word`() {
        val recall = find("E004", "G004", "Hun læser en avis.")
        val repair = find("E005", "G004", "Hun læser en avis.")
        assertEquals("Hun ", recall.prompt.before)
        assertEquals(" en avis.", recall.prompt.after)
        assertEquals("Present form", recall.prompt.inputLabel)
        assertNull(recall.prompt.replacementWord)
        assertTrue("læse" in recall.instruction && "read" in recall.instruction)
        assertTrue("læser" !in ordinarySupport(recall))
        assertEquals("Replacement verb", repair.prompt.inputLabel)
        assertEquals("læse", repair.prompt.replacementWord)
        assertTrue("læser" !in ordinarySupport(repair))
        assertOneWordDifference(requireNotNull(repair.inspector.canonicalSentence), displayed(repair))
    }

    @Test
    fun `article repair swaps one article for every eligible sentence`() {
        (0 until 92).forEach { index ->
            val exercise = engine.generate(current("E006", "G006", 13, index))
            val key = assertIs<AnswerKey.Article>(exercise.source.answerKey)
            assertEquals(if (key.expected == "en") "et" else "en", exercise.prompt.replacementWord)
            assertEquals("Replacement article", exercise.prompt.inputLabel)
            assertTrue("unstressed article" in exercise.instruction)
            assertOneWordDifference(requireNotNull(exercise.inspector.canonicalSentence), displayed(exercise))
        }
    }

    @Test
    fun `every verb repair corrupts one word and its answer restores the canonical sentence`() {
        (0 until 99).forEach { index ->
            val exercise = engine.generate(current("E005", "G004", 29, index))
            val key = assertIs<AnswerKey.PresentForm>(exercise.source.answerKey)
            assertOneWordDifference(requireNotNull(exercise.inspector.canonicalSentence), displayed(exercise))
            assertEquals(
                exercise.inspector.canonicalSentence,
                engine.evaluate(exercise, Answer.Text(key.present)).completedSentence,
            )
        }
    }

    @Test
    fun `finite grading recognizes only exact licensed substitutions`() {
        listOf("E004", "E005").forEach { format ->
            val exercise = find(format, "G004", "Hun læser en avis.")
            assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Text(" læser ")).targetStatus)
            val infinitive = engine.evaluate(exercise, Answer.Text("læse"))
            assertEquals("INFINITIVE_USED", infinitive.issues.single().code)
            assertEquals(LinguisticStatus.KNOWN_ISSUE, infinitive.linguisticStatus)
            val alternative = engine.evaluate(exercise, Answer.Text("har"))
            assertEquals("NAMED_VERB_REQUIRED", alternative.issues.single().code)
            assertEquals(LinguisticStatus.ACCEPTABLE, alternative.linguisticStatus)
            assertEquals("Hun har en avis.", alternative.completedSentence)
            val unsupported = engine.evaluate(exercise, Answer.Text("spiser"))
            assertEquals("UNRECOGNIZED_FORM", unsupported.issues.single().code)
            assertEquals(LinguisticStatus.UNKNOWN, unsupported.linguisticStatus)
        }
    }

    @Test
    fun `new text formats preserve NFC Danish distinctions uppercase I and answer shape`() {
        assertEquals("å", normalizeText("a\u030A"))
        assertEquals("ø", normalizeText("ø"))
        assertTrue(normalizeText("æøå") != normalizeText("aeoa"))
        val exercise = find("E004", "G004", "I læser et brev.")
        assertEquals("I læser et brev.", engine.evaluate(exercise, Answer.Text("læser")).completedSentence)
        assertEquals(LinguisticStatus.UNKNOWN, engine.evaluate(exercise, Answer.Text("laeser")).linguisticStatus)
        val mismatch = engine.evaluate(exercise, Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, mismatch.targetStatus)
        assertEquals("ANSWER_TYPE_MISMATCH", mismatch.issues.single().code)

        val article = find("E006", "G006", "Det er et æble.")
        assertEquals(TargetStatus.MET, engine.evaluate(article, Answer.Text(" et ")).targetStatus)
        assertEquals("NUMERAL_NOT_ARTICLE", engine.evaluate(article, Answer.Text("e\u0301t")).issues.single().code)
        assertEquals(LinguisticStatus.UNKNOWN, engine.evaluate(article, Answer.Text("aeble")).linguisticStatus)
    }

    private fun current(format: String, target: String, seed: Int = 0, index: Int = 0) =
        GenerationRequest(format, target, seed, index, "pilot-2", "generator-2")

    private fun find(format: String, target: String, sentence: String): GeneratedExercise =
        (0 until 99).asSequence()
            .map { engine.generate(current(format, target, index = it)) }
            .first { it.inspector.canonicalSentence == sentence }

    private fun ordinarySupport(exercise: GeneratedExercise): String =
        listOf(exercise.instruction, exercise.context, exercise.hint).joinToString(" ")

    private fun displayed(exercise: GeneratedExercise) =
        "${exercise.prompt.before}${exercise.prompt.replacementWord}${exercise.prompt.after}"

    private fun assertOneWordDifference(canonical: String, corrupted: String) {
        val expected = canonical.split(" ")
        val actual = corrupted.split(" ")
        assertEquals(expected.size, actual.size)
        assertEquals(1, expected.zip(actual).count { (left, right) -> left != right })
    }
}
