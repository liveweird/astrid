@file:Suppress("MaxLineLength", "MagicNumber") // Pinned digest and concrete migration/session bounds.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlanner
import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.AttemptWriteResult
import app.astrid.server.exercises.CreatedAttempt
import app.astrid.server.exercises.EVALUATION_REVISION
import app.astrid.server.exercises.ExerciseResponse
import app.astrid.server.exercises.GapResultDto
import app.astrid.server.exercises.GapSelectionDto
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.MatchingPairDto
import app.astrid.server.exercises.MatchingPairResultDto
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.exercises.PracticeCrossRunExposureDto
import app.astrid.server.exercises.StoredExercise
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.db.Database
import app.astrid.server.infra.db.migrate
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.NewPracticeItem
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticeCreateResult
import app.astrid.server.practice.PracticeFollowUpLineage
import app.astrid.server.practice.PracticeFollowUpRequest
import app.astrid.server.practice.PracticeMutationResult
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.StoredPracticeSession
import java.sql.DriverManager
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import org.flywaydb.core.Flyway

class PostgresRepositoryTest {
    @Test
    fun `present verb feedback exposure requires a prior met attempt in postgres`() = withRepositories { first, second ->
        val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
        val practiceId = UUID.randomUUID()
        val request = PracticeSessionRequest(practiceId.toString(), "present-verbs", "present-verbs-1", 17)
        val created = assertIs<PracticeCreateResult.Created>(
            first.practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) {
                practiceFixture(owner, request)
            },
        ).session
        setPracticeCursor(owner.id, practiceId, 1)
        val item = created.items[1]
        val presentKey = item.feedbackExposureKeys.single { it.startsWith("verb-present:") }

        suspend fun write(targetSatisfaction: String, expectPresentExposure: Boolean) {
            val attempt = practiceAttempt(item.exercise.id)
            val result = second.exercises.writeAttemptWithPractice(owner.id, attempt, attemptPayload(attempt)) {
                    stored, retry, now, source ->
                val frozen = assertNotNull(source).items.single { it.position == item.position }.feedbackExposureKeys
                assertEquals(expectPresentExposure, presentKey in frozen)
                CreatedAttempt(
                    attemptResponse(attempt.attemptId, stored, retry, now).copy(
                        targetSatisfaction = targetSatisfaction,
                    ),
                    PracticeAttemptContextDto(false, false, false, emptyList()),
                )
            }
            assertIs<AttemptWriteResult.Created>(result)
        }

