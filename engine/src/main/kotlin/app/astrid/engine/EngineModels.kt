package app.astrid.engine

data class GenerationRequest(
    val formatId: String,
    val targetSkillId: String? = null,
    val seed: Int,
    val index: Int,
    val contentRevision: String = "pilot-1",
    val generatorRevision: String = "generator-1",
)

data class Catalog(
    val skills: List<Skill>,
    val formats: List<ExerciseFormat>,
    val sentenceCount: Int,
    val contentRevision: String,
    val generatorRevision: String,
)

data class RevisionPair(
    val contentRevision: String,
    val generatorRevision: String,
)

data class Skill(
    val id: String,
    val title: String,
    val explanation: String,
    val prerequisiteIds: List<String>,
)

data class ExerciseFormat(
    val id: String,
    val title: String,
    val targetSkillId: String?,
    val lexicalTask: LexicalTask? = null,
    val readingTask: ReadingTask? = null,
)

data class LexicalTask(
    val facet: String,
    val direction: String,
)

data class ReadingTask(
    val facet: String,
)

data class LexicalTarget(
    val entryId: String,
    val senseId: String,
    val facet: String,
    val direction: String,
)

class GeneratedExercise internal constructor(
    val id: String,
    val formatId: String,
    val targetSkillId: String?,
    val lexicalTarget: LexicalTarget?,
    val supportSkillIds: List<String>,
    val presentation: ExercisePresentation,
    val contentRevision: String,
    val generatorRevision: String,
    val reproduction: Reproduction,
    val inspector: ExerciseInspector,
    internal val source: ExerciseSource,
    val readingTarget: ReadingTask? = null,
    val lexicalTargets: List<LexicalTarget>? = null,
) {
    val targetScopeId: String? get() = when (val exerciseSource = source) {
        is ModuleOneExerciseSource -> exerciseSource.task.targetScopeId
        is InanimateReferenceExerciseSource -> exerciseSource.task.targetScopeId
        is PresentOrderExerciseSource -> exerciseSource.task.targetScopeId
        is AbilityExerciseSource -> exerciseSource.task.targetScopeId
        is ModalInterpretationExerciseSource -> exerciseSource.task.targetScopeId
        is CircumstanceExerciseSource -> exerciseSource.task.targetScopeId
        is QuestionExerciseSource -> exerciseSource.task.targetScopeId
        is VocabularyP1ExerciseSource -> exerciseSource.task.targetScopeId
        is VocabularyP2ExerciseSource -> exerciseSource.task.targetScopeId
        else -> null
    }
    val instruction: String get() = presentation.instruction
    val context: String get() = presentation.context
    val prompt: ExercisePrompt get() = presentation.prompt
    val introduction: List<IntroductionSection> get() = presentation.introduction
    val hint: String get() = presentation.hint
}

data class ExercisePresentation(
    val instruction: String,
    val context: String,
    val prompt: ExercisePrompt,
    val introduction: List<IntroductionSection>,
    val hint: String,
)

data class ExercisePrompt(
    val before: String,
    val after: String,
    val choices: List<String> = emptyList(),
    val blocks: List<OrderBlock> = emptyList(),
    val punctuation: String = ".",
    val inputLabel: String? = null,
    val replacementWord: String? = null,
    val cue: String? = null,
    val cueLanguage: String? = null,
    val answerLanguage: String? = null,
    val passage: List<String>? = null,
    val tileMode: String? = null,
    val requiredTileCount: Int? = null,
    val matchingColumns: MatchingColumns? = null,
    val gapPrompt: GapPrompt? = null,
    val fixedPunctuation: List<FixedPunctuation> = emptyList(),
)

data class FixedPunctuation(val beforeBlockId: String, val text: String)

data class GapPrompt(val segments: List<String>, val slots: List<GapSlot>)

data class GapSlot(
    val id: String,
    val label: String,
    val targetSkillId: String,
    val assessmentRole: GapAssessmentRole,
    val choices: List<GapChoice>,
)

data class GapChoice(val id: String, val text: String)

enum class GapAssessmentRole(val wireValue: String) {
    PRIMARY("primary"),
    SUPPORT("support"),
}

data class GapSelection(val slotId: String, val choiceId: String)

data class MatchingColumns(
    val left: List<MatchingItem>,
    val right: List<MatchingItem>,
)

data class MatchingItem(val id: String, val text: String)

