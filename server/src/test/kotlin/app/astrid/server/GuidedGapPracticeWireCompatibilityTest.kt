package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GuidedGapPracticeWireCompatibilityTest {
    @Test
    fun `published 0_8_0 guided gap public and private wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, GuidedGapPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers profile creation support plans and selected follow ups`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[1].toInt() }.toSet())
        assertEquals(
            setOf(0, 1, 47, Int.MAX_VALUE),
            rows.filter { it[0] == "INITIAL_WIRE" }.map { it[1].toInt() }.toSet(),
        )
        assertEquals(
            setOf(0, 1, 47, Int.MAX_VALUE),
            rows.filter { it[0] == "SUPPORTED_WIRE" }.map { it[1].toInt() }.toSet(),
        )
        assertEquals(setOf("introduction", "lesson", "hint"), rows.filter { it[0] == "SUPPORT" }.map { it[1] }.toSet())
        assertEquals(1, rows.count { it[0] == "ATTEMPTED_WIRE" })
        assertEquals(1, rows.count { it[0] == "COMPLETED_WIRE" })
        assertEquals(3, rows.count { it[0] == "FOLLOW_UP_WIRE" })
        assertEquals(1, rows.count { it[0] == "RECURSIVE_FOLLOW_UP_WIRE" })
    }

    @Test
    fun `published payloads precede scoped teaching and completion fields`() {
        GuidedGapPracticeWireCompatibilityFixture.frozenWireSamples().forEach { wire ->
            assertFalse("targetScopeId" in wire)
            assertFalse("requiredTeaching" in wire)
            assertFalse("completion" in wire)
            assertTrue("\"assessments\"" in wire)
            assertTrue("\"assessmentRole\"" in wire)
        }
    }

    @Test
    fun `public samples freeze reachable support attempt context and completed summaries`() {
        val samples = GuidedGapPracticeWireCompatibilityFixture.frozenWireSamples()
        val supported = app.astrid.server.infra.json.wireJson.parseToJsonElement(samples[1]).jsonObject
        val attempted = app.astrid.server.infra.json.wireJson.parseToJsonElement(samples[2]).jsonObject
        val completed = app.astrid.server.infra.json.wireJson.parseToJsonElement(samples[3]).jsonObject

        assertEquals("active", supported.getValue("state").jsonPrimitive.content)
        assertTrue(supported.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
        val supportedItem = supported.getValue("items").jsonArray.first().jsonObject
        assertTrue(supportedItem.getValue("lessonOpened").jsonPrimitive.content.toBoolean())
        assertTrue(supportedItem.getValue("hintOpened").jsonPrimitive.content.toBoolean())

        val attemptedItem = attempted.getValue("items").jsonArray.first().jsonObject
        assertEquals(4, attemptedItem.getValue("attemptCount").jsonPrimitive.content.toInt())
        val latestExposure = attemptedItem.getValue("lastAttemptContext").jsonObject
            .getValue("priorExposure").jsonArray.single {
                it.jsonObject.getValue("source").jsonPrimitive.content == "feedback"
            }.jsonObject.getValue("keys").jsonArray
        assertEquals(2, latestExposure.size)

        assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
        val summary = completed.getValue("summary").jsonObject
        assertEquals(7, summary.getValue("attemptCount").jsonPrimitive.content.toInt())
        assertEquals(3, summary.getValue("retryCount").jsonPrimitive.content.toInt())
        val rows = summary.getValue("rows").jsonArray.map { it.jsonObject }
        assertEquals(setOf("primary", "support"), rows.map { it.getValue("assessmentRole").jsonPrimitive.content }.toSet())
        assertTrue(rows.all { it.getValue("latestOutcomes").jsonObject.getValue("unevaluable").jsonPrimitive.content == "1" })
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
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(GuidedGapPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/guided-gap-practice-0.8.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/guided-gap-practice-0.8.0-wire.provenance"
        const val FIXTURE_ROW_COUNT = 22
        const val FIXTURE_SHA256 = "ac9d48dfebfd3cd240688dad34fc544847720b8fbf106bfd198680ac6fd69674"
    }
}
