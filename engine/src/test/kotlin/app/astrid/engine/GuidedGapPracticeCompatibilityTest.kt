package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GuidedGapPracticeCompatibilityTest {
    @Test
    fun `published 0_8_0 guided gap practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, GuidedGapPracticeCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers boundaries slot states exposure and first latest unknown`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "PLAN" }.map { it[1].toInt() }.toSet())
        assertEquals(6, rows.count { it[0] == "FOLLOW_UP" })
        assertEquals(1, rows.count { it[0] == "RECURSIVE_FOLLOW_UP" })
        assertEquals(
            (listOf("missing", "correct", "wrong").flatMap { left ->
                listOf("missing", "correct", "wrong").map { right -> left to right }
            }).toSet(),
            rows.filter { it[0] == "SLOTS" }.map { it[1] to it[2] }.toSet(),
        )
        assertEquals(1, rows.count { it[0] == "FIRST_LATEST" })
    }

    @Test
    fun `follow up three still rejects invalid revision target and source pair`() {
        val plan = PracticePlanner().plan("verb-article-gaps", "verb-article-gaps-1", 0)
        val source = plan.items.map { PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments) }
        val planner = PracticeFollowUpPlanner()
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps("follow-up-2", 0, listOf("grammar:G004:selection"), source)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps("follow-up-3", 0, listOf("grammar:G006:recall"), source)
        }
        val pairThree = PracticePlanner().plan("present-verbs", "present-verbs-1", 0).items.first()
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3",
                0,
                listOf("grammar:G004:selection"),
                listOf(
                    PracticeFollowUpSourceItem(pairThree.exercise, pairThree.targetDescriptor, pairThree.assessments),
                ),
            )
        }
    }

    @Test
    fun `retry context uses both actually released slot answer keys`() {
        val item = PracticePlanner().plan("verb-article-gaps", "verb-article-gaps-1", 47).items.first()
        val source = requireNotNull(item.exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val prompt = requireNotNull(item.exercise.prompt.gapPrompt).slots.associateBy { it.id }
        val selections = listOf(
            GapSelection(
                "g0",
                prompt.getValue("g0").choices.single { it.id != source.getValue("g0").expectedChoiceId }.id,
            ),
            GapSelection("g1", source.getValue("g1").expectedChoiceId),
        )
        val evaluation = PilotEngine().evaluate(item.exercise, Answer.Gaps(selections))
        val released = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(PracticeFeedbackAttemptObservation(evaluation.targetStatus, requireNotNull(evaluation.gapResults))),
        )
        val exposureItem = item.copy(feedbackExposureKeys = released)
        val context = PracticeAttemptContextCalculator.calculate(
            exposureItem,
            false,
            listOf(PracticeItemExposureObservation(exposureItem, true, false, false, true)),
        )

        assertEquals(item.answerExposureKeys, context.priorExposure.single().keys)
    }

    @Test
    fun `provenance pins release source revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("baselineCommit=0dfc00afdc2bd21f5f06899ff47554689f0ae0c9" in provenance)
        assertTrue("releaseSourceCommit=cdb6e066288550fb958efab1e103f538c03824a5" in provenance)
        assertTrue("releaseVersion=0.8.0" in provenance)
        assertTrue("planRevision=verb-article-gaps-1" in provenance)
        assertTrue("followUpRevision=follow-up-3" in provenance)
        assertTrue("contentRevision=pilot-7" in provenance)
        assertTrue("generatorRevision=generator-7" in provenance)
        assertTrue("evaluationRevision=evaluation-7" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(GuidedGapPracticeCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "golden/guided-gap-practice-0.8.0.tsv"
        const val PROVENANCE_PATH = "golden/guided-gap-practice-0.8.0.provenance"
        const val FIXTURE_ROW_COUNT = 21
        const val FIXTURE_SHA256 = "471d452c76084975ea2d8345c7a27a75198b4467ce5e5d14b53fefc4af0e50da"
    }
}
