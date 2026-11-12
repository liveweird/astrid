package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotEighteenCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) {
            "Usage: PilotEighteenCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>"
        }
        PilotEighteenCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

/**
 * Pair eighteen has no retained production hook while it is CURRENT. This adapter is deliberately
 * fail-closed: the future pair-nineteen change must bind it to a retained snapshot/review renderer.
 */
internal fun publishedPairEighteenReviewBytes(): ByteArray {
    val engine = PilotEngine()
    require(PilotContent.currentSnapshot.contentRevision == CONTENT_REVISION)
    require(PilotContent.currentSnapshot.generatorRevision == GENERATOR_REVISION)
    require(engine.evaluationRevision(CONTENT_REVISION, GENERATOR_REVISION) == EVALUATION_REVISION)
    return PilotReview.renderMarkdown().toByteArray(Charsets.UTF_8)
}

internal object PilotEighteenCompatibilityGolden {
    private val engine = PilotEngine()
    private val inheritedFormats = (1..30).map { number ->
        val id = "E${number.toString().padStart(3, '0')}"
        FormatSpec(id, PilotContent.pilotSixteenSnapshot.formats.single { it.id == id }.targetSkillId, cycleSize(id))
    }
    private val p1Formats = (31..39).map { number ->
        val id = "E${number.toString().padStart(3, '0')}"
        FormatSpec(id, VocabularyP1Content.formats.single { it.id == id }.targetSkillId, VocabularyP1Content.tasks(id).size)
    }

    fun rows(): List<String> {
        requireLiteralPairEighteen()
        require(P1_TASK_COUNTS == VocabularyP1Content.tasks.groupingBy(VocabularyP1Task::formatId).eachCount())
        return buildList {
            inheritedFormats.forEach { format ->
                repeat(format.cycleSize) { index ->
                    val exercise = generate(format, FIXED_SEED, index)
                    add(generationRow(exercise))
                    add(evaluationRow(exercise, inheritedCanonicalAnswer(exercise)))
                }
                boundaryRows(format).forEach(::add)
            }
            p1Formats.forEach { format ->
                authoredExercises(format).forEach { exercise ->
                    add(generationRow(exercise))
                    answers(exercise).forEach { answer -> add(evaluationRow(exercise, answer)) }
                }
                boundaryRows(format).forEach(::add)
            }
        }
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-18 compatibility evidence"
        }
        val rows = rows()
        val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val reviewBytes = publishedPairEighteenReviewBytes()
        val provenance = buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.20.1")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("projectionSchema=pair18-engine-v1")
            appendLine("contentRevision=$CONTENT_REVISION")
            appendLine("generatorRevision=$GENERATOR_REVISION")
            appendLine("evaluationRevision=$EVALUATION_REVISION")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows.size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine("reviewSha256=${reviewBytes.sha256()}")
            appendLine(
                "description=All thirty-nine pair-eighteen format cycles and seed/index boundaries; all 23 literal " +
                    "P1 tasks and complete public, inspector, private source and answer-key projections; every choice, " +
                    "typed normalization boundary, tile order and malformed shape, and all 34/209 partial matching bijections.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    fun generationHash(exercise: GeneratedExercise): String = FreezeHasher("pilot-18-generation-v1").apply {
        field("pairSixteenProjection", PilotSixteenCompatibilityGolden.generationHash(exercise))
        field("id", exercise.id)
        field("formatId", exercise.formatId)
        nullable("targetSkillId", exercise.targetSkillId)
        nullable("lexicalTarget.presence", exercise.lexicalTarget?.let { "present" })
        exercise.lexicalTarget?.let { lexicalTarget("lexicalTarget", it) }
        strings("supportSkillIds", exercise.supportSkillIds)
        field("contentRevision", exercise.contentRevision)
        field("generatorRevision", exercise.generatorRevision)
        integer("reproduction.seed", exercise.reproduction.seed)
        integer("reproduction.index", exercise.reproduction.index)
        presentation("presentation", exercise.presentation)
        nullable("readingTarget.facet", exercise.readingTarget?.facet)
        lexicalTargets("lexicalTargets", exercise.lexicalTargets)
        nullable("targetScopeId", exercise.targetScopeId)
        nullable("inspector.canonicalSentenceId", exercise.inspector.canonicalSentenceId)
        nullable("inspector.constructionId", exercise.inspector.constructionId)
        strings("inspector.lexicalIds", exercise.inspector.lexicalIds)
        strings("inspector.clauseRoles", exercise.inspector.clauseRoles)
        nullable("inspector.canonicalSentence", exercise.inspector.canonicalSentence)
        vocabularyMetadata()
        vocabularyProjection(exercise)
    }.finish()

