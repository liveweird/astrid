@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.practice

import app.astrid.engine.PracticeAssessment
import app.astrid.engine.PracticeCompletionAttemptObservation
import app.astrid.engine.PracticeTargetDescriptor
import app.astrid.engine.PracticeItemPolicy
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.exercises.PracticeCrossRunExposureDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.LexicalTaskDto
import app.astrid.server.exercises.ReadingTaskDto
import app.astrid.server.exercises.MatchingPairResultDto
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
    val introductionRequired: Boolean = false,
    val completionRuleId: String? = null,
    val exposurePolicyId: String? = null,
    val cumulativeReview: CumulativeReviewPayload? = null,
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
    val targetScopeId: String? = null,
    val readingRole: String? = null,
    val matchingTarget: LexicalTaskDto? = null,
    val policy: PracticeItemPolicyPayload? = null,
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
    val introductionRequired: Boolean = false,
    val completionRuleId: String? = null,
    val exposurePolicyId: String? = null,
    val cumulativeReview: CumulativeReviewPayload? = null,
    val createdAt: Long = 0,
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
    val targetScopeId: String? = null,
    val readingRole: String? = null,
    val matchingTarget: LexicalTaskDto? = null,
    val policy: PracticeItemPolicyPayload? = null,
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val introductionRequired: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val completionRuleId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val exposurePolicyId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val cumulativeReview: CumulativeReviewPayload? = null,
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val targetScopeId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val readingRole: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val matchingTarget: LexicalTaskDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val policy: PracticeItemPolicyPayload? = null,
)

@Serializable
data class PracticeItemPolicyPayload(
    val completionRuleId: String? = null,
    val exposurePolicyId: String? = null,
)

@Serializable
data class CumulativeReviewPayload(
    val selectionPolicyRevision: String,
    val sourceLessonCount: Int,
    val selections: List<CumulativeReviewSelectionPayload>,
)

@Serializable
data class CumulativeReviewSelectionPayload(
    val candidateKey: String,
    val semanticId: String,
    val sourcePracticeSessionId: String,
    val sourcePosition: Int,
    val sourceLessonId: String,
)

@Serializable
data class PracticeAssessmentPayload(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val slotId: String? = null,
    val label: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val targetSkillId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val assessmentMode: String? = null,
    val assessmentRole: String,
    val answerExposureKeys: List<String>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val targetScopeId: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val lexicalTarget: LexicalTargetDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val readingTarget: ReadingTaskDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val readingRole: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val matchingTarget: LexicalTaskDto? = null,
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
            it.targetScopeId,
            it.readingRole,
            it.matchingTarget,
            it.policy,
        )
    },
    followUp?.let { PracticeFollowUpPayload(it.sourcePracticeSessionId.toString(), it.selectedTargetKeys) },
    sourceExposure,
    introductionExposureKeys,
    introductionRequired,
    completionRuleId,
    exposurePolicyId,
    cumulativeReview,
)

internal fun PracticeItemPolicyPayload.toEngine() = PracticeItemPolicy(completionRuleId, exposurePolicyId)

internal fun PracticeItemPolicyPayload?.effectiveCompletionRule(global: String?): String? =
    if (this != null) completionRuleId else global

internal fun PracticeItemPolicyPayload?.effectiveExposurePolicy(global: String?): String? =
    if (this != null) exposurePolicyId else global

internal fun PracticeItemPolicy?.toPayload() = this?.let { PracticeItemPolicyPayload(it.completionRuleId, it.exposurePolicyId) }

internal fun PracticeAssessmentPayload.toEngineAssessment() = PracticeAssessment(
    slotId,
    PracticeTargetDescriptor(
        label,
        targetSkillId,
        lexicalTarget?.let { app.astrid.engine.LexicalTarget(it.entryId, it.senseId, it.facet, it.direction) },
        assessmentMode,
        targetScopeId,
        readingTarget?.let { app.astrid.engine.ReadingTask(it.facet) },
        readingRole,
        matchingTarget?.let { app.astrid.engine.LexicalTask(it.facet, it.direction) },
    ),
    assessmentRole,
    answerExposureKeys,
)

internal fun AttemptResponse.toCompletionObservation(teachingSaved: Boolean) = PracticeCompletionAttemptObservation(
    targetStatus = enumByWire(targetSatisfaction),
    linguisticStatus = enumByWire(linguisticStatus),
    responseConstraintStatus = responseConstraintStatus?.let { enumByWire(it) },
    issueCodes = issues.map { it.code },
    teachingSaved = teachingSaved,
    meaningSatisfaction = meaningSatisfaction?.let { enumByWire(it) },
    matchingResults = matchingResults.orEmpty().map { it.toEngine() },
)

private fun MatchingPairResultDto.toEngine() = app.astrid.engine.MatchingPairResult(
    leftId,
    rightId,
    lexicalTarget.let { app.astrid.engine.LexicalTarget(it.entryId, it.senseId, it.facet, it.direction) },
    enumByWire(targetSatisfaction),
    enumByWire(meaningSatisfaction),
    enumByWire(responseConstraintStatus),
    enumByWire(linguisticStatus),
    feedback,
)

private inline fun <reified T : Enum<T>> enumByWire(value: String): T = enumValues<T>().single { entry ->
    when (entry) {
        is app.astrid.engine.TargetStatus -> entry.wireValue
        is app.astrid.engine.LinguisticStatus -> entry.wireValue
        is app.astrid.engine.ResponseConstraintStatus -> entry.wireValue
        is app.astrid.engine.MeaningSatisfaction -> entry.wireValue
        else -> error("Unsupported wire enum")
    } == value
}
