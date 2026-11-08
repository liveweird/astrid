package app.astrid.engine

import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotThreeCompatibilityGoldenTest {
    private val engine = PilotEngine()

    @Test
    fun `published pilot three generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()

        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        expected.forEachIndexed { index, row ->
            assertEquals(row, replay(row), "Published pilot-3 drift at frozen row ${index + 1}")
        }
    }

    @Test
    fun `pilot three provenance identifies immutable release baseline`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }

        assertTrue("baselineCommit=1d695c5" in provenance)
        assertTrue("codeEquivalentCommit=e3c7556" in provenance)
        assertTrue("contentRevision=pilot-3" in provenance)
        assertTrue("generatorRevision=generator-3" in provenance)
        assertTrue("evaluationRevision=evaluation-3" in provenance)
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
        val exercise =
            engine.generate(GenerationRequest(format, target, seed, index, "pilot-3", "generator-3"))
        val hash =
            when (kind) {
                "G" -> {
                    assertEquals(6, fields.size, "Malformed generation row")
                    PilotThreeCompatibilityGolden.generationHash(exercise)
                }
                "T" -> {
                    assertEquals(7, fields.size, "Malformed text-evaluation row")
                    val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
                    PilotThreeCompatibilityGolden.evaluationHash(engine.evaluate(exercise, Answer.Text(raw)))
                }
                "O" -> {
                    assertEquals(7, fields.size, "Malformed order-evaluation row")
                    val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
                    val blockIds = if (raw.isEmpty()) emptyList() else raw.split(PilotThreeCompatibilityGolden.ORDER_SEPARATOR)
                    PilotThreeCompatibilityGolden.evaluationHash(engine.evaluate(exercise, Answer.Order(blockIds)))
                }
                else -> error("Unknown frozen row kind $kind")
            }
        return fields.dropLast(1).plus(hash).joinToString("\t")
    }

    private fun ByteArray.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(this)
        return digest.joinToString("") { byte ->
            val value = byte.toInt() and 0xff
            "${HEX_DIGITS[value ushr 4]}${HEX_DIGITS[value and 0x0f]}"
        }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-3-generator-3-evaluation-3.tsv"
        const val PROVENANCE_PATH = "golden/pilot-3-generator-3-evaluation-3.provenance"
        const val FIXTURE_ROW_COUNT = 1_320
        const val FIXTURE_SHA256 = "ab467eeb5a97c334b9af2d201625de3f17ff9dcde338a319b3b372aeb1e12f0b"
        const val HEX_DIGITS = "0123456789abcdef"
    }
}
