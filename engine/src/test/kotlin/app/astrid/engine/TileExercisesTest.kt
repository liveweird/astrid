package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TileExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `revision five catalog appends exact word choice and tile metadata`() {
        val catalog = engine.catalog()

        assertEquals("pilot-10", catalog.contentRevision)
        assertEquals("generator-10", catalog.generatorRevision)
        assertEquals("evaluation-5", engine.evaluationRevision("pilot-5", "generator-5"))
        assertEquals("Choose the Danish word", catalog.formats.single { it.id == "E011" }.title)
        assertEquals(LexicalTask("meaning", "en-da"), catalog.formats.single { it.id == "E011" }.lexicalTask)
        assertEquals("Build a Danish sentence", catalog.formats.single { it.id == "E012" }.title)
        assertEquals("G005", catalog.formats.single { it.id == "E012" }.targetSkillId)
        assertEquals("Choose the present form with word tiles", catalog.formats.single { it.id == "E013" }.title)
        assertEquals("G004", catalog.formats.single { it.id == "E013" }.targetSkillId)
        assertFailsPairFour("E011", null)
        assertFailsPairFour("E012", "G005")
        assertFailsPairFour("E013", "G004")
    }

    @Test
    fun `word choice cycles four distinct fair senses and keeps the new sense out of older pools`() {
        val cycle = (0 until 4).map { engine.generate(request("E011", null, index = it)) }
        val expectedChoices = setOf("en bog", "en avis", "et brev", "et magasin")

        assertEquals(setOf("L014-S01", "L015-S01", "L018-S01", "L024-S01"), cycle.map {
            requireNotNull(it.lexicalTarget).senseId
        }.toSet())
        assertTrue(cycle.all { it.prompt.choices.toSet() == expectedChoices && it.prompt.choices.size == 4 })
        assertTrue(cycle.all { it.prompt.cueLanguage == "en" && it.prompt.answerLanguage == "da" })
        assertEquals(setOf(0, 1, 2, 3), (0 until 32).map { index ->
            val exercise = engine.generate(request("E011", null, index = index))
            val expected = (exercise.source.answerKey as AnswerKey.SuppliedChoice).expected
            exercise.prompt.choices.indexOf(expected)
        }.toSet())
        assertEquals(
            setOf("book", "newspaper", "letter (written message)", "magazine (printed publication)"),
            cycle.map { it.prompt.cue }.toSet(),
        )
        assertTrue(cycle.all { exercise ->
            exercise.introduction.map { it.body }.toSet() == expectedChoices &&
                exercise.introduction.all { it.bodyLanguage == "da" }
        })
        listOf("E007" to null, "E008" to null, "E009" to "G005", "E010" to null).forEach { (format, target) ->
            val limits = if (format == "E009") 99 else if (format == "E010") 24 else 3
            assertTrue((0 until limits).none { index ->
                val exercise = engine.generate(GenerationRequest(format, target, 0, index, "pilot-5", "generator-5"))
                "L024" in exercise.inspector.lexicalIds || exercise.prompt.choices.any { "magasin" in it }
            })
        }
    }

    @Test
    fun `supplied choice validation is whitespace tolerant NFC normalized and case sensitive`() {
        val exercise = (0 until 4).map { engine.generate(request("E011", null, index = it)) }
            .single { it.prompt.cue == "magazine (printed publication)" }

        assertTrue(engine.isSuppliedChoice(exercise, "  et   magasin  "))
        assertFalse(engine.isSuppliedChoice(exercise, "Et magasin"))
        assertFalse(engine.isSuppliedChoice(exercise, "e\u0301t magasin"))
        assertFalse(engine.isSuppliedChoice(engine.generate(request("E012", "G005")), "et magasin"))

        val raw = Answer.Text("  et   magasin  ")
        val correct = engine.evaluate(exercise, raw)
        assertEquals(raw, correct.rawAnswer)
        assertEquals(NormalizedAnswer.Text("et magasin"), correct.normalizedAnswer)
        assertFacets(correct, TargetStatus.MET, MeaningSatisfaction.MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE)

        val other = engine.evaluate(exercise, Answer.Text("en avis"))
        assertFacets(other, TargetStatus.NOT_MET, MeaningSatisfaction.NOT_MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE)
        val unknown = engine.evaluate(exercise, Answer.Text("ET MAGASIN"))
        assertFacets(
            unknown,
            TargetStatus.UNEVALUABLE,
            MeaningSatisfaction.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
        )
        assertEquals(LinguisticScope.DANISH_CHOICE, correct.linguisticScope)
    }

    @Test
    fun `all 99 tile propositions retain the writing permutation and private token identities`() {
        listOf("E012" to "G005", "E013" to "G004").forEach { (format, target) ->
            val cycle = (0 until 99).map { engine.generate(request(format, target, index = it)) }
            assertEquals(99, cycle.map { it.inspector.canonicalSentenceId }.toSet().size)
            assertEquals(PilotContent.sentences.map { it.id }.toSet(), cycle.map { it.inspector.canonicalSentenceId }.toSet())
            cycle.forEach { exercise ->
                val source = assertNotNull(exercise.inspector.tileSource)
                assertEquals(exercise.prompt.blocks.indices.map { "b$it" }, exercise.prompt.blocks.map { it.id })
                assertEquals(exercise.prompt.blocks.map { it.id }, source.tokens.map { it.blockId })
                assertEquals(exercise.prompt.blocks.size, exercise.prompt.blocks.map { it.id }.toSet().size)
                assertTrue(exercise.prompt.blocks.none { it.id.contains("subject", ignoreCase = true) })
                assertTrue(source.tokens.all { it.form in TILE_FORMS })
                assertTrue(source.licensedOrders.all { it.kind in setOf("subject-first", "fronted") })
                assertTrue(exercise.introduction.all { it.bodyLanguage == "da" })
                assertFalse(exercise.inspector.canonicalSentence.orEmpty() in exercise.introduction.joinToString(" ") { it.body })
            }
        }

        val writing = engine.generate(GenerationRequest("E009", "G005", 47, 5, "pilot-4", "generator-4"))
        val allWords = engine.generate(request("E012", "G005", seed = 47, index = 5))
        val spare = engine.generate(request("E013", "G004", seed = 47, index = 5))
        assertEquals("Hun læser et brev.", writing.inspector.canonicalSentence)
        assertEquals(writing.inspector.canonicalSentenceId, allWords.inspector.canonicalSentenceId)
        assertEquals(writing.inspector.canonicalSentenceId, spare.inspector.canonicalSentenceId)
        assertEquals("use-all", allWords.prompt.tileMode)
        assertEquals(4, allWords.prompt.requiredTileCount)
        assertEquals("with-spares", spare.prompt.tileMode)
        assertEquals(4, spare.prompt.requiredTileCount)
        assertEquals(5, spare.prompt.blocks.size)
        assertEquals(1, assertNotNull(spare.inspector.tileSource).distractors.size)
    }

    @Test
    fun `all-word tiles grade canonical fronted incomplete and unknown orders independently`() {
        val exercise = engine.generate(request("E012", "G005", seed = 47, index = 5))
        val source = assertNotNull(exercise.inspector.tileSource)
        val canonical = source.licensedOrders.single { it.kind == "subject-first" }.blockIds
        val fronted = source.licensedOrders.single { it.kind == "fronted" }.blockIds

        assertFacets(
            engine.evaluate(exercise, Answer.Order(canonical)),
            TargetStatus.MET,
            MeaningSatisfaction.MET,
            ResponseConstraintStatus.MET,
            LinguisticStatus.ACCEPTABLE,
        )
        assertFacets(
            engine.evaluate(exercise, Answer.Order(fronted)),
            TargetStatus.NOT_MET,
            MeaningSatisfaction.MET,
            ResponseConstraintStatus.MET,
            LinguisticStatus.ACCEPTABLE,
        )
        val articleId = source.tokens.single { it.form == "article" }.blockId
        assertUnknownConstraint(engine.evaluate(exercise, Answer.Order(canonical - articleId)))
        val unknownOrder = canonical.toMutableList().also { java.util.Collections.swap(it, 0, 1) }
        val unknown = engine.evaluate(exercise, Answer.Order(unknownOrder))
        assertFacets(
            unknown,
            TargetStatus.UNEVALUABLE,
            MeaningSatisfaction.UNKNOWN,
            ResponseConstraintStatus.MET,
            LinguisticStatus.UNKNOWN,
        )
    }

    @Test
    fun `spare tiles distinguish finite infinitive fronting and overlong selections`() {
        val exercise = engine.generate(request("E013", "G004", seed = 47, index = 5))
        val key = exercise.source.answerKey as AnswerKey.TileOrder
        val source = assertNotNull(exercise.inspector.tileSource)
        val canonical = idsFor(key.canonical, source.tokens)
        val infinitive = idsFor(requireNotNull(key.infinitive), source.tokens)
        val fronted = idsFor(requireNotNull(key.fronted), source.tokens)

        assertFacets(
            engine.evaluate(exercise, Answer.Order(canonical)),
            TargetStatus.MET,
            MeaningSatisfaction.MET,
            ResponseConstraintStatus.MET,
            LinguisticStatus.ACCEPTABLE,
        )
        assertFacets(
            engine.evaluate(exercise, Answer.Order(infinitive)),
            TargetStatus.NOT_MET,
            MeaningSatisfaction.MET,
            ResponseConstraintStatus.MET,
            LinguisticStatus.KNOWN_ISSUE,
        )
        assertFacets(
            engine.evaluate(exercise, Answer.Order(fronted)),
            TargetStatus.MET,
            MeaningSatisfaction.MET,
            ResponseConstraintStatus.NOT_MET,
            LinguisticStatus.ACCEPTABLE,
        )
        assertUnknownConstraint(engine.evaluate(exercise, Answer.Order(exercise.prompt.blocks.map { it.id })))
    }

    @Test
    fun `tile grading rejects malformed identities and accepts exchanged equivalent occurrences`() {
        val exercise = engine.generate(request("E012", "G005", seed = 47, index = 5))
        val canonical = assertNotNull(exercise.inspector.tileSource).licensedOrders.first().blockIds
        assertUnknownConstraint(engine.evaluate(exercise, Answer.Order(canonical.dropLast(1) + canonical.first())))
        assertUnknownConstraint(engine.evaluate(exercise, Answer.Order(canonical.dropLast(1) + "unknown")))
        val typeMismatch = engine.evaluate(exercise, Answer.Text("Hun læser et brev."))
        assertUnknownConstraint(typeMismatch)

        val repeated = repeatedOccurrenceExercise()
        val first = engine.evaluate(repeated, Answer.Order(listOf("b0", "b1")))
        val exchanged = engine.evaluate(repeated, Answer.Order(listOf("b1", "b0")))
        assertEquals(TargetStatus.MET, first.targetStatus)
        assertEquals(TargetStatus.MET, exchanged.targetStatus)
        assertUnknownConstraint(engine.evaluate(repeated, Answer.Order(listOf("b0", "b0"))))
    }

    @Test
    fun `tile generation is deterministic across seeds repetition and maximum boundaries`() {
        listOf(0, 1, Int.MAX_VALUE).forEach { seed ->
            listOf(0, 98, 99, 1_000_000).forEach { index ->
                listOf("E012" to "G005", "E013" to "G004").forEach { (format, target) ->
                    val first = engine.generate(request(format, target, seed, index))
                    val repeated = engine.generate(request(format, target, seed, index))
                    assertEquals(first.id, repeated.id)
                    assertEquals(first.prompt, repeated.prompt)
                    assertEquals(first.inspector, repeated.inspector)
                }
            }
        }
        val cycleStart = engine.generate(request("E012", "G005", seed = 47, index = 0))
        val repeated = engine.generate(request("E012", "G005", seed = 47, index = 99))
        assertEquals(cycleStart.inspector.canonicalSentenceId, repeated.inspector.canonicalSentenceId)
    }

    @Test
    fun `word choice generation is deterministic across cycles and boundaries`() {
        listOf(0, 1, Int.MAX_VALUE).forEach { seed ->
            listOf(0, 3, 4, 1_000_000).forEach { index ->
                val first = engine.generate(request("E011", null, seed, index))
                val repeated = engine.generate(request("E011", null, seed, index))
                assertEquals(first.id, repeated.id)
                assertEquals(first.prompt, repeated.prompt)
                assertEquals(first.inspector, repeated.inspector)
            }
        }
        val cycleStart = engine.generate(request("E011", null, seed = 47, index = 0))
        val repeated = engine.generate(request("E011", null, seed = 47, index = 4))
        assertEquals(cycleStart.lexicalTarget, repeated.lexicalTarget)
    }

    @Test
    fun `revision five review exposes all new choice and tile teaching units while retaining four`() {
        val retained = PilotReview.renderPilotFourMarkdown()
        val current = PilotReview.renderPilotFiveMarkdown()

        assertTrue("Content revision: `pilot-4`" in retained)
        assertFalse("Danish word-choice teaching units" in retained)
        assertTrue("Provenance: docs/knowledge/danish-vocabulary-seed.md at baseline 3b462a9" in retained)
        assertTrue("Source review: 2026-11-07; see docs/knowledge/pilot-source-review.md" in retained)
        assertTrue("Content revision: `pilot-5`" in current)
        assertTrue("24 lexical records; 99 sentences" in current)
        assertTrue("L024 authoring packet, 2026-11-08" in current)
        assertTrue("2026-11-08 pair-five extension" in current)
        assertTrue("https://ordnet.dk/ddo/ordbog/magasin" in current)
        assertEquals(4, numberedRows(current.substringAfter("## Danish word-choice teaching units").substringBefore("## All-word")))
        assertEquals(99, numberedRows(current.substringAfter("## All-word sentence tiles").substringBefore("## Finite-form")))
        assertEquals(99, numberedRows(current.substringAfter("## Finite-form sentence tiles")))
    }

    private fun request(format: String, target: String?, seed: Int = 47, index: Int = 0) =
        GenerationRequest(format, target, seed, index, "pilot-5", "generator-5")

    private fun assertFailsPairFour(format: String, target: String?) {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            engine.generate(GenerationRequest(format, target, 0, 0, "pilot-4", "generator-4"))
        }
    }

    private fun idsFor(identities: List<TileIdentity>, tokens: List<TileTokenDetails>): List<String> {
        val remaining = tokens.toMutableList()
        return identities.map { identity ->
            val index = remaining.indexOfFirst { it.lexicalId == identity.lexicalId && it.form == identity.form }
            assertTrue(index >= 0)
            remaining.removeAt(index).blockId
        }
    }

    private fun assertFacets(
        evaluation: Evaluation,
        target: TargetStatus,
        meaning: MeaningSatisfaction,
        constraint: ResponseConstraintStatus,
        language: LinguisticStatus,
    ) {
        assertEquals(target, evaluation.targetStatus)
        assertEquals(meaning, evaluation.meaningSatisfaction)
        assertEquals(constraint, evaluation.responseConstraintStatus)
        assertEquals(language, evaluation.linguisticStatus)
    }

    private fun assertUnknownConstraint(evaluation: Evaluation) {
        assertFacets(
            evaluation,
            TargetStatus.UNEVALUABLE,
            MeaningSatisfaction.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
        )
    }

    private fun repeatedOccurrenceExercise(): GeneratedExercise {
        val tokens = listOf(
            TileTokenDetails("b0", "L999", "subject"),
            TileTokenDetails("b1", "L999", "subject"),
        )
        val identity = TileIdentity("L999", "subject")
        return GeneratedExercise(
            id = "repeated-occurrence",
            formatId = "E012",
            targetSkillId = "G005",
            lexicalTarget = null,
            supportSkillIds = emptyList(),
            presentation = ExercisePresentation(
                "Test repeated occurrences",
                "Test only",
                ExercisePrompt(
                    "",
                    "",
                    blocks = listOf(OrderBlock("b0", "i"), OrderBlock("b1", "i")),
                    tileMode = "use-all",
                    requiredTileCount = 2,
                ),
                emptyList(),
                "Test only",
            ),
            contentRevision = "pilot-5",
            generatorRevision = "generator-5",
            reproduction = Reproduction(0, 0),
            inspector = ExerciseInspector(
                lexicalIds = listOf("L999", "L999"),
                clauseRoles = listOf("subject", "subject"),
                tileSource = TileSourceDetails(tokens, listOf(TileLicensedOrderDetails("subject-first", listOf("b0", "b1"))), emptyList()),
            ),
            source = TileExerciseSource(null, AnswerKey.TileOrder(listOf(identity, identity), null, null)),
        )
    }

    private fun numberedRows(markdown: String): Int =
        markdown.lineSequence().count { it.matches(Regex("\\| \\d+ \\|.*")) }

    private companion object {
        val TILE_FORMS = setOf("subject", "finite-present", "infinitive", "article", "singular-headword")
    }
}