    fun evaluationHash(evaluation: Evaluation): String = FreezeHasher("pilot-18-evaluation-v1").apply {
        field("pairSixteenProjection", PilotSixteenCompatibilityGolden.evaluationHash(evaluation))
        field("targetStatus", evaluation.targetStatus.wireValue)
        field("linguisticStatus", evaluation.linguisticStatus.wireValue)
        nullable("linguisticScope", evaluation.linguisticScope?.wireValue)
        nullable("responseConstraintStatus", evaluation.responseConstraintStatus?.wireValue)
        nullable("meaningSatisfaction", evaluation.meaningSatisfaction?.wireValue)
        integer("issues.count", evaluation.issues.size)
        evaluation.issues.forEachIndexed { index, issue ->
            field("issues[$index].code", issue.code)
            field("issues[$index].message", issue.message)
        }
        field("feedback", evaluation.feedback)
        nullable("completedSentence", evaluation.completedSentence)
        answer("rawAnswer", evaluation.rawAnswer)
        normalizedAnswer("normalizedAnswer", evaluation.normalizedAnswer)
        nullable("matchingResults.presence", evaluation.matchingResults?.let { "present" })
        evaluation.matchingResults?.let { results ->
            integer("matchingResults.count", results.size)
            results.forEachIndexed { index, result ->
                field("matchingResults[$index].leftId", result.leftId)
                nullable("matchingResults[$index].rightId", result.rightId)
                lexicalTarget("matchingResults[$index].lexicalTarget", result.lexicalTarget)
                field("matchingResults[$index].targetStatus", result.targetStatus.wireValue)
                field("matchingResults[$index].meaningSatisfaction", result.meaningSatisfaction.wireValue)
                field("matchingResults[$index].responseConstraintStatus", result.responseConstraintStatus.wireValue)
                field("matchingResults[$index].linguisticStatus", result.linguisticStatus.wireValue)
                field("matchingResults[$index].feedback", result.feedback)
            }
        }
    }.finish()

    internal fun answers(exercise: GeneratedExercise): List<Answer> {
        val task = (exercise.source as VocabularyP1ExerciseSource).task
        return when (task.kind) {
            VocabularyP1TaskKind.CHOICE, VocabularyP1TaskKind.READING -> choiceAnswers(task)
            VocabularyP1TaskKind.RECALL -> recallAnswers(task)
            VocabularyP1TaskKind.MATCHING -> matchingAnswers(exercise)
            VocabularyP1TaskKind.TILES -> tileAnswers(exercise)
        }
    }

    internal fun canonicalAnswer(exercise: GeneratedExercise): Answer {
        val task = (exercise.source as VocabularyP1ExerciseSource).task
        return when (task.kind) {
            VocabularyP1TaskKind.CHOICE -> Answer.Text(task.expected)
            VocabularyP1TaskKind.READING -> Answer.Text(task.targets.single().gloss)
            VocabularyP1TaskKind.RECALL -> Answer.Text(task.expected)
            VocabularyP1TaskKind.MATCHING -> Answer.Matching(
                requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map { MatchingPair(it.leftId, it.rightId) },
            )
            VocabularyP1TaskKind.TILES -> Answer.Order(
                requireNotNull(exercise.inspector.vocabularySource).acceptedOrders.single(),
            )
        }
    }

    private fun FreezeHasher.vocabularyMetadata() {
        val snapshot = PilotContent.currentSnapshot
        val formats = snapshot.formats.filter { it.id in P1_TASK_COUNTS }
        integer("p1Formats.count", formats.size)
        formats.forEachIndexed { index, format -> exerciseFormat("p1Formats[$index]", format) }
        val records = snapshot.lexicalReview.filter { it.id in P1_METADATA_IDS }.sortedBy { it.id }
        integer("p1LexicalMetadata.count", records.size)
        records.forEachIndexed { index, record -> lexicalRecord("p1LexicalMetadata[$index]", record) }
        integer("p1Tasks.count", VocabularyP1Content.tasks.size)
        VocabularyP1Content.tasks.forEachIndexed { index, task -> vocabularyTask("p1Tasks[$index]", task) }
    }

