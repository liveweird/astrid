package app.astrid.engine

data class CumulativeReviewSourceItem(
    val sourcePracticeSessionId: String,
    val sourceLessonId: String,
    val sourcePosition: Int,
    val sourceFocusId: String,
    val sourcePlanRevision: String,
    val sourceIntroduction: List<IntroductionSection>,
    val sourceIntroductionExposureKeys: List<String>,
    val item: PracticePlanItem,
    val policy: PracticeItemPolicy,
)

data class CumulativeReviewSelection(
    val candidateKey: String,
    val semanticId: String,
    val sourcePracticeSessionId: String,
    val sourcePosition: Int,
    val sourceLessonId: String,
)

data class CumulativeReviewResult(
    val plan: PracticePlan,
    val selections: List<CumulativeReviewSelection>,
    val sourceLessonCount: Int,
)

/** Builds the bounded mixed review from tasks proven introduced by saved source lessons. */
class CumulativeReviewPlanner(
    private val engine: PilotEngine = PilotEngine(),
    private val planRevision: String = PLAN_REVISION_ONE,
) {
    init {
        require(planRevision in setOf(PLAN_REVISION_ONE, PLAN_REVISION_TWO)) { "Unsupported cumulative-review revision" }
    }

    fun plan(
        seed: Int,
        sources: List<CumulativeReviewSourceItem>,
        previousReleaseRanks: Map<String, Long> = emptyMap(),
    ): CumulativeReviewResult? {
        require(seed >= 0) { "seed must be non-negative" }
        val usableCandidates = sources.mapNotNull { source ->
            val semanticId = candidateSemanticIdOrNull(source.item) ?: return@mapNotNull null
            source.item.rebased(seed, semanticId, source)?.let { rebased ->
                Candidate(source, semanticId, source.item.targetDescriptor.targetKey(), rebased)
            }
        }
        val sourceLessonCount = usableCandidates.map { it.source.sourceLessonId }.distinct().size
        val candidates = usableCandidates
            .sortedWith(compareBy({ it.key }, { it.source.sourcePracticeSessionId }, { it.source.sourcePosition }))
            .distinctBy(Candidate::key)
        if (candidates.isEmpty()) return null

        val rotated = candidates.rotate(seed.mod(candidates.size))
        val remaining = rotated.toMutableList()
        val selected = mutableListOf<Candidate>()
        while (remaining.isNotEmpty() && selected.size < ITEM_LIMIT) {
            val diversityComparator = compareBy<IndexedValue<Candidate>>(
                { if (it.value.semanticId !in previousReleaseRanks) 1 else 0 },
                { if (selected.none { picked -> picked.source.sourceLessonId == it.value.source.sourceLessonId }) 1 else 0 },
                { if (selected.lastOrNull()?.targetKey != it.value.targetKey) 1 else 0 },
                { if (selected.lastOrNull()?.source?.item?.exercise?.formatId != it.value.source.item.exercise.formatId) 1 else 0 },
                { if (selected.none { picked -> picked.targetKey == it.value.targetKey }) 1 else 0 },
            ).thenComparator { left, right ->
                // Smaller ranks were released less recently. Compare in reverse so maxWith chooses the oldest.
                compareValues(
                    previousReleaseRanks[right.value.semanticId] ?: Long.MAX_VALUE,
                    previousReleaseRanks[left.value.semanticId] ?: Long.MAX_VALUE,
                )
            }.thenBy { -it.index }
            val chosen = remaining.withIndex().maxWithOrNull(
                diversityComparator,
            )!!.value
            selected += chosen
            remaining.remove(chosen)
        }

        val items = selected.mapIndexed { position, candidate -> candidate.rebasedItem.copy(position = position) }
        val selectedSources = selected.distinctBy { it.source.sourcePracticeSessionId }
        return CumulativeReviewResult(
            PracticePlan(
                focusId = FOCUS_ID,
                planRevision = planRevision,
                seed = seed,
                title = TITLE,
                prerequisiteSkillIds = emptyList(),
                introduction = selectedSources.flatMap { it.source.sourceIntroduction }.distinct(),
                items = items,
                contentRevision = PER_ITEM_REVISION,
                generatorRevision = PER_ITEM_REVISION,
                evaluationRevision = PER_ITEM_REVISION,
                introductionExposureKeys = selectedSources.flatMap { it.source.sourceIntroductionExposureKeys }.distinct(),
                introductionRequired = true,
            ),
            selected.map {
                CumulativeReviewSelection(
                    it.key,
                    it.semanticId,
                    it.source.sourcePracticeSessionId,
                    it.source.sourcePosition,
                    it.source.sourceLessonId,
                )
            },
            sourceLessonCount,
        )
    }

    private fun PracticePlanItem.rebased(
        seed: Int,
        expectedSemanticId: String,
        source: CumulativeReviewSourceItem,
    ): PracticePlanItem? = when (exercise.source) {
        is VocabularyP1ExerciseSource -> rebaseVocabularyP1(seed, expectedSemanticId, source)
        is VocabularyP2ExerciseSource -> rebaseVocabularyP2(seed, expectedSemanticId, source)
        else -> rebaseSamePair(seed, expectedSemanticId, source)
    }

    private fun PracticePlanItem.rebaseVocabularyP1(
        seed: Int,
        expectedSemanticId: String,
        source: CumulativeReviewSourceItem,
    ): PracticePlanItem? {
        val currentRevision = source.sourceFocusId + "-2"
        val current = PracticePlanner(engine).plan(source.sourceFocusId, currentRevision, seed)
        val candidate = current.items.singleOrNull { candidateSemanticId(it) == expectedSemanticId }
        return candidate?.takeIf { authoredTemplateEquivalent(exercise, it.exercise) }?.copy(policy = source.policy)
    }

    private fun PracticePlanItem.rebaseVocabularyP2(
        seed: Int,
        expectedSemanticId: String,
        source: CumulativeReviewSourceItem,
    ): PracticePlanItem? {
        val current = PracticePlanner(engine).plan(source.sourceFocusId, source.sourcePlanRevision, seed)
        val candidate = current.items.singleOrNull { candidateSemanticIdOrNull(it) == expectedSemanticId }
        return candidate?.takeIf { authoredTemplateEquivalent(exercise, it.exercise) }?.copy(policy = source.policy)
    }

    private fun PracticePlanItem.rebaseSamePair(
        seed: Int,
        expectedSemanticId: String,
        source: CumulativeReviewSourceItem,
    ): PracticePlanItem {
        val contentRevision = exercise.contentRevision
        val generatorRevision = exercise.generatorRevision
        val generated = (0 until SCAN_LIMIT).asSequence().map { index ->
            engine.generate(
                GenerationRequest(
                    exercise.formatId,
                    exercise.targetSkillId,
                    seed,
                    index,
                    contentRevision,
                    generatorRevision,
                ),
            )
        }.firstOrNull {
            semanticId(it, targetDescriptor.targetKey()) == expectedSemanticId && authoredTemplateEquivalent(exercise, it)
        } ?: exercise
        return copy(exercise = generated, policy = source.policy)
    }

    private fun authoredTemplateEquivalent(source: GeneratedExercise, candidate: GeneratedExercise): Boolean {
        val headersMatch = listOf(
            source.formatId == candidate.formatId,
            source.targetSkillId == candidate.targetSkillId,
            source.supportSkillIds == candidate.supportSkillIds,
            source.targetScopeId == candidate.targetScopeId,
        ).all { it }
        return headersMatch && sameAuthoredSource(source.source, candidate.source)
    }

    private fun sameAuthoredSource(source: ExerciseSource, candidate: ExerciseSource): Boolean =
        sameCourseSource(source, candidate) ?: sameFoundationSource(source, candidate)

    private fun sameCourseSource(source: ExerciseSource, candidate: ExerciseSource): Boolean? =
        sameEarlyCourseSource(source, candidate) ?: sameLateCourseSource(source, candidate)

    private fun sameEarlyCourseSource(source: ExerciseSource, candidate: ExerciseSource): Boolean? = when (source) {
        is VocabularyP1ExerciseSource -> candidate is VocabularyP1ExerciseSource && source.task == candidate.task
        is VocabularyP2ExerciseSource -> candidate is VocabularyP2ExerciseSource && source.task == candidate.task
        is ModuleOneExerciseSource -> candidate is ModuleOneExerciseSource && source.task == candidate.task
        is InanimateReferenceExerciseSource ->
            candidate is InanimateReferenceExerciseSource && source.task == candidate.task
        is PresentOrderExerciseSource -> candidate is PresentOrderExerciseSource && source.task == candidate.task
        else -> null
    }

    private fun sameLateCourseSource(source: ExerciseSource, candidate: ExerciseSource): Boolean? = when (source) {
        is AbilityExerciseSource -> candidate is AbilityExerciseSource && source.task == candidate.task
        is ModalInterpretationExerciseSource ->
            candidate is ModalInterpretationExerciseSource && source.task == candidate.task
        is CircumstanceExerciseSource -> candidate is CircumstanceExerciseSource && source.task == candidate.task
        is QuestionExerciseSource -> candidate is QuestionExerciseSource && source.task == candidate.task
        else -> null
    }

    private fun sameFoundationSource(source: ExerciseSource, candidate: ExerciseSource): Boolean = when (source) {
        is LexicalSource -> candidate is LexicalSource && source.sense == candidate.sense
        is SentenceSource -> candidate is SentenceSource && source.sentence == candidate.sentence
        is ReadingSource -> candidate is ReadingSource && source == candidate
        is TileExerciseSource -> candidate is TileExerciseSource && source.sentence == candidate.sentence
        is MatchingExerciseSource -> candidate is MatchingExerciseSource
        is MultipleGapExerciseSource -> candidate is MultipleGapExerciseSource && source.sentence == candidate.sentence
        else -> false
    }

    fun candidateSemanticId(item: PracticePlanItem): String =
        requireNotNull(candidateSemanticIdOrNull(item)) { "Course task must have a stable task id" }

    fun candidateSemanticIdOrNull(item: PracticePlanItem): String? {
        if (item.exercise.source is VocabularyP2ExerciseSource && planRevision == PLAN_REVISION_ONE) return null
        return semanticId(item.exercise, item.targetDescriptor.targetKey())
    }

    private fun semanticId(exercise: GeneratedExercise, targetKey: String): String {
        val task = courseTaskId(exercise.source) ?: foundationTaskId(exercise, targetKey)
        return listOf(exercise.formatId, task, targetKey).joinToString("|")
    }

    private fun courseTaskId(source: ExerciseSource): String? = when (source) {
        is VocabularyP1ExerciseSource -> source.task.id
        is VocabularyP2ExerciseSource -> source.task.id
        is ModuleOneExerciseSource -> source.task.id
        is InanimateReferenceExerciseSource -> source.task.id
        is PresentOrderExerciseSource -> source.task.id
        is AbilityExerciseSource -> source.task.id
        is ModalInterpretationExerciseSource -> source.task.id
        is CircumstanceExerciseSource -> source.task.id
        is QuestionExerciseSource -> source.task.id
        else -> null
    }

    private fun foundationTaskId(exercise: GeneratedExercise, targetKey: String): String = when (val source = exercise.source) {
        is LexicalSource -> "${source.sense.entryId}:${source.sense.senseId}"
        is SentenceSource -> source.sentence.id
        is ReadingSource -> exercise.inspector.canonicalSentenceId ?: source.propositions.joinToString(",") { it.sentence.id }
        is TileExerciseSource -> exercise.inspector.canonicalSentenceId ?: error("Missing tile semantic identity")
        is MatchingExerciseSource -> exercise.inspector.canonicalSentenceId ?: targetKey
        is MultipleGapExerciseSource -> source.sentence.id
        else -> error("Course task must have a stable task id")
    }

    private fun <T> List<T>.rotate(offset: Int): List<T> = drop(offset) + take(offset)

    private data class Candidate(
        val source: CumulativeReviewSourceItem,
        val semanticId: String,
        val targetKey: String,
        val rebasedItem: PracticePlanItem,
    ) {
        val key = "$semanticId|$targetKey"
    }

    companion object {
        const val FOCUS_ID = "cumulative-review"
        const val PLAN_REVISION_ONE = "cumulative-review-1"
        const val PLAN_REVISION_TWO = "cumulative-review-2"
        const val PLAN_REVISION = PLAN_REVISION_ONE
        const val TITLE = "Mixed review from your lessons"
        private const val ITEM_LIMIT = 8
        private const val SCAN_LIMIT = 512
        private const val PER_ITEM_REVISION = "per-item"
    }
}
