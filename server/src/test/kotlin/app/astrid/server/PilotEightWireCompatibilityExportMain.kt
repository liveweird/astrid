package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object PilotEightWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Expected fixture and provenance output paths" }
        val fixture = Path.of(args[0])
        val provenance = Path.of(args[1])
        require(!Files.exists(fixture) && !Files.exists(provenance)) {
            "Refusing to overwrite frozen pilot-8 wire compatibility evidence"
        }
        val bytes = PilotEightWireCompatibilityFixture.fixtureBytes()
        Files.createDirectories(fixture.toAbsolutePath().parent)
        Files.createDirectories(provenance.toAbsolutePath().parent)
        Files.write(fixture, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenance,
            PilotEightWireCompatibilityFixture.provenance(bytes, bytes.count { it == '\n'.code.toByte() }),
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }
}
