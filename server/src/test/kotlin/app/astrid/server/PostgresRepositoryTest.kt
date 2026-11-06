package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class PostgresRepositoryTest {
    @Test
    fun `migrations restart reread isolation and concurrent idempotency use real postgres`() = runBlocking {
        val config = postgres.config()
        migrate(config)
        migrate(config)
        val firstRepository = PostgresRepository.connect(config, wireJson)
        val owner = firstRepository.createSession("a".repeat(64), Long.MAX_VALUE)
        val outsider = firstRepository.createSession("b".repeat(64), Long.MAX_VALUE)
        val exerciseId = UUID.randomUUID()
        val generated = PilotEngine().generate(GenerationRequest("E001", "G004", 19, 3))
        val stored = StoredExercise(
            id = exerciseId,
            sessionId = owner.id,
            formatId = generated.formatId,
            targetSkillId = generated.targetSkillId,
            seed = generated.reproduction.seed,
            index = generated.reproduction.index,
            contentRevision = generated.contentRevision,
            generatorRevision = generated.generatorRevision,
            presentation = generated.toResponse(exerciseId.toString()),
            reproductionFingerprint = generated.reproductionFingerprint(),
        )
        firstRepository.insertExercise(stored)
        assertNull(firstRepository.findExercise(outsider.id, exerciseId))
        firstRepository.close()

        val repository = PostgresRepository.connect(config, wireJson)
        assertEquals(stored, repository.findExercise(owner.id, exerciseId))
        val attemptId = UUID.randomUUID().toString()
        val request = AttemptRequest(attemptId, exerciseId.toString(), AttemptAnswerDto.Text("læser"), false)
        val payload = wireJson.encodeToString(AttemptRequest.serializer(), request)
        val writes = listOf(
            async {
                repository.writeAttempt(owner.id, request, payload) { exercise, retry, createdAt ->
                    attemptResponse(attemptId, exercise, retry, createdAt)
                }
            },
            async {
                repository.writeAttempt(owner.id, request, payload) { exercise, retry, createdAt ->
                    attemptResponse(attemptId, exercise, retry, createdAt)
                }
            },
        ).awaitAll()
        assertEquals(1, writes.count { it is AttemptWriteResult.Created })
        assertEquals(1, writes.count { it is AttemptWriteResult.Existing })
        val persisted = repository.findAttempt(owner.id, UUID.fromString(attemptId))
        assertEquals(1, persisted?.retryNumber)

        val changed = request.copy(answer = AttemptAnswerDto.Text("læse"))
        assertIs<AttemptWriteResult.PayloadConflict>(
            repository.writeAttempt(
                owner.id,
                changed,
                wireJson.encodeToString(AttemptRequest.serializer(), changed),
                { exercise, retry, createdAt -> attemptResponse(attemptId, exercise, retry, createdAt) },
            ),
        )
        assertNull(repository.findAttempt(outsider.id, UUID.fromString(attemptId)))
        repository.close()
    }

    private fun attemptResponse(attemptId: String, stored: StoredExercise, retry: Int, createdAt: Long) = AttemptResponse(
        attemptId = attemptId,
        exerciseId = stored.id.toString(),
        targetSatisfaction = "met",
        linguisticStatus = "acceptable",
        issues = emptyList(),
        feedback = "Correct",
        completedSentence = "Hun læser en avis.",
        evaluationRevision = EVALUATION_REVISION,
        hintUsed = false,
        retryNumber = retry,
        createdAt = createdAt,
    )

    private fun PostgreSQLContainer.config() = ServerConfig(
        developmentMode = true,
        inspectorEnabled = false,
        inspectorKey = null,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = emptySet(),
        allowedHosts = emptySet(),
        sessionTtlMillis = 60_000,
        webStaticDir = null,
        jdbcUrl = jdbcUrl,
        r2dbcUrl = "r2dbc:postgresql://$host:${getMappedPort(5432)}/astrid_test",
        postgresUser = username,
        postgresPassword = password,
        poolMaxSize = 4,
    )

    private companion object {
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
    }
}
