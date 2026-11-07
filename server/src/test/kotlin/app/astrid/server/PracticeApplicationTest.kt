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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PracticeApplicationTest {
    @Test
    fun `completed practice creates a selected follow-up with frozen lineage and exposure`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val auth = authenticate()
        val sourceId = UUID.randomUUID().toString()
        val source = create(auth.cookie, auth.ownerId, createBody(sourceId))
        assertEquals(HttpStatusCode.Created, source.status)
        val childId = UUID.randomUUID().toString()
        val earlyBody = followUpBody(childId, listOf("grammar:G005"))
        assertEquals(HttpStatusCode.Conflict, followUp(auth, sourceId, earlyBody).status)

        val completed = completePractice(auth, source.bodyAsText())
        val targetKeys = completed.getValue("summary").jsonObject.getValue("rows").jsonArray
            .map(::targetKey).take(2)
        val body = followUpBody(childId, targetKeys)
        val created = followUp(auth, sourceId, body)
        assertEquals(HttpStatusCode.Created, created.status)
        val state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        assertEquals("targeted-review", state.getValue("focusId").jsonPrimitive.content)
        assertEquals("follow-up-1", state.getValue("planRevision").jsonPrimitive.content)
        assertEquals(sourceId, state.getValue("followUp").jsonObject
            .getValue("sourcePracticeSessionId").jsonPrimitive.content)
        assertEquals(targetKeys.size, state.getValue("followUp").jsonObject
            .getValue("selectedTargetKeys").jsonArray.size)
        val itemCount = state.getValue("itemCount").jsonPrimitive.content.toInt()
        assertTrue(itemCount in targetKeys.size..8)
        val sourceAfterCreation = wireJson.parseToJsonElement(getPractice(auth.cookie, sourceId).bodyAsText()).jsonObject
        assertEquals("completed", sourceAfterCreation.getValue("state").jsonPrimitive.content)
        assertTrue("followUp" !in sourceAfterCreation)
        assertEquals(completed.getValue("summary"), sourceAfterCreation.getValue("summary"))

        val replay = followUp(auth, sourceId, body)
        assertEquals(HttpStatusCode.OK, replay.status)
        assertEquals(created.bodyAsText(), replay.bodyAsText())
        val changed = followUp(auth, sourceId, followUpBody(childId, targetKeys.reversed()))
        assertEquals(HttpStatusCode.Conflict, changed.status)
        val competingSourceId = UUID.randomUUID().toString()
        assertEquals(HttpStatusCode.Created, create(auth.cookie, auth.ownerId, createBody(competingSourceId)).status)
        assertEquals(HttpStatusCode.Conflict, followUp(auth, competingSourceId, body).status)

        assertEquals(HttpStatusCode.OK, support(auth, sourceId, """{"kind":"introduction"}""").status)
        val exercise = state.getValue("currentExercise").jsonObject
        assertEquals(HttpStatusCode.OK, submit(auth.cookie, attemptBodyFor(exercise)).status)
        val saved = wireJson.parseToJsonElement(getPractice(auth.cookie, childId).bodyAsText()).jsonObject
        val context = saved.getValue("items").jsonArray.first().jsonObject
            .getValue("firstAttemptContext").jsonObject
        assertTrue(context.getValue("sourceExposure").jsonArray.isNotEmpty())
        assertTrue(context.getValue("sourceExposure").jsonArray.all { exposure ->
            exposure.jsonObject.getValue("practiceSessionId").jsonPrimitive.content == sourceId
        })
        assertTrue(context.getValue("sourceExposure").jsonArray.none { exposure ->
            exposure.jsonObject.getValue("source").jsonPrimitive.content == "introduction"
        })
    }

    @Test
    fun `follow-up rejects missing ownership invalid targets and competing source identity`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val owner = authenticate()
        val outsider = authenticate()
        val sourceId = UUID.randomUUID().toString()
        val created = create(owner.cookie, owner.ownerId, createBody(sourceId))
        completePractice(owner, created.bodyAsText())
        val childId = UUID.randomUUID().toString()

        assertEquals(
            HttpStatusCode.NotFound,
            followUp(outsider, sourceId, followUpBody(childId, listOf("grammar:G005"))).status,
        )
        assertEquals(
            HttpStatusCode.Conflict,
            followUp(owner, sourceId, followUpBody(childId, listOf("grammar:G999"))).status,
        )
        assertEquals(
            HttpStatusCode.BadRequest,
            followUp(owner, sourceId, followUpBody(UUID.randomUUID().toString(), emptyList())).status,
        )
        assertEquals(
            HttpStatusCode.Conflict,
            followUp(owner, sourceId, followUpBody(sourceId, listOf("grammar:G005"))).status,
        )
    }

    @Test
    fun `nested follow-up keeps one original ancestor witness per relevant knowledge key`() = testApplication {
        application { astridModule(config(), InMemoryAstridRepository()) }
        val auth = authenticate()
        val sourceId = UUID.randomUUID().toString()
        val source = create(auth.cookie, auth.ownerId, createBody(sourceId))
        val completedSource = completePractice(auth, source.bodyAsText())
        assertEquals(HttpStatusCode.OK, support(auth, sourceId, """{"kind":"introduction"}""").status)
        val target = targetKey(completedSource.getValue("summary").jsonObject.getValue("rows").jsonArray.first())

        val childId = UUID.randomUUID().toString()
        val child = followUp(auth, sourceId, followUpBody(childId, listOf(target)))
        assertEquals(HttpStatusCode.Created, child.status)
        completePractice(auth, child.bodyAsText())

        val grandchildId = UUID.randomUUID().toString()
        val grandchild = followUp(auth, childId, followUpBody(grandchildId, listOf(target)))
        assertEquals(HttpStatusCode.Created, grandchild.status)
        val grandchildState = wireJson.parseToJsonElement(grandchild.bodyAsText()).jsonObject
        assertEquals(
            childId,
            grandchildState.getValue("followUp").jsonObject
                .getValue("sourcePracticeSessionId").jsonPrimitive.content,
        )
        assertEquals(
            HttpStatusCode.OK,
            submit(auth.cookie, attemptBodyFor(grandchildState.getValue("currentExercise").jsonObject)).status,
        )
        val saved = wireJson.parseToJsonElement(getPractice(auth.cookie, grandchildId).bodyAsText()).jsonObject
        val exposures = saved.getValue("items").jsonArray.first().jsonObject
            .getValue("firstAttemptContext").jsonObject.getValue("sourceExposure").jsonArray
        assertTrue(exposures.isNotEmpty())
        assertEquals(
            setOf(sourceId),
            exposures.map { it.jsonObject.getValue("practiceSessionId").jsonPrimitive.content }.toSet(),
        )
        val keys = exposures.flatMap { exposure ->
            exposure.jsonObject.getValue("keys").jsonArray.map { it.jsonPrimitive.content }
        }
        assertEquals(keys.distinct(), keys)
    }

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

    private suspend fun ApplicationTestBuilder.followUp(auth: Auth, sourceId: String, body: String) =
        client.post("/api/v1/practice-sessions/$sourceId/follow-ups") {
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

    private fun followUpBody(id: String, targets: List<String>) =
        """{"practiceSessionId":"$id","planRevision":"follow-up-1","seed":417,"targetKeys":${wireJson.encodeToString(targets)}}"""

    private fun targetKey(row: kotlinx.serialization.json.JsonElement): String {
        val value = row.jsonObject
        return value["targetSkillId"]?.jsonPrimitive?.content?.let { "grammar:$it" }
            ?: value.getValue("lexicalTarget").jsonObject.let { target ->
                listOf(
                    "lexical",
                    target.getValue("entryId").jsonPrimitive.content,
                    target.getValue("senseId").jsonPrimitive.content,
                    target.getValue("facet").jsonPrimitive.content,
                    target.getValue("direction").jsonPrimitive.content,
                ).joinToString(":")
            }
    }

    private suspend fun ApplicationTestBuilder.completePractice(auth: Auth, initial: String): kotlinx.serialization.json.JsonObject {
        var state = wireJson.parseToJsonElement(initial).jsonObject
        while (state.getValue("state").jsonPrimitive.content == "active") {
            val exercise = state.getValue("currentExercise").jsonObject
            assertEquals(HttpStatusCode.OK, submit(auth.cookie, attemptBodyFor(exercise)).status)
            val position = state.getValue("cursor").jsonPrimitive.content.toInt()
            val advanced = advance(auth, state.getValue("id").jsonPrimitive.content, position)
            assertEquals(HttpStatusCode.OK, advanced.status)
            state = wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
        }
        return state
    }

    private fun attemptBodyFor(exercise: kotlinx.serialization.json.JsonObject): String {
        val answer = if (exercise.getValue("formatId").jsonPrimitive.content == "E003") {
            val ids = exercise.getValue("prompt").jsonObject.getValue("blocks").jsonArray.map {
                it.jsonObject.getValue("id").jsonPrimitive.content
            }
            """{"kind":"order","blockIds":${wireJson.encodeToString(ids)}}"""
        } else {
            """{"kind":"text","text":"wrong"}"""
        }
        val exerciseId = exercise.getValue("id").jsonPrimitive.content
        return """{"attemptId":"${UUID.randomUUID()}","exerciseId":"$exerciseId","answer":$answer,"hintUsed":false}"""
    }

    private data class Auth(val cookie: String, val ownerId: String)
}
