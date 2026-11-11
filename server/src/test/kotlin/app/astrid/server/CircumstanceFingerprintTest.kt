package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.GenerationRequest
import app.astrid.engine.LinguisticStatus
import app.astrid.engine.PilotEngine
import app.astrid.engine.ResponseConstraintStatus
import app.astrid.engine.TargetStatus
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CircumstanceFingerprintTest {
    private val engine = PilotEngine()

    @Test
    fun `pair fifteen fingerprints the complete circumstance bank and existing inspector fields`() {
        val exercise = engine.generate(GenerationRequest("E029", "G016", 47, 0, "pilot-15", "generator-15"))
        val tiles = assertNotNull(exercise.inspector.tileSource)
        val first = tiles.tokens.first()
        val licensed = tiles.licensedOrders.single()
        val original = exercise.reproductionFingerprint()
        val canonicalEvaluation = engine.evaluate(exercise, Answer.Order(licensed.blockIds))

        assertEquals("one-final-circumstance", exercise.targetScopeId)
        assertNull(exercise.toResponse(ID).prompt.tileMode)
        assertNull(exercise.toResponse(ID).prompt.requiredTileCount)
        assertEquals(exercise.inspector.canonicalSentenceId, exercise.toInspector(ID).canonicalSentenceId)
        assertEquals(exercise.inspector.constructionId, exercise.toInspector(ID).constructionId)
        assertEquals(setOf("SUBJECT", "FINITE_VERB", "OBJECT", "CIRCUMSTANCE"), exercise.inspector.clauseRoles.toSet())
        assertEquals(TargetStatus.MET, canonicalEvaluation.targetStatus)
        assertEquals(ResponseConstraintStatus.MET, canonicalEvaluation.responseConstraintStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, canonicalEvaluation.linguisticStatus)

        listOf(
            exercise.inspector.copy(canonicalSentenceId = "different"),
            exercise.inspector.copy(constructionId = "different"),
            exercise.inspector.copy(lexicalIds = exercise.inspector.lexicalIds.reversed()),
            exercise.inspector.copy(clauseRoles = exercise.inspector.clauseRoles.reversed()),
            exercise.inspector.copy(canonicalSentence = "Different."),
            exercise.inspector.copy(tileSource = tiles.copy(tokens = tiles.tokens.reversed())),
            exercise.inspector.copy(
                tileSource = tiles.copy(tokens = listOf(first.copy(blockId = "different")) + tiles.tokens.drop(1)),
            ),
            exercise.inspector.copy(
                tileSource = tiles.copy(tokens = listOf(first.copy(lexicalId = "different")) + tiles.tokens.drop(1)),
            ),
            exercise.inspector.copy(
                tileSource = tiles.copy(tokens = listOf(first.copy(form = "different")) + tiles.tokens.drop(1)),
            ),
            exercise.inspector.copy(
                tileSource = tiles.copy(licensedOrders = listOf(licensed.copy(kind = "different"))),
            ),
            exercise.inspector.copy(
                tileSource = tiles.copy(licensedOrders = listOf(licensed.copy(blockIds = licensed.blockIds.reversed()))),
            ),
        ).forEach { changed ->
            assertNotEquals(original, exercise.reproductionFingerprint(inspectorForFingerprint = changed))
        }
        assertNotEquals(
            original,
            exercise.reproductionFingerprint(
                promptForFingerprint = exercise.prompt.copy(blocks = exercise.prompt.blocks.reversed()),
            ),
        )
        assertNotEquals(original, exercise.reproductionFingerprint(targetScopeIdForFingerprint = "different"))
    }

    private companion object {
        const val ID = "00000000-0000-0000-0000-000000000001"
    }
}
