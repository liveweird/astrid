package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path

object PilotReviewExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1 || (args.size == 2 && args.first() == "--legacy")) {
            "Expected an output path, optionally preceded by --legacy"
        }
        val legacy = args.first() == "--legacy"
        val output = Path.of(args.last())
        Files.createDirectories(output.parent)
        Files.writeString(output, if (legacy) PilotReview.renderLegacyMarkdown() else PilotReview.renderMarkdown())
    }
}
