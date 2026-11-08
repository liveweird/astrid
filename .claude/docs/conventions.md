# Conventions

How Astrid is laid out and how a feature is added end to end. Read before adding an endpoint, store, migration or page.
Long-form decisions and product records live in `docs/knowledge/` (index: `docs/knowledge/README.md`); this file is the
operational rulebook. Astrid was scaffolded from Flow and aligned with Flow/Lettuce/Toadie; what it deliberately omits is in
`docs/knowledge/flow-reuse-manifest.md` (no JWT, no i18n, no mail, no OpenTelemetry, no audit trail, no list endpoints,
no `application.yaml`).

## Modules

- `engine/` — pure Kotlin: content, validation, generation, grading, practice planning. No HTTP, database or file I/O (the review
  export adapters are test-source `main`s). Never references `app.astrid.server` (`PackageBoundaryTest`).
- `server/` — Ktor/Netty API, Exposed R2DBC + Flyway PostgreSQL, the hand-maintained OpenAPI spec.
- `web/` — React/Mantine SPA (`web/CLAUDE.md`). `e2e/` — Playwright against the assembled stack. `k8s/` — private single-replica
  deployment. `api-guidelines/` — shared API rulebook, copied from Flow, never edited here.

## Server packages and layering

Everything is `app.astrid.server.<package>` under `server/src/main/kotlin/app/astrid/server/`. Layering is one-way and
`PackageBoundaryTest` (a source scan, no Docker) fails on a violation:

| Package | Holds | May reference |
| --- | --- | --- |
| composition root (`Main`, `Application`, `Stores`, `PostgresStores`) | wiring only | everything; nothing references it |
| `plugins/` | Ktor wiring: `ErrorHandling` (problem+json), `StaticCompression`, `SecurityHeaders`, `Http` (negotiation, body limit, call logging), `Routing` (SPA), `Health` | `infra`, `authz` |
| `infra/` | `config/ServerConfig`, `db/` (Flyway bootstrap, R2DBC `Database`), `json/wireJson` | no feature, no `plugins` |
| `authz/` | typed HTTP exceptions, guards, `SessionPrincipal` + `Authenticator` seam | no feature, no `plugins` |
| `session/` | development session store, routes, `DevelopmentSessionAuthenticator` | foundation only |
| `exercises/` | catalog, exercises, attempts, inspector; the `AttemptPracticeHook` seam | `session` |
| `practice/` | practice runs, follow-ups, support, advance; implements the hook | `session`, `exercises` |

Features layer `session` < `exercises` < `practice`. `exercises/` reaches practice state only through `AttemptPracticeHook`
(implemented by `practice/PostgresPracticeHook`), which runs inside the attempt's own transaction (`persistence.md`).
Each feature owns its DTOs (`*Dtos.kt`), routes, store port (`*Store.kt`), Exposed tables (`*Tables.kt`, `internal`) and
Postgres store (`Postgres*Store.kt`). Test files all sit in the root package `app.astrid.server`.

## Composition root

`main()` builds `ServerConfig.fromEnvironment()`, runs Flyway (`migrate`), builds `PostgresStores(Database.connect(config), wireJson)`
and starts `embeddedServer(Netty, host, port) { astridModule(config, stores) }`. There is no `application.yaml`/`EngineMain`: wiring is
explicit so tests inject fakes and the clock directly. `astridModule(config, stores, engine, evaluator, clock)` is the single
entry point; `Stores` is the one persistence seam (`sessions`, `exercises`, `practice`, `readiness`). HTTP tests build the app
over `InMemoryStores` (`server/src/testFixtures`, Docker-free); `StoreContractTest` keeps the fake and Postgres on one contract
(`testing.md`). New configuration is an environment variable read in `ServerConfig.fromEnvironment` (validated with `require`),
documented in `README.md` and `.env.example`.

## Feature template — adding an endpoint end to end

Copy the shape of `practice/` (a stateful feature) or `exercises/` (read/write with a seam). In one commit:

1. **Spec first** — edit `server/src/main/resources/openapi/documentation.yaml`: path, `operationId`, every status the route can
   answer with `$ref: '#/components/responses/Problem'` for errors (including `'500'`), `security: []` only for public
   routes, `headers.Location` on a 201. Then `cd web && npm run gen:api` and commit the regenerated `src/api/schema.ts`
   (`npm run check:api` diffs it; `npm run lint:api` is the Spectral gate — fix the spec, never add an `off` for a fixable finding).
