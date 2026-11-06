@file:Suppress("MaxLineLength") // Complete JSON wire fixtures remain readable on one line.

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
import io.ktor.server.testing.testApplication
import java.util.UUID
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApplicationTest {
    @Test
    fun `development session fails closed`() = testApplication {
        application { astridModule(testConfig(developmentMode = false), InMemoryAstridRepository()) }
        val response = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `exercise omits inspector data and attempt retry is stable`() = testApplication {
        application { astridModule(testConfig(), InMemoryAstridRepository()) }
        val cookie = createSession()
        val exercise = client.post("/api/v1/exercises") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(exerciseRequest)
        }
        assertEquals(HttpStatusCode.Created, exercise.status)
        val exerciseBody = wireJson.parseToJsonElement(exercise.bodyAsText()) as JsonObject
        assertFalse("canonicalSentence" in exerciseBody)
        assertFalse("inspector" in exerciseBody)
        val exerciseId = exerciseBody.getValue("id").jsonPrimitive.content
        val attemptId = UUID.randomUUID().toString()
        val attemptBody = """{"attemptId":"$attemptId","exerciseId":"$exerciseId","answer":{"kind":"text","text":"læser"},"hintUsed":false}"""
        val first = submit(cookie, attemptBody)
        val duplicate = submit(cookie, attemptBody)
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(first.bodyAsText(), duplicate.bodyAsText())
        val changed = submit(cookie, attemptBody.replace("læser", "læse"))
        assertEquals(HttpStatusCode.Conflict, changed.status)
    }

    @Test
    fun `resources are isolated by development session`() = testApplication {
        application { astridModule(testConfig(), InMemoryAstridRepository()) }
        val owner = createSession()
        val outsider = createSession()
        val exercise = client.post("/api/v1/exercises") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, owner)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(exerciseRequest)
        }
        val id = (wireJson.parseToJsonElement(exercise.bodyAsText()) as JsonObject).getValue("id").jsonPrimitive.content
        val response = client.get("/api/v1/exercises/$id") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, outsider)
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `untrusted origin and forwarded headers are rejected`() = testApplication {
        application { astridModule(testConfig(), InMemoryAstridRepository()) }
        val origin = client.get("/api/v1/health") {
            trustedHost()
            header(HttpHeaders.Origin, "https://attacker.example")
        }
        assertEquals(HttpStatusCode.Forbidden, origin.status)
        val forwarded = client.get("/api/v1/health") {
            trustedHost()
            header("X-Forwarded-Host", "attacker.example")
        }
        assertEquals(HttpStatusCode.Forbidden, forwarded.status)
    }

    @Test
    fun `mutations require json media type`() = testApplication {
        application { astridModule(testConfig(), InMemoryAstridRepository()) }
        val missing = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
        }
        assertEquals(HttpStatusCode.BadRequest, missing.status)
        val wrong = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Text.Plain)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.UnsupportedMediaType, wrong.status)
    }

    @Test
    fun `inspector requires enabled key and session ownership`() = testApplication {
        val repository = InMemoryAstridRepository()
        application { astridModule(testConfig(inspectorKey = "k".repeat(32)), repository) }
        val owner = createSession()
        val outsider = createSession()
        val exerciseId = createExercise(owner)
        val wrongKey = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, owner)
            header("X-Astrid-Inspector-Key", "x".repeat(32))
        }
        assertEquals(HttpStatusCode.Forbidden, wrongKey.status)
        val crossSession = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, outsider)
            header("X-Astrid-Inspector-Key", "k".repeat(32))
        }
        assertEquals(HttpStatusCode.NotFound, crossSession.status)
        val ownerResponse = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, owner)
            header("X-Astrid-Inspector-Key", "k".repeat(32))
        }
        assertEquals(HttpStatusCode.OK, ownerResponse.status)
        assertTrue("canonicalSentenceId" in ownerResponse.bodyAsText())
    }

    @Test
    fun `inspector is hidden when disabled`() = testApplication {
        application { astridModule(testConfig(), InMemoryAstridRepository()) }
        val owner = createSession()
        val exerciseId = createExercise(owner)
        val response = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, owner)
            header("X-Astrid-Inspector-Key", "k".repeat(32))
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `fingerprint drift conflicts without persisting an attempt`() = testApplication {
        val repository = InMemoryAstridRepository()
        application { astridModule(testConfig(), repository) }
        val cookie = createSession()
        val exerciseId = createExercise(cookie)
        repository.tamperFingerprint(UUID.fromString(exerciseId))
        val attemptId = UUID.randomUUID().toString()
        val body =
            """{"attemptId":"$attemptId","exerciseId":"$exerciseId","answer":{"kind":"text","text":"læser"},"hintUsed":false}"""
        assertEquals(HttpStatusCode.Conflict, submit(cookie, body).status)
        val reread = client.get("/api/v1/attempts/$attemptId") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, cookie)
        }
        assertEquals(HttpStatusCode.NotFound, reread.status)
    }

    @Test
    fun `static assets are served before the spa fallback`() {
        val root = Files.createTempDirectory("astrid-static-test").toFile()
        try {
            val assets = root.resolve("assets").apply { mkdirs() }
            root.resolve("index.html").writeText(
                """<!doctype html><link rel="stylesheet" href="/assets/app.css"><main>Astrid shell</main>""",
            )
            assets.resolve("app.js").writeText("window.astridLoaded = true;")
            assets.resolve("app.css").writeText("body { color: #8f173b; }")
            testApplication {
                application { astridModule(testConfig(webStaticDir = root.path), InMemoryAstridRepository()) }
                val script = client.get("/assets/app.js") { trustedHost() }
                assertEquals(HttpStatusCode.OK, script.status)
                assertEquals("window.astridLoaded = true;", script.bodyAsText())
                assertFalse(script.contentType()?.match(ContentType.Text.Html) == true)
                val style = client.get("/assets/app.css") { trustedHost() }
                assertEquals(ContentType.Text.CSS, style.contentType()?.withoutParameters())
                assertEquals("body { color: #8f173b; }", style.bodyAsText())
                val navigation = client.get("/lesson/first") { trustedHost() }
                assertEquals(ContentType.Text.Html, navigation.contentType()?.withoutParameters())
                assertEquals(1, Regex("rel=\"stylesheet\"").findAll(navigation.bodyAsText()).count())
                assertEquals(HttpStatusCode.NotFound, client.get("/assets/missing.js") { trustedHost() }.status)
                assertEquals(HttpStatusCode.NotFound, client.get("/api") { trustedHost() }.status)
                assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/unknown") { trustedHost() }.status)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createSession(): String {
        val response = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val setCookie = response.headers[HttpHeaders.SetCookie] ?: error("Session cookie missing")
        assertTrue("HttpOnly" in setCookie && "SameSite=Strict" in setCookie)
        return setCookie.substringAfter("$SESSION_COOKIE_FOR_TEST=").substringBefore(';')
    }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.submit(cookieValue: String, body: String) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

    private suspend fun io.ktor.server.testing.ApplicationTestBuilder.createExercise(cookieValue: String): String {
        val response = client.post("/api/v1/exercises") {
            trustedHost()
            cookie(SESSION_COOKIE_FOR_TEST, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(exerciseRequest)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return (wireJson.parseToJsonElement(response.bodyAsText()) as JsonObject).getValue("id").jsonPrimitive.content
    }

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun testConfig(
        developmentMode: Boolean = true,
        inspectorKey: String? = null,
        webStaticDir: String? = null,
    ) = ServerConfig(
        developmentMode = developmentMode,
        inspectorEnabled = inspectorKey != null,
        inspectorKey = inspectorKey,
        secureCookie = false,
        trustedForwardedHeaders = false,
        allowedOrigins = setOf("http://localhost:8088"),
        allowedHosts = setOf("localhost:8088"),
        sessionTtlMillis = 60_000,
        webStaticDir = webStaticDir,
        jdbcUrl = "jdbc:postgresql://unused",
        r2dbcUrl = "r2dbc:postgresql://unused",
        postgresUser = "unused",
        postgresPassword = "unused",
        poolMaxSize = 2,
    )

    private companion object {
        const val SESSION_COOKIE_FOR_TEST = "astrid_session"
        const val exerciseRequest =
            """{"formatId":"E001","targetSkillId":"G004","seed":7,"index":0,"contentRevision":"pilot-1","generatorRevision":"generator-1"}"""
    }
}
