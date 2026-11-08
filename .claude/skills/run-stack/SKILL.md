---
name: run-stack
description: How to run, package and deploy the Astrid stack — docker compose, local dev (gradle + vite), installDist packaging, local Kubernetes (kustomize) — and how to inspect and tear it down safely.
---

# Running, packaging and deploying the Astrid stack

Ports (bound to `127.0.0.1`, chosen not to collide with the siblings Lettuce 8080/5173/5432, Toadie 8081/5174/5433, Covenant 8082/5175/5434, Flow 8084/5176/5435):
app **8088**, Vite **5180**, PostgreSQL **5438** (host) → 5432 (in-container). Always check before starting: `lsof -nP -iTCP:8088 -iTCP:5180 -iTCP:5438 -sTCP:LISTEN`.

## Packaging

`./gradlew :server:installDist` → `server/build/install/server/` (launcher `bin/server`). **Do not use `:server:buildFatJar`**: the duplicate Flyway
`META-INF/services` descriptors collapse and the jar fails at startup. No JVM tuning flags are baked in (measuring them is BL-044); override per run with `JAVA_OPTS`.
The Gradle build is strict-locked and checksum-verified (`.claude/docs/dependency-reproducibility.md`); never pass `--dependency-verification lenient|off`.

## One command (assembled stack)

```sh
cd /Users/sebastian/Sources/astrid
cp .env.example .env              # once; POSTGRES_PASSWORD is mandatory; never commit .env
docker compose up --build -d      # builds the SPA + server image, starts PostgreSQL, runs Flyway, serves http://localhost:8088
docker compose ps                 # app should become healthy (HEALTHCHECK start period is 240 s)
```

Open `http://localhost:8088` (SPA at `/`, practice at `/practice`, notes at `/changelog`). The app creates its own browser development session; there is no login. Rebuild after code changes
with `docker compose up --build -d` (the web stage reads `.git`, so build from a regular checkout). `docker compose logs -f app` shows the
`METHOD /path status` access lines and startup. Database shell: `docker compose exec postgres psql -U astrid -d astrid` (read-only inspection; no ad-hoc deletes).

**Teardown:** `docker compose down` keeps the database volume. **Never `docker compose down -v`** unless the user explicitly wants all learner history deleted.
In CI and scripts use `docker compose --env-file .env.example …` (`ci.md`).

## Local development (hot reload)

In separate terminals:

```sh
docker compose up -d postgres                                   # PostgreSQL on 127.0.0.1:5438
export POSTGRES_PASSWORD=astrid-local-development                # same as .env.example
ASTRID_DEVELOPMENT_MODE=true ./gradlew :server:run              # API on 127.0.0.1:8088 (no SPA: WEB_STATIC_DIR unset)
cd web && npm ci --legacy-peer-deps && npm run dev              # Vite on 127.0.0.1:5180, proxies /api -> 8088
```

Drive `http://localhost:5180`. Without `ASTRID_DEVELOPMENT_MODE=true` every session-scoped API answers 404 by design. `ASTRID_PORT` / `ASTRID_BIND_HOST` change the server's
listen address (a non-default port moves the default allowed hosts/origins but not the Vite proxy target, which stays 8088). The inspector needs `ASTRID_INSPECTOR_ENABLED=true` and an
`ASTRID_INSPECTOR_KEY` of 32+ characters (`openssl rand -hex 32`); restart after changing it. Other databases: set `POSTGRES_JDBC_URL`, `POSTGRES_R2DBC_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` together.
Tests that need Docker under OrbStack: `export DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock`.

## Local Kubernetes (OrbStack or another local cluster; private, one replica)

Check `kubectl config current-context` first; everything lives in the `astrid` namespace.

```sh
docker build -t astrid:local .                       # the cluster must see this image (OrbStack shares Docker's store)
kubectl apply -f k8s/namespace.yaml
kubectl -n astrid create secret generic astrid-secrets --from-env-file=.env
kubectl apply -k k8s
kubectl -n astrid rollout status deployment/astrid
kubectl -n astrid port-forward service/astrid 8088:8088      # the only way in: no Ingress
```

`k8s/configmap.yaml` sets `ASTRID_DEVELOPMENT_MODE=true`; the Secret (from `.env`) supplies `POSTGRES_PASSWORD` and optional inspector settings and wins on duplicate keys. Roll out a new build by
rebuilding the image and `kubectl -n astrid rollout restart deployment/astrid` (the tag is mutable and local; for a remote cluster publish a unique tag and edit `k8s/app.yaml`). The PostgreSQL
Deployment uses a 1 Gi PVC with `Recreate`: do not delete the PVC to "reset". Back up first (`pg_dump` custom format, mode 0600) before a migration-bearing rollout (`.claude/docs/persistence.md`).
**Teardown:** `kubectl -n astrid scale deployment/astrid --replicas=0` keeps data; deleting the namespace deletes the PVC.

## Rules of thumb

- Stop only what you started; verify PIDs/containers with `lsof`/`docker ps` before killing, and leave a stack the user was already running as you found it.
- Check readiness with `curl -fsS http://localhost:8088/api/v1/ready` (needs an allowed `Host`; `localhost:8088` and `127.0.0.1:8088` are).
- A "connection refused" on 5438 means PostgreSQL isn't up; a 404 on every API call means development mode is off; a 403 means a Host/Origin/forwarded-header guard (`.claude/docs/security.md`).
