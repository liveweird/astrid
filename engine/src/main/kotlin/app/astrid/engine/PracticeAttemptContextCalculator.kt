package app.astrid.engine

object PracticeAttemptContextCalculator {
    fun calculate(
        currentItem: PracticeExposureMetadata,
        introductionOpened: Boolean,
        observations: List<PracticeItemExposureObservation>,
        sourceExposure: List<PracticeCrossRunExposure> = emptyList(),
    ): PracticeAttemptContext {
        require(observations.map { it.item.position }.toSet().size == observations.size) {
            "Practice exposure observations must have unique positions"
        }
        val current = observations.singleOrNull { it.item.position == currentItem.position }
            ?: error("Current practice item ${currentItem.position} is missing")
        require(current.item.hasSameMetadata(currentItem)) {
            "Current practice item does not match its exposure observation"
        }
        val answerKeys = currentItem.answerExposureKeys.toSet()
        val exposure = observations
            .sortedBy { it.item.position }
            .flatMap { observation -> exposureFor(observation, currentItem.position, answerKeys) }
        val relevantSourceExposure = sourceExposure.mapNotNull { exposure ->
            val relevantKeys = exposure.keys.filter { it in answerKeys }.distinct()
            exposure.copy(keys = relevantKeys).takeIf { relevantKeys.isNotEmpty() }
        }.distinct()
        return PracticeAttemptContext(
            introductionOpened = introductionOpened,
            lessonOpened = current.lessonOpened,
            hintOpened = current.hintOpened,
            priorExposure = exposure,
            sourceExposure = relevantSourceExposure,
        )
    }

    private fun exposureFor(
        observation: PracticeItemExposureObservation,
        currentPosition: Int,
        answerKeys: Set<String>,
    ): List<PracticePriorExposure> =
        buildList {
            if (observation.promptReleased && observation.item.position != currentPosition) {
                addExposure(observation, PracticeExposureSource.PROMPT, observation.item.promptExposureKeys, answerKeys)
            }
            if (observation.lessonOpened) {
                addExposure(observation, PracticeExposureSource.LESSON, observation.item.lessonExposureKeys, answerKeys)
            }
            if (observation.hintOpened) {
                addExposure(observation, PracticeExposureSource.HINT, observation.item.hintExposureKeys, answerKeys)
            }
            if (observation.feedbackReleased) {
                addExposure(observation, PracticeExposureSource.FEEDBACK, observation.item.feedbackExposureKeys, answerKeys)
            }
        }

    private fun MutableList<PracticePriorExposure>.addExposure(
        observation: PracticeItemExposureObservation,
        source: PracticeExposureSource,
        possibleKeys: List<String>,
        answerKeys: Set<String>,
    ) {
        val relevantKeys = possibleKeys.filter { it in answerKeys }.distinct()
        if (relevantKeys.isNotEmpty()) {
            add(PracticePriorExposure(observation.item.position, source, relevantKeys))
        }
    }

    private fun PracticeExposureMetadata.hasSameMetadata(other: PracticeExposureMetadata): Boolean =
        position == other.position &&
            answerExposureKeys == other.answerExposureKeys &&
            promptExposureKeys == other.promptExposureKeys &&
            lessonExposureKeys == other.lessonExposureKeys &&
            hintExposureKeys == other.hintExposureKeys &&
            feedbackExposureKeys == other.feedbackExposureKeys
}
