package app.astrid.server

import app.astrid.server.infra.json.wireJson
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CumulativeReviewTwoWireCompatibilityTest {
    @Test
    fun `published cumulative review two exact saved and public wire remains frozen`() {
        val bytes = resource(FIXTURE).use { it.readBytes() }
        assertEquals(SHA256, bytes.sha256())
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, CumulativeReviewTwoWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers request private plan item policy lifecycle and replay at boundaries`() {
        val rows = fixtureRows()
        val kinds = setOf(
            "CREATE", "PRIVATE_PLAN", "PAYLOAD_ROUND_TRIP", "ITEM_POLICIES",
            "INITIAL", "TAUGHT", "ATTEMPTED", "SAVED_ATTEMPT_REPLAY",
        )
        assertEquals(kinds + "CREATE_FAILURE", rows.map { it[0] }.toSet())
        kinds.forEach { assertEquals(3, rows.count { row -> row[0] == it }, it) }
        assertEquals(1, rows.count { it[0] == "CREATE_FAILURE" })
        assertEquals(Int.MAX_VALUE.toString(), rows.single { it[0] == "CREATE_FAILURE" }[1])
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.map { it[1].toInt() }.toSet())
    }

    @Test
    fun `private plan pins twenty six sources per item revisions policies and selections`() {
        CumulativeReviewTwoWireCompatibilityFixture.samples().forEach { (planWire, publicWire) ->
            val plan = wireJson.parseToJsonElement(planWire).jsonObject
            assertEquals("per-item", plan.getValue("contentRevision").jsonPrimitive.content)
            assertEquals("per-item", plan.getValue("generatorRevision").jsonPrimitive.content)
            assertEquals("per-item", plan.getValue("evaluationRevision").jsonPrimitive.content)
            val cumulative = plan.getValue("cumulativeReview").jsonObject
            assertEquals("cumulative-review-2", cumulative.getValue("selectionPolicyRevision").jsonPrimitive.content)
            assertEquals(26, cumulative.getValue("sourceLessonCount").jsonPrimitive.content.toInt())
            val selections = cumulative.getValue("selections").jsonArray
            assertEquals(8, selections.size)
            assertTrue(selections.all { selection ->
                selection.jsonObject.keys == setOf(
                    "candidateKey", "semanticId", "sourcePracticeSessionId", "sourcePosition", "sourceLessonId",
                )
            })
            val items = plan.getValue("items").jsonArray.map { it.jsonObject }
            assertEquals(8, items.size)
            assertTrue(items.all { "policy" in it })

            val public = wireJson.parseToJsonElement(publicWire).jsonObject
            assertEquals("cumulative-review-2", public.getValue("planRevision").jsonPrimitive.content)
            assertTrue(public.getValue("items").jsonArray.all { item ->
                val revisions = item.jsonObject.getValue("revisions").jsonObject
                revisions.getValue("contentRevision").jsonPrimitive.content != "per-item" &&
                    revisions.getValue("generatorRevision").jsonPrimitive.content != "per-item" &&
                    revisions.getValue("evaluationRevision").jsonPrimitive.content != "per-item"
            })
        }
    }

    @Test
    fun `exporter refuses either existing evidence file`() {
        val directory = Files.createTempDirectory("astrid-cumulative-two-freeze-")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        try {
            Files.writeString(fixture, "occupied")
            assertFailsWith<IllegalArgumentException> {
                CumulativeReviewTwoWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
            }
            assertFalse(Files.exists(provenance))
            Files.delete(fixture)
            Files.writeString(provenance, "occupied")
            assertFailsWith<IllegalArgumentException> {
                CumulativeReviewTwoWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
            }
            assertFalse(Files.exists(fixture))
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    @Test
    fun `provenance pins source twenty six families rows and bytes`() {
        val properties = Properties().apply { resource(PROVENANCE).use { load(it) } }
        mapOf(
            "releaseSourceCommit" to CumulativeReviewTwoWireCompatibilityFixture.RELEASE_SOURCE_COMMIT,
            "releaseVersion" to "0.22.0",
            "captureDate" to "2026-11-13",
            "projectionSchema" to "cumulative-review-two-server-wire-v1",
            "planRevision" to "cumulative-review-2",
            "sourceFamilies" to "26",
            "reviewState" to "compatibility-only-not-linguistic-approval",
            "rowCount" to ROW_COUNT.toString(),
            "fixtureSha256" to SHA256,
        ).forEach { (key, value) -> assertEquals(value, properties.getProperty(key), key) }
        assertTrue(properties.getProperty("description").isNotBlank())
    }

    private fun fixtureRows() = resource(FIXTURE).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val PROVENANCE = "/golden/cumulative-review-0.22.0-wire.provenance"
        const val FIXTURE = "/golden/cumulative-review-0.22.0-wire.tsv"
        const val ROW_COUNT = 25 // Captured from v0.22.0 in two fresh JVMs.
        const val SHA256 = "525067707fbaf99f8df238b46883690cf718be7cfb8417fd75123b5edd9c5841"
    }
}
