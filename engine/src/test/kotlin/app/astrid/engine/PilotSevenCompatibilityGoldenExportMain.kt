package app.astrid.engine

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotSevenCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotSevenCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotSevenCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object PilotSevenCompatibilityGolden {
    private val engine = PilotEngine()
    private val inheritedFormats = listOf(
        FormatSpec("E001", "G004", 99), FormatSpec("E002", "G006", 92), FormatSpec("E003", "G005", 99),
        FormatSpec("E004", "G004", 99), FormatSpec("E005", "G004", 99), FormatSpec("E006", "G006", 92),
        FormatSpec("E007", null, 3), FormatSpec("E008", null, 3), FormatSpec("E009", "G005", 99),
        FormatSpec("E010", null, 24), FormatSpec("E011", null, 4), FormatSpec("E012", "G005", 99),
        FormatSpec("E013", "G004", 99),
    )

    fun fixtureBytes(): ByteArray = buildList {
        inheritedFormats.forEach { format ->
            repeat(format.cycleSize) { index ->
                val exercise = generate(format.id, format.targetSkillId, FIXED_SEED, index)
                add(generationRow(format.id, format.targetSkillId, FIXED_SEED, index, exercise))
                add(evaluationRow(format.id, format.targetSkillId, FIXED_SEED, index, exercise, correctAnswer(exercise)))
            }
        }
        inheritedFormats.forEach { format ->
            BOUNDARY_SEEDS.forEach { seed ->
                listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).forEach { index ->
                    add(generationRow(format.id, format.targetSkillId, seed, index, generate(format.id, format.targetSkillId, seed, index)))
                }
            }
        }
        inheritedFormats.filterNot { it.id == WRITING_FORMAT_ID }.forEach { format ->
            val exercise = specialExercise(format)
            specialAnswers(exercise).forEach { answer ->
                add(evaluationRow(format.id, format.targetSkillId, FIXED_SEED, exercise.reproduction.index, exercise, answer))
            }
        }
        writingEvaluationCases().forEach { case ->
            add(evaluationRow(WRITING_FORMAT_ID, "G005", FIXED_SEED, case.index, case.exercise, Answer.Text(case.answer)))
        }
        val writing = writingExercise("Hun læser en avis.")
        add(evaluationRow(WRITING_FORMAT_ID, "G005", FIXED_SEED, writing.reproduction.index, writing, Answer.Order(emptyList())))

        repeat(MATCHING_SAMPLE_SIZE) { index ->
            val exercise = generate(MATCHING_FORMAT_ID, null, FIXED_SEED, index)
            add(generationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, index, exercise))
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, index, exercise, correctAnswer(exercise)))
        }
        BOUNDARY_SEEDS.forEach { seed ->
            MATCHING_BOUNDARY_INDICES.forEach { index ->
                add(generationRow(MATCHING_FORMAT_ID, null, seed, index, generate(MATCHING_FORMAT_ID, null, seed, index)))
            }
        }
        val matching = generate(MATCHING_FORMAT_ID, null, FIXED_SEED, 0)
        validPartialBijections(matching).forEach { pairs ->
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, 0, matching, Answer.Matching(pairs)))
        }
        val correctPairs = requireNotNull(matching.inspector.matchingSource).pairs.map { MatchingPair(it.leftId, it.rightId) }
        listOf(correctPairs.reversed(), correctPairs.drop(1) + correctPairs.first()).forEach { pairs ->
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, 0, matching, Answer.Matching(pairs)))
        }
        malformedMatchingAnswers(matching).forEach { answer ->
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, 0, matching, answer))
        }

        addGapRows()
    }.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    private fun MutableList<String>.addGapRows() {
        repeat(GAP_CYCLE_SIZE) { index ->
            val exercise = generate(GAP_FORMAT_ID, GAP_TARGET, FIXED_SEED, index)
            add(generationRow(GAP_FORMAT_ID, GAP_TARGET, FIXED_SEED, index, exercise))
            orderedGapAnswers(exercise).forEach { answer ->
                add(evaluationRow(GAP_FORMAT_ID, GAP_TARGET, FIXED_SEED, index, exercise, Answer.Gaps(answer)))
            }
        }
        BOUNDARY_SEEDS.forEach { seed ->
            GAP_BOUNDARY_INDICES.forEach { index ->
                add(generationRow(GAP_FORMAT_ID, GAP_TARGET, seed, index, generate(GAP_FORMAT_ID, GAP_TARGET, seed, index)))
            }
        }
        val gap = generate(GAP_FORMAT_ID, GAP_TARGET, FIXED_SEED, 0)
        malformedGapAnswers(gap).forEach { answer ->
            add(evaluationRow(GAP_FORMAT_ID, GAP_TARGET, FIXED_SEED, 0, gap, answer))
        }
    }

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-7 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val rowCount = bytes.count { it == '\n'.code.toByte() }
        val reviewBytes = PilotReview.renderMarkdown().toByteArray(Charsets.UTF_8)
        val description = "All inherited cycles and boundaries, bounded E014 matching behavior, and every E015 clause " +
            "with all nine semantic states, all thirteen ordered selection arrays, malformed and raw Unicode wrong-kind " +
            "answers, nested hashes, and boundaries."
        val provenance = """
            baselineCommit=$BASELINE_COMMIT
            captureDate=2026-11-09
            reviewState=compatibility-only-not-linguistic-approval
            contentRevision=$CONTENT_REVISION
            generatorRevision=$GENERATOR_REVISION
            evaluationRevision=$EVALUATION_REVISION
            rowCount=$rowCount
            fixtureSha256=${bytes.sha256()}
            reviewSha256=${reviewBytes.sha256()}
            description=$description
        """.trimIndent() + "\n"
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
        println("Wrote $rowCount rows to $fixturePath (${bytes.sha256()})")
        println("Review SHA-256: ${reviewBytes.sha256()}")
    }

    fun generationHash(exercise: GeneratedExercise): String = CanonicalHasher("pilot-7-generation-v1").apply {
        field("inherited", PilotSixCompatibilityGolden.generationHash(exercise))
        gapPrompt(exercise.prompt.gapPrompt)
        gapSource(exercise.inspector.gapSource)
    }.finish()

    fun evaluationHash(evaluation: Evaluation): String = CanonicalHasher("pilot-7-evaluation-v1").apply {
        field("targetStatus", evaluation.targetStatus.wireValue)
        field("linguisticStatus", evaluation.linguisticStatus.wireValue)
        field("issues.count", evaluation.issues.size.toString())
        evaluation.issues.forEachIndexed { index, issue ->
            field("issues[$index].code", issue.code)
            field("issues[$index].message", issue.message)
        }
        field("feedback", evaluation.feedback)
        nullable("completedSentence", evaluation.completedSentence)
        answer("rawAnswer", evaluation.rawAnswer)
        normalizedAnswer(evaluation.normalizedAnswer)
        nullable("linguisticScope", evaluation.linguisticScope?.wireValue)
        nullable("responseConstraintStatus", evaluation.responseConstraintStatus?.wireValue)
        nullable("meaningSatisfaction", evaluation.meaningSatisfaction?.wireValue)
        nullable("matchingResults.presence", evaluation.matchingResults?.let { "present" })
        evaluation.matchingResults?.forEachIndexed { index, result ->
            field("matchingResults[$index]", result.toString())
        }
        nullable("gapResults.presence", evaluation.gapResults?.let { "present" })
        evaluation.gapResults?.let { results ->
            field("gapResults.count", results.size.toString())
            results.forEachIndexed { index, result ->
                field("gapResults[$index].slotId", result.slotId)
                nullable("gapResults[$index].choiceId", result.choiceId)
                field("gapResults[$index].targetSkillId", result.targetSkillId)
                field("gapResults[$index].assessmentRole", result.assessmentRole.wireValue)
                field("gapResults[$index].targetStatus", result.targetStatus.wireValue)
                field("gapResults[$index].responseConstraintStatus", result.responseConstraintStatus.wireValue)
                field("gapResults[$index].linguisticStatus", result.linguisticStatus.wireValue)
                field("gapResults[$index].feedback", result.feedback)
            }
        }
    }.finish()

    private fun CanonicalHasher.gapPrompt(prompt: GapPrompt?) {
        nullable("prompt.gapPrompt.presence", prompt?.let { "present" })
        prompt?.let {
            strings("prompt.gapPrompt.segments", it.segments)
            field("prompt.gapPrompt.slots.count", it.slots.size.toString())
            it.slots.forEachIndexed { index, slot ->
                field("prompt.gapPrompt.slots[$index].id", slot.id)
                field("prompt.gapPrompt.slots[$index].label", slot.label)
                field("prompt.gapPrompt.slots[$index].targetSkillId", slot.targetSkillId)
                field("prompt.gapPrompt.slots[$index].assessmentRole", slot.assessmentRole.wireValue)
                field("prompt.gapPrompt.slots[$index].choices.count", slot.choices.size.toString())
                slot.choices.forEachIndexed { choiceIndex, choice ->
                    field("prompt.gapPrompt.slots[$index].choices[$choiceIndex].id", choice.id)
                    field("prompt.gapPrompt.slots[$index].choices[$choiceIndex].text", choice.text)
                }
            }
        }
    }

    private fun CanonicalHasher.gapSource(source: GapSourceDetails?) {
        nullable("inspector.gapSource.presence", source?.let { "present" })
        source?.let {
            field("inspector.gapSource.slots.count", it.slots.size.toString())
            it.slots.forEachIndexed { index, slot ->
                field("inspector.gapSource.slots[$index].slotId", slot.slotId)
                field("inspector.gapSource.slots[$index].expectedChoiceId", slot.expectedChoiceId)
                field("inspector.gapSource.slots[$index].lexicalId", slot.lexicalId)
                field("inspector.gapSource.slots[$index].expectedForm", slot.expectedForm)
                field("inspector.gapSource.slots[$index].alternativeForm", slot.alternativeForm)
            }
        }
    }

    private fun CanonicalHasher.answer(name: String, answer: Answer) = when (answer) {
        is Answer.Text -> {
            field("$name.type", "text")
            field("$name.text", answer.text)
        }
        is Answer.Order -> {
            field("$name.type", "order")
            strings("$name.blockIds", answer.blockIds)
        }
        is Answer.Matching -> {
            field("$name.type", "matching")
            matchingPairs("$name.pairs", answer.pairs)
        }
        is Answer.Gaps -> {
            field("$name.type", "gaps")
            gapSelections("$name.selections", answer.selections)
        }
    }

    private fun CanonicalHasher.normalizedAnswer(answer: NormalizedAnswer) = when (answer) {
        is NormalizedAnswer.Text -> {
            field("normalizedAnswer.type", "text")
            field("normalizedAnswer.text", answer.text)
        }
        is NormalizedAnswer.Order -> {
            field("normalizedAnswer.type", "order")
            strings("normalizedAnswer.blockIds", answer.blockIds)
        }
        is NormalizedAnswer.Matching -> {
            field("normalizedAnswer.type", "matching")
            matchingPairs("normalizedAnswer.pairs", answer.pairs)
        }
        is NormalizedAnswer.Gaps -> {
            field("normalizedAnswer.type", "gaps")
            gapSelections("normalizedAnswer.selections", answer.selections)
        }
    }

    private fun CanonicalHasher.matchingPairs(name: String, pairs: List<MatchingPair>) {
        field("$name.count", pairs.size.toString())
        pairs.forEachIndexed { index, pair ->
            field("$name[$index].leftId", pair.leftId)
            field("$name[$index].rightId", pair.rightId)
        }
    }

    private fun CanonicalHasher.gapSelections(name: String, selections: List<GapSelection>) {
        field("$name.count", selections.size.toString())
        selections.forEachIndexed { index, selection ->
            field("$name[$index].slotId", selection.slotId)
            field("$name[$index].choiceId", selection.choiceId)
        }
    }

    private fun generate(format: String, target: String?, seed: Int, index: Int) =
        engine.generate(GenerationRequest(format, target, seed, index, CONTENT_REVISION, GENERATOR_REVISION))

    private fun generationRow(format: String, target: String?, seed: Int, index: Int, exercise: GeneratedExercise) =
        listOf("G", format, target ?: NULL_COLUMN, seed, index, generationHash(exercise)).joinToString("\t")

    private fun evaluationRow(
        format: String, target: String?, seed: Int, index: Int, exercise: GeneratedExercise, answer: Answer,
    ): String {
        val (kind, raw) = when (answer) {
            is Answer.Text -> "T" to answer.text
            is Answer.Order -> "O" to answer.blockIds.joinToString(ORDER_SEPARATOR)
            is Answer.Matching -> "M" to answer.pairs.joinToString(PAIR_SEPARATOR) { "${it.leftId}$ID_SEPARATOR${it.rightId}" }
            is Answer.Gaps -> "P" to answer.selections.joinToString(PAIR_SEPARATOR) { "${it.slotId}$ID_SEPARATOR${it.choiceId}" }
        }
        return listOf(
            kind, format, target ?: NULL_COLUMN, seed, index,
            Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8)),
            evaluationHash(engine.evaluate(exercise, answer)),
        ).joinToString("\t")
    }

    private fun correctAnswer(exercise: GeneratedExercise): Answer {
        if (exercise.formatId == WRITING_FORMAT_ID) return Answer.Text(requireNotNull(exercise.inspector.canonicalSentence))
        return when (val key = exercise.source.answerKey) {
            is AnswerKey.PresentForm -> Answer.Text(key.present)
            is AnswerKey.Article -> Answer.Text(key.expected)
            is AnswerKey.SubjectFirstOrder -> Answer.Order(orderForRoles(exercise, key.canonicalRoles))
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
        }
    }

    private fun specialAnswers(exercise: GeneratedExercise): List<Answer> = when (val key = exercise.source.answerKey) {
        is AnswerKey.PresentForm -> listOf(
            Answer.Text(" ${key.present} "), Answer.Text(key.infinitive),
            Answer.Text(PilotContent.verbs.first { it.present != key.present }.present),
            Answer.Text("laeser"), Answer.Text("e\u0301t"), Answer.Order(emptyList()),
        )
        is AnswerKey.Article -> {
            val other = if (key.expected == "en") "et" else "en"
            val numeral = if (key.expected == "en") "én" else "ét"
            val otherNumeral = if (key.expected == "en") "ét" else "én"
            listOf(Answer.Text(numeral), Answer.Text(other), Answer.Text(otherNumeral), Answer.Text("x"),
                Answer.Text(" ${key.expected} "), Answer.Order(emptyList()))
        }
        is AnswerKey.SubjectFirstOrder -> {
            val correct = orderForRoles(exercise, key.canonicalRoles)
            listOf(Answer.Order(correct.reversed()), Answer.Order(exercise.prompt.blocks.map { it.id }),
                Answer.Order(correct.dropLast(1)), Answer.Order(correct + correct.first()),
                Answer.Order(listOf("unknown-block")), Answer.Text("raw order text"))
        }
        is AnswerKey.MeaningChoice -> listOf(
            Answer.Text(" ${key.expected.uppercase(Locale.ROOT)} "),
            Answer.Text(key.suppliedChoices.first { it != key.expected }),
            Answer.Text("journal"), Answer.Text("e\u0301"), Answer.Order(emptyList()),
        )
        is AnswerKey.Headword -> {
            val other = PilotContent.nouns.first { it.id != key.expected.entryId }
            val wrongArticle = if (key.expected.article == "en") "et" else "en"
            val otherWrongArticle = if (other.gender.article == "en") "et" else "en"
            listOf(Answer.Text(key.expected.headword.uppercase(Locale.ROOT)), Answer.Text(other.text),
                Answer.Text("${key.expected.article} ${key.expected.headword}"), Answer.Text("$wrongArticle ${key.expected.headword}"),
                Answer.Text("${other.gender.article} ${other.text}"), Answer.Text("$otherWrongArticle ${other.text}"),
                Answer.Text("tidende"), Answer.Text("   "), Answer.Text("${key.expected.headword}\u00a0${other.text}"),
                Answer.Text("a\u030Avis"), Answer.Order(emptyList()))
        }
        is AnswerKey.SuppliedChoice -> listOf(
            Answer.Text("  ${key.expected}  "), Answer.Text(key.suppliedChoices.first { it != key.expected }),
            Answer.Text(key.expected.uppercase(Locale.ROOT)), Answer.Text("e\u0301t magasin"), Answer.Order(emptyList()),
        )
        is AnswerKey.TileOrder -> tileAnswers(exercise, key)
        is AnswerKey.Matching, is AnswerKey.Gaps -> error("Newer keys are covered separately")
    }

    private fun specialExercise(format: FormatSpec): GeneratedExercise {
        if (format.id !in setOf("E012", "E013")) return generate(format.id, format.targetSkillId, FIXED_SEED, 0)
        return (0 until format.cycleSize).asSequence().map { generate(format.id, format.targetSkillId, FIXED_SEED, it) }
            .first { (it.source.answerKey as AnswerKey.TileOrder).fronted != null }
    }

    private fun tileAnswers(exercise: GeneratedExercise, key: AnswerKey.TileOrder): List<Answer> {
        val canonical = idsFor(key.canonical, exercise)
        return buildList {
            add(Answer.Order(canonical))
            key.fronted?.let { add(Answer.Order(idsFor(it, exercise))) }
            key.infinitive?.let { add(Answer.Order(idsFor(it, exercise))) }
            add(Answer.Order(canonical.dropLast(1)))
            add(Answer.Order(canonical + canonical.first()))
            add(Answer.Order(canonical.dropLast(1) + "unknown-block"))
            add(Answer.Order(canonical.reversed()))
            add(Answer.Text(requireNotNull(exercise.inspector.canonicalSentence)))
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

    private fun orderedGapAnswers(exercise: GeneratedExercise): List<List<GapSelection>> {
        val prompt = requireNotNull(exercise.prompt.gapPrompt)
        return buildList {
            add(emptyList())
            prompt.slots.forEach { slot -> slot.choices.forEach { add(listOf(GapSelection(slot.id, it.id))) } }
            prompt.slots[0].choices.forEach { first ->
                prompt.slots[1].choices.forEach { second ->
                    val selections = listOf(GapSelection("g0", first.id), GapSelection("g1", second.id))
                    add(selections)
                    add(selections.reversed())
                }
            }
        }.also { check(it.size == GAP_ORDERED_ANSWER_COUNT) }
    }

    private fun malformedGapAnswers(exercise: GeneratedExercise): List<Answer> {
        val prompt = requireNotNull(exercise.prompt.gapPrompt)
        val selection = GapSelection("g0", prompt.slots[0].choices[0].id)
        return listOf(
            Answer.Gaps(listOf(selection, selection)), Answer.Gaps(listOf(GapSelection("unknown", "c0"))),
            Answer.Gaps(listOf(GapSelection("g0", "unknown"))),
            Answer.Gaps(listOf(selection, GapSelection("g1", "c0"), GapSelection("g0", "c1"))),
            Answer.Text(" rå\u00a0gaps e\u0301 "), Answer.Order(listOf("g0", "c0")),
            Answer.Matching(listOf(MatchingPair("g0", "c0"))),
        )
    }

    private fun validPartialBijections(exercise: GeneratedExercise): List<List<MatchingPair>> {
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        return buildList {
            for (size in 0..columns.left.size) combinations(columns.left.map { it.id }, size).forEach { left ->
                combinations(columns.right.map { it.id }, size).forEach { right ->
                    permutations(right).forEach { selected -> add(left.zip(selected) { l, r -> MatchingPair(l, r) }) }
                }
            }
        }.also { check(it.size == VALID_PARTIAL_BIJECTION_COUNT) }
    }

    private fun <T> combinations(values: List<T>, size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList())
        values.size < size -> emptyList()
        else -> values.indices.flatMap { index ->
            combinations(values.drop(index + 1), size - 1).map { listOf(values[index]) + it }
        }
    }

    private fun <T> permutations(values: List<T>): List<List<T>> =
        if (values.isEmpty()) listOf(emptyList()) else values.indices.flatMap { index ->
            permutations(values.filterIndexed { other, _ -> other != index }).map { listOf(values[index]) + it }
        }

    private fun malformedMatchingAnswers(exercise: GeneratedExercise): List<Answer> {
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        val left = columns.left.map { it.id }
        val right = columns.right.map { it.id }
        return listOf(
            Answer.Matching(listOf(MatchingPair(left[0], right[0]), MatchingPair(left[0], right[1]))),
            Answer.Matching(listOf(MatchingPair(left[0], right[0]), MatchingPair(left[1], right[0]))),
            Answer.Matching(listOf(MatchingPair("unknown-left", right[0]))),
            Answer.Matching(listOf(MatchingPair(left[0], "unknown-right"))),
            Answer.Matching((0..4).map { MatchingPair(left[it % 4], right[it % 4]) }),
            Answer.Text(" rå\u00a0matching e\u0301 "), Answer.Order(listOf(left[0], right[0])),
        )
    }

    private fun writingEvaluationCases(): List<WritingCase> {
        val newspaper = writingExercise("Hun læser en avis.")
        val pluralYou = writingExercise("I læser et brev.")
        val book = writingExercise("Hun læser en bog.")
        return listOf(
            " Hun  læser en avis. ", "hun læser en avis.", "Hun læser en avis", "En avis læser hun.",
            "Han læser en avis.", "Hun har en avis.", "Hun læser et avis.", "Hun læse en avis.",
            "Hun studerer en avis.", "Hun læser en avis. Han sover.", "Hun læser en avis; Han sover",
            "Hun læser en avis: Han sover", "Hun læser en avis. ekstra", "Hun læse et avis.",
        ).map { WritingCase(newspaper.reproduction.index, newspaper, it) } +
            listOf(" i  læser et brev. ", "i læse et brev.", "i læser en brev.")
                .map { WritingCase(pluralYou.reproduction.index, pluralYou, it) } +
            WritingCase(book.reproduction.index, book, "Hun læser en bo\u0301g.")
    }

    private fun writingExercise(canonical: String): GeneratedExercise = (0 until 99).asSequence()
        .map { generate(WRITING_FORMAT_ID, "G005", FIXED_SEED, it) }
        .single { it.inspector.canonicalSentence == canonical }

    private fun orderForRoles(exercise: GeneratedExercise, roles: List<String>) =
        roles.map { role -> exercise.source.blockRoles.entries.single { it.value == role }.key }

    private data class FormatSpec(val id: String, val targetSkillId: String?, val cycleSize: Int)
    private data class WritingCase(val index: Int, val exercise: GeneratedExercise, val answer: String)

    private class CanonicalHasher(schema: String) {
        private val digest = MessageDigest.getInstance("SHA-256")
        init { token(schema) }
        fun field(name: String, value: String) {
            token(name)
            token("value")
            token(value)
        }
        fun nullable(name: String, value: String?) {
            token(name)
            token(if (value == null) "null" else "value")
            value?.let(::token)
        }
        fun strings(name: String, values: List<String>) {
            field("$name.count", values.size.toString())
            values.forEachIndexed { index, value -> field("$name[$index]", value) }
        }
        fun finish(): String = digest.digest().toHex()
        private fun token(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(bytes.size).array())
            digest.update(bytes)
        }
    }

    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex() = joinToString("") { byte ->
        val value = byte.toInt() and 0xff
        "${HEX_DIGITS[value ushr 4]}${HEX_DIGITS[value and 0x0f]}"
    }

    const val ORDER_SEPARATOR = "\u001f"
    const val PAIR_SEPARATOR = "\u001e"
    const val ID_SEPARATOR = "\u001d"
    private const val BASELINE_COMMIT = "81388557402af7c562f23284bf9c9fb89480d573"
    private const val CONTENT_REVISION = "pilot-7"
    private const val GENERATOR_REVISION = "generator-7"
    private const val EVALUATION_REVISION = "evaluation-7"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val MATCHING_SAMPLE_SIZE = 32
    private const val VALID_PARTIAL_BIJECTION_COUNT = 209
    private const val GAP_CYCLE_SIZE = 92
    private const val GAP_ORDERED_ANSWER_COUNT = 13
    private const val NULL_COLUMN = "-"
    private const val HEX_DIGITS = "0123456789abcdef"
    private const val WRITING_FORMAT_ID = "E009"
    private const val MATCHING_FORMAT_ID = "E014"
    private const val GAP_FORMAT_ID = "E015"
    private const val GAP_TARGET = "G004"
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    private val MATCHING_BOUNDARY_INDICES = listOf(0, 1, MAX_INDEX)
    private val GAP_BOUNDARY_INDICES = listOf(0, 1, GAP_CYCLE_SIZE - 1, GAP_CYCLE_SIZE, MAX_INDEX)
}