    private fun FreezeHasher.vocabularyProjection(exercise: GeneratedExercise) {
        val inspector = exercise.inspector.vocabularySource
        val source = exercise.source as? VocabularyP1ExerciseSource
        nullable("vocabularyInspector.presence", inspector?.let { "present" })
        inspector?.let { vocabularyInspector("vocabularyInspector", it) }
        nullable("vocabularyPrivateSource.presence", source?.let { "present" })
        source?.let {
            vocabularyTask("vocabularyPrivateSource.task", it.task)
            answerKey("vocabularyPrivateSource.answerKey", it.answerKey)
            stringMap("vocabularyPrivateSource.blockRoles", it.blockRoles)
        }
    }

    private fun FreezeHasher.vocabularyTask(prefix: String, task: VocabularyP1Task) {
        field("$prefix.id", task.id)
        field("$prefix.formatId", task.formatId)
        field("$prefix.kind", task.kind.name)
        field("$prefix.cue", task.cue)
        strings("$prefix.passage", task.passage)
        strings("$prefix.choices", task.choices)
        field("$prefix.expected", task.expected)
        integer("$prefix.targets.count", task.targets.size)
        task.targets.forEachIndexed { index, lexeme -> vocabularyLexeme("$prefix.targets[$index]", lexeme) }
        integer("$prefix.matching.count", task.matching.size)
        task.matching.forEachIndexed { index, row ->
            vocabularyLexeme("$prefix.matching[$index].lexeme", row.first)
            field("$prefix.matching[$index].left", row.second.first)
            field("$prefix.matching[$index].right", row.second.second)
        }
        pairs("$prefix.tileWords", task.tileWords)
        strings("$prefix.canonicalWords", task.canonicalWords)
        nullableStrings("$prefix.knownOffTargetOrder", task.knownOffTargetOrder)
        nullableStrings("$prefix.knownIssueOrder", task.knownIssueOrder)
        nullable("$prefix.targetSkillId", task.targetSkillId)
        nullable("$prefix.targetScopeId", task.targetScopeId)
        nullable("$prefix.assessmentMode", task.assessmentMode)
        field("$prefix.responseShape", task.responseShape)
        nullable("$prefix.factRole", task.factRole)
        integer("$prefix.supportFacts.count", task.supportFacts.size)
        task.supportFacts.forEachIndexed { index, fact -> vocabularyFact("$prefix.supportFacts[$index]", fact) }
        nullable("$prefix.targetSupportFactId", task.targetSupportFactId)
    }

    private fun FreezeHasher.vocabularyLexeme(prefix: String, lexeme: VocabularyP1Lexeme) {
        field("$prefix.entryId", lexeme.entryId)
        field("$prefix.senseId", lexeme.senseId)
        field("$prefix.kind", lexeme.kind.wireValue)
        vocabularyForms("$prefix.forms", lexeme.forms)
        field("$prefix.gloss", lexeme.gloss)
        field("$prefix.selectedUse", lexeme.selectedUse)
    }

    private fun FreezeHasher.vocabularyFact(prefix: String, fact: VocabularyP1Fact) {
        field("$prefix.id", fact.id)
        field("$prefix.referentId", fact.referentId)
        field("$prefix.referentForm", fact.referentForm)
        field("$prefix.predicateId", fact.predicateId)
        field("$prefix.valueId", fact.valueId)
        field("$prefix.valueRole", fact.valueRole)
        field("$prefix.sentence", fact.sentence)
        strings("$prefix.exposureKeys", fact.exposureKeys)
    }

