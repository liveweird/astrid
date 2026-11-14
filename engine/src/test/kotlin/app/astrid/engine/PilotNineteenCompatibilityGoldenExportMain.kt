package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object PilotNineteenCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: PilotNineteenCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>" }
        PilotNineteenCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal fun publishedPairNineteenReviewBytes(): ByteArray {
    require(PilotContent.pilotNineteenSnapshot.contentRevision == "pilot-19")
    require(PilotContent.pilotNineteenSnapshot.generatorRevision == "generator-19")
    return PilotReview.renderPilotNineteenMarkdown().toByteArray(Charsets.UTF_8)
}

internal object PilotNineteenCompatibilityGolden {
    private val engine = PilotEngine()
    private val formats = (1..44).map { number ->
        val id = "E${number.toString().padStart(3, '0')}"
        Format(id, engine.catalog().formats.single { it.id == id }.targetSkillId, cycleSize(id))
    }

    fun rows(): List<String> {
        require(engine.evaluationRevision(CONTENT_REVISION, GENERATOR_REVISION) == EVALUATION_REVISION)
        require(PilotContent.pilotNineteenSnapshot.contentRevision == CONTENT_REVISION)
        require(PilotContent.pilotNineteenSnapshot.generatorRevision == GENERATOR_REVISION)
        return buildList {
            formats.forEach { format ->
                repeat(format.cycleSize) { index ->
                    val exercise = generate(format, FIXED_SEED, index)
                    add(generationRow(exercise))
                    answers(exercise).forEach { answer -> add(evaluationRow(exercise, answer)) }
                }
                BOUNDARY_SEEDS.forEach { seed ->
                    listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).forEach { index ->
                        add(generationRow(generate(format, seed, index)))
                    }
                }
            }
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-19 compatibility evidence"
        }
        val rows = rows()
        val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val review = publishedPairNineteenReviewBytes()
        Files.writeString(
            provenancePath,
            buildString {
                appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
                appendLine("releaseVersion=0.22.0")
                appendLine("captureDate=$CAPTURE_DATE")
                appendLine("projectionSchema=pair19-engine-v1")
                appendLine("contentRevision=$CONTENT_REVISION")
                appendLine("generatorRevision=$GENERATOR_REVISION")
                appendLine("evaluationRevision=$EVALUATION_REVISION")
                appendLine("reviewState=compatibility-only-not-linguistic-approval")
                appendLine("rowCount=${rows.size}")
                appendLine("fixtureSha256=${bytes.sha256()}")
                appendLine("reviewSha256=${review.sha256()}")
                appendLine(
                    "description=All forty-four pair-nineteen format cycles and seed/index boundaries; all P2 tasks, " +
                        "scalar public, inspector, private task and answer-key inputs, complete choice and tile cases, " +
                        "partial matching bijections, raw grading shapes and exact current review bytes.",
                )
            },
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }

    fun generationHash(exercise: GeneratedExercise) = FreezeHasher("pilot-19-generation-v1").apply {
        field("pair18Projection", PilotEighteenCompatibilityGolden.generationHash(exercise))
        val source = exercise.source as? VocabularyP2ExerciseSource
        nullable("p2Source.presence", source?.let { "present" })
        source?.let {
            task("p2Source.task", it.task)
            answerKey("p2Source.answerKey", it.answerKey)
            stringMap("p2Source.blockRoles", it.blockRoles)
        }
        val details = exercise.inspector.vocabularySource
        nullable("chronology.presence", details?.let { "present" })
        details?.let {
            integer("chronology.count", it.chronologyRelations.size)
            it.chronologyRelations.forEachIndexed { index, relation ->
                field("chronology[$index].id", relation.relationId)
                field("chronology[$index].beforeFactId", relation.beforeFactId)
                field("chronology[$index].afterFactId", relation.afterFactId)
            }
            nullable("targetChronologyRelationId", it.targetChronologyRelationId)
            it.supportFacts.forEachIndexed { index, fact ->
                nullable("supportFacts[$index].referentDisplay", fact.referentDisplay)
                nullable("supportFacts[$index].predicateSource", fact.predicateSource?.scalar())
                nullable("supportFacts[$index].objectSource", fact.objectSource?.scalar())
                nullable("supportFacts[$index].quantitySource", fact.quantitySource?.scalar())
                nullable("supportFacts[$index].translation", fact.translation)
            }
        }
    }.finish()

