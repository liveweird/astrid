package app.astrid.engine

internal class QuestionPracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, seed: Int): PracticePlan {
        require(focusId == FOCUS_ID) { "Unsupported neutral-question profile: $focusId" }
        val exercisesByTask = (0 until SCAN_SIZE).associate { index ->
            val exercise = engine.generate(
                GenerationRequest(FORMAT_ID, TARGET_SKILL_ID, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
            )
            (exercise.source as QuestionExerciseSource).task.id to exercise
        }
        val items = NeutralQuestionContent.tasks.mapIndexed { position, task ->
            practiceItem(position, exercisesByTask.getValue(task.id))
        }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = PLAN_REVISION,
            seed = seed,
            title = TITLE,
            prerequisiteSkillIds = PREREQUISITES,
            introduction = NeutralQuestionContent.introduction(),
            items = items,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = NeutralQuestionContent.LESSON_EXPOSURE_KEYS,
            introductionRequired = true,
            completionRuleId = PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID,
            exposurePolicyId = PresentOrderPracticePlanner.ORDER_EXPOSURE_POLICY_ID,
        )
    }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as QuestionExerciseSource).task
        val descriptor = PracticeTargetDescriptor(
            label = "Neutral positive yes/no question — constituent order",
            targetSkillId = TARGET_SKILL_ID,
            lexicalTarget = null,
            assessmentMode = "constituent-order",
            targetScopeId = NeutralQuestionContent.TARGET_SCOPE_ID,
        )
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = descriptor,
            answerExposureKeys = task.answerExposureKeys,
            promptExposureKeys = task.promptExposureKeys,
            lessonExposureKeys = task.lessonExposureKeys,
            hintExposureKeys = task.hintExposureKeys,
            feedbackExposureKeys = task.feedbackExposureKeys,
            assessments = listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    companion object {
        const val FOCUS_ID = "module-one-questions"
        const val PLAN_REVISION = "module-one-questions-1"
        const val TITLE = "Asking yes or no questions"
        const val DESCRIPTION =
            "Arrange supplied Danish constituents into neutral positive yes/no questions."
        const val ITEM_COUNT = 4
        const val CONTENT_REVISION = "pilot-16"
        const val GENERATOR_REVISION = "generator-16"
        const val EVALUATION_REVISION = "evaluation-16"
        const val FORMAT_ID = "E030"
        const val TARGET_SKILL_ID = "G018"
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        private const val SCAN_SIZE = 4

        fun profile() = PracticeProfile(
            focusId = FOCUS_ID,
            planRevision = PLAN_REVISION,
            title = TITLE,
            description = DESCRIPTION,
            itemCount = ITEM_COUNT,
            prerequisiteSkillIds = PREREQUISITES,
        )
    }
}
