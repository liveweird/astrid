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
    val readingTarget: ReadingTask? = null,
    val readingRole: String? = null,
    val matchingTarget: LexicalTask? = null,
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
        readingTarget: ReadingTask? = null,
        readingRole: String? = null,
        matchingTarget: LexicalTask? = null,
    ): String {
        require(listOf(targetSkillId, lexicalTarget, readingTarget, matchingTarget).count { it != null } == 1) {
            "A practice target must identify exactly one grammar, lexical, reading, or matching target"
        }
        return when {
            matchingTarget != null -> matchingKey(matchingTarget, targetScopeId, assessmentMode, readingRole)
            readingTarget != null -> readingKey(readingTarget, targetScopeId, assessmentMode, readingRole)
            targetSkillId != null -> grammarKey(targetSkillId, targetScopeId, assessmentMode)
            else -> lexicalKey(requireNotNull(lexicalTarget), targetScopeId, assessmentMode)
        }
    }

    private fun matchingKey(target: LexicalTask, scope: String?, mode: String?, role: String?): String {
        require(scope == null && mode == null && role == null) {
            "Matching board targets do not support scope, assessment mode, or reading role"
        }
        require(target == LexicalTask("meaning", "en-da")) { "Unsupported matching board target" }
        return listOf("matching", target.facet, target.direction).joinToString(":")
    }

    private fun readingKey(target: ReadingTask, scope: String?, mode: String?, role: String?): String {
        require(scope == null && mode == null && role in READING_ROLES) {
            "Reading practice targets require an explicit fact role only"
        }
        return listOf("reading", target.facet, role).joinToString(":")
    }

    private fun grammarKey(skillId: String, scope: String?, mode: String?): String {
        require(mode == null || mode in ASSESSMENT_MODES_BY_SKILL[skillId].orEmpty()) {
            "Unsupported assessment mode for $skillId: $mode"
        }
        require(scope == null || ScopedAssessment(skillId, scope, mode) in SCOPED_ASSESSMENTS) {
            "Unsupported scoped assessment for $skillId/$scope/$mode"
        }
        return listOfNotNull("grammar", skillId, scope, mode).joinToString(":")
    }

    private fun lexicalKey(target: LexicalTarget, scope: String?, mode: String?): String {
        require(scope == null) { "Lexical practice targets do not support scopes" }
        require(mode == null || mode == MATCHING_MODE && target.facet == "meaning" && target.direction == "en-da") {
            "Lexical practice targets support matching mode only for English-to-Danish meaning pairs"
        }
        return listOfNotNull("lexical", target.entryId, target.senseId, target.facet, target.direction, mode).joinToString(":")
    }

    private val ASSESSMENT_MODES_BY_SKILL = mapOf(
        "G003" to setOf("selection", "recall"),
        "G004" to setOf("selection", "recall", "repair"),
        "G005" to setOf("constituent-order", "word-order"),
        "G006" to setOf("selection", "recall"),
        "G038" to setOf("selection", "recall"),
        "G030" to setOf("selection", "recall"),
        "G016" to setOf("selection", "constituent-order"),
        "G018" to setOf("constituent-order"),
    )
        private val READING_ROLES = setOf(
            "object", "place", "time", "subject", "last-event", "action", "destination", "static-location",
        )
    private const val MATCHING_MODE = "matching"

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
        ScopedAssessment("G018", "neutral-positive-yes-no", "constituent-order"),
        ScopedAssessment("G004", "p1-studere-present", "recall"),
        ScopedAssessment("G004", "p1-drikke-present", "recall"),
        ScopedAssessment("G004", "p1-drikke-present", "selection"),
        ScopedAssessment("G005", "p1-subject-first-supplied", "word-order"),
        ScopedAssessment("G005", "p2-subject-first-supplied", "word-order"),
        ScopedAssessment("G005", "p2-polite-wish", "word-order"),
        ScopedAssessment("G005", "p2-polite-request", "word-order"),
        ScopedAssessment("G004", "p3-kore-present", "recall"),
        ScopedAssessment("G005", "p3-subject-first-supplied", "word-order"),
        ScopedAssessment("G016", "p3-home-spatial-contrast", "selection"),
        ScopedAssessment("G016", "p3-station-spatial-contrast", "selection"),
        ScopedAssessment("G016", "p3-home-direction", "selection"),
    )
}

fun PracticeTargetDescriptor.targetKey(): String =
    PracticeTargetKeys.key(targetSkillId, lexicalTarget, assessmentMode, targetScopeId, readingTarget, readingRole, matchingTarget)

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
    val policy: PracticeItemPolicy? = null,
) : PracticeExposureMetadata

/** A frozen item-level override used when one practice run mixes items from plans with different policies. */
data class PracticeItemPolicy(
    val completionRuleId: String?,
    val exposurePolicyId: String?,
)

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
    val meaningSatisfaction: MeaningSatisfaction? = null,
    val matchingResults: List<MatchingPairResult> = emptyList(),
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
    val readingTarget: ReadingTask? = null,
    val readingRole: String? = null,
    val matchingTarget: LexicalTask? = null,
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
    val meaningSatisfaction: MeaningSatisfaction? = null,
    val matchingResults: List<MatchingPairResult> = emptyList(),
)

data class PracticeOutcomeCounts(
    val fullyMet: Int = 0,
    val targetMet: Int = 0,
    val targetNotMet: Int = 0,
    val unevaluable: Int = 0,
    val languageUnknown: Int = 0,
    val languageKnownIssue: Int = 0,
    val responseConstraintNotMet: Int = 0,
    val meaningMet: Int? = null,
    val meaningNotMet: Int? = null,
    val meaningUnknown: Int? = null,
)
