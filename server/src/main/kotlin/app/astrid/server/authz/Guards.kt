@file:Suppress("ThrowsCount") // Each guard maps independent client faults to distinct safe problems.

package app.astrid.server.authz

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.contentType
import io.ktor.server.request.header
import java.security.MessageDigest
import java.util.UUID

private const val CLIENT_HEADER = "X-Astrid-Client"
private const val INSPECTOR_HEADER = "X-Astrid-Inspector-Key"
private const val PRACTICE_OWNER_HEADER = "X-Astrid-Practice-Owner"

internal fun requireMutationHeaders(call: ApplicationCall) {
    if (call.request.header(CLIENT_HEADER) != "playground") throw Forbidden("Missing required playground client header")
    val media = call.request.headers[HttpHeaders.ContentType] ?: throw BadRequest("Request body is missing or not JSON")
    if (media.isBlank() || call.request.contentType().withoutParameters() != ContentType.Application.Json) {
        throw UnsupportedMediaType()
    }
}

internal fun requireInspectorKey(call: ApplicationCall, expected: String) {
    val provided = call.request.header(INSPECTOR_HEADER) ?: throw Forbidden("Inspector key is required")
    if (!MessageDigest.isEqual(provided.toByteArray(), expected.toByteArray())) throw Forbidden("Inspector key is invalid")
}

internal fun requirePracticeOwner(call: ApplicationCall, expected: UUID) {
    val raw = call.request.header(PRACTICE_OWNER_HEADER) ?: throw BadRequest("Practice owner header is required")
    val supplied = parseStrictUuid(raw, "practice owner")
    if (supplied != expected) throw Conflict("Practice owner does not match the authenticated session")
}

internal fun parseStrictUuid(value: String, field: String): UUID {
    val parsed = runCatching { UUID.fromString(value) }.getOrElse { throw BadRequest("Invalid $field") }
    if (!value.equals(parsed.toString(), ignoreCase = true)) throw BadRequest("Invalid $field")
    return parsed
}

internal fun ApplicationCall.uuidParameter(name: String): UUID =
    parameters[name]?.let { parseStrictUuid(it, name) } ?: throw BadRequest("Invalid $name")
