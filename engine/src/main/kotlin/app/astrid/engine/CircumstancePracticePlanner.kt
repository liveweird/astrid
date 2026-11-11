package app.astrid.engine

internal class CircumstancePracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, seed: Int): PracticePlan {
        require(focusId == FOCUS_ID) { "Unsupported final-circumstance profile: $focusId" }
        val generatedByTask = (0 until SCAN_SIZE).associate { index ->
            val exercise = engine.generate(
                GenerationRequest(FORMAT_ID, TARGET_SKILL_ID, seed, index, CONTENT_REVISION, GENERATOR_REVISION),
            )
            (exercise.source as CircumstanceExerciseSource).task.id to exercise
        }
        val items = FinalCircumstanceContent.tasks.mapIndexed { position, task ->
            practiceItem(position, generatedByTask.getValue(task.id))
        }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = PLAN_REVISION,
            seed = seed,
            title = TITLE,
            prerequisiteSkillIds = PREREQUISITES,
            introduction = FinalCircumstanceContent.introduction(),
            items = items,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = FinalCircumstanceContent.LESSON_EXPOSURE_KEYS,
            introductionRequired = true,
            completionRuleId = PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID,
            exposurePolicyId = PresentOrderPracticePlanner.ORDER_EXPOSURE_POLICY_ID,
        )
    }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as CircumstanceExerciseSource).task
        val descriptor = PracticeTargetDescriptor(
            label = "One final circumstance — constituent order",
            targetSkillId = TARGET_SKILL_ID,
            lexicalTarget = null,
            assessmentMode = "constituent-order",
            targetScopeId = FinalCircumstanceContent.TARGET_SCOPE_ID,
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
        const val FOCUS_ID = "module-one-circumstances"
        const val PLAN_REVISION = "module-one-circumstances-1"
        const val TITLE = "Adding when and where"
        const val DESCRIPTION =
            "Keep a supplied clause core in order and add one supplied time or place circumstance at the end."
        const val ITEM_COUNT = 4
        const val CONTENT_REVISION = "pilot-15"
        const val GENERATOR_REVISION = "generator-15"
        const val EVALUATION_REVISION = "evaluation-15"
        const val FORMAT_ID = "E029"
        const val TARGET_SKILL_ID = "G016"
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
