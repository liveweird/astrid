# Flow Stack Assessment for Astrid

## Recommendation and status

Reuse Flow's general application foundation: Kotlin and Ktor, PostgreSQL and Flyway, React and TypeScript, its API contract workflow, test infrastructure, and enforceable quality rules. Adapt the application boundaries, learning interface, exercise contracts, and deployment size to Astrid. Following the assessment, the user confirmed retaining Kubernetes and Docker Compose. The user subsequently authorized the engine playground on 7 November 2026. Its concrete stack and reuse boundaries are now recorded in the [reuse manifest](flow-reuse-manifest.md) and [implementation decisions](implementation-decisions.md); the assessment below preserves the original evidence and recommendations.

The user explicitly prefers reuse where it makes sense. Favor a selective port of proven foundation code and conventions over copying Flow's full domain or building a shared framework in advance.

The user clarified that online mobile apps with reuse of the web interface are acceptable. Recommend responsive React/Mantine, a server-side Kotlin engine, and a possible web-wrapper distribution path. Native UI, offline lessons, and multiplatform engine compilation are not current requirements; keep them as possible later choices rather than introducing them now.

The user subsequently required the shared sibling UI style and layout, with Astrid's own accent color and logo. Red is the preferred accent direction; the logo must connect to Denmark or a Danish Astrid character. This settles the family visual-language requirement; see the [visual identity brief](visual-identity.md).