2. **DTOs and mapping** — wire DTOs are `@Serializable` data classes in `<feature>/<Feature>Dtos.kt`; an optional field that old
   clients/stored payloads must not see is `@EncodeDefault(NEVER)` (`compatibility.md`). Map explicitly
   (`GeneratedExercise.toResponse`, `toInspector`, `PracticeMapping.kt`). **Never serialize an engine object that contains an answer key**
   (`GeneratedExercise`, `Evaluation` inputs): the public `ExerciseResponse` omits the inspector/solution; only the key-guarded
   inspector DTO carries it.
3. **Route** — `internal fun Route.<x>Routes(...)` in `<feature>/<Feature>Routes.kt`, mounted in `astridModule` under `/api/v1`.
   Errors are thrown as `authz` exceptions (`BadRequest`, `Forbidden`, `NotFound`, `Conflict`, ...) and rendered once by
   `StatusPages` through `respondProblem` (`application/problem+json`, `code` = Astrid's stable machine-readable extension).
   Never `call.respond(status, ...)` an error body by hand; never reflect the query string in `detail`.
4. **Guard order in a mutating handler** — `requireMutationHeaders(call)` → `call.caller(authenticator)` → ownership checks
   (`requirePracticeOwner`) → `receive<T>()` → validation → store. Headers first so 403/415 win over 401 (`security.md`); body
   decoding last so a bad body never reveals whether the caller was authorized. GETs call `caller` first.
5. **Store** — add the method to the feature's store interface, the Postgres implementation, and the `InMemoryStores` fake in the
   same commit; extend `StoreContractTest` for every outcome the route branches on.
6. **Migration** if the shape needs one: new `V<n>__description.sql` plus its checksum line in `MigrationChecksumTest`
   (`persistence.md`). Never edit an applied one.
7. **Tests** — `testApplication` over `InMemoryStores` for status/ordering, `OpenApiContractTest` for the declared statuses
   and bodies, Postgres tests for transactional behaviour; for web, a colocated test; for a user journey, an `e2e/tests/*.spec.ts`
   (`testing.md`).
8. **Web** — client function in `web/src/api/client.ts`, page/component per `web/CLAUDE.md`.
9. **Version and changelog** if user-visible (below).

A new page: a folder under `web/src/pages/<name>/`, a `<Route>` in `App.tsx`, a nav entry in `PrimaryNavigation`, a colocated
test, and a Playwright journey. Skip link, focus target and `lang` rules are in `web/CLAUDE.md`.

## App version and changelog

User-visible feature → minor bump, user-visible fix → patch bump; docs/test/CI/dependency-only changes keep the version. In the
**same commit** update `web/src/changelog/version.ts` (`APP_VERSION`), prepend an authored English entry to
`web/src/changelog/entries.ts` (newest first, unique semver, descending ISO dates; old entries are immutable) and set `info.version`
in `documentation.yaml`. `OpenApiContractTest` and `entries.test.ts` guard the three agreeing. Gradle/npm package versions,
`/api/v1` and content/generator/evaluation revisions are separate numbers. Tagging/releases are explicit user requests:
`docs/knowledge/app-versioning.md`.

## Style

- Kotlin: four-space indent, `kotlin.code.style=official`, PascalCase types, camelCase members, `internal` by default for
  routes/tables/guards, KDoc on every seam (`Stores`, `AttemptPracticeHook`, `Authenticator`). Domain logic stays in `engine/`,
  not in routes. Prefer focused files and behaviour-named tests (`` `a session is found by token hash until it expires` ``).
- TypeScript: two-space indent, strict TS 6, PascalCase components (default export for components/pages, named elsewhere),
  camelCase functions, relative imports only.
- **detekt** (`./gradlew detekt`, rides `check`): zero findings, no baseline. Tune only in `config/detekt/detekt.yml`, one comment
  per override naming the idiom (today: `LongMethod` 80, `LongParameterList`, `LargeClass` and `MagicNumber` relaxed for tests/content,
  `MaxLineLength` 140, `TooManyFunctions` off as in all siblings, `UnusedImports` on). Fix code first; a per-file/site `@Suppress`
  needs a one-line justification (see `authz/Guards.kt`).
- **eslint** (`web`, `e2e`: `npm run lint`, `--max-warnings 0`) plus **knip** (`npm run knip`): zero findings. Tune only in
  `eslint.config.js`/`knip.json` with a comment; an inline `eslint-disable` needs one too. Delete what knip flags — keep an
  export only if another file uses it.
- Preserve raw learner answers, version every generation/evaluation change and add a regression case for every grading fix.
