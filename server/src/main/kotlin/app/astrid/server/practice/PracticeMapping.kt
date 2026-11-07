package app.astrid.server.practice

import app.astrid.engine.GeneratedExercise
import app.astrid.engine.LinguisticStatus
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticeAttemptContext
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
private const val TARGETED_REVIEW_FOCUS = "targeted-review"
private const val INTRODUCTION_POSITION = -1

internal fun app.astrid.engine.PracticePlan.toNewPracticeSession(ownerId: UUID) = NewPracticeSession(
    title,
    contentRevision,
    generatorRevision,
    evaluationRevision,
    prerequisiteSkillIds,
    introduction.map { IntroductionDto(it.title, it.body) },
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
        )
    },
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
            ),
        )
    }
    val plan = try {
        val planner = PracticeFollowUpPlanner(engine)
        when (request.planRevision) {
            FOLLOW_UP_REVISION -> planner.plan(
                request.planRevision,
                request.seed,
                request.targetKeys,
                sourceItems.map { it.exercise },
            )
            PRESENT_VERBS_FOLLOW_UP_REVISION -> {
                requirePresentVerbFollowUpSource()
                planner.planPresentVerbs(request.planRevision, request.seed, request.targetKeys, sourceItems)
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
        plan.introduction.map { IntroductionDto(it.title, it.body) },
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
            )
        },
        PracticeFollowUpLineage(sourceId, request.targetKeys),
        sourceExposureSnapshot(),
    )
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
                    )
                },
            )
        },
    )
    return PracticeSessionResponse(
        id.toString(), ownerSessionId.toString(), focusId, title, planRevision, seed,
        contentRevision, generatorRevision, evaluationRevision, sessionExpiresAt,
        if (cursor == items.size) "completed" else "active", cursor, items.size,
        prerequisiteSkillIds, introduction, introductionOpened,
        items.map { it.toDto() }, items.singleOrNull { it.position == cursor }?.exercise?.presentation,
        summary.toDto(includeRows = cursor == items.size),
        followUp?.let { PracticeFollowUpDto(it.sourcePracticeSessionId.toString(), it.selectedTargetKeys) },
        followUpPlanRevision(),
    )
}

private fun StoredPracticeSession.followUpPlanRevision(): String? =
    PRESENT_VERBS_FOLLOW_UP_REVISION.takeIf {
        (focusId == PRESENT_VERBS_FOCUS && planRevision == PRESENT_VERBS_REVISION) ||
            (focusId == TARGETED_REVIEW_FOCUS && planRevision == PRESENT_VERBS_FOLLOW_UP_REVISION)
    }

private fun StoredPracticeItem.toDto(): PracticeItemDto = PracticeItemDto(
    position,
    exercise.id.toString(),
    exercise.formatId,
    exercise.targetSkillId,
    exercise.presentation.lexicalTarget,
    lessonOpened,
    hintOpened,
    attempts.map { it.response.attemptId },
    attempts.size,
    attempts.firstOrNull()?.response,
    attempts.lastOrNull()?.response,
    attempts.firstOrNull()?.answer,
    attempts.lastOrNull()?.answer,
    attempts.firstOrNull()?.context,
    attempts.lastOrNull()?.context,
    assessmentMode,
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
                    items.flatMap { it.lessonExposureKeys }.distinct(),
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
                            item.attempts.any { it.response.targetSatisfaction == "met" },
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
