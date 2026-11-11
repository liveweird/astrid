package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class ModalFingerprintTest {
    private val engine = PilotEngine()

    @Test
    fun `modal interpretation fingerprint includes every selected sense and context field`() {
        val exercise = engine.generate(GenerationRequest("E028", "G030", 47, 0, "pilot-14", "generator-14"))
        val source = assertNotNull(exercise.inspector.modalSource)
        val mapped = assertNotNull(exercise.toInspector("00000000-0000-0000-0000-000000000001").modalSource)
        assertEquals(source.taskId, mapped.taskId)
        assertEquals(source.expectedInterpretation, mapped.expectedInterpretation)
        val original = exercise.reproductionFingerprint()
        val changes = listOf(
            source.copy(taskId = "different"),
            source.copy(modalEntryId = "different"),
            source.copy(modalSenseId = "different"),
            source.copy(expectedInterpretation = "different"),
            source.copy(frameVariantId = "different"),
            source.copy(contextVariantId = "different"),
        )
        changes.forEach { changed ->
            assertNotEquals(original, exercise.reproductionFingerprint(
                inspectorForFingerprint = exercise.inspector.copy(modalSource = changed),
            ))
        }
        assertNotEquals(original, exercise.reproductionFingerprint(
            promptForFingerprint = exercise.prompt.copy(choices = exercise.prompt.choices.reversed()),
        ))
        assertNotEquals(original, exercise.reproductionFingerprint(targetScopeIdForFingerprint = "different"))
    }

    @Test
    fun `supplied modal order fingerprints both word order and selected meaning provenance`() {
        val exercise = (0..5).map { index ->
            engine.generate(GenerationRequest("E025", "G005", 47, index, "pilot-14", "generator-14"))
        }.first { it.targetScopeId == "supplied-modal-order" }
        val source = assertNotNull(exercise.inspector.modalSource)
        val tiles = assertNotNull(exercise.inspector.tileSource)
        val canonical = tiles.licensedOrders.single()
        val original = exercise.reproductionFingerprint()
        assertNotEquals(original, exercise.reproductionFingerprint(
            inspectorForFingerprint = exercise.inspector.copy(
                modalSource = source.copy(modalSenseId = "different"),
            ),
        ))
        assertNotEquals(original, exercise.reproductionFingerprint(
            inspectorForFingerprint = exercise.inspector.copy(
                tileSource = tiles.copy(licensedOrders = listOf(canonical.copy(blockIds = canonical.blockIds.reversed()))),
            ),
        ))
    }
}
