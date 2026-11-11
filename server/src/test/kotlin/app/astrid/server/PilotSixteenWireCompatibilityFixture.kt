package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.Evaluation
import app.astrid.engine.GenerationRequest
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.NormalizedAnswer
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import app.astrid.server.plugins.ProblemDetail
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.encodeToString

internal object PilotSixteenWireCompatibilityFixture {
    const val RELEASE_SOURCE_COMMIT = "9bb10ae5633edb192651dc3d6956ff269b2a61eb"
    const val RELEASE_VERSION = "0.18.0"
    const val CONTENT_REVISION = "pilot-16"
    const val GENERATOR_REVISION = "generator-16"
    const val EVALUATION_REVISION = "evaluation-16"
    const val EXERCISE_ID = "00000000-0000-0000-0000-000000000901"
    const val MAX_INDEX = 1_000_000

    private val engine = PilotEngine()
    private val formats = listOf(
        Cycle("E001", "G004", 99), Cycle("E002", "G006", 92), Cycle("E003", "G005", 99),
        Cycle("E004", "G004", 99), Cycle("E005", "G004", 99), Cycle("E006", "G006", 92),
        Cycle("E007", null, 3), Cycle("E008", null, 3), Cycle("E009", "G005", 99),
        Cycle("E010", null, 24), Cycle("E011", null, 4), Cycle("E012", "G005", 99),
        Cycle("E013", "G004", 99), Cycle("E015", "G004", 92),
        Cycle("E016", "G003", 7), Cycle("E017", "G003", 7),
        Cycle("E018", "G004", 8), Cycle("E019", "G004", 8),
        Cycle("E020", "G038", 2), Cycle("E021", "G038", 2),
        Cycle("E022", "G004", 8), Cycle("E023", "G004", 8),
        Cycle("E024", "G005", 3), Cycle("E025", "G005", 6),
        Cycle("E026", "G030", 2), Cycle("E027", "G030", 2),
        Cycle("E028", "G030", 3),
        Cycle("E029", "G016", 4),
        Cycle("E030", "G018", 4),
    )

    fun rows(): List<String> = buildList {
        formats.forEach { format ->
            listOf(0, 47, Int.MAX_VALUE).forEach { seed ->
                repeat(format.size) { add(exerciseRow("C", format, seed, it)) }
                listOf(format.size, MAX_INDEX).forEach { add(exerciseRow("B", format, seed, it)) }
            }
        }
        repeat(32) { add(exerciseRow("M", Cycle("E014", null, 0), 47, it)) }
        listOf(0, 1, Int.MAX_VALUE).forEach { seed ->
            listOf(0, 1, MAX_INDEX).forEach { add(exerciseRow("B", Cycle("E014", null, 0), seed, it)) }
        }
        authoredExercises().forEach { exercise ->
            gradeCases(exercise).forEach { (caseId, answer) -> add(gradeRow(exercise, caseId, answer)) }
            val answer = canonicalAnswer(exercise)
            val first = evaluationCanonical(engine.evaluate(exercise, answer))
            val replay = evaluationCanonical(engine.evaluate(exercise, answer))
            require(first == replay)
            add(row("GRADE_REPLAY", taskId(exercise), hash(first, replay)))
            if (exercise.formatId in setOf("E029", "E030")) addAll(apiRejectionRows(exercise))
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=$RELEASE_VERSION")
        appendLine("captureDate=2026-11-11")
        appendLine("contentRevision=$CONTENT_REVISION")
        appendLine("generatorRevision=$GENERATOR_REVISION")
        appendLine("evaluationRevision=$EVALUATION_REVISION")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=All thirty pair-sixteen formats: finite cycles and boundaries, bounded E014 samples, all " +
                "E016-E030 tasks/scopes, sixty E029 and twenty E030 complete permutations, public and inspector wire, " +
                "question-position grades, malformed 400/no-persist contract, fingerprints and replay.",
        )
    }

    fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    fun authoredExercises(): List<GeneratedExercise> = formats.filter { it.id >= "E016" }.flatMap { format ->
        (0 until format.size).map { generate(format, 0, it) }
    }

