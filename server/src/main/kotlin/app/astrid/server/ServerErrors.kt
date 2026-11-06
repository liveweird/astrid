@file:Suppress("MagicNumber") // HTTP status codes.

package app.astrid.server

internal open class ApiException(val status: Int, val code: String, override val message: String) : RuntimeException(message)
internal class BadRequest(message: String) : ApiException(400, "bad_request", message)
internal class Unauthorized(message: String = "A development session is required") : ApiException(401, "unauthorized", message)
internal class Forbidden(message: String) : ApiException(403, "forbidden", message)
internal class NotFound(message: String = "Resource not found") : ApiException(404, "not_found", message)
internal class Conflict(message: String) : ApiException(409, "conflict", message)
internal class UnsupportedMediaType : ApiException(415, "unsupported_media_type", "Request body must use application/json")
internal class EvaluationUnavailable : ApiException(503, "evaluation_unavailable", "Evaluation is temporarily unavailable")
