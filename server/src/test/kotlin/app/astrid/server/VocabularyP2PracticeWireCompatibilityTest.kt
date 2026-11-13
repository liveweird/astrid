package app.astrid.server

import app.astrid.server.infra.json.wireJson
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.int

class VocabularyP2PracticeWireCompatibilityTest {
    @Test
    fun `P2 saved plans grades exposure summary and lifecycle wire remains exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, VocabularyP2PracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers six literal plans and every lifecycle projection at boundary seeds`() {
        val rows = fixtureRows()
        val lifecycle = listOf(
            "CREATE", "PLAN_TASK_ORDER", "PAYLOAD_ROUND_TRIP", "INITIAL", "BEFORE_TEACHING", "TAUGHT",
            "SUPPORTED", "ATTEMPTED", "COMPLETED", "SAVED_GRADE",
        )
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        lifecycle.forEach { kind -> assertEquals(21, rows.count { it[0] == kind }, kind) }
        assertEquals(3, rows.count { it[0] == "CREATE_FAILURE" })
        assertEquals(
            setOf("vocabulary-p2-s2", "vocabulary-p2-s3", "vocabulary-p2-s4"),
            rows.filter { it[0] == "CREATE_FAILURE" }.map { it[1] }.toSet(),
        )
        assertTrue(rows.filter { it[0] == "CREATE_FAILURE" }.all { it[2] == Int.MAX_VALUE.toString() })
        assertEquals(VocabularyP2PracticeWireCompatibilityFixture.profiles.map { it.first }.toSet(),
            rows.filter { it[0] in setOf("CREATE", "CREATE_FAILURE") }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE),
            rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        assertEquals(setOf(3, 4), rows.filter { it[0] == "PLAN_TASK_ORDER" }.map { it[4].split(',').size }.toSet())
        assertEquals(21, rows.filter { it[0] == "PLAN_TASK_ORDER" && it[3] == "47" }
            .flatMap { it[4].split(',') }.toSet().size)
    }

    @Test
    fun `private plans pin P2 revisions policies assessments completion exposure and summary`() {
        val samples = VocabularyP2PracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(6, samples.size)
        assertEquals(listOf(3, 4, 4, 3, 3, 4), samples.map { (plan) ->
            wireJson.parseToJsonElement(plan).jsonObject.getValue("items").jsonArray.size
        })
        samples.forEach { (planWire, initialWire, completedWire) ->
            val plan = wireJson.parseToJsonElement(planWire).jsonObject
            assertEquals("pilot-19", plan.getValue("contentRevision").jsonPrimitive.content)
            assertEquals("generator-19", plan.getValue("generatorRevision").jsonPrimitive.content)
            assertEquals("evaluation-19", plan.getValue("evaluationRevision").jsonPrimitive.content)
            assertEquals("vocabulary-p2-outcome-1", plan.getValue("completionRuleId").jsonPrimitive.content)
            assertEquals("vocabulary-p2-exposure-1", plan.getValue("exposurePolicyId").jsonPrimitive.content)
            assertTrue(plan.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            val privateItems = plan.getValue("items").jsonArray.map { it.jsonObject }
            assertTrue(privateItems.all { it.getValue("assessments").jsonArray.isNotEmpty() })
            assertTrue(privateItems.all { it.getValue("feedbackExposureKeys").jsonArray.isNotEmpty() })

            val initial = wireJson.parseToJsonElement(initialWire).jsonObject
            assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse(initial.getValue("items").jsonArray.first().jsonObject
                .getValue("canAdvance").jsonPrimitive.content.toBoolean())

            val completed = wireJson.parseToJsonElement(completedWire).jsonObject
            assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
            assertEquals(privateItems.size, completed.getValue("summary").jsonObject
                .getValue("completedItems").jsonPrimitive.int)
            assertTrue(completed.getValue("summary").jsonObject.getValue("rows").jsonArray.isNotEmpty())
            assertTrue(completed.getValue("items").jsonArray.all { item ->
                item.jsonObject.getValue("attemptCount").jsonPrimitive.int == 2
            })
        }
    }

    @Test
    fun `public sessions do not leak P2 private source or answer keys`() {
        VocabularyP2PracticeWireCompatibilityFixture.samples().chunked(3).forEach { (_, initial, completed) ->
            listOf(initial, completed).forEach { wire ->
                val keys = wireJson.parseToJsonElement(wire).recursiveKeys()
                assertTrue(keys.intersect(setOf(
                    "answerExposureKeys", "feedbackExposureKeys", "expectedAnswer", "matchingPairs", "acceptedOrders",
                    "chronologyRelations", "targetChronologyRelationId", "quantitySource", "reviewedOrders",
                )).isEmpty())
            }
        }
    }

    @Test
    fun `exporter refuses either output collision`() {
        val directory = createTempDirectory("p2-practice-wire-collision")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        Files.writeString(fixture, "occupied")
        assertFailsWith<IllegalArgumentException> {
            VocabularyP2PracticeWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
        Files.delete(fixture)
        Files.writeString(provenance, "occupied")
        assertFailsWith<IllegalArgumentException> {
            VocabularyP2PracticeWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
    }

    @Test
    fun `provenance pins release source all six plan pairs rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        val pairs = VocabularyP2PracticeWireCompatibilityFixture.profiles.joinToString(",") {
            "${it.second}:pilot-19/generator-19/evaluation-19"
        }
        listOf(
            "releaseSourceCommit=${VocabularyP2PracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.22.0", "captureDate=2026-11-13", "projectionSchema=p2-practice-server-wire-v1",
            "planPairs=$pairs", "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT",
            "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun kotlinx.serialization.json.JsonElement.recursiveKeys(): Set<String> = buildSet {
        fun collect(element: kotlinx.serialization.json.JsonElement) {
            when (element) {
                is JsonObject -> element.forEach { (key, value) -> add(key); collect(value) }
                is JsonArray -> element.forEach(::collect)
                else -> Unit
            }
        }
        collect(this@recursiveKeys)
    }
    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "/golden/vocabulary-p2-practice-0.22.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/vocabulary-p2-practice-0.22.0-wire.provenance"
        const val ROW_COUNT = 214 // Captured from v0.22.0 in two fresh JVMs.
        const val FIXTURE_SHA256 = "c594e660a1d11d3e901da5752edec88142e6807475db7112d295bf8eeaa1375f"
    }
}
