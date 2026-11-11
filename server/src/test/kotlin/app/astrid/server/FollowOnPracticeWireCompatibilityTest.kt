package app.astrid.server

import app.astrid.server.infra.json.wireJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class FollowOnPracticeWireCompatibilityTest {
    @Test
    fun `module one possession and reading words public private and replay wire remains frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, FollowOnPracticeWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers both four item S07 S08 plans at four seeds and completion boundaries`() {
        val rows = fixtureRows()
        assertEquals(1, rows.count { it[0] == "PROFILES" })
        listOf("CREATE", "INITIAL", "INTRODUCED", "SUPPORTED", "ATTEMPTED", "COMPLETED", "ATTEMPT_REPLAY").forEach { kind ->
            assertEquals(8, rows.count { it[0] == kind }, kind)
        }
        assertEquals(PROFILES, rows.filter { it[0] == "CREATE" }.map { it[1] }.toSet())
        assertEquals(setOf(0, 1, 47, Int.MAX_VALUE), rows.filter { it[0] == "CREATE" }.map { it[2].toInt() }.toSet())
        val eligibility = rows.filter { it[0] == "ELIGIBILITY" }
        assertEquals(10, eligibility.size)
        assertEquals(
            setOf(
                "correct", "known-wrong", "case", "shape", "unknown", "wrong-kind", "wrong-then-correct",
            ),
            eligibility.map { it[2] }.toSet(),
        )
    }

    @Test
    fun `public samples require introduction expose scoped metadata and omit follow up fallback`() {
        val samples = FollowOnPracticeWireCompatibilityFixture.samples().chunked(3)
        assertEquals(2, samples.size)
        samples.forEachIndexed { index, (privatePlan, initialWire, completedWire) ->
            val privateJson = wireJson.parseToJsonElement(privatePlan).jsonObject
            assertTrue(privateJson.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertEquals(
                "module-one-known-outcome-1",
                privateJson.getValue("completionRuleId").jsonPrimitive.content,
            )
            assertEquals(
                if (index == 0) "module-one-exposure-1" else "module-one-reading-word-exposure-1",
                privateJson.getValue("exposurePolicyId").jsonPrimitive.content,
            )
            assertEquals(4, privateJson.getValue("items").jsonArray.size)
            if (index == 0) {
                assertTrue(privateJson.getValue("items").jsonArray.all { "targetScopeId" in it.jsonObject })
            } else {
                assertEquals(
                    listOf("L024-S01", "L014-S01", "L015-S01", "L018-S01").map { senseId ->
                        listOf("module-one:reading-meaning:$senseId", "module-one:reading-noun-bundle:$senseId")
                    },
                    privateJson.getValue("items").jsonArray.map { item ->
                        item.jsonObject.getValue("answerExposureKeys").jsonArray.map { it.jsonPrimitive.content }
                    },
                )
            }

            val initial = wireJson.parseToJsonElement(initialWire).jsonObject
            if (index == 1) {
                assertTrue(initial.getValue("items").jsonArray.all { "lexicalTarget" in it.jsonObject })
            }
            assertTrue(initial.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertFalse(initial.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse("followUpPlanRevision" in initial)
            assertFalse("followUp" in initial)
            assertFalse(initial.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())

            val completed = wireJson.parseToJsonElement(completedWire).jsonObject
            assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
            assertEquals(if (index == 0) 2 else 4, completed.getValue("summary").jsonObject.getValue("rows").jsonArray.size)
            assertFalse("followUpPlanRevision" in completed)
        }
    }

    @Test
    fun `provenance pins release source plans revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "releaseSourceCommit=${FollowOnPracticeWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.13.0", "captureDate=2026-11-11",
            "planPairs=module-one-possession-1:pilot-11/generator-11/evaluation-11," +
                "module-one-reading-words-1:pilot-5/generator-5/evaluation-5",
            "reviewState=compatibility-only-not-linguistic-approval", "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = with(FollowOnPracticeWireCompatibilityFixture) { sha256() }

    private companion object {
        const val FIXTURE_PATH = "/golden/follow-on-practice-0.13.0-wire.tsv"
        const val PROVENANCE_PATH = "/golden/follow-on-practice-0.13.0-wire.provenance"
        const val ROW_COUNT = 67
        const val FIXTURE_SHA256 = "0dc164e1c6ec136f157a1d1b57edb6c57c062458b48c04f8b275319cb2f641d1"
        val PROFILES = setOf("module-one-possession", "module-one-reading-words")
    }
}
