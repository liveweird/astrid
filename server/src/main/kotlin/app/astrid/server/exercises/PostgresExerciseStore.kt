package app.astrid.server.exercises

import app.astrid.server.infra.db.Database
import app.astrid.server.session.lockSession
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class PostgresExerciseStore(
    private val database: Database,
    private val json: Json,
    private val practiceHook: AttemptPracticeHook,
    private val clock: () -> Long = System::currentTimeMillis,
) : ExerciseStore {
    override suspend fun insertExercise(exercise: StoredExercise) {
        suspendTransaction(database.r2dbc) {
            insertExerciseInTransaction(exercise, clock(), json)
        }
    }

    override suspend fun findExercise(sessionId: UUID, exerciseId: UUID): StoredExercise? = suspendTransaction(database.r2dbc) {
        findExerciseInTransaction(sessionId, exerciseId, json)
    }

    override suspend fun writeAttemptWithPractice(
        sessionId: UUID,
        request: AttemptRequest,
        requestPayload: String,
        create: suspend (StoredExercise, Int, Long, PracticeAttemptSource?) -> CreatedAttempt,
    ): AttemptWriteResult = suspendTransaction(database.r2dbc) {
        if (!lockSession(sessionId)) return@suspendTransaction AttemptWriteResult.ExerciseMissing
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
        val exercise = findExerciseInTransaction(sessionId, exerciseId, json)
            ?: return@suspendTransaction AttemptWriteResult.ExerciseMissing
        val practice = when (val binding = practiceHook.bind(sessionId, exerciseId)) {
            PracticeBinding.Unbound -> null
            PracticeBinding.PositionConflict -> return@suspendTransaction AttemptWriteResult.PracticePositionConflict
            PracticeBinding.IntroductionRequired -> return@suspendTransaction AttemptWriteResult.PracticeIntroductionRequired
            is PracticeBinding.Bound -> binding
        }
        val retry = Attempts.select(Attempts.attemptId).where {
            (Attempts.sessionId eq sessionId) and (Attempts.exerciseId eq exerciseId)
        }.count().toInt() + 1
        val now = clock()
        val created = create(exercise, retry, now, practice?.source)
        check((practice == null) == (created.practiceContext == null)) { "Practice attempt context mismatch" }
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
        if (practice != null) practiceHook.record(practice, attemptId, checkNotNull(created.practiceContext), now)
        AttemptWriteResult.Created(response)
    }

    override suspend fun findAttempt(sessionId: UUID, attemptId: UUID): AttemptResponse? = suspendTransaction(database.r2dbc) {
        Attempts.select(Attempts.responsePayload).where {
            (Attempts.sessionId eq sessionId) and (Attempts.attemptId eq attemptId)
        }.toList().singleOrNull()?.let { json.decodeFromString(it[Attempts.responsePayload]) }
    }
}

/** Inserts [exercise] in the CALLER's transaction; `practice/` uses it so a run and its exercises commit together. */
internal suspend fun insertExerciseInTransaction(exercise: StoredExercise, now: Long, json: Json) {
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

/** Reads one owned exercise in the CALLER's transaction. */
internal suspend fun findExerciseInTransaction(sessionId: UUID, exerciseId: UUID, json: Json): StoredExercise? =
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
