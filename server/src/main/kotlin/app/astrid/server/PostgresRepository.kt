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

    override suspend fun writeAttempt(
        sessionId: UUID,
        request: AttemptRequest,
        requestPayload: String,
        create: suspend (StoredExercise, Int, Long) -> AttemptResponse,
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
        val retry = Attempts.select(Attempts.attemptId).where {
            (Attempts.sessionId eq sessionId) and (Attempts.exerciseId eq exerciseId)
        }.count().toInt() + 1
        val now = System.currentTimeMillis()
        val response = create(exercise, retry, now)
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
        AttemptWriteResult.Created(response)
    }

    override suspend fun findAttempt(sessionId: UUID, attemptId: UUID): AttemptResponse? = suspendTransaction(database) {
        Attempts.select(Attempts.responsePayload).where {
            (Attempts.sessionId eq sessionId) and (Attempts.attemptId eq attemptId)
        }.toList().singleOrNull()?.let { json.decodeFromString(it[Attempts.responsePayload]) }
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
