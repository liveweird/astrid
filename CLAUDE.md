# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Codex reads `AGENTS.md`, which points here.

## Product

Astrid is a Danish reading and writing engine for English-speaking complete beginners. Today it is a
**private development preview**: a standalone engine playground (`/`) and short guided practice sessions
(`/practice`) over a connected beginner slice — 26 vocabulary entries (magazine is choice/matching-only;
teacher is supplied first-step vocabulary), 109 licensed complete sentences and
twenty-one exercise entries (grammar selection/supply/order/repair, vocabulary recognition/recall, constrained
sentence writing, short reading comprehension, English→Danish word choice and all/spare word tiles,
bilingual matching, verb/article gaps, and scoped subject-pronoun/stored `er` selection and recall, and inanimate subject reference).
Generation and grading are deterministic and reproducible
from seed, index and revision. All Danish content is a linguistic **draft awaiting teaching review**; a
passing test never makes content teacher-approved. Current app version: `web/src/changelog/version.ts`.
Guided practice offers Mixed practice (eight parts), Present verb forms (six) and Verbs and articles
together (four). The third profile and its selected-only reviews pin pair seven; older plans retain pair three.
**Module 1: First steps** includes three independent six-item beginner profiles, literally pinned to
pair eight, and a four-item **Referring to one thing** profile pinned to pair nine. They require saved teaching before assessment, expose server-computed continuation
eligibility and keep scoped selection/recall results. They offer repeat/next, not targeted reviews.
Existing format pools and all retained pairs/plans stay unchanged; the first-step contract is
`docs/knowledge/module-one-activation.md`; S04 is specified in
`docs/knowledge/inanimate-reference-activation.md`.
Next work: `BACKLOG.md`. Product scope and decisions: `docs/knowledge/product-direction.md`.

Brand: Danish red accent with separately labelled learning-feedback colours (`web/src/theme.ts`); the logo (a girl
with plaits in shades of red) is `web/public/logo-{light,dark}.svg` plus the `favicon.svg` fallback; palette work remains BL-021.

## Donors

Astrid's scaffold was ported from Flow and aligned with its siblings. **Port, don't reinvent**: when Astrid
needs a capability a sibling already has, port its implementation rather than designing a new one — Flow
(`~/Sources/flow`, the primary donor: build, gates, CI, image, server plugins, api-guidelines, web shell and
changelog), Lettuce (`~/Sources/lettuce`) and Toadie (`~/Sources/toadie`); Covenant and Astrolabe share the
same family UI. Versions match Flow/Toadie. What was kept, adapted or deliberately omitted (auth, i18n,
OpenTelemetry, JVM tuning…) and why: `docs/knowledge/flow-reuse-manifest.md`. Keep the MIT notice in
`THIRD_PARTY_NOTICES.md`.

## Commands

JDK 21 and Node 24 are pinned in `mise.toml` (also `.nvmrc`); run `mise install` once. Server tests need
Docker (Testcontainers); on OrbStack `export DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock`.

- **All local gates:** `./scripts/check.sh` — toolchain/knowledge/startup-budget checks, strict Gradle
  `check` (tests, detekt, Kover floors) + `installDist`, web `lint:api`/`check:api`/`lint`/`knip`/
  `test:coverage`/`build`, e2e `lint`/`knip`/`typecheck`/`check:scenarios`/`test:scripts`, Compose and kustomize rendering. Run it before
  every commit; CI (`.github/workflows/ci.yml`) repeats it plus image scan and the browser suite.
- Server: `./gradlew :server:run` (port 8088), `./gradlew :server:test`, one test:
  `./gradlew :server:test --tests "app.astrid.server.ApplicationTest"`.
- Engine: `./gradlew :engine:test`, `:engine:localeCompatibilityTest`, `:engine:exportPilotReview`.
- Static analysis: detekt rides `check`, zero findings, no baseline; tune only in `config/detekt/detekt.yml`
  with a comment. Never add an uncommented `@Suppress`.
- Package: `./gradlew :server:installDist`. Never a fat JAR (Flyway service discovery).
- Web: `cd web && npm ci --legacy-peer-deps`, then `npm run dev|build|lint|knip|test|test:coverage`;
  API contract: `npm run lint:api` (Spectral, `api-guidelines/`) and `npm run gen:api` / `check:api`.
- E2E: `cd e2e && npm ci && npx playwright install chromium webkit && npm test` against the full stack. Each spec has a
  scenario file in `e2e/scenarios/`, updated in the same commit; `npm run check:scenarios` checks the parity. A nightly
  workflow (`.github/workflows/e2e.yml`) re-runs the suite and the image scan on `master` and opens an issue when it fails.
- Knowledge base: `python3 scripts/check-knowledge.py` (IDs, prerequisites, crosswalk, local links).

## Running the full stack

