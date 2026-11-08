package app.astrid.engine

import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test

object PilotThreeCompatibilityGoldenExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) {
            "Usage: PilotThreeCompatibilityGoldenExportMain <fixture.tsv> <fixture.provenance>"
        }
        PilotThreeCompatibilityGolden.write(Path.of(args[0]), Path.of(args[1]))
    }
}

class PilotThreeCompatibilityGoldenExportTest {
    @Test
    fun `export pilot three compatibility evidence when explicitly requested`() {
        val outputDirectory = System.getenv("ASTRID_PILOT_THREE_GOLDEN_DIR") ?: return
        val directory = Path.of(outputDirectory)
        PilotThreeCompatibilityGolden.write(
            directory.resolve("pilot-3-generator-3-evaluation-3.tsv"),
            directory.resolve("pilot-3-generator-3-evaluation-3.provenance"),
        )
    }
}

internal object PilotThreeCompatibilityGolden {
    private val engine = PilotEngine()

    private val formats =
        listOf(
            FormatSpec("E001", "G004", 99),
            FormatSpec("E002", "G006", 92),
            FormatSpec("E003", "G005", 99),
            FormatSpec("E004", "G004", 99),
            FormatSpec("E005", "G004", 99),
            FormatSpec("E006", "G006", 92),
            FormatSpec("E007", null, 3),
            FormatSpec("E008", null, 3),
        )

