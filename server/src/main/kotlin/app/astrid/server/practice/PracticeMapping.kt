package app.astrid.server.practice

import app.astrid.engine.GeneratedExercise
import app.astrid.engine.LinguisticStatus
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticeAttemptContext
import app.astrid.engine.PracticeAssessment
import app.astrid.engine.PracticeCompletionAttemptObservation
import app.astrid.engine.PracticeCompletionPolicy
import app.astrid.engine.PracticeExposureSource
import app.astrid.engine.PracticeFollowUpPlanner
import app.astrid.engine.PracticeFollowUpSourceItem
import app.astrid.engine.PracticePriorExposure
import app.astrid.engine.PracticeSummaryAttemptObservation
import app.astrid.engine.PracticeSummaryCalculator
import app.astrid.engine.PracticeSummarySnapshotItemObservation
import app.astrid.engine.PracticeTargetDescriptor
import app.astrid.engine.ResponseConstraintStatus
import app.astrid.engine.TargetStatus
import app.astrid.server.authz.Conflict
import app.astrid.server.authz.NotFound
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.exercises.PracticeCrossRunExposureDto
import app.astrid.server.exercises.StoredExercise
import app.astrid.server.exercises.frozenFeedbackExposureKeys
import app.astrid.server.exercises.regenerateVerified
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toResponse
import java.util.UUID

internal const val PRACTICE_FOCUS = "reading-basics"
internal const val PRACTICE_REVISION = "practice-1"
internal const val PRACTICE_ITEM_COUNT = 8
internal const val FOLLOW_UP_REVISION = "follow-up-1"
internal const val PRESENT_VERBS_FOCUS = "present-verbs"
internal const val PRESENT_VERBS_REVISION = "present-verbs-1"
internal const val PRESENT_VERBS_FOLLOW_UP_REVISION = "follow-up-2"
internal const val GUIDED_GAPS_FOCUS = "verb-article-gaps"
internal const val GUIDED_GAPS_REVISION = "verb-article-gaps-1"
internal const val GUIDED_GAPS_FOLLOW_UP_REVISION = "follow-up-3"
internal const val MODULE_ONE_SINGULAR_FOCUS = "module-one-singular"
internal const val MODULE_ONE_SINGULAR_REVISION = "module-one-singular-1"
internal const val MODULE_ONE_GROUPS_FOCUS = "module-one-groups"
internal const val MODULE_ONE_GROUPS_REVISION = "module-one-groups-1"
internal const val MODULE_ONE_ER_FOCUS = "module-one-er"
internal const val MODULE_ONE_ER_REVISION = "module-one-er-1"
internal const val MODULE_ONE_THINGS_FOCUS = "module-one-things"
internal const val MODULE_ONE_THINGS_REVISION = "module-one-things-1"
internal const val MODULE_ONE_PRESENT_FOCUS = "module-one-present"
internal const val MODULE_ONE_PRESENT_REVISION = "module-one-present-1"
internal const val MODULE_ONE_ORDER_FOCUS = "module-one-order"
internal const val MODULE_ONE_ORDER_REVISION = "module-one-order-1"
internal const val MODULE_ONE_POSSESSION_FOCUS = "module-one-possession"
internal const val MODULE_ONE_POSSESSION_REVISION = "module-one-possession-1"
internal const val MODULE_ONE_READING_WORDS_FOCUS = "module-one-reading-words"
internal const val MODULE_ONE_READING_WORDS_REVISION = "module-one-reading-words-1"
internal const val MODULE_ONE_PERSONAL_FOCUS = "module-one-personal"
internal const val MODULE_ONE_PERSONAL_REVISION = "module-one-personal-1"
internal const val MODULE_ONE_ABILITY_FOCUS = "module-one-ability"
internal const val MODULE_ONE_ABILITY_REVISION = "module-one-ability-1"
internal const val MODULE_ONE_VOLITION_FOCUS = "module-one-volition"
internal const val MODULE_ONE_VOLITION_REVISION = "module-one-volition-1"
internal const val MODULE_ONE_OBLIGATION_FOCUS = "module-one-obligation"
internal const val MODULE_ONE_OBLIGATION_REVISION = "module-one-obligation-1"
internal const val MODULE_ONE_PERMISSION_FOCUS = "module-one-permission"
internal const val MODULE_ONE_PERMISSION_REVISION = "module-one-permission-1"
private const val TARGETED_REVIEW_FOCUS = "targeted-review"
private const val INTRODUCTION_POSITION = -1

