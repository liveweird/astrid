@file:Suppress("MaxLineLength") // Complete JSON wire fixtures remain readable on one line.

package app.astrid.server

import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.GapSelectionDto
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
import app.astrid.server.plugins.ProblemJson
import app.astrid.server.plugins.Readiness
import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.schema.ValidationConfiguration
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
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import java.io.File
import java.lang.management.ManagementFactory
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OpenApiContractTest {
    private val rawSpec = checkNotNull(javaClass.getResource("/openapi/documentation.yaml")).readText()
    // Retain schema refs so each cache entry does not copy the complete exercise union. Responses and request bodies
    // still resolve before validation; two cache entries bound injected component-definition copies at 512 MiB.
    private fun validatorBuilder() =
        OpenApiInteractionValidator.createForInlineApiSpecification(rawSpec)
            .withParseOptions(ParseOptions().apply {
                isResolve = true
                isResolveFully = false
                isResolveResponses = true
                isResolveRequestBody = true
                isResolveCombinators = false
                isExplicitObjectSchema = false
            })
            .withSchemaValidationConfiguration(ValidationConfiguration().setMaxCacheSize(SCHEMA_CACHE_ENTRIES))

    private val strictValidator by lazy { validatorBuilder().build() }
    private val validator by lazy {
        validatorBuilder()
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
    }

    @Test
    fun `spec info version equals the application version`() {
        val versionSource = File("../web/src/changelog/version.ts").readText()
        val version = checkNotNull(Regex("export const APP_VERSION = \"([^\"]+)\"").find(versionSource)).groupValues[1]
        val spec = checkNotNull(OpenAPIV3Parser().readContents(rawSpec).openAPI)
        assertEquals(version, spec.info.version)
    }

    @Test
    fun `validator retains schema refs while resolving shared problem responses`() {
        val options = ParseOptions().apply {
            isResolve = true
            isResolveFully = false
            isResolveResponses = true
            isResolveRequestBody = true
            isResolveCombinators = false
            isExplicitObjectSchema = false
        }
        val parsed = assertNotNull(OpenAPIV3Parser().readContents(rawSpec, null, options).openAPI)
        assertNotNull(parsed.components.schemas.getValue("Exercise").oneOf.first().`$ref`)
        assertNotNull(parsed.paths.getValue("/api/v1/catalog").get.responses.getValue("401")
            .content.getValue("application/problem+json").schema)

        val request = SimpleRequest.Builder.get("/api/v1/catalog").build()
        val validBody = """{"type":"about:blank","title":"Unauthorized","status":401,"detail":"No session","instance":"/api/v1/catalog","code":"UNAUTHORIZED"}"""
        fun report(contentType: String, body: String) = validator.validateResponse(
            "/api/v1/catalog",
            request.method,
            SimpleResponse.Builder(401).withHeader("Content-Type", contentType).withBody(body).build(),
        )
        assertFalse(report("application/problem+json", validBody).hasErrors())
        assertTrue(report("application/problem+json", "{}").hasErrors())
        assertTrue(report("application/json", validBody).hasErrors())
    }

    @Test
    fun `response-only validation keeps path method status and body checks strict`() {
        val healthPath = "/api/v1/health"
        val valid = SimpleResponse.Builder(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""{"status":"ok"}""")
            .build()
        val malformed = SimpleResponse.Builder(200)
            .withHeader("Content-Type", "application/json")
            .withBody("""{"unexpected":true}""")
            .build()
        val undeclared = SimpleResponse.Builder(418).build()
        val unknownPath = validator.validateResponse(
            "/api/v1/unknown",
            SimpleRequest.Builder.get("/api/v1/unknown").build().method,
            valid,
        )
        val wrongMethod = validator.validateResponse(
            healthPath,
            SimpleRequest.Builder.post(healthPath).build().method,
            valid,
        )

        assertFalse(validator.validateResponse(healthPath, SimpleRequest.Builder.get(healthPath).build().method, valid).hasErrors())
        assertTrue(unknownPath.hasErrors())
        assertTrue(wrongMethod.hasErrors())
        assertEquals(setOf("validation.request.path.missing"), unknownPath.messages.map { it.key }.toSet())
        assertEquals(setOf("validation.request.operation.notAllowed"), wrongMethod.messages.map { it.key }.toSet())
        assertTrue(validator.validateResponse(healthPath, SimpleRequest.Builder.get(healthPath).build().method, undeclared).hasErrors())
        assertTrue(validator.validateResponse(healthPath, SimpleRequest.Builder.get(healthPath).build().method, malformed).hasErrors())
    }

    @Test
    fun `practice creation schema admits only supported focus revision pairs`() {
        val requestValidator = validatorBuilder().build()
        val id = "4d2dc82d-bbdc-4645-a78a-ad0be38c4355"
        val supportedPairs = setOf(
            "reading-basics" to "practice-1",
            "present-verbs" to "present-verbs-1",
            "verb-article-gaps" to "verb-article-gaps-1",
            "module-one-singular" to "module-one-singular-1",
            "module-one-groups" to "module-one-groups-1",
            "module-one-er" to "module-one-er-1",
            "module-one-things" to "module-one-things-1",
            "module-one-present" to "module-one-present-1",
            "module-one-order" to "module-one-order-1",
            "module-one-possession" to "module-one-possession-1",
            "module-one-reading-words" to "module-one-reading-words-1",
            "module-one-personal" to "module-one-personal-1",
            "module-one-ability" to "module-one-ability-1",
            "module-one-volition" to "module-one-volition-1",
            "module-one-obligation" to "module-one-obligation-1",
            "module-one-permission" to "module-one-permission-1",
            "module-one-circumstances" to "module-one-circumstances-1",
            "module-one-questions" to "module-one-questions-1",
            "vocabulary-p1-s1" to "vocabulary-p1-s1-1",
            "vocabulary-p1-s2" to "vocabulary-p1-s2-1",
            "vocabulary-p1-s3" to "vocabulary-p1-s3-1",
            "vocabulary-p1-s4" to "vocabulary-p1-s4-1",
            "vocabulary-p1-s5" to "vocabulary-p1-s5-1",
            "vocabulary-p1-s1" to "vocabulary-p1-s1-2",
            "vocabulary-p1-s2" to "vocabulary-p1-s2-2",
            "vocabulary-p1-s3" to "vocabulary-p1-s3-2",
            "vocabulary-p1-s4" to "vocabulary-p1-s4-2",
            "vocabulary-p1-s5" to "vocabulary-p1-s5-2",
            "cumulative-review" to "cumulative-review-1",
        )
        for (focus in supportedPairs.map { it.first }) {
            for (revision in supportedPairs.map { it.second }) {
                val request = SimpleRequest.Builder.post("/api/v1/practice-sessions")
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Astrid-Client", "playground")
                    .withHeader("X-Astrid-Practice-Owner", id)
                    .withBody("""{"practiceSessionId":"$id","focusId":"$focus","planRevision":"$revision","seed":47}""")
                    .build()
                val report = requestValidator.validateRequest(request)
                val supported = focus to revision in supportedPairs
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
            exerciseRequest("E011", null, "pilot-5", "generator-5"),
            exerciseRequest("E012", "G005", "pilot-5", "generator-5"),
            exerciseRequest("E013", "G004", "pilot-5", "generator-5"),
            exerciseRequest("E001", "G004", "pilot-6", "generator-6"),
            exerciseRequest("E002", "G006", "pilot-6", "generator-6"),
            exerciseRequest("E003", "G005", "pilot-6", "generator-6"),
            exerciseRequest("E007", null, "pilot-6", "generator-6"),
            exerciseRequest("E009", "G005", "pilot-6", "generator-6"),
            exerciseRequest("E010", null, "pilot-6", "generator-6"),
            exerciseRequest("E011", null, "pilot-6", "generator-6"),
            exerciseRequest("E012", "G005", "pilot-6", "generator-6"),
            exerciseRequest("E013", "G004", "pilot-6", "generator-6"),
            exerciseRequest("E014", null, "pilot-6", "generator-6"),
            exerciseRequest("E001", "G004", "pilot-7", "generator-7"),
            exerciseRequest("E002", "G006", "pilot-7", "generator-7"),
            exerciseRequest("E003", "G005", "pilot-7", "generator-7"),
            exerciseRequest("E007", null, "pilot-7", "generator-7"),
            exerciseRequest("E009", "G005", "pilot-7", "generator-7"),
            exerciseRequest("E010", null, "pilot-7", "generator-7"),
            exerciseRequest("E011", null, "pilot-7", "generator-7"),
            exerciseRequest("E012", "G005", "pilot-7", "generator-7"),
            exerciseRequest("E013", "G004", "pilot-7", "generator-7"),
            exerciseRequest("E014", null, "pilot-7", "generator-7"),
            exerciseRequest("E015", "G004", "pilot-7", "generator-7"),
            exerciseRequest("E001", "G004", "pilot-8", "generator-8"),
            exerciseRequest("E014", null, "pilot-8", "generator-8"),
            exerciseRequest("E015", "G004", "pilot-8", "generator-8"),
            exerciseRequest("E016", "G003", "pilot-8", "generator-8"),
            exerciseRequest("E017", "G003", "pilot-8", "generator-8"),
            exerciseRequest("E018", "G004", "pilot-8", "generator-8"),
            exerciseRequest("E019", "G004", "pilot-8", "generator-8"),
            exerciseRequest("E001", "G004", "pilot-9", "generator-9"),
            exerciseRequest("E014", null, "pilot-9", "generator-9"),
            exerciseRequest("E015", "G004", "pilot-9", "generator-9"),
            exerciseRequest("E016", "G003", "pilot-9", "generator-9"),
            exerciseRequest("E019", "G004", "pilot-9", "generator-9"),
            exerciseRequest("E020", "G038", "pilot-9", "generator-9"),
            exerciseRequest("E021", "G038", "pilot-9", "generator-9"),
            exerciseRequest("E001", "G004", "pilot-10", "generator-10"),
            exerciseRequest("E014", null, "pilot-10", "generator-10"),
            exerciseRequest("E020", "G038", "pilot-10", "generator-10"),
            exerciseRequest("E022", "G004", "pilot-10", "generator-10"),
            exerciseRequest("E023", "G004", "pilot-10", "generator-10"),
            exerciseRequest("E024", "G005", "pilot-10", "generator-10"),
            exerciseRequest("E025", "G005", "pilot-10", "generator-10"),
            exerciseRequest("E022", "G004", "pilot-11", "generator-11"),
            exerciseRequest("E023", "G004", "pilot-11", "generator-11"),
            exerciseRequest("E025", "G005", "pilot-11", "generator-11"),
            exerciseRequest("E022", "G004", "pilot-12", "generator-12"),
            exerciseRequest("E023", "G004", "pilot-12", "generator-12"),
            exerciseRequest("E026", "G030", "pilot-13", "generator-13"),
            exerciseRequest("E027", "G030", "pilot-13", "generator-13"),
            exerciseRequest("E028", "G030", "pilot-14", "generator-14"),
            exerciseRequest("E025", "G005", "pilot-14", "generator-14"),
            exerciseRequest("E029", "G016", "pilot-15", "generator-15"),
            exerciseRequest("E030", "G018", "pilot-16", "generator-16"),
            exerciseRequest("E001", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E002", "G006", "pilot-17", "generator-17"),
            exerciseRequest("E003", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E007", null, "pilot-17", "generator-17"),
            exerciseRequest("E016", "G003", "pilot-17", "generator-17"),
            exerciseRequest("E020", "G038", "pilot-17", "generator-17"),
            exerciseRequest("E026", "G030", "pilot-17", "generator-17"),
            exerciseRequest("E029", "G016", "pilot-17", "generator-17"),
            exerciseRequest("E030", "G018", "pilot-17", "generator-17"),
            exerciseRequest("E031", null, "pilot-17", "generator-17"),
            exerciseRequest("E032", null, "pilot-17", "generator-17"),
            exerciseRequest("E033", null, "pilot-17", "generator-17"),
            exerciseRequest("E034", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E035", null, "pilot-17", "generator-17"),
            exerciseRequest("E036", null, "pilot-17", "generator-17"),
            exerciseRequest("E037", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E038", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E039", null, "pilot-17", "generator-17"),
            exerciseRequest("E001", "G004", "pilot-18", "generator-18"),
            exerciseRequest("E002", "G006", "pilot-18", "generator-18"),
            exerciseRequest("E003", "G005", "pilot-18", "generator-18"),
            exerciseRequest("E007", null, "pilot-18", "generator-18"),
            exerciseRequest("E016", "G003", "pilot-18", "generator-18"),
            exerciseRequest("E020", "G038", "pilot-18", "generator-18"),
            exerciseRequest("E026", "G030", "pilot-18", "generator-18"),
            exerciseRequest("E029", "G016", "pilot-18", "generator-18"),
            exerciseRequest("E030", "G018", "pilot-18", "generator-18"),
            exerciseRequest("E031", null, "pilot-18", "generator-18"),
            exerciseRequest("E032", null, "pilot-18", "generator-18"),
            exerciseRequest("E033", null, "pilot-18", "generator-18"),
            exerciseRequest("E034", "G004", "pilot-18", "generator-18"),
            exerciseRequest("E035", null, "pilot-18", "generator-18"),
            exerciseRequest("E036", null, "pilot-18", "generator-18"),
            exerciseRequest("E037", "G005", "pilot-18", "generator-18"),
            exerciseRequest("E038", "G004", "pilot-18", "generator-18"),
            exerciseRequest("E039", null, "pilot-18", "generator-18"),
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
            exerciseRequest("E011", "G005", "pilot-5", "generator-5"),
            exerciseRequest("E011", null, "pilot-4", "generator-4"),
            exerciseRequest("E012", "G004", "pilot-5", "generator-5"),
            exerciseRequest("E012", "G005", "pilot-5", "generator-4"),
            exerciseRequest("E013", "G005", "pilot-5", "generator-5"),
            exerciseRequest("E014", "G004", "pilot-6", "generator-6"),
            exerciseRequest("E014", null, "pilot-5", "generator-5"),
            exerciseRequest("E014", null, "pilot-6", "generator-5"),
            exerciseRequest("E015", null, "pilot-7", "generator-7"),
            exerciseRequest("E015", "G006", "pilot-7", "generator-7"),
            exerciseRequest("E015", "G004", "pilot-6", "generator-6"),
            exerciseRequest("E015", "G004", "pilot-7", "generator-6"),
            exerciseRequest("E016", "G004", "pilot-8", "generator-8"),
            exerciseRequest("E017", null, "pilot-8", "generator-8"),
            exerciseRequest("E018", "G003", "pilot-8", "generator-8"),
            exerciseRequest("E019", "G004", "pilot-7", "generator-7"),
            exerciseRequest("E020", "G003", "pilot-9", "generator-9"),
            exerciseRequest("E022", "G004", "pilot-9", "generator-9"),
            exerciseRequest("E023", "G004", "pilot-10", "generator-9"),
            exerciseRequest("E024", "G004", "pilot-10", "generator-10"),
            exerciseRequest("E025", null, "pilot-10", "generator-10"),
            exerciseRequest("E022", "G004", "pilot-10", "generator-11"),
            exerciseRequest("E023", "G005", "pilot-11", "generator-11"),
            exerciseRequest("E022", "G004", "pilot-12", "generator-11"),
            exerciseRequest("E023", "G005", "pilot-12", "generator-12"),
            exerciseRequest("E026", "G030", "pilot-12", "generator-12"),
            exerciseRequest("E027", "G004", "pilot-13", "generator-13"),
            exerciseRequest("E028", "G030", "pilot-13", "generator-13"),
            exerciseRequest("E028", "G005", "pilot-14", "generator-14"),
            exerciseRequest("E025", "G030", "pilot-14", "generator-14"),
            exerciseRequest("E029", "G016", "pilot-14", "generator-14"),
            exerciseRequest("E029", "G005", "pilot-15", "generator-15"),
            exerciseRequest("E028", "G030", "pilot-15", "generator-14"),
            exerciseRequest("E030", "G018", "pilot-15", "generator-15"),
            exerciseRequest("E030", "G016", "pilot-16", "generator-16"),
            exerciseRequest("E029", "G016", "pilot-16", "generator-15"),
            exerciseRequest("E021", null, "pilot-9", "generator-9"),
            exerciseRequest("E020", "G038", "pilot-8", "generator-8"),
            exerciseRequest("E020", "G038", "pilot-9", "generator-8"),
            exerciseRequest("E031", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E033", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E034", null, "pilot-17", "generator-17"),
            exerciseRequest("E034", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E037", null, "pilot-17", "generator-17"),
            exerciseRequest("E037", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E038", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E039", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E031", null, "pilot-17", "generator-17").replace("\"index\":0", "\"index\":1000001"),
            exerciseRequest("E001", null, "pilot-17", "generator-17"),
            exerciseRequest("E001", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E002", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E003", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E007", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E016", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E020", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E026", "G004", "pilot-17", "generator-17"),
            exerciseRequest("E029", "G005", "pilot-17", "generator-17"),
            exerciseRequest("E030", "G016", "pilot-17", "generator-17"),
            exerciseRequest("E033", null, "pilot-17", "generator-18"),
            exerciseRequest("E033", null, "pilot-18", "generator-17"),
            exerciseRequest("E034", null, "pilot-18", "generator-18"),
            exerciseRequest("E037", "G004", "pilot-18", "generator-18"),
            exerciseRequest("E039", "G004", "pilot-18", "generator-18"),
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
            exerciseResponse(
                "E011",
                "pilot-5",
                "generator-5",
                lexicalFacet = "meaning",
                lexicalDirection = "en-da",
            ),
            exerciseResponse("E012", "pilot-5", "generator-5", target = "G005", tileMode = "use-all"),
            exerciseResponse("E013", "pilot-5", "generator-5", target = "G004", tileMode = "with-spares"),
            exerciseResponse("E001", "pilot-6", "generator-6", target = "G004"),
            exerciseResponse("E011", "pilot-6", "generator-6", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            exerciseResponse("E012", "pilot-6", "generator-6", target = "G005", tileMode = "use-all"),
            matchingExerciseResponse(),
            exerciseResponse("E001", "pilot-7", "generator-7", target = "G004"),
            exerciseResponse("E011", "pilot-7", "generator-7", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            exerciseResponse("E012", "pilot-7", "generator-7", target = "G005", tileMode = "use-all"),
            matchingExerciseResponse(content = "pilot-7"),
            gapExerciseResponse(),
            exerciseResponse("E001", "pilot-8", "generator-8", target = "G004"),
            matchingExerciseResponse(content = "pilot-8"),
            gapExerciseResponse(content = "pilot-8"),
            moduleOneExerciseResponse("E016", "G003", "human-singular"),
            moduleOneExerciseResponse("E017", "G003", "human-group"),
            moduleOneExerciseResponse("E018", "G004", "vaere-present"),
            moduleOneExerciseResponse("E019", "G004", "vaere-present"),
            moduleOneExerciseResponse("E020", "G038", "inanimate-singular-subject", "pilot-9"),
            moduleOneExerciseResponse("E021", "G038", "inanimate-singular-subject", "pilot-9"),
            moduleOneExerciseResponse("E022", "G004", "named-three-verbs-present", "pilot-10"),
            moduleOneExerciseResponse("E024", "G005", "subject-first-supplied", "pilot-10"),
            moduleOneExerciseResponse("E022", "G004", "have-possession-present", "pilot-11"),
            moduleOneExerciseResponse("E023", "G004", "named-three-verbs-present", "pilot-11"),
            moduleOneExerciseResponse("E022", "G004", "personal-introduction-present", "pilot-12"),
            moduleOneExerciseResponse("E023", "G004", "personal-introduction-present", "pilot-12"),
            moduleOneExerciseResponse("E026", "G030", "kan-bare-infinitive", "pilot-13"),
            moduleOneExerciseResponse("E027", "G030", "kan-bare-infinitive", "pilot-13"),
            moduleOneExerciseResponse("E028", "G030", "modal-volition-meaning", "pilot-14"),
            moduleOneExerciseResponse("E028", "G030", "modal-obligation-meaning", "pilot-14"),
            moduleOneExerciseResponse("E028", "G030", "modal-permission-meaning", "pilot-14"),
            moduleOneExerciseResponse("E025", "G005", "supplied-modal-order", "pilot-14"),
            moduleOneExerciseResponse("E029", "G016", "one-final-circumstance", "pilot-15"),
            moduleOneExerciseResponse("E030", "G018", "neutral-positive-yes-no", "pilot-16"),
            questionExerciseResponse(questionPrompt("Hun arbejder.", WORK_QUESTION_BLOCKS)),
            questionExerciseResponse(questionPrompt("I bor i Aarhus.", RESIDENCE_QUESTION_BLOCKS)),
            questionExerciseResponse(questionPrompt("De har et brev.", POSSESSION_QUESTION_BLOCKS)),
            exerciseResponse("E001", "pilot-17", "generator-17", target = "G004"),
            exerciseResponse("E007", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "da-en"),
            moduleOneExerciseResponse("E030", "G018", "neutral-positive-yes-no", "pilot-17"),
            exerciseResponse("E031", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "da-en"),
            exerciseResponse("E032", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            vocabularyMatchingExerciseResponse(),
            moduleOneExerciseResponse("E034", "G004", "p1-studere-present", "pilot-17"),
            exerciseResponse("E035", "pilot-17", "generator-17", lexicalFacet = "fixed-chunk", lexicalDirection = "en-da"),
            exerciseResponse("E036", "pilot-17", "generator-17", lexicalFacet = "mass-headword", lexicalDirection = "en-da"),
            moduleOneExerciseResponse("E037", "G005", "p1-subject-first-supplied", "pilot-17"),
            moduleOneExerciseResponse("E038", "G004", "p1-drikke-present", "pilot-17"),
            exerciseResponse("E039", "pilot-17", "generator-17", readingFacet = "explicit-fact", passage = true),
            exerciseResponse("E001", "pilot-18", "generator-18", target = "G004"),
            exerciseResponse("E007", "pilot-18", "generator-18", lexicalFacet = "meaning", lexicalDirection = "da-en"),
            moduleOneExerciseResponse("E030", "G018", "neutral-positive-yes-no", "pilot-18"),
            exerciseResponse("E031", "pilot-18", "generator-18", lexicalFacet = "meaning", lexicalDirection = "da-en"),
            exerciseResponse("E032", "pilot-18", "generator-18", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            vocabularyMatchingExerciseResponse(content = "pilot-18"),
            moduleOneExerciseResponse("E034", "G004", "p1-studere-present", "pilot-18"),
            exerciseResponse("E035", "pilot-18", "generator-18", lexicalFacet = "fixed-chunk", lexicalDirection = "en-da"),
            exerciseResponse("E036", "pilot-18", "generator-18", lexicalFacet = "mass-headword", lexicalDirection = "en-da"),
            moduleOneExerciseResponse("E037", "G005", "p1-subject-first-supplied", "pilot-18"),
            moduleOneExerciseResponse("E038", "G004", "p1-drikke-present", "pilot-18"),
            exerciseResponse("E039", "pilot-18", "generator-18", readingFacet = "explicit-fact", passage = true),
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
            exerciseResponse(
                "E011",
                "pilot-5",
                "generator-5",
                lexicalFacet = "meaning",
                lexicalDirection = "da-en",
            ),
            exerciseResponse("E012", "pilot-5", "generator-5", target = "G004", tileMode = "use-all"),
            exerciseResponse("E012", "pilot-5", "generator-5", target = "G005", tileMode = "with-spares"),
            exerciseResponse("E013", "pilot-5", "generator-4", target = "G004", tileMode = "with-spares"),
            matchingExerciseResponse(content = "pilot-5"),
            matchingExerciseResponse(includeTargets = false),
            matchingExerciseResponse(answerLanguage = "en"),
            matchingExerciseResponse(firstLeftId = "r0"),
            matchingExerciseResponse(firstLeftId = "l4"),
            matchingExerciseResponse(firstRightId = "l0"),
            matchingExerciseResponse(firstRightId = "r4"),
            gapExerciseResponse(target = "G006"),
            gapExerciseResponse(content = "pilot-6"),
            gapExerciseResponse(primaryRole = "support"),
            gapExerciseResponse(supportSkill = "G004"),
            moduleOneExerciseResponse("E016", "G004", "human-singular"),
            moduleOneExerciseResponse("E017", "G003", "vaere-present"),
            moduleOneExerciseResponse("E018", "G004", "human-group"),
            moduleOneExerciseResponse("E020", "G003", "inanimate-singular-subject", "pilot-9"),
            moduleOneExerciseResponse("E021", "G038", "human-singular", "pilot-9"),
            moduleOneExerciseResponse("E023", "G004", "vaere-present", "pilot-10"),
            moduleOneExerciseResponse("E024", "G005", "subject-first-supplied", "pilot-9"),
            moduleOneExerciseResponse("E025", "G004", "subject-first-supplied", "pilot-10"),
            moduleOneExerciseResponse("E022", "G004", "have-possession-present", "pilot-10"),
            moduleOneExerciseResponse("E023", "G005", "have-possession-present", "pilot-11"),
            moduleOneExerciseResponse("E022", "G004", "personal-introduction-present", "pilot-11"),
            moduleOneExerciseResponse("E023", "G005", "personal-introduction-present", "pilot-12"),
            moduleOneExerciseResponse("E026", "G030", "kan-bare-infinitive", "pilot-12"),
            moduleOneExerciseResponse("E027", "G004", "kan-bare-infinitive", "pilot-13"),
            moduleOneExerciseResponse("E027", "G030", "personal-introduction-present", "pilot-13"),
            moduleOneExerciseResponse("E028", "G030", "modal-volition-meaning", "pilot-13"),
            moduleOneExerciseResponse("E028", "G005", "modal-volition-meaning", "pilot-14"),
            moduleOneExerciseResponse("E025", "G030", "supplied-modal-order", "pilot-14"),
            moduleOneExerciseResponse("E029", "G005", "one-final-circumstance", "pilot-15"),
            moduleOneExerciseResponse("E029", "G016", "subject-first-supplied", "pilot-15"),
            moduleOneExerciseResponse("E030", "G016", "neutral-positive-yes-no", "pilot-16"),
            moduleOneExerciseResponse("E030", "G018", "one-final-circumstance", "pilot-16"),
            moduleOneExerciseResponse("E030", "G018", "neutral-positive-yes-no", "pilot-15"),
            questionExerciseResponse(questionPrompt(blocks = "[]")),
            questionExerciseResponse(questionPrompt(blocks = """[{"id":"b0","text":"læser"},{"id":"b1","text":"du"}]""")),
            questionExerciseResponse(questionPrompt(blocks = """[{"id":"b0","text":"læser"},{"id":"b1","text":"du"},{"id":"b2","text":"en avis"},{"id":"b3","text":"ekstra"}]""")),
            questionExerciseResponse(questionPrompt(blocks = """[{"id":"b0","text":"læser"},{"id":"b1","text":"læser"},{"id":"b2","text":"en avis"}]""")),
            questionExerciseResponse(questionPrompt(blocks = WORK_QUESTION_BLOCKS)),
            questionExerciseResponse(questionPrompt(cue = "Du læser et brev.")),
            questionExerciseResponse(questionPrompt(punctuation = ".")),
            questionExerciseResponse(questionPrompt(cueLanguage = "en")),
            questionExerciseResponse(questionPrompt(answerLanguage = "en")),
            questionExerciseResponse(questionPrompt(before = "Start")),
            questionExerciseResponse(questionPrompt(after = "End")),
            questionExerciseResponse(questionPrompt(choices = "[\"læser\"]")),
            questionExerciseResponse(questionPrompt(extra = ",\"inputLabel\":\"Question\"")),
            questionExerciseResponse(questionPrompt(extra = ",\"replacementWord\":\"x\"")),
            questionExerciseResponse(questionPrompt(extra = ",\"passage\":[\"x\"]")),
            questionExerciseResponse(questionPrompt(extra = ",\"matchingColumns\":{\"left\":[],\"right\":[]}")),
            questionExerciseResponse(questionPrompt(extra = ",\"gapPrompt\":{\"segments\":[],\"slots\":[]}")),
            questionExerciseResponse(questionPrompt(extra = ",\"tileMode\":\"use-all\"")),
            questionExerciseResponse(questionPrompt(extra = ",\"requiredTileCount\":3")),
            exerciseResponse("E001", "pilot-17", "generator-17", target = "G005"),
            exerciseResponse("E007", "pilot-17", "generator-17", target = "G004", lexicalFacet = "meaning", lexicalDirection = "da-en"),
            moduleOneExerciseResponse("E030", "G016", "neutral-positive-yes-no", "pilot-17"),
            exerciseResponse("E031", "pilot-17", "generator-17", lexicalFacet = "fixed-chunk", lexicalDirection = "en-da"),
            exerciseResponse("E031", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            exerciseResponse("E032", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "da-en"),
            vocabularyMatchingExerciseResponse(facet = "fixed-chunk"),
            vocabularyMatchingExerciseResponse(direction = "da-en"),
            exerciseResponse("E035", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            exerciseResponse("E036", "pilot-17", "generator-17", lexicalFacet = "singular-headword", lexicalDirection = "en-da"),
            moduleOneExerciseResponse("E034", "G005", "p1-studere-present", "pilot-17"),
            moduleOneExerciseResponse("E034", "G004", "p1-subject-first-supplied", "pilot-17"),
            moduleOneExerciseResponse("E037", "G004", "p1-subject-first-supplied", "pilot-17"),
            moduleOneExerciseResponse("E038", "G004", "p1-studere-present", "pilot-17"),
            exerciseResponse("E039", "pilot-17", "generator-17", lexicalFacet = "meaning", lexicalDirection = "en-da"),
            exerciseResponse("E039", "pilot-17", "generator-17", readingFacet = "participant-action-complement", passage = true),
            exerciseResponse("E033", "pilot-17", "generator-18"),
            exerciseResponse("E033", "pilot-18", "generator-17"),
            moduleOneExerciseResponse("E034", "G005", "p1-studere-present", "pilot-18"),
            exerciseResponse("E039", "pilot-18", "generator-18", readingFacet = "participant-action-complement", passage = true),
        )

        accepted.forEach { assertExerciseResponseValidity(it, expected = true) }
        rejected.forEach { assertExerciseResponseValidity(it, expected = false) }
    }

    @Test
    fun `matching attempt schema admits exactly every partial bijection over the closed id domains`() {
        val fullMappings = permutations(listOf("r0", "r1", "r2", "r3")).map { rights ->
            rights.mapIndexed { index, right -> "l$index" to right }
        }
        val partialBijections = buildSet {
            fullMappings.forEach { mapping ->
                repeat(1 shl mapping.size) { mask ->
                    add(mapping.filterIndexed { index, _ -> mask and (1 shl index) != 0 })
                }
            }
        }
        assertEquals(24, fullMappings.size)
        assertEquals(209, partialBijections.size)
        assertEquals(24, partialBijections.count { it.size == 4 })
        partialBijections.forEach { assertAttemptRequestValidity(matchingAttemptRequest(it), expected = true) }
        assertAttemptRequestValidity(matchingAttemptRequest(fullMappings.first().reversed()), expected = true)

        val rejected = listOf(
            listOf("l0" to "r0", "l0" to "r1"),
            listOf("l0" to "r0", "l1" to "r0"),
            listOf("l0" to "r0", "l0" to "r0"),
            listOf("r0" to "l0"),
            listOf("l4" to "r0"),
            listOf("l0" to "r4"),
        )
        rejected.forEach { assertAttemptRequestValidity(matchingAttemptRequest(it), expected = false) }
    }

    @Test
    fun `gap attempt schema admits all thirteen ordered partial assignments and rejects duplicate slots`() {
        val assignments = listOf(
            emptyList(),
            listOf("g0" to "c0"),
            listOf("g0" to "c1"),
            listOf("g1" to "c0"),
            listOf("g1" to "c1"),
        ) + listOf("c0", "c1").flatMap { primary ->
            listOf("c0", "c1").flatMap { support ->
                val values = listOf("g0" to primary, "g1" to support)
                listOf(values, values.reversed())
            }
        }
        assertEquals(13, assignments.size)
        assignments.forEach { assertAttemptRequestValidity(gapAttemptRequest(it), expected = true) }

        val rejected = listOf(
            listOf("g0" to "c0", "g0" to "c1"),
            listOf("g1" to "c0", "g1" to "c1"),
            listOf("g0" to "c0", "g0" to "c0"),
            listOf("g2" to "c0"),
            listOf("g0" to "c2"),
        )
        rejected.forEach { assertAttemptRequestValidity(gapAttemptRequest(it), expected = false) }
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
    fun `current matching and gap attempt responses conform to the contract`() = testApplication {
        val inspectorKey = "k".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val sessionResponse = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val cookie = sessionResponse.headers[HttpHeaders.SetCookie]!!
            .substringAfter("astrid_session=").substringBefore(';')

        val matchingExercise = createCurrentExercise(cookie, "E014")
        val matchingId = matchingExercise.getValue("id").jsonPrimitive.content
        val matchingInspector = inspectExercise(cookie, inspectorKey, matchingId)
        val matchingPairs = matchingInspector.getValue("matchingSource").jsonObject
            .getValue("pairs").jsonArray.map { value ->
                val pair = value.jsonObject
                app.astrid.server.exercises.MatchingPairDto(
                    pair.getValue("leftId").jsonPrimitive.content,
                    pair.getValue("rightId").jsonPrimitive.content,
                )
            }
        val matchingAttempt = submitAttempt(
            cookie,
            matchingId,
            AttemptAnswerDto.Matching(matchingPairs),
        )
        validate(HttpMethod.Post, "/api/v1/attempts", matchingAttempt)
        assertEquals(
            "evaluation-16",
            wireJson.parseToJsonElement(matchingAttempt.bodyAsText()).jsonObject
                .getValue("evaluationRevision").jsonPrimitive.content,
        )

        val gapExercise = createCurrentExercise(cookie, "E015", "G004")
        val gapId = gapExercise.getValue("id").jsonPrimitive.content
        val gapInspector = inspectExercise(cookie, inspectorKey, gapId)
        val gapSelections = gapInspector.getValue("gapSource").jsonObject
            .getValue("slots").jsonArray.map { value ->
                val slot = value.jsonObject
                GapSelectionDto(
                    slot.getValue("slotId").jsonPrimitive.content,
                    slot.getValue("expectedChoiceId").jsonPrimitive.content,
                )
            }
        val gapAttempt = submitAttempt(cookie, gapId, AttemptAnswerDto.Gaps(gapSelections))
        validate(HttpMethod.Post, "/api/v1/attempts", gapAttempt)
        assertEquals(
            "evaluation-16",
            wireJson.parseToJsonElement(gapAttempt.bodyAsText()).jsonObject
                .getValue("evaluationRevision").jsonPrimitive.content,
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
        listOf("E011" to null, "E012" to "G005", "E013" to "G004").forEach { (format, target) ->
            val targetJson = target?.let { ",\"targetSkillId\":\"$it\"" }.orEmpty()
            val response = client.post("/api/v1/exercises") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"formatId":"$format"$targetJson,"seed":47,"index":0,"contentRevision":"pilot-5","generatorRevision":"generator-5"}""",
                )
            }
            validate(HttpMethod.Post, "/api/v1/exercises", response)
        }
        val matchingResponse = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"E014","seed":47,"index":0,"contentRevision":"pilot-6","generatorRevision":"generator-6"}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/exercises", matchingResponse)
        val matchingId = (wireJson.parseToJsonElement(matchingResponse.bodyAsText()) as JsonObject)
            .getValue("id").jsonPrimitive.content
        val matchingInspector = client.get("/api/v1/dev/exercises/$matchingId") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Inspector-Key", "k".repeat(32))
        }
        validate(HttpMethod.Get, "/api/v1/dev/exercises/$matchingId", matchingInspector)
        val matchingPairs = (wireJson.parseToJsonElement(matchingInspector.bodyAsText()) as JsonObject)
            .getValue("matchingSource").jsonObject.getValue("pairs").jsonArray.map { pair ->
                val value = pair.jsonObject
                app.astrid.server.exercises.MatchingPairDto(
                    value.getValue("leftId").jsonPrimitive.content,
                    value.getValue("rightId").jsonPrimitive.content,
                )
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
                        AttemptRequest(
                            UUID.randomUUID().toString(),
                            matchingId,
                            AttemptAnswerDto.Matching(matchingPairs),
                            false,
                        ),
                    ),
                )
            },
        )
        val gapResponse = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"E015","targetSkillId":"G004","seed":47,"index":0,"contentRevision":"pilot-7","generatorRevision":"generator-7"}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/exercises", gapResponse)
        val gapId = (wireJson.parseToJsonElement(gapResponse.bodyAsText()) as JsonObject)
            .getValue("id").jsonPrimitive.content
        val gapInspector = client.get("/api/v1/dev/exercises/$gapId") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Inspector-Key", "k".repeat(32))
        }
        validate(HttpMethod.Get, "/api/v1/dev/exercises/$gapId", gapInspector)
        validateAllGapOutcomeResponses(
            cookie,
            gapId,
            wireJson.parseToJsonElement(gapResponse.bodyAsText()).jsonObject,
            wireJson.parseToJsonElement(gapInspector.bodyAsText()).jsonObject,
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
    fun `guided practice and all nine saved gap outcomes conform to the contract`() = testApplication {
        val inspectorKey = "k".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val session = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val cookie = session.headers[HttpHeaders.SetCookie]!!.substringAfter("astrid_session=").substringBefore(';')
        val context = client.get("/api/v1/practice-context") { trustedHost(); cookie("astrid_session", cookie) }
        val owner = wireJson.parseToJsonElement(context.bodyAsText()).jsonObject
            .getValue("ownerSessionId").jsonPrimitive.content
        val practiceId = UUID.randomUUID()
        val created = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", owner)
            contentType(ContentType.Application.Json)
            setBody(
                """{"practiceSessionId":"$practiceId","focusId":"verb-article-gaps","planRevision":"verb-article-gaps-1","seed":47}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/practice-sessions", created)
        val state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        assertPracticeResponseValidity(state.toString(), expected = true)
        val badFollowUpThreePair = JsonObject(
            state + mapOf(
                "focusId" to JsonPrimitive("targeted-review"),
                "planRevision" to JsonPrimitive("follow-up-3"),
                "contentRevision" to JsonPrimitive("pilot-3"),
                "generatorRevision" to JsonPrimitive("generator-3"),
                "evaluationRevision" to JsonPrimitive("evaluation-3"),
            ),
        )
        assertPracticeResponseValidity(badFollowUpThreePair.toString(), expected = false)
        val badLegacyFollowUpPair = JsonObject(
            state + mapOf(
                "focusId" to JsonPrimitive("targeted-review"),
                "planRevision" to JsonPrimitive("follow-up-1"),
            ),
        )
        assertPracticeResponseValidity(badLegacyFollowUpPair.toString(), expected = false)
        val items = state.getValue("items").jsonArray
        val firstItem = items.first().jsonObject
        val verbAssessment = JsonObject(
            mapOf(
                "label" to JsonPrimitive("Present verb selection"),
                "targetSkillId" to JsonPrimitive("G004"),
                "assessmentMode" to JsonPrimitive("selection"),
                "assessmentRole" to JsonPrimitive("primary"),
            ),
        )
        val assessedE001Items = JsonArray(items.map { value ->
            JsonObject(
                value.jsonObject + mapOf(
                    "formatId" to JsonPrimitive("E001"),
                    "targetSkillId" to JsonPrimitive("G004"),
                    "assessments" to JsonArray(listOf(verbAssessment)),
                ),
            )
        })
        val pairThreeWithNewMetadata = JsonObject(
            state + mapOf(
                "focusId" to JsonPrimitive("targeted-review"),
                "planRevision" to JsonPrimitive("follow-up-1"),
                "contentRevision" to JsonPrimitive("pilot-3"),
                "generatorRevision" to JsonPrimitive("generator-3"),
                "evaluationRevision" to JsonPrimitive("evaluation-3"),
                "items" to assessedE001Items,
            ),
        )
        assertPracticeResponseValidity(pairThreeWithNewMetadata.toString(), expected = false)
        val legacyE001Items = JsonArray(items.map { value ->
            JsonObject(
                (value.jsonObject - "assessments") + mapOf(
                    "formatId" to JsonPrimitive("E001"),
                    "targetSkillId" to JsonPrimitive("G004"),
                ),
            )
        })
        val pairSevenWithoutNewMetadata = JsonObject(
            state + mapOf(
                "focusId" to JsonPrimitive("targeted-review"),
                "planRevision" to JsonPrimitive("follow-up-3"),
                "items" to legacyE001Items,
            ),
        )
        assertPracticeResponseValidity(pairSevenWithoutNewMetadata.toString(), expected = false)
        val withoutAssessments = JsonObject(firstItem.filterKeys { it != "assessments" })
        assertPracticeResponseValidity(
            JsonObject(state + ("items" to JsonArray(listOf(withoutAssessments) + items.drop(1)))).toString(),
            expected = false,
        )
        val firstAssessment = firstItem.getValue("assessments").jsonArray.first()
        val duplicatedAssessment = JsonObject(
            firstItem + ("assessments" to JsonArray(listOf(firstAssessment, firstAssessment))),
        )
        assertPracticeResponseValidity(
            JsonObject(state + ("items" to JsonArray(listOf(duplicatedAssessment) + items.drop(1)))).toString(),
            expected = false,
        )
        val exercise = state.getValue("currentExercise").jsonObject
        val exerciseId = exercise.getValue("id").jsonPrimitive.content
        val inspector = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Inspector-Key", inspectorKey)
        }
        val expected = wireJson.parseToJsonElement(inspector.bodyAsText()).jsonObject
            .getValue("gapSource").jsonObject.getValue("slots").jsonArray.associate { value ->
                value.jsonObject.let {
                    it.getValue("slotId").jsonPrimitive.content to it.getValue("expectedChoiceId").jsonPrimitive.content
                }
            }
        val choices = exercise.getValue("prompt").jsonObject.getValue("gapPrompt").jsonObject
            .getValue("slots").jsonArray.associate { value ->
                val slot = value.jsonObject
                slot.getValue("id").jsonPrimitive.content to slot.getValue("choices").jsonArray.map {
                    it.jsonObject.getValue("id").jsonPrimitive.content
                }
            }
        listOf<Boolean?>(null, true, false).forEach { primary ->
            listOf<Boolean?>(null, true, false).forEach { support ->
                val selections = listOf("g0" to primary, "g1" to support).mapNotNull { (slotId, correct) ->
                    correct?.let {
                        val answer = expected.getValue(slotId)
                        GapSelectionDto(slotId, if (it) answer else choices.getValue(slotId).single { choice -> choice != answer })
                    }
                }
                val attempt = client.post("/api/v1/attempts") {
                    trustedHost()
                    cookie("astrid_session", cookie)
                    header("X-Astrid-Client", "playground")
                    contentType(ContentType.Application.Json)
                    setBody(
                        wireJson.encodeToString(
                            AttemptRequest(
                                UUID.randomUUID().toString(),
                                exerciseId,
                                AttemptAnswerDto.Gaps(selections),
                                false,
                            ),
                        ),
                    )
                }
                validate(HttpMethod.Post, "/api/v1/attempts", attempt)
                validate(
                    HttpMethod.Get,
                    "/api/v1/practice-sessions/$practiceId",
                    client.get("/api/v1/practice-sessions/$practiceId") {
                        trustedHost()
                        cookie("astrid_session", cookie)
                    },
                )
            }
        }
        val advanced = client.post("/api/v1/practice-sessions/$practiceId/advance") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", owner)
            contentType(ContentType.Application.Json)
            setBody("""{"position":0}""")
        }
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
        var advancedState = wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
        for (position in 1..3) {
            val nextExerciseId = advancedState.getValue("currentExercise").jsonObject
                .getValue("id").jsonPrimitive.content
            val attempt = client.post("/api/v1/attempts") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    wireJson.encodeToString(
                        AttemptRequest(
                            UUID.randomUUID().toString(),
                            nextExerciseId,
                            AttemptAnswerDto.Gaps(emptyList()),
                            false,
                        ),
                    ),
                )
            }
            validate(HttpMethod.Post, "/api/v1/attempts", attempt)
            val next = client.post("/api/v1/practice-sessions/$practiceId/advance") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                header("X-Astrid-Practice-Owner", owner)
                contentType(ContentType.Application.Json)
                setBody("""{"position":$position}""")
            }
            validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", next)
            advancedState = wireJson.parseToJsonElement(next.bodyAsText()).jsonObject
        }
        assertEquals("completed", advancedState.getValue("state").jsonPrimitive.content)
        assertEquals(2, advancedState.getValue("summary").jsonObject.getValue("rows").jsonArray.size)
        val childId = UUID.randomUUID()
        val followUp = client.post("/api/v1/practice-sessions/$practiceId/follow-ups") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", owner)
            contentType(ContentType.Application.Json)
            setBody(
                """{"practiceSessionId":"$childId","planRevision":"follow-up-3","seed":48,"targetKeys":["grammar:G004:selection","grammar:G006:selection"]}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/follow-ups", followUp)
        val followUpState = wireJson.parseToJsonElement(followUp.bodyAsText()).jsonObject
        assertPracticeResponseValidity(followUpState.toString(), expected = true)
        assertPracticeResponseValidity(
            JsonObject(
                followUpState + ("items" to JsonArray(followUpState.getValue("items").jsonArray.take(3))),
            ).toString(),
            expected = false,
        )
    }

    @Test
    fun `all module one practice journeys and generated exercises conform to the contract`() = testApplication {
        val inspectorKey = "m".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val session = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        validate(HttpMethod.Post, "/api/v1/development-session", session)
        val cookie = session.headers[HttpHeaders.SetCookie]!!.substringAfter("astrid_session=").substringBefore(';')
        val context = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        validate(HttpMethod.Get, "/api/v1/practice-context", context)
        val owner = wireJson.parseToJsonElement(context.bodyAsText()).jsonObject
            .getValue("ownerSessionId").jsonPrimitive.content
        val profiles = listOf(
            Triple("module-one-singular", "module-one-singular-1", "human-singular"),
            Triple("module-one-groups", "module-one-groups-1", "human-group"),
            Triple("module-one-er", "module-one-er-1", "vaere-present"),
            Triple("module-one-present", "module-one-present-1", "named-three-verbs-present"),
            Triple("module-one-possession", "module-one-possession-1", "have-possession-present"),
            Triple("module-one-personal", "module-one-personal-1", "personal-introduction-present"),
            Triple("module-one-ability", "module-one-ability-1", "kan-bare-infinitive"),
        )
        val observedFormats = mutableSetOf<String>()

        profiles.forEachIndexed { profileIndex, (focusId, planRevision, expectedScope) ->
            val practiceId = UUID.randomUUID()
            val created = client.post("/api/v1/practice-sessions") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                header("X-Astrid-Practice-Owner", owner)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"practiceSessionId":"$practiceId","focusId":"$focusId","planRevision":"$planRevision","seed":${7 + profileIndex}}""",
                )
            }
            validate(HttpMethod.Post, "/api/v1/practice-sessions", created)
            var state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
            assertTrue(state.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertFalse(state.getValue("introductionOpened").jsonPrimitive.content.toBoolean())
            assertFalse(
                state.getValue("items").jsonArray.first().jsonObject
                    .getValue("canAdvance").jsonPrimitive.content.toBoolean(),
            )
            assertTrue(state.getValue("items").jsonArray.all {
                it.jsonObject.getValue("targetScopeId").jsonPrimitive.content == expectedScope
            })

            val initialExerciseId = state.getValue("currentExercise").jsonObject
                .getValue("id").jsonPrimitive.content
            val initialInspector = client.get("/api/v1/dev/exercises/$initialExerciseId") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Inspector-Key", inspectorKey)
            }
            validate(HttpMethod.Get, "/api/v1/dev/exercises/$initialExerciseId", initialInspector)
            val initialExpected = wireJson.parseToJsonElement(initialInspector.bodyAsText()).jsonObject
                .getValue("moduleOneSource").jsonObject.getValue("expectedForm").jsonPrimitive.content
            val earlyAttempt = submitModuleOneAttempt(cookie, initialExerciseId, initialExpected)
            validate(HttpMethod.Post, "/api/v1/attempts", earlyAttempt)
            assertEquals(409, earlyAttempt.status.value)

            val support = client.post("/api/v1/practice-sessions/$practiceId/support") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                header("X-Astrid-Practice-Owner", owner)
                contentType(ContentType.Application.Json)
                setBody("""{"kind":"introduction"}""")
            }
            validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/support", support)
            state = wireJson.parseToJsonElement(support.bodyAsText()).jsonObject
            assertTrue(state.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertTrue(state.getValue("introductionOpened").jsonPrimitive.content.toBoolean())

            var checkedCaseOutcome = false
            var checkedUnknownOutcome = false
            repeat(state.getValue("itemCount").jsonPrimitive.content.toInt()) { position ->
                val exercise = state.getValue("currentExercise").jsonObject
                val exerciseId = exercise.getValue("id").jsonPrimitive.content
                val formatId = exercise.getValue("formatId").jsonPrimitive.content
                observedFormats += formatId
                assertEquals(expectedScope, exercise.getValue("targetScopeId").jsonPrimitive.content)
                val inspector = client.get("/api/v1/dev/exercises/$exerciseId") {
                    trustedHost()
                    cookie("astrid_session", cookie)
                    header("X-Astrid-Inspector-Key", inspectorKey)
                }
                validate(HttpMethod.Get, "/api/v1/dev/exercises/$exerciseId", inspector)
                val source = wireJson.parseToJsonElement(inspector.bodyAsText()).jsonObject
                    .getValue("moduleOneSource").jsonObject
                val expected = source.getValue("expectedForm").jsonPrimitive.content
                val isRecall = formatId in setOf("E017", "E019", "E023", "E027")
                val answer = when {
                    isRecall && !checkedCaseOutcome -> expected.replaceFirstChar {
                        if (it.isUpperCase()) it.lowercase() else it.uppercase()
                    }
                    isRecall && !checkedUnknownOutcome -> ""
                    else -> expected
                }
                val attempt = submitModuleOneAttempt(cookie, exerciseId, answer)
                validate(HttpMethod.Post, "/api/v1/attempts", attempt)
                assertEquals(200, attempt.status.value)
                val outcome = wireJson.parseToJsonElement(attempt.bodyAsText()).jsonObject
                when {
                    isRecall && !checkedCaseOutcome -> {
                        checkedCaseOutcome = true
                        assertEquals("known-issue", outcome.getValue("linguisticStatus").jsonPrimitive.content)
                        assertTrue(outcome.getValue("issues").jsonArray.any {
                            it.jsonObject.getValue("code").jsonPrimitive.content.endsWith("CASE_REQUIRED")
                        })
                    }
                    isRecall && !checkedUnknownOutcome -> {
                        checkedUnknownOutcome = true
                        assertEquals("unknown", outcome.getValue("linguisticStatus").jsonPrimitive.content)
                        val saved = client.get("/api/v1/practice-sessions/$practiceId") {
                            trustedHost()
                            cookie("astrid_session", cookie)
                        }
                        validate(HttpMethod.Get, "/api/v1/practice-sessions/$practiceId", saved)
                        assertFalse(
                            wireJson.parseToJsonElement(saved.bodyAsText()).jsonObject
                                .getValue("items").jsonArray[position].jsonObject
                                .getValue("canAdvance").jsonPrimitive.content.toBoolean(),
                        )
                        val blocked = advanceModuleOne(cookie, owner, practiceId, position)
                        validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", blocked)
                        assertEquals(409, blocked.status.value)
                        val corrected = submitModuleOneAttempt(cookie, exerciseId, expected)
                        validate(HttpMethod.Post, "/api/v1/attempts", corrected)
                        assertEquals(
                            "met",
                            wireJson.parseToJsonElement(corrected.bodyAsText()).jsonObject
                                .getValue("targetSatisfaction").jsonPrimitive.content,
                        )
                    }
                    else -> assertEquals("met", outcome.getValue("targetSatisfaction").jsonPrimitive.content)
                }
                val advanced = advanceModuleOne(cookie, owner, practiceId, position)
                validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
                state = wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
            }
            assertTrue(checkedCaseOutcome)
            assertTrue(checkedUnknownOutcome)
            assertEquals("completed", state.getValue("state").jsonPrimitive.content)
            val summaryRows = state.getValue("summary").jsonObject.getValue("rows").jsonArray
            assertTrue(summaryRows.isNotEmpty())
            assertTrue(summaryRows.all {
                it.jsonObject.getValue("targetScopeId").jsonPrimitive.content == expectedScope
            })
        }
        assertEquals(setOf("E016", "E017", "E018", "E019", "E022", "E023", "E026", "E027"), observedFormats)

        val reviewContext = client.get("/api/v1/practice-review-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        validate(HttpMethod.Get, "/api/v1/practice-review-context", reviewContext)
        val reviewContextBody = reviewContext.bodyAsText().asObject()
        assertEquals(8, reviewContextBody.getValue("itemCount").jsonPrimitive.content.toInt())

        val cumulativeId = UUID.randomUUID()
        val cumulative = client.post("/api/v1/practice-sessions") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", owner)
            contentType(ContentType.Application.Json)
            setBody(
                """{"practiceSessionId":"$cumulativeId","focusId":"cumulative-review","planRevision":"cumulative-review-1","seed":31}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/practice-sessions", cumulative)
        assertEquals(201, cumulative.status.value, cumulative.bodyAsText())
        val cumulativeBody = cumulative.bodyAsText().asObject()
        assertEquals("per-item", cumulativeBody.getValue("contentRevision").jsonPrimitive.content)
        assertTrue(cumulativeBody.getValue("items").jsonArray.all { "revisions" in it.jsonObject })
        validate(
            HttpMethod.Get,
            "/api/v1/practice-sessions/$cumulativeId",
            client.get("/api/v1/practice-sessions/$cumulativeId") {
                trustedHost()
                cookie("astrid_session", cookie)
            },
        )
    }

    @Test
    fun `all vocabulary P1 practice journeys complete and conform to the contract`() = testApplication {
        val inspectorKey = "v".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val session = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        validate(HttpMethod.Post, "/api/v1/development-session", session)
        val cookie = session.headers[HttpHeaders.SetCookie]!!.substringAfter("astrid_session=").substringBefore(';')
        val context = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        validate(HttpMethod.Get, "/api/v1/practice-context", context)
        val owner = context.bodyAsText().asObject().getValue("ownerSessionId").jsonPrimitive.content

        (1..5).forEach { sessionIndex ->
            val practiceId = UUID.randomUUID()
            val focusId = "vocabulary-p1-s$sessionIndex"
            val created = client.post("/api/v1/practice-sessions") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                header("X-Astrid-Practice-Owner", owner)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"practiceSessionId":"$practiceId","focusId":"$focusId","planRevision":"$focusId-1","seed":11}""",
                )
            }
            validate(HttpMethod.Post, "/api/v1/practice-sessions", created)
            val support = client.post("/api/v1/practice-sessions/$practiceId/support") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                header("X-Astrid-Practice-Owner", owner)
                contentType(ContentType.Application.Json)
                setBody("""{"kind":"introduction"}""")
            }
            validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/support", support)
            var state = support.bodyAsText().asObject()

            repeat(state.getValue("itemCount").jsonPrimitive.content.toInt()) { position ->
                val exercise = state.getValue("currentExercise").jsonObject
                val exerciseId = exercise.getValue("id").jsonPrimitive.content
                val inspectorPath = "/api/v1/dev/exercises/$exerciseId"
                val inspected = client.get(inspectorPath) {
                    trustedHost()
                    cookie("astrid_session", cookie)
                    header("X-Astrid-Inspector-Key", inspectorKey)
                }
                validate(HttpMethod.Get, inspectorPath, inspected)
                val source = inspected.bodyAsText().asObject().getValue("vocabularySource").jsonObject
                val answer = when (exercise.getValue("formatId").jsonPrimitive.content) {
                    "E033" -> AttemptAnswerDto.Matching(source.getValue("matchingPairs").jsonArray.map { value ->
                        value.jsonObject.let {
                            app.astrid.server.exercises.MatchingPairDto(
                                it.getValue("leftId").jsonPrimitive.content,
                                it.getValue("rightId").jsonPrimitive.content,
                            )
                        }
                    })
                    "E037", "E038" -> AttemptAnswerDto.Order(
                        source.getValue("acceptedOrders").jsonArray.first().jsonArray.map { it.jsonPrimitive.content },
                    )
                    "E039" -> {
                        val targetFactId = source.getValue("targetSupportFactId").jsonPrimitive.content
                        val targetFact = source.getValue("supportFacts").jsonArray
                            .map { it.jsonObject }
                            .single { it.getValue("factId").jsonPrimitive.content == targetFactId }
                        AttemptAnswerDto.Text(targetFact.getValue("valueText").jsonPrimitive.content)
                    }
                    else -> AttemptAnswerDto.Text(source.getValue("expectedAnswer").jsonPrimitive.content)
                }
                val attempted = submitAttempt(cookie, exerciseId, answer)
                validate(HttpMethod.Post, "/api/v1/attempts", attempted)
                assertEquals(200, attempted.status.value, attempted.bodyAsText())
                val advanced = advanceModuleOne(cookie, owner, practiceId, position)
                validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
                state = advanced.bodyAsText().asObject()
            }
            assertEquals("completed", state.getValue("state").jsonPrimitive.content)
            assertTrue(state.getValue("summary").jsonObject.getValue("rows").jsonArray.isNotEmpty())
        }
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

    @Test
    fun `ability supplied choices and typed markers have distinct response constraints`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val cookie = moduleOneCookie()
        val selection = createCurrentExercise(cookie, "E026", "G030")
        val selectionId = selection.getValue("id").jsonPrimitive.content
        for (answer in listOf(AttemptAnswerDto.Text("kan"), AttemptAnswerDto.Order(emptyList()))) {
            val rejected = submitAttempt(cookie, selectionId, answer)
            validate(HttpMethod.Post, "/api/v1/attempts", rejected)
            assertEquals(400, rejected.status.value)
        }
        val selectedMarker = submitAttempt(cookie, selectionId, AttemptAnswerDto.Text("at læse"))
        validate(HttpMethod.Post, "/api/v1/attempts", selectedMarker)
        val selected = selectedMarker.bodyAsText().asObject()
        assertEquals(200, selectedMarker.status.value)
        assertEquals("not-met", selected.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("known-issue", selected.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("met", selected.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertEquals(1, selected.getValue("retryNumber").jsonPrimitive.content.toInt())

        val recall = createCurrentExercise(cookie, "E027", "G030")
        val recallId = recall.getValue("id").jsonPrimitive.content
        val typedMarker = submitAttempt(cookie, recallId, AttemptAnswerDto.Text("at læse"))
        validate(HttpMethod.Post, "/api/v1/attempts", typedMarker)
        val typed = typedMarker.bodyAsText().asObject()
        assertEquals("not-met", typed.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("known-issue", typed.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("not-met", typed.getValue("responseConstraintStatus").jsonPrimitive.content)
        val unknown = submitAttempt(cookie, recallId, AttemptAnswerDto.Text("læse."))
        validate(HttpMethod.Post, "/api/v1/attempts", unknown)
        val outside = unknown.bodyAsText().asObject()
        assertEquals("unknown", outside.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("", outside.getValue("completedSentence").jsonPrimitive.content)
    }

    @Test
    fun `modal rounds keep meaning and supplied order distinct through the runtime contract`() = testApplication {
        val inspectorKey = "m".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val cookie = moduleOneCookie()
        val context = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }
        validate(HttpMethod.Get, "/api/v1/practice-context", context)
        val owner = context.bodyAsText().asObject().getValue("ownerSessionId").jsonPrimitive.content
        for ((key, lexicalId) in listOf("volition" to "L034", "obligation" to "L035", "permission" to "L036")) {
            completeModalRound(cookie, owner, inspectorKey, key, lexicalId)
        }
    }

    @Suppress("CyclomaticComplexMethod") // The conformance matrix intentionally checks all nine closed response shapes.
    @Test
    fun `pair seventeen vocabulary exercises expose only their correlated public targets`() = testApplication {
        val inspectorKey = "k".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val session = client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        val cookie = session.headers[HttpHeaders.SetCookie]!!
            .substringAfter("astrid_session=").substringBefore(';')
        val cases = listOf(
            "E031" to null,
            "E032" to null,
            "E033" to null,
            "E034" to "G004",
            "E035" to null,
            "E036" to null,
            "E037" to "G005",
            "E038" to "G004",
            "E039" to null,
        )

        cases.forEach { (formatId, targetSkillId) ->
            val target = targetSkillId?.let { ",\"targetSkillId\":\"$it\"" }.orEmpty()
            val response = client.post("/api/v1/exercises") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(
                    """{"formatId":"$formatId"$target,"seed":11,"index":0,"contentRevision":"pilot-17","generatorRevision":"generator-17"}""",
                )
            }
            assertEquals(201, response.status.value, response.bodyAsText())
            validate(HttpMethod.Post, "/api/v1/exercises", response)
            val exercise = wireJson.parseToJsonElement(response.bodyAsText()).jsonObject
            assertFalse("vocabularySource" in exercise)
            assertFalse("expectedAnswer" in exercise)
            when (formatId) {
                "E031", "E032", "E035", "E036" -> {
                    assertTrue("lexicalTarget" in exercise)
                    val lexicalTarget = exercise.getValue("lexicalTarget").jsonObject
                    val expectedFacet = when (formatId) {
                        "E031", "E032" -> "meaning"
                        "E035" -> "fixed-chunk"
                        else -> "mass-headword"
                    }
                    assertEquals(expectedFacet, lexicalTarget.getValue("facet").jsonPrimitive.content)
                    assertEquals(
                        if (formatId == "E031") "da-en" else "en-da",
                        lexicalTarget.getValue("direction").jsonPrimitive.content,
                    )
                    assertFalse("lexicalTargets" in exercise)
                    assertFalse("readingTarget" in exercise)
                    assertFalse("targetSkillId" in exercise)
                }
                "E033" -> {
                    assertTrue("lexicalTargets" in exercise)
                    assertTrue(exercise.getValue("lexicalTargets").jsonArray.all {
                        val target = it.jsonObject
                        target.getValue("facet").jsonPrimitive.content == "meaning" &&
                            target.getValue("direction").jsonPrimitive.content == "en-da"
                    })
                    val columns = exercise.getValue("prompt").jsonObject.getValue("matchingColumns").jsonObject
                    assertTrue(columns.getValue("left").jsonArray.all {
                        it.jsonObject.getValue("id").jsonPrimitive.content.matches(Regex("m-[0-9a-z]+"))
                    })
                    assertTrue(columns.getValue("right").jsonArray.all {
                        it.jsonObject.getValue("id").jsonPrimitive.content.matches(Regex("n-[0-9a-z]+"))
                    })
                    assertFalse("lexicalTarget" in exercise)
                    assertFalse("readingTarget" in exercise)
                }
                "E034" -> {
                    assertEquals("G004", exercise.getValue("targetSkillId").jsonPrimitive.content)
                    assertTrue(
                        exercise.getValue("targetScopeId").jsonPrimitive.content in
                            setOf("p1-studere-present", "p1-drikke-present"),
                    )
                    assertFalse("lexicalTarget" in exercise)
                    assertFalse("readingTarget" in exercise)
                }
                "E037" -> {
                    assertEquals("G005", exercise.getValue("targetSkillId").jsonPrimitive.content)
                    assertEquals("p1-subject-first-supplied", exercise.getValue("targetScopeId").jsonPrimitive.content)
                    assertFalse("lexicalTarget" in exercise)
                    assertFalse("readingTarget" in exercise)
                }
                "E038" -> {
                    assertEquals("G004", exercise.getValue("targetSkillId").jsonPrimitive.content)
                    assertEquals("p1-drikke-present", exercise.getValue("targetScopeId").jsonPrimitive.content)
                    assertFalse("lexicalTarget" in exercise)
                    assertFalse("readingTarget" in exercise)
                }
                "E039" -> {
                    assertEquals("explicit-fact", exercise.getValue("readingTarget").jsonObject
                        .getValue("facet").jsonPrimitive.content)
                    assertFalse("lexicalTarget" in exercise)
                    assertFalse("lexicalTargets" in exercise)
                    assertFalse("targetSkillId" in exercise)
                }
            }
            val inspectorPath = "/api/v1/dev/exercises/${exercise.getValue("id").jsonPrimitive.content}"
            val inspector = client.get(inspectorPath) {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Inspector-Key", inspectorKey)
            }
            validate(HttpMethod.Get, inspectorPath, inspector)
            val inspectorBody = wireJson.parseToJsonElement(inspector.bodyAsText()).jsonObject
            assertTrue("vocabularySource" in inspectorBody)
            if (formatId == "E033") {
                val pairs = inspectorBody.getValue("vocabularySource").jsonObject
                    .getValue("matchingPairs").jsonArray.map { pair ->
                        pair.jsonObject.let {
                            app.astrid.server.exercises.MatchingPairDto(
                                it.getValue("leftId").jsonPrimitive.content,
                                it.getValue("rightId").jsonPrimitive.content,
                            )
                        }
                    }
                val attempt = submitAttempt(
                    cookie,
                    exercise.getValue("id").jsonPrimitive.content,
                    AttemptAnswerDto.Matching(pairs),
                )
                assertEquals(200, attempt.status.value, attempt.bodyAsText())
                validate(HttpMethod.Post, "/api/v1/attempts", attempt)
            }
        }

        listOf(
            """{"formatId":"E034","seed":11,"index":0,"contentRevision":"pilot-17","generatorRevision":"generator-17"}""",
            """{"formatId":"E037","targetSkillId":"G004","seed":11,"index":0,"contentRevision":"pilot-17","generatorRevision":"generator-17"}""",
            """{"formatId":"E039","targetSkillId":"G004","seed":11,"index":0,"contentRevision":"pilot-17","generatorRevision":"generator-17"}""",
        ).forEach { body ->
            val response = client.post("/api/v1/exercises") {
                trustedHost()
                cookie("astrid_session", cookie)
                header("X-Astrid-Client", "playground")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(400, response.status.value, response.bodyAsText())
            validate(HttpMethod.Post, "/api/v1/exercises", response)
        }
    }

    private suspend fun ApplicationTestBuilder.completeModalRound(
        cookie: String,
        owner: String,
        inspectorKey: String,
        key: String,
        lexicalId: String,
    ) {
        val id = UUID.randomUUID()
        val creation = moduleOnePost(
            cookie, owner, "/api/v1/practice-sessions",
            """{"practiceSessionId":"$id","focusId":"module-one-$key","planRevision":"module-one-$key-1","seed":47}""",
        )
        validate(HttpMethod.Post, "/api/v1/practice-sessions", creation)
        var state = creation.bodyAsText().asObject()
        assertEquals("pilot-14", state.getValue("contentRevision").jsonPrimitive.content)
        assertEquals(2, state.getValue("itemCount").jsonPrimitive.content.toInt())
        val early = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Text("An external requirement."))
        validate(HttpMethod.Post, "/api/v1/attempts", early)
        assertEquals(409, early.status.value)
        val support = moduleOnePost(
            cookie, owner, "/api/v1/practice-sessions/$id/support", """{"kind":"introduction"}""",
        )
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$id/support", support)
        state = support.bodyAsText().asObject()
        val exercise = state.getValue("currentExercise").jsonObject
        assertEquals("E028", exercise.getValue("formatId").jsonPrimitive.content)
        assertEquals("modal-$key-meaning", exercise.getValue("targetScopeId").jsonPrimitive.content)
        assertFalse("modalSource" in exercise)
        val source = inspectExercise(cookie, inspectorKey, currentExerciseId(state))
            .getValue("modalSource").jsonObject
        assertEquals(lexicalId, source.getValue("modalEntryId").jsonPrimitive.content)
        assertEquals("$lexicalId-S01", source.getValue("modalSenseId").jsonPrimitive.content)
        val expected = source.getValue("expectedInterpretation").jsonPrimitive.content
        val forged = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Text("I guess."))
        validate(HttpMethod.Post, "/api/v1/attempts", forged)
        assertEquals(400, forged.status.value)
        val wrong = exercise.getValue("prompt").jsonObject.getValue("choices").jsonArray
            .map { it.jsonPrimitive.content }.first { it != expected }
        val knownWrong = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Text(wrong))
        validate(HttpMethod.Post, "/api/v1/attempts", knownWrong)
        assertEquals("not-met", knownWrong.bodyAsText().asObject().getValue("meaningSatisfaction").jsonPrimitive.content)
        val corrected = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Text(expected))
        validate(HttpMethod.Post, "/api/v1/attempts", corrected)
        val advanced = advanceModuleOne(cookie, owner, id, 0)
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$id/advance", advanced)
        completeModalConstruction(cookie, owner, inspectorKey, id, advanced.bodyAsText().asObject())
    }

    private suspend fun ApplicationTestBuilder.completeModalConstruction(
        cookie: String,
        owner: String,
        inspectorKey: String,
        id: UUID,
        state: JsonObject,
    ) {
        val exercise = state.getValue("currentExercise").jsonObject
        assertEquals("E025", exercise.getValue("formatId").jsonPrimitive.content)
        assertEquals("G005", exercise.getValue("targetSkillId").jsonPrimitive.content)
        assertEquals("supplied-modal-order", exercise.getValue("targetScopeId").jsonPrimitive.content)
        val source = inspectExercise(cookie, inspectorKey, currentExerciseId(state))
        val order = source.getValue("tileSource").jsonObject.getValue("licensedOrders").jsonArray.single()
            .jsonObject.getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }
        val partial = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Order(order.take(3)))
        validate(HttpMethod.Post, "/api/v1/attempts", partial)
        val saved = partial.bodyAsText().asObject()
        assertEquals("unevaluable", saved.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("unknown", saved.getValue("linguisticStatus").jsonPrimitive.content)
        val finish = advanceModuleOne(cookie, owner, id, 1)
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$id/advance", finish)
        val completed = finish.bodyAsText().asObject()
        assertEquals("completed", completed.getValue("state").jsonPrimitive.content)
        val rows = completed.getValue("summary").jsonObject.getValue("rows").jsonArray.map { it.jsonObject }
        assertEquals(setOf("G030", "G005"), rows.map { it.getValue("targetSkillId").jsonPrimitive.content }.toSet())
        assertEquals(setOf("selection", "word-order"), rows.map { it.getValue("assessmentMode").jsonPrimitive.content }.toSet())
        val summary = completed.getValue("summary").jsonObject
        for ((skill, wrongMode) in listOf("G030" to "recall", "G005" to "constituent-order")) {
            val changedRows = kotlinx.serialization.json.JsonArray(rows.map { row ->
                if (row.getValue("targetSkillId").jsonPrimitive.content == skill) {
                    JsonObject(row + ("assessmentMode" to kotlinx.serialization.json.JsonPrimitive(wrongMode)))
                } else {
                    row
                }
            })
            val changedSummary = JsonObject(summary + ("rows" to changedRows))
            assertPracticeResponseValidity(
                JsonObject(completed + ("summary" to changedSummary)).toString(),
                expected = false,
            )
        }
    }

    @Test
    fun `final circumstance practice requires teaching validates the complete bank and keeps target separate from core order`() =
        testApplication {
            val inspectorKey = "c".repeat(32)
            application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
            val cookie = moduleOneCookie()
            val context = client.get("/api/v1/practice-context") {
                trustedHost()
                cookie("astrid_session", cookie)
            }
            validate(HttpMethod.Get, "/api/v1/practice-context", context)
            val owner = context.bodyAsText().asObject().getValue("ownerSessionId").jsonPrimitive.content
            val practiceId = UUID.randomUUID()
            val creation = moduleOnePost(
                cookie,
                owner,
                "/api/v1/practice-sessions",
                """{"practiceSessionId":"$practiceId","focusId":"module-one-circumstances","planRevision":"module-one-circumstances-1","seed":47}""",
            )
            validate(HttpMethod.Post, "/api/v1/practice-sessions", creation)
            var state = creation.bodyAsText().asObject()
            assertEquals("pilot-15", state.getValue("contentRevision").jsonPrimitive.content)
            assertEquals(4, state.getValue("itemCount").jsonPrimitive.content.toInt())

            val early = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Order(emptyList()))
            validate(HttpMethod.Post, "/api/v1/attempts", early)
            assertEquals(409, early.status.value)
            val support = moduleOnePost(
                cookie,
                owner,
                "/api/v1/practice-sessions/$practiceId/support",
                """{"kind":"introduction"}""",
            )
            validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/support", support)
            state = support.bodyAsText().asObject()

            repeat(4) { position ->
                val exercise = state.getValue("currentExercise").jsonObject
                val exerciseId = currentExerciseId(state)
                assertEquals("E029", exercise.getValue("formatId").jsonPrimitive.content)
                assertEquals("G016", exercise.getValue("targetSkillId").jsonPrimitive.content)
                assertEquals("one-final-circumstance", exercise.getValue("targetScopeId").jsonPrimitive.content)
                val prompt = exercise.getValue("prompt").jsonObject
                assertFalse("tileMode" in prompt)
                assertFalse("requiredTileCount" in prompt)
                val inspector = inspectExercise(cookie, inspectorKey, exerciseId)
                assertEquals("C007", inspector.getValue("constructionId").jsonPrimitive.content)
                val source = inspector.getValue("tileSource").jsonObject
                val licensed = source.getValue("licensedOrders").jsonArray.single().jsonObject
                assertTrue(
                    licensed.getValue("kind").jsonPrimitive.content in
                        setOf("s12-work-final-time", "s12-read-final-time", "s12-work-final-place"),
                )
                val order = licensed.getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }
                if (position == 0) {
                    val malformed = listOf(
                        AttemptAnswerDto.Order(emptyList()),
                        AttemptAnswerDto.Order(order.dropLast(1)),
                        AttemptAnswerDto.Order(order.dropLast(1) + order.first()),
                        AttemptAnswerDto.Order(order.dropLast(1) + "foreign"),
                        AttemptAnswerDto.Text("Jeg arbejder i dag."),
                    )
                    malformed.forEach { answer ->
                        val rejected = submitAttempt(cookie, exerciseId, answer)
                        validate(HttpMethod.Post, "/api/v1/attempts", rejected)
                        assertEquals(400, rejected.status.value)
                        val unchanged = client.get("/api/v1/practice-sessions/$practiceId") {
                            trustedHost()
                            cookie("astrid_session", cookie)
                        }
                        validate(HttpMethod.Get, "/api/v1/practice-sessions/$practiceId", unchanged)
                        assertEquals(
                            0,
                            unchanged.bodyAsText().asObject().getValue("items").jsonArray.first().jsonObject
                                .getValue("attemptCount").jsonPrimitive.content.toInt(),
                        )
                    }
                    val wrongCore = order.toMutableList().also { java.util.Collections.swap(it, 0, 1) }
                    val constrained = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Order(wrongCore))
                    validate(HttpMethod.Post, "/api/v1/attempts", constrained)
                    val outcome = constrained.bodyAsText().asObject()
                    assertEquals("met", outcome.getValue("targetSatisfaction").jsonPrimitive.content)
                    assertEquals("not-met", outcome.getValue("responseConstraintStatus").jsonPrimitive.content)
                    assertEquals("unknown", outcome.getValue("linguisticStatus").jsonPrimitive.content)
                }
                val canonical = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Order(order))
                validate(HttpMethod.Post, "/api/v1/attempts", canonical)
                val outcome = canonical.bodyAsText().asObject()
                assertEquals("met", outcome.getValue("targetSatisfaction").jsonPrimitive.content)
                assertEquals("met", outcome.getValue("responseConstraintStatus").jsonPrimitive.content)
                assertEquals("acceptable", outcome.getValue("linguisticStatus").jsonPrimitive.content)
                val advanced = advanceModuleOne(cookie, owner, practiceId, position)
                validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
                state = advanced.bodyAsText().asObject()
            }
            assertEquals("completed", state.getValue("state").jsonPrimitive.content)
            val summaryRow = state.getValue("summary").jsonObject.getValue("rows").jsonArray.single().jsonObject
            assertEquals("G016", summaryRow.getValue("targetSkillId").jsonPrimitive.content)
            assertEquals("one-final-circumstance", summaryRow.getValue("targetScopeId").jsonPrimitive.content)
            assertEquals("constituent-order", summaryRow.getValue("assessmentMode").jsonPrimitive.content)
        }

    @Test
    fun `neutral question practice requires teaching validates the complete bank and preserves Danish inspector metadata`() =
        testApplication {
            val inspectorKey = "q".repeat(32)
            application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
            val cookie = moduleOneCookie()
            val context = client.get("/api/v1/practice-context") {
                trustedHost()
                cookie("astrid_session", cookie)
            }
            validate(HttpMethod.Get, "/api/v1/practice-context", context)
            val owner = context.bodyAsText().asObject().getValue("ownerSessionId").jsonPrimitive.content
            val practiceId = UUID.randomUUID()
            val creation = moduleOnePost(
                cookie,
                owner,
                "/api/v1/practice-sessions",
                """{"practiceSessionId":"$practiceId","focusId":"module-one-questions","planRevision":"module-one-questions-1","seed":47}""",
            )
            validate(HttpMethod.Post, "/api/v1/practice-sessions", creation)
            var state = creation.bodyAsText().asObject()
            assertEquals("pilot-16", state.getValue("contentRevision").jsonPrimitive.content)
            assertEquals("Asking yes or no questions", state.getValue("title").jsonPrimitive.content)
            assertEquals(4, state.getValue("itemCount").jsonPrimitive.content.toInt())
            val introduction = state.getValue("introduction").jsonArray.map { it.jsonObject }
            assertTrue(introduction.any {
                it.getValue("body").jsonPrimitive.content == "Læser han en bog?" &&
                    it.getValue("bodyLanguage").jsonPrimitive.content == "da"
            })
            assertTrue(introduction.any {
                it.getValue("body").jsonPrimitive.content == "Arbejder I?" &&
                    it.getValue("bodyLanguage").jsonPrimitive.content == "da"
            })

            val early = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Order(emptyList()))
            validate(HttpMethod.Post, "/api/v1/attempts", early)
            assertEquals(409, early.status.value)
            val support = moduleOnePost(
                cookie,
                owner,
                "/api/v1/practice-sessions/$practiceId/support",
                """{"kind":"introduction"}""",
            )
            validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/support", support)
            state = support.bodyAsText().asObject()

            val expectedCues = listOf("Du læser en avis.", "Hun arbejder.", "I bor i Aarhus.", "De har et brev.")
            val expectedKinds = listOf(
                "s13-read-count-noun-question",
                "s13-work-question",
                "s13-residence-question",
                "s13-possession-question",
            )
            repeat(4) { position ->
                val exercise = state.getValue("currentExercise").jsonObject
                val exerciseId = currentExerciseId(state)
                assertEquals("E030", exercise.getValue("formatId").jsonPrimitive.content)
                assertEquals("G018", exercise.getValue("targetSkillId").jsonPrimitive.content)
                assertEquals("neutral-positive-yes-no", exercise.getValue("targetScopeId").jsonPrimitive.content)
                val prompt = exercise.getValue("prompt").jsonObject
                assertEquals(expectedCues[position], prompt.getValue("cue").jsonPrimitive.content)
                assertEquals("da", prompt.getValue("cueLanguage").jsonPrimitive.content)
                assertEquals("da", prompt.getValue("answerLanguage").jsonPrimitive.content)
                assertEquals("?", prompt.getValue("punctuation").jsonPrimitive.content)
                assertFalse("tileMode" in prompt)
                assertFalse("requiredTileCount" in prompt)

                val inspector = inspectExercise(cookie, inspectorKey, exerciseId)
                assertEquals("C010", inspector.getValue("constructionId").jsonPrimitive.content)
                val inspectorPrompt = inspector.getValue("exercise").jsonObject.getValue("prompt").jsonObject
                assertEquals(expectedCues[position], inspectorPrompt.getValue("cue").jsonPrimitive.content)
                assertEquals("da", inspectorPrompt.getValue("cueLanguage").jsonPrimitive.content)
                assertEquals("da", inspectorPrompt.getValue("answerLanguage").jsonPrimitive.content)
                val source = inspector.getValue("tileSource").jsonObject
                val tokens = source.getValue("tokens").jsonArray.map { it.jsonObject }
                assertTrue(tokens.all {
                    it.getValue("form").jsonPrimitive.content in
                        setOf("finite-verb", "subject", "object", "location-complement")
                })
                val licensed = source.getValue("licensedOrders").jsonArray.single().jsonObject
                assertEquals(expectedKinds[position], licensed.getValue("kind").jsonPrimitive.content)
                val order = licensed.getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }
                if (position == 2) {
                    assertTrue(prompt.getValue("blocks").jsonArray.any { it.jsonObject.getValue("text").jsonPrimitive.content == "I" })
                    assertTrue(prompt.getValue("blocks").jsonArray.any { it.jsonObject.getValue("text").jsonPrimitive.content == "i Aarhus" })
                    assertTrue(inspector.getValue("canonicalSentence").jsonPrimitive.content.contains(" I "))
                }
                if (position == 0) {
                    val malformed = listOf(
                        AttemptAnswerDto.Order(emptyList()),
                        AttemptAnswerDto.Order(order.dropLast(1)),
                        AttemptAnswerDto.Order(order.dropLast(1) + order.first()),
                        AttemptAnswerDto.Order(order.dropLast(1) + "foreign"),
                        AttemptAnswerDto.Text("Læser du en avis?"),
                        AttemptAnswerDto.Matching(emptyList()),
                        AttemptAnswerDto.Gaps(emptyList()),
                    )
                    malformed.forEach { answer ->
                        val rejected = submitAttempt(cookie, exerciseId, answer)
                        validate(HttpMethod.Post, "/api/v1/attempts", rejected)
                        assertEquals(400, rejected.status.value)
                        val unchanged = client.get("/api/v1/practice-sessions/$practiceId") {
                            trustedHost()
                            cookie("astrid_session", cookie)
                        }
                        validate(HttpMethod.Get, "/api/v1/practice-sessions/$practiceId", unchanged)
                        assertEquals(
                            0,
                            unchanged.bodyAsText().asObject().getValue("items").jsonArray.first().jsonObject
                                .getValue("attemptCount").jsonPrimitive.content.toInt(),
                        )
                    }
                    val wrongTarget = order.toMutableList().also { java.util.Collections.swap(it, 0, 1) }
                    val constrained = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Order(wrongTarget))
                    validate(HttpMethod.Post, "/api/v1/attempts", constrained)
                    val outcome = constrained.bodyAsText().asObject()
                    assertEquals("not-met", outcome.getValue("targetSatisfaction").jsonPrimitive.content)
                    assertEquals("not-met", outcome.getValue("responseConstraintStatus").jsonPrimitive.content)
                    assertEquals("unknown", outcome.getValue("linguisticStatus").jsonPrimitive.content)
                    assertEquals("unknown", outcome.getValue("meaningSatisfaction").jsonPrimitive.content)
                }
                val canonical = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Order(order))
                validate(HttpMethod.Post, "/api/v1/attempts", canonical)
                val outcome = canonical.bodyAsText().asObject()
                assertEquals("met", outcome.getValue("targetSatisfaction").jsonPrimitive.content)
                assertEquals("met", outcome.getValue("responseConstraintStatus").jsonPrimitive.content)
                assertEquals("acceptable", outcome.getValue("linguisticStatus").jsonPrimitive.content)
                val advanced = advanceModuleOne(cookie, owner, practiceId, position)
                validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
                state = advanced.bodyAsText().asObject()
            }
            assertEquals("completed", state.getValue("state").jsonPrimitive.content)
            val summaryRow = state.getValue("summary").jsonObject.getValue("rows").jsonArray.single().jsonObject
            assertEquals("Neutral positive yes/no question — constituent order", summaryRow.getValue("label").jsonPrimitive.content)
            assertEquals("G018", summaryRow.getValue("targetSkillId").jsonPrimitive.content)
            assertEquals("neutral-positive-yes-no", summaryRow.getValue("targetScopeId").jsonPrimitive.content)
            assertEquals("constituent-order", summaryRow.getValue("assessmentMode").jsonPrimitive.content)
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
        val report = validator.validateResponse(path, request.method, simpleResponse)
        if (report.hasErrors()) {
            throw AssertionError(SimpleValidationReportFormat.getInstance().apply(report) + "\n$body")
        }
    }

    private suspend fun ApplicationTestBuilder.validateAllGapOutcomeResponses(
        cookie: String,
        exerciseId: String,
        exercise: JsonObject,
        inspector: JsonObject,
    ) {
        val correctBySlot = inspector.getValue("gapSource").jsonObject.getValue("slots").jsonArray.associate { value ->
            value.jsonObject.let { it.getValue("slotId").jsonPrimitive.content to it.getValue("expectedChoiceId").jsonPrimitive.content }
        }
        val choicesBySlot = exercise.getValue("prompt").jsonObject.getValue("gapPrompt").jsonObject
            .getValue("slots").jsonArray.associate { value ->
                val slot = value.jsonObject
                slot.getValue("id").jsonPrimitive.content to
                    slot.getValue("choices").jsonArray.map { it.jsonObject.getValue("id").jsonPrimitive.content }
            }
        val states = listOf(null, true, false)
        states.forEach { primaryCorrect ->
            states.forEach { supportCorrect ->
                val selections = listOf("g0" to primaryCorrect, "g1" to supportCorrect).mapNotNull { (slotId, correct) ->
                    correct?.let {
                        val expected = correctBySlot.getValue(slotId)
                        val choiceId = if (it) expected else choicesBySlot.getValue(slotId).single { choice -> choice != expected }
                        app.astrid.server.exercises.GapSelectionDto(slotId, choiceId)
                    }
                }
                val response = client.post("/api/v1/attempts") {
                    trustedHost()
                    cookie("astrid_session", cookie)
                    header("X-Astrid-Client", "playground")
                    contentType(ContentType.Application.Json)
                    setBody(
                        wireJson.encodeToString(
                            AttemptRequest(
                                UUID.randomUUID().toString(),
                                exerciseId,
                                AttemptAnswerDto.Gaps(selections),
                                false,
                            ),
                        ),
                    )
                }
                validate(HttpMethod.Post, "/api/v1/attempts", response)
                val body = wireJson.parseToJsonElement(response.bodyAsText()).jsonObject
                assertGapOutcome(body, primaryCorrect, supportCorrect)
            }
        }
    }

    @Test
    fun `pair ten rejects forged forms and invalid orders before saving attempts`() = testApplication {
        val inspectorKey = "m".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val cookie = moduleOneCookie()
        val selection = createCurrentExercise(cookie, "E022", "G004", pair = 10)
        val selectionId = selection.getValue("id").jsonPrimitive.content
        val choices = selection.getValue("prompt").jsonObject.getValue("choices").jsonArray
            .map { it.jsonPrimitive.content }
        val forged = listOf("arbejder", "læser", "sover").first { it !in choices }
        val badChoice = submitAttempt(cookie, selectionId, AttemptAnswerDto.Text(forged))
        assertEquals(400, badChoice.status.value)
        validate(HttpMethod.Post, "/api/v1/attempts", badChoice)
        val correctForm = inspectExercise(cookie, inspectorKey, selectionId)
            .getValue("moduleOneSource").jsonObject.getValue("expectedForm").jsonPrimitive.content
        val checkedForm = submitAttempt(cookie, selectionId, AttemptAnswerDto.Text(correctForm))
        assertEquals(200, checkedForm.status.value)
        validate(HttpMethod.Post, "/api/v1/attempts", checkedForm)

        for (format in listOf("E024", "E025")) {
            val exercise = createCurrentExercise(cookie, format, "G005", pair = 10)
            val exerciseId = exercise.getValue("id").jsonPrimitive.content
            val order = canonicalPairTenOrder(cookie, inspectorKey, exerciseId)
            val malformed = listOf(
                AttemptAnswerDto.Text("Jeg læser."),
                AttemptAnswerDto.Order(emptyList()),
                AttemptAnswerDto.Order(listOf("foreign")),
                AttemptAnswerDto.Order(listOf(order.first(), order.first())),
            ) + if (format == "E024") listOf(AttemptAnswerDto.Order(order.dropLast(1))) else emptyList()
            for (answer in malformed) {
                val rejected = submitAttempt(cookie, exerciseId, answer)
                assertEquals(400, rejected.status.value, "$format $answer")
                validate(HttpMethod.Post, "/api/v1/attempts", rejected)
            }
            val accepted = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Order(order))
            assertEquals(200, accepted.status.value)
            validate(HttpMethod.Post, "/api/v1/attempts", accepted)
            assertEquals(1, accepted.bodyAsText().asObject().getValue("retryNumber").jsonPrimitive.content.toInt())
        }
    }

    @Test
    fun `pair ten guided order saves a partial response and replays its original outcome`() = testApplication {
        val inspectorKey = "m".repeat(32)
        application { astridModule(config(inspectorKey = inspectorKey), InMemoryStores()) }
        val cookie = moduleOneCookie()
        val owner = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }.bodyAsText().asObject().getValue("ownerSessionId").jsonPrimitive.content
        val practiceId = UUID.randomUUID()
        val created = moduleOnePost(cookie, owner, "/api/v1/practice-sessions",
            """{"practiceSessionId":"$practiceId","focusId":"module-one-order","planRevision":"module-one-order-1","seed":47}""",
        )
        assertEquals(201, created.status.value)
        validate(HttpMethod.Post, "/api/v1/practice-sessions", created)
        var state = created.bodyAsText().asObject()
        val firstExerciseId = currentExerciseId(state)
        val firstOrder = canonicalPairTenOrder(cookie, inspectorKey, firstExerciseId)
        assertEquals(409, submitAttempt(cookie, firstExerciseId, AttemptAnswerDto.Order(firstOrder)).status.value)

        val supportPath = "/api/v1/practice-sessions/$practiceId/support"
        val supported = moduleOnePost(cookie, owner, supportPath, """{"kind":"introduction"}""")
        assertEquals(200, supported.status.value)
        validate(HttpMethod.Post, supportPath, supported)
        state = supported.bodyAsText().asObject()
        repeat(3) { state = completePairTenOrder(cookie, owner, practiceId, inspectorKey, state) }

        val exerciseId = currentExerciseId(state)
        val order = canonicalPairTenOrder(cookie, inspectorKey, exerciseId)
        val request = AttemptRequest(UUID.randomUUID().toString(), exerciseId, AttemptAnswerDto.Order(order.take(2)), false)
        val body = wireJson.encodeToString(request)
        val partial = moduleOnePost(cookie, owner, "/api/v1/attempts", body)
        assertEquals(200, partial.status.value)
        validate(HttpMethod.Post, "/api/v1/attempts", partial)
        val partialText = partial.bodyAsText()
        val grade = partialText.asObject()
        assertEquals("unevaluable", grade.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("unknown", grade.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals("not-met", grade.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertTrue(grade.getValue("issues").jsonArray.any {
            it.jsonObject.getValue("code").jsonPrimitive.content == "REQUIRED_WORDS_MISSING"
        })

        val advanced = advanceModuleOne(cookie, owner, practiceId, 3)
        assertEquals(200, advanced.status.value)
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
        assertEquals(partialText, moduleOnePost(cookie, owner, "/api/v1/attempts", body).bodyAsText())
        state = advanced.bodyAsText().asObject()
        repeat(2) { state = completePairTenOrder(cookie, owner, practiceId, inspectorKey, state) }
        assertEquals("completed", state.getValue("state").jsonPrimitive.content)
        assertPairTenOrderSummary(state)
    }

    private suspend fun ApplicationTestBuilder.completePairTenOrder(
        cookie: String,
        owner: String,
        practiceId: UUID,
        inspectorKey: String,
        state: JsonObject,
    ): JsonObject {
        val exerciseId = currentExerciseId(state)
        val order = canonicalPairTenOrder(cookie, inspectorKey, exerciseId)
        val attempt = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Order(order))
        assertEquals(200, attempt.status.value)
        validate(HttpMethod.Post, "/api/v1/attempts", attempt)
        val advanced = advanceModuleOne(cookie, owner, practiceId, state.getValue("cursor").jsonPrimitive.content.toInt())
        assertEquals(200, advanced.status.value)
        validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
        return advanced.bodyAsText().asObject()
    }

    private fun assertPairTenOrderSummary(state: JsonObject) {
        val summary = state.getValue("summary").jsonObject
        val rows = summary.getValue("rows").jsonArray.map { it.jsonObject }
        assertEquals(setOf("constituent-order", "word-order"), rows.map {
            it.getValue("assessmentMode").jsonPrimitive.content
        }.toSet())
        assertTrue(rows.all { it.getValue("targetScopeId").jsonPrimitive.content == "subject-first-supplied" })
        for ((field, value) in listOf("targetSkillId" to "G004", "assessmentMode" to "recall")) {
            val badRows = JsonArray(listOf(JsonObject(rows.first() + (field to JsonPrimitive(value)))) + rows.drop(1))
            val badSummary = JsonObject(summary + ("rows" to badRows))
            assertPracticeResponseValidity(JsonObject(state + ("summary" to badSummary)).toString(), expected = false)
        }
    }

    @Test
    fun `reading words preserve four lexical meanings and only confirmed feedback exposure`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val cookie = moduleOneCookie()
        val owner = client.get("/api/v1/practice-context") {
            trustedHost()
            cookie("astrid_session", cookie)
        }.bodyAsText().asObject().getValue("ownerSessionId").jsonPrimitive.content
        val practiceId = UUID.randomUUID()
        val created = moduleOnePost(
            cookie, owner, "/api/v1/practice-sessions",
            """{"practiceSessionId":"$practiceId","focusId":"module-one-reading-words","planRevision":"module-one-reading-words-1","seed":47}""",
        )
        assertEquals(201, created.status.value)
        validate(HttpMethod.Post, "/api/v1/practice-sessions", created)
        val initial = created.bodyAsText().asObject()
        assertEquals("pilot-5", initial.getValue("contentRevision").jsonPrimitive.content)
        assertTrue(initial.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
        val firstId = currentExerciseId(initial)
        assertEquals(409, submitAttempt(cookie, firstId, AttemptAnswerDto.Text("et magasin")).status.value)
        val supportPath = "/api/v1/practice-sessions/$practiceId/support"
        val supported = moduleOnePost(cookie, owner, supportPath, """{"kind":"introduction"}""")
        assertEquals(200, supported.status.value)
        validate(HttpMethod.Post, supportPath, supported)
        var state = supported.bodyAsText().asObject()

        assertEquals(400, submitAttempt(cookie, firstId, AttemptAnswerDto.Text("magasin")).status.value)
        val wrong = submitAttempt(cookie, firstId, AttemptAnswerDto.Text("en bog"))
        validate(HttpMethod.Post, "/api/v1/attempts", wrong)
        val wrongGrade = wrong.bodyAsText().asObject()
        assertEquals("not-met", wrongGrade.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals("acceptable", wrongGrade.getValue("linguisticStatus").jsonPrimitive.content)
        assertTrue(readModuleOnePractice(cookie, practiceId).getValue("items").jsonArray.first().jsonObject
            .getValue("canAdvance").jsonPrimitive.content.toBoolean())

        val answers = listOf("et magasin", "en bog", "en avis", "et brev")
        val senses = listOf("L024-S01", "L014-S01", "L015-S01", "L018-S01")
        for ((position, answer) in answers.withIndex()) {
            val exercise = state.getValue("currentExercise").jsonObject
            assertEquals("E011", exercise.getValue("formatId").jsonPrimitive.content)
            assertEquals(senses[position], exercise.getValue("lexicalTarget").jsonObject.getValue("senseId").jsonPrimitive.content)
            val attempt = submitAttempt(cookie, currentExerciseId(state), AttemptAnswerDto.Text(answer))
            validate(HttpMethod.Post, "/api/v1/attempts", attempt)
            assertEquals("met", attempt.bodyAsText().asObject().getValue("targetSatisfaction").jsonPrimitive.content)
            if (position == 0) assertReadingConfirmation(cookie, practiceId, firstId)
            val advanced = advanceModuleOne(cookie, owner, practiceId, position)
            assertEquals(200, advanced.status.value)
            validate(HttpMethod.Post, "/api/v1/practice-sessions/$practiceId/advance", advanced)
            state = advanced.bodyAsText().asObject()
        }
        assertEquals("completed", state.getValue("state").jsonPrimitive.content)
        val rows = state.getValue("summary").jsonObject.getValue("rows").jsonArray.map { it.jsonObject }
        assertEquals(senses.toSet(), rows.map {
            it.getValue("lexicalTarget").jsonObject.getValue("senseId").jsonPrimitive.content
        }.toSet())
        assertTrue(rows.all { "assessmentMode" !in it && "targetSkillId" !in it })
    }

    private suspend fun ApplicationTestBuilder.assertReadingConfirmation(cookie: String, practiceId: UUID, exerciseId: String) {
        fun feedbackExposure(state: JsonObject) =
            state.getValue("items").jsonArray.first().jsonObject.getValue("lastAttemptContext").jsonObject
                .getValue("priorExposure").jsonArray.filter {
                    it.jsonObject.getValue("source").jsonPrimitive.content == "feedback"
                }
        assertTrue(feedbackExposure(readModuleOnePractice(cookie, practiceId)).isEmpty())
        val repeated = submitAttempt(cookie, exerciseId, AttemptAnswerDto.Text("et magasin"))
        assertEquals(200, repeated.status.value)
        val prior = feedbackExposure(readModuleOnePractice(cookie, practiceId)).single().jsonObject
        assertEquals("module-one:reading-meaning:L024-S01", prior.getValue("keys").jsonArray.single().jsonPrimitive.content)
    }

    private suspend fun ApplicationTestBuilder.readModuleOnePractice(cookie: String, practiceId: UUID) =
        client.get("/api/v1/practice-sessions/$practiceId") {
            trustedHost()
            cookie("astrid_session", cookie)
        }.bodyAsText().asObject()

    private suspend fun ApplicationTestBuilder.moduleOneCookie(): String =
        client.post("/api/v1/development-session") {
            trustedHost()
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }.headers[HttpHeaders.SetCookie]!!.substringAfter("astrid_session=").substringBefore(';')

    private suspend fun ApplicationTestBuilder.moduleOnePost(
        cookie: String,
        owner: String,
        path: String,
        body: String,
    ) = client.post(path) {
        trustedHost()
        cookie("astrid_session", cookie)
        header("X-Astrid-Client", "playground")
        header("X-Astrid-Practice-Owner", owner)
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    private suspend fun ApplicationTestBuilder.canonicalPairTenOrder(
        cookie: String,
        inspectorKey: String,
        exerciseId: String,
    ) = inspectExercise(cookie, inspectorKey, exerciseId)
        .getValue("tileSource").jsonObject.getValue("licensedOrders").jsonArray.single()
        .jsonObject.getValue("blockIds").jsonArray.map { it.jsonPrimitive.content }

    private fun currentExerciseId(state: JsonObject) =
        state.getValue("currentExercise").jsonObject.getValue("id").jsonPrimitive.content

    private fun String.asObject() = wireJson.parseToJsonElement(this).jsonObject

    private suspend fun ApplicationTestBuilder.createCurrentExercise(
        cookie: String,
        formatId: String,
        targetSkillId: String? = null,
        pair: Int = 16,
    ): JsonObject {
        val target = targetSkillId?.let { "\"targetSkillId\":\"$it\"," }.orEmpty()
        val response = client.post("/api/v1/exercises") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"$formatId",$target"seed":47,"index":0,"contentRevision":"pilot-$pair","generatorRevision":"generator-$pair"}""",
            )
        }
        validate(HttpMethod.Post, "/api/v1/exercises", response)
        return wireJson.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private suspend fun ApplicationTestBuilder.inspectExercise(
        cookie: String,
        inspectorKey: String,
        exerciseId: String,
    ): JsonObject {
        val response = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Inspector-Key", inspectorKey)
        }
        validate(HttpMethod.Get, "/api/v1/dev/exercises/$exerciseId", response)
        return wireJson.parseToJsonElement(response.bodyAsText()).jsonObject
    }

    private suspend fun ApplicationTestBuilder.submitAttempt(
        cookie: String,
        exerciseId: String,
        answer: AttemptAnswerDto,
    ): HttpResponse = client.post("/api/v1/attempts") {
        trustedHost()
        cookie("astrid_session", cookie)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody(
            wireJson.encodeToString(
                AttemptRequest(UUID.randomUUID().toString(), exerciseId, answer, false),
            ),
        )
    }

    private suspend fun ApplicationTestBuilder.submitModuleOneAttempt(
        cookie: String,
        exerciseId: String,
        answer: String,
    ) = client.post("/api/v1/attempts") {
        trustedHost()
        cookie("astrid_session", cookie)
        header("X-Astrid-Client", "playground")
        contentType(ContentType.Application.Json)
        setBody(
            wireJson.encodeToString(
                AttemptRequest(UUID.randomUUID().toString(), exerciseId, AttemptAnswerDto.Text(answer), false),
            ),
        )
    }

    private suspend fun ApplicationTestBuilder.advanceModuleOne(
        cookie: String,
        owner: String,
        practiceId: UUID,
        position: Int,
    ) = client.post("/api/v1/practice-sessions/$practiceId/advance") {
        trustedHost()
        cookie("astrid_session", cookie)
        header("X-Astrid-Client", "playground")
        header("X-Astrid-Practice-Owner", owner)
        contentType(ContentType.Application.Json)
        setBody("""{"position":$position}""")
    }

    private fun assertGapOutcome(body: JsonObject, primaryCorrect: Boolean?, supportCorrect: Boolean?) {
        val expectedTarget = when (primaryCorrect) {
            null -> "unevaluable"
            true -> "met"
            false -> "not-met"
        }
        val expectedConstraint = if (primaryCorrect != null && supportCorrect != null) "met" else "not-met"
        val expectedLanguage = when {
            primaryCorrect == false || supportCorrect == false -> "known-issue"
            primaryCorrect == null || supportCorrect == null -> "unknown"
            else -> "acceptable"
        }
        assertEquals(expectedTarget, body.getValue("targetSatisfaction").jsonPrimitive.content)
        assertEquals(expectedConstraint, body.getValue("responseConstraintStatus").jsonPrimitive.content)
        assertEquals(expectedLanguage, body.getValue("linguisticStatus").jsonPrimitive.content)
        assertEquals(false, "meaningSatisfaction" in body)
    }

    private fun assertExerciseRequestValidity(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/exercises")
            .withHeader("Content-Type", "application/json")
            .withHeader("X-Astrid-Client", "playground")
            .withBody(body)
            .build()
        val report = strictValidator.validateRequest(request)
        assertEquals(expected, !report.hasErrors(), "$body\n${SimpleValidationReportFormat.getInstance().apply(report)}")
    }

    private fun assertExerciseResponseValidity(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/exercises").build()
        val response = SimpleResponse.Builder(201)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .build()
        val report = validator.validateResponse("/api/v1/exercises", request.method, response)
        assertEquals(expected, !report.hasErrors(), "$body\n${SimpleValidationReportFormat.getInstance().apply(report)}")
    }

    private fun assertAttemptRequestValidity(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/attempts")
            .withHeader("Content-Type", "application/json")
            .withHeader("X-Astrid-Client", "playground")
            .withBody(body)
            .build()
        val report = strictValidator.validateRequest(request)
        assertEquals(expected, !report.hasErrors(), "$body\n${SimpleValidationReportFormat.getInstance().apply(report)}")
    }

    private fun assertPracticeResponseValidity(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/practice-sessions").build()
        val response = SimpleResponse.Builder(201)
            .withHeader("Content-Type", "application/json")
            .withHeader("Location", "/api/v1/practice-sessions/00000000-0000-0000-0000-000000000001")
            .withBody(body)
            .build()
        val report = validator.validateResponse("/api/v1/practice-sessions", request.method, response)
        assertEquals(expected, !report.hasErrors(), SimpleValidationReportFormat.getInstance().apply(report))
    }

    private fun matchingAttemptRequest(pairs: List<Pair<String, String>>): String {
        val pairJson = pairs.joinToString(",") { (left, right) ->
            """{"leftId":"$left","rightId":"$right"}"""
        }
        return """{"attemptId":"00000000-0000-0000-0000-000000000001","exerciseId":"00000000-0000-0000-0000-000000000002","answer":{"kind":"matching","pairs":[$pairJson]},"hintUsed":false}"""
    }

    private fun gapAttemptRequest(selections: List<Pair<String, String>>): String {
        val selectionJson = selections.joinToString(",") { (slot, choice) ->
            """{"slotId":"$slot","choiceId":"$choice"}"""
        }
        return """{"attemptId":"00000000-0000-0000-0000-000000000001","exerciseId":"00000000-0000-0000-0000-000000000002","answer":{"kind":"gaps","selections":[$selectionJson]},"hintUsed":false}"""
    }

    private fun <T> permutations(values: List<T>): List<List<T>> = if (values.isEmpty()) {
        listOf(emptyList())
    } else {
        values.flatMap { selected ->
            permutations(values - selected).map { remainder -> listOf(selected) + remainder }
        }
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
        tileMode: String? = null,
    ): String {
        val targetJson = target?.let { "\"targetSkillId\":\"$it\"," }.orEmpty()
        val lexicalJson = if (lexicalFacet != null && lexicalDirection != null) {
            """"lexicalTarget":{"entryId":"L014","senseId":"L014-S01","facet":"$lexicalFacet","direction":"$lexicalDirection"},"""
        } else {
            ""
        }
        val readingJson = readingFacet?.let { """"readingTarget":{"facet":"$it"},""" }.orEmpty()
        val passageJson = if (passage) ""","passage":["Hun læser en avis.","Han læser en bog.","Hun har et brev."]""" else ""
        val prompt = when (tileMode) {
            "use-all" -> """{"before":"","after":"","choices":[],"blocks":[{"id":"b0","text":"Hun"},{"id":"b1","text":"læser"}],"punctuation":".","cue":"She reads","cueLanguage":"en","answerLanguage":"da","tileMode":"use-all","requiredTileCount":2}"""
            "with-spares" -> """{"before":"","after":"","choices":[],"blocks":[{"id":"b0","text":"Hun"},{"id":"b1","text":"læser"},{"id":"b2","text":"læse"}],"punctuation":".","cue":"She reads","cueLanguage":"en","answerLanguage":"da","tileMode":"with-spares","requiredTileCount":2}"""
            else -> {
                val choices = if (format == "E011") {
                    "[\"en avis\",\"en bog\",\"et brev\",\"et magasin\"]"
                } else {
                    "[\"a newspaper\",\"a book\",\"a letter\"]"
                }
                val answerLanguage = if (format == "E011") "da" else "en"
                """{"before":"","after":"","choices":$choices,"blocks":[],"punctuation":"","cue":"Question","cueLanguage":"en","answerLanguage":"$answerLanguage"$passageJson}"""
            }
        }
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"$format",$targetJson$lexicalJson$readingJson"supportSkillIds":[],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
    }

    private fun matchingExerciseResponse(
        content: String = "pilot-6",
        includeTargets: Boolean = true,
        answerLanguage: String = "da",
        firstLeftId: String = "l0",
        firstRightId: String = "r0",
    ): String {
        val targets = if (includeTargets) {
            val values = listOf("L014", "L015", "L018", "L024").joinToString(",") { id ->
                """{"entryId":"$id","senseId":"$id-S01","facet":"meaning","direction":"en-da"}"""
            }
            "\"lexicalTargets\":[$values],"
        } else {
            ""
        }
        val left = (0..3).joinToString(",") { """{"id":"${if (it == 0) firstLeftId else "l$it"}","text":"English $it"}""" }
        val right = (0..3).joinToString(",") { """{"id":"${if (it == 0) firstRightId else "r$it"}","text":"Danish $it"}""" }
        val prompt = """{"before":"","after":"","choices":[],"blocks":[],"punctuation":"","cueLanguage":"en","answerLanguage":"$answerLanguage","matchingColumns":{"left":[$left],"right":[$right]}}"""
        val generator = when (content) {
            "pilot-8" -> "generator-8"
            "pilot-7" -> "generator-7"
            else -> "generator-6"
        }
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"E014",$targets"supportSkillIds":[],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
    }

    private fun vocabularyMatchingExerciseResponse(
        facet: String = "meaning",
        direction: String = "en-da",
        content: String = "pilot-17",
    ): String {
        val targets = (0..2).joinToString(",") { index ->
            """{"entryId":"L0$index","senseId":"L0$index-S01","facet":"$facet","direction":"$direction"}"""
        }
        val left = (0..2).joinToString(",") { index -> """{"id":"m-a$index","text":"English $index"}""" }
        val right = (0..2).joinToString(",") { index -> """{"id":"n-b$index","text":"Dansk $index"}""" }
        val generator = content.replace("pilot-", "generator-")
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"E033","lexicalTargets":[$targets],"supportSkillIds":[],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":{"before":"","after":"","choices":[],"blocks":[],"punctuation":"","cueLanguage":"en","answerLanguage":"da","matchingColumns":{"left":[$left],"right":[$right]}},"introduction":[],"hint":"Hint"}"""
    }

    private fun gapExerciseResponse(
        content: String = "pilot-7",
        target: String = "G004",
        primaryRole: String = "primary",
        supportSkill: String = "G006",
    ): String {
        val prompt = """{"before":"","after":"","choices":[],"blocks":[],"punctuation":"","cueLanguage":"da","answerLanguage":"da","gapPrompt":{"segments":["Jeg "," "," bog."],"slots":[{"id":"g0","label":"Present verb — Primary target","targetSkillId":"G004","assessmentRole":"$primaryRole","choices":[{"id":"c0","text":"læse"},{"id":"c1","text":"læser"}]},{"id":"g1","label":"Indefinite article — Supporting grammar","targetSkillId":"$supportSkill","assessmentRole":"support","choices":[{"id":"c0","text":"en"},{"id":"c1","text":"et"}]}]}}"""
        val generator = if (content == "pilot-8") "generator-8" else "generator-7"
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"E015","targetSkillId":"$target","supportSkillIds":["G001","G002","G003","G005","G006"],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
    }

    private fun moduleOneExerciseResponse(
        format: String,
        target: String,
        scope: String,
        content: String = "pilot-8",
    ): String {
        val choices = if (format in setOf("E016", "E018", "E020")) "[\"den\",\"det\"]" else "[]"
        val prompt = if (format == "E030") {
            questionPrompt()
        } else {
            val cueLanguage = if (format == "E029") "da" else "en"
            """{"before":"","after":"","choices":$choices,"blocks":[],"punctuation":"","inputLabel":"Danish word","cue":"English cue","cueLanguage":"$cueLanguage","answerLanguage":"da"}"""
        }
        val generator = content.replace("pilot-", "generator-")
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"$format","targetSkillId":"$target","targetScopeId":"$scope","supportSkillIds":[],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
    }

    private fun questionExerciseResponse(prompt: String) =
        moduleOneExerciseResponse("E030", "G018", "neutral-positive-yes-no", "pilot-16")
            .replace(questionPrompt(), prompt)

    private fun questionPrompt(
        cue: String = "Du læser en avis.",
        blocks: String = READ_QUESTION_BLOCKS,
        punctuation: String = "?",
        cueLanguage: String = "da",
        answerLanguage: String = "da",
        before: String = "",
        after: String = "",
        choices: String = "[]",
        extra: String = "",
    ) = """{"before":"$before","after":"$after","choices":$choices,"blocks":$blocks,"punctuation":"$punctuation","cue":"$cue","cueLanguage":"$cueLanguage","answerLanguage":"$answerLanguage"$extra}"""

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
        init {
            val runtime = Runtime.getRuntime()
            val collectors = ManagementFactory.getGarbageCollectorMXBeans().joinToString(",") { it.name }
            println(
                "OpenApiContractTest runtime: maxMemoryMiB=${runtime.maxMemory() / (1024 * 1024)}, " +
                    "availableProcessors=${runtime.availableProcessors()}, garbageCollectors=$collectors",
            )
        }

        const val SCHEMA_CACHE_ENTRIES = 2
        const val EXERCISE_REQUEST =
            """{"formatId":"E001","targetSkillId":"G004","seed":7,"index":0,"contentRevision":"pilot-1","generatorRevision":"generator-1"}"""
        const val READ_QUESTION_BLOCKS =
            """[{"id":"b0","text":"læser"},{"id":"b1","text":"du"},{"id":"b2","text":"en avis"}]"""
        const val WORK_QUESTION_BLOCKS = """[{"id":"b0","text":"arbejder"},{"id":"b1","text":"hun"}]"""
        const val RESIDENCE_QUESTION_BLOCKS =
            """[{"id":"b0","text":"bor"},{"id":"b1","text":"I"},{"id":"b2","text":"i Aarhus"}]"""
        const val POSSESSION_QUESTION_BLOCKS =
            """[{"id":"b0","text":"har"},{"id":"b1","text":"de"},{"id":"b2","text":"et brev"}]"""
    }
}
