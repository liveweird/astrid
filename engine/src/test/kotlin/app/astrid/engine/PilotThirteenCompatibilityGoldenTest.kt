package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PilotThirteenCompatibilityGoldenTest {
    @Test
    fun `published pilot thirteen generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotThirteenCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..27).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf(
            "E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8, "E020" to 2, "E021" to 2,
            "E022" to 8, "E023" to 8, "E024" to 3, "E025" to 3, "E026" to 2, "E027" to 2,
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
        assertTrue(setOf("læse", "at læse", "læser", "LÆSE", "ukendt").all { it in moduleOneTextAnswers })
    }

    @Test
    fun `provenance pins release source revisions review and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=43e9726b079f4483103badbd87d2209da943adb8" in provenance)
        assertTrue("releaseVersion=0.15.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue("contentRevision=pilot-13" in provenance)
        assertTrue("generatorRevision=generator-13" in provenance)
        assertTrue("evaluationRevision=evaluation-13" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairThirteenReviewBytes().sha256())
    }

    @Test
    fun `teaching review contains all nine released guided introductions`() {
        val review = publishedPairThirteenReviewBytes().toString(Charsets.UTF_8)
        val revisions = setOf(
            "module-one-singular-1", "module-one-groups-1", "module-one-er-1", "module-one-things-1",
            "module-one-present-1", "module-one-order-1", "module-one-possession-1",
            "module-one-personal-1",
            "module-one-ability-1",
        )
        revisions.forEach { assertTrue(it in review, it) }
    }

    @Test
    fun `pair thirteen literal ability task and metadata membership are included`() {
        val tasks = listOf("E026", "E027").flatMap(AbilityContent::tasks)
        assertEquals(4, tasks.size)
        assertEquals(
            setOf("ability-select-letter", "ability-select-danish", "ability-recall-letter", "ability-recall-danish"),
            tasks.map { it.id }.toSet(),
        )
        assertTrue(tasks.all { it.targetScopeId == "kan-bare-infinitive" })
        assertTrue(tasks.all {
            it.answerExposureKeys == listOf(
                "module-one:bare-infinitive:L011", "module-one:at-infinitive:L011",
                "module-one:verb-present:L011", "module-one:pattern:kan-bare-infinitive",
            )
        })
        assertEquals(
            setOf("L011", "L031", "L033"),
            PilotContent.currentSnapshot.lexicalReview.map { it.id }.filter { it in PAIR_THIRTEEN_IDS }.toSet(),
        )
        assertTrue(PilotContent.currentSnapshot.lexicalReview.single { it.id == "L011" }.constraints.contains("C014"))
        assertTrue(PilotContent.currentSnapshot.lexicalReview.single { it.id == "L031" }.constraints.contains("C014"))
        assertEquals("Bare infinitive after kan", PilotContent.currentSnapshot.skills.single { it.id == "G030" }.title)
        assertEquals("C014", AbilityContent.CONSTRUCTION_ID)
        assertEquals(
            (1..27).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.currentSnapshot.formats.map { it.id }.toSet(),
        )
        assertEquals(
            (1..25).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotTwelveSnapshot.formats.map { it.id }.toSet(),
        )
    }

    @Test
    fun `ability archive spans choice positions and marker response shapes`() {
        val engine = PilotEngine()
        val choiceSeeds = (0..16).toList() + listOf(47, Int.MAX_VALUE)
        val selectionPositions = choiceSeeds.map { seed ->
            engine.generate(GenerationRequest("E026", "G030", seed, 0, "pilot-13", "generator-13"))
                .prompt.choices.indexOf("læse")
        }.toSet()
        assertEquals(setOf(0, 1, 2), selectionPositions)
        val selected = engine.generate(GenerationRequest("E026", "G030", 0, 0, "pilot-13", "generator-13"))
        val typed = engine.generate(GenerationRequest("E027", "G030", 0, 0, "pilot-13", "generator-13"))
        val selectedMarker = engine.evaluate(selected, Answer.Text("  at   læse  "))
        val typedMarker = engine.evaluate(typed, Answer.Text("at læse"))
        assertEquals(listOf("INFINITIVE_MARKER_AFTER_MODAL"), selectedMarker.issues.map { it.code })
        assertEquals(listOf("INFINITIVE_MARKER_AFTER_MODAL"), typedMarker.issues.map { it.code })
        assertEquals(Answer.Text("  at   læse  "), selectedMarker.rawAnswer)
        assertEquals(NormalizedAnswer.Text("at læse"), selectedMarker.normalizedAnswer)
        assertEquals(ResponseConstraintStatus.MET, selectedMarker.responseConstraintStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, typedMarker.responseConstraintStatus)
    }

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-thirteen-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotThirteenCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-13-generator-13-evaluation-13.tsv"
        const val PROVENANCE_PATH = "golden/pilot-13-generator-13-evaluation-13.provenance"
        const val FIXTURE_ROW_COUNT = 3216
        const val FIXTURE_SHA256 = "30eeeb34adc5a7ab7d3575dcdaab40e69a5831bccdb9d880c50e6d6549dbf065"
        const val REVIEW_SHA256 = "f2d4a0c9150ec05f526f29510999d4fbe9d377b1aa907d38420ae1518a596e02"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val PAIR_THIRTEEN_IDS = setOf("L011", "L031", "L033")
    }
}
