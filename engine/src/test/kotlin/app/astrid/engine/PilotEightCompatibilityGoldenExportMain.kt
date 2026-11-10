package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotEightCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotEightCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotEightCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal fun publishedPairEightReviewBytes(): ByteArray {
    return PilotReview.renderPilotEightMarkdown().toByteArray(Charsets.UTF_8)
}

internal object PilotEightCompatibilityGolden {
    private val engine = PilotEngine()
    private val inheritedFormats = listOf(
        FormatSpec("E001", "G004", 99), FormatSpec("E002", "G006", 92), FormatSpec("E003", "G005", 99),
        FormatSpec("E004", "G004", 99), FormatSpec("E005", "G004", 99), FormatSpec("E006", "G006", 92),
        FormatSpec("E007", null, 3), FormatSpec("E008", null, 3), FormatSpec("E009", "G005", 99),
        FormatSpec("E010", null, 24), FormatSpec("E011", null, 4), FormatSpec("E012", "G005", 99),
        FormatSpec("E013", "G004", 99), FormatSpec("E014", null, 32), FormatSpec("E015", "G004", 92),
    )
    private val moduleOneFormats = listOf(
        FormatSpec("E016", "G003", ModuleOneContent.tasks("E016").size),
        FormatSpec("E017", "G003", ModuleOneContent.tasks("E017").size),
        FormatSpec("E018", "G004", ModuleOneContent.tasks("E018").size),
        FormatSpec("E019", "G004", ModuleOneContent.tasks("E019").size),
    )

