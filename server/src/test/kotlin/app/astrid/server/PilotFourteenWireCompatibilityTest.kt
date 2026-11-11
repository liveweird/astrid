package app.astrid.server

import app.astrid.engine.ModuleOneChoiceRationale
import app.astrid.server.exercises.reproductionFingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotFourteenWireCompatibilityTest {
    @Test
    fun `pilot fourteen public wire private source grading and reproduction remain exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotFourteenWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all formats module one tasks scopes and grading boundaries`() {
        val rows = fixtureRows()
        assertEquals(
            (1..28).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            rows.filter { it[0] in setOf("C", "B", "M") }.map { it[1] }.toSet(),
        )
        val exercises = PilotFourteenWireCompatibilityFixture.authoredExercises()
        assertEquals(66, exercises.size)
        assertEquals(
            (16..28).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            exercises.map { it.formatId }.toSet(),
        )
        assertEquals(
            setOf(
                "human-singular", "human-group", "vaere-present", "inanimate-singular-subject",
                "named-three-verbs-present", "subject-first-supplied", "have-possession-present",
                "personal-introduction-present",
                "kan-bare-infinitive",
                "modal-volition-meaning", "modal-obligation-meaning", "modal-permission-meaning",
                "supplied-modal-order",
            ),
            exercises.mapNotNull { it.targetScopeId }.toSet(),
        )
        val taskIds = exercises.map {
            it.inspector.moduleOneSource?.taskId ?: it.inspector.inanimateReferenceSource?.taskId
                ?: it.inspector.canonicalSentenceId
        }
        assertEquals(66, taskIds.toSet().size)
        val gradeCases = rows.filter { it[0] == "GRADE" }.groupBy { it[3] }.mapValues { it.value.map { row -> row[4] }.toSet() }
        assertEquals(66, gradeCases.size)
        val orderTasks = exercises.filter { it.formatId in setOf("E024", "E025") && it.targetScopeId != "supplied-modal-order" }
            .mapNotNull { it.inspector.canonicalSentenceId }.toSet()
        val modalOrderTasks = exercises.filter { it.targetScopeId == "supplied-modal-order" }
            .mapNotNull { it.inspector.canonicalSentenceId }.toSet()
        gradeCases.forEach { (taskId, cases) ->
            val exercise = exercises.single {
                it.inspector.moduleOneSource?.taskId == taskId || it.inspector.inanimateReferenceSource?.taskId == taskId ||
                    it.inspector.canonicalSentenceId == taskId
            }
            val expected = when {
                taskId in modalOrderTasks -> MODAL_ORDER_GRADE_CASES
                taskId in orderTasks -> ORDER_GRADE_CASES
                exercise.formatId == "E028" -> MODAL_MEANING_GRADE_CASES
                exercise.formatId == "E026" -> ABILITY_SELECTION_GRADE_CASES
                exercise.formatId == "E027" -> ABILITY_RECALL_GRADE_CASES
                else -> TEXT_GRADE_CASES
            }
            assertEquals(expected, cases, taskId)
        }
        assertEquals(66, rows.count { it[0] == "GRADE_REPLAY" })
        assertEquals(setOf(0, 1, 2), PilotFourteenWireCompatibilityFixture.modalChoicePositions())

        val personal = exercises.filter { it.targetScopeId == "personal-introduction-present" }
        assertEquals(6, personal.size)
        assertEquals(setOf("E022", "E023"), personal.map { it.formatId }.toSet())
        assertEquals(setOf("L028", "L029", "L030"), personal.map { requireNotNull(it.inspector.moduleOneSource).targetLexicalId }.toSet())
        assertEquals(setOf("C002", "C004", "C006"), personal.mapNotNull { it.inspector.constructionId }.toSet())
        assertTrue(personal.any { "L031" in it.inspector.lexicalIds })
        assertTrue(personal.any { "L032" in it.inspector.lexicalIds })
        val ability = exercises.filter { it.targetScopeId == "kan-bare-infinitive" }
        assertEquals(4, ability.size)
        assertEquals(setOf("E026", "E027"), ability.map { it.formatId }.toSet())
        assertTrue(ability.all { it.inspector.constructionId == "C014" })
        assertTrue(ability.all { setOf("L011", "L033").all(it.inspector.lexicalIds::contains) })
        val modal = exercises.filter { it.inspector.modalSource != null }
        assertEquals(6, modal.size)
        assertEquals(setOf("E025", "E028"), modal.map { it.formatId }.toSet())
        assertTrue(modal.all { it.inspector.constructionId == "C014" })
        assertEquals(setOf("L034", "L035", "L036"), modal.map { requireNotNull(it.inspector.modalSource).modalEntryId }.toSet())
    }

    @Test
    fun `pair nine inherited fingerprint frames every retained scope and private source field`() {
        val exercise = PilotFourteenWireCompatibilityFixture.authoredExercises().first { it.formatId == "E020" }
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
    fun `pair fourteen fingerprint frames named forms possession supplied order and scopes`() {
        val exercises = PilotFourteenWireCompatibilityFixture.authoredExercises()
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

        val modal = exercises.first { it.formatId == "E028" }
        val modalSource = requireNotNull(modal.inspector.modalSource)
        val modalMutations = listOf(
            modal.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(modal, modal.inspector.copy(modalSource = modalSource.copy(taskId = "changed"))),
            fingerprint(modal, modal.inspector.copy(modalSource = modalSource.copy(modalEntryId = "changed"))),
            fingerprint(modal, modal.inspector.copy(modalSource = modalSource.copy(modalSenseId = "changed"))),
            fingerprint(modal, modal.inspector.copy(modalSource = modalSource.copy(expectedInterpretation = "changed"))),
            fingerprint(modal, modal.inspector.copy(modalSource = modalSource.copy(frameVariantId = "changed"))),
            fingerprint(modal, modal.inspector.copy(modalSource = modalSource.copy(contextVariantId = "changed"))),
            modal.reproductionFingerprint(promptForFingerprint = modal.prompt.copy(choices = modal.prompt.choices.reversed())),
        )
        modalMutations.forEach { assertNotEquals(modal.reproductionFingerprint(), it) }
    }

    @Test
    fun `modal construction fingerprint frames its public prompt and pair nine inspector projection`() {
        val exercise = PilotFourteenWireCompatibilityFixture.authoredExercises().first {
            it.formatId == "E025" && it.targetScopeId == "supplied-modal-order"
        }
        val original = exercise.reproductionFingerprint()
        val inspector = exercise.inspector
        val tileSource = requireNotNull(inspector.tileSource)
        val token = tileSource.tokens.first()
        val licensed = tileSource.licensedOrders.single()
        val modalSource = requireNotNull(inspector.modalSource)
        val promptBlock = exercise.prompt.blocks.first()
        val mutations = listOf(
            exercise.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            exercise.reproductionFingerprint(
                promptForFingerprint = exercise.prompt.copy(blocks = exercise.prompt.blocks.reversed()),
            ),
            exercise.reproductionFingerprint(
                promptForFingerprint = exercise.prompt.copy(
                    blocks = listOf(promptBlock.copy(text = "changed")) + exercise.prompt.blocks.drop(1),
                ),
            ),
            exercise.reproductionFingerprint(promptForFingerprint = exercise.prompt.copy(requiredTileCount = 99)),
            fingerprint(exercise, inspector.copy(canonicalSentenceId = "changed")),
            fingerprint(exercise, inspector.copy(constructionId = "changed")),
            fingerprint(exercise, inspector.copy(lexicalIds = inspector.lexicalIds.reversed())),
            fingerprint(exercise, inspector.copy(clauseRoles = inspector.clauseRoles.reversed())),
            fingerprint(exercise, inspector.copy(canonicalSentence = "changed")),
            fingerprint(exercise, inspector.copy(tileSource = tileSource.copy(tokens = tileSource.tokens.reversed()))),
            fingerprint(
                exercise,
                inspector.copy(tileSource = tileSource.copy(tokens = listOf(token.copy(blockId = "changed")) + tileSource.tokens.drop(1))),
            ),
            fingerprint(
                exercise,
                inspector.copy(
                    tileSource = tileSource.copy(
                        tokens = listOf(token.copy(lexicalId = "changed")) + tileSource.tokens.drop(1),
                    ),
                ),
            ),
            fingerprint(
                exercise,
                inspector.copy(tileSource = tileSource.copy(tokens = listOf(token.copy(form = "changed")) + tileSource.tokens.drop(1))),
            ),
            fingerprint(
                exercise,
                inspector.copy(tileSource = tileSource.copy(licensedOrders = listOf(licensed.copy(kind = "changed")))),
            ),
            fingerprint(
                exercise,
                inspector.copy(
                    tileSource = tileSource.copy(licensedOrders = listOf(licensed.copy(blockIds = licensed.blockIds.reversed()))),
                ),
            ),
            fingerprint(exercise, inspector.copy(modalSource = modalSource.copy(taskId = "changed"))),
            fingerprint(exercise, inspector.copy(modalSource = modalSource.copy(modalEntryId = "changed"))),
            fingerprint(exercise, inspector.copy(modalSource = modalSource.copy(modalSenseId = "changed"))),
            fingerprint(exercise, inspector.copy(modalSource = modalSource.copy(expectedInterpretation = "changed"))),
            fingerprint(exercise, inspector.copy(modalSource = modalSource.copy(frameVariantId = "changed"))),
            fingerprint(exercise, inspector.copy(modalSource = modalSource.copy(contextVariantId = "changed"))),
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
            "releaseSourceCommit=${PilotFourteenWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.16.0", "captureDate=2026-11-11", "contentRevision=pilot-14",
            "generatorRevision=generator-14", "evaluationRevision=evaluation-14",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }
    private fun ByteArray.sha256() = with(PilotFourteenWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-14-generator-14-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-14-generator-14-wire.provenance"
        const val ROW_COUNT = 4005
        const val FIXTURE_SHA256 = "df1ae392474b1f7d3bf6cb34b99104bfca2e998dea76dbe6103d4d7bce19d4d6"
        val TEXT_GRADE_CASES = setOf("canonical", "known-wrong", "case", "blank", "unknown", "unicode", "multiword", "wrong-kind")
        val ORDER_GRADE_CASES = setOf(
            "canonical", "wrong-order", "partial", "blank", "duplicate", "unknown-id", "wrong-kind-text", "wrong-kind-gaps",
        )
        val ABILITY_SELECTION_GRADE_CASES = setOf(
            "canonical", "normalized", "at-marker-selected", "finite", "case", "blank", "unknown", "multiword", "wrong-kind",
        )
        val ABILITY_RECALL_GRADE_CASES = setOf(
            "canonical", "normalized", "at-marker-typed", "finite", "case", "blank", "unknown", "multiword", "wrong-kind",
        )
        val MODAL_MEANING_GRADE_CASES = setOf(
            "canonical", "normalized", "known-wrong", "blank", "unknown", "wrong-kind",
        )
        val MODAL_ORDER_GRADE_CASES = ORDER_GRADE_CASES + "np-reversal"
    }
}
