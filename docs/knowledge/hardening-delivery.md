# Pilot Hardening Delivery

## Scope and decisions — 7 November 2026

This pass implements the user's overnight continuation within the existing reading/writing pilot. It preserves `pilot-1`, `generator-1`, `evaluation-1`, 23 lexical entries and the three exercise formats. No new learner grammar, vocabulary, accounts, media or public deployment was introduced.

Typed Kotlin remains the authoritative pilot content store. Supplied snapshots now support negative validation without adding a runtime loader. A deterministic local review export records forms, authored meanings, constraints, source references, draft status and all 99 sentences. Its file-writing adapter belongs to the test/build classpath, not the production engine JAR.

## Observed software evidence

| Check | Result |
| --- | --- |
| Strict Gradle verification, detekt, distribution and coverage gates | Passed; 37 engine and 24 server tests, no failures or skips |
| Published generation compatibility | 335 TSV cases passed: complete representative cycles, boundary coordinates and selected grading results |
| Content validator | Malformed identities, references, cycles, forms, roles, noun phrases, lexical combinations and review metadata rejected |
| PostgreSQL transactions | Two independent pools proved same-ID replay, conflicting payload rejection, distinct retry numbering and rollback/lock release after failure or cancellation |
| HTTP boundaries | All formats, malformed bodies, wrong answer kinds/block sets, strict UUIDs, historical replay and OpenAPI responses checked |
| Frontend | 21 tests passed; lint, generated API drift and production build passed |
| Frontend coverage | 88.01% statements, 81.22% branches, 87.01% functions, 90.65% lines; configured floors passed |
| Assembled browser checks | 10 passed against the final Kubernetes image and its real CSP |
| Kubernetes schemas | All 7 rendered resources passed the pinned strict schema check |
| Toolchain/startup budgets | Alignment scripts passed under macOS system Python 3.9 as well as the development Python |
| Final packaged runtime scan | Pinned Trivy fixable high/critical gate passed |

The browser suite includes all formats, full keyboard ordering/retry/advance, light/dark Axe WCAG A/AA checks, 320px width with 200% text, explicit theme override, uncertain-response replay, initial/existing-session recovery and revision-conflict recovery. Automated accessibility checks do not establish complete accessibility conformance.

The compatibility fixture was captured before refactoring from the published baseline engine JAR. The published JAR's SHA-256 was independently compared with the packaged baseline: `dd91b64dc54144f65112770395e484a7727cbc7e28965bb363346ba3087a232c`. Fixture SHA-256: `6f51db2d983a81f4d0b0478d12c8bac5fba15a5649e45b46df537738504bad46`. Generated goldens establish compatibility; independently specified worked cases remain separate linguistic expectations.

## Local Kubernetes cold-start and persistence

Testing used only context `orbstack`, after verifying its loopback API address, and a newly created uniquely labeled namespace `astrid-qa-20261107-000724-2a7d`. Services were ClusterIP; access used a loopback port-forward. The user's existing Compose database was not used for these tests.

The initial image restarted twice when PostgreSQL was not ready. The corrected image adds bounded native Flyway connection retries. Starting the app before the database at the maximum configured 15 retries, observing retry warnings, and then starting PostgreSQL completed with **zero app restarts**. An exercise and attempt created before the upgrade remained retrievable afterward; same-ID replay returned the original byte-identical response and timestamp with retry number 1. The same PVC survived the app and database restarts.

Migration connections apply a 3-second TCP timeout and 5-second login deadline after parsing the URL, retaining other connection options. A real socket that accepted TCP but withheld the PostgreSQL handshake verified the login deadline. Native retry waits cap at 8 seconds. At 15 retries the connection/backoff envelope is 183 seconds; the guard requires another 30 seconds. Kubernetes allows at least 237 seconds before startup-probe failure, and Docker has a 240-second start period. These budgets cover the small pilot; slow future migrations require reassessment.

Relevant upstream contracts: [pgJDBC connection properties and URL precedence](https://jdbc.postgresql.org/documentation/use/), [Flyway connection retries](https://documentation.red-gate.com/fd/environment-connect-retries-setting-277578922.html), and [Flyway environment settings](https://documentation.red-gate.com/fd/environments-namespace-277578909.html). The configured pinned versions were also inspected locally; this timing envelope is a policy bound, not a guarantee for arbitrary DNS, migrations or infrastructure.

The owned QA namespace and its disposable PVC were removed after verification; a subsequent namespace lookup returned no resource. The port-forward was stopped. The existing Compose app and PostgreSQL remained healthy. Git/CI results are recorded once observed.

## Review limits

Independent engineering review confirmed the substantive fixes. The [resource assessment](danish-resource-assessment.md), [candidate COR crosswalk](cor-pilot-crosswalk.md) and [pilot source review](pilot-source-review.md) improve provenance without importing dictionary data. No source audit constitutes Danish teacher approval, a legal opinion or a learning-effectiveness result.
