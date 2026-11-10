package app.astrid.server

import app.astrid.engine.ModuleOneChoiceRationale
import app.astrid.server.exercises.reproductionFingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotNineWireCompatibilityTest {
    @Test
    fun `pilot nine public wire private source grading and reproduction remain exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotNineWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all formats module one tasks scopes and grading boundaries`() {
        val rows = fixtureRows()
        assertEquals((1..21).map { "E%03d".format(it) }.toSet(), rows.filter { it[0] in setOf("C", "B", "M") }.map { it[1] }.toSet())
        val exercises = PilotNineWireCompatibilityFixture.authoredExercises()
        assertEquals(34, exercises.size)
        assertEquals(setOf("E016", "E017", "E018", "E019", "E020", "E021"), exercises.map { it.formatId }.toSet())
        assertEquals(
            setOf("human-singular", "human-group", "vaere-present", "inanimate-singular-subject"),
            exercises.mapNotNull { it.targetScopeId }.toSet(),
        )
        val taskIds = exercises.map { it.inspector.moduleOneSource?.taskId ?: it.inspector.inanimateReferenceSource?.taskId }
        assertEquals(34, taskIds.toSet().size)
        val gradeCases = rows.filter { it[0] == "GRADE" }.groupBy { it[3] }.mapValues { it.value.map { row -> row[4] }.toSet() }
        assertEquals(34, gradeCases.size)
        assertTrue(gradeCases.values.all { it == GRADE_CASES })
        assertEquals(34, rows.count { it[0] == "GRADE_REPLAY" })
    }

    @Test
    fun `pair nine fingerprint frames every new scope and private source field`() {
        val exercise = PilotNineWireCompatibilityFixture.authoredExercises().first { it.formatId == "E020" }
        val original = exercise.reproductionFingerprint()
        val inspector = exercise.inspector
        val source = requireNotNull(inspector.inanimateReferenceSource)
        assertTrue(source.knownAlternativeForms.isNotEmpty())
        assertTrue(source.choiceRationales.size > 1)
        val mutations = listOf(
            exercise.reproductionFingerprint(targetScopeIdForFingerprint = "changed-scope"),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(taskId = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(targetEntryId = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(targetSenseId = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(expectedForm = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(knownAlternativeForms = listOf("changed")))),
            fingerprint(
                exercise,
                inspector.copy(inanimateReferenceSource = source.copy(knownAlternativeForms = source.knownAlternativeForms + "changed")),
            ),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(antecedentEntryId = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(antecedentSenseId = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(antecedentArticle = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(antecedentGender = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(antecedentNumber = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(requestedRole = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(casePolicy = "changed"))),
            fingerprint(exercise, inspector.copy(inanimateReferenceSource = source.copy(contextVariantId = "changed"))),
            fingerprint(
                exercise,
                inspector.copy(
                    inanimateReferenceSource = source.copy(
                        choiceRationales = source.choiceRationales.replaceFirst { it.copy(choice = "changed") },
                    ),
                ),
            ),
            fingerprint(
                exercise,
                inspector.copy(
                    inanimateReferenceSource = source.copy(
                        choiceRationales = source.choiceRationales.replaceFirst { it.copy(rationale = "changed") },
                    ),
                ),
            ),
            fingerprint(
                exercise,
                inspector.copy(inanimateReferenceSource = source.copy(choiceRationales = source.choiceRationales.reversed())),
            ),
            fingerprint(
                exercise,
                inspector.copy(
                    inanimateReferenceSource = source.copy(
                        choiceRationales = listOf(ModuleOneChoiceRationale("changed", "changed")),
                    ),
                ),
            ),
        )
        mutations.forEach { assertNotEquals(original, it) }
        assertEquals(mutations.size, mutations.toSet().size)
    }

    private fun fingerprint(
        exercise: app.astrid.engine.GeneratedExercise,
        inspector: app.astrid.engine.ExerciseInspector,
    ) = exercise.reproductionFingerprint(inspectorForFingerprint = inspector)

    private fun List<ModuleOneChoiceRationale>.replaceFirst(
        transform: (ModuleOneChoiceRationale) -> ModuleOneChoiceRationale,
    ) = mapIndexed { index, rationale -> if (index == 0) transform(rationale) else rationale }

    @Test
    fun `provenance pins release source revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "baselineCommit=${PilotNineWireCompatibilityFixture.BASELINE_COMMIT}",
            "releaseSourceCommit=${PilotNineWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.10.0", "captureDate=2026-11-10", "contentRevision=pilot-9",
            "generatorRevision=generator-9", "evaluationRevision=evaluation-9",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }
    private fun ByteArray.sha256() = with(PilotNineWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-9-generator-9-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-9-generator-9-wire.provenance"
        const val ROW_COUNT = 3578
        const val FIXTURE_SHA256 = "0d8922c87ac8d9e81fb4e85cea87f7f37b135354cb40f854884ca68c1a91b771"
        val GRADE_CASES = setOf("canonical", "known-wrong", "case", "blank", "unknown", "unicode", "multiword", "wrong-kind")
    }
}
