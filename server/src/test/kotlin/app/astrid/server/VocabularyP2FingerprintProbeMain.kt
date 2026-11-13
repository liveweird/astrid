package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.engine.VocabularyLexicalKind
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import kotlinx.serialization.encodeToString

object VocabularyP2FingerprintProbeMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val mode = args.single()
        val identityHashes = primeIdentityHashes(mode)
        println("META\t$mode\t${identityHashes.joinToString(",")}")
        val engine = PilotEngine()
        val seeds = listOf(0, 47, Int.MAX_VALUE)
        val p2Formats = listOf("E040", "E041", "E042", "E043", "E044")
        seeds.forEach { seed ->
            val generated = p2Formats.flatMap { format ->
                (0 until P2_PROBE_INDEX_COUNT).map { index ->
                    engine.generate(
                        GenerationRequest(
                            format,
                            "G005".takeIf { format == "E043" },
                            seed,
                            index,
                            "pilot-19",
                            "generator-19",
                        ),
                    )
                }
            }
            val byTask = generated.associateBy { exercise ->
                requireNotNull(exercise.inspector.vocabularySource).taskId
            }
            check(byTask.size == P2_TASK_COUNT) { "Expected $P2_TASK_COUNT P2 tasks, found ${byTask.keys.sorted()}" }
            byTask.toSortedMap().forEach { (taskId, exercise) ->
                println(fingerprintRow("P2", seed, taskId, exercise))
            }
            (0 until INHERITED_PROBE_INDEX_COUNT).forEach { index ->
                val exercise = engine.generate(GenerationRequest("E033", null, seed, index, "pilot-19", "generator-19"))
                println(fingerprintRow("E033", seed, index.toString(), exercise))
            }
        }
    }

    private fun fingerprintRow(
        format: String,
        seed: Int,
        discriminator: String,
        exercise: app.astrid.engine.GeneratedExercise,
    ) = listOf(
        format,
        seed,
        discriminator,
        wireJson.encodeToString(exercise.toResponse(STABLE_EXERCISE_ID)),
        exercise.reproductionFingerprint(),
    ).joinToString("\t")

    private fun primeIdentityHashes(mode: String): List<String> {
        if (mode == "churn-reverse") repeat(IDENTITY_HASH_CHURN_COUNT) { System.identityHashCode(Any()) }
        val kinds = if (mode == "forward") VocabularyLexicalKind.entries else VocabularyLexicalKind.entries.reversed()
        kinds.forEach(System::identityHashCode)
        return VocabularyLexicalKind.entries.map { kind -> "${kind.name}=${System.identityHashCode(kind)}" }
    }

    private const val P2_TASK_COUNT = 21
    private const val P2_PROBE_INDEX_COUNT = 32
    private const val INHERITED_PROBE_INDEX_COUNT = 8
    private const val IDENTITY_HASH_CHURN_COUNT = 4096
    private const val STABLE_EXERCISE_ID = "00000000-0000-0000-0000-000000000019"
}
