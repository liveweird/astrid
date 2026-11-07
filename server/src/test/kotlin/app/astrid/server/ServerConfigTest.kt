package app.astrid.server

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
