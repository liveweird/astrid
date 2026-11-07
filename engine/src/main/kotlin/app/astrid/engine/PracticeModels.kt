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

interface PracticeExposureMetadata {
    val position: Int
    val answerExposureKeys: List<String>
    val promptExposureKeys: List<String>
    val lessonExposureKeys: List<String>
    val hintExposureKeys: List<String>
    val feedbackExposureKeys: List<String>
}

data class PracticeExposureSnapshot(
    override val position: Int,
    override val answerExposureKeys: List<String>,
    override val promptExposureKeys: List<String>,
    override val lessonExposureKeys: List<String>,
    override val hintExposureKeys: List<String>,
    override val feedbackExposureKeys: List<String>,
) : PracticeExposureMetadata

data class PracticeTargetDescriptor(
    val label: String,
    val targetSkillId: String?,
    val lexicalTarget: LexicalTarget?,
)

object PracticeTargetKeys {
    fun key(
        targetSkillId: String?,
        lexicalTarget: LexicalTarget?,
    ): String {
        require((targetSkillId == null) != (lexicalTarget == null)) {
            "A practice target must identify exactly one grammar or lexical target"
        }
        return targetSkillId?.let { "grammar:$it" } ?: requireNotNull(lexicalTarget).run {
            listOf("lexical", entryId, senseId, facet, direction).joinToString(":")
        }
    }
}

fun PracticeTargetDescriptor.targetKey(): String = PracticeTargetKeys.key(targetSkillId, lexicalTarget)

data class PracticePlanItem(
    override val position: Int,
    val exercise: GeneratedExercise,
    val targetDescriptor: PracticeTargetDescriptor,
    override val answerExposureKeys: List<String>,
    override val promptExposureKeys: List<String>,
    override val lessonExposureKeys: List<String>,
    override val hintExposureKeys: List<String>,
    override val feedbackExposureKeys: List<String>,
) : PracticeExposureMetadata

enum class PracticeExposureSource(val wireValue: String) {
    INTRODUCTION("introduction"),
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

data class PracticeCrossRunExposure(
    val practiceSessionId: String,
    val position: Int,
    val source: PracticeExposureSource,
    val keys: List<String>,
)

data class PracticeAttemptContext(
    val introductionOpened: Boolean,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val priorExposure: List<PracticePriorExposure>,
    val sourceExposure: List<PracticeCrossRunExposure> = emptyList(),
)

data class PracticeItemExposureObservation(
    val item: PracticeExposureMetadata,
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

data class PracticeSummarySnapshotItemObservation(
    val position: Int,
    val targetDescriptor: PracticeTargetDescriptor,
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
