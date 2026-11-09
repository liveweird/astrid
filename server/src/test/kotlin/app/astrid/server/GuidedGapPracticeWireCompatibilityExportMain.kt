package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object GuidedGapPracticeWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) {
            "Usage: GuidedGapPracticeWireCompatibilityExportMain <fixture.tsv> <fixture.provenance>"
        }
        val bytes = GuidedGapPracticeWireCompatibilityFixture.fixtureBytes()
        Files.write(Path.of(args[0]), bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            Path.of(args[1]),
            GuidedGapPracticeWireCompatibilityFixture.provenance(
                bytes,
                bytes.toString(Charsets.UTF_8).lineSequence().count(String::isNotBlank),
            ),
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }
}