`cp .env.example .env && docker compose up --build -d` serves everything at `http://localhost:8088`
(PostgreSQL on host port 5438; both bind to 127.0.0.1). Local dev: `docker compose up -d postgres`,
`ASTRID_DEVELOPMENT_MODE=true POSTGRES_PASSWORD=astrid-local-development ./gradlew :server:run`, and
`cd web && npm run dev` (Vite on 5180, proxying `/api` to 8088). Ports avoid the siblings (Lettuce 8080,
Toadie 8081, Covenant 8082, Flow 8084). **Never `docker compose down -v`** unless asked: the volume holds
review data. Kubernetes (private, single replica, kustomize): README "Kubernetes" and `.claude/docs/ci.md`.

## Architecture

Gradle multi-module build (Kotlin DSL, `gradle/libs.versions.toml`, strict locking and checksum verification)
plus two standalone npm workspaces:

- **`engine`** — pure Kotlin: content, validation, generation, grading. No HTTP, database, file I/O or
  telemetry (export tools live in its test source set). It is a domain module, not the siblings' `core`.
- **`server`** — Ktor/Netty, depends on `engine`. Explicit wiring: `Main.kt` builds `ServerConfig`,
  `PostgresStores` and calls `astridModule(config, stores, …)`; there is no `application.yaml` and no DI
  framework. Packages under `app.astrid.server`, layering one-way (`PackageBoundaryTest`):
  - `plugins/` — cross-cutting Ktor wiring: ErrorHandling (RFC 9457 `application/problem+json` via
    `respondProblem`), Http (content negotiation, body limit, CallLogging), SecurityHeaders,
    StaticCompression, Health (`/api/v1/health`, `/ready`), Routing (SPA, registered last).
  - `infra/` — `config/ServerConfig`, `db/` (Flyway bootstrap, R2DBC pool), `json/`.
  - `authz/` — `SessionPrincipal`, the `Authenticator` seam, guards, typed HTTP exceptions.
  - `session/` → `exercises/` → `practice/` — features, each with DTOs, routes, a store interface, its
    Postgres store and tables. `exercises` reaches practice only through `AttemptPracticeHook`, which runs
    inside the caller's transaction.
- **`web/`** — Vite + React + Mantine SPA; see `web/CLAUDE.md`.
- **`e2e/`** — Playwright journeys (Chromium and WebKit) against the assembled stack.

### The OpenAPI contract

`server/src/main/resources/openapi/documentation.yaml` is hand-maintained and authoritative: an endpoint change
edits it in the same commit, `npm --prefix web run gen:api` regenerates the committed `web/src/api/schema.ts`,
`OpenApiContractTest` validates server responses against it, and `lint:api` checks it against
`api-guidelines/API-GUIDELINES.md` (cite rule IDs in reviews; app-specific exceptions in
`web/spectral.astrid.yaml`). Public DTOs are explicit mappings: never serialize an engine object that carries
an answer key.

## Cross-cutting conventions

Always loaded:

@.claude/docs/testing.md
@.claude/docs/compatibility.md

Every other doc is read on demand but binding: **before touching an area, read its doc in full** and name it
in any implementer/reviewer brief. If a doc and the configuration disagree, the configuration wins; fix the doc
in the same change.

| Doc | Read before you touch |
|---|---|
| `.claude/docs/conventions.md` | a new endpoint, store, migration or page (the feature template); package layout; style and lint policy; version bumps |
| `.claude/docs/compatibility.md` | anything in `engine/`, exercise mapping, revisions, golden fixtures, published pilots |
| `.claude/docs/persistence.md` | a migration (applied bytes are immutable, `MigrationChecksumTest`), a store, transactions, locking |
| `.claude/docs/security.md` | the development session, guards and status ordering, host/origin/header policy, inspector, secrets, logging |
| `.claude/docs/testing.md` | any test; coverage floors (ratchet up, never down); the fake/Postgres contract suite |
| `.claude/docs/ci.md` | `scripts/check.sh`, CI jobs, Dependabot, Compose, Kubernetes |
| `.claude/docs/dependency-reproducibility.md` | any dependency, lockfile or `gradle/verification-metadata.xml` change |
| `web/CLAUDE.md` | anything under `web/` |
| `api-guidelines/API-GUIDELINES.md` | any API change |

Playbooks: `.claude/skills/verify` (drive the app in a browser), `run-stack`, `api-review`.

## Project records and working agreement

The user's standing rule: **continuously document** assumptions, requirements, decisions, open questions,
principles and tenets in `docs/knowledge/` (start at `docs/knowledge/README.md`; rules in
`docs/knowledge/working-agreement.md` and `docs/AGENTS.md`). Distinguish user-confirmed decisions from
assistant proposals; mark superseded decisions explicitly. Each delivered contract records its evidence in an
"Observed delivery" section.

`BACKLOG.md` holds open work only: one `[next]`/`[todo]`/`[new]`/`[parked]`/`[blocked]` tag per item under
thematic `##` sections; items you add are `[new]`; only the user promotes them; remove an item when done.

Git: focused commits with imperative subjects; never edit an applied migration or a golden fixture; a
user-visible feature bumps the minor app version, a fix the patch (`docs/knowledge/app-versioning.md`).
Never commit `.env` or credentials.
