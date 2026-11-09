# Testing

The pyramid, bottom to top, and the binding rules. Commands run from the repo root with absolute paths (the shell cwd drifts).
Whole-suite order of operations is `scripts/check.sh` (`ci.md`). Do not report the suite as passing with Docker tests skipped.

## Engine (`engine/`) — fast, no Docker

Plain `kotlin.test` on JUnit (`./gradlew :engine:test`). Covers content validation (`PilotContentValidatorTest`), generation,
evaluation, ordering, vocabulary, writing/reading, practice planners/summaries, and the **golden compatibility tests**
(`Pilot*CompatibilityGoldenTest`) that replay `engine/src/test/resources/golden/*.tsv` byte for byte — see `compatibility.md`, which
governs those files. Rules: add a regression case for every grading fix; assert behaviour, not implementation; generation is
deterministic, so assert exact sentences for fixed `(seed, index)`.

`./gradlew :engine:localeCompatibilityTest` re-runs the same suite in a fresh JVM with `user.language=ar user.country=EG` (so a
`String.format`/`lowercase()`/digit-locale bug fails); it rides `check`.

## Server (`server/`) — three layers

Tests are `kotlin.test` on JUnit in `server/src/test/kotlin/app/astrid/server/` (all in the root package; `src/testFixtures/kotlin`
is compiled into the test source set).

- **HTTP tests, Docker-free** — `testApplication { application { astridModule(config, InMemoryStores(clock), …) } }`
  (`ApplicationTest`, `ApplicationHardeningTest`, `PracticeApplicationTest`, `VocabularyApplicationTest`,
  `RecallRepairApplicationTest`, `WritingReadingApplicationTest`, `StaticCompressionTest`, `CallLoggingTest`). Failure
  injection goes through the `evaluator` lambda and the `clock` parameter of `astridModule`, never through the store. The fake also
  offers `tamperRevision`/`tamperFingerprint` for regeneration-drift tests.
- **`StoreContractTest`** — abstract, run twice: `InMemoryStoreContractTest` and `PostgresStoreContractTest`. The fake and the real
  stores share one contract (session expiry, owner isolation, attempt replay/`PayloadConflict`/`ExerciseMissing`/
  `PracticePositionConflict`, practice create `Existing`/`RequestConflict`, follow-up `SourceMissing`/`SourceActive`, support/advance
  `Missing`/`Conflict`). When a route learns a new store outcome, add the case here **before** relying on it in an HTTP test, so the
  fake cannot drift. Concurrency, rollback and cross-pool serialization stay in `PostgresRepositoryTest` (two pools, real locks).
- **Postgres** — one Testcontainers PostgreSQL per test JVM via `PostgresTestDatabase` (lazy `object`; image pinned by tag+digest and
  checked against compose/k8s by `scripts/check-toolchain.py`; Ryuk removes it at exit). Tests call `migrate(...)` themselves, so one can
  `createDatabase("scratch")` and stop at an older migration (`V2 preserves V1 …`). Raw JDBC goes through `PostgresTestDatabase.connect()`.
  Never start a second container per class. Docker with OrbStack: `export DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock`.

Guard tests that must stay green and are never loosened to fit a change:

- `OpenApiContractTest` — validates every observed response (status, body, `application/problem+json`, `Location` on 201) against
  `documentation.yaml` with swagger-request-validator; request validation is deliberately IGNORE (decoding is tested elsewhere), but path
  and method existence are errors. Also pins `info.version == APP_VERSION` and the closed oneOf correlations (format ↔ target ↔
  revision pair). A new operation lands with its statuses exercised here.
- `MigrationChecksumTest` — Flyway CRC32 of every `V*.sql`; the file set must equal the pinned map (`persistence.md`).
- `PackageBoundaryTest` — layering (`conventions.md`); includes self-tests of its own regex scan.
- `PilotThreeWireCompatibilityTest`, `PilotFourWireCompatibilityTest`, `PilotFiveWireCompatibilityTest`, `PilotSixWireCompatibilityTest`, `PilotSevenWireCompatibilityTest`, `PublishedFingerprintCompatibilityTest`, `PracticeWireCompatibilityTest` — frozen wire and
  fingerprint evidence (`compatibility.md`).
