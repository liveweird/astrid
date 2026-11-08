package app.astrid.server

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.CRC32
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the Flyway checksum of every migration file. An APPLIED migration's bytes may never
 * change, comments included: Flyway validates stored checksums against the files at startup
 * (`validateOnMigrate`, the default), so any edit to an already-applied migration makes every
 * existing database refuse to boot with a checksum mismatch. This gate turns that silent
 * production-only failure into a red test: editing a listed file fails here, and the fix is
 * to revert the edit, never to update the pinned value. Adding a NEW migration adds one line;
 * compute it with the same algorithm below, or read it from `flyway_schema_history` after the
 * first local run.
 */
class MigrationChecksumTest {
    private val pinned = mapOf(
        "V1__astrid_pilot.sql" to -1604343897,
        "V2__nullable_exercise_target.sql" to -2087079022,
        "V3__practice_sessions.sql" to -2053289065,
    )

    @Test
    fun `every migration file matches its applied Flyway checksum`() {
        val dir = Path.of("src/main/resources/db/migration")
        val files = dir.listDirectoryEntries("*.sql").associate { it.name to flywayChecksum(it) }
        assertEquals(
            pinned.keys.sorted(),
            files.keys.sorted(),
            "Migration files and the pinned manifest must list the same set; add the new file's checksum here",
        )
        for ((name, expected) in pinned) {
            assertEquals(
                expected,
                files.getValue(name),
                "$name changed after being applied: revert the edit; existing databases would fail Flyway validation",
            )
        }
    }

    // Flyway's algorithm: CRC32 over each line's UTF-8 bytes (terminators excluded), BOM stripped.
    private fun flywayChecksum(file: Path): Int {
        val crc = CRC32()
        Files.readString(file).removePrefix("﻿").lineSequence().forEach { crc.update(it.toByteArray()) }
        return crc.value.toInt()
    }
}