    fun evaluationHash(evaluation: Evaluation) = FreezeHasher("pilot-19-evaluation-v1").apply {
        field("pair18Projection", PilotEighteenCompatibilityGolden.evaluationHash(evaluation))
    }.finish()

    fun answers(exercise: GeneratedExercise): List<Answer> {
        val source = exercise.source
        if (source !is VocabularyP2ExerciseSource) return listOf(canonicalAnswer(exercise))
        val task = source.task
        return when (task.kind) {
            VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT, VocabularyP2TaskKind.READING ->
                (task.choices.map { Answer.Text(it) } + malformed()).distinct()
            VocabularyP2TaskKind.MATCHING -> matchingAnswers(exercise)
            VocabularyP2TaskKind.TILES -> orderAnswers(exercise)
        }
    }

    fun canonicalAnswer(exercise: GeneratedExercise): Answer = when {
        exercise.formatId == "E009" -> Answer.Text(requireNotNull(exercise.inspector.canonicalSentence))
        else -> canonicalP2Answer(exercise) ?: canonicalKeyAnswer(exercise)
    }

    private fun canonicalP2Answer(exercise: GeneratedExercise): Answer? =
        when ((exercise.source as? VocabularyP2ExerciseSource)?.task?.kind) {
            VocabularyP2TaskKind.TILES -> Answer.Order(
                requireNotNull(exercise.inspector.vocabularySource).acceptedOrders.single(),
            )
            VocabularyP2TaskKind.MATCHING -> Answer.Matching(
                requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map { MatchingPair(it.leftId, it.rightId) },
            )
            else -> null
        }

    private fun canonicalKeyAnswer(exercise: GeneratedExercise): Answer = when (val key = exercise.source.answerKey) {
        is AnswerKey.PresentForm -> Answer.Text(key.present)
        is AnswerKey.Article -> Answer.Text(key.expected)
        is AnswerKey.SubjectFirstOrder -> Answer.Order(
            key.canonicalRoles.map { role -> exercise.source.blockRoles.entries.single { it.value == role }.key },
        )
        is AnswerKey.MeaningChoice -> Answer.Text(key.expected)
        is AnswerKey.Headword -> Answer.Text(key.expected.headword)
        is AnswerKey.SuppliedChoice -> Answer.Text(key.expected)
        is AnswerKey.TileOrder -> Answer.Order(tileIds(key.canonical, exercise))
        is AnswerKey.Matching -> Answer.Matching(
            exercise.inspector.vocabularySource?.matchingPairs?.map { MatchingPair(it.leftId, it.rightId) }
                ?: requireNotNull(exercise.inspector.matchingSource).pairs.map { MatchingPair(it.leftId, it.rightId) },
        )
        is AnswerKey.Gaps -> Answer.Gaps(
            requireNotNull(exercise.inspector.gapSource).slots.map { GapSelection(it.slotId, it.expectedChoiceId) },
        )
        is AnswerKey.ModuleOneForm -> Answer.Text(key.expected)
    }

