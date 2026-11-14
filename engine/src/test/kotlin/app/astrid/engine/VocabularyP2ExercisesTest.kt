package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyP2ExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair nineteen publishes the bounded P2 packet without changing pair eighteen`() {
        assertTrue(RevisionPair("pilot-19", "generator-19") in engine.supportedRevisionPairs())
        assertEquals("evaluation-18", engine.evaluationRevision("pilot-18", "generator-18"))
        assertEquals("evaluation-19", engine.evaluationRevision("pilot-19", "generator-19"))
        assertEquals((40..44).map(::formatId), PilotContent.pilotNineteenSnapshot.formats.takeLast(5).map { it.id })
        assertEquals(21, VocabularyP2Content.tasks.size)
        assertEquals(
            mapOf("E040" to 4, "E041" to 3, "E042" to 4, "E043" to 5, "E044" to 5),
            VocabularyP2Content.tasks.groupingBy(VocabularyP2Task::formatId).eachCount(),
        )
        assertEquals((50..63).map { "L${it.toString().padStart(3, '0')}" }.toSet(),
            VocabularyP2Content.newLexemes.map { it.entryId }.toSet())
        assertEquals((1..39).map(::formatId), PilotContent.pilotEighteenSnapshot.formats.map { it.id })
        assertEquals(49, PilotContent.pilotEighteenSnapshot.lexicalReview.size)
    }

    @Test
    fun `all twenty one literal tasks generate and their canonical answers meet the target`() {
        val exercises = VocabularyP2Content.tasks.associate { task -> task.id to exercise(task.id) }
        assertEquals(VocabularyP2Content.tasks.map(VocabularyP2Task::id).toSet(), exercises.keys)
        exercises.forEach { (taskId, exercise) ->
            val source = assertNotNull(exercise.inspector.vocabularySource)
            assertEquals(taskId, source.taskId)
            assertEquals("pilot-19", exercise.contentRevision)
            assertEquals("generator-19", exercise.generatorRevision)
            val evaluation = engine.evaluate(exercise, canonicalAnswer(exercise))
            assertEquals(TargetStatus.MET, evaluation.targetStatus, taskId)
            assertEquals(MeaningSatisfaction.MET, evaluation.meaningSatisfaction, taskId)
            assertEquals(ResponseConstraintStatus.MET, evaluation.responseConstraintStatus, taskId)
        }
    }

    @Test
    fun `reading facts preserve referents predicates values translations and explicit chronology`() {
        val bread = source("p2-1.3").supportFacts.single { it.factId == "p2-s1-f2" }
        assertEquals("p2:Nora", bread.referentId)
        assertEquals("Nora", bread.referentDisplay)
        assertEquals("L012", bread.predicateId)
        assertEquals("L012", bread.predicateSource?.entryId)
        assertEquals("L012-S01", bread.predicateSource?.senseId)
        assertEquals("L050", bread.valueId)
        assertEquals("L050", bread.objectSource?.entryId)
        assertEquals("Nora eats bread.", bread.translation)

        val request = source("p2-6.2").supportFacts.single { it.factId == "p2-s6-f2" }
        assertEquals("L009", request.predicateId)
        assertEquals("L009-S02", request.predicateSource?.senseId)
        assertEquals("p2-quantity-bottle-water", request.valueId)
        val quantity = assertNotNull(request.quantitySource)
        assertEquals("en flaske vand", quantity.display)
        assertEquals("L063", quantity.headSource.entryId)
        assertEquals(listOf("L063", "L047"), quantity.componentSources.map { it.entryId })

        val chronology = source("p2-4.2")
        assertEquals("p2-s4-buy-before-pay", chronology.targetChronologyRelationId)
        assertEquals(
            VocabularyChronologyRelationDetails("p2-s4-buy-before-pay", "p2-s4-f1", "p2-s4-f2"),
            chronology.chronologyRelations.single(),
        )
        assertEquals("the speaker pays", chronology.expectedAnswer)
        assertTrue(chronology.supportFacts.single { it.factId == "p2-s4-f1" }.sentence.contains("i en café"))
    }

    @Test
    fun `reading exercises publish one explicit fact target without a lexical primary target`() {
        val readings = VocabularyP2Content.tasks.filter { it.kind == VocabularyP2TaskKind.READING }.map { exercise(it.id) }
        assertEquals(
            setOf("object", "subject", "last-event"),
            readings.map { assertNotNull(it.inspector.vocabularySource).questionRole }.toSet(),
        )
        readings.forEach { exercise ->
            assertNull(exercise.lexicalTarget)
            assertEquals(ReadingTask("explicit-fact"), exercise.readingTarget)
            assertNull(exercise.targetSkillId)
        }
    }

    @Test
    fun `quantity requests carry intact composite tiles and bind the comma before tak`() {
        listOf("p2-6.3", "p2-6.4").forEach { taskId ->
            val exercise = exercise(taskId)
            val source = assertNotNull(exercise.inspector.vocabularySource)
            val quantity = source.tileOccurrences.single { it.quantitySource != null }.quantitySource!!
            assertEquals(quantity.display, source.tileOccurrences.single { it.quantitySource != null }.form)
            assertEquals(2, quantity.componentSources.size)
            val tak = source.tileOccurrences.single { it.form == "tak" }
            assertEquals(listOf(FixedPunctuation(tak.blockId, ",")), exercise.prompt.fixedPunctuation)
            assertTrue(assertNotNull(exercise.inspector.canonicalSentence).contains(", tak."))
        }
        assertTrue(exercise("p2-5.2").prompt.fixedPunctuation.isEmpty())
        assertTrue(pairEighteen("p1-5.4").prompt.fixedPunctuation.isEmpty())
    }

    @Test
    fun `matching grades each retained pair and rejects duplicate or foreign IDs`() {
        val exercise = exercise("p2-6.1")
        val expected = assertNotNull(exercise.inspector.vocabularySource).matchingPairs
        assertEquals(2, expected.size)
        val first = expected.first()
        val partial = engine.evaluate(exercise, Answer.Matching(listOf(MatchingPair(first.leftId, first.rightId))))
        assertEquals(TargetStatus.UNEVALUABLE, partial.targetStatus)
        assertEquals(listOf(TargetStatus.MET, TargetStatus.UNEVALUABLE), partial.matchingResults?.map { it.targetStatus })
        assertEquals(ResponseConstraintStatus.NOT_MET, partial.responseConstraintStatus)

        val duplicate = engine.evaluate(exercise, Answer.Matching(listOf(
            MatchingPair(first.leftId, first.rightId), MatchingPair(first.leftId, expected.last().rightId),
        )))
        assertEquals(TargetStatus.UNEVALUABLE, duplicate.targetStatus)
        assertEquals("MATCHING_SELECTION_INVALID", duplicate.issues.single().code)
        val foreign = engine.evaluate(exercise, Answer.Matching(listOf(MatchingPair("outside", first.rightId))))
        assertEquals("MATCHING_SELECTION_INVALID", foreign.issues.single().code)
        val duplicateRight = engine.evaluate(exercise, Answer.Matching(listOf(
            MatchingPair(first.leftId, first.rightId), MatchingPair(expected.last().leftId, first.rightId),
        )))
        assertEquals("MATCHING_SELECTION_INVALID", duplicateRight.issues.single().code)
        val foreignRight = engine.evaluate(exercise, Answer.Matching(listOf(MatchingPair(first.leftId, "outside"))))
        assertEquals("MATCHING_SELECTION_INVALID", foreignRight.issues.single().code)
        val oversized = engine.evaluate(exercise, Answer.Matching((0..4).map { MatchingPair("l-$it", "r-$it") }))
        assertEquals("MATCHING_SELECTION_INVALID", oversized.issues.single().code)
    }

    @Test
    fun `P2 matching IDs are opaque and independently assigned to displayed columns`() {
        val boards = listOf(0, 1, 47).map { seed -> exercise("p2-3.1", seed) }
        boards.forEach { exercise ->
            val columns = assertNotNull(exercise.prompt.matchingColumns)
            val pairs = assertNotNull(exercise.inspector.vocabularySource).matchingPairs
            assertTrue(columns.left.all { it.id.startsWith("l-") })
            assertTrue(columns.right.all { it.id.startsWith("r-") })
            assertTrue(pairs.all { it.leftId.removePrefix("l-") != it.rightId.removePrefix("r-") })
            pairs.forEach { pair ->
                assertEquals(pair.lexicalSource.gloss, columns.left.single { it.id == pair.leftId }.text)
                assertEquals(
                    pair.lexicalSource.forms.present ?: pair.lexicalSource.forms.bundle ?: pair.lexicalSource.forms.form,
                    columns.right.single { it.id == pair.rightId }.text,
                )
            }
        }
        assertTrue(boards.map { assertNotNull(it.prompt.matchingColumns).left.map { item -> item.text } }.distinct().size > 1)
        assertTrue(boards.map { assertNotNull(it.prompt.matchingColumns).right.map { item -> item.text } }.distinct().size > 1)
    }

    @Test
    fun `choice banks and complete tile orders stay closed`() {
        val choice = exercise("p2-2.1")
        assertTrue(engine.isSuppliedChoice(choice, " milk "))
        assertTrue(engine.isSuppliedChoice(choice, "coffee"))
        assertFalse(engine.isSuppliedChoice(choice, "juice"))
        assertFalse(engine.isSuppliedChoice(exercise("p2-3.1"), "a roll"))
        val outside = engine.evaluate(choice, Answer.Text("juice"))
        assertEquals(TargetStatus.UNEVALUABLE, outside.targetStatus)
        assertEquals("SUPPLIED_CHOICE_REQUIRED", outside.issues.single().code)
        val wrongSupplied = engine.evaluate(choice, Answer.Text("coffee"))
        assertEquals(TargetStatus.NOT_MET, wrongSupplied.targetStatus)
        assertEquals(MeaningSatisfaction.NOT_MET, wrongSupplied.meaningSatisfaction)
        val wrongKind = engine.evaluate(choice, Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, wrongKind.targetStatus)
        assertEquals(NormalizedAnswer.Text(""), wrongKind.normalizedAnswer)

        val order = exercise("p2-5.2")
        val canonical = assertNotNull(order.inspector.vocabularySource).acceptedOrders.single()
        val unknown = engine.evaluate(order, Answer.Order(canonical.reversed()))
        assertEquals(TargetStatus.UNEVALUABLE, unknown.targetStatus)
        assertEquals(ResponseConstraintStatus.MET, unknown.responseConstraintStatus)
        assertEquals("UNRECOGNIZED_P2_ORDER", unknown.issues.single().code)
        val missing = engine.evaluate(order, Answer.Order(canonical.dropLast(1)))
        assertEquals(ResponseConstraintStatus.NOT_MET, missing.responseConstraintStatus)
        assertEquals("REQUIRED_WORDS_MISSING", missing.issues.single().code)
        val duplicate = engine.evaluate(order, Answer.Order(canonical.dropLast(1) + canonical.first()))
        assertEquals("TILE_SELECTION_INVALID", duplicate.issues.single().code)
        val foreign = engine.evaluate(order, Answer.Order(canonical.dropLast(1) + "outside"))
        assertEquals("TILE_SELECTION_INVALID", foreign.issues.single().code)
        val wrongKindOrder = engine.evaluate(order, Answer.Text("not an order"))
        assertEquals("REQUIRED_WORDS_MISSING", wrongKindOrder.issues.single().code)
    }

    @Test
    fun `matching distinguishes empty partial wrong complete and correct complete boards`() {
        val exercise = exercise("p2-3.1")
        val expected = assertNotNull(exercise.inspector.vocabularySource).matchingPairs
        val empty = engine.evaluate(exercise, Answer.Text("wrong kind"))
        assertEquals(TargetStatus.UNEVALUABLE, empty.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, empty.linguisticStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, empty.meaningSatisfaction)

        val shiftedRights = expected.mapIndexed { index, pair ->
            MatchingPair(pair.leftId, expected[(index + 1) % expected.size].rightId)
        }
        val wrong = engine.evaluate(exercise, Answer.Matching(shiftedRights))
        assertEquals(TargetStatus.NOT_MET, wrong.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, wrong.linguisticStatus)
        assertEquals(MeaningSatisfaction.NOT_MET, wrong.meaningSatisfaction)
        assertTrue(assertNotNull(wrong.matchingResults).all { it.targetStatus == TargetStatus.NOT_MET })

        val correct = engine.evaluate(
            exercise,
            Answer.Matching(expected.map { MatchingPair(it.leftId, it.rightId) }),
        )
        assertEquals(TargetStatus.MET, correct.targetStatus)
        assertEquals(MeaningSatisfaction.MET, correct.meaningSatisfaction)
    }

    @Test
    fun `P2 lexical display requires one explicitly modelled form family`() {
        assertEquals("spiser", VocabularyP2Content.eat.display)
        assertEquals("en bolle", VocabularyP2Content.roll.display)
        assertEquals("brød", VocabularyP2Content.bread.display)
        assertFailsWith<IllegalStateException> {
            VocabularyP2Lexeme(
                "test", "test-sense", VocabularyLexicalKind.MASS_WORD, VocabularyLexicalForms(), "test", "test",
            ).display
        }
    }

    @Test
    fun `six lesson profiles expose exact titles sizes policies and taught material`() {
        val expected = listOf(
            "A small breakfast" to 3,
            "Drinks on the table" to 4,
            "Getting attention and receiving a roll" to 4,
            "Buying and paying" to 3,
            "Marking a polite wish" to 3,
            "Requesting a supplied quantity" to 4,
        )
        expected.forEachIndexed { index, (title, size) ->
            val number = index + 1
            val focus = "vocabulary-p2-s$number"
            val revision = "$focus-1"
            val profile = PracticePlanner().profiles().single { it.focusId == focus }
            assertEquals(title, profile.title)
            assertEquals(size, profile.itemCount)
            assertEquals(revision, profile.planRevision)
            val plan = PracticePlanner().plan(focus, revision, 47)
            assertEquals("vocabulary-p2-outcome-1", plan.completionRuleId)
            assertEquals("vocabulary-p2-exposure-1", plan.exposurePolicyId)
            assertEquals(size, plan.items.size)
            assertTrue(plan.introduction.isNotEmpty())
            assertFalse(plan.introductionExposureKeys.isNullOrEmpty())
        }
        assertFailsWith<IllegalArgumentException> {
            PracticePlanner().plan("vocabulary-p2-s7", "vocabulary-p2-s7-1", 47)
        }
        val buying = PracticePlanner().plan("vocabulary-p2-s4", "vocabulary-p2-s4-1", 47)
        assertTrue(buying.introduction.any { "Paying is last" in it.body })
        val request = PracticePlanner().plan("vocabulary-p2-s6", "vocabulary-p2-s6-1", 47)
        assertTrue(request.introduction.any { "possession har" in it.body })
        assertTrue(request.introduction.any { "en flaske vand" in it.body })
    }

    @Test
    fun `P2 completion requires teaching and the latest complete known outcome`() {
        val correct = completion(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        assertTrue(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P2_RULE, correct))
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(teachingSaved = false),
        ))
        assertTrue(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            completion(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET),
        ))
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            completion(TargetStatus.MET, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET),
        ))
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            completion(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.NOT_MET),
        ))

        val matching = exercise("p2-6.1")
        val pairs = assertNotNull(matching.inspector.vocabularySource).matchingPairs
        val partialEvaluation = engine.evaluate(
            matching,
            Answer.Matching(listOf(MatchingPair(pairs.first().leftId, pairs.first().rightId))),
        )
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(matchingResults = assertNotNull(partialEvaluation.matchingResults)),
        ))
        val swappedEvaluation = engine.evaluate(
            matching,
            Answer.Matching(listOf(
                MatchingPair(pairs[0].leftId, pairs[1].rightId),
                MatchingPair(pairs[1].leftId, pairs[0].rightId),
            )),
        )
        assertTrue(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(matchingResults = assertNotNull(swappedEvaluation.matchingResults)),
        ))
        val correctMatching = engine.evaluate(
            matching,
            Answer.Matching(pairs.map { MatchingPair(it.leftId, it.rightId) }),
        )
        assertTrue(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(matchingResults = assertNotNull(correctMatching.matchingResults)),
        ))
        val knownMatching = assertNotNull(swappedEvaluation.matchingResults)
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(matchingResults = knownMatching.take(1)),
        ))
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(matchingResults = List(5) { knownMatching.first() }),
        ))
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(matchingResults = knownMatching.map { it.copy(linguisticStatus = LinguisticStatus.UNKNOWN) }),
        ))
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            correct.copy(
                matchingResults = knownMatching.map {
                    it.copy(responseConstraintStatus = ResponseConstraintStatus.NOT_MET)
                },
            ),
        ))

        val order = exercise("p2-5.2")
        val unknown = engine.evaluate(
            order,
            Answer.Order(assertNotNull(order.inspector.vocabularySource).acceptedOrders.single().reversed()),
        )
        assertFalse(PracticeCompletionPolicy.canAdvance(
            PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            completion(unknown.targetStatus, unknown.linguisticStatus, assertNotNull(unknown.responseConstraintStatus)),
        ))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P2_RULE, null))
    }

    @Test
    fun `P2 matching feedback exposes only mappings confirmed by the submitted pairs`() {
        val s6 = PracticePlanner(engine).plan("vocabulary-p2-s6", "vocabulary-p2-s6-1", 47)
        val s6Board = s6.items.single { assertNotNull(it.exercise.inspector.vocabularySource).taskId == "p2-6.1" }
        val s6Pairs = assertNotNull(s6Board.exercise.inspector.vocabularySource).matchingPairs
        assertEquals(setOf("L062", "L063"), assertNotNull(s6Board.exercise.lexicalTargets).map { it.entryId }.toSet())
        assertEquals(
            setOf(
                "vocabulary-p2:mapping:L062:L062-S01",
                "vocabulary-p2:mapping:L063:L063-S01",
            ),
            s6Board.assessments.flatMap(PracticeAssessment::answerExposureKeys).toSet(),
        )
        val partial = engine.evaluate(
            s6Board.exercise,
            Answer.Matching(listOf(MatchingPair(s6Pairs.first().leftId, s6Pairs.first().rightId))),
        )
        assertEquals(
            listOf("vocabulary-p2:mapping:L062:L062-S01"),
            released(s6, s6Board, partial),
        )
        assertTrue(released(s6, s6Board, partial).none { "L046" in it || "L047" in it })
        val swapped = engine.evaluate(
            s6Board.exercise,
            Answer.Matching(listOf(
                MatchingPair(s6Pairs[0].leftId, s6Pairs[1].rightId),
                MatchingPair(s6Pairs[1].leftId, s6Pairs[0].rightId),
            )),
        )
        assertTrue(released(s6, s6Board, swapped).isEmpty())

        val s1 = PracticePlanner(engine).plan("vocabulary-p2-s1", "vocabulary-p2-s1-1", 47)
        val s1Board = s1.items.single { assertNotNull(it.exercise.inspector.vocabularySource).taskId == "p2-1.1" }
        val s1Pairs = assertNotNull(s1Board.exercise.inspector.vocabularySource).matchingPairs
        val mixed = engine.evaluate(
            s1Board.exercise,
            Answer.Matching(listOf(
                MatchingPair(s1Pairs[0].leftId, s1Pairs[0].rightId),
                MatchingPair(s1Pairs[1].leftId, s1Pairs[2].rightId),
                MatchingPair(s1Pairs[2].leftId, s1Pairs[1].rightId),
            )),
        )
        assertEquals(listOf("vocabulary-p2:mapping:L021:L021-S01"), released(s1, s1Board, mixed))
    }

    @Test
    fun `P2 request feedback releases only its reviewed order`() {
        val plan = PracticePlanner(engine).plan("vocabulary-p2-s6", "vocabulary-p2-s6-1", 47)
        val request = plan.items.single { assertNotNull(it.exercise.inspector.vocabularySource).taskId == "p2-6.3" }
        val source = assertNotNull(request.exercise.inspector.vocabularySource)
        val evaluation = engine.evaluate(request.exercise, Answer.Order(source.acceptedOrders.single()))
        val released = released(plan, request, evaluation)
        assertEquals(listOf("vocabulary-p2:order:p2-6.3"), released)
        assertTrue(released.none { "L009" in it || "L062" in it || "L045" in it })
        val unknown = engine.evaluate(request.exercise, Answer.Order(source.acceptedOrders.single().reversed()))
        assertTrue(released(plan, request, unknown).isEmpty())
        val fact = assertNotNull(exercise("p2-6.2").inspector.vocabularySource).supportFacts
            .single { it.factId == "p2-s6-f2" }
        assertEquals("L009-S02", fact.predicateSource?.senseId)
        val have = source.tileOccurrences.single { it.form == "have" }.lexicalSource
        assertEquals("L009", have?.entryId)
        assertEquals("L009-S02", have?.senseId)
    }

    @Test
    fun `P2 exposure rejects incomplete or contradictory frozen matching metadata`() {
        val plan = PracticePlanner(engine).plan("vocabulary-p2-s6", "vocabulary-p2-s6-1", 47)
        val item = plan.items.single { assertNotNull(it.exercise.inspector.vocabularySource).taskId == "p2-6.1" }
        val pairs = assertNotNull(item.exercise.inspector.vocabularySource).matchingPairs
        val evaluation = engine.evaluate(
            item.exercise,
            Answer.Matching(pairs.map { MatchingPair(it.leftId, it.rightId) }),
        )
        val observation = observation(evaluation)
        fun release(assessments: List<PracticeAssessment>, attempt: PracticeFeedbackAttemptObservation) =
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                assessments,
                listOf(attempt),
                plan.exposurePolicyId,
            )

        assertFailsWith<IllegalArgumentException> { release(item.assessments, observation.copy(matchingResults = emptyList())) }
        assertFailsWith<IllegalArgumentException> {
            release(
                item.assessments,
                observation.copy(matchingResults = observation.matchingResults.map { it.copy(leftId = "outside") }),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            release(listOf(item.assessments.first(), item.assessments.last().copy(slotId = item.assessments.first().slotId)), observation)
        }
        assertFailsWith<IllegalArgumentException> {
            release(item.assessments.map { it.copy(assessmentRole = "unexpected") }, observation)
        }
        assertFailsWith<IllegalArgumentException> {
            release(
                item.assessments.map {
                    it.copy(targetDescriptor = it.targetDescriptor.copy(lexicalTarget = null, targetSkillId = null))
                },
                observation,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            release(
                item.assessments.map {
                    it.copy(targetDescriptor = it.targetDescriptor.copy(assessmentMode = "recall"))
                },
                observation,
            )
        }
    }

    private fun exercise(taskId: String): GeneratedExercise {
        return exercise(taskId, 47)
    }

    private fun exercise(taskId: String, seed: Int): GeneratedExercise {
        val task = VocabularyP2Content.tasks.single { it.id == taskId }
        return (0 until VocabularyP2Content.tasks(task.formatId).size).asSequence().map { index ->
            engine.generate(GenerationRequest(task.formatId, if (task.kind == VocabularyP2TaskKind.TILES) "G005" else null,
                seed, index, "pilot-19", "generator-19"))
        }.first { assertNotNull(it.inspector.vocabularySource).taskId == taskId }
    }

    private fun pairEighteen(taskId: String): GeneratedExercise = VocabularyP1Content.tasks
        .filter { it.formatId == VocabularyP1Content.tasks.single { candidate -> candidate.id == taskId }.formatId }
        .indices.asSequence().map { index ->
            val task = VocabularyP1Content.tasks.single { candidate -> candidate.id == taskId }
            engine.generate(GenerationRequest(task.formatId, task.targetSkillId, 47, index, "pilot-18", "generator-18"))
        }.first { assertNotNull(it.inspector.vocabularySource).taskId == taskId }

    private fun source(taskId: String) = assertNotNull(exercise(taskId).inspector.vocabularySource)

    private fun canonicalAnswer(exercise: GeneratedExercise): Answer {
        val source = assertNotNull(exercise.inspector.vocabularySource)
        return when (exercise.formatId) {
            "E042" -> Answer.Matching(source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) })
            "E043" -> Answer.Order(source.acceptedOrders.single())
            else -> Answer.Text(assertNotNull(source.expectedAnswer))
        }
    }

    private fun formatId(number: Int) = "E${number.toString().padStart(3, '0')}"

    private fun completion(target: TargetStatus, language: LinguisticStatus, constraint: ResponseConstraintStatus) =
        PracticeCompletionAttemptObservation(target, language, constraint, emptyList())

    private fun released(plan: PracticePlan, item: PracticePlanItem, evaluation: Evaluation) =
        PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(observation(evaluation)),
            plan.exposurePolicyId,
        )

    private fun observation(evaluation: Evaluation) = PracticeFeedbackAttemptObservation(
        evaluation.targetStatus,
        linguisticStatus = evaluation.linguisticStatus,
        responseConstraintStatus = evaluation.responseConstraintStatus,
        issueCodes = evaluation.issues.map(EvaluationIssue::code),
        meaningSatisfaction = evaluation.meaningSatisfaction,
        matchingResults = evaluation.matchingResults.orEmpty(),
    )
}
