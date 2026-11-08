package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotSixWireCompatibilityTest {
    @Test
    fun `pilot six public wire inspector and reproduction remain exact`() {
        val frozenBytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, frozenBytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = frozenBytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotSixWireCompatibilityFixture.rows())
    }

    @Test
    fun `pilot six wire provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("baselineCommit=${PilotSixWireCompatibilityFixture.BASELINE_COMMIT}" in provenance)
        assertTrue("captureDate=2026-11-09" in provenance)
        assertTrue("contentRevision=${PilotSixWireCompatibilityFixture.CONTENT_REVISION}" in provenance)
        assertTrue("generatorRevision=${PilotSixWireCompatibilityFixture.GENERATOR_REVISION}" in provenance)
        assertTrue("evaluationRevision=${PilotSixWireCompatibilityFixture.EVALUATION_REVISION}" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(PilotSixWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-6-generator-6-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-6-generator-6-wire.provenance"
        const val ROW_COUNT = 2_852
        const val FIXTURE_SHA256 = "8cc606452d0eea26d39476a6b58d04b7c7aaef4b65fe16292583cea5a0ab59df"
    }
}
