package app.astrid.engine

internal class ReadingWordsPracticePlanner(private val engine: PilotEngine) {
    fun plan(seed: Int): PracticePlan {
        val cycle = SENSE_IDS.indices.map { index ->
            engine.generate(GenerationRequest("E011", null, seed, index, CONTENT_REVISION, GENERATOR_REVISION))
        }.associateBy { exercise -> requireNotNull(exercise.lexicalTarget).senseId }
        val items = SENSE_IDS.mapIndexed { position, senseId -> item(position, cycle.getValue(senseId)) }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = PLAN_REVISION,
            seed = seed,
            title = TITLE,
            prerequisiteSkillIds = emptyList(),
            introduction = introduction,
            items = items,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = items.flatMap(PracticePlanItem::answerExposureKeys).distinct(),
            introductionRequired = true,
            completionRuleId = ModuleOnePracticePlanner.COMPLETION_RULE_ID,
            exposurePolicyId = EXPOSURE_POLICY_ID,
        )
    }

    private fun item(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val target = requireNotNull(exercise.lexicalTarget)
        val sense = requireNotNull(exercise.inspector.lexicalSource)
        val meaning = meaningKey(target.senseId)
        val bundle = bundleKey(target.senseId)
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = PracticeTargetDescriptor(
                label = sense.gloss.replaceFirstChar(Char::uppercase),
                targetSkillId = null,
                lexicalTarget = target,
            ),
            answerExposureKeys = listOf(meaning, bundle),
            promptExposureKeys = SENSE_IDS.map(::bundleKey),
            lessonExposureKeys = SENSE_IDS.flatMap { listOf(bundleKey(it), meaningKey(it)) },
            hintExposureKeys = emptyList(),
            feedbackExposureKeys = listOf(meaning),
        )
    }

    private fun bundleKey(senseId: String) = "module-one:reading-noun-bundle:$senseId"

    private fun meaningKey(senseId: String) = "module-one:reading-meaning:$senseId"

    companion object {
        const val FOCUS_ID = "module-one-reading-words"
        const val PLAN_REVISION = "module-one-reading-words-1"
        const val TITLE = "Words for things you read"
        const val DESCRIPTION = "Choose Danish words for a magazine, a book, a newspaper and a letter."
        const val ITEM_COUNT = 4
        const val EXPOSURE_POLICY_ID = "module-one-reading-word-exposure-1"
        private const val CONTENT_REVISION = "pilot-5"
        private const val GENERATOR_REVISION = "generator-5"
        private const val EVALUATION_REVISION = "evaluation-5"
        private val SENSE_IDS = listOf("L024-S01", "L014-S01", "L015-S01", "L018-S01")
        private val introduction = listOf(
            IntroductionSection(
                "At the library",
                "You want a printed magazine to read. Learn its Danish name and review three other things you can read.",
            ),
            IntroductionSection("A magazine", "et magasin", "da"),
            IntroductionSection("A book", "en bog", "da"),
            IntroductionSection("A newspaper", "en avis", "da"),
            IntroductionSection("A written letter", "et brev", "da"),
            IntroductionSection(
                "How to answer",
                "Choose the Danish phrase with the meaning requested. Each phrase already includes its article.",
            ),
        )
    }
}
