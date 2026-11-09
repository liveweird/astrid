package app.astrid.engine

internal class ModuleOnePracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, planRevision: String, seed: Int): PracticePlan {
        val specification = SPECIFICATIONS.singleOrNull { it.focusId == focusId && it.planRevision == planRevision }
            ?: throw IllegalArgumentException("Unsupported Module 1 profile: $focusId/$planRevision")
        val exercises = when (focusId) {
            SINGULAR_FOCUS_ID -> singularExercises(seed)
            GROUP_FOCUS_ID -> groupExercises(seed)
            ER_FOCUS_ID -> erExercises(seed)
            else -> error("Unreachable Module 1 profile $focusId")
        }
        return PracticePlan(
            focusId = focusId,
            planRevision = planRevision,
            seed = seed,
            title = specification.title,
            prerequisiteSkillIds = specification.prerequisites,
            introduction = specification.introduction,
            items = exercises.mapIndexed(::practiceItem),
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = specification.introductionExposureKeys,
            introductionRequired = true,
            completionRuleId = COMPLETION_RULE_ID,
            exposurePolicyId = EXPOSURE_POLICY_ID,
        )
    }

    private fun singularExercises(seed: Int): List<GeneratedExercise> {
        val selection = SINGULAR_LEXICAL_IDS.map { lexicalId -> exercise("E016", seed, lexicalId = lexicalId) }
        val recallIds = SINGULAR_LEXICAL_IDS.rotate(seed.mod(SINGULAR_LEXICAL_IDS.size)).take(SINGULAR_RECALL_COUNT)
        return selection + recallIds.map { lexicalId -> exercise("E017", seed, lexicalId = lexicalId) }
    }

    private fun groupExercises(seed: Int): List<GeneratedExercise> =
        GROUP_LEXICAL_IDS.map { lexicalId -> exercise("E016", seed, lexicalId = lexicalId) } +
            GROUP_LEXICAL_IDS.map { lexicalId -> exercise("E017", seed, lexicalId = lexicalId) }

    private fun erExercises(seed: Int): List<GeneratedExercise> {
        val fixedSelection = exercise("E018", seed, framePrefix = if (seed and 1 == 0) "book" else "letter")
        val identitySelection = exercise("E018", seed, framePrefix = if (seed and 1 == 0) "identity-jeg" else "identity-du")
        val fixedRecall = listOf(
            exercise("E019", seed, framePrefix = "book"),
            exercise("E019", seed, framePrefix = "letter"),
        )
        val occupationFrames = OCCUPATION_FRAMES.rotate(seed.mod(OCCUPATION_FRAMES.size)).take(OCCUPATION_RECALL_COUNT)
        return listOf(fixedSelection, identitySelection) + fixedRecall +
            occupationFrames.map { frame -> exercise("E019", seed, framePrefix = frame) }
    }

    private fun exercise(
        formatId: String,
        seed: Int,
        lexicalId: String? = null,
        framePrefix: String? = null,
    ): GeneratedExercise =
        (0 until MODULE_ONE_SCAN_SIZE).asSequence()
            .map { index ->
                engine.generate(
                    GenerationRequest(
                        formatId,
                        if (formatId in setOf("E016", "E017")) "G003" else "G004",
                        seed,
                        index,
                        CONTENT_REVISION,
                        GENERATOR_REVISION,
                    ),
                )
            }
            .firstOrNull { generated ->
                val source = requireNotNull(generated.inspector.moduleOneSource)
                (lexicalId == null || source.targetLexicalId == lexicalId) &&
                    (framePrefix == null || requireNotNull(source.frameVariantId).startsWith(framePrefix))
            } ?: error("No Module 1 task for $formatId/$lexicalId/$framePrefix")

    private fun practiceItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as ModuleOneExerciseSource).task
        val mode = if (exercise.formatId in setOf("E016", "E018")) "selection" else "recall"
        val descriptor = PracticeTargetDescriptor(
            label = descriptorLabel(task.targetScopeId, mode),
            targetSkillId = requireNotNull(exercise.targetSkillId),
            lexicalTarget = null,
            assessmentMode = mode,
            targetScopeId = task.targetScopeId,
        )
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = descriptor,
            answerExposureKeys = task.answerExposureKeys,
            promptExposureKeys = task.promptExposureKeys.distinct(),
            lessonExposureKeys = task.lessonExposureKeys.distinct(),
            hintExposureKeys = task.hintExposureKeys.distinct(),
            feedbackExposureKeys = task.feedbackExposureKeys.distinct(),
            assessments = listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    private fun descriptorLabel(scope: String, mode: String): String =
        when (scope to mode) {
            ModuleOneContent.SINGULAR_SCOPE to "selection" -> "One-person subject words — selection"
            ModuleOneContent.SINGULAR_SCOPE to "recall" -> "One-person subject words — recall"
            ModuleOneContent.GROUP_SCOPE to "selection" -> "Group subject words — selection"
            ModuleOneContent.GROUP_SCOPE to "recall" -> "Group subject words — recall"
            ModuleOneContent.VAERE_SCOPE to "selection" -> "Present form of be — selection"
            ModuleOneContent.VAERE_SCOPE to "recall" -> "Present form of be — recall"
            else -> error("Unsupported Module 1 target $scope/$mode")
        }

    private fun <T> List<T>.rotate(offset: Int): List<T> = drop(offset) + take(offset)

    internal data class Specification(
        val focusId: String,
        val planRevision: String,
        val title: String,
        val description: String,
        val prerequisites: List<String>,
        val introduction: List<IntroductionSection>,
        val introductionExposureKeys: List<String>,
    )

    companion object {
        const val SINGULAR_FOCUS_ID = "module-one-singular"
        const val SINGULAR_PLAN_REVISION = "module-one-singular-1"
        const val GROUP_FOCUS_ID = "module-one-groups"
        const val GROUP_PLAN_REVISION = "module-one-groups-1"
        const val ER_FOCUS_ID = "module-one-er"
        const val ER_PLAN_REVISION = "module-one-er-1"
        const val ITEM_COUNT = 6
        const val COMPLETION_RULE_ID = "module-one-known-outcome-1"
        const val EXPOSURE_POLICY_ID = "module-one-exposure-1"
        private const val CONTENT_REVISION = "pilot-8"
        private const val GENERATOR_REVISION = "generator-8"
        private const val EVALUATION_REVISION = "evaluation-8"
        private const val MODULE_ONE_SCAN_SIZE = 32
        private const val SINGULAR_RECALL_COUNT = 2
        private const val OCCUPATION_RECALL_COUNT = 2
        private val SINGULAR_LEXICAL_IDS = listOf("L001", "L002", "L003", "L004")
        private val GROUP_LEXICAL_IDS = listOf("L005", "L006", "L007")
        private val OCCUPATION_FRAMES = listOf("teacher-jeg", "teacher-du", "teacher-han", "teacher-hun")
        private val FOUNDATION_INTRODUCTION = listOf(
            IntroductionSection(
                "Foundation for this profile",
                "This support covers only the written word boundaries, Danish letters, and sentence roles used here. " +
                    "It is not complete coverage of spelling or sentence analysis.",
            ),
            IntroductionSection(
                "Words and complete statements",
                "A written word is one unit separated from other words by spaces. " +
                    "A complete statement combines words that have different jobs.",
            ),
            IntroductionSection("Danish letters", "æ · ø · å", "da"),
            IntroductionSection(
                "Character entry",
                "Use the Danish character controls when a displayed form needs one of the three letters above. " +
                    "Pronunciation is not assessed here.",
            ),
            IntroductionSection("A supplied statement", "Jeg er Anna.", "da"),
            IntroductionSection(
                "Roles in the supplied statement",
                "The first word is a pronoun functioning as the subject. The second word is the present verb. " +
                    "Anna names the subject rather than an object. The statement means I am Anna.",
            ),
            IntroductionSection(
                "Standalone capitalization",
                "A standalone introduced form keeps its displayed capitalization. " +
                    "Sentence-initial capitalization does not change the standalone answer requested by a task.",
            ),
        )
        private val FOUNDATION_EXPOSURE_KEYS = listOf(
            "module-one:subject-form:L001",
            "module-one:verb-present:L010",
        )

        val SPECIFICATIONS = listOf(
            Specification(
                SINGULAR_FOCUS_ID,
                SINGULAR_PLAN_REVISION,
                "Talking about one person",
                "Choose and recall introduced subject words for the speaker, one addressee, and one stated referent.",
                listOf("G001", "G002"),
                FOUNDATION_INTRODUCTION + listOf(
                    IntroductionSection("One-person subject words", "jeg · du · han · hun", "da"),
                    IntroductionSection(
                        "Meanings in display order",
                        "The forms mean I, you for one person, he, and she.",
                    ),
                    IntroductionSection(
                        "One-person answer casing",
                        "Use the lowercase forms shown above when the task asks for a standalone word.",
                    ),
                ),
                (FOUNDATION_EXPOSURE_KEYS + SINGULAR_LEXICAL_IDS.map(ModuleOneContent::subjectOrVerbKey)).distinct(),
            ),
            Specification(
                GROUP_FOCUS_ID,
                GROUP_PLAN_REVISION,
                "Talking to and about groups",
                "Choose and recall introduced subject words for groups and several addressees.",
                listOf("G001", "G002", "G003"),
                FOUNDATION_INTRODUCTION + listOf(
                    IntroductionSection("Group subject words", "vi · I · de · du", "da"),
                    IntroductionSection(
                        "Meanings in display order",
                        "The forms mean we, you for several people, they for several people, and you for one person.",
                    ),
                    IntroductionSection("Worked example", "Vi sover.", "da"),
                    IntroductionSection(
                        "Worked example meaning",
                        "We sleep. The group includes the speaker, and the supplied present verb means sleep.",
                    ),
                    IntroductionSection(
                        "Standalone capitalization",
                        "For several addressees, use the capital form shown above. The other group forms shown above are lowercase.",
                    ),
                ),
                (FOUNDATION_EXPOSURE_KEYS + (GROUP_LEXICAL_IDS + "L002").map(ModuleOneContent::subjectOrVerbKey) +
                    "module-one:verb-present:L013").distinct(),
            ),
            Specification(
                ER_FOCUS_ID,
                ER_PLAN_REVISION,
                "Using the verb to be",
                "Choose and recall the introduced present form of the verb to be in bounded sentence frames.",
                listOf("G001", "G002", "G003"),
                FOUNDATION_INTRODUCTION + listOf(
                    IntroductionSection(
                        "Articles and nouns",
                        "In each noun bundle shown below, the first written word is the article and the second is the noun. " +
                            "The task supplies both and asks only for one verb word.",
                    ),
                    IntroductionSection(
                        "Dictionary and sentence forms",
                        "The dictionary teaching bundle and the one-word present form have different jobs.",
                    ),
                    IntroductionSection("Introduced verb forms", "at være → er", "da"),
                    IntroductionSection("Noun bundles", "en bog · et brev · en lærer", "da"),
                    IntroductionSection(
                        "Noun meanings in display order",
                        "The supplied nouns mean a book, a letter, and a teacher.",
                    ),
                    IntroductionSection("Worked examples", "Det er en bog. · Jeg er lærer. · Jeg er Anna.", "da"),
                    IntroductionSection(
                        "Worked meanings",
                        "This is a book. I am a teacher. I am Anna. " +
                            "The supplied noun, article, occupation, and subject are not assessed.",
                    ),
                    IntroductionSection(
                        "Present-form answer casing",
                        "Type the one-word present form in lowercase exactly as shown above.",
                    ),
                ),
                (FOUNDATION_EXPOSURE_KEYS + listOf(
                    "module-one:verb-infinitive:L010",
                    "module-one:verb-present:L010",
                    "module-one:identifying-form:L008",
                    "module-one:headword:L014-S01",
                    "module-one:article:L014",
                    "module-one:headword:L018-S01",
                    "module-one:article:L018",
                    "module-one:headword:L025-S01",
                    "module-one:article:L025",
                )).distinct(),
            ),
        )
    }
}