**Decision update, 6 November 2026:** Keep Kubernetes and Docker Compose. This supersedes the earlier suggestion to make Kubernetes optional or defer it. Platform choice is settled; topology, hosting, and production-readiness details remain open. See [Confirmed infrastructure decisions](product-direction.md#confirmed-infrastructure-decisions).

## Evidence and limits

Reviewed the clean local Flow checkout at `4e255e966804c5750c8fe0f1928a1461c17cc96f` on 6 November 2026. Evidence includes application code, build and test configuration, workflows, infrastructure manifests, repository instructions, and design documentation. Configuration takes precedence over descriptive documentation.

Ran `node scripts/check-toolchain-pins.mjs`: passed, reporting Node `24.22.1` and Java `21.0.12.1`. Flow remained unchanged. Full builds, runtime tests, live UI inspection, deployments, vulnerability scans, and GitHub branch-protection settings were not verified. Historic audit results are not fresh test results.

Source links below identify the reviewed revision. Versions describe Flow's snapshot, not a claim that those versions should be copied forever or are the newest available.

## Stack inventory and reuse boundary

| Area | Observed Flow foundation | Recommendation for Astrid |
| --- | --- | --- |
| Backend | Kotlin `2.4.21`, JDK 21, Ktor `3.6.0` on Netty, kotlinx serialization | Keep as the candidate application backend. Put learning behavior behind independent domain interfaces. |
| Persistence | PostgreSQL 18 container line, Exposed `1.6.0` through R2DBC, bounded pooling, Flyway `13.11.1` | Reuse persistence infrastructure and migration discipline; design new learning schemas. Preserve reviewed dependency alignment rather than changing database frameworks without evidence. |
| Web | React `19.3.1` range, TypeScript `6.0.3` range, Vite `8.3.5` range, Mantine `9.7.4` family, TanStack Query, React Router, i18next | Reuse the web foundation, transport, state and theme machinery. Build an exercise-oriented learner interface. Avoid importing unused chart/report dependencies. |
| Build | Gradle `9.8.2` wrapper with checksum, Kotlin DSL and version catalog; independent web and E2E npm packages with lockfiles; mise toolchains | Keep the existing build boundaries and reproducibility approach. |
| Local infrastructure | Multi-stage Docker image, Compose, PostgreSQL, Mailpit, connector stubs | Docker Compose is confirmed. Adapt the local full stack and integration-test configuration; use Mailpit when email is needed. Omit Jira/GitLab stubs and ingestion services. |
| Deployment | Kubernetes web/worker/database manifests and manual local apply helper | Kubernetes is confirmed. Adapt manifests and deployment conventions for Astrid, and complete the production delivery design. Flow's examples do not establish a complete production delivery system. |
| Observability | Structured logs, audit events, health/readiness, OpenTelemetry bootstrap | Reuse the foundation; choose Astrid telemetry and privacy rules explicitly. Flow defaults metrics and traces to disabled. |

Evidence: [version catalog](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/gradle/libs.versions.toml), [Gradle settings](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/settings.gradle.kts), [web dependencies](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/web/package.json), [wrapper](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/gradle/wrapper/gradle-wrapper.properties), [telemetry bootstrap](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/core/src/commonMain/kotlin/OpenTelemetry.kt).

## Application design

Flow uses feature packages, explicit service construction and `Application.attributes`, Ktor configuration modules, and a route/service/DTO pattern. Its configuration order is significant. These are useful conventions for Astrid's application services, but the learning engine should not depend on a database, HTTP server, UI, or telemetry SDK. Use explicit constructor dependencies for the new engine and keep application composition at the boundary. There is no need to add a dependency-injection framework or copy the donor's extensive module wiring into domain logic.

Proposed boundaries are curriculum and vocabulary knowledge, sentence construction, exercise generation, answer evaluation, learner attempts, and progress/review calculation. They are responsibilities to clarify, not a commitment to six deployable services. A modular monolith is a reasonable starting proposal.

Flow distinguishes stored source facts from derived metrics and records the revisions behind derived results. Apply that principle to content revisions, exercise instances, learner attempts, and derived mastery. Do not transplant Flow's raw/norm/metrics schemas or ingestion pipeline.

PostgreSQL can support the new content model without a separate graph or document database being justified yet. Propose relational identities and references for skills, vocabulary senses/forms, prerequisites, content revisions, and attempts, with typed, validated JSONB where variable exercise structures warrant it. PostgreSQL supports [JSONB storage and indexing](https://www.postgresql.org/docs/current/datatype-json.html); the schema boundary remains a design proposal.

Flow's `core` is named as a multiplatform module, but declares only `jvm()` and currently contains JVM OpenTelemetry setup. It is not an existing shared learning or mobile module. A new framework-independent engine can begin on the JVM; cross-platform compilation should be introduced deliberately if on-device execution becomes a requirement. Kotlin's [recommended structure](https://kotlinlang.org/docs/multiplatform/multiplatform-project-recommended-structure.html) separates shared logic from platform entry points and UI.

Evidence: [architecture and conventions](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/CLAUDE.md), [feature template](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.claude/docs/conventions.md), [domain provenance](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.claude/docs/domain-model.md), [core configuration](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/core/build.gradle.kts).

## API design

Reuse the hand-maintained OpenAPI contract, generated TypeScript types, lightweight handwritten transport, Spectral rules with stable IDs, authorization checks, structured problem responses, and runtime response-conformance tests. The in-memory generated-types check is particularly useful: verification detects drift without silently rewriting the expected artifact.

The conformance harness checks responses and whether a requested path/method exists; it deliberately ignores other request-schema validation so malformed-input tests can exercise errors. Astrid still needs explicit request-validation tests. Generated TypeScript declarations also provide no runtime validation of received JSON by themselves. Evidence: [conformance scope and configuration](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/server/src/test/kotlin/OpenApiConformance.kt#L33-L97).

Adapt these conventions for learning:

- Define explicit exercise variants and composed value objects for prompts, tokens, slots, instructions, hints, and feedback. Document bounded session bundles where they prevent excessive round trips; do not force an exercise into an administrative CRUD screen's resource shape.
- Record exercise identity, content/generator versions, and enough generation information to reproduce a reported problem.
- Make answer submissions retry-safe using stable attempt identity and a transactional deduplication rule or idempotency keys. Avoid recording the same learning event twice when a request is retried.
- Define concurrency behavior for progress changes and content editing; last-write-wins is not a safe universal default.
- Design caching separately for versioned published content and personal progress. Flow's current API generally uses `no-store`; selected reports have validators.
- Keep grading rules and valid-answer representations under server authority for an online design. An offline engine would need its own explicit trust and synchronization model.

Flow's API guidelines register incomplete idempotency, conditional writes, and other operational behaviors. These are known donor gaps, not capabilities Astrid inherits by copying the rulebook. The current spec identifies itself as OpenAPI 3.1 but its validators require 3.0-compatible constructs; preserve that compatibility intentionally or change and test the validator stack before relying on full 3.1 features.

Evidence: [API guidelines and gap register](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/api-guidelines/API-GUIDELINES.md), [generated-types drift check](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/web/scripts/check-api.mjs), [OpenAPI validation conventions](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.claude/docs/testing.md).

## UI design and mobile implications

Keep Mantine's component and token foundation, reusable loading/error states, accessible labels, keyboard support, responsive breakpoints, query/session boundaries, and co-located component tests. Following the user's visual requirement, retain the shared family application shell, layout conventions, and UI styling. Build focused exercise content within that shell, with distinct instruction, answer, hint, explanation, and progress states. Dense report tables and charts remain appropriate only where the learning or content-authoring task calls for them. The proposal to adapt learning interactions does not supersede the confirmed shared layout requirement.

Keep interface localization separate from language-learning content. Astrid initially teaches Danish through English; this does not require copying Flow's English/Polish locale coverage. Preserve useful typed-key conventions while designing target-language text, grammatical annotations, and accepted answers as domain content. Mark Danish passages with `lang="da"` within the English interface. A wrong learner answer is a normal learning outcome, distinct from a failed API request.

**Porting hazard:** Flow's search helper explicitly folds `ø` to `o`, `æ` to `ae`, and removes combining marks through NFD normalization; its database search uses `unaccent`. This is suitable for forgiving search, but must not define canonical vocabulary identity or silently equate spellings in Danish grading. Introduce a separate, explicit answer-normalization policy.

Responsive web does not establish native UI reuse or offline operation. Mantine provides [responsive browser styles](https://mantine.dev/styles/responsive/); React Native uses [native primitives](https://reactnative.dev/docs/components-and-apis). The user's online/web-reuse preference supports retaining the web interface. A wrapper, if chosen later, still needs platform input, navigation, session, lifecycle, and distribution testing. A native client could share API contracts and suitable logic while having a different presentation layer, but is not currently needed to satisfy the stated preference.

Evidence: [web conventions](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/web/CLAUDE.md), [search normalization](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/web/src/utils/text.ts#L3-L22), [database normalization](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/server/src/main/kotlin/infra/db/Sql.kt#L12-L31).

## Testing and quality rules

Flow supplies a substantial reusable test approach: Kotlin/Ktor tests against real PostgreSQL through Testcontainers, full Flyway migration application, OpenAPI conformance and operation/status coverage, Vitest and Testing Library, Playwright, accessibility checks, deterministic fixtures, and scenario-to-test parity checks.

The configured backend coverage floors are 98% lines and 85% branches, with serializable classes excluded. Web floors are 99% lines, 98% statements/functions, and 94% branches. These are executable thresholds, not newly measured results. Reuse coverage ratcheting with an intentional Astrid baseline; high percentages do not establish linguistic correctness.

Add learning-specific acceptance checks:

- Independently reviewed grammatical examples and counterexamples; do not let the same generator serve as its only correctness oracle.
- Seeded reproducibility and invariant tests for valid forms, ordering, prerequisites, difficulty constraints, and distractors.
- Grading examples covering valid alternatives, Danish letters, Unicode normalization, punctuation policies, and ambiguous prompts.
- Tests that retries do not double-count attempts and content updates do not silently change the interpretation of past answers.
- Content checks for broken references, missing prerequisites, incomplete forms, source/licence metadata, and review status.
- Keyboard and touch alternatives for ordering tasks, feedback announcements, narrow layouts, and virtual-keyboard behavior.

Reuse detekt, ESLint/SonarJS, Knip, compiler checks, migration immutability, source-size ratchets, and package-boundary checks. Adapt dependency rules to Astrid's modules and keep core logic free of framework dependencies. Flow's documentation-to-test reference checks are useful traceability tools, but a matching test name is not proof that the claimed behavior is tested adequately.

Do not inherit every large-suite optimization: fork balancing, shared database-fixture complexity, `isolate: false`, AppCDS class lists, and grandfathered file-size exceptions solve Flow-specific costs. Start with isolated, fast domain tests and add complexity when measured need justifies it.

Evidence: [server coverage configuration](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/server/build.gradle.kts#L121-L158), [web coverage configuration](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/web/vite.config.ts), [test conventions](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.claude/docs/testing.md), [boundary test](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/server/src/test/kotlin/PackageBoundaryTest.kt), [contract references](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/server/src/test/kotlin/MeasureContractTest.kt).

## Build and delivery

Reuse Gradle locking and checksum verification, aligned dependency families, toolchain drift checks, npm lockfiles, SHA-pinned Actions, dependency update automation, vulnerability scanning, local gate scripts, test reports, and image hardening. Preserve `installDist` packaging: Flow explicitly avoids the fat JAR because of Flyway discovery issues. These improve dependency integrity; live OS-package installation means the image build should not be described as bit-for-bit reproducible. The inspected Flow image gate rejects fixable HIGH/CRITICAL findings, while its npm audit steps cover runtime dependencies at high severity and above. This describes the gate policy, not an observed scan failure.

The web install currently needs `--legacy-peer-deps` for a documented TypeScript/openapi-typescript peer mismatch. If that combination is ported, keep the exception narrow and validated; assess compatible supported versions rather than making arbitrary peer bypasses a standing convention.

Flow runs backend/web/API/static checks on PRs and master pushes, with Markdown-only PR exceptions. Full browser E2E runs nightly and manually. Its three Playwright projects all use desktop Chromium; they are scheduling groups, not browser diversity. For Astrid, propose a small critical learning-flow suite on PRs, a larger nightly suite, and mobile-size/WebKit coverage as the web learner interface emerges. Playwright supports [multiple engines and emulated devices](https://playwright.dev/docs/browsers).

Ensure grammar and vocabulary changes trigger the appropriate content and engine checks, including changes stored in Markdown. Copying Flow's documentation-only skip rules without adapting them could skip verification of executable teaching content.

The inspected image workflow builds and scans images; it does not establish registry publication, deployment promotion, rollback, or backup restoration. Kubernetes templates and local apply scripts are not a complete production CI/CD platform. GitHub branch protections and required review/check settings need separate verification and configuration. Define production artifact publication and deployment controls when hosting is chosen; automated checks only protect merges when repository rules require them.

## Infrastructure changes and constraints

Retain Docker Compose for the local full stack and integration testing, and Kubernetes as a deployment target, following the user's explicit decision. For initial engine development, propose a small service topology: one application plus PostgreSQL, with Mailpit when email flows are needed. A separate worker remains optional until there is a background-work requirement. The Kubernetes environment, hosting, public TLS ingress, secrets delivery, database placement, backups/restoration, and rollout behavior remain decisions.

Flow pins the web tier to one replica because refresh-replay records, login lockout, MFA challenges, and reset throttles are held in-process. Its deployment uses `Recreate`, accepting upgrade downtime. Increasing replicas without changing that state model would invalidate an explicit donor constraint. Use shared state before scaling rather than merely changing the replica count.

Account onboarding, session storage, learner ownership rules, and desired scale must be reviewed for Astrid rather than copied from an ADMIN-managed internal tool. Flow's browser stores access and refresh tokens in localStorage; reuse the careful refresh/session race handling but make the browser credential-storage decision explicit for Astrid. A future native shell can require different storage. Learner self-service registration or guest use is not supplied by the donor account-creation flow. Evidence: [authentication and deployment constraints](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.claude/docs/security.md), [session storage](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/web/src/api/session.ts).

The shipped Kubernetes front door is also explicitly unresolved: a bare LoadBalancer is provided while production settings assume a trusted TLS-terminating proxy. Do not copy it as a ready public entry point. The single database Deployment/PVC has no backup-and-restore automation in the reviewed configuration. Reusing an existing operated database service, or choosing managed PostgreSQL, can avoid rebuilding that operational capability. Evidence: [front-door gap](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.claude/docs/security.md#L608-L616), [database deployment](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/k8s/postgres-deployment.yaml).

Re-measure runtime sizing. Flow deliberately tunes the JVM for a small I/O-heavy application, including SerialGC, a restricted processor count, and C1-only JIT. CPU-heavy sentence generation and answer analysis may have different needs. Container hardening is reusable; specific memory/GC flags are workload assumptions.

Evidence: [PR workflow](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.github/workflows/ci.yml), [nightly E2E](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/.github/workflows/e2e.yml), [browser configuration](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/e2e/playwright.config.ts), [single-replica deployment](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/k8s/web-deployment.yaml), [JVM tuning](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/server/build.gradle.kts#L44-L107).

## Original proposed reuse process

1. Agree the remaining stack boundaries, account model, and hosting expectations. Retain the confirmed Kubernetes and Docker Compose platforms and online/web-reuse mobile preference; choose a specific mobile distribution approach later.
2. Prepare a donor manifest listing each foundation component to keep, adapt, or omit, with its Flow revision, rationale, and required tests.
3. When implementation is authorized, port the smallest complete application foundation and prove its gates before adding the learning engine. Exclude Flow integrations, reporting models, migrations, sample data, credentials, brand, and unnecessary dependencies.
4. Keep provenance for copied components and preserve applicable notices from Flow's [MIT licence](https://github.com/liveweird/flow/blob/4e255e966804c5750c8fe0f1928a1461c17cc96f/LICENSE). Coordinate shared fixes deliberately; extract a common library only when real repeated maintenance justifies it.
5. Validate the new engine with the connected language subset and independent language review already proposed in [Language foundations](language-foundations.md).

The subsequent authorized pilot completed the concrete [reuse manifest](flow-reuse-manifest.md) and [delivery](playground-delivery.md); the steps above preserve the original assessment plan. Unresolved adoption decisions are tracked in [Open questions](open-questions.md); work planning remains in the [backlog](../../BACKLOG.md). This assessment authorizes no code port, dependency installation, deployment, or change to Flow.
