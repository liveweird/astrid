package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotFourWireCompatibilityTest {
    @Test
    fun `pilot four public wire inspector and reproduction remain exact`() {
        val frozenBytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, frozenBytes.sha256(), "The frozen fixture must never be regenerated")

        val expected = frozenBytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        val actual = PilotFourWireCompatibilityFixture.rows()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, actual)
    }

    @Test
    fun `pilot four wire provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }

        assertTrue("baselineCommit=${PilotFourWireCompatibilityFixture.BASELINE_COMMIT}" in provenance)
        assertTrue("contentRevision=${PilotFourWireCompatibilityFixture.CONTENT_REVISION}" in provenance)
        assertTrue("generatorRevision=${PilotFourWireCompatibilityFixture.GENERATOR_REVISION}" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256() = with(PilotFourWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-4-generator-4-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-4-generator-4-wire.provenance"
        const val ROW_COUNT = 2_187
        const val FIXTURE_SHA256 = "e421043ca55977c38d0d9e65cb4d1dedff367f0b7d19b9dcd43127344480e5dd"
    }
}
