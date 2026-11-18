package app.astrid.engine

data class VocabularyLibraryPracticeCounts(
    val recognitionCount: Int,
    val recallCount: Int,
)

/** Builds bounded practice from exact sense-and-form claims recorded by saved lesson teaching. */
object VocabularyLibraryPracticePlanner {
    const val FOCUS_RECOGNITION = "vocabulary-library-recognition"
    const val FOCUS_RECALL = "vocabulary-library-recall"
    const val REVISION = "vocabulary-library-practice-1"

    fun counts(claims: List<VocabularyTeachingClaim>): VocabularyLibraryPracticeCounts =
        VocabularyLibraryPracticeCounts(
            recognitionCount = eligible(RECOGNITION, claims).size,
            recallCount = eligible(RECALL, claims).size,
        )

    fun plan(
        engine: PilotEngine,
        focusId: String,
        seed: Int,
        claims: List<VocabularyTeachingClaim>,
    ): PracticePlan? {
        require(seed >= 0) { "seed must be non-negative" }
        val candidates = when (focusId) {
            FOCUS_RECOGNITION -> RECOGNITION
            FOCUS_RECALL -> RECALL
            else -> throw IllegalArgumentException("Unsupported vocabulary-library practice focus: $focusId")
        }
        val selected = eligible(candidates, claims)
        if (selected.isEmpty()) return null

        val sourcePlans = selected.map(Candidate::sourceFocusId).distinct().associateWith { sourceFocusId ->
            VocabularyP1PracticePlanner(engine).plan(sourceFocusId, "$sourceFocusId-2", seed)
        }
        val items = selected.rotate(seed.mod(selected.size)).mapIndexed { position, candidate ->
            sourcePlans.getValue(candidate.sourceFocusId).items.single { item ->
                val source = item.exercise.source as? VocabularyP1ExerciseSource
                source?.task?.id == candidate.taskId
            }.copy(position = position)
        }
        return PracticePlan(
            focusId = focusId,
            planRevision = REVISION,
            seed = seed,
            title = if (focusId == FOCUS_RECOGNITION) "Recognise saved vocabulary" else "Write saved Danish vocabulary",
            prerequisiteSkillIds = emptyList(),
            introduction = emptyList(),
            items = items,
            contentRevision = VocabularyP1Content.CONTENT_REVISION,
            generatorRevision = VocabularyP1Content.GENERATOR_REVISION,
            evaluationRevision = VocabularyP1Content.EVALUATION_REVISION,
            completionRuleId = PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            exposurePolicyId = PracticeFeedbackExposureCalculator.VOCABULARY_P1_EXPOSURE_POLICY,
        )
    }

    private fun eligible(candidates: List<Candidate>, claims: List<VocabularyTeachingClaim>): List<Candidate> =
        candidates.filter { candidate ->
            claims.any { claim ->
                claim.entryId == candidate.entryId && claim.senseId == candidate.senseId &&
                    claim.meaningRecorded && candidate.formId in claim.formIds
            }
        }

    private fun formId(entryId: String, senseId: String, text: String): String =
        VocabularyLibrary.catalog().items.single { it.entryId == entryId }.senses.single { it.senseId == senseId }
            .activeForms.single { it.text == text }.formId

    private fun candidate(taskId: String, sourceFocusId: String, entryId: String, form: String) = Candidate(
        taskId,
        sourceFocusId,
        entryId,
        "$entryId-S01",
        formId(entryId, "$entryId-S01", form),
    )

    private fun <T> List<T>.rotate(offset: Int): List<T> = drop(offset) + take(offset)

    private data class Candidate(
        val taskId: String,
        val sourceFocusId: String,
        val entryId: String,
        val senseId: String,
        val formId: String,
    )

    private val RECOGNITION = listOf(
        candidate("p1-1.1", "vocabulary-p1-s1", "L027", "arbejder"),
        candidate("p1-1.2", "vocabulary-p1-s1", "L014", "en bog"),
        candidate("p1-1.3", "vocabulary-p1-s1", "L037", "i dag"),
        candidate("p1-2.2", "vocabulary-p1-s2", "L015", "en avis"),
    )
    private val RECALL = listOf(
        candidate("p1-3.5", "vocabulary-p1-s3", "L038", "nu"),
        candidate("p1-4.5", "vocabulary-p1-s4", "L047", "vand"),
    )
}
