@file:Suppress("MatchingDeclarationName") // File is named for the error-handling feature, as in Flow; ProblemDetail is its one class.

package app.astrid.server.plugins

import app.astrid.server.authz.ApiException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * RFC 7807 problem detail. Serialized as `application/problem+json`.
 *
 * `type` defaults to `about:blank`, in which case `title` is the standard reason phrase for `status` (per the RFC).
 * `detail` is the occurrence-specific message; `code` is Astrid's stable machine-readable extension member that the
 * web client branches on.
 */
@Serializable
data class ProblemDetail(
    val type: String = "about:blank",
    val title: String,
    val status: Int,
    val detail: String,
    val instance: String,
    val code: String,
)

/** RFC 7807 media type for error bodies. */
val ProblemJson: ContentType = ContentType("application", "problem+json")

private val problemSerializer = Json { encodeDefaults = true; explicitNulls = false }

/**
 * Respond with an RFC 7807 `application/problem+json` body. Goes through [TextContent] explicitly so
 * ContentNegotiation does not relabel it as `application/json`.
 */
suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    code: String,
    detail: String = status.description,
    title: String = status.description,
    type: String = "about:blank",
) {
    // The occurrence path is useful without reflecting query parameters that may contain secrets.
    val problem = ProblemDetail(
        type = type,
        title = title,
        status = status.value,
        detail = detail,
        instance = request.path(),
        code = code,
    )
    respond(
        TextContent(
            problemSerializer.encodeToString(ProblemDetail.serializer(), problem),
            ProblemJson.withCharset(Charsets.UTF_8),
            status,
        ),
    )
}

internal fun Application.installProblemHandling() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respondProblem(HttpStatusCode.fromValue(cause.status), cause.code, cause.message)
        }
        exception<PayloadTooLargeException> { call, _ ->
            call.respondProblem(
                HttpStatusCode.PayloadTooLarge,
                "payload_too_large",
                "Request body exceeds $MAX_BODY_BYTES bytes",
            )
        }
        exception<KtorBadRequestException> { call, _ -> call.respondMalformed() }
        exception<SerializationException> { call, _ -> call.respondMalformed() }
        exception<CannotTransformContentToTypeException> { call, _ ->
            if (call.request.headers[HttpHeaders.ContentType] == null) call.respondMalformed()
            else call.respondProblem(
                HttpStatusCode.UnsupportedMediaType,
                "unsupported_media_type",
                "Request body must use application/json",
            )
        }
        exception<CancellationException> { _, cause -> throw cause }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled request failure", cause)
            call.respondProblem(HttpStatusCode.InternalServerError, "internal_error", "Internal server error")
        }
    }
}

private suspend fun ApplicationCall.respondMalformed() {
    respondProblem(HttpStatusCode.BadRequest, "bad_request", "Malformed JSON request")
}
