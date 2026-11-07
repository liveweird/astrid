package app.astrid.server

import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.db.MIGRATION_CONNECT_TIMEOUT_SECONDS
import app.astrid.server.infra.db.MIGRATION_LOGIN_TIMEOUT_SECONDS
import app.astrid.server.infra.db.buildMigrationDataSource
import app.astrid.server.infra.db.migrate
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.flywaydb.core.api.FlywayException

class DatabaseBootstrapTest {
    @Test
    fun `migration data source overrides url timeouts and retains other options`() {
        val dataSource = buildMigrationDataSource(
            config(
                "jdbc:postgresql://db.example:5432/astrid" +
                    "?connectTimeout=99&loginTimeout=99&sslmode=require&ApplicationName=astrid-migration",
            ),
        )

        assertEquals(MIGRATION_CONNECT_TIMEOUT_SECONDS, dataSource.connectTimeout)
        assertEquals(MIGRATION_LOGIN_TIMEOUT_SECONDS, dataSource.loginTimeout)
        assertEquals("astrid-user", dataSource.user)
        assertEquals("astrid-password", dataSource.password)
        assertTrue("sslmode=require" in dataSource.getURL())
        assertTrue("ApplicationName=astrid-migration" in dataSource.getURL())
    }

    @Test
    fun `migration login deadline bounds a peer that never completes postgres handshake`() {
        val listener = ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"))
        val releasePeer = CountDownLatch(1)
        val peerExecutor = Executors.newSingleThreadExecutor()
        val migrationExecutor = Executors.newSingleThreadExecutor()
        val peer = peerExecutor.submit {
            listener.accept().use { socket ->
                releasePeer.await(MIGRATION_TEST_DEADLINE_SECONDS, TimeUnit.SECONDS)
            }
        }
        val migration = migrationExecutor.submit<Unit> {
            migrate(
                config(
                    "jdbc:postgresql://127.0.0.1:${listener.localPort}/astrid" +
                        "?connectTimeout=99&loginTimeout=99&sslmode=disable",
                    retries = 0,
                ),
            )
        }

        try {
            val failure = assertFailsWith<java.util.concurrent.ExecutionException> {
                migration.get(MIGRATION_TEST_DEADLINE_SECONDS, TimeUnit.SECONDS)
            }
            assertTrue(failure.cause is FlywayException, "Expected Flyway failure, got ${failure.cause}")
        } catch (timeout: TimeoutException) {
            throw AssertionError("Migration exceeded its bounded login deadline", timeout)
        } finally {
            releasePeer.countDown()
            listener.close()
            migration.cancel(true)
            peer.cancel(true)
            migrationExecutor.shutdownNow()
            peerExecutor.shutdownNow()
        }
    }

    private fun config(jdbcUrl: String, retries: Int = 10) = ServerConfig(
        developmentMode = true,
        inspectorEnabled = false,
        inspectorKey = null,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = emptySet(),
        allowedHosts = emptySet(),
        sessionTtlMillis = 60_000,
        webStaticDir = null,
        jdbcUrl = jdbcUrl,
        r2dbcUrl = "r2dbc:postgresql://unused",
        postgresUser = "astrid-user",
        postgresPassword = "astrid-password",
        poolMaxSize = 2,
        connectRetries = retries,
    )

    private companion object {
        const val MIGRATION_TEST_DEADLINE_SECONDS = 12L
    }
}
