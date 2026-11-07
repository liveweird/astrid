package app.astrid.server

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

internal const val CONTENT_REVISION = "pilot-1"
internal const val GENERATOR_REVISION = "generator-1"
internal const val EVALUATION_REVISION = "evaluation-1"
internal const val CURRENT_CONTENT_REVISION = "pilot-2"
internal const val CURRENT_GENERATOR_REVISION = "generator-2"
internal const val CURRENT_EVALUATION_REVISION = "evaluation-2"

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
data class SkillDto(
    val id: String,
    val title: String,
    val explanation: String,
    val prerequisiteIds: List<String>,
)

@Serializable
data class FormatDto(val id: String, val title: String, val targetSkillId: String)

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
    val targetSkillId: String,
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
)

@Serializable
data class IntroductionDto(val title: String, val body: String)

@Serializable
data class ExerciseResponse(
    val id: String,
    val formatId: String,
    val targetSkillId: String,
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
data class ExerciseInspectorDto(
    val exercise: ExerciseResponse,
    val seed: Int,
    val index: Int,
    val canonicalSentenceId: String,
    val constructionId: String,
    val lexicalIds: List<String>,
    val clauseRoles: List<String>,
    val canonicalSentence: String,
)
