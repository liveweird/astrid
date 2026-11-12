package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbilityExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair thirteen publishes ability skill formats while pair twelve remains literal`() {
        val catalog = engine.catalog()
        assertEquals("pilot-18", catalog.contentRevision)
        assertEquals("generator-18", catalog.generatorRevision)
        assertTrue(catalog.skills.any { it.id == "G030" })
        assertEquals(setOf("E026", "E027"), catalog.formats.map { it.id }.filter { it in setOf("E026", "E027") }.toSet())
        assertEquals(RevisionPair("pilot-12", "generator-12"), engine.supportedRevisionPairs()[11])
        assertEquals("evaluation-12", engine.evaluationRevision("pilot-12", "generator-12"))
        assertEquals("evaluation-13", engine.evaluationRevision("pilot-13", "generator-13"))
        val failure = runCatching {
            engine.generate(GenerationRequest("E026", "G030", 0, 0, "pilot-12", "generator-12"))
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `language review licenses the current reading frame while retained pair twelve stays literal`() {
        val current = PilotContent.currentSnapshot.lexicalReview.single { it.id == "L031" }
        val retained = PilotContent.pilotTwelveSnapshot.lexicalReview.single { it.id == "L031" }
        assertTrue("C014 written-language reading frame" in current.constraints)
        assertEquals(
            "Fixed supplied language complement in the pair-twelve tale frame; adjective and nationality uses excluded.",
            retained.constraints,
        )
    }

    @Test
    fun `ability subjects gain scoped C014 evidence while pair twelve stays literal`() {
        val review = PilotReview.renderMarkdown()
        for (id in listOf("L001", "L002", "L003", "L004", "L005", "L007")) {
            val current = PilotContent.currentSnapshot.lexicalReview.single { it.id == id }
            val retained = PilotContent.pilotTwelveSnapshot.lexicalReview.single { it.id == id }
            assertTrue("C014 modal subjects" in current.constraints, id)
            assertTrue("https://lex.dk/infinitiv" in current.sourceUrls, id)
            assertTrue("https://ordnet.dk/ddo/ordbog/11028611" in current.sourceUrls, id)
            assertFalse("C014" in retained.constraints, id)
            val reviewRow = review.lineSequence().single { it.startsWith("| $id |") }
            assertTrue("C014 modal subjects" in reviewRow, id)
        }
    }

    @Test
    fun `pair thirteen publishes the four literal ability frames under two new formats`() {
        val exercises = TASK_IDS.map(::exercise)
        assertEquals(listOf("E026", "E026", "E027", "E027"), exercises.map { it.formatId })
        assertEquals(
            listOf(
                "Hun kan læse et brev.",
                "Vi kan læse dansk.",
                "Du kan læse et brev.",
                "De kan læse dansk.",
            ),
            exercises.map { it.inspector.canonicalSentence },
        )
        assertEquals(
            listOf("Hun kan ", "Vi kan ", "Du kan ", "De kan "),
            exercises.map { it.prompt.before },
        )
        assertEquals(listOf(" et brev.", " dansk.", " et brev.", " dansk."), exercises.map { it.prompt.after })
        assertEquals(listOf(3, 3, 0, 0), exercises.map { it.prompt.choices.size })
        assertEquals(listOf(null, null, "Danish verb after kan", "Danish verb after kan"), exercises.map {
            it.prompt.inputLabel
        })
        assertTrue(exercises.all { it.prompt.cue == "at læse" && it.prompt.cueLanguage == "da" })
        assertTrue(exercises.all { it.targetSkillId == "G030" && it.targetScopeId == "kan-bare-infinitive" })
        assertTrue(exercises.all { it.inspector.constructionId == "C014" })
        assertEquals(
            listOf("OBJECT", "LANGUAGE_COMPLEMENT", "OBJECT", "LANGUAGE_COMPLEMENT"),
            exercises.map { it.inspector.clauseRoles.last() },
        )
        assertTrue(exercises.all {
            it.inspector.clauseRoles.take(3) == listOf("SUBJECT", "FINITE_VERB", "NONFINITE_VERB")
        })
        assertEquals(
            listOf(
                listOf("L004", "L033", "L011", "L023", "L018"),
                listOf("L005", "L033", "L011", "L031"),
                listOf("L002", "L033", "L011", "L023", "L018"),
                listOf("L007", "L033", "L011", "L031"),
            ),
            exercises.map { it.inspector.lexicalIds },
        )
        exercises.forEach { exercise ->
            val source = requireNotNull(exercise.inspector.moduleOneSource)
            assertEquals("L011", source.targetLexicalId)
            assertEquals("læse", source.expectedForm)
            assertEquals(listOf("at læse", "læser"), source.knownAlternativeForms)
            assertEquals("bare-infinitive-after-kan", source.referenceRole)
            assertEquals(
                if ("letter" in requireNotNull(source.taskId)) "s10-kan-laese-letter" else "s10-kan-laese-danish",
                source.frameVariantId,
            )
        }
    }

    @Test
    fun `selection choices contain only the three exact forms and rotate through every position`() {
        listOf("ability-select-letter", "ability-select-danish").forEach { taskId ->
            val choices = (0..2).map { seed -> exercise(taskId, seed).prompt.choices }
            assertTrue(choices.all { it.toSet() == setOf("læse", "læser", "at læse") && it.size == 3 })
            listOf("læse", "læser", "at læse").forEach { form ->
                assertEquals(setOf(0, 1, 2), choices.map { it.indexOf(form) }.toSet(), "$taskId/$form")
            }
        }
    }

    @Test
    fun `at marked infinitive is diagnosed before generic multiword handling in both modes`() {
        val selection = exercise("ability-select-letter")
        val recall = exercise("ability-recall-letter")
        val selected = engine.evaluate(selection, Answer.Text("  at   læse  "))
        val typed = engine.evaluate(recall, Answer.Text("at læse"))

        assertEquals("INFINITIVE_MARKER_AFTER_MODAL", selected.issues.single().code)
        assertEquals(TargetStatus.NOT_MET, selected.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, selected.linguisticStatus)
        assertEquals(ResponseConstraintStatus.MET, selected.responseConstraintStatus)
        assertEquals(Answer.Text("  at   læse  "), selected.rawAnswer)
        assertEquals(NormalizedAnswer.Text("at læse"), selected.normalizedAnswer)
        assertTrue(engine.isSuppliedChoice(selection, "  at   læse  "))

        assertEquals("INFINITIVE_MARKER_AFTER_MODAL", typed.issues.single().code)
        assertEquals(TargetStatus.NOT_MET, typed.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, typed.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, typed.responseConstraintStatus)
        assertFalse(engine.isSuppliedChoice(recall, "at læse"))
    }

    @Test
    fun `ability evaluation preserves canonical known constraint and unknown boundaries`() {
        val recall = exercise("ability-recall-danish")
        val cases = listOf(
            "  læse  " to Expected(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, null, "læse"),
            "læser" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.MET,
                "FINITE_VERB_AFTER_MODAL", "læse",
            ),
            "Læse" to Expected(
                TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "MID_SENTENCE_VERB_CASE_REQUIRED", "læse",
            ),
            "læse nu" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "ONE_WORD_RESPONSE_REQUIRED", "læse",
            ),
            "læste" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_BARE_INFINITIVE", null,
            ),
            "læse." to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_BARE_INFINITIVE", null,
            ),
            "" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
                "EMPTY_RESPONSE", null,
            ),
        )
        cases.forEach { (answer, expected) ->
            val result = engine.evaluate(recall, Answer.Text(answer))
            assertEquals(expected.target, result.targetStatus, answer)
            assertEquals(expected.linguistic, result.linguisticStatus, answer)
            assertEquals(expected.constraint, result.responseConstraintStatus, answer)
            assertEquals(expected.issue, result.issues.singleOrNull()?.code, answer)
            assertEquals(expected.correction, result.completedSentence, answer)
            assertEquals(Answer.Text(answer), result.rawAnswer, answer)
            assertNull(result.meaningSatisfaction, answer)
        }

        val unsupplied = engine.evaluate(exercise("ability-select-danish"), Answer.Text("læste"))
        assertEquals("UNSUPPLIED_CHOICE", unsupplied.issues.single().code)
        assertEquals(ResponseConstraintStatus.NOT_MET, unsupplied.responseConstraintStatus)
        val wrongKind = engine.evaluate(recall, Answer.Order(emptyList()))
        assertEquals("ANSWER_TYPE_MISMATCH", wrongKind.issues.single().code)
        assertEquals(Answer.Order(emptyList()), wrongKind.rawAnswer)
    }

    private fun exercise(taskId: String, seed: Int = 47): GeneratedExercise {
        val formatId = if ("-select-" in taskId) "E026" else "E027"
        return (0 until 2).asSequence().map { index ->
            engine.generate(GenerationRequest(formatId, "G030", seed, index, "pilot-13", "generator-13"))
        }.first { (it.source as AbilityExerciseSource).task.id == taskId }
    }

    private data class Expected(
        val target: TargetStatus,
        val linguistic: LinguisticStatus,
        val constraint: ResponseConstraintStatus,
        val issue: String?,
        val correction: String?,
    )

    private companion object {
        val TASK_IDS = listOf(
            "ability-select-letter",
            "ability-select-danish",
            "ability-recall-letter",
            "ability-recall-danish",
        )
    }
}
