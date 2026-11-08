package app.astrid.engine

import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotSixCompatibilityGoldenTest {
    private val engine = PilotEngine()

    @Test
    fun `published pilot six generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        expected.forEachIndexed { index, row ->
            assertEquals(row, replay(row), "Published pilot-6 drift at frozen row ${index + 1}")
        }
    }

    @Test
    fun `pilot six archive covers inherited cycles and bounded matching behavior`() {
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
        val matchingGeneration = rows.filter { it[0] == "G" && it[1] == "E014" }
            .map { it[3].toInt() to it[4].toInt() }.toSet()
        (0 until MATCHING_SAMPLE_SIZE).forEach { index ->
            assertTrue(FIXED_SEED to index in matchingGeneration, "E014 misses bounded sample index $index")
        }
        BOUNDARY_SEEDS.forEach { seed ->
            MATCHING_BOUNDARY_INDICES.forEach { index ->
                assertTrue(seed to index in matchingGeneration, "E014 misses boundary $seed/$index")
            }
        }

        val matchingRows = rows.filter { it[0] == "M" && it[1] == "E014" }
        val exhaustiveRows = matchingRows.filter { it[3].toInt() == FIXED_SEED && it[4].toInt() == 0 }
        assertEquals(MATCHING_EVALUATION_ROWS_AT_ORACLE, exhaustiveRows.size)
        assertTrue(rows.any { it[0] == "T" && it[1] == "E014" }, "E014 must freeze a raw Unicode wrong kind")
        assertTrue(rows.any { it[0] == "O" && it[1] == "E014" }, "E014 must freeze an order wrong kind")
    }

    @Test
    fun `pilot six provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("baselineCommit=6d1c0fe2f957490216fda50e4a67a216c3b784ed" in provenance)
        assertTrue("captureDate=2026-11-09" in provenance)
        assertTrue("contentRevision=pilot-6" in provenance)
        assertTrue("generatorRevision=generator-6" in provenance)
        assertTrue("evaluationRevision=evaluation-6" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
    }

    @Test
    fun `pilot six teaching review remains byte exact`() {
        val bytes = PilotReview.renderPilotSixMarkdown().toByteArray(Charsets.UTF_8)
        assertEquals(REVIEW_SHA256, bytes.sha256(), "The frozen pilot-six review renderer must remain exact")
    }

    private fun replay(row: String): String {
        val fields = row.split('\t')
        val kind = fields[0]
        val format = fields[1]
        val target = fields[2].takeUnless { it == "-" }
        val seed = fields[3].toInt()
        val index = fields[4].toInt()
        val exercise = engine.generate(GenerationRequest(format, target, seed, index, "pilot-6", "generator-6"))
        val hash = when (kind) {
            "G" -> {
                assertEquals(6, fields.size, "Malformed generation row")
                PilotSixCompatibilityGolden.generationHash(exercise)
            }
            "T" -> evaluationHash(fields, exercise) { Answer.Text(it) }
            "O" -> evaluationHash(fields, exercise) { raw ->
                Answer.Order(if (raw.isEmpty()) emptyList() else raw.split(PilotSixCompatibilityGolden.ORDER_SEPARATOR))
            }
            "M" -> evaluationHash(fields, exercise) { raw ->
                Answer.Matching(
                    if (raw.isEmpty()) {
                        emptyList()
                    } else {
                        raw.split(PilotSixCompatibilityGolden.PAIR_SEPARATOR).map { encodedPair ->
                            val pair = encodedPair.split(PilotSixCompatibilityGolden.ID_SEPARATOR, limit = 2)
                            require(pair.size == 2) { "Malformed frozen matching pair" }
                            MatchingPair(pair[0], pair[1])
                        }
                    },
                )
            }
            else -> error("Unknown frozen row kind $kind")
        }
        return fields.dropLast(1).plus(hash).joinToString("\t")
    }

    private fun evaluationHash(
        fields: List<String>,
        exercise: GeneratedExercise,
        answer: (String) -> Answer,
    ): String {
        assertEquals(7, fields.size, "Malformed evaluation row")
        val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
        return PilotSixCompatibilityGolden.evaluationHash(engine.evaluate(exercise, answer(raw)))
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-6-generator-6-evaluation-6.tsv"
        const val PROVENANCE_PATH = "golden/pilot-6-generator-6-evaluation-6.provenance"
        const val FIXTURE_ROW_COUNT = 2_365
        const val FIXTURE_SHA256 = "c027bf9d370a0e2974f02120cca98e953820daf9b8c8e5699bc29c563b8636f8"
        const val REVIEW_SHA256 = "f6e95d88a629b162745e93b4952f87baca43624a82f12efca407d3ec0c61bab7"
        const val FIXED_SEED = 47
        const val MAX_INDEX = 1_000_000
        const val MATCHING_SAMPLE_SIZE = 32
        const val MATCHING_EVALUATION_ROWS_AT_ORACLE = 217 // One sampled correct + 209 exhaustive + 2 reordered + 5 malformed matching.
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val MATCHING_BOUNDARY_INDICES = listOf(0, 1, MAX_INDEX)
        val FORMAT_CYCLES = mapOf(
            "E001" to 99, "E002" to 92, "E003" to 99, "E004" to 99, "E005" to 99,
            "E006" to 92, "E007" to 3, "E008" to 3, "E009" to 99, "E010" to 24,
            "E011" to 4, "E012" to 99, "E013" to 99,
        )
        val FORMAT_TARGETS = mapOf(
            "E001" to "G004", "E002" to "G006", "E003" to "G005", "E004" to "G004",
            "E005" to "G004", "E006" to "G006", "E007" to "-", "E008" to "-",
            "E009" to "G005", "E010" to "-", "E011" to "-", "E012" to "G005", "E013" to "G004",
        )
    }
}
