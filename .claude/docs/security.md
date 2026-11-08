# Security

Astrid is a **private development tool**, not a public service: loopback-only ports, a cookie "development session" instead of
accounts, no JWT, no mail, no audit trail, no rate limiting. Everything below defends that model; nothing here makes it safe to
expose publicly. Keep the deployment private (`README.md`; k8s is reached by `port-forward`, no ingress).

## Authentication: the development session

- `POST /api/v1/development-session` creates (or reuses) a session: 32 random bytes, base64url (43 chars) in the `astrid_session` cookie
  (`HttpOnly`, `SameSite=Strict`, `Path=/`, 24 h `Max-Age`, `Secure` only when `ASTRID_SECURE_COOKIE=true`). Only the SHA-256 **hash**
  is stored (`development_sessions.token_hash`); the TTL is fixed, never renewed by use. Reading the changelog creates no session.
- Everything else authenticates through `Authenticator` (`authz/Principal.kt`): `DevelopmentSessionAuthenticator` is the **only**
  implementation and returns a `SessionPrincipal(id, expiresAt)`. Handlers call `call.caller(authenticator)` (memoized per call) and only
  ever see the principal. The whole API is **404 when `ASTRID_DEVELOPMENT_MODE` is not `true`** (fail closed: the image bakes `false`; compose
  and the k8s ConfigMap opt in), then 401 without a well-shaped, unexpired token.
- A real login slots in as a second `Authenticator` that returns a `SessionPrincipal` and replaces `DevelopmentSessionAuthenticator` at its
  single construction site in `astridModule`; routes and guards do not change. It is **not** free: stores and foreign keys are scoped by
  `development_sessions.id`, so accounts need a migration and a product decision first (see
  [implementation decisions](../../docs/knowledge/implementation-decisions.md)). The mutation-header, origin and body guards stay.

## Request guards, in order

All run before routing as one interceptor (`plugins/SecurityHeaders.kt`) and answer `403`:

1. Security headers on every response: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: no-referrer`,
   `Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; object-src 'none'; frame-ancestors 'none'`,
   and `Cache-Control: no-store` on `/api/` (also on the 403s). No inline scripts: do not add one; `'unsafe-inline'` exists for Mantine styles only.
2. **Host allowlist** (`ASTRID_ALLOWED_HOSTS`, case-insensitive, compared with port): default `localhost:<port>`, `127.0.0.1:<port>`, and the
   Vite dev host `localhost:5180`/`127.0.0.1:5180`. A missing `Host` is rejected. Probes must send an allowed `Host` (`k8s/app.yaml` sets
   `Host: localhost:8088`; the Docker `HEALTHCHECK` curls `127.0.0.1:8088`).
3. **Origin allowlist** (`ASTRID_ALLOWED_ORIGINS`): if an `Origin` header is present it must be listed (defaults follow the port and 5180).
4. **Forwarded headers**: any of `Forwarded`, `X-Forwarded-For/-Host/-Proto` is rejected unless `ASTRID_TRUST_FORWARDED_HEADERS=true`. Trusting
   them does not relax the host/origin lists. Behind any proxy set both lists explicitly; a non-default `ASTRID_PORT` moves only the defaults.
5. **Body limit**: `RequestBodyLimit` 16 KiB (`MAX_BODY_BYTES`) → `413 payload_too_large`. Strict JSON (`ignoreUnknownKeys = false`), strict
   UUID parsing (`parseStrictUuid`), bounded identifiers, text limits counted in code points.

Then, per handler (`conventions.md`): `requireMutationHeaders` (`X-Astrid-Client: playground`, else `403`; `Content-Type` missing → `400`,
not JSON → `415`) → `caller` (`404`/`401`) → `requirePracticeOwner` (`X-Astrid-Practice-Owner` header must equal the session id:
absent `400`, mismatch `409`) → body decode (`400`). Cookie auth is protected against cross-site writes by `SameSite=Strict`, the Origin allowlist
and the required custom header + JSON media type together; keep all three on any new mutation.

**Status ordering is a contract** (tested in `ApplicationTest`, `OpenApiContractTest`): on mutations 403/400/415 from the headers come before
404/401 from the session; `POST /development-session` checks development mode first (404) then the headers; GETs go straight to `caller`. Do
not reorder guards in a handler "for tidiness". Resources of another session are `404`, never `403`.

## Inspector

`GET /api/v1/dev/exercises/{id}` reveals the answer key and generation details. It exists only when `ASTRID_DEVELOPMENT_MODE=true`,
`ASTRID_INSPECTOR_ENABLED=true` **and** `ASTRID_INSPECTOR_KEY` is at least 32 characters (otherwise disabled and `404`; a shorter key silently
disables it). Order: mode check (`404`) → `X-Astrid-Inspector-Key` (constant-time compare via `MessageDigest.isEqual`, `403`) → session (`401`) →
ownership (`404`). The key never reaches the frontend build or persistent browser storage. Generate with `openssl rand -hex 32`.
Public DTOs never carry solutions (`conventions.md`).

## Secrets

- Real values live only in `.env` (git-ignored, also `.dockerignore`d; `.env.example` has placeholders only) and in the k8s Secret
  `astrid-secrets`, created from it: `kubectl -n astrid create secret generic astrid-secrets --from-env-file=.env`. Compose refuses to start without
  `POSTGRES_PASSWORD` (`${…:?}`). In k8s both the ConfigMap and the Secret are `envFrom` sources; on a duplicate key the later one (the Secret) wins,
  so keep `.env` free of values you do not want to override the ConfigMap. Rotating means updating the Secret and restarting; the PostgreSQL
  password must stay the one that initialized the volume.
- `ServerConfig` falls back to `POSTGRES_PASSWORD=astrid` only for tests/native development; never rely on it elsewhere.
- Never print a secret, a cookie, an inspector key or a full URL (JDBC/R2DBC URLs carry credentials in some forms) in logs, docs, test output or
  commit messages.

## Logging

Logback to stdout only (`server/src/main/resources/logback.xml`; no OpenTelemetry). `CallLogging` (`plugins/Http.kt`) logs `METHOD /path status` for
`/api/` calls only, **never the query string, headers or cookies**, and skips `/api/v1/health` and `/ready`. `respondProblem` puts only
`request.path()` in `instance`. Unhandled failures log the exception server-side and answer a generic `500 internal_error`; do not echo exception
text to the client. `CallLoggingTest` pins the no-leak behaviour.

## Container and cluster

Image: non-root uid 10001, no Git/tooling in the runtime stage, `ASTRID_BIND_HOST=0.0.0.0` inside the container only. Compose: ports bound to
`127.0.0.1`, `read_only: true`, `cap_drop: [ALL]`, `no-new-privileges`, `/tmp` tmpfs. Kubernetes: `runAsNonRoot`, `readOnlyRootFilesystem`,
`allowPrivilegeEscalation: false`, all capabilities dropped, `RuntimeDefault` seccomp, no service-account token, one replica, no Ingress; there is
no NetworkPolicy yet. Dependencies: strict Gradle locking and checksum verification, `npm audit --audit-level=high` in CI and a Trivy scan of the
locked dependencies and of the packaged image (`ci.md`, `dependency-reproducibility.md`).
