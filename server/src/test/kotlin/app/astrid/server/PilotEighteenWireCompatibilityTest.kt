package app.astrid.server

import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class PilotEighteenWireCompatibilityTest {
    @Test
    fun `pilot eighteen public wire inspector metadata grading and fingerprints remain exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotEighteenWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers thirty nine formats and every literal P1 task`() {
        val rows = fixtureRows()
        assertEquals(
            (1..39).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            rows.filter { it[0] in setOf("C", "B", "M") }.map { it[1] }.toSet(),
        )
        val exercises = PilotEighteenWireCompatibilityFixture.p1Exercises()
        assertEquals(23, exercises.size)
        assertEquals(
            mapOf(
                "E031" to 3, "E032" to 1, "E033" to 4, "E034" to 2, "E035" to 1,
                "E036" to 1, "E037" to 3, "E038" to 1, "E039" to 7,
            ),
            exercises.groupingBy { it.formatId }.eachCount(),
        )
        assertEquals(
            setOf(
                "p1-1.1", "p1-1.2", "p1-1.3", "p1-1.4",
                "p1-2.1", "p1-2.2", "p1-2.3", "p1-2.4",
                "p1-3.1", "p1-3.2", "p1-3.3", "p1-3.4", "p1-3.5",
                "p1-4.1", "p1-4.2", "p1-4.3", "p1-4.4", "p1-4.5",
                "p1-5.1", "p1-5.2", "p1-5.3", "p1-5.4", "p1-5.5",
            ),
            exercises.map { requireNotNull(it.inspector.vocabularySource).taskId }.toSet(),
        )
        assertEquals(
            setOf("object", "place", "time"),
            exercises.mapNotNull { it.inspector.vocabularySource?.questionRole }.toSet(),
        )
        assertEquals(
            setOf("p1-studere-present", "p1-drikke-present", "p1-subject-first-supplied"),
            exercises.mapNotNull { it.targetScopeId }.toSet(),
        )
    }

    @Test
    fun `matching rows exhaust every partial bijection and preserve complete pair results`() {
        val rows = fixtureRows().filter { it[0] == "GRADE" && it[1] == "E033" }
        val byTask = rows.groupBy { it[2] }
        assertEquals(4, byTask.size)
        PilotEighteenWireCompatibilityFixture.p1Exercises().filter { it.formatId == "E033" }.forEach { exercise ->
            val taskId = requireNotNull(exercise.inspector.vocabularySource).taskId
            val pairCount = requireNotNull(exercise.prompt.matchingColumns).left.size
            val expectedBijections = if (pairCount == 3) 34 else 209
            assertEquals(expectedBijections, byTask.getValue(taskId).count { it[3].startsWith("bijection-") }, taskId)
            assertEquals(
                setOf("reordered-array", "duplicate-left", "duplicate-right", "unknown-left", "unknown-right", "wrong-kind"),
                byTask.getValue(taskId).map { it[3] }.filterNot { it.startsWith("bijection-") }.toSet(),
            )
        }
    }

    @Test
    fun `public P1 samples contain no private answer or correspondence fields`() {
        PilotEighteenWireCompatibilityFixture.p1Exercises().forEach { exercise ->
            val publicJson = wireJson.parseToJsonElement(
                wireJson.encodeToString(exercise.toResponse(PilotEighteenWireCompatibilityFixture.EXERCISE_ID)),
            ).jsonObject
            val privateJson = wireJson.parseToJsonElement(
                wireJson.encodeToString(exercise.toInspector(PilotEighteenWireCompatibilityFixture.EXERCISE_ID)),
            ).jsonObject
            val publicKeys = publicJson.recursiveKeys()
            assertFalse("vocabularySource" in publicKeys)
            assertTrue(
                publicKeys.intersect(
                    setOf(
                        "expectedAnswer", "matchingPairs", "acceptedOrders", "knownAlternatives", "reviewedOrders",
                        "targetSupportFactId", "normalizationPolicy",
                    ),
                ).isEmpty(),
                requireNotNull(exercise.inspector.vocabularySource).taskId,
            )
            assertTrue("vocabularySource" in privateJson)
            exercise.prompt.matchingColumns?.let { columns ->
                val publicPrompt = publicJson.getValue("prompt").jsonObject
                val publicColumns = publicPrompt.getValue("matchingColumns").jsonObject
                assertEquals(columns.left.size, publicColumns.getValue("left").jsonArray.size)
                assertEquals(columns.right.size, publicColumns.getValue("right").jsonArray.size)
                assertTrue(
                    (publicColumns.getValue("left").jsonArray + publicColumns.getValue("right").jsonArray)
                        .all { it.jsonObject.keys == setOf("id", "text") },
                )
                assertFalse("pairs" in publicColumns)
            }
        }
    }

    @Test
    fun `private P1 metadata override changes fingerprint without changing generated public or inspector JSON`() {
        val exercise = PilotEighteenWireCompatibilityFixture.p1Exercises().first { it.formatId == "E039" }
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val originalPublic = wireJson.encodeToString(exercise.toResponse(PilotEighteenWireCompatibilityFixture.EXERCISE_ID))
        val originalInspector = wireJson.encodeToString(exercise.toInspector(PilotEighteenWireCompatibilityFixture.EXERCISE_ID))
        val changed = source.copy(
            supportFacts = source.supportFacts.toMutableList().also { facts ->
                facts[0] = facts[0].copy(valueId = "changed-private-value")
            },
        )
        assertNotEquals(
            exercise.reproductionFingerprint(),
            exercise.reproductionFingerprint(vocabularySourceForFingerprint = changed),
        )
        assertNotEquals(
            PilotEighteenWireCompatibilityFixture.privateMetadataProjection(source),
            PilotEighteenWireCompatibilityFixture.privateMetadataProjection(changed),
        )
        assertEquals(originalPublic, wireJson.encodeToString(exercise.toResponse(PilotEighteenWireCompatibilityFixture.EXERCISE_ID)))
        assertEquals(originalInspector, wireJson.encodeToString(exercise.toInspector(PilotEighteenWireCompatibilityFixture.EXERCISE_ID)))
    }

    @Test
    fun `mass recall explicitly freezes both synthetic article shapes`() {
        val cases = fixtureRows().filter { it[0] == "GRADE" && it[2] == "p1-4.5" }.map { it[3] }.toSet()
        assertTrue("mass-en-vand" in cases)
        assertTrue("mass-et-vand" in cases)
    }

    @Test
    fun `exporter refuses either output collision`() {
        val directory = createTempDirectory("pilot-eighteen-wire-collision")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        Files.writeString(fixture, "occupied")
        assertFailsWith<IllegalArgumentException> {
            PilotEighteenWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
        Files.delete(fixture)
        Files.writeString(provenance, "occupied")
        assertFailsWith<IllegalArgumentException> {
            PilotEighteenWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
    }

    @Test
    fun `provenance pins full release source revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "releaseSourceCommit=${PilotEighteenWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.20.1", "captureDate=$CAPTURE_DATE", "contentRevision=pilot-18",
            "generatorRevision=generator-18", "evaluationRevision=evaluation-18",
            "projectionSchema=${PilotEighteenWireCompatibilityFixture.PROJECTION_SCHEMA}",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT",
            "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun JsonObject.recursiveKeys(): Set<String> = buildSet {
        fun collect(element: kotlinx.serialization.json.JsonElement) {
            when (element) {
                is JsonObject -> element.forEach { (key, value) -> add(key); collect(value) }
                is kotlinx.serialization.json.JsonArray -> element.forEach(::collect)
                else -> Unit
            }
        }
        collect(this@recursiveKeys)
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(PilotEighteenWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-18-generator-18-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-18-generator-18-wire.provenance"
        const val ROW_COUNT = 5_862 // Captured from v0.20.1 in two fresh JVMs.
        const val FIXTURE_SHA256 = "9085319dd579f3872d1709ded0e7b0a1302bac4cc0178e793d1d5b9bb73bda17"
        const val CAPTURE_DATE = "2026-11-12"
    }
}
