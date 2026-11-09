package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PracticeFeedbackExposureCalculatorTest {
    private val plan = PracticePlanner().plan("verb-article-gaps", "verb-article-gaps-1", 0)
    private val item = plan.items.first()
    private val engine = PilotEngine()

    @Test
    fun `each gap releases only its own answer after correct or wrong feedback`() {
        val source = requireNotNull(item.exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val correctVerb = source.getValue("g0").expectedChoiceId
        val wrongArticle = requireNotNull(item.exercise.prompt.gapPrompt).slots.single { it.id == "g1" }
            .choices.single { it.id != source.getValue("g1").expectedChoiceId }.id
        val mixed = engine.evaluate(item.exercise, Answer.Gaps(listOf(GapSelection("g0", correctVerb), GapSelection("g1", wrongArticle))))
        val missingArticle = engine.evaluate(item.exercise, Answer.Gaps(listOf(GapSelection("g0", correctVerb))))
        val candidates = item.feedbackExposureKeys

        assertEquals(candidates, released(mixed))
        assertEquals(
            candidates.filterNot { it == item.assessments.single { assessment -> assessment.slotId == "g1" }.answerExposureKeys.single() },
            released(missingArticle),
        )
    }

    @Test
    fun `single target and legacy feedback preserve their distinct release rules`() {
        val single = listOf(PracticeAssessment(null, item.assessments.first().targetDescriptor, "primary", listOf("verb-present:L009")))
        assertEquals(
            listOf("grammar:G004"),
            PracticeFeedbackExposureCalculator.releasedKeys(
                listOf("verb-present:L009", "grammar:G004"), single,
                listOf(PracticeFeedbackAttemptObservation(TargetStatus.NOT_MET)),
            ),
        )
        assertEquals(
            listOf("verb-present:L009", "grammar:G004"),
            PracticeFeedbackExposureCalculator.releasedKeys(
                listOf("verb-present:L009", "grammar:G004"), emptyList(),
                listOf(PracticeFeedbackAttemptObservation(TargetStatus.MET)),
            ),
        )
        assertEquals(
            listOf("grammar:G004"),
            PracticeFeedbackExposureCalculator.releasedKeys(
                listOf("verb-present:L009", "grammar:G004"), emptyList(),
                listOf(PracticeFeedbackAttemptObservation(TargetStatus.NOT_MET)),
            ),
        )
    }

    @Test
    fun `mismatched saved gap identity is rejected instead of releasing another slot`() {
        val evaluation = engine.evaluate(item.exercise, Answer.Gaps(emptyList()))
        val changed = requireNotNull(evaluation.gapResults).map { result ->
            if (result.slotId == "g0") result.copy(targetSkillId = "G006") else result
        }
        assertFailsWith<IllegalArgumentException> {
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                item.assessments,
                listOf(PracticeFeedbackAttemptObservation(evaluation.targetStatus, changed)),
            )
        }
    }

    @Test
    fun `invalid frozen assessment metadata and incomplete outcomes are rejected`() {
        val first = item.assessments.first()
        val lexical = first.copy(
            targetDescriptor = PracticeTargetDescriptor(
                "lexical", null, LexicalTarget("L014", "L014-S01", "meaning", "da-en"),
            ),
        )
        val invalidAssessments = listOf(
            listOf(first, first),
            listOf(first.copy(assessmentRole = "other")),
            listOf(lexical),
        )
        invalidAssessments.forEach { assessments ->
            assertFailsWith<IllegalArgumentException> {
                PracticeFeedbackExposureCalculator.releasedKeys(emptyList(), assessments, emptyList())
            }
        }

        val missing = PracticeFeedbackAttemptObservation(TargetStatus.UNEVALUABLE, emptyList())
        assertFailsWith<IllegalArgumentException> {
            PracticeFeedbackExposureCalculator.releasedKeys(item.feedbackExposureKeys, item.assessments, listOf(missing))
        }
        val single = listOf(PracticeAssessment(null, first.targetDescriptor, "primary", first.answerExposureKeys))
        val gapResult = requireNotNull(engine.evaluate(item.exercise, Answer.Gaps(emptyList())).gapResults).first()
        assertFailsWith<IllegalArgumentException> {
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                single,
                listOf(PracticeFeedbackAttemptObservation(TargetStatus.MET, listOf(gapResult))),
            )
        }
    }

    private fun released(evaluation: Evaluation): List<String> =
        PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(PracticeFeedbackAttemptObservation(evaluation.targetStatus, requireNotNull(evaluation.gapResults))),
        )
}
