@file:Suppress("MaxLineLength")

package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.GenerationRequest
import app.astrid.engine.MatchingPair
import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlanner
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.CatalogResponse
import app.astrid.server.exercises.FormatDto
import app.astrid.server.exercises.IssueDto
import app.astrid.server.exercises.LexicalTargetDto
import app.astrid.server.exercises.LexicalTaskDto
import app.astrid.server.exercises.MatchingPairResultDto
import app.astrid.server.exercises.ReadingTaskDto
import app.astrid.server.exercises.SkillDto
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.PracticeProfileDto
import app.astrid.server.practice.PracticeProfilesResponse
import app.astrid.server.practice.PracticeReviewContextResponse
import app.astrid.server.practice.StoredPracticeItem
import app.astrid.server.practice.StoredPracticeSession
import app.astrid.server.practice.toNewPracticeSession
import app.astrid.server.practice.toPayload
import app.astrid.server.practice.toResponse
import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import com.atlassian.oai.validator.report.ValidationReport
import com.atlassian.oai.validator.schema.ValidationConfiguration
import io.swagger.v3.parser.core.models.ParseOptions
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class VocabularyP3ApiSchemaContractTest {
    private val rawSpec = checkNotNull(javaClass.getResource("/openapi/documentation.yaml")).readText()
    private fun validatorBuilder() = OpenApiInteractionValidator.createForInlineApiSpecification(rawSpec)
        .withParseOptions(ParseOptions().apply {
            isResolve = true
            isResolveFully = false
            isResolveResponses = true
            isResolveRequestBody = true
            isResolveCombinators = false
            isExplicitObjectSchema = false
        })
        .withSchemaValidationConfiguration(ValidationConfiguration().setMaxCacheSize(2))

    private val strictValidator by lazy { validatorBuilder().build() }
    private val responseValidator by lazy {
        validatorBuilder().withLevelResolver(
            LevelResolver.create()
                .withLevel("validation.request", ValidationReport.Level.IGNORE)
                .withLevel("validation.request.path.missing", ValidationReport.Level.ERROR)
                .withLevel("validation.request.operation.notAllowed", ValidationReport.Level.ERROR)
                .withLevel("validation.response", ValidationReport.Level.ERROR)
                .build(),
        ).build()
    }

    @Test
    fun `current catalog review context and all thirty five profiles satisfy the strict contract`() {
        val engine = PilotEngine()
        val catalog = engine.catalog()
        val catalogBody = wireJson.encodeToString(CatalogResponse(
            catalog.contentRevision,
            catalog.generatorRevision,
            checkNotNull(engine.evaluationRevision(catalog.contentRevision, catalog.generatorRevision)),
            catalog.skills.map { SkillDto(it.id, it.title, it.explanation, it.prerequisiteIds) },
            catalog.formats.map { format ->
                FormatDto(format.id, format.title, format.targetSkillId,
                    format.lexicalTask?.let { LexicalTaskDto(it.facet, it.direction) },
                    format.readingTask?.let { ReadingTaskDto(it.facet) })
            },
            catalog.sentenceCount,
        ))
        assertEquals(51, catalog.formats.size)
        assertResponse("/api/v1/catalog", "GET", 200, catalogBody)

        val profiles = PracticePlanner(engine).profiles()
        assertEquals(35, profiles.size)
        val profileBody = wireJson.encodeToString(PracticeProfilesResponse(
            OWNER_ID.toString(), 2_000_000_000_000,
            profiles.map { PracticeProfileDto(it.focusId, it.planRevision, it.title, it.description,
                it.itemCount, it.prerequisiteSkillIds) },
        ))
        assertResponse("/api/v1/practice-profiles", "GET", 200, profileBody)
        val p3 = profiles.filter { it.focusId.startsWith("vocabulary-p3-") }
        assertEquals(listOf(4, 4, 4, 3, 4, 4), p3.map { it.itemCount })
        assertTrue(p3.all { it.planRevision == "${it.focusId}-1" })

        val review = wireJson.encodeToString(PracticeReviewContextResponse(
            OWNER_ID.toString(), 2_000_000_000_000, "cumulative-review", "cumulative-review-3",
            "Review", 8, 32,
        ))
        assertResponse("/api/v1/practice-review-context", "GET", 200, review)
    }

    @Test
    fun `all twenty three P3 exercises and inspectors satisfy the public wire contract`() {
        val engine = PilotEngine()
        val generated = P3_FORMAT_COUNTS.flatMap { (format, count) ->
            (0 until count).map { index ->
                engine.generate(GenerationRequest(format, P3_TARGETS[format], 0, index, "pilot-20", "generator-20"))
            }
        }
        assertEquals(23, generated.size)
        generated.forEachIndexed { index, exercise ->
            val id = UUID(0, index.toLong() + 1).toString()
            val publicBody = wireJson.encodeToString(exercise.toResponse(id))
            assertResponse("/api/v1/exercises", "POST", 201, publicBody)
            assertResponse("/api/v1/dev/exercises/$id", "GET", 200,
                wireJson.encodeToString(exercise.toInspector(id)))
            assertNoPrivateAnswerLeak(wireJson.parseToJsonElement(publicBody))
        }

        val reading = generated.first { it.formatId == "E050" }
        val exerciseId = UUID(0, 50).toString()
        val expected = checkNotNull(reading.toInspector(exerciseId).vocabularySource?.expectedAnswer)
        val evaluation = engine.evaluate(reading, Answer.Text(expected))
        val attempt = AttemptResponse(
            UUID(0, 51).toString(), exerciseId, evaluation.targetStatus.wireValue,
            evaluation.linguisticStatus.wireValue, evaluation.issues.map { IssueDto(it.code, it.message) },
            evaluation.feedback, evaluation.completedSentence.orEmpty(), "evaluation-20", false, 1, 1,
            evaluation.linguisticScope?.wireValue, evaluation.responseConstraintStatus?.wireValue,
            evaluation.meaningSatisfaction?.wireValue,
        )
        assertEquals("met", attempt.targetSatisfaction)
        assertResponse("/api/v1/attempts", "POST", 200, wireJson.encodeToString(attempt))

        val matching = generated.first { it.formatId == "E047" }
        val matchingId = UUID(0, 52).toString()
        val selected = checkNotNull(matching.toInspector(matchingId).vocabularySource).matchingPairs.first()
        val partialEvaluation = engine.evaluate(
            matching,
            Answer.Matching(listOf(MatchingPair(selected.leftId, selected.rightId))),
        )
        val partialAttempt = AttemptResponse(
            UUID(0, 53).toString(), matchingId, partialEvaluation.targetStatus.wireValue,
            partialEvaluation.linguisticStatus.wireValue,
            partialEvaluation.issues.map { IssueDto(it.code, it.message) }, partialEvaluation.feedback,
            partialEvaluation.completedSentence.orEmpty(), "evaluation-20", false, 1, 2,
            partialEvaluation.linguisticScope?.wireValue, partialEvaluation.responseConstraintStatus?.wireValue,
            partialEvaluation.meaningSatisfaction?.wireValue,
            partialEvaluation.matchingResults?.map { result ->
                MatchingPairResultDto(
                    result.leftId, result.rightId,
                    LexicalTargetDto(
                        result.lexicalTarget.entryId, result.lexicalTarget.senseId,
                        result.lexicalTarget.facet, result.lexicalTarget.direction,
                    ),
                    result.targetStatus.wireValue, result.meaningSatisfaction.wireValue,
                    result.responseConstraintStatus.wireValue, result.linguisticStatus.wireValue, result.feedback,
                )
            },
        )
        assertEquals("unevaluable", partialAttempt.targetSatisfaction)
        assertResponse("/api/v1/attempts", "POST", 200, wireJson.encodeToString(partialAttempt))
    }

    @Test
    fun `P3 create requests retain exact focus revision pairing`() {
        (1..6).forEach { lesson ->
            val focus = "vocabulary-p3-s$lesson"
            assertRequest(sessionRequest(focus, "$focus-1"), true)
        }
        assertRequest(sessionRequest("vocabulary-p3-s1", "vocabulary-p3-s2-1"), false)
        assertRequest(sessionRequest("vocabulary-p3-s1", "vocabulary-p3-s1-1", extra = true), false)
        listOf("cumulative-review-1", "cumulative-review-2", "cumulative-review-3").forEach { revision ->
            assertRequest(sessionRequest("cumulative-review", revision), true)
        }
    }

    @Test
    fun `all six created P3 sessions preserve strict saved item descriptors`() {
        val planner = PracticePlanner(PilotEngine())
        val sessionBodies = (1..6).map { lesson ->
            val focus = "vocabulary-p3-s$lesson"
            val revision = "$focus-1"
            val created = planner.plan(focus, revision, 0).toNewPracticeSession(OWNER_ID)
            val privatePlan = wireJson.parseToJsonElement(wireJson.encodeToString(created.toPayload())).jsonObject
            assertTrue(privatePlan.getValue("items").toString().contains("answerExposureKeys"))
            val stored = StoredPracticeSession(
                UUID(1, lesson.toLong()), OWNER_ID, 2_000_000_000_000, focus, created.title, revision, 0,
                created.contentRevision, created.generatorRevision, created.evaluationRevision, 0, false,
                created.prerequisiteSkillIds, created.introduction,
                created.items.map { item ->
                    StoredPracticeItem(
                        item.position, item.exercise, item.targetLabel, item.answerExposureKeys,
                        item.promptExposureKeys, item.lessonExposureKeys, item.hintExposureKeys,
                        item.feedbackExposureKeys, false, false, emptyList(), item.assessmentMode,
                        item.assessments, item.targetScopeId, item.readingRole, item.matchingTarget, item.policy,
                    )
                },
                introductionExposureKeys = created.introductionExposureKeys,
                introductionRequired = created.introductionRequired,
                completionRuleId = created.completionRuleId,
                exposurePolicyId = created.exposurePolicyId,
            )
            wireJson.encodeToString(stored.toResponse()).also {
                assertResponse("/api/v1/practice-sessions", "POST", 201, it)
            }
        }
        assertEquals(6, sessionBodies.size)
        assertPracticeItemRejected(
            sessionBodies, "E048", "targetScopeId", JsonPrimitive("p2-subject-first-supplied"),
        )
        assertPracticeItemRejected(sessionBodies, "E050", "readingRole", JsonPrimitive("place"))
        assertPracticeItemRejected(sessionBodies, "E051", "assessmentMode", JsonPrimitive("recall"))
        val e045 = sessionItem(sessionBodies, "E045")
        val wrongE045Target = e045.getValue("lexicalTarget").jsonObject.with(
            "entryId", JsonPrimitive("L014"),
        ).with("senseId", JsonPrimitive("L014-S01"))
        assertPracticeItemRejected(sessionBodies, "E045", "lexicalTarget", wrongE045Target)
        val wrongE045Assessment = e045.getValue("assessments").jsonArray.first().jsonObject.with(
            "lexicalTarget", wrongE045Target,
        )
        assertPracticeItemRejected(
            sessionBodies, "E045", "assessments", JsonArray(listOf(wrongE045Assessment)),
        )
        val e047 = sessionItem(sessionBodies, "E047")
        val mismatchedAssessment = e047.getValue("assessments").jsonArray.first().jsonObject.let { assessment ->
            assessment.with(
                "lexicalTarget",
                assessment.getValue("lexicalTarget").jsonObject.with("senseId", JsonPrimitive("L064-S01")),
            )
        }
        assertPracticeItemRejected(
            sessionBodies, "E047", "assessments",
            JsonArray(listOf(mismatchedAssessment) + e047.getValue("assessments").jsonArray.drop(1)),
        )
    }

    @Test
    fun `P3 exercise schemas reject cross family targets malformed prompt fields and answer leaks`() {
        val engine = PilotEngine()
        val samples = P3_FORMAT_COUNTS.keys.associateWith { format ->
            engine.generate(GenerationRequest(format, P3_TARGETS[format], 0, 0, "pilot-20", "generator-20"))
                .toResponse(OWNER_ID.toString())
        }
        fun body(format: String) = wireJson.parseToJsonElement(
            wireJson.encodeToString(samples.getValue(format)),
        ).jsonObject

        assertExerciseRejected(body("E045").without("lexicalTarget"))
        assertExerciseRejected(body("E045").with(
            "lexicalTarget",
            body("E045").getValue("lexicalTarget").jsonObject
                .with("entryId", JsonPrimitive("L014"))
                .with("senseId", JsonPrimitive("L014-S01")),
        ))
        assertExerciseRejected(body("E046").replacePrompt("cueLanguage", JsonPrimitive("da")))
        assertExerciseRejected(body("E047").with("targetSkillId", JsonPrimitive("G004")))
        val mismatchedTargets = body("E047").getValue("lexicalTargets").jsonArray.mapIndexed { index, target ->
            if (index == 0) target.jsonObject.with("senseId", JsonPrimitive("L064-S01")) else target
        }
        assertExerciseRejected(body("E047").with("lexicalTargets", JsonArray(mismatchedTargets)))
        val recallPrompt = body("E048").getValue("prompt").jsonObject
        assertEquals(JsonPrimitive("at køre"), recallPrompt.getValue("cue"))
        assertEquals(JsonPrimitive("da"), recallPrompt.getValue("cueLanguage"))
        assertExerciseRejected(body("E048").replacePrompt("cue", JsonPrimitive("at vente")))
        assertExerciseRejected(body("E048").replacePrompt("cueLanguage", JsonPrimitive("en")))
        assertExerciseRejected(body("E048").with("targetScopeId", JsonPrimitive("p2-subject-first-supplied")))
        assertExerciseRejected(body("E049").with("assessmentMode", JsonPrimitive("selection")))
        assertExerciseRejected(body("E050").with("readingRole", JsonPrimitive("place")))
        val directionPrompt = body("E051").getValue("prompt").jsonObject
        assertEquals(JsonPrimitive("Nora går hjem."), directionPrompt.getValue("cue"))
        assertEquals(JsonPrimitive("da"), directionPrompt.getValue("cueLanguage"))
        assertExerciseRejected(body("E051").replacePrompt("cue", JsonPrimitive("Anna går hjem.")))
        assertExerciseRejected(body("E051").replacePrompt("cueLanguage", JsonPrimitive("en")))
        assertExerciseRejected(body("E051").with("targetScopeId", JsonPrimitive("p3-home-paired-contrast")))
        assertExerciseRejected(body("E045").replacePrompt("passageLabels", JsonPrimitive("private-answer")))
        assertExerciseRejected(body("E045").with("canonicalAnswer", JsonPrimitive("a bicycle")))

        val pairedSpatial = engine.generate(
            GenerationRequest("E051", "G016", 0, 1, "pilot-20", "generator-20"),
        ).toResponse(OWNER_ID.toString()).let { wireJson.parseToJsonElement(wireJson.encodeToString(it)).jsonObject }
        val pairedMappings = pairedSpatial.getValue("prompt").jsonObject.getValue("choiceMappings").jsonArray
        assertEquals(
            JsonPrimitive("en"), pairedSpatial.getValue("prompt").jsonObject.getValue("cueLanguage"),
        )
        assertExerciseRejected(pairedSpatial.replacePrompt("cueLanguage", JsonPrimitive("da")))
        assertExerciseRejected(pairedSpatial.replacePrompt("choiceMappings", JsonArray(emptyList())))
        assertExerciseRejected(pairedSpatial.replacePrompt("choiceMappings", JsonArray(listOf(pairedMappings.first()))))

        val labelledReading = engine.generate(
            GenerationRequest("E050", null, 0, 4, "pilot-20", "generator-20"),
        ).toResponse(OWNER_ID.toString()).let { wireJson.parseToJsonElement(wireJson.encodeToString(it)).jsonObject }
        val duplicateIndexLabels = JsonArray(listOf(
            labelledReading.getValue("prompt").jsonObject.getValue("passageLabels").jsonArray.first(),
            buildJsonObject { put("sentenceIndex", JsonPrimitive(1)); put("text", JsonPrimitive("Vehicle B")) },
        ))
        assertExerciseRejected(labelledReading.replacePrompt("passageLabels", duplicateIndexLabels))
    }

    private fun assertRequest(body: String, expected: Boolean) {
        val request = SimpleRequest.Builder.post("/api/v1/practice-sessions")
            .withHeader("Content-Type", "application/json")
            .withHeader("X-Astrid-Client", "playground")
            .withHeader("X-Astrid-Practice-Owner", OWNER_ID.toString())
            .withBody(body)
            .build()
        val report = strictValidator.validateRequest(request)
        assertEquals(
            expected,
            !report.hasErrors(),
            "$body\n${SimpleValidationReportFormat.getInstance().apply(report)}",
        )
    }

    private fun assertResponse(path: String, method: String, status: Int, body: String) {
        val request = if (method == "GET") {
            SimpleRequest.Builder.get(path).build()
        } else {
            SimpleRequest.Builder.post(path).build()
        }
        val response = SimpleResponse.Builder(status)
            .withHeader("Content-Type", "application/json")
            .withBody(body)
            .build()
        val report = responseValidator.validateResponse(path, request.method, response)
        assertFalse(report.hasErrors(), "$body\n${SimpleValidationReportFormat.getInstance().apply(report)}")
    }

    private fun assertExerciseRejected(body: JsonObject) {
        val request = SimpleRequest.Builder.post("/api/v1/exercises").build()
        val response = SimpleResponse.Builder(201).withHeader("Content-Type", "application/json")
            .withBody(body.toString()).build()
        assertTrue(
            responseValidator.validateResponse("/api/v1/exercises", request.method, response).hasErrors(),
            body.toString(),
        )
    }

    private fun assertPracticeItemRejected(
        sessions: List<String>,
        formatId: String,
        key: String,
        value: JsonElement,
    ) {
        val session = sessions.map { wireJson.parseToJsonElement(it).jsonObject }.first { candidate ->
            (candidate.getValue("items") as kotlinx.serialization.json.JsonArray).any { item ->
                item.jsonObject.getValue("formatId") == JsonPrimitive(formatId)
            }
        }
        val items = (session.getValue("items") as kotlinx.serialization.json.JsonArray).map { item ->
            if (item.jsonObject.getValue("formatId") == JsonPrimitive(formatId)) {
                item.jsonObject.with(key, value)
            } else {
                item
            }
        }
        val malformed = JsonObject(session + ("items" to kotlinx.serialization.json.JsonArray(items)))
        val request = SimpleRequest.Builder.post("/api/v1/practice-sessions").build()
        val response = SimpleResponse.Builder(201)
            .withHeader("Content-Type", "application/json")
            .withHeader("Location", "/api/v1/practice-sessions/${session.getValue("id")}")
            .withBody(malformed.toString())
            .build()
        assertTrue(
            responseValidator.validateResponse("/api/v1/practice-sessions", request.method, response).hasErrors(),
            malformed.toString(),
        )
    }

    private fun sessionItem(sessions: List<String>, formatId: String): JsonObject = sessions.asSequence()
        .map { wireJson.parseToJsonElement(it).jsonObject.getValue("items").jsonArray }
        .flatten()
        .map(JsonElement::jsonObject)
        .first { it.getValue("formatId") == JsonPrimitive(formatId) }

    private fun assertNoPrivateAnswerLeak(element: JsonElement) {
        if (element is JsonObject) {
            assertTrue(
                element.keys.none { it in PRIVATE_KEYS },
                "Public response leaked one of $PRIVATE_KEYS: $element",
            )
            element.values.forEach(::assertNoPrivateAnswerLeak)
        } else if (element is kotlinx.serialization.json.JsonArray) {
            element.forEach(::assertNoPrivateAnswerLeak)
        }
    }

    private fun JsonObject.with(key: String, value: JsonElement) = JsonObject(this + (key to value))
    private fun JsonObject.without(key: String) = JsonObject(this - key)
    private fun JsonObject.replacePrompt(key: String, value: JsonElement) = buildJsonObject {
        this@replacePrompt.forEach { (name, existing) ->
            put(name, if (name == "prompt") JsonObject(existing.jsonObject + (key to value)) else existing)
        }
    }

    private fun sessionRequest(focus: String, revision: String, extra: Boolean = false) = buildJsonObject {
        put("practiceSessionId", JsonPrimitive(OWNER_ID.toString()))
        put("focusId", JsonPrimitive(focus))
        put("planRevision", JsonPrimitive(revision))
        put("seed", JsonPrimitive(0))
        if (extra) put("extra", JsonPrimitive(true))
    }.toString()

    companion object {
        private val OWNER_ID: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val P3_FORMAT_COUNTS = linkedMapOf(
            "E045" to 1, "E046" to 1, "E047" to 6, "E048" to 1, "E049" to 3, "E050" to 8, "E051" to 3,
        )
        private val P3_TARGETS = mapOf("E048" to "G004", "E049" to "G005", "E051" to "G016")
        private val PRIVATE_KEYS = setOf(
            "canonicalAnswer", "acceptedAnswers", "expected", "licensedOrders", "lexicalSource", "vocabularySource",
        )
    }
}
