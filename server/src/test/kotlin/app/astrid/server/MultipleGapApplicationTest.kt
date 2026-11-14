@file:Suppress("MaxLineLength") // Complete fingerprint mutations and wire assertions remain readable together.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.GapSelectionDto
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
import app.astrid.server.session.SESSION_COOKIE
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class MultipleGapApplicationTest {
    @Test
    fun `pair seven fingerprint frames every gap prompt and private source value`() {
        val generated = PilotEngine().generate(GenerationRequest("E015", "G004", 47, 0, "pilot-7", "generator-7"))
        val original = generated.reproductionFingerprint()
        val prompt = requireNotNull(generated.prompt.gapPrompt)
        val slot = prompt.slots.first()
        val choice = slot.choices.first()
        val source = requireNotNull(generated.inspector.gapSource)
        val sourceSlot = source.slots.first()
        val mutations = listOf(
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = null)),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(segments = prompt.segments.reversed()))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(segments = listOf("changed") + prompt.segments.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = prompt.slots.reversed()))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(id = "changed")) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(label = "changed")) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(targetSkillId = "changed")) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(assessmentRole = prompt.slots.last().assessmentRole)) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(choices = slot.choices.reversed())) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(choices = listOf(choice.copy(id = "changed")) + slot.choices.drop(1))) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(gapPrompt = prompt.copy(slots = listOf(slot.copy(choices = listOf(choice.copy(text = "changed")) + slot.choices.drop(1))) + prompt.slots.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = null)),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = source.copy(slots = source.slots.reversed()))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = source.copy(slots = listOf(sourceSlot.copy(slotId = "changed")) + source.slots.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = source.copy(slots = listOf(sourceSlot.copy(expectedChoiceId = "changed")) + source.slots.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = source.copy(slots = listOf(sourceSlot.copy(lexicalId = "changed")) + source.slots.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = source.copy(slots = listOf(sourceSlot.copy(expectedForm = "changed")) + source.slots.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(gapSource = source.copy(slots = listOf(sourceSlot.copy(alternativeForm = "changed")) + source.slots.drop(1)))),
        )

        mutations.forEach { assertNotEquals(original, it) }
    }

    @Test
    fun `gap public wire exposes scoped choices while inspector alone carries answer keys`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val catalog = client.get("/api/v1/catalog") { trustedHost(); cookie(SESSION_COOKIE, owner) }.json()
        assertEquals("pilot-20", catalog.string("contentRevision"))
        assertEquals("generator-20", catalog.string("generatorRevision"))
        assertEquals("evaluation-20", catalog.string("evaluationRevision"))
        assertTrue(catalog.getValue("formats").jsonArray.any { it.jsonObject.string("id") == "E015" })

        val exercise = createExercise(owner).json()
        assertEquals("E015", exercise.string("formatId"))
        assertEquals("G004", exercise.string("targetSkillId"))
        assertFalse("lexicalTarget" in exercise)
        assertFalse("lexicalTargets" in exercise)
        val prompt = exercise.getValue("prompt").jsonObject
        assertEquals(setOf("before", "after", "choices", "blocks", "punctuation", "cueLanguage", "answerLanguage", "gapPrompt"), prompt.keys)
        val gapPrompt = prompt.getValue("gapPrompt").jsonObject
        assertEquals(3, gapPrompt.getValue("segments").jsonArray.size)
        val slots = gapPrompt.getValue("slots").jsonArray.map { it.jsonObject }
        assertEquals(listOf("g0", "g1"), slots.map { it.string("id") })
        assertEquals(listOf("primary", "support"), slots.map { it.string("assessmentRole") })
        assertTrue(slots.all { slot -> slot.getValue("choices").jsonArray.map { it.jsonObject.string("id") }.toSet() == setOf("c0", "c1") })
        assertFalse("gapSource" in exercise)

        val inspector = inspect(owner, exercise.string("id")).json()
        val sources = inspector.getValue("gapSource").jsonObject.getValue("slots").jsonArray.map { it.jsonObject }
        assertEquals(listOf("g0", "g1"), sources.map { it.string("slotId") })
        assertTrue(sources.all { it.keys == setOf("slotId", "expectedChoiceId", "lexicalId", "expectedForm", "alternativeForm") })
    }

    @Test
    fun `gap grading preserves selection order and keeps support errors separate from primary target`() = testApplication {
        val repository = InMemoryStores()
        application { astridModule(config(), repository) }
        val owner = createSession()
        val exercise = createExercise(owner).json()
        val exerciseId = exercise.string("id")
        val expected = expectedSelections(owner, exerciseId)
        val choices = exercise.getValue("prompt").jsonObject.getValue("gapPrompt").jsonObject
            .getValue("slots").jsonArray.map { it.jsonObject }.associate { slot ->
                slot.string("id") to slot.getValue("choices").jsonArray.map { it.jsonObject.string("id") }
            }
        val supportWrong = GapSelectionDto("g1", choices.getValue("g1").single { it != expected.getValue("g1") })
        val primaryCorrect = GapSelectionDto("g0", expected.getValue("g0"))
        val supplied = listOf(supportWrong, primaryCorrect)
        val attemptId = UUID.randomUUID().toString()
        val first = submitGaps(owner, attemptId, exerciseId, supplied)
        assertEquals(HttpStatusCode.OK, first.status)
        val result = first.json()
        assertEquals("met", result.string("targetSatisfaction"))
        assertEquals("known-issue", result.string("linguisticStatus"))
        assertEquals("met", result.string("responseConstraintStatus"))
        assertEquals("danish-gaps", result.string("linguisticScope"))
        assertFalse("meaningSatisfaction" in result)
        assertTrue(result.string("completedSentence").isNotEmpty())
        val rows = result.getValue("gapResults").jsonArray.map { it.jsonObject }
        assertEquals(listOf("g0", "g1"), rows.map { it.string("slotId") })
        assertEquals("met", rows[0].string("targetSatisfaction"))
        assertEquals("not-met", rows[1].string("targetSatisfaction"))
        assertTrue(rows.all { "meaningSatisfaction" !in it })

        repository.tamperFingerprint(UUID.fromString(exerciseId))
        assertEquals(first.bodyAsText(), submitGaps(owner, attemptId, exerciseId, supplied).bodyAsText())
        assertEquals(HttpStatusCode.Conflict, submitGaps(owner, attemptId, exerciseId, supplied.reversed()).status)
        assertEquals(HttpStatusCode.Conflict, submitGaps(owner, UUID.randomUUID().toString(), exerciseId, supplied).status)

        val partialExercise = createExercise(owner).json()
        val partialId = partialExercise.string("id")
        val partialExpected = expectedSelections(owner, partialId)
        val partial = submitGaps(owner, UUID.randomUUID().toString(), partialId, listOf(GapSelectionDto("g0", partialExpected.getValue("g0")))).json()
        assertEquals("met", partial.string("targetSatisfaction"))
        assertEquals("unknown", partial.string("linguisticStatus"))
        assertEquals("not-met", partial.string("responseConstraintStatus"))
        assertEquals("", partial.string("completedSentence"))
        assertFalse("choiceId" in partial.getValue("gapResults").jsonArray[1].jsonObject)
    }

    @Test
    fun `gap validation follows ownership and invalid attempts reserve no retry identity`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val outsider = createSession()
        val exercise = createExercise(owner).json()
        val exerciseId = exercise.string("id")
        val expected = expectedSelections(owner, exerciseId)
        val valid = listOf(GapSelectionDto("g0", expected.getValue("g0")), GapSelectionDto("g1", expected.getValue("g1")))
        val malformed = listOf(
            listOf(valid[0], valid[0].copy(choiceId = if (valid[0].choiceId == "c0") "c1" else "c0")),
            listOf(valid[0].copy(slotId = "g9")),
            listOf(valid[0].copy(choiceId = "c9")),
            valid + valid[0],
        )
        malformed.forEachIndexed { index, selections ->
            val attemptId = UUID.randomUUID().toString()
            assertEquals(HttpStatusCode.NotFound, submitGaps(outsider, attemptId, exerciseId, selections).status)
            assertEquals(HttpStatusCode.BadRequest, submitGaps(owner, attemptId, exerciseId, selections).status)
            val accepted = submitGaps(owner, attemptId, exerciseId, valid)
            assertEquals(HttpStatusCode.OK, accepted.status)
            assertEquals(index + 1, accepted.json().getValue("retryNumber").jsonPrimitive.content.toInt())
        }
        assertEquals(HttpStatusCode.BadRequest, submitText(owner, UUID.randomUUID().toString(), exerciseId).status)
        assertEquals(HttpStatusCode.OK, submitGaps(owner, UUID.randomUUID().toString(), exerciseId, emptyList()).status)
    }

    private suspend fun ApplicationTestBuilder.createSession(): String {
        val response = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        return response.headers[HttpHeaders.SetCookie]!!.substringAfter("$SESSION_COOKIE=").substringBefore(';')
    }

    private suspend fun ApplicationTestBuilder.createExercise(cookieValue: String) = client.post("/api/v1/exercises") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody("""{"formatId":"E015","targetSkillId":"G004","seed":47,"index":0,"contentRevision":"pilot-7","generatorRevision":"generator-7"}""")
    }

    private suspend fun ApplicationTestBuilder.expectedSelections(cookieValue: String, exerciseId: String) =
        inspect(cookieValue, exerciseId).json().getValue("gapSource").jsonObject.getValue("slots").jsonArray
            .associate { it.jsonObject.let { slot -> slot.string("slotId") to slot.string("expectedChoiceId") } }

    private suspend fun ApplicationTestBuilder.inspect(cookieValue: String, exerciseId: String) =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Inspector-Key", INSPECTOR_KEY)
        }

    private suspend fun ApplicationTestBuilder.submitGaps(
        cookieValue: String,
        attemptId: String,
        exerciseId: String,
        selections: List<GapSelectionDto>,
    ) = submit(cookieValue, AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Gaps(selections), false))

    private suspend fun ApplicationTestBuilder.submitText(cookieValue: String, attemptId: String, exerciseId: String) =
        submit(cookieValue, AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Text("wrong kind"), false))

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, request: AttemptRequest) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(request))
        }

    private suspend fun HttpResponse.json() = wireJson.parseToJsonElement(bodyAsText()) as JsonObject

    private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config() = ServerConfig(
        true, true, INSPECTOR_KEY, false, false,
        setOf("http://localhost:8088"), setOf("localhost:8088"), 60_000, null,
        "jdbc:postgresql://unused", "r2dbc:postgresql://unused", "unused", "unused", 2,
    )

    private companion object {
        const val INSPECTOR_KEY = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk"
    }
}
