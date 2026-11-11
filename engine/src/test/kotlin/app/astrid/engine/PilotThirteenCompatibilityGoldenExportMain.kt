package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotThirteenCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotThirteenCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotThirteenCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal fun publishedPairThirteenReviewBytes(): ByteArray {
    // Capture from 0.15.0 with renderMarkdown(); switch to renderPilotThirteenMarkdown() as soon as
    // the freeze commit retains pair thirteen and before S11 advances the moving current snapshot.
    return PilotReview.renderMarkdown().toByteArray(Charsets.UTF_8)
}

internal object PilotThirteenCompatibilityGolden {
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
        FormatSpec("E022", "G004", PresentOrderContent.tasks("E022", includePossession = true, includePersonal = true).size),
        FormatSpec("E023", "G004", PresentOrderContent.tasks("E023", includePossession = true, includePersonal = true).size),
        FormatSpec("E024", "G005", PresentOrderContent.tasks("E024").size),
        FormatSpec("E025", "G005", PresentOrderContent.tasks("E025").size),
        FormatSpec("E026", "G030", AbilityContent.tasks("E026").size),
        FormatSpec("E027", "G030", AbilityContent.tasks("E027").size),
    )

    fun rows(): List<String> {
        requireLiteralPairThirteen()
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
            "Refusing to overwrite frozen pilot-13 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val reviewBytes = publishedPairThirteenReviewBytes()
        val provenance = buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.15.0")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("contentRevision=$CONTENT_REVISION")
            appendLine("generatorRevision=$GENERATOR_REVISION")
            appendLine("evaluationRevision=$EVALUATION_REVISION")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows().size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine("reviewSha256=${reviewBytes.sha256()}")
            appendLine(
                "description=All twenty-seven pair-thirteen format cycles and seed/index boundaries plus every authored " +
                    "E016-E027 task, pair-thirteen ability task/source/exposure and L011/L031/L033/G030 metadata, and " +
                    "canonical, marker, finite, case, normalization, blank, unknown, partial, malformed and wrong-kind evaluations.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    fun generationHash(exercise: GeneratedExercise): String = CanonicalHasher("pilot-13-generation-v1").apply {
        field("pairTwelveProjection", PilotTwelveCompatibilityGolden.generationHash(exercise))
        pairThirteenMetadata()
        abilitySource(exercise)
    }.finish()

    private fun CanonicalHasher.pairThirteenMetadata() {
        val skill = PilotContent.currentSnapshot.skills.single { it.id == "G030" }
        field("pairThirteenSkill.id", skill.id)
        field("pairThirteenSkill.title", skill.title)
        field("pairThirteenSkill.explanation", skill.explanation)
        strings("pairThirteenSkill.prerequisiteIds", skill.prerequisiteIds)
        val formats = PilotContent.currentSnapshot.formats.filter { it.id in setOf("E026", "E027") }.sortedBy { it.id }
        integer("pairThirteenFormats.count", formats.size)
        formats.forEachIndexed { index, format ->
            field("pairThirteenFormats[$index].id", format.id)
            field("pairThirteenFormats[$index].title", format.title)
            nullable("pairThirteenFormats[$index].targetSkillId", format.targetSkillId)
        }
        val records = PilotContent.currentSnapshot.lexicalReview.filter { it.id in PAIR_THIRTEEN_METADATA_IDS }
            .sortedBy { it.id }
        integer("pairThirteenLexicalMetadata.count", records.size)
        records.forEachIndexed { index, record ->
            val prefix = "pairThirteenLexicalMetadata[$index]"
            field("$prefix.id", record.id)
            field("$prefix.headword", record.headword)
            field("$prefix.selectedSense", record.selectedSense)
            val activeForms = record.activeForms.entries.sortedBy { it.key }
            integer("$prefix.activeForms.count", activeForms.size)
            activeForms.forEachIndexed { formIndex, (name, value) ->
                field("$prefix.activeForms[$formIndex].name", name)
                field("$prefix.activeForms[$formIndex].value", value)
            }
            field("$prefix.constraints", record.constraints)
            strings("$prefix.sourceUrls", record.sourceUrls)
            field("$prefix.reviewState", record.reviewState)
        }
    }

    private fun CanonicalHasher.abilitySource(exercise: GeneratedExercise) {
        val source = exercise.source as? AbilityExerciseSource
        nullable("abilitySource.presence", source?.let { "present" })
        source ?: return
        val task = source.task
        field("abilityTask.id", task.id)
        field("abilityTask.formatId", task.formatId)
        field("abilityTask.frameId", task.frameId)
        field("abilityTask.subject.lexicalId", task.subject.lexicalId)
        field("abilityTask.subject.text", task.subject.text)
        field("abilityTask.subject.reference", task.subject.reference)
        field("abilityTask.englishProposition", task.englishProposition)
        field("abilityTask.complement.text", task.complement.text)
        field("abilityTask.complement.meaning", task.complement.meaning)
        strings("abilityTask.complement.lexicalIds", task.complement.lexicalIds)
        field("abilityTask.complement.role", task.complement.role)
        field("abilityTask.targetScopeId", task.targetScopeId)
        strings("abilityTask.answerExposureKeys", task.answerExposureKeys)
        strings("abilityTask.promptExposureKeys", task.promptExposureKeys)
        strings("abilityTask.lessonExposureKeys", task.lessonExposureKeys)
        strings("abilityTask.hintExposureKeys", task.hintExposureKeys)
        strings("abilityTask.feedbackExposureKeys", task.feedbackExposureKeys)
        nullable("abilityInspector.canonicalSentenceId", exercise.inspector.canonicalSentenceId)
        nullable("abilityInspector.constructionId", exercise.inspector.constructionId)
        strings("abilityInspector.lexicalIds", exercise.inspector.lexicalIds)
        strings("abilityInspector.clauseRoles", exercise.inspector.clauseRoles)
        nullable("abilityInspector.canonicalSentence", exercise.inspector.canonicalSentence)
        val details = requireNotNull(exercise.inspector.moduleOneSource)
        field("abilityInspector.moduleOneSource.taskId", details.taskId)
        field("abilityInspector.moduleOneSource.targetLexicalId", details.targetLexicalId)
        field("abilityInspector.moduleOneSource.expectedForm", details.expectedForm)
        strings("abilityInspector.moduleOneSource.knownAlternativeForms", details.knownAlternativeForms)
        nullable("abilityInspector.moduleOneSource.referenceRole", details.referenceRole)
        nullable("abilityInspector.moduleOneSource.frameVariantId", details.frameVariantId)
        field("abilityInspector.moduleOneSource.casePolicy", details.casePolicy)
        integer("abilityInspector.moduleOneSource.choiceRationales.count", details.choiceRationales.size)
        details.choiceRationales.forEachIndexed { index, rationale ->
            field("abilityInspector.moduleOneSource.choiceRationales[$index].choice", rationale.choice)
            field("abilityInspector.moduleOneSource.choiceRationales[$index].rationale", rationale.rationale)
        }
        integer("abilitySource.blockRoles.count", source.blockRoles.size)
        source.blockRoles.entries.sortedBy { it.key }.forEachIndexed { index, (blockId, role) ->
            field("abilitySource.blockRoles[$index].blockId", blockId)
            field("abilitySource.blockRoles[$index].role", role)
        }
        val key = source.answerKey as AnswerKey.ModuleOneForm
        field("abilitySource.answerKey.expected", key.expected)
        strings("abilitySource.answerKey.knownAlternatives", key.knownAlternatives.sorted())
        field("abilitySource.answerKey.selection", key.selection.toString())
    }

    fun evaluationHash(evaluation: Evaluation): String = CanonicalHasher("pilot-13-evaluation-v1").apply {
        field("pairTwelveProjection", PilotTwelveCompatibilityGolden.evaluationHash(evaluation))
    }.finish()

    private fun authoredExercises(format: FormatSpec): List<GeneratedExercise> {
        val expectedIds = when (format.id) {
            "E020", "E021" -> InanimateReferenceContent.tasks(format.id).map { it.id }.toSet()
            "E022", "E023" -> PresentOrderContent.tasks(
                format.id,
                includePossession = true,
                includePersonal = true,
            ).map { it.id }.toSet()
            "E024", "E025" -> PresentOrderContent.tasks(format.id).map { it.id }.toSet()
            "E026", "E027" -> AbilityContent.tasks(format.id).map { it.id }.toSet()
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

    private fun authoredAnswers(exercise: GeneratedExercise): List<Answer> = when {
        exercise.source is AbilityExerciseSource -> listOf(
            Answer.Text("læse"), Answer.Text(" læse "), Answer.Text("at læse"), Answer.Text("  at   læse  "),
            Answer.Text("læser"),
            Answer.Text("LÆSE"), Answer.Text(""), Answer.Text("ukendt"), Answer.Text("læse nu"),
            Answer.Order(emptyList()), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
        )
        exercise.source is PresentOrderExerciseSource && exercise.source.task is SubjectFirstOrderTask -> {
            val canonical = (correctAnswer(exercise) as Answer.Order).blockIds
            listOf(
                Answer.Order(canonical), Answer.Order(canonical.reversed()), Answer.Order(canonical.dropLast(1)),
                Answer.Order(emptyList()), Answer.Order(canonical + canonical.first()), Answer.Order(listOf("unknown")),
                Answer.Text("ukendt"), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
            ).distinct()
        }
        else -> {
            val moduleOneSource = exercise.inspector.moduleOneSource
            val referenceSource = exercise.inspector.inanimateReferenceSource
            val expected = moduleOneSource?.expectedForm ?: requireNotNull(referenceSource).expectedForm
            val alternatives = moduleOneSource?.knownAlternativeForms ?: requireNotNull(referenceSource).knownAlternativeForms
            val caseVariant = when (expected) {
                "I" -> "i"
                else -> expected.uppercase(Locale.ROOT)
            }
            buildList {
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

    private fun requireLiteralPairThirteen() {
        // Capture against currentSnapshot at released 0.15.0, then replace this moving hook with
        // pilotThirteenSnapshot in the freeze commit before S11 advances current.
        require(PilotContent.currentSnapshot.contentRevision == CONTENT_REVISION)
        require(PilotContent.currentSnapshot.generatorRevision == GENERATOR_REVISION)
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

    private const val RELEASE_SOURCE_COMMIT = "43e9726b079f4483103badbd87d2209da943adb8"
    private const val CAPTURE_DATE = "2026-11-11"
    private const val CONTENT_REVISION = "pilot-13"
    private const val GENERATOR_REVISION = "generator-13"
    private const val EVALUATION_REVISION = "evaluation-13"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val MODULE_ONE_SCAN_LIMIT = 256
    private const val NULL_COLUMN = "-"
    private const val ORDER_SEPARATOR = "\u001f"
    private const val PAIR_SEPARATOR = "\u001e"
    private const val ID_SEPARATOR = "\u001d"
    private val PAIR_THIRTEEN_METADATA_IDS = setOf("L011", "L031", "L033")
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
}
