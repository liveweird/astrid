package app.astrid.server.practice

import app.astrid.engine.VocabularyTeachingWitness
import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.exercises.PracticeCrossRunExposureDto
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

data class VocabularyTeachingHistory(
    val witnesses: List<VocabularyTeachingWitness>,
    val sourceExposure: List<PracticeCrossRunExposureDto> = emptyList(),
    val ownerAvailable: Boolean = true,
)

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
    /** Saved introduction witnesses only; implementations must not lock the owner or load attempts/items. */
    suspend fun savedVocabularyTeaching(ownerSession: SessionPrincipal): VocabularyTeachingHistory
    suspend fun createCumulativeReview(
        ownerSession: SessionPrincipal,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend (CumulativeReviewHistory) -> NewPracticeSession?,
    ): PracticeCreateResult
    suspend fun createVocabularyLibraryPractice(
        ownerSession: SessionPrincipal,
        request: PracticeSessionRequest,
        requestPayload: String,
        create: suspend (VocabularyTeachingHistory) -> NewPracticeSession?,
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
