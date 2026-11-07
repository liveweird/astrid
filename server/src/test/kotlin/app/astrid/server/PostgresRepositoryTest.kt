@file:Suppress("MaxLineLength") // The pinned container digest is intentionally kept intact.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.AfterClass
import org.flywaydb.core.Flyway
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresRepositoryTest {
    @Test
    fun `V2 preserves V1 grammar evidence and stores nullable lexical evidence with exact replay`() = runBlocking {
        val databaseName = "astrid_migration_${UUID.randomUUID().toString().replace("-", "")}"
        postgres.createDatabase(databaseName)
        val config = postgres.config(databaseName)
        Flyway.configure()
            .dataSource(config.jdbcUrl, config.postgresUser, config.postgresPassword)
            .locations("classpath:db/migration")
            .target("1")
            .load()
            .migrate()

        val engine = PilotEngine()
        val sessionId = UUID.randomUUID()
        val grammarId = UUID.randomUUID()
        val grammar = engine.generate(GenerationRequest("E001", "G004", 19, 3, "pilot-1", "generator-1"))
        DriverManager.getConnection(config.jdbcUrl, config.postgresUser, config.postgresPassword).use { connection ->
            connection.prepareStatement(
                "INSERT INTO development_sessions (id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?)",
            ).use { statement ->
                statement.setObject(1, sessionId)
                statement.setString(2, "a".repeat(64))
                statement.setLong(3, Long.MAX_VALUE)
                statement.setLong(4, 1)
                assertEquals(1, statement.executeUpdate())
            }
            connection.prepareStatement(
                """INSERT INTO exercises
                    (id, session_id, format_id, target_skill_id, seed, generation_index, content_revision,
                     generator_revision, presentation_payload, reproduction_fingerprint, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent(),
            ).use { statement ->
                statement.setObject(1, grammarId)
                statement.setObject(2, sessionId)
                statement.setString(3, grammar.formatId)
                statement.setString(4, grammar.targetSkillId)
                statement.setInt(5, grammar.reproduction.seed)
                statement.setInt(6, grammar.reproduction.index)
                statement.setString(7, grammar.contentRevision)
                statement.setString(8, grammar.generatorRevision)
                statement.setString(9, wireJson.encodeToString(ExerciseResponse.serializer(), grammar.toResponse(grammarId.toString())))
                statement.setString(10, grammar.reproductionFingerprint())
                statement.setLong(11, 1)
                assertEquals(1, statement.executeUpdate())
            }
        }

        migrate(config)
        val repository = PostgresRepository.connect(config, wireJson)
        try {
            val restoredGrammar = assertNotNull(repository.findExercise(sessionId, grammarId))
            assertEquals("G004", restoredGrammar.targetSkillId)
            assertEquals(grammar.reproductionFingerprint(), engine.generate(restoredGrammar.generationRequest()).reproductionFingerprint())

            val lexicalId = UUID.randomUUID()
            val lexical = engine.generate(GenerationRequest("E008", null, 47, 1, "pilot-3", "generator-3"))
            val storedLexical = StoredExercise(
                lexicalId,
                sessionId,
                lexical.formatId,
                lexical.targetSkillId,
                lexical.reproduction.seed,
                lexical.reproduction.index,
                lexical.contentRevision,
                lexical.generatorRevision,
                lexical.toResponse(lexicalId.toString()),
                lexical.reproductionFingerprint(),
            )
            repository.insertExercise(storedLexical)
            val restoredLexical = assertNotNull(repository.findExercise(sessionId, lexicalId))
            assertNull(restoredLexical.targetSkillId)
            assertNotNull(restoredLexical.presentation.lexicalTarget)
            assertEquals(storedLexical.presentation, restoredLexical.presentation)
            assertEquals(lexical.reproductionFingerprint(), engine.generate(restoredLexical.generationRequest()).reproductionFingerprint())

            val request = AttemptRequest(
                UUID.randomUUID().toString(),
                lexicalId.toString(),
                AttemptAnswerDto.Text("avis"),
                false,
            )
            val payload = wireJson.encodeToString(AttemptRequest.serializer(), request)
            val callbacks = AtomicInteger()
            val created = repository.writeAttempt(sessionId, request, payload) { exercise, retry, createdAt ->
                callbacks.incrementAndGet()
                AttemptResponse(
                    request.attemptId,
                    exercise.id.toString(),
                    "met",
                    "acceptable",
                    emptyList(),
                    "Correct",
                    "",
                    "evaluation-3",
                    false,
                    retry,
                    createdAt,
                    "danish-headword",
                    "met",
                )
            }
            val createdResponse = assertIs<AttemptWriteResult.Created>(created).response
            val replayed = repository.writeAttempt(sessionId, request, payload) { _, _, _ ->
                callbacks.incrementAndGet()
                error("saved attempt must replay before regeneration")
            }
            assertEquals(1, callbacks.get())
            assertEquals(createdResponse, assertIs<AttemptWriteResult.Existing>(replayed).response)
            assertEquals("danish-headword", repository.findAttempt(sessionId, UUID.fromString(request.attemptId))?.linguisticScope)
        } finally {
            repository.close()
        }
    }

    @Test
    fun `readiness reports database state without swallowing caller cancellation`() = withRepositories { first, second ->
        assertEquals(true, first.ready())

        first.close()
        assertEquals(false, first.ready())
        assertEquals(true, second.ready())

        val observedCall = CompletableDeferred<Result<Boolean>>()
        val cancelledChild = launch(start = CoroutineStart.UNDISPATCHED) {
            cancel(CancellationException("readiness caller left"))
            observedCall.complete(runCatching { second.ready() })
        }
        val result = withTimeout(TIMEOUT_MILLIS) { observedCall.await() }
        assertIs<CancellationException>(result.exceptionOrNull())
        cancelledChild.join()
    }

    @Test
    fun `two pools serialize identical and conflicting attempt identities`() = withRepositories { first, second ->
        val same = fixture(first)
        val callbackCount = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val sameWrites = listOf(first, second).map { repository ->
            async {
                gate.await()
                repository.writeAttempt(same.session.id, same.request, same.payload) { exercise, retry, createdAt ->
                    callbackCount.incrementAndGet()
                    attemptResponse(same.request.attemptId, exercise, retry, createdAt)
                }
            }
        }
        gate.complete(Unit)
        val sameResults = withTimeout(TIMEOUT_MILLIS) { sameWrites.awaitAll() }
        assertEquals(1, callbackCount.get())
        assertEquals(1, sameResults.count { it is AttemptWriteResult.Created })
        assertEquals(1, sameResults.count { it is AttemptWriteResult.Existing })
        val sameResponses = sameResults.map {
            when (it) {
                is AttemptWriteResult.Created -> it.response
                is AttemptWriteResult.Existing -> it.response
                else -> error("unexpected result $it")
            }
        }
        assertEquals(sameResponses.first(), sameResponses.last())

        val conflict = fixture(first)
        val changed = conflict.request.copy(answer = AttemptAnswerDto.Text("læse"))
        val conflictCallbackCount = AtomicInteger()
        val conflictGate = CompletableDeferred<Unit>()
        val competing = listOf(first to conflict.request, second to changed).map { (repository, request) ->
            async {
                conflictGate.await()
                repository.writeAttempt(
                    conflict.session.id,
                    request,
                    wireJson.encodeToString(AttemptRequest.serializer(), request),
                ) { exercise, retry, createdAt ->
                    conflictCallbackCount.incrementAndGet()
                    attemptResponse(request.attemptId, exercise, retry, createdAt)
                }
            }
        }
        conflictGate.complete(Unit)
        val conflictResults = withTimeout(TIMEOUT_MILLIS) { competing.awaitAll() }
        assertEquals(1, conflictCallbackCount.get())
        assertEquals(1, conflictResults.count { it is AttemptWriteResult.Created })
        assertEquals(1, conflictResults.count { it is AttemptWriteResult.PayloadConflict })
    }

    @Test
    fun `two pools assign consecutive retry numbers to distinct attempt ids`() = withRepositories { first, second ->
        val base = fixture(first)
        val requests = listOf(base.request, base.request.copy(attemptId = UUID.randomUUID().toString()))
        val gate = CompletableDeferred<Unit>()
        val writes = listOf(first, second).zip(requests).map { (repository, request) ->
            async {
                gate.await()
                repository.writeAttempt(
                    base.session.id,
                    request,
                    wireJson.encodeToString(AttemptRequest.serializer(), request),
                ) { exercise, retry, createdAt -> attemptResponse(request.attemptId, exercise, retry, createdAt) }
            }
        }
        gate.complete(Unit)
        val results = withTimeout(TIMEOUT_MILLIS) { writes.awaitAll() }
        val retries = results.map { assertIs<AttemptWriteResult.Created>(it).response.retryNumber }.sorted()
        assertEquals(listOf(1, 2), retries)
    }

    @Test
    fun `failed and cancelled callbacks rollback without retry gaps or retained locks`() = withRepositories { first, second ->
        val failed = fixture(first)
        assertFailsWith<IllegalStateException> {
            first.writeAttempt(failed.session.id, failed.request, failed.payload) { _, _, _ -> error("offline") }
        }
        assertNull(second.findAttempt(failed.session.id, UUID.fromString(failed.request.attemptId)))
        val recovered = withTimeout(TIMEOUT_MILLIS) {
            second.writeAttempt(failed.session.id, failed.request, failed.payload) { exercise, retry, createdAt ->
                attemptResponse(failed.request.attemptId, exercise, retry, createdAt)
            }
        }
        assertEquals(1, assertIs<AttemptWriteResult.Created>(recovered).response.retryNumber)

        val cancelled = fixture(first)
        assertFailsWith<CancellationException> {
            first.writeAttempt(cancelled.session.id, cancelled.request, cancelled.payload) { _, _, _ ->
                throw CancellationException("caller left")
            }
        }
        val afterCancellation = withTimeout(TIMEOUT_MILLIS) {
            second.writeAttempt(cancelled.session.id, cancelled.request, cancelled.payload) { exercise, retry, createdAt ->
                attemptResponse(cancelled.request.attemptId, exercise, retry, createdAt)
            }
        }
        assertEquals(1, assertIs<AttemptWriteResult.Created>(afterCancellation).response.retryNumber)
    }

    @Test
    fun `stored response replays after exercise metadata is retired`() = withRepositories { first, second ->
        val fixture = fixture(first)
        val created = first.writeAttempt(fixture.session.id, fixture.request, fixture.payload) { exercise, retry, createdAt ->
            attemptResponse(fixture.request.attemptId, exercise, retry, createdAt)
        }
        val original = assertIs<AttemptWriteResult.Created>(created).response
        postgres.updateExerciseRevision(fixture.exercise.id, "retired-content")
        val callbackCount = AtomicInteger()
        val replay = second.writeAttempt(fixture.session.id, fixture.request, fixture.payload) { exercise, retry, createdAt ->
            callbackCount.incrementAndGet()
            attemptResponse(fixture.request.attemptId, exercise, retry, createdAt)
        }
        assertEquals(0, callbackCount.get())
        assertEquals(original, assertIs<AttemptWriteResult.Existing>(replay).response)
        assertEquals(original, second.findAttempt(fixture.session.id, UUID.fromString(fixture.request.attemptId)))
        val changed = fixture.request.copy(answer = AttemptAnswerDto.Text("læse"))
        assertIs<AttemptWriteResult.PayloadConflict>(
            first.writeAttempt(
                fixture.session.id,
                changed,
                wireJson.encodeToString(AttemptRequest.serializer(), changed),
            ) { exercise, retry, createdAt -> attemptResponse(changed.attemptId, exercise, retry, createdAt) },
        )
    }

    private suspend fun fixture(repository: PostgresRepository): Fixture {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val session = repository.createSession(suffix.repeat(2), Long.MAX_VALUE)
        val exerciseId = UUID.randomUUID()
        val generated = PilotEngine().generate(GenerationRequest("E001", "G004", 19, 3))
        val exercise = StoredExercise(
            exerciseId,
            session.id,
            generated.formatId,
            generated.targetSkillId,
            generated.reproduction.seed,
            generated.reproduction.index,
            generated.contentRevision,
            generated.generatorRevision,
            generated.toResponse(exerciseId.toString()),
            generated.reproductionFingerprint(),
        )
        repository.insertExercise(exercise)
        val request = AttemptRequest(
            UUID.randomUUID().toString(),
            exerciseId.toString(),
            AttemptAnswerDto.Text("læser"),
            false,
        )
        return Fixture(session, exercise, request, wireJson.encodeToString(AttemptRequest.serializer(), request))
    }

    private fun attemptResponse(attemptId: String, stored: StoredExercise, retry: Int, createdAt: Long) = AttemptResponse(
        attemptId,
        stored.id.toString(),
        "met",
        "acceptable",
        emptyList(),
        "Correct",
        "Hun læser en avis.",
        EVALUATION_REVISION,
        false,
        retry,
        createdAt,
    )

    private fun withRepositories(block: suspend kotlinx.coroutines.CoroutineScope.(PostgresRepository, PostgresRepository) -> Unit) =
        runBlocking {
            val config = postgres.config()
            migrate(config)
            val first = PostgresRepository.connect(config, wireJson)
            val second = PostgresRepository.connect(config, wireJson)
            try {
                block(first, second)
            } finally {
                first.close()
                second.close()
            }
        }

    private fun PostgreSQLContainer.updateExerciseRevision(id: UUID, revision: String) {
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.prepareStatement("UPDATE exercises SET content_revision = ? WHERE id = ?").use { statement ->
                statement.setString(1, revision)
                statement.setObject(2, id)
                assertEquals(1, statement.executeUpdate())
            }
        }
    }

    private fun PostgreSQLContainer.createDatabase(databaseName: String) {
        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.createStatement().use { statement -> statement.executeUpdate("CREATE DATABASE $databaseName") }
        }
    }

    private fun StoredExercise.generationRequest() = GenerationRequest(
        formatId,
        targetSkillId,
        seed,
        index,
        contentRevision,
        generatorRevision,
    )

    private fun PostgreSQLContainer.config(databaseName: String? = null) = ServerConfig(
        true, false, null, false, false, emptySet(), emptySet(), 60_000, null,
        databaseName?.let { "jdbc:postgresql://$host:${getMappedPort(5432)}/$it" } ?: jdbcUrl,
        "r2dbc:postgresql://$host:${getMappedPort(5432)}/${databaseName ?: "astrid_test"}", username, password, 4,
    )

    private data class Fixture(
        val session: SessionRecord,
        val exercise: StoredExercise,
        val request: AttemptRequest,
        val payload: String,
    )

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val POSTGRES_IMAGE =
            "postgres:18.6-alpine@sha256:10a5c69a83035ab5239f5620d708520e8b28f5aa1f2b20143e6024532edadfb5"
        val postgres: PostgreSQLContainer = PostgreSQLContainer(
            DockerImageName.parse(POSTGRES_IMAGE).asCompatibleSubstituteFor("postgres"),
        ).apply {
            withDatabaseName("astrid_test")
            withUsername("astrid")
            withPassword("astrid")
            start()
        }

        @JvmStatic
        @AfterClass
        fun stopPostgres() = postgres.stop()
    }
}