data class MatchingPair(val leftId: String, val rightId: String)

data class OrderBlock(
    val id: String,
    val text: String,
)

data class IntroductionSection(
    val title: String,
    val body: String,
    val bodyLanguage: String? = null,
)

data class Reproduction(
    val seed: Int,
    val index: Int,
)

data class ExerciseInspector(
    val canonicalSentenceId: String? = null,
    val constructionId: String? = null,
    val lexicalIds: List<String>,
    val clauseRoles: List<String>,
    val canonicalSentence: String? = null,
    val lexicalSource: LexicalSourceDetails? = null,
    val readingSource: ReadingSourceDetails? = null,
    val tileSource: TileSourceDetails? = null,
    val matchingSource: MatchingSourceDetails? = null,
    val gapSource: GapSourceDetails? = null,
    val moduleOneSource: ModuleOneSourceDetails? = null,
    val inanimateReferenceSource: InanimateReferenceSourceDetails? = null,
    val modalSource: ModalSourceDetails? = null,
    val vocabularySource: VocabularyExerciseSourceDetails? = null,
)

enum class VocabularyLexicalKind(val wireValue: String) {
    COUNT_BUNDLE("count-bundle"),
    VERB_FORM("verb-form"),
    MASS_WORD("mass-word"),
    BARE_LANGUAGE("bare-language"),
    ADVERB("adverb"),
    FIXED_CHUNK("fixed-chunk"),
    ADJECTIVE("adjective"),
    INTERJECTION("interjection"),
}

data class VocabularyLexicalForms(
    val infinitive: String? = null,
    val present: String? = null,
    val bundle: String? = null,
    val form: String? = null,
)

data class VocabularyLexicalSourceDetails(
    val entryId: String,
    val senseId: String,
    val kind: VocabularyLexicalKind,
    val forms: VocabularyLexicalForms,
    val gloss: String,
    val selectedUse: String,
)

data class VocabularyExerciseSourceDetails(
    val taskId: String,
    val operation: String,
    val lexicalSources: List<VocabularyLexicalSourceDetails>,
    val expectedAnswer: String? = null,
    val suppliedChoices: List<String> = emptyList(),
    val supportFacts: List<VocabularySupportFactDetails> = emptyList(),
    val matchingPairs: List<VocabularyMatchingPairSourceDetails> = emptyList(),
    val tileOccurrences: List<VocabularyTileOccurrenceDetails> = emptyList(),
    val acceptedOrders: List<List<String>> = emptyList(),
    val targetSupportFactId: String? = null,
    val questionRole: String? = null,
    val normalizationPolicy: String? = null,
    val knownAlternatives: List<VocabularyKnownAlternativeDetails> = emptyList(),
    val reviewedOrders: List<VocabularyReviewedOrderDetails> = emptyList(),
    val chronologyRelations: List<VocabularyChronologyRelationDetails> = emptyList(),
    val targetChronologyRelationId: String? = null,
)

data class VocabularySupportFactDetails(
    val factId: String,
    val referentId: String,
    val predicateId: String,
    val valueId: String,
    val valueText: String,
    val valueRole: String,
    val sentence: String,
    val exposureKeys: List<String>,
    val referentDisplay: String? = null,
    val predicateSource: VocabularyLexicalSourceDetails? = null,
    val objectSource: VocabularyLexicalSourceDetails? = null,
    val quantitySource: VocabularyQuantitySourceDetails? = null,
    val translation: String? = null,
)

data class VocabularyChronologyRelationDetails(
    val relationId: String,
    val beforeFactId: String,
    val afterFactId: String,
)

data class VocabularyQuantitySourceDetails(
    val constructionId: String,
    val display: String,
    val headSource: VocabularyLexicalSourceDetails,
    val componentSources: List<VocabularyLexicalSourceDetails>,
)

data class VocabularyKnownAlternativeDetails(
    val text: String,
    val targetStatus: TargetStatus,
    val linguisticStatus: LinguisticStatus,
    val meaningSatisfaction: MeaningSatisfaction,
    val responseConstraintStatus: ResponseConstraintStatus,
    val issueCode: String?,
)

data class VocabularyReviewedOrderDetails(
    val words: List<String>,
    val targetStatus: TargetStatus,
    val linguisticStatus: LinguisticStatus,
    val meaningSatisfaction: MeaningSatisfaction,
    val responseConstraintStatus: ResponseConstraintStatus,
    val issueCode: String?,
)

