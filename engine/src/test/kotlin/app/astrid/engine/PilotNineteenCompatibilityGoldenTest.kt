package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PilotNineteenCompatibilityGoldenTest {
    @Test
    fun `published pair nineteen generation grading and private keys remain frozen`() {
        val bytes = resource(FIXTURE).readBytes()
        assertEquals(SHA256, bytes.sha256())
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(4_991, expected.size)
        assertEquals(expected, PilotNineteenCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats P2 tasks answer shapes and boundaries`() {
        val rows = resource(FIXTURE).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..44).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        assertEquals(VocabularyP2Content.tasks.map { it.id }.toSet(), rows.filter { it[1] >= "E040" }.mapNotNull { row ->
            val tasks = VocabularyP2Content.tasks(row[1])
            val index = Math.floorMod(row[4].toInt(), tasks.size)
            tasks.getOrNull(index)?.id
        }.toSet())
        assertEquals(setOf(0, 1, Int.MAX_VALUE), rows.filter { it[0] == "G" && it[4] == "1000000" }.map { it[3].toInt() }.toSet())
        assertTrue(rows.filter { it[0] == "E" && it[1] >= "E040" }.map { it[5].first() }.containsAll(listOf('T', 'O', 'M')))
    }

    @Test
    fun `provenance pins release source review and scalar schema`() {
        val bytes = resource(PROVENANCE).readBytes()
        assertEquals(PROVENANCE_SHA256, bytes.sha256())
        val properties = Properties().apply { bytes.inputStream().use(::load) }
        mapOf(
            "releaseSourceCommit" to "844b88af33cc729e095d443adb72fcd72cd17534",
            "releaseVersion" to "0.22.0", "projectionSchema" to "pair19-engine-v1",
            "contentRevision" to "pilot-19", "generatorRevision" to "generator-19",
            "evaluationRevision" to "evaluation-19", "rowCount" to "4991", "fixtureSha256" to SHA256,
            "reviewSha256" to "e2af9515ee4f46af2099524ecda307869b03b2f24d6b7f87f78b72a3d63bfa6d",
        ).forEach { (key, value) -> assertEquals(value, properties.getProperty(key), key) }
        assertEquals(properties.getProperty("reviewSha256"), publishedPairNineteenReviewBytes().sha256())
    }

    @Test
    fun `export refuses collisions`() {
        val directory = Files.createTempDirectory("pair19-collision")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        assertFailsWith<IllegalArgumentException> {
            PilotNineteenCompatibilityGolden.write(fixture, directory.resolve("fixture.provenance"))
        }
    }

    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path))
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private companion object {
        const val FIXTURE = "/golden/pilot-19-generator-19-evaluation-19.tsv"
        const val PROVENANCE = "/golden/pilot-19-generator-19-evaluation-19.provenance"
        const val SHA256 = "4bf1d117dac3f3b31a9bafc23b8bcbff20f4a23dea9273a07b158a80e467f94c"
        const val PROVENANCE_SHA256 = "cb8dbea20d3bc824d802efd450744ec772a73cf9bda27ca97b4fb71cc2dc54ac"
    }
}
