package app.astrid.engine

data class GenerationRequest(
    val formatId: String,
    val targetSkillId: String,
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

data class Skill(
    val id: String,
    val title: String,
    val explanation: String,
    val prerequisiteIds: List<String>,
)

data class ExerciseFormat(
    val id: String,
    val title: String,
    val targetSkillId: String,
)

class GeneratedExercise internal constructor(
    val id: String,
    val formatId: String,
    val targetSkillId: String,
    val supportSkillIds: List<String>,
    val presentation: ExercisePresentation,
    val contentRevision: String,
    val generatorRevision: String,
    val reproduction: Reproduction,
    val inspector: ExerciseInspector,
    internal val source: ExerciseSource,
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
)

data class OrderBlock(
    val id: String,
    val text: String,
)

data class IntroductionSection(
    val title: String,
    val body: String,
)

data class Reproduction(
    val seed: Int,
    val index: Int,
)

data class ExerciseInspector(
    val canonicalSentenceId: String,
    val constructionId: String,
    val lexicalIds: List<String>,
    val clauseRoles: List<String>,
    val canonicalSentence: String,
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

internal data class ExerciseSource(
    val sentence: Sentence,
    val answerKey: AnswerKey,
    val blockRoles: Map<String, String> = emptyMap(),
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
}