internal fun app.astrid.engine.PracticePlan.toNewPracticeSession(ownerId: UUID) = NewPracticeSession(
    title,
    contentRevision,
    generatorRevision,
    evaluationRevision,
    prerequisiteSkillIds,
    introduction.map { IntroductionDto(it.title, it.body, it.bodyLanguage) },
    items.map { item ->
        val id = UUID.randomUUID()
        NewPracticeItem(
            item.position,
            item.exercise.toStoredPractice(id, ownerId),
            item.targetDescriptor.label,
            item.answerExposureKeys,
            item.promptExposureKeys,
            item.lessonExposureKeys,
            item.hintExposureKeys,
            item.feedbackExposureKeys,
            item.targetDescriptor.assessmentMode,
            item.assessments.map { it.toPayload() },
            item.targetDescriptor.targetScopeId,
        )
    },
    introductionExposureKeys = introductionExposureKeys,
    introductionRequired = introductionRequired,
    completionRuleId = completionRuleId,
    exposurePolicyId = exposurePolicyId,
)

internal fun StoredPracticeSession.toFollowUp(
    request: PracticeFollowUpRequest,
    sourceId: UUID,
    ownerId: UUID,
    engine: PilotEngine,
): NewPracticeSession {
    val sourceItems = items.map { item ->
        PracticeFollowUpSourceItem(
            regenerateVerified(engine, item.exercise),
            PracticeTargetDescriptor(
                item.targetLabel,
                item.exercise.targetSkillId,
                item.exercise.presentation.lexicalTarget?.let {
                    app.astrid.engine.LexicalTarget(it.entryId, it.senseId, it.facet, it.direction)
                },
                item.assessmentMode,
                item.targetScopeId,
            ),
            item.assessments.map { it.toEngineAssessment() },
        )
    }
    val plan = try {
        val planner = PracticeFollowUpPlanner(engine)
        when (request.planRevision) {
            FOLLOW_UP_REVISION -> {
                requireLegacyFollowUpSource()
                planner.plan(request.planRevision, request.seed, request.targetKeys, sourceItems.map { it.exercise })
            }
            PRESENT_VERBS_FOLLOW_UP_REVISION -> {
                requirePresentVerbFollowUpSource()
                planner.planPresentVerbs(request.planRevision, request.seed, request.targetKeys, sourceItems)
            }
            GUIDED_GAPS_FOLLOW_UP_REVISION -> {
                requireGuidedGapsFollowUpSource()
                planner.planGuidedGaps(request.planRevision, request.seed, request.targetKeys, sourceItems)
            }
            else -> error("Unsupported follow-up practice plan")
        }
    } catch (cause: IllegalArgumentException) {
        throw Conflict(cause.message ?: "Unsupported follow-up practice plan").apply { initCause(cause) }
    }
    return NewPracticeSession(
        plan.title,
        plan.contentRevision,
        plan.generatorRevision,
        plan.evaluationRevision,
        plan.prerequisiteSkillIds,
        plan.introduction.map { IntroductionDto(it.title, it.body, it.bodyLanguage) },
        plan.items.map { item ->
            NewPracticeItem(
                item.position,
                item.exercise.toStoredPractice(UUID.randomUUID(), ownerId),
                item.targetDescriptor.label,
                item.answerExposureKeys,
                item.promptExposureKeys,
                item.lessonExposureKeys,
                item.hintExposureKeys,
                item.feedbackExposureKeys,
                item.targetDescriptor.assessmentMode,
                item.assessments.map { it.toPayload() },
                item.targetDescriptor.targetScopeId,
            )
        },
        PracticeFollowUpLineage(sourceId, request.targetKeys),
        sourceExposureSnapshot(),
        plan.introductionExposureKeys,
        plan.introductionRequired,
        plan.completionRuleId,
        plan.exposurePolicyId,
    )
}

