package app.astrid.engine

class PracticePlanner(private val engine: PilotEngine = PilotEngine()) {
    fun profiles(): List<PracticeProfile> =
        listOf(
            PracticeProfile(
                focusId = READING_FOCUS_ID,
                planRevision = READING_PLAN_REVISION,
                title = "Mixed practice",
                description = "Review words, simple statements, and indefinite articles.",
                itemCount = READING_ITEM_COUNT,
                prerequisiteSkillIds = listOf("G001", "G002", "G003", "G004"),
            ),
            PracticeProfile(
                focusId = PRESENT_VERBS_FOCUS_ID,
                planRevision = PRESENT_VERBS_PLAN_REVISION,
                title = "Present verb forms",
                description = "Choose, supply, and repair Danish finite present verbs.",
                itemCount = PRESENT_VERBS_ITEM_COUNT,
                prerequisiteSkillIds = listOf("G001", "G002", "G003"),
            ),
            PracticeProfile(
                focusId = PracticeGapPlanner.FOCUS_ID,
                planRevision = PracticeGapPlanner.PLAN_REVISION,
                title = PracticeGapPlanner.TITLE,
                description = "Practise a present verb and an indefinite article in the same sentence.",
                itemCount = PracticeGapPlanner.ITEM_COUNT,
                prerequisiteSkillIds = listOf("G001", "G002", "G003", "G005"),
            ),
        ) + ModuleOnePracticePlanner.SPECIFICATIONS.map { specification ->
            PracticeProfile(
                focusId = specification.focusId,
                planRevision = specification.planRevision,
                title = specification.title,
                description = specification.description,
                itemCount = ModuleOnePracticePlanner.ITEM_COUNT,
                prerequisiteSkillIds = specification.prerequisites,
            )
        } + PracticeProfile(
            focusId = InanimateReferencePracticePlanner.FOCUS_ID,
            planRevision = InanimateReferencePracticePlanner.PLAN_REVISION,
            title = InanimateReferencePracticePlanner.TITLE,
            description = "Choose and recall den or det for one previously mentioned thing with its gender supplied.",
            itemCount = InanimateReferencePracticePlanner.ITEM_COUNT,
            prerequisiteSkillIds = InanimateReferencePracticePlanner.PREREQUISITES,
        ) + PresentOrderPracticePlanner.SPECIFICATIONS.map { specification ->
            PracticeProfile(
                focusId = specification.focusId,
                planRevision = specification.planRevision,
                title = specification.title,
                description = specification.description,
                itemCount = PresentOrderPracticePlanner.ITEM_COUNT,
                prerequisiteSkillIds = specification.prerequisites,
            )
        } + PracticeProfile(
            focusId = PossessionPracticePlanner.FOCUS_ID,
            planRevision = PossessionPracticePlanner.PLAN_REVISION,
            title = PossessionPracticePlanner.TITLE,
            description = PossessionPracticePlanner.DESCRIPTION,
            itemCount = PossessionPracticePlanner.ITEM_COUNT,
            prerequisiteSkillIds = PossessionPracticePlanner.PREREQUISITES,
        ) + PracticeProfile(
            focusId = ReadingWordsPracticePlanner.FOCUS_ID,
            planRevision = ReadingWordsPracticePlanner.PLAN_REVISION,
            title = ReadingWordsPracticePlanner.TITLE,
            description = ReadingWordsPracticePlanner.DESCRIPTION,
            itemCount = ReadingWordsPracticePlanner.ITEM_COUNT,
            prerequisiteSkillIds = emptyList(),
        ) + PracticeProfile(
            focusId = PersonalIntroductionPracticePlanner.FOCUS_ID,
            planRevision = PersonalIntroductionPracticePlanner.PLAN_REVISION,
            title = PersonalIntroductionPracticePlanner.TITLE,
            description = PersonalIntroductionPracticePlanner.DESCRIPTION,
            itemCount = PersonalIntroductionPracticePlanner.ITEM_COUNT,
            prerequisiteSkillIds = PersonalIntroductionPracticePlanner.PREREQUISITES,
        )

