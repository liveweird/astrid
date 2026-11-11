package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CircumstanceExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair fifteen adds only G016 E029 and two lexical records over retained pair fourteen`() {
        val retained = PilotContent.pilotFourteenSnapshot
        val current = PilotContent.currentSnapshot
        assertEquals(109, retained.sentences.size)
        assertEquals(109, current.sentences.size)
        assertEquals(retained.formats.map { it.id } + "E029", current.formats.map { it.id })
        assertEquals(retained.skills.map { it.id } + "G016", current.skills.map { it.id })
        assertEquals(listOf("G005"), current.skills.single { it.id == "G016" }.prerequisiteIds)
        assertEquals(retained.fixedWords.map { it.id } + listOf("L037", "L038"), current.fixedWords.map { it.id })
        assertEquals("pilot-15", engine.catalog().contentRevision)
        assertEquals("generator-15", engine.catalog().generatorRevision)
        assertEquals(29, engine.catalog().formats.size)
        assertEquals("evaluation-14", engine.evaluationRevision("pilot-14", "generator-14"))
        assertEquals("evaluation-15", engine.evaluationRevision("pilot-15", "generator-15"))
    }

    @Test
    fun `pair fifteen publishes four literal C007 cards with intact constituents`() {
        val exercises = FinalCircumstanceContent.tasks.mapIndexed(::exercise)

        assertEquals(
            listOf(
                "Jeg arbejder i dag.",
                "Hun læser et brev nu.",
                "Vi arbejder i Aarhus.",
                "De læser en avis i dag.",
            ),
            exercises.map { it.inspector.canonicalSentence },
        )
        assertEquals(
            listOf(
                "s12-work-final-time",
                "s12-read-final-time",
                "s12-work-final-place",
                "s12-read-final-time",
            ),
            exercises.map { requireNotNull(it.inspector.tileSource).licensedOrders.single().kind },
        )
        assertTrue(exercises.all { it.inspector.constructionId == "C007" })
        assertTrue(exercises.all { it.prompt.punctuation == "." })
        assertTrue(exercises.all { it.prompt.tileMode == null && it.prompt.requiredTileCount == null })
        assertEquals(
            listOf(
                listOf("SUBJECT", "FINITE_VERB", "CIRCUMSTANCE"),
                listOf("SUBJECT", "FINITE_VERB", "OBJECT", "CIRCUMSTANCE"),
                listOf("SUBJECT", "FINITE_VERB", "CIRCUMSTANCE"),
                listOf("SUBJECT", "FINITE_VERB", "OBJECT", "CIRCUMSTANCE"),
            ),
            exercises.map { it.inspector.clauseRoles },
        )
        assertTrue(exercises.flatMap { it.prompt.blocks }.any { it.text == "i dag" })
        assertTrue(exercises.flatMap { it.prompt.blocks }.any { it.text == "i Aarhus" })
        assertTrue(
            exercises.flatMap { it.prompt.blocks }.none {
                it.text == "i" || it.text == "dag" || it.text == "Aarhus"
            },
        )
    }

    @Test
    fun `circumstance prompts supply the Danish core with separate English meanings`() {
        val exercises = FinalCircumstanceContent.tasks.indices.map(::exercise)
        assertEquals(
            listOf("Jeg arbejder.", "Hun læser et brev.", "Vi arbejder.", "De læser en avis."),
            exercises.map { it.prompt.cue },
        )
        assertTrue(exercises.all { it.prompt.cueLanguage == "da" })
        assertTrue(exercises.all { "means today" !in it.context && "nu" !in it.context && "i Aarhus" !in it.context })
        val introduced = FinalCircumstanceContent.introduction().filter { it.body in setOf("i dag", "nu", "i Aarhus") }
        assertEquals(3, introduced.size)
        assertTrue(introduced.all { it.bodyLanguage == "da" })
    }

    @Test
    fun `current location metadata includes C007 while retained pair fourteen stays literal`() {
        val current = PilotContent.currentSnapshot.lexicalReview.single { it.id == "L032" }
        val retained = PilotContent.pilotFourteenSnapshot.lexicalReview.single { it.id == "L032" }
        assertTrue("C007" in current.constraints)
        assertTrue("Aarhus" in current.constraints)
        assertEquals(
            "Fixed supplied preposition before Aarhus or Odense in the pair-twelve residence frame only.",
            retained.constraints,
        )
        val currentRow = PilotReview.renderMarkdown().lineSequence().single { it.startsWith("| L032 |") }
        assertTrue("C007" in currentRow)
    }

    @Test
    fun `reading metadata licenses both assessed C007 cards and the separate worked example`() {
        val current = PilotContent.currentSnapshot.lexicalReview.single { it.id == "L011" }
        val retained = PilotContent.pilotFourteenSnapshot.lexicalReview.single { it.id == "L011" }
        assertTrue("the two authored C007 reading propositions" in current.constraints)
        assertTrue("separate worked reading example" in current.constraints)
        assertFalse("C007" in retained.constraints)
    }

    @Test
    fun `circumstance subjects add C007 evidence over retained pair fourteen`() {
        val current = PilotContent.currentSnapshot.lexicalReview.single { it.id == "L001" }
        val retained = PilotContent.pilotFourteenSnapshot.lexicalReview.single { it.id == "L001" }
        assertTrue("C014 modal subjects" in current.constraints)
        assertTrue("C007 circumstance subjects" in current.constraints)
        assertTrue("C014 modal subjects" in retained.constraints)
        assertFalse("C007" in retained.constraints)
        assertTrue("sv00006990" in current.sourceUrls.single { "sv00006990" in it })
        val reviewRow = PilotReview.renderMarkdown().lineSequence().single { it.startsWith("| L001 |") }
        assertTrue("C007 circumstance subjects" in reviewRow)
    }

    @Test
    fun `all sixty complete permutations distinguish final placement from exact core order`() {
        var permutationCount = 0
        FinalCircumstanceContent.tasks.forEachIndexed { index, _ ->
            val exercise = exercise(index)
            val tokens = requireNotNull(exercise.inspector.tileSource).tokens.associateBy(TileTokenDetails::blockId)
            val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
            permutations(canonical).forEach { order ->
                permutationCount += 1
                val answer = Answer.Order(order)
                val evaluation = FinalCircumstanceExerciseEvaluator.evaluate(exercise, answer)
                assertSame(answer, evaluation.rawAnswer)
                assertEquals(NormalizedAnswer.Order(order), evaluation.normalizedAnswer)
                if (order == canonical) {
                    assertEquals(TargetStatus.MET, evaluation.targetStatus)
                    assertEquals(LinguisticStatus.ACCEPTABLE, evaluation.linguisticStatus)
                    assertEquals(ResponseConstraintStatus.MET, evaluation.responseConstraintStatus)
                    assertEquals(MeaningSatisfaction.MET, evaluation.meaningSatisfaction)
                    assertTrue(evaluation.issues.isEmpty())
                } else {
                    val circumstanceLast = tokens.getValue(order.last()).form == "circumstance"
                    assertEquals(
                        if (circumstanceLast) TargetStatus.MET else TargetStatus.NOT_MET,
                        evaluation.targetStatus,
                    )
                    assertEquals(LinguisticStatus.UNKNOWN, evaluation.linguisticStatus)
                    assertEquals(ResponseConstraintStatus.NOT_MET, evaluation.responseConstraintStatus)
                    assertEquals(MeaningSatisfaction.UNKNOWN, evaluation.meaningSatisfaction)
                    assertEquals(listOf("REQUESTED_ORDER_CONSTRAINT"), evaluation.issues.map(EvaluationIssue::code))
                }
            }
        }
        assertEquals(60, permutationCount)
    }

    @Test
    fun `incomplete and malformed answers remain safe and preserve raw input`() {
        val exercise = exercise(1)
        val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
        val cases = listOf(
            Answer.Order(canonical.dropLast(1)) to "REQUIRED_WORDS_MISSING",
            Answer.Order(emptyList()) to "ANSWER_TYPE_MISMATCH",
            Answer.Order(canonical.dropLast(1) + canonical.first()) to "ANSWER_TYPE_MISMATCH",
            Answer.Order(canonical.dropLast(1) + "foreign") to "ANSWER_TYPE_MISMATCH",
            Answer.Text("Hun læser et brev nu") to "ANSWER_TYPE_MISMATCH",
        )
        cases.forEach { (answer, issueCode) ->
            val result = FinalCircumstanceExerciseEvaluator.evaluate(exercise, answer)
            assertSame(answer, result.rawAnswer)
            assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
            assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
            assertEquals(MeaningSatisfaction.UNKNOWN, result.meaningSatisfaction)
            assertEquals(listOf(issueCode), result.issues.map(EvaluationIssue::code))
        }
    }

    @Test
    fun `private canonical key must match the fingerprinted licensed order`() {
        val exercise = exercise(1)
        val source = exercise.source as CircumstanceExerciseSource
        val key = source.answerKey as AnswerKey.TileOrder
        val licensed = requireNotNull(exercise.inspector.tileSource).licensedOrders.single()
        val conflictingPrivateSource = source.copy(
            answerKey = key.copy(canonical = key.canonical.reversed()),
        )
        val privateMismatch = assertFailsWith<IllegalArgumentException> {
            FinalCircumstanceExerciseEvaluator.evaluate(
                exercise.withPrivateState(source = conflictingPrivateSource),
                Answer.Order(licensed.blockIds),
            )
        }
        assertEquals(
            "Private circumstance answer key does not match the licensed inspector order",
            privateMismatch.message,
        )

        val conflictingInspector = exercise.inspector.copy(
            tileSource = requireNotNull(exercise.inspector.tileSource).copy(
                licensedOrders = listOf(licensed.copy(blockIds = licensed.blockIds.reversed())),
            ),
        )
        val inspectorMismatch = assertFailsWith<IllegalArgumentException> {
            FinalCircumstanceExerciseEvaluator.evaluate(
                exercise.withPrivateState(inspector = conflictingInspector),
                Answer.Order(licensed.blockIds),
            )
        }
        assertEquals(
            "Private circumstance answer key does not match the licensed inspector order",
            inspectorMismatch.message,
        )
    }

    @Test
    fun `seed and index select every authored task deterministically`() {
        (-3..3).forEach { seed ->
            val first = (0 until 4).map { index -> generated(seed, index) }
            val second = (0 until 4).map { index -> generated(seed, index) }
            assertEquals(first.map { it.id }, second.map { it.id })
            assertEquals(
                FinalCircumstanceContent.tasks.map(FinalCircumstanceTask::id).toSet(),
                first.map { (it.source as CircumstanceExerciseSource).task.id }.toSet(),
            )
        }
    }

    @Test
    fun `support vocabulary stays separate from structural and exact-order answers`() {
        FinalCircumstanceContent.tasks.forEach { task ->
            assertEquals(
                listOf(
                    FinalCircumstanceContent.FINAL_CIRCUMSTANCE_PATTERN_KEY,
                    FinalCircumstanceContent.exactOrderKey(task.id),
                ),
                task.answerExposureKeys,
            )
            assertTrue(task.promptExposureKeys.none(task.answerExposureKeys::contains))
            assertTrue(task.promptExposureKeys.any { it.startsWith("module-one:supplied-component:") })
            if (task.parts.any { "L037" in it.lexicalIds }) {
                assertTrue(FinalCircumstanceContent.TODAY_MEANING_KEY in task.promptExposureKeys)
            }
            if (task.parts.any { "L038" in it.lexicalIds }) {
                assertTrue(FinalCircumstanceContent.NOW_MEANING_KEY in task.promptExposureKeys)
            }
            assertEquals(
                listOf(
                    FinalCircumstanceContent.exactOrderKey(task.id),
                    FinalCircumstanceContent.FINAL_CIRCUMSTANCE_PATTERN_KEY,
                ),
                task.feedbackExposureKeys,
            )
        }
        assertEquals(
            listOf(
                FinalCircumstanceContent.FINAL_CIRCUMSTANCE_PATTERN_KEY,
                FinalCircumstanceContent.TODAY_MEANING_KEY,
                FinalCircumstanceContent.NOW_MEANING_KEY,
                FinalCircumstanceContent.componentKey("L032"),
                FinalCircumstanceContent.componentKey("L037"),
                FinalCircumstanceContent.componentKey("L038"),
            ),
            FinalCircumstanceContent.LESSON_EXPOSURE_KEYS,
        )
    }

    private fun exercise(index: Int, task: FinalCircumstanceTask = FinalCircumstanceContent.tasks[index]) =
        FinalCircumstanceExerciseGenerator.generate(request(47, index), task)

    private fun generated(seed: Int, index: Int) = FinalCircumstanceExerciseGenerator.generate(request(seed, index))

    private fun request(seed: Int, index: Int) =
        GenerationRequest("E029", "G016", seed, index, "pilot-15", "generator-15")

    private fun GeneratedExercise.withPrivateState(
        inspector: ExerciseInspector = this.inspector,
        source: ExerciseSource = this.source,
    ) = GeneratedExercise(
        id,
        formatId,
        targetSkillId,
        lexicalTarget,
        supportSkillIds,
        presentation,
        contentRevision,
        generatorRevision,
        reproduction,
        inspector,
        source,
        readingTarget,
        lexicalTargets,
    )

    private fun <T> permutations(values: List<T>): List<List<T>> =
        if (values.size <= 1) {
            listOf(values)
        } else {
            values.flatMap { value -> permutations(values - value).map { listOf(value) + it } }
        }
}
