# Pilot Hardening Plan

## Authorization and limits

On 7 November 2026 the user approved continued autonomous work through at least 08:00 Europe/Warsaw. Finish the baseline merge/push, then improve reliability and reviewability within the existing pilot. Preserve the 23 entries, three formats and published generation semantics; broader vocabulary, new grammar, media, public accounts/hosting and mobile packaging remain deferred.

## Working implementation decisions

Keep typed Kotlin definitions as the pilot's authoritative content store. A runtime JSON loader, mutable content database or editor would introduce synchronization and parsing complexity without a demonstrated authoring need. Extract only the typed snapshot and validation seams needed for negative fixtures and review export.

Before refactoring, freeze complete representative `pilot-1` / `generator-1` outputs. Preserve candidate enumeration order, prompts, choices, block identities, introductions, support skills and grading. Generated golden fixtures establish compatibility; independently specified worked examples remain the linguistic oracle. Neither substitutes for Danish teaching review.

Generation and evaluation remain free of I/O and HTTP dependencies. Any local review export writes through a build/review adapter, includes draft status and existing source references, and exposes no new public endpoint.

## Work packages

| Track | Scope | Acceptance |
| --- | --- | --- |
| Content integrity and review | `engine/` | Compatibility fixtures remain unchanged; supplied malformed snapshots identify bad IDs, references, cycles, forms, roles and licensing; deterministic local review export includes revisions and draft provenance |
| Historical replay and failures | `server/` | Saved attempts replay before current-revision checks; failed/cancelled evaluation persists no attempt or retry gap; two pools prove transactional idempotency and retry numbering; all formats and malformed payloads have contract evidence |
| Accessibility and recovery | `web/`, `e2e/` | Axe checks and complete keyboard journeys; measured 320px/enlarged-text layout; useful focus after disappearing controls; explicit recovery preserves uncertain attempt identity |
| Delivery evidence | Root tooling, CI and `k8s/` | Match PostgreSQL pins, audit both npm graphs, validate rendered manifests, and assess an isolated local Kubernetes smoke test after verifying the context |

The coordinator owns shared build/lock integration, documentation, backlog and Git delivery. Parallel writers use disjoint paths. Require independent review and relevant checks for substantive changes. Do not add abstractions or tests that merely restate implementation; respond to demonstrated gaps and preserve existing user changes.
