# Flow Reuse Manifest

## Decision and provenance

7 November 2026: implement the user-authorized playground using Flow's tested stack. Donor: `liveweird/flow`, commit `4e255e966804c5750c8fe0f1928a1461c17cc96f`. Flow remains read-only. Its MIT notice is preserved in root `THIRD_PARTY_NOTICES.md`.

| Boundary | Treatment | Reason |
| --- | --- | --- |
| Gradle wrapper, JDK/Node pins, version catalogue entries | Copy selected pins and wrapper; resolve Astrid's own graph and locks | Shared tooling without unrelated features |
| Dependency checksum metadata | Bootstrap from verified Flow metadata; verify strict resolution | Preserve checksums; no lenient verification |
| Ktor, Exposed R2DBC, PostgreSQL, Flyway | Adapt patterns into Astrid's server | Shared operational experience; forward-only schema |
| Ktor response compression | Adapt Flow's pinned plugin for qualifying static JS/CSS | [Measured transfer gap and verification](static-delivery.md); preserve API/cache/security boundaries |
| React, TypeScript, Mantine, Inter, theme and shell | Adapt visual conventions and component patterns | User explicitly wants the sibling-app family |
| App version, version stamp, lazy changelog and seen-state patterns | Adapt Flow's English release content, build metadata and navigation to Astrid | [User-authorized versioning port](app-versioning.md); keep learning revisions separate |
| OpenAPI, generated client types, validation and test approach | Adapt for the smaller contract | Detect API drift and test real persistence |
| Docker image, Compose, Kubernetes, CI | Adapt private single-app deployment; retain digest/action pins | User explicitly retained both deployment options |
| E2E scenario files, their parity check, nightly e2e and failure-alert issue | Port; scenario headings keep template-literal placeholders instead of Flow's registered skips; the nightly repeats the per-push suite rather than replacing it | Reviewable test intent and quiet-day coverage (image CVEs, drift); see `.claude/docs/ci.md` |
| Flow authentication, ingestion, telemetry, admin tools and domain model | Omit | Unrelated to validating Danish learning |
| i18next EN/PL localization | Omit | Single-language learning UI; Danish is content, not chrome |
| JVM tuning and class-data archive | Omitted from the initial milestone; runtime settings remain unchanged | Measure Astrid before adding optimization (BL-044); controlled comparison is in progress below |
| Ktor `application.yaml` / `EngineMain` configuration | Omit | `embeddedServer` + explicit wiring; no `application.yaml`. `Main.kt` builds `ServerConfig` from the environment and passes stores and clock as explicit parameters, so tests inject fakes directly; the unused `ktor-server-config-yaml` dependency was removed |
| Lesson generation and assessment | New pure Kotlin module | Astrid-specific typed language model and deterministic rules |

## Reproducibility and conventions

Use Java 21, Node 24 and the exact versions in `mise.toml`, `.nvmrc`, the Gradle catalogue and npm lockfile. Commit Astrid's dependency locks; do not copy Flow's resolved project graph. The narrow `npm ci --legacy-peer-deps` exception matches Flow's TypeScript 6 / openapi-typescript peer declaration and does not disable audit or type checking.

Keep the 48px header, sidebar/rail, responsive navigation, light/dark theme and Inter typography. Astrid uses a Danish red accent with separately labeled learning feedback. The [selected girl-with-plaits logo](visual-identity.md#selected-logo--8-november-2026) shipped on 8 November, superseding the initial provisional-mark note. Palette, monochrome and possible app-icon work remain BL-021; production lesson media remain BL-015.

## Validation boundary

Required checks cover engine invariants and worked cases, real PostgreSQL migrations and request isolation, API contract, frontend interaction and retry behavior, packaged application startup, deployment configuration, and independent review. A successful software test does not make draft Danish material teacher-approved or demonstrate learning effectiveness.

## Runtime-footprint observations and comparison protocol — 10 November 2026

**Observed baseline, not a tuning decision:** during 372 passing native browser journeys,
the released `0.9.0` process used 502.7–543.8 MiB sampled RSS. The 98 samples covered 484.85
seconds. Its Docker memory limit was unset; `memory.current` ranged 493.3–538.7 MiB.
The container was already warm and had served earlier smoke checks. Sampling included probe
processes; lifetime high-water counters were not reset. These observations do not establish
behavior under the declared Kubernetes ceiling of 768 MiB / 2 CPUs.

An independently reviewed **measurement protocol**, currently in progress, compares stock
settings, Flow's first four flags, and its full ten-flag set at donor
`4e43e48069cd5781c96332c2e660505ea358f644`. This later inspection does not replace the initial
scaffold-donor provenance above. No flags or class-data archive have been adopted.

- Use the same released image, 768 MiB app limit, 2 CPUs and no app swap in every case;
  balanced case order supplies three repetitions per variant. PostgreSQL is one unrestricted
  shared process with a fresh logical database per case. This is not a full Kubernetes or
  cold-host simulation.
- Verify the measured PID's exact JVM arguments, actual collector/heap/CPU initialization,
  and absence of alternate option channels. Identical GC observation logging is enabled in
  every variant; its overhead is part of the experiment.
- Hash the test tree, installed dependencies, Node and browser executables before/after each
  case. Run all 372 browser journeys without retries, then single-client and eight-client
  public generation/grading cohorts, followed by 30 seconds idle. Compare deterministic
  public-result facets without UUIDs; retain each failed case as a failure.
- Keep observed readiness, Compose health completion, browser duration and measured-case
  elapsed time distinct. Keep sampled RSS, process high-water RSS and cgroup peak distinct.
  Bound all case phases and remove only ownership-checked test resources; preserve the
  main app/database identities, processes and mounts.

The initial client-cookie parsing failure and a later pre-control exploratory attempt are
retained separately and excluded from the comparison. The accepted collection starts only
with the corrected client and reviewed controls. Conclusions and any adoption proposal will
follow completed measurements; BL-044 remains unselected.

Interpretation references: Docker documents [resource and swap constraints](https://docs.docker.com/engine/containers/resource_constraints/);
Java 21 documents [container awareness, CPU overrides and heap ergonomics](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html);
the kernel defines [cgroup current/peak accounting](https://docs.kernel.org/admin-guide/cgroup-v2.html).
