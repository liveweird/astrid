package app.astrid.server

import app.astrid.server.infra.config.ServerConfig
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.slf4j.LoggerFactory

class CallLoggingTest {
    @Test
    fun `api calls log method path and status only and probes and other paths are not logged`() = testApplication {
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
        root.addAppender(appender)
        try {
            application { astridModule(config(), InMemoryStores()) }
            client.get("/api/v1/catalog?token=query-secret") {
                header(HttpHeaders.Host, "localhost:8088")
                cookie("astrid_session", "cookie-secret")
            }
            client.get("/api/v1/health") { header(HttpHeaders.Host, "localhost:8088") }
            client.get("/api/v1/ready") { header(HttpHeaders.Host, "localhost:8088") }
            client.get("/not-api") { header(HttpHeaders.Host, "localhost:8088") }
        } finally {
            root.detachAppender(appender)
        }
        val lines = appender.list.map { it.formattedMessage }
        assertEquals(1, lines.count { it.startsWith("GET /api/v1/catalog ") }, lines.toString())
        assertFalse(lines.any { "query-secret" in it || "cookie-secret" in it || "?" in it })
        assertFalse(lines.any { "/not-api" in it || "/health" in it || "/ready" in it })
    }

    private fun config() = ServerConfig(
        developmentMode = true,
        inspectorEnabled = false,
        inspectorKey = null,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = setOf("http://localhost:8088"),
        allowedHosts = setOf("localhost:8088"),
        sessionTtlMillis = 60_000,
        webStaticDir = null,
        jdbcUrl = "jdbc:postgresql://unused",
        r2dbcUrl = "r2dbc:postgresql://unused",
        postgresUser = "unused",
        postgresPassword = "unused",
        poolMaxSize = 2,
    )
}
