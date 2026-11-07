package app.astrid.server

import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.AttemptWriteResult
import app.astrid.server.exercises.CreatedAttempt
import app.astrid.server.exercises.ExerciseStore
import app.astrid.server.exercises.IntroductionDto
import app.astrid.server.exercises.PracticeAttemptSource
import app.astrid.server.exercises.PracticeCrossRunExposureDto
import app.astrid.server.exercises.PracticeExposureState
import app.astrid.server.exercises.StoredExercise
import app.astrid.server.exercises.frozenFeedbackExposureKeys
import app.astrid.server.plugins.Readiness
import app.astrid.server.practice.NewPracticeItem
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticeCreateResult
import app.astrid.server.practice.PracticeFollowUpLineage
import app.astrid.server.practice.PracticeFollowUpRequest
import app.astrid.server.practice.PracticeMutationResult
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.PracticeStore
import app.astrid.server.practice.PracticeSupportRequest
import app.astrid.server.practice.StoredPracticeAttempt
import app.astrid.server.practice.StoredPracticeItem
import app.astrid.server.practice.StoredPracticeSession
import app.astrid.server.session.SessionStore
import java.util.UUID
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The Docker-free [Stores] for HTTP tests: one mutex-guarded state shared by the three store facets (the attempt write sees
 * practice state directly, standing in for `PostgresPracticeHook`), plus the tamper hooks the regeneration tests need.
 */
class InMemoryStores(private val clock: () -> Long = System::currentTimeMillis) : Stores {
    private val mutex = Mutex()
    private val sessionRecords = mutableMapOf<String, SessionPrincipal>()
    private val exerciseRecords = mutableMapOf<Pair<UUID, UUID>, StoredExercise>()
    private val attempts = mutableMapOf<Pair<UUID, UUID>, Pair<String, AttemptResponse>>()
    private val practiceSessions = mutableMapOf<Pair<UUID, UUID>, MemoryPracticeSession>()
    private val practiceRequests = mutableMapOf<Pair<UUID, UUID>, String>()

    override val sessions: SessionStore = object : SessionStore {
        override suspend fun findSession(tokenHash: String, now: Long) = mutex.withLock {
            sessionRecords[tokenHash]?.takeIf { it.expiresAt > now }
        }

        override suspend fun createSession(tokenHash: String, expiresAt: Long) = mutex.withLock {
            SessionPrincipal(UUID.randomUUID(), expiresAt).also { sessionRecords[tokenHash] = it }
        }
    }

    override val exercises: ExerciseStore = object : ExerciseStore {
        override suspend fun insertExercise(exercise: StoredExercise) = mutex.withLock {
            exerciseRecords[exercise.sessionId to exercise.id] = exercise
        }

        override suspend fun findExercise(sessionId: UUID, exerciseId: UUID) = mutex.withLock {
            exerciseRecords[sessionId to exerciseId]
        }

        override suspend fun writeAttemptWithPractice(
            sessionId: UUID,
            request: AttemptRequest,
            requestPayload: String,
            create: suspend (StoredExercise, Int, Long, PracticeAttemptSource?) -> CreatedAttempt,
        ): AttemptWriteResult = mutex.withLock {
            val attemptId = UUID.fromString(request.attemptId)
            val key = sessionId to attemptId
            attempts[key]?.let { (payload, response) ->
                return@withLock if (payload == requestPayload) AttemptWriteResult.Existing(response)
                else AttemptWriteResult.PayloadConflict
            }
            val exerciseId = UUID.fromString(request.exerciseId)
            val exercise = exerciseRecords[sessionId to exerciseId] ?: return@withLock AttemptWriteResult.ExerciseMissing
            val practice = practiceSessions.values.singleOrNull { candidate ->
                candidate.ownerSessionId == sessionId && candidate.items.any { it.exercise.id == exerciseId }
            }
            val practiceItem = practice?.items?.single { it.exercise.id == exerciseId }
            if (practice != null && practiceItem!!.position != practice.cursor) {
                return@withLock AttemptWriteResult.PracticePositionConflict
            }
            val retry = attempts.values.count { (_, response) -> response.exerciseId == request.exerciseId } + 1
            val source = practice?.let { current ->
                PracticeAttemptSource(
                    current.focusId,
                    current.planRevision,
                    current.seed,
                    checkNotNull(practiceItem).position,
                    current.introductionOpened,
                    current.items.map {
                        PracticeExposureState(
                            it.position,
                            it.answerExposureKeys,
                            it.promptExposureKeys,
                            it.lessonExposureKeys,
                            it.hintExposureKeys,
                            frozenFeedbackExposureKeys(
                                it.feedbackExposureKeys,
                                it.attempts.any { attempt -> attempt.response.targetSatisfaction == "met" },
                            ),
                            it.lessonOpened,
                            it.hintOpened,
                            it.attempts.isNotEmpty(),
                        )
                    },
                    current.sourceExposure,
                )
            }
            val created = create(exercise, retry, clock(), source)
            check((practice == null) == (created.practiceContext == null)) { "Practice attempt context mismatch" }
            attempts[key] = requestPayload to created.response
            if (practiceItem != null) {
                practiceItem.attempts += StoredPracticeAttempt(
                    created.response,
                    checkNotNull(created.practiceContext),
                    request.answer,
                )
            }
            AttemptWriteResult.Created(created.response)
        }

        override suspend fun findAttempt(sessionId: UUID, attemptId: UUID) = mutex.withLock {
            attempts[sessionId to attemptId]?.second
        }
    }

