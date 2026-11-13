package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object CumulativeReviewTwoWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: CumulativeReviewTwoWireCompatibilityExportMain <fixture.tsv> <provenance>" }
        val fixture = Path.of(args[0])
        val provenance = Path.of(args[1])
        require(!Files.exists(fixture) && !Files.exists(provenance)) {
            "Refusing to overwrite frozen cumulative-review-2 wire evidence"
        }
        val rows = CumulativeReviewTwoWireCompatibilityFixture.rows()
        val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixture.toAbsolutePath().parent)
        Files.createDirectories(provenance.toAbsolutePath().parent)
        Files.write(fixture, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenance, CumulativeReviewTwoWireCompatibilityFixture.provenance(bytes, rows.size), Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }
}