    private fun FreezeHasher.vocabularyInspector(prefix: String, details: VocabularyExerciseSourceDetails) {
        field("$prefix.taskId", details.taskId)
        field("$prefix.operation", details.operation)
        integer("$prefix.lexicalSources.count", details.lexicalSources.size)
        details.lexicalSources.forEachIndexed { index, source -> vocabularySource("$prefix.lexicalSources[$index]", source) }
        nullable("$prefix.expectedAnswer", details.expectedAnswer)
        strings("$prefix.suppliedChoices", details.suppliedChoices)
        integer("$prefix.supportFacts.count", details.supportFacts.size)
        details.supportFacts.forEachIndexed { index, fact ->
            val p = "$prefix.supportFacts[$index]"
            field("$p.factId", fact.factId); field("$p.referentId", fact.referentId)
            field("$p.predicateId", fact.predicateId); field("$p.valueId", fact.valueId)
            field("$p.valueText", fact.valueText); field("$p.valueRole", fact.valueRole)
            field("$p.sentence", fact.sentence); strings("$p.exposureKeys", fact.exposureKeys)
        }
        integer("$prefix.matchingPairs.count", details.matchingPairs.size)
        details.matchingPairs.forEachIndexed { index, pair ->
            val p = "$prefix.matchingPairs[$index]"
            field("$p.leftId", pair.leftId); field("$p.rightId", pair.rightId)
            vocabularySource("$p.lexicalSource", pair.lexicalSource)
        }
        integer("$prefix.tileOccurrences.count", details.tileOccurrences.size)
        details.tileOccurrences.forEachIndexed { index, tile ->
            field("$prefix.tileOccurrences[$index].blockId", tile.blockId)
            field("$prefix.tileOccurrences[$index].lexicalId", tile.lexicalId)
            field("$prefix.tileOccurrences[$index].form", tile.form)
        }
        lists("$prefix.acceptedOrders", details.acceptedOrders)
        nullable("$prefix.targetSupportFactId", details.targetSupportFactId)
        nullable("$prefix.questionRole", details.questionRole)
        nullable("$prefix.normalizationPolicy", details.normalizationPolicy)
        integer("$prefix.knownAlternatives.count", details.knownAlternatives.size)
        details.knownAlternatives.forEachIndexed { index, alternative ->
            val p = "$prefix.knownAlternatives[$index]"
            field("$p.text", alternative.text); field("$p.targetStatus", alternative.targetStatus.wireValue)
            field("$p.linguisticStatus", alternative.linguisticStatus.wireValue)
            field("$p.meaningSatisfaction", alternative.meaningSatisfaction.wireValue)
            field("$p.responseConstraintStatus", alternative.responseConstraintStatus.wireValue)
            nullable("$p.issueCode", alternative.issueCode)
        }
        integer("$prefix.reviewedOrders.count", details.reviewedOrders.size)
        details.reviewedOrders.forEachIndexed { index, order ->
            val p = "$prefix.reviewedOrders[$index]"
            strings("$p.words", order.words); field("$p.targetStatus", order.targetStatus.wireValue)
            field("$p.linguisticStatus", order.linguisticStatus.wireValue)
            field("$p.meaningSatisfaction", order.meaningSatisfaction.wireValue)
            field("$p.responseConstraintStatus", order.responseConstraintStatus.wireValue)
            nullable("$p.issueCode", order.issueCode)
        }
    }

    private fun FreezeHasher.vocabularySource(prefix: String, source: VocabularyLexicalSourceDetails) {
        field("$prefix.entryId", source.entryId); field("$prefix.senseId", source.senseId)
        field("$prefix.kind", source.kind.wireValue); vocabularyForms("$prefix.forms", source.forms)
        field("$prefix.gloss", source.gloss); field("$prefix.selectedUse", source.selectedUse)
    }

    private fun FreezeHasher.vocabularyForms(prefix: String, forms: VocabularyLexicalForms) {
        nullable("$prefix.infinitive", forms.infinitive); nullable("$prefix.present", forms.present)
        nullable("$prefix.bundle", forms.bundle); nullable("$prefix.form", forms.form)
    }

    private fun authoredExercises(format: FormatSpec): List<GeneratedExercise> {
        val expected = VocabularyP1Content.tasks(format.id).map(VocabularyP1Task::id).toSet()
        val found = mutableMapOf<String, GeneratedExercise>()
        var index = 0
        while (found.keys != expected && index <= P1_SCAN_LIMIT) {
            val exercise = generate(format, FIXED_SEED, index++)
            found.putIfAbsent(requireNotNull(exercise.inspector.vocabularySource).taskId, exercise)
        }
        require(found.keys == expected) { "Could not enumerate all authored ${format.id} tasks" }
        return expected.sorted().map(found::getValue)
    }

    private fun choiceAnswers(task: VocabularyP1Task): List<Answer> = buildList {
        task.choices.forEach { add(Answer.Text(it)) }
        add(Answer.Text("outside supplied bank")); add(Answer.Text(""))
        add(Answer.Order(emptyList())); add(Answer.Matching(emptyList())); add(Answer.Gaps(emptyList()))
    }.distinct()

