@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.practice

import app.astrid.engine.PracticeAssessment
import app.astrid.engine.PracticeTargetDescriptor
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.exercises.PracticeCrossRunExposureDto
import app.astrid.server.exercises.StoredExercise
import java.util.UUID
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

data class NewPracticeSession(
    val title: String,
    val contentRevision: String,
    val generatorRevision: String,
    val evaluationRevision: String,
    val prerequisiteSkillIds: List<String>,
    val introduction: List<IntroductionDto>,
    val items: List<NewPracticeItem>,
    val followUp: PracticeFollowUpLineage? = null,
    val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
    val introductionExposureKeys: List<String>? = null,
)

data class PracticeFollowUpLineage(
    val sourcePracticeSessionId: UUID,
    val selectedTargetKeys: List<String>,
)

data class NewPracticeItem(
    val position: Int,
    val exercise: StoredExercise,
    val targetLabel: String,
    val answerExposureKeys: List<String>,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
    val assessmentMode: String? = null,
    val assessments: List<PracticeAssessmentPayload> = emptyList(),
)

data class StoredPracticeSession(
    val id: UUID,
    val ownerSessionId: UUID,
    val sessionExpiresAt: Long,
    val focusId: String,
    val title: String,
    val planRevision: String,
    val seed: Int,
    val contentRevision: String,
    val generatorRevision: String,
    val evaluationRevision: String,
    val cursor: Int,
    val introductionOpened: Boolean,
    val prerequisiteSkillIds: List<String>,
    val introduction: List<IntroductionDto>,
    val items: List<StoredPracticeItem>,
    val followUp: PracticeFollowUpLineage? = null,
    val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
    val introductionExposureKeys: List<String>? = null,
)

data class StoredPracticeItem(
    val position: Int,
    val exercise: StoredExercise,
    val targetLabel: String,
    val answerExposureKeys: List<String>,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val attempts: List<StoredPracticeAttempt>,
    val assessmentMode: String? = null,
    val assessments: List<PracticeAssessmentPayload> = emptyList(),
)

data class StoredPracticeAttempt(
    val response: AttemptResponse,
    val context: PracticeAttemptContextDto,
    val answer: AttemptAnswerDto,
)

@Serializable
data class PracticePlanPayload(
    val title: String,
    val contentRevision: String,
    val generatorRevision: String,
    val evaluationRevision: String,
    val prerequisiteSkillIds: List<String>,
    val introduction: List<IntroductionDto>,
    val items: List<PracticeItemPayload>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val followUp: PracticeFollowUpPayload? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val introductionExposureKeys: List<String>? = null,
)

@Serializable
data class PracticeFollowUpPayload(
    val sourcePracticeSessionId: String,
    val selectedTargetKeys: List<String>,
)

@Serializable
data class PracticeItemPayload(
    val position: Int,
    val targetLabel: String,
    val answerExposureKeys: List<String>,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessmentMode: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessments: List<PracticeAssessmentPayload> = emptyList(),
)

@Serializable
data class PracticeAssessmentPayload(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val slotId: String? = null,
    val label: String,
    val targetSkillId: String,
    val assessmentMode: String,
    val assessmentRole: String,
    val answerExposureKeys: List<String>,
)

fun NewPracticeSession.toPayload() = PracticePlanPayload(
    title,
    contentRevision,
    generatorRevision,
    evaluationRevision,
    prerequisiteSkillIds,
    introduction,
    items.map {
        PracticeItemPayload(
            it.position,
            it.targetLabel,
            it.answerExposureKeys,
            it.promptExposureKeys,
            it.lessonExposureKeys,
            it.hintExposureKeys,
            it.feedbackExposureKeys,
            it.assessmentMode,
            it.assessments,
        )
    },
    followUp?.let { PracticeFollowUpPayload(it.sourcePracticeSessionId.toString(), it.selectedTargetKeys) },
    sourceExposure,
    introductionExposureKeys,
)

internal fun PracticeAssessmentPayload.toEngineAssessment() = PracticeAssessment(
    slotId,
    PracticeTargetDescriptor(label, targetSkillId, null, assessmentMode),
    assessmentRole,
    answerExposureKeys,
)
