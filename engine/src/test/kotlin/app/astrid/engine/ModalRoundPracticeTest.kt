package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModalRoundPracticeTest {
    private val engine = PilotEngine()
    private val planner = ModalRoundPracticePlanner(engine)

    @Test
    fun `the three guided modal rounds use all answer positions for each seed`() {
        for (seed in listOf(0, 1, 47, Int.MAX_VALUE)) {
            val positions = ModalRoundContent.definitions.map { definition ->
                val exercise = planner.plan(definition.focusId, seed).items.first().exercise
                exercise.prompt.choices.indexOf(definition.selectedMeaning)
            }
            assertEquals(setOf(0, 1, 2), positions.toSet(), "seed $seed")
        }
        ModalRoundContent.definitions.forEach { definition ->
            val positions = (0..16).map { seed ->
                val exercise = planner.plan(definition.focusId, seed).items.first().exercise
                exercise.prompt.choices.indexOf(definition.selectedMeaning)
            }
            assertEquals(setOf(0, 1, 2), positions.toSet(), definition.focusId)
        }
    }

    @Test
    fun `three required-teaching profiles pin interpretation then construction`() {
        assertEquals(
            listOf("module-one-volition", "module-one-obligation", "module-one-permission"),
            ModalRoundPracticePlanner.profiles().map { it.focusId },
        )
        assertEquals(
            listOf("Expressing an intention", "Understanding a requirement", "Understanding permission"),
            ModalRoundPracticePlanner.profiles().map { it.title },
        )
        assertTrue(ModalRoundPracticePlanner.profiles().all { it.itemCount == 2 })
        ModalRoundContent.definitions.forEach { definition ->
            val plan = planner.plan(definition.focusId, 47)
            assertEquals("${definition.focusId}-1", plan.planRevision)
            assertEquals(definition.title, plan.title)
            assertEquals(2, plan.items.size)
            assertEquals(listOf("E028", "E025"), plan.items.map { it.exercise.formatId })
            assertEquals(listOf("G030", "G005"), plan.items.map { it.targetDescriptor.targetSkillId })
            assertEquals(listOf("selection", "word-order"), plan.items.map { it.targetDescriptor.assessmentMode })
            assertEquals(
                listOf("modal-${definition.key}-meaning", "supplied-modal-order"),
                plan.items.map { it.targetDescriptor.targetScopeId },
            )
            assertEquals("pilot-14", plan.contentRevision)
            assertEquals("generator-14", plan.generatorRevision)
            assertEquals("evaluation-14", plan.evaluationRevision)
            assertEquals(ModalRoundPolicies.COMPLETION_RULE_ID, plan.completionRuleId)
            assertEquals(ModalRoundPolicies.EXPOSURE_POLICY_ID, plan.exposurePolicyId)
            assertTrue(plan.introductionRequired)
            assertEquals(ModalRoundContent.introduction(definition), plan.introduction)
            assertEquals(
                ModalRoundContent.introductionExposureKeys(definition).distinct(),
                plan.introductionExposureKeys,
            )
            assertTrue(plan.items.all { it.assessments.single().assessmentRole == "primary" })
        }
    }

    @Test
    fun `worked examples are distinct from both assessed cards`() {
        ModalRoundContent.definitions.forEach { definition ->
            val plan = planner.plan(definition.focusId, 47)
            val assessed = plan.items.mapNotNull { it.exercise.inspector.canonicalSentence }.toSet()
            assertTrue(definition.workedDanish in plan.introduction.map { it.body })
            assertFalse(definition.workedDanish in assessed)
            assertEquals(2, assessed.size)
        }
        assertEquals(
            setOf("Jeg vil læse en bog.", "Du skal læse et brev.", "Han må læse en avis."),
            ModalRoundContent.definitions.map { it.workedDanish }.toSet(),
        )
    }

    @Test
    fun `answer evidence excludes supplied modal support and separates meaning from exact order`() {
        ModalRoundContent.definitions.forEach { definition ->
            val plan = planner.plan(definition.focusId, 47)
            val interpretation = plan.items[0]
            val construction = plan.items[1]
            val meaningKey = ModalRoundContent.meaningKey(definition)
            val modalPresent = ModalRoundContent.presentKey(definition.modal.lexicalId)
            val modalInfinitive = ModalRoundContent.infinitiveKey(definition.modal.lexicalId)

            assertEquals(listOf(meaningKey), interpretation.answerExposureKeys)
            assertFalse(modalPresent in interpretation.answerExposureKeys)
            assertFalse(modalInfinitive in interpretation.answerExposureKeys)
            assertTrue(modalPresent in interpretation.promptExposureKeys)
            assertEquals(
                ModalRoundContent.introductionExposureKeys(definition).distinct(),
                interpretation.lessonExposureKeys,
            )
            assertEquals(listOf(meaningKey), interpretation.feedbackExposureKeys)

            assertEquals(
                listOf(
                    ModalRoundContent.exactOrderKey(definition.constructionTask.id),
                    ModalRoundContent.ORDER_PATTERN_KEY,
                ),
                construction.answerExposureKeys,
            )
            assertEquals(construction.answerExposureKeys, construction.feedbackExposureKeys)
            assertFalse(meaningKey in construction.answerExposureKeys)
            assertFalse(modalPresent in construction.answerExposureKeys)
            assertTrue(modalPresent in construction.promptExposureKeys)
            assertEquals(
                ModalRoundContent.introductionExposureKeys(definition).distinct(),
                construction.lessonExposureKeys,
            )
            assertEquals("G005", construction.assessments.single().targetDescriptor.targetSkillId)
            assertFalse(construction.assessments.any { it.targetDescriptor.targetSkillId == "G030" })
        }
    }

    @Test
    fun `mixed completion policy accepts known meanings and only named order exceptions`() {
        assertTrue(
            ModalRoundPolicies.canAdvance(
                completion(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET),
            ),
        )
        assertTrue(
            ModalRoundPolicies.canAdvance(
                completion(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET),
            ),
        )
        listOf("REQUESTED_ORDER_CONSTRAINT", "REQUIRED_WORDS_MISSING").forEach { issue ->
            assertTrue(
                ModalRoundPolicies.canAdvance(
                    completion(
                        TargetStatus.UNEVALUABLE,
                        LinguisticStatus.UNKNOWN,
                        ResponseConstraintStatus.NOT_MET,
                        issue,
                    ),
                ),
                issue,
            )
        }
        assertFalse(
            ModalRoundPolicies.canAdvance(
                completion(
                    TargetStatus.UNEVALUABLE,
                    LinguisticStatus.UNKNOWN,
                    ResponseConstraintStatus.NOT_MET,
                    "ANSWER_TYPE_MISMATCH",
                ),
            ),
        )
        assertFalse(ModalRoundPolicies.canAdvance(null))
    }

    @Test
    fun `mixed exposure policy releases only when actual feedback discloses the operation answer`() {
        val keys = listOf("meaning", "exact-order")
        assertEquals(
            keys,
            ModalRoundPolicies.releasedKeys(
                keys,
                listOf(feedback(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)),
            ),
        )
        assertEquals(
            keys,
            ModalRoundPolicies.releasedKeys(
                keys,
                listOf(
                    feedback(
                        TargetStatus.MET,
                        LinguisticStatus.UNKNOWN,
                        ResponseConstraintStatus.NOT_MET,
                        "REQUESTED_ORDER_CONSTRAINT",
                    ),
                ),
            ),
        )
        assertEquals(
            keys,
            ModalRoundPolicies.releasedKeys(
                keys,
                listOf(
                    feedback(
                        TargetStatus.UNEVALUABLE,
                        LinguisticStatus.UNKNOWN,
                        ResponseConstraintStatus.NOT_MET,
                        "REQUIRED_WORDS_MISSING",
                    ),
                ),
            ),
        )
        assertTrue(
            ModalRoundPolicies.releasedKeys(
                keys,
                listOf(
                    feedback(
                        TargetStatus.UNEVALUABLE,
                        LinguisticStatus.UNKNOWN,
                        ResponseConstraintStatus.NOT_MET,
                        "ANSWER_TYPE_MISMATCH",
                    ),
                ),
            ).isEmpty(),
        )
    }

    private fun completion(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus,
        vararg issues: String,
    ) = PracticeCompletionAttemptObservation(target, linguistic, constraint, issues.toList())

    private fun feedback(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus,
        vararg issues: String,
    ) = PracticeFeedbackAttemptObservation(
        targetStatus = target,
        linguisticStatus = linguistic,
        responseConstraintStatus = constraint,
        issueCodes = issues.toList(),
    )
}
