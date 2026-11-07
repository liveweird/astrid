@file:Suppress("MaxLineLength")

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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VocabularyApplicationTest {
    @Test
    fun `published pair exercise json omits all vocabulary-only fields`() {
        val engine = PilotEngine()
        listOf(
            GenerationRequest("E001", "G004", 0, 0, "pilot-1", "generator-1"),
            GenerationRequest("E005", "G004", 0, 0, "pilot-2", "generator-2"),
        ).forEach { request ->
            val serialized = wireJson.encodeToString(
                ExerciseResponse.serializer(),
                engine.generate(request).toResponse(UUID.randomUUID().toString()),
            )
            val exercise = wireJson.parseToJsonElement(serialized).jsonObject
            assertFalse("lexicalTarget" in exercise)
            assertFalse("readingTarget" in exercise)
            assertTrue("targetSkillId" in exercise)
            val prompt = exercise.getValue("prompt").jsonObject
            assertFalse("cue" in prompt)
            assertFalse("cueLanguage" in prompt)
            assertFalse("answerLanguage" in prompt)
            assertFalse("passage" in prompt)
        }
    }

    @Test
    fun `catalog separates grammar targets from lexical tasks`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val cookieValue = createSession()
        val response = client.get("/api/v1/catalog") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val formats = response.json().getValue("formats") as JsonArray
        val grammar = formats.first { it.jsonObject.getValue("id").jsonPrimitive.content == "E006" }.jsonObject
        assertEquals("G006", grammar.getValue("targetSkillId").jsonPrimitive.content)
        assertFalse("lexicalTask" in grammar)
        assertFalse("readingTask" in grammar)
        val recognition = formats.first { it.jsonObject.getValue("id").jsonPrimitive.content == "E007" }.jsonObject
        assertFalse("targetSkillId" in recognition)
        assertEquals("meaning", recognition.getValue("lexicalTask").jsonObject.getValue("facet").jsonPrimitive.content)
        assertEquals("da-en", recognition.getValue("lexicalTask").jsonObject.getValue("direction").jsonPrimitive.content)
    }

    @Test
    fun `lexical metadata is public only at its bounded level and private source stays authorized`() = testApplication {
        val repository = InMemoryAstridRepository()
        application { astridModule(config(inspectorKey = INSPECTOR_KEY), repository) }
        val owner = createSession()
        val outsider = createSession()
        val created = createExercise(owner, "E008")
        assertEquals(HttpStatusCode.Created, created.status)
        val public = created.json()
        assertFalse("targetSkillId" in public)
        val lexicalTarget = public.getValue("lexicalTarget").jsonObject
        assertEquals(setOf("entryId", "senseId", "facet", "direction"), lexicalTarget.keys)
        assertEquals("singular-headword", lexicalTarget.getValue("facet").jsonPrimitive.content)
        val prompt = public.getValue("prompt").jsonObject
        assertEquals("en", prompt.getValue("cueLanguage").jsonPrimitive.content)
        assertEquals("da", prompt.getValue("answerLanguage").jsonPrimitive.content)
        assertEquals("", prompt.getValue("before").jsonPrimitive.content)
        assertEquals("", prompt.getValue("after").jsonPrimitive.content)
        assertEquals("", prompt.getValue("punctuation").jsonPrimitive.content)
        assertFalse("lexicalSource" in public)

        val exerciseId = public.getValue("id").jsonPrimitive.content
        val denied = inspect(outsider, exerciseId)
        assertEquals(HttpStatusCode.NotFound, denied.status)
        val inspected = inspect(owner, exerciseId)
        assertEquals(HttpStatusCode.OK, inspected.status)
        val private = inspected.json()
        assertFalse("canonicalSentenceId" in private)
        assertFalse("constructionId" in private)
        assertFalse("canonicalSentence" in private)
        assertEquals(emptyList(), (private.getValue("clauseRoles") as JsonArray).toList())
        val source = private.getValue("lexicalSource").jsonObject
        assertEquals(lexicalTarget.getValue("entryId"), source.getValue("entryId"))
        assertTrue(source.getValue("headword").jsonPrimitive.content.isNotBlank())
        assertTrue(source.getValue("gloss").jsonPrimitive.content.isNotBlank())
        assertTrue(source.getValue("article").jsonPrimitive.content in setOf("en", "et"))

        val answer = "${source.getValue("article").jsonPrimitive.content} ${source.getValue("headword").jsonPrimitive.content}"
        val attempt = submit(owner, exerciseId, answer)
        assertEquals(HttpStatusCode.OK, attempt.status)
        val result = attempt.json()
        assertEquals("met", result.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("danish-headword", result.getValue("linguisticScope").jsonPrimitive.content)
        assertEquals("not-met", result.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertEquals("", result.getValue("completedSentence").jsonPrimitive.content)
    }

    @Test
    fun `target and revision validation rejects conflicting request shapes`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val cookieValue = createSession()
        assertEquals(HttpStatusCode.BadRequest, createExercise(cookieValue, "E001").status)
        assertEquals(HttpStatusCode.BadRequest, createExercise(cookieValue, "E007", "G004").status)
        assertEquals(
            HttpStatusCode.BadRequest,
            createExercise(cookieValue, "E007", content = "pilot-2", generator = "generator-2").status,
        )
        assertEquals(
            HttpStatusCode.Conflict,
            createExercise(cookieValue, "E007", content = "pilot-3", generator = "generator-2").status,
        )
    }

    @Test
    fun `pair three fingerprints cover every new lexical field`() {
        val generated = PilotEngine().generate(GenerationRequest("E008", null, 47, 0, "pilot-3", "generator-3"))
        val original = generated.reproductionFingerprint()
        val target = requireNotNull(generated.lexicalTarget)
        val source = requireNotNull(generated.inspector.lexicalSource)
        val mutations = listOf(
            generated.reproductionFingerprint(lexicalTargetForFingerprint = target.copy(entryId = target.entryId + "x")),
            generated.reproductionFingerprint(lexicalTargetForFingerprint = target.copy(senseId = target.senseId + "x")),
            generated.reproductionFingerprint(lexicalTargetForFingerprint = target.copy(facet = target.facet + "x")),
            generated.reproductionFingerprint(lexicalTargetForFingerprint = target.copy(direction = target.direction + "x")),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(cue = generated.prompt.cue + "x")),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(cueLanguage = "x")),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(answerLanguage = "x")),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(lexicalSource = source.copy(entryId = source.entryId + "x"))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(lexicalSource = source.copy(senseId = source.senseId + "x"))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(lexicalSource = source.copy(headword = source.headword + "x"))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(lexicalSource = source.copy(gloss = source.gloss + "x"))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(lexicalSource = source.copy(article = source.article + "x"))),
            generated.reproductionFingerprint(supportSkillIdsForFingerprint = listOf("G999")),
        )
        mutations.forEach { assertNotEquals(original, it) }
    }

    @Test
    fun `legacy attempt json remains readable without lexical metadata`() {
        val json = """{"attemptId":"00000000-0000-0000-0000-000000000001","exerciseId":"00000000-0000-0000-0000-000000000002","targetSatisfaction":"met","linguisticStatus":"acceptable","issues":[],"feedback":"Correct","completedSentence":"Hun læser.","evaluationRevision":"evaluation-1","hintUsed":false,"retryNumber":1,"createdAt":1}"""
        val decoded = wireJson.decodeFromString(AttemptResponse.serializer(), json)
        assertNull(decoded.linguisticScope)
        assertNull(decoded.responseConstraintStatus)
        assertNull(decoded.meaningSatisfaction)
        assertFalse("linguisticScope" in wireJson.encodeToString(AttemptResponse.serializer(), decoded))
        assertFalse("responseConstraintStatus" in wireJson.encodeToString(AttemptResponse.serializer(), decoded))
        assertFalse("meaningSatisfaction" in wireJson.encodeToString(AttemptResponse.serializer(), decoded))
        assertEquals(json, wireJson.encodeToString(AttemptResponse.serializer(), decoded))
        listOf("evaluation-1", "evaluation-2", "evaluation-3").forEach { revision ->
            val published = wireJson.encodeToString(AttemptResponse.serializer(), decoded.copy(evaluationRevision = revision))
            assertFalse("linguisticScope" in published)
            assertFalse("responseConstraintStatus" in published)
            assertFalse("meaningSatisfaction" in published)
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

    private suspend fun ApplicationTestBuilder.createExercise(
        cookieValue: String,
        format: String,
        target: String? = null,
        content: String = "pilot-3",
        generator: String = "generator-3",
    ) = client.post("/api/v1/exercises") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        val targetJson = target?.let { ",\"targetSkillId\":\"$it\"" }.orEmpty()
        setBody("""{"formatId":"$format"$targetJson,"seed":47,"index":0,"contentRevision":"$content","generatorRevision":"$generator"}""")
    }

    private suspend fun ApplicationTestBuilder.inspect(cookieValue: String, exerciseId: String) =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Inspector-Key", INSPECTOR_KEY)
        }

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, exerciseId: String, answer: String) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                wireJson.encodeToString(
                    AttemptRequest.serializer(),
                    AttemptRequest(UUID.randomUUID().toString(), exerciseId, AttemptAnswerDto.Text(answer), false),
                ),
            )
        }

    private suspend fun io.ktor.client.statement.HttpResponse.json() =
        wireJson.parseToJsonElement(bodyAsText()) as JsonObject

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config(inspectorKey: String? = null) = ServerConfig(
        true, inspectorKey != null, inspectorKey, false, false,
        setOf("http://localhost:8088"), setOf("localhost:8088"), 60_000, null,
        "jdbc:postgresql://unused", "r2dbc:postgresql://unused", "unused", "unused", 2,
    )

    private companion object {
        const val SESSION_COOKIE = "astrid_session"
        const val INSPECTOR_KEY = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk"
    }
}
