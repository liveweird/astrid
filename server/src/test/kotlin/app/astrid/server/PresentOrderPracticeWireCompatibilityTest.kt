package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PresentOrderPracticeWireCompatibilityTest {
    @Test
    fun `module one present and order practice public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PresentOrderPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers both six item S05 S06 plans at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        listOf("CREATE", "INITIAL", "INTRODUCED", "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY").forEach { kind ->
            assertEquals(8, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(10, eligibility.size)
        assertEquals(
            setOf(
                "correct", "known-wrong", "case", "shape", "unknown", "wrong-order", "required-missing",
                "unknown-id", "wrong-kind",
            ),
            eligibility.map { it[2] }.toSet(),
        )
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val samples = PresentOrderPracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(2, samples.size)
        samples.forEachIndexed { index, (privatePlan, initialWire, completedWire) ->
            val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
            assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertEquals(
                if (index == 0) "module-one-known-outcome-1" else "module-one-order-outcome-1",
                privateJson.getValue("completionRuleId").jsonPrimitive.content,
            )
            assertEquals(
                if (index == 0) "module-one-exposure-1" else "module-one-order-exposure-1",
                privateJson.getValue("exposurePolicyId").jsonPrimitive.content,
            )
            assertEquals(6, privateJson.getValue("items").jsonArray.size)
            assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })

            val initial = wireJson.parseToJsonElement(initialWire).jsonObject
            assertTrue(initial.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse("followUpPlanRevision" in initial)
            assertFalse("followUp" in initial)
            assertFalse(initial.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())

            val completed = wireJson.parseToJsonElement(completedWire).jsonObject
            assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
            assertEquals(2, completed.getValue("summary").jsonObject.getValue("rows").jsonArray.size)
            assertTrue(completed.getValue("summary").jsonObject.getValue("rows").jsonArray.all { "targetScopeId" in it.jsonObject })
            assertFalse("followUpPlanRevision" in completed)
        }
    }

    @Test
    fun `provenance pins release source plans revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "baselineCommit=${PresentOrderPracticeWireCompatibilityFixture.BASELINE_COMMIT}",
            "releaseSourceCommit=${PresentOrderPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.11.0", "captureDate=2026-11-10",
            "planRevisions=module-one-present-1,module-one-order-1",
            "contentRevision=pilot-10", "generatorRevision=generator-10", "evaluationRevision=evaluation-10",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(PresentOrderPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/present-order-practice-0.11.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/present-order-practice-0.11.0-wire.provenance"
        const val ROW_COUNT = 67
        const val FIXTURE_SHA256 = "eb1b09c9197a8dc538f553c22d412961b382c5161f0439565383e843cca2e5ba"
        val PROFILES = setOf("module-one-present", "module-one-order")
    }
}
