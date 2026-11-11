package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CircumstancePracticeCompatibilityTest {
    @Test
    fun `published 0_17_0 circumstance practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, CircumstancePracticeCompatibilityFixture.rows())
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
                "missing-attempt" to false,
                "met" to true,
                "target-last-core-wrong" to true,
                "missing-correction" to true,
                "order-correction" to true,
                "malformed" to false,
            ),
            completion.getValue("module-one-order-outcome-1"),
        )
        val exposure = rows.filter { it[0] == "EXPOSURE" }.groupBy { it[1] }
            .mapValues { entry -> entry.value.associate { "${it[2]}:${it[3]}" to it[4] } }
        assertTrue(PROFILES.all { profile -> exposure.getValue(profile).values.all { it != "-" } })
        val evaluations = rows.filter { it[0] == "EVALUATION" }
        assertEquals(36, evaluations.size)
        assertEquals((0..3).toSet(), evaluations.map { it[1].toInt() }.toSet())
        assertEquals(setOf("E029"), evaluations.map { it[2] }.toSet())
        assertTrue(setOf("canonical", "target-last-core-wrong", "target-not-last", "partial-missing", "unknown", "wrong-kind").all {
            case -> evaluations.any { it[3] == case }
        })
    }

    @Test
    fun `additive module one catalogue leaves published older profiles available`() {
        val profiles = PracticePlanner().profiles()
        assertTrue(
            setOf(
                "reading-basics", "present-verbs", "verb-article-gaps", "module-one-possession",
                "module-one-reading-words", "module-one-personal",
            ).all { old ->
                profiles.any { it.focusId == old }
            },
        )
        assertEquals(PROFILES, profiles.filter { it.focusId in PROFILES }.map { it.focusId }.toSet())
    }

    @Test
    fun `circumstance plan pins full bank teaching and scoped exposure`() {
        PROFILES.forEach { focus ->
            val plan = PracticePlanner().plan(focus, "$focus-1", 47)
            assertEquals(List(4) { "E029" }, plan.items.map { it.exercise.formatId })
            assertTrue(plan.introductionRequired)
            assertEquals("module-one-order-outcome-1", plan.completionRuleId)
            assertEquals("module-one-order-exposure-1", plan.exposurePolicyId)
            assertEquals(12, plan.introduction.size)
            assertTrue(plan.introductionExposureKeys.orEmpty().contains("module-one:pattern:one-final-circumstance"))
            assertTrue(plan.introductionExposureKeys.orEmpty().contains("module-one:meaning:L037:L037-S01"))
            assertTrue(plan.introductionExposureKeys.orEmpty().contains("module-one:meaning:L038:L038-S01"))
            plan.items.forEach { item ->
                assertEquals(
                    listOf(
                        "module-one:exact-order:${item.exercise.inspector.canonicalSentenceId}",
                        "module-one:pattern:one-final-circumstance",
                    ),
                    item.feedbackExposureKeys,
                )
            }
            assertTrue(plan.items.all { it.lessonExposureKeys == plan.introductionExposureKeys })
        }
    }

    @Test
    fun `provenance pins release revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=0f1b322903fc243871b3b7293a26b52af80dd0dc" in provenance)
        assertTrue("releaseVersion=0.17.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue(
            "module-one-circumstances-1:pilot-15/generator-15/evaluation-15" in provenance,
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
        assertFailsWith<IllegalArgumentException> { CircumstancePracticeCompatibilityFixture.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/circumstance-practice-0.17.0.tsv"
        const val PROVENANCE_PATH = "golden/circumstance-practice-0.17.0.provenance"
        const val FIXTURE_ROW_COUNT = 60
        const val FIXTURE_SHA256 = "1504cdd5a0b624b4a148352182e910535b11dc7410c92d100ea38bd2ca228c4f"
        val PROFILES = setOf("module-one-circumstances")
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
    }
}
