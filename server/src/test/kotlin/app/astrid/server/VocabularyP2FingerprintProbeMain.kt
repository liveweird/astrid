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
            val p3Formats = listOf("E045", "E046", "E047", "E048", "E049", "E050", "E051")
            val p3ByTask = p3Formats.flatMap { format ->
                (0 until P3_PROBE_INDEX_COUNT).map { index ->
                    val target = when (format) { "E048" -> "G004"; "E049" -> "G005"; "E051" -> "G016"; else -> null }
                    engine.generate(GenerationRequest(format, target, seed, index, "pilot-20", "generator-20"))
                }
            }.associateBy { requireNotNull(it.inspector.vocabularySource).taskId }
            check(p3ByTask.size == P3_TASK_COUNT) { "Expected $P3_TASK_COUNT P3 tasks, found ${p3ByTask.keys.sorted()}" }
            p3ByTask.toSortedMap().forEach { (taskId, exercise) ->
                println(fingerprintRow("P3", seed, taskId, exercise))
            }
            listOf("E033", "E042").forEach { format ->
                (0 until INHERITED_PROBE_INDEX_COUNT).forEach { index ->
                    val exercise = engine.generate(GenerationRequest(format, null, seed, index, "pilot-20", "generator-20"))
                    println(fingerprintRow("${format}P20", seed, index.toString(), exercise))
                }
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
    private const val P3_PROBE_INDEX_COUNT = 32
    private const val P3_TASK_COUNT = 23
    private const val IDENTITY_HASH_CHURN_COUNT = 4096
    private const val STABLE_EXERCISE_ID = "00000000-0000-0000-0000-000000000019"
}
