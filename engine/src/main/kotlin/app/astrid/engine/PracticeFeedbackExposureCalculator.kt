package app.astrid.engine

object PracticeFeedbackExposureCalculator {
    fun releasedKeys(
        candidateKeys: List<String>,
        assessments: List<PracticeAssessment>,
        attempts: List<PracticeFeedbackAttemptObservation>,
        exposurePolicyId: String? = null,
    ): List<String> {
        return if (exposurePolicyId != null) {
            releasedByPolicy(candidateKeys, assessments, attempts, exposurePolicyId)
        } else if (assessments.isEmpty()) {
            if (attempts.any { it.targetStatus == TargetStatus.MET }) {
                candidateKeys
            } else {
                candidateKeys.filterNot { it.startsWith(VERB_PRESENT_EXPOSURE_PREFIX) }
            }
        } else {
            requireValidAssessments(assessments)
            attempts.forEach { requireValidAttempt(assessments, it) }
            val conditionalKeys = assessments.flatMapTo(mutableSetOf()) { it.answerExposureKeys }
            val released = assessments.filter { assessment ->
                attempts.any { attempt -> attempt.releases(assessment) }
            }.flatMapTo(mutableSetOf()) { it.answerExposureKeys }
            candidateKeys.filter { it !in conditionalKeys || it in released }
        }
    }

    private fun releasedByPolicy(
        candidateKeys: List<String>,
        assessments: List<PracticeAssessment>,
        attempts: List<PracticeFeedbackAttemptObservation>,
        exposurePolicyId: String,
    ): List<String> = when (exposurePolicyId) {
        MODULE_ONE_EXPOSURE_POLICY -> if (attempts.any { it.disclosesModuleOneAnswer() }) candidateKeys else emptyList()
        MODULE_ONE_ORDER_EXPOSURE_POLICY -> if (attempts.any { it.disclosesModuleOneOrder() }) candidateKeys else emptyList()
        ReadingWordsPracticePlanner.EXPOSURE_POLICY_ID ->
            if (attempts.any { it.confirmsReadingMeaning() }) candidateKeys else emptyList()
        VOCABULARY_P1_EXPOSURE_POLICY -> vocabularyP1ReleasedKeys(candidateKeys, assessments, attempts)
        ModalRoundPolicies.EXPOSURE_POLICY_ID -> ModalRoundPolicies.releasedKeys(candidateKeys, attempts)
        else -> error("Unsupported exposure policy: $exposurePolicyId")
    }

    private fun PracticeFeedbackAttemptObservation.confirmsReadingMeaning(): Boolean =
        targetStatus == TargetStatus.MET &&
            linguisticStatus == LinguisticStatus.ACCEPTABLE &&
            responseConstraintStatus == ResponseConstraintStatus.MET

    private fun PracticeFeedbackAttemptObservation.disclosesModuleOneAnswer(): Boolean =
        targetStatus != TargetStatus.UNEVALUABLE ||
            linguisticStatus in setOf(LinguisticStatus.ACCEPTABLE, LinguisticStatus.KNOWN_ISSUE) ||
            responseConstraintStatus == ResponseConstraintStatus.NOT_MET && issueCodes.any { it in MODULE_ONE_DISCLOSURE_CODES }

    private fun PracticeFeedbackAttemptObservation.disclosesModuleOneOrder(): Boolean =
        targetStatus == TargetStatus.MET &&
            linguisticStatus == LinguisticStatus.ACCEPTABLE &&
            responseConstraintStatus == ResponseConstraintStatus.MET ||
            responseConstraintStatus == ResponseConstraintStatus.NOT_MET &&
            issueCodes.any { it in MODULE_ONE_ORDER_DISCLOSURE_CODES }

    private fun PracticeFeedbackAttemptObservation.releases(assessment: PracticeAssessment): Boolean =
        assessment.slotId?.let { slotId ->
            if (isLexicalMatchingAssessment(assessment)) {
                return matchingResults.singleOrNull { result ->
                    result.leftId == slotId && result.lexicalTarget == assessment.targetDescriptor.lexicalTarget
                }?.targetStatus == TargetStatus.MET
            }
            val matching = gapResults.filter { result ->
                result.slotId == slotId &&
                    result.targetSkillId == assessment.targetDescriptor.targetSkillId &&
                    result.assessmentRole.wireValue == assessment.assessmentRole
            }
            require(matching.size == 1) { "Saved gap results must match each frozen assessment exactly once" }
            matching.single().targetStatus != TargetStatus.UNEVALUABLE
        } ?: (targetStatus == TargetStatus.MET)

