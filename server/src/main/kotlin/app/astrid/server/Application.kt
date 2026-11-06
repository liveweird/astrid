package app.astrid.server

import app.astrid.engine.PilotEngine
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

internal const val MAX_BODY_BYTES = 16L * 1024

val wireJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
    encodeDefaults = true
}

fun Application.astridModule(
    config: ServerConfig = ServerConfig.fromEnvironment(),
    repository: AstridRepository,
    engine: PilotEngine = PilotEngine(),
    clock: () -> Long = System::currentTimeMillis,
) {
    install(ContentNegotiation) { json(wireJson) }
    install(RequestBodyLimit) { bodyLimit { MAX_BODY_BYTES } }
    installProblemHandling()
    installSecurityHeadersAndGuards(config)
    routing { astridRoutes(config, repository, engine, clock) }
}
