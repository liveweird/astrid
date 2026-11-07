@file:Suppress("MaxLineLength") // Complete JSON wire fixtures remain readable on one line.

package app.astrid.server

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
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CancellationException

class ApplicationHardeningTest {
    @Test
    fun `stored attempt replays after its exercise revision becomes unsupported`() = testApplication {
        val repository = InMemoryAstridRepository()
        application { astridModule(config(), repository) }
        val cookie = createSession()
        val exerciseId = createExercise(cookie).id
        val attemptId = UUID.randomUUID().toString()
        val body = attemptBody(attemptId, exerciseId, "læser")
        val original = submit(cookie, body)
        assertEquals(HttpStatusCode.OK, original.status)
        val originalBody = original.bodyAsText()
        repository.tamperRevision(UUID.fromString(exerciseId))

        assertEquals(originalBody, submit(cookie, body).bodyAsText())
        val reread = client.get("/api/v1/attempts/$attemptId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
        }
        assertEquals(originalBody, reread.bodyAsText())
        assertEquals(HttpStatusCode.Conflict, submit(cookie, attemptBody(attemptId, exerciseId, "læse")).status)
        assertEquals(
            HttpStatusCode.Conflict,
            submit(cookie, attemptBody(UUID.randomUUID().toString(), exerciseId, "læser")).status,
        )
    }

    @Test
    fun `failed evaluation stores nothing and same request recovers at retry one`() = testApplication {
        val repository = InMemoryAstridRepository()
        val engine = PilotEngine()
        var available = false
        application {
            astridModule(
                config(),
                repository,
                engine,
                evaluator = { exercise, answer ->
                    if (!available) error("evaluation offline")
                    engine.evaluate(exercise, answer)
                },
            )
        }
        val cookie = createSession()
        val exerciseId = createExercise(cookie).id
        val attemptId = UUID.randomUUID().toString()
        val body = attemptBody(attemptId, exerciseId, "læser")
        assertEquals(HttpStatusCode.ServiceUnavailable, submit(cookie, body).status)
        val missing = client.get("/api/v1/attempts/$attemptId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
        }
        assertEquals(HttpStatusCode.NotFound, missing.status)
        available = true
        val recovered = submit(cookie, body)
        assertEquals(HttpStatusCode.OK, recovered.status)
        assertEquals(1, wireJson.decodeFromString<AttemptResponse>(recovered.bodyAsText()).retryNumber)
    }

    @Test
    fun `cancelled evaluation is not graded and same request remains unused`() = testApplication {
        val repository = InMemoryAstridRepository()
        val engine = PilotEngine()
        var cancel = true
        application {
            astridModule(
                config(),
                repository,
                engine,
                evaluator = { exercise, answer ->
                    if (cancel) throw CancellationException("request cancelled")
                    engine.evaluate(exercise, answer)
                },
            )
        }
        val cookie = createSession()
        val exerciseId = createExercise(cookie).id
        val attemptId = UUID.randomUUID().toString()
        val body = attemptBody(attemptId, exerciseId, "læser")
        val cancelled = submit(cookie, body)
        assertEquals(HttpStatusCode.InternalServerError, cancelled.status)
        val missing = client.get("/api/v1/attempts/$attemptId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
        }
        assertEquals(HttpStatusCode.NotFound, missing.status)
        cancel = false
        val recovered = submit(cookie, body)
        assertEquals(HttpStatusCode.OK, recovered.status)
        assertEquals(1, wireJson.decodeFromString<AttemptResponse>(recovered.bodyAsText()).retryNumber)
    }

    @Test
    fun `all formats expose evaluation facets and reject malformed order answers`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val cookie = createSession()
        val present = createExercise(cookie, "E001", "G004", 0)
        val presentCorrect = present.prompt.choices.single { it.endsWith("r") }
        val presentWrong = present.prompt.choices.single { it != presentCorrect }
        assertEquals("met", submitText(cookie, present.id, presentCorrect).targetSatisfaction)
        assertEquals("not-met", submitText(cookie, present.id, presentWrong).targetSatisfaction)
        assertEquals("unknown", submitText(cookie, present.id, "xyz").linguisticStatus)

        val article = createExercise(cookie, "E002", "G006", 0)
        val noun = article.prompt.after.trim().removeSuffix(".")
        val expectedArticle = if (noun in setOf("bog", "avis", "bil", "hund")) "en" else "et"
        val wrongArticle = if (expectedArticle == "en") "et" else "en"
        assertEquals("met", submitText(cookie, article.id, expectedArticle).targetSatisfaction)
        assertEquals("known-issue", submitText(cookie, article.id, wrongArticle).linguisticStatus)
        assertEquals("unknown", submitText(cookie, article.id, "xyz").linguisticStatus)

