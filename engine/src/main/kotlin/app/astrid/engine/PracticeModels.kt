package app.astrid.engine

data class PracticePlan(
    val focusId: String,
    val planRevision: String,
    val seed: Int,
    val title: String,
    val prerequisiteSkillIds: List<String>,
    val introduction: List<IntroductionSection>,
    val items: List<PracticePlanItem>,
    val contentRevision: String,
    val generatorRevision: String,
    val evaluationRevision: String,
)

data class PracticePlanItem(
    val position: Int,
    val exercise: GeneratedExercise,
    val answerExposureKeys: List<String>,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
)

enum class PracticeExposureSource(val wireValue: String) {
    PROMPT("prompt"),
    LESSON("lesson"),
    HINT("hint"),
    FEEDBACK("feedback"),
}

data class PracticePriorExposure(
    val position: Int,
    val source: PracticeExposureSource,
    val keys: List<String>,
)

data class PracticeAttemptContext(
    val introductionOpened: Boolean,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val priorExposure: List<PracticePriorExposure>,
)

data class PracticeItemExposureObservation(
    val item: PracticePlanItem,
    val promptReleased: Boolean,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val feedbackReleased: Boolean,
)

data class PracticeSummaryAttemptObservation(
    val targetStatus: TargetStatus,
    val linguisticStatus: LinguisticStatus,
    val responseConstraintStatus: ResponseConstraintStatus?,
    val context: PracticeAttemptContext,
    val hintUsed: Boolean = false,
)

data class PracticeSummaryItemObservation(
    val item: PracticePlanItem,
    val advanced: Boolean,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val attempts: List<PracticeSummaryAttemptObservation>,
)

data class PracticeSummary(
    val plannedItems: Int,
    val attemptedItems: Int,
    val completedItems: Int,
    val attemptCount: Int,
    val retryCount: Int,
    val introductionOpened: Boolean,
    val rows: List<PracticeTargetSummary>,
)

data class PracticeTargetSummary(
    val label: String,
    val targetSkillId: String?,
    val lexicalTarget: LexicalTarget?,
    val plannedItems: Int,
    val attemptedItems: Int,
    val advancedItems: Int,
    val attemptCount: Int,
    val retryCount: Int,
    val first: PracticeOutcomeCounts,
    val latest: PracticeOutcomeCounts,
    val firstWithSupport: Int,
    val firstWithPriorExposure: Int,
    val lessonOpenedItems: Int,
    val hintOpenedItems: Int,
)

data class PracticeOutcomeCounts(
    val fullyMet: Int = 0,
    val targetMet: Int = 0,
    val targetNotMet: Int = 0,
    val unevaluable: Int = 0,
    val languageUnknown: Int = 0,
    val languageKnownIssue: Int = 0,
    val responseConstraintNotMet: Int = 0,
)
