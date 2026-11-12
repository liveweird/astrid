package app.astrid.server

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CumulativeReviewOneWireCompatibilityTest {
    @Test
    fun `published 0_21_0 cumulative review wire remains frozen`() {
        val bytes = requireNotNull(javaClass.getResourceAsStream(FIXTURE)).use { it.readBytes() }
        assertEquals(SHA256, bytes.sha256())
        assertProvenance()
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(25, expected.size)
        assertEquals(expected, CumulativeReviewOneWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers request payload public states replay and explicit null policy`() {
        val rows = CumulativeReviewOneWireCompatibilityFixture.rows().map { it.split('\t') }
        val expected = setOf("CREATE", "PAYLOAD_ROUND_TRIP", "INITIAL", "TAUGHT", "ATTEMPTED", "SAVED_ATTEMPT_REPLAY")
        assertEquals(expected, rows.filter { it.size == 3 }.map { it[0] }.toSet())
        expected.forEach { type -> assertEquals(4, rows.count { it[0] == type }) }
        assertTrue(rows.any { it[0] == "EXPLICIT_NULL_POLICY" })
    }

    @Test
    fun `export refuses either existing evidence file without changing it`() {
        val directory = Files.createTempDirectory("astrid-cumulative-freeze-")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        try {
            Files.writeString(fixture, "existing fixture")
            assertFailsWith<IllegalArgumentException> {
                CumulativeReviewOneWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
            }
            assertEquals("existing fixture", Files.readString(fixture))
            assertFalse(Files.exists(provenance))
            Files.delete(fixture)
            Files.writeString(provenance, "existing provenance")
            assertFailsWith<IllegalArgumentException> {
                CumulativeReviewOneWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
            }
            assertEquals("existing provenance", Files.readString(provenance))
            assertFalse(Files.exists(fixture))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    private fun assertProvenance() {
        val bytes = requireNotNull(javaClass.getResourceAsStream(PROVENANCE)).use { it.readBytes() }
        assertEquals(PROVENANCE_SHA256, bytes.sha256(), "Frozen provenance must retain its original bytes")
        val properties = Properties().apply { bytes.inputStream().use { load(it) } }
        mapOf(
            "releaseSourceCommit" to "eab86c6ddd44175e2d9b6dde7e2283f30943800e",
            "releaseVersion" to "0.21.0",
            "captureDate" to "2026-11-12",
            "projectionSchema" to "cumulative-review-one-server-wire-v1",
            "planRevision" to "cumulative-review-1",
            "sourceFamilies" to "20",
            "reviewState" to "compatibility-only-not-linguistic-approval",
            "rowCount" to "25",
            "fixtureSha256" to SHA256,
        ).forEach { (key, value) -> assertEquals(value, properties.getProperty(key), key) }
        assertTrue(properties.getProperty("description").isNotBlank())
    }

    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val PROVENANCE = "/golden/cumulative-review-0.21.0-wire.provenance"
        const val PROVENANCE_SHA256 = "904b6081ef17147d54c1316c85144b226f7e9e1f863ed2a3a1a76e30412be75e"
        const val FIXTURE = "/golden/cumulative-review-0.21.0-wire.tsv"
        const val SHA256 = "35eda669026797ea3379d5b010bace1136c96c9596936089349b47224d3da8fe"
    }
}
