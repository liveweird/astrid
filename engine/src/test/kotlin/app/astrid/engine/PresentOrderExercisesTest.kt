package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresentOrderExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair ten publishes exact present and order activities while retaining pair nine`() {
        val catalog = engine.catalog()
        assertEquals("pilot-20", catalog.contentRevision)
        assertEquals("generator-20", catalog.generatorRevision)
        assertEquals(109, catalog.sentenceCount)
        assertEquals((1..51).map { "E${it.toString().padStart(3, '0')}" }, catalog.formats.map { it.id })
        assertEquals("evaluation-9", engine.evaluationRevision("pilot-9", "generator-9"))
        assertEquals("evaluation-10", engine.evaluationRevision("pilot-10", "generator-10"))
        assertEquals("evaluation-11", engine.evaluationRevision("pilot-11", "generator-11"))

        val present = listOf("E022", "E023").flatMap(::allExercises)
        assertEquals(
            listOf("Jeg arbejder.", "Hun læser en avis.", "Vi sover.", "De arbejder.", "I læser en bog.", "Han sover."),
            present.map { it.inspector.canonicalSentence },
        )
        val order = listOf("E024", "E025").flatMap(::allExercises)
        assertEquals(
            listOf("Vi arbejder.", "Du sover.", "Han læser en bog.", "Jeg læser en avis.", "I læser et brev.", "De læser en bog."),
            order.map { it.inspector.canonicalSentence },
        )
        assertTrue(present.take(3).all { it.prompt.choices.size == 2 })
        assertTrue(present.drop(3).all { it.prompt.choices.isEmpty() && it.prompt.inputLabel == "Danish present verb" })
        assertTrue(order.take(3).all { it.prompt.tileMode == null })
        assertTrue(order.drop(3).all { it.prompt.tileMode == "use-all" && it.prompt.requiredTileCount == 4 })
    }

    @Test
    fun `named present forms preserve exact known and unknown boundaries`() {
        val selection = exercise("E022", "present-select-work")
        assertTrue(engine.isSuppliedChoice(selection, "  arbejder  "))
        assertFalse(engine.isSuppliedChoice(selection, "læser"))
        assertFalse(engine.isSuppliedChoice(selection, "Arbejder"))
        val cases = listOf(
            "arbejder" to Expected(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, null, true),
            "arbejde" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.MET,
                "INFINITIVE_USED", true,
            ),
            "læser" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET,
                "NAMED_VERB_MISMATCH", true,
            ),
            "Arbejder" to Expected(
                TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "MID_SENTENCE_VERB_CASE_REQUIRED", true,
            ),
            "arbejder nu" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "ONE_WORD_RESPONSE_REQUIRED", true,
            ),
            "arbejdede" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_PRESENT_FORM", false,
            ),
            "arbejder." to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_PRESENT_FORM", false,
            ),
            "  " to Expected(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET, "EMPTY_RESPONSE", false),
        )
        cases.forEach { (answer, expected) ->
            val result = engine.evaluate(selection, Answer.Text(answer))
            assertEquals(expected.target, result.targetStatus, answer)
            assertEquals(expected.linguistic, result.linguisticStatus, answer)
            assertEquals(expected.constraint, result.responseConstraintStatus, answer)
            assertEquals(expected.issue, result.issues.singleOrNull()?.code, answer)
            assertNull(result.meaningSatisfaction, answer)
            if (expected.disclosesCorrection) assertEquals("arbejder", result.completedSentence, answer)
            else assertNull(result.completedSentence, answer)
        }
        val wrongKind = engine.evaluate(selection, Answer.Order(emptyList()))
        assertEquals("ANSWER_TYPE_MISMATCH", wrongKind.issues.single().code)
        assertNull(wrongKind.meaningSatisfaction)
    }

    @Test
    fun `selection choice position varies by seed for every authored card`() {
        PresentOrderContent.tasks("E022").forEach { task ->
            val positions = (0..3).map { seed ->
                val exercise = exercise("E022", task.id, seed)
                exercise.prompt.choices.indexOf((task as NamedPresentTask).verb.present)
            }.toSet()
            assertEquals(setOf(0, 1), positions, task.id)
        }
    }

    @Test
    fun `pair seventeen retains its literal three-card selection and index-dependent choice order`() {
        val exercises = (0..2).map { index ->
            engine.generate(GenerationRequest("E022", "G004", 47, index, "pilot-17", "generator-17"))
        }

        assertEquals(
            listOf("Vi sover.", "Hun læser en avis.", "Jeg arbejder."),
            exercises.map { it.inspector.canonicalSentence },
        )
        assertEquals(
            listOf(listOf("sover", "sove"), listOf("læse", "læser"), listOf("arbejder", "arbejde")),
            exercises.map { it.prompt.choices },
        )
    }

    @Test
    fun `every four-word bank has literal outcomes for all complete permutations and nonempty proper subsets`() {
        allExercises("E025").forEach { exercise ->
            val canonical = canonicalIds(exercise)
            val complete = permutations(canonical)
            assertEquals(24, complete.size)
            complete.forEach { order ->
                val result = engine.evaluate(exercise, Answer.Order(order))
                if (order == canonical) {
                    assertEquals(TargetStatus.MET, result.targetStatus)
                    assertEquals(LinguisticStatus.ACCEPTABLE, result.linguisticStatus)
                    assertEquals(ResponseConstraintStatus.MET, result.responseConstraintStatus)
                    assertTrue(result.issues.isEmpty())
                } else {
                    assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
                    assertEquals(MeaningSatisfaction.UNKNOWN, result.meaningSatisfaction)
                    assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
                    assertEquals("REQUESTED_ORDER_CONSTRAINT", result.issues.single().code)
                }
                assertEquals(exercise.inspector.canonicalSentence, result.completedSentence)
            }
            val subsets = (1 until canonical.size).flatMap { size ->
                combinations(canonical, size).flatMap(::permutations)
            }
            assertEquals(40, subsets.size)
            subsets.forEach { order ->
                val result = engine.evaluate(exercise, Answer.Order(order))
                assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
                assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
                assertEquals(MeaningSatisfaction.UNKNOWN, result.meaningSatisfaction)
                assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
                assertEquals("REQUIRED_WORDS_MISSING", result.issues.single().code)
                assertEquals(exercise.inspector.canonicalSentence, result.completedSentence)
            }
        }
    }

    @Test
    fun `placement evidence stays separate from article noun order and malformed constituent subsets`() {
        val exercise = exercise("E025", "order-words-i-read-newspaper")
        val byForm = requireNotNull(exercise.inspector.tileSource).tokens.associateBy { it.form }
        val scrambledBundle = listOf("subject", "finite-present", "singular-headword", "article").map { byForm.getValue(it).blockId }
        val result = engine.evaluate(exercise, Answer.Order(scrambledBundle))
        assertEquals(TargetStatus.MET, result.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
        assertEquals("REQUESTED_ORDER_CONSTRAINT", result.issues.single().code)

        val constituent = exercise("E024", "order-constituents-he-reads-book")
        val malformed = engine.evaluate(constituent, Answer.Order(canonicalIds(constituent).dropLast(1)))
        assertEquals(TargetStatus.UNEVALUABLE, malformed.targetStatus)
        assertEquals("ANSWER_TYPE_MISMATCH", malformed.issues.single().code)
        assertNull(malformed.completedSentence)
        val empty = engine.evaluate(exercise, Answer.Order(emptyList()))
        assertEquals("ANSWER_TYPE_MISMATCH", empty.issues.single().code)
    }

    private fun allExercises(formatId: String): List<GeneratedExercise> =
        PresentOrderContent.tasks(formatId).map { task -> exercise(formatId, task.id) }

    private fun exercise(formatId: String, taskId: String, seed: Int = 47): GeneratedExercise =
        (0 until PresentOrderContent.tasks(formatId).size).asSequence().map { index ->
            engine.generate(
                GenerationRequest(
                    formatId,
                    if (formatId in setOf("E022", "E023")) "G004" else "G005",
                    seed,
                    index,
                    "pilot-10",
                    "generator-10",
                ),
            )
        }.first { (it.source as PresentOrderExerciseSource).task.id == taskId }

    private fun canonicalIds(exercise: GeneratedExercise) = requireNotNull(exercise.inspector.tileSource)
        .licensedOrders.single().blockIds

    private fun <T> permutations(values: List<T>): List<List<T>> =
        if (values.isEmpty()) listOf(emptyList()) else values.indices.flatMap { index ->
            permutations(values.filterIndexed { candidate, _ -> candidate != index }).map { listOf(values[index]) + it }
        }

    private fun <T> combinations(values: List<T>, size: Int): List<List<T>> = when {
        size == 0 -> listOf(emptyList())
        values.size < size -> emptyList()
        else -> combinations(values.drop(1), size - 1).map { listOf(values.first()) + it } +
            combinations(values.drop(1), size)
    }

    private data class Expected(
        val target: TargetStatus,
        val linguistic: LinguisticStatus,
        val constraint: ResponseConstraintStatus,
        val issue: String?,
        val disclosesCorrection: Boolean,
    )
}
