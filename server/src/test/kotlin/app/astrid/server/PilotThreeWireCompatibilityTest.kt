package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotThreeWireCompatibilityTest {
    @Test
    fun `pilot three public wire and reproduction remain exact`() {
        val frozenBytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, frozenBytes.sha256(), "The frozen fixture must never be regenerated")

        val expected = frozenBytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        val actual = PilotThreeWireCompatibilityFixture.rows()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, actual)
    }

    @Test
    fun `pilot three wire provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }

        assertTrue("baselineCommit=${PilotThreeWireCompatibilityFixture.BASELINE_COMMIT}" in provenance)
        assertTrue("contentRevision=${PilotThreeWireCompatibilityFixture.CONTENT_REVISION}" in provenance)
        assertTrue("generatorRevision=${PilotThreeWireCompatibilityFixture.GENERATOR_REVISION}" in provenance)
        assertTrue("rowCount=$ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(PilotThreeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-3-generator-3-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-3-generator-3-wire.provenance"
        const val ROW_COUNT = 1_806
        const val FIXTURE_SHA256 = "ef8f62beb500a0e25da6d94030271043c6cb5c7b987544089e0ca6579024c93d"
    }
}
