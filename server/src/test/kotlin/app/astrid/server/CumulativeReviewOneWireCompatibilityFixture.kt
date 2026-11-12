package app.astrid.server

import app.astrid.engine.PilotEngine
import app.astrid.engine.PracticePlanner
import app.astrid.server.exercises.AttemptAnswerDto
import app.astrid.server.exercises.AttemptRequest
import app.astrid.server.exercises.AttemptResponse
import app.astrid.server.exercises.PracticeAttemptContextDto
import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.CumulativeReviewHistory
import app.astrid.server.practice.NewPracticeSession
import app.astrid.server.practice.PracticePlanPayload
import app.astrid.server.practice.PracticeSessionRequest
import app.astrid.server.practice.StoredPracticeAttempt
import app.astrid.server.practice.StoredPracticeItem
import app.astrid.server.practice.StoredPracticeSession
import app.astrid.server.practice.toCumulativeReview
import app.astrid.server.practice.toNewPracticeSession
import app.astrid.server.practice.toPayload
import app.astrid.server.practice.toResponse
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

internal object CumulativeReviewOneWireCompatibilityFixture {
    private val ownerId = uuid(7_000)
    private val engine = PilotEngine()
    private val practicePlanner = PracticePlanner(engine)

    fun rows(): List<String> = buildList {
        SEEDS.forEachIndexed { seedIndex, seed ->
            val history = history()
            val request = PracticeSessionRequest(uuid(7_100 + seedIndex).toString(), "cumulative-review", "cumulative-review-1", seed)
            val created = requireNotNull(history.toCumulativeReview(request, ownerId, engine)).deterministic(8_000 + seedIndex * 20)
            val payloadJson = wireJson.encodeToString(created.toPayload())
            val restored = wireJson.decodeFromString<PracticePlanPayload>(payloadJson)
            val initial = stored(request, created)
            val taught = initial.copy(introductionOpened = true)
            val attempted = withUnknownAttempt(taught)
            val attempt = attempted.items.first().attempts.single()
            val attemptRequest = AttemptRequest(
                attempt.response.attemptId,
                attempted.items.first().exercise.id.toString(),
                attempt.answer,
                false,
            )
            add(row("CREATE", seed.toString(), hash(wireJson.encodeToString(request), payloadJson)))
            add(row("PAYLOAD_ROUND_TRIP", seed.toString(), hash(payloadJson, wireJson.encodeToString(restored))))
            add(row("INITIAL", seed.toString(), hash(wireJson.encodeToString(initial.toResponse()))))
            add(row("TAUGHT", seed.toString(), hash(wireJson.encodeToString(taught.toResponse()))))
            add(row("ATTEMPTED", seed.toString(), hash(wireJson.encodeToString(attempted.toResponse()))))
            add(
                row(
                    "SAVED_ATTEMPT_REPLAY",
                    seed.toString(),
                    hash(
                        wireJson.encodeToString(attemptRequest),
                        wireJson.encodeToString(attempt.response),
                        wireJson.encodeToString(attempt.context),
                    ),
                ),
            )
        }
        val nullPolicyHistory = history().let { value ->
            val first = value.sessions.first()
            value.copy(
                sessions = listOf(
                    first.copy(
                        items = listOf(first.items.first().copy(policy = app.astrid.server.practice.PracticeItemPolicyPayload(null, null))),
                    ),
                ),
            )
        }
        val nullRequest = PracticeSessionRequest(uuid(7_200).toString(), "cumulative-review", "cumulative-review-1", 0)
        val nullCreated = requireNotNull(nullPolicyHistory.toCumulativeReview(nullRequest, ownerId, engine)).deterministic(8_500)
        add(row("EXPLICIT_NULL_POLICY", hash(wireJson.encodeToString(nullCreated.toPayload()))))
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=0.21.0")
        appendLine("captureDate=2026-11-12")
        appendLine("projectionSchema=cumulative-review-one-server-wire-v1")
        appendLine("planRevision=cumulative-review-1")
        appendLine("sourceFamilies=${SOURCE_PLANS.size}")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=Published cumulative-review-1 request and private plan payloads at four boundary seeds, " +
                "all twenty taught source families, deterministic selection witnesses, exact public initial/taught/attempted " +
                "wire, per-item revisions, cross-run exposure, saved attempt request/response/context replay and explicit null policy.",
        )
    }

    private fun history() = CumulativeReviewHistory(
        SOURCE_PLANS.mapIndexed { index, source -> sourceSession(source, index) },
    )

    private fun sourceSession(source: Pair<String, String>, index: Int): StoredPracticeSession {
        val seed = index + 31
        val plan = practicePlanner.plan(source.first, source.second, seed)
        val created = plan.toNewPracticeSession(ownerId).deterministic(9_000 + index * 20)
        return StoredPracticeSession(
            id = uuid(10_000 + index), ownerSessionId = ownerId, sessionExpiresAt = 9_999_999,
            focusId = source.first, title = created.title, planRevision = source.second, seed = seed,
            contentRevision = created.contentRevision, generatorRevision = created.generatorRevision,
            evaluationRevision = created.evaluationRevision, cursor = created.items.size, introductionOpened = true,
            prerequisiteSkillIds = created.prerequisiteSkillIds, introduction = created.introduction,
            items = created.items.map { it.stored() }, sourceExposure = created.sourceExposure,
            introductionExposureKeys = created.introductionExposureKeys, introductionRequired = created.introductionRequired,
            completionRuleId = created.completionRuleId, exposurePolicyId = created.exposurePolicyId, createdAt = 100L + index,
        )
    }

    private fun stored(request: PracticeSessionRequest, session: NewPracticeSession) = StoredPracticeSession(
        id = UUID.fromString(request.practiceSessionId), ownerSessionId = ownerId, sessionExpiresAt = 9_999_999,
        focusId = request.focusId, title = session.title, planRevision = request.planRevision, seed = request.seed,
        contentRevision = session.contentRevision, generatorRevision = session.generatorRevision,
        evaluationRevision = session.evaluationRevision, cursor = 0, introductionOpened = false,
        prerequisiteSkillIds = session.prerequisiteSkillIds, introduction = session.introduction,
        items = session.items.map { it.stored() }, sourceExposure = session.sourceExposure,
        introductionExposureKeys = session.introductionExposureKeys, introductionRequired = session.introductionRequired,
        completionRuleId = session.completionRuleId, exposurePolicyId = session.exposurePolicyId,
        cumulativeReview = session.cumulativeReview, createdAt = 500,
    )

    private fun app.astrid.server.practice.NewPracticeItem.stored() = StoredPracticeItem(
        position, exercise, targetLabel, answerExposureKeys, promptExposureKeys, lessonExposureKeys, hintExposureKeys,
        feedbackExposureKeys, false, false, emptyList(), assessmentMode, assessments, targetScopeId, readingRole,
        matchingTarget, policy,
    )

    private fun withUnknownAttempt(session: StoredPracticeSession): StoredPracticeSession {
        val first = session.items.first()
        val attemptId = uuid(11_000)
        val response = AttemptResponse(
            attemptId.toString(), first.exercise.id.toString(), "unevaluable", "unknown", emptyList(),
            "This answer is outside the reviewed material.", "",
            requireNotNull(engine.evaluationRevision(first.exercise.contentRevision, first.exercise.generatorRevision)),
            false, 1, 600, responseConstraintStatus = "not-met",
        )
        val context = PracticeAttemptContextDto(true, false, false, emptyList(), session.sourceExposure)
        val attempt = StoredPracticeAttempt(response, context, AttemptAnswerDto.Text("ukendt svar"))
        return session.copy(items = session.items.map { if (it.position == 0) it.copy(attempts = listOf(attempt)) else it })
    }

    private fun NewPracticeSession.deterministic(base: Int) = copy(
        items = items.mapIndexed { index, item ->
            val id = uuid(base + index)
            item.copy(exercise = item.exercise.copy(id = id, presentation = item.exercise.presentation.copy(id = id.toString())))
        },
    )

    private fun hash(vararg fields: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray()
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array()); digest.update(bytes)
        }
        return digest.digest().toHex()
    }

    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private fun uuid(value: Int) = UUID.fromString("00000000-0000-0000-0000-${value.toString().padStart(12, '0')}")

    const val RELEASE_SOURCE_COMMIT = "eab86c6ddd44175e2d9b6dde7e2283f30943800e"
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
    val SOURCE_PLANS = listOf(
        "module-one-singular" to "module-one-singular-1", "module-one-groups" to "module-one-groups-1",
        "module-one-er" to "module-one-er-1", "module-one-things" to "module-one-things-1",
        "module-one-present" to "module-one-present-1", "module-one-order" to "module-one-order-1",
        "module-one-possession" to "module-one-possession-1", "module-one-reading-words" to "module-one-reading-words-1",
        "module-one-personal" to "module-one-personal-1", "module-one-ability" to "module-one-ability-1",
        "module-one-volition" to "module-one-volition-1", "module-one-obligation" to "module-one-obligation-1",
        "module-one-permission" to "module-one-permission-1", "module-one-circumstances" to "module-one-circumstances-1",
        "module-one-questions" to "module-one-questions-1", "vocabulary-p1-s1" to "vocabulary-p1-s1-2",
        "vocabulary-p1-s2" to "vocabulary-p1-s2-2", "vocabulary-p1-s3" to "vocabulary-p1-s3-2",
        "vocabulary-p1-s4" to "vocabulary-p1-s4-2", "vocabulary-p1-s5" to "vocabulary-p1-s5-2",
    )
}
