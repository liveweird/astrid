@file:Suppress("MaxLineLength") // Complete JSON wire fixtures remain readable on one line.

package app.astrid.server

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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenApiContractTest {
    private val rawSpec = checkNotNull(javaClass.getResource("/openapi.yaml")).readText()
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
    fun `spec parses and validates critical runtime responses`() = testApplication {
        val parse = OpenAPIV3Parser().readContents(rawSpec)
        assertNotNull(parse.openAPI, "OpenAPI parse failed: ${parse.messages}")
        application { astridModule(config(inspectorKey = "k".repeat(32)), InMemoryAstridRepository()) }

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
        application { astridModule(config(developmentMode = false), InMemoryAstridRepository()) }
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

    private suspend fun validate(method: HttpMethod, path: String, response: HttpResponse) {
        val body = response.bodyAsText()
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
