package app.astrid.server.exercises

import java.util.UUID

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

interface ExerciseStore {
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
}

/** Whether the attempted exercise belongs to a practice run, as seen by [AttemptPracticeHook.bind]. */
sealed interface PracticeBinding {
    /** A free-standing playground exercise. */
    data object Unbound : PracticeBinding

    /** A practice item that is no longer (or not yet) the run's current position. */
    data object PositionConflict : PracticeBinding

    /** The run's current item, with the exposure state the attempt context is calculated from. */
    data class Bound(
        val ownerSessionId: UUID,
        val practiceSessionId: UUID,
        val position: Int,
        val source: PracticeAttemptSource,
    ) : PracticeBinding
}

/**
 * The seam through which the attempt write reaches practice state without `exercises/` depending on `practice/`.
 *
 * Both calls run INSIDE the attempt write's transaction (the same coroutine, so the same `suspendTransaction`, after the
 * session row lock): [bind] after the exercise is found and before the retry count, [record] right after the attempt row is
 * inserted. An implementation must never open a transaction of its own, or the attempt and its practice context could
 * commit separately.
 */
interface AttemptPracticeHook {
    suspend fun bind(sessionId: UUID, exerciseId: UUID): PracticeBinding
    suspend fun record(binding: PracticeBinding.Bound, attemptId: UUID, context: PracticeAttemptContextDto, now: Long)
}
