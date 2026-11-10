package app.astrid.server

import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlanner
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.AttemptWriteResult
import app.astrid.server.exercises.CreatedAttempt
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.toAttemptContext
import app.astrid.server.infra.db.Database
import app.astrid.server.infra.db.migrate
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.PracticeCreateResult
import app.astrid.server.practice.PracticeMutationResult
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.PracticeSupportRequest
import app.astrid.server.practice.toNewPracticeSession
import app.astrid.server.practice.toResponse
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ModuleOneLifecycleTest {
    @Test
    fun `module one support and latest retry serialize with advance across postgres pools`() = runBlocking {
        val config = PostgresTestDatabase.config()
        migrate(config)
        val first = PostgresStores(Database.connect(config), wireJson)
        val second = PostgresStores(Database.connect(config), wireJson)
        try {
            withTimeout(TIMEOUT_MILLIS) {
                val owner = first.sessions.createSession("a".repeat(64), Long.MAX_VALUE)
                suspend fun createPractice(seed: Int) = PracticeSessionRequest(
                    UUID.randomUUID().toString(),
                    "module-one-singular",
                    "module-one-singular-1",
                    seed,
                ).let { request ->
                    assertIs<PracticeCreateResult.Created>(
                        first.practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) {
                            PracticePlanner(PilotEngine()).plan(request.focusId, request.planRevision, request.seed)
                                .toNewPracticeSession(owner.id)
                        },
                    ).session
                }

                val attemptFirst = createPractice(0)
                val blockedAttempt = attempt(attemptFirst.items.first().exercise.id, "jeg")
                assertIs<AttemptWriteResult.PracticeIntroductionRequired>(
                    second.exercises.writeAttemptWithPractice(
                        owner.id,
                        blockedAttempt,
                        wireJson.encodeToString(blockedAttempt),
                    ) { _, _, _, _ -> error("Attempt-first ordering must not evaluate") },
                )
                assertTrue(
                    assertNotNull(first.practice.findPracticeSession(owner.id, attemptFirst.id)).items[0].attempts.isEmpty(),
                )
                assertIs<PracticeMutationResult.Success>(
                    first.practice.openPracticeSupport(
                        owner.id,
                        attemptFirst.id,
                        PracticeSupportRequest("introduction"),
                    ),
                )

                val concurrent = createPractice(2)
                val concurrentAttempt = attempt(concurrent.items.first().exercise.id, "jeg")
                val (concurrentSupport, concurrentWrite) = PostgresTestDatabase.connect().use { blocker ->
                    blocker.autoCommit = false
                    blocker.prepareStatement(
                        "SELECT id FROM development_sessions WHERE id = ? FOR UPDATE",
                    ).use { statement ->
                        statement.setObject(1, owner.id)
                        statement.executeQuery().use { result -> assertTrue(result.next()) }
                    }
                    coroutineScope {
                        val start = CompletableDeferred<Unit>()
                        val support = async {
                            start.await()
                            first.practice.openPracticeSupport(
                                owner.id,
                                concurrent.id,
                                PracticeSupportRequest("introduction"),
                            )
                        }
                        val write = async {
                            start.await()
                            second.exercises.writeAttemptWithPractice(
                                owner.id,
                                concurrentAttempt,
                                wireJson.encodeToString(concurrentAttempt),
                            ) { stored, retry, now, source ->
                                val context = assertNotNull(source).toAttemptContext()
                                assertTrue(context.introductionOpened)
                                CreatedAttempt(
                                    response(
                                        concurrentAttempt,
                                        stored.id,
                                        retry,
                                        now,
                                        "met",
                                        "acceptable",
                                        "met",
                                    ),
                                    context,
                                )
                            }
                        }
                        start.complete(Unit)
                        try {
                            awaitBlockedSessionLocks(2)
                        } finally {
                            blocker.rollback()
                        }
                        support.await() to write.await()
                    }
                }
                assertIs<PracticeMutationResult.Success>(concurrentSupport)
                val concurrentSaved = assertNotNull(first.practice.findPracticeSession(owner.id, concurrent.id))
                assertTrue(concurrentSaved.introductionOpened)
                when (concurrentWrite) {
                    AttemptWriteResult.PracticeIntroductionRequired -> {
                        assertTrue(concurrentSaved.items[0].attempts.isEmpty())
                    }
                    is AttemptWriteResult.Created -> {
                        assertEquals(1, concurrentSaved.items[0].attempts.size)
                        assertTrue(concurrentSaved.items[0].attempts.single().context.introductionOpened)
                    }
                    else -> error("Concurrent introduction operations returned $concurrentWrite")
                }

                val practice = createPractice(1)
                assertIs<PracticeMutationResult.Success>(
                    first.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("introduction")),
                )
                val exercise = practice.items.first().exercise
                val initialAttempt = attempt(exercise.id, "jeg")
                assertIs<AttemptWriteResult.Created>(
                    second.exercises.writeAttemptWithPractice(
                        owner.id,
                        initialAttempt,
                        wireJson.encodeToString(initialAttempt),
                    ) { stored, retry, now, source ->
                        val context = assertNotNull(source).toAttemptContext()
                        assertTrue(context.introductionOpened)
                        CreatedAttempt(response(initialAttempt, stored.id, retry, now, "met", "acceptable", "met"), context)
                    },
                )
                val supportFirst = assertNotNull(first.practice.findPracticeSession(owner.id, practice.id)).items[0]
                assertEquals(1, supportFirst.attempts.size)
                assertTrue(supportFirst.attempts.single().context.introductionOpened)

                val unknown = attempt(exercise.id, "ukendt")
                val evaluatorEntered = CompletableDeferred<Unit>()
                val releaseEvaluator = CompletableDeferred<Unit>()
                val (unknownResult, advanceResult) = coroutineScope {
                    val write = async {
                        second.exercises.writeAttemptWithPractice(
                            owner.id,
                            unknown,
                            wireJson.encodeToString(unknown),
                        ) { stored, retry, now, source ->
                            evaluatorEntered.complete(Unit)
                            releaseEvaluator.await()
                            CreatedAttempt(
                                response(unknown, stored.id, retry, now, "unevaluable", "unknown", "met"),
                                assertNotNull(source).toAttemptContext(),
                            )
                        }
                    }
                    evaluatorEntered.await()
                    val advance = async { first.practice.advancePracticeSession(owner.id, practice.id, 0) }
                    awaitBlockedSessionLocks(1)
                    releaseEvaluator.complete(Unit)
                    write.await() to advance.await()
                }
                assertEquals(2, assertIs<AttemptWriteResult.Created>(unknownResult).response.retryNumber)
                assertIs<PracticeMutationResult.Conflict>(advanceResult)
                val blocked = assertNotNull(first.practice.findPracticeSession(owner.id, practice.id))
                assertEquals(0, blocked.cursor)
                assertEquals("unevaluable", blocked.items[0].attempts.last().response.targetSatisfaction)
                assertEquals("unknown", blocked.items[0].attempts.last().response.linguisticStatus)
                assertFalse(blocked.toResponse().items[0].canAdvance!!)

                val shaped = attempt(exercise.id, "jeg er")
                assertIs<AttemptWriteResult.Created>(
                    first.exercises.writeAttemptWithPractice(
                        owner.id,
                        shaped,
                        wireJson.encodeToString(shaped),
                    ) { stored, retry, now, source ->
                        CreatedAttempt(
                            response(
                                shaped,
                                stored.id,
                                retry,
                                now,
                                "unevaluable",
                                "known-issue",
                                "not-met",
                                listOf(IssueDto("ONE_WORD_RESPONSE_REQUIRED", "Type one word.")),
                            ),
                            assertNotNull(source).toAttemptContext(),
                        )
                    },
                )
                val advances = coroutineScope {
                    listOf(first, second).map { stores ->
                        async { stores.practice.advancePracticeSession(owner.id, practice.id, 0) }
                    }.awaitAll()
                }
                assertTrue(advances.all { it is PracticeMutationResult.Success })
                assertEquals(1, assertNotNull(first.practice.findPracticeSession(owner.id, practice.id)).cursor)

                val stale = attempt(exercise.id, "jeg")
                assertIs<AttemptWriteResult.PracticePositionConflict>(
                    second.exercises.writeAttemptWithPractice(owner.id, stale, wireJson.encodeToString(stale)) { _, _, _, _ ->
                        error("A stale item must not evaluate")
                    },
                )
                assertIs<AttemptWriteResult.Existing>(
                    second.exercises.writeAttemptWithPractice(owner.id, unknown, wireJson.encodeToString(unknown)) { _, _, _, _ ->
                        error("An exact replay must return before the position guard")
                    },
                )
            }
            Unit
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun `inanimate reference practice persists replay and owner isolation across postgres stores`() = runBlocking {
        val config = PostgresTestDatabase.config()
        migrate(config)
        val first = PostgresStores(Database.connect(config), wireJson)
        val second = PostgresStores(Database.connect(config), wireJson)
        try {
            withTimeout(TIMEOUT_MILLIS) {
                val owner = first.sessions.createSession("b".repeat(64), Long.MAX_VALUE)
                val otherOwner = first.sessions.createSession("c".repeat(64), Long.MAX_VALUE)
                val request = PracticeSessionRequest(
                    UUID.randomUUID().toString(),
                    "module-one-things",
                    "module-one-things-1",
                    7,
                )
                val practice = assertIs<PracticeCreateResult.Created>(
                    first.practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) {
                        PracticePlanner(PilotEngine()).plan(request.focusId, request.planRevision, request.seed)
                            .toNewPracticeSession(owner.id)
                    },
                ).session
                assertEquals("pilot-9", practice.contentRevision)
                assertEquals(4, practice.items.size)
                assertEquals(null, second.practice.findPracticeSession(otherOwner.id, practice.id))
                assertIs<PracticeMutationResult.Success>(
                    second.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("introduction")),
                )

                val exercise = practice.items.first().exercise
                val submitted = attempt(exercise.id, "den")
                val requestPayload = wireJson.encodeToString(submitted)
                val created = assertIs<AttemptWriteResult.Created>(
                    first.exercises.writeAttemptWithPractice(owner.id, submitted, requestPayload) { stored, retry, now, source ->
                        CreatedAttempt(
                            response(
                                submitted,
                                stored.id,
                                retry,
                                now,
                                "met",
                                "acceptable",
                                "met",
                                evaluationRevision = "evaluation-9",
                            ),
                            assertNotNull(source).toAttemptContext(),
                        )
                    },
                )
                assertEquals("evaluation-9", created.response.evaluationRevision)
                assertIs<AttemptWriteResult.Existing>(
                    second.exercises.writeAttemptWithPractice(owner.id, submitted, requestPayload) { _, _, _, _ ->
                        error("An exact replay must not evaluate")
                    },
                )
                val reloaded = assertNotNull(second.practice.findPracticeSession(owner.id, practice.id))
                assertEquals("den", (reloaded.items.first().attempts.single().answer as AttemptAnswerDto.Text).text)
                assertTrue(reloaded.items.first().attempts.single().context.introductionOpened)
                assertIs<PracticeMutationResult.Success>(
                    second.practice.advancePracticeSession(owner.id, practice.id, 0),
                )
            }
            Unit
        } finally {
            first.close()
            second.close()
        }
    }

    @Test
    fun `module one owner expires at the fixed boundary without renewal or reassociation`() = testApplication {
        val now = AtomicLong(1_000)
        application { astridModule(testConfig(), InMemoryStores(clock = now::get), clock = now::get) }
        val auth = authenticate()
        val practiceId = UUID.randomUUID().toString()
        val profile = PROFILE_CASES.first()
        val created = create(auth, practiceId, profile)
        assertEquals(HttpStatusCode.Created, created.first)
        val exerciseId = created.second.getValue("currentExercise").jsonObject.string("id")

        now.set(31_000)
        val halfway = client.get("/api/v1/practice-sessions/$practiceId") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }
        assertEquals(HttpStatusCode.OK, halfway.status)
        assertEquals(null, halfway.headers[HttpHeaders.SetCookie])

        now.set(61_000)
        val expiredResponses = listOf(
            client.get("/api/v1/practice-sessions/$practiceId") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
            },
            client.post("/api/v1/practice-sessions/$practiceId/support") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
                mutationHeaders()
                header("X-Astrid-Practice-Owner", auth.ownerId)
                setBody("""{"kind":"introduction"}""")
            },
            client.post("/api/v1/attempts") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
                mutationHeaders()
                setBody(wireJson.encodeToString(attempt(UUID.fromString(exerciseId), "jeg")))
            },
            client.post("/api/v1/practice-sessions/$practiceId/advance") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
                mutationHeaders()
                header("X-Astrid-Practice-Owner", auth.ownerId)
                setBody("""{"position":0}""")
            },
        )
        expiredResponses.forEach { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals(null, response.headers[HttpHeaders.SetCookie])
        }

        val renewed = authenticate()
        assertTrue(renewed.ownerId != auth.ownerId)
        val inaccessible = client.get("/api/v1/practice-sessions/$practiceId") {
            trustedHost()
            cookie("astrid_session", renewed.cookie)
        }
        assertEquals(HttpStatusCode.NotFound, inaccessible.status)
        val reassociation = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie("astrid_session", renewed.cookie)
            mutationHeaders()
            header("X-Astrid-Practice-Owner", auth.ownerId)
            setBody(
                """{"practiceSessionId":"$practiceId","focusId":"${profile.focusId}","planRevision":"${profile.planRevision}","seed":0}""",
            )
        }
        assertEquals(HttpStatusCode.Conflict, reassociation.status)
        assertEquals(null, reassociation.headers[HttpHeaders.SetCookie])
    }

    @Test
    fun `module one summaries freeze literal non-success outcomes for all scoped operations`() = testApplication {
        application { astridModule(testConfig(), InMemoryStores()) }
        val auth = authenticate()
        val observedKeys = mutableSetOf<String>()
        PROFILE_CASES.forEach { profile ->
            val practiceId = UUID.randomUUID().toString()
            var state = create(auth, practiceId, profile).also { created ->
                assertEquals(HttpStatusCode.Created, created.first)
            }.second
            state = support(auth, practiceId)
            val expectedRaw = mutableMapOf<Int, Pair<String, String>>()
            while (state.string("state") != "completed") {
                val position = state.int("cursor")
                val exercise = state.getValue("currentExercise").jsonObject
                val correct = correctAnswer(exercise)
                val attempts = profile.attempts(position, correct, exercise.getValue("prompt").jsonObject)
                attempts.forEach { expected ->
                    val result = submit(auth.cookie, exercise.string("id"), expected.raw)
                    expected.assertResult(result)
                }
                expectedRaw[position] = attempts.first().raw to attempts.last().raw
                state = advance(auth, practiceId, position)
            }

            assertEquals(profile.itemCount, state.getValue("summary").jsonObject.int("completedItems"))
            val rows = state.getValue("summary").jsonObject.getValue("rows").jsonArray.map { it.jsonObject }
            assertEquals(profile.expectedRows, rows.map(::summaryRow))
            rows.forEach { observedKeys += targetKey(it) }
            val items = state.getValue("items").jsonArray.map { it.jsonObject }
            expectedRaw.forEach { (position, raw) ->
                assertEquals(raw.first, items[position].getValue("firstAnswer").jsonObject.string("text"))
                assertEquals(raw.second, items[position].getValue("lastAnswer").jsonObject.string("text"))
            }
        }
        assertEquals(
            setOf(
                "grammar:G003:human-singular:selection",
                "grammar:G003:human-singular:recall",
                "grammar:G003:human-group:selection",
                "grammar:G003:human-group:recall",
                "grammar:G004:vaere-present:selection",
                "grammar:G004:vaere-present:recall",
                "grammar:G038:inanimate-singular-subject:selection",
                "grammar:G038:inanimate-singular-subject:recall",
            ),
            observedKeys,
        )
    }

    private suspend fun ApplicationTestBuilder.authenticate(): Auth {
        val response = client.post("/api/v1/development-session") {
            trustedHost()
            mutationHeaders()
            setBody("{}")
        }
        val cookie = assertNotNull(response.headers[HttpHeaders.SetCookie])
            .substringAfter("astrid_session=").substringBefore(';')
        val ownerResponse = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        val owner = parse(ownerResponse.bodyAsText()).string("ownerSessionId")
        return Auth(cookie, owner)
    }

    private suspend fun ApplicationTestBuilder.create(auth: Auth, id: String, profile: ProfileCase): Pair<HttpStatusCode, JsonObject> {
        val response = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
            mutationHeaders()
            header("X-Astrid-Practice-Owner", auth.ownerId)
            setBody(
                """{"practiceSessionId":"$id","focusId":"${profile.focusId}","planRevision":"${profile.planRevision}","seed":0}""",
            )
        }
        return response.status to parse(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.support(auth: Auth, id: String): JsonObject =
        client.post("/api/v1/practice-sessions/$id/support") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
            mutationHeaders()
            header("X-Astrid-Practice-Owner", auth.ownerId)
            setBody("""{"kind":"introduction"}""")
        }.also { assertEquals(HttpStatusCode.OK, it.status) }.let { parse(it.bodyAsText()) }

    private suspend fun ApplicationTestBuilder.advance(auth: Auth, id: String, position: Int): JsonObject =
        client.post("/api/v1/practice-sessions/$id/advance") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
            mutationHeaders()
            header("X-Astrid-Practice-Owner", auth.ownerId)
            setBody("""{"position":$position}""")
        }.also { assertEquals(HttpStatusCode.OK, it.status) }.let { parse(it.bodyAsText()) }

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, exerciseId: String, answer: String): JsonObject =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie("astrid_session", cookieValue)
            mutationHeaders()
            setBody(wireJson.encodeToString(attempt(UUID.fromString(exerciseId), answer)))
        }.also { assertEquals(HttpStatusCode.OK, it.status) }.let { parse(it.bodyAsText()) }

    private fun response(
        request: AttemptRequest,
        exerciseId: UUID,
        retry: Int,
        now: Long,
        target: String,
        language: String,
        constraint: String,
        issues: List<IssueDto> = emptyList(),
        evaluationRevision: String = "evaluation-8",
    ) = AttemptResponse(
        request.attemptId,
        exerciseId.toString(),
        target,
        language,
        issues,
        "Saved feedback",
        "",
        evaluationRevision,
        false,
        retry,
        now,
        responseConstraintStatus = constraint,
    )

    private fun attempt(exerciseId: UUID, answer: String) = AttemptRequest(
        UUID.randomUUID().toString(),
        exerciseId.toString(),
        AttemptAnswerDto.Text(answer),
        false,
    )

    private suspend fun awaitBlockedSessionLocks(expected: Int) {
        repeat(LOCK_WAIT_POLLS) {
            val blocked = PostgresTestDatabase.connect().use { connection ->
                connection.prepareStatement(
                    """SELECT COUNT(*)
                        FROM pg_stat_activity
                        WHERE datname = current_database()
                          AND pid <> pg_backend_pid()
                          AND wait_event_type = 'Lock'
                          AND query ILIKE '%development_sessions%'
                    """.trimIndent(),
                ).use { statement ->
                    statement.executeQuery().use { result ->
                        assertTrue(result.next())
                        result.getInt(1) >= expected
                    }
                }
            }
            if (blocked) return
            delay(LOCK_WAIT_POLL_MILLIS)
        }
        error("$expected operations never became database-visible as blocked on the owner-session lock")
    }

    private fun correctAnswer(exercise: JsonObject): String = when (exercise.string("targetScopeId")) {
        "vaere-present" -> "er"
        "inanimate-singular-subject" -> if (exercise.string("context").contains("common gender")) "den" else "det"
        else -> SUBJECTS.getValue(exercise.getValue("prompt").jsonObject.string("cue"))
    }

    private fun summaryRow(row: JsonObject) = ExpectedRow(
        targetKey(row),
        row.int("plannedItems"),
        row.int("attemptedItems"),
        row.int("advancedItems"),
        row.int("attemptCount"),
        row.int("retryCount"),
        outcomes(row.getValue("firstOutcomes").jsonObject),
        outcomes(row.getValue("latestOutcomes").jsonObject),
    )

    private fun outcomes(value: JsonObject) = Outcomes(
        value.int("fullyMet"),
        value.int("targetMet"),
        value.int("targetNotMet"),
        value.int("unevaluable"),
        value.int("languageUnknown"),
        value.int("languageKnownIssue"),
        value.int("responseConstraintNotMet"),
    )

    private fun targetKey(row: JsonObject) = buildString {
        append("grammar:")
        append(row.string("targetSkillId"))
        append(':')
        append(row.string("targetScopeId"))
        append(':')
        append(row.string("assessmentMode"))
    }

    private fun parse(text: String) = wireJson.parseToJsonElement(text).jsonObject

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() = header(HttpHeaders.Host, "localhost:8088")

    private fun io.ktor.client.request.HttpRequestBuilder.mutationHeaders() {
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
    }

    private fun testConfig() = ServerConfig(
        developmentMode = true,
        inspectorEnabled = false,
        inspectorKey = null,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = setOf("http://localhost:8088"),
        allowedHosts = setOf("localhost:8088"),
        sessionTtlMillis = 60_000,
        webStaticDir = null,
        jdbcUrl = "jdbc:postgresql://unused",
        r2dbcUrl = "r2dbc:postgresql://unused",
        postgresUser = "unused",
        postgresPassword = "unused",
        poolMaxSize = 2,
    )

    private data class Auth(val cookie: String, val ownerId: String)

    private data class Outcomes(
        val fullyMet: Int,
        val targetMet: Int,
        val targetNotMet: Int,
        val unevaluable: Int,
        val languageUnknown: Int,
        val languageKnownIssue: Int,
        val responseConstraintNotMet: Int,
    )

    private data class ExpectedRow(
        val key: String,
        val plannedItems: Int,
        val attemptedItems: Int,
        val advancedItems: Int,
        val attemptCount: Int,
        val retryCount: Int,
        val first: Outcomes,
        val latest: Outcomes,
    )

    private data class ProfileCase(
        val focusId: String,
        val planRevision: String,
        val attempts: (Int, String, JsonObject) -> List<ExpectedAttempt>,
        val expectedRows: List<ExpectedRow>,
        val itemCount: Int = 6,
    )

    private data class ExpectedAttempt(
        val raw: String,
        val target: String,
        val language: String,
        val constraint: String,
        val issueCodes: List<String> = emptyList(),
    ) {
        fun assertResult(result: JsonObject) {
            assertEquals(target, result.string("targetSatisfaction"), raw)
            assertEquals(language, result.string("linguisticStatus"), raw)
            assertEquals(constraint, result.string("responseConstraintStatus"), raw)
            assertEquals(issueCodes, result.issueCodes(), raw)
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val LOCK_WAIT_POLLS = 200
        const val LOCK_WAIT_POLL_MILLIS = 10L
        val SUBJECTS = mapOf(
            "I" to "jeg",
            "you (one person)" to "du",
            "he" to "han",
            "she" to "hun",
            "we" to "vi",
            "you (several people)" to "I",
            "they (several people)" to "de",
        )
        val PROFILE_CASES = listOf(
            ProfileCase(
                "module-one-singular",
                "module-one-singular-1",
                attempts = { position, correct, prompt ->
                    when (position) {
                        0 -> listOf(
                            ExpectedAttempt(
                                prompt.getValue("choices").jsonArray.map { it.jsonPrimitive.content }
                                    .first { it != correct },
                                "not-met",
                                "acceptable",
                                "met",
                                listOf("INTRODUCED_REFERENCE_REQUIRED"),
                            ),
                        )
                        4 -> listOf(
                            ExpectedAttempt(
                                "ukendt", "unevaluable", "unknown", "met",
                                listOf("UNRECOGNIZED_SUBJECT_WORD"),
                            ),
                            correct(correct),
                        )
                        5 -> listOf(
                            ExpectedAttempt(
                                correct.replaceFirstChar { it.uppercase() },
                                "met",
                                "known-issue",
                                "not-met",
                                listOf("STANDALONE_PRONOUN_CASE_REQUIRED"),
                            ),
                        )
                        else -> listOf(correct(correct))
                    }
                },
                expectedRows = listOf(
                    ExpectedRow(
                        "grammar:G003:human-singular:selection",
                        4, 4, 4, 4, 0,
                        Outcomes(3, 3, 1, 0, 0, 0, 0),
                        Outcomes(3, 3, 1, 0, 0, 0, 0),
                    ),
                    ExpectedRow(
                        "grammar:G003:human-singular:recall",
                        2, 2, 2, 3, 1,
                        Outcomes(0, 1, 0, 1, 1, 1, 1),
                        Outcomes(1, 2, 0, 0, 0, 1, 1),
                    ),
                ),
            ),
            ProfileCase(
                "module-one-groups",
                "module-one-groups-1",
                attempts = { position, correct, prompt ->
                    when {
                        position == 0 -> listOf(
                            ExpectedAttempt(
                                prompt.getValue("choices").jsonArray.map { it.jsonPrimitive.content }
                                    .first { it != correct },
                                "not-met",
                                "acceptable",
                                "met",
                                listOf("INTRODUCED_REFERENCE_REQUIRED"),
                            ),
                        )
                        position >= 3 && correct == "vi" -> listOf(
                            ExpectedAttempt(
                                "Vi", "met", "known-issue", "not-met",
                                listOf("STANDALONE_PRONOUN_CASE_REQUIRED"),
                            ),
                        )
                        position >= 3 && correct == "I" -> listOf(
                            ExpectedAttempt(
                                "ukendt", "unevaluable", "unknown", "met",
                                listOf("UNRECOGNIZED_SUBJECT_WORD"),
                            ),
                            correct(correct),
                        )
                        position >= 3 && correct == "de" -> listOf(
                            ExpectedAttempt(
                                "de sover", "unevaluable", "known-issue", "not-met",
                                listOf("ONE_WORD_RESPONSE_REQUIRED"),
                            ),
                        )
                        else -> listOf(correct(correct))
                    }
                },
                expectedRows = listOf(
                    ExpectedRow(
                        "grammar:G003:human-group:selection",
                        3, 3, 3, 3, 0,
                        Outcomes(2, 2, 1, 0, 0, 0, 0),
                        Outcomes(2, 2, 1, 0, 0, 0, 0),
                    ),
                    ExpectedRow(
                        "grammar:G003:human-group:recall",
                        3, 3, 3, 4, 1,
                        Outcomes(0, 1, 0, 2, 1, 2, 2),
                        Outcomes(1, 2, 0, 1, 0, 2, 2),
                    ),
                ),
            ),
            ProfileCase(
                "module-one-er",
                "module-one-er-1",
                attempts = { position, correct, _ ->
                    when (position) {
                        0 -> listOf(
                            ExpectedAttempt(
                                "være", "not-met", "known-issue", "met", listOf("INFINITIVE_USED"),
                            ),
                        )
                        2 -> listOf(
                            ExpectedAttempt(
                                "Er", "not-met", "known-issue", "not-met",
                                listOf("PRESENT_FORM_CASE_REQUIRED"),
                            ),
                        )
                        3 -> listOf(
                            ExpectedAttempt(
                                "var", "unevaluable", "unknown", "met",
                                listOf("UNRECOGNIZED_PRESENT_FORM"),
                            ),
                            correct(correct),
                        )
                        4 -> listOf(
                            ExpectedAttempt(
                                "vi er", "unevaluable", "known-issue", "not-met",
                                listOf("ONE_WORD_RESPONSE_REQUIRED"),
                            ),
                        )
                        else -> listOf(correct(correct))
                    }
                },
                expectedRows = listOf(
                    ExpectedRow(
                        "grammar:G004:vaere-present:selection",
                        2, 2, 2, 2, 0,
                        Outcomes(1, 1, 1, 0, 0, 1, 0),
                        Outcomes(1, 1, 1, 0, 0, 1, 0),
                    ),
                    ExpectedRow(
                        "grammar:G004:vaere-present:recall",
                        4, 4, 4, 5, 1,
                        Outcomes(1, 1, 1, 2, 1, 2, 2),
                        Outcomes(2, 2, 1, 1, 0, 2, 2),
                    ),
                ),
            ),
            ProfileCase(
                "module-one-things",
                "module-one-things-1",
                attempts = { position, correct, prompt ->
                    when (position) {
                        0 -> listOf(
                            ExpectedAttempt(
                                prompt.getValue("choices").jsonArray.map { it.jsonPrimitive.content }
                                    .first { it != correct },
                                "not-met",
                                "acceptable",
                                "met",
                                listOf("INTRODUCED_REFERENCE_REQUIRED"),
                            ),
                        )
                        2 -> listOf(
                            ExpectedAttempt(
                                correct.replaceFirstChar { it.uppercase() },
                                "met",
                                "known-issue",
                                "not-met",
                                listOf("STANDALONE_PRONOUN_CASE_REQUIRED"),
                            ),
                        )
                        3 -> listOf(
                            ExpectedAttempt(
                                "dem",
                                "unevaluable",
                                "unknown",
                                "met",
                                listOf("UNRECOGNIZED_REFERENCE_WORD"),
                            ),
                            correct(correct),
                        )
                        else -> listOf(correct(correct))
                    }
                },
                expectedRows = listOf(
                    ExpectedRow(
                        "grammar:G038:inanimate-singular-subject:selection",
                        2, 2, 2, 2, 0,
                        Outcomes(1, 1, 1, 0, 0, 0, 0),
                        Outcomes(1, 1, 1, 0, 0, 0, 0),
                    ),
                    ExpectedRow(
                        "grammar:G038:inanimate-singular-subject:recall",
                        2, 2, 2, 3, 1,
                        Outcomes(0, 1, 0, 1, 1, 1, 1),
                        Outcomes(1, 2, 0, 0, 0, 1, 1),
                    ),
                ),
                itemCount = 4,
            ),
        )

        private fun correct(raw: String) = ExpectedAttempt(raw, "met", "acceptable", "met")
    }
}

private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content
private fun JsonObject.int(name: String) = string(name).toInt()
private fun JsonObject.issueCodes() = getValue("issues").jsonArray.map { it.jsonObject.string("code") }