    private fun FreezeHasher.task(prefix: String, task: VocabularyP2Task) {
        field("$prefix.id", task.id); field("$prefix.formatId", task.formatId); field("$prefix.kind", task.kind.name)
        field("$prefix.cue", task.cue); strings("$prefix.choices", task.choices); field("$prefix.expected", task.expected)
        integer("$prefix.targets.count", task.targets.size)
        task.targets.forEachIndexed { index, value -> field("$prefix.targets[$index]", value.scalar()) }
        integer("$prefix.matching.count", task.matching.size)
        task.matching.forEachIndexed { index, value ->
            field("$prefix.matching[$index].label", value.first); field("$prefix.matching[$index].lexeme", value.second.scalar())
        }
        strings("$prefix.passage", task.passage)
        integer("$prefix.facts.count", task.facts.size)
        task.facts.forEachIndexed { index, fact ->
            val name = "$prefix.facts[$index]"
            field("$name.id", fact.id); field("$name.referentId", fact.referentId)
            field("$name.referentDisplay", fact.referentDisplay); field("$name.referentAnswer", fact.referentAnswer)
            field("$name.predicate", fact.predicate.scalar()); nullable("$name.objectSource", fact.objectSource?.scalar())
            nullable("$name.quantitySource", fact.quantitySource?.scalar()); field("$name.sentence", fact.sentence)
            field("$name.translation", fact.translation); strings("$name.exposureKeys", fact.exposureKeys)
        }
        nullable("$prefix.targetFactId", task.targetFactId); nullable("$prefix.questionRole", task.questionRole)
        integer("$prefix.chronology.count", task.chronology.size)
        task.chronology.forEachIndexed { index, relation ->
            field("$prefix.chronology[$index].id", relation.id)
            field("$prefix.chronology[$index].beforeFactId", relation.beforeFactId)
            field("$prefix.chronology[$index].afterFactId", relation.afterFactId)
        }
        nullable("$prefix.targetChronologyId", task.targetChronologyId)
        integer("$prefix.tiles.count", task.tiles.size)
        task.tiles.forEachIndexed { index, tile ->
            field("$prefix.tiles[$index].text", tile.text); nullable("$prefix.tiles[$index].lexical", tile.lexical?.scalar())
            nullable("$prefix.tiles[$index].quantity", tile.quantity?.scalar())
        }
        nullable("$prefix.targetScopeId", task.targetScopeId); boolean("$prefix.fixedCommaBeforeLast", task.fixedCommaBeforeLast)
    }

    private fun VocabularyP2Lexeme.scalar() = listOf(
        entryId, senseId, kind.wireValue, forms.infinitive.orEmpty(), forms.present.orEmpty(), forms.bundle.orEmpty(),
        forms.form.orEmpty(), gloss, selectedUse,
    ).joinToString("\u001f")

    private fun VocabularyLexicalSourceDetails.scalar() = listOf(
        entryId, senseId, kind.wireValue, forms.infinitive.orEmpty(), forms.present.orEmpty(), forms.bundle.orEmpty(),
        forms.form.orEmpty(), gloss, selectedUse,
    ).joinToString("\u001f")

    private fun VocabularyP2Quantity.scalar() = listOf(constructionId, display, head.scalar(), beverage.scalar()).joinToString("\u001e")
    private fun VocabularyQuantitySourceDetails.scalar() = listOf(
        constructionId, display, headSource.scalar(), componentSources.joinToString("\u001d") { it.scalar() },
    ).joinToString("\u001e")

    private fun matchingAnswers(exercise: GeneratedExercise): List<Answer> {
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        val left = columns.left.map { it.id }; val right = columns.right.map { it.id }
        val valid = (0..left.size).flatMap { size ->
            left.combinations(size).flatMap { selectedLeft ->
                right.combinations(size).flatMap { selectedRight ->
                    selectedRight.permutations().map { selectedLeft.zip(it, ::MatchingPair) }
                }
            }
        }.map { Answer.Matching(it) }
        return valid + listOf(
            Answer.Matching(listOf(MatchingPair(left.first(), "unknown"))),
            Answer.Matching(listOf(MatchingPair("unknown", right.first()))),
            Answer.Text("not matching"), Answer.Order(emptyList()), Answer.Gaps(emptyList()),
        )
    }

