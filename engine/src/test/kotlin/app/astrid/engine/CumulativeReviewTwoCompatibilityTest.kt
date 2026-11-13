package app.astrid.engine

import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CumulativeReviewTwoCompatibilityTest {
    @Test
    fun `published cumulative review two selection and private items remain frozen`() {
        val bytes = resource(FIXTURE).readBytes(); assertEquals(SHA256, bytes.sha256())
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(33, expected.size); assertEquals(expected, CumulativeReviewTwoCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all twenty six sources pools policies and released boundary failure`() {
        val rows = CumulativeReviewTwoCompatibilityFixture.rows().map { it.split('\t') }
        assertEquals(CumulativeReviewTwoCompatibilityFixture.SOURCE_PLANS.toSet(),
            rows.filter { it[0] == "SOURCE_FAMILY" }.map { it[1] to it[2] }.toSet())
        assertTrue(rows.any { it[0] == "SHORT" }); assertTrue(rows.any { it[0] == "EXPLICIT_NULL_POLICY" })
        assertEquals(listOf("EMPTY", "true"), rows.single { it[0] == "EMPTY" })
        assertEquals(Int.MAX_VALUE.toString(), rows.single { it[0] == "FULL_FAILURE" }[1])
    }

    @Test
    fun `provenance identifies cumulative two and its published source set`() {
        val bytes = resource(PROVENANCE).readBytes(); assertEquals(PROVENANCE_SHA256, bytes.sha256())
        val properties = Properties().apply { bytes.inputStream().use(::load) }
        assertEquals("844b88af33cc729e095d443adb72fcd72cd17534", properties.getProperty("releaseSourceCommit"))
        assertEquals("cumulative-review-two-engine-v1", properties.getProperty("projectionSchema"))
        assertEquals("cumulative-review-2", properties.getProperty("planRevision"))
        assertEquals("26", properties.getProperty("sourceFamilies")); assertEquals("33", properties.getProperty("rowCount"))
        assertEquals(SHA256, properties.getProperty("fixtureSha256"))
    }

    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path))
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private companion object {
        const val FIXTURE = "/golden/cumulative-review-2-0.22.0.tsv"
        const val PROVENANCE = "/golden/cumulative-review-2-0.22.0.provenance"
        const val SHA256 = "230230fbbbdf5081bd2eb343d948e9ce89cbba466fac348175db12825048797f"
        const val PROVENANCE_SHA256 = "2bd4e55dbe40f7b19b8f615ca94edecfde2e694f1863d6af63282a2a68af038c"
    }
}
