package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotFiveWireCompatibilityTest {
    @Test
    fun `pilot five public wire inspector and reproduction remain exact`() {
        val frozenBytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, frozenBytes.sha256(), "The frozen fixture must never be regenerated")

        val expected = frozenBytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        val actual = PilotFiveWireCompatibilityFixture.rows()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, actual)
    }

    @Test
    fun `pilot five wire provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }

        assertTrue("baselineCommit=${PilotFiveWireCompatibilityFixture.BASELINE_COMMIT}" in provenance)
        assertTrue("captureDate=2026-11-08" in provenance)
        assertTrue("contentRevision=${PilotFiveWireCompatibilityFixture.CONTENT_REVISION}" in provenance)
        assertTrue("generatorRevision=${PilotFiveWireCompatibilityFixture.GENERATOR_REVISION}" in provenance)
        assertTrue("evaluationRevision=${PilotFiveWireCompatibilityFixture.EVALUATION_REVISION}" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(PilotFiveWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-5-generator-5-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-5-generator-5-wire.provenance"
        const val ROW_COUNT = 2_811
        const val FIXTURE_SHA256 = "d1dd0ee31e163b4fd5f62b371a199d3c1eb589d23885039e6cd83a173ab0fb27"
    }
}