    private fun requireValidAssessments(assessments: List<PracticeAssessment>) {
        require(assessments.map { it.slotId }.distinct().size == assessments.size) {
            "Practice assessment slots must be unique"
        }
        require(assessments.all { it.assessmentRole in ASSESSMENT_ROLES }) {
            "Unsupported practice assessment role"
        }
        require(assessments.all {
            it.targetDescriptor.targetSkillId != null || it.targetDescriptor.lexicalTarget != null
        }) {
            "Multiple assessments support grammar gaps or lexical matching pairs"
        }
        require(assessments.all {
            it.targetDescriptor.lexicalTarget == null || it.slotId == null ||
                it.targetDescriptor.assessmentMode == MATCHING_MODE
        }) {
            "Lexical matching assessments require matching mode"
        }
    }

    private fun requireValidAttempt(
        assessments: List<PracticeAssessment>,
        attempt: PracticeFeedbackAttemptObservation,
    ) {
        val slotAssessments = assessments.filter { it.slotId != null }
        if (assessments.all(::isLexicalMatchingAssessment)) {
            require(slotAssessments.size == assessments.size && attempt.matchingResults.size == assessments.size) {
                "Saved matching results must cover every frozen lexical assessment"
            }
            assessments.forEach { assessment ->
                require(attempt.matchingResults.count {
                    it.leftId == assessment.slotId && it.lexicalTarget == assessment.targetDescriptor.lexicalTarget
                } == 1) { "Saved matching results must match each frozen lexical assessment exactly once" }
            }
            return
        }
        if (slotAssessments.isEmpty()) {
            require(assessments.size == 1 && attempt.gapResults.isEmpty()) {
                "Single-target assessments use whole-attempt outcomes"
            }
        } else {
            require(slotAssessments.size == assessments.size && attempt.gapResults.size == assessments.size) {
                "Saved gap results must cover every frozen assessment"
            }
            assessments.forEach { assessment ->
                val matching = attempt.gapResults.count { result ->
                    result.slotId == assessment.slotId &&
                        result.targetSkillId == assessment.targetDescriptor.targetSkillId &&
                        result.assessmentRole.wireValue == assessment.assessmentRole
                }
                require(matching == 1) { "Saved gap results must match each frozen assessment exactly once" }
            }
        }
    }

    private fun isLexicalMatchingAssessment(assessment: PracticeAssessment): Boolean =
        assessment.slotId != null && assessment.targetDescriptor.lexicalTarget != null &&
            assessment.targetDescriptor.assessmentMode == MATCHING_MODE

    private const val VERB_PRESENT_EXPOSURE_PREFIX = "verb-present:"
    private val ASSESSMENT_ROLES = setOf("primary", "support")
    private const val MODULE_ONE_EXPOSURE_POLICY = "module-one-exposure-1"
    private const val MODULE_ONE_ORDER_EXPOSURE_POLICY = "module-one-order-exposure-1"
    const val VOCABULARY_P1_EXPOSURE_POLICY = "vocabulary-p1-exposure-1"
    private const val MATCHING_MODE = "matching"
    private val MODULE_ONE_DISCLOSURE_CODES = setOf(
        "STANDALONE_PRONOUN_CASE_REQUIRED",
        "ONE_WORD_RESPONSE_REQUIRED",
    )
    private val MODULE_ONE_ORDER_DISCLOSURE_CODES = setOf("REQUESTED_ORDER_CONSTRAINT", "REQUIRED_WORDS_MISSING")

    private fun vocabularyP1ReleasedKeys(
        candidateKeys: List<String>,
        assessments: List<PracticeAssessment>,
        attempts: List<PracticeFeedbackAttemptObservation>,
    ): List<String> {
        if (assessments.size == 1 && assessments.single().slotId == null) {
            return if (attempts.any { it.targetStatus == TargetStatus.MET }) candidateKeys else emptyList()
        }
        requireValidAssessments(assessments)
        attempts.forEach { requireValidAttempt(assessments, it) }
        val released = assessments.filter { assessment -> attempts.any { it.releases(assessment) } }
            .flatMapTo(mutableSetOf()) { it.answerExposureKeys }
        return candidateKeys.filter(released::contains)
    }
}
