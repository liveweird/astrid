package app.astrid.engine

object PracticeSummaryCalculator {
    fun calculate(
        plan: PracticePlan,
        introductionOpened: Boolean,
        items: List<PracticeSummaryItemObservation>,
    ): PracticeSummary {
        require(items.map { it.item.position }.toSet().size == items.size) {
            "Practice summary observations must have unique positions"
        }
        val byPosition = items.associateBy { it.item.position }
        require(plan.items.all {
            val observed = byPosition[it.position]?.item
            observed != null && observed.exercise.id == it.exercise.id &&
                observed.targetDescriptor == it.targetDescriptor && observed.assessments == it.assessments
        }) {
            "Practice summary observations must match every planned item"
        }
        require(items.size == plan.items.size) { "Practice summary observations must not contain unplanned items" }
        val ordered = plan.items.map { byPosition.getValue(it.position) }
        return calculate(
            plannedItemCount = plan.items.size,
            introductionOpened = introductionOpened,
            items = ordered.map {
                PracticeSummarySnapshotItemObservation(
                    position = it.item.position,
                    targetDescriptor = it.item.targetDescriptor,
                    advanced = it.advanced,
                    lessonOpened = it.lessonOpened,
                    hintOpened = it.hintOpened,
                    attempts = it.attempts,
                    assessments = it.item.assessments,
                )
            },
        )
    }

    fun calculate(
        plannedItemCount: Int,
        introductionOpened: Boolean,
        items: List<PracticeSummarySnapshotItemObservation>,
    ): PracticeSummary {
        require(plannedItemCount >= 0) { "plannedItemCount must be non-negative" }
        require(items.map { it.position }.toSet().size == items.size) {
            "Practice summary observations must have unique positions"
        }
        require(items.size == plannedItemCount && items.map { it.position }.toSet() == (0 until plannedItemCount).toSet()) {
            "Practice summary observations must match every planned position"
        }
        val ordered = items.sortedBy { it.position }
        val units = ordered.flatMap(::summaryUnits)
        val rows = units.groupBy(::targetIdentity).map { (_, observations) -> summarizeRow(observations) }
        val attemptCount = ordered.sumOf { it.attempts.size }
        return PracticeSummary(
            plannedItems = plannedItemCount,
            attemptedItems = ordered.count { it.attempts.isNotEmpty() },
            completedItems = ordered.count { it.advanced },
            attemptCount = attemptCount,
            retryCount = attemptCount - ordered.count { it.attempts.isNotEmpty() },
            introductionOpened = introductionOpened,
            rows = rows,
        )
    }

    private fun summarizeRow(items: List<SummaryUnitObservation>): PracticeTargetSummary {
        val firstAttempts = items.mapNotNull { it.attempts.firstOrNull() }
        val latestAttempts = items.mapNotNull { it.attempts.lastOrNull() }
        val representative = items.first()
        val attemptCount = items.sumOf { it.attempts.size }
        return PracticeTargetSummary(
            label = representative.targetDescriptor.label,
            targetSkillId = representative.targetDescriptor.targetSkillId,
            lexicalTarget = representative.targetDescriptor.lexicalTarget,
            plannedItems = items.size,
            attemptedItems = items.count { it.attempts.isNotEmpty() },
            advancedItems = items.count { it.advanced },
            attemptCount = attemptCount,
            retryCount = attemptCount - items.count { it.attempts.isNotEmpty() },
            first = outcomeCounts(firstAttempts),
            latest = outcomeCounts(latestAttempts),
            firstWithSupport = items.count { item ->
                item.attempts.firstOrNull()?.let { it.hintUsed || it.context.hasSupport() } == true
            },
            firstWithPriorExposure = items.count { item ->
                item.attempts.firstOrNull()?.let {
                    item.answerExposureKeys.let { keys ->
                        if (keys.isEmpty()) {
                            it.context.priorExposure.isNotEmpty() || it.context.sourceExposure.isNotEmpty()
                        } else {
                            it.context.priorExposure.any { exposure -> exposure.keys.any(keys::contains) } ||
                                it.context.sourceExposure.any { exposure -> exposure.keys.any(keys::contains) }
                        }
                    }
                } == true
            },
            lessonOpenedItems = items.count { it.lessonOpened },
            hintOpenedItems = items.count { it.hintOpened },
            assessmentMode = representative.targetDescriptor.assessmentMode,
            assessmentRole = representative.assessmentRole,
            targetScopeId = representative.targetDescriptor.targetScopeId,
            readingTarget = representative.targetDescriptor.readingTarget,
            readingRole = representative.targetDescriptor.readingRole,
            matchingTarget = representative.targetDescriptor.matchingTarget,
        )
    }

    private fun targetIdentity(observation: SummaryUnitObservation): String =
        observation.targetDescriptor.targetKey()

    private fun summaryUnits(item: PracticeSummarySnapshotItemObservation): List<SummaryUnitObservation> {
        if (item.assessments.isEmpty()) {
            return listOf(
                SummaryUnitObservation(
                    item.targetDescriptor,
                    null,
                    emptyList(),
                    item.advanced,
                    item.lessonOpened,
                    item.hintOpened,
                    item.attempts,
                ),
            )
        }
        require(item.assessments.map { it.slotId }.distinct().size == item.assessments.size) {
            "Practice assessment slots must be unique"
        }
        item.attempts.forEach { attempt -> requireValidResults(item.assessments, attempt) }
        return item.assessments.map { assessment ->
            SummaryUnitObservation(
                assessment.targetDescriptor,
                assessment.assessmentRole,
                assessment.answerExposureKeys,
                item.advanced,
                item.lessonOpened,
                item.hintOpened,
                item.attempts.map { attempt -> projectAttempt(attempt, assessment) },
            )
        }
    }

