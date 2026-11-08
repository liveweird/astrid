@file:Suppress("MaxLineLength") // Complete JSON wire fixtures remain readable on one line.

package app.astrid.server

import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
import app.astrid.server.plugins.ProblemJson
import app.astrid.server.plugins.Readiness
import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import com.atlassian.oai.validator.report.ValidationReport
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import io.swagger.v3.parser.OpenAPIV3Parser
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenApiContractTest {
    private val rawSpec = checkNotNull(javaClass.getResource("/openapi/documentation.yaml")).readText()
    private val strictValidator = OpenApiInteractionValidator.createForInlineApiSpecification(rawSpec).build()
    private val validator = OpenApiInteractionValidator.createForInlineApiSpecification(rawSpec)
        .withLevelResolver(
            LevelResolver.create()
                // This harness is deliberately response-only. ApplicationTest exercises positive and malformed
                // request decoding; here path/method existence and every observed response status/body stay strict.
                .withLevel("validation.request", ValidationReport.Level.IGNORE)
                .withLevel("validation.request.path.missing", ValidationReport.Level.ERROR)
                .withLevel("validation.request.operation.notAllowed", ValidationReport.Level.ERROR)
                .withLevel("validation.response", ValidationReport.Level.ERROR)
                .build(),
        ).build()

    @Test
    fun `spec info version equals the application version`() {
        val versionSource = File("../web/src/changelog/version.ts").readText()
        val version = checkNotNull(Regex("export const APP_VERSION = \"([^\"]+)\"").find(versionSource)).groupValues[1]
        val spec = checkNotNull(OpenAPIV3Parser().readContents(rawSpec).openAPI)
        assertEquals(version, spec.info.version)
    }

    @Test
    fun `practice creation schema admits only supported focus revision pairs`() {
        val requestValidator = OpenApiInteractionValidator.createForInlineApiSpecification(rawSpec).build()
        val id = "4d2dc82d-bbdc-4645-a78a-ad0be38c4355"
        for (focus in listOf("reading-basics", "present-verbs")) {
            for (revision in listOf("practice-1", "present-verbs-1")) {
                val request = SimpleRequest.Builder.post("/api/v1/practice-sessions")
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Astrid-Client", "playground")
                    .withHeader("X-Astrid-Practice-Owner", id)
                    .withBody("""{"practiceSessionId":"$id","focusId":"$focus","planRevision":"$revision","seed":47}""")
                    .build()
                val report = requestValidator.validateRequest(request)
                val supported = (focus == "reading-basics") == (revision == "practice-1")
                assertEquals(supported, !report.hasErrors(), SimpleValidationReportFormat.getInstance().apply(report))
            }
        }
    }

    @Test
    fun `exercise request schema correlates formats targets and retained revision pairs`() {
        val accepted = listOf(
            exerciseRequest("E001", "G004", "pilot-1", "generator-1"),
            exerciseRequest("E002", "G006", "pilot-1", "generator-1"),
            exerciseRequest("E003", "G005", "pilot-1", "generator-1"),
            exerciseRequest("E004", "G004", "pilot-2", "generator-2"),
            exerciseRequest("E006", "G006", "pilot-2", "generator-2"),
            exerciseRequest("E007", null, "pilot-3", "generator-3"),
            exerciseRequest("E008", null, "pilot-4", "generator-4"),
            exerciseRequest("E009", "G005", "pilot-4", "generator-4"),
            exerciseRequest("E010", null, "pilot-4", "generator-4"),
        )
        val rejected = listOf(
            exerciseRequest("E009", null, "pilot-4", "generator-4"),
            exerciseRequest("E009", "G004", "pilot-4", "generator-4"),
            exerciseRequest("E009", "G005", "pilot-3", "generator-3"),
            exerciseRequest("E009", "G005", "pilot-4", "generator-3"),
            exerciseRequest("E010", "G005", "pilot-4", "generator-4"),
            exerciseRequest("E010", null, "pilot-3", "generator-3"),
            exerciseRequest("E010", null, "pilot-4", "generator-3"),
            exerciseRequest("E001", "G004", "pilot-1", "generator-2"),
            exerciseRequest("E007", null, "pilot-3", "generator-4"),
        )

        accepted.forEach { assertExerciseRequestValidity(it, expected = true) }
        rejected.forEach { assertExerciseRequestValidity(it, expected = false) }
    }

    @Test
    fun `exercise response schema enforces writing reading and lexical categories`() {
        val accepted = listOf(
            exerciseResponse("E001", "pilot-1", "generator-1", target = "G004"),
            exerciseResponse("E004", "pilot-2", "generator-2", target = "G004"),
            exerciseResponse("E006", "pilot-3", "generator-3", target = "G006"),
            exerciseResponse("E003", "pilot-4", "generator-4", target = "G005"),
            exerciseResponse("E009", "pilot-4", "generator-4", target = "G005"),
            exerciseResponse(
                "E010",
                "pilot-4",
                "generator-4",
                readingFacet = "participant-action-complement",
                passage = true,
            ),
            exerciseResponse(
                "E008",
                "pilot-3",
                "generator-3",
                lexicalFacet = "singular-headword",
                lexicalDirection = "en-da",
            ),
            exerciseResponse(
                "E007",
                "pilot-4",
                "generator-4",
                lexicalFacet = "meaning",
                lexicalDirection = "da-en",
            ),
        )
        val rejected = listOf(
            exerciseResponse("E009", "pilot-4", "generator-4"),
            exerciseResponse("E009", "pilot-3", "generator-3", target = "G005"),
            exerciseResponse(
                "E009",
                "pilot-4",
                "generator-4",
                target = "G005",
                readingFacet = "participant-action-complement",
            ),
            exerciseResponse("E010", "pilot-4", "generator-4", passage = true),
            exerciseResponse("E010", "pilot-4", "generator-4", readingFacet = "wrong", passage = true),
            exerciseResponse(
                "E010",
                "pilot-4",
                "generator-4",
                target = "G005",
                readingFacet = "participant-action-complement",
                passage = true,
            ),
            exerciseResponse(
                "E010",
                "pilot-4",
                "generator-4",
                lexicalFacet = "singular-headword",
                lexicalDirection = "en-da",
                readingFacet = "participant-action-complement",
                passage = true,
            ),
            exerciseResponse("E008", "pilot-3", "generator-3"),
            exerciseResponse("E001", "pilot-1", "generator-2", target = "G004"),
            exerciseResponse(
                "E008",
                "pilot-3",
                "generator-4",
                lexicalFacet = "singular-headword",
                lexicalDirection = "en-da",
            ),
            exerciseResponse("E001", "pilot-1", "generator-1", target = "G006"),
            exerciseResponse("E002", "pilot-2", "generator-2", target = "G004"),
            exerciseResponse(
                "E008",
                "pilot-3",
                "generator-3",
                lexicalFacet = "meaning",
                lexicalDirection = "en-da",
            ),
            exerciseResponse(
                "E008",
                "pilot-3",
                "generator-3",
                lexicalFacet = "singular-headword",
                lexicalDirection = "da-en",
            ),
            exerciseResponse(
                "E007",
                "pilot-4",
                "generator-4",
                lexicalFacet = "singular-headword",
                lexicalDirection = "da-en",
            ),
        )

        accepted.forEach { assertExerciseResponseValidity(it, expected = true) }
        rejected.forEach { assertExerciseResponseValidity(it, expected = false) }
    }

    @Test
    fun `practice profile operation mapping stays stable`() {
        val spec = checkNotNull(OpenAPIV3Parser().readContents(rawSpec).openAPI)
        assertEquals("getPracticeProfiles", spec.paths.getValue("/api/v1/practice-profiles").get.operationId)
        assertEquals("createPracticeSession", spec.paths.getValue("/api/v1/practice-sessions").post.operationId)
        assertEquals(
            "createPracticeFollowUp",
            spec.paths.getValue("/api/v1/practice-sessions/{id}/follow-ups").post.operationId,
        )
    }

    @Test
    fun `spec parses and validates critical runtime responses`() = testApplication {
        val parse = OpenAPIV3Parser().readContents(rawSpec)
        assertNotNull(parse.openAPI, "OpenAPI parse failed: ${parse.messages}")
        application { astridModule(config(inspectorKey = "k".repeat(32)), InMemoryStores()) }

        val sessionResponse = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        validate(HttpMethod.Post, "/api/v1/development-session", sessionResponse)
        val malformedSession = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{")
        }
        validate(HttpMethod.Post, "/api/v1/development-session", malformedSession)
        val unknownSession = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("""{"unexpected":true}""")
        }
        validate(HttpMethod.Post, "/api/v1/development-session", unknownSession)
        val oversizedSession = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("""{"padding":"${"x".repeat(17 * 1024)}"}""")
        }
        validate(HttpMethod.Post, "/api/v1/development-session", oversizedSession)
        val cookie = sessionResponse.headers[HttpHeaders.SetCookie]!!
            .substringAfter("astrid_session=").substringBefore(';')
        validate(HttpMethod.Get, "/api/v1/catalog", client.get("/api/v1/catalog") {
            trustedHost()
            cookie("astrid_session", cookie)
        })
        validate(HttpMethod.Get, "/api/v1/practice-profiles", client.get("/api/v1/practice-profiles") {
            trustedHost()
            cookie("astrid_session", cookie)
        })
        val exerciseResponse = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(EXERCISE_REQUEST)
        }
        validate(HttpMethod.Post, "/api/v1/exercises", exerciseResponse)
        listOf("E002" to "G006", "E003" to "G005").forEach { (format, skill) ->
            val response = client.post("/api/v1/exercises") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"formatId":"$format","targetSkillId":"$skill","seed":0,"index":0,"contentRevision":"pilot-1","generatorRevision":"generator-1"}""",
                )
            }
            validate(HttpMethod.Post, "/api/v1/exercises", response)
        }
        listOf("E004" to "G004", "E005" to "G004", "E006" to "G006").forEach { (format, skill) ->
            val response = client.post("/api/v1/exercises") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"formatId":"$format","targetSkillId":"$skill","seed":0,"index":0,"contentRevision":"pilot-2","generatorRevision":"generator-2"}""",
                )
            }
            validate(HttpMethod.Post, "/api/v1/exercises", response)
        }
        val lexicalResponse = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"E007","seed":47,"index":0,"contentRevision":"pilot-3","generatorRevision":"generator-3"}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/exercises", lexicalResponse)
        val lexicalId = (wireJson.parseToJsonElement(lexicalResponse.bodyAsText()) as JsonObject)
            .getValue("id").jsonPrimitive.content
        val lexicalAttemptId = UUID.randomUUID()
        validate(
            HttpMethod.Post,
            "/api/v1/attempts",
            client.post("/api/v1/attempts") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"attemptId":"$lexicalAttemptId","exerciseId":"$lexicalId","answer":{"kind":"text","text":"book"},"hintUsed":false}""",
                )
            },
        )
        validate(
            HttpMethod.Get,
            "/api/v1/dev/exercises/$lexicalId",
            client.get("/api/v1/dev/exercises/$lexicalId") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Inspector-Key", "k".repeat(32))
            },
        )
        val recallResponse = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"E008","seed":47,"index":0,"contentRevision":"pilot-3","generatorRevision":"generator-3"}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/exercises", recallResponse)
        listOf("E009" to "G005", "E010" to null).forEach { (format, target) ->
            val targetJson = target?.let { ",\"targetSkillId\":\"$it\"" }.orEmpty()
            val currentResponse = client.post("/api/v1/exercises") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"formatId":"$format"$targetJson,"seed":47,"index":0,"contentRevision":"pilot-4","generatorRevision":"generator-4"}""",
                )
            }
            validate(HttpMethod.Post, "/api/v1/exercises", currentResponse)
            val currentId = (wireJson.parseToJsonElement(currentResponse.bodyAsText()) as JsonObject)
                .getValue("id").jsonPrimitive.content
            val inspectorResponse = client.get("/api/v1/dev/exercises/$currentId") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Inspector-Key", "k".repeat(32))
            }
            validate(HttpMethod.Get, "/api/v1/dev/exercises/$currentId", inspectorResponse)
            val inspector = (wireJson.parseToJsonElement(inspectorResponse.bodyAsText()) as JsonObject)
            val answer = if (format == "E009") {
                inspector.getValue("canonicalSentence").jsonPrimitive.content
            } else {
                (inspector.getValue("readingSource") as JsonObject).getValue("answer").jsonPrimitive.content
            }
            validate(
                HttpMethod.Post,
                "/api/v1/attempts",
                client.post("/api/v1/attempts") {
                    trustedHost()
                    cookie("astrid_session", cookie)
                    header("X-Astrid-Client", "playground")
                    contentType(ContentType.Application.Json)
                    setBody(
                        wireJson.encodeToString(
                            AttemptRequest(UUID.randomUUID().toString(), currentId, AttemptAnswerDto.Text(answer), false),
                        ),
                    )
                },
            )
        }
        val exerciseId = (wireJson.parseToJsonElement(exerciseResponse.bodyAsText()) as JsonObject)
            .getValue("id").jsonPrimitive.content
        val attemptId = UUID.randomUUID()
        val attemptResponse = client.post("/api/v1/attempts") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"attemptId":"$attemptId","exerciseId":"$exerciseId","answer":{"kind":"text","text":"læser"},"hintUsed":false}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/attempts", attemptResponse)
        validate(HttpMethod.Get, "/api/v1/attempts/$attemptId", client.get("/api/v1/attempts/$attemptId") {
            trustedHost()
            cookie("astrid_session", cookie)
        })
        val problem = client.get("/api/v1/exercises/not-a-uuid") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        validate(HttpMethod.Get, "/api/v1/exercises/not-a-uuid", problem)
    }

    @Test
    fun `security and production mode responses conform to declared api statuses`() = testApplication {
        application { astridModule(config(developmentMode = false), InMemoryStores()) }
        val forbiddenCatalog = client.get("/api/v1/catalog") {
            trustedHost()
            header(HttpHeaders.Origin, "https://attacker.example")
        }
        validate(HttpMethod.Get, "/api/v1/catalog", forbiddenCatalog)
        val forbiddenHealth = client.get("/api/v1/health") {
            trustedHost()
            header("X-Forwarded-Host", "attacker.example")
        }
        validate(HttpMethod.Get, "/api/v1/health", forbiddenHealth)
        val forbiddenReady = client.get("/api/v1/ready") {
            trustedHost()
            header(HttpHeaders.Origin, "https://attacker.example")
        }
        validate(HttpMethod.Get, "/api/v1/ready", forbiddenReady)
        validate(HttpMethod.Get, "/api/v1/catalog", client.get("/api/v1/catalog") { trustedHost() })
        val createExercise = client.post("/api/v1/exercises") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(EXERCISE_REQUEST)
        }
        validate(HttpMethod.Post, "/api/v1/exercises", createExercise)
    }

    @Test
    fun `creation responses carry a Location header and an unready service answers a problem`() = testApplication {
        val repository = InMemoryStores()
        val unready = object : Stores by repository {
            override val readiness = Readiness { false }
        }
        application { astridModule(config(), unready) }
        val session = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val cookie = session.headers[HttpHeaders.SetCookie]!!.substringAfter("astrid_session=").substringBefore(';')
        val exercise = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(EXERCISE_REQUEST)
        }
        validate(HttpMethod.Post, "/api/v1/exercises", exercise)
        val id = (wireJson.parseToJsonElement(exercise.bodyAsText()) as JsonObject).getValue("id").jsonPrimitive.content
        assertEquals("/api/v1/exercises/$id", exercise.headers[HttpHeaders.Location])

        val context = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        val owner = (wireJson.parseToJsonElement(context.bodyAsText()) as JsonObject)
            .getValue("ownerSessionId").jsonPrimitive.content
        val practiceId = UUID.randomUUID()
        val practice = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", owner)
            contentType(ContentType.Application.Json)
            setBody("""{"practiceSessionId":"$practiceId","focusId":"reading-basics","planRevision":"practice-1","seed":47}""")
        }
        validate(HttpMethod.Post, "/api/v1/practice-sessions", practice)
        assertEquals(201, practice.status.value)
        assertEquals("/api/v1/practice-sessions/$practiceId", practice.headers[HttpHeaders.Location])

        val ready = client.get("/api/v1/ready") { trustedHost() }
        validate(HttpMethod.Get, "/api/v1/ready", ready)
        assertEquals(503, ready.status.value)
        val code = (wireJson.parseToJsonElement(ready.bodyAsText()) as JsonObject).getValue("code").jsonPrimitive.content
        assertEquals("not_ready", code)
    }

    private suspend fun validate(method: HttpMethod, path: String, response: HttpResponse) {
        val body = response.bodyAsText()
        if (response.status.value >= 400) {
            assertEquals(
                ProblemJson.toString(),
                response.contentType()?.withoutParameters()?.toString(),
                "Error responses must be application/problem+json: ${response.status} $body",
            )
        }
        val request = SimpleRequest.Builder(method.value, path).build()
        val simpleResponse = SimpleResponse.Builder(response.status.value).apply {
            response.headers.forEach { name, values -> values.forEach { withHeader(name, it) } }
            if (body.isNotEmpty()) withBody(body)
        }.build()
        val report = validator.validate(request, simpleResponse)
        if (report.hasErrors()) {
            throw AssertionError(SimpleValidationReportFormat.getInstance().apply(report) + "\n$body")
        }
    }

    private fun assertExerciseRequestValidity(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/exercises")
            .withHeader("Content-Type", "application/json")
            .withHeader("X-Astrid-Client", "playground")
            .withBody(body)
            .build()
        val report = strictValidator.validateRequest(request)
        assertEquals(expected, !report.hasErrors(), SimpleValidationReportFormat.getInstance().apply(report))
    }

    private fun assertExerciseResponseValidity(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/exercises").build()
        val response = SimpleResponse.Builder(201)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .build()
        val report = validator.validate(request, response)
        assertEquals(expected, !report.hasErrors(), SimpleValidationReportFormat.getInstance().apply(report))
    }

    private fun exerciseRequest(format: String, target: String?, content: String, generator: String): String {
        val targetJson = target?.let { "\"targetSkillId\":\"$it\"," }.orEmpty()
        return """{"formatId":"$format",$targetJson"seed":47,"index":0,"contentRevision":"$content","generatorRevision":"$generator"}"""
    }

    private fun exerciseResponse(
        format: String,
        content: String,
        generator: String,
        target: String? = null,
        lexicalFacet: String? = null,
        lexicalDirection: String? = null,
        readingFacet: String? = null,
        passage: Boolean = false,
    ): String {
        val targetJson = target?.let { "\"targetSkillId\":\"$it\"," }.orEmpty()
        val lexicalJson = if (lexicalFacet != null && lexicalDirection != null) {
            """"lexicalTarget":{"entryId":"L014","senseId":"L014-S01","facet":"$lexicalFacet","direction":"$lexicalDirection"},"""
        } else {
            ""
        }
        val readingJson = readingFacet?.let { """"readingTarget":{"facet":"$it"},""" }.orEmpty()
        val passageJson = if (passage) ""","passage":["Hun læser en avis.","Han læser en bog.","Hun har et brev."]""" else ""
        val prompt =
            """{"before":"","after":"","choices":["a newspaper","a book","a letter"],"blocks":[],"punctuation":"","cue":"Question","cueLanguage":"en","answerLanguage":"en"$passageJson}"""
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"$format",$targetJson$lexicalJson$readingJson"supportSkillIds":[],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
    }

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config(developmentMode: Boolean = true, inspectorKey: String? = null) = ServerConfig(
        developmentMode = developmentMode,
        inspectorEnabled = inspectorKey != null,
        inspectorKey = inspectorKey,
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

    private companion object {
        const val EXERCISE_REQUEST =
            """{"formatId":"E001","targetSkillId":"G004","seed":7,"index":0,"contentRevision":"pilot-1","generatorRevision":"generator-1"}"""
    }
}
