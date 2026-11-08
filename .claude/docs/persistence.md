# Persistence

PostgreSQL 18 through Flyway (schema) and Exposed R2DBC (stores). Learner evidence — exercises, raw answers, graded responses,
practice runs — is the product's memory: it is never rewritten, never regraded, and only ever grows. Compatibility of what is
stored is in `compatibility.md`; the outage/recovery evidence is in
[runtime recovery](../../docs/knowledge/runtime-recovery.md) and [server outage boundary](../../docs/knowledge/server-outage-boundary.md).

## Migrations (Flyway, forward-only)

- `server/src/main/resources/db/migration/V<n>__description.sql`, applied at startup by `infra/db/Flyway.kt` (`migrate`) before the
  listener binds. Today V1 (sessions, exercises, attempts), V2 (nullable `exercises.target_skill_id` for lexical targets),
  V3 (practice sessions, items, attempt context).
- **An applied migration's bytes are immutable, comments included**: Flyway validates stored checksums on every boot, so any edit makes
  every existing database refuse to start. `MigrationChecksumTest` pins the CRC32 of each file and requires the file set to equal the
  pinned map. Add a migration, never edit one; add its `"V<n>__….sql" to <checksum>` line in the same commit (compute with the
  test's algorithm, or read `flyway_schema_history.checksum` after a first local run). If the test fails on an existing line,
  revert the file — never update the pin.
- No `down` migrations. Make additive, backward-readable changes (nullable column, new table); a destructive or rewriting
  migration needs an explicit plan and a verified backup first. Rollback after V2/V3 writes needs a compatible reader
  (`vocabulary-assessment.md` "rollback").
- Keep a migration fast: startup budgets assume a small pilot (below).
- Schema conventions: UUID ids, epoch-millisecond `BIGINT` timestamps from the injected clock, `TEXT` JSON payloads written with
  `wireJson`, composite keys scoped by owner session (`(session_id, attempt_id)`, `(owner_session_id, practice_session_id)`),
  `ON DELETE CASCADE` from `development_sessions`, `CHECK` constraints for bounds (`cursor BETWEEN 0 AND 8`).

## Stores (Exposed R2DBC)

- One interface per feature (`SessionStore`, `ExerciseStore`, `PracticeStore`), one `Postgres*Store` class each, internal Exposed
  `Table` objects in `<feature>/*Tables.kt` (`Sessions` lives beside `PostgresSessionStore`). `PostgresStores` shares one
  `Database` (R2DBC pool: `POSTGRES_POOL_MAX_SIZE` 1–100, default 10; acquire timeout 10 s; `defaultMaxAttempts = 1`, so Exposed
  never silently retries a write). Stores take an injectable `clock: () -> Long`; never call `System.currentTimeMillis()` in a store body.
- Every store method runs in its own `suspendTransaction(database.r2dbc) { … }`. Do all reads and writes of one decision inside it.
- `ready()` (`/api/v1/ready`) probes the session table; a database failure is "not ready", never an unhandled 500 (cancellation still propagates).
- Each new store method also gets an `InMemoryStores` implementation and a `StoreContractTest` case (`testing.md`).

## Transactions, the practice hook and lock ordering

- **The attempt write is one transaction** (`PostgresExerciseStore.writeAttemptWithPractice`): lock the session row → return the stored result for a
  known `attemptId` → load the exercise → `AttemptPracticeHook.bind` → count retries → run the grading callback → insert the attempt →
  `AttemptPracticeHook.record`. A failing or cancelled callback rolls everything back with no retry-number gap
  (`PostgresRepositoryTest`).
- **The hook joins the caller's transaction. A hook implementation must never open its own transaction** (`suspendTransaction`,
  a second connection, a new pool lease): it runs in the same coroutine, so it already sits in the attempt's transaction, and a
  nested one would commit separately, not see uncommitted rows, and can exhaust the pool. The attempt and its practice context commit or
  roll back together. `PostgresPracticeHook` reads and inserts with plain Exposed statements only.
- Practice writes reuse exercises code the same way: `PostgresPracticeStore` inserts a run's exercises through
  `insertExerciseInTransaction` inside the run's transaction.
- **Lock order**: the first statement of every attempt or practice write is `lockSession(sessionId)` (`SELECT … FOR UPDATE` on the
  `development_sessions` row; `session/PostgresSessionStore.kt`), so all writes of one session serialize; row locks on practice/exercise rows
  come only after it. New write paths take that lock first, lock only one session, and check its boolean where a vanished session is reachable.
  Reads (`find*`) take no lock.

## Retry-safe identity and deduplication

- Attempts are idempotent on `(session_id, attempt_id)`: the same id with a byte-identical `request_payload` returns the stored response
  (`AttemptWriteResult.Existing`: no regrade, no new retry number); the same id with a different payload is `PayloadConflict` (409). Retry
  numbers are `count(attempts for session+exercise) + 1`, assigned under the lock, so concurrent distinct attempts get consecutive numbers.
- Practice runs and follow-ups are idempotent on `(owner_session_id, practice_session_id)` with their stored `request_payload`
  (`Existing` / `RequestConflict`); the follow-up identity includes the source run id.
- The comparison is on the server-serialized request (`wireJson.encodeToString(request)`). **Changing how a request DTO serializes (field
  order, a default that is now encoded, a new always-encoded field) changes the identity of every stored row and turns old replays into 409s.**
  New optional request fields are `@EncodeDefault(NEVER)` (`compatibility.md`).
- The browser stores the original identity and serialized body in its outbox and replays those exact bytes (`web/CLAUDE.md`); the server
  never regrades a stored attempt.
- Ownership is part of every key: a row is found only with its owner session id, so another session gets 404, not 403 (a wrong `X-Astrid-Practice-Owner` header is a 409 before any store call).

## Startup budget

`migrate` retries the first connection `POSTGRES_CONNECT_RETRIES` times (default 10, validated 0–15) with waits capped at
`CONNECT_RETRY_MAX_INTERVAL_SECONDS` = 8; the migration data source forces a 3 s TCP connect and 5 s login deadline *after* applying the URL, so
URL options cannot lengthen them. `python3 scripts/check-startup-budget.py` (run by `scripts/check.sh` and CI) computes the worst-case bootstrap
envelope `(retries + 1) × login + Σ min(2^a, interval)` plus a 30 s margin and asserts both the Kubernetes `startupProbe`
((`failureThreshold` − 1) × `periodSeconds`, `k8s/app.yaml`) and the Dockerfile `HEALTHCHECK --start-period` exceed it. Today: 183 s + 30 s
against 237 s and 240 s. Change a bound, a probe or a slow migration → update all of them together; never disable a probe to fit.

## Backups and operations

- **Back up before a migration, a promotion or any destructive operation**: a custom-format `pg_dump` kept private (mode 0600) and verified
  readable, as in the delivery records (`app-versioning.md`, `runtime-recovery.md`). No backup policy or retention exists; this is a private preview.
- Compose keeps data in the `postgres-data` volume mounted at `/var/lib/postgresql` (PostgreSQL 18 versioned layout). **Never run
  `docker compose down -v` to "reset" a stack that holds data you care about**; `down` is safe. Kubernetes: `postgres` runs one replica with
  `Recreate` on a 1 Gi PVC; rotate the Secret but keep the password the volume was initialized with.
- Promote app-only (same database process and mounts) and confirm schema versions in `flyway_schema_history` before and after.
