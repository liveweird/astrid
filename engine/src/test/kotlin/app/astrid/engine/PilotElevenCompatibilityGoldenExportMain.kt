package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotElevenCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotElevenCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotElevenCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal fun publishedPairElevenReviewBytes(): ByteArray {
    return PilotReview.renderPilotElevenMarkdown().toByteArray(Charsets.UTF_8)
}

internal object PilotElevenCompatibilityGolden {
    private val engine = PilotEngine()
    private val inheritedFormats = listOf(
        FormatSpec("E001", "G004", 99), FormatSpec("E002", "G006", 92), FormatSpec("E003", "G005", 99),
        FormatSpec("E004", "G004", 99), FormatSpec("E005", "G004", 99), FormatSpec("E006", "G006", 92),
        FormatSpec("E007", null, 3), FormatSpec("E008", null, 3), FormatSpec("E009", "G005", 99),
        FormatSpec("E010", null, 24), FormatSpec("E011", null, 4), FormatSpec("E012", "G005", 99),
        FormatSpec("E013", "G004", 99), FormatSpec("E014", null, 32), FormatSpec("E015", "G004", 92),
    )
    private val authoredFormats = listOf(
        FormatSpec("E016", "G003", ModuleOneContent.tasks("E016").size),
        FormatSpec("E017", "G003", ModuleOneContent.tasks("E017").size),
        FormatSpec("E018", "G004", ModuleOneContent.tasks("E018").size),
        FormatSpec("E019", "G004", ModuleOneContent.tasks("E019").size),
        FormatSpec("E020", "G038", InanimateReferenceContent.tasks("E020").size),
        FormatSpec("E021", "G038", InanimateReferenceContent.tasks("E021").size),
        FormatSpec("E022", "G004", PresentOrderContent.tasks("E022", includePossession = true).size),
        FormatSpec("E023", "G004", PresentOrderContent.tasks("E023", includePossession = true).size),
        FormatSpec("E024", "G005", PresentOrderContent.tasks("E024").size),
        FormatSpec("E025", "G005", PresentOrderContent.tasks("E025").size),
    )

