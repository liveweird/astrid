# CI, the local gate and the stacks

Read before changing `scripts/check.sh`, a workflow, Dependabot, a pin, the Dockerfile, compose or `k8s/`. Command recipes for a session:
`.claude/skills/{verify,run-stack,api-review}`.

## The local gate: `scripts/check.sh`

Runs from any directory, needs Docker (Testcontainers) and `kubectl`. Steps, in order, all must pass:

1. `python3 scripts/check-toolchain.py` — runtime/action/image pins agree (below).
2. `python3 -m unittest discover -s scripts/tests -p 'test_*.py'` — tests of the knowledge checker.
3. `python3 scripts/check-knowledge.py` — offline integrity of `docs/knowledge` (foundation IDs, prerequisites, source refs, crosswalk rows) and local
   Markdown links in `README.md`, `BACKLOG.md` and `docs/`. It does not read `.claude/`, so check links there by hand.
4. `python3 scripts/check-startup-budget.py` — bootstrap envelope vs Docker/Kubernetes grace (`persistence.md`).
5. `git diff --check`.
6. `./gradlew check :engine:koverVerify :server:koverVerify :server:installDist --dependency-verification strict --no-daemon` — engine and
   server tests (incl. Arabic-locale engine run), detekt, Kover floors, packaging, strict checksum verification.
