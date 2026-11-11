package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PresentOrderPracticeCompatibilityTest {
    @Test
    fun `published 0_11_0 module one present and order practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PresentOrderPracticeCompatibilityFixture.rows())
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
        assertEquals(
            mapOf(
                "missing" to false,
                "canonical" to true,
                "generic-unknown" to false,
                "wrong-order" to true,
                "required-missing" to true,
            ),
            completion.getValue("module-one-order-outcome-1"),
        )
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
        assertTrue("baselineCommit=8934fb1bea132854d91d592801af33dd10fbc338" in provenance)
        assertTrue("releaseVersion=0.11.0" in provenance)
        assertTrue("captureDate=2026-11-10" in provenance)
        assertTrue("contentRevision=pilot-10" in provenance)
        assertTrue("generatorRevision=generator-10" in provenance)
        assertTrue("evaluationRevision=evaluation-10" in provenance)
        assertTrue("planRevisions=module-one-present-1,module-one-order-1" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("module-one-practice-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PresentOrderPracticeCompatibilityFixture.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/present-order-practice-0.11.0.tsv"
        const val PROVENANCE_PATH = "golden/present-order-practice-0.11.0.provenance"
        const val FIXTURE_ROW_COUNT = 23
        const val FIXTURE_SHA256 = "207c01c103abf2ea83cd20ed6530dd3a5433c85676fb637b98c454590bdf057e"
        val PROFILES = setOf("module-one-present", "module-one-order")
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
    }
}
