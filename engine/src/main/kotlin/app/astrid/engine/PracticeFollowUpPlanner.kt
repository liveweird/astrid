package app.astrid.engine

class PracticeFollowUpPlanner(private val engine: PilotEngine = PilotEngine()) {
    fun plan(
        planRevision: String,
        seed: Int,
        selectedTargetKeys: List<String>,
        sourceExercises: List<GeneratedExercise>,
    ): PracticePlan {
        require(planRevision == PLAN_REVISION) { "Unsupported follow-up plan revision: $planRevision" }
        require(seed >= 0) { "seed must be non-negative" }
        require(selectedTargetKeys.isNotEmpty()) { "At least one follow-up target is required" }
        require(selectedTargetKeys.size <= MAX_ITEMS) { "At most $MAX_ITEMS follow-up targets are supported" }
        require(selectedTargetKeys.distinct().size == selectedTargetKeys.size) { "Follow-up targets must be unique" }
        require(sourceExercises.isNotEmpty()) { "A follow-up requires source exercises" }
        require(sourceExercises.all {
            it.contentRevision == CONTENT_REVISION && it.generatorRevision == GENERATOR_REVISION
        }) { "Follow-up source exercises must use $CONTENT_REVISION/$GENERATOR_REVISION" }

        val sourceByTarget = sourceExercises.groupBy(::targetKey)
        require(selectedTargetKeys.all { it in sourceByTarget }) {
            "Every follow-up target must have been assessed in the source practice"
        }
        require(selectedTargetKeys.all { it in supportedTargetKeys }) { "Unsupported follow-up target" }

        val descriptors = selectedTargetKeys.associateWith { key -> targetDescriptor(sourceByTarget.getValue(key).first()) }
        val targetSequence = targetSequence(selectedTargetKeys, seed)
        val usedSentenceIds = sourceByTarget.mapValues { (_, exercises) ->
            exercises.mapNotNullTo(mutableSetOf()) { it.inspector.canonicalSentenceId }
        }.toMutableMap()
        val occurrences = mutableMapOf<String, Int>()
        val exercises = targetSequence.map { key ->
            val occurrence = occurrences.getOrDefault(key, 0)
            occurrences[key] = occurrence + 1
            val exercise = exerciseFor(key, seed, occurrence, usedSentenceIds.getOrPut(key, ::mutableSetOf))
            exercise.inspector.canonicalSentenceId?.let(usedSentenceIds.getValue(key)::add)
            exercise
        }
        val items = exercises.mapIndexed { position, exercise ->
            practiceItem(position, exercise, descriptors.getValue(targetSequence[position]))
        }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = planRevision,
            seed = seed,
            title = "Review selected targets",
            prerequisiteSkillIds = listOf("G001", "G002", "G003", "G004"),
            introduction = exercises.flatMap { it.introduction }.distinct(),
            items = items,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
        )
    }

    private fun targetSequence(selectedTargetKeys: List<String>, seed: Int): List<String> {
        val grammar = selectedTargetKeys.filter { it.startsWith(GRAMMAR_PREFIX) }
        val desiredSize = if (grammar.isEmpty()) selectedTargetKeys.size else maxOf(MIN_GRAMMAR_RUN_SIZE, selectedTargetKeys.size)
        val expanded = selectedTargetKeys.toMutableList()
        var filler = seed.mod(grammar.size.coerceAtLeast(1))
        while (expanded.size < desiredSize) {
            expanded += grammar[filler % grammar.size]
            filler += 1
        }
        val remaining = expanded.groupingBy { it }.eachCount().toMutableMap()
        val seededPriority = remaining.keys.associateWith { stableRank(seed, it) }
        return buildList {
            while (remaining.isNotEmpty()) {
                val alternatives = remaining.keys.filter { it != lastOrNull() }.ifEmpty { remaining.keys }
                val next = alternatives.sortedWith(
                    compareByDescending<String> { remaining.getValue(it) }
                        .thenBy { seededPriority.getValue(it) }
                        .thenBy { it },
                ).first()
                add(next)
                val count = remaining.getValue(next) - 1
                if (count == 0) remaining.remove(next) else remaining[next] = count
            }
        }
    }

    private fun stableRank(seed: Int, key: String): Long {
        var value = FNV_OFFSET
        "$seed:$key".forEach { character ->
            value = (value xor character.code.toULong()) * FNV_PRIME
        }
        return value.toLong()
    }

    private fun exerciseFor(
        targetKey: String,
        seed: Int,
        occurrence: Int,
        excludedSentenceIds: Set<String>,
    ): GeneratedExercise = lexicalTarget(targetKey)?.let { target ->
        val formatId = if (target.direction == "da-en") "E007" else "E008"
        (0 until LEXICAL_SCAN_SIZE).asSequence()
            .map { engine.generate(currentRequest(formatId, null, seed, it)) }
            .first { it.lexicalTarget == target }
    } ?: run {
        val targetSkillId = targetKey.removePrefix(GRAMMAR_PREFIX)
        val formatId = when (targetSkillId) {
            "G005" -> "E003"
            "G006" -> if ((seed + occurrence).mod(2) == 0) "E002" else "E006"
            else -> error("Unsupported follow-up grammar target: $targetSkillId")
        }
        (0 until GRAMMAR_SCAN_SIZE).asSequence()
            .map { engine.generate(currentRequest(formatId, targetSkillId, seed, it)) }
            .firstOrNull { exercise ->
                val lexicalIds = exercise.inspector.lexicalIds
                exercise.inspector.canonicalSentenceId !in excludedSentenceIds &&
                    READ_ENTRY_ID in lexicalIds && lexicalIds.any { it in REVIEW_NOUN_IDS }
            } ?: error("No unused licensed sentence for $targetSkillId and $formatId")
    }

    private fun lexicalTarget(key: String): LexicalTarget? {
        if (!key.startsWith(LEXICAL_PREFIX)) return null
        val parts = key.split(":")
        require(parts.size == LEXICAL_KEY_PARTS) { "Invalid lexical follow-up target: $key" }
        return LexicalTarget(
            parts[LEXICAL_ENTRY_INDEX],
            parts[LEXICAL_SENSE_INDEX],
            parts[LEXICAL_FACET_INDEX],
            parts[LEXICAL_DIRECTION_INDEX],
        )
    }

    private fun currentRequest(formatId: String, targetSkillId: String?, seed: Int, index: Int) =
        GenerationRequest(
            formatId = formatId,
            targetSkillId = targetSkillId,
            seed = seed,
            index = index,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
        )

    private fun targetKey(exercise: GeneratedExercise) =
        PracticeTargetKeys.key(exercise.targetSkillId, exercise.lexicalTarget)

    private fun targetDescriptor(exercise: GeneratedExercise) =
        PracticeTargetDescriptor(
            label = exercise.targetSkillId?.let { target ->
                PilotContent.skills.single { it.id == target }.title
            } ?: requireNotNull(exercise.inspector.lexicalSource).run {
                val target = requireNotNull(exercise.lexicalTarget)
                if (target.direction == "da-en") "$headword — $gloss" else "$gloss — $headword"
            },
            targetSkillId = exercise.targetSkillId,
            lexicalTarget = exercise.lexicalTarget,
        )

    private fun practiceItem(position: Int, exercise: GeneratedExercise, target: PracticeTargetDescriptor): PracticePlanItem {
        val answerKeys = answerExposureKeys(exercise)
        val contextualKeys = contextualExposureKeys(exercise)
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = target,
            answerExposureKeys = answerKeys,
            promptExposureKeys = promptExposureKeys(exercise, contextualKeys),
            lessonExposureKeys = (answerKeys + contextualKeys).distinct(),
            hintExposureKeys = if (exercise.formatId == "E003") answerKeys else emptyList(),
            feedbackExposureKeys = when (exercise.formatId) {
                "E008" -> (answerKeys + articleKey(requireNotNull(exercise.lexicalTarget).entryId)).distinct()
                "E002", "E003", "E006" ->
                    (answerKeys + contextualKeys.filterNot { it.startsWith("gloss:") }).distinct()
                else -> answerKeys
            },
        )
    }

    private fun answerExposureKeys(exercise: GeneratedExercise): List<String> =
        when (exercise.formatId) {
            "E007" -> listOf(glossKey(requireNotNull(exercise.lexicalTarget).senseId))
            "E008" -> listOf(headwordKey(requireNotNull(exercise.lexicalTarget).senseId))
            "E003" -> listOf(grammarKey("G005"))
            "E002", "E006" -> listOf(articleKey(grammarNoun(exercise).entryId))
            else -> error("Unsupported follow-up format ${exercise.formatId}")
        }

    private fun promptExposureKeys(exercise: GeneratedExercise, contextualKeys: List<String>): List<String> =
        when (exercise.formatId) {
            "E007" -> PilotContent.vocabularySenses.map { glossKey(it.senseId) } +
                headwordKey(requireNotNull(exercise.lexicalTarget).senseId)
            "E008" -> listOf(glossKey(requireNotNull(exercise.lexicalTarget).senseId))
            else -> contextualKeys
        }.distinct()

    private fun contextualExposureKeys(exercise: GeneratedExercise): List<String> =
        when (exercise.formatId) {
            "E007", "E008" -> requireNotNull(exercise.lexicalTarget).run {
                listOf(headwordKey(senseId), articleKey(entryId), glossKey(senseId))
            }
            else -> grammarNoun(exercise).run {
                listOf(headwordKey(senseId), articleKey(entryId), glossKey(senseId))
            }
        }

    private fun grammarNoun(exercise: GeneratedExercise): LexicalSense {
        val nounEntryId = exercise.inspector.lexicalIds.single { it in REVIEW_NOUN_IDS }
        return PilotContent.vocabularySenses.single { it.entryId == nounEntryId }
    }

    private fun glossKey(senseId: String) = "gloss:$senseId"
    private fun headwordKey(senseId: String) = "headword:$senseId"
    private fun articleKey(entryId: String) = "article:$entryId"
    private fun grammarKey(skillId: String) = "grammar:$skillId"

    private companion object {
        const val FOCUS_ID = "targeted-review"
        const val PLAN_REVISION = "follow-up-1"
        const val CONTENT_REVISION = "pilot-3"
        const val GENERATOR_REVISION = "generator-3"
        const val EVALUATION_REVISION = "evaluation-3"
        const val GRAMMAR_PREFIX = "grammar:"
        const val LEXICAL_PREFIX = "lexical:"
        const val LEXICAL_KEY_PARTS = 5
        const val LEXICAL_ENTRY_INDEX = 1
        const val LEXICAL_SENSE_INDEX = 2
        const val LEXICAL_FACET_INDEX = 3
        const val LEXICAL_DIRECTION_INDEX = 4
        const val LEXICAL_SCAN_SIZE = 3
        const val GRAMMAR_SCAN_SIZE = 99
        const val MIN_GRAMMAR_RUN_SIZE = 4
        const val MAX_ITEMS = 8
        const val READ_ENTRY_ID = "L011"
        val REVIEW_NOUN_IDS = PilotContent.vocabularySenses.mapTo(mutableSetOf()) { it.entryId }
        const val FNV_OFFSET = 0xcbf29ce484222325UL
        const val FNV_PRIME = 0x100000001b3UL
        val supportedTargetKeys = buildSet {
            add("grammar:G005")
            add("grammar:G006")
            PilotContent.vocabularySenses.forEach { sense ->
                add("lexical:${sense.entryId}:${sense.senseId}:meaning:da-en")
                add("lexical:${sense.entryId}:${sense.senseId}:singular-headword:en-da")
            }
        }
    }
}
