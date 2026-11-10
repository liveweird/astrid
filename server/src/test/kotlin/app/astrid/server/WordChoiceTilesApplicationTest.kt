@file:Suppress("MaxLineLength") // Complete request fixtures remain readable on one line.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
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

class WordChoiceTilesApplicationTest {
    @Test
    fun `pair five fingerprint frames tile prompt and every private nested value`() {
        val generated = PilotEngine().generate(GenerationRequest("E013", "G004", 47, 0, "pilot-5", "generator-5"))
        val original = generated.reproductionFingerprint()
        val source = requireNotNull(generated.inspector.tileSource)
        val firstToken = source.tokens.first()
        val firstOrder = source.licensedOrders.first()
        val firstDistractor = source.distractors.single()
        val mutations = listOf(
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(tileMode = "changed")),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(requiredTileCount = 99)),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(tileMode = null)),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(tileSource = null)),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(tokens = source.tokens.reversed()),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(tokens = listOf(firstToken.copy(blockId = firstToken.blockId + "x")) + source.tokens.drop(1)),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(tokens = listOf(firstToken.copy(lexicalId = firstToken.lexicalId + "x")) + source.tokens.drop(1)),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(tokens = listOf(firstToken.copy(form = firstToken.form + "x")) + source.tokens.drop(1)),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(
                        licensedOrders = listOf(firstOrder.copy(kind = firstOrder.kind + "x")) + source.licensedOrders.drop(1),
                    ),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(
                        licensedOrders = listOf(firstOrder.copy(blockIds = firstOrder.blockIds.reversed())) + source.licensedOrders.drop(1),
                    ),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(distractors = listOf(firstDistractor.copy(blockId = firstDistractor.blockId + "x"))),
                ),
            ),
            generated.reproductionFingerprint(
                inspectorForFingerprint = generated.inspector.copy(
                    tileSource = source.copy(distractors = listOf(firstDistractor.copy(rationale = firstDistractor.rationale + "x"))),
                ),
            ),
        )
        mutations.forEach { assertNotEquals(original, it) }
    }

    @Test
    fun `catalog and public exercises expose bounded pair five descriptors without private answers`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val catalog = client.get("/api/v1/catalog") {
            trustedHost()
            cookie(SESSION_COOKIE, owner)
        }.json()
        assertEquals("pilot-10", catalog.getValue("contentRevision").jsonPrimitive.content)
        assertEquals("generator-10", catalog.getValue("generatorRevision").jsonPrimitive.content)
        assertEquals("evaluation-10", catalog.getValue("evaluationRevision").jsonPrimitive.content)
        val formats = catalog.getValue("formats").jsonArray.associateBy { it.jsonObject.getValue("id").jsonPrimitive.content }
        assertEquals("en-da", formats.getValue("E011").jsonObject.getValue("lexicalTask").jsonObject.getValue("direction").jsonPrimitive.content)
        assertEquals("G005", formats.getValue("E012").jsonObject.getValue("targetSkillId").jsonPrimitive.content)
        assertEquals("G004", formats.getValue("E013").jsonObject.getValue("targetSkillId").jsonPrimitive.content)

        val choice = createExercise(owner, "E011").json()
        val choicePrompt = choice.getValue("prompt").jsonObject
        assertEquals(4, choicePrompt.getValue("choices").jsonArray.size)
        assertEquals("en", choicePrompt.getValue("cueLanguage").jsonPrimitive.content)
        assertEquals("da", choicePrompt.getValue("answerLanguage").jsonPrimitive.content)
        assertFalse("tileSource" in choice)
        assertFalse("tileMode" in choicePrompt)

        listOf("E012" to "G005", "E013" to "G004").forEach { (format, target) ->
            val exercise = createExercise(owner, format, target).json()
            val prompt = exercise.getValue("prompt").jsonObject
            assertEquals(if (format == "E012") "use-all" else "with-spares", prompt.getValue("tileMode").jsonPrimitive.content)
            assertTrue(prompt.getValue("requiredTileCount").jsonPrimitive.content.toInt() in setOf(2, 4))
            assertEquals("en", prompt.getValue("cueLanguage").jsonPrimitive.content)
            assertEquals("da", prompt.getValue("answerLanguage").jsonPrimitive.content)
            assertFalse("tileSource" in exercise)
            assertFalse("canonicalSentence" in exercise)
        }
    }

    @Test
    fun `word choice validates only supplied options after ownership and preserves raw replay`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val outsider = createSession()
        val exercise = createExercise(owner, "E011").json()
        val exerciseId = exercise.getValue("id").jsonPrimitive.content
        val choices = exercise.getValue("prompt").jsonObject.getValue("choices").jsonArray.map { it.jsonPrimitive.content }
        val inspector = inspect(owner, exerciseId).json()
        val source = inspector.getValue("lexicalSource").jsonObject
        val answer = "${source.getValue("article").jsonPrimitive.content} ${source.getValue("headword").jsonPrimitive.content}"
        assertTrue(answer in choices)

        val outsiderAttempt = UUID.randomUUID().toString()
        assertEquals(HttpStatusCode.NotFound, submitText(outsider, outsiderAttempt, exerciseId, "not supplied").status)
        val raw = "  $answer  "
        val first = submitText(owner, outsiderAttempt, exerciseId, raw)
        assertEquals(HttpStatusCode.OK, first.status)
        val result = first.json()
        assertEquals("met", result.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("met", result.getValue("meaningSatisfaction").jsonPrimitive.content)
        assertEquals("met", result.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertEquals("acceptable", result.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("danish-choice", result.getValue("linguisticScope").jsonPrimitive.content)
        assertEquals(1, result.getValue("retryNumber").jsonPrimitive.content.toInt())
        assertEquals(first.bodyAsText(), submitText(owner, outsiderAttempt, exerciseId, raw).bodyAsText())

        val other = choices.first { it != answer }
        assertEquals("not-met", submitText(owner, UUID.randomUUID().toString(), exerciseId, other).json().getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals(HttpStatusCode.BadRequest, submitText(owner, UUID.randomUUID().toString(), exerciseId, "not supplied").status)
        assertEquals(HttpStatusCode.BadRequest, submitOrder(owner, UUID.randomUUID().toString(), exerciseId, emptyList()).status)
    }

    @Test
    fun `tile submissions grade incomplete selections while rejecting unknown and duplicate ids`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()

        val all = createExercise(owner, "E012", "G005").json()
        val allId = all.getValue("id").jsonPrimitive.content
        val allInspector = inspect(owner, allId).json()
        val allSource = allInspector.getValue("tileSource").jsonObject
        val subjectFirst = allSource.getValue("licensedOrders").jsonArray
            .map { it.jsonObject }
            .single { it.getValue("kind").jsonPrimitive.content == "subject-first" }
            .getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }
        val complete = submitOrder(owner, UUID.randomUUID().toString(), allId, subjectFirst)
        assertEquals(HttpStatusCode.OK, complete.status)
        assertEquals("met", complete.json().getValue("targetSatisfaction").jsonPrimitive.content)
        val incomplete = submitOrder(owner, UUID.randomUUID().toString(), allId, subjectFirst.dropLast(1))
        assertEquals(HttpStatusCode.OK, incomplete.status)
        assertEquals("not-met", incomplete.json().getValue("responseConstraintStatus").jsonPrimitive.content)
        assertEquals(HttpStatusCode.BadRequest, submitOrder(owner, UUID.randomUUID().toString(), allId, listOf(subjectFirst.first(), subjectFirst.first())).status)
        assertEquals(HttpStatusCode.BadRequest, submitOrder(owner, UUID.randomUUID().toString(), allId, listOf("missing")).status)
        assertEquals(HttpStatusCode.BadRequest, submitText(owner, UUID.randomUUID().toString(), allId, "wrong kind").status)

        val spare = createExercise(owner, "E013", "G004").json()
        val spareId = spare.getValue("id").jsonPrimitive.content
        val spareSource = inspect(owner, spareId).json().getValue("tileSource").jsonObject
        val canonical = spareSource.getValue("licensedOrders").jsonArray
            .map { it.jsonObject }
            .single { it.getValue("kind").jsonPrimitive.content == "subject-first" }
            .getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }
        val distractor = spareSource.getValue("distractors").jsonArray.single().jsonObject.getValue("blockId").jsonPrimitive.content
        val correct = submitOrder(owner, UUID.randomUUID().toString(), spareId, canonical)
        assertEquals("met", correct.json().getValue("targetSatisfaction").jsonPrimitive.content)
        val bothVerbs = submitOrder(owner, UUID.randomUUID().toString(), spareId, canonical + distractor)
        assertEquals(HttpStatusCode.OK, bothVerbs.status)
        assertEquals("not-met", bothVerbs.json().getValue("responseConstraintStatus").jsonPrimitive.content)
    }

    @Test
    fun `malformed tile answers remain owner isolated and do not reserve retry numbers`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val outsider = createSession()
        listOf("E012" to "G005", "E013" to "G004").forEach { (format, target) ->
            val exercise = createExercise(owner, format, target).json()
            val id = exercise.getValue("id").jsonPrimitive.content
            val source = inspect(owner, id).json().getValue("tileSource").jsonObject
            val canonical = source.getValue("licensedOrders").jsonArray.map { it.jsonObject }
                .single { it.getValue("kind").jsonPrimitive.content == "subject-first" }
                .getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }
            listOf("duplicate", "unknown", "wrong-kind").forEachIndexed { index, malformed ->
                val attemptId = UUID.randomUUID().toString()
                listOf(outsider to HttpStatusCode.NotFound, owner to HttpStatusCode.BadRequest).forEach { (cookie, status) ->
                    val response = when (malformed) {
                        "duplicate" -> submitOrder(cookie, attemptId, id, listOf(canonical.first(), canonical.first()))
                        "unknown" -> submitOrder(cookie, attemptId, id, listOf("not-supplied"))
                        else -> submitText(cookie, attemptId, id, "wrong kind")
                    }
                    assertEquals(status, response.status)
                }
                val valid = submitOrder(owner, attemptId, id, canonical)
                assertEquals(HttpStatusCode.OK, valid.status)
                assertEquals(index + 1, valid.json().getValue("retryNumber").jsonPrimitive.content.toInt())
            }
        }
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

    private suspend fun ApplicationTestBuilder.createExercise(cookieValue: String, format: String, target: String? = null) =
        client.post("/api/v1/exercises") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            val targetJson = target?.let { ",\"targetSkillId\":\"$it\"" }.orEmpty()
            setBody("""{"formatId":"$format"$targetJson,"seed":47,"index":0,"contentRevision":"pilot-5","generatorRevision":"generator-5"}""")
        }

    private suspend fun ApplicationTestBuilder.inspect(cookieValue: String, exerciseId: String) =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Inspector-Key", INSPECTOR_KEY)
        }

    private suspend fun ApplicationTestBuilder.submitText(cookieValue: String, attemptId: String, exerciseId: String, answer: String) =
        submit(cookieValue, AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Text(answer), false))

    private suspend fun ApplicationTestBuilder.submitOrder(cookieValue: String, attemptId: String, exerciseId: String, blockIds: List<String>) =
        submit(cookieValue, AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Order(blockIds), false))

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, request: AttemptRequest) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(request))
        }

    private suspend fun HttpResponse.json() = wireJson.parseToJsonElement(bodyAsText()) as JsonObject

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
