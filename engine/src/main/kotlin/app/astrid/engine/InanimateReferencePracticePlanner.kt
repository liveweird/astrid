package app.astrid.engine

internal class InanimateReferencePracticePlanner(private val engine: PilotEngine) {
    fun plan(seed: Int): PracticePlan {
        val selection = orderedTasks("E020", seed).map { task -> exercise(task, seed) }
        val recall = orderedTasks("E021", seed xor RECALL_SEED_XOR).map { task -> exercise(task, seed) }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = PLAN_REVISION,
            seed = seed,
            title = TITLE,
            prerequisiteSkillIds = PREREQUISITES,
            introduction = INTRODUCTION,
            items = (selection + recall).mapIndexed(::practiceItem),
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = INTRODUCTION_EXPOSURE_KEYS,
            introductionRequired = true,
            completionRuleId = ModuleOnePracticePlanner.COMPLETION_RULE_ID,
            exposurePolicyId = ModuleOnePracticePlanner.EXPOSURE_POLICY_ID,
        )
    }

    private fun orderedTasks(formatId: String, seed: Int): List<InanimateReferenceTask> {
        val tasks = InanimateReferenceContent.tasks(formatId)
        return if (seed and 1 == 0) tasks else tasks.reversed()
    }

    private fun exercise(task: InanimateReferenceTask, seed: Int): GeneratedExercise =
        (0 until SCAN_SIZE).asSequence()
            .map { index ->
                engine.generate(
                    GenerationRequest(task.formatId, "G038", seed, index, CONTENT_REVISION, GENERATOR_REVISION),
                )
            }
            .first { generated -> generated.inspector.inanimateReferenceSource?.taskId == task.id }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as InanimateReferenceExerciseSource).task
        val mode = if (exercise.formatId == "E020") "selection" else "recall"
        val descriptor = PracticeTargetDescriptor(
            label = if (mode == "selection") {
                "One-thing reference words — selection"
            } else {
                "One-thing reference words — recall"
            },
            targetSkillId = "G038",
            lexicalTarget = null,
            assessmentMode = mode,
            targetScopeId = InanimateReferenceContent.TARGET_SCOPE_ID,
        )
        return PracticePlanItem(
            position,
            exercise,
            descriptor,
            task.answerExposureKeys,
            task.promptExposureKeys.distinct(),
            task.lessonExposureKeys.distinct(),
            task.hintExposureKeys,
            task.feedbackExposureKeys,
            listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    companion object {
        const val FOCUS_ID = "module-one-things"
        const val PLAN_REVISION = "module-one-things-1"
        const val TITLE = "Referring to one thing"
        const val ITEM_COUNT = 4
        val PREREQUISITES = listOf("G001", "G002", "G003", "G006")
        private const val CONTENT_REVISION = "pilot-9"
        private const val GENERATOR_REVISION = "generator-9"
        private const val EVALUATION_REVISION = "evaluation-9"
        private const val SCAN_SIZE = 4
        private const val RECALL_SEED_XOR = 0x41
        private val INTRODUCTION = listOf(
            IntroductionSection(
                "Words, noun bundles, and subjects",
                "A written word is separated by spaces, and a noun bundle here contains an article and noun. " +
                    "In the English statement “It is here”, “it” is the subject.",
            ),
            IntroductionSection("Supplied noun bundles", "en bog · et brev", "da"),
            IntroductionSection(
                "Meanings and grammatical gender",
                "The first bundle means a book (common gender); the second means a letter, a written message (neuter). " +
                    "The task always supplies this information, so you do not have to guess gender.",
            ),
            IntroductionSection("Reference forms", "en bog → den · et brev → det", "da"),
            IntroductionSection("A different identifying pattern", "Det er …", "da"),
            IntroductionSection(
                "Reference, not identification",
                "The reference words shown above refer back to the same mentioned thing. " +
                    "The separate pattern above means this or it is … and identifies something.",
            ),
            IntroductionSection(
                "Standalone lowercase spelling",
                "Use the lowercase forms shown above. Capitals at the start of a complete sentence are normal, " +
                    "but this task requests one standalone word. " +
                    "These activities ask for one subject word referring back to the same thing. Object reference comes later.",
            ),
        )
        private val INTRODUCTION_EXPOSURE_KEYS = listOf(
            "module-one:headword:L014-S01",
            "module-one:article:L014",
            "module-one:headword:L018-S01",
            "module-one:article:L018",
            "module-one:reference-sense:L026-S01",
            "module-one:reference-sense:L008-S02",
            "module-one:visible-form:den",
            "module-one:visible-form:det",
            "module-one:identifying-form:L008",
            "module-one:verb-present:L010",
        )
    }
}
