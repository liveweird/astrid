# Astrid

A Danish reading and writing engine for English-speaking beginners. The private preview offers guided practice and a standalone playground: ten exercise entries covering grammar selection/supply/order/repair, vocabulary recognition/recall, full-sentence writing and short reading comprehension, a structured 23-entry vocabulary seed, and 99 licensed sentence combinations. Content is a linguistic draft awaiting Danish teaching review.

## Run the complete app

Requires Docker Compose. From the repository root:

```sh
cp .env.example .env
docker compose up --build -d
```

Open **http://localhost:8088**. The app creates an isolated development session in your browser. PostgreSQL preserves exercises and attempts across ordinary restarts. `docker compose down` stops the stack; `docker compose down -v` also deletes its data.

This is a private development tool. Ports bind to loopback, sessions require `ASTRID_DEVELOPMENT_MODE=true`, and there is no public account system. Keep this deployment private.

## Develop locally

Use the runtime versions in `mise.toml` (Java 21, Node 24) and the committed Gradle wrapper. `mise install` / `mise exec -- <command>` are optional conveniences; `.nvmrc` also records Node's exact version.

```sh
cp .env.example .env
docker compose up -d postgres
export POSTGRES_PASSWORD=astrid-local-development
ASTRID_DEVELOPMENT_MODE=true ./gradlew :server:run
```

In another terminal:

```sh
cd web
npm ci --legacy-peer-deps
npm run dev
```

Open **http://localhost:5180**; Vite proxies API calls to port 8088. The narrow peer-dependency exception supports openapi-typescript's TypeScript 5 declaration alongside Flow's tested TypeScript 6 toolchain. It does not replace type checks or dependency audit.

Native development binds to `127.0.0.1`; `ASTRID_BIND_HOST` overrides it. The container explicitly binds its internal interface, with Compose publishing only loopback ports. Default database URLs use `localhost:5438/astrid`. Override `POSTGRES_JDBC_URL`, `POSTGRES_R2DBC_URL`, `POSTGRES_USER` and `POSTGRES_PASSWORD` together when using another database. Do not commit `.env` or credentials. OrbStack users may need `export DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock` for Testcontainers.

## Use the playground

- **Present form:** choose between related infinitive and present forms.
- **En or et:** type an unstressed indefinite article.
- **Sentence order:** arrange intact constituents into a subject-first statement.
- **Present recall and repair:** supply a named present verb or replace one marked verb/article.
- **Meaning recognition:** choose the English meaning of `bog`, `avis` or `brev` from related alternatives.
- **Headword recall:** type the introduced Danish singular noun for an English cue, without its article.
- **Sentence writing:** type a complete subject-first Danish statement from an English meaning, using introduced words. Feedback separates order, meaning, bounded language diagnosis and one-statement constraints.
- **Short reading:** read three Danish sentences, then choose the English answer matching the asked participant and action. Related alternatives require following the stated facts.

Feedback reports both the requested target and the supported linguistic judgment. An acceptable fronted statement can miss the subject-first target. Unknown writing is not automatically invalid Danish. Vocabulary feedback also separates knowing the headword from supplying the requested one-word response; `en avis` can identify the word while needing correction to the response format. Hints and retries are recorded; there is no mastery or adaptive scheduling claim.

API requests have a 15-second browser deadline, including their response body. An unconfirmed submission keeps the exact attempt available through “Check the same attempt.” Keep that page open for recovery. This bounds visible waiting in an active browser; it does not establish whether a timed-out write committed or guarantee server cleanup during a database outage. See the [recovery evidence](docs/knowledge/runtime-recovery.md).

Seed, index, content revision and generator revision make exercises reproducible. A seeded cycle visits every eligible sentence before repeating (99 for present/order, 92 for articles); each vocabulary direction visits its three selected senses before repeating. Sentence writing uses the 99 licensed statements; reading cycles through 24 three-fact units with seeded sentence/choice order. The two new families are initially standalone; guided practice retains its existing plans and revisions.

The optional inspector reveals private generation details. Set `ASTRID_INSPECTOR_ENABLED=true` and a random `ASTRID_INSPECTOR_KEY` of at least 32 characters in the server environment, then enter the key in the inspector. Generate one with `openssl rand -hex 32`. The key stays out of frontend builds and browser persistent storage. Restart the server after configuration changes.

`POSTGRES_CONNECT_RETRIES` defaults to 10 (allowed 0–15). Flyway retries initial database connections with waits capped at 8 seconds. Migration connections use a 3-second TCP connect timeout and 5-second login deadline, overriding URL timeout settings while retaining TLS and other connection options. Docker and Kubernetes startup grace periods cover the maximum supported bootstrap envelope; `scripts/check-startup-budget.py` guards their agreement. Reassess those budgets before adding slow migrations.

