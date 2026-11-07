package app.astrid.server.session

import app.astrid.server.authz.SessionPrincipal

interface SessionStore {
    suspend fun findSession(tokenHash: String, now: Long): SessionPrincipal?
    suspend fun createSession(tokenHash: String, expiresAt: Long): SessionPrincipal
}
