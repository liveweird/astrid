# Guided Practice Delivery

## Scope and behavior

**7 November 2026 — BL-009:** The authorized `practice-1` flow adds `/practice` beside the unchanged standalone playground. Eight items mix meaning recognition, subject-first ordering, headword recall and article repair using existing beginner content. A fresh persisted seed varies new runs; resume keeps the original plan.

Optional introduction, lessons and hints are recorded before release. Continue accepts any saved outcome; retries are optional. Completed target cards preserve first/latest results, response constraints, support and possible earlier exposure. Completion is not mastery. Grammar/vocabulary expansion, accounts, adaptive scheduling, audio and public hosting remain outside scope. See the [contract](practice-sessions.md).

## Verified local evidence

| Gate | Observed result |
| --- | --- |
| Engine | 75 tests in the normal JVM and 75 in a fresh Arabic-locale JVM; required detekt and coverage pass |
| Server | 50 tests, including real PostgreSQL, two-pool concurrency, exact replay, frozen context and transaction rollback; detekt/coverage pass |
| Frontend | 95 tests; lint, build and generated API drift check pass |
| Frontend coverage | 94.55% statements, 87.53% branches, 95.49% functions, 96.64% lines; existing 92/87/93/94 floors unchanged |
| Packaged macOS browsers | All 94 Chromium/WebKit journeys pass with retries disabled, including the 58 existing journeys |
| Compatibility | All 335 and 952 retained engine cases and 978 published fingerprints remain unchanged; all three evaluator pairs submit and replay exactly |
| Other gates | Knowledge/toolchain/startup guards, 11 script tests, strict dependency verification, Compose/Kubernetes rendering, dependency audit policy and pinned image scan pass |

All five jobs in the [source CI run](https://github.com/liveweird/astrid/actions/runs/42373281826) passed at `d8262cc`. The packaged Linux job passed all 94 Chromium/WebKit journeys in 6.2 minutes without retries or flaky results. The subsequent [master CI run](https://github.com/liveweird/astrid/actions/runs/42374564854) also passed all five jobs. No implementation changed within BL-009 after that verified source commit; its documentation-only completion record accompanied the authorized master delivery. Later changes and current promotion evidence are recorded in the [continuity follow-up](practice-continuity.md).

## Review and recovery corrections

Independent engine, server and web reviews found and resolved missing feedback exposure, invented generic-hint exposure, current-code replanning of saved sessions, timestamp-tied retry ordering, and several pending-operation races. Reads now consume frozen labels/target/exposure metadata. Attempt/context writes are atomic; a forced context-insert failure rolls back the attempt, and retry remains number one.

Browser checks reproduced timer-before-render focus failures, an unbound URL after lost initial creation, and enlarged-text focus behind the fixed header. Post-commit focus, durable pre-send URL binding and Practice-only header-aware scrolling correct those cases. Summary cards replace the horizontally scrolling results table in narrow/enlarged views. Both themes were rendered and inspected.

The durable outbox preserves original bytes, identity and enqueue chronology, drains remaining operations after retry, and keeps progress blocked while unresolved work remains. Definitive rejection is explained; explicit abandonment is available once no request is active. Owner checks precede cookie renewal and protect creation replay from reassociation.

## Migration and rollback evidence

The isolated V2→V3 upgrade preserved every prior exercise/attempt hash and exact reads/replay, with fresh submissions still using their original evaluators. The older vocabulary image also starts on V3, with all five table histories preserved and Practice clients quiesced during that check. Downgrade requires quiescing Practice clients/writes; pre-Practice code does not enforce the new session protocol. Retain V3 tables and schema history, and resume Practice using a compatible build.

At BL-009 promotion, the main preview at [localhost:8088/practice](http://localhost:8088/practice) used image `sha256:d963f2d527eb57198a83c6cba4cb40b10065c42b617051a167ec7a58606383d3`. A 55,846-byte PostgreSQL custom-format backup was retained privately before V3. All 286 existing exercises and 168 attempts kept their evidence hashes. The same database container, process and mounts remained in place; all three evaluator pairs submitted and replayed exactly. Ten main Chromium/WebKit completion, refresh/lost-response, keyboard and theme/reflow checks passed. The continuity follow-up subsequently replaced that image while preserving this history.

Ownership-checked cleanup removed only this milestone’s QA containers, network and volume, and reconfirmed the main database state unchanged. Backups and detailed private evidence remain under ignored `.local/practice-sessions/`. Further reliability work uses a new owned QA run. The application remains private on loopback/ClusterIP, with development cookies and browser-local recovery; no cross-device recovery or teaching approval is claimed.