        write("not-met", expectPresentExposure = false)
        write("met", expectPresentExposure = false)
        write("not-met", expectPresentExposure = true)
    }

    @Test
    fun `follow-up creation is atomic replayable and retains bounded ancestry across pools`() = withRepositories { first, second ->
        val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
        val sourceId = UUID.randomUUID()
        val sourceRequest = PracticeSessionRequest(sourceId.toString(), "reading-basics", "practice-1", 73)
        val source = assertIs<PracticeCreateResult.Created>(
            first.practice.createPracticeSession(owner, sourceRequest, wireJson.encodeToString(sourceRequest)) {
                practiceFixture(owner, sourceRequest)
            },
        ).session
        setPracticeCursor(owner.id, sourceId, source.items.size)

        val childRequest = PracticeFollowUpRequest(
            UUID.randomUUID().toString(),
            "follow-up-1",
            91,
            listOf("grammar:G005"),
        )
        val payload = "source=$sourceId|${wireJson.encodeToString(childRequest)}"
        val callbacks = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val creations = listOf(first, second).map { repository ->
            async {
                gate.await()
                repository.practice.createPracticeFollowUp(owner, sourceId, childRequest, payload) { completed ->
                    callbacks.incrementAndGet()
                    followUpFixture(completed, childRequest, sourceId)
                }
            }
        }
        gate.complete(Unit)
        val results = withTimeout(TIMEOUT_MILLIS) { creations.awaitAll() }
        assertEquals(1, callbacks.get())
        assertEquals(1, results.count { it is PracticeCreateResult.Created })
        assertEquals(1, results.count { it is PracticeCreateResult.Existing })
        val childId = UUID.fromString(childRequest.practiceSessionId)
        val child = assertNotNull(first.practice.findPracticeSession(owner.id, childId))
        assertEquals(sourceId, child.followUp?.sourcePracticeSessionId)
        assertEquals(1, child.sourceExposure.size)

        val failedRequest = childRequest.copy(practiceSessionId = UUID.randomUUID().toString())
        val failedPayload = "source=$sourceId|${wireJson.encodeToString(failedRequest)}"
        assertFailsWith<IllegalStateException> {
            first.practice.createPracticeFollowUp(owner, sourceId, failedRequest, failedPayload) { error("planning failed") }
        }
        assertNull(second.practice.findPracticeSession(owner.id, UUID.fromString(failedRequest.practiceSessionId)))
        assertIs<PracticeCreateResult.Created>(
            second.practice.createPracticeFollowUp(owner, sourceId, failedRequest, failedPayload) { completed ->
                followUpFixture(completed, failedRequest, sourceId)
            },
        )

        setPracticeCursor(owner.id, childId, child.items.size)
        val nestedRequest = childRequest.copy(practiceSessionId = UUID.randomUUID().toString(), seed = 92)
        val nested = first.practice.createPracticeFollowUp(
            owner,
            childId,
            nestedRequest,
            "source=$childId|${wireJson.encodeToString(nestedRequest)}",
        ) { completed ->
            assertEquals(sourceId.toString(), completed.sourceExposure.single().practiceSessionId)
            followUpFixture(completed, nestedRequest, childId)
        }
        val nestedSession = assertIs<PracticeCreateResult.Created>(nested).session
        assertEquals(sourceId.toString(), nestedSession.sourceExposure.single().practiceSessionId)
    }

    @Test
    fun `practice create attempts advance and ownership are atomic across pools`() = withRepositories { first, second ->
        val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
        val outsider = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
        val practiceId = UUID.randomUUID()
        val request = PracticeSessionRequest(practiceId.toString(), "reading-basics", "practice-1", 20261107)
        val requestPayload = wireJson.encodeToString(PracticeSessionRequest.serializer(), request)
        val created = first.practice.createPracticeSession(owner, request, requestPayload) { practiceFixture(owner, request) }
        val initial = assertIs<PracticeCreateResult.Created>(created).session
        assertEquals(8, initial.items.size)
        assertNull(second.practice.findPracticeSession(outsider.id, practiceId))
        val replayed = second.practice.createPracticeSession(owner, request, requestPayload) {
            error("saved practice request must replay before planning")
        }
        assertEquals(initial, assertIs<PracticeCreateResult.Existing>(replayed).session)
        val changed = request.copy(seed = request.seed + 1)
        assertIs<PracticeCreateResult.RequestConflict>(
            first.practice.createPracticeSession(
                owner,
                changed,
                wireJson.encodeToString(PracticeSessionRequest.serializer(), changed),
            ) { practiceFixture(owner, changed) },
        )

        val presentRequest = PracticeSessionRequest(
            UUID.randomUUID().toString(),
            "present-verbs",
            "present-verbs-1",
            request.seed,
        )
        val presentPayload = wireJson.encodeToString(presentRequest)
        val present = assertIs<PracticeCreateResult.Created>(
            first.practice.createPracticeSession(owner, presentRequest, presentPayload) { practiceFixture(owner, presentRequest) },
        ).session
        assertEquals(6, present.items.size)
        assertEquals(
            listOf("selection", "recall", "repair", "selection", "recall", "repair"),
            present.items.map { it.assessmentMode },
        )
        assertEquals(
            present,
            assertIs<PracticeCreateResult.Existing>(
                second.practice.createPracticeSession(owner, presentRequest, presentPayload) {
                    error("saved present-verb request must replay before planning")
                },
            ).session,
        )

        val exercise = initial.items.first().exercise
        val failedRequest = practiceAttempt(exercise.id)
        assertFailsWith<IllegalStateException> {
            first.exercises.writeAttemptWithPractice(owner.id, failedRequest, attemptPayload(failedRequest)) { _, _, _, _ ->
                error("offline")
            }
        }
        assertTrue(checkNotNull(second.practice.findPracticeSession(owner.id, practiceId)).items.first().attempts.isEmpty())

        val trigger = installContextFailureTrigger(owner.id, UUID.fromString(failedRequest.attemptId))
        try {
            assertFails {
                first.exercises.writeAttemptWithPractice(
                    owner.id,
                    failedRequest,
                    attemptPayload(failedRequest),
                ) { stored, retry, now, _ ->
                    CreatedAttempt(
                        attemptResponse(failedRequest.attemptId, stored, retry, now),
                        PracticeAttemptContextDto(false, false, false, emptyList()),
                    )
                }
            }
        } finally {
            dropContextFailureTrigger(trigger)
        }
        assertNull(second.exercises.findAttempt(owner.id, UUID.fromString(failedRequest.attemptId)))
        assertTrue(checkNotNull(second.practice.findPracticeSession(owner.id, practiceId)).items.first().attempts.isEmpty())

        val requests = listOf(
            failedRequest,
            practiceAttempt(exercise.id).copy(answer = AttemptAnswerDto.Text("different")),
        )
        val gate = CompletableDeferred<Unit>()
        val writes = listOf(first, second).zip(requests).map { (repository, attempt) ->
            async {
                gate.await()
                repository.exercises.writeAttemptWithPractice(owner.id, attempt, attemptPayload(attempt)) { stored, retry, now, source ->
                    assertNotNull(source)
                    CreatedAttempt(
                        attemptResponse(attempt.attemptId, stored, retry, now),
                        PracticeAttemptContextDto(false, false, false, emptyList()),
                    )
                }
            }
        }
        gate.complete(Unit)
        val results = withTimeout(TIMEOUT_MILLIS) { writes.awaitAll() }
        assertEquals(listOf(1, 2), results.map { assertIs<AttemptWriteResult.Created>(it).response.retryNumber }.sorted())
        equalizePracticeAttemptTimes(owner.id, practiceId, 0)
        val loadedAttempts = checkNotNull(first.practice.findPracticeSession(owner.id, practiceId)).items.first().attempts
        assertEquals(listOf(1, 2), loadedAttempts.map { it.response.retryNumber })
        val expectedByRetry = requests.zip(results).sortedBy { (_, result) ->
            assertIs<AttemptWriteResult.Created>(result).response.retryNumber
        }
        assertEquals(expectedByRetry.map { it.first.attemptId }, loadedAttempts.map { it.response.attemptId })
        assertEquals(expectedByRetry.map { it.first.answer }, loadedAttempts.map { it.answer })

        val advances = listOf(first, second).map { repository ->
            async { repository.practice.advancePracticeSession(owner.id, practiceId, 0) }
        }
        assertTrue(withTimeout(TIMEOUT_MILLIS) { advances.awaitAll() }.all { it is PracticeMutationResult.Success })
        assertEquals(1, checkNotNull(first.practice.findPracticeSession(owner.id, practiceId)).cursor)
        val stale = practiceAttempt(exercise.id)
        assertIs<AttemptWriteResult.PracticePositionConflict>(
            second.exercises.writeAttemptWithPractice(owner.id, stale, attemptPayload(stale)) { _, _, _, _ ->
                error("stale practice attempt must not evaluate")
            },
        )
    }

    @Test
    fun `V2 preserves V1 grammar evidence and stores nullable lexical evidence with exact replay`() = runBlocking {
        val databaseName = "astrid_migration_${UUID.randomUUID().toString().replace("-", "")}"
        PostgresTestDatabase.createDatabase(databaseName)
        val config = PostgresTestDatabase.config(databaseName)
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
        val repository = PostgresStores(Database.connect(config), wireJson)
        try {
            val restoredGrammar = assertNotNull(repository.exercises.findExercise(sessionId, grammarId))
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
            repository.exercises.insertExercise(storedLexical)
            val restoredLexical = assertNotNull(repository.exercises.findExercise(sessionId, lexicalId))
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
            val created = repository.exercises.writeAttempt(sessionId, request, payload) { exercise, retry, createdAt ->
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
            val replayed = repository.exercises.writeAttempt(sessionId, request, payload) { _, _, _ ->
                callbacks.incrementAndGet()
                error("saved attempt must replay before regeneration")
            }
            assertEquals(1, callbacks.get())
            assertEquals(createdResponse, assertIs<AttemptWriteResult.Existing>(replayed).response)
            assertEquals("danish-headword", repository.exercises.findAttempt(sessionId, UUID.fromString(request.attemptId))?.linguisticScope)
        } finally {
            repository.close()
        }
    }

    @Test
    fun `readiness reports database state without swallowing caller cancellation`() = withRepositories { first, second ->
        assertEquals(true, first.readiness.ready())

        first.close()
        assertEquals(false, first.readiness.ready())
        assertEquals(true, second.readiness.ready())

        val observedCall = CompletableDeferred<Result<Boolean>>()
        val cancelledChild = launch(start = CoroutineStart.UNDISPATCHED) {
            cancel(CancellationException("readiness caller left"))
            observedCall.complete(runCatching { second.readiness.ready() })
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
                repository.exercises.writeAttempt(same.session.id, same.request, same.payload) { exercise, retry, createdAt ->
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
                repository.exercises.writeAttempt(
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
                repository.exercises.writeAttempt(
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
            first.exercises.writeAttempt(failed.session.id, failed.request, failed.payload) { _, _, _ -> error("offline") }
        }
        assertNull(second.exercises.findAttempt(failed.session.id, UUID.fromString(failed.request.attemptId)))
        val recovered = withTimeout(TIMEOUT_MILLIS) {
            second.exercises.writeAttempt(failed.session.id, failed.request, failed.payload) { exercise, retry, createdAt ->
                attemptResponse(failed.request.attemptId, exercise, retry, createdAt)
            }
        }
        assertEquals(1, assertIs<AttemptWriteResult.Created>(recovered).response.retryNumber)

        val cancelled = fixture(first)
        assertFailsWith<CancellationException> {
            first.exercises.writeAttempt(cancelled.session.id, cancelled.request, cancelled.payload) { _, _, _ ->
                throw CancellationException("caller left")
            }
        }
        val afterCancellation = withTimeout(TIMEOUT_MILLIS) {
            second.exercises.writeAttempt(cancelled.session.id, cancelled.request, cancelled.payload) { exercise, retry, createdAt ->
                attemptResponse(cancelled.request.attemptId, exercise, retry, createdAt)
            }
        }
        assertEquals(1, assertIs<AttemptWriteResult.Created>(afterCancellation).response.retryNumber)
    }

    @Test
    fun `stored response replays after exercise metadata is retired`() = withRepositories { first, second ->
        val fixture = fixture(first)
        val created = first.exercises.writeAttempt(fixture.session.id, fixture.request, fixture.payload) { exercise, retry, createdAt ->
            attemptResponse(fixture.request.attemptId, exercise, retry, createdAt)
        }
        val original = assertIs<AttemptWriteResult.Created>(created).response
        updateExerciseRevision(fixture.exercise.id, "retired-content")
        val callbackCount = AtomicInteger()
        val replay = second.exercises.writeAttempt(fixture.session.id, fixture.request, fixture.payload) { exercise, retry, createdAt ->
            callbackCount.incrementAndGet()
            attemptResponse(fixture.request.attemptId, exercise, retry, createdAt)
        }
        assertEquals(0, callbackCount.get())
        assertEquals(original, assertIs<AttemptWriteResult.Existing>(replay).response)
        assertEquals(original, second.exercises.findAttempt(fixture.session.id, UUID.fromString(fixture.request.attemptId)))
        val changed = fixture.request.copy(answer = AttemptAnswerDto.Text("læse"))
        assertIs<AttemptWriteResult.PayloadConflict>(
            first.exercises.writeAttempt(
                fixture.session.id,
                changed,
                wireJson.encodeToString(AttemptRequest.serializer(), changed),
            ) { exercise, retry, createdAt -> attemptResponse(changed.attemptId, exercise, retry, createdAt) },
        )
    }

    @Test
    fun `pair four writing and reading evidence persists replays and remains owner isolated`() =
        withRepositories { first, second ->
            val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val outsider = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val engine = PilotEngine()
            val cases = listOf(
                Triple("E009", "G005", "Hun læser en avis."),
                Triple("E010", null, "a newspaper"),
            )

            cases.forEach { (format, target, answer) ->
                val generated = engine.generate(GenerationRequest(format, target, 47, 0, "pilot-4", "generator-4"))
                val exerciseId = UUID.randomUUID()
                val stored = StoredExercise(
                    exerciseId,
                    owner.id,
                    generated.formatId,
                    generated.targetSkillId,
                    generated.reproduction.seed,
                    generated.reproduction.index,
                    generated.contentRevision,
                    generated.generatorRevision,
                    generated.toResponse(exerciseId.toString()),
                    generated.reproductionFingerprint(),
                )
                first.exercises.insertExercise(stored)

                assertEquals(stored, second.exercises.findExercise(owner.id, exerciseId))
                assertNull(second.exercises.findExercise(outsider.id, exerciseId))

                val request = AttemptRequest(
                    UUID.randomUUID().toString(),
                    exerciseId.toString(),
                    AttemptAnswerDto.Text(answer),
                    false,
                )
                val payload = wireJson.encodeToString(request)
                val callbackCount = AtomicInteger()
                val created = second.exercises.writeAttempt(owner.id, request, payload) { exercise, retry, createdAt ->
                    callbackCount.incrementAndGet()
                    AttemptResponse(
                        attemptId = request.attemptId,
                        exerciseId = exercise.id.toString(),
                        targetSatisfaction = "met",
                        linguisticStatus = "acceptable",
                        issues = emptyList(),
                        feedback = "Correct",
                        completedSentence = if (format == "E009") answer else "",
                        evaluationRevision = "evaluation-4",
                        hintUsed = false,
                        retryNumber = retry,
                        createdAt = createdAt,
                        linguisticScope = if (format == "E009") "danish-sentence" else "english-choice",
                        responseConstraintStatus = "met",
                        meaningSatisfaction = if (format == "E009") "met" else null,
                    )
                }
                val response = assertIs<AttemptWriteResult.Created>(created).response
                val replay = first.exercises.writeAttempt(owner.id, request, payload) { _, _, _ ->
                    callbackCount.incrementAndGet()
                    error("saved pair-four attempt must replay before regeneration")
                }
                assertEquals(1, callbackCount.get())
                assertEquals(response, assertIs<AttemptWriteResult.Existing>(replay).response)
                assertEquals(response, second.exercises.findAttempt(owner.id, UUID.fromString(request.attemptId)))
                assertNull(second.exercises.findAttempt(outsider.id, UUID.fromString(request.attemptId)))
            }
        }

    @Test
    fun `pair five word choice and tile evidence persists exact answers and replays owner scoped`() =
        withRepositories { first, second ->
            val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val outsider = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val engine = PilotEngine()
            val requests = listOf(
                GenerationRequest("E011", null, 47, 0, "pilot-5", "generator-5"),
                GenerationRequest("E012", "G005", 47, 0, "pilot-5", "generator-5"),
                GenerationRequest("E013", "G004", 47, 0, "pilot-5", "generator-5"),
            )

            requests.forEach { generation ->
                val generated = engine.generate(generation)
                val exerciseId = UUID.randomUUID()
                val stored = StoredExercise(
                    exerciseId,
                    owner.id,
                    generated.formatId,
                    generated.targetSkillId,
                    generated.reproduction.seed,
                    generated.reproduction.index,
                    generated.contentRevision,
                    generated.generatorRevision,
                    generated.toResponse(exerciseId.toString()),
                    generated.reproductionFingerprint(),
                )
                first.exercises.insertExercise(stored)
                assertEquals(stored, second.exercises.findExercise(owner.id, exerciseId))
                assertNull(second.exercises.findExercise(outsider.id, exerciseId))

                val answer = if (generated.formatId == "E011") {
                    val source = requireNotNull(generated.inspector.lexicalSource)
                    AttemptAnswerDto.Text("\u00a0${source.article}\u2003${source.headword}  ")
                } else {
                    val order = requireNotNull(generated.inspector.tileSource).licensedOrders
                        .single { it.kind == "subject-first" }
                    AttemptAnswerDto.Order(order.blockIds)
                }
                val request = AttemptRequest(UUID.randomUUID().toString(), exerciseId.toString(), answer, false)
                val payload = wireJson.encodeToString(request)
                val callbackCount = AtomicInteger()
                val created = second.exercises.writeAttempt(owner.id, request, payload) { exercise, retry, createdAt ->
                    callbackCount.incrementAndGet()
                    AttemptResponse(
                        attemptId = request.attemptId,
                        exerciseId = exercise.id.toString(),
                        targetSatisfaction = "met",
                        linguisticStatus = "acceptable",
                        issues = emptyList(),
                        feedback = "Correct",
                        completedSentence = "",
                        evaluationRevision = "evaluation-5",
                        hintUsed = false,
                        retryNumber = retry,
                        createdAt = createdAt,
                        linguisticScope = if (generated.formatId == "E011") "danish-choice" else "danish-sentence",
                        responseConstraintStatus = "met",
                        meaningSatisfaction = "met",
                    )
                }
                val response = assertIs<AttemptWriteResult.Created>(created).response
                val replay = first.exercises.writeAttempt(owner.id, request, payload) { _, _, _ ->
                    callbackCount.incrementAndGet()
                    error("saved pair-five attempt must replay before regeneration")
                }
                assertEquals(1, callbackCount.get())
                assertEquals(response, assertIs<AttemptWriteResult.Existing>(replay).response)
                assertStoredRawAnswer(owner.id, request, payload)
                assertEquals(response, second.exercises.findAttempt(owner.id, UUID.fromString(request.attemptId)))
                assertNull(second.exercises.findAttempt(outsider.id, UUID.fromString(request.attemptId)))
            }
        }

    @Test
    fun `pair six matching raw order and per-pair result persist and replay across pools`() =
        withRepositories { first, second ->
            val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val outsider = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val generated = PilotEngine().generate(GenerationRequest("E014", null, 47, 0, "pilot-6", "generator-6"))
            val exerciseId = UUID.randomUUID()
            val stored = StoredExercise(
                exerciseId,
                owner.id,
                generated.formatId,
                generated.targetSkillId,
                generated.reproduction.seed,
                generated.reproduction.index,
                generated.contentRevision,
                generated.generatorRevision,
                generated.toResponse(exerciseId.toString()),
                generated.reproductionFingerprint(),
            )
            first.exercises.insertExercise(stored)
            assertEquals(stored, second.exercises.findExercise(owner.id, exerciseId))
            assertNull(second.exercises.findExercise(outsider.id, exerciseId))

            val source = requireNotNull(generated.inspector.matchingSource)
            val rawPairs = source.pairs.reversed().map { MatchingPairDto(it.leftId, it.rightId) }
            val request = AttemptRequest(
                UUID.randomUUID().toString(),
                exerciseId.toString(),
                AttemptAnswerDto.Matching(rawPairs),
                false,
            )
            val payload = wireJson.encodeToString(request)
            val callbackCount = AtomicInteger()
            val created = second.exercises.writeAttempt(owner.id, request, payload) { exercise, retry, createdAt ->
                callbackCount.incrementAndGet()
                AttemptResponse(
                    attemptId = request.attemptId,
                    exerciseId = exercise.id.toString(),
                    targetSatisfaction = "met",
                    linguisticStatus = "acceptable",
                    issues = emptyList(),
                    feedback = "All four meanings match.",
                    completedSentence = "",
                    evaluationRevision = "evaluation-6",
                    hintUsed = false,
                    retryNumber = retry,
                    createdAt = createdAt,
                    linguisticScope = "danish-matching",
                    responseConstraintStatus = "met",
                    meaningSatisfaction = "met",
                    matchingResults = source.pairs.map { pair ->
                        MatchingPairResultDto(
                            leftId = pair.leftId,
                            rightId = pair.rightId,
                            lexicalTarget = pair.lexicalSource.let {
                                LexicalTargetDto(it.entryId, it.senseId, "meaning", "en-da")
                            },
                            targetSatisfaction = "met",
                            meaningSatisfaction = "met",
                            responseConstraintStatus = "met",
                            linguisticStatus = "acceptable",
                            feedback = "Match confirmed.",
                        )
                    },
                )
            }
            val response = assertIs<AttemptWriteResult.Created>(created).response
            val replay = first.exercises.writeAttempt(owner.id, request, payload) { _, _, _ ->
                callbackCount.incrementAndGet()
                error("saved matching attempt must replay before regeneration")
            }
            assertEquals(1, callbackCount.get())
            assertEquals(response, assertIs<AttemptWriteResult.Existing>(replay).response)
            assertStoredRawAnswer(owner.id, request, payload)
            assertEquals(response, second.exercises.findAttempt(owner.id, UUID.fromString(request.attemptId)))
            assertNull(second.exercises.findAttempt(outsider.id, UUID.fromString(request.attemptId)))

            val reordered = request.copy(answer = AttemptAnswerDto.Matching(rawPairs.reversed()))
            assertIs<AttemptWriteResult.PayloadConflict>(
                first.exercises.writeAttempt(owner.id, reordered, wireJson.encodeToString(reordered)) { _, _, _ ->
                    error("conflicting raw order must not grade")
                },
            )
        }

    @Test
    fun `pair seven gap raw order and per-slot result persist and replay across pools`() =
        withRepositories { first, second ->
            val owner = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val outsider = first.sessions.createSession(UUID.randomUUID().toString().replace("-", "").repeat(2), Long.MAX_VALUE)
            val generated = PilotEngine().generate(GenerationRequest("E015", "G004", 47, 0, "pilot-7", "generator-7"))
            val exerciseId = UUID.randomUUID()
            val stored = StoredExercise(
                exerciseId,
                owner.id,
                generated.formatId,
                generated.targetSkillId,
                generated.reproduction.seed,
                generated.reproduction.index,
                generated.contentRevision,
                generated.generatorRevision,
                generated.toResponse(exerciseId.toString()),
                generated.reproductionFingerprint(),
            )
            first.exercises.insertExercise(stored)
            assertEquals(stored, second.exercises.findExercise(owner.id, exerciseId))
            assertNull(second.exercises.findExercise(outsider.id, exerciseId))

            val source = requireNotNull(generated.inspector.gapSource)
            val rawSelections = source.slots.reversed().map { GapSelectionDto(it.slotId, it.expectedChoiceId) }
            val request = AttemptRequest(
                UUID.randomUUID().toString(),
                exerciseId.toString(),
                AttemptAnswerDto.Gaps(rawSelections),
                false,
            )
            val payload = wireJson.encodeToString(request)
            val callbackCount = AtomicInteger()
            val created = second.exercises.writeAttempt(owner.id, request, payload) { exercise, retry, createdAt ->
                callbackCount.incrementAndGet()
                AttemptResponse(
                    attemptId = request.attemptId,
                    exerciseId = exercise.id.toString(),
                    targetSatisfaction = "met",
                    linguisticStatus = "acceptable",
                    issues = emptyList(),
                    feedback = "Both gaps are correct.",
                    completedSentence = requireNotNull(generated.inspector.canonicalSentence),
                    evaluationRevision = "evaluation-7",
                    hintUsed = false,
                    retryNumber = retry,
                    createdAt = createdAt,
                    linguisticScope = "danish-gaps",
                    responseConstraintStatus = "met",
                    gapResults = source.slots.map { slot ->
                        GapResultDto(
                            slotId = slot.slotId,
                            choiceId = slot.expectedChoiceId,
                            targetSkillId = if (slot.slotId == "g0") "G004" else "G006",
                            assessmentRole = if (slot.slotId == "g0") "primary" else "support",
                            targetSatisfaction = "met",
                            responseConstraintStatus = "met",
                            linguisticStatus = "acceptable",
                            feedback = "Correct.",
                        )
                    },
                )
            }
            val response = assertIs<AttemptWriteResult.Created>(created).response
            val replay = first.exercises.writeAttempt(owner.id, request, payload) { _, _, _ ->
                callbackCount.incrementAndGet()
                error("saved gap attempt must replay before regeneration")
            }
            assertEquals(1, callbackCount.get())
            assertEquals(response, assertIs<AttemptWriteResult.Existing>(replay).response)
            assertStoredRawAnswer(owner.id, request, payload)
            assertEquals(response, second.exercises.findAttempt(owner.id, UUID.fromString(request.attemptId)))
            assertNull(second.exercises.findAttempt(outsider.id, UUID.fromString(request.attemptId)))

            val reordered = request.copy(answer = AttemptAnswerDto.Gaps(rawSelections.reversed()))
            assertIs<AttemptWriteResult.PayloadConflict>(
                first.exercises.writeAttempt(owner.id, reordered, wireJson.encodeToString(reordered)) { _, _, _ ->
                    error("conflicting raw order must not grade")
                },
            )
        }

    private fun assertStoredRawAnswer(ownerId: UUID, request: AttemptRequest, payload: String) {
        PostgresTestDatabase.connect().use { connection ->
            connection.prepareStatement(
                "SELECT raw_answer, request_payload FROM attempts WHERE session_id = ? AND attempt_id = ?",
            ).use { statement ->
                statement.setObject(1, ownerId)
                statement.setObject(2, UUID.fromString(request.attemptId))
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(wireJson.encodeToString(AttemptAnswerDto.serializer(), request.answer), rows.getString(1))
                    assertEquals(payload, rows.getString(2))
                    assertTrue(!rows.next())
                }
            }
        }
    }

    private suspend fun fixture(repository: PostgresStores): Fixture {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val session = repository.sessions.createSession(suffix.repeat(2), Long.MAX_VALUE)
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
        repository.exercises.insertExercise(exercise)
        val request = AttemptRequest(
            UUID.randomUUID().toString(),
            exerciseId.toString(),
            AttemptAnswerDto.Text("læser"),
            false,
        )
        return Fixture(session, exercise, request, wireJson.encodeToString(AttemptRequest.serializer(), request))
    }

    private fun practiceFixture(owner: SessionPrincipal, request: PracticeSessionRequest): NewPracticeSession {
        val plan = PracticePlanner().plan(request.focusId, request.planRevision, request.seed)
        return NewPracticeSession(
            plan.title,
            plan.contentRevision,
            plan.generatorRevision,
            plan.evaluationRevision,
            plan.prerequisiteSkillIds,
            plan.introduction.map { IntroductionDto(it.title, it.body) },
            plan.items.map { item ->
                val id = UUID.randomUUID()
                NewPracticeItem(
                    item.position,
                    StoredExercise(
                        id,
                        owner.id,
                        item.exercise.formatId,
                        item.exercise.targetSkillId,
                        item.exercise.reproduction.seed,
                        item.exercise.reproduction.index,
                        item.exercise.contentRevision,
                        item.exercise.generatorRevision,
                        item.exercise.toResponse(id.toString()),
                        item.exercise.reproductionFingerprint(),
                    ),
                    item.targetDescriptor.label,
                    item.answerExposureKeys,
                    item.promptExposureKeys,
                    item.lessonExposureKeys,
                    item.hintExposureKeys,
                    item.feedbackExposureKeys,
                    item.targetDescriptor.assessmentMode,
                )
            },
        )
    }

    private fun followUpFixture(
        source: StoredPracticeSession,
        request: PracticeFollowUpRequest,
        sourceId: UUID,
    ): NewPracticeSession {
        val sourceItem = source.items.first()
        val exerciseId = UUID.randomUUID()
        val item = NewPracticeItem(
            0,
            sourceItem.exercise.copy(
                id = exerciseId,
                presentation = sourceItem.exercise.presentation.copy(id = exerciseId.toString()),
            ),
            sourceItem.targetLabel,
            sourceItem.answerExposureKeys,
            sourceItem.promptExposureKeys,
            sourceItem.lessonExposureKeys,
            sourceItem.hintExposureKeys,
            sourceItem.feedbackExposureKeys,
            sourceItem.assessmentMode,
        )
        val ancestry = source.sourceExposure.ifEmpty {
            listOf(PracticeCrossRunExposureDto(sourceId.toString(), -1, "introduction", listOf("grammar:G005")))
        }
        return NewPracticeSession(
            "Review selected targets",
            source.contentRevision,
            source.generatorRevision,
            source.evaluationRevision,
            source.prerequisiteSkillIds,
            source.introduction,
            listOf(item),
            PracticeFollowUpLineage(sourceId, request.targetKeys),
            ancestry,
        )
    }

    private fun setPracticeCursor(ownerId: UUID, practiceId: UUID, cursor: Int) {
        PostgresTestDatabase.connect().use { connection ->
            connection.prepareStatement(
                "UPDATE practice_sessions SET cursor = ? WHERE owner_session_id = ? AND practice_session_id = ?",
            ).use { statement ->
                statement.setInt(1, cursor)
                statement.setObject(2, ownerId)
                statement.setObject(3, practiceId)
                assertEquals(1, statement.executeUpdate())
            }
        }
    }

    private fun practiceAttempt(exerciseId: UUID) = AttemptRequest(
        UUID.randomUUID().toString(),
        exerciseId.toString(),
        AttemptAnswerDto.Text("wrong"),
        false,
    )

    private fun attemptPayload(request: AttemptRequest) =
        wireJson.encodeToString(AttemptRequest.serializer(), request)

    private fun installContextFailureTrigger(ownerId: UUID, attemptId: UUID): String {
        val suffix = UUID.randomUUID().toString().replace("-", "")
        val function = "fail_practice_context_$suffix"
        val trigger = "practice_context_trigger_$suffix"
        PostgresTestDatabase.connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """CREATE FUNCTION $function() RETURNS trigger AS ${'$'}${'$'}
                        BEGIN
                            IF NEW.owner_session_id = '$ownerId'::uuid AND NEW.attempt_id = '$attemptId'::uuid THEN
                                RAISE EXCEPTION 'forced practice context failure';
                            END IF;
                            RETURN NEW;
                        END;
                        ${'$'}${'$'} LANGUAGE plpgsql""".trimIndent(),
                )
                statement.execute(
                    "CREATE TRIGGER $trigger BEFORE INSERT ON practice_attempt_context " +
                        "FOR EACH ROW EXECUTE FUNCTION $function()",
                )
            }
        }
        return "$trigger:$function"
    }

    private fun dropContextFailureTrigger(names: String) {
        val (trigger, function) = names.split(':')
        PostgresTestDatabase.connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("DROP TRIGGER IF EXISTS $trigger ON practice_attempt_context")
                statement.execute("DROP FUNCTION IF EXISTS $function()")
            }
        }
    }

    private fun equalizePracticeAttemptTimes(ownerId: UUID, practiceId: UUID, position: Int) {
        PostgresTestDatabase.connect().use { connection ->
            connection.prepareStatement(
                """UPDATE attempts SET created_at = 1
                    WHERE session_id = ? AND attempt_id IN (
                        SELECT attempt_id FROM practice_attempt_context
                        WHERE owner_session_id = ? AND practice_session_id = ? AND position = ?
                    )""".trimIndent(),
            ).use { statement ->
                statement.setObject(1, ownerId)
                statement.setObject(2, ownerId)
                statement.setObject(3, practiceId)
                statement.setInt(4, position)
                assertEquals(2, statement.executeUpdate())
            }
            connection.prepareStatement(
                """UPDATE practice_attempt_context SET created_at = 1
                    WHERE owner_session_id = ? AND practice_session_id = ? AND position = ?""".trimIndent(),
            ).use { statement ->
                statement.setObject(1, ownerId)
                statement.setObject(2, practiceId)
                statement.setInt(3, position)
                assertEquals(2, statement.executeUpdate())
            }
        }
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

    private fun withRepositories(block: suspend kotlinx.coroutines.CoroutineScope.(PostgresStores, PostgresStores) -> Unit) =
        runBlocking {
            val config = PostgresTestDatabase.config()
            migrate(config)
            val first = PostgresStores(Database.connect(config), wireJson)
            val second = PostgresStores(Database.connect(config), wireJson)
            try {
                block(first, second)
            } finally {
                first.close()
                second.close()
            }
        }

    private fun updateExerciseRevision(id: UUID, revision: String) {
        PostgresTestDatabase.connect().use { connection ->
            connection.prepareStatement("UPDATE exercises SET content_revision = ? WHERE id = ?").use { statement ->
                statement.setString(1, revision)
                statement.setObject(2, id)
                assertEquals(1, statement.executeUpdate())
            }
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

    private data class Fixture(
        val session: SessionPrincipal,
        val exercise: StoredExercise,
        val request: AttemptRequest,
        val payload: String,
    )

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