    fun rows(): List<String> {
        requireLiteralPairEight()
        return buildList {
            inheritedFormats.forEach { format ->
                repeat(format.cycleSize) { index ->
                    val exercise = generate(format, FIXED_SEED, index)
                    add(generationRow(exercise))
                    add(evaluationRow(exercise, correctAnswer(exercise)))
                }
                BOUNDARY_SEEDS.forEach { seed ->
                    listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).forEach { index ->
                        add(generationRow(generate(format, seed, index)))
                    }
                }
            }
            moduleOneFormats.forEach { format ->
                val exercises = authoredExercises(format)
                require(exercises.size == format.cycleSize)
                exercises.forEach { exercise ->
                    add(generationRow(exercise))
                    moduleOneAnswers(exercise).forEach { answer -> add(evaluationRow(exercise, answer)) }
                }
                BOUNDARY_SEEDS.forEach { seed ->
                    listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).forEach { index ->
                        add(generationRow(generate(format, seed, index)))
                    }
                }
            }
        }
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-8 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val reviewBytes = publishedPairEightReviewBytes()
        val provenance = buildString {
            appendLine("baselineCommit=$BASELINE_COMMIT")
            appendLine("captureDate=2026-11-10")
            appendLine("contentRevision=$CONTENT_REVISION")
            appendLine("generatorRevision=$GENERATOR_REVISION")
            appendLine("evaluationRevision=$EVALUATION_REVISION")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows().size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine("reviewSha256=${reviewBytes.sha256()}")
            appendLine(
                "description=All inherited pair-eight format cycles and seed/index boundaries plus every authored " +
                    "E016-E019 task, private Module One source and exposure metadata, and canonical, known-wrong, " +
                    "case, normalization, blank, unknown, multiword and wrong-kind evaluations.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    fun generationHash(exercise: GeneratedExercise): String = CanonicalHasher("pilot-8-generation-v2").apply {
        field("pairSevenProjection", PilotSevenCompatibilityGolden.generationHash(exercise))
        nullable("targetScopeId", exercise.targetScopeId)
        val source = exercise.inspector.moduleOneSource
        nullable("moduleOneSource.presence", source?.let { "present" })
        source?.let {
            field("moduleOneSource.taskId", it.taskId)
            field("moduleOneSource.targetLexicalId", it.targetLexicalId)
            field("moduleOneSource.expectedForm", it.expectedForm)
            strings("moduleOneSource.knownAlternativeForms", it.knownAlternativeForms)
            nullable("moduleOneSource.referenceRole", it.referenceRole)
            nullable("moduleOneSource.frameVariantId", it.frameVariantId)
            field("moduleOneSource.casePolicy", it.casePolicy)
            integer("moduleOneSource.choiceRationales.count", it.choiceRationales.size)
            it.choiceRationales.forEachIndexed { index, rationale ->
                field("moduleOneSource.choiceRationales[$index].choice", rationale.choice)
                field("moduleOneSource.choiceRationales[$index].rationale", rationale.rationale)
            }
        }
        val task = (exercise.source as? ModuleOneExerciseSource)?.task
        nullable("moduleOneTask.presence", task?.let { "present" })
        task?.let {
            field("moduleOneTask.context", it.context)
            field("moduleOneTask.cue", it.cue)
            field("moduleOneTask.before", it.before)
            field("moduleOneTask.after", it.after)
            field("moduleOneTask.sentenceGloss", it.sentenceGloss)
            strings("moduleOneTask.answerExposureKeys", it.answerExposureKeys)
            strings("moduleOneTask.promptExposureKeys", it.promptExposureKeys)
            strings("moduleOneTask.lessonExposureKeys", it.lessonExposureKeys)
            strings("moduleOneTask.hintExposureKeys", it.hintExposureKeys)
            strings("moduleOneTask.feedbackExposureKeys", it.feedbackExposureKeys)
        }
        val key = exercise.source.answerKey as? AnswerKey.ModuleOneForm
        nullable("moduleOneAnswerKey.presence", key?.let { "present" })
        key?.let {
            field("moduleOneAnswerKey.expected", it.expected)
            strings("moduleOneAnswerKey.knownAlternatives", it.knownAlternatives.toList())
            field("moduleOneAnswerKey.selection", it.selection.toString())
        }
    }.finish()

    fun evaluationHash(evaluation: Evaluation): String = CanonicalHasher("pilot-8-evaluation-v2").apply {
        field("pairSevenProjection", PilotSevenCompatibilityGolden.evaluationHash(evaluation))
    }.finish()

    private fun authoredExercises(format: FormatSpec): List<GeneratedExercise> {
        val expectedIds = ModuleOneContent.tasks(format.id).map { it.id }.toSet()
        val exercises = mutableMapOf<String, GeneratedExercise>()
        var index = 0
        while (exercises.keys != expectedIds && index <= MODULE_ONE_SCAN_LIMIT) {
            val exercise = generate(format, FIXED_SEED, index++)
            exercises.putIfAbsent(requireNotNull(exercise.inspector.moduleOneSource).taskId, exercise)
        }
        require(exercises.keys == expectedIds) { "Could not enumerate all authored ${format.id} tasks" }
        return expectedIds.sorted().map(exercises::getValue)
    }

    private fun moduleOneAnswers(exercise: GeneratedExercise): List<Answer> {
        val source = requireNotNull(exercise.inspector.moduleOneSource)
        val expected = source.expectedForm
        val key = exercise.source.answerKey as AnswerKey.ModuleOneForm
        val caseVariant = when (expected) {
            "I" -> "i"
            else -> expected.uppercase(Locale.ROOT)
        }
        return buildList {
            add(Answer.Text(expected))
            add(Answer.Text(" $expected "))
            key.knownAlternatives.forEach { add(Answer.Text(it)) }
            add(Answer.Text(caseVariant))
            if (expected == "de") add(Answer.Text("De"))
            add(Answer.Text(""))
            add(Answer.Text("ukendt"))
            add(Answer.Text("$expected sover"))
            add(Answer.Text(if (expected == "er") "e\u0301r" else expected.replace("æ", "a\u0308")))
            add(Answer.Order(emptyList()))
            add(Answer.Matching(emptyList()))
            add(Answer.Gaps(emptyList()))
        }.distinct()
    }

    private fun correctAnswer(exercise: GeneratedExercise): Answer {
        if (exercise.formatId == "E009") return Answer.Text(requireNotNull(exercise.inspector.canonicalSentence))
        return when (val key = exercise.source.answerKey) {
        is AnswerKey.PresentForm -> Answer.Text(key.present)
        is AnswerKey.Article -> Answer.Text(key.expected)
        is AnswerKey.SubjectFirstOrder -> Answer.Order(
            key.canonicalRoles.map { role -> exercise.source.blockRoles.entries.single { it.value == role }.key },
        )
        is AnswerKey.MeaningChoice -> Answer.Text(key.expected)
        is AnswerKey.Headword -> Answer.Text(key.expected.headword)
        is AnswerKey.SuppliedChoice -> Answer.Text(key.expected)
        is AnswerKey.TileOrder -> Answer.Order(idsFor(key.canonical, exercise))
        is AnswerKey.Matching -> Answer.Matching(
            requireNotNull(exercise.inspector.matchingSource).pairs.map { MatchingPair(it.leftId, it.rightId) },
        )
        is AnswerKey.Gaps -> Answer.Gaps(
            requireNotNull(exercise.inspector.gapSource).slots.map { GapSelection(it.slotId, it.expectedChoiceId) },
        )
        is AnswerKey.ModuleOneForm -> Answer.Text(key.expected)
        }
    }

    private fun idsFor(identities: List<TileIdentity>, exercise: GeneratedExercise): List<String> {
        val remaining = requireNotNull(exercise.inspector.tileSource).tokens.toMutableList()
        return identities.map { identity ->
            val index = remaining.indexOfFirst { it.lexicalId == identity.lexicalId && it.form == identity.form }
            check(index >= 0)
            remaining.removeAt(index).blockId
        }
    }

    private fun generate(format: FormatSpec, seed: Int, index: Int): GeneratedExercise = engine.generate(
        GenerationRequest(format.id, format.targetSkillId, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
    )

    private fun generationRow(exercise: GeneratedExercise): String = listOf(
        "G", exercise.formatId, exercise.targetSkillId ?: NULL_COLUMN, exercise.reproduction.seed,
        exercise.reproduction.index, generationHash(exercise),
    ).joinToString("\t")

    private fun evaluationRow(exercise: GeneratedExercise, answer: Answer): String {
        val (kind, raw) = when (answer) {
            is Answer.Text -> "T" to answer.text
            is Answer.Order -> "O" to answer.blockIds.joinToString(ORDER_SEPARATOR)
            is Answer.Matching -> "M" to answer.pairs.joinToString(PAIR_SEPARATOR) { "${it.leftId}$ID_SEPARATOR${it.rightId}" }
            is Answer.Gaps -> "P" to answer.selections.joinToString(PAIR_SEPARATOR) { "${it.slotId}$ID_SEPARATOR${it.choiceId}" }
        }
        return listOf(
            kind, exercise.formatId, exercise.targetSkillId ?: NULL_COLUMN, exercise.reproduction.seed,
            exercise.reproduction.index, Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8)),
            evaluationHash(engine.evaluate(exercise, answer)),
        ).joinToString("\t")
    }

    private fun requireLiteralPairEight() {
        require(PilotContent.pilotEightSnapshot.contentRevision == CONTENT_REVISION)
        require(PilotContent.pilotEightSnapshot.generatorRevision == GENERATOR_REVISION)
        require(engine.evaluationRevision(CONTENT_REVISION, GENERATOR_REVISION) == EVALUATION_REVISION)
    }

    private data class FormatSpec(val id: String, val targetSkillId: String?, val cycleSize: Int)

    private class CanonicalHasher(schema: String) {
        private val digest = MessageDigest.getInstance("SHA-256")

        init { token(schema) }

        fun field(name: String, value: String) { token(name); token("value"); token(value) }
        fun nullable(name: String, value: String?) { token(name); token(if (value == null) "null" else "value"); value?.let(::token) }
        fun integer(name: String, value: Int) = field(name, value.toString())
        fun strings(name: String, values: List<String>) {
            integer("$name.count", values.size)
            values.forEachIndexed { index, value -> field("$name[$index]", value) }
        }
        fun finish(): String = digest.digest().toHex()
        private fun token(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(
                byteArrayOf(
                    (bytes.size ushr 24).toByte(),
                    (bytes.size ushr 16).toByte(),
                    (bytes.size ushr 8).toByte(),
                    bytes.size.toByte(),
                ),
            )
            digest.update(bytes)
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private const val BASELINE_COMMIT = "8913dea94a6c3bc0332f464e56952b9a126118a3"
    private const val CONTENT_REVISION = "pilot-8"
    private const val GENERATOR_REVISION = "generator-8"
    private const val EVALUATION_REVISION = "evaluation-8"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val MODULE_ONE_SCAN_LIMIT = 256
    private const val NULL_COLUMN = "-"
    private const val ORDER_SEPARATOR = "\u001f"
    private const val PAIR_SEPARATOR = "\u001e"
    private const val ID_SEPARATOR = "\u001d"
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
}
