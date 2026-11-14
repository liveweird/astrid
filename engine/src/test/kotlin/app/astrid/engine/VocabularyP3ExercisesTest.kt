package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class VocabularyP3ExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair twenty publishes exact inventory formats tasks and target channels`() {
        assertEquals(RevisionPair("pilot-20", "generator-20"), engine.supportedRevisionPairs().last())
        assertEquals("evaluation-20", engine.evaluationRevision("pilot-20", "generator-20"))
        assertEquals(79, PilotContent.currentSnapshot.lexicalReview.size)
        assertEquals((64..79).map(::lexicalId).toSet(), VocabularyP3Content.newLexemes.map { it.entryId }.toSet())
        assertEquals(23, VocabularyP3Content.tasks.size)
        assertEquals(
            mapOf("E045" to 1, "E046" to 1, "E047" to 6, "E048" to 1, "E049" to 3, "E050" to 8, "E051" to 3),
            VocabularyP3Content.tasks.groupingBy(VocabularyP3Task::formatId).eachCount(),
        )
        assertEquals((45..51).map(::formatId), engine.catalog().formats.takeLast(7).map { it.id })
        assertEquals(mapOf("E048" to "G004", "E049" to "G005", "E051" to "G016"),
            VocabularyP3Content.formats.mapNotNull { it.targetSkillId?.let { target -> it.id to target } }.toMap())
    }

    @Test
    fun `P3 authored task guards reject malformed target shapes roles scenes and closed answers`() {
        val choice = VocabularyP3Content.tasks.single { it.id == "p3-1.1" }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(choice.copy(targets = emptyList())) }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(choice.copy(expected = "outside")) }
        val matching = VocabularyP3Content.tasks.single { it.id == "p3-1.2" }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(matching.copy(matching = matching.matching.take(2))) }
        assertFailsWith<IllegalArgumentException> {
            VocabularyP3Content.validate(matching.copy(matching = matching.matching + matching.matching.take(2)))
        }
        val recall = VocabularyP3Content.tasks.single { it.id == "p3-5.4" }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(recall.copy(formatId = "E045")) }
        val tiles = VocabularyP3Content.tasks.single { it.id == "p3-1.4" }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(tiles.copy(tiles = emptyList())) }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(tiles.copy(targetScopeId = "other")) }
        val reading = VocabularyP3Content.tasks.single { it.id == "p3-6.2" }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(reading.copy(questionRole = "chronology")) }
        val wrongScene = reading.copy(facts = reading.facts.map { fact ->
            if (fact.requiredSceneId == null) fact else fact.copy(requiredSceneId = "another-scene")
        })
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(wrongScene) }
        val contrast = VocabularyP3Content.tasks.single { it.id == "p3-3.4" }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(contrast.copy(contrastId = null)) }
        assertFailsWith<IllegalArgumentException> { VocabularyP3Content.validate(contrast.copy(expected = "outside")) }
    }

    @Test
    fun `all authored tasks generate at ordinary and maximum seeds with complete private source`() {
        listOf(0, 47, Int.MAX_VALUE).forEach { seed ->
            val generated = VocabularyP3Content.formats.flatMap { format ->
                val target = format.targetSkillId
                VocabularyP3Content.tasks(format.id).indices.map { index ->
                    engine.generate(GenerationRequest(format.id, target, seed, index, "pilot-20", "generator-20"))
                }
            }
            assertEquals(VocabularyP3Content.tasks.map { it.id }.toSet(),
                generated.map { requireNotNull(it.inspector.vocabularySource).taskId }.toSet())
            generated.forEach { exercise ->
                assertEquals("pilot-20", exercise.contentRevision)
                assertEquals("generator-20", exercise.generatorRevision)
                assertTrue(requireNotNull(exercise.inspector.vocabularySource).lexicalSources.all {
                    it.entryId.isNotBlank() && it.senseId.endsWith("-S01") && it.selectedUse.isNotBlank()
                })
            }
        }
    }

    @Test
    fun `all 23 prompts pin their literal format scope and language channels`() {
        data class Expected(val format: String, val scope: String?, val cue: String, val answer: String)
        val expectations = mapOf(
            "p3-1.1" to Expected("E045", null, "da", "en"),
            "p3-1.2" to Expected("E047", null, "en", "da"),
            "p3-1.3" to Expected("E050", null, "en", "en"),
            "p3-1.4" to Expected("E049", "p3-subject-first-supplied", "en", "da"),
            "p3-2.1" to Expected("E047", null, "en", "da"),
            "p3-2.2" to Expected("E050", null, "en", "en"),
            "p3-2.3" to Expected("E051", "p3-home-direction", "da", "en"),
            "p3-2.4" to Expected("E049", "p3-subject-first-supplied", "en", "da"),
            "p3-3.1" to Expected("E047", null, "en", "da"),
            "p3-3.2" to Expected("E050", null, "en", "en"),
            "p3-3.3" to Expected("E050", null, "en", "en"),
            "p3-3.4" to Expected("E051", "p3-home-spatial-contrast", "en", "en"),
            "p3-4.1" to Expected("E047", null, "en", "da"),
            "p3-4.2" to Expected("E050", null, "en", "en"),
            "p3-4.3" to Expected("E046", null, "en", "da"),
            "p3-5.1" to Expected("E047", null, "en", "da"),
            "p3-5.2" to Expected("E050", null, "en", "en"),
            "p3-5.3" to Expected("E051", "p3-station-spatial-contrast", "en", "en"),
            "p3-5.4" to Expected("E048", "p3-kore-present", "da", "da"),
            "p3-6.1" to Expected("E047", null, "en", "da"),
            "p3-6.2" to Expected("E050", null, "en", "en"),
            "p3-6.3" to Expected("E050", null, "en", "en"),
            "p3-6.4" to Expected("E049", "p3-subject-first-supplied", "en", "da"),
        )
        assertEquals(VocabularyP3Content.tasks.map(VocabularyP3Task::id).toSet(), expectations.keys)
        expectations.forEach { (taskId, expected) ->
            val exercise = task(taskId)
            assertEquals(expected.format, exercise.formatId, taskId)
            assertEquals(expected.scope, exercise.targetScopeId, taskId)
            assertEquals(expected.cue, exercise.prompt.cueLanguage, taskId)
            assertEquals(expected.answer, exercise.prompt.answerLanguage, taskId)
        }
    }

    @Test
    fun `reading facts preserve roles full sources labels and minimal assessment scenes`() {
        val vehicle = task("p3-4.2")
        assertEquals(listOf(PassageLabel(1, "Vehicle A"), PassageLabel(2, "Vehicle B")), vehicle.prompt.passageLabels)
        val rail = task("p3-6.2")
        assertEquals("Rail context: This journey is by train.", rail.context)
        assertFalse("changes" in rail.context.lowercase())
        assertFalse("aarhus" in rail.context.lowercase())
        val source = requireNotNull(rail.inspector.vocabularySource)
        assertEquals("p3-assessment-rail", source.scene?.id)
        val target = source.supportFacts.single { it.factId == "p3-s6-f1" }
        assertEquals("action", target.valueRole)
        assertEquals("changes trains/connections", target.actionText)
        assertEquals("L077", target.valueId)
        assertEquals("L032", target.complementSource?.entryId)
        assertEquals("static-location", target.complementRole)
        assertEquals("p3-rail-change", target.constructionId)
        assertEquals("p3-assessment-rail", target.sceneId)
        assertEquals("Anna changes trains/connections in Aarhus.", target.translation)
        assertTrue(source.supportFacts.single { it.factId == "p3-s6-f1" }.exposureKeys.any { ":spatial:" in it })
        assertTrue(task("p3-1.3").inspector.vocabularySource!!.supportFacts
            .filter { it.valueRole in setOf("subject", "object", "time") }.all { fact -> fact.exposureKeys.none { ":spatial:" in it } })
    }

    @Test
    fun `paired spatial choices expose structured mappings but grade one whole contrast`() {
        listOf("p3-3.4", "p3-5.3").forEach { id ->
            val exercise = task(id)
            val source = requireNotNull(exercise.inspector.vocabularySource)
            assertEquals(2, exercise.prompt.choiceMappings.size)
            assertEquals(exercise.prompt.choices.toSet(), exercise.prompt.choiceMappings.map { it.choice }.toSet())
            assertEquals(2, source.spatialMeanings.size)
            assertEquals(setOf("destination", "static-location"), source.spatialMeanings.map { it.classification }.toSet())
            val wrong = exercise.prompt.choices.single { it != source.expectedAnswer }
            val evaluation = engine.evaluate(exercise, Answer.Text(wrong))
            assertEquals(TargetStatus.NOT_MET, evaluation.targetStatus)
            assertTrue(evaluation.matchingResults.orEmpty().isEmpty())
            val assessment = PracticeAssessment(null,
                PracticeTargetDescriptor(id, "G016", null, targetScopeId = exercise.targetScopeId),
                "primary", listOf("vocabulary-p3:contrast:${source.contrastId}"))
            assertEquals(emptyList(), PracticeFeedbackExposureCalculator.releasedKeys(
                assessment.answerExposureKeys, listOf(assessment), listOf(evaluation.observation()),
                PracticeFeedbackExposureCalculator.VOCABULARY_P3_EXPOSURE_POLICY,
            ))
        }
    }

    @Test
    fun `paired mapping metadata follows shuffled display order without a stable answer position`() {
        listOf("p3-3.4", "p3-5.3").forEach { taskId ->
            val task = VocabularyP3Content.tasks.single { it.id == taskId }
            val exercises = (0..20).map { seed ->
                VocabularyP3Content.tasks("E051").indices.asSequence().map { index ->
                    engine.generate(GenerationRequest("E051", "G016", seed, index, "pilot-20", "generator-20"))
                }.first { it.inspector.vocabularySource?.taskId == taskId }
            }
            exercises.forEach { exercise ->
                assertEquals(exercise.prompt.choices, exercise.prompt.choiceMappings.map { it.choice })
                assertTrue(exercise.prompt.choiceMappings.all { it.mappings.size == 2 })
            }
            assertTrue(exercises.any { it.prompt.choiceMappings.first().choice == task.expected })
            assertTrue(exercises.any { it.prompt.choiceMappings.first().choice != task.expected })
        }
    }

    @Test
    fun `recall keeps known wrong unknown and response shape outcomes separate`() {
        val exercise = task("p3-5.4")
        assertEquals("The person is travelling home by car. Type the introduced present form of this verb.",
            exercise.instruction)
        assertEquals("Car context: This journey is by car.", exercise.context)
        assertEquals("at køre", exercise.prompt.cue)
        assertEquals("da", exercise.prompt.cueLanguage)
        assertEquals("da", exercise.prompt.answerLanguage)
        assertEquals("Danish present form", exercise.prompt.inputLabel)
        assertFalse("køre" in exercise.instruction)
        fun evaluate(text: String) = engine.evaluate(exercise, Answer.Text(text))
        assertEquals(TargetStatus.MET, evaluate("kører").targetStatus)
        assertEquals("PRESENT_FORM_REQUIRED", evaluate("køre").issues.single().code)
        listOf("cykler", "venter").forEach { known ->
            val result = evaluate(known)
            assertEquals(TargetStatus.NOT_MET, result.targetStatus)
            assertEquals(LinguisticStatus.ACCEPTABLE, result.linguisticStatus)
            assertEquals("VEHICULAR_ACTION_REQUIRED", result.issues.single().code)
        }
        val unknown = evaluate("ruller")
        assertEquals(LinguisticStatus.UNKNOWN, unknown.linguisticStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, unknown.meaningSatisfaction)
        val prose = evaluate("kører nu")
        assertEquals(ResponseConstraintStatus.NOT_MET, prose.responseConstraintStatus)
        assertEquals("ONE_WORD_REQUIRED", prose.issues.single().code)
    }

    @Test
    fun `English-only supplied distractors receive neutral feedback`() {
        val meaning = engine.evaluate(task("p3-1.1"), Answer.Text("a station"))
        val reading = engine.evaluate(task("p3-3.2"), Answer.Text("at the station"))
        listOf(meaning, reading).forEach { evaluation ->
            assertEquals(TargetStatus.NOT_MET, evaluation.targetStatus)
            assertEquals("SUPPLIED_ANSWER_INCORRECT", evaluation.issues.single().code)
            assertFalse("introduced meaning" in evaluation.feedback.lowercase())
        }
    }

    @Test
    fun `all reading tasks persist the same literal private oracle in the answer key and inspector`() {
        val expectedAnswers = mapOf(
            "p3-1.3" to "Nora",
            "p3-2.2" to "to the station",
            "p3-3.2" to "in Odense",
            "p3-3.3" to "at home",
            "p3-4.2" to "a ticket",
            "p3-5.2" to "at the station",
            "p3-6.2" to "changes trains/connections",
            "p3-6.3" to "to the pharmacy",
        )
        expectedAnswers.forEach { (taskId, expected) ->
            val exercise = task(taskId)
            val source = exercise.source as VocabularyP3ExerciseSource
            assertEquals(expected, (source.answerKey as AnswerKey.MeaningChoice).expected, taskId)
            assertEquals(expected, exercise.inspector.vocabularySource?.expectedAnswer, taskId)
            assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Text(expected)).targetStatus, taskId)
        }
    }

    @Test
    fun `every P3 answer family covers correct partial wrong unknown and malformed shapes`() {
        VocabularyP3Content.tasks.forEach { task ->
            val exercise = task(task.id)
            val source = requireNotNull(exercise.inspector.vocabularySource)
            when (task.kind) {
                VocabularyP3TaskKind.CHOICE, VocabularyP3TaskKind.READING, VocabularyP3TaskKind.CONTRAST -> {
                    val expected = source.expectedAnswer ?: task.targetFactId?.let { factId ->
                        source.supportFacts.single { it.factId == factId }.valueText
                    }.orEmpty()
                    assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Text(expected)).targetStatus, task.id)
                    val suppliedWrong = exercise.prompt.choices.firstOrNull { it != expected }
                    if (suppliedWrong != null) {
                        assertEquals(TargetStatus.NOT_MET,
                            engine.evaluate(exercise, Answer.Text(suppliedWrong)).targetStatus, task.id)
                    }
                    assertEquals(ResponseConstraintStatus.NOT_MET,
                        engine.evaluate(exercise, Answer.Text("outside supplied bank")).responseConstraintStatus, task.id)
                    assertEquals(ResponseConstraintStatus.NOT_MET,
                        engine.evaluate(exercise, Answer.Order(emptyList())).responseConstraintStatus, task.id)
                }
                VocabularyP3TaskKind.MATCHING -> assertMatchingOutcomes(exercise, task.id, source)
                VocabularyP3TaskKind.TILES -> {
                    val order = source.acceptedOrders.single()
                    assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Order(order)).targetStatus, task.id)
                    val reversed = engine.evaluate(exercise, Answer.Order(order.reversed()))
                    assertEquals(TargetStatus.UNEVALUABLE, reversed.targetStatus, task.id)
                    assertEquals(ResponseConstraintStatus.MET, reversed.responseConstraintStatus, task.id)
                    listOf(order.dropLast(1), order + order.first(), order.dropLast(1) + "outside").forEach {
                        assertEquals(ResponseConstraintStatus.NOT_MET,
                            engine.evaluate(exercise, Answer.Order(it)).responseConstraintStatus, task.id)
                    }
                    assertEquals(ResponseConstraintStatus.NOT_MET,
                        engine.evaluate(exercise, Answer.Text("wrong shape")).responseConstraintStatus, task.id)
                }
                VocabularyP3TaskKind.RECALL -> {
                    assertEquals(TargetStatus.MET,
                        engine.evaluate(exercise, Answer.Text(requireNotNull(source.expectedAnswer))).targetStatus, task.id)
                    assertEquals(ResponseConstraintStatus.NOT_MET,
                        engine.evaluate(exercise, Answer.Text("")).responseConstraintStatus, task.id)
                    assertEquals(ResponseConstraintStatus.NOT_MET,
                        engine.evaluate(exercise, Answer.Order(emptyList())).responseConstraintStatus, task.id)
                }
            }
        }
    }

    @Test
    fun `six plans pin exact sizes revisions policies and retain complete teaching scenes`() {
        assertFailsWith<IllegalArgumentException> {
            PracticePlanner(engine).plan("vocabulary-p3-s1", "vocabulary-p3-s1-unknown", 47)
        }
        assertFailsWith<IllegalStateException> { VocabularyP3Teaching.forSession(7) }
        assertEquals(listOf(4, 4, 4, 3, 4, 4), VocabularyP3PracticePlanner.SPECIFICATIONS.map { it.taskIds.size })
        VocabularyP3PracticePlanner.SPECIFICATIONS.forEachIndexed { index, specification ->
            val plan = PracticePlanner(engine).plan(specification.focusId, specification.planRevision, Int.MAX_VALUE)
            assertEquals("pilot-20", plan.contentRevision)
            assertEquals("generator-20", plan.generatorRevision)
            assertEquals("evaluation-20", plan.evaluationRevision)
            assertEquals("vocabulary-p3-outcome-1", plan.completionRuleId)
            assertEquals("vocabulary-p3-exposure-1", plan.exposurePolicyId)
            assertEquals(index + 1, specification.focusId.removePrefix("vocabulary-p3-s").toInt())
            assertEquals(specification.introduction, plan.introduction)
            assertTrue(specification.introductionKeys.isNotEmpty())
            plan.items.forEach { item ->
                assertEquals(specification.introduction, item.exercise.introduction)
                assertEquals(specification.introductionKeys, item.lessonExposureKeys)
                assertTrue(item.hintExposureKeys.isEmpty())
                if (item.exercise.formatId == "E051") {
                    assertTrue(item.promptExposureKeys.isNotEmpty())
                    assertTrue(item.promptExposureKeys.all { it.startsWith("vocabulary-p3:form:") })
                    assertTrue(item.promptExposureKeys.none { it.startsWith("vocabulary-p3:spatial:") })
                }
            }
        }
        val rail = PracticePlanner(engine).plan("vocabulary-p3-s6", "vocabulary-p3-s6-1", 47)
        assertTrue(rail.introduction.any { "changes trains/connections in Aarhus" in it.body })
        val railAssessment = rail.items.single { it.exercise.inspector.vocabularySource?.taskId == "p3-6.2" }
        assertEquals("Rail context: This journey is by train.", railAssessment.exercise.context)
    }

    @Test
    fun `long widened task selection is deterministic and does not repeat reversed source hashes`() {
        val requests = listOf(0, 47, Int.MAX_VALUE).flatMap { seed ->
            VocabularyP3Content.formats.flatMap { format ->
                VocabularyP3Content.tasks(format.id).indices.map { index -> GenerationRequest(
                    format.id, format.targetSkillId, seed, index, "pilot-20", "generator-20",
                ) }
            }
        }
        val forward = requests.map(engine::generate)
        val repeated = requests.map(engine::generate)
        assertEquals(forward.map { listOf(it.id, it.prompt, it.inspector.vocabularySource) },
            repeated.map { listOf(it.id, it.prompt, it.inspector.vocabularySource) })
        val matching = forward.filter { it.formatId == "E047" }
        assertTrue(matching.any { exercise ->
            val columns = requireNotNull(exercise.prompt.matchingColumns)
            columns.left.map { it.text } != columns.left.map { it.text }.sorted()
        })
        assertNotEquals(matching.first().prompt.matchingColumns, matching.last().prompt.matchingColumns)
    }

    private fun task(id: String): GeneratedExercise {
        val task = VocabularyP3Content.tasks.single { it.id == id }
        return VocabularyP3Content.tasks(task.formatId).indices.asSequence().map { index ->
            engine.generate(GenerationRequest(task.formatId, VocabularyP3Content.formats.single { it.id == task.formatId }.targetSkillId,
                47, index, "pilot-20", "generator-20"))
        }.first { it.inspector.vocabularySource?.taskId == id }
    }

    private fun assertMatchingOutcomes(exercise: GeneratedExercise, taskId: String, source: VocabularyExerciseSourceDetails) {
        val pairs = source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) }
        assertEquals(TargetStatus.MET, engine.evaluate(exercise, Answer.Matching(pairs)).targetStatus, taskId)
        assertEquals(TargetStatus.UNEVALUABLE,
            engine.evaluate(exercise, Answer.Matching(pairs.dropLast(1))).targetStatus, taskId)
        val swapped = pairs.toMutableList().apply {
            val first = this[0]
            val second = this[1]
            this[0] = first.copy(rightId = second.rightId)
            this[1] = second.copy(rightId = first.rightId)
        }
        val knownWrong = engine.evaluate(exercise, Answer.Matching(swapped))
        assertEquals(TargetStatus.NOT_MET, knownWrong.targetStatus, taskId)
        assertEquals(ResponseConstraintStatus.MET, knownWrong.responseConstraintStatus, taskId)
        listOf(
            pairs.mapIndexed { index, pair -> if (index == 0) pair.copy(leftId = "outside") else pair },
            pairs.mapIndexed { index, pair -> if (index == 0) pair.copy(rightId = "outside") else pair },
            pairs.mapIndexed { index, pair -> if (index == 0) pair.copy(leftId = pairs.last().leftId) else pair },
            pairs + pairs.first(),
            pairs.mapIndexed { index, pair ->
                if (index == 0) pair.copy(rightId = pairs.last().rightId) else pair
            },
        ).forEach { malformed ->
            assertEquals(ResponseConstraintStatus.NOT_MET,
                engine.evaluate(exercise, Answer.Matching(malformed)).responseConstraintStatus, taskId)
        }
        assertEquals(ResponseConstraintStatus.NOT_MET,
            engine.evaluate(exercise, Answer.Text("wrong shape")).responseConstraintStatus, taskId)
    }

    private fun Evaluation.observation() = PracticeFeedbackAttemptObservation(
        targetStatus = targetStatus,
        linguisticStatus = linguisticStatus,
        responseConstraintStatus = responseConstraintStatus,
        issueCodes = issues.map { it.code },
        meaningSatisfaction = meaningSatisfaction,
        matchingResults = matchingResults.orEmpty(),
    )
    private fun formatId(number: Int) = "E${number.toString().padStart(3, '0')}"
    private fun lexicalId(number: Int) = "L${number.toString().padStart(3, '0')}"
}
