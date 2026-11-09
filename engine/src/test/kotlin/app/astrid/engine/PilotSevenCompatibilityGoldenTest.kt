package app.astrid.engine

import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotSevenCompatibilityGoldenTest {
    private val engine = PilotEngine()

    @Test
    fun `published pilot seven generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        expected.forEachIndexed { index, row ->
            assertEquals(row, replay(row), "Published pilot-7 drift at frozen row ${index + 1}")
        }
    }

    @Test
    fun `pilot seven archive covers all gap states ordered arrays and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        val gapGeneration = rows.filter { it[0] == "G" && it[1] == "E015" }
            .map { it[3].toInt() to it[4].toInt() }.toSet()
        (0 until GAP_CYCLE_SIZE).forEach { index ->
            assertTrue(FIXED_SEED to index in gapGeneration, "E015 misses cycle index $index")
            val evaluationRows = rows.filter { it[0] == "P" && it[1] == "E015" && it[3] == "$FIXED_SEED" && it[4] == "$index" }
            val exercise = engine.generate(GenerationRequest("E015", "G004", FIXED_SEED, index, "pilot-7", "generator-7"))
            assertEquals(GAP_ORDERED_ANSWER_COUNT, evaluationRows.count { isValidGapAnswer(it, exercise) })
        }
        BOUNDARY_SEEDS.forEach { seed ->
            GAP_BOUNDARY_INDICES.forEach { index ->
                assertTrue(seed to index in gapGeneration, "E015 misses boundary $seed/$index")
            }
        }
        val oracleRows = rows.filter { it[1] == "E015" && it[3] == "$FIXED_SEED" && it[4] == "0" }
        assertTrue(oracleRows.any { it[0] == "T" }, "E015 must freeze a raw Unicode wrong kind")
        assertTrue(oracleRows.any { it[0] == "O" }, "E015 must freeze an order wrong kind")
        assertTrue(oracleRows.any { it[0] == "M" }, "E015 must freeze a matching wrong kind")
    }

    @Test
    fun `pilot seven provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("baselineCommit=81388557402af7c562f23284bf9c9fb89480d573" in provenance)
        assertTrue("captureDate=2026-11-09" in provenance)
        assertTrue("contentRevision=pilot-7" in provenance)
        assertTrue("generatorRevision=generator-7" in provenance)
        assertTrue("evaluationRevision=evaluation-7" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
    }

    @Test
    fun `pilot seven teaching review remains byte exact`() {
        val bytes = PilotReview.renderPilotSevenMarkdown().toByteArray(Charsets.UTF_8)
        assertEquals(REVIEW_SHA256, bytes.sha256(), "The frozen pilot-seven review renderer must remain exact")
    }

    private fun isValidGapAnswer(fields: List<String>, exercise: GeneratedExercise): Boolean {
        val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
        val pairs = decodePairs(raw)
        val slots = requireNotNull(exercise.prompt.gapPrompt).slots.associateBy { it.id }
        return pairs.size <= slots.size && pairs.map { it.first }.distinct().size == pairs.size &&
            pairs.all { (slotId, choiceId) -> slots[slotId]?.choices?.any { it.id == choiceId } == true }
    }

    private fun replay(row: String): String {
        val fields = row.split('\t')
        val kind = fields[0]
        val format = fields[1]
        val target = fields[2].takeUnless { it == "-" }
        val seed = fields[3].toInt()
        val index = fields[4].toInt()
        val exercise = engine.generate(GenerationRequest(format, target, seed, index, "pilot-7", "generator-7"))
        val hash = when (kind) {
            "G" -> {
                assertEquals(6, fields.size, "Malformed generation row")
                PilotSevenCompatibilityGolden.generationHash(exercise)
            }
            "T" -> evaluationHash(fields, exercise) { Answer.Text(it) }
            "O" -> evaluationHash(fields, exercise) { raw ->
                Answer.Order(if (raw.isEmpty()) emptyList() else raw.split(PilotSevenCompatibilityGolden.ORDER_SEPARATOR))
            }
            "M" -> evaluationHash(fields, exercise) { raw -> Answer.Matching(decodePairs(raw).map { MatchingPair(it.first, it.second) }) }
            "P" -> evaluationHash(fields, exercise) { raw -> Answer.Gaps(decodePairs(raw).map { GapSelection(it.first, it.second) }) }
            else -> error("Unknown frozen row kind $kind")
        }
        return fields.dropLast(1).plus(hash).joinToString("\t")
    }

    private fun decodePairs(raw: String): List<Pair<String, String>> = if (raw.isEmpty()) {
        emptyList()
    } else {
        raw.split(PilotSevenCompatibilityGolden.PAIR_SEPARATOR).map { encodedPair ->
            val pair = encodedPair.split(PilotSevenCompatibilityGolden.ID_SEPARATOR, limit = 2)
            require(pair.size == 2) { "Malformed frozen pair" }
            pair[0] to pair[1]
        }
    }

    private fun evaluationHash(fields: List<String>, exercise: GeneratedExercise, answer: (String) -> Answer): String {
        assertEquals(7, fields.size, "Malformed evaluation row")
        val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
        return PilotSevenCompatibilityGolden.evaluationHash(engine.evaluate(exercise, answer(raw)))
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { byte -> "%02x".format(byte) }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-7-generator-7-evaluation-7.tsv"
        const val PROVENANCE_PATH = "golden/pilot-7-generator-7-evaluation-7.provenance"
        const val FIXTURE_ROW_COUNT = 3_675
        const val FIXTURE_SHA256 = "fe25485c618f3906ad25a8986779e45e58a8c9b0f910a69eb73651278db75393"
        const val REVIEW_SHA256 = "0a1caa4c367e8408ac8e35cbffa0249b524493d5c4122c8361be8081b4a3d0d2"
        const val FIXED_SEED = 47
        const val GAP_CYCLE_SIZE = 92
        const val GAP_ORDERED_ANSWER_COUNT = 13
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val GAP_BOUNDARY_INDICES = listOf(0, 1, GAP_CYCLE_SIZE - 1, GAP_CYCLE_SIZE, 1_000_000)
    }
}
