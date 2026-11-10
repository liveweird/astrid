package app.astrid.server

import app.astrid.engine.ModuleOneChoiceRationale
import app.astrid.server.exercises.reproductionFingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotEightWireCompatibilityTest {
    @Test
    fun `pilot eight public wire private source grading and reproduction remain exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotEightWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all formats module one tasks scopes and grading boundaries`() {
        val rows = fixtureRows()
        assertEquals((1..19).map { "E%03d".format(it) }.toSet(), rows.filter { it[0] in setOf("C", "B", "M") }.map { it[1] }.toSet())
        val exercises = PilotEightWireCompatibilityFixture.moduleOneExercises()
        assertEquals(30, exercises.size)
        assertEquals(setOf("E016", "E017", "E018", "E019"), exercises.map { it.formatId }.toSet())
        assertEquals(setOf("human-singular", "human-group", "vaere-present"), exercises.mapNotNull { it.targetScopeId }.toSet())
        assertEquals(30, exercises.map { requireNotNull(it.inspector.moduleOneSource).taskId }.toSet().size)
        val gradeCases = rows.filter { it[0] == "GRADE" }.groupBy { it[3] }.mapValues { it.value.map { row -> row[4] }.toSet() }
        assertEquals(30, gradeCases.size)
        assertTrue(gradeCases.values.all { it == GRADE_CASES })
        assertEquals(30, rows.count { it[0] == "GRADE_REPLAY" })
    }

    @Test
    fun `pair eight fingerprint frames every new scope and private source field`() {
        val exercise = PilotEightWireCompatibilityFixture.moduleOneExercises().first()
        val original = exercise.reproductionFingerprint()
        val inspector = exercise.inspector
        val source = requireNotNull(inspector.moduleOneSource)
        assertTrue(source.knownAlternativeForms.size > 1)
        assertTrue(source.choiceRationales.size > 1)
        val mutations = listOf(
            exercise.reproductionFingerprint(targetScopeIdForFingerprint = "changed-scope"),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(taskId = "changed"))),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(targetLexicalId = "changed"))),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(expectedForm = "changed"))),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(knownAlternativeForms = listOf("changed")))),
            fingerprint(
                exercise,
                inspector.copy(moduleOneSource = source.copy(knownAlternativeForms = source.knownAlternativeForms.reversed())),
            ),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(referenceRole = "changed"))),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(frameVariantId = "changed"))),
            fingerprint(exercise, inspector.copy(moduleOneSource = source.copy(casePolicy = "changed"))),
            fingerprint(
                exercise,
                inspector.copy(
                    moduleOneSource = source.copy(
                        choiceRationales = source.choiceRationales.replaceFirst { it.copy(choice = "changed") },
                    ),
                ),
            ),
            fingerprint(
                exercise,
                inspector.copy(
                    moduleOneSource = source.copy(
                        choiceRationales = source.choiceRationales.replaceFirst { it.copy(rationale = "changed") },
                    ),
                ),
            ),
            fingerprint(
                exercise,
                inspector.copy(moduleOneSource = source.copy(choiceRationales = source.choiceRationales.reversed())),
            ),
            fingerprint(
                exercise,
                inspector.copy(
                    moduleOneSource = source.copy(
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
            "baselineCommit=${PilotEightWireCompatibilityFixture.BASELINE_COMMIT}",
            "releaseSourceCommit=${PilotEightWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.9.0", "captureDate=2026-11-10", "contentRevision=pilot-8",
            "generatorRevision=generator-8", "evaluationRevision=evaluation-8",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }
    private fun ByteArray.sha256() = with(PilotEightWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-8-generator-8-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-8-generator-8-wire.provenance"
        const val ROW_COUNT = 3_518
        const val FIXTURE_SHA256 = "d580c5a296ead61fcfd712705e866cedac1956b2896355175a13ffbf8856bb01"
        val GRADE_CASES = setOf("canonical", "known-wrong", "case", "blank", "unknown", "unicode", "multiword", "wrong-kind")
    }
}
