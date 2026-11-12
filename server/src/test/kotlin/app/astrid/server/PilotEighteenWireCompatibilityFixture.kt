package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.Evaluation
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.GenerationRequest
import app.astrid.engine.MatchingPair
import app.astrid.engine.NormalizedAnswer
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.ExerciseRequest
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.MatchingPairDto
import app.astrid.server.exercises.MatchingPairResultDto
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.encodeToString

internal object PilotEighteenWireCompatibilityFixture {
    const val RELEASE_SOURCE_COMMIT = "9eefcfed708670ce45dbdd9b11adc41f9fe17d25"
    const val RELEASE_VERSION = "0.20.1"
    const val CONTENT_REVISION = "pilot-18"
    const val GENERATOR_REVISION = "generator-18"
    const val EVALUATION_REVISION = "evaluation-18"
    const val PROJECTION_SCHEMA = "pair18-server-wire-v1"
    const val CAPTURE_DATE = "2026-11-12"
    const val EXERCISE_ID = "00000000-0000-0000-0000-000000001801"
    const val MAX_INDEX = 1_000_000

    private val engine = PilotEngine()
    private val formats = listOf(
        Cycle("E001", "G004", 99), Cycle("E002", "G006", 92), Cycle("E003", "G005", 99),
        Cycle("E004", "G004", 99), Cycle("E005", "G004", 99), Cycle("E006", "G006", 92),
        Cycle("E007", null, 3), Cycle("E008", null, 3), Cycle("E009", "G005", 99),
        Cycle("E010", null, 24), Cycle("E011", null, 4), Cycle("E012", "G005", 99),
        Cycle("E013", "G004", 99), Cycle("E015", "G004", 92), Cycle("E016", "G003", 7),
        Cycle("E017", "G003", 7), Cycle("E018", "G004", 8), Cycle("E019", "G004", 8),
        Cycle("E020", "G038", 2), Cycle("E021", "G038", 2), Cycle("E022", "G004", 8),
        Cycle("E023", "G004", 8), Cycle("E024", "G005", 3), Cycle("E025", "G005", 6),
        Cycle("E026", "G030", 2), Cycle("E027", "G030", 2), Cycle("E028", "G030", 3),
        Cycle("E029", "G016", 4), Cycle("E030", "G018", 4), Cycle("E031", null, 3),
        Cycle("E032", null, 1), Cycle("E033", null, 4), Cycle("E034", "G004", 2),
        Cycle("E035", null, 1), Cycle("E036", null, 1), Cycle("E037", "G005", 3),
        Cycle("E038", "G004", 1), Cycle("E039", null, 7),
    )

