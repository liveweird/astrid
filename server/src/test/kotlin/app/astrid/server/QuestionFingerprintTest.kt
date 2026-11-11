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
import kotlin.test.assertNull

class QuestionFingerprintTest {
    private val engine = PilotEngine()

    @Test
    fun `pair sixteen fingerprints the complete question bank and existing inspector fields`() {
        val expectedRolesByCue = mapOf(
            "Du læser en avis." to setOf("FINITE_VERB", "SUBJECT", "OBJECT"),
            "Hun arbejder." to setOf("FINITE_VERB", "SUBJECT"),
            "I bor i Aarhus." to setOf("FINITE_VERB", "SUBJECT", "LOCATION_COMPLEMENT"),
            "De har et brev." to setOf("FINITE_VERB", "SUBJECT", "OBJECT"),
        )
        val seenCues = mutableSetOf<String>()

        repeat(4) { index ->
            val exercise = engine.generate(GenerationRequest("E030", "G018", 47, index, "pilot-16", "generator-16"))
            val tiles = assertNotNull(exercise.inspector.tileSource)
            val first = tiles.tokens.first()
            val licensed = tiles.licensedOrders.single()
            val original = exercise.reproductionFingerprint()
            val cue = assertNotNull(exercise.prompt.cue)
            seenCues += cue

            assertEquals("neutral-positive-yes-no", exercise.targetScopeId)
            assertEquals("da", exercise.prompt.cueLanguage)
            assertEquals("da", exercise.prompt.answerLanguage)
            assertEquals("?", exercise.prompt.punctuation)
            assertNull(exercise.toResponse(ID).prompt.tileMode)
            assertNull(exercise.toResponse(ID).prompt.requiredTileCount)
            assertEquals(exercise.inspector.canonicalSentenceId, exercise.toInspector(ID).canonicalSentenceId)
            assertEquals(exercise.inspector.constructionId, exercise.toInspector(ID).constructionId)
            assertEquals(expectedRolesByCue.getValue(cue), exercise.inspector.clauseRoles.toSet())

            listOf(
                exercise.inspector.copy(canonicalSentenceId = "different"),
                exercise.inspector.copy(constructionId = "different"),
                exercise.inspector.copy(lexicalIds = exercise.inspector.lexicalIds.reversed()),
                exercise.inspector.copy(clauseRoles = exercise.inspector.clauseRoles.reversed()),
                exercise.inspector.copy(canonicalSentence = "Different?"),
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

        assertEquals(expectedRolesByCue.keys, seenCues)
    }

    private companion object {
        const val ID = "00000000-0000-0000-0000-000000000001"
    }
}
