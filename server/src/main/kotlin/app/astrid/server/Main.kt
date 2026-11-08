package app.astrid.server

import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.db.Database
import app.astrid.server.infra.db.migrate
import app.astrid.server.infra.json.wireJson
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = ServerConfig.fromEnvironment()
    migrate(config)
    val stores = PostgresStores(Database.connect(config), wireJson)
    Runtime.getRuntime().addShutdownHook(Thread(stores::close))
    embeddedServer(Netty, host = config.bindHost, port = config.port) {
        astridModule(config, stores)
    }.start(wait = true)
}
