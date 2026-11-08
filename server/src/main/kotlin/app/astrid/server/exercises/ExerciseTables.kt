@file:Suppress("MagicNumber") // SQL column widths.

package app.astrid.server.exercises

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

internal object Exercises : Table("exercises") {
    val id = javaUUID("id")
    val sessionId = javaUUID("session_id")
    val formatId = varchar("format_id", 32)
    val targetSkillId = varchar("target_skill_id", 32).nullable()
    val seed = integer("seed")
    val generationIndex = integer("generation_index")
    val contentRevision = varchar("content_revision", 64)
    val generatorRevision = varchar("generator_revision", 64)
    val presentationPayload = text("presentation_payload")
    val reproductionFingerprint = char("reproduction_fingerprint", 64)
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(id)
}

internal object Attempts : Table("attempts") {
    val sessionId = javaUUID("session_id")
    val attemptId = javaUUID("attempt_id")
    val exerciseId = javaUUID("exercise_id")
    val requestPayload = text("request_payload")
    val rawAnswer = text("raw_answer")
    val responsePayload = text("response_payload")
    val contentRevision = varchar("content_revision", 64)
    val generatorRevision = varchar("generator_revision", 64)
    val evaluationRevision = varchar("evaluation_revision", 64)
    val retryNumber = integer("retry_number")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(sessionId, attemptId)
}
