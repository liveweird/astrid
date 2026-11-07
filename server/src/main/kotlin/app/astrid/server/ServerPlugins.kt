@file:Suppress("MagicNumber", "ThrowsCount") // HTTP status codes and distinct request-guard failures.

package app.astrid.server

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.CancellationException

internal fun Application.installProblemHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(HttpStatusCode.fromValue(cause.status), problem(cause.status, cause.code, cause.message, call.request.path()))
        }
        exception<PayloadTooLargeException> { call, _ ->
            call.respond(
                HttpStatusCode.PayloadTooLarge,
                problem(413, "payload_too_large", "Request body exceeds $MAX_BODY_BYTES bytes", call.request.path()),
            )
        }
        exception<KtorBadRequestException> { call, _ -> call.respondMalformed() }
        exception<SerializationException> { call, _ -> call.respondMalformed() }
        exception<CannotTransformContentToTypeException> { call, _ ->
            if (call.request.headers[HttpHeaders.ContentType] == null) call.respondMalformed()
            else call.respond(
                HttpStatusCode.UnsupportedMediaType,
                problem(415, "unsupported_media_type", "Request body must use application/json", call.request.path()),
            )
        }
        exception<CancellationException> { _, cause -> throw cause }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled request failure", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                problem(500, "internal_error", "Internal server error", call.request.path()),
            )
        }
    }
}

internal fun Application.installSecurityHeadersAndGuards(config: ServerConfig) {
    val allowedHosts = config.allowedHosts.map(String::lowercase).toSet()
    intercept(ApplicationCallPipeline.Plugins) {
        val path = call.request.path()
        if (path == "/api" || path.startsWith("/api/")) {
            call.response.headers.append(HttpHeaders.CacheControl, "no-store")
        }
        call.response.headers.append("X-Content-Type-Options", "nosniff")
        call.response.headers.append("X-Frame-Options", "DENY")
        call.response.headers.append("Referrer-Policy", "no-referrer")
        call.response.headers.append(
            "Content-Security-Policy",
            "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none'",
        )
        val host = call.request.headers[HttpHeaders.Host]?.lowercase()
        if (host == null || host !in allowedHosts) throw Forbidden("Host is not allowed")
        val origin = call.request.headers[HttpHeaders.Origin]
        if (origin != null && origin !in config.allowedOrigins) throw Forbidden("Origin is not allowed")
        val forwarded = listOf("Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Proto")
        if (!config.trustedForwardedHeaders && forwarded.any { call.request.headers[it] != null }) {
            throw Forbidden("Forwarded headers are not trusted")
        }
    }
}

private suspend fun ApplicationCall.respondMalformed() {
    respond(HttpStatusCode.BadRequest, problem(400, "bad_request", "Malformed JSON request", request.path()))
}

private fun problem(status: Int, code: String, detail: String, instance: String) = ProblemResponse(
    title = HttpStatusCode.fromValue(status).description,
    status = status,
    detail = detail,
    instance = instance,
    code = code,
)
