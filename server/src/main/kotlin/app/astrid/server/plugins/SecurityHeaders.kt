@file:Suppress("ThrowsCount") // Distinct request-guard failures.

package app.astrid.server.plugins

import app.astrid.server.authz.Forbidden
import app.astrid.server.infra.config.ServerConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.path

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
