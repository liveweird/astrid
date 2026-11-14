package app.astrid.server

import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VocabularyLibraryApplicationTest {
    @Test
    fun `reference catalogue is cookie free finite passive and contains no private exercise data`() = testApplication {
        val stores = InMemoryStores()
        application { astridModule(config(), stores) }

        val response = client.get("/api/v1/vocabulary-entries") { trustedHost() }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
        assertEquals("no-store", response.headers[HttpHeaders.CacheControl])
        assertNull(response.headers[HttpHeaders.SetCookie])
        val body = response.json()
        assertEquals(79, body.getValue("items").jsonArray.size)
        assertEquals(setOf("libraryRevision", "contentRevision", "items"), body.keys)
        val keys = body.descendantKeys()
        listOf(
            "canonicalSentence",
            "acceptedAnswers",
            "licensedOrders",
            "matchingColumns",
            "answerExposureKeys",
            "introductionExposureKeys",
        ).forEach { privateKey -> assertFalse(privateKey in keys, privateKey) }

        val context = client.get("/api/v1/vocabulary-context") { trustedHost() }
        assertEquals(HttpStatusCode.Unauthorized, context.status)
        assertNull(context.headers[HttpHeaders.SetCookie])
    }

    @Test
    fun `saved teaching claims are isolated to the current owner`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val ownerCookie = createSession()
        val ownerId = ownerContext(ownerCookie).getValue("ownerSessionId").jsonPrimitive.content
        val practiceId = UUID.randomUUID()
        val created = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", ownerId)
            contentType(ContentType.Application.Json)
            setBody(
                """{"practiceSessionId":"$practiceId","focusId":"vocabulary-p1-s1","planRevision":"vocabulary-p1-s1-2","seed":17}""",
            )
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val opened = client.post("/api/v1/practice-sessions/$practiceId/support") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", ownerId)
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"introduction"}""")
        }
        assertEquals(HttpStatusCode.OK, opened.status)

        val ownerLibrary = vocabularyContext(ownerCookie)
        assertEquals(ownerId, ownerLibrary.getValue("ownerSessionId").jsonPrimitive.content)
        val claims = ownerLibrary.getValue("items").jsonArray
        assertTrue(claims.isNotEmpty())
        assertTrue(claims.all {
            it.jsonObject.keys == setOf("entryId", "senseId", "meaningRecorded", "formIds")
        })

        val outsiderCookie = createSession()
        val outsiderLibrary = vocabularyContext(outsiderCookie)
        assertTrue(outsiderLibrary.getValue("items").jsonArray.isEmpty())
        assertFalse(outsiderLibrary.getValue("ownerSessionId").jsonPrimitive.content == ownerId)
    }

    @Test
    fun `catalogue is mode gated and global request guards still run`() = testApplication {
        application { astridModule(config(developmentMode = false), InMemoryStores()) }

        val forbidden = client.get("/api/v1/vocabulary-entries") {
            trustedHost()
            header(HttpHeaders.Origin, "https://attacker.example")
        }
        assertEquals(HttpStatusCode.Forbidden, forbidden.status)
        val hidden = client.get("/api/v1/vocabulary-entries") { trustedHost() }
        assertEquals(HttpStatusCode.NotFound, hidden.status)
        val hiddenContext = client.get("/api/v1/vocabulary-context") { trustedHost() }
        assertEquals(HttpStatusCode.NotFound, hiddenContext.status)
    }

    @Test
    fun `expired owner context is unavailable and creates no replacement session`() = testApplication {
        var now = 10_000L
        application { astridModule(config(sessionTtlMillis = 1_000), InMemoryStores { now }, clock = { now }) }
        val cookie = createSession()
        now += 1_000

        val expired = client.get("/api/v1/vocabulary-context") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
        }

        assertEquals(HttpStatusCode.Unauthorized, expired.status)
        assertNull(expired.headers[HttpHeaders.SetCookie])
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

    private suspend fun ApplicationTestBuilder.ownerContext(cookieValue: String) =
        client.get("/api/v1/practice-context") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
        }.json()

    private suspend fun ApplicationTestBuilder.vocabularyContext(cookieValue: String) =
        client.get("/api/v1/vocabulary-context") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
        }.let { response ->
            assertEquals(HttpStatusCode.OK, response.status)
            response.json()
        }

    private suspend fun HttpResponse.json() = wireJson.parseToJsonElement(bodyAsText()).jsonObject

    private fun JsonElement.descendantKeys(): Set<String> = when (this) {
        is JsonObject -> keys + values.flatMap { it.descendantKeys() }
        is JsonArray -> flatMap { it.descendantKeys() }.toSet()
        else -> emptySet()
    }

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config(developmentMode: Boolean = true, sessionTtlMillis: Long = 60_000) = ServerConfig(
        developmentMode, false, null, false, false,
        setOf("http://localhost:8088"), setOf("localhost:8088"), sessionTtlMillis, null,
        "jdbc:postgresql://unused", "r2dbc:postgresql://unused", "unused", "unused", 2,
    )

    private companion object {
        const val SESSION_COOKIE = "astrid_session"
    }
}
