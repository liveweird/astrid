package app.astrid.server

import app.astrid.server.authz.BadRequest
import app.astrid.server.authz.Conflict
import app.astrid.server.authz.NotFound
import app.astrid.server.authz.Unauthorized
import app.astrid.server.infra.config.ServerConfig
import app.astrid.server.infra.json.wireJson
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.GapSelectionDto
import app.astrid.server.exercises.MatchingPairDto
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
import java.util.concurrent.atomic.AtomicLong
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
    fun `cumulative review uses only taught lessons persists mixed revisions and replays exactly`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val auth = authenticate()
        val empty = client.get("/api/v1/practice-review-context") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }
        assertEquals(HttpStatusCode.OK, empty.status)
        assertEquals(
            0,
            wireJson.parseToJsonElement(empty.bodyAsText()).jsonObject.getValue("itemCount").jsonPrimitive.content.toInt(),
        )

        val sourceId = UUID.randomUUID().toString()
        assertEquals(
            HttpStatusCode.Created,
            create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"$sourceId","focusId":"module-one-singular","planRevision":"module-one-singular-1","seed":7}""",
            ).status,
        )
        assertEquals(HttpStatusCode.OK, support(auth, sourceId, """{"kind":"introduction"}""").status)
        val context = client.get("/api/v1/practice-review-context") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }
        val contextJson = wireJson.parseToJsonElement(context.bodyAsText()).jsonObject
        assertEquals(6, contextJson.getValue("itemCount").jsonPrimitive.content.toInt())
        assertEquals(1, contextJson.getValue("sourceLessonCount").jsonPrimitive.content.toInt())

        val reviewId = UUID.randomUUID().toString()
        val body =
            """{"practiceSessionId":"$reviewId","focusId":"cumulative-review","planRevision":"cumulative-review-1","seed":17}"""
        val created = create(auth.cookie, auth.ownerId, body)
        assertEquals(HttpStatusCode.Created, created.status)
        val createdText = created.bodyAsText()
        val review = wireJson.parseToJsonElement(createdText).jsonObject
        assertEquals("per-item", review.getValue("contentRevision").jsonPrimitive.content)
        assertTrue(review.getValue("items").jsonArray.all { item ->
            item.jsonObject.getValue("revisions").jsonObject.getValue("contentRevision").jsonPrimitive.content == "pilot-8"
        })
        assertEquals(HttpStatusCode.OK, create(auth.cookie, auth.ownerId, body).status)
        assertEquals(createdText, create(auth.cookie, auth.ownerId, body).bodyAsText())
        assertEquals(
            HttpStatusCode.Conflict,
            create(auth.cookie, auth.ownerId, body.replace("\"seed\":17", "\"seed\":18")).status,
        )
    }

    @Test
    fun `cumulative eligibility excludes untaught and legacy reviews while replay survives source drift`() = testApplication {
        val stores = InMemoryStores()
        application { astridModule(config(), stores) }
        val auth = authenticate()
        suspend fun createSource(focus: String, revision: String, seed: Int, introduced: Boolean): Pair<String, String> {
            val id = UUID.randomUUID().toString()
            val response = create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"$id","focusId":"$focus","planRevision":"$revision","seed":$seed}""",
            )
            assertEquals(HttpStatusCode.Created, response.status)
            if (introduced) assertEquals(HttpStatusCode.OK, support(auth, id, """{"kind":"introduction"}""").status)
            val exerciseId = wireJson.parseToJsonElement(response.bodyAsText()).jsonObject
                .getValue("currentExercise").jsonObject.getValue("id").jsonPrimitive.content
            return id to exerciseId
        }

        createSource("module-one-groups", "module-one-groups-1", 3, introduced = false)
        createSource("present-verbs", "present-verbs-1", 5, introduced = true)
        val (eligibleId, eligibleExerciseId) =
            createSource("module-one-singular", "module-one-singular-1", 7, introduced = true)
        val context = client.get("/api/v1/practice-review-context") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }
        val contextBody = wireJson.parseToJsonElement(context.bodyAsText()).jsonObject
        assertEquals(1, contextBody.getValue("sourceLessonCount").jsonPrimitive.content.toInt())
        assertEquals(6, contextBody.getValue("itemCount").jsonPrimitive.content.toInt())

        val reviewId = UUID.randomUUID().toString()
        val body =
            """{"practiceSessionId":"$reviewId","focusId":"cumulative-review","planRevision":"cumulative-review-1","seed":13}"""
        val created = create(auth.cookie, auth.ownerId, body)
        assertEquals(HttpStatusCode.Created, created.status)
        val replay = create(auth.cookie, auth.ownerId, body)
        assertEquals(HttpStatusCode.OK, replay.status)
        assertEquals(created.bodyAsText(), replay.bodyAsText())

        assertEquals(HttpStatusCode.OK, support(auth, reviewId, """{"kind":"introduction"}""").status)
        stores.tamperFingerprint(UUID.fromString(eligibleExerciseId))
        assertEquals(
            HttpStatusCode.Conflict,
            client.get("/api/v1/practice-review-context") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
            }.status,
        )
        assertEquals(
            HttpStatusCode.Conflict,
            create(
                auth.cookie,
                auth.ownerId,
                body.replace(reviewId, UUID.randomUUID().toString()),
            ).status,
        )
        assertNotNull(stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(eligibleId)))
    }

    @Test
    fun `cumulative source exposure freezes released support and mixed P1 formats only`() = testApplication {
        val stores = InMemoryStores()
        application { astridModule(config(), stores) }
        val auth = authenticate()
        suspend fun introduced(focus: String, revision: String, seed: Int): Pair<String, kotlinx.serialization.json.JsonObject> {
            val id = UUID.randomUUID().toString()
            val created = create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"$id","focusId":"$focus","planRevision":"$revision","seed":$seed}""",
            )
            assertEquals(HttpStatusCode.Created, created.status)
            assertEquals(HttpStatusCode.OK, support(auth, id, """{"kind":"introduction"}""").status)
            return id to wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        }

        val (grammarId, grammar) = introduced("module-one-singular", "module-one-singular-1", 7)
        assertEquals(HttpStatusCode.OK, support(auth, grammarId, """{"kind":"lesson","position":0}""").status)
        assertEquals(HttpStatusCode.OK, support(auth, grammarId, """{"kind":"hint","position":0}""").status)
        val grammarExercise = grammar.getValue("currentExercise").jsonObject
        val suppliedAnswer = grammarExercise.getValue("prompt").jsonObject.getValue("choices").jsonArray.first()
            .jsonPrimitive.content
        assertEquals(
            HttpStatusCode.OK,
            submit(
                auth.cookie,
                wireJson.encodeToString(
                    AttemptRequest(
                        UUID.randomUUID().toString(),
                        grammarExercise.getValue("id").jsonPrimitive.content,
                        AttemptAnswerDto.Text(suppliedAnswer),
                        false,
                    ),
                ),
            ).status,
        )
        val (matchingId, _) = introduced("vocabulary-p1-s2", "vocabulary-p1-s2-1", 19)
        val (readingId, _) = introduced("vocabulary-p1-s5", "vocabulary-p1-s5-1", 13)

        val mixed = (0..64).firstNotNullOfOrNull { seed ->
            val reviewId = UUID.randomUUID().toString()
            val created = create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"$reviewId","focusId":"cumulative-review","planRevision":"cumulative-review-1","seed":$seed}""",
            )
            assertEquals(HttpStatusCode.Created, created.status)
            val state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
            val formats = state.getValue("items").jsonArray
                .map { it.jsonObject.getValue("formatId").jsonPrimitive.content }.toSet()
            if ("E033" in formats && "E039" in formats) reviewId to formats else null
        }
        val (reviewId, formats) = assertNotNull(mixed)
        assertTrue("E033" in formats, formats.toString())
        assertTrue("E039" in formats, formats.toString())

        val stored = assertNotNull(
            stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(reviewId)),
        )
        assertTrue(stored.sourceExposure.isNotEmpty())
        assertTrue(stored.sourceExposure.all { it.practiceSessionId in setOf(grammarId, matchingId, readingId) })
        assertTrue(stored.sourceExposure.filter { it.source == "prompt" }.all { it.position == 0 })
        assertTrue(stored.sourceExposure.none { it.position > 0 && it.source == "prompt" })
        assertEquals(stored.sourceExposure.flatMap { it.keys }.distinct(), stored.sourceExposure.flatMap { it.keys })
        assertTrue(stored.sourceExposure.any { it.source == "introduction" })
        assertTrue(stored.sourceExposure.all { it.keys.isNotEmpty() })
        assertTrue(stored.sourceExposure.all {
            it.source in setOf("introduction", "prompt", "lesson", "hint", "feedback")
        })
    }

    @Test
    fun `cumulative context and creation reject an owner that expires after authentication`() = testApplication {
        val stores = InMemoryStores(clock = { Long.MAX_VALUE })
        application { astridModule(config(), stores, clock = { 0L }) }
        val auth = authenticate()

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.get("/api/v1/practice-review-context") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
            }.status,
        )
        assertEquals(
            HttpStatusCode.Unauthorized,
            create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"${UUID.randomUUID()}","focusId":"cumulative-review",""" +
                    """"planRevision":"cumulative-review-1","seed":0}""",
            ).status,
        )
    }

    @Test
    fun `vocabulary matching requires saved teaching validates the board and exposes pair outcomes`() = testApplication {
        val inspectorKey = "v".repeat(32)
        val stores = InMemoryStores()
        application { astridModule(config(inspectorKey), stores) }
        val auth = authenticate()
        val practiceId = UUID.randomUUID().toString()
        val created = create(
            auth.cookie,
            auth.ownerId,
            """{"practiceSessionId":"$practiceId","focusId":"vocabulary-p1-s4","planRevision":"vocabulary-p1-s4-1","seed":11}""",
        )
        assertEquals(HttpStatusCode.Created, created.status)
        var state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        val item = state.getValue("items").jsonArray.first().jsonObject
        assertEquals("E033", item.getValue("formatId").jsonPrimitive.content)
        assertEquals(3, item.getValue("assessments").jsonArray.size)
        assertEquals("meaning", item.getValue("matchingTarget").jsonObject.getValue("facet").jsonPrimitive.content)
        assertFalse(item.getValue("canAdvance").jsonPrimitive.content.toBoolean())
        assertEquals(
            "p1-drikke-present",
            state.getValue("items").jsonArray.single {
                it.jsonObject.getValue("formatId").jsonPrimitive.content == "E038"
            }.jsonObject.getValue("targetScopeId").jsonPrimitive.content,
        )
        val publicExercise = state.getValue("currentExercise").jsonObject
        assertFalse("vocabularySource" in publicExercise)
        val exerciseId = publicExercise.getValue("id").jsonPrimitive.content
        val source = client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
            header("X-Astrid-Inspector-Key", inspectorKey)
        }.bodyAsText().let { wireJson.parseToJsonElement(it).jsonObject.getValue("vocabularySource").jsonObject }
        val pairs = source.getValue("matchingPairs").jsonArray.map { value ->
            val pair = value.jsonObject
            MatchingPairDto(
                pair.getValue("leftId").jsonPrimitive.content,
                pair.getValue("rightId").jsonPrimitive.content,
            )
        }
        fun attemptBody(attemptId: String, selected: List<MatchingPairDto>) = wireJson.encodeToString(
            AttemptRequest(attemptId, exerciseId, AttemptAnswerDto.Matching(selected), false),
        )

        assertEquals(HttpStatusCode.Conflict, submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), pairs)).status)
        assertEquals(HttpStatusCode.OK, support(auth, practiceId, """{"kind":"introduction"}""").status)

        val partial = submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), pairs.dropLast(1)))
        assertEquals(HttpStatusCode.OK, partial.status)
        assertEquals(3, wireJson.parseToJsonElement(partial.bodyAsText()).jsonObject
            .getValue("matchingResults").jsonArray.size)
        state = wireJson.parseToJsonElement(getPractice(auth.cookie, practiceId).bodyAsText()).jsonObject
        assertFalse(state.getValue("items").jsonArray.first().jsonObject
            .getValue("canAdvance").jsonPrimitive.content.toBoolean())

        val malformedAttemptId = UUID.randomUUID().toString()
        val beforeMalformed = assertNotNull(
            stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(practiceId)),
        ).items.first().attempts.size
        assertEquals(
            HttpStatusCode.BadRequest,
            submit(
                auth.cookie,
                attemptBody(malformedAttemptId, pairs + MatchingPairDto("unknown", pairs.first().rightId)),
            ).status,
        )
        assertEquals(
            beforeMalformed,
            assertNotNull(
                stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(practiceId)),
            ).items.first().attempts.size,
        )
        assertEquals(
            HttpStatusCode.NotFound,
            client.get("/api/v1/attempts/$malformedAttemptId") {
                trustedHost()
                cookie("astrid_session", auth.cookie)
            }.status,
        )
        val beforeCorrect = assertNotNull(
            stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(practiceId)),
        ).items.first().attempts.size

        val correctAttemptId = UUID.randomUUID().toString()
        val correctBody = attemptBody(correctAttemptId, pairs)
        val correct = submit(auth.cookie, correctBody)
        assertEquals(HttpStatusCode.OK, correct.status)
        val correctText = correct.bodyAsText()
        assertEquals(correctText, submit(auth.cookie, correctBody).bodyAsText())
        assertEquals(
            HttpStatusCode.Conflict,
            submit(auth.cookie, attemptBody(correctAttemptId, pairs.reversed())).status,
        )
        state = wireJson.parseToJsonElement(getPractice(auth.cookie, practiceId).bodyAsText()).jsonObject
        assertTrue(state.getValue("items").jsonArray.first().jsonObject
            .getValue("canAdvance").jsonPrimitive.content.toBoolean())
        val storedAttempts = assertNotNull(
            stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(practiceId)),
        ).items.first().attempts
        assertEquals(beforeCorrect + 1, storedAttempts.size)
        assertEquals(HttpStatusCode.OK, advance(auth, practiceId, 0).status)

        val sessionThreeId = UUID.randomUUID().toString()
        val sessionThree = create(
            auth.cookie,
            auth.ownerId,
            """{"practiceSessionId":"$sessionThreeId","focusId":"vocabulary-p1-s3","planRevision":"vocabulary-p1-s3-1","seed":11}""",
        ).bodyAsText().let { wireJson.parseToJsonElement(it).jsonObject.getValue("items").jsonArray }
        assertEquals(
            setOf("p1-studere-present", "p1-subject-first-supplied"),
            sessionThree.mapNotNull { it.jsonObject["targetScopeId"]?.jsonPrimitive?.content }.toSet(),
        )
    }

    @Test
    fun `guided gaps preserve per-slot outcomes snapshots exposure and selected follow-ups`() = testApplication {
        val inspectorKey = "k".repeat(32)
        val stores = InMemoryStores()
        application { astridModule(config(inspectorKey), stores) }
        val auth = authenticate()
        val profiles = client.get("/api/v1/practice-profiles") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }.bodyAsText().let { wireJson.parseToJsonElement(it).jsonObject.getValue("profiles").jsonArray }
        assertEquals(23, profiles.size)
        val profile = profiles.single { it.jsonObject.getValue("focusId").jsonPrimitive.content == "verb-article-gaps" }.jsonObject
        assertEquals("verb-article-gaps-1", profile.getValue("planRevision").jsonPrimitive.content)
        assertEquals("Verbs and articles together", profile.getValue("title").jsonPrimitive.content)
        assertEquals(4, profile.getValue("itemCount").jsonPrimitive.content.toInt())

        val sourceId = UUID.randomUUID().toString()
        val body = guidedGapBody(sourceId)
        val created = create(auth.cookie, auth.ownerId, body)
        assertEquals(HttpStatusCode.Created, created.status)
        val createdText = created.bodyAsText()
        assertEquals(createdText, create(auth.cookie, auth.ownerId, body).bodyAsText())
        var state = wireJson.parseToJsonElement(createdText).jsonObject
        assertEquals("follow-up-3", state.getValue("followUpPlanRevision").jsonPrimitive.content)
        assertTrue(state.getValue("introduction").jsonArray.any { section ->
            section.jsonObject["bodyLanguage"]?.jsonPrimitive?.content == "da" &&
                section.jsonObject.getValue("body").jsonPrimitive.content == "Jeg læser en bog."
        })
        val firstItem = state.getValue("items").jsonArray.first().jsonObject
        val assessments = firstItem.getValue("assessments").jsonArray.map { it.jsonObject }
        assertEquals(listOf("g0", "g1"), assessments.map { it.getValue("slotId").jsonPrimitive.content })
        assertEquals(listOf("primary", "support"), assessments.map { it.getValue("assessmentRole").jsonPrimitive.content })
        assertTrue(assessments.all { "answerExposureKeys" !in it })
        assertEquals(HttpStatusCode.OK, support(auth, sourceId, """{"kind":"introduction"}""").status)

        repeat(4) { position ->
            val exercise = state.getValue("currentExercise").jsonObject
            val exerciseId = exercise.getValue("id").jsonPrimitive.content
            val expected = inspectGap(auth.cookie, exerciseId, inspectorKey)
            val choices = exercise.getValue("prompt").jsonObject.getValue("gapPrompt").jsonObject
                .getValue("slots").jsonArray.associate { value ->
                    val slot = value.jsonObject
                    slot.getValue("id").jsonPrimitive.content to slot.getValue("choices").jsonArray.map {
                        it.jsonObject.getValue("id").jsonPrimitive.content
                    }
                }
            if (position == 0) {
                listOf<Boolean?>(null, true, false).forEach { primary ->
                    listOf<Boolean?>(null, true, false).forEach { support ->
                        val selections = listOf("g0" to primary, "g1" to support).mapNotNull { (slotId, correct) ->
                            correct?.let {
                                val choice = expected.getValue(slotId)
                                val selected = if (it) choice else choices.getValue(slotId).single { candidate ->
                                    candidate != choice
                                }
                                GapSelectionDto(slotId, selected)
                            }
                        }
                        assertEquals(HttpStatusCode.OK, submitGaps(auth.cookie, exerciseId, selections).status)
                    }
                }
            } else {
                val correct = listOf("g0", "g1").map { GapSelectionDto(it, expected.getValue(it)) }
                assertEquals(HttpStatusCode.OK, submitGaps(auth.cookie, exerciseId, correct).status)
            }
            state = wireJson.parseToJsonElement(advance(auth, sourceId, position).bodyAsText()).jsonObject
        }
        assertEquals("completed", state.getValue("state").jsonPrimitive.content)
        val summary = state.getValue("summary").jsonObject
        assertEquals(12, summary.getValue("attemptCount").jsonPrimitive.content.toInt())
        val rows = summary.getValue("rows").jsonArray.map { it.jsonObject }
        assertEquals(setOf("primary", "support"), rows.map { it.getValue("assessmentRole").jsonPrimitive.content }.toSet())
        assertTrue(rows.all { it.getValue("plannedItems").jsonPrimitive.content.toInt() == 4 })
        assertTrue(rows.all { it.getValue("attemptCount").jsonPrimitive.content.toInt() == 12 })
        assertTrue(rows.all { it.getValue("firstOutcomes").jsonObject.getValue("unevaluable").jsonPrimitive.content.toInt() == 1 })
        assertTrue(rows.all { it.getValue("latestOutcomes").jsonObject.getValue("targetNotMet").jsonPrimitive.content.toInt() == 1 })
        val targetKeys = rows.map(::targetKey)
        val childId = UUID.randomUUID().toString()
        val child = followUp(auth, sourceId, followUpBody(childId, targetKeys, "follow-up-3"))
        assertEquals(HttpStatusCode.Created, child.status)
        val childState = wireJson.parseToJsonElement(child.bodyAsText()).jsonObject
        assertEquals(4, childState.getValue("itemCount").jsonPrimitive.content.toInt())
        assertEquals(setOf("E001", "E002"), childState.getValue("items").jsonArray.map {
            it.jsonObject.getValue("formatId").jsonPrimitive.content
        }.toSet())
        assertTrue(childState.getValue("items").jsonArray.all { it.jsonObject.getValue("assessments").jsonArray.size == 1 })
        val childStored = assertNotNull(
            stores.practice.findPracticeSession(UUID.fromString(auth.ownerId), UUID.fromString(childId)),
        )
        val sourceIntroduction = childStored.sourceExposure.single { it.source == "introduction" }
        assertEquals(
            setOf(
                "verb-infinitive:L011",
                "verb-present:L011",
                "article:L014",
                "headword:L014-S01",
                "grammar:G004",
                "grammar:G006",
            ),
            sourceIntroduction.keys.toSet(),
        )
    }

    @Test
    fun `practice profiles are authenticated stable and do not renew the session`() = testApplication {
        val now = AtomicLong(1_000)
        application { astridModule(config(), InMemoryStores(clock = now::get), clock = now::get) }
        val auth = authenticate()

        val profiles = client.get("/api/v1/practice-profiles") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }
        assertEquals(HttpStatusCode.OK, profiles.status)
        assertNull(profiles.headers[HttpHeaders.SetCookie])
        val payload = wireJson.parseToJsonElement(profiles.bodyAsText()).jsonObject
        assertEquals(auth.ownerId, payload.getValue("ownerSessionId").jsonPrimitive.content)
        val choices = payload.getValue("profiles").jsonArray
        assertEquals(
            listOf(
                "reading-basics/practice-1",
                "present-verbs/present-verbs-1",
                "verb-article-gaps/verb-article-gaps-1",
                "module-one-singular/module-one-singular-1",
                "module-one-groups/module-one-groups-1",
                "module-one-er/module-one-er-1",
                "module-one-things/module-one-things-1",
                "module-one-present/module-one-present-1",
                "module-one-order/module-one-order-1",
                "module-one-possession/module-one-possession-1",
                "module-one-reading-words/module-one-reading-words-1",
                "module-one-personal/module-one-personal-1",
                "module-one-ability/module-one-ability-1",
                "module-one-volition/module-one-volition-1",
                "module-one-obligation/module-one-obligation-1",
                "module-one-permission/module-one-permission-1",
                "module-one-circumstances/module-one-circumstances-1",
                "module-one-questions/module-one-questions-1",
                "vocabulary-p1-s1/vocabulary-p1-s1-2",
                "vocabulary-p1-s2/vocabulary-p1-s2-2",
                "vocabulary-p1-s3/vocabulary-p1-s3-2",
                "vocabulary-p1-s4/vocabulary-p1-s4-2",
                "vocabulary-p1-s5/vocabulary-p1-s5-2",
            ),
            choices.map { profile ->
                profile.jsonObject.let {
                    "${it.getValue("focusId").jsonPrimitive.content}/${it.getValue("planRevision").jsonPrimitive.content}"
                }
            },
        )
        assertEquals(listOf(8, 6, 4, 6, 6, 6, 4, 6, 6, 4, 4, 6, 4, 2, 2, 2, 4, 4, 4, 4, 5, 5, 5), choices.map {
            it.jsonObject.getValue("itemCount").jsonPrimitive.content.toInt()
        })

        now.set(61_001)
        val expired = client.get("/api/v1/practice-profiles") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }
        assertEquals(HttpStatusCode.Unauthorized, expired.status)
        assertNull(expired.headers[HttpHeaders.SetCookie])
    }

    @Test
    fun `every advertised and retained P1 plan creates with required teaching and rereads`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val auth = authenticate()
        val profiles = client.get("/api/v1/practice-profiles") {
            trustedHost()
            cookie("astrid_session", auth.cookie)
        }.bodyAsText().let { wireJson.parseToJsonElement(it).jsonObject.getValue("profiles").jsonArray }
            .map { it.jsonObject }
            .filter { it.getValue("focusId").jsonPrimitive.content.startsWith("vocabulary-p1-") }
        assertEquals(5, profiles.size)

        val pairs = profiles.map { profile ->
            profile.getValue("focusId").jsonPrimitive.content to
                profile.getValue("planRevision").jsonPrimitive.content
        } + (1..5).map { number -> "vocabulary-p1-s$number" to "vocabulary-p1-s$number-1" }
        pairs.forEachIndexed { index, (focus, revision) ->
            val id = UUID.randomUUID().toString()
            val created = create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"$id","focusId":"$focus","planRevision":"$revision","seed":${47 + index}}""",
            )
            assertEquals(HttpStatusCode.Created, created.status, "$focus/$revision: ${created.bodyAsText()}")
            val createdState = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
            assertEquals(revision, createdState.getValue("planRevision").jsonPrimitive.content)
            val pair = if (revision.endsWith("-2")) "18" else "17"
            assertEquals("pilot-$pair", createdState.getValue("contentRevision").jsonPrimitive.content)
            assertEquals("generator-$pair", createdState.getValue("generatorRevision").jsonPrimitive.content)
            assertEquals("evaluation-$pair", createdState.getValue("evaluationRevision").jsonPrimitive.content)
            assertTrue(createdState.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
            assertFalse(createdState.getValue("introductionOpened").jsonPrimitive.content.toBoolean())

            val restored = getPractice(auth.cookie, id)
            assertEquals(HttpStatusCode.OK, restored.status, "$focus/$revision: ${restored.bodyAsText()}")
            assertEquals(created.bodyAsText(), restored.bodyAsText())
        }
    }

    @Test
    fun `module one requires its introduction and exposes the server completion decision`() = testApplication {
        val inspectorKey = "m".repeat(32)
        application { astridModule(config(inspectorKey), InMemoryStores()) }
        val auth = authenticate()
        val practiceId = UUID.randomUUID().toString()
        val created = create(
            auth.cookie,
            auth.ownerId,
            """{"practiceSessionId":"$practiceId","focusId":"module-one-singular","planRevision":"module-one-singular-1","seed":7}""",
        )
        assertEquals(HttpStatusCode.Created, created.status)
        var state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        assertTrue(state.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
        val item = state.getValue("items").jsonArray.first().jsonObject
        assertEquals("human-singular", item.getValue("targetScopeId").jsonPrimitive.content)
        assertFalse(item.getValue("canAdvance").jsonPrimitive.content.toBoolean())
        val exercise = state.getValue("currentExercise").jsonObject
        val exerciseId = exercise.getValue("id").jsonPrimitive.content
        val expected = inspectModuleOne(auth.cookie, exerciseId, inspectorKey)

        assertEquals(HttpStatusCode.Conflict, submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), exerciseId, expected)).status)
        assertEquals(HttpStatusCode.OK, support(auth, practiceId, """{"kind":"introduction"}""").status)
        assertEquals(
            HttpStatusCode.BadRequest,
            submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), exerciseId, "not a supplied choice")).status,
        )
        assertEquals(HttpStatusCode.OK, submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), exerciseId, expected)).status)
        state = wireJson.parseToJsonElement(getPractice(auth.cookie, practiceId).bodyAsText()).jsonObject
        assertTrue(state.getValue("items").jsonArray.first().jsonObject.getValue("canAdvance").jsonPrimitive.content.toBoolean())
        var advanced = advance(auth, practiceId, 0)
        assertEquals(HttpStatusCode.OK, advanced.status)
        state = wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
        for (position in 1 until 6) {
            val current = state.getValue("currentExercise").jsonObject
            val currentId = current.getValue("id").jsonPrimitive.content
            val currentExpected = inspectModuleOne(auth.cookie, currentId, inspectorKey)
            assertEquals(
                HttpStatusCode.OK,
                submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), currentId, currentExpected)).status,
            )
            if (position == 4) {
                assertEquals(
                    HttpStatusCode.OK,
                    submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), currentId, "")).status,
                )
                val latestUnknown = wireJson.parseToJsonElement(getPractice(auth.cookie, practiceId).bodyAsText()).jsonObject
                assertFalse(
                    latestUnknown.getValue("items").jsonArray[position].jsonObject
                        .getValue("canAdvance").jsonPrimitive.content.toBoolean(),
                )
                assertEquals(HttpStatusCode.Conflict, advance(auth, practiceId, position).status)
                assertEquals(
                    HttpStatusCode.OK,
                    submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), currentId, currentExpected)).status,
                )
            }
            advanced = advance(auth, practiceId, position)
            assertEquals(HttpStatusCode.OK, advanced.status)
            state = wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
        }
        assertEquals("completed", state.getValue("state").jsonPrimitive.content)
        val completedItems = state.getValue("items").jsonArray.map { it.jsonObject }
        assertTrue(completedItems.first().getValue("lastAttemptContext").jsonObject.getValue("priorExposure").jsonArray.any {
            val exposure = it.jsonObject
            exposure.getValue("position").jsonPrimitive.content.toInt() == -1 &&
                exposure.getValue("source").jsonPrimitive.content == "introduction"
        })
        assertTrue(completedItems.drop(1).any { completed ->
            completed.getValue("lastAttemptContext").jsonObject.getValue("priorExposure").jsonArray.any {
                it.jsonObject.getValue("source").jsonPrimitive.content == "prompt"
            }
        })
        assertEquals(
            HttpStatusCode.Conflict,
            followUp(
                auth,
                practiceId,
                followUpBody(UUID.randomUUID().toString(), listOf("grammar:G003:human-singular:selection")),
            ).status,
        )
    }

    @Test
    fun `referring to things rejects invalid choices preserves replay and blocks on latest unknown`() = testApplication {
        val inspectorKey = "r".repeat(32)
        application { astridModule(config(inspectorKey), InMemoryStores()) }
        val auth = authenticate()
        val practiceId = UUID.randomUUID().toString()
        val created = create(
            auth.cookie,
            auth.ownerId,
            """{"practiceSessionId":"$practiceId","focusId":"module-one-things","planRevision":"module-one-things-1","seed":7}""",
        )
        assertEquals(HttpStatusCode.Created, created.status)
        var state = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        assertEquals(4, state.getValue("itemCount").jsonPrimitive.content.toInt())
        assertTrue(state.getValue("introductionRequired").jsonPrimitive.content.toBoolean())
        assertEquals(HttpStatusCode.OK, support(auth, practiceId, """{"kind":"introduction"}""").status)

        repeat(4) { position ->
            val exercise = state.getValue("currentExercise").jsonObject
            val exerciseId = exercise.getValue("id").jsonPrimitive.content
            val expected = inspectInanimateReference(auth.cookie, exerciseId, inspectorKey)
            if (position == 0) {
                val invalid = submit(
                    auth.cookie,
                    attemptBody(UUID.randomUUID().toString(), exerciseId, "not a displayed choice"),
                )
                assertEquals(HttpStatusCode.BadRequest, invalid.status)
                val unchanged = wireJson.parseToJsonElement(getPractice(auth.cookie, practiceId).bodyAsText()).jsonObject
                assertEquals(0, unchanged.getValue("items").jsonArray.first().jsonObject
                    .getValue("attemptCount").jsonPrimitive.content.toInt())
            }
            val attemptId = UUID.randomUUID().toString()
            val body = attemptBody(attemptId, exerciseId, expected)
            val accepted = submit(auth.cookie, body)
            assertEquals(HttpStatusCode.OK, accepted.status)
            assertEquals(accepted.bodyAsText(), submit(auth.cookie, body).bodyAsText())
            if (position == 2) {
                assertEquals(
                    HttpStatusCode.OK,
                    submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), exerciseId, "dem")).status,
                )
                val blocked = wireJson.parseToJsonElement(getPractice(auth.cookie, practiceId).bodyAsText()).jsonObject
                assertFalse(blocked.getValue("items").jsonArray[position].jsonObject
                    .getValue("canAdvance").jsonPrimitive.content.toBoolean())
                assertEquals(HttpStatusCode.Conflict, advance(auth, practiceId, position).status)
                assertEquals(
                    HttpStatusCode.OK,
                    submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), exerciseId, expected)).status,
                )
            }
            val advanced = advance(auth, practiceId, position)
            assertEquals(HttpStatusCode.OK, advanced.status)
            state = wireJson.parseToJsonElement(advanced.bodyAsText()).jsonObject
        }
        assertEquals("completed", state.getValue("state").jsonPrimitive.content)
        val rows = state.getValue("summary").jsonObject.getValue("rows").jsonArray.map { it.jsonObject }
        assertEquals(setOf("selection", "recall"), rows.map { it.getValue("assessmentMode").jsonPrimitive.content }.toSet())
        assertTrue(rows.all {
            it.getValue("targetScopeId").jsonPrimitive.content == "inanimate-singular-subject"
        })
    }

    @Test
    fun `present verb mode metadata is frozen and follow-up 2 is source constrained`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
        val auth = authenticate()
        assertEquals(
            HttpStatusCode.Conflict,
            create(
                auth.cookie,
                auth.ownerId,
                """{"practiceSessionId":"${UUID.randomUUID()}","focusId":"present-verbs","planRevision":"practice-1","seed":0}""",
            ).status,
        )
        val sourceId = UUID.randomUUID().toString()
        val created = create(auth.cookie, auth.ownerId, presentVerbBody(sourceId))
        assertEquals(HttpStatusCode.Created, created.status)
        val initial = wireJson.parseToJsonElement(created.bodyAsText()).jsonObject
        assertEquals("follow-up-2", initial.getValue("followUpPlanRevision").jsonPrimitive.content)
        val modes = initial.getValue("items").jsonArray.map {
            it.jsonObject.getValue("assessmentMode").jsonPrimitive.content
        }
        assertEquals(listOf("selection", "recall", "repair", "selection", "recall", "repair"), modes)

        val firstExercise = initial.getValue("currentExercise").jsonObject
        assertEquals(HttpStatusCode.OK, submit(auth.cookie, attemptBodyFor(firstExercise)).status)
        val recallState = wireJson.parseToJsonElement(advance(auth, sourceId, 0).bodyAsText()).jsonObject
        val recallExercise = recallState.getValue("currentExercise").jsonObject
        val recallExerciseId = recallExercise.getValue("id").jsonPrimitive.content
        val targetExposureKey = presentVerbTarget(recallExercise).first
        val incorrect = submit(auth.cookie, attemptBody(UUID.randomUUID().toString(), recallExerciseId))
        assertEquals(HttpStatusCode.OK, incorrect.status)
        val afterIncorrect = wireJson.parseToJsonElement(getPractice(auth.cookie, sourceId).bodyAsText()).jsonObject
        assertFalse(targetExposureKey in itemExposureKeys(afterIncorrect, 1))

        val correct = submit(
            auth.cookie,
            attemptBody(UUID.randomUUID().toString(), recallExerciseId, presentVerbTarget(recallExercise).second),
        )
        assertEquals(
            "met",
            wireJson.parseToJsonElement(correct.bodyAsText()).jsonObject
                .getValue("targetSatisfaction").jsonPrimitive.content,
        )
        assertEquals(HttpStatusCode.OK, submit(auth.cookie, attemptBodyFor(recallExercise)).status)
        val afterMet = wireJson.parseToJsonElement(getPractice(auth.cookie, sourceId).bodyAsText()).jsonObject
        assertTrue(targetExposureKey in itemExposureKeys(afterMet, 1))

        val completed = completePractice(auth, advance(auth, sourceId, 1).bodyAsText())
        val targetKeys = completed.getValue("summary").jsonObject.getValue("rows").jsonArray.map(::targetKey)
        assertEquals(setOf("grammar:G004:selection", "grammar:G004:recall", "grammar:G004:repair"), targetKeys.toSet())
        val childId = UUID.randomUUID().toString()
        val childBody = followUpBody(childId, targetKeys.take(1), "follow-up-2")
        val child = followUp(auth, sourceId, childBody)
        assertEquals(HttpStatusCode.Created, child.status)
        val childText = child.bodyAsText()
        assertEquals("follow-up-2", wireJson.parseToJsonElement(childText).jsonObject
            .getValue("followUpPlanRevision").jsonPrimitive.content)
        val childReplay = followUp(auth, sourceId, childBody)
        assertEquals(HttpStatusCode.OK, childReplay.status)
        assertEquals(childText, childReplay.bodyAsText())
        assertEquals(
            HttpStatusCode.Conflict,
            followUp(
                auth,
                sourceId,
                followUpBody(UUID.randomUUID().toString(), listOf("grammar:G004:unknown"), "follow-up-2"),
            ).status,
        )

        val legacyId = UUID.randomUUID().toString()
        val legacy = create(auth.cookie, auth.ownerId, createBody(legacyId))
        val completedLegacy = completePractice(auth, legacy.bodyAsText())
        val legacyTarget = targetKey(completedLegacy.getValue("summary").jsonObject.getValue("rows").jsonArray.first())
        assertEquals(
            HttpStatusCode.Conflict,
            followUp(auth, legacyId, followUpBody(UUID.randomUUID().toString(), listOf(legacyTarget), "follow-up-2")).status,
        )
        assertEquals(HttpStatusCode.Conflict, followUp(auth, legacyId, childBody).status)
    }

    @Test
    fun `completed practice creates a selected follow-up with frozen lineage and exposure`() = testApplication {
        application { astridModule(config(), InMemoryStores()) }
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
        application { astridModule(config(), InMemoryStores()) }
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
        application { astridModule(config(), InMemoryStores()) }
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
        application { astridModule(config(), InMemoryStores()) }
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
            astridModule(config(), InMemoryStores(), engine, evaluator = { exercise, answer ->
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

    private fun config(inspectorKey: String? = null) = ServerConfig(
        developmentMode = true,
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

    private fun createBody(id: String) =
        """{"practiceSessionId":"$id","focusId":"reading-basics","planRevision":"practice-1","seed":20261107}"""

    private fun attemptBody(attemptId: String, exerciseId: String) =
        attemptBody(attemptId, exerciseId, "wrong")

    private fun attemptBody(attemptId: String, exerciseId: String, answer: String) =
        """{"attemptId":"$attemptId","exerciseId":"$exerciseId","answer":{"kind":"text","text":"$answer"},"hintUsed":false}"""

    private fun followUpBody(id: String, targets: List<String>, revision: String = "follow-up-1") =
        """{"practiceSessionId":"$id","planRevision":"$revision","seed":417,"targetKeys":${wireJson.encodeToString(targets)}}"""

    private fun presentVerbBody(id: String) =
        """{"practiceSessionId":"$id","focusId":"present-verbs","planRevision":"present-verbs-1","seed":20261107}"""

    private fun guidedGapBody(id: String) =
        """{"practiceSessionId":"$id","focusId":"verb-article-gaps","planRevision":"verb-article-gaps-1","seed":20261109}"""

    private suspend fun ApplicationTestBuilder.inspectGap(cookie: String, exerciseId: String, key: String): Map<String, String> =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Inspector-Key", key)
        }.bodyAsText().let { text ->
            wireJson.parseToJsonElement(text).jsonObject.getValue("gapSource").jsonObject.getValue("slots").jsonArray.associate {
                val slot = it.jsonObject
                slot.getValue("slotId").jsonPrimitive.content to slot.getValue("expectedChoiceId").jsonPrimitive.content
            }
        }

    private suspend fun ApplicationTestBuilder.inspectModuleOne(cookie: String, exerciseId: String, key: String): String =
        client.get("/api/v1/dev/exercises/$exerciseId") {
            trustedHost()
            cookie("astrid_session", cookie)
            header("X-Astrid-Inspector-Key", key)
        }.bodyAsText().let { text ->
            wireJson.parseToJsonElement(text).jsonObject.getValue("moduleOneSource").jsonObject
                .getValue("expectedForm").jsonPrimitive.content
        }

    private suspend fun ApplicationTestBuilder.inspectInanimateReference(
        cookie: String,
        exerciseId: String,
        key: String,
    ): String = client.get("/api/v1/dev/exercises/$exerciseId") {
        trustedHost()
        cookie("astrid_session", cookie)
        header("X-Astrid-Inspector-Key", key)
    }.bodyAsText().let { text ->
        wireJson.parseToJsonElement(text).jsonObject.getValue("inanimateReferenceSource").jsonObject
            .getValue("expectedForm").jsonPrimitive.content
    }

    private suspend fun ApplicationTestBuilder.submitGaps(
        cookie: String,
        exerciseId: String,
        selections: List<GapSelectionDto>,
    ) = submit(
        cookie,
        wireJson.encodeToString(
            AttemptRequest(UUID.randomUUID().toString(), exerciseId, AttemptAnswerDto.Gaps(selections), false),
        ),
    )

    private fun targetKey(row: kotlinx.serialization.json.JsonElement): String {
        val value = row.jsonObject
        return value["targetSkillId"]?.jsonPrimitive?.content?.let { skill ->
            value["assessmentMode"]?.jsonPrimitive?.content?.let { "grammar:$skill:$it" } ?: "grammar:$skill"
        }
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

    private fun itemExposureKeys(state: kotlinx.serialization.json.JsonObject, position: Int): List<String> =
        state.getValue("items").jsonArray[position].jsonObject.getValue("lastAttemptContext").jsonObject
            .getValue("priorExposure").jsonArray.flatMap { exposure ->
                exposure.jsonObject.getValue("keys").jsonArray.map { it.jsonPrimitive.content }
            }

    private fun presentVerbTarget(exercise: kotlinx.serialization.json.JsonObject): Pair<String, String> {
        val instruction = exercise.getValue("instruction").jsonPrimitive.content
        return listOf(
            Triple("have", "L009", "har"),
            Triple("være", "L010", "er"),
            Triple("læse", "L011", "læser"),
            Triple("spise", "L012", "spiser"),
            Triple("sove", "L013", "sover"),
        ).single { it.first in instruction }.let { (_, entryId, present) ->
            "verb-present:$entryId" to present
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
