package app.astrid.engine

class PracticePlanner(private val engine: PilotEngine = PilotEngine()) {
    fun plan(
        focusId: String,
        planRevision: String,
        seed: Int,
    ): PracticePlan {
        require(focusId == FOCUS_ID) { "Unsupported practice focus: $focusId" }
        require(planRevision == PLAN_REVISION) { "Unsupported practice plan revision: $planRevision" }
        require(seed >= 0) { "seed must be non-negative" }

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
        const val FOCUS_ID = "reading-basics"
        const val PLAN_REVISION = "practice-1"
        const val CONTENT_REVISION = "pilot-3"
        const val GENERATOR_REVISION = "generator-3"
        const val EVALUATION_REVISION = "evaluation-3"
        const val READ_ENTRY_ID = "L011"
        const val GRAMMAR_SCAN_SIZE = 99
    }
}
