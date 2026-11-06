package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PilotEvaluationTest {
    private val engine = PilotEngine()

    @Test
    fun `every present form and infinitive pair is evaluated against its own verb`() {
        (0 until 99).forEach { index ->
            val exercise = engine.generate(GenerationRequest("E001", "G004", seed = 11, index = index))
            val key = assertIs<AnswerKey.PresentForm>(exercise.source.answerKey)

            assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Text(key.present)).targetStatus)
            val infinitive = engine.evaluate(exercise, Answer.Text(key.infinitive))
            assertEquals(TargetStatus.NOT_MET, infinitive.targetStatus)
            assertEquals(LinguisticStatus.KNOWN_ISSUE, infinitive.linguisticStatus)
            assertEquals("INFINITIVE_USED", infinitive.issues.single().code)
        }
    }

    @Test
    fun `every licensed noun uses its stored article`() {
        (0 until 92).forEach { index ->
            val exercise = engine.generate(GenerationRequest("E002", "G006", seed = 17, index = index))
            val key = assertIs<AnswerKey.Article>(exercise.source.answerKey)

            assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Text(key.expected)).targetStatus)
            val other = if (key.expected == "en") "et" else "en"
            val mismatch = engine.evaluate(exercise, Answer.Text(other))
            assertEquals(TargetStatus.NOT_MET, mismatch.targetStatus)
            assertEquals(LinguisticStatus.KNOWN_ISSUE, mismatch.linguisticStatus)
            assertEquals("ARTICLE_GENDER_MISMATCH", mismatch.issues.single().code)
        }
    }

    @Test
    fun `worked text cases preserve raw input unicode and unsupported uncertainty`() {
        val readNewspaper = find("E001", "G004", "Hun læser en avis.")
        assertEquals("Hun læser en avis.", engine.evaluate(readNewspaper, Answer.Text("læser")).completedSentence)
        assertEquals(TargetStatus.NOT_MET, engine.evaluate(readNewspaper, Answer.Text("læse")).targetStatus)
        val transliteration = engine.evaluate(readNewspaper, Answer.Text("laeser"))
        assertEquals(LinguisticStatus.UNKNOWN, transliteration.linguisticStatus)

        val house = find("E002", "G006", "Jeg har et hus.")
        val spaced = engine.evaluate(house, Answer.Text(" et "))
        assertEquals(TargetStatus.MET, spaced.targetStatus)
        assertEquals(Answer.Text(" et "), spaced.rawAnswer)
        assertEquals(NormalizedAnswer.Text("et"), spaced.normalizedAnswer)

        val numeral = engine.evaluate(house, Answer.Text("ét"))
        assertEquals(TargetStatus.NOT_MET, numeral.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, numeral.linguisticStatus)
        assertEquals("Jeg har ét hus.", numeral.completedSentence)

        val decomposed = engine.evaluate(house, Answer.Text("e\u0301t"))
        assertEquals(NormalizedAnswer.Text("ét"), decomposed.normalizedAnswer)
        assertEquals(Answer.Text("e\u0301t"), decomposed.rawAnswer)

        val book = find("E002", "G006", "Vi har en bog.")
        val commonNumeral = engine.evaluate(book, Answer.Text("én"))
        assertEquals(TargetStatus.NOT_MET, commonNumeral.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, commonNumeral.linguisticStatus)
        assertEquals("Vi har én bog.", commonNumeral.completedSentence)

        assertEquals(
            LinguisticStatus.KNOWN_ISSUE,
            engine.evaluate(book, Answer.Text("ét")).linguisticStatus,
        )
        assertEquals(
            LinguisticStatus.KNOWN_ISSUE,
            engine.evaluate(house, Answer.Text("én")).linguisticStatus,
        )
    }

    @Test
    fun `Danish letters and case remain distinct`() {
        assertEquals(normalizeText("å"), normalizeText("a\u030A"))
        assertTrue(normalizeText("å") != normalizeText("a"))

        val apple = find("E002", "G006", "Det er et æble.")
        assertEquals(TargetStatus.MET, engine.evaluate(apple, Answer.Text("et")).targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, engine.evaluate(apple, Answer.Text("aeble")).linguisticStatus)

        val pluralYou = find("E001", "G004", "I sover.")
        assertEquals("I sover.", engine.evaluate(pluralYou, Answer.Text("sover")).completedSentence)
        assertTrue(pluralYou.inspector.canonicalSentence.startsWith("I "))
    }

    @Test
    fun `answer type mismatch is unevaluable rather than learner error`() {
        val exercise = engine.generate(GenerationRequest("E001", "G004", 0, 0))
        val result = engine.evaluate(exercise, Answer.Order(emptyList()))

        assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
        assertEquals("ANSWER_TYPE_MISMATCH", result.issues.single().code)
        assertNull(result.completedSentence)
    }

    private fun find(
        format: String,
        target: String,
        sentence: String,
    ): GeneratedExercise =
        (0 until 99).asSequence()
            .map { engine.generate(GenerationRequest(format, target, seed = 0, index = it)) }
            .first { it.inspector.canonicalSentence == sentence }
}