    fun rows(): List<String> = buildList {
        formats.forEach { format ->
            listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
                repeat(format.size) { add(exerciseRow("C", format, seed, it)) }
                listOf(format.size, MAX_INDEX).forEach { add(exerciseRow("B", format, seed, it)) }
            }
        }
        repeat(32) { add(exerciseRow("M", Cycle("E014", null, 0), 47, it)) }
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            listOf(0, 1, MAX_INDEX).forEach { add(exerciseRow("B", Cycle("E014", null, 0), seed, it)) }
        }
        p1Exercises().forEach { exercise ->
            gradeCases(exercise).forEach { (caseId, answer) -> add(gradeRow(exercise, caseId, answer)) }
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=$RELEASE_VERSION")
        appendLine("captureDate=$CAPTURE_DATE")
        appendLine("projectionSchema=$PROJECTION_SCHEMA")
        appendLine("contentRevision=$CONTENT_REVISION")
        appendLine("generatorRevision=$GENERATOR_REVISION")
        appendLine("evaluationRevision=$EVALUATION_REVISION")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=All thirty-nine pair-eighteen formats at literal cycles and boundary seeds; all 23 P1 tasks, " +
                "public and inspector JSON, mapped private inspector metadata, requests, complete responses including per-pair " +
                "matching results, exhaustive partial matching bijections, tile alternatives and reproduction fingerprints.",
        )
    }

    fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    fun p1Exercises(): List<GeneratedExercise> = formats.filter { it.id >= "E031" }.flatMap { format ->
        (0 until format.size).map { generate(format, 0, it) }
    }

    private fun exerciseRow(kind: String, format: Cycle, seed: Int, index: Int): String {
        val exercise = generate(format, seed, index)
        val request = ExerciseRequest(
            format.id, format.skill, seed, index, CONTENT_REVISION, GENERATOR_REVISION,
        )
        return row(
            kind, format.id, format.skill.orEmpty(), seed.toString(), index.toString(), taskId(exercise),
            hash(
                wireJson.encodeToString(request),
                wireJson.encodeToString(exercise.toResponse(EXERCISE_ID)),
                wireJson.encodeToString(exercise.toInspector(EXERCISE_ID)),
                exercise.inspector.vocabularySource?.let(::privateMetadataProjection).orEmpty(),
                exercise.reproductionFingerprint(),
            ),
        )
    }

    private fun gradeRow(exercise: GeneratedExercise, caseId: String, answer: Answer): String {
        val evaluation = engine.evaluate(exercise, answer)
        val attemptId = UUID.nameUUIDFromBytes("${taskId(exercise)}:$caseId".toByteArray()).toString()
        val request = AttemptRequest(attemptId, EXERCISE_ID, answer.toDto(), false)
        val response = AttemptResponse(
            attemptId, EXERCISE_ID, evaluation.targetStatus.wireValue, evaluation.linguisticStatus.wireValue,
            evaluation.issues.map { IssueDto(it.code, it.message) }, evaluation.feedback,
            evaluation.completedSentence.orEmpty(), EVALUATION_REVISION, false, 1, 1_800_000L,
            evaluation.linguisticScope?.wireValue, evaluation.responseConstraintStatus?.wireValue,
            evaluation.meaningSatisfaction?.wireValue, evaluation.matchingResults?.map { result ->
                MatchingPairResultDto(
                    result.leftId, result.rightId,
                    LexicalTargetDto(
                        result.lexicalTarget.entryId, result.lexicalTarget.senseId,
                        result.lexicalTarget.facet, result.lexicalTarget.direction,
                    ),
                    result.targetStatus.wireValue, result.meaningSatisfaction.wireValue,
                    result.responseConstraintStatus.wireValue, result.linguisticStatus.wireValue, result.feedback,
                )
            },
        )
        return row(
            "GRADE", exercise.formatId, taskId(exercise), caseId,
            hash(wireJson.encodeToString(request), wireJson.encodeToString(response), evaluationCanonical(evaluation)),
        )
    }

    private fun gradeCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        val source = requireNotNull(exercise.inspector.vocabularySource)
        return when (exercise.formatId) {
            "E031", "E032", "E039" -> source.suppliedChoices.mapIndexed { index, choice ->
                "choice-$index" to Answer.Text(choice)
            } + listOf("out-of-bank" to Answer.Text("not supplied"), "wrong-kind" to Answer.Order(emptyList()))
            "E033" -> matchingCases(source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) })
            "E034", "E035", "E036" -> buildList {
                val expected = requireNotNull(source.expectedAnswer)
                add("canonical" to Answer.Text(expected))
                add("trim-case" to Answer.Text("  ${expected.uppercase(Locale.ROOT)}  "))
                source.knownAlternatives.forEachIndexed { index, alternative ->
                    add("known-$index" to Answer.Text(alternative.text))
                }
                if (exercise.formatId == "E036") {
                    add("mass-en-vand" to Answer.Text("en vand"))
                    add("mass-et-vand" to Answer.Text("et vand"))
                }
                add("blank" to Answer.Text(" \t "))
                add("unicode" to Answer.Text("bla\u030Abær"))
                add("prose" to Answer.Text("$expected nu"))
                add("wrong-kind" to Answer.Order(emptyList()))
            }
            "E037", "E038" -> orderCases(exercise)
            else -> error("Unsupported P1 format ${exercise.formatId}")
        }
    }

    private fun matchingCases(canonical: List<MatchingPair>): List<Pair<String, Answer>> {
        val left = canonical.map { it.leftId }
        val right = canonical.map { it.rightId }
        val valid = (0..left.size).flatMap { count ->
            combinations(left, count).flatMap { selectedLeft ->
                combinations(right, count).flatMap { selectedRight ->
                    selectedRight.permutations().map { paired ->
                        Answer.Matching(selectedLeft.zip(paired) { l, r -> MatchingPair(l, r) })
                    }
                }
            }
        }
        return valid.distinct().mapIndexed { index, answer -> "bijection-$index" to answer } + listOf(
            "reordered-array" to Answer.Matching(canonical.reversed()),
            "duplicate-left" to Answer.Matching(listOf(canonical[0], canonical[0].copy(rightId = canonical[1].rightId))),
            "duplicate-right" to Answer.Matching(listOf(canonical[0], canonical[1].copy(rightId = canonical[0].rightId))),
            "unknown-left" to Answer.Matching(listOf(MatchingPair("unknown", canonical[0].rightId))),
            "unknown-right" to Answer.Matching(listOf(MatchingPair(canonical[0].leftId, "unknown"))),
            "wrong-kind" to Answer.Text("not matching"),
        )
    }

    private fun orderCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val all = exercise.prompt.blocks.map { it.id }
        val permutations = (0..all.size).flatMap { count -> combinations(all, count).flatMap { it.permutations() } }
        return permutations.mapIndexed { index, ids -> "permutation-$index" to Answer.Order(ids) } +
            source.reviewedOrders.mapIndexed { index, reviewed ->
                val byForm = source.tileOccurrences.groupBy { it.form }.mapValues { it.value.toMutableList() }
                val ids = reviewed.words.map { word -> requireNotNull(byForm.getValue(word).removeFirstOrNull()).blockId }
                "reviewed-$index" to Answer.Order(ids)
            } + listOf(
                "partial" to Answer.Order(source.acceptedOrders.single().dropLast(1)),
                "empty" to Answer.Order(emptyList()),
                "duplicate" to Answer.Order(listOf(all.first(), all.first())),
                "unknown" to Answer.Order(listOf("unknown")),
                "wrong-kind" to Answer.Text("not an order"),
            )
    }

    private fun evaluationCanonical(value: Evaluation) = framedList(
        listOf(
            value.targetStatus.wireValue, value.linguisticStatus.wireValue,
            framedList(value.issues.map { framedList(listOf(it.code, it.message)) }), value.feedback,
            nullable(value.completedSentence), answerCanonical(value.rawAnswer), normalizedCanonical(value.normalizedAnswer),
            nullable(value.linguisticScope?.wireValue), nullable(value.responseConstraintStatus?.wireValue),
            nullable(value.meaningSatisfaction?.wireValue),
            framedList(value.matchingResults.orEmpty().map { result ->
                framedList(
                    listOf(
                        result.leftId, nullable(result.rightId), result.lexicalTarget.entryId, result.lexicalTarget.senseId,
                        result.lexicalTarget.facet, result.lexicalTarget.direction, result.targetStatus.wireValue,
                        result.meaningSatisfaction.wireValue, result.responseConstraintStatus.wireValue,
                        result.linguisticStatus.wireValue, result.feedback,
                    ),
                )
            }),
        ),
    )

    fun privateMetadataProjection(value: app.astrid.engine.VocabularyExerciseSourceDetails) = framedList(
        listOf(
            value.taskId,
            value.operation,
            framedList(value.lexicalSources.map { lexical ->
                framedList(
                    listOf(
                        lexical.entryId, lexical.senseId, lexical.kind.wireValue,
                        nullable(lexical.forms.infinitive), nullable(lexical.forms.present),
                        nullable(lexical.forms.bundle), nullable(lexical.forms.form), lexical.gloss, lexical.selectedUse,
                    ),
                )
            }),
            nullable(value.expectedAnswer),
            framedList(value.suppliedChoices),
            framedList(value.supportFacts.map { fact ->
                framedList(
                    listOf(
                        fact.factId, fact.referentId, fact.predicateId, fact.valueId, fact.valueText,
                        fact.valueRole, fact.sentence, framedList(fact.exposureKeys),
                    ),
                )
            }),
            framedList(value.matchingPairs.map { pair ->
                framedList(
                    listOf(
                        pair.leftId, pair.rightId,
                        pair.lexicalSource.entryId, pair.lexicalSource.senseId, pair.lexicalSource.kind.wireValue,
                        nullable(pair.lexicalSource.forms.infinitive), nullable(pair.lexicalSource.forms.present),
                        nullable(pair.lexicalSource.forms.bundle), nullable(pair.lexicalSource.forms.form),
                        pair.lexicalSource.gloss, pair.lexicalSource.selectedUse,
                    ),
                )
            }),
            framedList(value.tileOccurrences.map { occurrence ->
                framedList(listOf(occurrence.blockId, occurrence.lexicalId, occurrence.form))
            }),
            framedList(value.acceptedOrders.map(::framedList)),
            nullable(value.targetSupportFactId),
            nullable(value.questionRole),
            nullable(value.normalizationPolicy),
            framedList(value.knownAlternatives.map { alternative ->
                framedList(
                    listOf(
                        alternative.text, alternative.targetStatus.wireValue, alternative.linguisticStatus.wireValue,
                        alternative.meaningSatisfaction.wireValue, alternative.responseConstraintStatus.wireValue,
                        nullable(alternative.issueCode),
                    ),
                )
            }),
            framedList(value.reviewedOrders.map { reviewed ->
                framedList(
                    listOf(
                        framedList(reviewed.words), reviewed.targetStatus.wireValue, reviewed.linguisticStatus.wireValue,
                        reviewed.meaningSatisfaction.wireValue, reviewed.responseConstraintStatus.wireValue,
                        nullable(reviewed.issueCode),
                    ),
                )
            }),
        ),
    )

    private fun answerCanonical(value: Answer) = when (value) {
        is Answer.Text -> framedList(listOf("text", value.text))
        is Answer.Order -> framedList(listOf("order", framedList(value.blockIds)))
        is Answer.Matching -> framedList(
            listOf("matching", framedList(value.pairs.map { framedList(listOf(it.leftId, it.rightId)) })),
        )
        is Answer.Gaps -> framedList(
            listOf("gaps", framedList(value.selections.map { framedList(listOf(it.slotId, it.choiceId)) })),
        )
    }

    private fun normalizedCanonical(value: NormalizedAnswer) = when (value) {
        is NormalizedAnswer.Text -> framedList(listOf("text", value.text))
        is NormalizedAnswer.Order -> framedList(listOf("order", framedList(value.blockIds)))
        is NormalizedAnswer.Matching -> framedList(
            listOf("matching", framedList(value.pairs.map { framedList(listOf(it.leftId, it.rightId)) })),
        )
        is NormalizedAnswer.Gaps -> framedList(
            listOf("gaps", framedList(value.selections.map { framedList(listOf(it.slotId, it.choiceId)) })),
        )
    }

    private fun Answer.toDto() = when (this) {
        is Answer.Text -> AttemptAnswerDto.Text(text)
        is Answer.Order -> AttemptAnswerDto.Order(blockIds)
        is Answer.Matching -> AttemptAnswerDto.Matching(pairs.map { MatchingPairDto(it.leftId, it.rightId) })
        is Answer.Gaps -> AttemptAnswerDto.Gaps(
            selections.map { app.astrid.server.exercises.GapSelectionDto(it.slotId, it.choiceId) },
        )
    }

    private fun generate(format: Cycle, seed: Int, index: Int) = engine.generate(
        GenerationRequest(format.id, format.skill, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
    )

    private fun taskId(exercise: GeneratedExercise) = exercise.inspector.vocabularySource?.taskId
        ?: exercise.inspector.moduleOneSource?.taskId
        ?: exercise.inspector.inanimateReferenceSource?.taskId
        ?: exercise.inspector.canonicalSentenceId.orEmpty()

    private fun <T> combinations(values: List<T>, size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList())
        values.size < size -> emptyList()
        else -> values.indices.flatMap { index ->
            combinations(values.drop(index + 1), size - 1).map { listOf(values[index]) + it }
        }
    }

    private fun <T> List<T>.permutations(): List<List<T>> = if (size <= 1) {
        listOf(this)
    } else {
        indices.flatMap { index ->
            (take(index) + drop(index + 1)).permutations().map { listOf(this[index]) + it }
        }
    }

    private fun nullable(value: String?) = if (value == null) "0" else "1${framed(value)}"
    private fun framedList(values: List<String>) = "${values.size}[${values.joinToString("", transform = ::framed)}]"
    private fun framed(value: String) = "${value.toByteArray().size}:$value"
    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun hash(vararg fields: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray()
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        return digest.digest().toHex()
    }
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private data class Cycle(val id: String, val skill: String?, val size: Int)
}
