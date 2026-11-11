package app.astrid.engine

internal class PossessionPracticePlanner(private val engine: PilotEngine) {
    fun plan(seed: Int): PracticePlan {
        val exercises = TASK_IDS.mapIndexed { position, taskId -> exercise(taskId, seed, position) }
        return PracticePlan(
            FOCUS_ID, PLAN_REVISION, seed, TITLE, PREREQUISITES, introduction(),
            exercises.mapIndexed(::practiceItem), CONTENT_REVISION, GENERATOR_REVISION, EVALUATION_REVISION,
            INTRODUCTION_EXPOSURE_KEYS, true, ModuleOnePracticePlanner.COMPLETION_RULE_ID,
            ModuleOnePracticePlanner.EXPOSURE_POLICY_ID,
        )
    }

    private fun exercise(taskId: String, seed: Int, position: Int): GeneratedExercise =
        (0 until SCAN_SIZE).asSequence().map { index ->
            val formatId = if (position < SELECTION_COUNT) "E022" else "E023"
            engine.generate(GenerationRequest(formatId, "G004", seed, index, CONTENT_REVISION, GENERATOR_REVISION))
        }.first { generated -> (generated.source as PresentOrderExerciseSource).task.id == taskId }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as PresentOrderExerciseSource).task
        val mode = if (exercise.formatId == "E022") "selection" else "recall"
        val descriptor = PracticeTargetDescriptor(
            if (mode == "selection") "Possession with har — selection" else "Possession with har — recall",
            "G004", null, mode, PresentOrderContent.POSSESSION_SCOPE_ID,
        )
        return PracticePlanItem(
            position, exercise, descriptor, task.answerExposureKeys, task.promptExposureKeys.distinct(),
            task.lessonExposureKeys.distinct(), task.hintExposureKeys.distinct(), task.feedbackExposureKeys.distinct(),
            listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    companion object {
        const val FOCUS_ID = "module-one-possession"
        const val PLAN_REVISION = "module-one-possession-1"
        const val TITLE = "Saying what someone has"
        const val DESCRIPTION = "Choose and type the Danish present form har for possession."
        const val ITEM_COUNT = 4
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        private const val CONTENT_REVISION = "pilot-11"
        private const val GENERATOR_REVISION = "generator-11"
        private const val EVALUATION_REVISION = "evaluation-11"
        private const val SCAN_SIZE = 5
        private const val SELECTION_COUNT = 2
        private val TASK_IDS = listOf(
            "possession-select-letter", "possession-select-newspaper", "possession-recall-book", "possession-recall-letter",
        )
        val INTRODUCTION_EXPOSURE_KEYS = listOf(
            PresentOrderContent.POSSESSION_PATTERN_KEY,
            PresentOrderContent.infinitiveKey("L009"),
            PresentOrderContent.presentKey("L009"),
            PresentOrderContent.componentKey("L001"),
            PresentOrderContent.componentKey("L022"),
            PresentOrderContent.componentKey("L014"),
        )

        private fun introduction() = listOf(
            IntroductionSection("Dictionary and present forms", "at have → har", "da"),
            IntroductionSection("Meaning", "The verb expresses possession here. Use the same present form with each subject."),
            IntroductionSection("Worked example", "Jeg har en bog.", "da"),
            IntroductionSection("Worked meaning", "I have a book."),
            IntroductionSection("Answer form", "Choose or type the lowercase one-word present form without punctuation."),
        )
    }
}
