@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.exercises

import app.astrid.engine.GapAssessmentRole
import app.astrid.engine.GapResult
import app.astrid.engine.LinguisticStatus
import app.astrid.engine.PracticeAssessment
import app.astrid.engine.PracticeAttemptContextCalculator
import app.astrid.engine.PracticeCrossRunExposure
import app.astrid.engine.PracticeExposureSnapshot
import app.astrid.engine.PracticeFeedbackAttemptObservation
import app.astrid.engine.PracticeFeedbackExposureCalculator
import app.astrid.engine.PracticeItemExposureObservation
import app.astrid.engine.ResponseConstraintStatus
import app.astrid.engine.TargetStatus
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
    assessments: List<PracticeAssessment>,
    attempts: List<AttemptResponse>,
): List<String> = PracticeFeedbackExposureCalculator.releasedKeys(
    candidateKeys,
    assessments,
    attempts.map { response ->
        PracticeFeedbackAttemptObservation(
            enumByWire(response.targetSatisfaction),
            response.gapResults.orEmpty().map { it.toEngine() },
        )
    },
)

private fun GapResultDto.toEngine() = GapResult(
    slotId,
    choiceId,
    targetSkillId,
    GapAssessmentRole.entries.single { it.wireValue == assessmentRole },
    enumByWire(targetSatisfaction),
    enumByWire(responseConstraintStatus),
    enumByWire(linguisticStatus),
    feedback,
)

private inline fun <reified T : Enum<T>> enumByWire(value: String): T = enumValues<T>().single { entry ->
    when (entry) {
        is TargetStatus -> entry.wireValue
        is LinguisticStatus -> entry.wireValue
        is ResponseConstraintStatus -> entry.wireValue
        else -> error("Unsupported wire enum")
    } == value
}

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
