package app.astrid.engine

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object PilotSixCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotSixCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotSixCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object PilotSixCompatibilityGolden {
    private val engine = PilotEngine()
    private val formats = listOf(
        FormatSpec("E001", "G004", 99),
        FormatSpec("E002", "G006", 92),
        FormatSpec("E003", "G005", 99),
        FormatSpec("E004", "G004", 99),
        FormatSpec("E005", "G004", 99),
        FormatSpec("E006", "G006", 92),
        FormatSpec("E007", null, 3),
        FormatSpec("E008", null, 3),
        FormatSpec("E009", "G005", 99),
        FormatSpec("E010", null, 24),
        FormatSpec("E011", null, 4),
        FormatSpec("E012", "G005", 99),
        FormatSpec("E013", "G004", 99),
    )

    fun fixtureBytes(): ByteArray = buildList {
        formats.forEach { format ->
            repeat(format.cycleSize) { index ->
                val exercise = generate(format.id, format.targetSkillId, FIXED_SEED, index)
                add(generationRow(format.id, format.targetSkillId, FIXED_SEED, index, exercise))
                add(evaluationRow(format.id, format.targetSkillId, FIXED_SEED, index, exercise, correctAnswer(exercise)))
            }
        }
        formats.forEach { format ->
            BOUNDARY_SEEDS.forEach { seed ->
                listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).forEach { index ->
                    val exercise = generate(format.id, format.targetSkillId, seed, index)
                    add(generationRow(format.id, format.targetSkillId, seed, index, exercise))
                }
            }
        }
        formats.filterNot { it.id == WRITING_FORMAT_ID }.forEach { format ->
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
                val exercise = generate(MATCHING_FORMAT_ID, null, seed, index)
                add(generationRow(MATCHING_FORMAT_ID, null, seed, index, exercise))
            }
        }
        val matching = generate(MATCHING_FORMAT_ID, null, FIXED_SEED, 0)
        validPartialBijections(matching).forEach { pairs ->
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, 0, matching, Answer.Matching(pairs)))
        }
        val correctPairs = requireNotNull(matching.inspector.matchingSource).pairs.map { MatchingPair(it.leftId, it.rightId) }
        listOf(correctPairs.reversed(), correctPairs.drop(1) + correctPairs.first()).forEach { reordered ->
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, 0, matching, Answer.Matching(reordered)))
        }
        malformedMatchingAnswers(matching).forEach { answer ->
            add(evaluationRow(MATCHING_FORMAT_ID, null, FIXED_SEED, 0, matching, answer))
        }
    }.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-6 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val rowCount = bytes.count { it == '\n'.code.toByte() }
        val reviewBytes = PilotReview.renderMarkdown().toByteArray(Charsets.UTF_8)
        val description = "All thirteen inherited complete cycles and boundaries plus bounded E014 generation, " +
            "all 209 valid partial bijections, reordered raw pairs, malformed selections, wrong answer kinds, " +
            "matching facets, and framed hashes."
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

    fun generationHash(exercise: GeneratedExercise): String = CanonicalHasher("pilot-6-generation-v1").apply {
        field("id", exercise.id)
        field("formatId", exercise.formatId)
        nullable("targetSkillId", exercise.targetSkillId)
        lexicalTarget("lexicalTarget", exercise.lexicalTarget)
        nullable("lexicalTargets.presence", exercise.lexicalTargets?.let { "present" })
        exercise.lexicalTargets?.let { targets ->
            field("lexicalTargets.count", targets.size.toString())
            targets.forEachIndexed { index, target -> lexicalTarget("lexicalTargets[$index]", target) }
        }
        strings("supportSkillIds", exercise.supportSkillIds)
        field("contentRevision", exercise.contentRevision)
        field("generatorRevision", exercise.generatorRevision)
        field("reproduction.seed", exercise.reproduction.seed.toString())
        field("reproduction.index", exercise.reproduction.index.toString())
        field("presentation.instruction", exercise.instruction)
        field("presentation.context", exercise.context)
        field("prompt.before", exercise.prompt.before)
        field("prompt.after", exercise.prompt.after)
        strings("prompt.choices", exercise.prompt.choices)
        field("prompt.blocks.count", exercise.prompt.blocks.size.toString())
        exercise.prompt.blocks.forEachIndexed { index, block ->
            field("prompt.blocks[$index].id", block.id)
            field("prompt.blocks[$index].text", block.text)
        }
        field("prompt.punctuation", exercise.prompt.punctuation)
        nullable("prompt.inputLabel", exercise.prompt.inputLabel)
        nullable("prompt.replacementWord", exercise.prompt.replacementWord)
        nullable("prompt.cue", exercise.prompt.cue)
        nullable("prompt.cueLanguage", exercise.prompt.cueLanguage)
        nullable("prompt.answerLanguage", exercise.prompt.answerLanguage)
        nullableStrings("prompt.passage", exercise.prompt.passage)
        nullable("prompt.tileMode", exercise.prompt.tileMode)
        nullable("prompt.requiredTileCount", exercise.prompt.requiredTileCount?.toString())
        matchingColumns(exercise.prompt.matchingColumns)
        field("introduction.count", exercise.introduction.size.toString())
        exercise.introduction.forEachIndexed { index, section ->
            field("introduction[$index].title", section.title)
            field("introduction[$index].body", section.body)
            nullable("introduction[$index].bodyLanguage", section.bodyLanguage)
        }
        field("hint", exercise.hint)
        nullable("readingTarget.facet", exercise.readingTarget?.facet)
        nullable("inspector.canonicalSentenceId", exercise.inspector.canonicalSentenceId)
        nullable("inspector.constructionId", exercise.inspector.constructionId)
        strings("inspector.lexicalIds", exercise.inspector.lexicalIds)
        strings("inspector.clauseRoles", exercise.inspector.clauseRoles)
        nullable("inspector.canonicalSentence", exercise.inspector.canonicalSentence)
        lexicalSource("inspector.lexicalSource", exercise.inspector.lexicalSource)
        readingSource(exercise.inspector.readingSource)
        tileSource(exercise.inspector.tileSource)
        matchingSource(exercise.inspector.matchingSource)
    }.finish()

    fun evaluationHash(evaluation: Evaluation): String = CanonicalHasher("pilot-6-evaluation-v1").apply {
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
        evaluation.matchingResults?.let { results ->
            field("matchingResults.count", results.size.toString())
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

    private fun CanonicalHasher.lexicalTarget(name: String, target: LexicalTarget?) {
        nullable("$name.entryId", target?.entryId)
        nullable("$name.senseId", target?.senseId)
        nullable("$name.facet", target?.facet)
        nullable("$name.direction", target?.direction)
    }

    private fun CanonicalHasher.lexicalSource(name: String, source: LexicalSourceDetails?) {
        nullable("$name.entryId", source?.entryId)
        nullable("$name.senseId", source?.senseId)
        nullable("$name.headword", source?.headword)
        nullable("$name.gloss", source?.gloss)
        nullable("$name.article", source?.article)
    }

    private fun CanonicalHasher.matchingColumns(columns: MatchingColumns?) {
        nullable("prompt.matchingColumns.presence", columns?.let { "present" })
        columns?.let {
            matchingItems("prompt.matchingColumns.left", it.left)
            matchingItems("prompt.matchingColumns.right", it.right)
        }
    }

    private fun CanonicalHasher.matchingItems(name: String, items: List<MatchingItem>) {
        field("$name.count", items.size.toString())
        items.forEachIndexed { index, item ->
            field("$name[$index].id", item.id)
            field("$name[$index].text", item.text)
        }
    }

    private fun CanonicalHasher.matchingSource(source: MatchingSourceDetails?) {
        nullable("inspector.matchingSource.presence", source?.let { "present" })
        source?.let {
            field("inspector.matchingSource.pairs.count", it.pairs.size.toString())
            it.pairs.forEachIndexed { index, pair ->
                field("inspector.matchingSource.pairs[$index].leftId", pair.leftId)
                field("inspector.matchingSource.pairs[$index].rightId", pair.rightId)
                lexicalSource("inspector.matchingSource.pairs[$index].lexicalSource", pair.lexicalSource)
            }
        }
    }

    private fun CanonicalHasher.readingSource(source: ReadingSourceDetails?) {
        nullable("inspector.readingSource.presence", source?.let { "present" })
        nullable("inspector.readingSource.questionSubjectId", source?.questionSubjectId)
        nullable("inspector.readingSource.questionVerbId", source?.questionVerbId)
        nullable("inspector.readingSource.answerNounId", source?.answerNounId)
        nullable("inspector.readingSource.answer", source?.answer)
        field("inspector.readingSource.propositions.count", source?.propositions?.size?.toString() ?: "0")
        source?.propositions?.forEachIndexed { index, proposition ->
            field("inspector.readingSource.propositions[$index].sentenceId", proposition.sentenceId)
            field("inspector.readingSource.propositions[$index].subjectId", proposition.subjectId)
            field("inspector.readingSource.propositions[$index].verbId", proposition.verbId)
            field("inspector.readingSource.propositions[$index].objectId", proposition.objectId)
        }
    }

    private fun CanonicalHasher.tileSource(source: TileSourceDetails?) {
        nullable("inspector.tileSource.presence", source?.let { "present" })
        field("inspector.tileSource.tokens.count", source?.tokens?.size?.toString() ?: "0")
        source?.tokens?.forEachIndexed { index, token ->
            field("inspector.tileSource.tokens[$index].blockId", token.blockId)
            field("inspector.tileSource.tokens[$index].lexicalId", token.lexicalId)
            field("inspector.tileSource.tokens[$index].form", token.form)
        }
        field("inspector.tileSource.licensedOrders.count", source?.licensedOrders?.size?.toString() ?: "0")
        source?.licensedOrders?.forEachIndexed { index, order ->
            field("inspector.tileSource.licensedOrders[$index].kind", order.kind)
            strings("inspector.tileSource.licensedOrders[$index].blockIds", order.blockIds)
        }
        field("inspector.tileSource.distractors.count", source?.distractors?.size?.toString() ?: "0")
        source?.distractors?.forEachIndexed { index, distractor ->
            field("inspector.tileSource.distractors[$index].blockId", distractor.blockId)
            field("inspector.tileSource.distractors[$index].rationale", distractor.rationale)
        }
    }

    private fun CanonicalHasher.answer(name: String, answer: Answer) {
        when (answer) {
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
        }
    }

    private fun CanonicalHasher.normalizedAnswer(answer: NormalizedAnswer) {
        when (answer) {
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
        }
    }

    private fun CanonicalHasher.matchingPairs(name: String, pairs: List<MatchingPair>) {
        field("$name.count", pairs.size.toString())
        pairs.forEachIndexed { index, pair ->
            field("$name[$index].leftId", pair.leftId)
            field("$name[$index].rightId", pair.rightId)
        }
    }

    private fun generate(format: String, target: String?, seed: Int, index: Int) =
        engine.generate(GenerationRequest(format, target, seed, index, CONTENT_REVISION, GENERATOR_REVISION))

    private fun generationRow(format: String, target: String?, seed: Int, index: Int, exercise: GeneratedExercise) =
        listOf("G", format, target ?: NULL_COLUMN, seed, index, generationHash(exercise)).joinToString("\t")

    private fun evaluationRow(
        format: String,
        target: String?,
        seed: Int,
        index: Int,
        exercise: GeneratedExercise,
        answer: Answer,
    ): String {
        val kind: String
        val raw: String
        when (answer) {
            is Answer.Text -> {
                kind = "T"
                raw = answer.text
            }
            is Answer.Order -> {
                kind = "O"
                raw = answer.blockIds.joinToString(ORDER_SEPARATOR)
            }
            is Answer.Matching -> {
                kind = "M"
                raw = answer.pairs.joinToString(PAIR_SEPARATOR) { "${it.leftId}$ID_SEPARATOR${it.rightId}" }
            }
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
            Answer.Text(key.suppliedChoices.first { it != key.expected }), Answer.Text("journal"),
            Answer.Text("e\u0301"), Answer.Order(emptyList()),
        )
        is AnswerKey.Headword -> {
            val other = PilotContent.nouns.first { it.id != key.expected.entryId }
            val wrongArticle = if (key.expected.article == "en") "et" else "en"
            val otherWrongArticle = if (other.gender.article == "en") "et" else "en"
            listOf(Answer.Text(key.expected.headword.uppercase(Locale.ROOT)), Answer.Text(other.text),
                Answer.Text("${key.expected.article} ${key.expected.headword}"),
                Answer.Text("$wrongArticle ${key.expected.headword}"), Answer.Text("${other.gender.article} ${other.text}"),
                Answer.Text("$otherWrongArticle ${other.text}"), Answer.Text("tidende"), Answer.Text("   "),
                Answer.Text("${key.expected.headword}\u00a0${other.text}"), Answer.Text("a\u030Avis"), Answer.Order(emptyList()))
        }
        is AnswerKey.SuppliedChoice -> listOf(
            Answer.Text("  ${key.expected}  "), Answer.Text(key.suppliedChoices.first { it != key.expected }),
            Answer.Text(key.expected.uppercase(Locale.ROOT)), Answer.Text("e\u0301t magasin"), Answer.Order(emptyList()),
        )
        is AnswerKey.TileOrder -> tileAnswers(exercise, key)
        is AnswerKey.Matching -> error("Matching is covered separately")
    }

    private fun specialExercise(format: FormatSpec): GeneratedExercise {
        if (format.id !in setOf("E012", "E013")) return generate(format.id, format.targetSkillId, FIXED_SEED, 0)
        return (0 until format.cycleSize).asSequence()
            .map { generate(format.id, format.targetSkillId, FIXED_SEED, it) }
            .first { (it.source.answerKey as AnswerKey.TileOrder).fronted != null }
    }

    private fun tileAnswers(exercise: GeneratedExercise, key: AnswerKey.TileOrder): List<Answer> {
        val canonical = idsFor(key.canonical, exercise)
        val answers = mutableListOf<Answer>(Answer.Order(canonical))
        key.fronted?.let { answers += Answer.Order(idsFor(it, exercise)) }
        key.infinitive?.let { answers += Answer.Order(idsFor(it, exercise)) }
        answers += Answer.Order(canonical.dropLast(1))
        answers += Answer.Order(canonical + canonical.first())
        answers += Answer.Order(canonical.dropLast(1) + "unknown-block")
        answers += Answer.Order(canonical.reversed())
        answers += Answer.Text(requireNotNull(exercise.inspector.canonicalSentence))
        return answers
    }

    private fun idsFor(identities: List<TileIdentity>, exercise: GeneratedExercise): List<String> {
        val remaining = requireNotNull(exercise.inspector.tileSource).tokens.toMutableList()
        return identities.map { identity ->
            val index = remaining.indexOfFirst { it.lexicalId == identity.lexicalId && it.form == identity.form }
            check(index >= 0)
            remaining.removeAt(index).blockId
        }
    }

    private fun validPartialBijections(exercise: GeneratedExercise): List<List<MatchingPair>> {
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        return buildList {
            for (size in 0..columns.left.size) {
                combinations(columns.left.map { it.id }, size).forEach { left ->
                    combinations(columns.right.map { it.id }, size).forEach { right ->
                        permutations(right).forEach { selected ->
                            add(left.zip(selected) { leftId, rightId -> MatchingPair(leftId, rightId) })
                        }
                    }
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
            Answer.Text(" rå\u00a0matching e\u0301 "),
            Answer.Order(listOf(left[0], right[0])),
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
            if (value != null) token(value)
        }
        fun strings(name: String, values: List<String>) {
            field("$name.count", values.size.toString())
            values.forEachIndexed { index, value -> field("$name[$index]", value) }
        }
        fun nullableStrings(name: String, values: List<String>?) {
            nullable("$name.presence", values?.let { "present" })
            if (values != null) strings(name, values)
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
    private const val BASELINE_COMMIT = "6d1c0fe2f957490216fda50e4a67a216c3b784ed"
    private const val CONTENT_REVISION = "pilot-6"
    private const val GENERATOR_REVISION = "generator-6"
    private const val EVALUATION_REVISION = "evaluation-6"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val MATCHING_SAMPLE_SIZE = 32
    private const val VALID_PARTIAL_BIJECTION_COUNT = 209
    private const val NULL_COLUMN = "-"
    private const val HEX_DIGITS = "0123456789abcdef"
    private const val WRITING_FORMAT_ID = "E009"
    private const val MATCHING_FORMAT_ID = "E014"
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    private val MATCHING_BOUNDARY_INDICES = listOf(0, 1, MAX_INDEX)
}
