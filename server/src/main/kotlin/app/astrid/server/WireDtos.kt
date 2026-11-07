@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.json.JsonClassDiscriminator

internal const val CONTENT_REVISION = "pilot-1"
internal const val GENERATOR_REVISION = "generator-1"
internal const val EVALUATION_REVISION = "evaluation-1"
internal const val CURRENT_CONTENT_REVISION = "pilot-3"
internal const val CURRENT_GENERATOR_REVISION = "generator-3"
internal const val CURRENT_EVALUATION_REVISION = "evaluation-3"

@Serializable
class DevelopmentSessionRequest

@Serializable
data class DevelopmentSessionResponse(
    val mode: String = "development",
    val inspectorEnabled: Boolean,
    val contentRevision: String = CURRENT_CONTENT_REVISION,
    val generatorRevision: String = CURRENT_GENERATOR_REVISION,
    val evaluationRevision: String = CURRENT_EVALUATION_REVISION,
)

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
data class SkillDto(
    val id: String,
    val title: String,
    val explanation: String,
    val prerequisiteIds: List<String>,
)

@Serializable
data class LexicalTaskDto(val facet: String, val direction: String)

@Serializable
data class FormatDto(
    val id: String,
    val title: String,
    val targetSkillId: String? = null,
    val lexicalTask: LexicalTaskDto? = null,
)

@Serializable
data class CatalogResponse(
    val contentRevision: String,
    val generatorRevision: String,
    val evaluationRevision: String,
    val skills: List<SkillDto>,
    val formats: List<FormatDto>,
    val sentenceCount: Int,
)

@Serializable
data class ExerciseRequest(
    val formatId: String,
    val targetSkillId: String? = null,
    val seed: Int,
    val index: Int,
    val contentRevision: String,
    val generatorRevision: String,
)

@Serializable
data class PromptBlockDto(val id: String, val text: String)

@Serializable
data class ExercisePromptDto(
    val before: String,
    val after: String,
    val choices: List<String> = emptyList(),
    val blocks: List<PromptBlockDto> = emptyList(),
    val punctuation: String = ".",
    val inputLabel: String? = null,
    val replacementWord: String? = null,
    val cue: String? = null,
    val cueLanguage: String? = null,
    val answerLanguage: String? = null,
)

@Serializable
data class IntroductionDto(val title: String, val body: String)

@Serializable
data class LexicalTargetDto(
    val entryId: String,
    val senseId: String,
    val facet: String,
    val direction: String,
)

@Serializable
data class ExerciseResponse(
    val id: String,
    val formatId: String,
    val targetSkillId: String? = null,
    val lexicalTarget: LexicalTargetDto? = null,
    val supportSkillIds: List<String>,
    val contentRevision: String,
    val generatorRevision: String,
    val instruction: String,
    val context: String,
    val prompt: ExercisePromptDto,
    val introduction: List<IntroductionDto>,
    val hint: String,
)

@Serializable
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
@JsonClassDiscriminator("kind")
sealed interface AttemptAnswerDto {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : AttemptAnswerDto

    @Serializable
    @SerialName("order")
    data class Order(val blockIds: List<String>) : AttemptAnswerDto
}

@Serializable
data class AttemptRequest(
    val attemptId: String,
    val exerciseId: String,
    val answer: AttemptAnswerDto,
    val hintUsed: Boolean,
)

@Serializable
data class IssueDto(val code: String, val message: String)

@Serializable
data class AttemptResponse(
    val attemptId: String,
    val exerciseId: String,
    val targetSatisfaction: String,
    val linguisticStatus: String,
    val issues: List<IssueDto>,
    val feedback: String,
    val completedSentence: String,
    val evaluationRevision: String,
    val hintUsed: Boolean,
    val retryNumber: Int,
    val createdAt: Long,
    val linguisticScope: String? = null,
    val responseConstraintStatus: String? = null,
)

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
)

@Serializable
data class ProblemResponse(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String? = null,
    val code: String,
)

@Serializable
data class HealthResponse(val status: String)

@Serializable
data class LexicalSourceDto(
    val entryId: String,
    val senseId: String,
    val headword: String,
    val gloss: String,
    val article: String,
)

@Serializable
data class ExerciseInspectorDto(
    val exercise: ExerciseResponse,
    val seed: Int,
    val index: Int,
    val canonicalSentenceId: String? = null,
    val constructionId: String? = null,
    val lexicalIds: List<String>,
    val clauseRoles: List<String>,
    val canonicalSentence: String? = null,
    val lexicalSource: LexicalSourceDto? = null,
)
