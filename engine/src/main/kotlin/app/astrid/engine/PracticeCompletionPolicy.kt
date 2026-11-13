package app.astrid.engine

data class PracticeCompletionAttemptObservation(
    val targetStatus: TargetStatus,
    val linguisticStatus: LinguisticStatus,
    val responseConstraintStatus: ResponseConstraintStatus?,
    val issueCodes: List<String>,
    val teachingSaved: Boolean = true,
    val meaningSatisfaction: MeaningSatisfaction? = null,
    val matchingResults: List<MatchingPairResult> = emptyList(),
)

object PracticeCompletionPolicy {
    fun canAdvance(
        completionRuleId: String?,
        latestAttempt: PracticeCompletionAttemptObservation?,
    ): Boolean {
        if (latestAttempt == null) return false
        return when (completionRuleId) {
            null -> true
            MODULE_ONE_RULE -> latestAttempt.hasKnownTargetOutcome() || latestAttempt.hasKnownConstraintOutcome()
            MODULE_ONE_ORDER_RULE -> latestAttempt.hasKnownOrderConstraintOutcome()
            ModalRoundPolicies.COMPLETION_RULE_ID -> ModalRoundPolicies.canAdvance(latestAttempt)
            VOCABULARY_P1_RULE -> latestAttempt.hasVocabularyP1Outcome()
            VOCABULARY_P2_RULE -> latestAttempt.hasVocabularyP2Outcome()
            else -> false
        }
    }

    private fun PracticeCompletionAttemptObservation.hasKnownTargetOutcome(): Boolean =
        targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
            linguisticStatus in setOf(LinguisticStatus.ACCEPTABLE, LinguisticStatus.KNOWN_ISSUE)

    private fun PracticeCompletionAttemptObservation.hasKnownConstraintOutcome(): Boolean =
        responseConstraintStatus == ResponseConstraintStatus.NOT_MET &&
            issueCodes.any { it in COMPLETION_ISSUE_CODES }

    private fun PracticeCompletionAttemptObservation.hasKnownOrderConstraintOutcome(): Boolean =
        targetStatus == TargetStatus.MET &&
            linguisticStatus == LinguisticStatus.ACCEPTABLE &&
            responseConstraintStatus == ResponseConstraintStatus.MET ||
            responseConstraintStatus == ResponseConstraintStatus.NOT_MET &&
            issueCodes.any { it in ORDER_COMPLETION_ISSUE_CODES }

    private fun PracticeCompletionAttemptObservation.hasVocabularyP1Outcome(): Boolean {
        val knownOutcome = if (matchingResults.isNotEmpty()) {
            matchingResults.size in P1_MATCHING_MIN..P1_MATCHING_MAX && matchingResults.all {
                it.targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
                    it.linguisticStatus == LinguisticStatus.ACCEPTABLE &&
                    it.responseConstraintStatus == ResponseConstraintStatus.MET
            }
        } else {
            targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
                linguisticStatus in setOf(LinguisticStatus.ACCEPTABLE, LinguisticStatus.KNOWN_ISSUE) &&
                responseConstraintStatus == ResponseConstraintStatus.MET ||
                responseConstraintStatus == ResponseConstraintStatus.NOT_MET &&
                issueCodes.any { it in VOCABULARY_P1_ORDER_ISSUES }
        }
        return teachingSaved && knownOutcome
    }

    private fun PracticeCompletionAttemptObservation.hasVocabularyP2Outcome(): Boolean {
        val knownOutcome = if (matchingResults.isNotEmpty()) {
            matchingResults.size in P2_MATCHING_MIN..P2_MATCHING_MAX && matchingResults.all {
                it.targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
                    it.linguisticStatus == LinguisticStatus.ACCEPTABLE &&
                    it.responseConstraintStatus == ResponseConstraintStatus.MET
            }
        } else {
            targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
                linguisticStatus == LinguisticStatus.ACCEPTABLE &&
                responseConstraintStatus == ResponseConstraintStatus.MET
        }
        return teachingSaved && knownOutcome
    }

    private const val MODULE_ONE_RULE = "module-one-known-outcome-1"
    private const val MODULE_ONE_ORDER_RULE = "module-one-order-outcome-1"
    const val VOCABULARY_P1_RULE = "vocabulary-p1-outcome-1"
    const val VOCABULARY_P2_RULE = "vocabulary-p2-outcome-1"
    private val COMPLETION_ISSUE_CODES = setOf(
        "STANDALONE_PRONOUN_CASE_REQUIRED",
        "ONE_WORD_RESPONSE_REQUIRED",
    )
    private val ORDER_COMPLETION_ISSUE_CODES = setOf("REQUESTED_ORDER_CONSTRAINT", "REQUIRED_WORDS_MISSING")
    private val VOCABULARY_P1_ORDER_ISSUES = setOf("REQUESTED_ORDER_CONSTRAINT")
    private const val P1_MATCHING_MIN = 3
    private const val P1_MATCHING_MAX = 4
    private const val P2_MATCHING_MIN = 2
    private const val P2_MATCHING_MAX = 4
}
