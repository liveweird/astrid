package app.astrid.engine

internal class PresentOrderPracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, seed: Int): PracticePlan {
        val specification = SPECIFICATIONS.singleOrNull { it.focusId == focusId }
            ?: throw IllegalArgumentException("Unsupported present/order profile: $focusId")
        val exercises = specification.formats.flatMap { formatId ->
            PresentOrderContent.tasks(formatId).map { task -> exercise(task, seed) }
        }
        return PracticePlan(
            focusId, specification.planRevision, seed, specification.title, specification.prerequisites,
            specification.introduction, exercises.mapIndexed(::practiceItem), CONTENT_REVISION, GENERATOR_REVISION,
            EVALUATION_REVISION, specification.introductionExposureKeys, true, specification.completionRuleId,
            specification.exposurePolicyId,
        )
    }

    private fun exercise(task: PresentOrderTask, seed: Int): GeneratedExercise =
        (0 until SCAN_SIZE).asSequence().map { index ->
            engine.generate(
                GenerationRequest(
                    task.formatId, if (task is NamedPresentTask) "G004" else "G005", seed, index,
                    CONTENT_REVISION, GENERATOR_REVISION,
                ),
            )
        }.first { generated -> (generated.source as PresentOrderExerciseSource).task.id == task.id }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as PresentOrderExerciseSource).task
        val mode = when (exercise.formatId) {
            "E022" -> "selection"
            "E023" -> "recall"
            "E024" -> "constituent-order"
            else -> "word-order"
        }
        val descriptor = PracticeTargetDescriptor(
            descriptorLabel(mode), requireNotNull(exercise.targetSkillId), null, mode, task.targetScopeId,
        )
        return PracticePlanItem(
            position, exercise, descriptor, task.answerExposureKeys, task.promptExposureKeys.distinct(),
            task.lessonExposureKeys.distinct(), task.hintExposureKeys.distinct(), task.feedbackExposureKeys.distinct(),
            listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    private fun descriptorLabel(mode: String) = when (mode) {
        "selection" -> "Named present forms — selection"
        "recall" -> "Named present forms — recall"
        "constituent-order" -> "Subject-first statements — constituent order"
        else -> "Subject-first statements — word order"
    }

    internal data class Specification(
        val focusId: String,
        val planRevision: String,
        val title: String,
        val description: String,
        val prerequisites: List<String>,
        val formats: List<String>,
        val introduction: List<IntroductionSection>,
        val introductionExposureKeys: List<String>,
        val completionRuleId: String,
        val exposurePolicyId: String,
    )

    companion object {
        const val PRESENT_FOCUS_ID = "module-one-present"
        const val PRESENT_PLAN_REVISION = "module-one-present-1"
        const val ORDER_FOCUS_ID = "module-one-order"
        const val ORDER_PLAN_REVISION = "module-one-order-1"
        const val ITEM_COUNT = 6
        const val ORDER_COMPLETION_RULE_ID = "module-one-order-outcome-1"
        const val ORDER_EXPOSURE_POLICY_ID = "module-one-order-exposure-1"
        private const val CONTENT_REVISION = "pilot-10"
        private const val GENERATOR_REVISION = "generator-10"
        private const val EVALUATION_REVISION = "evaluation-10"
        private const val SCAN_SIZE = 3

        val SPECIFICATIONS = listOf(
            Specification(
                PRESENT_FOCUS_ID, PRESENT_PLAN_REVISION, "Using present verb forms",
                "Choose and type present forms of three Danish verbs.", listOf("G001", "G002", "G003"),
                listOf("E022", "E023"), presentIntroduction(),
                listOf(PresentOrderContent.PRESENT_PATTERN_KEY) + listOf("L027", "L011", "L013").flatMap {
                    listOf(PresentOrderContent.infinitiveKey(it), PresentOrderContent.presentKey(it))
                },
                ModuleOnePracticePlanner.COMPLETION_RULE_ID, ModuleOnePracticePlanner.EXPOSURE_POLICY_ID,
            ),
            Specification(
                ORDER_FOCUS_ID, ORDER_PLAN_REVISION, "Building subject-first statements",
                "Put Danish sentence parts and words in order.", listOf("G001", "G002", "G003", "G004"),
                listOf("E024", "E025"), orderIntroduction(),
                listOf(PresentOrderContent.ORDER_PATTERN_KEY) +
                    listOf("L027", "L011", "L013").map(PresentOrderContent::presentKey) +
                    listOf("L002", "L003", "L007", "L018", "L023")
                        .map(PresentOrderContent::componentKey),
                ORDER_COMPLETION_RULE_ID, ORDER_EXPOSURE_POLICY_ID,
            ),
        )

        private fun presentIntroduction() = listOf(
            IntroductionSection(
                "Words and statements",
                "A written word is separated by spaces. A complete statement combines words with different jobs.",
            ),
            IntroductionSection(
                "Dictionary and present forms",
                "at arbejde → arbejder · at læse → læser · at sove → sover",
                "da",
            ),
            IntroductionSection("Meanings in display order", "The verbs mean to work, to read, and to sleep."),
            IntroductionSection(
                "Observed present pattern",
                "For these three introduced verbs, the one-word present form adds -r to the dictionary form without at.",
            ),
            IntroductionSection("Worked examples", "Du arbejder. · Jeg læser et brev. · Hun sover.", "da"),
            IntroductionSection("Worked meanings", "You work. I read a letter. She sleeps."),
            IntroductionSection(
                "Roles",
                "Each worked statement has a subject and a finite verb. The middle statement also has the supplied object shown above.",
            ),
            IntroductionSection("Answer form", "Choose or type one lowercase present verb without punctuation."),
        )
        private fun orderIntroduction() = listOf(
            IntroductionSection("Constituents and words", "A constituent can contain one word or several words that work together."),
            IntroductionSection("Requested statement pattern", "subject | finite verb | supplied object"),
            IntroductionSection(
                "Position in these statements",
                "The subject fills the first constituent position and the finite verb comes second.",
            ),
            IntroductionSection(
                "Other statement beginnings",
                "Other constituents can begin Danish statements. Later practice studies those patterns.",
            ),
            IntroductionSection("Worked examples", "Han arbejder. · De sover. · Du læser et brev.", "da"),
            IntroductionSection("Worked meanings", "He works. They sleep. You read a letter."),
            IntroductionSection("Last example fields", "Du | læser | et brev", "da"),
            IntroductionSection(
                "Constituents and words in the last example",
                "The last example has three constituents and four written words.",
            ),
        )
    }
}