        var order = createExercise(cookie, "E003", "G005", 0)
        var index = 1
        while (order.prompt.blocks.size != 3) order = createExercise(cookie, "E003", "G005", index++)
        val subjectWords = setOf("jeg", "du", "han", "hun", "vi", "i", "de", "det")
        val verbWords = setOf("har", "læser", "spiser", "sover", "er")
        val subject = order.prompt.blocks.single { it.text.lowercase() in subjectWords }.id
        val verb = order.prompt.blocks.single { it.text in verbWords }.id
        val complement = order.prompt.blocks.single { it.id != subject && it.id != verb }.id
        assertEquals("met", submitOrder(cookie, order.id, listOf(subject, verb, complement)).targetSatisfaction)
        assertEquals("not-met", submitOrder(cookie, order.id, listOf(complement, verb, subject)).targetSatisfaction)
        assertEquals("unknown", submitOrder(cookie, order.id, listOf(subject, complement, verb)).linguisticStatus)
        assertEquals(HttpStatusCode.BadRequest, submitOrderRaw(cookie, order.id, listOf(subject, subject, verb)).status)
        assertEquals(HttpStatusCode.BadRequest, submitOrderRaw(cookie, order.id, listOf(subject, verb)).status)
        assertEquals(HttpStatusCode.BadRequest, submit(cookie, attemptBody(UUID.randomUUID().toString(), order.id, "wrong-kind")).status)
    }

    @Test
    fun `malformed unknown and oversized json bodies are bounded`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        assertEquals(HttpStatusCode.BadRequest, sessionRequest("{").status)
        assertEquals(HttpStatusCode.BadRequest, sessionRequest("""{"unexpected":true}""").status)
        assertEquals(
            HttpStatusCode.PayloadTooLarge,
            sessionRequest("""{"padding":"${"x".repeat(17 * 1024)}"}""").status,
        )
    }

    @Test
    fun `uuid boundaries reject jdk shorthand and accept canonical uppercase`() = testApplication {
        val repository = InMemoryAstridRepository()
        val engine = PilotEngine()
        val evaluations = AtomicInteger()
        application {
            astridModule(
                config(),
                repository,
                engine,
                evaluator = { exercise, answer ->
                    evaluations.incrementAndGet()
                    engine.evaluate(exercise, answer)
                },
            )
        }
        val cookie = createSession()
        val exercise = createExercise(cookie)
        val malformedIds = listOf(
            "1-1-1-1-1",
            "${UUID.randomUUID()}0",
            "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz",
            "not-a-uuid",
        )
        malformedIds.forEach { malformed ->
            assertEquals(
                HttpStatusCode.BadRequest,
                submit(cookie, attemptBody(malformed, exercise.id, "læser")).status,
            )
            assertEquals(HttpStatusCode.BadRequest, client.get("/api/v1/exercises/$malformed") {
                trustedHost()
                cookie(SESSION_COOKIE, cookie)
            }.status)
        }
        val unusedAttempt = UUID.randomUUID().toString()
        assertEquals(
            HttpStatusCode.BadRequest,
            submit(cookie, attemptBody(unusedAttempt, "1-1-1-1-1", "læser")).status,
        )
        assertEquals(0, evaluations.get())
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/attempts/$unusedAttempt") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
        }.status)

        val uppercaseAttempt = UUID.randomUUID().toString().uppercase()
        val uppercaseExercise = exercise.id.uppercase()
        val accepted = submit(cookie, attemptBody(uppercaseAttempt, uppercaseExercise, "læser"))
        assertEquals(HttpStatusCode.OK, accepted.status)
        assertEquals(uppercaseAttempt, wireJson.decodeFromString<AttemptResponse>(accepted.bodyAsText()).attemptId)
        assertEquals(HttpStatusCode.OK, client.get("/api/v1/attempts/$uppercaseAttempt") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
        }.status)
    }

    private suspend fun ApplicationTestBuilder.createSession(): String {
        val response = sessionRequest("{}")
        assertEquals(HttpStatusCode.OK, response.status)
        return response.headers[HttpHeaders.SetCookie]!!.substringAfter("$SESSION_COOKIE=").substringBefore(';')
    }

    private suspend fun ApplicationTestBuilder.sessionRequest(body: String) = client.post("/api/v1/development-session") {
        trustedHost()
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun ApplicationTestBuilder.createExercise(
        cookieValue: String,
        formatId: String = "E001",
        targetSkillId: String = "G004",
        index: Int = 0,
    ): ExerciseResponse {
        val response = client.post("/api/v1/exercises") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"$formatId","targetSkillId":"$targetSkillId","seed":0,"index":$index,"contentRevision":"pilot-1","generatorRevision":"generator-1"}""",
            )
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return wireJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, body: String) = client.post("/api/v1/attempts") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun ApplicationTestBuilder.submitText(
        cookieValue: String,
        exerciseId: String,
        text: String,
    ): AttemptResponse {
        val response = submit(cookieValue, attemptBody(UUID.randomUUID().toString(), exerciseId, text))
        assertEquals(HttpStatusCode.OK, response.status)
        return wireJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.submitOrder(
        cookieValue: String,
        exerciseId: String,
        blockIds: List<String>,
    ): AttemptResponse {
        val response = submitOrderRaw(cookieValue, exerciseId, blockIds)
        assertEquals(HttpStatusCode.OK, response.status)
        return wireJson.decodeFromString(response.bodyAsText())
    }

    private suspend fun ApplicationTestBuilder.submitOrderRaw(
        cookieValue: String,
        exerciseId: String,
        blockIds: List<String>,
    ) = submit(
        cookieValue,
        """{"attemptId":"${UUID.randomUUID()}","exerciseId":"$exerciseId","answer":{"kind":"order","blockIds":${wireJson.encodeToString(blockIds)}},"hintUsed":false}""",
    )

    private fun attemptBody(attemptId: String, exerciseId: String, text: String) =
        """{"attemptId":"$attemptId","exerciseId":"$exerciseId","answer":{"kind":"text","text":"$text"},"hintUsed":false}"""

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
