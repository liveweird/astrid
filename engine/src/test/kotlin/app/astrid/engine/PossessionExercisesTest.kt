package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PossessionExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair eleven publishes literal possession cards while pair ten remains unchanged`() {
        assertEquals("pilot-12", engine.catalog().contentRevision)
        assertEquals("generator-12", engine.catalog().generatorRevision)
        assertEquals("evaluation-10", engine.evaluationRevision("pilot-10", "generator-10"))
        assertEquals("evaluation-11", engine.evaluationRevision("pilot-11", "generator-11"))

        val cards = possessionExercises()
        assertEquals(
            listOf(
                "Han har et brev.", "Vi har en avis.", "Du har en bog.", "De har et brev.",
            ),
            cards.map { it.inspector.canonicalSentence },
        )
        assertEquals(
            listOf("Han ", "Vi ", "Du ", "De "),
            cards.map { it.prompt.before },
        )
        assertEquals(listOf(" et brev.", " en avis.", " en bog.", " et brev."), cards.map { it.prompt.after })
        assertEquals(listOf("at have", "at have", "at have", "at have"), cards.map { it.prompt.cue })
        assertEquals(listOf("da", "da", "da", "da"), cards.map { it.prompt.cueLanguage })
        assertEquals(listOf("da", "da", "da", "da"), cards.map { it.prompt.answerLanguage })
        assertEquals(listOf(2, 2, 0, 0), cards.map { it.prompt.choices.size })
        assertEquals(listOf(null, null, "Danish present verb", "Danish present verb"), cards.map { it.prompt.inputLabel })
        assertEquals(
            listOf(
                "English: He has a letter. The subject refers to one stated man. The supplied object et brev means a letter.",
                "English: We have a newspaper. The subject refers to the speaker and another person. " +
                    "The supplied object en avis means a newspaper.",
                "English: You have a book. The subject refers to one addressee. The supplied object en bog means a book.",
                "English: They have a letter. The subject refers to several other people. " +
                    "The supplied object et brev means a letter.",
            ),
            cards.map { it.context },
        )
        cards.forEach { exercise ->
            assertEquals("have-possession-present", exercise.targetScopeId)
            assertEquals("G004", exercise.targetSkillId)
            assertEquals("L009", requireNotNull(exercise.inspector.moduleOneSource).targetLexicalId)
            assertEquals("har", requireNotNull(exercise.inspector.moduleOneSource).expectedForm)
            assertTrue(exercise.introduction.any { it.body == "at have → har" })
            assertTrue(exercise.introduction.any { it.body == "Jeg har en bog." })
            assertTrue("ending in -r" !in exercise.introduction.joinToString { it.body })
            assertEquals("Use the stored one-word present form har from at have.", exercise.hint)
        }

        val retainedSelection = retainedExercise("E022", "present-select-work")
        assertEquals("named-three-verbs-present", retainedSelection.targetScopeId)
        assertTrue(retainedSelection.introduction.any { "ends in -r" in it.body })
        assertEquals(3, PresentOrderContent.tasks("E022").size)
        assertEquals(5, PresentOrderContent.tasks("E022", includePossession = true).size)
    }

    @Test
    fun `possession forms preserve exact known and unknown grading boundaries and raw answer`() {
        val selection = currentExercise("E022", "possession-select-letter")
        assertTrue(engine.isSuppliedChoice(selection, "  har  "))
        assertTrue(engine.isSuppliedChoice(selection, "have"))
        assertFalse(engine.isSuppliedChoice(selection, "arbejder"))
        val cases = listOf(
            "har" to Expected(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, null, "har"),
            "have" to Expected(TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.MET, "INFINITIVE_USED", "har"),
            "arbejder" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, "NAMED_VERB_MISMATCH", "har",
            ),
            "Har" to Expected(
                TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "MID_SENTENCE_VERB_CASE_REQUIRED", "har",
            ),
            "har nu" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "ONE_WORD_RESPONSE_REQUIRED", "har",
            ),
            "havde" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_PRESENT_FORM", null,
            ),
            "har." to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_PRESENT_FORM", null,
            ),
            "  " to Expected(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET, "EMPTY_RESPONSE", null),
        )
        cases.forEach { (answer, expected) ->
            val result = engine.evaluate(selection, Answer.Text(answer))
            assertEquals(expected.target, result.targetStatus, answer)
            assertEquals(expected.linguistic, result.linguisticStatus, answer)
            assertEquals(expected.constraint, result.responseConstraintStatus, answer)
            assertEquals(expected.issue, result.issues.singleOrNull()?.code, answer)
            assertEquals(expected.correction, result.completedSentence, answer)
            assertEquals(Answer.Text(answer), result.rawAnswer, answer)
            assertNull(result.meaningSatisfaction, answer)
        }
        val mismatch = engine.evaluate(selection, Answer.Order(emptyList()))
        assertEquals("ANSWER_TYPE_MISMATCH", mismatch.issues.single().code)
        assertEquals(Answer.Order(emptyList()), mismatch.rawAnswer)
    }

    @Test
    fun `both possession selection cards vary the have and har positions by seed`() {
        listOf("possession-select-letter", "possession-select-newspaper").forEach { taskId ->
            val positions = (0..3).map { seed ->
                currentExercise("E022", taskId, seed).prompt.choices.indexOf("har")
            }.toSet()
            assertEquals(setOf(0, 1), positions, taskId)
        }
    }

    private fun possessionExercises() = listOf(
        currentExercise("E022", "possession-select-letter"),
        currentExercise("E022", "possession-select-newspaper"),
        currentExercise("E023", "possession-recall-book"),
        currentExercise("E023", "possession-recall-letter"),
    )

    private fun currentExercise(formatId: String, taskId: String, seed: Int = 47) =
        exercise(formatId, taskId, seed, "pilot-11", "generator-11", includePossession = true)

    private fun retainedExercise(formatId: String, taskId: String, seed: Int = 47) =
        exercise(formatId, taskId, seed, "pilot-10", "generator-10", includePossession = false)

    private fun exercise(
        formatId: String,
        taskId: String,
        seed: Int,
        contentRevision: String,
        generatorRevision: String,
        includePossession: Boolean,
    ): GeneratedExercise = (0 until PresentOrderContent.tasks(formatId, includePossession).size).asSequence().map { index ->
        engine.generate(GenerationRequest(formatId, "G004", seed, index, contentRevision, generatorRevision))
    }.first { (it.source as PresentOrderExerciseSource).task.id == taskId }

    private data class Expected(
        val target: TargetStatus,
        val linguistic: LinguisticStatus,
        val constraint: ResponseConstraintStatus,
        val issue: String?,
        val correction: String?,
    )
}
