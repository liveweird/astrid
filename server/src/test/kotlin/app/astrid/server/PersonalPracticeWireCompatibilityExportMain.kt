package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object PersonalPracticeWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Expected fixture and provenance output paths" }
        val fixture = Path.of(args[0])
        val provenance = Path.of(args[1])
        require(!Files.exists(fixture) && !Files.exists(provenance)) {
            "Refusing to overwrite frozen Module 1 S09 personal practice wire compatibility evidence"
        }
        val bytes = PersonalPracticeWireCompatibilityFixture.fixtureBytes()
        Files.createDirectories(fixture.toAbsolutePath().parent)
        Files.createDirectories(provenance.toAbsolutePath().parent)
        Files.write(fixture, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenance,
            PersonalPracticeWireCompatibilityFixture.provenance(bytes, bytes.count { it == '\n'.code.toByte() }),
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }
}