## Use guided practice

Open **http://localhost:8088/practice** and choose a focus. **Mixed practice** combines meaning recognition, sentence ordering, introduced-headword recall and article repair in eight parts. **Present verb forms** has six parts alternating form selection, typed recall and single-word repair across five introduced verbs. Optional session introduction, item lessons and hints are recorded before display. Continue after any saved answer; retrying is optional. The summary preserves first and latest outcomes, support and possible earlier exposure. Completion does not establish mastery.

Practice saves operation identities in this browser before sending them. Refresh resumes the URL-bound session and replays uncertain operations with their original identities; confirmed grades are read back without regrading. Recovery requires the original valid development cookie and browser storage. If either is unavailable, use **Start new practice**; old server history remains intact. Unsent drafts need not survive refresh. The standalone playground retains its existing in-tab recovery behavior.

From a completed summary, select individual grammar or vocabulary targets and choose **Start selected review**. Recognition and written recall stay separate. Vocabulary targets add one item each; selecting grammar adds distinct sentences for a short run of up to eight items. No targets are selected automatically. Present-verb summaries keep selection, written recall and repair separate; choosing one, two or three operations produces four, four or six review items. The review saves its source link and possible earlier exposure, and keeps the original session's results intact. A single-word review can contain one item because repeating its cue would not add meaningful variation.

The [session](docs/knowledge/practice-sessions.md), [follow-up](docs/knowledge/targeted-follow-up.md) and [topic-practice](docs/knowledge/topic-practice.md) contracts record pinned plans, ownership, frozen exposure and persistence rules. All use existing beginner content, without adaptive scheduling or new language material.

## Application version and changelog

The navigation footer shows the running app version, source commit and commit time; its link and **Changelog** open the release notes. A device-local indicator marks an unread version. Reading notes does not start or renew a learning session.

Use a regular Git checkout when building the Docker image: the web builder reads `.git` for identity, and the runtime receives only the compiled stamp. Local Vite builds show `+dirty` for uncommitted work. `GIT_SHA` and `GIT_COMMIT_TIME` can explicitly supply build metadata.

For a user-visible feature, bump the minor application version; for a fix, bump the patch. In the same commit, update `web/src/changelog/version.ts`, prepend an authored English entry in `web/src/changelog/entries.ts`, and match `info.version` in `server/src/main/resources/openapi/documentation.yaml`. Tests guard consistency. Keep old entries unchanged. Documentation/test/CI/dependency-only changes may retain the version. Gradle/npm package and learning-content revision numbers are separate. See the [versioning contract](docs/knowledge/app-versioning.md) for explicit tag/release publication.

## Structure and conventions

| Path | Responsibility |
| --- | --- |
| `engine/` | Pure Kotlin language content, validation, generation and grading; no HTTP/database dependencies |
| `server/` | Ktor API (`application/problem+json` errors, hand-maintained spec at `server/src/main/resources/openapi/documentation.yaml`), PostgreSQL and Flyway migrations. Packages under `app.astrid.server`: the composition root (`Main`, `Application`, `Stores`, `PostgresStores`); `plugins/` (Ktor wiring: errors, compression, security headers, HTTP, SPA routing, health); `infra/` (`config`, `db` Flyway and pool, `json` wire format); `authz/` (typed HTTP exceptions, guards, the `SessionPrincipal`/`Authenticator` seam); then the features, one-way `session` < `exercises` < `practice` (each: DTOs, routes, store port, Postgres store). `exercises/` reaches practice state only through `AttemptPracticeHook`; `PackageBoundaryTest` enforces the layering |
| `api-guidelines/` | Shared API rulebook and Spectral ruleset (copied from Flow, never edited here); Astrid's severity decisions live in `web/spectral.astrid.yaml` |
| `web/` | React/Mantine shell, exercise controls, generated API types and interaction tests |
| `e2e/` | Browser journeys against the assembled app |
| `k8s/` | Private single-replica playground deployment |
| `docs/knowledge/` | Requirements, decisions, language drafts, sources and open points |
| `BACKLOG.md` | Planned work and completed delivery evidence |

Use four-space Kotlin and two-space TypeScript indentation, PascalCase types/components, and camelCase functions/properties. Keep domain logic out of routes and React components. Public API objects are explicit mappings: never serialize an engine object containing an answer key. Prefer focused files and behavior-based tests. Preserve raw answers, version generation and evaluation changes, and add regression cases for grading fixes.

## Verify changes

