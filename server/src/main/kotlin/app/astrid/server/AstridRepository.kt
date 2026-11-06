package app.astrid.server

import java.util.UUID

data class SessionRecord(val id: UUID, val expiresAt: Long)

data class StoredExercise(
    val id: UUID,
    val sessionId: UUID,
    val formatId: String,
    val targetSkillId: String,
    val seed: Int,
    val index: Int,
    val contentRevision: String,
    val generatorRevision: String,
    val presentation: ExerciseResponse,
    val reproductionFingerprint: String,
)

sealed interface AttemptWriteResult {
    data class Created(val response: AttemptResponse) : AttemptWriteResult
    data class Existing(val response: AttemptResponse) : AttemptWriteResult
    data object PayloadConflict : AttemptWriteResult
    data object ExerciseMissing : AttemptWriteResult
}

interface AstridRepository {
    suspend fun findSession(tokenHash: String, now: Long): SessionRecord?
    suspend fun createSession(tokenHash: String, expiresAt: Long): SessionRecord
    suspend fun insertExercise(exercise: StoredExercise)
    suspend fun findExercise(sessionId: UUID, exerciseId: UUID): StoredExercise?
    suspend fun writeAttempt(
        sessionId: UUID,
        request: AttemptRequest,
        requestPayload: String,
        create: suspend (StoredExercise, Int, Long) -> AttemptResponse,
    ): AttemptWriteResult
    suspend fun findAttempt(sessionId: UUID, attemptId: UUID): AttemptResponse?
    suspend fun ready(): Boolean
    fun close() = Unit
}
