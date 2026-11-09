package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotSevenWireCompatibilityTest {
    @Test
    fun `pilot seven public wire inspector and reproduction remain exact`() {
        val frozenBytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, frozenBytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = frozenBytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotSevenWireCompatibilityFixture.rows())
    }

    @Test
    fun `pilot seven wire archive covers nested gap payloads`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        val gapRows = rows.filter { it[1] == "E015" }
        assertTrue(gapRows.isNotEmpty())
        listOf(0, 47, Int.MAX_VALUE).forEach { seed ->
            (0 until 92).forEach { index ->
                assertTrue(gapRows.any { it[0] == "C" && it[3] == "$seed" && it[4] == "$index" })
            }
            assertTrue(gapRows.any { it[0] == "B" && it[3] == "$seed" && it[4] == "92" })
            assertTrue(gapRows.any { it[0] == "B" && it[3] == "$seed" && it[4] == "1000000" })
        }
    }

    @Test
    fun `pilot seven wire provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("baselineCommit=${PilotSevenWireCompatibilityFixture.BASELINE_COMMIT}" in provenance)
        assertTrue("captureDate=2026-11-09" in provenance)
        assertTrue("contentRevision=${PilotSevenWireCompatibilityFixture.CONTENT_REVISION}" in provenance)
        assertTrue("generatorRevision=${PilotSevenWireCompatibilityFixture.GENERATOR_REVISION}" in provenance)
        assertTrue("evaluationRevision=${PilotSevenWireCompatibilityFixture.EVALUATION_REVISION}" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(PilotSevenWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-7-generator-7-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-7-generator-7-wire.provenance"
        const val ROW_COUNT = 3_134
        const val FIXTURE_SHA256 = "20d5e590db0c7f1c26e8a8ebfdf93f9dc9b6f5df3dee88e6020e9b2754bf4a51"
    }
}
