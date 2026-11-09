package app.astrid.engine

internal class PracticeGapPlanner(private val engine: PilotEngine) {
    fun plan(seed: Int): PracticePlan {
        val verbs = ELIGIBLE_VERB_IDS.rotate(seed.mod(ELIGIBLE_VERB_IDS.size))
        val usedSentenceIds = mutableSetOf<String>()
        val exercises = verbs.map { verbId ->
            (0 until GAP_CYCLE_SIZE).asSequence()
                .map { index -> engine.generate(request(seed, index)) }
                .firstOrNull { exercise ->
                    verbId in exercise.inspector.lexicalIds &&
                        exercise.inspector.canonicalSentenceId !in usedSentenceIds
                }?.also { usedSentenceIds += requireNotNull(it.inspector.canonicalSentenceId) }
                ?: error("No unused licensed E015 sentence for $verbId")
        }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = PLAN_REVISION,
            seed = seed,
            title = TITLE,
            prerequisiteSkillIds = listOf("G001", "G002", "G003", "G005"),
            introduction = INTRODUCTION,
            items = exercises.mapIndexed(::practiceItem),
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = INTRODUCTION_EXPOSURE_KEYS,
        )
    }

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val gapSource = requireNotNull(exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val verb = PilotContent.verbs.single { it.lexicalId == gapSource.getValue("g0").lexicalId }
        val noun = PilotContent.nouns.single { it.id == gapSource.getValue("g1").lexicalId }
        val verbKey = "verb-present:${verb.lexicalId}"
        val articleKey = "article:${noun.id}"
        val assessments = listOf(
            PracticeAssessment("g0", VERB_DESCRIPTOR, "primary", listOf(verbKey)),
            PracticeAssessment("g1", ARTICLE_DESCRIPTOR, "support", listOf(articleKey)),
        )
        val contextualKeys = listOfNotNull(
            "verb-infinitive:${verb.lexicalId}",
            verbKey,
            articleKey,
            headwordKey(noun.id),
        )
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = VERB_DESCRIPTOR,
            answerExposureKeys = assessments.flatMap { it.answerExposureKeys },
            promptExposureKeys = contextualKeys,
            lessonExposureKeys = contextualKeys + listOf("grammar:G004", "grammar:G006"),
            hintExposureKeys = listOf("grammar:G004", "grammar:G006"),
            feedbackExposureKeys = listOf(verbKey, articleKey, "grammar:G004", "grammar:G006"),
            assessments = assessments,
        )
    }

    private fun headwordKey(nounId: String): String? =
        PilotContent.vocabularySenses.singleOrNull { it.entryId == nounId }?.let { "headword:${it.senseId}" }

    private fun request(seed: Int, index: Int) = GenerationRequest(
        formatId = "E015",
        targetSkillId = "G004",
        seed = seed,
        index = index,
        contentRevision = CONTENT_REVISION,
        generatorRevision = GENERATOR_REVISION,
    )

    private fun <T> List<T>.rotate(offset: Int): List<T> = drop(offset) + take(offset)

    companion object {
        const val FOCUS_ID = "verb-article-gaps"
        const val PLAN_REVISION = "verb-article-gaps-1"
        const val TITLE = "Verbs and articles together"
        const val ITEM_COUNT = 4
        private const val GAP_CYCLE_SIZE = 92
        private const val CONTENT_REVISION = "pilot-7"
        private const val GENERATOR_REVISION = "generator-7"
        private const val EVALUATION_REVISION = "evaluation-7"
        private val ELIGIBLE_VERB_IDS = listOf("L009", "L011", "L012", "L010")
        private val VERB_DESCRIPTOR = PracticeTargetDescriptor("Present verb selection", "G004", null, "selection")
        private val ARTICLE_DESCRIPTOR = PracticeTargetDescriptor("Indefinite article selection", "G006", null, "selection")
        private val INTRODUCTION = listOf(
            IntroductionSection(
                "Two grammar targets",
                "Choose a finite present verb and the noun's stored article. " +
                    "The primary verb and supporting article receive separate feedback.",
            ),
            IntroductionSection("Forms in the example", "læse → læser · en bog", "da"),
            IntroductionSection("Worked example", "Jeg læser en bog.", "da"),
        )
        private val INTRODUCTION_EXPOSURE_KEYS = listOf(
            "verb-infinitive:L011",
            "verb-present:L011",
            "article:L014",
            "headword:L014-S01",
            "grammar:G004",
            "grammar:G006",
        )
    }
}
