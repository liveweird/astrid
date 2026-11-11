package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class PresentOrderFingerprintTest {
    private val engine = PilotEngine()

    @Test
    fun `pair eleven fingerprints the scoped possession task and its private grading metadata`() {
        val exercise = (0 until 5).map { index ->
            engine.generate(GenerationRequest("E022", "G004", 47, index, "pilot-11", "generator-11"))
        }.first { it.targetScopeId == "have-possession-present" }
        val source = assertNotNull(exercise.inspector.moduleOneSource)
        val id = "00000000-0000-0000-0000-000000000001"
        assertEquals("L009", source.targetLexicalId)
        assertEquals("har", exercise.toInspector(id).moduleOneSource?.expectedForm)
        assertEquals("have-possession-present", exercise.toResponse(id).targetScopeId)
        val original = exercise.reproductionFingerprint()
        assertNotEquals(original, exercise.reproductionFingerprint(targetScopeIdForFingerprint = "named-three-verbs-present"))
        assertNotEquals(
            original,
            exercise.reproductionFingerprint(
                inspectorForFingerprint = exercise.inspector.copy(moduleOneSource = source.copy(expectedForm = "have")),
            ),
        )
    }

    @Test
    fun `named present source stays private and its choices and grading metadata are fingerprinted`() {
        val exercise = engine.generate(GenerationRequest("E022", "G004", 47, 0, "pilot-10", "generator-10"))
        val source = assertNotNull(exercise.inspector.moduleOneSource)
        val id = "00000000-0000-0000-0000-000000000001"
        assertEquals(source.expectedForm, exercise.toInspector(id).moduleOneSource?.expectedForm)
        assertEquals("named-three-verbs-present", exercise.toResponse(id).targetScopeId)

        val original = exercise.reproductionFingerprint()
        val mutatedSources = listOf(
            source.copy(taskId = "different"),
            source.copy(targetLexicalId = "different"),
            source.copy(expectedForm = "different"),
            source.copy(knownAlternativeForms = source.knownAlternativeForms + "different"),
            source.copy(referenceRole = "different"),
            source.copy(frameVariantId = "different"),
            source.copy(casePolicy = "different"),
            source.copy(choiceRationales = source.choiceRationales.reversed()),
        )
        for (mutation in mutatedSources) {
            assertNotEquals(original, exercise.reproductionFingerprint(
                inspectorForFingerprint = exercise.inspector.copy(moduleOneSource = mutation),
            ))
        }
        assertNotEquals(original, exercise.reproductionFingerprint(
            promptForFingerprint = exercise.prompt.copy(choices = exercise.prompt.choices.reversed()),
        ))
    }

    @Test
    fun `word-order fingerprint covers the bank and its private canonical order`() {
        val exercise = engine.generate(GenerationRequest("E025", "G005", 47, 0, "pilot-10", "generator-10"))
        val source = assertNotNull(exercise.inspector.tileSource)
        val order = source.licensedOrders.single()
        val first = source.tokens.first()
        val original = exercise.reproductionFingerprint()
        val mutatedSources = listOf(
            source.copy(tokens = source.tokens.reversed()),
            source.copy(tokens = listOf(first.copy(blockId = "different")) + source.tokens.drop(1)),
            source.copy(tokens = listOf(first.copy(lexicalId = "different")) + source.tokens.drop(1)),
            source.copy(tokens = listOf(first.copy(form = "different")) + source.tokens.drop(1)),
            source.copy(licensedOrders = listOf(order.copy(kind = "different"))),
            source.copy(licensedOrders = listOf(order.copy(blockIds = order.blockIds.reversed()))),
        )
        for (mutation in mutatedSources) {
            assertNotEquals(original, exercise.reproductionFingerprint(
                inspectorForFingerprint = exercise.inspector.copy(tileSource = mutation),
            ))
        }
        val prompt = exercise.prompt
        for (mutation in listOf(
            prompt.copy(blocks = prompt.blocks.reversed()),
            prompt.copy(tileMode = "with-spares"),
            prompt.copy(requiredTileCount = 3),
        )) {
            assertNotEquals(original, exercise.reproductionFingerprint(promptForFingerprint = mutation))
        }
        assertNotEquals(original, exercise.reproductionFingerprint(targetScopeIdForFingerprint = "different"))
    }
}
