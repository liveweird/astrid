package app.astrid.server

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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PracticeApplicationTest {
    @Test
    fun `practice owner cursor support and replay invariants hold`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val auth = authenticate()
        val practiceId = UUID.randomUUID().toString()
        val body = createBody(practiceId)

        assertEquals(HttpStatusCode.BadRequest, create(auth.cookie, null, body).status)
        assertEquals(HttpStatusCode.Conflict, create(auth.cookie, UUID.randomUUID().toString(), body).status)
        val created = create(auth.cookie, auth.ownerId, body)
        assertEquals(HttpStatusCode.Created, created.status)
        val createdText = created.bodyAsText()
        val initial = wireJson.parseToJsonElement(createdText).jsonObject
        assertEquals("0", initial.getValue("cursor").jsonPrimitive.content)
        assertEquals("active", initial.getValue("state").jsonPrimitive.content)
        val items = initial.getValue("items").jsonArray
        assertEquals(8, items.size)
        assertFalse("prompt" in items[1].jsonObject)
        assertTrue(initial.getValue("summary").jsonObject.getValue("rows").jsonArray.isEmpty())
        val exercise = initial.getValue("currentExercise").jsonObject
        val exerciseId = exercise.getValue("id").jsonPrimitive.content
        val outsider = authenticate()
        assertEquals(HttpStatusCode.NotFound, getPractice(outsider.cookie, practiceId).status)

        val replayCreate = create(auth.cookie, auth.ownerId, body)
        assertEquals(HttpStatusCode.OK, replayCreate.status)
        assertEquals(createdText, replayCreate.bodyAsText())
        assertEquals(HttpStatusCode.Conflict, support(auth, practiceId, """{"kind":"hint","position":1}""").status)
        val opened = support(auth, practiceId, """{"kind":"introduction"}""")
        assertEquals(HttpStatusCode.OK, opened.status)

        val attemptId = UUID.randomUUID().toString()
        val attemptBody = attemptBody(attemptId, exerciseId)
        val attempt = submit(auth.cookie, attemptBody)
        assertEquals(HttpStatusCode.OK, attempt.status)
        val attemptText = attempt.bodyAsText()
        val state = getPractice(auth.cookie, practiceId)
        val currentItem = wireJson.parseToJsonElement(state.bodyAsText()).jsonObject
            .getValue("items").jsonArray.first().jsonObject
        assertEquals("1", currentItem.getValue("attemptCount").jsonPrimitive.content)
        assertEquals("wrong", currentItem.getValue("lastAnswer").jsonObject.getValue("text").jsonPrimitive.content)
        assertTrue(currentItem.getValue("firstAttemptContext").jsonObject
            .getValue("introductionOpened").jsonPrimitive.content.toBoolean())

        val advanced = advance(auth, practiceId, 0)
        assertEquals(HttpStatusCode.OK, advanced.status)
        assertEquals("1", wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
            .getValue("cursor").jsonPrimitive.content)
        assertEquals(advanced.bodyAsText(), advance(auth, practiceId, 0).bodyAsText())
        assertEquals(attemptText, submit(auth.cookie, attemptBody).bodyAsText())
        assertEquals(
            HttpStatusCode.Conflict,
            submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), exerciseId)).status,
        )
    }

    @Test
    fun `failed practice evaluation rolls back attempt and context`() = testApplication {
        val fail = AtomicBoolean(true)
        application {
            val engine = app.astrid.engine.PilotEngine()
            astridModule(config(), InMemoryAstridRepository(), engine, evaluator = { exercise, answer ->
                if (fail.getAndSet(false)) error("offline")
                engine.evaluate(exercise, answer)
            })
        }
        val auth = authenticate()
        val practiceId = UUID.randomUUID().toString()
        val created = create(auth.cookie, auth.ownerId, createBody(practiceId))
        val exerciseId = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
            .getValue("currentExercise").jsonObject.getValue("id").jsonPrimitive.content
        val attemptId = UUID.randomUUID().toString()
        val body = attemptBody(attemptId, exerciseId)
        assertEquals(HttpStatusCode.ServiceUnavailable, submit(auth.cookie, body).status)
        val afterFailure = getPractice(auth.cookie, practiceId)
        assertEquals("0", wireJson.parseToJsonElement(afterFailure.bodyAsText()).jsonObject
            .getValue("summary").jsonObject.getValue("attemptCount").jsonPrimitive.content)
        val recovered = submit(auth.cookie, body)
        assertEquals(HttpStatusCode.OK, recovered.status)
        assertEquals("1", wireJson.parseToJsonElement(recovered.bodyAsText()).jsonObject
            .getValue("retryNumber").jsonPrimitive.content)
    }

    private suspend fun ApplicationTestBuilder.authenticate(): Auth {
        val session = client.post("/api/v1/development-session") {
            trustedHost()
            mutationHeaders()
            setBody("{}")
        }
        val cookie = assertNotNull(session.headers[HttpHeaders.SetCookie])
            .substringAfter("astrid_session=").substringBefore(';')
        val context = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        assertEquals(HttpStatusCode.OK, context.status)
        assertNull(context.headers[HttpHeaders.SetCookie])
        val owner = wireJson.parseToJsonElement(context.bodyAsText()).jsonObject
            .getValue("ownerSessionId").jsonPrimitive.content
        return Auth(cookie, owner)
    }

    private suspend fun ApplicationTestBuilder.create(cookie: String, owner: String?, body: String) =
        client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie("astrid_session", cookie)
            mutationHeaders()
            owner?.let { header("X-Astrid-Practice-Owner", it) }
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.support(auth: Auth, id: String, body: String) =
        client.post("/api/v1/practice-sessions/$id/support") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
            mutationHeaders()
            header("X-Astrid-Practice-Owner", auth.ownerId)
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.advance(auth: Auth, id: String, position: Int) =
        client.post("/api/v1/practice-sessions/$id/advance") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
            mutationHeaders()
            header("X-Astrid-Practice-Owner", auth.ownerId)
            setBody("""{"position":$position}""")
        }

    private suspend fun ApplicationTestBuilder.submit(cookie: String, body: String) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie("astrid_session", cookie)
            mutationHeaders()
            setBody(body)
        }

    private suspend fun ApplicationTestBuilder.getPractice(cookie: String, id: String) =
        client.get("/api/v1/practice-sessions/$id") {
            trustedHost()
            cookie("astrid_session", cookie)
        }

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() = header(HttpHeaders.Host, "localhost:8088")

    private fun io.ktor.client.request.HttpRequestBuilder.mutationHeaders() {
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
    }

    private fun config() = ServerConfig(
        developmentMode = true,
        inspectorEnabled = false,
        inspectorKey = null,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = setOf("http://localhost:8088"),
        allowedHosts = setOf("localhost:8088"),
        sessionTtlMillis = 60_000,
        webStaticDir = null,
        jdbcUrl = "jdbc:postgresql://unused",
        r2dbcUrl = "r2dbc:postgresql://unused",
        postgresUser = "unused",
        postgresPassword = "unused",
        poolMaxSize = 2,
    )

    private fun createBody(id: String) =
        """{"practiceSessionId":"$id","focusId":"reading-basics","planRevision":"practice-1","seed":20261107}"""

    private fun attemptBody(attemptId: String, exerciseId: String) =
        """{"attemptId":"$attemptId","exerciseId":"$exerciseId","answer":{"kind":"text","text":"wrong"},"hintUsed":false}"""

    private data class Auth(val cookie: String, val ownerId: String)
}
