package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object CumulativeReviewOneWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: CumulativeReviewOneWireCompatibilityExportMain <fixture.tsv> <fixture.provenance>" }
        val fixture = Path.of(args[0]); val provenance = Path.of(args[1])
        require(!Files.exists(fixture) && !Files.exists(provenance)) {
            "Refusing to overwrite frozen cumulative review wire evidence"
        }
        val bytes = CumulativeReviewOneWireCompatibilityFixture.fixtureBytes()
        Files.createDirectories(fixture.toAbsolutePath().parent); Files.createDirectories(provenance.toAbsolutePath().parent)
        Files.write(fixture, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenance,
            CumulativeReviewOneWireCompatibilityFixture.provenance(bytes, CumulativeReviewOneWireCompatibilityFixture.rows().size),
            StandardOpenOption.CREATE_NEW,
        )
    }
}