    private fun recallAnswers(task: VocabularyP1Task): List<Answer> = buildList {
        add(Answer.Text(task.expected)); add(Answer.Text("  ${task.expected}  "))
        add(Answer.Text(task.expected.uppercase(Locale.ROOT)))
        unicodeVariants(task.expected).forEach { add(Answer.Text(it)) }
        val inspectorAlternatives = generatedTask(task).inspector.vocabularySource?.knownAlternatives.orEmpty()
        inspectorAlternatives.forEach { add(Answer.Text(it.text)) }
        if (task.id == "p1-4.5") {
            add(Answer.Text("en vand"))
            add(Answer.Text("et vand"))
        }
        add(Answer.Text("ukendt")); add(Answer.Text("${task.expected} ekstra")); add(Answer.Text(""))
        add(Answer.Order(emptyList())); add(Answer.Matching(emptyList())); add(Answer.Gaps(emptyList()))
    }.distinct()

    private fun matchingAnswers(exercise: GeneratedExercise): List<Answer> {
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        val partial = partialBijections(columns.left.map { it.id }, columns.right.map { it.id })
        val correct = requireNotNull(exercise.inspector.vocabularySource).matchingPairs
            .map { MatchingPair(it.leftId, it.rightId) }
        val orderedArrayVariants = listOf(
            Answer.Matching(correct.reversed()),
            Answer.Matching(correct.dropLast(1).reversed()),
        )
        val malformed = listOf(
            Answer.Matching(listOf(MatchingPair("unknown", columns.right.first().id))),
            Answer.Matching(listOf(MatchingPair(columns.left.first().id, "unknown"))),
            Answer.Matching(
                listOf(
                    MatchingPair(columns.left.first().id, columns.right.first().id),
                    MatchingPair(columns.left.first().id, columns.right.last().id),
                ),
            ),
            Answer.Matching(
                listOf(
                    MatchingPair(columns.left.first().id, columns.right.first().id),
                    MatchingPair(columns.left.last().id, columns.right.first().id),
                ),
            ),
            Answer.Text("ukendt"), Answer.Order(emptyList()), Answer.Gaps(emptyList()),
        )
        return partial.map { Answer.Matching(it) } + orderedArrayVariants + malformed
    }

    private fun unicodeVariants(expected: String): List<String> {
        val index = expected.indexOfFirst { it in "aeiou" }
        if (index < 0) return listOf(expected + "\u0301")
        val vowel = expected[index]
        val composed = when (vowel) {
            'a' -> "á"
            'e' -> "é"
            'i' -> "í"
            'o' -> "ó"
            else -> "ú"
        }
        return listOf(
            expected.replaceRange(index, index + 1, composed),
            expected.replaceRange(index, index + 1, "$vowel\u0301"),
        )
    }

    private fun tileAnswers(exercise: GeneratedExercise): List<Answer> {
        val blocks = exercise.prompt.blocks.map { it.id }
        val details = requireNotNull(exercise.inspector.vocabularySource)
        val authoredOrders = details.acceptedOrders.map { Answer.Order(it) } + details.reviewedOrders.map { reviewed ->
            Answer.Order(tileIdsForWords(details, reviewed.words))
        }
        return (blocks.permutations().map { Answer.Order(it) } + authoredOrders + listOf(
            Answer.Order(blocks.dropLast(1)), Answer.Order(emptyList()), Answer.Order(blocks + blocks.first()),
            Answer.Order(listOf("unknown")), Answer.Text("ukendt"), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
        )).distinct()
    }

    private fun tileIdsForWords(details: VocabularyExerciseSourceDetails, words: List<String>): List<String> {
        val idsByForm = details.tileOccurrences.groupBy(VocabularyTileOccurrenceDetails::form).mapValues { (_, occurrences) ->
            ArrayDeque(occurrences.map(VocabularyTileOccurrenceDetails::blockId))
        }
        return words.map { word ->
            requireNotNull(idsByForm[word]?.removeFirstOrNull()) { "No unused tile occurrence for reviewed word $word" }
        }
    }

    private fun partialBijections(left: List<String>, right: List<String>): List<List<MatchingPair>> =
        (0..left.size).flatMap { size ->
            left.combinations(size).flatMap { selectedLeft ->
                right.combinations(size).flatMap { selectedRight ->
                    selectedRight.permutations().map { permutation ->
                        selectedLeft.zip(permutation, ::MatchingPair)
                    }
                }
            }
        }

    private fun generatedTask(task: VocabularyP1Task): GeneratedExercise = authoredExercises(
        p1Formats.single { it.id == task.formatId },
    ).single { it.inspector.vocabularySource?.taskId == task.id }

