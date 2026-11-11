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
| JVM tuning and class-data archive | Omitted from the initial milestone; runtime settings remain unchanged | Measure Astrid before adding optimization (BL-044); controlled comparison stopped for a browser failure; see below |
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

An independently reviewed **measurement protocol** compares stock
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

### Interrupted comparison — 10 November 2026

The accepted collection stopped at case four, as required by its no-retry protocol. Stock,
Flow-basic and Flow-full each completed one 372-journey native run and the API cohorts. The
second Flow-full run recorded 336 passing journeys, one WebKit matching-regeneration timeout
and 35 not run. The failure waited for a Danish-word button to become stable for clicking;
its captured UI showed the expected English selection and an enabled Danish button.
The 105 memory samples recorded no observed OOM kill and at most 185.0 MiB sampled process
RSS in that case. Sampling cannot exclude an event between observations or establish cause.

All trace/screenshot/log/result evidence is retained. Ownership-checked cleanup completed,
with main application/database identities, processes and mounts unchanged. Three complete
cases do not supply the planned three repetitions per variant: there is **no completed 3×3
comparison**, latency ranking, tuning acceptance or JVM-flag adoption. Diagnose the browser
failure before any separately recorded collection; do not replace this failed case with a
passing retry or pool incompatible workloads. BL-044 remains unselected.


The three complete cases are exploratory observations, **one case per variant**, not medians:

| Variant | Sampled peak process RSS (MiB) | Process RSS high-water (MiB) | Cgroup peak (MiB) | Serial / eight-client grade p95 (ms) |
| --- | ---: | ---: | ---: | ---: |
| Stock | 232.3 | 232.3 | 229.5 | 5.699 / 12.302 |
| Flow first four flags | 151.4 | 161.0 | 181.1 | 8.927 / 22.462 |
| Flow ten flags | 192.4 | 192.4 | 214.1 | 5.471 / 12.397 |

These are local HTTP round trips over five mixed standalone formats, with 120 serial and
240 eight-client measured grades after each worker's ten warm-up pairs. Public-result facet
hashes matched across the three cases. Stock already selected Serial GC with a 192 MiB maximum
heap under the imposed limits; both Flow variants used Serial GC and 256 MiB. The different
heap caps, observer overhead, warm-up and one-case sample prevent individual-flag attribution
or production latency/memory conclusions. The earlier warm unlimited process is not a paired
control for these fresh constrained processes.

### Browser failure investigation and decision boundary

