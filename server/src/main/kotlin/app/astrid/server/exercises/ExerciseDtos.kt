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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tileMode: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val requiredTileCount: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val matchingColumns: MatchingColumnsDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val gapPrompt: GapPromptDto? = null,
)

@Serializable
data class MatchingColumnsDto(
    val left: List<MatchingItemDto>,
    val right: List<MatchingItemDto>,
)

@Serializable
data class MatchingItemDto(val id: String, val text: String)

@Serializable
data class GapPromptDto(
    val segments: List<String>,
    val slots: List<GapSlotDto>,
)

@Serializable
data class GapSlotDto(
    val id: String,
    val label: String,
    val targetSkillId: String,
    val assessmentRole: String,
    val choices: List<GapChoiceDto>,
)

@Serializable
data class GapChoiceDto(val id: String, val text: String)

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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val lexicalTargets: List<LexicalTargetDto>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val targetScopeId: String? = null,
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

    @Serializable
    @SerialName("matching")
    data class Matching(val pairs: List<MatchingPairDto>) : AttemptAnswerDto

    @Serializable
    @SerialName("gaps")
    data class Gaps(val selections: List<GapSelectionDto>) : AttemptAnswerDto
}

@Serializable
data class MatchingPairDto(val leftId: String, val rightId: String)

@Serializable
data class GapSelectionDto(val slotId: String, val choiceId: String)

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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val matchingResults: List<MatchingPairResultDto>? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val gapResults: List<GapResultDto>? = null,
)

@Serializable
data class MatchingPairResultDto(
    val leftId: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val rightId: String? = null,
    val lexicalTarget: LexicalTargetDto,
    val targetSatisfaction: String,
    val meaningSatisfaction: String,
    val responseConstraintStatus: String,
    val linguisticStatus: String,
    val feedback: String,
)

@Serializable
data class GapResultDto(
    val slotId: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val choiceId: String? = null,
    val targetSkillId: String,
    val assessmentRole: String,
    val targetSatisfaction: String,
    val responseConstraintStatus: String,
    val linguisticStatus: String,
    val feedback: String,
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
data class TileTokenDetailsDto(
    val blockId: String,
    val lexicalId: String,
    val form: String,
)

@Serializable
data class TileLicensedOrderDetailsDto(
    val kind: String,
    val blockIds: List<String>,
)

@Serializable
data class TileDistractorDetailsDto(
    val blockId: String,
    val rationale: String,
)

@Serializable
data class TileSourceDetailsDto(
    val tokens: List<TileTokenDetailsDto>,
    val licensedOrders: List<TileLicensedOrderDetailsDto>,
    val distractors: List<TileDistractorDetailsDto>,
)

@Serializable
data class MatchingPairSourceDetailsDto(
    val leftId: String,
    val rightId: String,
    val lexicalSource: LexicalSourceDto,
)

@Serializable
data class MatchingSourceDetailsDto(val pairs: List<MatchingPairSourceDetailsDto>)

@Serializable
data class GapSlotSourceDto(
    val slotId: String,
    val expectedChoiceId: String,
    val lexicalId: String,
    val expectedForm: String,
    val alternativeForm: String,
)

@Serializable
data class GapSourceDto(val slots: List<GapSlotSourceDto>)

@Serializable
data class ModuleOneChoiceRationaleDto(val choice: String, val rationale: String)

@Serializable
data class ModuleOneSourceDetailsDto(
    val taskId: String,
    val targetLexicalId: String,
    val expectedForm: String,
    val knownAlternativeForms: List<String>,
    val referenceRole: String? = null,
    val frameVariantId: String? = null,
    val casePolicy: String,
    val choiceRationales: List<ModuleOneChoiceRationaleDto>,
)

@Serializable
data class InanimateReferenceSourceDetailsDto(
    val taskId: String,
    val targetEntryId: String,
    val targetSenseId: String,
    val expectedForm: String,
    val antecedentEntryId: String,
    val antecedentSenseId: String,
    val antecedentArticle: String,
    val antecedentGender: String,
    val antecedentNumber: String,
    val requestedRole: String,
    val casePolicy: String,
    val contextVariantId: String,
    val knownAlternativeForms: List<String>,
    val choiceRationales: List<ModuleOneChoiceRationaleDto>,
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val tileSource: TileSourceDetailsDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val matchingSource: MatchingSourceDetailsDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val gapSource: GapSourceDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val moduleOneSource: ModuleOneSourceDetailsDto? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val inanimateReferenceSource: InanimateReferenceSourceDetailsDto? = null,
)