    override val practice: PracticeStore = object : PracticeStore {
        override suspend fun createPracticeSession(
            ownerSession: SessionPrincipal,
            request: PracticeSessionRequest,
            requestPayload: String,
            create: suspend () -> NewPracticeSession,
        ): PracticeCreateResult = mutex.withLock {
            val id = UUID.fromString(request.practiceSessionId)
            val key = ownerSession.id to id
            practiceSessions[key]?.let {
                return@withLock if (practiceRequests.getValue(key) == requestPayload) {
                    PracticeCreateResult.Existing(it.snapshot())
                } else {
                    PracticeCreateResult.RequestConflict
                }
            }
            val plan = create()
            val stored = MemoryPracticeSession(
                id,
                ownerSession.id,
                ownerSession.expiresAt,
                request.focusId,
                plan.title,
                request.planRevision,
                request.seed,
                plan.contentRevision,
                plan.generatorRevision,
                plan.evaluationRevision,
                prerequisiteSkillIds = plan.prerequisiteSkillIds.toList(),
                introduction = plan.introduction.toList(),
                items = plan.items.sortedBy { it.position }.map { MemoryPracticeItem(it) }.toMutableList(),
            )
            plan.items.forEach { exerciseRecords[ownerSession.id to it.exercise.id] = it.exercise }
            practiceRequests[key] = requestPayload
            practiceSessions[key] = stored
            PracticeCreateResult.Created(stored.snapshot())
        }

        override suspend fun createPracticeFollowUp(
            ownerSession: SessionPrincipal,
            sourcePracticeSessionId: UUID,
            request: PracticeFollowUpRequest,
            requestPayload: String,
            create: suspend (StoredPracticeSession) -> NewPracticeSession,
        ): PracticeCreateResult = mutex.withLock {
            val id = UUID.fromString(request.practiceSessionId)
            val key = ownerSession.id to id
            practiceSessions[key]?.let {
                return@withLock if (practiceRequests.getValue(key) == requestPayload) {
                    PracticeCreateResult.Existing(it.snapshot())
                } else {
                    PracticeCreateResult.RequestConflict
                }
            }
            if (id == sourcePracticeSessionId) return@withLock PracticeCreateResult.RequestConflict
            val source = practiceSessions[ownerSession.id to sourcePracticeSessionId]
                ?: return@withLock PracticeCreateResult.SourceMissing
            if (source.cursor != source.items.size) return@withLock PracticeCreateResult.SourceActive
            val plan = create(source.snapshot())
            require(plan.items.isNotEmpty() && plan.items.size <= 8)
            require(plan.items.map { it.position } == plan.items.indices.toList())
            val stored = MemoryPracticeSession(
                id,
                ownerSession.id,
                ownerSession.expiresAt,
                "targeted-review",
                plan.title,
                request.planRevision,
                request.seed,
                plan.contentRevision,
                plan.generatorRevision,
                plan.evaluationRevision,
                prerequisiteSkillIds = plan.prerequisiteSkillIds.toList(),
                introduction = plan.introduction.toList(),
                items = plan.items.sortedBy { it.position }.map { MemoryPracticeItem(it) }.toMutableList(),
                followUp = plan.followUp,
                sourceExposure = plan.sourceExposure.toList(),
            )
            plan.items.forEach { exerciseRecords[ownerSession.id to it.exercise.id] = it.exercise }
            practiceRequests[key] = requestPayload
            practiceSessions[key] = stored
            PracticeCreateResult.Created(stored.snapshot())
        }

        override suspend fun findPracticeSession(ownerSessionId: UUID, practiceSessionId: UUID) = mutex.withLock {
            practiceSessions[ownerSessionId to practiceSessionId]?.snapshot()
        }

        override suspend fun openPracticeSupport(
            ownerSessionId: UUID,
            practiceSessionId: UUID,
            request: PracticeSupportRequest,
        ): PracticeMutationResult = mutex.withLock {
            val stored = practiceSessions[ownerSessionId to practiceSessionId]
                ?: return@withLock PracticeMutationResult.Missing
            when (request.kind) {
                "introduction" -> {
                    if (request.position == null) stored.introductionOpened = true
                    else return@withLock PracticeMutationResult.Conflict
                }
                "lesson", "hint" -> {
                    val position = request.position ?: return@withLock PracticeMutationResult.Conflict
                    if (position !in 0 until stored.items.size || position > stored.cursor) {
                        return@withLock PracticeMutationResult.Conflict
                    }
                    val item = stored.items.single { it.position == position }
                    if (request.kind == "lesson") item.lessonOpened = true else item.hintOpened = true
                }
                else -> return@withLock PracticeMutationResult.Conflict
            }
            PracticeMutationResult.Success(stored.snapshot())
        }

        override suspend fun advancePracticeSession(
            ownerSessionId: UUID,
            practiceSessionId: UUID,
            position: Int,
        ): PracticeMutationResult = mutex.withLock {
            val stored = practiceSessions[ownerSessionId to practiceSessionId]
                ?: return@withLock PracticeMutationResult.Missing
            when {
                position !in stored.items.indices -> PracticeMutationResult.Conflict
                position < stored.cursor -> PracticeMutationResult.Success(stored.snapshot())
                position > stored.cursor -> PracticeMutationResult.Conflict
                stored.items.single { it.position == position }.attempts.isEmpty() -> PracticeMutationResult.Conflict
                else -> {
                    stored.cursor += 1
                    PracticeMutationResult.Success(stored.snapshot())
                }
            }
        }
    }

