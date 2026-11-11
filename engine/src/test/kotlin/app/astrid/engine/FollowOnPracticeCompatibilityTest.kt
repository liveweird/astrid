package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FollowOnPracticeCompatibilityTest {
    @Test
    fun `published 0_13_0 possession and reading words practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, FollowOnPracticeCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all profiles seeds summaries and completion boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        val plans = rows.filter { it[0] == "PLAN" }
        assertEquals(PROFILES, plans.map { it[1] }.toSet())
        PROFILES.forEach { profile ->
            assertEquals(SEEDS, plans.filter { it[1] == profile }.map { it[2].toInt() }.toSet())
            assertEquals(1, rows.count { it[0] == "SUMMARY" && it[1] == profile })
        }
        val completion = rows.filter { it[0] == "COMPLETION" }.groupBy { it[1] }.mapValues { entry ->
            entry.value.associate { it[2] to it[3].toBooleanStrict() }
        }
        assertEquals(
            mapOf(
                "missing" to false,
                "met" to true,
                "known-wrong" to true,
                "unknown" to false,
                "case" to true,
                "multiword" to true,
                "blank" to false,
            ),
            completion.getValue("module-one-known-outcome-1"),
        )
        val exposure = rows.filter { it[0] == "EXPOSURE" }.groupBy { it[1] }
            .mapValues { entry -> entry.value.associate { it[2] to it[3] } }
        val reading = exposure.getValue("module-one-reading-words")
        assertEquals("-", reading.getValue("wrong"))
        assertTrue(reading.getValue("right") != "-")
        assertEquals(reading.getValue("right"), reading.getValue("wrong-then-right"))
        assertTrue(exposure.getValue("module-one-possession").values.all { it != "-" })
    }

    @Test
    fun `additive module one catalogue leaves the three published older profiles available`() {
        val profiles = PracticePlanner().profiles()
        assertTrue(
            setOf("reading-basics", "present-verbs", "verb-article-gaps").all { old ->
                profiles.any { it.focusId == old }
            },
        )
        assertEquals(PROFILES, profiles.filter { it.focusId in PROFILES }.map { it.focusId }.toSet())
    }

    @Test
    fun `provenance pins release revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=b82ad3615b74de932ae903e7d479d454debc0740" in provenance)
        assertTrue("releaseVersion=0.13.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue(
            "planPairs=module-one-possession-1:pilot-11/generator-11/evaluation-11," +
                "module-one-reading-words-1:pilot-5/generator-5/evaluation-5" in provenance,
        )
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("module-one-practice-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { FollowOnPracticeCompatibilityFixture.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/follow-on-practice-0.13.0.tsv"
        const val PROVENANCE_PATH = "golden/follow-on-practice-0.13.0.provenance"
        const val FIXTURE_ROW_COUNT = 24
        const val FIXTURE_SHA256 = "9d53d3b0b74598ab0c3d8cc0d87caa1a520642aaae7b3a13b967368cb4f069b2"
        val PROFILES = setOf("module-one-possession", "module-one-reading-words")
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
    }
}