    private fun boundaryRows(format: FormatSpec): List<String> = BOUNDARY_SEEDS.flatMap { seed ->
        listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).map { index ->
            generationRow(generate(format, seed, index))
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
        val (kind, raw) = answerColumns(answer)
        return listOf(
            kind, exercise.formatId, exercise.targetSkillId ?: NULL_COLUMN, exercise.reproduction.seed,
            exercise.reproduction.index, Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8)),
            evaluationHash(engine.evaluate(exercise, answer)),
        ).joinToString("\t")
    }

    internal fun inheritedCanonicalStatuses(): List<Pair<String, TargetStatus>> = inheritedFormats.flatMap { format ->
        List(format.cycleSize) { index ->
            val exercise = generate(format, FIXED_SEED, index)
            format.id to engine.evaluate(exercise, inheritedCanonicalAnswer(exercise)).targetStatus
        }
    }

    private fun inheritedCanonicalAnswer(exercise: GeneratedExercise): Answer {
        if (exercise.formatId == "E009") {
            return Answer.Text(requireNotNull(exercise.inspector.canonicalSentence))
        }
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

    private fun requireLiteralPairEighteen() {
        require(PilotContent.currentSnapshot.contentRevision == CONTENT_REVISION)
        require(PilotContent.currentSnapshot.generatorRevision == GENERATOR_REVISION)
        require(engine.evaluationRevision(CONTENT_REVISION, GENERATOR_REVISION) == EVALUATION_REVISION)
    }

    private fun cycleSize(id: String): Int = when (id) {
        "E001", "E003", "E004", "E005", "E009", "E012", "E013" -> 99
        "E002", "E006", "E015" -> 92
        "E007", "E008" -> 3
        "E010" -> 24
        "E011" -> 4
        "E014" -> 32
        "E016", "E017" -> 7
        "E018", "E019", "E022", "E023" -> 8
        "E020", "E021", "E026", "E027" -> 2
        "E024", "E028" -> 3
        "E025" -> 6
        "E029", "E030" -> 4
        else -> error("Unknown inherited format $id")
    }

    private fun <T> List<T>.permutations(): List<List<T>> = if (size <= 1) listOf(this) else indices.flatMap { index ->
        (take(index) + drop(index + 1)).permutations().map { listOf(this[index]) + it }
    }

    private fun <T> List<T>.combinations(size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList())
        size > this.size -> emptyList()
        else -> indices.flatMap { index -> drop(index + 1).combinations(size - 1).map { listOf(this[index]) + it } }
    }

    private data class FormatSpec(val id: String, val targetSkillId: String?, val cycleSize: Int)

    private const val RELEASE_SOURCE_COMMIT = "9eefcfed708670ce45dbdd9b11adc41f9fe17d25"
    private const val CAPTURE_DATE = "2026-11-12"
    private const val CONTENT_REVISION = "pilot-18"
    private const val GENERATOR_REVISION = "generator-18"
    private const val EVALUATION_REVISION = "evaluation-18"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val P1_SCAN_LIMIT = 64
    private const val NULL_COLUMN = "-"
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    private val P1_METADATA_IDS = (41..49).map { "L${it.toString().padStart(3, '0')}" }.toSet()
    internal val P1_TASK_COUNTS = mapOf(
        "E031" to 3, "E032" to 1, "E033" to 4, "E034" to 2, "E035" to 1,
        "E036" to 1, "E037" to 3, "E038" to 1, "E039" to 7,
    )
}

