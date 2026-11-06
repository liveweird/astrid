package app.astrid.server

import org.flywaydb.core.Flyway

internal fun migrate(config: ServerConfig) {
    Flyway.configure()
        .dataSource(config.jdbcUrl, config.postgresUser, config.postgresPassword)
        .locations("classpath:db/migration")
        .load()
        .migrate()
}