data class VocabularyMatchingPairSourceDetails(
    val leftId: String,
    val rightId: String,
    val lexicalSource: VocabularyLexicalSourceDetails,
)

data class VocabularyTileOccurrenceDetails(
    val blockId: String,
    val lexicalId: String,
    val form: String,
    val lexicalSource: VocabularyLexicalSourceDetails? = null,
    val quantitySource: VocabularyQuantitySourceDetails? = null,
)

data class InanimateReferenceSourceDetails(
    val taskId: String,
    val targetEntryId: String,
    val targetSenseId: String,
    val expectedForm: String,
    val knownAlternativeForms: List<String>,
    val antecedentEntryId: String,
    val antecedentSenseId: String,
    val antecedentArticle: String,
    val antecedentGender: String,
    val antecedentNumber: String,
    val requestedRole: String,
    val casePolicy: String,
    val contextVariantId: String,
    val choiceRationales: List<ModuleOneChoiceRationale>,
)

data class ModuleOneSourceDetails(
    val taskId: String,
    val targetLexicalId: String,
    val expectedForm: String,
    val knownAlternativeForms: List<String>,
    val referenceRole: String?,
    val frameVariantId: String?,
    val casePolicy: String,
    val choiceRationales: List<ModuleOneChoiceRationale>,
)

data class ModuleOneChoiceRationale(
    val choice: String,
    val rationale: String,
)

data class GapSourceDetails(val slots: List<GapSlotSourceDetails>)

data class GapSlotSourceDetails(
    val slotId: String,
    val expectedChoiceId: String,
    val lexicalId: String,
    val expectedForm: String,
    val alternativeForm: String,
)

data class MatchingSourceDetails(val pairs: List<MatchingPairSourceDetails>)

data class MatchingPairSourceDetails(
    val leftId: String,
    val rightId: String,
    val lexicalSource: LexicalSourceDetails,
)

data class TileSourceDetails(
    val tokens: List<TileTokenDetails>,
    val licensedOrders: List<TileLicensedOrderDetails>,
    val distractors: List<TileDistractorDetails>,
)

data class TileTokenDetails(
    val blockId: String,
    val lexicalId: String,
    val form: String,
)

data class TileLicensedOrderDetails(
    val kind: String,
    val blockIds: List<String>,
)

data class TileDistractorDetails(
    val blockId: String,
    val rationale: String,
)

data class ReadingSourceDetails(
    val propositions: List<ReadingPropositionDetails>,
    val questionSubjectId: String,
    val questionVerbId: String,
    val answerNounId: String,
    val answer: String,
)

data class ReadingPropositionDetails(
    val sentenceId: String,
    val subjectId: String,
    val verbId: String,
    val objectId: String,
)

data class LexicalSourceDetails(
    val entryId: String,
    val senseId: String,
    val headword: String,
    val gloss: String,
    val article: String,
)

sealed interface Answer {
    data class Text(val text: String) : Answer

    data class Order(val blockIds: List<String>) : Answer

    data class Matching(val pairs: List<MatchingPair>) : Answer

    data class Gaps(val selections: List<GapSelection>) : Answer
}

data class Evaluation(
    val targetStatus: TargetStatus,
    val linguisticStatus: LinguisticStatus,
    val issues: List<EvaluationIssue>,
    val feedback: String,
    val completedSentence: String?,
    val rawAnswer: Answer,
    val normalizedAnswer: NormalizedAnswer,
    val linguisticScope: LinguisticScope? = null,
    val responseConstraintStatus: ResponseConstraintStatus? = null,
    val meaningSatisfaction: MeaningSatisfaction? = null,
    val matchingResults: List<MatchingPairResult>? = null,
    val gapResults: List<GapResult>? = null,
)

data class GapResult(
    val slotId: String,
    val choiceId: String?,
    val targetSkillId: String,
    val assessmentRole: GapAssessmentRole,
    val targetStatus: TargetStatus,
    val responseConstraintStatus: ResponseConstraintStatus,
    val linguisticStatus: LinguisticStatus,
    val feedback: String,
)

data class MatchingPairResult(
    val leftId: String,
    val rightId: String?,
    val lexicalTarget: LexicalTarget,
    val targetStatus: TargetStatus,
    val meaningSatisfaction: MeaningSatisfaction,
    val responseConstraintStatus: ResponseConstraintStatus,
    val linguisticStatus: LinguisticStatus,
    val feedback: String,
)