private fun StoredPracticeSession.requireLegacyFollowUpSource() {
    val eligible =
        (focusId == PRACTICE_FOCUS && planRevision == PRACTICE_REVISION) ||
            (focusId == TARGETED_REVIEW_FOCUS && planRevision == FOLLOW_UP_REVISION)
    require(eligible && items.all { it.targetScopeId == null }) { "Follow-up-1 requires a legacy mixed-practice source" }
}

private fun StoredPracticeSession.requirePresentVerbFollowUpSource() {
    val eligible =
        (focusId == PRESENT_VERBS_FOCUS && planRevision == PRESENT_VERBS_REVISION) ||
            (focusId == TARGETED_REVIEW_FOCUS && planRevision == PRESENT_VERBS_FOLLOW_UP_REVISION)
    require(eligible) { "Follow-up-2 requires a present-verb source practice" }
    require(items.all { it.exercise.targetSkillId != "G004" || it.assessmentMode != null }) {
        "Follow-up-2 requires frozen assessment modes"
    }
}

private fun StoredPracticeSession.requireGuidedGapsFollowUpSource() {
    val eligible =
        (focusId == GUIDED_GAPS_FOCUS && planRevision == GUIDED_GAPS_REVISION) ||
            (focusId == TARGETED_REVIEW_FOCUS && planRevision == GUIDED_GAPS_FOLLOW_UP_REVISION)
    require(eligible) { "Follow-up-3 requires a guided verb-and-article source practice" }
    require(items.all { it.assessments.isNotEmpty() }) { "Follow-up-3 requires frozen assessments" }
}

internal fun PracticeMutationResult.response() = when (this) {
    is PracticeMutationResult.Success -> session.toResponse()
    PracticeMutationResult.Missing -> throw NotFound()
    PracticeMutationResult.Conflict -> throw Conflict("Practice operation conflicts with current progress")
}

private fun GeneratedExercise.toStoredPractice(id: UUID, ownerSessionId: UUID) = StoredExercise(
    id,
    ownerSessionId,
    formatId,
    targetSkillId,
    reproduction.seed,
    reproduction.index,
    contentRevision,
    generatorRevision,
    toResponse(id.toString()),
    reproductionFingerprint(),
)

internal fun StoredPracticeSession.toResponse(): PracticeSessionResponse {
    val summary = PracticeSummaryCalculator.calculate(
        items.size,
        introductionOpened,
        items.map { stored ->
            PracticeSummarySnapshotItemObservation(
                stored.position,
                PracticeTargetDescriptor(
                    stored.targetLabel,
                    stored.exercise.targetSkillId,
                    stored.exercise.presentation.lexicalTarget?.let {
                        app.astrid.engine.LexicalTarget(it.entryId, it.senseId, it.facet, it.direction)
                    },
                    stored.assessmentMode,
                    stored.targetScopeId,
                ),
                advanced = stored.position < cursor,
                lessonOpened = stored.lessonOpened,
                hintOpened = stored.hintOpened,
                attempts = stored.attempts.map { attempt ->
                    PracticeSummaryAttemptObservation(
                        enumByWire<TargetStatus>(attempt.response.targetSatisfaction),
                        enumByWire<LinguisticStatus>(attempt.response.linguisticStatus),
                        attempt.response.responseConstraintStatus?.let { enumByWire<ResponseConstraintStatus>(it) },
                        attempt.context.toEngine(),
                        attempt.response.hintUsed,
                        attempt.response.gapResults.orEmpty().map { it.toEngine() },
                    )
                },
                assessments = stored.assessments.map { it.toEngineAssessment() },
            )
        },
    )
    return PracticeSessionResponse(
        id.toString(), ownerSessionId.toString(), focusId, title, planRevision, seed,
        contentRevision, generatorRevision, evaluationRevision, sessionExpiresAt,
        if (cursor == items.size) "completed" else "active", cursor, items.size,
        prerequisiteSkillIds, introduction, introductionOpened,
        items.map { item -> itemToDto(item) }, items.singleOrNull { it.position == cursor }?.exercise?.presentation,
        summary.toDto(includeRows = cursor == items.size),
        followUp?.let { PracticeFollowUpDto(it.sourcePracticeSessionId.toString(), it.selectedTargetKeys) },
        followUpPlanRevision(),
        introductionRequired,
    )
}

