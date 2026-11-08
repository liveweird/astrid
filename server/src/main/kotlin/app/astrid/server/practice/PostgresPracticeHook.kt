package app.astrid.server.practice

import app.astrid.server.exercises.AttemptPracticeHook
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.Attempts
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.exercises.PracticeAttemptSource
import app.astrid.server.exercises.PracticeBinding
import app.astrid.server.exercises.PracticeExposureState
import app.astrid.server.exercises.frozenFeedbackExposureKeys
import java.util.UUID
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll

/**
 * The practice side of the attempt write. Runs only inside `PostgresExerciseStore.writeAttemptWithPractice`'s transaction
 * (see [AttemptPracticeHook]), after that transaction has locked the owning session row; it never opens one of its own.
 */
class PostgresPracticeHook(private val json: Json) : AttemptPracticeHook {
    override suspend fun bind(sessionId: UUID, exerciseId: UUID): PracticeBinding {
        val practiceItem = PracticeItems.selectAll().where {
            (PracticeItems.ownerSessionId eq sessionId) and (PracticeItems.exerciseId eq exerciseId)
        }.toList().singleOrNull() ?: return PracticeBinding.Unbound
        val practiceId = practiceItem[PracticeItems.practiceSessionId]
        val practice = PracticeSessions.selectAll().where {
            (PracticeSessions.ownerSessionId eq sessionId) and
                (PracticeSessions.practiceSessionId eq practiceId)
        }.toList().single()
        return if (practiceItem[PracticeItems.position] != practice[PracticeSessions.cursor]) {
            PracticeBinding.PositionConflict
        } else {
            PracticeBinding.Bound(
                sessionId,
                practiceId,
                practiceItem[PracticeItems.position],
                practiceAttemptSource(sessionId, practiceId, practice),
            )
        }
    }

    override suspend fun record(
        binding: PracticeBinding.Bound,
        attemptId: UUID,
        context: PracticeAttemptContextDto,
        now: Long,
    ) {
        PracticeAttemptContexts.insert {
            it[ownerSessionId] = binding.ownerSessionId
            it[PracticeAttemptContexts.attemptId] = attemptId
            it[practiceSessionId] = binding.practiceSessionId
            it[position] = binding.position
            it[contextPayload] = json.encodeToString(context)
            it[createdAt] = now
        }
    }

    private suspend fun practiceAttemptSource(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        practice: ResultRow,
    ): PracticeAttemptSource {
        val items = PracticeItems.selectAll().where {
            (PracticeItems.ownerSessionId eq ownerSessionId) and
                (PracticeItems.practiceSessionId eq practiceSessionId)
        }.toList().sortedBy { it[PracticeItems.position] }
        val feedbackPositions = PracticeAttemptContexts.select(PracticeAttemptContexts.position).where {
            (PracticeAttemptContexts.ownerSessionId eq ownerSessionId) and
                (PracticeAttemptContexts.practiceSessionId eq practiceSessionId)
        }.toList().map { it[PracticeAttemptContexts.position] }.toSet()
        val targetMetPositions = PracticeAttemptContexts.selectAll().where {
            (PracticeAttemptContexts.ownerSessionId eq ownerSessionId) and
                (PracticeAttemptContexts.practiceSessionId eq practiceSessionId)
        }.toList().mapNotNull { context ->
            val response = Attempts.select(Attempts.responsePayload).where {
                (Attempts.sessionId eq ownerSessionId) and
                    (Attempts.attemptId eq context[PracticeAttemptContexts.attemptId])
            }.toList().singleOrNull()?.let { json.decodeFromString<AttemptResponse>(it[Attempts.responsePayload]) }
            context[PracticeAttemptContexts.position].takeIf { response?.targetSatisfaction == "met" }
        }.toSet()
        val payload: PracticePlanPayload = json.decodeFromString(practice[PracticeSessions.planPayload])
        val metadata = payload.items.associateBy { it.position }
        return PracticeAttemptSource(
            practice[PracticeSessions.focusId],
            practice[PracticeSessions.planRevision],
            practice[PracticeSessions.seed],
            practice[PracticeSessions.cursor],
            practice[PracticeSessions.introductionOpened],
            items.map {
                PracticeExposureState(
                    it[PracticeItems.position],
                    metadata.getValue(it[PracticeItems.position]).answerExposureKeys,
                    metadata.getValue(it[PracticeItems.position]).promptExposureKeys,
                    metadata.getValue(it[PracticeItems.position]).lessonExposureKeys,
                    metadata.getValue(it[PracticeItems.position]).hintExposureKeys,
                    frozenFeedbackExposureKeys(
                        metadata.getValue(it[PracticeItems.position]).feedbackExposureKeys,
                        it[PracticeItems.position] in targetMetPositions,
                    ),
                    it[PracticeItems.lessonOpened],
                    it[PracticeItems.hintOpened],
                    it[PracticeItems.position] in feedbackPositions,
                )
            },
            payload.sourceExposure,
        )
    }
}
