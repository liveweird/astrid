package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VocabularyP1RevisionTwoPracticeCompatibilityTest {
    @Test
    fun `published 0_20_1 vocabulary P1 practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, VocabularyP1RevisionTwoPracticeCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers five literal plans seeds orders summaries evaluations and policies`() {
        val rows = rows()
        assertPlanCoverage(rows)
        assertEvaluationCoverage(rows)
        assertPartialMatchingFacets()
        assertPartialTileCoverage(rows)
    }

    private fun assertPlanCoverage(rows: List<List<String>>) {
        val plans = rows.filter { it[0] == "PLAN" }
        assertEquals(PROFILES, plans.map { it[1] }.toSet())
        PROFILES.forEach { profile ->
            assertEquals(SEEDS, plans.filter { it[1] == profile }.map { it[2].toInt() }.toSet())
            assertEquals(SUMMARY_CASES, rows.filter { it[0] == "SUMMARY" && it[1] == profile }.map { it[2] }.toSet())
        }
        val planned = LITERAL_PLAN_REVISIONS.mapValues { (focus, revision) ->
            PracticePlanner().plan(focus, revision, 47).items.map {
                requireNotNull(it.exercise.inspector.vocabularySource).taskId
            }
        }
        assertEquals(listOf(4, 4, 5, 5, 5), PROFILES.sorted().map { planned.getValue(it).size })
        assertEquals(LITERAL_PLAN_TASKS, planned)
    }

    private fun assertEvaluationCoverage(rows: List<List<String>>) {
        assertTrue(rows.any { it[0] == "EVALUATION" && it[4] == "unknown" && it[6] == "false" })
        assertTrue(rows.any { it[0] == "EVALUATION" && it[4] == "partial" && it[6] == "false" })
        assertKnownWrongCoverage(rows)
        assertTypedUnknownCoverage(rows)
        assertPartialMatchingCoverage(rows)
    }

    private fun assertKnownWrongCoverage(rows: List<List<String>>) {
        val knownWrong = rows.filter { it[0] == "EVALUATION" && it[4] == "known-wrong" }
        assertTrue(knownWrong.isNotEmpty())
        assertTrue(knownWrong.all {
            it[7] == "not-met"
        })
        assertTrue(knownWrong.filter { it[3] in setOf("p1-3.3", "p1-5.5") }.all {
            it[8] == "acceptable" && it[9] == "not-met" && it[10] == "met"
        })
        assertTrue(knownWrong.single { it[3] == "p1-4.3" }.let {
            it[8] == "known-issue" && it[9] == "met" && it[10] == "met"
        })
        val unsupportedOrder = rows.filter { it[0] == "EVALUATION" && it[4] == "unsupported-order-unknown" }
        assertTrue(unsupportedOrder.isNotEmpty())
        assertTrue(unsupportedOrder.all {
            it[7] == "unevaluable" && it[8] == "unknown"
        })
    }

    private fun assertTypedUnknownCoverage(rows: List<List<String>>) {
        val typedUnknown = rows.filter {
            it[0] == "EVALUATION" && it[3] in TYPED_TASK_IDS && it[4] == "unknown"
        }
        assertEquals(TYPED_TASK_IDS, typedUnknown.map { it[3] }.toSet())
        assertTrue(typedUnknown.all {
            it[6] == "false" && it[7] == "not-met" && it[8] == "unknown" && it[9] == "met" && it[10] == "unknown"
        })
    }

    private fun assertPartialMatchingCoverage(rows: List<List<String>>) {
        val validPartialMatching = rows.filter {
            it[0] == "EVALUATION" && it[3] in MATCHING_TASK_IDS && it[4] == "partial"
        }
        assertEquals(MATCHING_TASK_IDS, validPartialMatching.map { it[3] }.toSet())
        assertTrue(validPartialMatching.all {
            it[6] == "false" && it[7] == "unevaluable" && it[8] == "acceptable" &&
                it[9] == "not-met" && it[10] == "unknown"
        })
    }

    private fun assertPartialMatchingFacets() {
        VocabularyP1RevisionTwoPracticeCompatibilityFixture.profiles.drop(1).forEach { (focus, revision) ->
            val matchingItem = PracticePlanner().plan(focus, revision, 47).items.firstOrNull {
                it.exercise.formatId == "E033"
            } ?: return@forEach
            val correct = PilotEighteenCompatibilityGolden.canonicalAnswer(matchingItem.exercise) as Answer.Matching
            val evaluation = PilotEngine().evaluate(matchingItem.exercise, Answer.Matching(correct.pairs.dropLast(1)))
            assertTrue(evaluation.matchingResults.orEmpty().any {
                it.targetStatus == TargetStatus.UNEVALUABLE && it.linguisticStatus == LinguisticStatus.UNKNOWN &&
                    it.meaningSatisfaction == MeaningSatisfaction.UNKNOWN
            })
        }
    }

    private fun assertPartialTileCoverage(rows: List<List<String>>) {
        val validPartialTile = rows.filter {
            it[0] == "EVALUATION" && it[3] in TILE_TASK_IDS && it[4] == "partial"
        }
        assertEquals(TILE_TASK_IDS, validPartialTile.map { it[3] }.toSet())
        assertTrue(validPartialTile.all {
            it[6] == "false" && it[7] == "unevaluable" && it[8] == "unknown" && it[10] == "unknown"
        })
    }

    @Test
    fun `plans pin teaching policies complete descriptors private assessments and ordered exposure`() {
        VocabularyP1RevisionTwoPracticeCompatibilityFixture.profiles.forEach { (focus, revision) ->
            val plan = PracticePlanner().plan(focus, revision, 47)
            assertTrue(plan.introductionRequired)
            assertEquals("pilot-18", plan.contentRevision)
            assertEquals("generator-18", plan.generatorRevision)
            assertEquals("evaluation-18", plan.evaluationRevision)
            assertEquals("vocabulary-p1-outcome-1", plan.completionRuleId)
            assertEquals("vocabulary-p1-exposure-1", plan.exposurePolicyId)
            assertEquals(listOf("G001", "G002", "G003", "G004", "G005"), plan.prerequisiteSkillIds)
            assertTrue(plan.introduction.isNotEmpty())
            assertTrue(plan.introductionExposureKeys.orEmpty().isNotEmpty())
            plan.items.forEachIndexed { index, item ->
                assertEquals(index, item.position)
                assertTrue(item.answerExposureKeys.isNotEmpty())
                assertTrue(item.lessonExposureKeys.isNotEmpty() || item.exercise.formatId in setOf("E037", "E038"))
                assertTrue(item.assessments.isNotEmpty())
                assertTrue(item.assessments.all { it.assessmentRole == "primary" })
                assertTrue(item.targetDescriptor.targetKey().isNotEmpty())
            }
        }
        val allItems = VocabularyP1RevisionTwoPracticeCompatibilityFixture.profiles.flatMap { (focus, revision) ->
            PracticePlanner().plan(focus, revision, 47).items
        }
        assertEquals(setOf("object", "place", "time"), allItems.mapNotNull { it.targetDescriptor.readingRole }.toSet())
        assertTrue(allItems.filter { it.exercise.formatId == "E039" }.all {
            it.targetDescriptor.readingTarget == ReadingTask("explicit-fact")
        })
        assertTrue(allItems.filter { it.exercise.formatId == "E033" }.all {
            it.targetDescriptor.matchingTarget == LexicalTask("meaning", "en-da") &&
                it.assessments.size in 3..4 && it.assessments.all { assessment -> assessment.slotId != null }
        })
    }

    @Test
    fun `actual P1 feedback policy releases only confirmed matching mappings`() {
        val item = PracticePlanner().plan("vocabulary-p1-s2", "vocabulary-p1-s2-2", 47).items.first()
        val pairs = requireNotNull(item.exercise.inspector.vocabularySource).matchingPairs
        val correct = PilotEngine().evaluate(
            item.exercise,
            Answer.Matching(pairs.map { MatchingPair(it.leftId, it.rightId) }),
        )
        val rotated = pairs.map { it.rightId }.toMutableList().also {
            val first = it[0]
            it[0] = it[1]
            it[1] = first
        }
        val wrong = PilotEngine().evaluate(
            item.exercise,
            Answer.Matching(pairs.mapIndexed { index, pair -> MatchingPair(pair.leftId, rotated[index]) }),
        )
        fun released(evaluation: Evaluation) = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(
                PracticeFeedbackAttemptObservation(
                    evaluation.targetStatus, linguisticStatus = evaluation.linguisticStatus,
                    responseConstraintStatus = evaluation.responseConstraintStatus,
                    issueCodes = evaluation.issues.map { it.code }, meaningSatisfaction = evaluation.meaningSatisfaction,
                    matchingResults = evaluation.matchingResults.orEmpty(),
                ),
            ),
            "vocabulary-p1-exposure-1",
        )
        assertEquals(item.feedbackExposureKeys, released(correct))
        assertTrue(released(wrong).size < item.feedbackExposureKeys.size)
        val expectedCorrectMappings = wrong.matchingResults.orEmpty().filter { it.targetStatus == TargetStatus.MET }
            .map { result -> item.assessments.single { it.slotId == result.leftId }.answerExposureKeys.single() }
        assertEquals(expectedCorrectMappings.toSet(), released(wrong).toSet())
    }

    @Test
    fun `retry context contains only feedback actually released by the first attempt`() {
        val plan = PracticePlanner().plan("vocabulary-p1-s2", "vocabulary-p1-s2-2", 47)
        val item = plan.items.first()
        val contexts = VocabularyP1RevisionTwoPracticeCompatibilityFixture.retryContexts(plan, item)
        assertEquals(2, contexts.size)
        assertTrue(contexts.first().priorExposure.none { it.source == PracticeExposureSource.FEEDBACK })

        val pairs = requireNotNull(item.exercise.inspector.vocabularySource).matchingPairs
        val rotated = pairs.map { it.rightId }.toMutableList().also {
            val first = it[0]
            it[0] = it[1]
            it[1] = first
        }
        val firstEvaluation = PilotEngine().evaluate(
            item.exercise,
            Answer.Matching(pairs.mapIndexed { index, pair -> MatchingPair(pair.leftId, rotated[index]) }),
        )
        val expectedReleased = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(
                PracticeFeedbackAttemptObservation(
                    firstEvaluation.targetStatus, linguisticStatus = firstEvaluation.linguisticStatus,
                    responseConstraintStatus = firstEvaluation.responseConstraintStatus,
                    issueCodes = firstEvaluation.issues.map { it.code },
                    meaningSatisfaction = firstEvaluation.meaningSatisfaction,
                    matchingResults = firstEvaluation.matchingResults.orEmpty(),
                ),
            ),
            plan.exposurePolicyId,
        )
        val retryFeedback = contexts.last().priorExposure.single { it.source == PracticeExposureSource.FEEDBACK }
        assertEquals(item.position, retryFeedback.position)
        assertEquals(expectedReleased, retryFeedback.keys)
        assertTrue(retryFeedback.keys.isNotEmpty())
        assertTrue(retryFeedback.keys.size < item.feedbackExposureKeys.size)
    }

    @Test
    fun `completion freezes teaching complete matching known order and latest unknown boundaries`() {
        val completion = rows().filter { it[0] == "COMPLETION" }.associate { it[1] to it[2].toBooleanStrict() }
        assertEquals(
            mapOf(
                "missing" to false,
                "correct-three-pair" to true,
                "correct-four-pair" to true,
                "complete-known-wrong-four-pair" to true,
                "partial-matching" to false,
                "teaching-not-saved" to false,
                "unknown-latest" to false,
                "known-fronted-order" to true,
            ),
            completion,
        )
        PROFILES.forEach { profile ->
            assertTrue(rows().any {
                it[0] == "SUMMARY" && it[1] == profile && it[2] == "eligible-then-valid-nonsuccess"
            })
            assertTrue(rows().any { it[0] == "SUMMARY" && it[1] == profile && it[2] == "retry-correct" })
        }
    }

    @Test
    fun `P1 profiles do not enter existing follow up source allowlists`() {
        val item = PracticePlanner().plan("vocabulary-p1-s3", "vocabulary-p1-s3-2", 47).items.first()
        val source = PracticeFollowUpSourceItem(item.exercise, item.targetDescriptor, item.assessments)
        assertFailsWith<IllegalArgumentException> {
            PracticeFollowUpPlanner().planGuidedGaps("follow-up-3", 47, listOf(item.targetDescriptor.targetKey()), listOf(source))
        }
        assertFalse(item.targetDescriptor.targetKey().startsWith("grammar:G004:named-three-verbs-present"))
    }

    @Test
    fun `provenance pins published source all five plan pairs rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=9eefcfed708670ce45dbdd9b11adc41f9fe17d25" in provenance)
        assertTrue("releaseVersion=0.20.1" in provenance)
        assertTrue("captureDate=$CAPTURE_DATE" in provenance)
        assertTrue("projectionSchema=p1-revision-two-engine-v1" in provenance)
        (1..5).forEach { number ->
            assertTrue("vocabulary-p1-s$number-2:pilot-18/generator-18/evaluation-18" in provenance)
        }
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    @Test
    fun `export refuses fixture and provenance collisions independently`() {
        val fixtureDir = Files.createTempDirectory("vocabulary-p1-practice-fixture-collision")
        val fixture = Files.createFile(fixtureDir.resolve("fixture.tsv"))
        assertFailsWith<IllegalArgumentException> {
            VocabularyP1RevisionTwoPracticeCompatibilityFixture.write(fixture, fixtureDir.resolve("fixture.provenance"))
        }
        val provenanceDir = Files.createTempDirectory("vocabulary-p1-practice-provenance-collision")
        val provenance = Files.createFile(provenanceDir.resolve("fixture.provenance"))
        assertFailsWith<IllegalArgumentException> {
            VocabularyP1RevisionTwoPracticeCompatibilityFixture.write(provenanceDir.resolve("fixture.tsv"), provenance)
        }
    }

    private fun rows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/vocabulary-p1-practice-0.20.1.tsv"
        const val PROVENANCE_PATH = "golden/vocabulary-p1-practice-0.20.1.provenance"
        const val FIXTURE_ROW_COUNT = 261 // Captured from v0.20.1 in two fresh JVMs.
        const val FIXTURE_SHA256 = "e382a0f5e6512ed2c3a7f178553ab01b1c31df6ceb51a1f730d4db6968c10d4b"
        const val CAPTURE_DATE = "2026-11-12"
        val PROFILES = (1..5).map { "vocabulary-p1-s$it" }.toSet()
        val LITERAL_PLAN_REVISIONS = mapOf(
            "vocabulary-p1-s1" to "vocabulary-p1-s1-2",
            "vocabulary-p1-s2" to "vocabulary-p1-s2-2",
            "vocabulary-p1-s3" to "vocabulary-p1-s3-2",
            "vocabulary-p1-s4" to "vocabulary-p1-s4-2",
            "vocabulary-p1-s5" to "vocabulary-p1-s5-2",
        )
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
        val SUMMARY_CASES = setOf(
            "all-correct", "non-success", "retry-correct", "eligible-then-valid-nonsuccess",
            "partial-or-valid-nonsuccess",
        )
        val TYPED_TASK_IDS = setOf("p1-3.4", "p1-3.5", "p1-4.4", "p1-4.5")
        val MATCHING_TASK_IDS = setOf("p1-2.1", "p1-3.1", "p1-4.1", "p1-5.1")
        val TILE_TASK_IDS = setOf("p1-3.3", "p1-4.3", "p1-5.4", "p1-5.5")
        val LITERAL_PLAN_TASKS = mapOf(
            "vocabulary-p1-s1" to listOf("p1-1.1", "p1-1.2", "p1-1.3", "p1-1.4"),
            "vocabulary-p1-s2" to listOf("p1-2.1", "p1-2.2", "p1-2.3", "p1-2.4"),
            "vocabulary-p1-s3" to listOf("p1-3.1", "p1-3.2", "p1-3.3", "p1-3.4", "p1-3.5"),
            "vocabulary-p1-s4" to listOf("p1-4.1", "p1-4.2", "p1-4.3", "p1-4.4", "p1-4.5"),
            "vocabulary-p1-s5" to listOf("p1-5.1", "p1-5.2", "p1-5.3", "p1-5.4", "p1-5.5"),
        )
    }
}

