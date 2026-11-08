package app.astrid.server.infra.json

import kotlinx.serialization.json.Json

val wireJson = Json {
    ignoreUnknownKeys = false
    explicitNulls = false
    encodeDefaults = true
}
