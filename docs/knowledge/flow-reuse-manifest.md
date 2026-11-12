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

**Final exact-source Linux server result.** The server job in
[CI run 43024813909](https://github.com/liveweird/astrid/actions/runs/43024813909)
at source `0f1b322` passed. Its Gradle verification step completed in 5m57s under the
unchanged test budget; `OpenApiContractTest` reported a 512 MiB maximum heap, four
available processors and G1 collectors. This closes the Linux uncertainty for the
lazy, response-only, two-entry test harness. It is test-only CI evidence, not a
production-runtime memory measurement or JVM-tuning result. BL-044 and BL-055
remain separate work.

## Container-based CI browser execution — 11 November 2026

S13 source `f3c5249` passed the complete local gate, all 430 native journeys, and four
[CI jobs](https://github.com/liveweird/astrid/actions/runs/43037116250). Its two image-job
attempts were stopped before browser tests: host Playwright dependency installation
repeatedly retried Ubuntu package fetches and recorded DNS failures resolving
`azure.archive.ubuntu.com`. The first application Docker build also recorded Ubuntu
DNS warnings. These observations establish those runner failures, not a global outage.
Logs and both stopped attempts remain retained as evidence.

**Implementation decision:** port the existing packaged-Linux QA execution pattern into
one shared CI/nightly runner using the same digest-pinned Playwright image. This removes
host browser-library installation while preserving both browser projects, the full suite,
configured reporters/workers/retries/timeouts, audits, runtime scan, artifacts and cleanup.
Flow's browser cache still calls host `install-deps`, so it does not remove this dependency.
The image includes browser system libraries but requires the matching installed package.
[Playwright Docker documentation](https://playwright.dev/docs/docker)

An actual probe of the pinned amd64 image reported bundled Node `v26.13.0`, while
Astrid pins `24.22.1`. Mount and explicitly execute the setup-node Linux binary instead;
check the version/path in CLI and inherited workers. The isolated probe verified
Node `v24.22.1`, Playwright `1.64.1`, Chromium `156.0.8078.80` and WebKit `27.2` opening
the real QA app without host APT. This is a CI-only execution correction; application
behavior, npm/Gradle dependencies, JVM settings and the app version remain unchanged. Final delivery
requires fresh exact-source/image evidence and CI success for the new runner commit.

The first actual shared-runner probe exposed `EROFS` when Playwright tried to
remove an output directory mounted beneath the read-only checkout. The corrected
runner writes ordinary child directories beneath a separate private writable
artifact parent, then exports reports/traces to the original host upload paths.
Only artifact routing changes; test projects, reporters, workers and limits do not.

The actual container runner passed 34 focused Chromium/WebKit journeys against
the S13 QA image, including real commit/time identity, lazy-route recovery, S12/S13
lifecycle and both-theme enlarged-text checks. CLI and forked workers reported
Node `v24.22.1` through `/astrid-node/node`; HTML and result artifacts exported.
Intentional CLI failure and SIGTERM probes preserved exit codes 1 and 143 and
left no owned container behind. Independent review also verified that ownership,
removal or export errors cannot turn a passing browser run into a false green.
These were runner acceptance checks; final exact-source CI and release remained required
at that stage.

## Runtime APT fail-closed bootstrap — 11 November 2026

The corrected browser runner reached all 430 Linux journeys in about 30.5 minutes in
[CI run 43047887486](https://github.com/liveweird/astrid/actions/runs/43047887486) at
`ecc3463`, without retries or flakies. The later packaged-image scan nevertheless failed on
two fixable HIGH findings: Noble's `apt` and `libapt-pkg6.0t64` stayed at `2.8.3` while
`2.8.4ubuntu0.1` was available. During the runtime image build, an Ubuntu index DNS fetch
warned but returned success; the following full upgrade reported zero changes. Thus the
browser execution correction worked, while the separate runtime-image gate correctly stayed
red. This is not evidence of an application or browser-test defect.

Astrid ports Flow commit `3c9cfc52`'s minimum-version guard pattern only. The runtime keeps
its full Ubuntu upgrade, curl install, fixed non-root user and pinned Temurin base. It now
makes any APT index error fatal, disables acquisition retries for the bounded bootstrap, and
requires both packages at least `2.8.4ubuntu0.1`. Signed repositories and TLS verification
remain enabled. Canonical's
[APT vulnerability notice](https://ubuntu.com/blog/apt-vulnerability-fixes-available)
records the fixed Noble revision.

The exact base image succeeded against the current repositories in local arm64 and amd64
probes. The amd64 probe completed the full guarded upgrade and reported both packages at
`2.8.4ubuntu0.1`; an intentionally unreachable index proxy returned status 100 with retries
disabled. Those observations validate the local success and fail-closed paths, but do not
establish GitHub-hosted network behavior or close the CI gate. A fresh exact-source image
build and clean runtime scan remained required. Mirror changes were outside the initial fix; the separately reviewed paired probe and architecture decision below
provide the later evidence.

### Scoped hosted mirror selector

The paired diagnostic supplied the missing hosted evidence. Run
[`43055420763`](https://github.com/liveweird/astrid/actions/runs/43055420763) reproduced DNS
failure on the GitHub host and in Docker builds using both default and host networking. The saved official Canonical status
feed ([live RSS](https://status.canonical.com/rss)) reported **DNS — Major Outage** at
14:26 UTC on 11 November. A second hosted run
[`43056349808`](https://github.com/liveweird/astrid/actions/runs/43056349808), diagnostic source
`59be95a`, completed the current full guarded bootstrap using the kernel.org Ubuntu mirror over
HTTPS, installed both fixed APT packages and passed the release-policy Trivy scan with zero
findings. Those diagnostics supported the bounded selector; final release acceptance followed
in the separately verified source below.

The selector is closed rather than configurable infrastructure: its default is empty, and its
only accepted nonempty value is `https://mirrors.edge.kernel.org/ubuntu/`. CI/nightly opt in,
and any nonempty use requires amd64. Before changing anything it requires the pinned base's
exact two Noble source stanzas, including archive/security URIs, suites, components and Ubuntu
archive keyring. It then rewrites only the two `URIs:` fields to the same allowed HTTPS mirror.
A base shape change, other URL or unsupported architecture fails closed. Both CI and nightly
pass the value explicitly to their existing direct/Compose build commands; local Compose,
native arm64, signatures, TLS/validity checks, full upgrade, APT floors and runtime scan remain
unchanged.

The selected endpoint is operated by the [Linux Kernel Archives](https://www.kernel.org/) and
appears in Ubuntu's [registered archive mirrors](https://launchpad.net/ubuntu/+archivemirrors).
After a separately reviewed healthy upstream probe, remove both workflow build arguments and
the pin-checker correlation assertion; normal empty/default builds then resume without changing
the fail-closed index handling, retry mitigation or version floors. The failed `ecc3463` scan
and paired outage diagnostics remain historical evidence and are not superseded by removal.

### Final S13 verification

Source `9bb10ae` passed all five
[release CI jobs](https://github.com/liveweird/astrid/actions/runs/43058818834), including
430 Linux browser journeys without retries/flakies and the unchanged packaged-image scan.
The image job took 1,711 seconds; browser execution used 1,289 seconds and the runtime scan
35 seconds. The 40-minute budget, test policy, coverage floors and production JVM settings
remain unchanged. These observations close the pending integrated validation above.

The exact native `linux/arm64` image also passed Trivy and 430 browser journeys without
retries. Its ordinary default repositories produced both APT packages at `2.8.4ubuntu0.1`.
Verified isolated backup restoration preceded app-only promotion; all 136 live-preview checks
and owned QA cleanup passed. The [S13 delivery record](module-one-course-slice.md#observed-s13-delivery)
records the source/image, immutable-history and database-process safeguards.

### Retained schema references for P1 — 12 November 2026

The unpublished P1 response contract exhausted the same 512 MiB test heap even with a
compact seven-branch `oneOf`. Failure occurred in schema transformation before body
validation (`RequiredFieldTransformer` / `readAndTransformSchemaObject`), so dropping
format/target correlations would hide the contract rather than resolve the cause.
Independent inspection of pinned validator 3.0.0 confirmed support for component references
and component-definition injection. Flow's cache pattern remains the donor; the parser
configuration below is Astrid's bounded adaptation, not a claimed existing Flow solution.

The shared lazy builder now sets `resolve=true`, `resolveFully=false`,
`resolveResponses=true`, `resolveRequestBody=true`, `resolveCombinators=false` and
`explicitObjectSchema=false`. Resolving the shared response/request-body references is
necessary to preserve referenced Problem and strict request validation when full schema
dereferencing is disabled. Keep the two-entry compiled cache, all resolver levels,
positive/negative matrices, timeouts, 512 MiB heap and coverage floors. Complete closed
P1 response leaves retain exact format/facet/direction/scope and revision correlations;
old payloads and production JVM settings are unaffected.

The original focused request/runtime/Problem checks passed in about three seconds. An
initial OpenAPI run completed without OOM but exposed separate contract/lifecycle defects;
that run is not a clean acceptance claim. After correction, the current full class's JUnit
report contains **24 tests, zero failures/errors/skips, 7.905 seconds** at the unchanged heap.
The full server check and complete Docker-backed repository gate also passed. Exact-source
Linux CI remains required before release. No peak-memory or production-performance measurement
is claimed.