private fun StoredPracticeSession.followUpPlanRevision(): String? =
    when {
        (focusId == PRESENT_VERBS_FOCUS && planRevision == PRESENT_VERBS_REVISION) ||
            (focusId == TARGETED_REVIEW_FOCUS && planRevision == PRESENT_VERBS_FOLLOW_UP_REVISION) ->
            PRESENT_VERBS_FOLLOW_UP_REVISION
        (focusId == GUIDED_GAPS_FOCUS && planRevision == GUIDED_GAPS_REVISION) ||
            (focusId == TARGETED_REVIEW_FOCUS && planRevision == GUIDED_GAPS_FOLLOW_UP_REVISION) ->
            GUIDED_GAPS_FOLLOW_UP_REVISION
        else -> null
    }

private fun StoredPracticeSession.itemToDto(item: StoredPracticeItem): PracticeItemDto = PracticeItemDto(
    item.position,
    item.exercise.id.toString(),
    item.exercise.formatId,
    item.exercise.targetSkillId,
    item.exercise.presentation.lexicalTarget,
    item.lessonOpened,
    item.hintOpened,
    item.attempts.map { it.response.attemptId },
    item.attempts.size,
    item.attempts.firstOrNull()?.response,
    item.attempts.lastOrNull()?.response,
    item.attempts.firstOrNull()?.answer,
    item.attempts.lastOrNull()?.answer,
    item.attempts.firstOrNull()?.context,
    item.attempts.lastOrNull()?.context,
    item.assessmentMode,
    item.assessments.map {
        PracticeAssessmentDto(it.slotId, it.label, it.targetSkillId, it.assessmentMode, it.assessmentRole, it.targetScopeId)
    },
    item.targetScopeId,
    completionRuleId?.let { rule ->
        if (item.position == cursor) {
            PracticeCompletionPolicy.canAdvance(rule, item.attempts.lastOrNull()?.response?.completionObservation())
        } else {
            null
        }
    },
)

private fun AttemptResponse.completionObservation() = PracticeCompletionAttemptObservation(
    enumByWire(targetSatisfaction),
    enumByWire(linguisticStatus),
    responseConstraintStatus?.let { enumByWire(it) },
    issues.map { it.code },
)
private fun PracticeAttemptContextDto.toEngine() = PracticeAttemptContext(
    introductionOpened,
    lessonOpened,
    hintOpened,
    priorExposure.map { exposure ->
        PracticePriorExposure(
            exposure.position,
            PracticeExposureSource.entries.single { it.wireValue == exposure.source },
            exposure.keys,
        )
    },
    sourceExposure.map { exposure ->
        app.astrid.engine.PracticeCrossRunExposure(
            exposure.practiceSessionId,
            exposure.position,
            PracticeExposureSource.entries.single { it.wireValue == exposure.source },
            exposure.keys,
        )
    },
)

