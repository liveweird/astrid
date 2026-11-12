package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CumulativeReviewOneCompatibilityTest {
    @Test
    fun `published 0_21_0 cumulative review plan remains frozen`() {
        val bytes = requireNotNull(javaClass.getResourceAsStream(FIXTURE)).use { it.readBytes() }
        assertEquals(SHA256, bytes.sha256())
        assertProvenance()
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(27, expected.size)
        assertEquals(expected, CumulativeReviewOneCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all sources pools boundaries and explicit null policy`() {
        val rows = CumulativeReviewOneCompatibilityFixture.rows().map { it.split('\t') }
        assertEquals(CumulativeReviewOneCompatibilityFixture.SOURCE_PLANS.toSet(), rows.filter { it[0] == "SOURCE_FAMILY" }
            .map { it[1] to it[2] }.toSet())
        assertEquals(setOf("0", "1", "47", Int.MAX_VALUE.toString()), rows.filter { it[0] == "FULL" }.map { it[1] }.toSet())
        assertTrue(rows.any { it[0] == "SHORT" })
        assertTrue(rows.any { it[0] == "EXPLICIT_NULL_POLICY" })
        assertEquals(listOf("EMPTY", "true"), rows.single { it[0] == "EMPTY" })
    }

    @Test
    fun `export refuses either existing evidence file without changing it`() {
        val directory = Files.createTempDirectory("astrid-cumulative-freeze-")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        try {
            Files.writeString(fixture, "existing fixture")
            assertFailsWith<IllegalArgumentException> { CumulativeReviewOneCompatibilityFixture.write(fixture, provenance) }
            assertEquals("existing fixture", Files.readString(fixture))
            assertFalse(Files.exists(provenance))
            Files.delete(fixture)
            Files.writeString(provenance, "existing provenance")
            assertFailsWith<IllegalArgumentException> { CumulativeReviewOneCompatibilityFixture.write(fixture, provenance) }
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
            "projectionSchema" to "cumulative-review-one-engine-v1",
            "planRevision" to "cumulative-review-1",
            "sourceFamilies" to "20",
            "reviewState" to "compatibility-only-not-linguistic-approval",
            "rowCount" to "27",
            "fixtureSha256" to SHA256,
        ).forEach { (key, value) -> assertEquals(value, properties.getProperty(key), key) }
        assertTrue(properties.getProperty("description").isNotBlank())
    }

    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val PROVENANCE = "/golden/cumulative-review-0.21.0.provenance"
        const val PROVENANCE_SHA256 = "ea4ecca0af36abb750a1f233dc5e2db192edc817d28e931b9e13c0acad130356"
        const val FIXTURE = "/golden/cumulative-review-0.21.0.tsv"
        const val SHA256 = "6d5ed07c07a4a1b2c88b692ff51f53ad27069a848890613cbdd268a60b504ffa"
    }
}
