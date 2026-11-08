package app.astrid.server

import app.astrid.server.infra.config.ServerConfig
import java.sql.Connection
import java.sql.DriverManager
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * The one PostgreSQL container of the server test JVM, started on first use; every Postgres-backed test class shares it (as
 * the sibling repos do) instead of paying for its own. Testcontainers' reaper (Ryuk) removes it when the test JVM exits.
 * Tests run `migrate` themselves, so a test can still create a scratch database and stop at an older migration.
 */
object PostgresTestDatabase {
    /** Tag + digest; must equal the compose and k8s pins (`scripts/check-toolchain.py`). */
    private const val IMAGE =
        "postgres:18.6-alpine@sha256:10a5c69a83035ab5239f5620d708520e8b28f5aa1f2b20143e6024532edadfb5"
    private const val DATABASE = "astrid_test"
    private const val POSTGRES_PORT = 5432

    private val container: PostgreSQLContainer by lazy {
        PostgreSQLContainer(DockerImageName.parse(IMAGE).asCompatibleSubstituteFor("postgres")).apply {
            withDatabaseName(DATABASE)
            withUsername("astrid")
            withPassword("astrid")
            start()
        }
    }

    /** Server configuration pointing at [databaseName], or at the default test database when null. */
    fun config(databaseName: String? = null): ServerConfig = with(container) {
        ServerConfig(
            true, false, null, false, false, emptySet(), emptySet(), 60_000, null,
            databaseName?.let { "jdbc:postgresql://$host:${getMappedPort(POSTGRES_PORT)}/$it" } ?: jdbcUrl,
            "r2dbc:postgresql://$host:${getMappedPort(POSTGRES_PORT)}/${databaseName ?: DATABASE}", username, password, 4,
        )
    }

    /** A raw JDBC connection to the default test database, for fixture setup the stores cannot express. */
    fun connect(): Connection = DriverManager.getConnection(container.jdbcUrl, container.username, container.password)

    fun createDatabase(databaseName: String) {
        connect().use { connection ->
            connection.createStatement().use { statement -> statement.executeUpdate("CREATE DATABASE $databaseName") }
        }
    }
}
