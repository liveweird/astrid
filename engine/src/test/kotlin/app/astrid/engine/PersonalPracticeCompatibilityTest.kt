package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PersonalPracticeCompatibilityTest {
    @Test
    fun `published 0_14_0 personal introduction practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PersonalPracticeCompatibilityFixture.rows())
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
        assertTrue(exposure.getValue("module-one-personal").values.all { it != "-" })
        val evaluations = rows.filter { it[0] == "EVALUATION" }
        assertEquals(24, evaluations.size)
        assertEquals((0..5).toSet(), evaluations.map { it[1].toInt() }.toSet())
        assertEquals(setOf("E022", "E023"), evaluations.map { it[2] }.toSet())
        assertEquals(setOf("canonical", "known-wrong", "case", "unknown"), evaluations.map { it[3] }.toSet())
    }

    @Test
    fun `additive module one catalogue leaves published older profiles available`() {
        val profiles = PracticePlanner().profiles()
        assertTrue(
            setOf(
                "reading-basics", "present-verbs", "verb-article-gaps", "module-one-possession",
                "module-one-reading-words",
            ).all { old ->
                profiles.any { it.focusId == old }
            },
        )
        assertEquals(PROFILES, profiles.filter { it.focusId in PROFILES }.map { it.focusId }.toSet())
    }

    @Test
    fun `personal plan alternates selection and recall and requires complete teaching`() {
        val plan = PracticePlanner().plan("module-one-personal", "module-one-personal-1", 47)
        assertEquals(listOf("E022", "E023", "E022", "E023", "E022", "E023"), plan.items.map { it.exercise.formatId })
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-known-outcome-1", plan.completionRuleId)
        assertEquals("module-one-exposure-1", plan.exposurePolicyId)
        assertTrue(plan.introduction.size >= 15)
        assertTrue(plan.introductionExposureKeys.orEmpty().containsAll(
            listOf(
                "module-one:pattern:personal-introduction-present",
                "module-one:verb-infinitive:L028", "module-one:verb-present:L028",
                "module-one:verb-infinitive:L029", "module-one:verb-present:L029",
                "module-one:verb-infinitive:L030", "module-one:verb-present:L030",
                "module-one:supplied-component:L031", "module-one:supplied-component:L032",
            ),
        ))
    }

    @Test
    fun `provenance pins release revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=f0c95687c387bd23652cab2f50311e8bda756f20" in provenance)
        assertTrue("releaseVersion=0.14.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue(
            "planPairs=module-one-personal-1:pilot-12/generator-12/evaluation-12" in provenance,
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
        assertFailsWith<IllegalArgumentException> { PersonalPracticeCompatibilityFixture.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/personal-practice-0.14.0.tsv"
        const val PROVENANCE_PATH = "golden/personal-practice-0.14.0.provenance"
        const val FIXTURE_ROW_COUNT = 40
        const val FIXTURE_SHA256 = "6dea8fbdbf18bc2ef26b58182df9b5cf3cc0506c9c110a6abf5cafe611066b52"
        val PROFILES = setOf("module-one-personal")
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
    }
}
