package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PracticeGapFollowUpPlannerTest {
    private val practice = PracticePlanner().plan("verb-article-gaps", "verb-article-gaps-1", 47)
    private val source = practice.items.map { PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments) }
    private val planner = PracticeFollowUpPlanner()

    @Test
    fun `guided follow up uses only selected targets with explicit article recall transition`() {
        val cases = listOf(
            listOf("grammar:G004:selection") to listOf("E001", "E001", "E001", "E001"),
            listOf("grammar:G006:selection") to listOf("E002", "E002", "E002", "E002"),
            listOf("grammar:G004:selection", "grammar:G006:selection") to listOf("E001", "E002", "E001", "E002"),
        )
        cases.forEach { (selected, expectedFormats) ->
            val plan = planner.planGuidedGaps("follow-up-3", 0, selected, source)
            assertEquals(expectedFormats, plan.items.map { it.exercise.formatId })
            assertEquals(4, plan.items.size)
            assertEquals(4, plan.items.map { it.exercise.inspector.canonicalSentenceId }.distinct().size)
            assertTrue(plan.items.none { child ->
                child.exercise.inspector.canonicalSentenceId in source.map { it.exercise.inspector.canonicalSentenceId }
            })
            assertEquals(listOf("pilot-7", "generator-7", "evaluation-7"),
                listOf(plan.contentRevision, plan.generatorRevision, plan.evaluationRevision))
            assertTrue(plan.items.all { it.assessments.single().slotId == null })
            plan.items.filter { it.exercise.formatId == "E002" }.forEach {
                assertEquals("grammar:G006:recall", it.targetDescriptor.targetKey())
                assertEquals("Written indefinite article recall", it.targetDescriptor.label)
            }
        }
    }

    @Test
    fun `guided follow up is deterministic excludes immediate children and supports recall chains`() {
        val selected = listOf("grammar:G004:selection", "grammar:G006:selection")
        val first = planner.planGuidedGaps("follow-up-3", 73, selected, source)
        val repeated = planner.planGuidedGaps("follow-up-3", 73, selected, source)
        assertEquals(first.items.map { it.exercise.id }, repeated.items.map { it.exercise.id })

        val nestedSource = first.items.map { PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments) }
        val nested = planner.planGuidedGaps(
            "follow-up-3",
            Int.MAX_VALUE,
            listOf("grammar:G004:selection", "grammar:G006:recall"),
            nestedSource,
        )
        assertEquals(setOf("grammar:G004:selection", "grammar:G006:recall"),
            nested.items.map { it.targetDescriptor.targetKey() }.toSet())
        assertTrue(nested.items.none { child ->
            child.exercise.inspector.canonicalSentenceId in nestedSource.map { it.exercise.inspector.canonicalSentenceId }
        })
    }

    @Test
    fun `guided follow up rejects invalid revisions targets and unfrozen or unpinned sources`() {
        assertFailsWith<IllegalArgumentException> { planner.planGuidedGaps("follow-up-2", 0, listOf("grammar:G004:selection"), source) }
        assertFailsWith<IllegalArgumentException> { planner.planGuidedGaps("follow-up-3", -1, listOf("grammar:G004:selection"), source) }
        assertFailsWith<IllegalArgumentException> { planner.planGuidedGaps("follow-up-3", 0, emptyList(), source) }
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3", 0,
                listOf("grammar:G004:selection", "grammar:G004:selection"), source,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps("follow-up-3", 0, listOf("grammar:G004:selection"), emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps("follow-up-3", 0, listOf("grammar:G006:recall"), source)
        }
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps("follow-up-3", 0, listOf("grammar:G004:selection"), source.map { it.copy(assessments = emptyList()) })
        }
        val wrongRole = source.map { sourceItem ->
            sourceItem.copy(
                assessments = sourceItem.assessments.map { assessment ->
                    if (assessment.targetDescriptor.targetSkillId == "G004") assessment.copy(assessmentRole = "support") else assessment
                },
            )
        }
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps("follow-up-3", 0, listOf("grammar:G004:selection"), wrongRole)
        }
        val firstSource = source.first()
        val verbAssessment = firstSource.assessments.single { it.slotId == "g0" }
        val articleAssessment = firstSource.assessments.single { it.slotId == "g1" }
        val invalidAssessmentMatrices = listOf(
            listOf(verbAssessment.copy(slotId = "other"), articleAssessment),
            listOf(verbAssessment, verbAssessment),
            listOf(verbAssessment),
            listOf(
                verbAssessment.copy(targetDescriptor = articleAssessment.targetDescriptor),
                articleAssessment,
            ),
        )
        invalidAssessmentMatrices.forEach { assessments ->
            assertFailsWith<IllegalArgumentException> {
                planner.planGuidedGaps(
                    "follow-up-3",
                    0,
                    listOf("grammar:G004:selection"),
                    listOf(firstSource.copy(assessments = assessments)),
                )
            }
        }
        val realArticleExercise = PilotEngine().generate(
            GenerationRequest("E002", "G006", 0, 0, "pilot-7", "generator-7"),
        )
        val forgedVerbSource = PracticeFollowUpSourceItem(
            realArticleExercise,
            verbAssessment.targetDescriptor,
            listOf(verbAssessment.copy(slotId = null)),
        )
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3",
                0,
                listOf("grammar:G004:selection"),
                listOf(forgedVerbSource),
            )
        }
        val missingPrimaryDescriptor = firstSource.copy(targetDescriptor = articleAssessment.targetDescriptor)
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3", 0, listOf("grammar:G004:selection"), listOf(missingPrimaryDescriptor),
            )
        }
        val mismatchedExerciseTarget = firstSource.copy(
            exercise = firstSource.exercise.let { original ->
                GeneratedExercise(
                    original.id, original.formatId, "G006", original.lexicalTarget, original.supportSkillIds,
                    original.presentation, original.contentRevision, original.generatorRevision, original.reproduction,
                    original.inspector, original.source, original.readingTarget, original.lexicalTargets,
                )
            },
        )
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3", 0, listOf("grammar:G004:selection"), listOf(mismatchedExerciseTarget),
            )
        }
        val unsupportedExercise = PilotEngine().generate(
            GenerationRequest("E003", "G005", 0, 0, "pilot-7", "generator-7"),
        )
        val unsupportedFormatSource = firstSource.copy(exercise = unsupportedExercise)
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3", 0, listOf("grammar:G004:selection"), listOf(unsupportedFormatSource),
            )
        }
        val pairThree = PracticePlanner().plan("present-verbs", "present-verbs-1", 0).items.first()
        assertFailsWith<IllegalArgumentException> {
            planner.planGuidedGaps(
                "follow-up-3", 0, listOf("grammar:G004:selection"),
                listOf(PracticeFollowUpSourceItem(pairThree.exercise, pairThree.targetDescriptor, pairThree.assessments)),
            )
        }
    }
}
