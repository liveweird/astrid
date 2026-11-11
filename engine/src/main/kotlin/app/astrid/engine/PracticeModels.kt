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
    val introductionExposureKeys: List<String>? = null,
    val introductionRequired: Boolean = false,
    val completionRuleId: String? = null,
    val exposurePolicyId: String? = null,
)

data class PracticeProfile(
    val focusId: String,
    val planRevision: String,
    val title: String,
    val description: String,
    val itemCount: Int,
    val prerequisiteSkillIds: List<String>,
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
    val assessmentMode: String? = null,
    val targetScopeId: String? = null,
)

data class PracticeAssessment(
    val slotId: String?,
    val targetDescriptor: PracticeTargetDescriptor,
    val assessmentRole: String,
    val answerExposureKeys: List<String>,
)

object PracticeTargetKeys {
    fun key(
        targetSkillId: String?,
        lexicalTarget: LexicalTarget?,
        assessmentMode: String? = null,
        targetScopeId: String? = null,
    ): String {
        require((targetSkillId == null) != (lexicalTarget == null)) {
            "A practice target must identify exactly one grammar or lexical target"
        }
        require(assessmentMode == null || assessmentMode in ASSESSMENT_MODES_BY_SKILL[targetSkillId].orEmpty()) {
            "Unsupported assessment mode for $targetSkillId: $assessmentMode"
        }
        require(
            targetScopeId == null ||
                ScopedAssessment(targetSkillId, targetScopeId, assessmentMode) in SCOPED_ASSESSMENTS,
        ) {
            "Unsupported scoped assessment for $targetSkillId/$targetScopeId/$assessmentMode"
        }
        return targetSkillId?.let { skillId ->
            listOfNotNull("grammar", skillId, targetScopeId, assessmentMode).joinToString(":")
        } ?: requireNotNull(lexicalTarget).run {
            require(assessmentMode == null) { "Lexical practice targets do not support assessment modes" }
            listOf("lexical", entryId, senseId, facet, direction).joinToString(":")
        }
    }

    private val ASSESSMENT_MODES_BY_SKILL = mapOf(
        "G003" to setOf("selection", "recall"),
        "G004" to setOf("selection", "recall", "repair"),
        "G005" to setOf("constituent-order", "word-order"),
        "G006" to setOf("selection", "recall"),
        "G038" to setOf("selection", "recall"),
        "G030" to setOf("selection", "recall"),
        "G016" to setOf("constituent-order"),
    )

    private data class ScopedAssessment(
        val skillId: String?,
        val scopeId: String,
        val mode: String?,
    )

    private val SCOPED_ASSESSMENTS = setOf(
        ScopedAssessment("G003", "human-singular", "selection"),
        ScopedAssessment("G003", "human-singular", "recall"),
        ScopedAssessment("G003", "human-group", "selection"),
        ScopedAssessment("G003", "human-group", "recall"),
        ScopedAssessment("G004", "vaere-present", "selection"),
        ScopedAssessment("G004", "vaere-present", "recall"),
        ScopedAssessment("G004", "named-three-verbs-present", "selection"),
        ScopedAssessment("G004", "named-three-verbs-present", "recall"),
        ScopedAssessment("G004", "have-possession-present", "selection"),
        ScopedAssessment("G004", "have-possession-present", "recall"),
        ScopedAssessment("G004", "personal-introduction-present", "selection"),
        ScopedAssessment("G004", "personal-introduction-present", "recall"),
        ScopedAssessment("G005", "subject-first-supplied", "constituent-order"),
        ScopedAssessment("G005", "subject-first-supplied", "word-order"),
        ScopedAssessment("G038", "inanimate-singular-subject", "selection"),
        ScopedAssessment("G038", "inanimate-singular-subject", "recall"),
        ScopedAssessment("G030", "kan-bare-infinitive", "selection"),
        ScopedAssessment("G030", "kan-bare-infinitive", "recall"),
        ScopedAssessment("G030", "modal-volition-meaning", "selection"),
        ScopedAssessment("G030", "modal-obligation-meaning", "selection"),
        ScopedAssessment("G030", "modal-permission-meaning", "selection"),
        ScopedAssessment("G005", "supplied-modal-order", "word-order"),
        ScopedAssessment("G016", "one-final-circumstance", "constituent-order"),
    )
}

fun PracticeTargetDescriptor.targetKey(): String =
    PracticeTargetKeys.key(targetSkillId, lexicalTarget, assessmentMode, targetScopeId)

data class PracticePlanItem(
    override val position: Int,
    val exercise: GeneratedExercise,
    val targetDescriptor: PracticeTargetDescriptor,
    override val answerExposureKeys: List<String>,
    override val promptExposureKeys: List<String>,
    override val lessonExposureKeys: List<String>,
    override val hintExposureKeys: List<String>,
    override val feedbackExposureKeys: List<String>,
    val assessments: List<PracticeAssessment> = emptyList(),
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
    val gapResults: List<GapResult> = emptyList(),
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
    val assessments: List<PracticeAssessment> = emptyList(),
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
    val assessmentMode: String? = null,
    val assessmentRole: String? = null,
    val targetScopeId: String? = null,
)

data class PracticeFollowUpSourceItem(
    val exercise: GeneratedExercise,
    val targetDescriptor: PracticeTargetDescriptor,
    val assessments: List<PracticeAssessment> = emptyList(),
)

data class PracticeFeedbackAttemptObservation(
    val targetStatus: TargetStatus,
    val gapResults: List<GapResult> = emptyList(),
    val linguisticStatus: LinguisticStatus? = null,
    val responseConstraintStatus: ResponseConstraintStatus? = null,
    val issueCodes: List<String> = emptyList(),
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
