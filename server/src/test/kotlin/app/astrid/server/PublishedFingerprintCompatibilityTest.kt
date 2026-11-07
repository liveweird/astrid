package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.reproductionFingerprint
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals

class PublishedFingerprintCompatibilityTest {
    @Test
    fun `all frozen published fingerprints remain exact`() {
        val bytes = checkNotNull(javaClass.getResourceAsStream(FIXTURE_PATH)).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val rows = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(978, rows.size)

        val engine = PilotEngine()
        rows.forEachIndexed { rowIndex, row ->
            val columns = row.split('\t')
            assertEquals(6, columns.size, "Malformed frozen row ${rowIndex + 1}")
            val revision = columns[0]
            val request = GenerationRequest(
                formatId = columns[1],
                targetSkillId = columns[2],
                seed = columns[3].toInt(),
                index = columns[4].toInt(),
                contentRevision = "pilot-$revision",
                generatorRevision = "generator-$revision",
            )
            assertEquals(
                columns[5],
                engine.generate(request).reproductionFingerprint(),
                "Published fingerprint drift at frozen row ${rowIndex + 1}",
            )
        }
    }

    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val FIXTURE_PATH = "/golden/published-fingerprints.tsv"
        const val FIXTURE_SHA256 = "2833d9891daa1bde37f58cc8bee74558e519118c48d455339175af7eb1497cd2"
    }
}