    override val readiness = Readiness { true }

    suspend fun tamperFingerprint(exerciseId: UUID) = mutex.withLock {
        val entry = exerciseRecords.entries.single { it.key.second == exerciseId }
        exerciseRecords[entry.key] = entry.value.copy(reproductionFingerprint = "0".repeat(64))
    }

    suspend fun tamperRevision(exerciseId: UUID) = mutex.withLock {
        val entry = exerciseRecords.entries.single { it.key.second == exerciseId }
        exerciseRecords[entry.key] = entry.value.copy(
            contentRevision = "retired-content",
            reproductionFingerprint = "0".repeat(64),
        )
    }

    private class MemoryPracticeSession(
        val id: UUID,
        val ownerSessionId: UUID,
        val sessionExpiresAt: Long,
        val focusId: String,
        val title: String,
        val planRevision: String,
        val seed: Int,
        val contentRevision: String,
        val generatorRevision: String,
        val evaluationRevision: String,
        var cursor: Int = 0,
        var introductionOpened: Boolean = false,
        val prerequisiteSkillIds: List<String>,
        val introduction: List<IntroductionDto>,
        val items: MutableList<MemoryPracticeItem>,
        val followUp: PracticeFollowUpLineage? = null,
        val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
    ) {
        fun snapshot() = StoredPracticeSession(
            id, ownerSessionId, sessionExpiresAt, focusId, title, planRevision, seed,
            contentRevision, generatorRevision, evaluationRevision, cursor, introductionOpened,
            prerequisiteSkillIds.toList(), introduction.toList(), items.map(MemoryPracticeItem::snapshot),
            followUp, sourceExposure.toList(),
        )
    }

    private class MemoryPracticeItem(item: NewPracticeItem) {
        val position = item.position
        val exercise = item.exercise
        val targetLabel = item.targetLabel
        val answerExposureKeys = item.answerExposureKeys.toList()
        val promptExposureKeys = item.promptExposureKeys.toList()
        val lessonExposureKeys = item.lessonExposureKeys.toList()
        val hintExposureKeys = item.hintExposureKeys.toList()
        val feedbackExposureKeys = item.feedbackExposureKeys.toList()
        val assessmentMode = item.assessmentMode
        var lessonOpened = false
        var hintOpened = false
        var attempts = mutableListOf<StoredPracticeAttempt>()

        fun snapshot() = StoredPracticeItem(
            position, exercise, targetLabel, answerExposureKeys, promptExposureKeys, lessonExposureKeys, hintExposureKeys,
            feedbackExposureKeys, lessonOpened, hintOpened, attempts.toList(),
            assessmentMode,
        )
    }
}
