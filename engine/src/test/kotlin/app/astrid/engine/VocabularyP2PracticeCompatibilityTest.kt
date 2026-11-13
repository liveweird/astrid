package app.astrid.engine

import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VocabularyP2PracticeCompatibilityTest {
    @Test
    fun `published P2 plans policies exposure and grading remain frozen`() {
        val bytes = resource(FIXTURE).readBytes()
        assertEquals(SHA256, bytes.sha256())
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(194, expected.size)
        assertEquals(expected, VocabularyP2PracticeCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers six plans lifecycle cases and released boundary failures`() {
        val rows = VocabularyP2PracticeCompatibilityFixture.rows().map { it.split('\t') }
        assertEquals(VocabularyP2PracticeCompatibilityFixture.profiles.map { it.first }.toSet(),
            rows.filter { it[0] == "PLAN" || it[0] == "PLAN_FAILURE" }.map { it[1] }.toSet())
        assertEquals(setOf("EVALUATION", "EXPOSURE", "RETRY_EXPOSURE", "TEACHING_REQUIRED"),
            rows.map { it[0] }.filter { it in setOf("EVALUATION", "EXPOSURE", "RETRY_EXPOSURE", "TEACHING_REQUIRED") }.toSet())
        assertEquals(setOf("correct", "wrong", "partial"), rows.filter { it[0] == "EVALUATION" }.map { it[4] }.toSet())
        assertEquals(setOf("vocabulary-p2-s2", "vocabulary-p2-s3", "vocabulary-p2-s4"),
            rows.filter { it[0] == "PLAN_FAILURE" && it[2] == Int.MAX_VALUE.toString() }.map { it[1] }.toSet())
    }

    @Test
    fun `provenance identifies the genuine published baseline`() {
        val bytes = resource(PROVENANCE).readBytes(); assertEquals(PROVENANCE_SHA256, bytes.sha256())
        val properties = Properties().apply { bytes.inputStream().use(::load) }
        assertEquals("844b88af33cc729e095d443adb72fcd72cd17534", properties.getProperty("releaseSourceCommit"))
        assertEquals("p2-practice-engine-v1", properties.getProperty("projectionSchema"))
        assertEquals("194", properties.getProperty("rowCount")); assertEquals(SHA256, properties.getProperty("fixtureSha256"))
        assertTrue(properties.getProperty("planRevisions").split(',').size == 6)
    }

    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path))
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private companion object {
        const val FIXTURE = "/golden/vocabulary-p2-practice-0.22.0.tsv"
        const val PROVENANCE = "/golden/vocabulary-p2-practice-0.22.0.provenance"
        const val SHA256 = "9573a78a065ffe947f9419c8ee217d69cfb0ca738b036aa5157a4147c6e6924d"
        const val PROVENANCE_SHA256 = "a848670c6c29088742f500444572cc0d818f1e67edbcccb0424060d3754dfe77"
    }
}
