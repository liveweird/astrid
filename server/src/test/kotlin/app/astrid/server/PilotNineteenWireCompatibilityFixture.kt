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

internal object PilotNineteenWireCompatibilityFixture {
    private val engine = PilotEngine()
    private val formats = (1..44).map { number ->
        val id = "E${number.toString().padStart(3, '0')}"
        Cycle(id, engine.catalog().formats.single { it.id == id }.targetSkillId, cycleSize(id))
    }

    fun rows(): List<String> = buildList {
        formats.forEach { format ->
            SEEDS.forEach { seed ->
                repeat(format.size) { add(exerciseRow("C", format, seed, it)) }
                listOf(format.size, MAX_INDEX).forEach { add(exerciseRow("B", format, seed, it)) }
            }
        }
        p2Exercises().forEach { exercise ->
            gradeCases(exercise).forEachIndexed { index, answer ->
                add(gradeRow(exercise, "case-$index", answer))
            }
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=0.22.0")
        appendLine("captureDate=$CAPTURE_DATE")
        appendLine("projectionSchema=pair19-server-wire-v1")
        appendLine("contentRevision=$CONTENT_REVISION")
        appendLine("generatorRevision=$GENERATOR_REVISION")
        appendLine("evaluationRevision=$EVALUATION_REVISION")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=All forty-four pair-nineteen public and inspector JSON projections at cycle and boundary seeds, " +
                "literal private generation evidence, requests, fingerprints, complete P2 grading responses and replay bytes.",
        )
    }

    fun p2Exercises() = formats.filter { it.id >= "E040" }.flatMap { format ->
        (0 until format.size).map { generate(format, 47, it) }
    }

