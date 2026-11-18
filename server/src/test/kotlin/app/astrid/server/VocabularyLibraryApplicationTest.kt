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
        val practice = ownerLibrary.getValue("practice").jsonObject
        assertEquals("vocabulary-library-practice-1", practice.getValue("planRevision").jsonPrimitive.content)
        assertEquals(3, practice.getValue("recognitionCount").jsonPrimitive.content.toInt())
        assertEquals(0, practice.getValue("recallCount").jsonPrimitive.content.toInt())

        val outsiderCookie = createSession()
        val outsiderLibrary = vocabularyContext(outsiderCookie)
        assertTrue(outsiderLibrary.getValue("items").jsonArray.isEmpty())
        assertFalse(outsiderLibrary.getValue("ownerSessionId").jsonPrimitive.content == ownerId)
        assertEquals(0, outsiderLibrary.getValue("practice").jsonObject
            .getValue("recognitionCount").jsonPrimitive.content.toInt())
    }

    @Test
    fun `library practice uses saved teaching supports both operations and preserves source exposure`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val ownerCookie = createSession()
        val ownerId = ownerContext(ownerCookie).getValue("ownerSessionId").jsonPrimitive.content
        val emptyId = UUID.randomUUID()
        assertEquals(
            HttpStatusCode.Conflict,
            createLibraryPractice(ownerCookie, ownerId, emptyId, "vocabulary-library-recognition", 0).status,
        )

        val sourceOne = createAndOpenLesson(ownerCookie, ownerId, "vocabulary-p1-s1", 17)
        val moduleSource = createAndOpenLesson(
            ownerCookie, ownerId, "module-one-circumstances", 19, "module-one-circumstances-1",
        )
        createAndOpenLesson(ownerCookie, ownerId, "vocabulary-p1-s4", 23)
        val context = vocabularyContext(ownerCookie).getValue("practice").jsonObject
        assertEquals(3, context.getValue("recognitionCount").jsonPrimitive.content.toInt())
        assertEquals(2, context.getValue("recallCount").jsonPrimitive.content.toInt())
        val reviewBefore = client.get("/api/v1/practice-review-context") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
        }.json()

        val recognitionId = UUID.randomUUID()
        val recognition = createLibraryPractice(
            ownerCookie, ownerId, recognitionId, "vocabulary-library-recognition", 0,
        )
        assertEquals(HttpStatusCode.Created, recognition.status)
        val recognitionBody = recognition.json()
        assertEquals(3, recognitionBody.getValue("itemCount").jsonPrimitive.content.toInt())
        assertEquals("pilot-18", recognitionBody.getValue("contentRevision").jsonPrimitive.content)
        assertEquals("E031", recognitionBody.getValue("currentExercise").jsonObject
            .getValue("formatId").jsonPrimitive.content)
        val exerciseId = recognitionBody.getValue("currentExercise").jsonObject.getValue("id").jsonPrimitive.content
        val attempted = client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"attemptId":"${UUID.randomUUID()}","exerciseId":"$exerciseId",
                "answer":{"kind":"text","text":"works"},"hintUsed":false}
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.OK, attempted.status)
        val refreshed = client.get("/api/v1/practice-sessions/$recognitionId") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
        }.json()
        val firstItem = refreshed.getValue("items").jsonArray.first().jsonObject
        assertTrue(firstItem.getValue("canAdvance").jsonPrimitive.content.toBoolean())
        val exposure = firstItem.getValue("lastAttemptContext").jsonObject
            .getValue("sourceExposure").jsonArray.single().jsonObject
        assertEquals(sourceOne.toString(), exposure.getValue("practiceSessionId").jsonPrimitive.content)
        assertEquals("introduction", exposure.getValue("source").jsonPrimitive.content)

        val replay = createLibraryPractice(ownerCookie, ownerId, recognitionId, "vocabulary-library-recognition", 0)
        assertEquals(HttpStatusCode.OK, replay.status)
        val conflict = createLibraryPractice(ownerCookie, ownerId, recognitionId, "vocabulary-library-recognition", 1)
        assertEquals(HttpStatusCode.Conflict, conflict.status)

        val recallId = UUID.randomUUID()
        val recall = createLibraryPractice(ownerCookie, ownerId, recallId, "vocabulary-library-recall", 0)
        assertEquals(HttpStatusCode.Created, recall.status)
        val recallBody = recall.json()
        assertEquals(2, recallBody.getValue("itemCount").jsonPrimitive.content.toInt())
        val recallExerciseId = recallBody.getValue("currentExercise").jsonObject.getValue("id").jsonPrimitive.content
        val recallAttempt = client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {"attemptId":"${UUID.randomUUID()}","exerciseId":"$recallExerciseId",
                "answer":{"kind":"text","text":"nu"},"hintUsed":false}
                """.trimIndent(),
            )
        }
        assertEquals(HttpStatusCode.OK, recallAttempt.status)
        val recalled = client.get("/api/v1/practice-sessions/$recallId") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
        }.json()
        val recallExposure = recalled.getValue("items").jsonArray.first().jsonObject
            .getValue("lastAttemptContext").jsonObject.getValue("sourceExposure").jsonArray.single().jsonObject
        assertEquals(moduleSource.toString(), recallExposure.getValue("practiceSessionId").jsonPrimitive.content)
        val reviewAfter = client.get("/api/v1/practice-review-context") {
            trustedHost()
            cookie(SESSION_COOKIE, ownerCookie)
        }.json()
        assertEquals(reviewBefore, reviewAfter)

        val outsiderCookie = createSession()
        val outsiderId = ownerContext(outsiderCookie).getValue("ownerSessionId").jsonPrimitive.content
        assertEquals(
            HttpStatusCode.Conflict,
            createLibraryPractice(
                outsiderCookie,
                outsiderId,
                UUID.randomUUID(),
                "vocabulary-library-recall",
                0,
            ).status,
        )
    }

    @Test
    fun `library practice keeps mutation authentication and owner guards`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val id = UUID.randomUUID()
        val body = """
            {"practiceSessionId":"$id","focusId":"vocabulary-library-recognition",
            "planRevision":"vocabulary-library-practice-1","seed":0}
        """.trimIndent()
        val missingOwner = client.post("/api/v1/practice-sessions") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Unauthorized, missingOwner.status)

        val cookie = createSession()
        val ownerId = ownerContext(cookie).getValue("ownerSessionId").jsonPrimitive.content
        val wrongOwner = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie(SESSION_COOKIE, cookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", UUID.randomUUID().toString())
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.Conflict, wrongOwner.status)
        assertTrue(ownerId.isNotBlank())
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

    private suspend fun ApplicationTestBuilder.createAndOpenLesson(
        cookieValue: String,
        ownerId: String,
        focusId: String,
        seed: Int,
        planRevision: String = "$focusId-2",
    ): UUID {
        val id = UUID.randomUUID()
        val created = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", ownerId)
            contentType(ContentType.Application.Json)
            setBody(
                """{"practiceSessionId":"$id","focusId":"$focusId","planRevision":"$planRevision","seed":$seed}""",
            )
        }
        assertEquals(HttpStatusCode.Created, created.status)
        val opened = client.post("/api/v1/practice-sessions/$id/support") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", ownerId)
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"introduction"}""")
        }
        assertEquals(HttpStatusCode.OK, opened.status)
        return id
    }

    private suspend fun ApplicationTestBuilder.createLibraryPractice(
        cookieValue: String,
        ownerId: String,
        id: UUID,
        focusId: String,
        seed: Int,
    ) = client.post("/api/v1/practice-sessions") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        header("X-Astrid-Practice-Owner", ownerId)
        contentType(ContentType.Application.Json)
        setBody(
            """{"practiceSessionId":"$id","focusId":"$focusId","planRevision":"vocabulary-library-practice-1","seed":$seed}""",
        )
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
