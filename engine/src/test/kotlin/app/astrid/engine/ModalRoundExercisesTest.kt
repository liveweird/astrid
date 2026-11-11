package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModalRoundExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `modal subjects retain scoped C014 evidence while pair twelve stays literal`() {
        val current = PilotContent.currentSnapshot.lexicalReview.single { it.id == "L005" }
        val retained = PilotContent.pilotTwelveSnapshot.lexicalReview.single { it.id == "L005" }
        assertTrue("C014 modal subjects" in current.constraints)
        assertTrue("https://lex.dk/infinitiv" in current.sourceUrls)
        assertTrue("https://ordnet.dk/ddo/ordbog/11028611" in current.sourceUrls)
        assertFalse("C014" in retained.constraints)
        val reviewRow = PilotReview.renderMarkdown().lineSequence().single { it.startsWith("| L005 |") }
        assertTrue("C014 modal subjects" in reviewRow)
    }

    @Test
    fun `pair fourteen publishes the six exact cards with bounded modal sources`() {
        val interpretations = ModalRoundContent.definitions.map { interpretation(it) }
        val constructions = ModalRoundContent.definitions.map { construction(it) }

        assertEquals(
            listOf(
                "Hun vil læse et brev.",
                "De skal læse en bog.",
                "Du må læse et brev.",
            ),
            interpretations.map { it.prompt.cue },
        )
        assertEquals(
            listOf(
                "Vi vil læse en avis.",
                "Jeg skal læse en avis.",
                "Hun må læse en bog.",
            ),
            constructions.map { it.inspector.canonicalSentence },
        )
        assertEquals(
            listOf(
                "We intend to read a newspaper. Use vil.",
                "I am required to read a newspaper. Use skal.",
                "She is permitted to read a book. Use må.",
            ),
            constructions.map { it.prompt.cue },
        )
        assertEquals(
            listOf(
                "modal-volition-interpret-letter",
                "modal-obligation-interpret-book",
                "modal-permission-interpret-letter",
                "modal-volition-build-newspaper",
                "modal-obligation-build-newspaper",
                "modal-permission-build-book",
            ),
            (interpretations + constructions).map { requireNotNull(it.inspector.modalSource).taskId },
        )
        assertTrue((interpretations + constructions).all { it.inspector.constructionId == "C014" })
        assertTrue((interpretations + constructions).all {
            it.inspector.clauseRoles == listOf("SUBJECT", "FINITE_VERB", "NONFINITE_VERB", "OBJECT")
        })
        assertEquals(
            listOf(
                listOf("L004", "L034", "L011", "L023", "L018"),
                listOf("L007", "L035", "L011", "L022", "L014"),
                listOf("L002", "L036", "L011", "L023", "L018"),
                listOf("L005", "L034", "L011", "L022", "L015"),
                listOf("L001", "L035", "L011", "L022", "L015"),
                listOf("L004", "L036", "L011", "L022", "L014"),
            ),
            (interpretations + constructions).map { it.inspector.lexicalIds },
        )
        assertTrue(interpretations.all {
            it.formatId == "E028" && it.targetSkillId == "G030" && it.prompt.cueLanguage == "da" &&
                it.prompt.answerLanguage == "en" && it.linguisticChoiceSet() == ModalRoundContent.meaningChoices.toSet()
        })
        assertTrue(constructions.all {
            it.formatId == "E025" && it.targetSkillId == "G005" && it.targetScopeId == "supplied-modal-order" &&
                it.prompt.requiredTileCount == 5 && it.prompt.tileMode == "use-all" && "G030" !in it.supportSkillIds
        })
    }

    @Test
    fun `modal entries senses sources frames and contexts are exact and narrow`() {
        assertEquals(listOf("L034", "L035", "L036"), ModalRoundContent.definitions.map { it.modal.lexicalId })
        assertEquals(listOf("L034-S01", "L035-S01", "L036-S01"), ModalRoundContent.definitions.map { it.senseId })
        assertEquals(
            listOf("ville" to "vil", "skulle" to "skal", "måtte" to "må"),
            ModalRoundContent.definitions.map { it.modal.lemma to it.modal.present },
        )
        assertEquals(
            listOf(
                "https://ordnet.dk/ddo/ordbog/ville",
                "https://ordnet.dk/ddo/ordbog/skulle",
                "https://ordnet.dk/ddo/ordbog/m%C3%A5tte",
            ),
            ModalRoundContent.definitions.map { it.sourceUrl },
        )
        ModalRoundContent.definitions.forEach { definition ->
            val source = requireNotNull(interpretation(definition).inspector.modalSource)
            assertEquals(definition.modal.lexicalId, source.modalEntryId)
            assertEquals(definition.senseId, source.modalSenseId)
            assertEquals(definition.selectedMeaning, source.expectedInterpretation)
            assertEquals(definition.frameVariantId, source.frameVariantId)
            assertEquals(definition.interpretationContextVariantId, source.contextVariantId)
            val constructionSource = requireNotNull(construction(definition).inspector.modalSource)
            assertEquals(definition.constructionContextVariantId, constructionSource.contextVariantId)
        }
        val combinedTeaching = ModalRoundContent.definitions
            .flatMap(ModalRoundContent::introduction)
            .joinToString { it.body }
        listOf("prediction", "hearsay", "requests", "negation", "past forms", "ellipsis").forEach {
            assertTrue(it in combinedTeaching)
        }
    }

    @Test
    fun `each contextual meaning occupies all three supplied positions`() {
        ModalRoundContent.definitions.forEach { definition ->
            val positions = (0..8).map { seed ->
                interpretation(definition, seed).prompt.choices.indexOf(definition.selectedMeaning)
            }
            assertEquals(setOf(0, 1, 2), positions.toSet(), definition.key)
            (0..8).forEach { seed ->
                val choices = interpretation(definition, seed).prompt.choices
                assertEquals(3, choices.size)
                assertEquals(ModalRoundContent.meaningChoices.toSet(), choices.toSet())
            }
        }
    }

    @Test
    fun `standalone fixed index interpretations reach all positions across seeds`() {
        val positions = (0..16).map { seed ->
            val exercise = ModalRoundExerciseGenerator.generateInterpretation(
                GenerationRequest("E028", "G030", seed, 0, "pilot-14", "generator-14"),
            )
            val source = exercise.source as ModalInterpretationExerciseSource
            exercise.prompt.choices.indexOf(source.task.definition.selectedMeaning)
        }
        assertEquals(setOf(0, 1, 2), positions.toSet())
    }

    @Test
    fun `interpretation grades only exact supplied English choices and preserves raw input`() {
        ModalRoundContent.definitions.forEach { definition ->
            val card = interpretation(definition)
            val correctChoice = engine.evaluate(card, Answer.Text(definition.selectedMeaning))
            assertEquals(TargetStatus.MET, correctChoice.targetStatus, definition.key)
            assertEquals(MeaningSatisfaction.MET, correctChoice.meaningSatisfaction, definition.key)
            assertEquals(LinguisticStatus.ACCEPTABLE, correctChoice.linguisticStatus, definition.key)
            assertEquals(ResponseConstraintStatus.MET, correctChoice.responseConstraintStatus, definition.key)

            val otherChoice = ModalRoundContent.meaningChoices.first { it != definition.selectedMeaning }
            val incorrectChoice = engine.evaluate(card, Answer.Text(otherChoice))
            assertEquals(TargetStatus.NOT_MET, incorrectChoice.targetStatus, definition.key)
            assertEquals(MeaningSatisfaction.NOT_MET, incorrectChoice.meaningSatisfaction, definition.key)
            assertEquals(LinguisticStatus.ACCEPTABLE, incorrectChoice.linguisticStatus, definition.key)
            assertEquals(ResponseConstraintStatus.MET, incorrectChoice.responseConstraintStatus, definition.key)
            assertEquals("CONTEXTUAL_MODAL_MEANING_REQUIRED", incorrectChoice.issues.single().code, definition.key)
        }

        val exercise = interpretation(ModalRoundContent.definitions.single { it.key == "permission" })
        val correctRaw = Answer.Text("  Permission   to do it.  ")
        val correct = engine.evaluate(exercise, correctRaw)
        assertEquals(TargetStatus.MET, correct.targetStatus)
        assertEquals(MeaningSatisfaction.MET, correct.meaningSatisfaction)
        assertEquals(LinguisticStatus.ACCEPTABLE, correct.linguisticStatus)
        assertEquals(ResponseConstraintStatus.MET, correct.responseConstraintStatus)
        assertEquals(LinguisticScope.ENGLISH_CHOICE, correct.linguisticScope)
        assertEquals(correctRaw, correct.rawAnswer)
        assertEquals(NormalizedAnswer.Text("Permission to do it."), correct.normalizedAnswer)

        val wrong = engine.evaluate(exercise, Answer.Text("An external requirement."))
        assertEquals(TargetStatus.NOT_MET, wrong.targetStatus)
        assertEquals(MeaningSatisfaction.NOT_MET, wrong.meaningSatisfaction)
        assertEquals(LinguisticStatus.ACCEPTABLE, wrong.linguisticStatus)
        assertEquals(ResponseConstraintStatus.MET, wrong.responseConstraintStatus)
        assertEquals("CONTEXTUAL_MODAL_MEANING_REQUIRED", wrong.issues.single().code)
        assertTrue("permission to do it" in wrong.feedback.lowercase())

        listOf("permission to do it.", "Permission", "", "It means permission.").forEach { unsupported ->
            val result = engine.evaluate(exercise, Answer.Text(unsupported))
            assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus, unsupported)
            assertEquals(MeaningSatisfaction.UNKNOWN, result.meaningSatisfaction, unsupported)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus, unsupported)
            assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus, unsupported)
            assertEquals("UNSUPPLIED_MEANING_CHOICE", result.issues.single().code, unsupported)
            assertNull(result.completedSentence, unsupported)
        }
        val wrongKind = engine.evaluate(exercise, Answer.Order(emptyList()))
        assertEquals("ANSWER_TYPE_MISMATCH", wrongKind.issues.single().code)
        assertEquals(Answer.Order(emptyList()), wrongKind.rawAnswer)
    }

    @Test
    fun `construction distinguishes target order from noun phrase response constraint`() {
        val exercise = construction(ModalRoundContent.definitions.single { it.key == "volition" })
        val canonical = canonicalIds(exercise)
        val correct = engine.evaluate(exercise, Answer.Order(canonical))
        assertEquals(TargetStatus.MET, correct.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, correct.linguisticStatus)
        assertEquals(ResponseConstraintStatus.MET, correct.responseConstraintStatus)
        assertEquals(MeaningSatisfaction.MET, correct.meaningSatisfaction)

        val nounBeforeArticle = idsByForms(
            exercise,
            "subject", "finite-modal", "bare-infinitive", "singular-headword", "article",
        )
        val nounReversal = engine.evaluate(exercise, Answer.Order(nounBeforeArticle))
        assertEquals(TargetStatus.MET, nounReversal.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, nounReversal.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, nounReversal.responseConstraintStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, nounReversal.meaningSatisfaction)
        assertEquals("REQUESTED_ORDER_CONSTRAINT", nounReversal.issues.single().code)

        val infinitiveBeforeModal = idsByForms(
            exercise,
            "subject", "bare-infinitive", "finite-modal", "article", "singular-headword",
        )
        val modalReversal = engine.evaluate(exercise, Answer.Order(infinitiveBeforeModal))
        assertEquals(TargetStatus.NOT_MET, modalReversal.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, modalReversal.linguisticStatus)
        assertEquals("REQUESTED_ORDER_CONSTRAINT", modalReversal.issues.single().code)
    }

    @Test
    fun `construction rejects missing duplicate foreign and wrong-kind responses without broad language claims`() {
        val exercise = construction(ModalRoundContent.definitions.single { it.key == "obligation" })
        val canonical = canonicalIds(exercise)
        val missing = engine.evaluate(exercise, Answer.Order(canonical.dropLast(1)))
        assertEquals(TargetStatus.UNEVALUABLE, missing.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, missing.linguisticStatus)
        assertEquals("REQUIRED_WORDS_MISSING", missing.issues.single().code)
        assertEquals(ResponseConstraintStatus.NOT_MET, missing.responseConstraintStatus)

        listOf(
            Answer.Order(listOf(canonical[0], canonical[0])),
            Answer.Order(canonical.dropLast(1) + "foreign"),
            Answer.Order(emptyList()),
        ).forEach { malformed ->
            val result = engine.evaluate(exercise, malformed)
            assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
            assertEquals("ANSWER_TYPE_MISMATCH", result.issues.single().code)
        }
        val wrongKind = engine.evaluate(exercise, Answer.Text("Jeg skal læse en avis"))
        assertEquals("ANSWER_TYPE_MISMATCH", wrongKind.issues.single().code)
        assertFalse(wrongKind.feedback.contains("ungrammatical", ignoreCase = true))
    }

    private fun interpretation(definition: ModalRoundDefinition, seed: Int = 47): GeneratedExercise =
        (0 until 3).asSequence().map { index ->
            engine.generate(GenerationRequest("E028", "G030", seed, index, "pilot-14", "generator-14"))
        }.first { (it.source as ModalInterpretationExerciseSource).task.definition.key == definition.key }

    private fun construction(definition: ModalRoundDefinition, seed: Int = 47): GeneratedExercise =
        (0 until 6).asSequence().map { index ->
            engine.generate(GenerationRequest("E025", "G005", seed, index, "pilot-14", "generator-14"))
        }.first {
            val task = (it.source as PresentOrderExerciseSource).task
            task is ModalConstructionTask && task.definition.key == definition.key
        }

    private fun canonicalIds(exercise: GeneratedExercise): List<String> =
        requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds

    private fun idsByForms(exercise: GeneratedExercise, vararg forms: String): List<String> {
        val tokens = requireNotNull(exercise.inspector.tileSource).tokens
        return forms.map { form -> tokens.single { it.form == form }.blockId }
    }

    private fun GeneratedExercise.linguisticChoiceSet() = prompt.choices.toSet()
}
