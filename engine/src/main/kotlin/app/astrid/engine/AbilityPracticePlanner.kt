package app.astrid.engine

internal class AbilityPracticePlanner(private val engine: PilotEngine) {
    fun plan(seed: Int): PracticePlan {
        val exercises = TASK_IDS.map { taskId -> exercise(taskId, seed) }
        return PracticePlan(
            FOCUS_ID,
            PLAN_REVISION,
            seed,
            TITLE,
            PREREQUISITES,
            AbilityContent.introduction(),
            exercises.mapIndexed(::practiceItem),
            CONTENT_REVISION,
            GENERATOR_REVISION,
            EVALUATION_REVISION,
            AbilityContent.introductionExposureKeys(),
            true,
            ModuleOnePracticePlanner.COMPLETION_RULE_ID,
            ModuleOnePracticePlanner.EXPOSURE_POLICY_ID,
        )
    }

    private fun exercise(taskId: String, seed: Int): GeneratedExercise {
        val formatId = if ("-select-" in taskId) "E026" else "E027"
        return (0 until SCAN_SIZE).asSequence().map { index ->
            engine.generate(GenerationRequest(formatId, "G030", seed, index, CONTENT_REVISION, GENERATOR_REVISION))
        }.first { generated -> (generated.source as AbilityExerciseSource).task.id == taskId }
    }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as AbilityExerciseSource).task
        val mode = if (exercise.formatId == "E026") "selection" else "recall"
        val descriptor = PracticeTargetDescriptor(
            if (mode == "selection") "Bare infinitive after kan — selection" else "Bare infinitive after kan — recall",
            "G030",
            null,
            mode,
            AbilityContent.SCOPE_ID,
        )
        return PracticePlanItem(
            position,
            exercise,
            descriptor,
            task.answerExposureKeys,
            task.promptExposureKeys.distinct(),
            task.lessonExposureKeys.distinct(),
            task.hintExposureKeys.distinct(),
            task.feedbackExposureKeys.distinct(),
            listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    companion object {
        const val FOCUS_ID = "module-one-ability"
        const val PLAN_REVISION = "module-one-ability-1"
        const val TITLE = "Using a verb after kan"
        const val DESCRIPTION = "Choose and type the bare infinitive læse after kan."
        const val ITEM_COUNT = 4
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        private const val CONTENT_REVISION = "pilot-13"
        private const val GENERATOR_REVISION = "generator-13"
        private const val EVALUATION_REVISION = "evaluation-13"
        private const val SCAN_SIZE = 2
        private val TASK_IDS = listOf(
            "ability-select-letter",
            "ability-select-danish",
            "ability-recall-letter",
            "ability-recall-danish",
        )
    }
}
