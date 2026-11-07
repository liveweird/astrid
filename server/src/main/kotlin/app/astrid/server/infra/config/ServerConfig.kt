@file:Suppress("MagicNumber") // Reviewed security and pool bounds.

package app.astrid.server.infra.config

data class ServerConfig(
    val developmentMode: Boolean,
    val inspectorEnabled: Boolean,
    val inspectorKey: String?,
    val secureCookie: Boolean,
    val trustedForwardedHeaders: Boolean,
    val allowedOrigins: Set<String>,
    val allowedHosts: Set<String>,
    val sessionTtlMillis: Long,
    val webStaticDir: String?,
    val jdbcUrl: String,
    val r2dbcUrl: String,
    val postgresUser: String,
    val postgresPassword: String,
    val poolMaxSize: Int,
    val connectRetries: Int = 10,
) {
    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()): ServerConfig {
            fun flag(name: String, default: Boolean = false) = env[name]?.equals("true", ignoreCase = true) ?: default
            fun list(name: String, defaults: Set<String>) = env[name]
                ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)?.toSet() ?: defaults
            val key = env["ASTRID_INSPECTOR_KEY"]?.takeIf { it.length >= 32 }
            val poolSize = env["POSTGRES_POOL_MAX_SIZE"]?.toIntOrNull() ?: 10
            require(poolSize in 1..100) { "POSTGRES_POOL_MAX_SIZE must be between 1 and 100" }
            val connectRetries = env["POSTGRES_CONNECT_RETRIES"]?.let { raw ->
                requireNotNull(raw.toIntOrNull()) { "POSTGRES_CONNECT_RETRIES must be an integer" }
            } ?: 10
            require(connectRetries in 0..15) { "POSTGRES_CONNECT_RETRIES must be between 0 and 15" }
            return ServerConfig(
                developmentMode = flag("ASTRID_DEVELOPMENT_MODE"),
                inspectorEnabled = flag("ASTRID_INSPECTOR_ENABLED") && key != null,
                inspectorKey = key,
                secureCookie = flag("ASTRID_SECURE_COOKIE"),
                trustedForwardedHeaders = flag("ASTRID_TRUST_FORWARDED_HEADERS"),
                allowedOrigins = list(
                    "ASTRID_ALLOWED_ORIGINS",
                    setOf(
                        "http://localhost:8088",
                        "http://127.0.0.1:8088",
                        "http://localhost:5180",
                        "http://127.0.0.1:5180",
                    ),
                ),
                allowedHosts = list(
                    "ASTRID_ALLOWED_HOSTS",
                    setOf("localhost:8088", "127.0.0.1:8088", "localhost:5180", "127.0.0.1:5180"),
                ),
                sessionTtlMillis = 24L * 60 * 60 * 1000,
                webStaticDir = env["WEB_STATIC_DIR"],
                jdbcUrl = env["POSTGRES_JDBC_URL"] ?: "jdbc:postgresql://localhost:5438/astrid",
                r2dbcUrl = env["POSTGRES_R2DBC_URL"] ?: "r2dbc:postgresql://localhost:5438/astrid",
                postgresUser = env["POSTGRES_USER"] ?: "astrid",
                postgresPassword = env["POSTGRES_PASSWORD"] ?: "astrid",
                poolMaxSize = poolSize,
                connectRetries = connectRetries,
            )
        }
    }
}
