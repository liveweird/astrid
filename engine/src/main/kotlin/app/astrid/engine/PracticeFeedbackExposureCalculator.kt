package app.astrid.engine

object PracticeFeedbackExposureCalculator {
    fun releasedKeys(
        candidateKeys: List<String>,
        assessments: List<PracticeAssessment>,
        attempts: List<PracticeFeedbackAttemptObservation>,
    ): List<String> {
        if (assessments.isEmpty()) {
            return if (attempts.any { it.targetStatus == TargetStatus.MET }) {
                candidateKeys
            } else {
                candidateKeys.filterNot { it.startsWith(VERB_PRESENT_EXPOSURE_PREFIX) }
            }
        }
        requireValidAssessments(assessments)
        attempts.forEach { requireValidAttempt(assessments, it) }
        val conditionalKeys = assessments.flatMapTo(mutableSetOf()) { it.answerExposureKeys }
        val released = assessments.filter { assessment ->
            attempts.any { attempt -> attempt.releases(assessment) }
        }.flatMapTo(mutableSetOf()) { it.answerExposureKeys }
        return candidateKeys.filter { it !in conditionalKeys || it in released }
    }

    private fun PracticeFeedbackAttemptObservation.releases(assessment: PracticeAssessment): Boolean =
        assessment.slotId?.let { slotId ->
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
        require(assessments.all { it.targetDescriptor.lexicalTarget == null }) {
            "Multiple assessments currently support grammar targets only"
        }
    }

    private fun requireValidAttempt(
        assessments: List<PracticeAssessment>,
        attempt: PracticeFeedbackAttemptObservation,
    ) {
        val slotAssessments = assessments.filter { it.slotId != null }
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

    private const val VERB_PRESENT_EXPOSURE_PREFIX = "verb-present:"
    private val ASSESSMENT_ROLES = setOf("primary", "support")
}
