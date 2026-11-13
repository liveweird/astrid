package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object VocabularyP2PracticeWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: VocabularyP2PracticeWireCompatibilityExportMain <fixture.tsv> <provenance>" }
        val fixture = Path.of(args[0])
        val provenance = Path.of(args[1])
        require(!Files.exists(fixture) && !Files.exists(provenance)) {
            "Refusing to overwrite frozen P2 practice wire evidence"
        }
        val rows = VocabularyP2PracticeWireCompatibilityFixture.rows()
        val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixture.toAbsolutePath().parent)
        Files.createDirectories(provenance.toAbsolutePath().parent)
        Files.write(fixture, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenance,
            VocabularyP2PracticeWireCompatibilityFixture.provenance(bytes, rows.size),
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }
}
