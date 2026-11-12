@file:Suppress("MaxLineLength") // Released wire payloads and their exact request coordinates stay visible in assertions.

package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.server.authz.Conflict
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.ExerciseResponse
import app.astrid.server.exercises.MatchingPairDto
import app.astrid.server.exercises.StoredExercise
import app.astrid.server.exercises.regenerateVerified
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.db.Database
import app.astrid.server.infra.db.migrate
import app.astrid.server.infra.json.wireJson
import app.astrid.server.session.SESSION_COOKIE
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
import java.security.MessageDigest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class LegacyVocabularyMatchingRecoveryTest {
    @Test
    fun `every released pair seventeen board restores its exact public response and fingerprint`() {
        val engine = PilotEngine()
        assertEquals(24, fixture.standalone.size)
        var mismatchCount = 0

        fixture.standalone.forEach { captured ->
            val stored = captured.stored()
            val normal = engine.generate(captured.request)
            if (captured.fingerprint != normal.reproductionFingerprint()) mismatchCount += 1

            val restored = regenerateVerified(engine, stored)

            assertEquals(captured.fingerprint, restored.reproductionFingerprint(), captured.coordinate)
            assertEquals(stored.presentation, restored.toResponse(stored.id.toString()), captured.coordinate)
        }
        assertTrue(mismatchCount > 0, "fixture must exercise the released/current matching incompatibility")
    }

    @Test
    fun `legacy recovery rejects altered fingerprint board unrelated public field and nonlegacy pair`() {
        val engine = PilotEngine()
        val captured = fixture.mismatchingStandalone()
        assertTrue(captured.hasNormalFingerprintMismatch())
        val original = captured.stored()
        val columns = checkNotNull(original.presentation.prompt.matchingColumns)
        val changedLeft = columns.left.toMutableList().also { rows ->
            rows[0] = rows[0].copy(text = "not an authored label")
        }
        val candidates = listOf(
            original.copy(reproductionFingerprint = "0".repeat(64)),
            original.copy(presentation = original.presentation.copy(
                prompt = original.presentation.prompt.copy(matchingColumns = null),
            )),
            original.copy(presentation = original.presentation.copy(
                prompt = original.presentation.prompt.copy(matchingColumns = columns.copy(left = changedLeft)),
            )),
            original.copy(presentation = original.presentation.copy(hint = "unrelated public mutation")),
            original.copy(formatId = "E014"),
            original.copy(formatId = "E014", contentRevision = "pilot-6", generatorRevision = "generator-6"),
        )

        candidates.forEach { stored ->
            assertFailsWith<Conflict> { regenerateVerified(engine, stored) }
        }
    }

    @Test
    fun `released standalone board grades fresh and exact replay bypasses later reproduction failure`() = testApplication {
        val stores = InMemoryStores()
        application { astridModule(config(), stores) }
        val cookie = createSession()
        val captured = fixture.mismatchingStandalone()
        assertTrue(captured.hasNormalFingerprintMismatch())
        val exerciseId = createVocabularyExercise(cookie, captured.request).json().string("id")
        stores.replaceExercise(UUID.fromString(exerciseId), captured.stored(exerciseId))
        val before = stores.storedExercise(UUID.fromString(exerciseId))
        val attemptId = UUID.randomUUID().toString()
        val request = AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Matching(captured.correctPairs), false)

        val fresh = submit(cookie, request)

        assertEquals(HttpStatusCode.OK, fresh.status, fresh.bodyAsText())
        assertEquals(captured.correctPairs.size, fresh.json().getValue("matchingResults").jsonArray.size)
        assertEquals(before, stores.storedExercise(UUID.fromString(exerciseId)))
        stores.tamperFingerprint(UUID.fromString(exerciseId))
        assertEquals(fresh.bodyAsText(), submit(cookie, request).bodyAsText())
        assertEquals(HttpStatusCode.Conflict, submit(cookie, request.copy(
            answer = AttemptAnswerDto.Matching(captured.correctPairs.reversed()),
        )).status)
    }

    @Test
    fun `released guided postgres plan keeps raw bytes and original associations while grading and reopening`() {
        val postgresConfig = postgresConfig()
        migrate(postgresConfig)
        val stores = PostgresStores(Database.connect(postgresConfig), wireJson)
        try {
            testApplication {
                application { astridModule(postgresConfig, stores) }
                val cookie = createSession()
                val owner = client.get("/api/v1/practice-context") {
                    trustedHost()
                    cookie(SESSION_COOKIE, cookie)
                }.json().string("ownerSessionId")
                val captured = fixture.mismatchingGuided()
                assertTrue(captured.hasNormalFingerprintMismatch())
                val practiceId = UUID.randomUUID().toString()
                val created = createPractice(cookie, owner, practiceId, captured)
                assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())
                val generatedIds = created.json().getValue("items").jsonArray.map {
                    it.jsonObject.string("exerciseId")
                }
                installCapturedGuidedPlan(owner, practiceId, generatedIds, captured)

                val support = openIntroduction(cookie, owner, practiceId)
                assertEquals(HttpStatusCode.OK, support.status, support.bodyAsText())
                val exerciseId = captured.publicExerciseIds.first()
                val graded = submit(
                    cookie,
                    AttemptRequest(
                        UUID.randomUUID().toString(),
                        exerciseId,
                        AttemptAnswerDto.Matching(captured.correctPairs),
                        false,
                    ),
                )

                assertEquals(HttpStatusCode.OK, graded.status, graded.bodyAsText())
                val reopened = client.get("/api/v1/practice-sessions/$practiceId") {
                    trustedHost()
                    cookie(SESSION_COOKIE, cookie)
                }
                assertEquals(HttpStatusCode.OK, reopened.status, reopened.bodyAsText())
                val reopenedItems = reopened.json().getValue("items").jsonArray.map { it.jsonObject }
                assertEquals(captured.publicExerciseIds, reopenedItems.map { it.string("exerciseId") })
                assertEquals(1, reopenedItems.first().getValue("attemptCount").jsonPrimitive.content.toInt())
                assertCapturedGuidedBytesUnchanged(owner, practiceId, captured)
            }
        } finally {
            stores.close()
        }
    }

    @Test
    fun `released board grades through the real postgres store without rewriting its saved bytes`() {
        val postgresConfig = postgresConfig()
        migrate(postgresConfig)
        val stores = PostgresStores(Database.connect(postgresConfig), wireJson)
        try {
            testApplication {
                application { astridModule(postgresConfig, stores) }
                val cookie = createSession()
                val captured = fixture.mismatchingStandalone()
                assertTrue(captured.hasNormalFingerprintMismatch())
                val exerciseId = createVocabularyExercise(cookie, captured.request).json().string("id")
                val stored = captured.stored(exerciseId)
                val presentationPayload = wireJson.encodeToString(ExerciseResponse.serializer(), stored.presentation)
                PostgresTestDatabase.connect().use { connection ->
                    connection.prepareStatement(
                        """UPDATE exercises SET seed = ?, generation_index = ?, content_revision = ?,
                            generator_revision = ?, presentation_payload = ?, reproduction_fingerprint = ?
                            WHERE id = ?""".trimIndent(),
                    ).use { statement ->
                        statement.setInt(1, stored.seed)
                        statement.setInt(2, stored.index)
                        statement.setString(3, stored.contentRevision)
                        statement.setString(4, stored.generatorRevision)
                        statement.setString(5, presentationPayload)
                        statement.setString(6, stored.reproductionFingerprint)
                        statement.setObject(7, stored.id)
                        assertEquals(1, statement.executeUpdate())
                    }
                }

                val attemptId = UUID.randomUUID().toString()
                val response = submit(
                    cookie,
                    AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Matching(captured.correctPairs), false),
                )

                assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
                val reopened = client.get("/api/v1/attempts/$attemptId") {
                    trustedHost()
                    cookie(SESSION_COOKIE, cookie)
                }
                assertEquals(response.bodyAsText(), reopened.bodyAsText())
                PostgresTestDatabase.connect().use { connection ->
                    connection.prepareStatement(
                        "SELECT presentation_payload, reproduction_fingerprint FROM exercises WHERE id = ?",
                    ).use { statement ->
                        statement.setObject(1, stored.id)
                        statement.executeQuery().use { rows ->
                            assertTrue(rows.next())
                            assertEquals(presentationPayload, rows.getString(1))
                            assertEquals(stored.reproductionFingerprint, rows.getString(2))
                        }
                    }
                }
            }
        } finally {
            stores.close()
        }
    }

    private suspend fun ApplicationTestBuilder.createPractice(
        cookieValue: String,
        owner: String,
        practiceId: String,
        captured: CapturedLegacyMatching,
    ) = client.post("/api/v1/practice-sessions") {
        trustedHost()
        cookie(SESSION_COOKIE, cookieValue)
        header("X-Astrid-Client", "playground")
        header("X-Astrid-Practice-Owner", owner)
        contentType(ContentType.Application.Json)
        setBody(
            """{"practiceSessionId":"$practiceId","focusId":"${captured.focusId}","planRevision":"${captured.planRevision}","seed":${captured.seed}}""",
        )
    }

    private suspend fun ApplicationTestBuilder.openIntroduction(cookieValue: String, owner: String, practiceId: String) =
        client.post("/api/v1/practice-sessions/$practiceId/support") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            header("X-Astrid-Practice-Owner", owner)
            contentType(ContentType.Application.Json)
            setBody("""{"kind":"introduction"}""")
        }

    private fun installCapturedGuidedPlan(
        owner: String,
        practiceId: String,
        generatedIds: List<String>,
        captured: CapturedLegacyMatching,
    ) {
        assertEquals(generatedIds.size, captured.publicExerciseIds.size)
        assertEquals(captured.presentation.id, captured.publicExerciseIds.first())
        PostgresTestDatabase.connect().use { connection ->
            connection.autoCommit = false
            generatedIds.zip(captured.publicExerciseIds).forEachIndexed { position, (generatedId, capturedId) ->
                val insert = if (position == 0) {
                    """INSERT INTO exercises
                        (id, session_id, format_id, target_skill_id, seed, generation_index, content_revision,
                         generator_revision, presentation_payload, reproduction_fingerprint, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent()
                } else {
                    """INSERT INTO exercises
                        (id, session_id, format_id, target_skill_id, seed, generation_index, content_revision,
                         generator_revision, presentation_payload, reproduction_fingerprint, created_at)
                        SELECT ?, session_id, format_id, target_skill_id, seed, generation_index, content_revision,
                               generator_revision, presentation_payload, reproduction_fingerprint, created_at
                        FROM exercises WHERE id = ?""".trimIndent()
                }
                connection.prepareStatement(insert).use { statement ->
                    statement.setObject(1, UUID.fromString(capturedId))
                    if (position == 0) {
                        statement.setObject(2, UUID.fromString(owner))
                        statement.setString(3, captured.request.formatId)
                        statement.setString(4, captured.request.targetSkillId)
                        statement.setInt(5, captured.request.seed)
                        statement.setInt(6, captured.request.index)
                        statement.setString(7, captured.request.contentRevision)
                        statement.setString(8, captured.request.generatorRevision)
                        statement.setString(9, captured.presentationRaw)
                        statement.setString(10, captured.fingerprint)
                        statement.setLong(11, 1)
                    } else {
                        statement.setObject(2, UUID.fromString(generatedId))
                    }
                    assertEquals(1, statement.executeUpdate())
                }
                connection.prepareStatement(
                    """UPDATE practice_items SET exercise_id = ?
                        WHERE owner_session_id = ? AND practice_session_id = ? AND position = ?""".trimIndent(),
                ).use { statement ->
                    statement.setObject(1, UUID.fromString(capturedId))
                    statement.setObject(2, UUID.fromString(owner))
                    statement.setObject(3, UUID.fromString(practiceId))
                    statement.setInt(4, position)
                    assertEquals(1, statement.executeUpdate())
                }
                connection.prepareStatement("DELETE FROM exercises WHERE id = ?").use { statement ->
                    statement.setObject(1, UUID.fromString(generatedId))
                    assertEquals(1, statement.executeUpdate())
                }
            }
            connection.prepareStatement(
                """UPDATE practice_sessions SET plan_payload = ?
                    WHERE owner_session_id = ? AND practice_session_id = ?""".trimIndent(),
            ).use { statement ->
                statement.setString(1, checkNotNull(captured.planPayloadRaw))
                statement.setObject(2, UUID.fromString(owner))
                statement.setObject(3, UUID.fromString(practiceId))
                assertEquals(1, statement.executeUpdate())
            }
            connection.commit()
        }
    }

    private fun assertCapturedGuidedBytesUnchanged(
        owner: String,
        practiceId: String,
        captured: CapturedLegacyMatching,
    ) {
        PostgresTestDatabase.connect().use { connection ->
            connection.prepareStatement(
                """SELECT p.plan_payload, e.presentation_payload, e.reproduction_fingerprint
                    FROM practice_sessions p
                    JOIN practice_items i ON i.owner_session_id = p.owner_session_id
                        AND i.practice_session_id = p.practice_session_id AND i.position = 0
                    JOIN exercises e ON e.session_id = i.owner_session_id AND e.id = i.exercise_id
                    WHERE p.owner_session_id = ? AND p.practice_session_id = ?""".trimIndent(),
            ).use { statement ->
                statement.setObject(1, UUID.fromString(owner))
                statement.setObject(2, UUID.fromString(practiceId))
                statement.executeQuery().use { rows ->
                    assertTrue(rows.next())
                    assertEquals(captured.planPayloadRaw, rows.getString(1))
                    assertEquals(captured.presentationRaw, rows.getString(2))
                    assertEquals(captured.fingerprint, rows.getString(3))
                }
            }
        }
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

    private suspend fun ApplicationTestBuilder.createVocabularyExercise(cookieValue: String, request: GenerationRequest) =
        client.post("/api/v1/exercises") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(
                """{"formatId":"${request.formatId}","seed":${request.seed},"index":${request.index},"contentRevision":"${request.contentRevision}","generatorRevision":"${request.generatorRevision}"}""",
            )
        }

    private suspend fun ApplicationTestBuilder.submit(cookieValue: String, request: AttemptRequest) =
        client.post("/api/v1/attempts") {
            trustedHost()
            cookie(SESSION_COOKIE, cookieValue)
            header("X-Astrid-Client", "playground")
            contentType(ContentType.Application.Json)
            setBody(wireJson.encodeToString(request))
        }

    private suspend fun io.ktor.client.statement.HttpResponse.json() =
        wireJson.parseToJsonElement(bodyAsText()).jsonObject

    private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content

    private fun io.ktor.client.request.HttpRequestBuilder.trustedHost() {
        header(HttpHeaders.Host, "localhost:8088")
    }

    private fun config() = ServerConfig(
        true, false, null, false, false,
        setOf("http://localhost:8088"), setOf("localhost:8088"), 60_000, null,
        "jdbc:postgresql://unused", "r2dbc:postgresql://unused", "unused", "unused", 2,
    )

    private fun postgresConfig() = PostgresTestDatabase.config().copy(
        developmentMode = true,
        allowedOrigins = setOf("http://localhost:8088"),
        allowedHosts = setOf("localhost:8088"),
    )

    private companion object {
        val fixture = LegacyFixture.load()
    }
}

private data class CapturedLegacyMatching(
    val process: String,
    val request: GenerationRequest,
    val presentation: ExerciseResponse,
    val fingerprint: String,
    val correctPairs: List<MatchingPairDto>,
    val presentationRaw: String,
    val focusId: String? = null,
    val planRevision: String? = null,
    val seed: Int = request.seed,
    val planPayloadRaw: String? = null,
    val publicExerciseIds: List<String> = emptyList(),
) {
    val coordinate = "$process seed=${request.seed} index=${request.index}"

    fun stored(id: String = presentation.id) = StoredExercise(
        UUID.fromString(id),
        UUID(0, 0),
        request.formatId,
        request.targetSkillId,
        request.seed,
        request.index,
        request.contentRevision,
        request.generatorRevision,
        presentation.copy(id = id),
        fingerprint,
    )

    fun hasNormalFingerprintMismatch() =
        PilotEngine().generate(request).reproductionFingerprint() != fingerprint
}

private data class LegacyFixture(
    val standalone: List<CapturedLegacyMatching>,
    val guided: List<CapturedLegacyMatching>,
) {
    fun mismatchingStandalone() = standalone.first { it.correctPairs.isNotEmpty() && it.hasNormalFingerprintMismatch() }

    fun mismatchingGuided() = guided.first { it.correctPairs.isNotEmpty() && it.hasNormalFingerprintMismatch() }

    companion object {
        fun load(): LegacyFixture {
            val resource = checkNotNull(LegacyVocabularyMatchingRecoveryTest::class.java.getResource(
                "/legacy-restore/legacy-matching-source-3c22cb6.json",
            ))
            val bytes = resource.readBytes()
            check(bytes.sha256() == FIXTURE_SHA)
            val root = wireJson.parseToJsonElement(bytes.decodeToString()).jsonObject
            check(root.getValue("schemaVersion").jsonPrimitive.content == "1")
            check(root.getValue("sourceSha").jsonPrimitive.content == SOURCE_SHA)
            check(root.getValue("imageId").jsonPrimitive.content == IMAGE_ID)
            val provenanceResource = checkNotNull(LegacyVocabularyMatchingRecoveryTest::class.java.getResource(
                "/legacy-restore/legacy-matching-source-3c22cb6.provenance.json",
            ))
            val provenance = wireJson.parseToJsonElement(provenanceResource.readText()).jsonObject
            check(provenance.keys == setOf(
                "schemaVersion", "description", "sourceSha", "imageId", "standaloneCoordinatesPerProcess",
                "guidedPendingProfiles", "fixtureSha256",
            ))
            check(provenance.getValue("schemaVersion").jsonPrimitive.content == "1")
            check(provenance.string("description") == DESCRIPTION)
            check(provenance.string("sourceSha") == SOURCE_SHA)
            check(provenance.string("imageId") == IMAGE_ID)
            check(provenance.string("fixtureSha256") == FIXTURE_SHA)
            check(provenance.getValue("standaloneCoordinatesPerProcess").jsonPrimitive.content == "12")
            check(provenance.getValue("guidedPendingProfiles").jsonArray.map { it.jsonPrimitive.content } == listOf(
                "vocabulary-p1-s2", "vocabulary-p1-s3", "vocabulary-p1-s4", "vocabulary-p1-s5",
            ))
            return LegacyFixture(
                root.getValue("standalone").jsonArray.map { entry -> captured(entry.jsonObject) },
                root.getValue("guidedPending").jsonArray.map { entry ->
                    val value = entry.jsonObject
                    captured(
                        value,
                        requestKey = "firstItemGenerationRequest",
                        presentationKey = "firstItemPresentationPayload",
                        fingerprintKey = "firstItemReproductionFingerprint",
                        inspectorKey = "inspectorResponse",
                    ).copy(
                        process = "guided-released",
                        focusId = value.string("focusId"),
                        planRevision = value.string("planRevision"),
                        seed = value.getValue("seed").jsonPrimitive.content.toInt(),
                        planPayloadRaw = value.string("planPayloadRaw"),
                        publicExerciseIds = value.getValue("publicState").jsonObject.getValue("items").jsonArray.map {
                            it.jsonObject.string("exerciseId")
                        },
                    )
                },
            )
        }

        private fun captured(
            value: JsonObject,
            requestKey: String = "generationRequest",
            presentationKey: String = "presentationPayload",
            fingerprintKey: String = "reproductionFingerprint",
            inspectorKey: String = "inspectorResponse",
        ): CapturedLegacyMatching {
            val request = value.getValue(requestKey).jsonObject
            val inspector = value[inspectorKey]?.let { if (it.toString() == "null") null else it.jsonObject }
            val pairs = inspector?.getValue("vocabularySource")?.jsonObject?.getValue("matchingPairs")?.jsonArray
                ?.map { pair ->
                    pair.jsonObject.let { MatchingPairDto(it.string("leftId"), it.string("rightId")) }
                }.orEmpty()
            val presentationRaw = value.string(presentationKey)
            return CapturedLegacyMatching(
                process = value["process"]?.jsonPrimitive?.content ?: "guided-released",
                request = GenerationRequest(
                    request.string("formatId"),
                    request["targetSkillId"]?.jsonPrimitive?.contentOrNull,
                    request.getValue("seed").jsonPrimitive.content.toInt(),
                    request.getValue("index").jsonPrimitive.content.toInt(),
                    request.string("contentRevision"),
                    request.string("generatorRevision"),
                ),
                presentation = wireJson.decodeFromString(presentationRaw),
                fingerprint = value.string(fingerprintKey),
                correctPairs = pairs,
                presentationRaw = presentationRaw,
            )
        }

        private fun JsonObject.string(name: String) = getValue(name).jsonPrimitive.content

        private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
            .joinToString("") { "%02x".format(it) }

        private const val SOURCE_SHA = "3c22cb645c2bea45c7969e93f7f3088734f67ce0"
        private const val IMAGE_ID =
            "sha256:469111118540d81a292fabb19f809d4985cabfd993e5a8aba7be8a74b515f9e4"
        private const val FIXTURE_SHA = "5f9e73c94f0489c998face3a5aa487a5139e4af0d09dc090fdb99ce8faba541f"
        private const val DESCRIPTION =
            "Exact released-process pilot-17/generator-17 E033 public, request and fingerprint evidence from main plus " +
                "full public/inspector evidence from isolated QA, with guided plans; authentication and owner/practice " +
                "session identifiers removed."
    }
}
