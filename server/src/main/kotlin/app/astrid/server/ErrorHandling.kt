@file:Suppress("MatchingDeclarationName") // File is named for the error-handling feature, as in Flow; ProblemDetail is its one class.

package app.astrid.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
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
