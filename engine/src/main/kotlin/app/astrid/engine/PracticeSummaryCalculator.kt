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
            observed != null && observed.exercise.id == it.exercise.id && observed.targetDescriptor == it.targetDescriptor
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
        val rows = ordered.groupBy(::targetIdentity).map { (_, observations) -> summarizeRow(observations) }
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

    private fun summarizeRow(items: List<PracticeSummarySnapshotItemObservation>): PracticeTargetSummary {
        val firstAttempts = items.mapNotNull { it.attempts.firstOrNull() }
        val latestAttempts = items.mapNotNull { it.attempts.lastOrNull() }
        val representative = items.first().targetDescriptor
        val attemptCount = items.sumOf { it.attempts.size }
        return PracticeTargetSummary(
            label = representative.label,
            targetSkillId = representative.targetSkillId,
            lexicalTarget = representative.lexicalTarget,
            plannedItems = items.size,
            attemptedItems = items.count { it.attempts.isNotEmpty() },
            advancedItems = items.count { it.advanced },
            attemptCount = attemptCount,
            retryCount = attemptCount - items.count { it.attempts.isNotEmpty() },
            first = outcomeCounts(firstAttempts),
            latest = outcomeCounts(latestAttempts),
            firstWithSupport = firstAttempts.count { it.hintUsed || it.context.hasSupport() },
            firstWithPriorExposure = firstAttempts.count { it.context.priorExposure.isNotEmpty() },
            lessonOpenedItems = items.count { it.lessonOpened },
            hintOpenedItems = items.count { it.hintOpened },
        )
    }

    private fun targetIdentity(observation: PracticeSummarySnapshotItemObservation): String {
        val target = observation.targetDescriptor
        return target.targetSkillId ?: requireNotNull(target.lexicalTarget).run {
            listOf(entryId, senseId, facet, direction).joinToString(IDENTITY_SEPARATOR)
        }
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
        )

    private fun PracticeSummaryAttemptObservation.isFullyMet(): Boolean =
        targetStatus == TargetStatus.MET &&
            linguisticStatus == LinguisticStatus.ACCEPTABLE &&
            responseConstraintStatus != ResponseConstraintStatus.NOT_MET

    private fun PracticeAttemptContext.hasSupport(): Boolean = introductionOpened || lessonOpened || hintOpened

    private const val IDENTITY_SEPARATOR = "\u0000"
}
