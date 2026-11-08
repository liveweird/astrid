@file:Suppress("MagicNumber") // SQL column widths.

package app.astrid.server.session

import app.astrid.server.authz.SessionPrincipal
import app.astrid.server.infra.db.Database
import java.util.UUID
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

internal object Sessions : Table("development_sessions") {
    val id = javaUUID("id")
    val tokenHash = char("token_hash", 64)
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

class PostgresSessionStore(
    private val database: Database,
    private val clock: () -> Long = System::currentTimeMillis,
) : SessionStore {
    override suspend fun findSession(tokenHash: String, now: Long): SessionPrincipal? = suspendTransaction(database.r2dbc) {
        Sessions.selectAll().where { (Sessions.tokenHash eq tokenHash) and (Sessions.expiresAt greater now) }
            .toList().singleOrNull()?.let { SessionPrincipal(it[Sessions.id], it[Sessions.expiresAt]) }
    }

    override suspend fun createSession(tokenHash: String, expiresAt: Long): SessionPrincipal = suspendTransaction(database.r2dbc) {
        val id = UUID.randomUUID()
        Sessions.insert {
            it[Sessions.id] = id
            it[Sessions.tokenHash] = tokenHash
            it[Sessions.expiresAt] = expiresAt
            it[createdAt] = clock()
        }
        SessionPrincipal(id, expiresAt)
    }

    /** Readiness probe: the session table answers. */
    suspend fun ready(): Boolean = database.ready { Sessions.select(Sessions.id).limit(1).toList() }
}

/**
 * Locks the owning session row (`SELECT ... FOR UPDATE`) inside the CALLER's transaction; every attempt and practice write
 * takes this lock first, so writes of one session serialize. Returns false when the session no longer exists.
 */
internal suspend fun lockSession(sessionId: UUID): Boolean =
    Sessions.select(Sessions.id).where { Sessions.id eq sessionId }.forUpdate().toList().isNotEmpty()
