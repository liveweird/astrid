package app.astrid.server.plugins

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.parseHeaderValue
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.application.install
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.CompressionConfig
import io.ktor.server.plugins.compression.condition
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.compression.matchContentType
import io.ktor.server.plugins.compression.minimumSize
import io.ktor.server.request.path

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
