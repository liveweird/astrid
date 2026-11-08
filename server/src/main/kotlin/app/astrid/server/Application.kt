package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.Evaluation
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.exerciseRoutes
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.plugins.healthRoutes
import app.astrid.server.plugins.installHttp
import app.astrid.server.plugins.installProblemHandling
import app.astrid.server.plugins.installSecurityHeadersAndGuards
import app.astrid.server.plugins.installStaticCompression
import app.astrid.server.plugins.staticRoutes
import app.astrid.server.practice.practiceRoutes
import app.astrid.server.session.DevelopmentSessionAuthenticator
import app.astrid.server.session.sessionRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.astridModule(
    config: ServerConfig = ServerConfig.fromEnvironment(),
    stores: Stores,
    engine: PilotEngine = PilotEngine(),
    evaluator: suspend (GeneratedExercise, Answer) -> Evaluation = engine::evaluate,
    clock: () -> Long = System::currentTimeMillis,
) {
    installHttp()
    installStaticCompression()
    installProblemHandling()
    installSecurityHeadersAndGuards(config)
    val authenticator = DevelopmentSessionAuthenticator(config, stores.sessions, clock)
    routing {
        route("/api/v1") {
            healthRoutes(stores.readiness)
            sessionRoutes(config, stores.sessions, clock)
            exerciseRoutes(config, stores.exercises, authenticator, engine, evaluator)
            practiceRoutes(stores.practice, authenticator, engine)
        }
        staticRoutes(config)
    }
}
