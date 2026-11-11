package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AbilityPracticeWireCompatibilityTest {
    @Test
    fun `module one ability public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, AbilityPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers the four item S10 ability plan at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        listOf("CREATE", "INITIAL", "INTRODUCED", "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY").forEach { kind ->
            assertEquals(4, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(7, eligibility.size)
        assertEquals(
            setOf(
                "correct", "at-marker-selected", "finite", "case", "unknown", "at-marker-typed", "wrong-kind",
            ),
            eligibility.map { it[2] }.toSet(),
        )
        assertEquals(
            mapOf("selection" to "met", "recall" to "not-met"),
            AbilityPracticeWireCompatibilityFixture.markerConstraintStatuses(),
        )
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val samples = AbilityPracticeWireCompatibilityFixture.samples().chunked(3)
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
            assertEquals(4, privateJson.getValue("items").jsonArray.size)
            assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })
            assertEquals(
                listOf("selection", "selection", "recall", "recall"),
                privateJson.getValue("items").jsonArray.map {
                    it.jsonObject.getValue("assessmentMode").jsonPrimitive.content
                },
            )
            assertEquals(
                listOf(
                    "module-one:verb-infinitive:L033", "module-one:verb-present:L033",
                    "module-one:at-infinitive:L011", "module-one:bare-infinitive:L011",
                    "module-one:verb-present:L011", "module-one:pattern:kan-bare-infinitive",
                    "module-one:supplied-component:L023", "module-one:supplied-component:L018",
                    "module-one:supplied-component:L031",
                ),
                privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content },
            )
            val privateItems = privateJson.getValue("items").jsonArray.map { it.jsonObject }
            assertEquals(
                listOf(
                    "module-one:bare-infinitive:L011", "module-one:at-infinitive:L011",
                    "module-one:verb-present:L011", "module-one:pattern:kan-bare-infinitive",
                ),
                privateItems.first().getValue("answerExposureKeys").jsonArray.map { it.jsonPrimitive.content },
            )
            assertTrue(privateItems.all {
                it.getValue("targetScopeId").jsonPrimitive.content == "kan-bare-infinitive"
            })
            assertTrue(privateItems.all { item ->
                item.getValue("lessonExposureKeys").jsonArray.map { it.jsonPrimitive.content } ==
                    privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content }
            })
            assertTrue(privateItems.all { item ->
                item.getValue("answerExposureKeys").jsonArray.none { it.jsonPrimitive.content.contains("L033") }
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
            "releaseSourceCommit=${AbilityPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.15.0", "captureDate=2026-11-11",
            "planPairs=module-one-ability-1:pilot-13/generator-13/evaluation-13",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(AbilityPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/ability-practice-0.15.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/ability-practice-0.15.0-wire.provenance"
        const val ROW_COUNT = 36
        const val FIXTURE_SHA256 = "e7c3c06deb0be9c09cbc8f3e67212832806098a059ed192b04d65346c242fbf9"
        val PROFILES = setOf("module-one-ability")
    }
}
