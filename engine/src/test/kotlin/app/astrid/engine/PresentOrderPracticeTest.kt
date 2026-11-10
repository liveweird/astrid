package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PresentOrderPracticeTest {
    private val planner = PracticePlanner()

    @Test
    fun `new profiles pin exact operations teaching policies and target keys`() {
        val profiles = planner.profiles().takeLast(2)
        assertEquals(listOf("module-one-present", "module-one-order"), profiles.map { it.focusId })
        assertEquals(listOf(6, 6), profiles.map { it.itemCount })

        val present = planner.plan("module-one-present", "module-one-present-1", 47)
        assertEquals(listOf("E022", "E022", "E022", "E023", "E023", "E023"), present.items.map { it.exercise.formatId })
        assertEquals(
            listOf("selection", "selection", "selection", "recall", "recall", "recall"),
            present.items.map { it.targetDescriptor.assessmentMode },
        )
        assertEquals("module-one-known-outcome-1", present.completionRuleId)
        assertEquals("module-one-exposure-1", present.exposurePolicyId)
        assertTrue(present.introductionRequired)
        assertTrue(present.introduction.joinToString().contains("Du arbejder."))

        val order = planner.plan("module-one-order", "module-one-order-1", 47)
        assertEquals(listOf("E024", "E024", "E024", "E025", "E025", "E025"), order.items.map { it.exercise.formatId })
        assertEquals(
            listOf("constituent-order", "constituent-order", "constituent-order", "word-order", "word-order", "word-order"),
            order.items.map { it.targetDescriptor.assessmentMode },
        )
        assertEquals("module-one-order-outcome-1", order.completionRuleId)
        assertEquals("module-one-order-exposure-1", order.exposurePolicyId)
        assertTrue(order.introduction.joinToString().contains("three constituents and four written words"))
        assertTrue(order.items.all { it.assessments.single().targetDescriptor == it.targetDescriptor })
    }

    @Test
    fun `order completion and disclosure allow only canonical or exact bounded exceptions`() {
        val canonical = observation(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        val generic = observation(TargetStatus.MET, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET, "OTHER_UNKNOWN")
        val wrongOrder = observation(
            TargetStatus.NOT_MET, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
            "REQUESTED_ORDER_CONSTRAINT",
        )
        val missing = observation(
            TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
            "REQUIRED_WORDS_MISSING",
        )
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", canonical))
        assertFalse(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", generic))
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", wrongOrder))
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", missing))
        assertFalse(PracticeCompletionPolicy.canAdvance("module-one-order-outcome-1", null))

        val plan = planner.plan("module-one-order", "module-one-order-1", 47)
        val item = plan.items.first()
        fun released(observation: PracticeCompletionAttemptObservation) = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(
                PracticeFeedbackAttemptObservation(
                    observation.targetStatus, emptyList(), observation.linguisticStatus,
                    observation.responseConstraintStatus, observation.issueCodes,
                ),
            ),
            "module-one-order-exposure-1",
        )
        assertEquals(item.feedbackExposureKeys, released(canonical))
        assertTrue(released(generic).isEmpty())
        assertEquals(item.feedbackExposureKeys, released(wrongOrder))
        assertEquals(item.feedbackExposureKeys, released(missing))
    }

    @Test
    fun `attempt context records pattern form component and exact-order exposure independently`() {
        val present = planner.plan("module-one-present", "module-one-present-1", 47)
        val presentItem = present.items.first()
        val presentContext = PracticeAttemptContextCalculator.calculate(
            presentItem,
            true,
            present.items.map { item ->
                PracticeItemExposureObservation(
                    item,
                    promptReleased = item.position == presentItem.position,
                    lessonOpened = item.position == presentItem.position,
                    hintOpened = item.position == presentItem.position,
                    feedbackReleased = item.position == presentItem.position,
                )
            },
            introductionExposureKeys = present.introductionExposureKeys,
        )
        assertTrue(presentContext.priorExposure.any {
            it.source == PracticeExposureSource.INTRODUCTION && PresentOrderContent.PRESENT_PATTERN_KEY in it.keys
        })
        assertEquals(
            listOf(PresentOrderContent.PRESENT_PATTERN_KEY),
            presentContext.priorExposure.single { it.source == PracticeExposureSource.HINT }.keys,
        )
        assertEquals(presentItem.feedbackExposureKeys, presentContext.priorExposure.single {
            it.source == PracticeExposureSource.FEEDBACK
        }.keys)

        val order = planner.plan("module-one-order", "module-one-order-1", 47)
        val orderItem = order.items.first { it.exercise.formatId == "E025" }
        val earlier = order.items.first()
        val orderContext = PracticeAttemptContextCalculator.calculate(
            orderItem,
            true,
            order.items.map { item ->
                PracticeItemExposureObservation(
                    item,
                    promptReleased = item.position == earlier.position || item.position == orderItem.position,
                    lessonOpened = item.position == orderItem.position,
                    hintOpened = item.position == orderItem.position,
                    feedbackReleased = item.position == orderItem.position,
                )
            },
            introductionExposureKeys = order.introductionExposureKeys,
        )
        assertTrue(orderItem.promptExposureKeys.all { it in orderItem.answerExposureKeys })
        assertTrue(PresentOrderContent.ORDER_PATTERN_KEY in orderItem.answerExposureKeys)
        assertEquals(
            listOf(PresentOrderContent.ORDER_PATTERN_KEY),
            orderContext.priorExposure.single { it.source == PracticeExposureSource.HINT }.keys,
        )
        assertEquals(orderItem.feedbackExposureKeys, orderContext.priorExposure.single {
            it.source == PracticeExposureSource.FEEDBACK
        }.keys)
    }

    private fun observation(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus,
        vararg issues: String,
    ) = PracticeCompletionAttemptObservation(target, linguistic, constraint, issues.toList())
}
