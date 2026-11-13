package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyP1ExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair eighteen exposes all nine literal operations and 23 authored tasks`() {
        assertEquals(RevisionPair("pilot-19", "generator-19"), engine.supportedRevisionPairs().last())
        assertEquals("evaluation-17", engine.evaluationRevision("pilot-17", "generator-17"))
        assertEquals("evaluation-18", engine.evaluationRevision("pilot-18", "generator-18"))
        assertEquals((31..39).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotEighteenSnapshot.formats.map { it.id }.filter { it >= "E031" }.toSet())
        assertEquals(23, VocabularyP1Content.tasks.size)
        assertEquals(18, VocabularyP1Content.placements.size)
        assertEquals(9, VocabularyP1Content.newLexemes.size)
        assertEquals(49, PilotContent.pilotEighteenSnapshot.lexicalReview.size)
        assertEquals((41..49).map { "L${it.toString().padStart(3, '0')}" }.toSet(),
            VocabularyP1Content.newLexemes.map { it.entryId }.toSet())
        assertEquals(
            VocabularyLexicalKind.entries.toSet() - setOf(VocabularyLexicalKind.ADJECTIVE, VocabularyLexicalKind.INTERJECTION),
            VocabularyP1Content.placements.map { it.kind }.toSet(),
        )
        assertEquals(
            mapOf("E031" to 3, "E032" to 1, "E033" to 4, "E034" to 2, "E035" to 1,
                "E036" to 1, "E037" to 3, "E038" to 1, "E039" to 7),
            VocabularyP1Content.tasks.groupingBy { it.formatId }.eachCount(),
        )

        VocabularyP1Content.tasks.forEach { task ->
            val exercise = exercise(task.id)
            val source = assertNotNull(exercise.inspector.vocabularySource)
            assertEquals(task.id, source.taskId)
            assertEquals(task.formatId, source.operation)
            assertEquals("pilot-18", exercise.contentRevision)
            assertEquals("generator-18", exercise.generatorRevision)
            assertContractedTargetFamily(exercise)
            val evaluation = engine.evaluate(exercise, correctAnswer(exercise))
            assertEquals(TargetStatus.MET, evaluation.targetStatus, task.id)
            assertEquals(MeaningSatisfaction.MET, evaluation.meaningSatisfaction, task.id)
            assertEquals(ResponseConstraintStatus.MET, evaluation.responseConstraintStatus, task.id)
            assertEquals(expectedScope(task.formatId), evaluation.linguisticScope, task.id)
        }
    }

    @Test
    fun `typed packet recall uses isolated root-casefolded words without prefix acceptance`() {
        val study = exercise("p1-3.4")
        val exact = engine.evaluate(study, Answer.Text("  STUDERER  "))
        assertEquals(TargetStatus.MET, exact.targetStatus)
        assertEquals(ResponseConstraintStatus.MET, exact.responseConstraintStatus)
        assertEquals("STUDERER", (exact.normalizedAnswer as NormalizedAnswer.Text).text)

        val infinitive = engine.evaluate(study, Answer.Text("studere"))
        assertEquals(TargetStatus.NOT_MET, infinitive.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, infinitive.linguisticStatus)
        assertEquals(MeaningSatisfaction.MET, infinitive.meaningSatisfaction)
        assertEquals(ResponseConstraintStatus.MET, infinitive.responseConstraintStatus)
        assertEquals("INFINITIVE_USED", infinitive.issues.single().code)

        val prose = engine.evaluate(study, Answer.Text("studerer nu"))
        assertEquals(TargetStatus.NOT_MET, prose.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, prose.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, prose.responseConstraintStatus)

        val knownBundle = engine.evaluate(study, Answer.Text("et kursus"))
        assertEquals(LinguisticStatus.ACCEPTABLE, knownBundle.linguisticStatus)
        assertEquals(MeaningSatisfaction.NOT_MET, knownBundle.meaningSatisfaction)
        assertEquals(ResponseConstraintStatus.NOT_MET, knownBundle.responseConstraintStatus)
        assertEquals("ONE_WORD_RESPONSE_REQUIRED", knownBundle.issues.single().code)
        val derivedHeadword = engine.evaluate(study, Answer.Text("bog"))
        assertEquals(LinguisticStatus.UNKNOWN, derivedHeadword.linguisticStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, derivedHeadword.meaningSatisfaction)
        val frozenAlternatives = assertNotNull(study.inspector.vocabularySource).knownAlternatives
        assertTrue(frozenAlternatives.any { it.text == "et kursus" && it.responseConstraintStatus == ResponseConstraintStatus.NOT_MET })
        assertTrue(frozenAlternatives.none { it.text == "bog" || it.text == "vand" || it.text == "hjemme" })

        val water = engine.evaluate(exercise("p1-4.5"), Answer.Text("en vand"))
        assertEquals(TargetStatus.NOT_MET, water.targetStatus)
        assertEquals(MeaningSatisfaction.UNKNOWN, water.meaningSatisfaction)
        assertEquals(LinguisticStatus.UNKNOWN, water.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, water.responseConstraintStatus)
    }

    @Test
    fun `P1 evaluators reject answers outside each supplied response shape`() {
        val choice = exercise("p1-1.1")
        val arbitraryChoice = engine.evaluate(choice, Answer.Text("not a supplied choice"))
        assertEquals(TargetStatus.UNEVALUABLE, arbitraryChoice.targetStatus)
        assertEquals("SUPPLIED_CHOICE_REQUIRED", arbitraryChoice.issues.single().code)
        assertEquals(LinguisticScope.ENGLISH_CHOICE, arbitraryChoice.linguisticScope)
        val wrongChoiceShape = engine.evaluate(choice, Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, wrongChoiceShape.targetStatus)
        assertEquals("SUPPLIED_CHOICE_REQUIRED", wrongChoiceShape.issues.single().code)

        val danishChoice = exercise("p1-2.2")
        val unknownDanishChoice = engine.evaluate(danishChoice, Answer.Text("ukendt"))
        assertEquals(TargetStatus.UNEVALUABLE, unknownDanishChoice.targetStatus)
        assertEquals(LinguisticScope.DANISH_CHOICE, unknownDanishChoice.linguisticScope)

        val recall = engine.evaluate(exercise("p1-3.4"), Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, recall.targetStatus)
        assertEquals("TEXT_RESPONSE_REQUIRED", recall.issues.single().code)

        val matching = exercise("p1-4.1")
        val columns = assertNotNull(matching.prompt.matchingColumns)
        val firstLeft = columns.left.first().id
        val firstRight = columns.right.first().id
        val secondRight = columns.right[1].id
        listOf(
            List(4) { MatchingPair(firstLeft, firstRight) },
            listOf(MatchingPair(firstLeft, firstRight), MatchingPair(firstLeft, secondRight)),
            listOf(MatchingPair(columns.left[0].id, firstRight), MatchingPair(columns.left[1].id, firstRight)),
            listOf(MatchingPair("not-supplied", firstRight)),
            listOf(MatchingPair(firstLeft, "not-supplied")),
        ).forEach { pairs ->
            val invalid = engine.evaluate(matching, Answer.Matching(pairs))
            assertEquals(TargetStatus.UNEVALUABLE, invalid.targetStatus)
            assertEquals("MATCHING_SELECTION_INVALID", invalid.issues.single().code)
            assertEquals(LinguisticScope.DANISH_MATCHING, invalid.linguisticScope)
        }
        val wrongMatchingShape = engine.evaluate(matching, Answer.Text("not matching pairs"))
        assertEquals(TargetStatus.UNEVALUABLE, wrongMatchingShape.targetStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, wrongMatchingShape.responseConstraintStatus)

        val tiles = exercise("p1-4.3")
        val firstTile = tiles.prompt.blocks.first().id
        listOf(
            Answer.Order(listOf(firstTile, firstTile)),
            Answer.Order(listOf("not-supplied")),
        ).forEach { answer ->
            val invalid = engine.evaluate(tiles, answer)
            assertEquals(TargetStatus.UNEVALUABLE, invalid.targetStatus)
            assertEquals("TILE_SELECTION_INVALID", invalid.issues.single().code)
        }
        val wrongTileShape = engine.evaluate(tiles, Answer.Text("not tile IDs"))
        assertEquals(TargetStatus.UNEVALUABLE, wrongTileShape.targetStatus)
        assertEquals("REQUIRED_WORDS_MISSING", wrongTileShape.issues.single().code)
    }

    @Test
    fun `P1 practice target keys enforce closed grammar lexical reading and matching families`() {
        val meaning = LexicalTarget("L041", "write-with-tool", "meaning", "en-da")
        assertEquals("lexical:L041:write-with-tool:meaning:en-da:matching",
            PracticeTargetKeys.key(null, meaning, assessmentMode = "matching"))
        assertEquals("reading:explicit-fact:object",
            PracticeTargetKeys.key(null, null, readingTarget = ReadingTask("explicit-fact"), readingRole = "object"))
        assertEquals("matching:meaning:en-da",
            PracticeTargetKeys.key(null, null, matchingTarget = LexicalTask("meaning", "en-da")))

        assertFailsWith<IllegalArgumentException> { PracticeTargetKeys.key(null, null) }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key("G004", meaning)
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key(null, null, targetScopeId = "unexpected",
                matchingTarget = LexicalTask("meaning", "en-da"))
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key(null, null, matchingTarget = LexicalTask("form", "da-en"))
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key(null, null, readingTarget = ReadingTask("explicit-fact"))
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key(null, null, assessmentMode = "matching",
                readingTarget = ReadingTask("explicit-fact"), readingRole = "object")
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key(null, meaning, targetScopeId = "unexpected")
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key(null, meaning.copy(facet = "form"), assessmentMode = "matching")
        }
    }

    @Test
    fun `matching supports three and four pairs and grades every lexical identity`() {
        listOf("p1-2.1" to 4, "p1-4.1" to 3).forEach { (taskId, count) ->
            val exercise = exercise(taskId)
            val source = assertNotNull(exercise.inspector.vocabularySource)
            assertEquals(count, source.matchingPairs.size)
            assertTrue(exercise.prompt.matchingColumns!!.left.none { it.id.matches(Regex("l\\d+")) })
            assertTrue(exercise.prompt.matchingColumns!!.right.none { it.id.matches(Regex("r\\d+")) })
            val correct = source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) }
            val evaluation = engine.evaluate(exercise, Answer.Matching(correct))
            assertEquals(TargetStatus.MET, evaluation.targetStatus)
            assertEquals(count, assertNotNull(evaluation.matchingResults).size)
            assertTrue(assertNotNull(evaluation.matchingResults).all { it.targetStatus == TargetStatus.MET })

            val wrong = correct.toMutableList()
            if (wrong.size >= 2) {
                wrong[0] = MatchingPair(wrong[0].leftId, correct[1].rightId)
                wrong[1] = MatchingPair(wrong[1].leftId, correct[0].rightId)
            }
            val wrongEvaluation = engine.evaluate(exercise, Answer.Matching(wrong))
            assertEquals(TargetStatus.NOT_MET, wrongEvaluation.targetStatus)
            assertEquals(ResponseConstraintStatus.MET, wrongEvaluation.responseConstraintStatus)

            val partial = engine.evaluate(exercise, Answer.Matching(correct.dropLast(1)))
            assertEquals(TargetStatus.UNEVALUABLE, partial.targetStatus)
            assertEquals(ResponseConstraintStatus.NOT_MET, partial.responseConstraintStatus)
        }
    }

    @Test
    fun `reading answers come from the targeted structured fact and expose every visible fact`() {
        val reading = exercise("p1-2.3")
        val source = assertNotNull(reading.inspector.vocabularySource)
        assertNull(source.expectedAnswer)
        assertEquals("p1-s2-f1", source.targetSupportFactId)
        assertEquals("object", source.questionRole)
        assertEquals(listOf("p1-s2-f1", "p1-s2-f2", "p1-s2-f3"), source.supportFacts.map { it.factId })
        assertEquals("a written letter", source.supportFacts.single { it.factId == source.targetSupportFactId }.valueText)
        assertTrue(source.supportFacts.flatMap { it.exposureKeys }.containsAll(listOf(
            "vocabulary-p1:form:L041:skriver",
            "vocabulary-p1:bundle:L018:et brev",
            "vocabulary-p1:form:L009:har",
            "vocabulary-p1:bundle:L042:en computer",
            "vocabulary-p1:form:L011:læser",
            "vocabulary-p1:bundle:L015:en avis",
        )))
        assertEquals(TargetStatus.MET, engine.evaluate(reading, Answer.Text("a written letter")).targetStatus)
        assertEquals(TargetStatus.NOT_MET, engine.evaluate(reading, Answer.Text("a newspaper")).targetStatus)

        val plan = PracticePlanner(engine).plan("vocabulary-p1-s2", "vocabulary-p1-s2-1", 11)
        val readingItem = plan.items.single { assertNotNull(it.exercise.inspector.vocabularySource).taskId == "p1-2.3" }
        assertTrue(readingItem.promptExposureKeys.containsAll(source.supportFacts.flatMap { it.exposureKeys }))
    }

    @Test
    fun `tiles distinguish exact fronting infinitive and unknown order cases`() {
        val reading = exercise("p1-3.3")
        assertTrue(reading.prompt.blocks.none { it.id.matches(Regex("b\\d+")) })
        assertFalse(reading.prompt.blocks.map { it.id } ==
            assertNotNull(reading.inspector.vocabularySource).acceptedOrders.single())
        val readingByText = reading.prompt.blocks.associateBy { it.text }
        val fronted = listOf("nu", "læser", "hun", "en", "bog").map { readingByText.getValue(it).id }
        val frontedEvaluation = engine.evaluate(reading, Answer.Order(fronted))
        assertEquals(TargetStatus.NOT_MET, frontedEvaluation.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, frontedEvaluation.linguisticStatus)
        assertEquals(MeaningSatisfaction.MET, frontedEvaluation.meaningSatisfaction)
        assertEquals(ResponseConstraintStatus.NOT_MET, frontedEvaluation.responseConstraintStatus)
        assertEquals("REQUESTED_ORDER_CONSTRAINT", frontedEvaluation.issues.single().code)

        val drink = exercise("p1-4.3")
        val drinkByText = drink.prompt.blocks.associateBy { it.text }
        val infinitive = listOf("Nora", "drikke", "vand").map { drinkByText.getValue(it).id }
        val infinitiveEvaluation = engine.evaluate(drink, Answer.Order(infinitive))
        assertEquals(TargetStatus.NOT_MET, infinitiveEvaluation.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, infinitiveEvaluation.linguisticStatus)
        assertEquals(ResponseConstraintStatus.MET, infinitiveEvaluation.responseConstraintStatus)

        val unsupported = listOf("Nora", "vand", "drikker").map { drinkByText.getValue(it).id }
        val unknownEvaluation = engine.evaluate(drink, Answer.Order(unsupported))
        assertEquals(TargetStatus.UNEVALUABLE, unknownEvaluation.targetStatus)
        assertEquals(ResponseConstraintStatus.MET, unknownEvaluation.responseConstraintStatus)

        val all = drink.prompt.blocks.map { it.id }
        assertEquals(ResponseConstraintStatus.NOT_MET,
            engine.evaluate(drink, Answer.Order(all)).responseConstraintStatus)
    }

    @Test
    fun `P1 grammar operations export every literal scope for fingerprinting`() {
        val expected = mapOf(
            "p1-3.4" to "p1-studere-present",
            "p1-4.4" to "p1-drikke-present",
            "p1-3.3" to "p1-subject-first-supplied",
            "p1-5.4" to "p1-subject-first-supplied",
            "p1-5.5" to "p1-subject-first-supplied",
            "p1-4.3" to "p1-drikke-present",
        )
        expected.forEach { (taskId, scopeId) ->
            val generated = exercise(taskId)
            assertEquals(scopeId, generated.targetScopeId, taskId)
            assertEquals(scopeId, (generated.source as VocabularyP1ExerciseSource).task.targetScopeId, taskId)
        }
    }

    @Test
    fun `five profiles require teaching and retain literal item order`() {
        val planner = PracticePlanner(engine)
        val profiles = planner.profiles().filter { it.focusId.startsWith("vocabulary-p1-") }
        val expectedTaskIds = listOf(
            listOf("p1-1.1", "p1-1.2", "p1-1.3", "p1-1.4"),
            listOf("p1-2.1", "p1-2.2", "p1-2.3", "p1-2.4"),
            listOf("p1-3.1", "p1-3.2", "p1-3.3", "p1-3.4", "p1-3.5"),
            listOf("p1-4.1", "p1-4.2", "p1-4.3", "p1-4.4", "p1-4.5"),
            listOf("p1-5.1", "p1-5.2", "p1-5.3", "p1-5.4", "p1-5.5"),
        )
        val plannedTaskIds = VocabularyP1PracticePlanner.SPECIFICATIONS.map { it.taskIds }
        assertEquals(expectedTaskIds, plannedTaskIds)
        val coverage = plannedTaskIds.flatten()
        assertEquals(23, coverage.size)
        assertEquals(coverage.size, coverage.distinct().size)
        assertEquals(VocabularyP1Content.tasks.map { it.id }.toSet(), coverage.toSet())
        assertEquals(listOf(4, 4, 5, 5, 5), profiles.map { it.itemCount })
        assertEquals(listOf(
            "Work, reading and today", "Reading and writing", "Study, courses and Danish", "Coffee and water", "Where and when",
        ), profiles.map { it.title })
        VocabularyP1PracticePlanner.SPECIFICATIONS.forEach { specification ->
            val plan = planner.plan(specification.focusId, specification.planRevision, 19)
            assertTrue(plan.introductionRequired)
            assertTrue(assertNotNull(plan.introductionExposureKeys).isNotEmpty())
            assertEquals(PracticeCompletionPolicy.VOCABULARY_P1_RULE, plan.completionRuleId)
            assertEquals(PracticeFeedbackExposureCalculator.VOCABULARY_P1_EXPOSURE_POLICY, plan.exposurePolicyId)
            assertEquals(specification.taskIds,
                plan.items.map { assertNotNull(it.exercise.inspector.vocabularySource).taskId })
            plan.items.forEach { item ->
                val source = assertNotNull(item.exercise.inspector.vocabularySource)
                val mappings = source.matchingPairs.map {
                    VocabularyP1PracticePlanner.mappingKey(it.lexicalSource.entryId, it.lexicalSource.senseId)
                }
                assertEquals(source.lexicalSources.flatMap(::lessonKeys).distinct(),
                    item.lessonExposureKeys.filterNot { it.startsWith("vocabulary-p1:mapping:") }, source.taskId)
                assertEquals(mappings.toSet(),
                    item.lessonExposureKeys.filter { it.startsWith("vocabulary-p1:mapping:") }.toSet(), source.taskId)
            }
        }
        val sessionOne = planner.plan("vocabulary-p1-s1", "vocabulary-p1-s1-1", 19)
        assertTrue(sessionOne.items.flatMap { it.lessonExposureKeys }.none {
            it.contains("L045") || it.contains("L047") || it.contains("L049")
        })
        val allIntroduction = VocabularyP1PracticePlanner.SPECIFICATIONS.flatMap { it.introduction }
        listOf(
            "at arbejde → arbejder",
            "at læse → læser",
            "at skrive → skriver",
            "at studere → studerer",
            "at drikke → drikker",
        ).forEach { body -> assertTrue(allIntroduction.any { it.body == body && it.bodyLanguage == "da" }, body) }
        val sessionFive = VocabularyP1PracticePlanner.SPECIFICATIONS.single { it.focusId == "vocabulary-p1-s5" }
        assertTrue(sessionFive.introduction.any { it.body == "i Aarhus / i Odense" && it.bodyLanguage == "da" })
        assertTrue(VocabularyP1PracticePlanner.formKey("L032", "i Odense") in sessionFive.introductionKeys)
        assertTrue(VocabularyP1PracticePlanner.formKey("L027", "arbejder") in sessionFive.introductionKeys)
        assertTrue(VocabularyP1PracticePlanner.formKey("L043", "studerer") in sessionFive.introductionKeys)
        VocabularyP1PracticePlanner.SPECIFICATIONS.forEachIndexed { index, specification ->
            val visibleFacts = VocabularyP1Content.tasks.filter { it.id.startsWith("p1-${index + 1}.") }
                .flatMap(VocabularyP1Task::supportFacts).distinctBy(VocabularyP1Fact::id)
            val required = visibleFacts.flatMap {
                it.exposureKeys + VocabularyP1PracticePlanner.factKey(it.id)
            }
            assertTrue(specification.introductionKeys.containsAll(required), specification.focusId)
            val matchingMappings = VocabularyP1Content.tasks.filter {
                it.id in specification.taskIds && it.kind == VocabularyP1TaskKind.MATCHING
            }.flatMap(VocabularyP1Task::targets).map {
                VocabularyP1PracticePlanner.mappingKey(it.entryId, it.senseId)
            }
            assertTrue(specification.introductionKeys.containsAll(matchingMappings), specification.focusId)
        }
        val matchingItem = planner.plan("vocabulary-p1-s2", "vocabulary-p1-s2-1", 19).items.first()
        assertEquals(LexicalTask("meaning", "en-da"), matchingItem.targetDescriptor.matchingTarget)
        assertNull(matchingItem.targetDescriptor.lexicalTarget)
        assertEquals("matching:meaning:en-da", matchingItem.targetDescriptor.targetKey())
        assertTrue(matchingItem.assessments.all {
            it.targetDescriptor.assessmentMode == "matching" && it.targetDescriptor.targetKey().endsWith(":matching")
        })
    }

    @Test
    fun `prompt exposure records only exact visible Danish surfaces`() {
        VocabularyP1PracticePlanner.SPECIFICATIONS.forEach { specification ->
            val plan = PracticePlanner(engine).plan(specification.focusId, specification.planRevision, 19)
            plan.items.forEach { item ->
                val task = VocabularyP1Content.tasks.single {
                    it.id == assertNotNull(item.exercise.inspector.vocabularySource).taskId
                }
                assertPromptExposure(task, item)
            }
        }
    }

    @Test
    fun `P1 completion uses latest saved teaching and complete matching outcomes`() {
        val correct = completion(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        assertTrue(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE, correct))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            correct.copy(teachingSaved = false)))
        assertTrue(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            completion(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)))
        assertTrue(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            completion(TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.MET)))
        assertTrue(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            completion(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.NOT_MET)
                .copy(issueCodes = listOf("REQUESTED_ORDER_CONSTRAINT"))))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            completion(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.NOT_MET)
                .copy(issueCodes = listOf("REQUIRED_WORDS_MISSING"))))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            completion(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET)))

        val board = exercise("p1-4.1")
        val completeEvaluation = engine.evaluate(board, Answer.Matching(
            assertNotNull(board.inspector.vocabularySource).matchingPairs.map { MatchingPair(it.leftId, it.rightId) },
        ))
        val matching = correct.copy(matchingResults = assertNotNull(completeEvaluation.matchingResults))
        assertTrue(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE, matching))
        val completeRows = matching.matchingResults
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            matching.copy(matchingResults = completeRows + completeRows.take(2))))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            matching.copy(matchingResults = completeRows.mapIndexed { index, row ->
                if (index == 0) row.copy(targetStatus = TargetStatus.UNEVALUABLE) else row
            })))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            matching.copy(matchingResults = completeRows.mapIndexed { index, row ->
                if (index == 0) row.copy(linguisticStatus = LinguisticStatus.KNOWN_ISSUE) else row
            })))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            matching.copy(matchingResults = completeRows.mapIndexed { index, row ->
                if (index == 0) row.copy(responseConstraintStatus = ResponseConstraintStatus.NOT_MET) else row
            })))
        assertFalse(PracticeCompletionPolicy.canAdvance(PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            matching.copy(matchingResults = matching.matchingResults.dropLast(1))))
    }

    @Test
    fun `matching feedback releases only mappings confirmed correct`() {
        val plan = PracticePlanner(engine).plan("vocabulary-p1-s4", "vocabulary-p1-s4-1", 7)
        val item = plan.items.first()
        val board = item.exercise
        val pairs = assertNotNull(board.inspector.vocabularySource).matchingPairs
        val response = listOf(MatchingPair(pairs[0].leftId, pairs[0].rightId))
        val evaluation = engine.evaluate(board, Answer.Matching(response))
        val released = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(PracticeFeedbackAttemptObservation(
                evaluation.targetStatus,
                linguisticStatus = evaluation.linguisticStatus,
                responseConstraintStatus = evaluation.responseConstraintStatus,
                matchingResults = assertNotNull(evaluation.matchingResults),
            )),
            plan.exposurePolicyId,
        )
        assertEquals(item.assessments.first().answerExposureKeys, released)
    }

    @Test
    fun `known wrong feedback never releases a hidden expected answer`() {
        val plan = PracticePlanner(engine).plan("vocabulary-p1-s3", "vocabulary-p1-s3-1", 7)
        listOf("p1-3.2", "p1-3.3", "p1-3.4", "p1-3.5").forEach { taskId ->
            val item = plan.items.single { assertNotNull(it.exercise.inspector.vocabularySource).taskId == taskId }
            val answer = when (taskId) {
                "p1-3.2" -> Answer.Text("a newspaper")
                "p1-3.3" -> {
                    val byText = item.exercise.prompt.blocks.associateBy { it.text }
                    Answer.Order(listOf("nu", "læser", "hun", "en", "bog").map { byText.getValue(it).id })
                }
                "p1-3.4" -> Answer.Text("studere")
                else -> Answer.Text("dansk")
            }
            val evaluation = engine.evaluate(item.exercise, answer)
            val released = PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                item.assessments,
                listOf(PracticeFeedbackAttemptObservation(
                    evaluation.targetStatus,
                    linguisticStatus = evaluation.linguisticStatus,
                    responseConstraintStatus = evaluation.responseConstraintStatus,
                    issueCodes = evaluation.issues.map { it.code },
                    meaningSatisfaction = evaluation.meaningSatisfaction,
                )),
                plan.exposurePolicyId,
            )
            assertTrue(released.isEmpty(), taskId)
        }
    }

    @Test
    fun `matching summary projects atomic lexical identities and meaning outcomes`() {
        val plan = PracticePlanner(engine).plan("vocabulary-p1-s4", "vocabulary-p1-s4-1", 7)
        val board = plan.items.first()
        val exercise = board.exercise
        val pairs = assertNotNull(exercise.inspector.vocabularySource).matchingPairs
        val evaluation = engine.evaluate(exercise, Answer.Matching(pairs.map { MatchingPair(it.leftId, it.rightId) }))
        val emptyContext = PracticeAttemptContext(false, false, false, emptyList())
        val observations = plan.items.map { item ->
            PracticeSummaryItemObservation(
                item,
                advanced = false,
                lessonOpened = false,
                hintOpened = false,
                attempts = if (item.position == board.position) listOf(
                    PracticeSummaryAttemptObservation(
                        evaluation.targetStatus,
                        evaluation.linguisticStatus,
                        evaluation.responseConstraintStatus,
                        emptyContext,
                        meaningSatisfaction = evaluation.meaningSatisfaction,
                        matchingResults = assertNotNull(evaluation.matchingResults),
                    ),
                ) else emptyList(),
            )
        }
        val matchingRows = PracticeSummaryCalculator.calculate(plan, false, observations).rows
            .filter { it.assessmentMode == "matching" }
        assertEquals(3, matchingRows.size)
        assertEquals(pairs.map { it.lexicalSource.senseId }.toSet(),
            matchingRows.map { assertNotNull(it.lexicalTarget).senseId }.toSet())
        assertTrue(matchingRows.all { it.first.meaningMet == 1 && it.first.fullyMet == 1 })
        assertTrue(matchingRows.none { it.matchingTarget != null })
    }

    @Test
    fun `all five P1 profiles complete every saved item and summarize ordinary lexical targets`() {
        VocabularyP1PracticePlanner.SPECIFICATIONS.forEach { specification ->
            val plan = PracticePlanner(engine).plan(specification.focusId, specification.planRevision, 23)
            val observations = plan.items.map { item ->
                val evaluation = engine.evaluate(item.exercise, correctAnswer(item.exercise))
                val feedbackObservation = PracticeFeedbackAttemptObservation(
                    evaluation.targetStatus,
                    linguisticStatus = evaluation.linguisticStatus,
                    responseConstraintStatus = evaluation.responseConstraintStatus,
                    issueCodes = evaluation.issues.map(EvaluationIssue::code),
                    meaningSatisfaction = evaluation.meaningSatisfaction,
                    matchingResults = evaluation.matchingResults.orEmpty(),
                )
                assertEquals(item.feedbackExposureKeys, PracticeFeedbackExposureCalculator.releasedKeys(
                    item.feedbackExposureKeys,
                    item.assessments,
                    listOf(feedbackObservation),
                    plan.exposurePolicyId,
                ), assertNotNull(item.exercise.inspector.vocabularySource).taskId)
                PracticeSummaryItemObservation(
                    item,
                    advanced = true,
                    lessonOpened = false,
                    hintOpened = false,
                    attempts = listOf(PracticeSummaryAttemptObservation(
                        evaluation.targetStatus,
                        evaluation.linguisticStatus,
                        evaluation.responseConstraintStatus,
                        PracticeAttemptContext(true, false, false, emptyList()),
                        meaningSatisfaction = evaluation.meaningSatisfaction,
                        matchingResults = evaluation.matchingResults.orEmpty(),
                    )),
                )
            }
            val summary = PracticeSummaryCalculator.calculate(plan, true, observations)
            assertEquals(plan.items.size, summary.attemptedItems, specification.focusId)
            assertEquals(plan.items.size, summary.completedItems, specification.focusId)
            assertTrue(summary.rows.all { it.first.targetMet == it.plannedItems }, specification.focusId)
            assertTrue(summary.rows.all { it.first.unevaluable == 0 }, specification.focusId)
        }
    }

    @Test
    fun `matching teaching mappings project introduction lesson and source prior exposure atomically`() {
        val plan = PracticePlanner(engine).plan("vocabulary-p1-s4", "vocabulary-p1-s4-1", 29)
        val board = plan.items.first()
        val mappingKeys = board.assessments.flatMap(PracticeAssessment::answerExposureKeys)
        assertTrue(assertNotNull(plan.introductionExposureKeys).containsAll(mappingKeys))
        assertTrue(board.lessonExposureKeys.containsAll(mappingKeys))
        assertTrue(board.promptExposureKeys.none { it.startsWith("vocabulary-p1:mapping:") })

        val untouched = plan.items.map { item ->
            PracticeItemExposureObservation(item, promptReleased = false, lessonOpened = false,
                hintOpened = false, feedbackReleased = false)
        }
        val introductionContext = PracticeAttemptContextCalculator.calculate(
            board,
            introductionOpened = true,
            observations = untouched,
            introductionExposureKeys = plan.introductionExposureKeys,
        )
        val lessonContext = PracticeAttemptContextCalculator.calculate(
            board,
            introductionOpened = false,
            observations = untouched.map { if (it.item.position == board.position) it.copy(lessonOpened = true) else it },
            introductionExposureKeys = plan.introductionExposureKeys,
        )
        val sourceContext = PracticeAttemptContextCalculator.calculate(
            board,
            introductionOpened = false,
            observations = untouched,
            sourceExposure = listOf(PracticeCrossRunExposure(
                "prior-p1-s4",
                board.position,
                PracticeExposureSource.LESSON,
                board.lessonExposureKeys,
            )),
            introductionExposureKeys = plan.introductionExposureKeys,
        )
        assertEquals(mappingKeys.toSet(), introductionContext.priorExposure.flatMap { it.keys }.toSet())
        assertEquals(mappingKeys.toSet(), lessonContext.priorExposure.flatMap { it.keys }.toSet())
        assertEquals(mappingKeys.toSet(), sourceContext.sourceExposure.flatMap { it.keys }.toSet())

        val pairs = assertNotNull(board.exercise.inspector.vocabularySource).matchingPairs
        val evaluation = engine.evaluate(board.exercise,
            Answer.Matching(pairs.map { MatchingPair(it.leftId, it.rightId) }))
        listOf(introductionContext, lessonContext, sourceContext).forEach { context ->
            val observations = plan.items.map { item ->
                PracticeSummaryItemObservation(
                    item,
                    advanced = false,
                    lessonOpened = item.position == board.position && context.lessonOpened,
                    hintOpened = false,
                    attempts = if (item.position == board.position) listOf(PracticeSummaryAttemptObservation(
                        evaluation.targetStatus,
                        evaluation.linguisticStatus,
                        evaluation.responseConstraintStatus,
                        context,
                        meaningSatisfaction = evaluation.meaningSatisfaction,
                        matchingResults = assertNotNull(evaluation.matchingResults),
                    )) else emptyList(),
                )
            }
            val rows = PracticeSummaryCalculator.calculate(plan, context.introductionOpened, observations).rows
                .filter { it.assessmentMode == "matching" }
            assertEquals(3, rows.size)
            assertTrue(rows.all { it.firstWithPriorExposure == 1 })
        }
    }

    @Test
    fun `new lexical source types never encode legacy empty articles`() {
        VocabularyP1Content.placements.forEach { lexeme ->
            val source = lexeme.details()
            assertNotNull(source.kind)
            assertTrue(source.forms != VocabularyLexicalForms())
        }
        assertNull(exercise("p1-4.5").inspector.lexicalSource)
    }

    @Test
    fun `P1 review exports complete private source provenance without changing retained review`() {
        val retained = PilotReview.renderPilotSixteenMarkdown()
        val current = PilotReview.renderMarkdown()
        assertFalse("## P1 vocabulary unit: A day of work and study" in retained)
        assertTrue("## P1 vocabulary unit: A day of work and study" in current)
        VocabularyP1Content.tasks.forEach { assertTrue("`${it.id}`" in current, it.id) }
        VocabularyP1Content.newLexemes.forEach { assertTrue("`${it.entryId}`" in current, it.entryId) }
        assertTrue("(question target)" in current)
        assertTrue(VocabularyP1ReviewMetadata.records.all {
            it.sourceUrls.single().startsWith("https://ordregister.dk/") &&
                it.reviewState == PilotReviewMetadata.REVIEW_STATE
        })
    }

    private fun exercise(taskId: String): GeneratedExercise {
        val task = VocabularyP1Content.tasks.single { it.id == taskId }
        return (0 until VocabularyP1Content.tasks(task.formatId).size).asSequence().map { index ->
            engine.generate(GenerationRequest(task.formatId, task.targetSkillId, 11, index, "pilot-18", "generator-18"))
        }.first { assertNotNull(it.inspector.vocabularySource).taskId == taskId }
    }

    private fun correctAnswer(exercise: GeneratedExercise): Answer {
        val source = assertNotNull(exercise.inspector.vocabularySource)
        return when {
            source.matchingPairs.isNotEmpty() -> Answer.Matching(source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) })
            source.acceptedOrders.isNotEmpty() -> Answer.Order(source.acceptedOrders.single())
            source.targetSupportFactId != null -> Answer.Text(
                source.supportFacts.single { it.factId == source.targetSupportFactId }.valueText,
            )
            else -> Answer.Text(assertNotNull(source.expectedAnswer))
        }
    }

    private fun lessonKeys(source: VocabularyLexicalSourceDetails): List<String> = buildList {
        add(VocabularyP1PracticePlanner.meaningKey(source.entryId, source.senseId))
        source.forms.infinitive?.let { add(VocabularyP1PracticePlanner.formKey(source.entryId, it)) }
        source.forms.present?.let { add(VocabularyP1PracticePlanner.formKey(source.entryId, it)) }
        source.forms.bundle?.let { add(VocabularyP1PracticePlanner.bundleKey(source.entryId, it)) }
        source.forms.form?.let { add(VocabularyP1PracticePlanner.formKey(source.entryId, it)) }
        if (source.entryId == "L032") add(VocabularyP1PracticePlanner.formKey(source.entryId, "i Odense"))
    }

    private fun assertContractedTargetFamily(exercise: GeneratedExercise) {
        val presentFamilies = listOf(
            exercise.targetSkillId,
            exercise.lexicalTarget,
            exercise.readingTarget,
            exercise.lexicalTargets,
        ).count { it != null }
        assertEquals(1, presentFamilies, exercise.formatId)
        when (exercise.formatId) {
            "E031" -> assertEquals("da-en", assertNotNull(exercise.lexicalTarget).direction)
            "E032" -> assertEquals("en-da", assertNotNull(exercise.lexicalTarget).direction)
            "E033" -> assertTrue(assertNotNull(exercise.lexicalTargets).size in 3..4)
            "E034" -> assertEquals("G004", exercise.targetSkillId)
            "E035" -> assertEquals("fixed-chunk", assertNotNull(exercise.lexicalTarget).facet)
            "E036" -> assertEquals("mass-headword", assertNotNull(exercise.lexicalTarget).facet)
            "E037" -> assertEquals("G005", exercise.targetSkillId)
            "E038" -> assertEquals("G004", exercise.targetSkillId)
            "E039" -> assertEquals(ReadingTask("explicit-fact"), exercise.readingTarget)
        }
    }

    private fun expectedScope(formatId: String): LinguisticScope = when (formatId) {
        "E031", "E039" -> LinguisticScope.ENGLISH_CHOICE
        "E032" -> LinguisticScope.DANISH_CHOICE
        "E033" -> LinguisticScope.DANISH_MATCHING
        "E037", "E038" -> LinguisticScope.DANISH_SENTENCE
        else -> LinguisticScope.DANISH_WORD_FORM
    }

    private fun assertPromptExposure(task: VocabularyP1Task, item: PracticePlanItem) {
        when (task.formatId) {
            "E031" -> assertEquals(1, item.promptExposureKeys.size, task.id)
            "E032" -> assertEquals(task.choices.size, item.promptExposureKeys.size, task.id)
            "E033" -> {
                assertEquals(task.targets.size, item.promptExposureKeys.size, task.id)
                assertTrue(item.promptExposureKeys.none { it.endsWith(":skrive") || it.endsWith(":studere") ||
                    it.endsWith(":drikke") }, task.id)
            }
            "E034" -> assertEquals(
                listOf(VocabularyP1PracticePlanner.formKey(
                    task.targets.single().entryId,
                    assertNotNull(task.targets.single().forms.infinitive),
                )),
                item.promptExposureKeys,
                task.id,
            )
            "E035", "E036" -> assertTrue(item.promptExposureKeys.isEmpty(), task.id)
            "E037", "E038" -> assertEquals(task.tileWords.size, item.promptExposureKeys.size, task.id)
            "E039" -> {
                assertTrue(item.promptExposureKeys.containsAll(task.supportFacts.flatMap { it.exposureKeys }), task.id)
                assertTrue(item.promptExposureKeys.none { it.endsWith(":arbejde") || it.endsWith(":læse") ||
                    it.endsWith(":studere") || it.endsWith(":drikke") }, task.id)
            }
        }
    }

    private fun completion(target: TargetStatus, language: LinguisticStatus, constraint: ResponseConstraintStatus) =
        PracticeCompletionAttemptObservation(target, language, constraint, emptyList())
}
