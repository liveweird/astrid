package app.astrid.server.session

import app.astrid.server.authz.NotFound
import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.authz.Unauthorized
import app.astrid.server.authz.requireMutationHeaders
import app.astrid.server.infra.config.ServerConfig
import io.ktor.http.Cookie
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

internal const val SESSION_COOKIE = "astrid_session"
private const val TOKEN_BYTES = 32
private val secureRandom = SecureRandom()

internal fun Route.sessionRoutes(config: ServerConfig, sessions: SessionStore, clock: () -> Long) {
    post("/development-session") {
        if (!config.developmentMode) throw NotFound()
        requireMutationHeaders(call)
        call.receive<DevelopmentSessionRequest>()
        val existing = call.request.cookies[SESSION_COOKIE]
            ?.takeIf(::validTokenShape)
            ?.let { sessions.findSession(hashToken(it), clock()) }
        val session = existing ?: newSession(sessions, config, call, clock)
        if (session.expiresAt <= clock()) throw Unauthorized()
        call.respond(DevelopmentSessionResponse(inspectorEnabled = config.inspectorEnabled))
    }
}

private suspend fun newSession(
    sessions: SessionStore,
    config: ServerConfig,
    call: ApplicationCall,
    clock: () -> Long,
): SessionPrincipal {
    val token = ByteArray(TOKEN_BYTES).also(secureRandom::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
    val session = sessions.createSession(hashToken(token), clock() + config.sessionTtlMillis)
    call.response.cookies.append(
        Cookie(SESSION_COOKIE, token, maxAge = (config.sessionTtlMillis / 1000).toInt(), path = "/", secure = config.secureCookie,
            httpOnly = true, extensions = mapOf("SameSite" to "Strict")),
    )
    return session
}

internal fun validTokenShape(value: String) = value.matches(Regex("[A-Za-z0-9_-]{43}"))

internal fun hashToken(token: String): String = MessageDigest.getInstance("SHA-256")
    .digest(token.toByteArray(Charsets.US_ASCII)).joinToString("") { "%02x".format(it) }
