package app.astrid.server

import java.util.UUID

data class SessionRecord(val id: UUID, val expiresAt: Long)

data class StoredExercise(
    val id: UUID,
    val sessionId: UUID,
    val formatId: String,
    val targetSkillId: String?,
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
    data object PracticePositionConflict : AttemptWriteResult
}

data class CreatedAttempt(
    val response: AttemptResponse,
    val practiceContext: PracticeAttemptContextDto? = null,
)

data class PracticeAttemptSource(
    val focusId: String,
    val planRevision: String,
    val seed: Int,
    val position: Int,
    val introductionOpened: Boolean,
    val items: List<PracticeExposureState>,
)

data class PracticeExposureState(
    val position: Int,
    val answerExposureKeys: List<String>,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
    val lessonOpened: Boolean,
    val hintOpened: Boolean,
    val feedbackReleased: Boolean,
)

sealed interface PracticeCreateResult {
    data class Created(val session: StoredPracticeSession) : PracticeCreateResult
    data class Existing(val session: StoredPracticeSession) : PracticeCreateResult
    data object RequestConflict : PracticeCreateResult
}

sealed interface PracticeMutationResult {
    data class Success(val session: StoredPracticeSession) : PracticeMutationResult
    data object Missing : PracticeMutationResult
    data object Conflict : PracticeMutationResult
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
    ): AttemptWriteResult = writeAttemptWithPractice(sessionId, request, requestPayload) { exercise, retry, now, _ ->
        CreatedAttempt(create(exercise, retry, now))
    }
    suspend fun writeAttemptWithPractice(
        sessionId: UUID,
        request: AttemptRequest,
        requestPayload: String,
        create: suspend (StoredExercise, Int, Long, PracticeAttemptSource?) -> CreatedAttempt,
    ): AttemptWriteResult
    suspend fun findAttempt(sessionId: UUID, attemptId: UUID): AttemptResponse?
    suspend fun createPracticeSession(
        ownerSession: SessionRecord,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend () -> NewPracticeSession,
    ): PracticeCreateResult
    suspend fun findPracticeSession(ownerSessionId: UUID, practiceSessionId: UUID): StoredPracticeSession?
    suspend fun openPracticeSupport(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        request: PracticeSupportRequest,
    ): PracticeMutationResult
    suspend fun advancePracticeSession(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        position: Int,
    ): PracticeMutationResult
    suspend fun ready(): Boolean
    fun close() = Unit
}
