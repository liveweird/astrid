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

internal object CumulativeReviewTwoWireCompatibilityFixture {
    private val ownerId = uuid(30_000)
    private val engine = PilotEngine()
    private val practicePlanner = PracticePlanner(engine)

    fun rows(): List<String> = buildList {
        SEEDS.forEachIndexed { seedIndex, seed ->
            val request = PracticeSessionRequest(
                uuid(30_100 + seedIndex).toString(), "cumulative-review", "cumulative-review-2", seed,
            )
            val created = try {
                requireNotNull(history().toCumulativeReview(request, ownerId, engine))
                    .deterministic(31_000 + seedIndex * 20)
            } catch (failure: NoSuchElementException) {
                add(row(
                    "CREATE_FAILURE", seed.toString(),
                    hash(wireJson.encodeToString(request), failure::class.qualifiedName.orEmpty(), failure.message.orEmpty()),
                ))
                return@forEachIndexed
            }
            val payload = wireJson.encodeToString(created.toPayload())
            val restored = wireJson.decodeFromString<PracticePlanPayload>(payload)
            val initial = stored(request, created)
            val taught = initial.copy(introductionOpened = true)
            val attempted = taught.withSyntheticAttempt()
            val attempt = attempted.items.first().attempts.single()
            add(row("CREATE", seed.toString(), hash(wireJson.encodeToString(request), payload)))
            add(row("PRIVATE_PLAN", seed.toString(), hash(payload)))
            add(row("PAYLOAD_ROUND_TRIP", seed.toString(), hash(payload, wireJson.encodeToString(restored))))
            add(row("ITEM_POLICIES", seed.toString(), hash(wireJson.encodeToString(created.toPayload().items))))
            add(row("INITIAL", seed.toString(), hash(wireJson.encodeToString(initial.toResponse()))))
            add(row("TAUGHT", seed.toString(), hash(wireJson.encodeToString(taught.toResponse()))))
            add(row("ATTEMPTED", seed.toString(), hash(wireJson.encodeToString(attempted.toResponse()))))
            add(row("SAVED_ATTEMPT_REPLAY", seed.toString(), hash(
                wireJson.encodeToString(AttemptRequest(
                    attempt.response.attemptId, attempted.items.first().exercise.id.toString(), attempt.answer, false,
                )),
                wireJson.encodeToString(attempt.response), wireJson.encodeToString(attempt.context),
            )))
        }
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int) = buildString {
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=0.22.0")
        appendLine("captureDate=$CAPTURE_DATE")
        appendLine("projectionSchema=cumulative-review-two-server-wire-v1")
        appendLine("planRevision=cumulative-review-2")
        appendLine("sourceFamilies=${SOURCE_PLANS.size}")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=Published cumulative-review-2 exact request and private plan/item-policy payloads over all 26 " +
                "source families, plus synthetic initial/taught/attempted public wire and saved request/response/context replay.",
        )
    }

    fun samples(): List<Pair<String, String>> = SEEDS.filterNot { it == Int.MAX_VALUE }.mapIndexed { index, seed ->
        val request = PracticeSessionRequest(uuid(34_000 + index).toString(), "cumulative-review", "cumulative-review-2", seed)
        val created = requireNotNull(history().toCumulativeReview(request, ownerId, engine)).deterministic(35_000 + index * 20)
        wireJson.encodeToString(created.toPayload()) to wireJson.encodeToString(stored(request, created).toResponse())
    }

    private fun history() = CumulativeReviewHistory(
        SOURCE_PLANS.mapIndexed { index, source -> sourceSession(source, index) },
    )

    private fun sourceSession(source: Pair<String, String>, index: Int): StoredPracticeSession {
        val seed = index + 31
        val created = practicePlanner.plan(source.first, source.second, seed).toNewPracticeSession(ownerId)
            .deterministic(36_000 + index * 20)
        return StoredPracticeSession(
            uuid(37_000 + index), ownerId, 9_999_999, source.first, created.title, source.second, seed,
            created.contentRevision, created.generatorRevision, created.evaluationRevision, created.items.size, true,
            created.prerequisiteSkillIds, created.introduction, created.items.map { it.stored() }, created.followUp,
            created.sourceExposure, created.introductionExposureKeys, created.introductionRequired,
            created.completionRuleId, created.exposurePolicyId, created.cumulativeReview, 100L + index,
        )
    }

    private fun stored(request: PracticeSessionRequest, session: NewPracticeSession) = StoredPracticeSession(
        UUID.fromString(request.practiceSessionId), ownerId, 9_999_999, request.focusId, session.title,
        request.planRevision, request.seed, session.contentRevision, session.generatorRevision, session.evaluationRevision,
        0, false, session.prerequisiteSkillIds, session.introduction, session.items.map { it.stored() }, session.followUp,
        session.sourceExposure, session.introductionExposureKeys, session.introductionRequired,
        session.completionRuleId, session.exposurePolicyId, session.cumulativeReview, 500,
    )

    private fun app.astrid.server.practice.NewPracticeItem.stored() = StoredPracticeItem(
        position, exercise, targetLabel, answerExposureKeys, promptExposureKeys, lessonExposureKeys, hintExposureKeys,
        feedbackExposureKeys, false, false, emptyList(), assessmentMode, assessments, targetScopeId, readingRole,
        matchingTarget, policy,
    )

    private fun StoredPracticeSession.withSyntheticAttempt(): StoredPracticeSession {
        val first = items.first()
        val response = AttemptResponse(
            uuid(38_000).toString(), first.exercise.id.toString(), "unevaluable", "unknown", emptyList(),
            "This answer is outside the reviewed material.", "", evaluationRevision,
            false, 1, 600, responseConstraintStatus = "not-met",
        )
        val context = PracticeAttemptContextDto(true, false, false, emptyList(), sourceExposure)
        val attempt = StoredPracticeAttempt(response, context, AttemptAnswerDto.Text("ukendt svar"))
        return copy(items = items.map { if (it.position == 0) it.copy(attempts = listOf(attempt)) else it })
    }

    private fun NewPracticeSession.deterministic(base: Int) = copy(items = items.mapIndexed { index, item ->
        val id = uuid(base + index)
        item.copy(exercise = item.exercise.copy(id = id, presentation = item.exercise.presentation.copy(id = id.toString())))
    })

    private fun hash(vararg fields: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray()
            digest.update(ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        return digest.digest().toHex()
    }
    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex() = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }
    private fun uuid(value: Int) = UUID.fromString("00000000-0000-0000-0000-${value.toString().padStart(12, '0')}")

    const val RELEASE_SOURCE_COMMIT = "844b88af33cc729e095d443adb72fcd72cd17534"
    const val CAPTURE_DATE = "2026-11-13"
    val SOURCE_PLANS = CumulativeReviewOneWireCompatibilityFixture.SOURCE_PLANS + (1..6).map {
        "vocabulary-p2-s$it" to "vocabulary-p2-s$it-1"
    }
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}
