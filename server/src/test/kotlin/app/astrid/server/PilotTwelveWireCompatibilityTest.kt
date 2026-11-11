package app.astrid.server

import app.astrid.engine.ModuleOneChoiceRationale
import app.astrid.server.exercises.reproductionFingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotTwelveWireCompatibilityTest {
    @Test
    fun `pilot twelve public wire private source grading and reproduction remain exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotTwelveWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all formats module one tasks scopes and grading boundaries`() {
        val rows = fixtureRows()
        assertEquals((1..25).map { "E%03d".format(it) }.toSet(), rows.filter { it[0] in setOf("C", "B", "M") }.map { it[1] }.toSet())
        val exercises = PilotTwelveWireCompatibilityFixture.authoredExercises()
        assertEquals(56, exercises.size)
        assertEquals((16..25).map { "E%03d".format(it) }.toSet(), exercises.map { it.formatId }.toSet())
        assertEquals(
            setOf(
                "human-singular", "human-group", "vaere-present", "inanimate-singular-subject",
                "named-three-verbs-present", "subject-first-supplied", "have-possession-present",
                "personal-introduction-present",
            ),
            exercises.mapNotNull { it.targetScopeId }.toSet(),
        )
        val taskIds = exercises.map {
            it.inspector.moduleOneSource?.taskId ?: it.inspector.inanimateReferenceSource?.taskId
                ?: it.inspector.canonicalSentenceId
        }
        assertEquals(56, taskIds.toSet().size)
        val gradeCases = rows.filter { it[0] == "GRADE" }.groupBy { it[3] }.mapValues { it.value.map { row -> row[4] }.toSet() }
        assertEquals(56, gradeCases.size)
        val orderTasks = exercises.filter { it.formatId in setOf("E024", "E025") }
            .mapNotNull { it.inspector.canonicalSentenceId }.toSet()
        gradeCases.forEach { (taskId, cases) ->
            assertEquals(if (taskId in orderTasks) ORDER_GRADE_CASES else TEXT_GRADE_CASES, cases, taskId)
        }
        assertEquals(56, rows.count { it[0] == "GRADE_REPLAY" })

        val personal = exercises.filter { it.targetScopeId == "personal-introduction-present" }
        assertEquals(6, personal.size)
        assertEquals(setOf("E022", "E023"), personal.map { it.formatId }.toSet())
        assertEquals(setOf("L028", "L029", "L030"), personal.map { requireNotNull(it.inspector.moduleOneSource).targetLexicalId }.toSet())
        assertEquals(setOf("C002", "C004", "C006"), personal.mapNotNull { it.inspector.constructionId }.toSet())
        assertTrue(personal.any { "L031" in it.inspector.lexicalIds })
        assertTrue(personal.any { "L032" in it.inspector.lexicalIds })
    }

    @Test
    fun `pair nine inherited fingerprint frames every retained scope and private source field`() {
        val exercise = PilotTwelveWireCompatibilityFixture.authoredExercises().first { it.formatId == "E020" }
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

    @Test
    fun `pair twelve fingerprint frames named forms possession supplied order and scopes`() {
        val exercises = PilotTwelveWireCompatibilityFixture.authoredExercises()
        val present = exercises.first { it.formatId == "E022" }
        val presentSource = requireNotNull(present.inspector.moduleOneSource)
        val presentMutations = listOf(
            present.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(present, present.inspector.copy(moduleOneSource = presentSource.copy(taskId = "changed"))),
            fingerprint(present, present.inspector.copy(moduleOneSource = presentSource.copy(expectedForm = "changed"))),
            fingerprint(
                present,
                present.inspector.copy(moduleOneSource = presentSource.copy(choiceRationales = presentSource.choiceRationales.reversed())),
            ),
            present.reproductionFingerprint(promptForFingerprint = present.prompt.copy(choices = present.prompt.choices.reversed())),
        )
        presentMutations.forEach { assertNotEquals(present.reproductionFingerprint(), it) }

        val possession = exercises.first { it.targetScopeId == "have-possession-present" }
        val possessionSource = requireNotNull(possession.inspector.moduleOneSource)
        val possessionMutations = listOf(
            possession.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(
                possession,
                possession.inspector.copy(moduleOneSource = possessionSource.copy(frameVariantId = "changed")),
            ),
            possession.reproductionFingerprint(
                promptForFingerprint = possession.prompt.copy(cue = "changed"),
            ),
            possession.reproductionFingerprint(
                introductionForFingerprint = possession.introduction.reversed(),
            ),
        )
        possessionMutations.forEach { assertNotEquals(possession.reproductionFingerprint(), it) }

        val order = exercises.first { it.formatId == "E025" }
        val tileSource = requireNotNull(order.inspector.tileSource)
        val licensed = tileSource.licensedOrders.single()
        val token = tileSource.tokens.first()
        val orderMutations = listOf(
            order.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(order, order.inspector.copy(tileSource = tileSource.copy(tokens = tileSource.tokens.reversed()))),
            fingerprint(
                order,
                order.inspector.copy(
                    tileSource = tileSource.copy(tokens = listOf(token.copy(form = "changed")) + tileSource.tokens.drop(1)),
                ),
            ),
            fingerprint(
                order,
                order.inspector.copy(
                    tileSource = tileSource.copy(
                        licensedOrders = listOf(licensed.copy(blockIds = licensed.blockIds.reversed())),
                    ),
                ),
            ),
            order.reproductionFingerprint(promptForFingerprint = order.prompt.copy(requiredTileCount = 99)),
        )
        orderMutations.forEach { assertNotEquals(order.reproductionFingerprint(), it) }
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
            "releaseSourceCommit=${PilotTwelveWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.14.0", "captureDate=2026-11-11", "contentRevision=pilot-12",
            "generatorRevision=generator-12", "evaluationRevision=evaluation-12",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }
    private fun ByteArray.sha256() = with(PilotTwelveWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-12-generator-12-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-12-generator-12-wire.provenance"
        const val ROW_COUNT = 3866
        const val FIXTURE_SHA256 = "4876562acb894f474aacaae9561ae04a1c05bdd2007edcc3e4cbed17a46d8a6a"
        val TEXT_GRADE_CASES = setOf("canonical", "known-wrong", "case", "blank", "unknown", "unicode", "multiword", "wrong-kind")
        val ORDER_GRADE_CASES = setOf(
            "canonical", "wrong-order", "partial", "blank", "duplicate", "unknown-id", "wrong-kind-text", "wrong-kind-gaps",
        )
    }
}
