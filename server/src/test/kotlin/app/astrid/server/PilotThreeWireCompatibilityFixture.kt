package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import java.security.MessageDigest
import kotlinx.serialization.encodeToString

internal object PilotThreeWireCompatibilityFixture {
    const val BASELINE_COMMIT = "1d695c5"
    const val CONTENT_REVISION = "pilot-3"
    const val GENERATOR_REVISION = "generator-3"
    const val EXERCISE_ID = "00000000-0000-0000-0000-000000000301"
    const val MAX_INDEX = 1_000_000

    private val formats = listOf(
        FormatCycle("E001", "G004", 99),
        FormatCycle("E002", "G006", 92),
        FormatCycle("E003", "G005", 99),
        FormatCycle("E004", "G004", 99),
        FormatCycle("E005", "G004", 99),
        FormatCycle("E006", "G006", 92),
        FormatCycle("E007", null, 3),
        FormatCycle("E008", null, 3),
    )
    private val seeds = listOf(0, 47, Int.MAX_VALUE)

    fun rows(engine: PilotEngine = PilotEngine()): List<String> = buildList {
        formats.forEach { format ->
            seeds.forEach { seed ->
                repeat(format.cycleSize) { index -> add(row("C", format, seed, index, engine)) }
                listOf(format.cycleSize, MAX_INDEX).forEach { index ->
                    add(row("B", format, seed, index, engine))
                }
            }
        }
    }

    fun fixtureBytes(engine: PilotEngine = PilotEngine()): ByteArray =
        rows(engine).joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(fixtureBytes: ByteArray, rowCount: Int): String = buildString {
        appendLine("baselineCommit=$BASELINE_COMMIT")
        appendLine("contentRevision=$CONTENT_REVISION")
        appendLine("generatorRevision=$GENERATOR_REVISION")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${fixtureBytes.sha256()}")
        appendLine(
            "description=All eight pilot-3 formats; complete cycles for seeds 0, 47, and Int.MAX_VALUE " +
                "plus cycle and maximum-index boundaries; SHA-256 over public ExerciseResponse, inspector, " +
                "and reproduction fingerprint.",
        )
    }

    fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(it) }

    private fun row(kind: String, format: FormatCycle, seed: Int, index: Int, engine: PilotEngine): String {
        val exercise = engine.generate(
            GenerationRequest(
                formatId = format.id,
                targetSkillId = format.targetSkillId,
                seed = seed,
                index = index,
                contentRevision = CONTENT_REVISION,
                generatorRevision = GENERATOR_REVISION,
            ),
        )
        val response = wireJson.encodeToString(exercise.toResponse(EXERCISE_ID))
        val inspector = wireJson.encodeToString(exercise.toInspector(EXERCISE_ID))
        val publicWireHash = hashFields(response, inspector, exercise.reproductionFingerprint())
        return listOf(kind, format.id, format.targetSkillId.orEmpty(), seed, index, publicWireHash)
            .joinToString("\t")
    }

    private fun hashFields(vararg fields: String): String {
        val canonical = fields.joinToString("\u001f") { field ->
            val bytes = field.toByteArray(Charsets.UTF_8)
            "${bytes.size}:$field"
        }
        return canonical.toByteArray(Charsets.UTF_8).sha256()
    }

    private data class FormatCycle(val id: String, val targetSkillId: String?, val cycleSize: Int)
}