    fun plan(
        focusId: String,
        planRevision: String,
        seed: Int,
    ): PracticePlan {
        require(seed >= 0) { "seed must be non-negative" }
        return when (focusId to planRevision) {
            READING_FOCUS_ID to READING_PLAN_REVISION -> readingPlan(focusId, planRevision, seed)
            PRESENT_VERBS_FOCUS_ID to PRESENT_VERBS_PLAN_REVISION -> presentVerbsPlan(focusId, planRevision, seed)
            PracticeGapPlanner.FOCUS_ID to PracticeGapPlanner.PLAN_REVISION -> PracticeGapPlanner(engine).plan(seed)
            ModuleOnePracticePlanner.SINGULAR_FOCUS_ID to ModuleOnePracticePlanner.SINGULAR_PLAN_REVISION,
            ModuleOnePracticePlanner.GROUP_FOCUS_ID to ModuleOnePracticePlanner.GROUP_PLAN_REVISION,
            ModuleOnePracticePlanner.ER_FOCUS_ID to ModuleOnePracticePlanner.ER_PLAN_REVISION,
            -> ModuleOnePracticePlanner(engine).plan(focusId, planRevision, seed)
            InanimateReferencePracticePlanner.FOCUS_ID to InanimateReferencePracticePlanner.PLAN_REVISION ->
                InanimateReferencePracticePlanner(engine).plan(seed)
            PossessionPracticePlanner.FOCUS_ID to PossessionPracticePlanner.PLAN_REVISION ->
                PossessionPracticePlanner(engine).plan(seed)
            PresentOrderPracticePlanner.PRESENT_FOCUS_ID to PresentOrderPracticePlanner.PRESENT_PLAN_REVISION,
            PresentOrderPracticePlanner.ORDER_FOCUS_ID to PresentOrderPracticePlanner.ORDER_PLAN_REVISION,
            -> PresentOrderPracticePlanner(engine).plan(focusId, seed)
            ReadingWordsPracticePlanner.FOCUS_ID to ReadingWordsPracticePlanner.PLAN_REVISION ->
                ReadingWordsPracticePlanner(engine).plan(seed)
            PersonalIntroductionPracticePlanner.FOCUS_ID to PersonalIntroductionPracticePlanner.PLAN_REVISION ->
                PersonalIntroductionPracticePlanner(engine).plan(seed)
            else -> throw IllegalArgumentException("Unsupported practice profile: $focusId/$planRevision")
        }
    }

    private fun readingPlan(
        focusId: String,
        planRevision: String,
        seed: Int,
    ): PracticePlan {
        val recognitions = (0..2).map { lexicalExercise("E007", seed, it) }
        val recalls = (0..2).map { lexicalExercise("E008", seed, it) }
        val senses = recognitions.map { requireNotNull(it.lexicalTarget) }
        val ordering = grammarExercise("E003", "G005", seed, senses[2].entryId)
        val articleRepair = grammarExercise("E006", "G006", seed, senses[0].entryId)
        val exercises = listOf(
            recognitions[0],
            ordering,
            recognitions[1],
            recalls[0],
            recognitions[2],
            recalls[1],
            articleRepair,
            recalls[2],
        )
        val items = exercises.mapIndexed(::practiceItem)
        return PracticePlan(
            focusId = focusId,
            planRevision = planRevision,
            seed = seed,
            title = "Words and simple statements",
            prerequisiteSkillIds = listOf("G001", "G002", "G003", "G004"),
            introduction = exercises.flatMap { it.introduction }.distinct(),
            items = items,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
        )
    }

    private fun presentVerbsPlan(
        focusId: String,
        planRevision: String,
        seed: Int,
    ): PracticePlan {
        val formats = listOf("E001", "E004", "E005", "E001", "E004", "E005")
        val firstVerbIndex = seed.mod(PilotContent.verbs.size)
        val verbs = PilotContent.verbs.rotate(firstVerbIndex) + PilotContent.verbs[firstVerbIndex]
        val usedSentenceIds = mutableSetOf<String>()
        val exercises = formats.zip(verbs).mapIndexed { position, (formatId, verb) ->
            presentVerbExercise(formatId, seed, position, verb.lexicalId, usedSentenceIds).also { exercise ->
                usedSentenceIds += requireNotNull(exercise.inspector.canonicalSentenceId)
            }
        }
        return PracticePlan(
            focusId = focusId,
            planRevision = planRevision,
            seed = seed,
            title = "Present verb forms",
            prerequisiteSkillIds = listOf("G001", "G002", "G003"),
            introduction = exercises.flatMap { it.introduction }.distinct(),
            items = exercises.mapIndexed(::presentVerbPracticeItem),
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
        )
    }

