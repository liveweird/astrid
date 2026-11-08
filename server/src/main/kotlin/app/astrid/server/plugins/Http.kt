package app.astrid.server.plugins

import app.astrid.server.infra.json.wireJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level

internal const val MAX_BODY_BYTES = 16L * 1024

/**
 * JSON content negotiation on the shared wire format, the request-body ceiling, and API access
 * logging. The log line carries method, path and status only: never the query string, headers or
 * cookies (the session cookie is a credential). The health/readiness probes are not logged: the
 * container healthcheck and Kubernetes probes would add a line every few seconds.
 */
private val UNLOGGED_PATHS = setOf("/api/v1/health", "/api/v1/ready")


internal fun Application.installHttp() {
    install(ContentNegotiation) { json(wireJson) }
    install(RequestBodyLimit) { bodyLimit { MAX_BODY_BYTES } }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().let { it.startsWith("/api/") && it !in UNLOGGED_PATHS } }
        format { call -> "${call.request.httpMethod.value} ${call.request.path()} ${call.response.status()?.value}" }
    }
}
