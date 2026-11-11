package app.astrid.engine

internal class PersonalIntroductionPracticePlanner(private val engine: PilotEngine) {
    fun plan(seed: Int): PracticePlan {
        val exercises = TASK_IDS.map { taskId -> exercise(taskId, seed) }
        return PracticePlan(
            FOCUS_ID, PLAN_REVISION, seed, TITLE, PREREQUISITES, introduction(),
            exercises.mapIndexed(::practiceItem), CONTENT_REVISION, GENERATOR_REVISION, EVALUATION_REVISION,
            INTRODUCTION_EXPOSURE_KEYS, true, ModuleOnePracticePlanner.COMPLETION_RULE_ID,
            ModuleOnePracticePlanner.EXPOSURE_POLICY_ID,
        )
    }

    private fun exercise(taskId: String, seed: Int): GeneratedExercise =
        (0 until SCAN_SIZE).asSequence().flatMap { index ->
            listOf("E022", "E023").asSequence().map { formatId ->
                engine.generate(GenerationRequest(formatId, "G004", seed, index, CONTENT_REVISION, GENERATOR_REVISION))
            }
        }.first { generated -> (generated.source as PresentOrderExerciseSource).task.id == taskId }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as PresentOrderExerciseSource).task
        val mode = if (exercise.formatId == "E022") "selection" else "recall"
        val descriptor = PracticeTargetDescriptor(
            if (mode == "selection") "Personal introductions — selection" else "Personal introductions — recall",
            "G004", null, mode, PresentOrderContent.PERSONAL_SCOPE_ID,
        )
        return PracticePlanItem(
            position, exercise, descriptor, task.answerExposureKeys, task.promptExposureKeys.distinct(),
            task.lessonExposureKeys.distinct(), task.hintExposureKeys.distinct(), task.feedbackExposureKeys.distinct(),
            listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    companion object {
        const val FOCUS_ID = "module-one-personal"
        const val PLAN_REVISION = "module-one-personal-1"
        const val TITLE = "Names, homes and languages"
        const val DESCRIPTION = "Choose and type Danish present forms for names, places and languages."
        const val ITEM_COUNT = 6
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        private const val CONTENT_REVISION = "pilot-12"
        private const val GENERATOR_REVISION = "generator-12"
        private const val EVALUATION_REVISION = "evaluation-12"
        private const val SCAN_SIZE = 8
        private val TASK_IDS = listOf(
            "personal-select-name", "personal-recall-name",
            "personal-select-residence", "personal-recall-residence",
            "personal-select-language", "personal-recall-language",
        )
        val INTRODUCTION_EXPOSURE_KEYS = listOf(PresentOrderContent.PERSONAL_PATTERN_KEY) +
            listOf("L028", "L029", "L030").flatMap { lexicalId ->
                listOf(PresentOrderContent.infinitiveKey(lexicalId), PresentOrderContent.presentKey(lexicalId))
            } + listOf("L031", "L032").map(PresentOrderContent::componentKey)

        private fun introduction() = listOf(
            IntroductionSection(
                "Personal statement frames",
                "After the subject and verb, supply a name, i with a city, or a language. The names in these exercises are fictional.",
            ),
        ) + PersonalIntroductionContent.requiredTeaching() + listOf(
            IntroductionSection("Subject agreement", "Use the same stored present form with each supplied subject."),
            IntroductionSection("Answer form", "Choose or type one lowercase present verb without punctuation."),
        )
    }
}
