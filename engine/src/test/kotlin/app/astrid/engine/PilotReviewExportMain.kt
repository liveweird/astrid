package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path

object PilotReviewExportMain {
    private val retainedRenderers: Map<String, () -> String> = mapOf(
        "--legacy" to PilotReview::renderLegacyMarkdown,
        "--pilot-two" to PilotReview::renderPilotTwoMarkdown,
        "--pilot-three" to PilotReview::renderPilotThreeMarkdown,
        "--pilot-four" to PilotReview::renderPilotFourMarkdown,
        "--pilot-five" to PilotReview::renderPilotFiveMarkdown,
        "--pilot-six" to PilotReview::renderPilotSixMarkdown,
        "--pilot-seven" to PilotReview::renderPilotSevenMarkdown,
        "--pilot-eight" to PilotReview::renderPilotEightMarkdown,
        "--pilot-nine" to PilotReview::renderPilotNineMarkdown,
        "--pilot-ten" to PilotReview::renderPilotTenMarkdown,
        "--pilot-eleven" to PilotReview::renderPilotElevenMarkdown,
        "--pilot-twelve" to PilotReview::renderPilotTwelveMarkdown,
        "--pilot-thirteen" to PilotReview::renderPilotThirteenMarkdown,
        "--pilot-fourteen" to PilotReview::renderPilotFourteenMarkdown,
        "--pilot-fifteen" to PilotReview::renderPilotFifteenMarkdown,
        "--pilot-sixteen" to PilotReview::renderPilotSixteenMarkdown,
        "--pilot-seventeen" to PilotReview::renderPilotSeventeenMarkdown,
        "--pilot-eighteen" to PilotReview::renderPilotEighteenMarkdown,
        "--pilot-nineteen" to PilotReview::renderPilotNineteenMarkdown,
    )

    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 1 || (args.size == 2 && args.first() in retainedRenderers)) {
            "Expected an output path, optionally preceded by a retained-pilot flag from --legacy through --pilot-nineteen"
        }
        val output = Path.of(args.last())
        Files.createDirectories(output.parent)
        val render = retainedRenderers[args.first()] ?: PilotReview::renderMarkdown
        Files.writeString(output, render())
    }
}
