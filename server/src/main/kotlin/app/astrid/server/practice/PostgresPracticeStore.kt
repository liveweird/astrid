package app.astrid.server.practice

import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.Attempts
import app.astrid.server.exercises.findExerciseInTransaction
import app.astrid.server.exercises.insertExerciseInTransaction
import app.astrid.server.infra.db.Database
import app.astrid.server.session.lockSession
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * Practice runs. Every mutation locks the owning session row first (the same lock the attempt write takes), and a run's
 * exercises are inserted in the run's own transaction through `exercises/`' [insertExerciseInTransaction].
 */
class PostgresPracticeStore(
    private val database: Database,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
) : PracticeStore {
    override suspend fun createPracticeSession(
        ownerSession: SessionPrincipal,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend () -> NewPracticeSession,
    ): PracticeCreateResult = suspendTransaction(database.r2dbc) {
        lockSession(ownerSession.id)
        val practiceId = UUID.fromString(request.practiceSessionId)
        val existing = PracticeSessions.selectAll().where {
            (PracticeSessions.ownerSessionId eq ownerSession.id) and
                (PracticeSessions.practiceSessionId eq practiceId)
        }.toList().singleOrNull()
        if (existing != null) {
            return@suspendTransaction if (existing[PracticeSessions.requestPayload] == requestPayload) {
                PracticeCreateResult.Existing(loadPracticeSession(ownerSession.id, practiceId))
            } else {
                PracticeCreateResult.RequestConflict
            }
        }
        val plan = create()
        require(plan.items.map { it.position } == plan.items.indices.toList()) { "Practice positions must be contiguous" }
        val now = clock()
        PracticeSessions.insert {
            it[ownerSessionId] = ownerSession.id
            it[practiceSessionId] = practiceId
            it[PracticeSessions.requestPayload] = requestPayload
            it[planPayload] = json.encodeToString(plan.toPayload())
            it[focusId] = request.focusId
            it[planRevision] = request.planRevision
            it[seed] = request.seed
            it[sessionExpiresAt] = ownerSession.expiresAt
            it[cursor] = 0
            it[introductionOpened] = false
            it[createdAt] = now
            it[updatedAt] = now
        }
        plan.items.forEach { item ->
            insertExerciseInTransaction(item.exercise, now, json)
            PracticeItems.insert {
                it[ownerSessionId] = ownerSession.id
                it[practiceSessionId] = practiceId
                it[position] = item.position
                it[exerciseId] = item.exercise.id
                it[lessonOpened] = false
                it[hintOpened] = false
            }
        }
        PracticeCreateResult.Created(loadPracticeSession(ownerSession.id, practiceId))
    }

    override suspend fun createPracticeFollowUp(
        ownerSession: SessionPrincipal,
        sourcePracticeSessionId: UUID,
        request: PracticeFollowUpRequest,
        requestPayload: String,
        create: suspend (StoredPracticeSession) -> NewPracticeSession,
    ): PracticeCreateResult = suspendTransaction(database.r2dbc) {
        lockSession(ownerSession.id)
        val practiceId = UUID.fromString(request.practiceSessionId)
        val existing = findPracticeRow(ownerSession.id, practiceId)
        if (existing != null) {
            return@suspendTransaction if (existing[PracticeSessions.requestPayload] == requestPayload) {
                PracticeCreateResult.Existing(loadPracticeSession(ownerSession.id, practiceId))
            } else {
                PracticeCreateResult.RequestConflict
            }
        }
        if (practiceId == sourcePracticeSessionId) return@suspendTransaction PracticeCreateResult.RequestConflict
        val sourceRow = findPracticeRow(ownerSession.id, sourcePracticeSessionId)
            ?: return@suspendTransaction PracticeCreateResult.SourceMissing
        val source = loadPracticeSession(ownerSession.id, sourcePracticeSessionId)
        if (source.cursor != source.items.size) return@suspendTransaction PracticeCreateResult.SourceActive
        val plan = create(source)
        require(plan.items.isNotEmpty() && plan.items.size <= MAX_PRACTICE_ITEMS) { "Invalid follow-up item count" }
        require(plan.items.map { it.position } == plan.items.indices.toList()) { "Practice positions must be contiguous" }
        check(sourceRow[PracticeSessions.cursor] == source.items.size)
        insertPracticeSession(
            ownerSession,
            practiceId,
            requestPayload,
            TARGETED_REVIEW_FOCUS,
            request.planRevision,
            request.seed,
            plan,
        )
        PracticeCreateResult.Created(loadPracticeSession(ownerSession.id, practiceId))
    }

    override suspend fun findPracticeSession(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
    ): StoredPracticeSession? = suspendTransaction(database.r2dbc) {
        if (!lockSession(ownerSessionId)) return@suspendTransaction null
        PracticeSessions.select(PracticeSessions.practiceSessionId).where {
            (PracticeSessions.ownerSessionId eq ownerSessionId) and
                (PracticeSessions.practiceSessionId eq practiceSessionId)
        }.toList().singleOrNull()?.let { loadPracticeSession(ownerSessionId, practiceSessionId) }
    }

    override suspend fun openPracticeSupport(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        request: PracticeSupportRequest,
    ): PracticeMutationResult = suspendTransaction(database.r2dbc) {
        if (!lockSession(ownerSessionId)) return@suspendTransaction PracticeMutationResult.Missing
        val practice = findPracticeRow(ownerSessionId, practiceSessionId)
            ?: return@suspendTransaction PracticeMutationResult.Missing
        val now = clock()
        when (request.kind) {
            "introduction" -> PracticeSessions.update({
                (PracticeSessions.ownerSessionId eq ownerSessionId) and
                    (PracticeSessions.practiceSessionId eq practiceSessionId)
            }) {
                it[introductionOpened] = true
                it[updatedAt] = now
            }
            "lesson", "hint" -> {
                val position = request.position ?: return@suspendTransaction PracticeMutationResult.Conflict
                if (position > practice[PracticeSessions.cursor]) return@suspendTransaction PracticeMutationResult.Conflict
                val changed = PracticeItems.update({
                    (PracticeItems.ownerSessionId eq ownerSessionId) and
                        (PracticeItems.practiceSessionId eq practiceSessionId) and
                        (PracticeItems.position eq position)
                }) {
                    if (request.kind == "lesson") it[lessonOpened] = true else it[hintOpened] = true
                }
                if (changed == 0) return@suspendTransaction PracticeMutationResult.Conflict
                PracticeSessions.update({
                    (PracticeSessions.ownerSessionId eq ownerSessionId) and
                        (PracticeSessions.practiceSessionId eq practiceSessionId)
                }) { it[updatedAt] = now }
            }
            else -> return@suspendTransaction PracticeMutationResult.Conflict
        }
        PracticeMutationResult.Success(loadPracticeSession(ownerSessionId, practiceSessionId))
    }

    override suspend fun advancePracticeSession(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        position: Int,
    ): PracticeMutationResult = suspendTransaction(database.r2dbc) {
        if (!lockSession(ownerSessionId)) return@suspendTransaction PracticeMutationResult.Missing
        val practice = findPracticeRow(ownerSessionId, practiceSessionId)
            ?: return@suspendTransaction PracticeMutationResult.Missing
        val cursor = practice[PracticeSessions.cursor]
        val itemCount = json.decodeFromString<PracticePlanPayload>(practice[PracticeSessions.planPayload]).items.size
        when {
            position !in 0 until itemCount -> PracticeMutationResult.Conflict
            position < cursor -> PracticeMutationResult.Success(loadPracticeSession(ownerSessionId, practiceSessionId))
            position > cursor -> PracticeMutationResult.Conflict
            else -> {
                val attempted = PracticeAttemptContexts.select(PracticeAttemptContexts.attemptId).where {
                    (PracticeAttemptContexts.ownerSessionId eq ownerSessionId) and
                        (PracticeAttemptContexts.practiceSessionId eq practiceSessionId) and
                        (PracticeAttemptContexts.position eq position)
                }.limit(1).toList().isNotEmpty()
                if (!attempted) return@suspendTransaction PracticeMutationResult.Conflict
                PracticeSessions.update({
                    (PracticeSessions.ownerSessionId eq ownerSessionId) and
                        (PracticeSessions.practiceSessionId eq practiceSessionId)
                }) {
                    it[PracticeSessions.cursor] = cursor + 1
                    it[updatedAt] = clock()
                }
                PracticeMutationResult.Success(loadPracticeSession(ownerSessionId, practiceSessionId))
            }
        }
    }

    private suspend fun findPracticeRow(ownerSessionId: UUID, practiceSessionId: UUID) =
        PracticeSessions.selectAll().where {
            (PracticeSessions.ownerSessionId eq ownerSessionId) and
                (PracticeSessions.practiceSessionId eq practiceSessionId)
        }.toList().singleOrNull()

    private suspend fun loadPracticeSession(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
    ): StoredPracticeSession {
        val practice = checkNotNull(findPracticeRow(ownerSessionId, practiceSessionId))
        val payload: PracticePlanPayload = json.decodeFromString(practice[PracticeSessions.planPayload])
        val payloadByPosition = payload.items.associateBy { it.position }
        val items = PracticeItems.selectAll().where {
            (PracticeItems.ownerSessionId eq ownerSessionId) and
                (PracticeItems.practiceSessionId eq practiceSessionId)
        }.toList().sortedBy { it[PracticeItems.position] }.map { item ->
            val position = item[PracticeItems.position]
            val itemPayload = payloadByPosition.getValue(position)
            val exercise = checkNotNull(findExerciseInTransaction(ownerSessionId, item[PracticeItems.exerciseId], json))
            val contexts = PracticeAttemptContexts.selectAll().where {
                (PracticeAttemptContexts.ownerSessionId eq ownerSessionId) and
                    (PracticeAttemptContexts.practiceSessionId eq practiceSessionId) and
                    (PracticeAttemptContexts.position eq position)
            }.toList()
            val attempts = contexts.map { context ->
                val attempt = Attempts.select(Attempts.responsePayload, Attempts.requestPayload).where {
                    (Attempts.sessionId eq ownerSessionId) and
                        (Attempts.attemptId eq context[PracticeAttemptContexts.attemptId])
                }.toList().single()
                StoredPracticeAttempt(
                    json.decodeFromString(attempt[Attempts.responsePayload]),
                    json.decodeFromString(context[PracticeAttemptContexts.contextPayload]),
                    json.decodeFromString<AttemptRequest>(attempt[Attempts.requestPayload]).answer,
                )
            }.sortedBy { it.response.retryNumber }
            StoredPracticeItem(
                position,
                exercise,
                itemPayload.targetLabel,
                itemPayload.answerExposureKeys,
                itemPayload.promptExposureKeys,
                itemPayload.lessonExposureKeys,
                itemPayload.hintExposureKeys,
                itemPayload.feedbackExposureKeys,
                item[PracticeItems.lessonOpened],
                item[PracticeItems.hintOpened],
                attempts,
                itemPayload.assessmentMode,
                itemPayload.assessments,
            )
        }
        return StoredPracticeSession(
            practiceSessionId,
            ownerSessionId,
            practice[PracticeSessions.sessionExpiresAt],
            practice[PracticeSessions.focusId],
            payload.title,
            practice[PracticeSessions.planRevision],
            practice[PracticeSessions.seed],
            payload.contentRevision,
            payload.generatorRevision,
            payload.evaluationRevision,
            practice[PracticeSessions.cursor],
            practice[PracticeSessions.introductionOpened],
            payload.prerequisiteSkillIds,
            payload.introduction,
            items,
            payload.followUp?.let {
                PracticeFollowUpLineage(UUID.fromString(it.sourcePracticeSessionId), it.selectedTargetKeys)
            },
            payload.sourceExposure,
            payload.introductionExposureKeys,
        )
    }

    private suspend fun insertPracticeSession(
        ownerSession: SessionPrincipal,
        practiceId: UUID,
        requestPayload: String,
        focus: String,
        revision: String,
        seedValue: Int,
        plan: NewPracticeSession,
    ) {
        val now = clock()
        PracticeSessions.insert {
            it[ownerSessionId] = ownerSession.id
            it[practiceSessionId] = practiceId
            it[PracticeSessions.requestPayload] = requestPayload
            it[planPayload] = json.encodeToString(plan.toPayload())
            it[focusId] = focus
            it[planRevision] = revision
            it[seed] = seedValue
            it[sessionExpiresAt] = ownerSession.expiresAt
            it[cursor] = 0
            it[introductionOpened] = false
            it[createdAt] = now
            it[updatedAt] = now
        }
        plan.items.forEach { item ->
            insertExerciseInTransaction(item.exercise, now, json)
            PracticeItems.insert {
                it[ownerSessionId] = ownerSession.id
                it[practiceSessionId] = practiceId
                it[position] = item.position
                it[exerciseId] = item.exercise.id
                it[lessonOpened] = false
                it[hintOpened] = false
            }
        }
    }

    private companion object {
        const val MAX_PRACTICE_ITEMS = 8
        const val TARGETED_REVIEW_FOCUS = "targeted-review"
    }
}
