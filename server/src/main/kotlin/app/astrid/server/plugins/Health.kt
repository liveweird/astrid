package app.astrid.server.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable

/** Whether the backing store answers; the composition root supplies the probe. */
fun interface Readiness {
    suspend fun ready(): Boolean
}

@Serializable
data class HealthResponse(val status: String)

internal fun Route.healthRoutes(readiness: Readiness) {
    get("/health") { call.respond(HealthResponse("ok")) }
    get("/ready") {
        if (readiness.ready()) call.respond(HealthResponse("ready"))
        else call.respondProblem(HttpStatusCode.ServiceUnavailable, "not_ready", "Service is not ready")
    }
}
