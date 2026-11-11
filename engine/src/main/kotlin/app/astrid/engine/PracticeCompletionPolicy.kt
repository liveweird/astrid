package app.astrid.engine

data class PracticeCompletionAttemptObservation(
    val targetStatus: TargetStatus,
    val linguisticStatus: LinguisticStatus,
    val responseConstraintStatus: ResponseConstraintStatus?,
    val issueCodes: List<String>,
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

    private const val MODULE_ONE_RULE = "module-one-known-outcome-1"
    private const val MODULE_ONE_ORDER_RULE = "module-one-order-outcome-1"
    private val COMPLETION_ISSUE_CODES = setOf(
        "STANDALONE_PRONOUN_CASE_REQUIRED",
        "ONE_WORD_RESPONSE_REQUIRED",
    )
    private val ORDER_COMPLETION_ISSUE_CODES = setOf("REQUESTED_ORDER_CONSTRAINT", "REQUIRED_WORDS_MISSING")
}
