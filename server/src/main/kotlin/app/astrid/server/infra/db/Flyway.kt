package app.astrid.server.infra.db

import app.astrid.server.infra.config.ServerConfig
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource

internal fun migrate(config: ServerConfig) {
    Flyway.configure()
        .dataSource(buildMigrationDataSource(config))
        .locations("classpath:db/migration")
        .connectRetries(config.connectRetries)
        .connectRetriesInterval(CONNECT_RETRY_MAX_INTERVAL_SECONDS)
        .load()
        .migrate()
}

internal const val CONNECT_RETRY_MAX_INTERVAL_SECONDS = 8
internal const val MIGRATION_CONNECT_TIMEOUT_SECONDS = 3
internal const val MIGRATION_LOGIN_TIMEOUT_SECONDS = 5

internal fun buildMigrationDataSource(config: ServerConfig) = PGSimpleDataSource().apply {
    // URL first: pgjdbc URL properties override separately supplied properties. These setters intentionally
    // follow it so bootstrap cannot inherit an unbounded/operator-expanded timeout while all other URL options remain.
    setURL(config.jdbcUrl)
    user = config.postgresUser
    password = config.postgresPassword
    connectTimeout = MIGRATION_CONNECT_TIMEOUT_SECONDS
    loginTimeout = MIGRATION_LOGIN_TIMEOUT_SECONDS
}
