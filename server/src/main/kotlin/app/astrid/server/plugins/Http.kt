package app.astrid.server.plugins

import app.astrid.server.infra.json.wireJson
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.bodylimit.RequestBodyLimit
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation

internal const val MAX_BODY_BYTES = 16L * 1024

/** JSON content negotiation on the shared wire format and the request-body ceiling. */
internal fun Application.installHttp() {
    install(ContentNegotiation) { json(wireJson) }
    install(RequestBodyLimit) { bodyLimit { MAX_BODY_BYTES } }
}
