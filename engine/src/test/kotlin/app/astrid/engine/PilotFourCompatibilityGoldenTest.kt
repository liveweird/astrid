package app.astrid.engine

import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotFourCompatibilityGoldenTest {
    private val engine = PilotEngine()

    @Test
    fun `published pilot four generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()

        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        expected.forEachIndexed { index, row ->
            assertEquals(row, replay(row), "Published pilot-4 drift at frozen row ${index + 1}")
        }
    }

    @Test
    fun `pilot four archive covers all cycles boundaries and grading facets`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }

        FORMAT_CYCLES.forEach { (format, cycleSize) ->
            val target = FORMAT_TARGETS.getValue(format)
            val generationKeys = rows.filter { it[0] == "G" && it[1] == format }
                .map { Triple(it[3].toInt(), it[4].toInt(), it[2]) }.toSet()
            (0 until cycleSize).forEach { index ->
                assertTrue(Triple(FIXED_SEED, index, target) in generationKeys, "$format misses cycle index $index")
            }
            BOUNDARY_SEEDS.forEach { seed ->
                listOf(0, cycleSize - 1, cycleSize, MAX_INDEX).forEach { index ->
                    assertTrue(Triple(seed, index, target) in generationKeys, "$format misses $seed/$index")
                }
            }
        }

        val writingAnswers = rows.filter { it[0] == "T" && it[1] == "E009" }
            .map { String(Base64.getDecoder().decode(it[5]), Charsets.UTF_8) }.toSet()
        WRITING_ORACLES.forEach { answer -> assertTrue(answer in writingAnswers, "Missing writing oracle '$answer'") }
    }

    @Test
    fun `pilot four provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }

        assertTrue("baselineCommit=e37749c" in provenance)
        assertTrue("contentRevision=pilot-4" in provenance)
        assertTrue("generatorRevision=generator-4" in provenance)
        assertTrue("evaluationRevision=evaluation-4" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
    }

    private fun replay(row: String): String {
        val fields = row.split('\t')
        val kind = fields[0]
        val format = fields[1]
        val target = fields[2].takeUnless { it == "-" }
        val seed = fields[3].toInt()
        val index = fields[4].toInt()
        val exercise = engine.generate(GenerationRequest(format, target, seed, index, "pilot-4", "generator-4"))
        val hash =
            when (kind) {
                "G" -> {
                    assertEquals(6, fields.size, "Malformed generation row")
                    PilotFourCompatibilityGolden.generationHash(exercise)
                }
                "T" -> {
                    assertEquals(7, fields.size, "Malformed text-evaluation row")
                    val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
                    PilotFourCompatibilityGolden.evaluationHash(engine.evaluate(exercise, Answer.Text(raw)))
                }
                "O" -> {
                    assertEquals(7, fields.size, "Malformed order-evaluation row")
                    val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
                    val blockIds = if (raw.isEmpty()) emptyList() else raw.split(PilotFourCompatibilityGolden.ORDER_SEPARATOR)
                    PilotFourCompatibilityGolden.evaluationHash(engine.evaluate(exercise, Answer.Order(blockIds)))
                }
                else -> error("Unknown frozen row kind $kind")
            }
        return fields.dropLast(1).plus(hash).joinToString("\t")
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-4-generator-4-evaluation-4.tsv"
        const val PROVENANCE_PATH = "golden/pilot-4-generator-4-evaluation-4.provenance"
        const val FIXTURE_ROW_COUNT = 1_614
        const val FIXTURE_SHA256 = "5fbe98aadf7489d96678e936bdb7829940795abb1d1bda5ff4a0e62277678053"
        const val FIXED_SEED = 47
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val FORMAT_CYCLES = mapOf(
            "E001" to 99,
            "E002" to 92,
            "E003" to 99,
            "E004" to 99,
            "E005" to 99,
            "E006" to 92,
            "E007" to 3,
            "E008" to 3,
            "E009" to 99,
            "E010" to 24,
        )
        val FORMAT_TARGETS = mapOf(
            "E001" to "G004",
            "E002" to "G006",
            "E003" to "G005",
            "E004" to "G004",
            "E005" to "G004",
            "E006" to "G006",
            "E007" to "-",
            "E008" to "-",
            "E009" to "G005",
            "E010" to "-",
        )
        val WRITING_ORACLES = setOf(
            "Hun læser en avis.",
            "En avis læser hun.",
            "Han læser en avis.",
            "Hun har en avis.",
            "Hun læser et avis.",
            "Hun læse en avis.",
            "Hun studerer en avis.",
            "Hun læser en avis. Han sover.",
            " i  læser et brev. ",
            "Hun læser en bo\u0301g.",
        )
    }
}