private fun StoredPracticeSession.sourceExposureSnapshot(): List<PracticeCrossRunExposureDto> {
    val events = buildList {
        addAll(sourceExposure)
        if (introductionOpened) {
            add(
                PracticeCrossRunExposureDto(
                    id.toString(),
                    INTRODUCTION_POSITION,
                    PracticeExposureSource.INTRODUCTION.wireValue,
                    (introductionExposureKeys ?: items.flatMap { it.lessonExposureKeys }).distinct(),
                ),
            )
        }
        items.sortedBy { it.position }.forEach { item ->
            add(PracticeCrossRunExposureDto(id.toString(), item.position, "prompt", item.promptExposureKeys))
            if (item.lessonOpened) add(PracticeCrossRunExposureDto(id.toString(), item.position, "lesson", item.lessonExposureKeys))
            if (item.hintOpened) add(PracticeCrossRunExposureDto(id.toString(), item.position, "hint", item.hintExposureKeys))
            if (item.attempts.isNotEmpty()) {
                add(
                    PracticeCrossRunExposureDto(
                        id.toString(),
                        item.position,
                        "feedback",
                        frozenFeedbackExposureKeys(
                            item.feedbackExposureKeys,
                            item.assessments.map { it.toEngineAssessment() },
                            item.attempts.map { it.response },
                            exposurePolicyId,
                        ),
                    ),
                )
            }
        }
    }
    val witnessByKey = linkedMapOf<String, PracticeCrossRunExposureDto>()
    events.forEach { event -> event.keys.distinct().forEach { key -> witnessByKey.putIfAbsent(key, event.copy(keys = listOf(key))) } }
    return witnessByKey.values.groupBy { Triple(it.practiceSessionId, it.position, it.source) }.map { (origin, witnesses) ->
        PracticeCrossRunExposureDto(origin.first, origin.second, origin.third, witnesses.flatMap { it.keys })
    }
}

private fun app.astrid.engine.PracticeSummary.toDto(includeRows: Boolean) = PracticeSummaryDto(
    plannedItems, attemptedItems, completedItems, attemptCount, retryCount, introductionOpened,
    rows.takeIf { includeRows }.orEmpty().map { row ->
        PracticeSummaryRowDto(
            row.label,
            row.targetSkillId,
            row.lexicalTarget?.let { LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction) },
            row.plannedItems,
            row.attemptedItems,
            row.advancedItems,
            row.attemptCount,
            row.retryCount,
            row.first.toDto(),
            row.latest.toDto(),
            row.firstWithSupport,
            row.firstWithPriorExposure,
            row.lessonOpenedItems,
            row.hintOpenedItems,
            row.assessmentMode,
            row.assessmentRole,
            row.targetScopeId,
        )
    },
)

private fun app.astrid.engine.PracticeOutcomeCounts.toDto() = PracticeOutcomeCountsDto(
    fullyMet, targetMet, targetNotMet, unevaluable, languageUnknown, languageKnownIssue, responseConstraintNotMet,
)

private inline fun <reified T : Enum<T>> enumByWire(value: String): T = enumValues<T>().single { entry ->
    when (entry) {
        is TargetStatus -> entry.wireValue
        is LinguisticStatus -> entry.wireValue
        is ResponseConstraintStatus -> entry.wireValue
        else -> error("Unsupported wire enum")
    } == value
}

private fun PracticeAssessment.toPayload() = PracticeAssessmentPayload(
    slotId,
    targetDescriptor.label,
    requireNotNull(targetDescriptor.targetSkillId),
    requireNotNull(targetDescriptor.assessmentMode),
    assessmentRole,
    answerExposureKeys,
    targetDescriptor.targetScopeId,
)

private fun app.astrid.server.exercises.GapResultDto.toEngine() = app.astrid.engine.GapResult(
    slotId,
    choiceId,
    targetSkillId,
    app.astrid.engine.GapAssessmentRole.entries.single { it.wireValue == assessmentRole },
    enumByWire(targetSatisfaction),
    enumByWire(responseConstraintStatus),
    enumByWire(linguisticStatus),
    feedback,
)
