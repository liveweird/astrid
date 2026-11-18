package app.astrid.server.vocabulary

import app.astrid.engine.VocabularyLibrary
import app.astrid.engine.VocabularyLibraryPracticePlanner
import app.astrid.engine.VocabularyTeachingMapper
import app.astrid.server.authz.Authenticator
import app.astrid.server.authz.NotFound
import app.astrid.server.authz.Unauthorized
import app.astrid.server.authz.caller
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.practice.PracticeStore
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

internal fun Route.vocabularyRoutes(
    config: ServerConfig,
    practice: PracticeStore,
    authenticator: Authenticator,
) {
    get("/vocabulary-entries") {
        if (!config.developmentMode) throw NotFound()
        call.respond(VocabularyLibrary.catalog().toResponse())
    }
    get("/vocabulary-context") {
        val owner = call.caller(authenticator)
        val history = practice.savedVocabularyTeaching(owner)
        if (!history.ownerAvailable) throw Unauthorized()
        val catalog = VocabularyLibrary.catalog()
        val claims = VocabularyTeachingMapper.claims(history.witnesses)
        val practiceCounts = VocabularyLibraryPracticePlanner.counts(claims)
        call.respond(
            VocabularyContextResponse(
                catalog.libraryRevision,
                owner.id.toString(),
                owner.expiresAt,
                claims.map { it.toDto() },
                VocabularyLibraryPracticeAvailabilityDto(
                    VocabularyLibraryPracticePlanner.REVISION,
                    practiceCounts.recognitionCount,
                    practiceCounts.recallCount,
                ),
            ),
        )
    }
}
