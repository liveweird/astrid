package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class QuestionPracticeWireCompatibilityTest {
    @Test
    fun `module one question public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, QuestionPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers the four item S13 question plan at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        val lifecycleKinds = listOf(
            "CREATE", "INITIAL", "BEFORE_TEACHING", "INTRODUCED",
            "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY",
        )
        lifecycleKinds.forEach { kind ->
            assertEquals(4, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(4, eligibility.size)
        assertEquals(
            setOf(
                "correct", "known-order-error-advances", "plural-known-order-error-advances", "reversed",
            ),
            eligibility.map { it[2] }.toSet(),
        )
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val samples = QuestionPracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(1, samples.size)
        samples.forEach { (privatePlan, initialWire, completedWire) ->
            val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
            assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertEquals(
                "module-one-order-outcome-1",
                privateJson.getValue("completionRuleId").jsonPrimitive.content,
            )
            assertEquals(
                "module-one-order-exposure-1",
                privateJson.getValue("exposurePolicyId").jsonPrimitive.content,
            )
            assertEquals(4, privateJson.getValue("items").jsonArray.size)
            assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })
            assertEquals(
                List(4) { "constituent-order" },
                privateJson.getValue("items").jsonArray.map {
                    it.jsonObject.getValue("assessmentMode").jsonPrimitive.content
                },
            )
            val introductionKeys = privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content }
            assertTrue("module-one:pattern:neutral-positive-yes-no" in introductionKeys)
            assertTrue("module-one:meaning:L039:L039-S01" in introductionKeys)
            assertTrue("module-one:meaning:L040:L040-S01" in introductionKeys)
            val privateItems = privateJson.getValue("items").jsonArray.map { it.jsonObject }
            assertTrue(privateItems.all { it.getValue("answerExposureKeys").jsonArray.size == 2 })
            assertTrue(privateItems.all {
                it.getValue("targetScopeId").jsonPrimitive.content == "neutral-positive-yes-no"
            })
            assertTrue(privateItems.all { item ->
                item.getValue("lessonExposureKeys").jsonArray.map { it.jsonPrimitive.content } ==
                    privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content }
            })

            val initial = wireJson.parseToJsonElement(initialWire).jsonObject
            assertTrue(initial.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse("followUpPlanRevision" in initial)
            assertFalse("followUp" in initial)
            assertFalse(initial.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())

            val completed = wireJson.parseToJsonElement(completedWire).jsonObject
            assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
            assertEquals(1, completed.getValue("summary").jsonObject.getValue("rows").jsonArray.size)
            assertFalse("followUpPlanRevision" in completed)
        }
    }

    @Test
    fun `provenance pins release source plans revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "releaseSourceCommit=${QuestionPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.18.0", "captureDate=2026-11-11",
            "module-one-questions-1:pilot-16/generator-16/evaluation-16",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(QuestionPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/question-practice-0.18.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/question-practice-0.18.0-wire.provenance"
        const val ROW_COUNT = 37
        const val FIXTURE_SHA256 = "7f6698e10b465dfd12b216e3f5931c8e5bb0133d84818b1f4e1429d985cb7bca"
        val PROFILES = setOf("module-one-questions")
    }
}

