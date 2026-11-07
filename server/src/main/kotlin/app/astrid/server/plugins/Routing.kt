package app.astrid.server.plugins

import app.astrid.server.authz.NotFound
import app.astrid.server.infra.config.ServerConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.path
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

/** The built web app and its SPA fallback; registered after the API so `/api/...` never falls through to `index.html`. */
internal fun Route.staticRoutes(config: ServerConfig) {
    val root = config.webStaticDir?.let(::File)?.takeIf(File::isDirectory) ?: return
    get("/") { call.serveStaticOrSpa(root) }
    get("/{...}") { call.serveStaticOrSpa(root) }
}

private suspend fun ApplicationCall.serveStaticOrSpa(root: File) {
    val path = request.path()
    if (path == "/api" || path.startsWith("/api/")) throw NotFound()
    response.headers.append(HttpHeaders.CacheControl, "no-cache")
    val canonicalRoot = root.canonicalFile
    val requested = File(canonicalRoot, path.removePrefix("/")).canonicalFile
    val insideRoot = requested.toPath().startsWith(canonicalRoot.toPath())
    if (insideRoot && requested.isFile) {
        respondFile(requested)
    } else if (path == "/assets" || path.startsWith("/assets/")) {
        throw NotFound()
    } else {
        respondFile(File(canonicalRoot, "index.html"))
    }
}
