package app.astrid.server.session

import kotlinx.serialization.Serializable

// The revision pair the development session advertises as current; the exercise fingerprints switch on the same pair.
internal const val CURRENT_CONTENT_REVISION = "pilot-4"
internal const val CURRENT_GENERATOR_REVISION = "generator-4"
internal const val CURRENT_EVALUATION_REVISION = "evaluation-4"

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
