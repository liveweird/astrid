@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.practice

import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.ExerciseResponse
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.PracticeAttemptContextDto
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class PracticeContextResponse(
    val ownerSessionId: String,
    val sessionExpiresAt: Long,
    val focusId: String,
    val title: String,
    val planRevision: String,
    val itemCount: Int,
)

@Serializable
data class PracticeProfileDto(
    val focusId: String,
    val planRevision: String,
    val title: String,
    val description: String,
    val itemCount: Int,
    val prerequisiteSkillIds: List<String>,
)

@Serializable
data class PracticeProfilesResponse(
    val ownerSessionId: String,
    val sessionExpiresAt: Long,
    val profiles: List<PracticeProfileDto>,
)

@Serializable
data class PracticeSessionRequest(
    val practiceSessionId: String,
    val focusId: String,
    val planRevision: String,
    val seed: Int,
)

@Serializable
data class PracticeFollowUpRequest(
    val practiceSessionId: String,
    val planRevision: String,
    val seed: Int,
    val targetKeys: List<String>,
)

@Serializable
data class PracticeSupportRequest(val kind: String, val position: Int? = null)

@Serializable
data class PracticeAdvanceRequest(val position: Int)

@Serializable
data class PracticeFollowUpDto(
    val sourcePracticeSessionId: String,
    val selectedTargetKeys: List<String>,
)

@Serializable
data class PracticeItemDto(
    val position: Int,
    val exerciseId: String,
    val formatId: String,
    val targetSkillId: String? = null,
    val lexicalTarget: LexicalTargetDto? = null,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val attemptIds: List<String>,
    val attemptCount: Int,
    val firstAttempt: AttemptResponse? = null,
    val lastAttempt: AttemptResponse? = null,
    val firstAnswer: AttemptAnswerDto? = null,
    val lastAnswer: AttemptAnswerDto? = null,
    val firstAttemptContext: PracticeAttemptContextDto? = null,
    val lastAttemptContext: PracticeAttemptContextDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessmentMode: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessments: List<PracticeAssessmentDto> = emptyList(),
)

@Serializable
data class PracticeAssessmentDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val slotId: String? = null,
    val label: String,
    val targetSkillId: String,
    val assessmentMode: String,
    val assessmentRole: String,
)

@Serializable
data class PracticeOutcomeCountsDto(
    val fullyMet: Int,
    val targetMet: Int,
    val targetNotMet: Int,
    val unevaluable: Int,
    val languageUnknown: Int,
    val languageKnownIssue: Int,
    val responseConstraintNotMet: Int,
)

@Serializable
data class PracticeSummaryRowDto(
    val label: String,
    val targetSkillId: String? = null,
    val lexicalTarget: LexicalTargetDto? = null,
    val plannedItems: Int,
    val attemptedItems: Int,
    val advancedItems: Int,
    val attemptCount: Int,
    val retryCount: Int,
    val firstOutcomes: PracticeOutcomeCountsDto,
    val latestOutcomes: PracticeOutcomeCountsDto,
    val firstWithSupport: Int,
    val firstWithPriorExposure: Int,
    val lessonOpenedItems: Int,
    val hintOpenedItems: Int,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessmentMode: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessmentRole: String? = null,
)

@Serializable
data class PracticeSummaryDto(
    val plannedItems: Int,
    val attemptedItems: Int,
    val completedItems: Int,
    val attemptCount: Int,
    val retryCount: Int,
    val introductionOpened: Boolean,
    val rows: List<PracticeSummaryRowDto>,
)

@Serializable
data class PracticeSessionResponse(
    val id: String,
    val ownerSessionId: String,
    val focusId: String,
    val title: String,
    val planRevision: String,
    val seed: Int,
    val contentRevision: String,
    val generatorRevision: String,
    val evaluationRevision: String,
    val sessionExpiresAt: Long,
    val state: String,
    val cursor: Int,
    val itemCount: Int,
    val prerequisiteSkillIds: List<String>,
    val introduction: List<IntroductionDto>,
    val introductionOpened: Boolean,
    val items: List<PracticeItemDto>,
    val currentExercise: ExerciseResponse? = null,
    val summary: PracticeSummaryDto,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val followUp: PracticeFollowUpDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val followUpPlanRevision: String? = null,
)
