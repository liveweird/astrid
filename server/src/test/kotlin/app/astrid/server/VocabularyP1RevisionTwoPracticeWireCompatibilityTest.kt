package app.astrid.server

import app.astrid.server.infra.json.wireJson
import java.nio.file.Files
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

class VocabularyP1RevisionTwoPracticeWireCompatibilityTest {
    @Test
    fun `P1 practice public private lifecycle and saved context wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, VocabularyP1RevisionTwoPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers five literal plans at every boundary seed and lifecycle projection`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        val lifecycleKinds = listOf(
            "CREATE", "PAYLOAD_ROUND_TRIP", "INITIAL", "BEFORE_TEACHING", "TAUGHT", "SUPPORTED",
            "ATTEMPTED", "COMPLETED", "SAVED_ATTEMPT",
        )
        lifecycleKinds.forEach { kind -> assertEquals(20, rows.count { it[0] == kind }, kind) }
        assertEquals(20, rows.count { it[0] == "PLAN_TASK_ORDER" })
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        assertEquals(
            (1..5).mapTo(mutableSetOf()) { "vocabulary-p1-s$it-2" },
            rows.filter { it[0] == "PLAN_TASK_ORDER" }.map { it[2] }.toSet(),
        )
        assertEquals(
            setOf(4, 5),
            rows.filter { it[0] == "PLAN_TASK_ORDER" }.map { it[4].split(',').size }.toSet(),
        )
        assertEquals(
            mapOf(
                "vocabulary-p1-s1" to "p1-1.1,p1-1.2,p1-1.3,p1-1.4",
                "vocabulary-p1-s2" to "p1-2.1,p1-2.2,p1-2.3,p1-2.4",
                "vocabulary-p1-s3" to "p1-3.1,p1-3.2,p1-3.3,p1-3.4,p1-3.5",
                "vocabulary-p1-s4" to "p1-4.1,p1-4.2,p1-4.3,p1-4.4,p1-4.5",
                "vocabulary-p1-s5" to "p1-5.1,p1-5.2,p1-5.3,p1-5.4,p1-5.5",
            ),
            rows.filter { it[0] == "PLAN_TASK_ORDER" }.associate { it[1] to it[4] },
        )
        assertEquals(10, rows.count { it[0] == "ELIGIBILITY" })
        assertEquals(8, rows.count { it[0] == "MATCHING_BOUNDARY" })
        assertEquals(2, rows.count { it[0] == "LATEST_BOUNDARY" })
        assertTrue(rows.none { it[0].contains("REPLAY") }, "Serialization evidence must not be labelled stored replay")
    }

    @Test
    fun `private plans retain exact counts revisions teaching policies and P1 target projections`() {
        val samples = VocabularyP1RevisionTwoPracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(5, samples.size)
        assertEquals(listOf(4, 4, 5, 5, 5), samples.map { (privatePlan) ->
            wireJson.parseToJsonElement(privatePlan).jsonObject.getValue("items").jsonArray.size
        })
        samples.forEachIndexed { index, (privatePlan, initialWire, completedWire) ->
            val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
            assertEquals("pilot-18", privateJson.getValue("contentRevision").jsonPrimitive.content)
            assertEquals("generator-18", privateJson.getValue("generatorRevision").jsonPrimitive.content)
            assertEquals("evaluation-18", privateJson.getValue("evaluationRevision").jsonPrimitive.content)
            assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertEquals("vocabulary-p1-outcome-1", privateJson.getValue("completionRuleId").jsonPrimitive.content)
            assertEquals("vocabulary-p1-exposure-1", privateJson.getValue("exposurePolicyId").jsonPrimitive.content)
            assertEquals(listOf("G001", "G002", "G003", "G004", "G005"),
                privateJson.getValue("prerequisiteSkillIds").jsonArray.map { it.jsonPrimitive.content })
            assertTrue(privateJson.getValue("introductionExposureKeys").jsonArray.isNotEmpty())
            assertTrue(privateJson.getValue("introduction").jsonArray.isNotEmpty())

            val privateItems = privateJson.getValue("items").jsonArray.map { it.jsonObject }
            assertTrue(privateItems.all { it.getValue("answerExposureKeys").jsonArray.isNotEmpty() })
            assertTrue(privateItems.any { it.getValue("promptExposureKeys").jsonArray.isNotEmpty() })
            assertTrue(privateItems.all {
                it.getValue("lessonExposureKeys").jsonArray.isNotEmpty() ||
                    it.getValue("hintExposureKeys").jsonArray.isNotEmpty()
            })
            assertTrue(privateItems.all { it.getValue("feedbackExposureKeys").jsonArray.isNotEmpty() })
            val readingAssessments = privateItems.flatMap { it.getValue("assessments").jsonArray }
                .map { it.jsonObject }.filter { "readingTarget" in it }
            readingAssessments.forEach { assessment ->
                assertEquals("explicit-fact", assessment.getValue("readingTarget").jsonObject.getValue("facet").jsonPrimitive.content)
                assertTrue(assessment.getValue("readingRole").jsonPrimitive.content in setOf("object", "place", "time"))
            }
            val matchingItems = privateItems.filter { "matchingTarget" in it }
            matchingItems.forEach { item ->
                assertEquals("meaning", item.getValue("matchingTarget").jsonObject.getValue("facet").jsonPrimitive.content)
                assertEquals("en-da", item.getValue("matchingTarget").jsonObject.getValue("direction").jsonPrimitive.content)
                assertTrue(item.getValue("assessments").jsonArray.size in 3..4)
            }

            val initial = wireJson.parseToJsonElement(initialWire).jsonObject
            assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse(initial.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())
            assertFalse("followUp" in initial)
            assertFalse("followUpPlanRevision" in initial)

            val completed = wireJson.parseToJsonElement(completedWire).jsonObject
            assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
            assertEquals(privateItems.size, completed.getValue("summary").jsonObject.getValue("completedItems").jsonPrimitive.int)
            val summaryRows = completed.getValue("summary").jsonObject.getValue("rows").jsonArray.map { it.jsonObject }
            assertTrue(summaryRows.isNotEmpty())
            assertTrue(summaryRows.all { row ->
                val latest = row.getValue("latestOutcomes").jsonObject
                setOf("meaningMet", "meaningNotMet", "meaningUnknown").all(latest::containsKey)
            })
            assertFalse("followUp" in completed)
            assertFalse("followUpPlanRevision" in completed)
            assertEquals(index + 1, initial.getValue("focusId").jsonPrimitive.content.removePrefix("vocabulary-p1-s").toInt())
        }
    }

    @Test
    fun `matching saved results and actual feedback contexts reveal only confirmed mappings`() {
        val completedSamples = VocabularyP1RevisionTwoPracticeWireCompatibilityFixture.samples().chunked(3).map { it[2] }
        val matchingItems = completedSamples.flatMap { wire ->
            wireJson.parseToJsonElement(wire).jsonObject.getValue("items").jsonArray.map { it.jsonObject }
        }.filter { "matchingTarget" in it }
        assertEquals(4, matchingItems.size)
        matchingItems.forEach { item ->
            val assessmentCount = item.getValue("assessments").jsonArray.size
            val first = item.getValue("firstAttempt").jsonObject
            val results = first.getValue("matchingResults").jsonArray.map { it.jsonObject }
            assertEquals(assessmentCount, results.size)
            assertEquals(assessmentCount - 2, results.count {
                it.getValue("targetSatisfaction").jsonPrimitive.content == "met"
            })
            val context = item.getValue("lastAttemptContext").jsonObject
            val feedbackKeys = context.getValue("priorExposure").jsonArray.map { it.jsonObject }
                .filter { it.getValue("source").jsonPrimitive.content == "feedback" }
                .flatMap { exposure -> exposure.getValue("keys").jsonArray.map { it.jsonPrimitive.content } }
                .filter { it.startsWith("vocabulary-p1:mapping:") }
            assertEquals(assessmentCount - 2, feedbackKeys.distinct().size)
        }
    }

    @Test
    fun `public sessions do not leak frozen private keys plans or answer correspondences`() {
        VocabularyP1RevisionTwoPracticeWireCompatibilityFixture.samples().chunked(3).forEach { (_, initialWire, completedWire) ->
            listOf(initialWire, completedWire).forEach { wire ->
                val public = wireJson.parseToJsonElement(wire)
                val keys = public.recursiveKeys()
                assertTrue(
                    keys.intersect(
                        setOf(
                            "answerExposureKeys", "promptExposureKeys", "lessonExposureKeys", "hintExposureKeys",
                            "feedbackExposureKeys", "matchingPairs", "expectedAnswer", "acceptedOrders", "knownAlternatives",
                        ),
                    ).isEmpty(),
                )
            }
        }
    }

    @Test
    fun `exporter refuses either output collision`() {
        val directory = createTempDirectory("vocabulary-p1-practice-wire-collision")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        Files.writeString(fixture, "occupied")
        assertFailsWith<IllegalArgumentException> {
            VocabularyP1RevisionTwoPracticeWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
        Files.delete(fixture)
        Files.writeString(provenance, "occupied")
        assertFailsWith<IllegalArgumentException> {
            VocabularyP1RevisionTwoPracticeWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
    }

    @Test
    fun `provenance pins full release source five plan pairs rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        val planPairs = (1..5).map { number ->
            "vocabulary-p1-s$number-2:pilot-18/generator-18/evaluation-18"
        }
        listOf(
            "releaseSourceCommit=${VocabularyP1RevisionTwoPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.20.1", "captureDate=$CAPTURE_DATE",
            "projectionSchema=${VocabularyP1RevisionTwoPracticeWireCompatibilityFixture.PROJECTION_SCHEMA}",
            "planPairs=${planPairs.joinToString(",")}",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT",
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
    private fun ByteArray.sha256() = with(VocabularyP1RevisionTwoPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/vocabulary-p1-practice-0.20.1-wire.tsv"
        const val PROVENANCE_PATH = "/golden/vocabulary-p1-practice-0.20.1-wire.provenance"
        const val ROW_COUNT = 221 // Captured from v0.20.1 in two fresh JVMs.
        const val FIXTURE_SHA256 = "288a676d09607fa21c2758a138aa392e8c2b5ee19a4e8315ff7a34ac68d20ac6"
        const val CAPTURE_DATE = "2026-11-12"
        val PROFILES = (1..5).mapTo(mutableSetOf()) { "vocabulary-p1-s$it" }
    }
}