    fun rows(): List<String> {
        requireLiteralPairEleven()
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
            authoredFormats.forEach { format ->
                val exercises = authoredExercises(format)
                require(exercises.size == format.cycleSize)
                exercises.forEach { exercise ->
                    add(generationRow(exercise))
                    authoredAnswers(exercise).forEach { answer -> add(evaluationRow(exercise, answer)) }
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
            "Refusing to overwrite frozen pilot-11 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val reviewBytes = publishedPairElevenReviewBytes()
        val provenance = buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.13.0")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("contentRevision=$CONTENT_REVISION")
            appendLine("generatorRevision=$GENERATOR_REVISION")
            appendLine("evaluationRevision=$EVALUATION_REVISION")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows().size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine("reviewSha256=${reviewBytes.sha256()}")
            appendLine(
                "description=All twenty-five pair-eleven format cycles and seed/index boundaries plus every authored " +
                    "E016-E025 task, private Module One, inanimate-reference, present/order and possession source/exposure metadata, " +
                    "and canonical, known-wrong, case, normalization, blank, unknown, partial, malformed and wrong-kind evaluations.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    fun generationHash(exercise: GeneratedExercise): String = CanonicalHasher("pilot-11-generation-v1").apply {
        field("pairTenProjection", PilotTenCompatibilityGolden.generationHash(exercise))
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
        val referenceSource = exercise.inspector.inanimateReferenceSource
        nullable("inanimateReferenceSource.presence", referenceSource?.let { "present" })
        referenceSource?.let {
            field("inanimateReferenceSource.taskId", it.taskId)
            field("inanimateReferenceSource.targetEntryId", it.targetEntryId)
            field("inanimateReferenceSource.targetSenseId", it.targetSenseId)
            field("inanimateReferenceSource.expectedForm", it.expectedForm)
            strings("inanimateReferenceSource.knownAlternativeForms", it.knownAlternativeForms)
            field("inanimateReferenceSource.antecedentEntryId", it.antecedentEntryId)
            field("inanimateReferenceSource.antecedentSenseId", it.antecedentSenseId)
            field("inanimateReferenceSource.antecedentArticle", it.antecedentArticle)
            field("inanimateReferenceSource.antecedentGender", it.antecedentGender)
            field("inanimateReferenceSource.antecedentNumber", it.antecedentNumber)
            field("inanimateReferenceSource.requestedRole", it.requestedRole)
            field("inanimateReferenceSource.casePolicy", it.casePolicy)
            field("inanimateReferenceSource.contextVariantId", it.contextVariantId)
            integer("inanimateReferenceSource.choiceRationales.count", it.choiceRationales.size)
            it.choiceRationales.forEachIndexed { index, rationale ->
                field("inanimateReferenceSource.choiceRationales[$index].choice", rationale.choice)
                field("inanimateReferenceSource.choiceRationales[$index].rationale", rationale.rationale)
            }
        }
        val referenceTask = (exercise.source as? InanimateReferenceExerciseSource)?.task
        nullable("inanimateReferenceTask.presence", referenceTask?.let { "present" })
        referenceTask?.let {
            field("inanimateReferenceTask.context", it.context)
            strings("inanimateReferenceTask.answerExposureKeys", it.answerExposureKeys)
            strings("inanimateReferenceTask.promptExposureKeys", it.promptExposureKeys)
            strings("inanimateReferenceTask.lessonExposureKeys", it.lessonExposureKeys)
            strings("inanimateReferenceTask.hintExposureKeys", it.hintExposureKeys)
            strings("inanimateReferenceTask.feedbackExposureKeys", it.feedbackExposureKeys)
        }
        presentOrderTask(exercise)
    }.finish()

    private fun CanonicalHasher.presentOrderTask(exercise: GeneratedExercise) {
        val task = (exercise.source as? PresentOrderExerciseSource)?.task
        nullable("presentOrderTask.presence", task?.let { "present" })
        task ?: return
        field("presentOrderTask.id", task.id)
        field("presentOrderTask.formatId", task.formatId)
        field("presentOrderTask.targetScopeId", task.targetScopeId)
        strings("presentOrderTask.answerExposureKeys", task.answerExposureKeys)
        strings("presentOrderTask.promptExposureKeys", task.promptExposureKeys)
        strings("presentOrderTask.lessonExposureKeys", task.lessonExposureKeys)
        strings("presentOrderTask.hintExposureKeys", task.hintExposureKeys)
        strings("presentOrderTask.feedbackExposureKeys", task.feedbackExposureKeys)
        when (task) {
            is NamedPresentTask -> namedPresentTask(task)
            is SubjectFirstOrderTask -> subjectFirstOrderTask(task)
            is ModalConstructionTask -> error("Pair eleven cannot contain modal construction tasks")
        }
    }

    private fun CanonicalHasher.namedPresentTask(task: NamedPresentTask) {
        field("presentOrderTask.kind", "named-present")
        field("presentOrderTask.subjectId", task.subjectId)
        field("presentOrderTask.subject", task.subject)
        field("presentOrderTask.reference", task.reference)
        field("presentOrderTask.verb.lexicalId", task.verb.lexicalId)
        field("presentOrderTask.verb.lemma", task.verb.lemma)
        field("presentOrderTask.verb.present", task.verb.present)
        field("presentOrderTask.verb.gloss", task.verb.gloss)
        nullable("presentOrderTask.complement", task.complement)
        strings("presentOrderTask.complementLexicalIds", task.complementLexicalIds)
        field("presentOrderTask.scopeId", task.scopeId)
        field("presentOrderTask.patternKey", task.patternKey)
        nullable("presentOrderTask.englishProposition", task.englishProposition)
        nullable("presentOrderTask.complementMeaning", task.complementMeaning)
    }

    private fun CanonicalHasher.subjectFirstOrderTask(task: SubjectFirstOrderTask) {
        field("presentOrderTask.kind", "subject-first-order")
        field("presentOrderTask.english", task.english)
        field("presentOrderTask.reference", task.reference)
        integer("presentOrderTask.parts.count", task.parts.size)
        task.parts.forEachIndexed { index, part ->
            field("presentOrderTask.parts[$index].lexicalId", part.lexicalId)
            field("presentOrderTask.parts[$index].text", part.text)
            field("presentOrderTask.parts[$index].form", part.form)
            strings("presentOrderTask.parts[$index].lexicalIds", part.lexicalIds)
        }
    }

    fun evaluationHash(evaluation: Evaluation): String = CanonicalHasher("pilot-11-evaluation-v1").apply {
        field("pairTenProjection", PilotTenCompatibilityGolden.evaluationHash(evaluation))
    }.finish()

    private fun authoredExercises(format: FormatSpec): List<GeneratedExercise> {
        val expectedIds = when (format.id) {
            "E020", "E021" -> InanimateReferenceContent.tasks(format.id).map { it.id }.toSet()
            "E022", "E023" -> PresentOrderContent.tasks(format.id, includePossession = true).map { it.id }.toSet()
            "E024", "E025" -> PresentOrderContent.tasks(format.id).map { it.id }.toSet()
            else -> ModuleOneContent.tasks(format.id).map { it.id }.toSet()
        }
        val exercises = mutableMapOf<String, GeneratedExercise>()
        var index = 0
        while (exercises.keys != expectedIds && index <= MODULE_ONE_SCAN_LIMIT) {
            val exercise = generate(format, FIXED_SEED, index++)
            val taskId = exercise.inspector.moduleOneSource?.taskId
                ?: exercise.inspector.inanimateReferenceSource?.taskId
                ?: requireNotNull(exercise.inspector.canonicalSentenceId)
            exercises.putIfAbsent(taskId, exercise)
        }
        require(exercises.keys == expectedIds) { "Could not enumerate all authored ${format.id} tasks" }
        return expectedIds.sorted().map(exercises::getValue)
    }

    private fun authoredAnswers(exercise: GeneratedExercise): List<Answer> {
        if (exercise.source is PresentOrderExerciseSource && exercise.source.task is SubjectFirstOrderTask) {
            val canonical = (correctAnswer(exercise) as Answer.Order).blockIds
            return listOf(
                Answer.Order(canonical), Answer.Order(canonical.reversed()), Answer.Order(canonical.dropLast(1)),
                Answer.Order(emptyList()), Answer.Order(canonical + canonical.first()), Answer.Order(listOf("unknown")),
                Answer.Text("ukendt"), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
            ).distinct()
        }
        val moduleOneSource = exercise.inspector.moduleOneSource
        val referenceSource = exercise.inspector.inanimateReferenceSource
        val expected = moduleOneSource?.expectedForm ?: requireNotNull(referenceSource).expectedForm
        val alternatives = moduleOneSource?.knownAlternativeForms ?: requireNotNull(referenceSource).knownAlternativeForms
        val key = exercise.source.answerKey as AnswerKey.ModuleOneForm
        val caseVariant = when (expected) {
            "I" -> "i"
            else -> expected.uppercase(Locale.ROOT)
        }
        return buildList {
            add(Answer.Text(expected))
            add(Answer.Text(" $expected "))
            alternatives.forEach { add(Answer.Text(it)) }
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

    private fun requireLiteralPairEleven() {
        require(PilotContent.pilotElevenSnapshot.contentRevision == CONTENT_REVISION)
        require(PilotContent.pilotElevenSnapshot.generatorRevision == GENERATOR_REVISION)
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

    private const val RELEASE_SOURCE_COMMIT = "b82ad3615b74de932ae903e7d479d454debc0740"
    private const val CAPTURE_DATE = "2026-11-11"
    private const val CONTENT_REVISION = "pilot-11"
    private const val GENERATOR_REVISION = "generator-11"
    private const val EVALUATION_REVISION = "evaluation-11"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val MODULE_ONE_SCAN_LIMIT = 256
    private const val NULL_COLUMN = "-"
    private const val ORDER_SEPARATOR = "\u001f"
    private const val PAIR_SEPARATOR = "\u001e"
    private const val ID_SEPARATOR = "\u001d"
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
}
