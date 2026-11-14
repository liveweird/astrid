package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WritingReadingTest {
    private val engine = PilotEngine()

    @Test
    fun `revision four catalog appends writing and reading without changing request defaults`() {
        val catalog = engine.catalog()

        assertEquals("pilot-20", catalog.contentRevision)
        assertEquals("generator-20", catalog.generatorRevision)
        assertEquals((1..51).map { "E${it.toString().padStart(3, '0')}" }, catalog.formats.map { it.id })
        assertEquals("Write a Danish sentence", catalog.formats.single { it.id == "E009" }.title)
        assertEquals("Read a short text", catalog.formats.single { it.id == "E010" }.title)
        assertEquals(
            ReadingTask("participant-action-complement"),
            catalog.formats.single { it.id == "E010" }.readingTask,
        )
        assertEquals("pilot-1", GenerationRequest("E001", "G004", 0, 0).contentRevision)
        assertEquals("generator-1", GenerationRequest("E001", "G004", 0, 0).generatorRevision)
        assertEquals("evaluation-4", engine.evaluationRevision("pilot-4", "generator-4"))
        assertEquals(20, engine.supportedRevisionPairs().size)
    }

    @Test
    fun `all 99 writing prompts preserve meaning and accept their canonical sentence`() {
        val cycle = (0 until 99).map { engine.generate(writing(index = it)) }

        assertEquals(99, cycle.map { it.inspector.canonicalSentenceId }.toSet().size)
        cycle.forEach { exercise ->
            val canonical = assertNotNull(exercise.inspector.canonicalSentence)
            assertEquals("", exercise.prompt.punctuation)
            assertEquals("en", exercise.prompt.cueLanguage)
            assertEquals("da", exercise.prompt.answerLanguage)
            assertFalse(canonical in ordinarySupport(exercise))
            assertEquals(expectedWritingCue(exercise), exercise.prompt.cue, canonical)
            assertTrue(exercise.introduction.all { it.bodyLanguage == "da" })
            assertTrue(exercise.introduction.all { it.body.split(' ').size <= 2 })
            val evaluation = engine.evaluate(exercise, Answer.Text(canonical))
            assertEquals(TargetStatus.MET, evaluation.targetStatus)
            assertEquals(MeaningSatisfaction.MET, evaluation.meaningSatisfaction)
            assertEquals(LinguisticStatus.ACCEPTABLE, evaluation.linguisticStatus)
            assertEquals(ResponseConstraintStatus.MET, evaluation.responseConstraintStatus)
            assertEquals(LinguisticScope.DANISH_SENTENCE, evaluation.linguisticScope)
            assertEquals(canonical, evaluation.completedSentence)
        }
    }

    @Test
    fun `writing grades target meaning language and shape independently`() {
        val exercise = findWriting("Hun læser en avis.")

        assertWriting(exercise, "Hun læser en avis.", TargetStatus.MET, MeaningSatisfaction.MET, LinguisticStatus.ACCEPTABLE)
        assertWriting(exercise, "En avis læser hun.", TargetStatus.NOT_MET, MeaningSatisfaction.MET, LinguisticStatus.ACCEPTABLE)
        assertWriting(exercise, "Han læser en avis.", TargetStatus.MET, MeaningSatisfaction.NOT_MET, LinguisticStatus.ACCEPTABLE)
        assertWriting(exercise, "Hun har en avis.", TargetStatus.MET, MeaningSatisfaction.NOT_MET, LinguisticStatus.ACCEPTABLE)
        assertWriting(exercise, "Hun læser et avis.", TargetStatus.MET, MeaningSatisfaction.MET, LinguisticStatus.KNOWN_ISSUE)
        assertWriting(exercise, "Hun læse en avis.", TargetStatus.NOT_MET, MeaningSatisfaction.MET, LinguisticStatus.KNOWN_ISSUE)

        val unknown = engine.evaluate(exercise, Answer.Text("Hun studerer en avis."))
        assertEquals(TargetStatus.UNEVALUABLE, unknown.targetStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, unknown.meaningSatisfaction)
        assertEquals(LinguisticStatus.UNKNOWN, unknown.linguisticStatus)
        assertEquals(ResponseConstraintStatus.MET, unknown.responseConstraintStatus)

        val shape = engine.evaluate(exercise, Answer.Text("Hun læser en avis. Han sover."))
        assertEquals(TargetStatus.UNEVALUABLE, shape.targetStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, shape.meaningSatisfaction)
        assertEquals(ResponseConstraintStatus.NOT_MET, shape.responseConstraintStatus)
        assertEquals("ONE_STATEMENT_REQUIRED", shape.issues.single().code)
        assertFalse("supplied choice" in shape.feedback.lowercase())

        listOf("Hun læser en avis; Han sover", "Hun læser en avis: Han sover").forEach { answer ->
            val separated = engine.evaluate(exercise, Answer.Text(answer))
            assertEquals(TargetStatus.UNEVALUABLE, separated.targetStatus, answer)
            assertEquals(MeaningSatisfaction.UNKNOWN, separated.meaningSatisfaction, answer)
            assertEquals(ResponseConstraintStatus.NOT_MET, separated.responseConstraintStatus, answer)
            assertEquals("ONE_STATEMENT_REQUIRED", separated.issues.single().code, answer)
        }
    }

    @Test
    fun `writing reference separates English labels from Danish forms without assembling the answer`() {
        val exercise = findWriting("Hun læser et brev.")

        assertEquals(
            listOf(
                IntroductionSection("Noun phrase: letter (written message)", "et brev", "da"),
                IntroductionSection("Finite present verb: read", "læser", "da"),
                IntroductionSection("Pronoun: she", "hun", "da"),
            ),
            exercise.introduction,
        )
        assertFalse(exercise.introduction.joinToString(" ") { it.body }.contains("Hun læser et brev"))
    }

    @Test
    fun `writing preserves unicode normalized submission and uppercase plural you diagnosis`() {
        val pluralYou = findWriting("I læser et brev.")
        val caseIssue = engine.evaluate(pluralYou, Answer.Text(" i  læser et brev. "))

        assertEquals(Answer.Text(" i  læser et brev. "), caseIssue.rawAnswer)
        assertEquals(NormalizedAnswer.Text("i læser et brev."), caseIssue.normalizedAnswer)
        assertEquals("i læser et brev.", caseIssue.completedSentence)
        assertEquals(TargetStatus.MET, caseIssue.targetStatus)
        assertEquals(MeaningSatisfaction.MET, caseIssue.meaningSatisfaction)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, caseIssue.linguisticStatus)
        assertTrue(caseIssue.issues.any { it.code == "PRONOUN_I_CASE" })

        val decomposed = engine.evaluate(findWriting("Hun læser en bog."), Answer.Text("Hun læser en bo\u0301g."))
        assertEquals(NormalizedAnswer.Text("Hun læser en bóg."), decomposed.normalizedAnswer)
        assertEquals(LinguisticStatus.UNKNOWN, decomposed.linguisticStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, decomposed.meaningSatisfaction)

        val prefix = engine.evaluate(findWriting("Hun læser en avis."), Answer.Text("Hun læser en avis. ekstra"))
        assertEquals(ResponseConstraintStatus.NOT_MET, prefix.responseConstraintStatus)
    }

    @Test
    fun `multiple controlled writing deviations remain unknown rather than guessed repairs`() {
        val newspaper = findWriting("Hun læser en avis.")
        val pluralYou = findWriting("I læser et brev.")

        listOf(
            newspaper to "Hun læse et avis.",
            pluralYou to "i læse et brev.",
            pluralYou to "i læser en brev.",
        ).forEach { (exercise, answer) ->
            val evaluation = engine.evaluate(exercise, Answer.Text(answer))
            assertEquals(TargetStatus.UNEVALUABLE, evaluation.targetStatus, answer)
            assertEquals(MeaningSatisfaction.UNKNOWN, evaluation.meaningSatisfaction, answer)
            assertEquals(LinguisticStatus.UNKNOWN, evaluation.linguisticStatus, answer)
            assertEquals(ResponseConstraintStatus.MET, evaluation.responseConstraintStatus, answer)
            assertEquals("WRITING_OUTSIDE_BOUNDED_DIAGNOSIS", evaluation.issues.single().code, answer)
        }
    }

    @Test
    fun `all 24 reading units bind one participant action and object with varied evidence`() {
        val cycle = (0 until 24).map { engine.generate(reading(index = it)) }
        val semanticKeys = cycle.map { exercise ->
            val source = assertNotNull(exercise.inspector.readingSource)
            listOf(source.questionSubjectId, source.questionVerbId) +
                source.propositions.sortedWith(compareBy({ it.subjectId }, { it.verbId }))
                    .map { "${it.subjectId}:${it.verbId}:${it.objectId}" }
        }

        assertEquals(24, semanticKeys.toSet().size)
        assertEquals(setOf(0, 1, 2), cycle.map { exercise ->
            val source = assertNotNull(exercise.inspector.readingSource)
            source.propositions.indexOfFirst {
                it.subjectId == source.questionSubjectId && it.verbId == source.questionVerbId
            }
        }.toSet())
        assertEquals(setOf(0, 1, 2), cycle.map { exercise ->
            exercise.prompt.choices.indexOf(assertNotNull(exercise.inspector.readingSource).answer)
        }.toSet())

        cycle.forEach { exercise ->
            val source = assertNotNull(exercise.inspector.readingSource)
            val propositions = source.propositions
            assertEquals(3, propositions.size)
            assertEquals(3, propositions.map { it.objectId }.toSet().size)
            assertEquals(1, propositions.count {
                it.subjectId == source.questionSubjectId && it.verbId == source.questionVerbId
            })
            assertEquals(source.answerNounId, propositions.single {
                it.subjectId == source.questionSubjectId && it.verbId == source.questionVerbId
            }.objectId)
            assertEquals(READING_GLOSSES.getValue(source.answerNounId), source.answer)
            assertEquals(3, exercise.prompt.choices.toSet().size)
            assertEquals(READING_GLOSSES.values.toSet(), exercise.prompt.choices.toSet())
            assertEquals(3, assertNotNull(exercise.prompt.passage).size)
            assertEquals(
                propositions.map(::expectedReadingSentence),
                exercise.prompt.passage,
                source.toString(),
            )
            propositions.forEach { proposition ->
                assertEquals(
                    "C002-${proposition.subjectId}-${proposition.verbId}-${proposition.objectId}",
                    proposition.sentenceId,
                )
            }
            assertEquals("", exercise.prompt.punctuation)
            assertEquals(ReadingTask("participant-action-complement"), exercise.readingTarget)
            assertNull(exercise.targetSkillId)
            assertNull(exercise.lexicalTarget)
            assertNull(exercise.inspector.canonicalSentenceId)
            assertNull(exercise.inspector.constructionId)
            assertNull(exercise.inspector.canonicalSentence)
            assertTrue(exercise.inspector.clauseRoles.isEmpty())
            assertEquals(exercise.inspector.lexicalIds.distinct(), exercise.inspector.lexicalIds)
            assertEquals(exercise.prompt, engine.generate(reading(index = exercise.reproduction.index)).prompt)

            val correct = engine.evaluate(exercise, Answer.Text(source.answer))
            assertEquals(TargetStatus.MET, correct.targetStatus)
            val distractor = exercise.prompt.choices.first { it != source.answer }
            assertEquals(TargetStatus.NOT_MET, engine.evaluate(exercise, Answer.Text(distractor)).targetStatus)
        }
    }

    @Test
    fun `pilot four review includes every reading passage while retained review stays pilot three`() {
        val retained = PilotReview.renderPilotThreeMarkdown()
        val retainedFour = PilotReview.renderPilotFourMarkdown()
        val current = PilotReview.renderMarkdown()

        assertTrue("Content revision: `pilot-3`" in retained)
        assertFalse("Short-reading teaching units" in retained)
        assertTrue("Content revision: `pilot-4`" in retainedFour)
        assertTrue("Generator revision: `generator-4`" in retainedFour)
        assertTrue("Content revision: `pilot-20`" in current)
        assertTrue("Content revision: `pilot-10`" in PilotReview.renderPilotTenMarkdown())
        val writingReview = retainedFour.substringAfter("## Sentence-writing teaching units")
            .substringBefore("## Short-reading teaching units")
        val readingReview = retainedFour.substringAfter("## Short-reading teaching units")
        assertEquals(99, numberedRows(writingReview))
        assertEquals(24, numberedRows(readingReview))
        assertTrue("English cue | Optional language reference | Hint" in writingReview)
        assertTrue("Choices in display order | Answer" in readingReview)
    }

    private fun writing(seed: Int = 0, index: Int = 0) =
        GenerationRequest("E009", "G005", seed, index, "pilot-4", "generator-4")

    private fun reading(seed: Int = 0, index: Int = 0) =
        GenerationRequest("E010", null, seed, index, "pilot-4", "generator-4")

    private fun findWriting(sentence: String): GeneratedExercise =
        (0 until 99).asSequence().map { engine.generate(writing(index = it)) }
            .first { it.inspector.canonicalSentence == sentence }

    private fun assertWriting(
        exercise: GeneratedExercise,
        answer: String,
        target: TargetStatus,
        meaning: MeaningSatisfaction,
        language: LinguisticStatus,
    ) {
        val evaluation = engine.evaluate(exercise, Answer.Text(answer))
        assertEquals(target, evaluation.targetStatus, answer)
        assertEquals(meaning, evaluation.meaningSatisfaction, answer)
        assertEquals(language, evaluation.linguisticStatus, answer)
        assertEquals(ResponseConstraintStatus.MET, evaluation.responseConstraintStatus, answer)
    }

    private fun ordinarySupport(exercise: GeneratedExercise): String =
        listOf(exercise.instruction, exercise.context, exercise.hint, exercise.prompt.cue.orEmpty()).joinToString(" ")

    private fun expectedWritingCue(exercise: GeneratedExercise): String {
        val sentence = (exercise.source as SentenceSource).sentence
        val subjectId = sentence.subject.lexicalIds.single()
        val subject = WRITING_SUBJECTS.getValue(subjectId)
        val thirdPerson = subjectId in setOf("L003", "L004", "L008")
        val verb = WRITING_VERBS.getValue(sentence.verb.lexicalId)[if (thirdPerson) 1 else 0]
        val nounId = sentence.complement?.lexicalIds?.lastOrNull()
        val complement = nounId?.let { " ${WRITING_NOUNS.getValue(it)}" }.orEmpty()
        return "$subject $verb$complement."
    }

    private fun expectedReadingSentence(proposition: ReadingPropositionDetails): String =
        "${READING_SUBJECTS.getValue(proposition.subjectId)} " +
            "${READING_VERBS.getValue(proposition.verbId)} ${READING_NOUN_PHRASES.getValue(proposition.objectId)}."

    private fun numberedRows(markdown: String): Int =
        markdown.lineSequence().count { it.matches(Regex("\\| \\d+ \\|.*")) }

    private companion object {
        val WRITING_SUBJECTS = mapOf(
            "L001" to "I",
            "L002" to "You (one person)",
            "L003" to "He",
            "L004" to "She",
            "L005" to "We",
            "L006" to "You (more than one person)",
            "L007" to "They",
            "L008" to "It or that",
        )
        val WRITING_VERBS = mapOf(
            "L009" to listOf("have", "has"),
            "L010" to listOf("is", "is"),
            "L011" to listOf("read", "reads"),
            "L012" to listOf("eat", "eats"),
            "L013" to listOf("sleep", "sleeps"),
        )
        val WRITING_NOUNS = mapOf(
            "L014" to "a book",
            "L015" to "a newspaper",
            "L016" to "a car",
            "L017" to "a dog",
            "L018" to "a letter (written message)",
            "L019" to "a house",
            "L020" to "a table",
            "L021" to "an apple",
        )
        val READING_SUBJECTS = mapOf("L003" to "Han", "L004" to "Hun")
        val READING_VERBS = mapOf("L009" to "har", "L011" to "læser")
        val READING_NOUN_PHRASES = mapOf("L014" to "en bog", "L015" to "en avis", "L018" to "et brev")
        val READING_GLOSSES = mapOf(
            "L014" to "book",
            "L015" to "newspaper",
            "L018" to "letter (written message)",
        )
    }
}
