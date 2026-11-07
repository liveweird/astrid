package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

object PilotThreeWireCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Expected fixture and provenance output paths" }
        val fixturePath = Path.of(args[0])
        val provenancePath = Path.of(args[1])
        val fixtureBytes = PilotThreeWireCompatibilityFixture.fixtureBytes()
        val rowCount = fixtureBytes.toString(Charsets.UTF_8).lineSequence().count(String::isNotBlank)

        Files.createDirectories(fixturePath.parent)
        Files.createDirectories(provenancePath.parent)
        Files.write(fixturePath, fixtureBytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenancePath,
            PilotThreeWireCompatibilityFixture.provenance(fixtureBytes, rowCount),
            StandardOpenOption.CREATE_NEW,
        )
    }
}
