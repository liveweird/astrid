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
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import kotlinx.serialization.encodeToString

internal object PilotThirteenWireCompatibilityFixture {
    const val RELEASE_SOURCE_COMMIT = "43e9726b079f4483103badbd87d2209da943adb8"
    const val RELEASE_VERSION = "0.15.0"
    const val CONTENT_REVISION = "pilot-13"
    const val GENERATOR_REVISION = "generator-13"
    const val EVALUATION_REVISION = "evaluation-13"
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
        Cycle("E024", "G005", 3), Cycle("E025", "G005", 3),
        Cycle("E026", "G030", 2), Cycle("E027", "G030", 2),
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
            "description=All twenty-seven pair-thirteen formats: finite cycles and boundaries, bounded E014 samples, all " +
                "E016-E027 tasks/scopes/private ability metadata, all three E026 choice positions, public and inspector wire, " +
                "fingerprints, canonical/normalized/marker/finite/case/blank/unknown/wrong-kind and replay grades.",
        )
    }

    fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    fun authoredExercises(): List<GeneratedExercise> = formats.filter { it.id >= "E016" }.flatMap { format ->
        (0 until format.size).map { generate(format, 0, it) }
    }

    fun abilityChoicePositions(): Set<Int> = listOf(0, 47, Int.MAX_VALUE).flatMap { seed ->
        (0 until 2).map { index ->
            generate(Cycle("E026", "G030", 2), seed, index).prompt.choices.indexOf("læse")
        }
    }.toSet()

    fun abilityMarkerConstraintStatuses(): Map<String, String?> = listOf("E026", "E027").associateWith { formatId ->
        engine.evaluate(generate(Cycle(formatId, "G030", 2), 0, 0), Answer.Text("at læse"))
            .responseConstraintStatus?.wireValue
    }

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

    private fun gradeCases(exercise: GeneratedExercise): List<Pair<String, Answer>> {
        return if (exercise.formatId in setOf("E026", "E027")) {
            listOf(
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
        } else {
            exercise.inspector.tileSource?.takeIf { exercise.formatId in setOf("E024", "E025") }?.let { source ->
                val canonical = source.licensedOrders.single().blockIds
                return listOf(
                    "canonical" to Answer.Order(canonical),
                    "wrong-order" to Answer.Order(canonical.reversed()),
                    "partial" to Answer.Order(canonical.dropLast(1)),
                    "blank" to Answer.Order(emptyList()),
                    "duplicate" to Answer.Order(canonical + canonical.first()),
                    "unknown-id" to Answer.Order(listOf("unknown")),
                    "wrong-kind-text" to Answer.Text("ukendt"),
                    "wrong-kind-gaps" to Answer.Gaps(emptyList()),
                )
            }
            val moduleOneSource = exercise.inspector.moduleOneSource
            val referenceSource = exercise.inspector.inanimateReferenceSource
            val expected = moduleOneSource?.expectedForm ?: requireNotNull(referenceSource).expectedForm
            val alternatives = moduleOneSource?.knownAlternativeForms ?: requireNotNull(referenceSource).knownAlternativeForms
            val case = if (expected == "I") "i" else expected.replaceFirstChar(Char::uppercase)
            listOf(
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
    }

    private fun canonicalAnswer(exercise: GeneratedExercise): Answer = exercise.inspector.tileSource
        ?.takeIf { exercise.formatId in setOf("E024", "E025") }
        ?.licensedOrders?.single()?.blockIds?.let(Answer::Order)
        ?: Answer.Text(
            exercise.inspector.moduleOneSource?.expectedForm
                ?: requireNotNull(exercise.inspector.inanimateReferenceSource).expectedForm,
        )

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
