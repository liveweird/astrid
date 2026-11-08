package app.astrid.server

import app.astrid.server.exercises.ExerciseStore
import app.astrid.server.plugins.Readiness
import app.astrid.server.practice.PracticeStore
import app.astrid.server.session.SessionStore

/** Every persistence port the API needs; Postgres in production ([PostgresStores]), an in-memory fake in HTTP tests. */
interface Stores {
    val sessions: SessionStore
    val exercises: ExerciseStore
    val practice: PracticeStore
    val readiness: Readiness
}
