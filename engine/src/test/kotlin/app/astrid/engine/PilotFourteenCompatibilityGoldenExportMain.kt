package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotFourteenCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotFourteenCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotFourteenCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal fun publishedPairFourteenReviewBytes(): ByteArray {
    // Capture from 0.16.0 with renderMarkdown(); switch to renderPilotFourteenMarkdown() as soon as
    // the freeze commit retains pair fourteen and before S12 advances the moving current snapshot.
    return PilotReview.renderMarkdown().toByteArray(Charsets.UTF_8)
}

internal object PilotFourteenCompatibilityGolden {
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
        FormatSpec("E025", "G005", PresentOrderContent.tasks("E025", includeModalConstruction = true).size),
        FormatSpec("E026", "G030", AbilityContent.tasks("E026").size),
        FormatSpec("E027", "G030", AbilityContent.tasks("E027").size),
        FormatSpec("E028", "G030", ModalRoundContent.interpretationTasks().size),
    )

    fun rows(): List<String> {
        requireLiteralPairFourteen()
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
            "Refusing to overwrite frozen pilot-14 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val reviewBytes = publishedPairFourteenReviewBytes()
        val provenance = buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.16.0")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("contentRevision=$CONTENT_REVISION")
            appendLine("generatorRevision=$GENERATOR_REVISION")
            appendLine("evaluationRevision=$EVALUATION_REVISION")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows().size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine("reviewSha256=${reviewBytes.sha256()}")
            appendLine(
                "description=All twenty-eight pair-fourteen format cycles and seed/index boundaries plus every authored " +
                    "E016-E028 task, all six E025 tasks, all three E028 tasks, complete modal task/source/exposure and " +
                    "L011/L034-L036/G005/G030 metadata, and canonical, known-wrong, normalization, missing, order, " +
                    "unknown, malformed and wrong-kind evaluations.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    fun generationHash(exercise: GeneratedExercise): String = CanonicalHasher("pilot-14-generation-v1").apply {
        val modalConstruction = (exercise.source as? PresentOrderExerciseSource)?.task is ModalConstructionTask
        if (modalConstruction) {
            field("projectionBranch", "pair-nine-common-modal-construction")
            field("pairNineCommonProjection", PilotNineCompatibilityGolden.generationHash(exercise))
        } else {
            field("projectionBranch", "pair-thirteen")
            field("pairThirteenProjection", PilotThirteenCompatibilityGolden.generationHash(exercise))
        }
        pairFourteenMetadata()
        modalProjection(exercise)
    }.finish()

    private fun CanonicalHasher.pairFourteenMetadata() {
        val skills = PilotContent.currentSnapshot.skills.filter { it.id in setOf("G005", "G030") }.sortedBy { it.id }
        integer("pairFourteenSkills.count", skills.size)
        skills.forEachIndexed { index, skill ->
            field("pairFourteenSkills[$index].id", skill.id)
            field("pairFourteenSkills[$index].title", skill.title)
            field("pairFourteenSkills[$index].explanation", skill.explanation)
            strings("pairFourteenSkills[$index].prerequisiteIds", skill.prerequisiteIds)
        }
        val formats = PilotContent.currentSnapshot.formats.filter { it.id in setOf("E025", "E028") }.sortedBy { it.id }
        integer("pairFourteenFormats.count", formats.size)
        formats.forEachIndexed { index, format ->
            field("pairFourteenFormats[$index].id", format.id)
            field("pairFourteenFormats[$index].title", format.title)
            nullable("pairFourteenFormats[$index].targetSkillId", format.targetSkillId)
            nullable("pairFourteenFormats[$index].lexicalTask.presence", format.lexicalTask?.let { "present" })
            format.lexicalTask?.let {
                field("pairFourteenFormats[$index].lexicalTask.facet", it.facet)
                field("pairFourteenFormats[$index].lexicalTask.direction", it.direction)
            }
            nullable("pairFourteenFormats[$index].readingTask.presence", format.readingTask?.let { "present" })
            format.readingTask?.let {
                field("pairFourteenFormats[$index].readingTask.facet", it.facet)
            }
        }
        strings("pairFourteenMeaningChoices", ModalRoundContent.meaningChoices)
        val records = PilotContent.currentSnapshot.lexicalReview.filter { it.id in PAIR_FOURTEEN_METADATA_IDS }
            .sortedBy { it.id }
        integer("pairFourteenLexicalMetadata.count", records.size)
        records.forEachIndexed { index, record -> lexicalRecord("pairFourteenLexicalMetadata[$index]", record) }
        integer("pairFourteenDefinitions.count", ModalRoundContent.definitions.size)
        ModalRoundContent.definitions.forEachIndexed { index, definition ->
            definition("pairFourteenDefinitions[$index]", definition)
        }
    }

    private fun CanonicalHasher.lexicalRecord(prefix: String, record: LexicalReviewRecord) {
        field("$prefix.id", record.id)
        field("$prefix.headword", record.headword)
        field("$prefix.selectedSense", record.selectedSense)
        val forms = record.activeForms.entries.sortedBy { it.key }
        integer("$prefix.activeForms.count", forms.size)
        forms.forEachIndexed { index, (name, value) ->
            field("$prefix.activeForms[$index].name", name)
            field("$prefix.activeForms[$index].value", value)
        }
        field("$prefix.constraints", record.constraints)
        strings("$prefix.sourceUrls", record.sourceUrls)
        field("$prefix.reviewState", record.reviewState)
    }

    private fun CanonicalHasher.definition(prefix: String, value: ModalRoundDefinition) {
        field("$prefix.key", value.key)
        field("$prefix.focusId", value.focusId)
        field("$prefix.title", value.title)
        field("$prefix.modal.lexicalId", value.modal.lexicalId)
        field("$prefix.modal.lemma", value.modal.lemma)
        field("$prefix.modal.present", value.modal.present)
        field("$prefix.modal.gloss", value.modal.gloss)
        field("$prefix.senseId", value.senseId)
        field("$prefix.selectedMeaning", value.selectedMeaning)
        field("$prefix.sourceUrl", value.sourceUrl)
        field("$prefix.frameVariantId", value.frameVariantId)
        field("$prefix.interpretationContextVariantId", value.interpretationContextVariantId)
        field("$prefix.constructionContextVariantId", value.constructionContextVariantId)
        val interpret = value.interpretTask
        field("$prefix.interpretTask.id", interpret.id)
        field("$prefix.interpretTask.subjectId", interpret.subjectId)
        field("$prefix.interpretTask.subject", interpret.subject)
        field("$prefix.interpretTask.complement", interpret.complement)
        strings("$prefix.interpretTask.complementLexicalIds", interpret.complementLexicalIds)
        field("$prefix.interpretTask.context", interpret.context)
        val construction = value.constructionTask
        field("$prefix.constructionTask.id", construction.id)
        field("$prefix.constructionTask.subjectId", construction.subjectId)
        field("$prefix.constructionTask.subject", construction.subject)
        field("$prefix.constructionTask.subjectReference", construction.subjectReference)
        field("$prefix.constructionTask.complement", construction.complement)
        strings("$prefix.constructionTask.complementLexicalIds", construction.complementLexicalIds)
        field("$prefix.constructionTask.intendedMeaning", construction.intendedMeaning)
        field("$prefix.workedDanish", value.workedDanish)
        field("$prefix.workedEnglish", value.workedEnglish)
        strings("$prefix.workedComponentLexicalIds", value.workedComponentLexicalIds)
    }

    private fun CanonicalHasher.modalProjection(exercise: GeneratedExercise) {
        val modalDetails = exercise.inspector.modalSource
        nullable("modalInspector.presence", modalDetails?.let { "present" })
        modalDetails?.let {
            field("modalInspector.taskId", it.taskId)
            field("modalInspector.modalEntryId", it.modalEntryId)
            field("modalInspector.modalSenseId", it.modalSenseId)
            field("modalInspector.expectedInterpretation", it.expectedInterpretation)
            field("modalInspector.frameVariantId", it.frameVariantId)
            field("modalInspector.contextVariantId", it.contextVariantId)
        }
        when (val source = exercise.source) {
            is ModalInterpretationExerciseSource -> interpretationSource(source)
            is PresentOrderExerciseSource -> {
                val task = source.task as? ModalConstructionTask
                if (task == null) field("modalSource.discriminator", "absent") else constructionSource(source, task)
            }
            else -> field("modalSource.discriminator", "absent")
        }
    }

    private fun CanonicalHasher.interpretationSource(source: ModalInterpretationExerciseSource) {
        field("modalSource.discriminator", "interpretation")
        val task = source.task
        field("modalTask.id", task.id)
        field("modalTask.formatId", "E028")
        field("modalTask.targetScopeId", task.targetScopeId)
        strings("modalTask.answerExposureKeys", task.answerExposureKeys)
        strings("modalTask.promptExposureKeys", task.promptExposureKeys)
        strings("modalTask.lessonExposureKeys", task.lessonExposureKeys)
        strings("modalTask.hintExposureKeys", task.hintExposureKeys)
        strings("modalTask.feedbackExposureKeys", task.feedbackExposureKeys)
        definition("modalTask.definition", task.definition)
        blockRoles("modalSource.blockRoles", source.blockRoles)
        val key = source.answerKey as AnswerKey.SuppliedChoice
        field("modalSource.answerKey.kind", "supplied-choice")
        field("modalSource.answerKey.expected", key.expected)
        strings("modalSource.answerKey.suppliedChoices", key.suppliedChoices.sorted())
    }

    private fun CanonicalHasher.constructionSource(source: PresentOrderExerciseSource, task: ModalConstructionTask) {
        field("modalSource.discriminator", "construction")
        field("modalTask.id", task.id)
        field("modalTask.formatId", task.formatId)
        field("modalTask.targetScopeId", task.targetScopeId)
        strings("modalTask.answerExposureKeys", task.answerExposureKeys)
        strings("modalTask.promptExposureKeys", task.promptExposureKeys)
        strings("modalTask.lessonExposureKeys", task.lessonExposureKeys)
        strings("modalTask.hintExposureKeys", task.hintExposureKeys)
        strings("modalTask.feedbackExposureKeys", task.feedbackExposureKeys)
        definition("modalTask.definition", task.definition)
        integer("modalTask.parts.count", task.parts.size)
        task.parts.forEachIndexed { index, part ->
            field("modalTask.parts[$index].lexicalId", part.lexicalId)
            field("modalTask.parts[$index].text", part.text)
            field("modalTask.parts[$index].form", part.form)
            strings("modalTask.parts[$index].lexicalIds", part.lexicalIds)
        }
        blockRoles("modalSource.blockRoles", source.blockRoles)
        val key = source.answerKey as AnswerKey.TileOrder
        field("modalSource.answerKey.kind", "tile-order")
        identities("modalSource.answerKey.canonical", key.canonical)
        nullable("modalSource.answerKey.fronted.presence", key.fronted?.let { "present" })
        key.fronted?.let { identities("modalSource.answerKey.fronted", it) }
        nullable("modalSource.answerKey.infinitive.presence", key.infinitive?.let { "present" })
        key.infinitive?.let { identities("modalSource.answerKey.infinitive", it) }
    }

    private fun CanonicalHasher.blockRoles(prefix: String, roles: Map<String, String>) {
        val entries = roles.entries.sortedBy { it.key }
        integer("$prefix.count", entries.size)
        entries.forEachIndexed { index, (blockId, role) ->
            field("$prefix[$index].blockId", blockId)
            field("$prefix[$index].role", role)
        }
    }

    private fun CanonicalHasher.identities(prefix: String, values: List<TileIdentity>) {
        integer("$prefix.count", values.size)
        values.forEachIndexed { index, identity ->
            field("$prefix[$index].lexicalId", identity.lexicalId)
            field("$prefix[$index].form", identity.form)
        }
    }

    fun evaluationHash(evaluation: Evaluation): String = CanonicalHasher("pilot-14-evaluation-v1").apply {
        field("pairThirteenProjection", PilotThirteenCompatibilityGolden.evaluationHash(evaluation))
    }.finish()

    private fun authoredExercises(format: FormatSpec): List<GeneratedExercise> {
        val expectedIds = when (format.id) {
            "E020", "E021" -> InanimateReferenceContent.tasks(format.id).map { it.id }.toSet()
            "E022", "E023" -> PresentOrderContent.tasks(
                format.id,
                includePossession = true,
                includePersonal = true,
            ).map { it.id }.toSet()
            "E024" -> PresentOrderContent.tasks(format.id).map { it.id }.toSet()
            "E025" -> PresentOrderContent.tasks(format.id, includeModalConstruction = true).map { it.id }.toSet()
            "E026", "E027" -> AbilityContent.tasks(format.id).map { it.id }.toSet()
            "E028" -> ModalRoundContent.interpretationTasks().map { it.id }.toSet()
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

    private fun authoredAnswers(exercise: GeneratedExercise): List<Answer> = when (val source = exercise.source) {
        is ModalInterpretationExerciseSource -> interpretationAnswers(source)
        is AbilityExerciseSource -> abilityAnswers()
        is PresentOrderExerciseSource -> when (source.task) {
            is ModalConstructionTask -> modalConstructionAnswers(exercise)
            is SubjectFirstOrderTask -> subjectFirstAnswers(exercise)
            else -> moduleOneAnswers(exercise)
        }
        else -> moduleOneAnswers(exercise)
    }

    private fun interpretationAnswers(source: ModalInterpretationExerciseSource): List<Answer> {
        val key = source.answerKey as AnswerKey.SuppliedChoice
        return buildList {
            add(Answer.Text(key.expected))
            add(Answer.Text("  ${key.expected}  "))
            key.suppliedChoices.sorted().filterNot { it == key.expected }.forEach { add(Answer.Text(it)) }
            add(Answer.Text("unknown meaning"))
            add(Answer.Text(""))
            add(Answer.Order(emptyList()))
            add(Answer.Matching(emptyList()))
            add(Answer.Gaps(emptyList()))
        }.distinct()
    }

    private fun modalConstructionAnswers(exercise: GeneratedExercise): List<Answer> {
        val canonical = (correctAnswer(exercise) as Answer.Order).blockIds
        val subject = canonical.first()
        val finite = canonical[1]
        return listOf(
            Answer.Order(canonical), Answer.Order(canonical.dropLast(1)),
            Answer.Order(canonical.drop(1) + canonical.first()),
            Answer.Order(listOf(finite, subject) + canonical.filterNot { it == subject || it == finite }),
            Answer.Order(emptyList()), Answer.Order(canonical + canonical.first()), Answer.Order(listOf("unknown")),
            Answer.Text("ukendt"), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
        ).distinct()
    }

    private fun abilityAnswers(): List<Answer> = listOf(
        Answer.Text("læse"), Answer.Text(" læse "), Answer.Text("at læse"), Answer.Text("  at   læse  "),
        Answer.Text("læser"), Answer.Text("LÆSE"), Answer.Text(""), Answer.Text("ukendt"), Answer.Text("læse nu"),
        Answer.Order(emptyList()), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
    )

    private fun subjectFirstAnswers(exercise: GeneratedExercise): List<Answer> {
        val canonical = (correctAnswer(exercise) as Answer.Order).blockIds
        return listOf(
            Answer.Order(canonical), Answer.Order(canonical.reversed()), Answer.Order(canonical.dropLast(1)),
            Answer.Order(emptyList()), Answer.Order(canonical + canonical.first()), Answer.Order(listOf("unknown")),
            Answer.Text("ukendt"), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
        ).distinct()
    }

    private fun moduleOneAnswers(exercise: GeneratedExercise): List<Answer> {
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

    private fun requireLiteralPairFourteen() {
        // Capture against currentSnapshot at released 0.16.0, then replace this moving hook with
        // pilotFourteenSnapshot in the freeze commit before S12 advances current.
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

    private const val RELEASE_SOURCE_COMMIT = "9dd787c14b3a5d4a414da763b4ef28ce511e63be"
    private const val CAPTURE_DATE = "2026-11-11"
    private const val CONTENT_REVISION = "pilot-14"
    private const val GENERATOR_REVISION = "generator-14"
    private const val EVALUATION_REVISION = "evaluation-14"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val MODULE_ONE_SCAN_LIMIT = 256
    private const val NULL_COLUMN = "-"
    private const val ORDER_SEPARATOR = "\u001f"
    private const val PAIR_SEPARATOR = "\u001e"
    private const val ID_SEPARATOR = "\u001d"
    private val PAIR_FOURTEEN_METADATA_IDS = setOf("L011", "L034", "L035", "L036")
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
}