    fun fixtureBytes(): ByteArray =
        buildList {
            formats.forEach { format ->
                repeat(format.cycleSize) { index ->
                    val exercise = generate(format, FIXED_SEED, index)
                    add(generationRow(format, FIXED_SEED, index, exercise))
                    add(evaluationRow(format, FIXED_SEED, index, exercise, correctAnswer(exercise)))
                }
            }
            formats.forEach { format ->
                BOUNDARY_SEEDS.forEach { seed ->
                    listOf(0, format.cycleSize - 1, format.cycleSize, MAX_INDEX).forEach { index ->
                        add(generationRow(format, seed, index, generate(format, seed, index)))
                    }
                }
            }
            formats.forEach { format ->
                val exercise = generate(format, FIXED_SEED, 0)
                specialAnswers(exercise).forEach { answer ->
                    add(evaluationRow(format, FIXED_SEED, 0, exercise, answer))
                }
            }
        }.joinToString(separator = "\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(
        fixturePath: Path,
        provenancePath: Path,
    ) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-3 compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val rowCount = bytes.count { it == '\n'.code.toByte() }
        val provenance =
            """
            baselineCommit=1d695c5
            codeEquivalentCommit=e3c7556
            captureDate=2026-11-07
            reviewState=compatibility-only-not-linguistic-approval
            contentRevision=pilot-3
            generatorRevision=generator-3
            evaluationRevision=evaluation-3
            rowCount=$rowCount
            fixtureSha256=${bytes.sha256()}
            description=Eight cycles, boundaries, raw and Unicode inputs, ordering, lexical facets and byte-framed UTF-8 hashes.
            """.trimIndent() + "\n"
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
        println("Wrote $rowCount rows to $fixturePath (${bytes.sha256()})")
        println("Wrote provenance to $provenancePath")
    }

    fun generationHash(exercise: GeneratedExercise): String =
        CanonicalHasher("pilot-3-generation-v1").apply {
            field("id", exercise.id)
            field("formatId", exercise.formatId)
            nullable("targetSkillId", exercise.targetSkillId)
            exercise.lexicalTarget.let { target ->
                nullable("lexicalTarget.entryId", target?.entryId)
                nullable("lexicalTarget.senseId", target?.senseId)
                nullable("lexicalTarget.facet", target?.facet)
                nullable("lexicalTarget.direction", target?.direction)
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
            field("introduction.count", exercise.introduction.size.toString())
            exercise.introduction.forEachIndexed { index, section ->
                field("introduction[$index].title", section.title)
                field("introduction[$index].body", section.body)
            }
            field("hint", exercise.hint)
            nullable("inspector.canonicalSentenceId", exercise.inspector.canonicalSentenceId)
            nullable("inspector.constructionId", exercise.inspector.constructionId)
            strings("inspector.lexicalIds", exercise.inspector.lexicalIds)
            strings("inspector.clauseRoles", exercise.inspector.clauseRoles)
            nullable("inspector.canonicalSentence", exercise.inspector.canonicalSentence)
            exercise.inspector.lexicalSource.let { source ->
                nullable("inspector.lexicalSource.entryId", source?.entryId)
                nullable("inspector.lexicalSource.senseId", source?.senseId)
                nullable("inspector.lexicalSource.headword", source?.headword)
                nullable("inspector.lexicalSource.gloss", source?.gloss)
                nullable("inspector.lexicalSource.article", source?.article)
            }
        }.finish()

    fun evaluationHash(evaluation: Evaluation): String =
        CanonicalHasher("pilot-3-evaluation-v1").apply {
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
            when (val answer = evaluation.normalizedAnswer) {
                is NormalizedAnswer.Text -> {
                    field("normalizedAnswer.type", "text")
                    field("normalizedAnswer.text", answer.text)
                }
                is NormalizedAnswer.Order -> {
                    field("normalizedAnswer.type", "order")
                    strings("normalizedAnswer.blockIds", answer.blockIds)
                }
                is NormalizedAnswer.Matching -> error("Matching answer is unreachable in pilot three")
                is NormalizedAnswer.Gaps -> error("Gap answer is unreachable in pilot three")
            }
            nullable("linguisticScope", evaluation.linguisticScope?.wireValue)
            nullable("responseConstraintStatus", evaluation.responseConstraintStatus?.wireValue)
        }.finish()

    private fun CanonicalHasher.answer(
        name: String,
        answer: Answer,
    ) {
        when (answer) {
            is Answer.Text -> {
                field("$name.type", "text")
                field("$name.text", answer.text)
            }
            is Answer.Order -> {
                field("$name.type", "order")
                strings("$name.blockIds", answer.blockIds)
            }
            is Answer.Matching -> error("Matching answer is unreachable in pilot three")
            is Answer.Gaps -> error("Gap answer is unreachable in pilot three")
        }
    }

    private fun generate(
        format: FormatSpec,
        seed: Int,
        index: Int,
    ) = engine.generate(
        GenerationRequest(format.id, format.targetSkillId, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
    )

    private fun generationRow(
        format: FormatSpec,
        seed: Int,
        index: Int,
        exercise: GeneratedExercise,
    ) = listOf("G", format.id, format.targetSkillId ?: NULL_COLUMN, seed, index, generationHash(exercise)).joinToString("\t")

    private fun evaluationRow(
        format: FormatSpec,
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
            is Answer.Matching -> error("Matching answer is unreachable in pilot three")
            is Answer.Gaps -> error("Gap answer is unreachable in pilot three")
        }
        return listOf(
            kind,
            format.id,
            format.targetSkillId ?: NULL_COLUMN,
            seed,
            index,
            Base64.getEncoder().encodeToString(raw.toByteArray(Charsets.UTF_8)),
            evaluationHash(engine.evaluate(exercise, answer)),
        ).joinToString("\t")
    }

    private fun correctAnswer(exercise: GeneratedExercise): Answer =
        when (val key = exercise.source.answerKey) {
            is AnswerKey.PresentForm -> Answer.Text(key.present)
            is AnswerKey.Article -> Answer.Text(key.expected)
            is AnswerKey.SubjectFirstOrder -> Answer.Order(orderForRoles(exercise, key.canonicalRoles))
            is AnswerKey.MeaningChoice -> Answer.Text(key.expected)
            is AnswerKey.Headword -> Answer.Text(key.expected.headword)
            is AnswerKey.Matching -> error("Matching key is unreachable in pilot three")
            else -> error("Newer answer key is unreachable in the frozen pilot-three exporter")
        }

    private fun specialAnswers(exercise: GeneratedExercise): List<Answer> =
        when (val key = exercise.source.answerKey) {
            is AnswerKey.PresentForm ->
                listOf(
                    Answer.Text(" ${key.present} "),
                    Answer.Text(key.infinitive),
                    Answer.Text(PilotContent.verbs.first { it.present != key.present }.present),
                    Answer.Text("laeser"),
                    Answer.Text("e\u0301t"),
                    Answer.Order(emptyList()),
                )
            is AnswerKey.Article -> {
                val other = if (key.expected == "en") "et" else "en"
                val numeral = if (key.expected == "en") "én" else "ét"
                val otherNumeral = if (key.expected == "en") "ét" else "én"
                listOf(
                    Answer.Text(numeral),
                    Answer.Text(other),
                    Answer.Text(otherNumeral),
                    Answer.Text("x"),
                    Answer.Text(" ${key.expected} "),
                    Answer.Order(emptyList()),
                )
            }
            is AnswerKey.SubjectFirstOrder -> {
                val correct = orderForRoles(exercise, key.canonicalRoles)
                listOf(
                    Answer.Order(correct.reversed()),
                    Answer.Order(exercise.prompt.blocks.map { it.id }),
                    Answer.Order(correct.dropLast(1)),
                    Answer.Order(correct + correct.first()),
                    Answer.Order(listOf("unknown-block")),
                    Answer.Text("raw order text"),
                )
            }
            is AnswerKey.MeaningChoice ->
                listOf(
                    Answer.Text(" ${key.expected.uppercase(Locale.ROOT)} "),
                    Answer.Text(key.suppliedChoices.first { it != key.expected }),
                    Answer.Text("journal"),
                    Answer.Text("e\u0301"),
                    Answer.Order(emptyList()),
                )
            is AnswerKey.Headword -> {
                val other = PilotContent.nouns.first { it.id != key.expected.entryId }
                val wrongArticle = if (key.expected.article == "en") "et" else "en"
                val otherWrongArticle = if (other.gender.article == "en") "et" else "en"
                listOf(
                    Answer.Text(key.expected.headword.uppercase(Locale.ROOT)),
                    Answer.Text(other.text),
                    Answer.Text("${key.expected.article} ${key.expected.headword}"),
                    Answer.Text("$wrongArticle ${key.expected.headword}"),
                    Answer.Text("${other.gender.article} ${other.text}"),
                    Answer.Text("$otherWrongArticle ${other.text}"),
                    Answer.Text("tidende"),
                    Answer.Text("   "),
                    Answer.Text("${key.expected.headword}\u00a0${other.text}"),
                    Answer.Text("a\u030Avis"),
                    Answer.Order(emptyList()),
                )
            }
            is AnswerKey.Matching -> error("Matching key is unreachable in pilot three")
            else -> error("Newer answer key is unreachable in the frozen pilot-three exporter")
        }

    private fun orderForRoles(
        exercise: GeneratedExercise,
        roles: List<String>,
    ): List<String> = roles.map { role -> exercise.source.blockRoles.entries.single { it.value == role }.key }

    private data class FormatSpec(
        val id: String,
        val targetSkillId: String?,
        val cycleSize: Int,
    )

    private class CanonicalHasher(schema: String) {
        private val digest = MessageDigest.getInstance("SHA-256")

        init {
            token(schema)
        }

        fun field(
            name: String,
            value: String,
        ) {
            token(name)
            token("value")
            token(value)
        }

        fun nullable(
            name: String,
            value: String?,
        ) {
            token(name)
            token(if (value == null) "null" else "value")
            if (value != null) token(value)
        }

        fun strings(
            name: String,
            values: List<String>,
        ) {
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

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        val value = byte.toInt() and 0xff
        "${HEX_DIGITS[value ushr 4]}${HEX_DIGITS[value and 0x0f]}"
    }

    private const val CONTENT_REVISION = "pilot-3"
    private const val GENERATOR_REVISION = "generator-3"
    private const val FIXED_SEED = 47
    private const val MAX_INDEX = 1_000_000
    private const val NULL_COLUMN = "-"
    const val ORDER_SEPARATOR = "\u001f"
    private const val HEX_DIGITS = "0123456789abcdef"
    private val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
}
