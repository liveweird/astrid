package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ModuleOneThingsPracticeWireCompatibilityTest {
    @Test
    fun `module one things practice public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, ModuleOneThingsPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers the four item S04 plan at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        listOf("CREATE", "INITIAL", "INTRODUCED", "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY").forEach { kind ->
            assertEquals(4, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(5, eligibility.size)
        assertEquals(setOf("correct", "known-wrong", "case", "shape", "unknown"), eligibility.map { it[2] }.toSet())
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val (privatePlan, initialWire, completedWire) = ModuleOneThingsPracticeWireCompatibilityFixture.samples()
        val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
        assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
        assertEquals("module-one-known-outcome-1", privateJson.getValue("completionRuleId").jsonPrimitive.content)
        assertEquals("module-one-exposure-1", privateJson.getValue("exposurePolicyId").jsonPrimitive.content)
        assertEquals(4, privateJson.getValue("items").jsonArray.size)
        assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })

        val initial = wireJson.parseToJsonElement(initialWire).jsonObject
        assertTrue(initial.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
        assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
        assertFalse("followUpPlanRevision" in initial)
        assertFalse("followUp" in initial)
        assertEquals(false, initial.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())

        val completed = wireJson.parseToJsonElement(completedWire).jsonObject
        assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
        assertEquals(2, completed.getValue("summary").jsonObject.getValue("rows").jsonArray.size)
        assertTrue(completed.getValue("summary").jsonObject.getValue("rows").jsonArray.all { "targetScopeId" in it.jsonObject })
        assertFalse("followUpPlanRevision" in completed)
    }

    @Test
    fun `provenance pins release source plans revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "baselineCommit=${ModuleOneThingsPracticeWireCompatibilityFixture.BASELINE_COMMIT}",
            "releaseSourceCommit=${ModuleOneThingsPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.10.0", "captureDate=2026-11-10",
            "planRevisions=module-one-things-1",
            "contentRevision=pilot-9", "generatorRevision=generator-9", "evaluationRevision=evaluation-9",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(ModuleOneThingsPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/module-one-things-practice-0.10.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/module-one-things-practice-0.10.0-wire.provenance"
        const val ROW_COUNT = 34
        const val FIXTURE_SHA256 = "37403f0a4bb18cf96fe935b0ce0a4fcd3322028d201bc49fe931e1bcba9e3453"
        val PROFILES = setOf("module-one-things")
    }
}
