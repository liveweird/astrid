package app.astrid.server.authz

import io.ktor.server.application.ApplicationCall
import io.ktor.util.AttributeKey
import java.util.UUID

/** The authenticated caller: today a development session, identified by its id and bounded by its expiry. */
data class SessionPrincipal(val id: UUID, val expiresAt: Long)

/**
 * Resolves the caller of a request or throws the matching [ApiException] (404 when the mode is off, 401 without a valid
 * credential). One implementation per authentication scheme; routes only ever see [SessionPrincipal].
 */
fun interface Authenticator {
    suspend fun authenticate(call: ApplicationCall): SessionPrincipal
}

private val CallerKey = AttributeKey<SessionPrincipal>("astrid.caller")

/**
 * The caller of this request, authenticated at most once per call. Handlers call it AFTER `requireMutationHeaders`, so a
 * missing client header or a non-JSON body (403/415) wins over a missing session (401).
 */
suspend fun ApplicationCall.caller(authenticator: Authenticator): SessionPrincipal =
    attributes.getOrNull(CallerKey) ?: authenticator.authenticate(this).also { attributes.put(CallerKey, it) }
