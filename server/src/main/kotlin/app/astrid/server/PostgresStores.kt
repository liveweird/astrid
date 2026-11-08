package app.astrid.server

import app.astrid.server.exercises.PostgresExerciseStore
import app.astrid.server.infra.db.Database
import app.astrid.server.plugins.Readiness
import app.astrid.server.practice.PostgresPracticeHook
import app.astrid.server.practice.PostgresPracticeStore
import app.astrid.server.session.PostgresSessionStore
import kotlinx.serialization.json.Json

/**
 * The Postgres composition: one [Database] shared by every store. The attempt write reaches practice state through
 * [PostgresPracticeHook], which runs inside the attempt's own transaction, so `exercises/` never imports `practice/`.
 */
class PostgresStores(
    private val database: Database,
    json: Json,
    clock: () -> Long = System::currentTimeMillis,
) : Stores {
    private val sessionStore = PostgresSessionStore(database, clock)
    override val sessions = sessionStore
    override val exercises = PostgresExerciseStore(database, json, PostgresPracticeHook(json), clock)
    override val practice = PostgresPracticeStore(database, json, clock)
    override val readiness = Readiness(sessionStore::ready)

    fun close() = database.close()
}
