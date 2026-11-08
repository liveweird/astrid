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
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.exercises.StoredExercise
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.db.Database
import app.astrid.server.infra.db.migrate
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.NewPracticeItem
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticeCreateResult
import app.astrid.server.practice.PracticeFollowUpRequest
import app.astrid.server.practice.PracticeMutationResult
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.PracticeSupportRequest
import app.astrid.server.practice.StoredPracticeSession
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString

/**
 * The behaviour every [Stores] implementation shares, run against the in-memory fake the HTTP tests use
 * ([InMemoryStoreContractTest]) and against PostgreSQL ([PostgresStoreContractTest]), so the fake cannot drift from the
 * real stores on the outcomes the routes branch on. Concurrency and rollback stay in `PostgresRepositoryTest`.
 */
abstract class StoreContractTest {
    protected abstract fun withStores(block: suspend (Stores) -> Unit)

    @Test
    fun `a session is found by token hash until it expires`() = withStores { stores ->
        val hash = tokenHash()
        val created = stores.sessions.createSession(hash, EXPIRES_AT)
        assertEquals(EXPIRES_AT, created.expiresAt)
        assertEquals(created, stores.sessions.findSession(hash, EXPIRES_AT - 1))
        assertNull(stores.sessions.findSession(hash, EXPIRES_AT))
        assertNull(stores.sessions.findSession(tokenHash(), 0))
    }

