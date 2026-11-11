@file:Suppress("MaxLineLength") // Complete request fixtures are clearer on one line.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.authz.Conflict
import app.astrid.server.authz.NotFound
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toResponse
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class WritingReadingApplicationTest {
    @Test
    fun `catalog and public exercises expose only bounded writing and reading descriptors`() = testApplication {
        application { astridModule(config(inspectorKey = INSPECTOR_KEY), InMemoryStores()) }
        val owner = createSession()
        val catalog = client.get("/api/v1/catalog") {
            trustedHost()
            cookie(SESSION_COOKIE, owner)
        }.json()
        assertEquals("pilot-13", catalog.getValue("contentRevision").jsonPrimitive.content)
        val formats = catalog.getValue("formats") as JsonArray
        val writingFormat = formats.single { it.jsonObject.getValue("id").jsonPrimitive.content == "E009" }.jsonObject
        assertEquals("G005", writingFormat.getValue("targetSkillId").jsonPrimitive.content)
        assertFalse("readingTask" in writingFormat)
        val readingFormat = formats.single { it.jsonObject.getValue("id").jsonPrimitive.content == "E010" }.jsonObject
        assertFalse("targetSkillId" in readingFormat)
        assertFalse("lexicalTask" in readingFormat)
        assertEquals(
            "participant-action-complement",
            readingFormat.getValue("readingTask").jsonObject.getValue("facet").jsonPrimitive.content,
        )

        val writing = createExercise(owner, "E009", "G005").json()
        assertFalse("readingTarget" in writing)
        assertFalse("readingSource" in writing)
        val writingPrompt = writing.getValue("prompt").jsonObject
        assertEquals("en", writingPrompt.getValue("cueLanguage").jsonPrimitive.content)
        assertEquals("da", writingPrompt.getValue("answerLanguage").jsonPrimitive.content)
        assertFalse("passage" in writingPrompt)

        val reading = createExercise(owner, "E010").json()
        assertFalse("targetSkillId" in reading)
        assertFalse("lexicalTarget" in reading)
        assertFalse("readingSource" in reading)
        assertEquals(
            "participant-action-complement",
            reading.getValue("readingTarget").jsonObject.getValue("facet").jsonPrimitive.content,
        )
        val readingPrompt = reading.getValue("prompt").jsonObject
        assertEquals(3, readingPrompt.getValue("passage").jsonArray.size)
        assertEquals(3, readingPrompt.getValue("choices").jsonArray.size)
        assertEquals("en", readingPrompt.getValue("cueLanguage").jsonPrimitive.content)
        assertEquals("en", readingPrompt.getValue("answerLanguage").jsonPrimitive.content)
    }

    @Test
    fun `writing and reading attempts expose four facets and replay before regeneration`() = testApplication {
        val repository = InMemoryStores()
        application { astridModule(config(inspectorKey = INSPECTOR_KEY), repository) }
        val owner = createSession()
        val outsider = createSession()

        val writing = createExercise(owner, "E009", "G005").json()
        val writingId = writing.getValue("id").jsonPrimitive.content
        val writingInspector = inspect(owner, writingId).json()
        assertFalse("readingSource" in writingInspector)
        val canonical = writingInspector.getValue("canonicalSentence").jsonPrimitive.content
        val writingAttemptId = UUID.randomUUID().toString()
        val writingAttempt = submit(owner, writingAttemptId, writingId, canonical)
        assertEquals(HttpStatusCode.OK, writingAttempt.status)
        val writingResult = writingAttempt.json()
        assertEquals("met", writingResult.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("met", writingResult.getValue("meaningSatisfaction").jsonPrimitive.content)
        assertEquals("acceptable", writingResult.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("danish-sentence", writingResult.getValue("linguisticScope").jsonPrimitive.content)
        assertEquals("met", writingResult.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertEquals(canonical, writingResult.getValue("completedSentence").jsonPrimitive.content)

        val reading = createExercise(owner, "E010").json()
        val readingId = reading.getValue("id").jsonPrimitive.content
        assertEquals(HttpStatusCode.NotFound, inspect(outsider, readingId).status)
        val readingInspector = inspect(owner, readingId).json()
        val readingSource = readingInspector.getValue("readingSource").jsonObject
        assertEquals(3, readingSource.getValue("propositions").jsonArray.size)
        assertEquals(
            reading.getValue("prompt").jsonObject.getValue("passage").jsonArray.size,
            readingSource.getValue("propositions").jsonArray.size,
        )
        val readingAnswer = readingSource.getValue("answer").jsonPrimitive.content
        val readingAttemptId = UUID.randomUUID().toString()
        val first = submit(owner, readingAttemptId, readingId, readingAnswer)
        assertEquals(HttpStatusCode.OK, first.status)
        val firstBody = first.bodyAsText()
        val readingResult = wireJson.parseToJsonElement(firstBody).jsonObject
        assertEquals("met", readingResult.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("acceptable", readingResult.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("english-choice", readingResult.getValue("linguisticScope").jsonPrimitive.content)
        assertEquals("met", readingResult.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertFalse("meaningSatisfaction" in readingResult)

        assertEquals(firstBody, submit(owner, readingAttemptId, readingId, readingAnswer).bodyAsText())
        repository.tamperFingerprint(UUID.fromString(readingId))
        assertEquals(firstBody, submit(owner, readingAttemptId, readingId, readingAnswer).bodyAsText())
        assertEquals(
            HttpStatusCode.Conflict,
            submit(owner, UUID.randomUUID().toString(), readingId, readingAnswer).status,
        )
        assertEquals(HttpStatusCode.NotFound, getAttempt(outsider, readingAttemptId).status)
    }

    @Test
    fun `writing reference language is public and fingerprinted without changing legacy omission`() {
        val engine = PilotEngine()
        val writing = engine.generate(GenerationRequest("E009", "G005", 47, 5, "pilot-4", "generator-4"))
        val reference = writing.toResponse("writing").introduction
        kotlin.test.assertTrue(reference.isNotEmpty())
        kotlin.test.assertTrue(reference.all { it.bodyLanguage == "da" })
        assertNotEquals(
            writing.reproductionFingerprint(),
            writing.reproductionFingerprint(
                introductionForFingerprint = writing.introduction.map { it.copy(bodyLanguage = "en") },
            ),
        )
        val legacy = engine.generate(GenerationRequest("E004", "G004", 47, 5, "pilot-3", "generator-3"))
        assertFalse("bodyLanguage" in wireJson.encodeToString(legacy.toResponse("legacy")))
    }

    @Test
    fun `pair four fingerprint frames every nested reading value and order`() {
        val generated = PilotEngine().generate(GenerationRequest("E010", null, 47, 0, "pilot-4", "generator-4"))
        val original = generated.reproductionFingerprint()
        val source = requireNotNull(generated.inspector.readingSource)
        val first = source.propositions.first()
        val propositionMutations = listOf(
            first.copy(sentenceId = first.sentenceId + "x"),
            first.copy(subjectId = first.subjectId + "x"),
            first.copy(verbId = first.verbId + "x"),
            first.copy(objectId = first.objectId + "x"),
        ).map { changed -> source.copy(propositions = listOf(changed) + source.propositions.drop(1)) }
        val inspectorMutations = propositionMutations + listOf(
            source.copy(propositions = source.propositions.reversed()),
            source.copy(questionSubjectId = source.questionSubjectId + "x"),
            source.copy(questionVerbId = source.questionVerbId + "x"),
            source.copy(answerNounId = source.answerNounId + "x"),
            source.copy(answer = source.answer + "x"),
        )
        inspectorMutations.forEach { changed ->
            assertNotEquals(
                original,
                generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(readingSource = changed)),
            )
        }

        assertNotEquals(
            original,
            generated.reproductionFingerprint(readingTargetForFingerprint = requireNotNull(generated.readingTarget).copy(facet = "x")),
        )
        val passage = requireNotNull(generated.prompt.passage)
        assertNotEquals(
            original,
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(passage = passage.reversed())),
        )
        val splitOne = generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(passage = listOf("ab", "c")))
        val splitTwo = generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(passage = listOf("a", "bc")))
        assertNotEquals(splitOne, splitTwo)
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
            setBody("""{"formatId":"$format"$targetJson,"seed":47,"index":0,"contentRevision":"pilot-4","generatorRevision":"generator-4"}""")
        }

    private suspend fun ApplicationTestBuilder.inspect(cookieValue: String, exerciseId: String) =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Inspector-Key", INSPECTOR_KEY)
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
        setBody(wireJson.encodeToString(AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Text(answer), false)))
    }

    private suspend fun ApplicationTestBuilder.getAttempt(cookieValue: String, attemptId: String) =
        client.get("/api/v1/attempts/$attemptId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
        }

    private suspend fun HttpResponse.json() = wireJson.parseToJsonElement(bodyAsText()) as JsonObject

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config(inspectorKey: String) = ServerConfig(
        true, true, inspectorKey, false, false,
        setOf("http://localhost:8088"), setOf("localhost:8088"), 60_000, null,
        "jdbc:postgresql://unused", "r2dbc:postgresql://unused", "unused", "unused", 2,
    )

    private companion object {
        const val SESSION_COOKIE = "astrid_session"
        const val INSPECTOR_KEY = "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk"
    }
}
