package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ModuleOnePracticeWireCompatibilityTest {
    @Test
    fun `module one practice public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, ModuleOnePracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers three six item plans at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        listOf("CREATE", "INITIAL", "INTRODUCED", "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY").forEach { kind ->
            assertEquals(12, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(15, eligibility.size)
        assertEquals(setOf("correct", "known-wrong", "case", "shape", "unknown"), eligibility.map { it[2] }.toSet())
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val (privatePlan, initialWire, completedWire) = ModuleOnePracticeWireCompatibilityFixture.samples()
        val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
        assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
        assertEquals("module-one-known-outcome-1", privateJson.getValue("completionRuleId").jsonPrimitive.content)
        assertEquals("module-one-exposure-1", privateJson.getValue("exposurePolicyId").jsonPrimitive.content)
        assertEquals(6, privateJson.getValue("items").jsonArray.size)
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
            "baselineCommit=${ModuleOnePracticeWireCompatibilityFixture.BASELINE_COMMIT}",
            "releaseSourceCommit=${ModuleOnePracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.9.0", "captureDate=2026-11-10",
            "planRevisions=module-one-singular-1,module-one-groups-1,module-one-er-1",
            "contentRevision=pilot-8", "generatorRevision=generator-8", "evaluationRevision=evaluation-8",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(ModuleOnePracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/module-one-practice-0.9.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/module-one-practice-0.9.0-wire.provenance"
        const val ROW_COUNT = 100
        const val FIXTURE_SHA256 = "9f09be95bd8f3356e8e1cabbe9b3980829f001674157712454d4131730dba5ec"
        val PROFILES = setOf("module-one-singular", "module-one-groups", "module-one-er")
    }
}