    fun modalChoicePositions(): Set<Int> = listOf(0, 1, 2).map { seed ->
        val exercise = generate(Cycle("E028", "G030", 3), seed, 0)
        exercise.prompt.choices.indexOf(requireNotNull(exercise.inspector.modalSource).expectedInterpretation)
    }.toSet()

    private fun exerciseRow(kind: String, format: Cycle, seed: Int, index: Int): String {
        val exercise = generate(format, seed, index)
        return row(
            kind, format.id, format.skill.orEmpty(), seed.toString(), index.toString(),
            hash(
                wireJson.encodeToString(exercise.toResponse(EXERCISE_ID)),
                wireJson.encodeToString(exercise.toInspector(EXERCISE_ID)),
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
            evaluation.meaningSatisfaction?.wireValue,
        )
        return row(
            "GRADE", exercise.formatId, requireNotNull(exercise.targetScopeId), taskId(exercise), caseId,
            hash(wireJson.encodeToString(request), wireJson.encodeToString(response), evaluationCanonical(evaluation)),
        )
    }

    private fun gradeCases(exercise: GeneratedExercise): List<Pair<String, Answer>> = when {
        exercise.formatId == "E029" -> circumstanceCases(exercise)
        exercise.formatId == "E030" -> questionCases(exercise)
        exercise.formatId == "E028" -> interpretationCases(exercise)
        exercise.formatId in setOf("E026", "E027") -> abilityCases(exercise)
        exercise.formatId in setOf("E024", "E025") -> orderCases(exercise)
        else -> moduleOneCases(exercise)
    }

    private fun circumstanceCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
        return canonical.permutations().mapIndexed { index, ids -> "permutation-$index" to Answer.Order(ids) } +
            listOf(
                    "partial" to Answer.Order(canonical.dropLast(1)),
                    "blank" to Answer.Order(emptyList()),
                    "duplicate" to Answer.Order(canonical + canonical.first()),
                    "unknown-id" to Answer.Order(listOf("unknown")),
                    "wrong-kind" to Answer.Text("ukendt"),
                )
    }

    private fun questionCases(exercise: GeneratedExercise): List<Pair<String, Answer>> = circumstanceCases(exercise)

