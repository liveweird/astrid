---
name: api-review
description: Validate an OpenAPI spec (default Astrid's own) against api-guidelines/API-GUIDELINES.md — Spectral lint pass plus the LLM review checklist, reported per rule ID.
---

# API review against the guidelines

Validate an OpenAPI document against the authoritative standard in `api-guidelines/API-GUIDELINES.md`, in two passes. The target spec is the argument if one was
given; otherwise `server/src/main/resources/openapi/documentation.yaml`. `api-guidelines/` is a shared rulebook copied from Flow and **never edited in Astrid**;
Astrid's own severity decisions live in `web/spectral.astrid.yaml`.

## Pass 1 — mechanical (Spectral)

```sh
cd /Users/sebastian/Sources/astrid/web
npm run lint:api                       # the CI gate: the spec AND api-guidelines/examples/conformant.yaml, --fail-severity=warn
# ad hoc, another target:
npx --no-install spectral lint <target-spec> --ruleset spectral.astrid.yaml
# the shared ruleset alone (shows the two rules Astrid switches off):
npx --no-install spectral lint <target-spec> --ruleset ../api-guidelines/api-guidelines.spectral.yaml
```

Interpret by severity (model in `api-guidelines/README.md`):

- **error / warning** — a real violation; report it citing the rule ID from the message. Under Astrid's ruleset the project spec has **0 errors and 0 warnings**
  (`lint:api` fails on warn), so any finding on it is new drift: fix the spec, do not add a local `off`.
- **off in `web/spectral.astrid.yaml`** — `sla-extension-present` (API-META-001) and `terms-of-service-present` (API-META-002) are registered gaps: Astrid publishes no SLA or terms.
  Report them only as "registered gap".
- **hint** — registered noisy gaps (`X-Request-Id`, `ETag`, `Idempotency-Key`). Today the project spec yields only these (about 34 hints). Do not list them individually; one summary line
  ("N hints from registered gaps") suffices. Note: Astrid achieves idempotency through client-generated `attemptId` / `practiceSessionId` identities, not an `Idempotency-Key` header
  (`.claude/docs/persistence.md`) — that is the substance behind the registered gap.

## Pass 2 — review (LLM checklist)

Read `api-guidelines/API-GUIDELINES.md` and work through its **LLM review checklist** section against the target spec — and, where a check concerns runtime behaviour (guard-before-validate
ordering, error media type, disclosure policy, `Location` headers), against the route code under `server/src/main/kotlin/app/astrid/server/` (`plugins/ErrorHandling.kt`, `authz/Guards.kt`,
`*/…Routes.kt`). For each checklist item return **pass / fail / N-A** with the rule ID; a fail needs a one-line justification with a file/spec citation. Say N-A explicitly where an item has no
subject — do not skip it silently. For Astrid that is typically:

- **N-A today (no subject):** list/envelope/pagination/sort/filter rules (API-STRUCT-004, API-LIST-*), `PUT` full-replace and delete semantics (API-RES-004/007; Astrid has no list, PUT or DELETE
  endpoints), rate limiting (no limiter exists), ETag/conditional requests (registered gap), collection facets.
- **Applies and is checked in code:** everything under `/api/v1/`; camelCase flat DTOs; epoch-millis instants (API-DATA); `201` carries `Location` (`ExerciseRoutes`, `PracticeRoutes`); every error is
  `application/problem+json` with `type`, `title`, `status`, `detail`, `instance` and Astrid's `code` extension, rendered only through `respondProblem`; the 4xx/5xx statuses each route can answer
  are declared, including `500`; `instance` carries the path only (no query); the cookie `developmentSession` security scheme with `security: []` only on `/development-session`, `/health`, `/ready`;
  `403/415/400` from the mutation guards come before `401/404` from the session; another learner's resource is `404`, never `403`; answer keys never appear in public schemas
  (only the inspector schema); in-version changes are additive (tolerant optional fields, closed `oneOf` correlations extended, retained revision pairs still accepted).
- **Spec ↔ implementation conformance:** `OpenApiContractTest` validates every observed response against the spec; confirm a new operation has its statuses exercised there, that the spec is OpenAPI 3.0
  compatible (`nullable:`), and that `npm --prefix web run check:api` shows no type drift.

Items on the rulebook's **known-gaps register** (its final appendix) are reported as **"registered gap"**, never as findings. If a gap turns out to be closed in code, say so and suggest removing the
register row (the rulebook is shared, so propose it rather than editing it here).

## Report

End with: findings ranked by severity (rule ID + location + one-line defect), then the registered-gap summary, then the checklist pass/fail/N-A counts. If there are zero findings, say the spec conforms
and give the counts.
