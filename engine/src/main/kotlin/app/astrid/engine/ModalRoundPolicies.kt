package app.astrid.engine

/** Pair-14 mixed policy implementation; core calculators dispatch to these IDs. */
object ModalRoundPolicies {
    const val COMPLETION_RULE_ID = "module-one-modal-round-outcome-1"
    const val EXPOSURE_POLICY_ID = "module-one-modal-round-exposure-1"

    fun canAdvance(attempt: PracticeCompletionAttemptObservation?): Boolean =
        attempt != null && (attempt.hasKnownTargetOutcome() || attempt.hasDisclosedOrderException())

    fun releasedKeys(
        candidateKeys: List<String>,
        attempts: List<PracticeFeedbackAttemptObservation>,
    ): List<String> =
        if (attempts.any { it.hasKnownTargetOutcome() || it.hasDisclosedOrderException() }) {
            candidateKeys
        } else {
            emptyList()
        }

    private fun PracticeCompletionAttemptObservation.hasKnownTargetOutcome() =
        targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
            linguisticStatus in setOf(LinguisticStatus.ACCEPTABLE, LinguisticStatus.KNOWN_ISSUE)

    private fun PracticeCompletionAttemptObservation.hasDisclosedOrderException() =
        responseConstraintStatus == ResponseConstraintStatus.NOT_MET && issueCodes.any(ORDER_ISSUE_CODES::contains)

    private fun PracticeFeedbackAttemptObservation.hasKnownTargetOutcome() =
        targetStatus in setOf(TargetStatus.MET, TargetStatus.NOT_MET) &&
            linguisticStatus in setOf(LinguisticStatus.ACCEPTABLE, LinguisticStatus.KNOWN_ISSUE)

    private fun PracticeFeedbackAttemptObservation.hasDisclosedOrderException() =
        responseConstraintStatus == ResponseConstraintStatus.NOT_MET && issueCodes.any(ORDER_ISSUE_CODES::contains)

    private val ORDER_ISSUE_CODES = setOf("REQUESTED_ORDER_CONSTRAINT", "REQUIRED_WORDS_MISSING")
}
