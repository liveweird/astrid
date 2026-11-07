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
) {
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
)

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
)

data class EvaluationIssue(
    val code: String,
    val message: String,
)

sealed interface NormalizedAnswer {
    data class Text(val text: String) : NormalizedAnswer

    data class Order(val blockIds: List<String>) : NormalizedAnswer
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
    DANISH_HEADWORD("danish-headword"),
    DANISH_SENTENCE("danish-sentence"),
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

}
