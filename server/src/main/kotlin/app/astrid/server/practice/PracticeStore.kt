package app.astrid.server.practice

import app.astrid.server.authz.SessionPrincipal
import java.util.UUID

sealed interface PracticeCreateResult {
    data class Created(val session: StoredPracticeSession) : PracticeCreateResult
    data class Existing(val session: StoredPracticeSession) : PracticeCreateResult
    data object RequestConflict : PracticeCreateResult
    data object SourceMissing : PracticeCreateResult
    data object SourceActive : PracticeCreateResult
    data object NoEligibleItems : PracticeCreateResult
    data object OwnerExpired : PracticeCreateResult
}

data class CumulativeReviewHistory(val sessions: List<StoredPracticeSession>, val ownerAvailable: Boolean = true)

sealed interface PracticeMutationResult {
    data class Success(val session: StoredPracticeSession) : PracticeMutationResult
    data object Missing : PracticeMutationResult
    data object Conflict : PracticeMutationResult
}

interface PracticeStore {
    suspend fun createPracticeSession(
        ownerSession: SessionPrincipal,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend () -> NewPracticeSession,
    ): PracticeCreateResult
    suspend fun createPracticeFollowUp(
        ownerSession: SessionPrincipal,
        sourcePracticeSessionId: UUID,
        request: PracticeFollowUpRequest,
        requestPayload: String,
        create: suspend (StoredPracticeSession) -> NewPracticeSession,
    ): PracticeCreateResult
    suspend fun cumulativeReviewHistory(ownerSession: SessionPrincipal): CumulativeReviewHistory
    suspend fun createCumulativeReview(
        ownerSession: SessionPrincipal,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend (CumulativeReviewHistory) -> NewPracticeSession?,
    ): PracticeCreateResult
    suspend fun findPracticeSession(ownerSessionId: UUID, practiceSessionId: UUID): StoredPracticeSession?
    suspend fun openPracticeSupport(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        request: PracticeSupportRequest,
    ): PracticeMutationResult
    suspend fun advancePracticeSession(
        ownerSessionId: UUID,
        practiceSessionId: UUID,
        position: Int,
    ): PracticeMutationResult
}