Static trace review confirms regeneration reset the board, the first pair completed, and the
next English word was selected. The timeout occurred before the Danish click was dispatched;
no application error or pending API request appeared. Relevant exercise responses completed
in about 8 ms. [Playwright's actionability definition](https://playwright.dev/docs/actionability)
uses stable geometry over animation frames; the waiting stage is consistent with an
animation-frame/actionability stall, but its cause is unconfirmed.

A separately recorded fresh native diagnostic then aborted at WebKit launch with SIGABRT,
before a page or app request. A server-free WebKit control did the same. Code-signature checks
passed; native diagnostic services were unavailable, so no current assertion/backtrace was
obtained. This **later launch failure is distinct from the original in-page timeout** and
does not establish its cause. The unchanged regeneration test passed once without retries
in pinned Linux WebKit against the same release image, full flags and resource envelope.
That is an independent diagnostic, not a replacement for the failed native measurement.

**Current boundary:** retain default preview settings. Resolve/reproduce the native browser
issue and select a fresh, explicitly identified comparison before reconsidering JVM adoption.
No force-click, larger timeout, retry workaround, runtime change or teaching-content correction
was introduced. BL-055 captures the unresolved diagnostic follow-up; BL-044 remains a separate
unselected tuning proposal. Ownership-checked diagnostic cleanup completed; main app/database processes and mounts
remained unchanged.

## Test-only API schema cache — 11 November 2026

The larger first-step contract reproduced a heap failure in `OpenApiContractTest`.
The pinned validator expands schema references and injects a copy of all definitions
into each compiled-schema cache entry. Its default maximum is 100. The S10 integration
adopted Flow's `ValidationConfiguration.setMaxCacheSize` pattern, with four entries for all
strict, response-only and request-matrix builders. The donor is
`server/src/test/kotlin/OpenApiConformance.kt` at Flow commit
`4e43e48069cd5781c96332c2e660505ea358f644`.

The isolated S10 rehearsal reproduced the failure at cache maxima 100 and 20. The
unchanged compiled journey passed at four entries in 5.29 seconds and at two entries
in 15.25 seconds in 512 MiB JVMs. Post-GC live heap was 339 MiB and 255 MiB respectively,
including JShell overhead. These are diagnostic measurements. The normal Gradle/Kover
gate supplies repository verification. The gate exercises parser defaults, complete schema validation,
positive/negative correlations and referenced error responses.

### Lazy strict-validator construction for S11

The combined S11 server suite exceeded its existing 512 MiB heap with eagerly constructed
response and strict-request validator models. Constructing the strict request validator on
first use retained the same builder, parser settings, four-entry cache and assertions.
The full 219-test server suite passed, including all 18 OpenAPI methods in 23.152 test
seconds, with 98.26% line and 81.83% branch coverage. This is a test-lifecycle correction.
Production JVM settings and public contracts retain their existing configuration.


### Two-entry bound for the expanded S12 contract

Candidate `21d3f9c` passed the complete local gate, but [Linux CI run
43018549675](https://github.com/liveweird/astrid/actions/runs/43018549675) exhausted the
unchanged 512 MiB server-test heap in four `OpenApiContractTest` methods. The log records
`OutOfMemoryError` while expanding/copying schemas and in cache maintenance; 231 tests
ran and four failed. This is distinct from the earlier schema-correlation corrections.

S12 reduces the compiled-schema bound from four entries to two through the same
Flow-derived builder, retaining lazy strict-validator initialization, parser settings,
all positive/negative assertions and the existing heap and coverage floors. Production
JVM settings and application behavior are unchanged. On the current source, all 19
OpenAPI methods passed locally in 107.90 seconds and the full 231-test server check
passed in 141.61 seconds, including detekt and Kover. The final Linux gate is required
before release; its outcome belongs in the [S12 delivery record](module-one-course-slice.md#s12-adding-when-and-where).


### Response-only validation and lazy models for S12

The two-entry bound alone did not pass Linux CI. [Run 43021145158](https://github.com/liveweird/astrid/actions/runs/43021145158)
at `0502670` ran 231 tests with ten failures. Its first Module 1 journey hit the one-minute
coroutine deadline at 61.16 seconds, with no suppressed OOM; the next request matrix failed
while deep-copying schema definitions. The four-entry run had already failed during schema
transformation before any timeout. Timeout-induced retention is not established.

Inspection of pinned validator 3.0.0 bytecode confirmed that `validate(request, response)`
still compiles UUID path schemas before filtering ignored request reports. Each cache miss
copies all expanded definitions twice; a cache entry limit does not bound transient bytes.
The eager response validator also built a second unused model in request-only tests.

S12 now constructs both validators lazily and uses `validateResponse(path, method, response)`
in its three response helpers. This API uses the same operation resolver. New negative
checks preserve unknown-path, unsupported-method, undeclared-status and invalid-body
rejection; existing problem media-type and 201 Location checks remain intact. Strict request
matrices, resolver levels, parser settings, two-entry bound, 512 MiB test heap, timeouts and
coverage floors are unchanged. One test-only diagnostic records heap size, processor count
and collector names without logging environment values.

On the current local toolchain, all 20 OpenAPI methods passed in 62.37 seconds and the full
232-test server check passed in 84.83 seconds, compared with 107.90 and 141.61 seconds for
the two-entry combined-validation path. These are local measurements, not a claimed Linux
pass. The exact-source Linux job remains the release gate. Application code, migrations,
public schemas, dependencies and production JVM settings are unaffected.