    private fun interpretationCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        val expected = requireNotNull(exercise.inspector.modalSource).expectedInterpretation
        return listOf(
                "canonical" to Answer.Text(expected),
                "normalized" to Answer.Text("  $expected  "),
                "known-wrong" to Answer.Text(exercise.prompt.choices.first { it != expected }),
                "blank" to Answer.Text(""),
                "unknown" to Answer.Text("unknown meaning"),
                "wrong-kind" to Answer.Order(emptyList()),
            )
    }

    private fun abilityCases(exercise: GeneratedExercise): List<Pair<String, Answer>> = listOf(
                "canonical" to Answer.Text("læse"),
                "normalized" to Answer.Text(" læse "),
                (if (exercise.formatId == "E026") "at-marker-selected" else "at-marker-typed") to
                    Answer.Text(if (exercise.formatId == "E026") "  at   læse  " else "at læse"),
                "finite" to Answer.Text("læser"),
                "case" to Answer.Text("LÆSE"),
                "blank" to Answer.Text(" \t "),
                "unknown" to Answer.Text("ukendt"),
                "multiword" to Answer.Text("læse nu"),
                "wrong-kind" to Answer.Order(emptyList()),
            )

    private fun orderCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
        return buildList {
            addAll(listOf(
                "canonical" to Answer.Order(canonical),
                "wrong-order" to Answer.Order(canonical.reversed()),
                "partial" to Answer.Order(canonical.dropLast(1)),
                "blank" to Answer.Order(emptyList()),
                "duplicate" to Answer.Order(canonical + canonical.first()),
                "unknown-id" to Answer.Order(listOf("unknown")),
                "wrong-kind-text" to Answer.Text("ukendt"),
                "wrong-kind-gaps" to Answer.Gaps(emptyList()),
            ))
            if (exercise.targetScopeId == "supplied-modal-order") {
                add("np-reversal" to Answer.Order(canonical.dropLast(2) + canonical.takeLast(2).reversed()))
            }
        }
    }

    private fun moduleOneCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        val moduleOneSource = exercise.inspector.moduleOneSource
        val referenceSource = exercise.inspector.inanimateReferenceSource
        val expected = moduleOneSource?.expectedForm ?: requireNotNull(referenceSource).expectedForm
        val alternatives = moduleOneSource?.knownAlternativeForms ?: requireNotNull(referenceSource).knownAlternativeForms
        val case = if (expected == "I") "i" else expected.replaceFirstChar(Char::uppercase)
        return listOf(
            "canonical" to Answer.Text(expected),
            "known-wrong" to Answer.Text(alternatives.first()),
            "case" to Answer.Text(case),
            "blank" to Answer.Text(" \t "),
            "unknown" to Answer.Text("ukendt"),
            "unicode" to Answer.Text("blåbær"),
            "multiword" to Answer.Text("vi sover"),
            "wrong-kind" to Answer.Order(listOf("b0")),
        )
    }

    private fun canonicalAnswer(exercise: GeneratedExercise): Answer = exercise.inspector.tileSource
        ?.takeIf { exercise.formatId in setOf("E024", "E025", "E029", "E030") }
        ?.licensedOrders?.single()?.blockIds?.let(Answer::Order)
        ?: Answer.Text(exercise.inspector.modalSource?.expectedInterpretation ?: run {
            exercise.inspector.moduleOneSource?.expectedForm
                ?: requireNotNull(exercise.inspector.inanimateReferenceSource).expectedForm
        })

    private fun apiRejectionRows(exercise: GeneratedExercise): List<String> {
        val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
        return listOf(
            "malformed" to AttemptAnswerDto.Order(emptyList()),
            "partial" to AttemptAnswerDto.Order(canonical.dropLast(1)),
            "wrong-kind" to AttemptAnswerDto.Text("ukendt"),
        ).map { (caseId, answer) ->
            val request = AttemptRequest(
                UUID.nameUUIDFromBytes("${taskId(exercise)}:$caseId-api".toByteArray()).toString(),
                EXERCISE_ID,
                answer,
                false,
            )
            val detail = if (caseId != "wrong-kind") {
                "The answer must contain every supplied block exactly once"
            } else {
                "This exercise requires an order answer"
            }
            val problem = ProblemDetail(
                title = "Bad Request", status = 400, detail = detail,
                instance = "/api/v1/attempts", code = "bad_request",
            )
            row(
                "API_REJECTION", taskId(exercise), caseId, "400", "persisted-attempts=0",
                hash(wireJson.encodeToString(request), wireJson.encodeToString(problem)),
            )
        }
    }

    private fun <T> List<T>.permutations(): List<List<T>> = if (size <= 1) {
        listOf(this)
    } else {
        indices.flatMap { index ->
            val selected = this[index]
            (take(index) + drop(index + 1)).permutations().map { listOf(selected) + it }
        }
    }

    private fun evaluationCanonical(value: Evaluation) = framedList(
        listOf(
            value.targetStatus.wireValue, value.linguisticStatus.wireValue,
            framedList(value.issues.map { framedList(listOf(it.code, it.message)) }), value.feedback,
            nullable(value.completedSentence), answerCanonical(value.rawAnswer), normalizedCanonical(value.normalizedAnswer),
            nullable(value.linguisticScope?.wireValue), nullable(value.responseConstraintStatus?.wireValue),
            nullable(value.meaningSatisfaction?.wireValue),
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
        is Answer.Matching -> AttemptAnswerDto.Matching(pairs.map { app.astrid.server.exercises.MatchingPairDto(it.leftId, it.rightId) })
        is Answer.Gaps -> AttemptAnswerDto.Gaps(selections.map { app.astrid.server.exercises.GapSelectionDto(it.slotId, it.choiceId) })
    }

    private fun generate(format: Cycle, seed: Int, index: Int) = engine.generate(
        GenerationRequest(format.id, format.skill, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
    )

    private fun taskId(exercise: GeneratedExercise) = exercise.inspector.moduleOneSource?.taskId
        ?: exercise.inspector.inanimateReferenceSource?.taskId
        ?: requireNotNull(exercise.inspector.canonicalSentenceId)
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

