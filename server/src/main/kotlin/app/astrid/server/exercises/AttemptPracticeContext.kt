@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.exercises

import app.astrid.engine.PracticeAttemptContextCalculator
import app.astrid.engine.PracticeCrossRunExposure
import app.astrid.engine.PracticeExposureSnapshot
import app.astrid.engine.PracticeItemExposureObservation
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/**
 * What an attempt on a practice item needs to know about its practice run: the frozen exposure state the attempt context is
 * calculated from. Owned here (not in `practice/`) because the attempt write produces the context; `practice/` supplies the
 * state through [AttemptPracticeHook].
 */
data class PracticeAttemptSource(
    val focusId: String,
    val planRevision: String,
    val seed: Int,
    val position: Int,
    val introductionOpened: Boolean,
    val items: List<PracticeExposureState>,
    val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
)

data class PracticeExposureState(
    val position: Int,
    val answerExposureKeys: List<String>,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val feedbackReleased: Boolean,
)

internal fun frozenFeedbackExposureKeys(
    candidateKeys: List<String>,
    hasPriorTargetMetAttempt: Boolean,
): List<String> = if (hasPriorTargetMetAttempt) {
    candidateKeys
} else {
    candidateKeys.filterNot { it.startsWith(VERB_PRESENT_EXPOSURE_PREFIX) }
}

private const val VERB_PRESENT_EXPOSURE_PREFIX = "verb-present:"

@Serializable
data class PracticePriorExposureDto(
    val position: Int,
    val source: String,
    val keys: List<String>,
)

@Serializable
data class PracticeAttemptContextDto(
    val introductionOpened: Boolean,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val priorExposure: List<PracticePriorExposureDto>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
)

@Serializable
data class PracticeCrossRunExposureDto(
    val practiceSessionId: String,
    val position: Int,
    val source: String,
    val keys: List<String>,
)

internal fun PracticeAttemptSource.toAttemptContext(): PracticeAttemptContextDto {
    val snapshots = items.map { state ->
        PracticeExposureSnapshot(
            state.position,
            state.answerExposureKeys,
            state.promptExposureKeys,
            state.lessonExposureKeys,
            state.hintExposureKeys,
            state.feedbackExposureKeys,
        )
    }
    val byPosition = items.associateBy { it.position }
    val current = snapshots.single { it.position == position }
    val calculated = PracticeAttemptContextCalculator.calculate(
        current,
        introductionOpened,
        snapshots.map { item ->
            val state = byPosition.getValue(item.position)
            PracticeItemExposureObservation(
                item,
                promptReleased = item.position <= position,
                lessonOpened = state.lessonOpened,
                hintOpened = state.hintOpened,
                feedbackReleased = state.feedbackReleased,
            )
        },
        sourceExposure.map { exposure ->
            PracticeCrossRunExposure(
                exposure.practiceSessionId,
                exposure.position,
                app.astrid.engine.PracticeExposureSource.entries.single { it.wireValue == exposure.source },
                exposure.keys,
            )
        },
    )
    return PracticeAttemptContextDto(
        calculated.introductionOpened,
        calculated.lessonOpened,
        calculated.hintOpened,
        calculated.priorExposure.map { PracticePriorExposureDto(it.position, it.source.wireValue, it.keys) },
        calculated.sourceExposure.map {
            PracticeCrossRunExposureDto(it.practiceSessionId, it.position, it.source.wireValue, it.keys)
        },
    )
}
