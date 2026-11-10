package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PilotEightCompatibilityGoldenTest {
    @Test
    fun `published pilot eight generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotEightCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..19).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf("E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8)
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
    }

    @Test
    fun `provenance pins release source revisions review and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("baselineCommit=8913dea94a6c3bc0332f464e56952b9a126118a3" in provenance)
        assertTrue("captureDate=2026-11-10" in provenance)
        assertTrue("contentRevision=pilot-8" in provenance)
        assertTrue("generatorRevision=generator-8" in provenance)
        assertTrue("evaluationRevision=evaluation-8" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairEightReviewBytes().sha256())
    }

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-eight-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotEightCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-8-generator-8-evaluation-8.tsv"
        const val PROVENANCE_PATH = "golden/pilot-8-generator-8-evaluation-8.provenance"
        const val FIXTURE_ROW_COUNT = 2674
        const val FIXTURE_SHA256 = "2c7d1851cf84a71cfbecd04da0d332cb01b509904431e60a0646e89a9f058b48"
        const val REVIEW_SHA256 = "341d0b1a798331397ec1c199ad86dbb17cbb02fd515a4574da472a23a6a488f0"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    }
}
