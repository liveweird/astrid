package app.astrid.server

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val config = ServerConfig.fromEnvironment()
    migrate(config)
    val repository = PostgresRepository.connect(config, wireJson)
    Runtime.getRuntime().addShutdownHook(Thread(repository::close))
    val bindHost = System.getenv("ASTRID_BIND_HOST") ?: "127.0.0.1"
    embeddedServer(Netty, host = bindHost, port = 8088) {
        astridModule(config, repository)
    }.start(wait = true)
}
