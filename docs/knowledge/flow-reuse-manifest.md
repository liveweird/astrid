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
| Flow authentication, ingestion, telemetry, admin tools and domain model | Omit | Unrelated to validating Danish learning |
| JVM tuning and class-data archive | Omit for this milestone | Measure Astrid before adding optimization |
| Lesson generation and assessment | New pure Kotlin module | Astrid-specific typed language model and deterministic rules |

## Reproducibility and conventions

Use Java 21, Node 24 and the exact versions in `mise.toml`, `.nvmrc`, the Gradle catalogue and npm lockfile. Commit Astrid's dependency locks; do not copy Flow's resolved project graph. The narrow `npm ci --legacy-peer-deps` exception matches Flow's TypeScript 6 / openapi-typescript peer declaration and does not disable audit or type checking.

Keep the 48px header, sidebar/rail, responsive navigation, light/dark theme and Inter typography. Astrid uses a Danish red accent with separately labeled learning feedback. The initial Denmark-related vector mark is provisional; final artwork remains BL-015/BL-021.

## Validation boundary

Required checks cover engine invariants and worked cases, real PostgreSQL migrations and request isolation, API contract, frontend interaction and retry behavior, packaged application startup, deployment configuration, and independent review. A successful software test does not make draft Danish material teacher-approved or demonstrate learning effectiveness.
