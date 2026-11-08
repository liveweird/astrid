@file:Suppress("MagicNumber") // SQL column widths.

package app.astrid.server.practice

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID

internal object PracticeSessions : Table("practice_sessions") {
    val ownerSessionId = javaUUID("owner_session_id")
    val practiceSessionId = javaUUID("practice_session_id")
    val requestPayload = text("request_payload")
    val planPayload = text("plan_payload")
    val focusId = varchar("focus_id", 32)
    val planRevision = varchar("plan_revision", 64)
    val seed = integer("seed")
    val sessionExpiresAt = long("session_expires_at")
    val cursor = integer("cursor")
    val introductionOpened = bool("introduction_opened")
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(ownerSessionId, practiceSessionId)
}

internal object PracticeItems : Table("practice_items") {
    val ownerSessionId = javaUUID("owner_session_id")
    val practiceSessionId = javaUUID("practice_session_id")
    val position = integer("position")
    val exerciseId = javaUUID("exercise_id")
    val lessonOpened = bool("lesson_opened")
    val hintOpened = bool("hint_opened")
    override val primaryKey = PrimaryKey(ownerSessionId, practiceSessionId, position)
}

internal object PracticeAttemptContexts : Table("practice_attempt_context") {
    val ownerSessionId = javaUUID("owner_session_id")
    val attemptId = javaUUID("attempt_id")
    val practiceSessionId = javaUUID("practice_session_id")
    val position = integer("position")
    val contextPayload = text("context_payload")
    val createdAt = long("created_at")
    override val primaryKey = PrimaryKey(ownerSessionId, attemptId)
}
