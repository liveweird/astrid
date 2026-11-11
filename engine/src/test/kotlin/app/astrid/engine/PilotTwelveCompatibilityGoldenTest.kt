package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PilotTwelveCompatibilityGoldenTest {
    @Test
    fun `published pilot twelve generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotTwelveCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..25).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf(
            "E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8, "E020" to 2, "E021" to 2,
            "E022" to 8, "E023" to 8, "E024" to 3, "E025" to 3,
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
        assertTrue("releaseSourceCommit=f0c95687c387bd23652cab2f50311e8bda756f20" in provenance)
        assertTrue("releaseVersion=0.14.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue("contentRevision=pilot-12" in provenance)
        assertTrue("generatorRevision=generator-12" in provenance)
        assertTrue("evaluationRevision=evaluation-12" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairTwelveReviewBytes().sha256())
    }

    @Test
    fun `teaching review contains all eight released guided introductions`() {
        val review = publishedPairTwelveReviewBytes().toString(Charsets.UTF_8)
        val revisions = setOf(
            "module-one-singular-1", "module-one-groups-1", "module-one-er-1", "module-one-things-1",
            "module-one-present-1", "module-one-order-1", "module-one-possession-1",
            "module-one-personal-1",
        )
        revisions.forEach { assertTrue(it in review, it) }
    }

    @Test
    fun `pair twelve private task and lexical metadata are included`() {
        val tasks = listOf("E022", "E023").flatMap {
            PresentOrderContent.tasks(it, includePossession = true, includePersonal = true)
        }.filterIsInstance<NamedPresentTask>()
        assertEquals(16, tasks.size)
        val personal = tasks.filter { it.scopeId == PresentOrderContent.PERSONAL_SCOPE_ID }
        assertEquals(6, personal.size)
        assertEquals(setOf("C002", "C004", "C006"), personal.map { it.constructionId }.toSet())
        assertEquals(setOf("LANGUAGE_COMPLEMENT", "LOCATION_COMPLEMENT", "PREDICATIVE"), personal.mapNotNull { it.complementRole }.toSet())
        assertTrue(personal.all { it.knownPresentForms.isNotEmpty() && it.teaching != null && it.suppliedLabel.isNotBlank() })
        assertEquals(
            setOf("L028", "L029", "L030", "L031", "L032"),
            PilotContent.currentSnapshot.lexicalReview.map { it.id }.filter { it in PAIR_TWELVE_IDS }.toSet(),
        )
    }

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-twelve-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotTwelveCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-12-generator-12-evaluation-12.tsv"
        const val PROVENANCE_PATH = "golden/pilot-12-generator-12-evaluation-12.provenance"
        const val FIXTURE_ROW_COUNT = 3140
        const val FIXTURE_SHA256 = "4e0a0ff88d01279c6aa6c3fa3d39696c87149894eaa207a6904189b5456b3464"
        const val REVIEW_SHA256 = "51e6d235c750481545a03e418fd78a28c840bb9ea6147b5333c53b8007991798"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val PAIR_TWELVE_IDS = setOf("L028", "L029", "L030", "L031", "L032")
    }
}
