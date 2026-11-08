package app.astrid.server

import app.astrid.server.infra.config.ServerConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerConfigTest {
    @Test
    fun `postgres connect retries accept only the bounded integer range`() {
        assertEquals(10, ServerConfig.fromEnvironment(emptyMap()).connectRetries)
        assertEquals(0, ServerConfig.fromEnvironment(mapOf("POSTGRES_CONNECT_RETRIES" to "0")).connectRetries)
        assertEquals(15, ServerConfig.fromEnvironment(mapOf("POSTGRES_CONNECT_RETRIES" to "15")).connectRetries)
        listOf("-1", "16", "not-a-number").forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                ServerConfig.fromEnvironment(mapOf("POSTGRES_CONNECT_RETRIES" to value))
            }
        }
    }

    @Test
    fun `listen address defaults to loopback 8088 and the default allow lists follow the port`() {
        val defaults = ServerConfig.fromEnvironment(emptyMap())
        assertEquals("127.0.0.1", defaults.bindHost)
        assertEquals(8088, defaults.port)
        assertTrue("localhost:8088" in defaults.allowedHosts)
        assertTrue("http://127.0.0.1:8088" in defaults.allowedOrigins)

        val custom = ServerConfig.fromEnvironment(
            mapOf("ASTRID_PORT" to "8089", "ASTRID_BIND_HOST" to "0.0.0.0"),
        )
        assertEquals("0.0.0.0", custom.bindHost)
        assertEquals(8089, custom.port)
        assertTrue("localhost:8089" in custom.allowedHosts)
        assertTrue("127.0.0.1:8089" in custom.allowedHosts)
        assertTrue("http://localhost:8089" in custom.allowedOrigins)
        assertFalse(custom.allowedHosts.any { it.endsWith(":8088") })
    }

    @Test
    fun `explicit allow lists are not rewritten by the port`() {
        val config = ServerConfig.fromEnvironment(
            mapOf(
                "ASTRID_PORT" to "8089",
                "ASTRID_ALLOWED_HOSTS" to "astrid.example",
                "ASTRID_ALLOWED_ORIGINS" to "https://astrid.example",
            ),
        )
        assertEquals(setOf("astrid.example"), config.allowedHosts)
        assertEquals(setOf("https://astrid.example"), config.allowedOrigins)
    }

    @Test
    fun `port accepts only an integer from 1 to 65535`() {
        assertEquals(1, ServerConfig.fromEnvironment(mapOf("ASTRID_PORT" to "1")).port)
        assertEquals(65535, ServerConfig.fromEnvironment(mapOf("ASTRID_PORT" to "65535")).port)
        listOf("0", "-1", "65536", "", "http", "80.5").forEach { value ->
            assertFailsWith<IllegalArgumentException>(value) {
                ServerConfig.fromEnvironment(mapOf("ASTRID_PORT" to value))
            }
        }
    }

    @Test
    fun `blank bind host is rejected`() {
        listOf("", "   ").forEach { value ->
            assertFailsWith<IllegalArgumentException>(value) {
                ServerConfig.fromEnvironment(mapOf("ASTRID_BIND_HOST" to value))
            }
        }
    }
}
