package app.astrid.server.session

import kotlinx.serialization.Serializable

// The revision pair advertised for new playground work; stored fingerprint branches remain literal and immutable.
internal const val CURRENT_CONTENT_REVISION = "pilot-16"
internal const val CURRENT_GENERATOR_REVISION = "generator-16"
internal const val CURRENT_EVALUATION_REVISION = "evaluation-16"

@Serializable
class DevelopmentSessionRequest

@Serializable
data class DevelopmentSessionResponse(
    val mode: String = "development",
    val inspectorEnabled: Boolean,
    val contentRevision: String = CURRENT_CONTENT_REVISION,
    val generatorRevision: String = CURRENT_GENERATOR_REVISION,
    val evaluationRevision: String = CURRENT_EVALUATION_REVISION,
)
