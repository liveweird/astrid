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
        require(plan.items.all { byPosition[it.position]?.item?.exercise?.id == it.exercise.id }) {
            "Practice summary observations must match every planned item"
        }
        require(items.size == plan.items.size) { "Practice summary observations must not contain unplanned items" }
        val ordered = plan.items.map { byPosition.getValue(it.position) }
        val rows = ordered.groupBy(::targetIdentity).map { (_, observations) -> summarizeRow(observations) }
        val attemptCount = ordered.sumOf { it.attempts.size }
        return PracticeSummary(
            plannedItems = plan.items.size,
            attemptedItems = ordered.count { it.attempts.isNotEmpty() },
            completedItems = ordered.count { it.advanced },
            attemptCount = attemptCount,
            retryCount = attemptCount - ordered.count { it.attempts.isNotEmpty() },
            introductionOpened = introductionOpened,
            rows = rows,
        )
    }

    private fun summarizeRow(items: List<PracticeSummaryItemObservation>): PracticeTargetSummary {
        val firstAttempts = items.mapNotNull { it.attempts.firstOrNull() }
        val latestAttempts = items.mapNotNull { it.attempts.lastOrNull() }
        val representative = items.first().item.exercise
        val attemptCount = items.sumOf { it.attempts.size }
        return PracticeTargetSummary(
            label = targetLabel(representative),
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

    private fun targetIdentity(observation: PracticeSummaryItemObservation): String {
        val exercise = observation.item.exercise
        return exercise.targetSkillId ?: requireNotNull(exercise.lexicalTarget).run {
            listOf(entryId, senseId, facet, direction).joinToString(IDENTITY_SEPARATOR)
        }
    }

    private fun targetLabel(exercise: GeneratedExercise): String =
        exercise.targetSkillId?.let { target ->
            PilotContent.skills.single { it.id == target }.title
        } ?: requireNotNull(exercise.inspector.lexicalSource).run {
            val target = requireNotNull(exercise.lexicalTarget)
            if (target.direction == "da-en") "$headword — $gloss" else "$gloss — $headword"
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
