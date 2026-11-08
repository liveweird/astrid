@file:Suppress("MagicNumber") // Bounded pool time settings.

package app.astrid.server.infra.db

import app.astrid.server.infra.config.ServerConfig
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import java.time.Duration
import kotlinx.coroutines.CancellationException
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/** The R2DBC pool and the Exposed database on top of it; every Postgres store shares one instance. */
class Database private constructor(
    val r2dbc: R2dbcDatabase,
    private val pool: ConnectionPool,
) {
    /** Runs [probe] in its own transaction and reports whether it succeeded. */
    @Suppress("TooGenericExceptionCaught") // Any database failure means not ready; coroutine cancellation still propagates.
    suspend fun ready(probe: suspend () -> Unit): Boolean = try {
        suspendTransaction(r2dbc) { probe() }
        true
    } catch (cause: CancellationException) {
        throw cause
    } catch (_: Exception) {
        false
    }

    fun close() = pool.dispose()

    companion object {
        fun connect(config: ServerConfig): Database {
            val options = ConnectionFactoryOptions.parse(config.r2dbcUrl).mutate()
                .option(ConnectionFactoryOptions.USER, config.postgresUser)
                .option(ConnectionFactoryOptions.PASSWORD, config.postgresPassword)
                .build()
            val pool = ConnectionPool(
                ConnectionPoolConfiguration.builder(ConnectionFactories.get(options))
                    .maxSize(config.poolMaxSize)
                    .initialSize(1)
                    .maxAcquireTime(Duration.ofSeconds(10))
                    .maxIdleTime(Duration.ofMinutes(10))
                    .build(),
            )
            val dbConfig = R2dbcDatabaseConfig.Builder().apply {
                connectionFactoryOptions = options
                defaultMaxAttempts = 1
            }
            return Database(R2dbcDatabase.connect(pool, dbConfig), pool)
        }
    }
}
