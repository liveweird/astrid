package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ModalRoundPracticeCompatibilityTest {
    @Test
    fun `published 0_16_0 modal round practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, ModalRoundPracticeCompatibilityFixture.rows())
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
                "known-wrong" to true,
                "missing-correction" to true,
                "order-correction" to true,
                "unknown" to false,
                "malformed" to false,
            ),
            completion.getValue("module-one-modal-round-outcome-1"),
        )
        val exposure = rows.filter { it[0] == "EXPOSURE" }.groupBy { it[1] }
            .mapValues { entry -> entry.value.associate { "${it[2]}:${it[3]}" to it[4] } }
        assertTrue(PROFILES.all { profile -> exposure.getValue(profile).values.all { it != "-" } })
        val evaluations = rows.filter { it[0] == "EVALUATION" }
        assertEquals(42, evaluations.size)
        assertEquals(setOf(0, 1), evaluations.map { it[1].toInt() }.toSet())
        assertEquals(setOf("E025", "E028"), evaluations.map { it[2] }.toSet())
        assertTrue(setOf("canonical", "known-wrong", "partial-missing", "np-reversal", "order-violation", "unknown", "wrong-kind").all {
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
    fun `modal round plans pin interpretation then construction with full teaching and scoped exposure`() {
        PROFILES.forEach { focus ->
            val plan = PracticePlanner().plan(focus, "$focus-1", 47)
            assertEquals(listOf("E028", "E025"), plan.items.map { it.exercise.formatId })
            assertTrue(plan.introductionRequired)
            assertEquals("module-one-modal-round-outcome-1", plan.completionRuleId)
            assertEquals("module-one-modal-round-exposure-1", plan.exposurePolicyId)
            assertEquals(7, plan.introduction.size)
            assertTrue(plan.introductionExposureKeys.orEmpty().contains("module-one:bare-infinitive:L011"))
            assertTrue(plan.introductionExposureKeys.orEmpty().contains("module-one:pattern:contextual-modal-meaning"))
            assertTrue(plan.introductionExposureKeys.orEmpty().contains("module-one:pattern:supplied-modal-order"))
            assertEquals(1, plan.items[0].answerExposureKeys.size)
            assertEquals("module-one:pattern:supplied-modal-order", plan.items[1].answerExposureKeys.last())
            assertEquals(
                listOf(
                    "module-one:exact-order:${plan.items[1].exercise.inspector.canonicalSentenceId}",
                    "module-one:pattern:supplied-modal-order",
                ),
                plan.items[1].feedbackExposureKeys,
            )
            assertTrue(plan.items.all { it.lessonExposureKeys == plan.introductionExposureKeys })
        }
    }

    @Test
    fun `provenance pins release revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=9dd787c14b3a5d4a414da763b4ef28ce511e63be" in provenance)
        assertTrue("releaseVersion=0.16.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue(
            "module-one-volition-1:pilot-14/generator-14/evaluation-14" in provenance,
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
        assertFailsWith<IllegalArgumentException> { ModalRoundPracticeCompatibilityFixture.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/modal-round-practice-0.16.0.tsv"
        const val PROVENANCE_PATH = "golden/modal-round-practice-0.16.0.provenance"
        const val FIXTURE_ROW_COUNT = 83
        const val FIXTURE_SHA256 = "abe5f39811cde456c04c85d444a2b98af283127c858d855944a058acf8c6eca6"
        val PROFILES = setOf("module-one-volition", "module-one-obligation", "module-one-permission")
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
    }
}
