package app.astrid.server

import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryAstridRepository(private val clock: () -> Long = System::currentTimeMillis) : AstridRepository {
    private val mutex = Mutex()
    private val sessions = mutableMapOf<String, SessionRecord>()
    private val exercises = mutableMapOf<Pair<UUID, UUID>, StoredExercise>()
    private val attempts = mutableMapOf<Pair<UUID, UUID>, Pair<String, AttemptResponse>>()

    override suspend fun findSession(tokenHash: String, now: Long) = mutex.withLock {
        sessions[tokenHash]?.takeIf { it.expiresAt > now }
    }

    override suspend fun createSession(tokenHash: String, expiresAt: Long) = mutex.withLock {
        SessionRecord(UUID.randomUUID(), expiresAt).also { sessions[tokenHash] = it }
    }

    override suspend fun insertExercise(exercise: StoredExercise) = mutex.withLock {
        exercises[exercise.sessionId to exercise.id] = exercise
    }

    override suspend fun findExercise(sessionId: UUID, exerciseId: UUID) = mutex.withLock {
        exercises[sessionId to exerciseId]
    }

    override suspend fun writeAttempt(
        sessionId: UUID,
        request: AttemptRequest,
        requestPayload: String,
        create: suspend (StoredExercise, Int, Long) -> AttemptResponse,
    ): AttemptWriteResult = mutex.withLock {
        val attemptId = UUID.fromString(request.attemptId)
        val key = sessionId to attemptId
        attempts[key]?.let { (payload, response) ->
            return@withLock if (payload == requestPayload) AttemptWriteResult.Existing(response)
            else AttemptWriteResult.PayloadConflict
        }
        val exerciseId = UUID.fromString(request.exerciseId)
        val exercise = exercises[sessionId to exerciseId] ?: return@withLock AttemptWriteResult.ExerciseMissing
        val retry = attempts.values.count { (_, response) -> response.exerciseId == request.exerciseId } + 1
        val response = create(exercise, retry, clock())
        attempts[key] = requestPayload to response
        AttemptWriteResult.Created(response)
    }

    override suspend fun findAttempt(sessionId: UUID, attemptId: UUID) = mutex.withLock {
        attempts[sessionId to attemptId]?.second
    }

    override suspend fun ready() = true

    suspend fun tamperFingerprint(exerciseId: UUID) = mutex.withLock {
        val entry = exercises.entries.single { it.key.second == exerciseId }
        exercises[entry.key] = entry.value.copy(reproductionFingerprint = "0".repeat(64))
    }

    suspend fun tamperRevision(exerciseId: UUID) = mutex.withLock {
        val entry = exercises.entries.single { it.key.second == exerciseId }
        exercises[entry.key] = entry.value.copy(
            contentRevision = "retired-content",
            reproductionFingerprint = "0".repeat(64),
        )
    }
}