    private fun orderAnswers(exercise: GeneratedExercise): List<Answer> {
        val ids = exercise.prompt.blocks.map { it.id }
        return ids.permutations().map { Answer.Order(it) } + listOf(
            Answer.Order(ids.dropLast(1)), Answer.Order(emptyList()), Answer.Order(ids + ids.first()),
            Answer.Order(listOf("unknown")), Answer.Text("not order"), Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
        )
    }

    private fun malformed() = listOf(
        Answer.Text("outside supplied bank"), Answer.Text(""), Answer.Order(emptyList()),
        Answer.Matching(emptyList()), Answer.Gaps(emptyList()),
    )

    private fun tileIds(identities: List<TileIdentity>, exercise: GeneratedExercise): List<String> {
        val remaining = requireNotNull(exercise.inspector.tileSource).tokens.toMutableList()
        return identities.map { identity ->
            val index = remaining.indexOfFirst { it.lexicalId == identity.lexicalId && it.form == identity.form }
            require(index >= 0)
            remaining.removeAt(index).blockId
        }
    }

    private fun generate(format: Format, seed: Int, index: Int) = engine.generate(
        GenerationRequest(format.id, format.skillId, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
    )

    private fun generationRow(exercise: GeneratedExercise) = listOf(
        "G", exercise.formatId, exercise.targetSkillId ?: "-", exercise.reproduction.seed, exercise.reproduction.index,
        generationHash(exercise),
    ).joinToString("\t")

    private fun evaluationRow(exercise: GeneratedExercise, answer: Answer) = listOf(
        "E", exercise.formatId, exercise.targetSkillId ?: "-", exercise.reproduction.seed, exercise.reproduction.index,
        answer.scalar(), evaluationHash(engine.evaluate(exercise, answer)),
    ).joinToString("\t")

    private fun Answer.scalar() = when (this) {
        is Answer.Text -> "T:$text"
        is Answer.Order -> "O:${blockIds.joinToString("\u001f")}"
        is Answer.Matching -> "M:${pairs.joinToString("\u001f") { "${it.leftId}\u001d${it.rightId}" }}"
        is Answer.Gaps -> "P:${selections.joinToString("\u001f") { "${it.slotId}\u001d${it.choiceId}" }}"
    }

    private fun cycleSize(id: String): Int = when {
        id in (1..30).map { "E${it.toString().padStart(3, '0')}" } -> inheritedCycleSize(id)
        id in (31..39).map { "E${it.toString().padStart(3, '0')}" } -> VocabularyP1Content.tasks(id).size
        else -> VocabularyP2Content.tasks(id).size
    }

    private fun inheritedCycleSize(id: String) = when (id) {
        "E001", "E003", "E004", "E005", "E009", "E012", "E013" -> 99
        "E002", "E006", "E015" -> 92
        "E007", "E008" -> 3; "E010" -> 24; "E011" -> 4; "E014" -> 32
        "E016", "E017" -> 7; "E018", "E019", "E022", "E023" -> 8
        "E020", "E021", "E026", "E027" -> 2; "E024", "E028" -> 3; "E025" -> 6
        "E029", "E030" -> 4; else -> error("Unknown inherited format $id")
    }

    private fun <T> List<T>.permutations(): List<List<T>> = if (size <= 1) listOf(this) else indices.flatMap { index ->
        (take(index) + drop(index + 1)).permutations().map { listOf(this[index]) + it }
    }
    private fun <T> List<T>.combinations(size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList()); size > this.size -> emptyList()
        else -> indices.flatMap { index -> drop(index + 1).combinations(size - 1).map { listOf(this[index]) + it } }
    }
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private data class Format(val id: String, val skillId: String?, val cycleSize: Int)
    private const val RELEASE_SOURCE_COMMIT = "844b88af33cc729e095d443adb72fcd72cd17534"
    private const val CAPTURE_DATE = "2026-11-13"
    private const val CONTENT_REVISION = "pilot-19"
    private const val GENERATOR_REVISION = "generator-19"
    private const val EVALUATION_REVISION = "evaluation-19"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
}