    private fun presentVerbExercise(
        formatId: String,
        seed: Int,
        position: Int,
        verbId: String,
        excludedSentenceIds: Set<String>,
    ): GeneratedExercise =
        (0 until GRAMMAR_SCAN_SIZE).asSequence()
            .map { engine.generate(currentRequest(formatId, "G004", seed, it + position)) }
            .firstOrNull { exercise ->
                verbId in exercise.inspector.lexicalIds && exercise.inspector.canonicalSentenceId !in excludedSentenceIds
            }
            ?: error("No unused licensed sentence for $verbId and $formatId")

    private fun <T> List<T>.rotate(offset: Int): List<T> = drop(offset) + take(offset)

    private fun presentVerbPracticeItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val mode = assessmentMode(exercise.formatId)
        val answerKeys = listOf(presentVerbKey(exercise))
        val infinitiveKey = infinitiveVerbKey(exercise)
        val conceptKey = grammarKey("G004")
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = PracticeTargetDescriptor(
                label = assessmentLabel(mode),
                targetSkillId = "G004",
                lexicalTarget = null,
                assessmentMode = mode,
            ),
            answerExposureKeys = answerKeys,
            promptExposureKeys = presentVerbPromptKeys(exercise, infinitiveKey),
            lessonExposureKeys = listOf(infinitiveKey, presentVerbKey(exercise), conceptKey),
            hintExposureKeys = listOf(conceptKey),
            feedbackExposureKeys = listOf(presentVerbKey(exercise), conceptKey),
        )
    }

    private fun assessmentMode(formatId: String): String =
        when (formatId) {
            "E001" -> "selection"
            "E004" -> "recall"
            "E005" -> "repair"
            else -> error("Unsupported present-verb format $formatId")
        }

    private fun assessmentLabel(mode: String): String =
        when (mode) {
            "selection" -> "Present verbs — choosing the form"
            "recall" -> "Present verbs — written recall"
            "repair" -> "Present verbs — repairing the form"
            else -> error("Unsupported present-verb assessment mode $mode")
        }

    private fun presentVerbPromptKeys(exercise: GeneratedExercise, infinitiveKey: String): List<String> =
        when (exercise.formatId) {
            "E001" -> listOf(infinitiveKey, presentVerbKey(exercise))
            "E004", "E005" -> listOf(infinitiveKey)
            else -> error("Unsupported present-verb format ${exercise.formatId}")
        }

    private fun presentVerb(exercise: GeneratedExercise): Verb =
        PilotContent.verbs.single { it.lexicalId in exercise.inspector.lexicalIds }

    private fun presentVerbKey(exercise: GeneratedExercise): String = "verb-present:${presentVerb(exercise).lexicalId}"

    private fun infinitiveVerbKey(exercise: GeneratedExercise): String = "verb-infinitive:${presentVerb(exercise).lexicalId}"

    private fun lexicalExercise(
        formatId: String,
        seed: Int,
        index: Int,
    ): GeneratedExercise = engine.generate(currentRequest(formatId, null, seed, index))

    private fun grammarExercise(
        formatId: String,
        targetSkillId: String,
        seed: Int,
        nounEntryId: String,
    ): GeneratedExercise =
        (0 until GRAMMAR_SCAN_SIZE).asSequence()
            .map { engine.generate(currentRequest(formatId, targetSkillId, seed, it)) }
            .firstOrNull {
                it.inspector.lexicalIds.contains(READ_ENTRY_ID) && it.inspector.lexicalIds.contains(nounEntryId)
            }
            ?: error("No licensed reading sentence for $nounEntryId and $formatId")

    private fun currentRequest(
        formatId: String,
        targetSkillId: String?,
        seed: Int,
        index: Int,
    ) = GenerationRequest(
        formatId = formatId,
        targetSkillId = targetSkillId,
        seed = seed,
        index = index,
        contentRevision = CONTENT_REVISION,
        generatorRevision = GENERATOR_REVISION,
    )

    private fun practiceItem(
        position: Int,
        exercise: GeneratedExercise,
    ): PracticePlanItem {
        val answerKeys = answerExposureKeys(exercise)
        val contextualKeys = contextualExposureKeys(exercise)
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = targetDescriptor(exercise),
            answerExposureKeys = answerKeys,
            promptExposureKeys = promptExposureKeys(exercise, contextualKeys),
            lessonExposureKeys = (answerKeys + contextualKeys).distinct(),
            hintExposureKeys = if (exercise.formatId == "E003") answerKeys else emptyList(),
            feedbackExposureKeys = when (exercise.formatId) {
                "E008" -> (answerKeys + articleKey(requireNotNull(exercise.lexicalTarget).entryId)).distinct()
                "E003", "E006" -> (answerKeys + contextualKeys.filterNot { it.startsWith("gloss:") }).distinct()
                else -> answerKeys
            },
        )
    }

    private fun targetDescriptor(exercise: GeneratedExercise): PracticeTargetDescriptor =
        PracticeTargetDescriptor(
            label =
                exercise.targetSkillId?.let { target ->
                    PilotContent.skills.single { it.id == target }.title
                } ?: requireNotNull(exercise.inspector.lexicalSource).run {
                    val target = requireNotNull(exercise.lexicalTarget)
                    if (target.direction == "da-en") "$headword — $gloss" else "$gloss — $headword"
                },
            targetSkillId = exercise.targetSkillId,
            lexicalTarget = exercise.lexicalTarget,
        )

    private fun answerExposureKeys(exercise: GeneratedExercise): List<String> =
        when (exercise.formatId) {
            "E007" -> listOf(glossKey(requireNotNull(exercise.lexicalTarget).senseId))
            "E008" -> listOf(headwordKey(requireNotNull(exercise.lexicalTarget).senseId))
            "E003" -> listOf(grammarKey("G005"))
            "E006" -> listOf(articleKey(grammarNoun(exercise).entryId))
            else -> error("Unsupported practice format ${exercise.formatId}")
        }

    private fun promptExposureKeys(
        exercise: GeneratedExercise,
        contextualKeys: List<String>,
    ): List<String> =
        when (exercise.formatId) {
            "E007" ->
                PilotContent.vocabularySenses.map { glossKey(it.senseId) } +
                    headwordKey(requireNotNull(exercise.lexicalTarget).senseId)
            "E008" -> listOf(glossKey(requireNotNull(exercise.lexicalTarget).senseId))
            else -> contextualKeys
        }.distinct()

    private fun contextualExposureKeys(exercise: GeneratedExercise): List<String> =
        when (exercise.formatId) {
            "E007", "E008" -> {
                val target = requireNotNull(exercise.lexicalTarget)
                listOf(headwordKey(target.senseId), articleKey(target.entryId), glossKey(target.senseId))
            }
            "E003", "E006" -> {
                val noun = grammarNoun(exercise)
                listOf(headwordKey(noun.senseId), articleKey(noun.entryId), glossKey(noun.senseId))
            }
            else -> emptyList()
        }

    private fun grammarNoun(exercise: GeneratedExercise): LexicalSense {
        val nounEntryId = exercise.inspector.lexicalIds.single { id ->
            PilotContent.vocabularySenses.any { it.entryId == id }
        }
        return PilotContent.vocabularySenses.single { it.entryId == nounEntryId }
    }

    private fun glossKey(senseId: String) = "gloss:$senseId"

    private fun headwordKey(senseId: String) = "headword:$senseId"

    private fun articleKey(entryId: String) = "article:$entryId"

    private fun grammarKey(skillId: String) = "grammar:$skillId"

    private companion object {
        const val READING_FOCUS_ID = "reading-basics"
        const val READING_PLAN_REVISION = "practice-1"
        const val PRESENT_VERBS_FOCUS_ID = "present-verbs"
        const val PRESENT_VERBS_PLAN_REVISION = "present-verbs-1"
        const val READING_ITEM_COUNT = 8
        const val PRESENT_VERBS_ITEM_COUNT = 6
        const val CONTENT_REVISION = "pilot-3"
        const val GENERATOR_REVISION = "generator-3"
        const val EVALUATION_REVISION = "evaluation-3"
        const val READ_ENTRY_ID = "L011"
        const val GRAMMAR_SCAN_SIZE = 99
    }
}