7. `npm --prefix web run lint:api` (Spectral), `check:api` (generated types in sync), `lint`, `knip`, `test:coverage`, `build`.
8. `npm --prefix e2e run lint`, `knip`, `typecheck`, `check:scenarios` (spec ↔ scenario parity, `testing.md`), `test:scripts` (the checker's own tests).
9. `docker compose --env-file .env.example config --quiet` and `kubectl kustomize k8s > /dev/null` — manifests render.

It does **not** run the Playwright suite (needs a stack), `npm audit`, Trivy or kubeconform (live databases / downloaded tools: CI only).
Whenever you touch UI or API behaviour also run the e2e suite (`testing.md`). Never skip Docker tests and call the suite green.

## CI (`.github/workflows/ci.yml`)

Triggers: push to `master`, pull requests, manual dispatch; `permissions: contents: read`; a superseded run is cancelled only for pull requests
(a `master` push runs in its own group, so every master commit is fully verified). Every action is pinned to a 40-hex SHA with a version comment.

| Job | What |
| --- | --- |
| `server` | JDK from `java-version` pin; steps 1–4 above, then `./gradlew check :engine:exportPilotReview :engine:koverVerify :server:koverVerify :server:installDist --dependency-verification strict`; **`git diff --exit-code -- gradle/verification-metadata.xml '*gradle.lockfile'`** (drift = someone forgot to commit regenerated locks); uploads `server-reports` (reports + `engine/build/review/`). |
| `web` | `npm ci --legacy-peer-deps`; `lint:api && check:api && lint && knip && test:coverage && build` with `GIT_SHA`; `npm audit --audit-level=high`. |
| `dependency-scan` | Trivy filesystem scan (HIGH/CRITICAL, dev deps included) over all Gradle lockfiles. |
| `kubernetes` | kubeconform (checksum-verified binary, k8s 1.33.0 schemas pinned by commit) over the manifests and over `kubectl kustomize k8s`. |
| `image` | `docker compose config`; `docker build -t astrid:local .`; e2e `npm ci && lint && knip && typecheck && check:scenarios && test:scripts && audit`; `playwright install --with-deps chromium webkit`; `docker compose --env-file .env.example up -d --wait` then `npm --prefix e2e test`; logs on failure; uploads `browser-reports`; Trivy scan of the packaged image (HIGH/CRITICAL, `--ignore-unfixed`); `compose down` (never `-v`). |

Unlike Flow there is no path filter (`changes` job): browser journeys run on every push and PR inside `image`, and the nightly below repeats them.
A new gate lands in `scripts/check.sh` **and** the matching job in the same commit; the two lists above must stay equal.

## Nightly e2e (`.github/workflows/e2e.yml`)

Ported from Flow. Runs at 03:41 UTC against `master` and on demand (Actions → "e2e" → "Run workflow"). It builds the image with
`docker compose build`, starts the stack (`up -d --wait --wait-timeout 300`), runs `npm --prefix e2e test` (Chromium and WebKit), uploads
`browser-reports`, and Trivy-scans `astrid:local` with the same pin and flags as `image` (`check-toolchain.py` fails if the workflows' Trivy pins
differ). The scan runs after the suite even when it fails, so neither result hides the other. Because Astrid's PR gate already runs the suite, the
nightly is there for quiet days: a fixable HIGH/CRITICAL advisory published after the last merge, or drift in base images or runners, shows up within
a day instead of at the next push. Keep its steps in step with `image` when either changes.

## Failure alerting (`.github/workflows/ci-alert.yml`)

A reusable workflow, ported from Flow. When a **scheduled** nightly fails, `e2e.yml`'s `alert` job calls it with `issues: write`. It opens an issue
titled "Nightly e2e is failing" labelled `ci-failure`, or comments on the open one with that title, giving the run URL, attempt, date, SHA and trigger.
Manual runs never alert. Nothing closes the issue automatically: close it by hand once the cause is understood. `ci.yml` does not alert, because a red
push or PR is already in front of its author.

## Dependabot and pins

`.github/dependabot.yml` (ported from Flow) checks weekly: `gradle /`, `npm /web`, `npm /e2e`, `docker /` and `/k8s`, `docker-compose /`,
`github-actions /`. Minor and patch bumps are grouped into one PR per workspace (`gradle-minor-patch`, `web-minor-patch`, `e2e-minor-patch`; vitest,
and the Netty, reactor-netty and Exposed families, arrive separately); a major arrives alone. Ignored majors: `typescript` (openapi-typescript does
not support TypeScript 7), `@types/node`, `node` and `eclipse-temurin` (they track the Node 24 and JDK 21 LTS pins in `mise.toml`). Lifting an ignore is
a deliberate decision. Bumps are reviewed by their gates; every Gradle bump needs `verification-metadata.xml` regenerated by hand, and a Ktor catalog bump
its `settings-gradle.lockfile` too (`dependency-reproducibility.md`).

Pins are exact and intentional; update related pins together. `scripts/check-toolchain.py` enforces: `.nvmrc` = `mise.toml` node; the Dockerfile `node`,
JDK and JRE images match the `mise.toml`/CI versions and every `FROM` carries `@sha256:`; CI reads Node from `.nvmrc` and the JDK from `java-version`;
every `uses:` is SHA-pinned; **one** PostgreSQL image (tag + digest) is identical in `docker-compose.yaml`, `k8s/postgres.yaml` and `PostgresTestDatabase.kt`.
npm dependencies are exact versions (no `^`/`~`); Gradle versions live in `gradle/libs.versions.toml`.

## The full stack (Docker Compose)

```sh
cp .env.example .env          # POSTGRES_PASSWORD is required; never commit .env
docker compose up --build -d  # app on http://localhost:8088, PostgreSQL on 127.0.0.1:5438
```

Ports deliberately avoid the siblings so every stack can run side by side: Astrid **8088** (app) / **5180** (Vite) / **5438** (PostgreSQL) vs Lettuce
8080/5173/5432, Toadie 8081/5174/5433, Covenant 8082/5175/5434, Flow 8084/5176/5435. Host ports bind to `127.0.0.1` only. Local development: `docker compose up -d postgres`,
`POSTGRES_PASSWORD=… ASTRID_DEVELOPMENT_MODE=true ./gradlew :server:run` (binds `127.0.0.1:8088`; `ASTRID_BIND_HOST`/`ASTRID_PORT` override), and
`cd web && npm run dev` (Vite proxies `/api` to 8088 and rewrites Origin). The compose file is `docker-compose.yaml` (project name `astrid`).

Dockerfile (3 stages, every image pinned by digest): (1) Node builds `web/dist` with `npm ci --legacy-peer-deps` and reads `.git` for the version stamp
(`GIT_SHA`/`GIT_COMMIT_TIME` build args override; build from a regular checkout); (2) Temurin JDK runs `:server:installDist --dependency-verification strict`
(**never `:server:buildFatJar`** — Flyway's ServiceLoader descriptors collapse in a fat jar); (3) Temurin JRE with the install and the SPA only (no Git, no
Gradle), uid 10001, `ASTRID_DEVELOPMENT_MODE=false`, `ASTRID_BIND_HOST=0.0.0.0`, `HEALTHCHECK` on `/api/v1/ready` with a 240 s start period. `.dockerignore` keeps
`.env`, `docs/`, build output and `node_modules` out of the context. Compose runs the image read-only with `cap_drop: [ALL]`; no JVM tuning flags are baked in
(measure first: BL-044).

## Kubernetes (`k8s/`, kustomize)

`kubectl apply -k k8s` installs namespace `astrid`, ConfigMap, a PostgreSQL Deployment (one replica, `Recreate`, 1 Gi PVC) and the app Deployment+Service
(**one replica**, port 8088, startup/readiness probes on `/api/v1/ready`, liveness on `/health`, `768Mi` limit). It is a **private** deployment: no Ingress, reached
with `kubectl -n astrid port-forward service/astrid 8088:8088`. The Secret is created by hand from `.env` and is not part of the kustomization (`security.md`). The image
`astrid:local` must exist in the cluster's image store; for a remote cluster publish a uniquely tagged image and change `k8s/app.yaml` — never rely on a mutable shared tag.
Check the kube context before applying; all commands touch only the `astrid` namespace. Changing probes or timeouts means re-running `check-startup-budget.py`.
