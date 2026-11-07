# Astrid

A Danish reading and writing engine for English-speaking beginners. This first milestone is a private playground: three exercise formats, a structured 23-entry vocabulary seed, and 99 licensed sentence combinations. Content is a linguistic draft awaiting Danish teaching review.

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

Feedback reports both the requested target and the supported linguistic judgment. An acceptable fronted statement can miss the subject-first target. Unknown writing is not automatically invalid Danish. Hints and retries are recorded; there is no mastery or adaptive scheduling claim.

Seed, index, content revision and generator revision make exercises reproducible. A seeded cycle visits every eligible sentence before repeating (99 for present/order, 92 for articles).

The optional inspector reveals private generation details. Set `ASTRID_INSPECTOR_ENABLED=true` and a random `ASTRID_INSPECTOR_KEY` of at least 32 characters in the server environment, then enter the key in the inspector. Generate one with `openssl rand -hex 32`. The key stays out of frontend builds and browser persistent storage. Restart the server after configuration changes.

`POSTGRES_CONNECT_RETRIES` defaults to 10 (allowed 0–15). Flyway retries initial database connections with waits capped at 8 seconds. Migration connections use a 3-second TCP connect timeout and 5-second login deadline, overriding URL timeout settings while retaining TLS and other connection options. Docker and Kubernetes startup grace periods cover the maximum supported bootstrap envelope; `scripts/check-startup-budget.py` guards their agreement. Reassess those budgets before adding slow migrations.

## Structure and conventions

| Path | Responsibility |
| --- | --- |
| `engine/` | Pure Kotlin language content, validation, generation and grading; no HTTP/database dependencies |
| `server/` | Ktor API, public DTO mapping, session ownership, PostgreSQL and Flyway migrations |
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

The gate runs strict Gradle checksum verification, engine/server tests, detekt, coverage verification, distribution packaging, API type drift detection, frontend lint/tests/build, and deployment rendering. PostgreSQL integration tests use the pinned Testcontainers image. Do not skip Docker tests when reporting the whole suite as passing.

```sh
./gradlew :engine:test                 # Focused language-engine tests
./gradlew :server:test                 # API and real PostgreSQL integration tests
npm --prefix web run gen:api           # Refresh committed types after an OpenAPI change
npm --prefix web run check:api         # Check without rewriting committed types
```

Browser journeys run against the complete stack (port 8088 by default):

```sh
npm --prefix e2e ci
cd e2e
npx playwright install chromium
npm test
```

Override `E2E_BASE_URL` for another private preview URL. The suite covers all formats, keyboard ordering on a narrow viewport, and replay after the server commits an attempt but its response is lost.

CI repeats the gates on pull requests and master, validates Kubernetes schemas and scans the packaged image. Dependency/action/image pins are intentional; update related runtime pins together. Commit focused changes with imperative subjects; PRs should explain behavior, link relevant backlog/issues, report executed checks and include screenshots for UI changes. Add forward-only database migrations rather than editing an applied migration.

## Review the pilot content

```sh
./gradlew :engine:exportPilotReview
```

The deterministic report is written to `engine/build/review/pilot-1.md`. It includes the 23 lexical records, sources and draft review status, all 99 sentences and reproducible exercise examples. The production engine remains free of file I/O; the export adapter runs only as a local build tool. Published compatibility fixtures guard 335 baseline outputs. Their generated origin establishes compatibility, while the independently specified worked examples and eventual Danish teaching review address linguistic correctness.

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
