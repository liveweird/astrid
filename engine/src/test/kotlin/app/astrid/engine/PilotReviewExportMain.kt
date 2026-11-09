package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path

object PilotReviewExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(
            args.size == 1 ||
                (args.size == 2 && args.first() in
                    setOf(
                        "--legacy",
                        "--pilot-two",
                        "--pilot-three",
                        "--pilot-four",
                        "--pilot-five",
                        "--pilot-six",
                        "--pilot-seven",
                    )),
        ) {
            "Expected an output path, optionally preceded by a retained-pilot flag from --legacy through --pilot-seven"
        }
        val output = Path.of(args.last())
        Files.createDirectories(output.parent)
        val markdown = when (args.first()) {
            "--legacy" -> PilotReview.renderLegacyMarkdown()
            "--pilot-two" -> PilotReview.renderPilotTwoMarkdown()
            "--pilot-three" -> PilotReview.renderPilotThreeMarkdown()
            "--pilot-four" -> PilotReview.renderPilotFourMarkdown()
            "--pilot-five" -> PilotReview.renderPilotFiveMarkdown()
            "--pilot-six" -> PilotReview.renderPilotSixMarkdown()
            "--pilot-seven" -> PilotReview.renderPilotSevenMarkdown()
            else -> PilotReview.renderMarkdown()
        }
        Files.writeString(output, markdown)
    }
}
