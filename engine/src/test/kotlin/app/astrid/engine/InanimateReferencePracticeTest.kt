package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InanimateReferencePracticeTest {
    private val planner = PracticePlanner()

    @Test
    fun `things profile pins four pair nine items and the unchanged module one policies`() {
        val profile = planner.profiles().single { it.focusId == "module-one-things" }
        assertEquals("module-one-things-1", profile.planRevision)
        assertEquals("Referring to one thing", profile.title)
        assertEquals(4, profile.itemCount)
        assertEquals(listOf("G001", "G002", "G003", "G006"), profile.prerequisiteSkillIds)
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val plan = planner.plan(profile.focusId, profile.planRevision, seed)
            assertEquals("pilot-9", plan.contentRevision)
            assertEquals("generator-9", plan.generatorRevision)
            assertEquals("evaluation-9", plan.evaluationRevision)
            assertEquals(listOf("E020", "E020", "E021", "E021"), plan.items.map { it.exercise.formatId })
            assertEquals(setOf("den", "det"), plan.items.take(2).map(::expected).toSet())
            assertEquals(setOf("den", "det"), plan.items.takeLast(2).map(::expected).toSet())
            val expectedTaskIds = if (seed % 2 == 0) {
                listOf("e020-book", "e020-letter", "e021-letter", "e021-book")
            } else {
                listOf("e020-letter", "e020-book", "e021-book", "e021-letter")
            }
            assertEquals(
                expectedTaskIds,
                plan.items.map { requireNotNull(it.exercise.inspector.inanimateReferenceSource).taskId },
            )
            assertTrue(plan.introductionRequired)
            assertEquals("module-one-known-outcome-1", plan.completionRuleId)
            assertEquals("module-one-exposure-1", plan.exposurePolicyId)
            assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == "inanimate-singular-subject" })
            assertEquals(
                listOf(
                    "grammar:G038:inanimate-singular-subject:selection",
                    "grammar:G038:inanimate-singular-subject:selection",
                    "grammar:G038:inanimate-singular-subject:recall",
                    "grammar:G038:inanimate-singular-subject:recall",
                ),
                plan.items.map { it.targetDescriptor.targetKey() },
            )
        }
    }

    @Test
    fun `exposure separates reference senses while retaining shared visible det form`() {
        val plan = planner.plan("module-one-things", "module-one-things-1", 0)
        val bothSenseAndFormKeys = listOf(
            "module-one:reference-sense:L026-S01",
            "module-one:reference-sense:L008-S02",
            "module-one:visible-form:den",
            "module-one:visible-form:det",
        )
        val completeLessonKeys = bothSenseAndFormKeys + listOf(
            "module-one:headword:L014-S01",
            "module-one:article:L014",
            "module-one:headword:L018-S01",
            "module-one:article:L018",
            "module-one:identifying-form:L008",
            "module-one:verb-present:L010",
        )
        val introductionKeys = listOf(
            "module-one:headword:L014-S01",
            "module-one:article:L014",
            "module-one:headword:L018-S01",
            "module-one:article:L018",
        ) + bothSenseAndFormKeys + listOf(
            "module-one:identifying-form:L008",
            "module-one:verb-present:L010",
        )
        assertEquals(introductionKeys, requireNotNull(plan.introductionExposureKeys))
        plan.items.filter { it.exercise.formatId == "E020" }.forEach { item ->
            val source = requireNotNull(item.exercise.inspector.inanimateReferenceSource)
            assertEquals(
                listOf(
                    "module-one:headword:${source.antecedentSenseId}",
                    "module-one:article:${source.antecedentEntryId}",
                ) + bothSenseAndFormKeys,
                item.promptExposureKeys,
            )
            assertEquals(completeLessonKeys, item.lessonExposureKeys)
        }
        plan.items.filter { it.exercise.formatId == "E021" }.forEach { item ->
            val source = requireNotNull(item.exercise.inspector.inanimateReferenceSource)
            assertEquals(
                listOf(
                    "module-one:headword:${source.antecedentSenseId}",
                    "module-one:article:${source.antecedentEntryId}",
                ),
                item.promptExposureKeys,
            )
            assertEquals(completeLessonKeys, item.lessonExposureKeys)
        }
        val det = plan.items.first { expected(it) == "det" }
        assertEquals(
            listOf("module-one:reference-sense:L008-S02", "module-one:visible-form:det"),
            det.answerExposureKeys,
        )
        assertEquals(det.answerExposureKeys, det.feedbackExposureKeys)
        val unknown = PracticeFeedbackAttemptObservation(TargetStatus.UNEVALUABLE, linguisticStatus = LinguisticStatus.UNKNOWN)
        assertTrue(
            PracticeFeedbackExposureCalculator.releasedKeys(
                det.feedbackExposureKeys,
                det.assessments,
                listOf(unknown),
                "module-one-exposure-1",
            ).isEmpty(),
        )
        val knownWrong = PracticeFeedbackAttemptObservation(TargetStatus.NOT_MET, linguisticStatus = LinguisticStatus.ACCEPTABLE)
        assertEquals(
            det.feedbackExposureKeys,
            PracticeFeedbackExposureCalculator.releasedKeys(
                det.feedbackExposureKeys,
                det.assessments,
                listOf(knownWrong),
                "module-one-exposure-1",
            ),
        )
    }

    @Test
    fun `latest unknown retry blocks continuation after an eligible first result`() {
        val eligible = PracticeCompletionAttemptObservation(
            TargetStatus.NOT_MET,
            LinguisticStatus.ACCEPTABLE,
            ResponseConstraintStatus.MET,
            listOf("INTRODUCED_REFERENCE_REQUIRED"),
        )
        val unknown = PracticeCompletionAttemptObservation(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.MET,
            listOf("UNRECOGNIZED_REFERENCE_WORD"),
        )
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-known-outcome-1", eligible))
        assertFalse(PracticeCompletionPolicy.canAdvance("module-one-known-outcome-1", unknown))

        val plan = planner.plan("module-one-things", "module-one-things-1", 0)
        val emptyContext = PracticeAttemptContext(false, false, false, emptyList())
        val observations = plan.items.mapIndexed { index, item ->
            PracticeSummaryItemObservation(
                item,
                advanced = false,
                lessonOpened = false,
                hintOpened = false,
                attempts = if (index == 0) {
                    listOf(
                        PracticeSummaryAttemptObservation(
                            eligible.targetStatus,
                            eligible.linguisticStatus,
                            eligible.responseConstraintStatus,
                            emptyContext,
                        ),
                        PracticeSummaryAttemptObservation(
                            unknown.targetStatus,
                            unknown.linguisticStatus,
                            unknown.responseConstraintStatus,
                            emptyContext,
                        ),
                    )
                } else {
                    emptyList()
                },
            )
        }
        val selection = PracticeSummaryCalculator.calculate(plan, true, observations).rows
            .single { it.assessmentMode == "selection" }
        assertEquals(1, selection.first.targetNotMet)
        assertEquals(0, selection.first.unevaluable)
        assertEquals(0, selection.latest.targetNotMet)
        assertEquals(1, selection.latest.unevaluable)
        assertEquals(1, selection.latest.languageUnknown)
    }

    @Test
    fun `pair eight plans and review remain pinned after pair nine activation`() {
        listOf(
            "module-one-singular" to "module-one-singular-1",
            "module-one-groups" to "module-one-groups-1",
            "module-one-er" to "module-one-er-1",
        ).forEach { (focus, revision) ->
            val plan = planner.plan(focus, revision, 47)
            assertEquals("pilot-8", plan.contentRevision)
            assertEquals("generator-8", plan.generatorRevision)
            assertEquals("evaluation-8", plan.evaluationRevision)
        }
        assertEquals(
            "341d0b1a798331397ec1c199ad86dbb17cbb02fd515a4574da472a23a6a488f0",
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(PilotReview.renderPilotEightMarkdown().toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) },
        )
        val current = PilotReview.renderMarkdown()
        assertTrue("| module-one-things | module-one-things-1 | Referring to one thing |" in current)
        assertTrue("L026-S01" in current)
        assertTrue("L008-S02" in current)
    }

    private fun expected(item: PracticePlanItem): String =
        requireNotNull(item.exercise.inspector.inanimateReferenceSource).expectedForm
}