    private fun requireValidResults(assessments: List<PracticeAssessment>, attempt: PracticeSummaryAttemptObservation) {
        val slotAssessments = assessments.filter { it.slotId != null }
        if (assessments.all(::isLexicalMatchingAssessment)) {
            require(slotAssessments.size == assessments.size && attempt.matchingResults.size == assessments.size) {
                "Saved matching results must cover every frozen lexical assessment"
            }
            assessments.forEach { assessment -> matchingResult(attempt.matchingResults, assessment) }
            return
        }
        if (slotAssessments.isEmpty()) {
            require(assessments.size == 1 && attempt.gapResults.isEmpty()) {
                "Single-target assessments use whole-attempt outcomes"
            }
            return
        }
        require(slotAssessments.size == assessments.size && attempt.gapResults.size == assessments.size) {
            "Saved gap results must cover every frozen assessment"
        }
        assessments.forEach { assessment -> matchingResult(attempt.gapResults, assessment) }
    }

    private fun isLexicalMatchingAssessment(assessment: PracticeAssessment): Boolean =
        assessment.slotId != null && assessment.targetDescriptor.lexicalTarget != null &&
            assessment.targetDescriptor.assessmentMode == "matching"

    private fun projectAttempt(
        attempt: PracticeSummaryAttemptObservation,
        assessment: PracticeAssessment,
    ): PracticeSummaryAttemptObservation = assessment.slotId?.let {
        if (isLexicalMatchingAssessment(assessment)) {
            val result = matchingResult(attempt.matchingResults, assessment)
            return attempt.copy(
                targetStatus = result.targetStatus,
                linguisticStatus = result.linguisticStatus,
                responseConstraintStatus = result.responseConstraintStatus,
                meaningSatisfaction = result.meaningSatisfaction,
                gapResults = emptyList(),
                matchingResults = emptyList(),
            )
        }
        val result = matchingResult(attempt.gapResults, assessment)
        attempt.copy(
            targetStatus = result.targetStatus,
            linguisticStatus = result.linguisticStatus,
            responseConstraintStatus = result.responseConstraintStatus,
            gapResults = emptyList(),
            matchingResults = emptyList(),
        )
    } ?: attempt.copy(gapResults = emptyList(), matchingResults = emptyList())

    private fun matchingResult(results: List<MatchingPairResult>, assessment: PracticeAssessment): MatchingPairResult {
        val matching = results.filter { result ->
            result.leftId == assessment.slotId && result.lexicalTarget == assessment.targetDescriptor.lexicalTarget
        }
        require(matching.size == 1) { "Saved matching results must match each frozen lexical assessment exactly once" }
        return matching.single()
    }

    private fun matchingResult(results: List<GapResult>, assessment: PracticeAssessment): GapResult {
        val matching = results.filter { result ->
            result.slotId == assessment.slotId &&
                result.targetSkillId == assessment.targetDescriptor.targetSkillId &&
                result.assessmentRole.wireValue == assessment.assessmentRole
        }
        require(matching.size == 1) { "Saved gap results must match each frozen assessment exactly once" }
        return matching.single()
    }

    private fun outcomeCounts(attempts: List<PracticeSummaryAttemptObservation>): PracticeOutcomeCounts =
        PracticeOutcomeCounts(
            fullyMet = attempts.count { it.isFullyMet() },
            targetMet = attempts.count { it.targetStatus == TargetStatus.MET },
            targetNotMet = attempts.count { it.targetStatus == TargetStatus.NOT_MET },
            unevaluable = attempts.count { it.targetStatus == TargetStatus.UNEVALUABLE },
            languageUnknown = attempts.count { it.linguisticStatus == LinguisticStatus.UNKNOWN },
            languageKnownIssue = attempts.count { it.linguisticStatus == LinguisticStatus.KNOWN_ISSUE },
            responseConstraintNotMet =
                attempts.count { it.responseConstraintStatus == ResponseConstraintStatus.NOT_MET },
            meaningMet = attempts.meaningCount(MeaningSatisfaction.MET),
            meaningNotMet = attempts.meaningCount(MeaningSatisfaction.NOT_MET),
            meaningUnknown = attempts.meaningCount(MeaningSatisfaction.UNKNOWN),
        )

    private fun List<PracticeSummaryAttemptObservation>.meaningCount(value: MeaningSatisfaction): Int? =
        if (none { it.meaningSatisfaction != null }) null else count { it.meaningSatisfaction == value }

    private fun PracticeSummaryAttemptObservation.isFullyMet(): Boolean =
        targetStatus == TargetStatus.MET &&
            linguisticStatus == LinguisticStatus.ACCEPTABLE &&
            responseConstraintStatus != ResponseConstraintStatus.NOT_MET
            && meaningSatisfaction !in setOf(MeaningSatisfaction.NOT_MET, MeaningSatisfaction.UNKNOWN)

    private fun PracticeAttemptContext.hasSupport(): Boolean = introductionOpened || lessonOpened || hintOpened

    private data class SummaryUnitObservation(
        val targetDescriptor: PracticeTargetDescriptor,
        val assessmentRole: String?,
        val answerExposureKeys: List<String>,
        val advanced: Boolean,
        val lessonOpened: Boolean,
        val hintOpened: Boolean,
        val attempts: List<PracticeSummaryAttemptObservation>,
    )
}
