@file:Suppress("MaxLineLength") // Complete wire requests are intentionally kept as readable JSON fixtures.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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
import kotlin.test.assertNotNull
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class RecallRepairApplicationTest {
    @Test
    fun `session and catalog advertise the complete current revision`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val (cookieValue, sessionBody) = createSession()
        val session = wireJson.parseToJsonElement(sessionBody).jsonObject
        assertEquals("pilot-4", session.getValue("contentRevision").jsonPrimitive.content)
        assertEquals("generator-4", session.getValue("generatorRevision").jsonPrimitive.content)
        assertEquals("evaluation-4", session.getValue("evaluationRevision").jsonPrimitive.content)

        val catalogResponse = client.get("/api/v1/catalog") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
        }
        assertEquals(HttpStatusCode.OK, catalogResponse.status)
        val catalog = wireJson.parseToJsonElement(catalogResponse.bodyAsText()).jsonObject
        assertEquals("pilot-4", catalog.getValue("contentRevision").jsonPrimitive.content)
        assertEquals("generator-4", catalog.getValue("generatorRevision").jsonPrimitive.content)
        assertEquals("evaluation-4", catalog.getValue("evaluationRevision").jsonPrimitive.content)
        assertEquals(
            listOf("E001", "E002", "E003", "E004", "E005", "E006", "E007", "E008", "E009", "E010"),
            (catalog.getValue("formats") as JsonArray).map { it.jsonObject.getValue("id").jsonPrimitive.content },
        )
    }

    @Test
    fun `only complete supported revision pairs are accepted`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val cookieValue = createSession().first

        assertEquals(HttpStatusCode.Created, createExercise(cookieValue, "E001", "G004", "pilot-1", "generator-1").status)
        assertEquals(HttpStatusCode.Created, createExercise(cookieValue, "E001", "G004", "pilot-2", "generator-2").status)
        assertEquals(HttpStatusCode.Created, createExercise(cookieValue, "E001", "G004", "pilot-3", "generator-3").status)
        assertEquals(HttpStatusCode.Created, createExercise(cookieValue, "E001", "G004", "pilot-4", "generator-4").status)
        assertEquals(HttpStatusCode.Conflict, createExercise(cookieValue, "E001", "G004", "pilot-1", "generator-2").status)
        assertEquals(HttpStatusCode.Conflict, createExercise(cookieValue, "E001", "G004", "pilot-2", "generator-1").status)
        assertEquals(HttpStatusCode.BadRequest, createExercise(cookieValue, "E004", "G004", "pilot-1", "generator-1").status)
    }

    @Test
    fun `new recall and repair prompts expose only public text metadata`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val cookieValue = createSession().first

        listOf("E004" to "G004", "E005" to "G004", "E006" to "G006").forEach { (format, target) ->
            val response = createExercise(cookieValue, format, target, "pilot-2", "generator-2")
            assertEquals(HttpStatusCode.Created, response.status)
            val body = wireJson.parseToJsonElement(response.bodyAsText()).jsonObject
            val prompt = body.getValue("prompt").jsonObject
            assertTrue(prompt.getValue("before").jsonPrimitive.content.isNotEmpty())
            assertTrue(prompt.getValue("after").jsonPrimitive.content.isNotEmpty())
            assertTrue(prompt.getValue("choices") is JsonArray)
            assertTrue(prompt.getValue("blocks") is JsonArray)
            assertNotNull(prompt["inputLabel"])
            if (format == "E004") assertNull(prompt["replacementWord"])
            else assertTrue(prompt.getValue("replacementWord").jsonPrimitive.content.isNotEmpty())
            assertFalse("answerKey" in body)
            assertFalse("blockRoles" in body)
            assertFalse("source" in body)
            val exerciseId = body.getValue("id").jsonPrimitive.content
            assertEquals(HttpStatusCode.BadRequest, submitOrder(cookieValue, exerciseId, emptyList()).status)
        }

        val orderBody = wireJson.parseToJsonElement(
            createExercise(cookieValue, "E003", "G005", "pilot-2", "generator-2").bodyAsText(),
        ).jsonObject
        val orderIds = (orderBody.getValue("prompt").jsonObject.getValue("blocks") as JsonArray)
            .map { it.jsonObject.getValue("id").jsonPrimitive.content }
        assertTrue(orderIds.size >= 2)
        assertEquals(
            HttpStatusCode.BadRequest,
            submitOrder(cookieValue, orderBody.getValue("id").jsonPrimitive.content, orderIds.dropLast(1) + orderIds.first()).status,
        )
    }

    @Test
    fun `legacy presentations omit new metadata and old persisted json remains readable`() {
        val engine = PilotEngine()
        val generated = engine.generate(GenerationRequest("E001", "G004", 0, 0, "pilot-1", "generator-1"))
        assertEquals(
            "1c8d09f406623fb4de656758f213374d174a00809c6efb004a5c0b2bf46ec4f8",
            generated.reproductionFingerprint(),
        )
        val serialized = wireJson.encodeToString(
            ExerciseResponse.serializer(),
            generated.toResponse(UUID.randomUUID().toString()),
        )
        assertFalse("inputLabel" in serialized)
        assertFalse("replacementWord" in serialized)
        assertFalse("lexicalTarget" in serialized)
        assertFalse("cue" in serialized)
        assertFalse("cueLanguage" in serialized)
        assertFalse("answerLanguage" in serialized)
        val restored = wireJson.decodeFromString(ExerciseResponse.serializer(), serialized)
        assertNull(restored.prompt.inputLabel)
        assertNull(restored.prompt.replacementWord)

        val oldPrompt = """{"before":"Jeg ","after":".","choices":["sove","sover"],"blocks":[],"punctuation":"."}"""
        val decoded = wireJson.decodeFromString(ExercisePromptDto.serializer(), oldPrompt)
        assertNull(decoded.inputLabel)
        assertNull(decoded.replacementWord)
        assertNull(decoded.cue)
        assertNull(decoded.cueLanguage)
        assertNull(decoded.answerLanguage)
        assertEquals(oldPrompt, wireJson.encodeToString(ExercisePromptDto.serializer(), decoded))
    }

    @Test
    fun `each new prompt metadata field participates in the reproduction fingerprint`() {
        val generated = PilotEngine().generate(
            GenerationRequest("E005", "G004", 0, 0, "pilot-2", "generator-2"),
        )
        val original = generated.reproductionFingerprint()
        assertNotEquals(
            original,
            generated.reproductionFingerprint(
                generated.prompt.copy(inputLabel = checkNotNull(generated.prompt.inputLabel) + " changed"),
            ),
        )
        assertNotEquals(
            original,
            generated.reproductionFingerprint(
                generated.prompt.copy(replacementWord = checkNotNull(generated.prompt.replacementWord) + " changed"),
            ),
        )
    }

    @Test
    fun `attempt revision follows stored pair and new attempts preserve replay semantics`() = testApplication {
        val repository = InMemoryAstridRepository()
        application { astridModule(config(), repository) }
        val cookieValue = createSession().first
        val legacyId = exerciseId(createExercise(cookieValue, "E001", "G004", "pilot-1", "generator-1").bodyAsText())
        val currentId = exerciseId(createExercise(cookieValue, "E004", "G004", "pilot-2", "generator-2").bodyAsText())

        val legacy = submit(cookieValue, UUID.randomUUID().toString(), legacyId, "x")
        assertEquals("evaluation-1", wireJson.decodeFromString<AttemptResponse>(legacy.bodyAsText()).evaluationRevision)

        val attemptId = UUID.randomUUID().toString()
        val first = submit(cookieValue, attemptId, currentId, "x")
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals("evaluation-2", wireJson.decodeFromString<AttemptResponse>(first.bodyAsText()).evaluationRevision)
        assertEquals(first.bodyAsText(), submit(cookieValue, attemptId, currentId, "x").bodyAsText())
        assertEquals(HttpStatusCode.Conflict, submit(cookieValue, attemptId, currentId, "different").status)

        repository.tamperFingerprint(UUID.fromString(currentId))
        assertEquals(first.bodyAsText(), submit(cookieValue, attemptId, currentId, "x").bodyAsText())
        assertEquals(
            HttpStatusCode.Conflict,
            submit(cookieValue, UUID.randomUUID().toString(), currentId, "x").status,
        )
    }

    private suspend fun ApplicationTestBuilder.createSession(): Pair<String, String> {
        val response = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val cookieValue = response.headers[HttpHeaders.SetCookie]!!
            .substringAfter("$SESSION_COOKIE=").substringBefore(';')
        return cookieValue to response.bodyAsText()
    }

    private suspend fun ApplicationTestBuilder.createExercise(
        cookieValue: String,
        format: String,
        target: String,
        contentRevision: String,
        generatorRevision: String,
    ) = client.post("/api/v1/exercises") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody(
            """{"formatId":"$format","targetSkillId":"$target","seed":0,"index":0,"contentRevision":"$contentRevision","generatorRevision":"$generatorRevision"}""",
        )
    }

    private suspend fun ApplicationTestBuilder.submit(
        cookieValue: String,
        attemptId: String,
        exerciseId: String,
        answer: String,
    ) = client.post("/api/v1/attempts") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody(
            wireJson.encodeToString(
                AttemptRequest.serializer(),
                AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Text(answer), hintUsed = false),
            ),
        )
    }

    private suspend fun ApplicationTestBuilder.submitOrder(
        cookieValue: String,
        exerciseId: String,
        blockIds: List<String>,
    ) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                wireJson.encodeToString(
                    AttemptRequest.serializer(),
                    AttemptRequest(
                        UUID.randomUUID().toString(),
                        exerciseId,
                        AttemptAnswerDto.Order(blockIds),
                        hintUsed = false,
                    ),
                ),
            )
        }

    private fun exerciseId(body: String) =
        (wireJson.parseToJsonElement(body) as JsonObject).getValue("id").jsonPrimitive.content

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config() = ServerConfig(
        true, false, null, false, false,
        setOf("http://localhost:8088"), setOf("localhost:8088"), 60_000, null,
        "jdbc:postgresql://unused", "r2dbc:postgresql://unused", "unused", "unused", 2,
    )

    private companion object {
        const val SESSION_COOKIE = "astrid_session"
    }
}
