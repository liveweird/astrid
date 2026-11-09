package app.astrid.engine

class PracticeFollowUpPlanner(private val engine: PilotEngine = PilotEngine()) {
    fun planGuidedGaps(
        planRevision: String,
        seed: Int,
        selectedTargetKeys: List<String>,
        sourceItems: List<PracticeFollowUpSourceItem>,
    ): PracticePlan {
        require(planRevision == GUIDED_GAP_PLAN_REVISION) { "Unsupported guided-gap follow-up plan revision: $planRevision" }
        require(seed >= 0) { "seed must be non-negative" }
        require(selectedTargetKeys.isNotEmpty()) { "At least one follow-up target is required" }
        require(selectedTargetKeys.distinct().size == selectedTargetKeys.size) { "Follow-up targets must be unique" }
        require(sourceItems.isNotEmpty()) { "A follow-up requires source items" }
        val sourceAssessments = sourceItems.flatMap { source ->
            requireGuidedSource(source)
            source.assessments
        }
        val sourceKeys = sourceAssessments.map { it.targetDescriptor.targetKey() }.toSet()
        require(selectedTargetKeys.all { it in sourceKeys && it in GUIDED_SOURCE_TARGET_KEYS }) {
            "Every guided-gap follow-up target must have been assessed in the source practice"
        }
        val mappedKeys = selectedTargetKeys.map(::guidedChildKey).distinct()
        require(mappedKeys.size == selectedTargetKeys.size) { "Selected targets must map to distinct follow-up operations" }
        val base = mappedKeys.rotate(seed.mod(mappedKeys.size))
        val sequence = List(GUIDED_FOLLOW_UP_ITEM_COUNT) { base[it.mod(base.size)] }
        val excludedSentenceIds = sourceItems.mapNotNullTo(mutableSetOf()) { it.exercise.inspector.canonicalSentenceId }
        val items = sequence.mapIndexed { position, key ->
            val descriptor = if (key == GUIDED_VERB_TARGET_KEY) GUIDED_VERB_DESCRIPTOR else GUIDED_ARTICLE_DESCRIPTOR
            val formatId = if (key == GUIDED_VERB_TARGET_KEY) "E001" else "E002"
            val exercise = guidedChildExercise(formatId, descriptor.targetSkillId!!, seed, position, excludedSentenceIds)
            excludedSentenceIds += requireNotNull(exercise.inspector.canonicalSentenceId)
            guidedChildItem(position, exercise, descriptor)
        }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = planRevision,
            seed = seed,
            title = "Review selected targets",
            prerequisiteSkillIds = listOf("G001", "G002", "G003", "G005"),
            introduction = items.flatMap { it.exercise.introduction }.distinct(),
            items = items,
            contentRevision = GUIDED_CONTENT_REVISION,
            generatorRevision = GUIDED_GENERATOR_REVISION,
            evaluationRevision = GUIDED_EVALUATION_REVISION,
        )
    }

    private fun requireGuidedSource(source: PracticeFollowUpSourceItem) {
        require(
            source.exercise.contentRevision == GUIDED_CONTENT_REVISION &&
                source.exercise.generatorRevision == GUIDED_GENERATOR_REVISION,
        ) { "Guided-gap follow-up sources must use $GUIDED_CONTENT_REVISION/$GUIDED_GENERATOR_REVISION" }
        val expected = when (source.exercise.formatId) {
            "E015" -> listOf(
                GuidedAssessmentIdentity("g0", GUIDED_VERB_DESCRIPTOR, "primary"),
                GuidedAssessmentIdentity("g1", GUIDED_ARTICLE_SELECTION_DESCRIPTOR, "support"),
            )
            "E001" -> listOf(GuidedAssessmentIdentity(null, GUIDED_VERB_DESCRIPTOR, "primary"))
            "E002" -> listOf(GuidedAssessmentIdentity(null, GUIDED_ARTICLE_DESCRIPTOR, "primary"))
            else -> throw IllegalArgumentException("Unsupported guided-gap source format: ${source.exercise.formatId}")
        }
        require(source.targetDescriptor == expected.first().targetDescriptor) {
            "Guided-gap source item descriptor must match its primary assessment"
        }
        require(source.exercise.targetSkillId == expected.first().targetDescriptor.targetSkillId) {
            "Guided-gap source exercise target must match its primary assessment"
        }
        require(source.assessments.map { it.guidedIdentity() } == expected) {
            "Guided-gap source assessments must match the exercise format exactly"
        }
    }

    private fun PracticeAssessment.guidedIdentity() =
        GuidedAssessmentIdentity(slotId, targetDescriptor, assessmentRole)

    private fun guidedChildKey(sourceKey: String): String = when (sourceKey) {
        GUIDED_VERB_TARGET_KEY -> GUIDED_VERB_TARGET_KEY
        in GUIDED_ARTICLE_SOURCE_TARGET_KEYS -> GUIDED_ARTICLE_TARGET_KEY
        else -> error("Unsupported guided-gap target $sourceKey")
    }

    private fun guidedChildExercise(
        formatId: String,
        targetSkillId: String,
        seed: Int,
        position: Int,
        excludedSentenceIds: Set<String>,
    ): GeneratedExercise =
        (0 until GRAMMAR_SCAN_SIZE).asSequence()
            .map { engine.generate(guidedRequest(formatId, targetSkillId, seed, it + position)) }
            .firstOrNull {
                it.inspector.canonicalSentenceId !in excludedSentenceIds &&
                    (formatId != "E002" || it.inspector.lexicalIds.any(REVIEW_NOUN_IDS::contains))
            }
            ?: error("No unused licensed sentence for $targetSkillId and $formatId")

    private fun guidedRequest(formatId: String, targetSkillId: String, seed: Int, index: Int) = GenerationRequest(
        formatId,
        targetSkillId,
        seed,
        index,
        GUIDED_CONTENT_REVISION,
        GUIDED_GENERATOR_REVISION,
    )

    private fun guidedChildItem(
        position: Int,
        exercise: GeneratedExercise,
        descriptor: PracticeTargetDescriptor,
    ): PracticePlanItem {
        val answerKey: String
        val promptKeys: List<String>
        val lessonKeys: List<String>
        val conceptKey = "grammar:${descriptor.targetSkillId}"
        if (exercise.formatId == "E001") {
            val verb = presentVerb(exercise)
            answerKey = "verb-present:${verb.lexicalId}"
            val infinitiveKey = "verb-infinitive:${verb.lexicalId}"
            promptKeys = listOf(infinitiveKey, answerKey)
            lessonKeys = listOf(infinitiveKey, answerKey, conceptKey)
        } else {
            val noun = grammarNoun(exercise)
            answerKey = articleKey(noun.entryId)
            promptKeys = listOf(headwordKey(noun.senseId), glossKey(noun.senseId))
            lessonKeys = (promptKeys + answerKey + conceptKey).distinct()
        }
        val assessment = PracticeAssessment(null, descriptor, "primary", listOf(answerKey))
        return PracticePlanItem(
            position,
            exercise,
            descriptor,
            listOf(answerKey),
            promptKeys,
            lessonKeys,
            listOf(conceptKey),
            listOf(answerKey, conceptKey),
            listOf(assessment),
        )
    }


    fun planPresentVerbs(
        planRevision: String,
        seed: Int,
        selectedTargetKeys: List<String>,
        sourceItems: List<PracticeFollowUpSourceItem>,
    ): PracticePlan {
        require(planRevision == PRESENT_VERBS_PLAN_REVISION) {
            "Unsupported present-verb follow-up plan revision: $planRevision"
        }
        require(seed >= 0) { "seed must be non-negative" }
        require(selectedTargetKeys.isNotEmpty()) { "At least one follow-up target is required" }
        require(selectedTargetKeys.distinct().size == selectedTargetKeys.size) { "Follow-up targets must be unique" }
        require(selectedTargetKeys.all { it in PRESENT_VERB_TARGET_KEYS }) { "Unsupported present-verb follow-up target" }
        require(sourceItems.isNotEmpty()) { "A follow-up requires source items" }
        validatePresentVerbSources(sourceItems)

        val sourceByTarget = sourceItems.groupBy { it.targetDescriptor.targetKey() }
        require(selectedTargetKeys.all { it in sourceByTarget }) {
            "Every follow-up target must have been assessed in the source practice"
        }
        val modes = presentVerbModeSequence(selectedTargetKeys, seed)
        val excludedSentenceIds = sourceItems.mapNotNullTo(mutableSetOf()) {
            it.exercise.inspector.canonicalSentenceId
        }
        val verbs = PilotContent.verbs.rotate(seed.mod(PilotContent.verbs.size))
        val exercises = modes.mapIndexed { position, mode ->
            presentVerbExercise(
                formatId = PRESENT_VERB_FORMAT_BY_MODE.getValue(mode),
                seed = seed,
                position = position,
                verbId = verbs[position.mod(verbs.size)].lexicalId,
                excludedSentenceIds = excludedSentenceIds,
            ).also { exercise -> excludedSentenceIds += requireNotNull(exercise.inspector.canonicalSentenceId) }
        }
        val descriptors = selectedTargetKeys.associateWith { sourceByTarget.getValue(it).first().targetDescriptor }
        val items = exercises.mapIndexed { position, exercise ->
            presentVerbPracticeItem(position, exercise, descriptors.getValue(targetKeyForMode(modes[position])))
        }
        return PracticePlan(
            focusId = FOCUS_ID,
            planRevision = planRevision,
            seed = seed,
            title = "Review selected targets",
            prerequisiteSkillIds = listOf("G001", "G002", "G003"),
            introduction = exercises.flatMap { it.introduction }.distinct(),
            items = items,
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
        )
    }

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

    private fun validatePresentVerbSources(sourceItems: List<PracticeFollowUpSourceItem>) {
        require(sourceItems.all {
            it.exercise.contentRevision == CONTENT_REVISION && it.exercise.generatorRevision == GENERATOR_REVISION
        }) { "Follow-up source exercises must use $CONTENT_REVISION/$GENERATOR_REVISION" }
        sourceItems.forEach { source ->
            val mode = requireNotNull(source.targetDescriptor.assessmentMode) {
                "Present-verb source targets must freeze their assessment mode"
            }
            require(source.targetDescriptor.targetSkillId == "G004" && source.targetDescriptor.lexicalTarget == null) {
                "Present-verb source targets must identify grammar skill G004"
            }
            require(source.exercise.targetSkillId == "G004") { "Present-verb source exercises must assess G004" }
            require(PRESENT_VERB_FORMAT_BY_MODE[mode] == source.exercise.formatId) {
                "Present-verb source assessment mode must match its exercise format"
            }
        }
    }

    private fun presentVerbModeSequence(selectedTargetKeys: List<String>, seed: Int): List<String> {
        val selectedModes = selectedTargetKeys.map { it.removePrefix(PRESENT_VERB_TARGET_PREFIX) }
        val modes = selectedModes.rotate(seed.mod(selectedModes.size))
        val desiredSize = maxOf(MIN_GRAMMAR_RUN_SIZE, selectedModes.size * PRESENT_VERB_ITEMS_PER_MODE)
        return List(desiredSize) { modes[it.mod(modes.size)] }
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

    private fun presentVerbPracticeItem(
        position: Int,
        exercise: GeneratedExercise,
        descriptor: PracticeTargetDescriptor,
    ): PracticePlanItem {
        val presentKey = presentVerbKey(exercise)
        val conceptKey = grammarKey("G004")
        val infinitiveKey = infinitiveVerbKey(exercise)
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = descriptor,
            answerExposureKeys = listOf(presentKey),
            promptExposureKeys = when (exercise.formatId) {
                "E001" -> listOf(infinitiveKey, presentKey)
                "E004", "E005" -> listOf(infinitiveKey)
                else -> error("Unsupported present-verb format ${exercise.formatId}")
            },
            lessonExposureKeys = listOf(infinitiveKey, presentKey, conceptKey),
            hintExposureKeys = listOf(conceptKey),
            feedbackExposureKeys = listOf(presentKey, conceptKey),
        )
    }

    private fun presentVerb(exercise: GeneratedExercise): Verb =
        PilotContent.verbs.single { it.lexicalId in exercise.inspector.lexicalIds }

    private fun presentVerbKey(exercise: GeneratedExercise): String = "verb-present:${presentVerb(exercise).lexicalId}"

    private fun infinitiveVerbKey(exercise: GeneratedExercise): String = "verb-infinitive:${presentVerb(exercise).lexicalId}"

    private fun targetKeyForMode(mode: String): String = "$PRESENT_VERB_TARGET_PREFIX$mode"

    private fun <T> List<T>.rotate(offset: Int): List<T> = drop(offset) + take(offset)

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
        const val PRESENT_VERBS_PLAN_REVISION = "follow-up-2"
        const val GUIDED_GAP_PLAN_REVISION = "follow-up-3"
        const val CONTENT_REVISION = "pilot-3"
        const val GENERATOR_REVISION = "generator-3"
        const val EVALUATION_REVISION = "evaluation-3"
        const val GUIDED_CONTENT_REVISION = "pilot-7"
        const val GUIDED_GENERATOR_REVISION = "generator-7"
        const val GUIDED_EVALUATION_REVISION = "evaluation-7"
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
        const val PRESENT_VERB_ITEMS_PER_MODE = 2
        const val MAX_ITEMS = 8
        const val READ_ENTRY_ID = "L011"
        val REVIEW_NOUN_IDS = PilotContent.vocabularySenses.mapTo(mutableSetOf()) { it.entryId }
        const val FNV_OFFSET = 0xcbf29ce484222325UL
        const val FNV_PRIME = 0x100000001b3UL
        const val PRESENT_VERB_TARGET_PREFIX = "grammar:G004:"
        const val GUIDED_FOLLOW_UP_ITEM_COUNT = 4
        const val GUIDED_VERB_TARGET_KEY = "grammar:G004:selection"
        const val GUIDED_ARTICLE_TARGET_KEY = "grammar:G006:recall"
        val GUIDED_ARTICLE_SOURCE_TARGET_KEYS = setOf("grammar:G006:selection", GUIDED_ARTICLE_TARGET_KEY)
        val GUIDED_SOURCE_TARGET_KEYS = GUIDED_ARTICLE_SOURCE_TARGET_KEYS + GUIDED_VERB_TARGET_KEY
        val GUIDED_VERB_DESCRIPTOR = PracticeTargetDescriptor("Present verb selection", "G004", null, "selection")
        val GUIDED_ARTICLE_SELECTION_DESCRIPTOR =
            PracticeTargetDescriptor("Indefinite article selection", "G006", null, "selection")
        val GUIDED_ARTICLE_DESCRIPTOR =
            PracticeTargetDescriptor("Written indefinite article recall", "G006", null, "recall")
        val PRESENT_VERB_FORMAT_BY_MODE = mapOf(
            "selection" to "E001",
            "recall" to "E004",
            "repair" to "E005",
        )
        val PRESENT_VERB_TARGET_KEYS = PRESENT_VERB_FORMAT_BY_MODE.keys.mapTo(mutableSetOf()) {
            "$PRESENT_VERB_TARGET_PREFIX$it"
        }
        val supportedTargetKeys = buildSet {
            add("grammar:G005")
            add("grammar:G006")
            PilotContent.vocabularySenses.forEach { sense ->
                add("lexical:${sense.entryId}:${sense.senseId}:meaning:da-en")
                add("lexical:${sense.entryId}:${sense.senseId}:singular-headword:en-da")
            }
        }
    }

    private data class GuidedAssessmentIdentity(
        val slotId: String?,
        val targetDescriptor: PracticeTargetDescriptor,
        val assessmentRole: String,
    )
}
