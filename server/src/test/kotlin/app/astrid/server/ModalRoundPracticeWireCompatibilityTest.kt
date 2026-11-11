package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ModalRoundPracticeWireCompatibilityTest {
    @Test
    fun `module one modal round public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, ModalRoundPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers the three two item S12 modal round plans at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        val lifecycleKinds = listOf(
            "CREATE", "INITIAL", "BEFORE_TEACHING", "INTRODUCED",
            "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY",
        )
        lifecycleKinds.forEach { kind ->
            assertEquals(12, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(21, eligibility.size)
        assertEquals(
            setOf(
                "correct", "known-wrong", "unknown", "partial", "np-reversal", "order-violation", "wrong-kind",
            ),
            eligibility.map { it[2] }.toSet(),
        )
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val samples = ModalRoundPracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(3, samples.size)
        samples.forEach { (privatePlan, initialWire, completedWire) ->
            val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
            assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertEquals(
                "module-one-modal-round-outcome-1",
                privateJson.getValue("completionRuleId").jsonPrimitive.content,
            )
            assertEquals(
                "module-one-modal-round-exposure-1",
                privateJson.getValue("exposurePolicyId").jsonPrimitive.content,
            )
            assertEquals(2, privateJson.getValue("items").jsonArray.size)
            assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })
            assertEquals(
                listOf("selection", "word-order"),
                privateJson.getValue("items").jsonArray.map {
                    it.jsonObject.getValue("assessmentMode").jsonPrimitive.content
                },
            )
            val introductionKeys = privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content }
            assertTrue("module-one:bare-infinitive:L011" in introductionKeys)
            assertTrue("module-one:pattern:contextual-modal-meaning" in introductionKeys)
            assertTrue("module-one:pattern:supplied-modal-order" in introductionKeys)
            val privateItems = privateJson.getValue("items").jsonArray.map { it.jsonObject }
            assertEquals(1, privateItems.first().getValue("answerExposureKeys").jsonArray.size)
            assertEquals("supplied-modal-order", privateItems.last().getValue("targetScopeId").jsonPrimitive.content)
            assertTrue(privateItems.all { item ->
                item.getValue("lessonExposureKeys").jsonArray.map { it.jsonPrimitive.content } ==
                    privateJson.getValue("introductionExposureKeys").jsonArray.map { it.jsonPrimitive.content }
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
            "releaseSourceCommit=${ModalRoundPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.16.0", "captureDate=2026-11-11",
            "module-one-volition-1:pilot-14/generator-14/evaluation-14",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(ModalRoundPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/modal-round-practice-0.16.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/modal-round-practice-0.16.0-wire.provenance"
        const val ROW_COUNT = 118
        const val FIXTURE_SHA256 = "426b58e4376f8c7ee075c31de91c8811eaeb828ba87e5737e1ea4215307213c1"
        val PROFILES = setOf("module-one-volition", "module-one-obligation", "module-one-permission")
    }
}
