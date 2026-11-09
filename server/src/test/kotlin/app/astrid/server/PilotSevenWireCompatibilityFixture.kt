package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import java.security.MessageDigest
import kotlinx.serialization.encodeToString

internal object PilotSevenWireCompatibilityFixture {
    const val BASELINE_COMMIT = "81388557402af7c562f23284bf9c9fb89480d573"
    const val CONTENT_REVISION = "pilot-7"
    const val GENERATOR_REVISION = "generator-7"
    const val EVALUATION_REVISION = "evaluation-7"
    const val EXERCISE_ID = "00000000-0000-0000-0000-000000000701"
    const val MAX_INDEX = 1_000_000

    private val formats = listOf(
        FormatCycle("E001", "G004", 99), FormatCycle("E002", "G006", 92), FormatCycle("E003", "G005", 99),
        FormatCycle("E004", "G004", 99), FormatCycle("E005", "G004", 99), FormatCycle("E006", "G006", 92),
        FormatCycle("E007", null, 3), FormatCycle("E008", null, 3), FormatCycle("E009", "G005", 99),
        FormatCycle("E010", null, 24), FormatCycle("E011", null, 4), FormatCycle("E012", "G005", 99),
        FormatCycle("E013", "G004", 99), FormatCycle("E015", "G004", 92),
    )
    private val seeds = listOf(0, 47, Int.MAX_VALUE)

    fun rows(engine: PilotEngine = PilotEngine()): List<String> = buildList {
        formats.forEach { format ->
            seeds.forEach { seed ->
                repeat(format.cycleSize) { index -> add(row("C", format.id, format.targetSkillId, seed, index, engine)) }
                listOf(format.cycleSize, MAX_INDEX).forEach { index ->
                    add(row("B", format.id, format.targetSkillId, seed, index, engine))
                }
            }
        }
        repeat(MATCHING_SAMPLE_SIZE) { index -> add(row("M", "E014", null, FIXED_SEED, index, engine)) }
        BOUNDARY_SEEDS.forEach { seed ->
            MATCHING_BOUNDARY_INDICES.forEach { index -> add(row("B", "E014", null, seed, index, engine)) }
        }
    }

    fun fixtureBytes(engine: PilotEngine = PilotEngine()): ByteArray =
        rows(engine).joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(fixtureBytes: ByteArray, rowCount: Int): String = buildString {
        appendLine("baselineCommit=$BASELINE_COMMIT")
        appendLine("captureDate=2026-11-09")
        appendLine("contentRevision=$CONTENT_REVISION")
        appendLine("generatorRevision=$GENERATOR_REVISION")
        appendLine("evaluationRevision=$EVALUATION_REVISION")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${fixtureBytes.sha256()}")
        appendLine(
            "description=All inherited cycles for seeds 0, 47, and Int.MAX_VALUE, bounded E014 samples, and the " +
                "complete E015 cycle and boundaries; SHA-256 over byte-exact public response, private nested inspector, " +
                "and reproduction fingerprint.",
        )
    }

    fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(it) }

    private fun row(
        kind: String,
        formatId: String,
        targetSkillId: String?,
        seed: Int,
        index: Int,
        engine: PilotEngine,
    ): String {
        val exercise = engine.generate(
            GenerationRequest(formatId, targetSkillId, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
        )
        val response = wireJson.encodeToString(exercise.toResponse(EXERCISE_ID))
        val inspector = wireJson.encodeToString(exercise.toInspector(EXERCISE_ID))
        val wireHash = hashFields(response, inspector, exercise.reproductionFingerprint())
        return listOf(kind, formatId, targetSkillId.orEmpty(), seed, index, wireHash).joinToString("\t")
    }

    private fun hashFields(vararg fields: String): String {
        val canonical = fields.joinToString("\u001f") { field ->
            val bytes = field.toByteArray(Charsets.UTF_8)
            "${bytes.size}:$field"
        }
        return canonical.toByteArray(Charsets.UTF_8).sha256()
    }

    private data class FormatCycle(val id: String, val targetSkillId: String?, val cycleSize: Int)

    private const val FIXED_SEED = 47
    private const val MATCHING_SAMPLE_SIZE = 32
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    private val MATCHING_BOUNDARY_INDICES = listOf(0, 1, MAX_INDEX)
}
