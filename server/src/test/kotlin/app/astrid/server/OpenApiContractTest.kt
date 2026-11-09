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
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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
        val supportedPairs = setOf(
            "reading-basics" to "practice-1",
            "present-verbs" to "present-verbs-1",
            "verb-article-gaps" to "verb-article-gaps-1",
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
        val report = validator.validate(request, response)
        assertEquals(expected, !report.hasErrors(), SimpleValidationReportFormat.getInstance().apply(report))
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
        val report = validator.validate(request, response)
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
        val generator = if (content == "pilot-7") "generator-7" else "generator-6"
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"E014",$targets"supportSkillIds":[],"contentRevision":"$content","generatorRevision":"$generator","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
    }

    private fun gapExerciseResponse(
        content: String = "pilot-7",
        target: String = "G004",
        primaryRole: String = "primary",
        supportSkill: String = "G006",
    ): String {
        val prompt = """{"before":"","after":"","choices":[],"blocks":[],"punctuation":"","cueLanguage":"da","answerLanguage":"da","gapPrompt":{"segments":["Jeg "," "," bog."],"slots":[{"id":"g0","label":"Present verb — Primary target","targetSkillId":"G004","assessmentRole":"$primaryRole","choices":[{"id":"c0","text":"læse"},{"id":"c1","text":"læser"}]},{"id":"g1","label":"Indefinite article — Supporting grammar","targetSkillId":"$supportSkill","assessmentRole":"support","choices":[{"id":"c0","text":"en"},{"id":"c1","text":"et"}]}]}}"""
        return """{"id":"00000000-0000-0000-0000-000000000001","formatId":"E015","targetSkillId":"$target","supportSkillIds":["G001","G002","G003","G005","G006"],"contentRevision":"$content","generatorRevision":"generator-7","instruction":"Instruction","context":"Context","prompt":$prompt,"introduction":[],"hint":"Hint"}"""
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
