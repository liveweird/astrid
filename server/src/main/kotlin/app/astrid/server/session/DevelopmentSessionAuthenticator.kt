package app.astrid.server.session

import app.astrid.server.authz.Authenticator
import app.astrid.server.authz.NotFound
import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.authz.Unauthorized
import app.astrid.server.infra.config.ServerConfig
import io.ktor.server.application.ApplicationCall

/**
 * The development-session cookie scheme: the whole API is 404 outside development mode, then 401 without a well-formed,
 * unexpired session token. The only [Authenticator] today; a real login replaces this class, not the routes.
 */
class DevelopmentSessionAuthenticator(
    private val config: ServerConfig,
    private val sessions: SessionStore,
    private val clock: () -> Long,
) : Authenticator {
    override suspend fun authenticate(call: ApplicationCall): SessionPrincipal {
        if (!config.developmentMode) throw NotFound()
        val token = call.request.cookies[SESSION_COOKIE]?.takeIf(::validTokenShape) ?: throw Unauthorized()
        return sessions.findSession(hashToken(token), clock()) ?: throw Unauthorized()
    }
}