With Docker running, `kubectl` installed, and dependencies installed in both `web/` and `e2e/`:

```sh
./scripts/check.sh
```

The gate runs offline documentation integrity checks, strict Gradle checksum verification, engine/server tests, detekt, coverage verification, distribution packaging, API guideline linting (Spectral) and type drift detection, frontend and e2e lint, dead-code detection (knip) in both workspaces, frontend tests/build, e2e type checking, and deployment rendering. Kover line and branch coverage floors for the engine and server only ratchet upward (see the comments in each `build.gradle.kts`). `MigrationChecksumTest` pins the Flyway checksum of every applied migration: add a migration, never edit one. PostgreSQL integration tests use the pinned Testcontainers image. Do not skip Docker tests when reporting the whole suite as passing.

```sh
./gradlew :engine:test                 # Focused language-engine tests
./gradlew :engine:localeCompatibilityTest # Same suite in a fresh Arabic-locale JVM
./gradlew :server:test                 # API and real PostgreSQL integration tests
npm --prefix web run gen:api           # Refresh committed types after an OpenAPI change
npm --prefix web run lint:api          # Spectral-lint the spec against api-guidelines/
npm --prefix web run check:api         # Check without rewriting committed types
```

Browser journeys run against the complete stack (port 8088 by default):

```sh
npm --prefix e2e ci
cd e2e
npx playwright install chromium webkit
npm test
```

Override `E2E_BASE_URL` for another private preview URL. The suite runs the same critical journeys in Chromium and WebKit, covering all formats, keyboard ordering on a narrow viewport and replay after a committed response is lost or exceeds the browser deadline. macOS WebKit uses Safari's native Option-Tab/Shift-Option-Tab traversal; no system preferences are changed. These are browser-engine checks, not native iOS-device testing.

CI repeats the gates on pull requests and master, validates Kubernetes schemas and scans the packaged image. Dependency/action/image pins are intentional; update related runtime pins together. Commit focused changes with imperative subjects; PRs should explain behavior, link relevant backlog/issues, report executed checks and include screenshots for UI changes. Add forward-only database migrations rather than editing an applied migration.

## Review the pilot content

```sh
./gradlew :engine:exportPilotReview
```

The deterministic report is written to `engine/build/review/pilot-3.md`. It includes the 23 lexical records, sources and draft review status, all 99 sentences and reproducible exercise examples. The production engine remains free of file I/O; the export adapter runs only as a local build tool. Run `./gradlew :engine:exportLegacyPilotReview` or `./gradlew :engine:exportPilotTwoReview` for the retained `pilot-1`/`pilot-2` reports. Compatibility fixtures preserve 335 original cases, 952 revision-2 generation/evaluation cases and 978 published fingerprint cases. Their generated origin establishes compatibility, while the independently specified worked examples and eventual Danish teaching review address linguistic correctness.

Vocabulary introduces a forward migration permitting a null grammar target; its separate lexical target lives in the stored presentation. After vocabulary writes, rollback requires a reader compatible with new metadata and nullable targets; a bare older image is unsafe. See the [contract](docs/knowledge/vocabulary-assessment.md).

CI also exports this report in the `server-reports` artifact. Run `python3 scripts/check-knowledge.py` for a fast offline check of foundation IDs, prerequisites, source references, crosswalk rows and local file links; it does not validate linguistic claims, link anchors or external sites. The [coverage crosswalk](docs/knowledge/foundation-coverage.md) distinguishes existing assessed targets from support and later authoring proposals.

## Kubernetes

Build/load `astrid:local` into your local cluster's image store. Review your context first. These commands affect only the `astrid` namespace:

```sh
kubectl apply -f k8s/namespace.yaml
kubectl -n astrid create secret generic astrid-secrets --from-env-file=.env
kubectl apply -k k8s
kubectl -n astrid rollout status deployment/astrid
kubectl -n astrid port-forward service/astrid 8088:8088
```

No ingress or public service is installed. The database needs a default storage class for its PVC. For a remote cluster, publish your own tagged image and change `k8s/app.yaml`; do not rely on a shared mutable local tag. Secret rotation requires updating the Secret and restarting pods; preserve the PostgreSQL password already used to initialize its volume. Back up the database before future migrations or destructive operations.

## Project records

Start with the [knowledge index](docs/knowledge/README.md), [implementation decisions](docs/knowledge/implementation-decisions.md), [Flow reuse manifest](docs/knowledge/flow-reuse-manifest.md), and [backlog](BACKLOG.md). The root `AGENTS.md` is preserved from repository initialization; its empty-repository description is historical. Reused Flow tooling/patterns retain their [MIT notice](THIRD_PARTY_NOTICES.md).
