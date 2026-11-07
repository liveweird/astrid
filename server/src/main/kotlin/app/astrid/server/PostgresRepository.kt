@file:Suppress("MagicNumber") // SQL widths and bounded pool time settings.

package app.astrid.server

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import java.time.Duration
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PostgresRepository private constructor(
    private val database: R2dbcDatabase,
    private val pool: ConnectionPool,
    private val json: Json,
) : AstridRepository {
    private object Sessions : Table("development_sessions") {
        val id = javaUUID("id")
        val tokenHash = char("token_hash", 64)
        val expiresAt = long("expires_at")
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(id)
    }

    private object Exercises : Table("exercises") {
        val id = javaUUID("id")
        val sessionId = javaUUID("session_id")
        val formatId = varchar("format_id", 32)
        val targetSkillId = varchar("target_skill_id", 32).nullable()
        val seed = integer("seed")
        val generationIndex = integer("generation_index")
        val contentRevision = varchar("content_revision", 64)
        val generatorRevision = varchar("generator_revision", 64)
        val presentationPayload = text("presentation_payload")
        val reproductionFingerprint = char("reproduction_fingerprint", 64)
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(id)
    }

    private object Attempts : Table("attempts") {
        val sessionId = javaUUID("session_id")
        val attemptId = javaUUID("attempt_id")
        val exerciseId = javaUUID("exercise_id")
        val requestPayload = text("request_payload")
        val rawAnswer = text("raw_answer")
        val responsePayload = text("response_payload")
        val contentRevision = varchar("content_revision", 64)
        val generatorRevision = varchar("generator_revision", 64)
        val evaluationRevision = varchar("evaluation_revision", 64)
        val retryNumber = integer("retry_number")
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(sessionId, attemptId)
    }

    private object PracticeSessions : Table("practice_sessions") {
        val ownerSessionId = javaUUID("owner_session_id")
        val practiceSessionId = javaUUID("practice_session_id")
        val requestPayload = text("request_payload")
        val planPayload = text("plan_payload")
        val focusId = varchar("focus_id", 32)
        val planRevision = varchar("plan_revision", 64)
        val seed = integer("seed")
        val sessionExpiresAt = long("session_expires_at")
        val cursor = integer("cursor")
        val introductionOpened = bool("introduction_opened")
        val createdAt = long("created_at")
        val updatedAt = long("updated_at")
        override val primaryKey = PrimaryKey(ownerSessionId, practiceSessionId)
    }

    private object PracticeItems : Table("practice_items") {
        val ownerSessionId = javaUUID("owner_session_id")
        val practiceSessionId = javaUUID("practice_session_id")
        val position = integer("position")
        val exerciseId = javaUUID("exercise_id")
        val lessonOpened = bool("lesson_opened")
        val hintOpened = bool("hint_opened")
        override val primaryKey = PrimaryKey(ownerSessionId, practiceSessionId, position)
    }

    private object PracticeAttemptContexts : Table("practice_attempt_context") {
        val ownerSessionId = javaUUID("owner_session_id")
        val attemptId = javaUUID("attempt_id")
        val practiceSessionId = javaUUID("practice_session_id")
        val position = integer("position")
        val contextPayload = text("context_payload")
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(ownerSessionId, attemptId)
    }

    override suspend fun findSession(tokenHash: String, now: Long): SessionRecord? = suspendTransaction(database) {
        Sessions.selectAll().where { (Sessions.tokenHash eq tokenHash) and (Sessions.expiresAt greater now) }
            .toList().singleOrNull()?.let { SessionRecord(it[Sessions.id], it[Sessions.expiresAt]) }
    }

    override suspend fun createSession(tokenHash: String, expiresAt: Long): SessionRecord = suspendTransaction(database) {
        val id = UUID.randomUUID()
        Sessions.insert {
            it[Sessions.id] = id
            it[Sessions.tokenHash] = tokenHash
            it[Sessions.expiresAt] = expiresAt
            it[createdAt] = System.currentTimeMillis()
        }
        SessionRecord(id, expiresAt)
    }

    override suspend fun insertExercise(exercise: StoredExercise) {
        suspendTransaction(database) {
            Exercises.insert {
                it[id] = exercise.id
                it[sessionId] = exercise.sessionId
                it[formatId] = exercise.formatId
                it[targetSkillId] = exercise.targetSkillId
                it[seed] = exercise.seed
                it[generationIndex] = exercise.index
                it[contentRevision] = exercise.contentRevision
                it[generatorRevision] = exercise.generatorRevision
                it[presentationPayload] = json.encodeToString(exercise.presentation)
                it[reproductionFingerprint] = exercise.reproductionFingerprint
                it[createdAt] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun findExercise(sessionId: UUID, exerciseId: UUID): StoredExercise? = suspendTransaction(database) {
        findExerciseInTransaction(sessionId, exerciseId)
    }

    override suspend fun writeAttemptWithPractice(
        sessionId: UUID,
        request: AttemptRequest,
        requestPayload: String,
        create: suspend (StoredExercise, Int, Long, PracticeAttemptSource?) -> CreatedAttempt,
    ): AttemptWriteResult = suspendTransaction(database) {
        val locked = Sessions.select(Sessions.id).where { Sessions.id eq sessionId }.forUpdate().toList().isNotEmpty()
        if (!locked) return@suspendTransaction AttemptWriteResult.ExerciseMissing
        val attemptId = UUID.fromString(request.attemptId)
        Attempts.selectAll().where { (Attempts.sessionId eq sessionId) and (Attempts.attemptId eq attemptId) }
            .toList().singleOrNull()?.let { existing ->
                return@suspendTransaction if (existing[Attempts.requestPayload] == requestPayload) {
                    AttemptWriteResult.Existing(json.decodeFromString(existing[Attempts.responsePayload]))
                } else {
                    AttemptWriteResult.PayloadConflict
                }
            }
        val exerciseId = UUID.fromString(request.exerciseId)
        val exercise = findExerciseInTransaction(sessionId, exerciseId)
            ?: return@suspendTransaction AttemptWriteResult.ExerciseMissing
        val practiceItem = PracticeItems.selectAll().where {
            (PracticeItems.ownerSessionId eq sessionId) and (PracticeItems.exerciseId eq exerciseId)
        }.toList().singleOrNull()
        val practiceSource = practiceItem?.let { item ->
            val practiceId = item[PracticeItems.practiceSessionId]
            val practice = PracticeSessions.selectAll().where {
                (PracticeSessions.ownerSessionId eq sessionId) and
                    (PracticeSessions.practiceSessionId eq practiceId)
            }.toList().single()
            if (item[PracticeItems.position] != practice[PracticeSessions.cursor]) {
                return@suspendTransaction AttemptWriteResult.PracticePositionConflict
            }
            practiceAttemptSource(sessionId, practiceId, practice)
        }
        val retry = Attempts.select(Attempts.attemptId).where {
            (Attempts.sessionId eq sessionId) and (Attempts.exerciseId eq exerciseId)
        }.count().toInt() + 1
        val now = System.currentTimeMillis()
        val created = create(exercise, retry, now, practiceSource)
        check((practiceItem == null) == (created.practiceContext == null)) { "Practice attempt context mismatch" }
        val response = created.response
        Attempts.insert {
            it[Attempts.sessionId] = sessionId
            it[Attempts.attemptId] = attemptId
            it[Attempts.exerciseId] = exerciseId
            it[Attempts.requestPayload] = requestPayload
            it[rawAnswer] = json.encodeToString(AttemptAnswerDto.serializer(), request.answer)
            it[responsePayload] = json.encodeToString(response)
            it[contentRevision] = exercise.contentRevision
            it[generatorRevision] = exercise.generatorRevision
            it[evaluationRevision] = response.evaluationRevision
            it[retryNumber] = retry
            it[createdAt] = now
        }
        if (practiceItem != null) {
            PracticeAttemptContexts.insert {
                it[ownerSessionId] = sessionId
                it[PracticeAttemptContexts.attemptId] = attemptId
                it[practiceSessionId] = practiceItem[PracticeItems.practiceSessionId]
                it[position] = practiceItem[PracticeItems.position]
                it[contextPayload] = json.encodeToString(checkNotNull(created.practiceContext))
                it[createdAt] = now
            }
        }
        AttemptWriteResult.Created(response)
    }

    override suspend fun findAttempt(sessionId: UUID, attemptId: UUID): AttemptResponse? = suspendTransaction(database) {
        Attempts.select(Attempts.responsePayload).where {
            (Attempts.sessionId eq sessionId) and (Attempts.attemptId eq attemptId)
        }.toList().singleOrNull()?.let { json.decodeFromString(it[Attempts.responsePayload]) }
    }

    override suspend fun createPracticeSession(
        ownerSession: SessionRecord,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend () -> NewPracticeSession,
    ): PracticeCreateResult = suspendTransaction(database) {
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
        val now = System.currentTimeMillis()
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
            insertExerciseInTransaction(item.exercise, now)
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
        ownerSession: SessionRecord,
        sourcePracticeSessionId: UUID,
        request: PracticeFollowUpRequest,
        requestPayload: String,
        create: suspend (StoredPracticeSession) -> NewPracticeSession,
    ): PracticeCreateResult = suspendTransaction(database) {
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
    ): StoredPracticeSession? = suspendTransaction(database) {
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
    ): PracticeMutationResult = suspendTransaction(database) {
        if (!lockSession(ownerSessionId)) return@suspendTransaction PracticeMutationResult.Missing
        val practice = findPracticeRow(ownerSessionId, practiceSessionId)
            ?: return@suspendTransaction PracticeMutationResult.Missing
        val now = System.currentTimeMillis()
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
    ): PracticeMutationResult = suspendTransaction(database) {
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
                    it[updatedAt] = System.currentTimeMillis()
                }
                PracticeMutationResult.Success(loadPracticeSession(ownerSessionId, practiceSessionId))
            }
        }
    }

    private suspend fun lockSession(sessionId: UUID): Boolean =
        Sessions.select(Sessions.id).where { Sessions.id eq sessionId }.forUpdate().toList().isNotEmpty()

    private suspend fun findPracticeRow(ownerSessionId: UUID, practiceSessionId: UUID) =
        PracticeSessions.selectAll().where {
            (PracticeSessions.ownerSessionId eq ownerSessionId) and
                (PracticeSessions.practiceSessionId eq practiceSessionId)
        }.toList().singleOrNull()

    private suspend fun practiceAttemptSource(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        practice: org.jetbrains.exposed.v1.core.ResultRow,
    ): PracticeAttemptSource {
        val items = PracticeItems.selectAll().where {
            (PracticeItems.ownerSessionId eq ownerSessionId) and
                (PracticeItems.practiceSessionId eq practiceSessionId)
        }.toList().sortedBy { it[PracticeItems.position] }
        val feedbackPositions = PracticeAttemptContexts.select(PracticeAttemptContexts.position).where {
            (PracticeAttemptContexts.ownerSessionId eq ownerSessionId) and
                (PracticeAttemptContexts.practiceSessionId eq practiceSessionId)
        }.toList().map { it[PracticeAttemptContexts.position] }.toSet()
        val targetMetPositions = PracticeAttemptContexts.selectAll().where {
            (PracticeAttemptContexts.ownerSessionId eq ownerSessionId) and
                (PracticeAttemptContexts.practiceSessionId eq practiceSessionId)
        }.toList().mapNotNull { context ->
            val response = Attempts.select(Attempts.responsePayload).where {
                (Attempts.sessionId eq ownerSessionId) and
                    (Attempts.attemptId eq context[PracticeAttemptContexts.attemptId])
            }.toList().singleOrNull()?.let { json.decodeFromString<AttemptResponse>(it[Attempts.responsePayload]) }
            context[PracticeAttemptContexts.position].takeIf { response?.targetSatisfaction == "met" }
        }.toSet()
        val payload: PracticePlanPayload = json.decodeFromString(practice[PracticeSessions.planPayload])
        val metadata = payload.items.associateBy { it.position }
        return PracticeAttemptSource(
            practice[PracticeSessions.focusId],
            practice[PracticeSessions.planRevision],
            practice[PracticeSessions.seed],
            practice[PracticeSessions.cursor],
            practice[PracticeSessions.introductionOpened],
            items.map {
                PracticeExposureState(
                    it[PracticeItems.position],
                    metadata.getValue(it[PracticeItems.position]).answerExposureKeys,
                    metadata.getValue(it[PracticeItems.position]).promptExposureKeys,
                    metadata.getValue(it[PracticeItems.position]).lessonExposureKeys,
                    metadata.getValue(it[PracticeItems.position]).hintExposureKeys,
                    frozenFeedbackExposureKeys(
                        metadata.getValue(it[PracticeItems.position]).feedbackExposureKeys,
                        it[PracticeItems.position] in targetMetPositions,
                    ),
                    it[PracticeItems.lessonOpened],
                    it[PracticeItems.hintOpened],
                    it[PracticeItems.position] in feedbackPositions,
                )
            },
            payload.sourceExposure,
        )
    }

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
            val exercise = checkNotNull(findExerciseInTransaction(ownerSessionId, item[PracticeItems.exerciseId]))
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
        )
    }

    private suspend fun insertPracticeSession(
        ownerSession: SessionRecord,
        practiceId: UUID,
        requestPayload: String,
        focus: String,
        revision: String,
        seedValue: Int,
        plan: NewPracticeSession,
    ) {
        val now = System.currentTimeMillis()
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
            insertExerciseInTransaction(item.exercise, now)
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

    private suspend fun insertExerciseInTransaction(exercise: StoredExercise, now: Long) {
        Exercises.insert {
            it[id] = exercise.id
            it[sessionId] = exercise.sessionId
            it[formatId] = exercise.formatId
            it[targetSkillId] = exercise.targetSkillId
            it[seed] = exercise.seed
            it[generationIndex] = exercise.index
            it[contentRevision] = exercise.contentRevision
            it[generatorRevision] = exercise.generatorRevision
            it[presentationPayload] = json.encodeToString(exercise.presentation)
            it[reproductionFingerprint] = exercise.reproductionFingerprint
            it[createdAt] = now
        }
    }

    @Suppress("TooGenericExceptionCaught") // Any database failure means not ready; coroutine cancellation still propagates.
    override suspend fun ready(): Boolean = try {
        suspendTransaction(database) { Sessions.select(Sessions.id).limit(1).toList() }
        true
    } catch (cause: CancellationException) {
        throw cause
    } catch (_: Exception) {
        false
    }

    override fun close() = pool.dispose()

    private suspend fun findExerciseInTransaction(sessionId: UUID, exerciseId: UUID): StoredExercise? =
        Exercises.selectAll().where { (Exercises.sessionId eq sessionId) and (Exercises.id eq exerciseId) }
            .toList().singleOrNull()?.let {
                StoredExercise(
                    id = it[Exercises.id],
                    sessionId = it[Exercises.sessionId],
                    formatId = it[Exercises.formatId],
                    targetSkillId = it[Exercises.targetSkillId],
                    seed = it[Exercises.seed],
                    index = it[Exercises.generationIndex],
                    contentRevision = it[Exercises.contentRevision],
                    generatorRevision = it[Exercises.generatorRevision],
                    presentation = json.decodeFromString(it[Exercises.presentationPayload]),
                    reproductionFingerprint = it[Exercises.reproductionFingerprint],
                )
            }

    companion object {
        private const val MAX_PRACTICE_ITEMS = 8
        private const val TARGETED_REVIEW_FOCUS = "targeted-review"
        fun connect(config: ServerConfig, json: Json): PostgresRepository {
            val options = ConnectionFactoryOptions.parse(config.r2dbcUrl).mutate()
                .option(ConnectionFactoryOptions.USER, config.postgresUser)
                .option(ConnectionFactoryOptions.PASSWORD, config.postgresPassword)
                .build()
            val pool = ConnectionPool(
                ConnectionPoolConfiguration.builder(ConnectionFactories.get(options))
                    .maxSize(config.poolMaxSize)
                    .initialSize(1)
                    .maxAcquireTime(Duration.ofSeconds(10))
                    .maxIdleTime(Duration.ofMinutes(10))
                    .build(),
            )
            val dbConfig = R2dbcDatabaseConfig.Builder().apply {
                connectionFactoryOptions = options
                defaultMaxAttempts = 1
            }
            return PostgresRepository(R2dbcDatabase.connect(pool, dbConfig), pool, json)
        }
    }
}