    private fun exerciseRow(kind: String, format: Cycle, seed: Int, index: Int): String {
        val exercise = generate(format, seed, index)
        val request = ExerciseRequest(format.id, format.skill, seed, index, CONTENT_REVISION, GENERATOR_REVISION)
        return row(
            kind, format.id, format.skill.orEmpty(), seed.toString(), index.toString(), taskId(exercise),
            hash(
                wireJson.encodeToString(request), wireJson.encodeToString(exercise.toResponse(EXERCISE_ID)),
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
            evaluation.completedSentence.orEmpty(), EVALUATION_REVISION, false, 1, 1_900_000L,
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
            hash(
                wireJson.encodeToString(request), wireJson.encodeToString(response),
                evaluationCanonical(evaluation),
            ),
        )
    }

    private fun gradeCases(exercise: GeneratedExercise): List<Answer> {
        val source = requireNotNull(exercise.inspector.vocabularySource)
        return when (exercise.formatId) {
            "E042" -> matchingCases(source.matchingPairs.map { MatchingPair(it.leftId, it.rightId) })
            "E043" -> exercise.prompt.blocks.map { it.id }.permutations().map(Answer::Order) + listOf(
                Answer.Order(source.acceptedOrders.single().dropLast(1)), Answer.Order(emptyList()),
                Answer.Order(listOf("unknown")), Answer.Text("not an order"),
            )
            else -> source.suppliedChoices.map { Answer.Text(it) } + listOf(
                Answer.Text("outside supplied bank"), Answer.Text(""), Answer.Order(emptyList()),
            )
        }
    }

    private fun matchingCases(canonical: List<MatchingPair>): List<Answer> {
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
        return valid + listOf(
            Answer.Matching(listOf(MatchingPair(left.first(), "unknown"))),
            Answer.Matching(listOf(MatchingPair("unknown", right.first()))),
            Answer.Text("not matching"), Answer.Order(emptyList()),
        )
    }

    private fun evaluationCanonical(value: Evaluation) = framedList(listOf(
        value.targetStatus.wireValue, value.linguisticStatus.wireValue,
        framedList(value.issues.map { framedList(listOf(it.code, it.message)) }), value.feedback,
        nullable(value.completedSentence), answerCanonical(value.rawAnswer), normalizedCanonical(value.normalizedAnswer),
        nullable(value.linguisticScope?.wireValue), nullable(value.responseConstraintStatus?.wireValue),
        nullable(value.meaningSatisfaction?.wireValue),
        framedList(value.matchingResults.orEmpty().map { result -> framedList(listOf(
            result.leftId, nullable(result.rightId), result.lexicalTarget.entryId, result.lexicalTarget.senseId,
            result.lexicalTarget.facet, result.lexicalTarget.direction, result.targetStatus.wireValue,
            result.meaningSatisfaction.wireValue, result.responseConstraintStatus.wireValue,
            result.linguisticStatus.wireValue, result.feedback,
        )) }),
    ))

    fun privateMetadataProjection(value: app.astrid.engine.VocabularyExerciseSourceDetails) = framedList(listOf(
        value.taskId, value.operation,
        framedList(value.lexicalSources.map(::lexicalProjection)), nullable(value.expectedAnswer),
        framedList(value.suppliedChoices),
        framedList(value.supportFacts.map { fact -> framedList(listOf(
            fact.factId, fact.referentId, fact.predicateId, fact.valueId, fact.valueText, fact.valueRole,
            fact.sentence, framedList(fact.exposureKeys), nullable(fact.referentDisplay),
            nullable(fact.predicateSource?.let(::lexicalProjection)),
            nullable(fact.objectSource?.let(::lexicalProjection)),
            nullable(fact.quantitySource?.let(::quantityProjection)), nullable(fact.translation),
        )) }),
        framedList(value.matchingPairs.map { pair -> framedList(listOf(
            pair.leftId, pair.rightId, lexicalProjection(pair.lexicalSource),
        )) }),
        framedList(value.tileOccurrences.map { occurrence -> framedList(listOf(
            occurrence.blockId, occurrence.lexicalId, occurrence.form,
            nullable(occurrence.lexicalSource?.let(::lexicalProjection)),
            nullable(occurrence.quantitySource?.let(::quantityProjection)),
        )) }),
        framedList(value.acceptedOrders.map(::framedList)), nullable(value.targetSupportFactId),
        nullable(value.questionRole), nullable(value.normalizationPolicy),
        framedList(value.knownAlternatives.map { alternative -> framedList(listOf(
            alternative.text, alternative.targetStatus.wireValue, alternative.linguisticStatus.wireValue,
            alternative.meaningSatisfaction.wireValue, alternative.responseConstraintStatus.wireValue,
            nullable(alternative.issueCode),
        )) }),
        framedList(value.reviewedOrders.map { order -> framedList(listOf(
            framedList(order.words), order.targetStatus.wireValue, order.linguisticStatus.wireValue,
            order.meaningSatisfaction.wireValue, order.responseConstraintStatus.wireValue, nullable(order.issueCode),
        )) }),
        framedList(value.chronologyRelations.map { relation -> framedList(listOf(
            relation.relationId, relation.beforeFactId, relation.afterFactId,
        )) }),
        nullable(value.targetChronologyRelationId),
    ))

    private fun lexicalProjection(value: app.astrid.engine.VocabularyLexicalSourceDetails) = framedList(listOf(
        value.entryId, value.senseId, value.kind.wireValue, nullable(value.forms.infinitive),
        nullable(value.forms.present), nullable(value.forms.bundle), nullable(value.forms.form), value.gloss, value.selectedUse,
    ))

    private fun quantityProjection(value: app.astrid.engine.VocabularyQuantitySourceDetails) = framedList(listOf(
        value.constructionId, value.display, lexicalProjection(value.headSource),
        framedList(value.componentSources.map(::lexicalProjection)),
    ))

    private fun answerCanonical(value: Answer) = when (value) {
        is Answer.Text -> framedList(listOf("text", value.text))
        is Answer.Order -> framedList(listOf("order", framedList(value.blockIds)))
        is Answer.Matching -> framedList(listOf(
            "matching", framedList(value.pairs.map { framedList(listOf(it.leftId, it.rightId)) }),
        ))
        is Answer.Gaps -> framedList(listOf(
            "gaps", framedList(value.selections.map { framedList(listOf(it.slotId, it.choiceId)) }),
        ))
    }

    private fun normalizedCanonical(value: NormalizedAnswer) = when (value) {
        is NormalizedAnswer.Text -> framedList(listOf("text", value.text))
        is NormalizedAnswer.Order -> framedList(listOf("order", framedList(value.blockIds)))
        is NormalizedAnswer.Matching -> framedList(listOf(
            "matching", framedList(value.pairs.map { framedList(listOf(it.leftId, it.rightId)) }),
        ))
        is NormalizedAnswer.Gaps -> framedList(listOf(
            "gaps", framedList(value.selections.map { framedList(listOf(it.slotId, it.choiceId)) }),
        ))
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
        ?: exercise.inspector.moduleOneSource?.taskId ?: exercise.inspector.inanimateReferenceSource?.taskId
        ?: exercise.inspector.canonicalSentenceId.orEmpty()
    private fun cycleSize(id: String) = when {
        id == "E014" -> 32
        id in (1..30).map { "E${it.toString().padStart(3, '0')}" } -> inheritedCycleSize(id)
        id <= "E039" -> mapOf(
            "E031" to 3, "E032" to 1, "E033" to 4, "E034" to 2, "E035" to 1,
            "E036" to 1, "E037" to 3, "E038" to 1, "E039" to 7,
        ).getValue(id)
        else -> mapOf("E040" to 4, "E041" to 3, "E042" to 4, "E043" to 5, "E044" to 5).getValue(id)
    }
    private fun inheritedCycleSize(id: String) = when (id) {
        "E001", "E003", "E004", "E005", "E009", "E012", "E013" -> 99
        "E002", "E006", "E015" -> 92; "E007", "E008" -> 3; "E010" -> 24; "E011" -> 4
        "E016", "E017" -> 7; "E018", "E019", "E022", "E023" -> 8
        "E020", "E021", "E026", "E027" -> 2; "E024", "E028" -> 3; "E025" -> 6
        "E029", "E030" -> 4; else -> error("Unknown format $id")
    }
    private fun <T> combinations(values: List<T>, size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList())
        values.size < size -> emptyList()
        else -> values.indices.flatMap { index ->
            combinations(values.drop(index + 1), size - 1).map { listOf(values[index]) + it }
        }
    }
    private fun <T> List<T>.permutations(): List<List<T>> = if (size <= 1) listOf(this) else indices.flatMap { index ->
        (take(index) + drop(index + 1)).permutations().map { listOf(this[index]) + it }
    }
    private fun nullable(value: String?) = if (value == null) "0" else "1${framed(value)}"
    private fun framedList(values: List<String>) = "${values.size}[${values.joinToString("", transform = ::framed)}]"
    private fun framed(value: String) = "${value.toByteArray().size}:$value"
    private fun hash(vararg fields: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray(); digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array()); digest.update(bytes)
        }
        return digest.digest().toHex()
    }
    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private data class Cycle(val id: String, val skill: String?, val size: Int)

    const val RELEASE_SOURCE_COMMIT = "844b88af33cc729e095d443adb72fcd72cd17534"
    const val CAPTURE_DATE = "2026-11-13"
    const val CONTENT_REVISION = "pilot-19"
    const val GENERATOR_REVISION = "generator-19"
    const val EVALUATION_REVISION = "evaluation-19"
    const val EXERCISE_ID = "00000000-0000-0000-0000-000000001901"
    const val MAX_INDEX = 1_000_000
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}