internal class FreezeHasher(schema: String) {
    private val digest = MessageDigest.getInstance("SHA-256")
    init { token(schema) }
    fun field(name: String, value: String) { token(name); token("value"); token(value) }
    fun nullable(name: String, value: String?) {
        token(name)
        token(if (value == null) "null" else "value")
        value?.let(::token)
    }
    fun integer(name: String, value: Int) = field(name, value.toString())
    fun boolean(name: String, value: Boolean) = field(name, value.toString())
    fun strings(name: String, values: List<String>) {
        integer("$name.count", values.size)
        values.forEachIndexed { i, v -> field("$name[$i]", v) }
    }
    fun nullableStrings(name: String, values: List<String>?) {
        nullable("$name.presence", values?.let { "present" })
        values?.let { strings(name, it) }
    }
    fun pairs(name: String, values: List<Pair<String, String>>) {
        integer("$name.count", values.size)
        values.forEachIndexed { i, v ->
            field("$name[$i].first", v.first)
            field("$name[$i].second", v.second)
        }
    }
    fun lists(name: String, values: List<List<String>>) {
        integer("$name.count", values.size)
        values.forEachIndexed { i, v -> strings("$name[$i]", v) }
    }
    fun presentation(name: String, value: ExercisePresentation) {
        field("$name.instruction", value.instruction)
        field("$name.context", value.context)
        field("$name.hint", value.hint)
        prompt("$name.prompt", value.prompt)
        integer("$name.introduction.count", value.introduction.size)
        value.introduction.forEachIndexed { i, section ->
            field("$name.introduction[$i].title", section.title)
            field("$name.introduction[$i].body", section.body)
            nullable("$name.introduction[$i].bodyLanguage", section.bodyLanguage)
        }
    }
    fun prompt(name: String, value: ExercisePrompt) {
        field("$name.before", value.before)
        field("$name.after", value.after)
        strings("$name.choices", value.choices)
        integer("$name.blocks.count", value.blocks.size)
        value.blocks.forEachIndexed { i, block ->
            field("$name.blocks[$i].id", block.id)
            field("$name.blocks[$i].text", block.text)
        }
        field("$name.punctuation", value.punctuation)
        nullable("$name.inputLabel", value.inputLabel)
        nullable("$name.replacementWord", value.replacementWord)
        nullable("$name.cue", value.cue)
        nullable("$name.cueLanguage", value.cueLanguage)
        nullable("$name.answerLanguage", value.answerLanguage)
        nullableStrings("$name.passage", value.passage)
        nullable("$name.tileMode", value.tileMode)
        nullable("$name.requiredTileCount", value.requiredTileCount?.toString())
        nullable("$name.matchingColumns.presence", value.matchingColumns?.let { "present" })
        value.matchingColumns?.let { columns ->
            integer("$name.matchingColumns.left.count", columns.left.size)
            columns.left.forEachIndexed { i, item ->
                field("$name.matchingColumns.left[$i].id", item.id)
                field("$name.matchingColumns.left[$i].text", item.text)
            }
            integer("$name.matchingColumns.right.count", columns.right.size)
            columns.right.forEachIndexed { i, item ->
                field("$name.matchingColumns.right[$i].id", item.id)
                field("$name.matchingColumns.right[$i].text", item.text)
            }
        }
    }
    fun lexicalTargets(name: String, values: List<LexicalTarget>?) {
        nullable("$name.presence", values?.let { "present" })
        values?.let {
            integer("$name.count", it.size)
            it.forEachIndexed { i, value -> lexicalTarget("$name[$i]", value) }
        }
    }
    fun lexicalTarget(name: String, value: LexicalTarget) {
        field("$name.entryId", value.entryId)
        field("$name.senseId", value.senseId)
        field("$name.facet", value.facet)
        field("$name.direction", value.direction)
    }
    fun exerciseFormat(name: String, value: ExerciseFormat) {
        field("$name.id", value.id)
        field("$name.title", value.title)
        nullable("$name.targetSkillId", value.targetSkillId)
        nullable("$name.lexicalTask.facet", value.lexicalTask?.facet)
        nullable("$name.lexicalTask.direction", value.lexicalTask?.direction)
        nullable("$name.readingTask.facet", value.readingTask?.facet)
    }
    fun lexicalRecord(name: String, value: LexicalReviewRecord) {
        field("$name.id", value.id)
        field("$name.headword", value.headword)
        field("$name.selectedSense", value.selectedSense)
        integer("$name.activeForms.count", value.activeForms.size)
        value.activeForms.entries.sortedBy { it.key }.forEachIndexed { i, entry ->
            field("$name.activeForms[$i].name", entry.key)
            field("$name.activeForms[$i].value", entry.value)
        }
        field("$name.constraints", value.constraints)
        strings("$name.sourceUrls", value.sourceUrls)
        field("$name.reviewState", value.reviewState)
    }
    fun stringMap(name: String, value: Map<String, String>) {
        integer("$name.count", value.size)
        value.entries.sortedBy { it.key }.forEachIndexed { i, entry ->
            field("$name[$i].key", entry.key)
            field("$name[$i].value", entry.value)
        }
    }
    fun answerKey(name: String, value: AnswerKey) {
        when (value) {
            is AnswerKey.PresentForm -> {
                field("$name.kind", "present-form")
                field("$name.infinitive", value.infinitive)
                field("$name.present", value.present)
            }
            is AnswerKey.Article -> {
                field("$name.kind", "article")
                field("$name.expected", value.expected)
                field("$name.noun", value.noun)
            }
            is AnswerKey.SubjectFirstOrder -> {
                field("$name.kind", "subject-first-order")
                strings("$name.canonicalRoles", value.canonicalRoles)
            }
            is AnswerKey.MeaningChoice -> {
                field("$name.kind", "meaning-choice")
                field("$name.expected", value.expected)
                strings("$name.suppliedChoices", value.suppliedChoices.sorted())
            }
            is AnswerKey.Headword -> {
                field("$name.kind", "headword")
                field("$name.expected.entryId", value.expected.entryId)
                field("$name.expected.senseId", value.expected.senseId)
                field("$name.expected.headword", value.expected.headword)
                field("$name.expected.gloss", value.expected.gloss)
                field("$name.expected.article", value.expected.article)
            }
            is AnswerKey.SuppliedChoice -> {
                field("$name.kind", "supplied-choice")
                field("$name.expected", value.expected)
                strings("$name.suppliedChoices", value.suppliedChoices.sorted())
            }
            is AnswerKey.TileOrder -> tileOrder(name, value)
            is AnswerKey.Matching -> {
                field("$name.kind", "matching")
                stringMap("$name.expectedByLeftId", value.expectedByLeftId)
            }
            is AnswerKey.Gaps -> {
                field("$name.kind", "gaps")
                stringMap("$name.expectedBySlotId", value.expectedBySlotId)
            }
            is AnswerKey.ModuleOneForm -> {
                field("$name.kind", "module-one-form")
                field("$name.expected", value.expected)
                strings("$name.knownAlternatives", value.knownAlternatives.sorted())
                boolean("$name.selection", value.selection)
            }
        }
    }
    private fun tileOrder(name: String, value: AnswerKey.TileOrder) {
        field("$name.kind", "tile-order")
        identities("$name.canonical", value.canonical)
        nullable("$name.fronted.presence", value.fronted?.let { "present" })
        value.fronted?.let { identities("$name.fronted", it) }
        nullable("$name.infinitive.presence", value.infinitive?.let { "present" })
        value.infinitive?.let { identities("$name.infinitive", it) }
    }
    private fun identities(name: String, values: List<TileIdentity>) {
        integer("$name.count", values.size)
        values.forEachIndexed { i, value ->
            field("$name[$i].lexicalId", value.lexicalId)
            field("$name[$i].form", value.form)
        }
    }
    fun answer(name: String, value: Answer) {
        val (kind, raw) = answerColumns(value)
        field("$name.kind", kind)
        field("$name.raw", raw)
    }
    fun normalizedAnswer(name: String, value: NormalizedAnswer) {
        when (value) {
            is NormalizedAnswer.Text -> {
                field("$name.kind", "T")
                field("$name.raw", value.text)
            }
            is NormalizedAnswer.Order -> {
                field("$name.kind", "O")
                strings("$name.ids", value.blockIds)
            }
            is NormalizedAnswer.Matching -> {
                field("$name.kind", "M")
                integer("$name.count", value.pairs.size)
                value.pairs.forEachIndexed { i, pair ->
                    field("$name[$i].leftId", pair.leftId)
                    field("$name[$i].rightId", pair.rightId)
                }
            }
            is NormalizedAnswer.Gaps -> {
                field("$name.kind", "P")
                integer("$name.count", value.selections.size)
                value.selections.forEachIndexed { i, selection ->
                    field("$name[$i].slotId", selection.slotId)
                    field("$name[$i].choiceId", selection.choiceId)
                }
            }
        }
    }
    fun finish(): String = digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
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

private fun answerColumns(answer: Answer): Pair<String, String> = when (answer) {
    is Answer.Text -> "T" to answer.text
    is Answer.Order -> "O" to answer.blockIds.joinToString("\u001f")
    is Answer.Matching -> "M" to answer.pairs.joinToString("\u001e") { "${it.leftId}\u001d${it.rightId}" }
    is Answer.Gaps -> "P" to answer.selections.joinToString("\u001e") { "${it.slotId}\u001d${it.choiceId}" }
}

private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
    .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

private const val CONTENT_REVISION = "pilot-18"
private const val GENERATOR_REVISION = "generator-18"
private const val EVALUATION_REVISION = "evaluation-18"

