@file:Suppress("MagicNumber", "ThrowsCount") // HTTP status codes and distinct request-guard failures.

package app.astrid.server

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.parseHeaderValue
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.BadRequestException as KtorBadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.PayloadTooLargeException
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.CompressionConfig
import io.ktor.server.plugins.compression.condition
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.matchContentType
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import kotlinx.serialization.SerializationException
import kotlinx.coroutines.CancellationException

internal const val STATIC_COMPRESSION_MIN_BYTES = 1024L

private val StaticCompressionVary = createApplicationPlugin("StaticCompressionVary") {
    on(ResponseBodyReadyForSend) { call, content ->
        if (call.isCompressibleStaticResponse(content) && !content.variesByAcceptEncoding()) {
            call.response.headers.append(HttpHeaders.Vary, HttpHeaders.AcceptEncoding)
        }
    }
}

internal fun Application.installStaticCompression() {
    install(StaticCompressionVary)
    install(Compression) {
        mode = CompressionConfig.Mode.CompressResponse
        gzip {
            minimumSize(STATIC_COMPRESSION_MIN_BYTES)
            matchContentType(ContentType.Application.JavaScript, ContentType.Text.JavaScript, ContentType.Text.CSS)
            condition { _ -> request.path().isAssetPath() && identityIsNotExplicitlyPreferred() }
        }
    }
}

private fun ApplicationCall.isCompressibleStaticResponse(content: OutgoingContent): Boolean {
    val status = content.status ?: response.status() ?: HttpStatusCode.OK
    if (status != HttpStatusCode.OK || !request.path().isAssetPath()) return false
    val compressibleType = content.contentType?.let { type ->
        type.match(ContentType.Application.JavaScript) ||
            type.match(ContentType.Text.JavaScript) ||
            type.match(ContentType.Text.CSS)
    } == true
    val encoded = content.headers[HttpHeaders.ContentEncoding].equals("gzip", ignoreCase = true)
    return compressibleType && (encoded || (content.contentLength ?: -1L) >= STATIC_COMPRESSION_MIN_BYTES)
}

private fun OutgoingContent.variesByAcceptEncoding(): Boolean =
    headers.getAll(HttpHeaders.Vary).orEmpty()
        .flatMap { it.split(',') }
        .any { it.trim().equals(HttpHeaders.AcceptEncoding, ignoreCase = true) }

/**
 * Ktor's gzip-only negotiation already handles absent codings, wildcards, and q=0. The plugin does not compare an
 * unregistered identity representation, though, while registering its identity encoder would emit the discouraged
 * `Content-Encoding: identity`. Keep this compatibility check deliberately narrow: only an explicit higher-quality
 * identity preference suppresses gzip, and leave every other negotiation decision to Ktor.
 */
private fun ApplicationCall.identityIsNotExplicitlyPreferred(): Boolean {
    val accepted = request.headers.getAll(HttpHeaders.AcceptEncoding)
        .orEmpty()
        .flatMap(::parseHeaderValue)
    fun quality(coding: String): Double? = accepted
        .firstOrNull { it.value.equals(coding, ignoreCase = true) }
        ?.quality

    val identityQuality = quality("identity")
    val gzipQuality = quality("gzip") ?: quality("*")
    return identityQuality == null || gzipQuality == null || gzipQuality >= identityQuality
}

private fun String.isAssetPath() = this == "/assets" || startsWith("/assets/")

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
    respondProblem(HttpStatusCode.BadRequest, "bad_request", "Malformed JSON request")
}
