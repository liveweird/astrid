package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AbilityPracticeCompatibilityTest {
    @Test
    fun `published 0_15_0 ability practice remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, AbilityPracticeCompatibilityFixture.rows())
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
        assertTrue(exposure.getValue("module-one-ability").values.all { it != "-" })
        val evaluations = rows.filter { it[0] == "EVALUATION" }
        assertEquals(28, evaluations.size)
        assertEquals((0..3).toSet(), evaluations.map { it[1].toInt() }.toSet())
        assertEquals(setOf("E026", "E027"), evaluations.map { it[2] }.toSet())
        assertEquals(
            setOf("canonical", "normalized", "at-marker", "finite", "case", "unknown", "wrong-kind"),
            evaluations.map { it[3] }.toSet(),
        )
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
    fun `ability plan pins selection then recall with full teaching and scoped exposure`() {
        val plan = PracticePlanner().plan("module-one-ability", "module-one-ability-1", 47)
        assertEquals(listOf("E026", "E026", "E027", "E027"), plan.items.map { it.exercise.formatId })
        assertTrue(plan.introductionRequired)
        assertEquals("module-one-known-outcome-1", plan.completionRuleId)
        assertEquals("module-one-exposure-1", plan.exposurePolicyId)
        assertEquals(9, plan.introduction.size)
        assertEquals(
            listOf(
                "module-one:verb-infinitive:L033", "module-one:verb-present:L033",
                "module-one:at-infinitive:L011", "module-one:bare-infinitive:L011",
                "module-one:verb-present:L011", "module-one:pattern:kan-bare-infinitive",
                "module-one:supplied-component:L023", "module-one:supplied-component:L018",
                "module-one:supplied-component:L031",
            ),
            plan.introductionExposureKeys,
        )
        val latestAnswerKeys = listOf(
            "module-one:bare-infinitive:L011", "module-one:at-infinitive:L011",
            "module-one:verb-present:L011", "module-one:pattern:kan-bare-infinitive",
        )
        assertTrue(plan.items.all { it.answerExposureKeys == latestAnswerKeys })
        assertTrue(plan.items.all { "module-one:verb-present:L033" !in it.answerExposureKeys })
        assertTrue(plan.items.all { it.lessonExposureKeys == plan.introductionExposureKeys })
    }

    @Test
    fun `provenance pins release revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=43e9726b079f4483103badbd87d2209da943adb8" in provenance)
        assertTrue("releaseVersion=0.15.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue(
            "planPairs=module-one-ability-1:pilot-13/generator-13/evaluation-13" in provenance,
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
        assertFailsWith<IllegalArgumentException> { AbilityPracticeCompatibilityFixture.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/ability-practice-0.15.0.tsv"
        const val PROVENANCE_PATH = "golden/ability-practice-0.15.0.provenance"
        const val FIXTURE_ROW_COUNT = 44
        const val FIXTURE_SHA256 = "4b68503bcc2a17f61a0402b2f5a04bef1e97c347facb62ac7c125397eed3b332"
        val PROFILES = setOf("module-one-ability")
        val SEEDS = setOf(0, 1, 47, Int.MAX_VALUE)
    }
}