    @Test
    fun `an exercise is visible to its owner only`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val outsider = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val exercise = playgroundExercise(owner.id)
        stores.exercises.insertExercise(exercise)
        assertEquals(exercise, stores.exercises.findExercise(owner.id, exercise.id))
        assertNull(stores.exercises.findExercise(outsider.id, exercise.id))
    }

    @Test
    fun `an attempt replays its stored response and refuses a changed payload`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val exercise = playgroundExercise(owner.id).also { stores.exercises.insertExercise(it) }
        val request = attempt(exercise.id)
        val callbacks = AtomicInteger()
        val created = stores.exercises.writeAttempt(owner.id, request, payload(request)) { stored, retry, now ->
            callbacks.incrementAndGet()
            response(request, stored, retry, now)
        }
        val response = assertIs<AttemptWriteResult.Created>(created).response
        assertEquals(1, response.retryNumber)

        val replay = stores.exercises.writeAttempt(owner.id, request, payload(request)) { _, _, _ ->
            error("a stored attempt must replay without evaluating")
        }
        assertEquals(response, assertIs<AttemptWriteResult.Existing>(replay).response)
        assertEquals(response, stores.exercises.findAttempt(owner.id, UUID.fromString(request.attemptId)))

        val changed = request.copy(answer = AttemptAnswerDto.Text("changed"))
        assertIs<AttemptWriteResult.PayloadConflict>(
            stores.exercises.writeAttempt(owner.id, changed, payload(changed)) { _, _, _ -> error("conflict must not evaluate") },
        )
        val second = attempt(exercise.id)
        val retried = stores.exercises.writeAttempt(owner.id, second, payload(second)) { stored, retry, now ->
            callbacks.incrementAndGet()
            response(second, stored, retry, now)
        }
        assertEquals(2, assertIs<AttemptWriteResult.Created>(retried).response.retryNumber)
        assertEquals(2, callbacks.get())
    }

    @Test
    fun `retries are counted per owner and exercise whatever the id's letter case`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val exercise = playgroundExercise(owner.id).also { stores.exercises.insertExercise(it) }
        val retries = listOf(exercise.id.toString(), exercise.id.toString().uppercase(), exercise.id.toString()).map { id ->
            val request = attempt(exercise.id).copy(exerciseId = id)
            val created = stores.exercises.writeAttempt(owner.id, request, payload(request)) { stored, retry, now ->
                response(request, stored, retry, now)
            }
            assertIs<AttemptWriteResult.Created>(created).response.retryNumber
        }
        assertEquals(listOf(1, 2, 3), retries)
    }

    @Test
    fun `an attempt on an unknown or foreign exercise is missing`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val outsider = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val exercise = playgroundExercise(owner.id).also { stores.exercises.insertExercise(it) }
        val unknown = attempt(UUID.randomUUID())
        assertIs<AttemptWriteResult.ExerciseMissing>(
            stores.exercises.writeAttempt(owner.id, unknown, payload(unknown)) { _, _, _ -> error("missing must not evaluate") },
        )
        val foreign = attempt(exercise.id)
        assertIs<AttemptWriteResult.ExerciseMissing>(
            stores.exercises.writeAttempt(outsider.id, foreign, payload(foreign)) { _, _, _ -> error("foreign must not evaluate") },
        )
        assertNull(stores.exercises.findAttempt(outsider.id, UUID.fromString(foreign.attemptId)))
    }

    @Test
    fun `practice creation replays the same request and refuses a changed one`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val request = practiceRequest()
        val created = assertIs<PracticeCreateResult.Created>(
            stores.practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) { plan(owner, request) },
        ).session
        assertEquals(0, created.cursor)
        assertEquals(PRACTICE_ITEMS, created.items.size)
        created.items.forEach { assertEquals(it.exercise, stores.exercises.findExercise(owner.id, it.exercise.id)) }

        val replay = stores.practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) {
            error("a stored practice request must replay without planning")
        }
        assertEquals(created, assertIs<PracticeCreateResult.Existing>(replay).session)
        val changed = request.copy(seed = request.seed + 1)
        assertIs<PracticeCreateResult.RequestConflict>(
            stores.practice.createPracticeSession(owner, changed, wireJson.encodeToString(changed)) {
                error("a conflicting request must not plan")
            },
        )
        assertEquals(created, stores.practice.findPracticeSession(owner.id, created.id))
        val outsider = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        assertNull(stores.practice.findPracticeSession(outsider.id, created.id))
    }

    @Test
    fun `a follow-up needs an existing completed source and a new id`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val source = createPractice(stores, owner)
        val missing = followUpRequest()
        assertIs<PracticeCreateResult.SourceMissing>(
            stores.practice.createPracticeFollowUp(owner, UUID.randomUUID(), missing, wireJson.encodeToString(missing)) {
                error("a missing source must not plan")
            },
        )
        val active = followUpRequest()
        assertIs<PracticeCreateResult.SourceActive>(
            stores.practice.createPracticeFollowUp(owner, source.id, active, wireJson.encodeToString(active)) {
                error("an active source must not plan")
            },
        )
        val sameId = followUpRequest().copy(practiceSessionId = source.id.toString())
        assertIs<PracticeCreateResult.RequestConflict>(
            stores.practice.createPracticeFollowUp(owner, source.id, sameId, wireJson.encodeToString(sameId)) {
                error("reusing the source id must not plan")
            },
        )
    }

    @Test
    fun `practice attempts bind to the current item and advance moves the cursor`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val practice = createPractice(stores, owner)
        val ahead = attempt(practice.items[1].exercise.id)
        assertIs<AttemptWriteResult.PracticePositionConflict>(
            stores.exercises.writeAttemptWithPractice(owner.id, ahead, payload(ahead)) { _, _, _, _ ->
                error("a later item must not evaluate")
            },
        )
        assertIs<PracticeMutationResult.Conflict>(stores.practice.advancePracticeSession(owner.id, practice.id, 0))

        val current = attempt(practice.items[0].exercise.id)
        val written = stores.exercises.writeAttemptWithPractice(owner.id, current, payload(current)) { stored, retry, now, source ->
            val bound = assertNotNull(source, "a practice item attempt receives its practice state")
            assertEquals(0, bound.position)
            assertEquals(practice.focusId, bound.focusId)
            assertEquals(PRACTICE_ITEMS, bound.items.size)
            CreatedAttempt(response(current, stored, retry, now), context())
        }
        assertIs<AttemptWriteResult.Created>(written)
        val recorded = assertNotNull(stores.practice.findPracticeSession(owner.id, practice.id)).items[0].attempts.single()
        assertEquals(context(), recorded.context)
        assertEquals(current.answer, recorded.answer)

        assertIs<PracticeMutationResult.Conflict>(stores.practice.advancePracticeSession(owner.id, practice.id, 2))
        val advanced = assertIs<PracticeMutationResult.Success>(stores.practice.advancePracticeSession(owner.id, practice.id, 0))
        assertEquals(1, advanced.session.cursor)
        val replayed = assertIs<PracticeMutationResult.Success>(stores.practice.advancePracticeSession(owner.id, practice.id, 0))
        assertEquals(1, replayed.session.cursor)
        assertIs<PracticeMutationResult.Conflict>(stores.practice.advancePracticeSession(owner.id, practice.id, 1))

        val stale = attempt(practice.items[0].exercise.id)
        assertIs<AttemptWriteResult.PracticePositionConflict>(
            stores.exercises.writeAttemptWithPractice(owner.id, stale, payload(stale)) { _, _, _, _ ->
                error("a passed item must not evaluate")
            },
        )
    }

    @Test
    fun `support opens reached items only and unknown runs are missing`() = withStores { stores ->
        val owner = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        val practice = createPractice(stores, owner)
        val introduction = assertIs<PracticeMutationResult.Success>(
            stores.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("introduction")),
        )
        assertTrue(introduction.session.introductionOpened)
        val lesson = assertIs<PracticeMutationResult.Success>(
            stores.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("lesson", 0)),
        )
        assertTrue(lesson.session.items[0].lessonOpened)
        val hint = assertIs<PracticeMutationResult.Success>(
            stores.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("hint", 0)),
        )
        assertTrue(hint.session.items[0].hintOpened)
        assertIs<PracticeMutationResult.Conflict>(
            stores.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("hint", 1)),
        )
        assertIs<PracticeMutationResult.Conflict>(
            stores.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("lesson")),
        )
        assertIs<PracticeMutationResult.Conflict>(
            stores.practice.openPracticeSupport(owner.id, practice.id, PracticeSupportRequest("glossary", 0)),
        )

        val unknown = UUID.randomUUID()
        assertIs<PracticeMutationResult.Missing>(
            stores.practice.openPracticeSupport(owner.id, unknown, PracticeSupportRequest("introduction")),
        )
        assertIs<PracticeMutationResult.Missing>(stores.practice.advancePracticeSession(owner.id, unknown, 0))
        val outsider = stores.sessions.createSession(tokenHash(), Long.MAX_VALUE)
        assertIs<PracticeMutationResult.Missing>(stores.practice.advancePracticeSession(outsider.id, practice.id, 0))
    }

    private suspend fun createPractice(stores: Stores, owner: SessionPrincipal): StoredPracticeSession {
        val request = practiceRequest()
        return assertIs<PracticeCreateResult.Created>(
            stores.practice.createPracticeSession(owner, request, wireJson.encodeToString(request)) { plan(owner, request) },
        ).session
    }

    private fun tokenHash() = UUID.randomUUID().toString().replace("-", "").repeat(2)

    private fun playgroundExercise(ownerId: UUID): StoredExercise {
        val id = UUID.randomUUID()
        val generated = PilotEngine().generate(GenerationRequest("E001", "G004", 19, 3))
        return StoredExercise(
            id,
            ownerId,
            generated.formatId,
            generated.targetSkillId,
            generated.reproduction.seed,
            generated.reproduction.index,
            generated.contentRevision,
            generated.generatorRevision,
            generated.toResponse(id.toString()),
            generated.reproductionFingerprint(),
        )
    }

    private fun practiceRequest() = PracticeSessionRequest(UUID.randomUUID().toString(), "reading-basics", "practice-1", 11)

    private fun followUpRequest() =
        PracticeFollowUpRequest(UUID.randomUUID().toString(), "follow-up-1", 5, listOf("grammar:G005"))

    private fun plan(owner: SessionPrincipal, request: PracticeSessionRequest): NewPracticeSession {
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

    private fun attempt(exerciseId: UUID) =
        AttemptRequest(UUID.randomUUID().toString(), exerciseId.toString(), AttemptAnswerDto.Text("læser"), false)

    private fun payload(request: AttemptRequest) = wireJson.encodeToString(AttemptRequest.serializer(), request)

    private fun context() = PracticeAttemptContextDto(false, false, false, emptyList())

    private fun response(request: AttemptRequest, stored: StoredExercise, retry: Int, createdAt: Long) = AttemptResponse(
        request.attemptId,
        stored.id.toString(),
        "met",
        "acceptable",
        emptyList(),
        "Correct",
        "",
        "evaluation-1",
        request.hintUsed,
        retry,
        createdAt,
    )

    private companion object {
        const val EXPIRES_AT = 1_000_000L
        const val PRACTICE_ITEMS = 8
    }
}

class InMemoryStoreContractTest : StoreContractTest() {
    override fun withStores(block: suspend (Stores) -> Unit) = runBlocking { block(InMemoryStores()) }
}

class PostgresStoreContractTest : StoreContractTest() {
    override fun withStores(block: suspend (Stores) -> Unit) = runBlocking {
        val config = PostgresTestDatabase.config()
        migrate(config)
        val stores = PostgresStores(Database.connect(config), wireJson)
        try {
            block(stores)
        } finally {
            stores.close()
        }
    }
}