- `ServerConfigTest`, `DatabaseBootstrapTest` — env validation, bounded migration connect/login deadlines.

## Web (`web/`)

Vitest + happy-dom + Testing Library, tests colocated (`Foo.test.tsx` beside `Foo.tsx`). `renderWithProviders` (`src/test/render.tsx`) wraps
Mantine (`env="test"`, else Select/Popover dropdowns stay hidden), a fresh `QueryClient` with the app's defaults and a real
`BrowserRouter` — position the URL with `window.history.replaceState(...)` before rendering and assert `window.location` afterwards.
Fixtures live in `src/test/playgroundFixtures.tsx` and `practiceFixtures.ts`; stub `fetch` per test, unstub in `afterEach`. Test names state
behaviour. `npm --prefix web run test:coverage` is the gate. The recovery outbox (`practiceRecovery.test.ts`, `PracticeReplay.test.tsx`)
must keep asserting replay with the **stored identity and serialized body**.

## E2E (`e2e/`)

Playwright (`@playwright/test` 1.64) against the assembled compose stack, **Chromium and WebKit**, `workers: 1`, `fullyParallel: false`,
`retries: 1` only when `CI` is set, base URL `E2E_BASE_URL` (default `http://127.0.0.1:8088`). Locate by role/label/text — the app has no
`data-testid`. Axe (`@axe-core/playwright`, WCAG 2 A/AA tags) runs in `accessibility.spec.ts` and the journeys that render new states; a new
UI state needs a scan. `e2e/support/` holds shared page helpers; `recovery.spec.ts` simulates lost/slow responses by mocking
`application/problem+json` and aborted fetches. Setup: `npm --prefix /Users/sebastian/Sources/astrid/e2e ci && npx playwright install chromium webkit`
(from `e2e/`), stack up with `docker compose --env-file .env.example up -d --wait`, run `npm --prefix …/e2e test`. Gates without a
stack: `npm run lint`, `npm run knip`, `npm run typecheck`, `npm run check:scenarios`.

**Scenario files** (`e2e/scenarios/`, format in its `README.md`; ported from Flow): every `tests/<name>.spec.ts` has a `scenarios/<name>.md`
with one `## Scenario:` heading per `test(` call, equal to the title's source text, and a new or behaviourally changed test lands with its
scenario in the **same commit**. `npm run check:scenarios` enforces the parity in both directions (missing file, title without heading,
heading without title, orphan file) and runs in `scripts/check.sh`. Template-literal titles (the per-theme, per-viewport and
committed/uncommitted loops) keep their `${...}` placeholders verbatim in the heading, with no skip list; give a `test(` a literal title or
a template, never a variable. There is no coverage map: the scenario files are the record. The checker's extractor has a tiny
`node --test` suite (`npm run test:scripts`).

## Coverage floors and the ratchet

A floor is the **integer below the measured value**, written with a dated comment, raised whenever coverage improves and **never lowered**
(not for new code either; add tests). A tight-loop Kover disable is never committed.

| Gate | Where | Floor (2026-11-08) |
| --- | --- | --- |
| engine Kover | `engine/build.gradle.kts` | line 98, branch 84 |
| server Kover (`@Serializable` classes excluded) | `server/build.gradle.kts` | line 98, branch 80 |
| web vitest v8 | `web/vite.config.ts` `test.coverage.thresholds` | statements 94, branches 88, functions 97, lines 97 |

Re-measure: `./gradlew :engine:koverXmlReport :server:koverXmlReport` (`check` only runs `koverVerify`, which reports pass/fail, not the
actual) and read the `npm run test:coverage` table. Then edit the floor and its comment in the same commit.
