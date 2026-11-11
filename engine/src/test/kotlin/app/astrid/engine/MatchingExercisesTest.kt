package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MatchingExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `revision seven retains matching while appending multiple gaps`() {
        val catalog = engine.catalog()

        assertEquals("pilot-11", catalog.contentRevision)
        assertEquals("generator-11", catalog.generatorRevision)
        assertEquals("evaluation-6", engine.evaluationRevision("pilot-6", "generator-6"))
        assertEquals("evaluation-5", engine.evaluationRevision("pilot-5", "generator-5"))
        assertEquals((1..25).map { "E${it.toString().padStart(3, '0')}" }, catalog.formats.map { it.id })
        assertEquals(
            ExerciseFormat("E014", "Match English and Danish words", null, LexicalTask("meaning", "en-da")),
            catalog.formats.single { it.id == "E014" },
        )
        assertEquals(109, catalog.sentenceCount)
        assertEquals(13, PilotContent.pilotFiveSnapshot.formats.size)
        assertEquals(24, PilotContent.pilotFiveSnapshot.lexicalReview.size)
        assertEquals(4, PilotContent.pilotFiveSnapshot.vocabularySenses.size)
    }

    @Test
    fun `matching generation exposes opaque independently shuffled columns and private correspondences`() {
        val exercise = generate(47, 0)
        val columns = assertNotNull(exercise.prompt.matchingColumns)
        val source = assertNotNull(exercise.inspector.matchingSource)

        assertNull(exercise.lexicalTarget)
        assertNull(exercise.targetSkillId)
        assertNull(exercise.readingTarget)
        assertEquals(listOf("L014-S01", "L015-S01", "L018-S01", "L024-S01"), exercise.lexicalTargets?.map { it.senseId })
        assertEquals(listOf("l0", "l1", "l2", "l3"), columns.left.map { it.id })
        assertEquals(listOf("r0", "r1", "r2", "r3"), columns.right.map { it.id })
        assertEquals(
            setOf("book", "newspaper", "letter (written message)", "magazine (printed publication)"),
            columns.left.map { it.text }.toSet(),
        )
        assertEquals(setOf("en bog", "en avis", "et brev", "et magasin"), columns.right.map { it.text }.toSet())
        assertEquals("en", exercise.prompt.cueLanguage)
        assertEquals("da", exercise.prompt.answerLanguage)
        assertTrue(exercise.prompt.before.isEmpty() && exercise.prompt.after.isEmpty())
        assertTrue(exercise.prompt.choices.isEmpty() && exercise.prompt.blocks.isEmpty())
        assertEquals("", exercise.prompt.punctuation)
        assertEquals(columns.left.map { it.id }, source.pairs.map { it.leftId })
        assertEquals(4, source.pairs.map { it.rightId }.toSet().size)
        assertEquals(exercise.lexicalTargets?.map { it.senseId }?.toSet(), source.pairs.map { it.lexicalSource.senseId }.toSet())
        source.pairs.forEach { pair ->
            assertEquals(pair.lexicalSource.gloss, columns.left.single { it.id == pair.leftId }.text)
            assertEquals(
                "${pair.lexicalSource.article} ${pair.lexicalSource.headword}",
                columns.right.single { it.id == pair.rightId }.text,
            )
        }
    }

    @Test
    fun `matching generation is deterministic and varies both columns at seed and index boundaries`() {
        val requests = listOf(0, 1, Int.MAX_VALUE).flatMap { seed ->
            listOf(0, 1, 1_000_000).map { index -> seed to index }
        }
        val boards = requests.map { (seed, index) -> generate(seed, index) }

        boards.forEachIndexed { index, exercise ->
            assertEquals(exercise.prompt, generate(requests[index].first, requests[index].second).prompt)
        }
        assertTrue(boards.map { requireNotNull(it.prompt.matchingColumns).left.map(MatchingItem::text) }.toSet().size > 1)
        assertTrue(boards.map { requireNotNull(it.prompt.matchingColumns).right.map(MatchingItem::text) }.toSet().size > 1)
        assertTrue(boards.any { exercise ->
            val columns = requireNotNull(exercise.prompt.matchingColumns)
            val leftSenseOrder = columns.left.map { left -> englishSense.getValue(left.text) }
            val rightSenseOrder = columns.right.map { right -> danishSense.getValue(right.text) }
            leftSenseOrder != rightSenseOrder
        })
    }

    @Test
    fun `all 209 partial bijections and 24 complete mappings grade by meaning independent of pair order`() {
        val exercise = generate(47, 0)
        val source = assertNotNull(exercise.inspector.matchingSource)
        val leftIds = source.pairs.map { it.leftId }
        val rightIds = source.pairs.map { it.rightId }
        val selections = partialBijections(leftIds, rightIds)

        assertEquals(209, selections.size)
        assertEquals(24, selections.count { it.size == 4 })
        selections.forEach { pairs ->
            val evaluation = engine.evaluate(exercise, Answer.Matching(pairs))
            val anyWrong = pairs.any { source.pairs.single { expected -> expected.leftId == it.leftId }.rightId != it.rightId }
            val complete = pairs.size == 4
            assertEquals(pairs, (evaluation.rawAnswer as Answer.Matching).pairs)
            assertEquals(pairs, (evaluation.normalizedAnswer as NormalizedAnswer.Matching).pairs)
            val expectedTarget = if (anyWrong) TargetStatus.NOT_MET else if (complete) TargetStatus.MET else TargetStatus.UNEVALUABLE
            val expectedMeaning =
                if (anyWrong) MeaningSatisfaction.NOT_MET else if (complete) MeaningSatisfaction.MET else MeaningSatisfaction.UNKNOWN
            assertEquals(expectedTarget, evaluation.targetStatus)
            assertEquals(expectedMeaning, evaluation.meaningSatisfaction)
            assertEquals(
                if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
                evaluation.responseConstraintStatus,
            )
            assertEquals(if (pairs.isEmpty()) LinguisticStatus.UNKNOWN else LinguisticStatus.ACCEPTABLE, evaluation.linguisticStatus)
            assertEquals(LinguisticScope.DANISH_MATCHING, evaluation.linguisticScope)
            assertEquals("", evaluation.completedSentence)
            assertEquals(leftIds, evaluation.matchingResults?.map { it.leftId })

            val reversed = pairs.reversed()
            val reordered = engine.evaluate(exercise, Answer.Matching(reversed))
            assertEquals(evaluation.copy(rawAnswer = reordered.rawAnswer, normalizedAnswer = reordered.normalizedAnswer), reordered)
            assertEquals(reversed, (reordered.rawAnswer as Answer.Matching).pairs)
        }
    }

    @Test
    fun `pair results disclose selected IDs and targets but never expected IDs for wrong or unmatched rows`() {
        val exercise = generate(47, 0)
        val source = assertNotNull(exercise.inspector.matchingSource)
        val first = source.pairs[0]
        val wrongRight = source.pairs[1].rightId
        val evaluation = engine.evaluate(exercise, Answer.Matching(listOf(MatchingPair(first.leftId, wrongRight))))
        val results = assertNotNull(evaluation.matchingResults)
        val wrong = results.single { it.leftId == first.leftId }

        assertEquals(wrongRight, wrong.rightId)
        assertEquals(TargetStatus.NOT_MET, wrong.targetStatus)
        assertEquals(first.lexicalSource.senseId, wrong.lexicalTarget.senseId)
        assertFalse(first.rightId in wrong.feedback)
        results.filterNot { it.leftId == first.leftId }.forEach { unmatched ->
            assertNull(unmatched.rightId)
            assertEquals(TargetStatus.UNEVALUABLE, unmatched.targetStatus)
            assertFalse(source.pairs.single { it.leftId == unmatched.leftId }.rightId in unmatched.feedback)
        }
    }

    @Test
    fun `malformed identities duplicate selections excess pairs and wrong kinds fail conservatively`() {
        val exercise = generate(47, 0)
        val source = assertNotNull(exercise.inspector.matchingSource)
        val correct = source.pairs.map { MatchingPair(it.leftId, it.rightId) }
        val malformed = listOf<Answer>(
            Answer.Matching(listOf(MatchingPair("unknown", correct[0].rightId))),
            Answer.Matching(listOf(MatchingPair(correct[0].leftId, "unknown"))),
            Answer.Matching(listOf(correct[0], MatchingPair(correct[0].leftId, correct[1].rightId))),
            Answer.Matching(listOf(correct[0], MatchingPair(correct[1].leftId, correct[0].rightId))),
            Answer.Matching(correct + correct[0]),
            Answer.Text("book=en bog"),
            Answer.Order(listOf(correct[0].leftId, correct[0].rightId)),
        )

        malformed.forEach { answer ->
            val evaluation = engine.evaluate(exercise, answer)
            assertEquals(TargetStatus.UNEVALUABLE, evaluation.targetStatus)
            assertEquals(MeaningSatisfaction.UNKNOWN, evaluation.meaningSatisfaction)
            assertEquals(ResponseConstraintStatus.NOT_MET, evaluation.responseConstraintStatus)
            assertEquals(LinguisticStatus.UNKNOWN, evaluation.linguisticStatus)
            assertTrue(assertNotNull(evaluation.matchingResults).all { it.targetStatus == TargetStatus.UNEVALUABLE })
        }
    }

    @Test
    fun `retained five rejects matching and current six retains prior formats`() {
        val error = runCatching {
            engine.generate(GenerationRequest("E014", null, 0, 0, "pilot-5", "generator-5"))
        }.exceptionOrNull()

        assertNotNull(error)
        (1..13).forEach { number ->
            val format = "E${number.toString().padStart(3, '0')}"
            val target = engine.catalog().formats.single { it.id == format }.targetSkillId
            assertNotNull(engine.generate(GenerationRequest(format, target, 0, 0, "pilot-6", "generator-6")))
        }
        assertNotEquals(PilotReview.renderPilotFiveMarkdown(), PilotReview.renderMarkdown())
        assertTrue("Content revision: `pilot-5`" in PilotReview.renderPilotFiveMarkdown())
        assertTrue("| E014 |" in PilotReview.renderMarkdown())
        assertTrue("E011 and E014 only" in PilotReview.renderMarkdown())
        assertTrue("E011 only" in PilotReview.renderPilotFiveMarkdown())
    }

    private fun generate(seed: Int, index: Int) =
        engine.generate(GenerationRequest("E014", null, seed, index, "pilot-6", "generator-6"))

    private fun partialBijections(left: List<String>, right: List<String>): List<List<MatchingPair>> = buildList {
        for (size in 0..4) {
            combinations(left, size).forEach { selectedLeft ->
                combinations(right, size).forEach { selectedRight ->
                    permutations(selectedRight).forEach { orderedRight ->
                        add(selectedLeft.zip(orderedRight, ::MatchingPair))
                    }
                }
            }
        }
    }

    private fun <T> combinations(values: List<T>, size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList())
        values.size < size -> emptyList()
        else -> values.indices.flatMap { index ->
            combinations(values.drop(index + 1), size - 1).map { listOf(values[index]) + it }
        }
    }

    private fun <T> permutations(values: List<T>): List<List<T>> =
        if (values.isEmpty()) listOf(emptyList()) else values.indices.flatMap { index ->
            permutations(values.toMutableList().also { it.removeAt(index) }).map { listOf(values[index]) + it }
        }

    private companion object {
        val englishSense = mapOf(
            "book" to "L014-S01",
            "newspaper" to "L015-S01",
            "letter (written message)" to "L018-S01",
            "magazine (printed publication)" to "L024-S01",
        )
        val danishSense = mapOf(
            "en bog" to "L014-S01",
            "en avis" to "L015-S01",
            "et brev" to "L018-S01",
            "et magasin" to "L024-S01",
        )
    }
}
