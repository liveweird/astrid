package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.reproductionFingerprint

object VocabularyP2FingerprintProbeMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val engine = PilotEngine()
        println(engine.generate(GenerationRequest("E033", null, 47, 0, "pilot-19", "generator-19"))
            .reproductionFingerprint())
        println(engine.generate(GenerationRequest("E042", null, 47, 0, "pilot-19", "generator-19"))
            .reproductionFingerprint())
    }
}
