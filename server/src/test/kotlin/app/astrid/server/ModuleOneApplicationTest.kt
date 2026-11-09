package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.PracticeAttemptSource
import app.astrid.server.exercises.PracticeExposureState
import app.astrid.server.exercises.frozenFeedbackExposureKeys
import app.astrid.server.exercises.toAttemptContext
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModuleOneApplicationTest {
    private val engine = PilotEngine()

    @Test
    fun `pair eight exposes scope publicly and keeps teaching source private`() {
        val exercise = engine.generate(GenerationRequest("E016", "G003", 7, 0, "pilot-8", "generator-8"))
        val public = exercise.toResponse(EXERCISE_ID)
        val inspector = exercise.toInspector(EXERCISE_ID)

        assertEquals(exercise.targetScopeId, public.targetScopeId)
        val moduleOneSource = assertNotNull(inspector.moduleOneSource)
        assertEquals(exercise.inspector.moduleOneSource?.expectedForm, moduleOneSource.expectedForm)
        assertNull(public.lexicalTarget)
    }

    @Test
    fun `pair eight fingerprint covers scope and every nested module one source field and order`() {
        val exercise = engine.generate(GenerationRequest("E016", "G003", 7, 0, "pilot-8", "generator-8"))
        val source = assertNotNull(exercise.inspector.moduleOneSource)
        val rationale = source.choiceRationales.first()
        val original = exercise.reproductionFingerprint()
        fun fingerprint(changed: app.astrid.engine.ModuleOneSourceDetails) = exercise.reproductionFingerprint(
            inspectorForFingerprint = exercise.inspector.copy(moduleOneSource = changed),
        )
        val mutations = listOf(
            exercise.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(source.copy(taskId = "changed")),
            fingerprint(source.copy(targetLexicalId = "changed")),
            fingerprint(source.copy(expectedForm = "changed")),
            fingerprint(source.copy(knownAlternativeForms = source.knownAlternativeForms + "changed")),
            fingerprint(source.copy(knownAlternativeForms = source.knownAlternativeForms.reversed())),
            fingerprint(source.copy(referenceRole = "changed")),
            fingerprint(source.copy(frameVariantId = "changed")),
            fingerprint(source.copy(casePolicy = "changed")),
            fingerprint(
                source.copy(choiceRationales = listOf(rationale.copy(choice = "changed")) + source.choiceRationales.drop(1)),
            ),
            fingerprint(
                source.copy(choiceRationales = listOf(rationale.copy(rationale = "changed")) + source.choiceRationales.drop(1)),
            ),
            fingerprint(source.copy(choiceRationales = source.choiceRationales.reversed())),
        )
        mutations.forEach { assertNotEquals(original, it) }
    }

    @Test
    fun `required introduction and known feedback release only explicit module one exposure`() {
        val source = PracticeAttemptSource(
            focusId = "module-one-singular",
            planRevision = "module-one-singular-1",
            seed = 3,
            position = 0,
            introductionOpened = true,
            items = listOf(
                PracticeExposureState(
                    position = 0,
                    answerExposureKeys = listOf("module-one:subject-form:L001", "not-relevant"),
                    promptExposureKeys = emptyList(),
                    lessonExposureKeys = emptyList(),
                    hintExposureKeys = emptyList(),
                    feedbackExposureKeys = listOf("module-one:subject-form:L001"),
                    lessonOpened = false,
                    hintOpened = false,
                    feedbackReleased = false,
                ),
            ),
            introductionExposureKeys = listOf("module-one:subject-form:L001", "different-item"),
            exposurePolicyId = "module-one-exposure-1",
        )
        val context = source.toAttemptContext()
        assertEquals(-1, context.priorExposure.single().position)
        assertEquals("introduction", context.priorExposure.single().source)
        assertEquals(listOf("module-one:subject-form:L001"), context.priorExposure.single().keys)

        val candidate = listOf("module-one:subject-form:L001")
        assertEquals(
            candidate,
            frozenFeedbackExposureKeys(candidate, emptyList(), listOf(attempt("not-met", "known-issue")), "module-one-exposure-1"),
        )
        assertTrue(
            frozenFeedbackExposureKeys(candidate, emptyList(), listOf(attempt("unevaluable", "unknown")), "module-one-exposure-1")
                .isEmpty(),
        )
    }

    private fun attempt(target: String, language: String) = AttemptResponse(
        attemptId = EXERCISE_ID,
        exerciseId = EXERCISE_ID,
        targetSatisfaction = target,
        linguisticStatus = language,
        issues = if (language == "known-issue") listOf(IssueDto("KNOWN_FORM", "Known form")) else emptyList(),
        feedback = "Saved feedback",
        completedSentence = "",
        evaluationRevision = "evaluation-8",
        hintUsed = false,
        retryNumber = 1,
        createdAt = 1,
        responseConstraintStatus = if (target == "unevaluable") null else "met",
    )

    private companion object {
        const val EXERCISE_ID = "00000000-0000-0000-0000-000000000001"
    }
}
