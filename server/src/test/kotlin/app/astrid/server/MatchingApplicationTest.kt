@file:Suppress("MaxLineLength") // Complete fingerprint mutations and JSON wire assertions remain readable together.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.MatchingPairDto
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

class MatchingApplicationTest {
    @Test
    fun `pair six fingerprint frames every matching target column and private source value`() {
        val generated = PilotEngine().generate(GenerationRequest("E014", null, 47, 0, "pilot-6", "generator-6"))
        val original = generated.reproductionFingerprint()
        val targets = requireNotNull(generated.lexicalTargets)
        val columns = requireNotNull(generated.prompt.matchingColumns)
        val source = requireNotNull(generated.inspector.matchingSource)
        val target = targets.first()
        val left = columns.left.first()
        val right = columns.right.first()
        val pair = source.pairs.first()
        val lexical = pair.lexicalSource
        val mutations = listOf(
            generated.reproductionFingerprint(lexicalTargetsForFingerprint = null),
            generated.reproductionFingerprint(lexicalTargetsForFingerprint = targets.reversed()),
            generated.reproductionFingerprint(lexicalTargetsForFingerprint = listOf(target.copy(entryId = "changed")) + targets.drop(1)),
            generated.reproductionFingerprint(lexicalTargetsForFingerprint = listOf(target.copy(senseId = "changed")) + targets.drop(1)),
            generated.reproductionFingerprint(lexicalTargetsForFingerprint = listOf(target.copy(facet = "changed")) + targets.drop(1)),
            generated.reproductionFingerprint(lexicalTargetsForFingerprint = listOf(target.copy(direction = "changed")) + targets.drop(1)),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = null)),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = columns.copy(left = columns.left.reversed()))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = columns.copy(left = listOf(left.copy(id = "changed")) + columns.left.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = columns.copy(left = listOf(left.copy(text = "changed")) + columns.left.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = columns.copy(right = columns.right.reversed()))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = columns.copy(right = listOf(right.copy(id = "changed")) + columns.right.drop(1)))),
            generated.reproductionFingerprint(promptForFingerprint = generated.prompt.copy(matchingColumns = columns.copy(right = listOf(right.copy(text = "changed")) + columns.right.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = null)),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = source.pairs.reversed()))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(leftId = "changed")) + source.pairs.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(rightId = "changed")) + source.pairs.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(lexicalSource = lexical.copy(entryId = "changed"))) + source.pairs.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(lexicalSource = lexical.copy(senseId = "changed"))) + source.pairs.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(lexicalSource = lexical.copy(headword = "changed"))) + source.pairs.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(lexicalSource = lexical.copy(gloss = "changed"))) + source.pairs.drop(1)))),
            generated.reproductionFingerprint(inspectorForFingerprint = generated.inspector.copy(matchingSource = source.copy(pairs = listOf(pair.copy(lexicalSource = lexical.copy(article = "changed"))) + source.pairs.drop(1)))),
        )

        mutations.forEach { assertNotEquals(original, it) }
    }

    @Test
    fun `matching public wire exposes two opaque columns while inspector alone carries correspondences`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val catalog = client.get("/api/v1/catalog") { trustedHost(); cookie(SESSION_COOKIE, owner) }.json()
        assertEquals("pilot-17", catalog.string("contentRevision"))
        assertEquals("generator-17", catalog.string("generatorRevision"))
        assertEquals("evaluation-17", catalog.string("evaluationRevision"))
        val matchingFormat = catalog.getValue("formats").jsonArray.map { it.jsonObject }
            .single { it.string("id") == "E014" }
        assertEquals("meaning", matchingFormat.getValue("lexicalTask").jsonObject.string("facet"))
        assertEquals("en-da", matchingFormat.getValue("lexicalTask").jsonObject.string("direction"))

        val exercise = createExercise(owner).json()
        assertEquals("E014", exercise.string("formatId"))
        assertFalse("lexicalTarget" in exercise)
        assertFalse("targetSkillId" in exercise)
        val targets = exercise.getValue("lexicalTargets").jsonArray.map { it.jsonObject }
        assertEquals(listOf("L014-S01", "L015-S01", "L018-S01", "L024-S01"), targets.map { it.string("senseId") })
        val columns = exercise.getValue("prompt").jsonObject.getValue("matchingColumns").jsonObject
        val left = columns.getValue("left").jsonArray.map { it.jsonObject }
        val right = columns.getValue("right").jsonArray.map { it.jsonObject }
        assertEquals(setOf("l0", "l1", "l2", "l3"), left.map { it.string("id") }.toSet())
        assertEquals(setOf("r0", "r1", "r2", "r3"), right.map { it.string("id") }.toSet())
        assertTrue(left.all { it.keys == setOf("id", "text") })
        assertTrue(right.all { it.keys == setOf("id", "text") })
        assertFalse("matchingSource" in exercise)

        val inspector = inspect(owner, exercise.string("id")).json()
        val pairs = inspector.getValue("matchingSource").jsonObject.getValue("pairs").jsonArray.map { it.jsonObject }
        assertEquals(4, pairs.size)
        assertTrue(pairs.all { it.keys == setOf("leftId", "rightId", "lexicalSource") })
    }

    @Test
    fun `partial and mixed matching grades every displayed left item and replays raw pair order`() = testApplication {
        val repository = InMemoryStores()
        application { astridModule(config(), repository) }
        val owner = createSession()
        val exercise = createExercise(owner).json()
        val id = exercise.string("id")
        val sourcePairs = inspect(owner, id).json().getValue("matchingSource").jsonObject
            .getValue("pairs").jsonArray.map { value ->
                value.jsonObject.let { MatchingPairDto(it.string("leftId"), it.string("rightId")) }
            }
        val wrongRight = sourcePairs.map { it.rightId }.drop(1) + sourcePairs.first().rightId
        val mixedPairs = listOf(sourcePairs.first(), MatchingPairDto(sourcePairs[1].leftId, wrongRight[1]))
        val attemptId = UUID.randomUUID().toString()
        val first = submitMatching(owner, attemptId, id, mixedPairs)
        assertEquals(HttpStatusCode.OK, first.status)
        val result = first.json()
        assertEquals("not-met", result.string("targetSatisfaction"))
        assertEquals("not-met", result.string("meaningSatisfaction"))
        assertEquals("not-met", result.string("responseConstraintStatus"))
        assertEquals("acceptable", result.string("linguisticStatus"))
        assertEquals("danish-matching", result.string("linguisticScope"))
        assertEquals("", result.string("completedSentence"))
        val rows = result.getValue("matchingResults").jsonArray.map { it.jsonObject }
        assertEquals(sourcePairs.map { it.leftId }, rows.map { it.string("leftId") })
        assertEquals(4, rows.size)
        assertEquals(2, rows.count { "rightId" !in it })
        repository.tamperFingerprint(UUID.fromString(id))
        assertEquals(first.bodyAsText(), submitMatching(owner, attemptId, id, mixedPairs).bodyAsText())
        assertEquals(HttpStatusCode.Conflict, submitMatching(owner, attemptId, id, mixedPairs.reversed()).status)
        assertEquals(HttpStatusCode.Conflict, submitMatching(owner, UUID.randomUUID().toString(), id, mixedPairs).status)

        val freshExercise = createExercise(owner).json()
        val freshId = freshExercise.string("id")
        val freshPairs = inspect(owner, freshId).json().getValue("matchingSource").jsonObject
            .getValue("pairs").jsonArray.map { value ->
                value.jsonObject.let { MatchingPairDto(it.string("leftId"), it.string("rightId")) }
            }
        val complete = submitMatching(owner, UUID.randomUUID().toString(), freshId, freshPairs).json()
        assertEquals("met", complete.string("targetSatisfaction"))
        assertEquals("met", complete.string("responseConstraintStatus"))
        assertEquals(4, complete.getValue("matchingResults").jsonArray.count { it.jsonObject.string("targetSatisfaction") == "met" })
    }

    @Test
    fun `matching validation follows ownership and invalid attempts reserve no retry identity`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val owner = createSession()
        val outsider = createSession()
        val exercise = createExercise(owner).json()
        val id = exercise.string("id")
        val sourcePairs = inspect(owner, id).json().getValue("matchingSource").jsonObject
            .getValue("pairs").jsonArray.map { value ->
                value.jsonObject.let { MatchingPairDto(it.string("leftId"), it.string("rightId")) }
            }
        val malformed = listOf(
            listOf(sourcePairs[0], sourcePairs[0].copy(rightId = sourcePairs[1].rightId)),
            listOf(sourcePairs[0], sourcePairs[1].copy(rightId = sourcePairs[0].rightId)),
            listOf(sourcePairs[0].copy(leftId = "l9")),
            listOf(sourcePairs[0].copy(rightId = "r9")),
            sourcePairs + sourcePairs.first(),
        )
        malformed.forEachIndexed { index, pairs ->
            val attemptId = UUID.randomUUID().toString()
            assertEquals(HttpStatusCode.NotFound, submitMatching(outsider, attemptId, id, pairs).status)
            assertEquals(HttpStatusCode.BadRequest, submitMatching(owner, attemptId, id, pairs).status)
            val valid = submitMatching(owner, attemptId, id, sourcePairs)
            assertEquals(HttpStatusCode.OK, valid.status)
            assertEquals(index + 1, valid.json().getValue("retryNumber").jsonPrimitive.content.toInt())
        }
        assertEquals(HttpStatusCode.BadRequest, submitText(owner, UUID.randomUUID().toString(), id).status)
        val empty = submitMatching(owner, UUID.randomUUID().toString(), id, emptyList())
        assertEquals(HttpStatusCode.OK, empty.status)
        assertEquals("unknown", empty.json().string("linguisticStatus"))
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

    private suspend fun ApplicationTestBuilder.createExercise(cookieValue: String) = client.post("/api/v1/exercises") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody("""{"formatId":"E014","seed":47,"index":0,"contentRevision":"pilot-6","generatorRevision":"generator-6"}""")
    }

    private suspend fun ApplicationTestBuilder.inspect(cookieValue: String, exerciseId: String) =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Inspector-Key", INSPECTOR_KEY)
        }

    private suspend fun ApplicationTestBuilder.submitMatching(
        cookieValue: String,
        attemptId: String,
        exerciseId: String,
        pairs: List<MatchingPairDto>,
    ) = submit(cookieValue, AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Matching(pairs), false))

    private suspend fun ApplicationTestBuilder.submitText(cookieValue: String, attemptId: String, exerciseId: String) =
        submit(cookieValue, AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Text("wrong kind"), false))

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, request: AttemptRequest) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(request))
        }

    private suspend fun HttpResponse.json() = wireJson.parseToJsonElement(bodyAsText()) as JsonObject

    private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content

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