data class EvaluationIssue(
    val code: String,
    val message: String,
)

sealed interface NormalizedAnswer {
    data class Text(val text: String) : NormalizedAnswer

    data class Order(val blockIds: List<String>) : NormalizedAnswer

    data class Matching(val pairs: List<MatchingPair>) : NormalizedAnswer

    data class Gaps(val selections: List<GapSelection>) : NormalizedAnswer
}

enum class TargetStatus(val wireValue: String) {
    MET("met"),
    NOT_MET("not-met"),
    UNEVALUABLE("unevaluable"),
}

enum class LinguisticStatus(val wireValue: String) {
    ACCEPTABLE("acceptable"),
    KNOWN_ISSUE("known-issue"),
    UNKNOWN("unknown"),
}

enum class LinguisticScope(val wireValue: String) {
    ENGLISH_CHOICE("english-choice"),
    DANISH_CHOICE("danish-choice"),
    DANISH_HEADWORD("danish-headword"),
    DANISH_SENTENCE("danish-sentence"),
    DANISH_MATCHING("danish-matching"),
    DANISH_GAPS("danish-gaps"),
    DANISH_WORD_FORM("danish-word-form"),
}

enum class MeaningSatisfaction(val wireValue: String) {
    MET("met"),
    NOT_MET("not-met"),
    UNKNOWN("unknown"),
}

enum class ResponseConstraintStatus(val wireValue: String) {
    MET("met"),
    NOT_MET("not-met"),
}

internal sealed interface ExerciseSource {
    val answerKey: AnswerKey
    val blockRoles: Map<String, String>
}

internal data class VocabularyP1ExerciseSource(
    val task: VocabularyP1Task,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class VocabularyP2ExerciseSource(
    val task: VocabularyP2Task,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class SentenceSource(
    val sentence: Sentence,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class LexicalSource(
    val sense: LexicalSense,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class ReadingSource(
    val propositions: List<ReadingProposition>,
    val questionSubjectId: String,
    val questionVerbId: String,
    val answerNounId: String,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class TileExerciseSource(
    val sentence: Sentence?,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class MatchingExerciseSource(
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class MultipleGapExerciseSource(
    val sentence: Sentence,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class ModuleOneExerciseSource(
    val task: ModuleOneTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class InanimateReferenceExerciseSource(
    val task: InanimateReferenceTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class PresentOrderExerciseSource(
    val task: PresentOrderTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal data class ReadingProposition(
    val sentence: Sentence,
    val subjectId: String,
    val verbId: String,
    val objectId: String,
)

internal data class LexicalSense(
    val entryId: String,
    val senseId: String,
    val headword: String,
    val gloss: String,
    val article: String,
)

internal data class Sentence(
    val id: String,
    val constructionId: String,
    val subject: Constituent,
    val verb: Verb,
    val complement: Constituent?,
)

internal data class Constituent(
    val lexicalIds: List<String>,
    val text: String,
    val role: String,
)

internal data class Verb(
    val lexicalId: String,
    val lemma: String,
    val present: String,
    val gloss: String,
)

internal sealed interface AnswerKey {
    data class PresentForm(val infinitive: String, val present: String) : AnswerKey

    data class Article(val expected: String, val noun: String) : AnswerKey

    data class SubjectFirstOrder(val canonicalRoles: List<String>) : AnswerKey

    data class MeaningChoice(val expected: String, val suppliedChoices: Set<String>) : AnswerKey

    data class Headword(val expected: LexicalSense) : AnswerKey

    data class SuppliedChoice(val expected: String, val suppliedChoices: Set<String>) : AnswerKey

    data class TileOrder(
        val canonical: List<TileIdentity>,
        val fronted: List<TileIdentity>?,
        val infinitive: List<TileIdentity>?,
    ) : AnswerKey

    data class Matching(val expectedByLeftId: Map<String, String>) : AnswerKey

    data class Gaps(val expectedBySlotId: Map<String, String>) : AnswerKey

    data class ModuleOneForm(
        val expected: String,
        val knownAlternatives: Set<String>,
        val selection: Boolean,
    ) : AnswerKey

}

internal data class TileIdentity(
    val lexicalId: String,
    val form: String,
)
