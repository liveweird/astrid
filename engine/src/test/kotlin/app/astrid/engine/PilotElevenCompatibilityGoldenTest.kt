package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PilotElevenCompatibilityGoldenTest {
    @Test
    fun `published pilot eleven generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotElevenCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..25).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf(
            "E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8, "E020" to 2, "E021" to 2,
            "E022" to 5, "E023" to 5, "E024" to 3, "E025" to 3,
        )
        authoredCounts.forEach { (formatId, taskCount) ->
            val formatRows = rows.filter { it[1] == formatId }
            assertEquals(taskCount + BOUNDARY_ROW_COUNT, formatRows.count { it[0] == "G" })
            assertTrue(setOf("T", "O", "M", "P").all { kind -> formatRows.any { it[0] == kind } })
            BOUNDARY_SEEDS.forEach { seed ->
                assertTrue(formatRows.any { it[0] == "G" && it[3] == seed.toString() && it[4] == MAX_INDEX.toString() })
            }
        }
        val moduleOneTextAnswers = rows.filter { it[0] == "T" && it[1] in authoredCounts }
            .map { Base64.getDecoder().decode(it[5]).toString(Charsets.UTF_8) }
        assertTrue("i" in moduleOneTextAnswers)
        assertTrue("De" in moduleOneTextAnswers)
        assertTrue(setOf("jeg", "du", "han", "hun", "vi", "I", "de", "være").all { it in moduleOneTextAnswers })
        assertTrue(setOf("den", "det").all { it in moduleOneTextAnswers })
        assertTrue(setOf("arbejder", "læser", "sover", "arbejde", "læse", "sove").all { it in moduleOneTextAnswers })
        assertTrue(setOf("har", "have").all { it in moduleOneTextAnswers })
    }

    @Test
    fun `provenance pins release source revisions review and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=b82ad3615b74de932ae903e7d479d454debc0740" in provenance)
        assertTrue("releaseVersion=0.13.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue("contentRevision=pilot-11" in provenance)
        assertTrue("generatorRevision=generator-11" in provenance)
        assertTrue("evaluationRevision=evaluation-11" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairElevenReviewBytes().sha256())
    }

    @Test
    fun `teaching review contains all seven released guided introductions`() {
        val review = publishedPairElevenReviewBytes().toString(Charsets.UTF_8)
        val revisions = setOf(
            "module-one-singular-1", "module-one-groups-1", "module-one-er-1", "module-one-things-1",
            "module-one-present-1", "module-one-order-1", "module-one-possession-1",
        )
        revisions.forEach { assertTrue(it in review, it) }
    }

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-eleven-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotElevenCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-11-generator-11-evaluation-11.tsv"
        const val PROVENANCE_PATH = "golden/pilot-11-generator-11-evaluation-11.provenance"
        const val FIXTURE_ROW_COUNT = 2986
        const val FIXTURE_SHA256 = "04b274e4bb32d8f813f10509924eea23c2433d97fe87e99a2a408a8af94254de"
        const val REVIEW_SHA256 = "6755d11d95fc7d4a77f9623f4fc5bc960aa9adc14b28fb7da52f39d9a2fbb65f"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    }
}
