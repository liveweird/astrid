package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PersonalPracticeWireCompatibilityTest {
    @Test
    fun `module one personal public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PersonalPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers the six item S09 plan at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        listOf("CREATE", "INITIAL", "INTRODUCED", "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY").forEach { kind ->
            assertEquals(4, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(5, eligibility.size)
        assertEquals(
            setOf(
                "correct", "known-wrong", "case", "shape", "unknown",
            ),
            eligibility.map { it[2] }.toSet(),
        )
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val samples = PersonalPracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(1, samples.size)
        samples.forEach { (privatePlan, initialWire, completedWire) ->
            val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
            assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertEquals(
                "module-one-known-outcome-1",
                privateJson.getValue("completionRuleId").jsonPrimitive.content,
            )
            assertEquals(
                "module-one-exposure-1",
                privateJson.getValue("exposurePolicyId").jsonPrimitive.content,
            )
            assertEquals(6, privateJson.getValue("items").jsonArray.size)
            assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })
            assertEquals(
                listOf("selection", "recall", "selection", "recall", "selection", "recall"),
                privateJson.getValue("items").jsonArray.map {
                    it.jsonObject.getValue("assessmentMode").jsonPrimitive.content
                },
            )
            assertEquals(
                listOf(
                    "module-one:pattern:personal-introduction-present",
                    "module-one:verb-infinitive:L028", "module-one:verb-present:L028",
                    "module-one:verb-infinitive:L029", "module-one:verb-present:L029",
                    "module-one:verb-infinitive:L030", "module-one:verb-present:L030",
                    "module-one:supplied-component:L031", "module-one:supplied-component:L032",
                ),
                privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content },
            )
            val privateItems = privateJson.getValue("items").jsonArray.map { it.jsonObject }
            assertEquals(
                listOf(
                    "module-one:verb-present:L028", "module-one:verb-infinitive:L028",
                    "module-one:pattern:personal-introduction-present",
                ),
                privateItems.first().getValue("answerExposureKeys").jsonArray.map { it.jsonPrimitive.content },
            )
            assertTrue(privateItems.all {
                it.getValue("targetScopeId").jsonPrimitive.content == "personal-introduction-present"
            })

            val initial = wireJson.parseToJsonElement(initialWire).jsonObject
            assertTrue(initial.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse("followUpPlanRevision" in initial)
            assertFalse("followUp" in initial)
            assertFalse(initial.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())

            val completed = wireJson.parseToJsonElement(completedWire).jsonObject
            assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
            assertEquals(2, completed.getValue("summary").jsonObject.getValue("rows").jsonArray.size)
            assertFalse("followUpPlanRevision" in completed)
        }
    }

    @Test
    fun `provenance pins release source plans revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "releaseSourceCommit=${PersonalPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.14.0", "captureDate=2026-11-11",
            "planPairs=module-one-personal-1:pilot-12/generator-12/evaluation-12",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(PersonalPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/personal-practice-0.14.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/personal-practice-0.14.0-wire.provenance"
        const val ROW_COUNT = 34
        const val FIXTURE_SHA256 = "c3d5099f19f548ecd5a7fd9d0197324d47e3f84a168060b41042dc987e537c85"
        val PROFILES = setOf("module-one-personal")
    }
}
