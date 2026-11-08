package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object PilotFourWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Expected fixture and provenance output paths" }
        val fixturePath = Path.of(args[0])
        val provenancePath = Path.of(args[1])
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen pilot-4 wire compatibility evidence"
        }
        val fixtureBytes = PilotFourWireCompatibilityFixture.fixtureBytes()
        val rowCount = fixtureBytes.toString(Charsets.UTF_8).lineSequence().count(String::isNotBlank)

        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, fixtureBytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenancePath,
            PilotFourWireCompatibilityFixture.provenance(fixtureBytes, rowCount),
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }
}
