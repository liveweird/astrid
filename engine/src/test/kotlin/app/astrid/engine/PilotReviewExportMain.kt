package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path

object PilotReviewExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1) { "Expected one output path" }
        val output = Path.of(args.single())
        Files.createDirectories(output.parent)
        Files.writeString(output, PilotReview.renderMarkdown())
    }
}
