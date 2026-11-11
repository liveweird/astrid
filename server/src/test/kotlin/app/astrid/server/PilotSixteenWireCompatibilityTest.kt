package app.astrid.server

import app.astrid.engine.GeneratedExercise
import app.astrid.engine.ModuleOneChoiceRationale
import app.astrid.server.exercises.reproductionFingerprint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotSixteenWireCompatibilityTest {
    @Test
    fun `pilot sixteen public wire private source grading and reproduction remain exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotSixteenWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all formats module one tasks scopes and grading boundaries`() {
        val rows = fixtureRows()
        assertEquals(
            (1..30).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            rows.filter { it[0] in setOf("C", "B", "M") }.map { it[1] }.toSet(),
        )
        val exercises = PilotSixteenWireCompatibilityFixture.authoredExercises()
        assertEquals(74, exercises.size)
        assertEquals(
            (16..30).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
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
                "one-final-circumstance",
                "neutral-positive-yes-no",
            ),
            exercises.mapNotNull { it.targetScopeId }.toSet(),
        )
        val taskIds = exercises.map {
            it.inspector.moduleOneSource?.taskId ?: it.inspector.inanimateReferenceSource?.taskId
                ?: it.inspector.canonicalSentenceId
        }
        assertEquals(74, taskIds.toSet().size)
        assertGradeCoverage(rows, exercises)
        assertNewExerciseFamilies(exercises)
    }

    private fun assertGradeCoverage(rows: List<List<String>>, exercises: List<GeneratedExercise>) {
        val gradeCases = rows.filter { it[0] == "GRADE" }
            .groupBy { it[3] }
            .mapValues { it.value.map { row -> row[4] }.toSet() }
        assertEquals(74, gradeCases.size)
        val orderTasks = exercises.filter { it.formatId in setOf("E024", "E025") && it.targetScopeId != "supplied-modal-order" }
            .mapNotNull { it.inspector.canonicalSentenceId }.toSet()
        val modalOrderTasks = exercises.filter { it.targetScopeId == "supplied-modal-order" }
            .mapNotNull { it.inspector.canonicalSentenceId }.toSet()
        gradeCases.forEach { (taskId, cases) ->
            assertTaskGradeCoverage(taskId, cases, exercises, orderTasks, modalOrderTasks)
        }
        assertGradeRowMetadata(rows)
    }

    private fun assertTaskGradeCoverage(
        taskId: String,
        cases: Set<String>,
        exercises: List<GeneratedExercise>,
        orderTasks: Set<String>,
        modalOrderTasks: Set<String>,
    ) {
        val exercise = exercises.single {
            it.inspector.moduleOneSource?.taskId == taskId || it.inspector.inanimateReferenceSource?.taskId == taskId ||
                it.inspector.canonicalSentenceId == taskId
        }
        val expected = when {
            exercise.formatId in setOf("E029", "E030") -> null
            taskId in modalOrderTasks -> MODAL_ORDER_GRADE_CASES
            taskId in orderTasks -> ORDER_GRADE_CASES
            exercise.formatId == "E028" -> MODAL_MEANING_GRADE_CASES
            exercise.formatId == "E026" -> ABILITY_SELECTION_GRADE_CASES
            exercise.formatId == "E027" -> ABILITY_RECALL_GRADE_CASES
            else -> TEXT_GRADE_CASES
        }
        if (expected == null) {
            assertTrue("permutation-0" in cases, taskId)
            assertTrue(setOf("partial", "blank", "duplicate", "unknown-id", "wrong-kind").all(cases::contains), taskId)
        } else {
            assertEquals(expected, cases, taskId)
        }
    }

    private fun assertGradeRowMetadata(rows: List<List<String>>) {
        assertEquals(74, rows.count { it[0] == "GRADE_REPLAY" })
        assertEquals(24, rows.count { it[0] == "API_REJECTION" })
        assertEquals(
            setOf("malformed", "partial", "wrong-kind"),
            rows.filter { it[0] == "API_REJECTION" }.map { it[2] }.toSet(),
        )
        assertTrue(rows.filter { it[0] == "API_REJECTION" }.all { it[3] == "400" && it[4] == "persisted-attempts=0" })
        assertEquals(80, rows.count { it[0] == "GRADE" && it[1] == "E029" })
        assertEquals(40, rows.count { it[0] == "GRADE" && it[1] == "E030" })
        assertEquals(setOf(0, 1, 2), PilotSixteenWireCompatibilityFixture.modalChoicePositions())
    }

    private fun assertNewExerciseFamilies(exercises: List<GeneratedExercise>) {
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
        val circumstance = exercises.filter { it.targetScopeId == "one-final-circumstance" }
        assertEquals(4, circumstance.size)
        assertTrue(circumstance.all { it.formatId == "E029" && it.inspector.constructionId == "C007" })
        val question = exercises.filter { it.targetScopeId == "neutral-positive-yes-no" }
        assertEquals(4, question.size)
        assertTrue(question.all { it.formatId == "E030" && it.inspector.constructionId == "C010" })
    }

    @Test
    fun `pair nine inherited fingerprint frames every retained scope and private source field`() {
        val exercise = PilotSixteenWireCompatibilityFixture.authoredExercises().first { it.formatId == "E020" }
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
        val exercises = PilotSixteenWireCompatibilityFixture.authoredExercises()
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

        val circumstance = exercises.first { it.formatId == "E029" }
        val circumstanceTiles = requireNotNull(circumstance.inspector.tileSource)
        val circumstanceMutations = listOf(
            circumstance.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(circumstance, circumstance.inspector.copy(canonicalSentenceId = "changed")),
            fingerprint(circumstance, circumstance.inspector.copy(constructionId = "changed")),
            fingerprint(
                circumstance,
                circumstance.inspector.copy(tileSource = circumstanceTiles.copy(tokens = circumstanceTiles.tokens.reversed())),
            ),
            circumstance.reproductionFingerprint(
                promptForFingerprint = circumstance.prompt.copy(blocks = circumstance.prompt.blocks.reversed()),
            ),
        )
        circumstanceMutations.forEach { assertNotEquals(circumstance.reproductionFingerprint(), it) }

        val question = exercises.first { it.formatId == "E030" }
        val questionTiles = requireNotNull(question.inspector.tileSource)
        val questionMutations = listOf(
            question.reproductionFingerprint(targetScopeIdForFingerprint = "changed"),
            fingerprint(question, question.inspector.copy(canonicalSentenceId = "changed")),
            fingerprint(question, question.inspector.copy(constructionId = "changed")),
            fingerprint(question, question.inspector.copy(tileSource = questionTiles.copy(tokens = questionTiles.tokens.reversed()))),
            question.reproductionFingerprint(promptForFingerprint = question.prompt.copy(cue = "changed")),
        )
        questionMutations.forEach { assertNotEquals(question.reproductionFingerprint(), it) }
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
            "releaseSourceCommit=${PilotSixteenWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.18.0", "captureDate=2026-11-11", "contentRevision=pilot-16",
            "generatorRevision=generator-16", "evaluationRevision=evaluation-16",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing test resource $path" }
    private fun ByteArray.sha256() = with(PilotSixteenWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-16-generator-16-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-16-generator-16-wire.provenance"
        const val ROW_COUNT = 4_193
        const val FIXTURE_SHA256 = "76a7605da791cf067c5071897cd748fa07d946a08556ca87c8d9b352af14ac18"
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

