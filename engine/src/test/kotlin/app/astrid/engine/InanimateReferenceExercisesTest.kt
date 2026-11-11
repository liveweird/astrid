package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InanimateReferenceExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair nine adds two bounded formats while retaining pair eight literally`() {
        assertEquals(
            RevisionPair("pilot-8", "generator-8"),
            engine.supportedRevisionPairs()[7],
        )
        assertEquals(RevisionPair("pilot-9", "generator-9"), engine.supportedRevisionPairs()[8])
        assertEquals(
            RevisionPair("pilot-10", "generator-10"),
            engine.supportedRevisionPairs()[9],
        )
        assertEquals(RevisionPair("pilot-12", "generator-12"), engine.supportedRevisionPairs()[11])
        assertEquals(RevisionPair("pilot-13", "generator-13"), engine.supportedRevisionPairs()[12])
        assertEquals(RevisionPair("pilot-14", "generator-14"), engine.supportedRevisionPairs().last())
        assertEquals("evaluation-8", engine.evaluationRevision("pilot-8", "generator-8"))
        assertEquals("evaluation-9", engine.evaluationRevision("pilot-9", "generator-9"))
        assertEquals((1..28).map { "E${it.toString().padStart(3, '0')}" }, engine.catalog().formats.map { it.id })
        assertEquals(109, engine.catalog().sentenceCount)
        assertEquals(setOf("E020", "E021"), engine.catalog().formats.filter { it.targetSkillId == "G038" }.map { it.id }.toSet())
        assertEquals(
            listOf("G001", "G002", "G003", "G006"),
            engine.catalog().skills.single { it.id == "G038" }.prerequisiteIds,
        )
        assertFalse(PilotContent.humanPronouns.any { it.id == "L026" })
        assertEquals("det", PilotContent.identifyingPronoun.text)
    }

    @Test
    fun `finite cycles cover both senses and keep selection and recall contexts distinct`() {
        listOf("E020", "E021").forEach { formatId ->
            listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
                val exercises = (0..1).map { index -> generate(formatId, seed, index) }
                assertEquals(setOf("L026-S01", "L008-S02"), exercises.map { source(it).targetSenseId }.toSet())
                assertEquals(setOf("den", "det"), exercises.map { source(it).expectedForm }.toSet())
            }
        }
        val selection = (0..1).map { generate("E020", 47, it) }.associateBy { source(it).targetSenseId }
        val recall = (0..1).map { generate("E021", 47, it) }.associateBy { source(it).targetSenseId }
        selection.keys.forEach { sense ->
            assertFalse(selection.getValue(sense).context == recall.getValue(sense).context)
            assertFalse(source(selection.getValue(sense)).contextVariantId == source(recall.getValue(sense)).contextVariantId)
        }
    }

    @Test
    fun `selection bank order varies independently of the selected task`() {
        val positionsBySense = (0..7).flatMap { seed ->
            (0..1).map { index -> generate("E020", seed, index) }
        }.groupBy { source(it).targetSenseId }.mapValues { (_, exercises) ->
            exercises.map { it.prompt.choices.indexOf(source(it).expectedForm) }.toSet()
        }
        assertEquals(
            mapOf("L026-S01" to setOf(0, 1), "L008-S02" to setOf(0, 1)),
            positionsBySense,
        )
    }

    @Test
    fun `private source binds reference and antecedent identities without public sentence claims`() {
        val book = (0..1).map { generate("E020", 0, it) }.single { source(it).expectedForm == "den" }
        assertEquals("en bog", book.prompt.cue)
        assertEquals("da", book.prompt.cueLanguage)
        assertEquals("", book.prompt.before)
        assertEquals("", book.prompt.after)
        assertEquals("", book.prompt.punctuation)
        assertEquals(listOf("G001", "G002", "G003", "G006"), book.supportSkillIds)
        assertEquals("inanimate-singular-subject", book.targetScopeId)
        assertNull(book.inspector.canonicalSentence)
        assertNull(book.inspector.moduleOneSource)
        val details = source(book)
        assertEquals("L026", details.targetEntryId)
        assertEquals("L026-S01", details.targetSenseId)
        assertEquals("L014", details.antecedentEntryId)
        assertEquals("L014-S01", details.antecedentSenseId)
        assertEquals("en", details.antecedentArticle)
        assertEquals("common", details.antecedentGender)
        assertEquals("singular", details.antecedentNumber)
        assertEquals("subject", details.requestedRole)
        assertEquals("standalone-introduced-form-1", details.casePolicy)
        assertEquals(listOf("den", "det"), book.prompt.choices.toSet().sorted())
        assertTrue(details.choiceRationales.all { it.rationale.isNotBlank() })
        val privateSource = assertIs<InanimateReferenceExerciseSource>(book.source)
        assertEquals(details.taskId, privateSource.task.id)
        assertEquals(
            AnswerKey.ModuleOneForm("den", setOf("det"), selection = true),
            privateSource.answerKey,
        )
    }

    @Test
    fun `grading preserves raw input and implements literal reference boundaries`() {
        val denRecall = find("E021", "den")
        val detRecall = find("E021", "det")

        assertEvaluation(denRecall, "  den  ", TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
            .also { assertEquals(Answer.Text("  den  "), it.rawAnswer) }
        assertEvaluation(denRecall, "det", TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
            .also { assertEquals(listOf("INTRODUCED_REFERENCE_REQUIRED"), it.issues.map(EvaluationIssue::code)) }
        assertEvaluation(denRecall, "Den", TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET)
            .also { assertEquals("den", it.completedSentence) }
        assertEvaluation(detRecall, "Det", TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET)
        assertEvaluation(detRecall, "det er", TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET)
            .also { assertEquals(listOf("ONE_WORD_RESPONSE_REQUIRED"), it.issues.map(EvaluationIssue::code)) }
        assertEvaluation(detRecall, "dem", TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET)
            .also { assertNull(it.completedSentence) }
        assertEvaluation(detRecall, "", TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET)
            .also { assertNull(it.completedSentence) }
        val mismatch = engine.evaluate(detRecall, Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, mismatch.targetStatus)
        assertEquals("ANSWER_TYPE_MISMATCH", mismatch.issues.single().code)
        assertNull(mismatch.completedSentence)
    }

    @Test
    fun `choice validation accepts only the displayed bank`() {
        val selection = find("E020", "den")
        selection.prompt.choices.forEach { assertTrue(engine.isSuppliedChoice(selection, it)) }
        assertTrue(engine.isSuppliedChoice(selection, " den "))
        assertFalse(engine.isSuppliedChoice(selection, "dem"))
        assertFalse(engine.isSuppliedChoice(selection, "det er"))
    }

    private fun assertEvaluation(
        exercise: GeneratedExercise,
        text: String,
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus,
    ): Evaluation = engine.evaluate(exercise, Answer.Text(text)).also {
        assertEquals(target, it.targetStatus)
        assertEquals(linguistic, it.linguisticStatus)
        assertEquals(constraint, it.responseConstraintStatus)
        assertEquals(LinguisticScope.DANISH_WORD_FORM, it.linguisticScope)
    }

    private fun find(formatId: String, expected: String): GeneratedExercise =
        (0..1).map { generate(formatId, 0, it) }.single { source(it).expectedForm == expected }

    private fun generate(formatId: String, seed: Int, index: Int): GeneratedExercise =
        engine.generate(GenerationRequest(formatId, "G038", seed, index, "pilot-9", "generator-9"))

    private fun source(exercise: GeneratedExercise): InanimateReferenceSourceDetails =
        requireNotNull(exercise.inspector.inanimateReferenceSource)
}
