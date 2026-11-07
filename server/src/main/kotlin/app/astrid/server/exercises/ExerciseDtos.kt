@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.exercises

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.json.JsonClassDiscriminator

internal const val CONTENT_REVISION = "pilot-1"
internal const val GENERATOR_REVISION = "generator-1"
internal const val EVALUATION_REVISION = "evaluation-1"

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
data class ReadingTaskDto(val facet: String)

@Serializable
data class FormatDto(
    val id: String,
    val title: String,
    val targetSkillId: String? = null,
    val lexicalTask: LexicalTaskDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val readingTask: ReadingTaskDto? = null,
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val passage: List<String>? = null,
)

@Serializable
data class IntroductionDto(
    val title: String,
    val body: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val bodyLanguage: String? = null,
)

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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val readingTarget: ReadingTaskDto? = null,
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val meaningSatisfaction: String? = null,
)

@Serializable
data class LexicalSourceDto(
    val entryId: String,
    val senseId: String,
    val headword: String,
    val gloss: String,
    val article: String,
)

@Serializable
data class ReadingPropositionDetailsDto(
    val sentenceId: String,
    val subjectId: String,
    val verbId: String,
    val objectId: String,
)

@Serializable
data class ReadingSourceDetailsDto(
    val propositions: List<ReadingPropositionDetailsDto>,
    val questionSubjectId: String,
    val questionVerbId: String,
    val answerNounId: String,
    val answer: String,
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val readingSource: ReadingSourceDetailsDto? = null,
)
