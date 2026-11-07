# Targeted Follow-up Practice Contract

## Scope and implementation defaults

**7 November 2026 — User-authorized:** “OK, proceed” selects BL-039. From a completed practice summary, the learner selects existing grammar or vocabulary targets and starts a separate review. These defaults implement that scope; they are not a separately selected curriculum or scheduling policy. No targets are preselected and no automatic recommendations are introduced. Keep existing outcome, support, unknown-language and response-constraint evidence visible.

Recognition and written headword recall remain separate targets. Canonical identities are `grammar:G005`, `grammar:G006`, or `lexical:<entryId>:<senseId>:<facet>:<direction>`. Only targets actually assessed in the owned, completed source run are eligible. Empty, duplicate, unknown and unassessed selections are rejected. A child has a fresh UUID/seed; the source history remains unchanged.

## Plan and finite variation

Use `follow-up-1`, focus `targeted-review`, pinned to `pilot-3/generator-3/evaluation-3`. Reuse existing content, introductions, graders and formats: E003 for subject-first order; E002/E006 for indefinite article supply/repair; E007 for meaning recognition; E008 for headword recall. Never introduce an unselected target or reverse lexical direction silently.

Each selected lexical target appears once: its current pool contains one meaningful cue, even when choice order can shuffle. Vocabulary-only runs therefore contain one to six items. If grammar is selected, fill to `max(4, selected target count)`, at most eight items, using distinct licensed reading sentences for each grammar target. Prefer sentences absent from that target in the immediate source; presentation changes alone do not count as new sentences. Avoid adjacent equal targets where the selected inventory permits it. Same-word recurrence across separate review runs remains explicit.

G001–G004 remain supplied prerequisites rather than newly assessed targets. Reuse optional teaching support; completion still requires an attempted answer and Continue, with no pass gate. Review results do not establish independent knowledge, retention or mastery.

## Creation, lineage and exposure

Add `POST /api/v1/practice-sessions/{sourceId}/follow-ups` with `{practiceSessionId, planRevision, seed, targetKeys}`. Retain existing authentication, mutation guards and owner-continuity header. Under the owner-row lock, check the child creation identity before loading or planning its source. Exact requests return the saved child; changed requests conflict. Require an owned completed source for fresh creation and insert its plan/exercises/lineage atomically. Source and child IDs must differ.

Persist lineage, selected keys and conservative source-exposure witnesses in the existing immutable plan payload. Existing V3 bounds already support one to eight items, so no schema migration is planned. New optional fields must omit defaults to preserve historical wire encoding. Never rewrite applied migrations, old plan payloads, attempt contexts or results. An older binary may reject new payload/context fields despite the unchanged schema: quiesce follow-up access before any downgrade, retain all evidence, and verify original-practice and standalone compatibility separately.

For new child creation only, regenerate pinned source exercises from their stored coordinates and verify their saved fingerprints before using canonical sentence identities for exclusion. Resume, state reads and exact creation replay use saved snapshots without regeneration.

Freeze possible exposure from source prompts, recorded teaching/support and feedback at child creation. Retain inherited witnesses with their original run identity; use one conservative witness per knowledge key to bound repeated chains. Include any opened source introduction. At each child attempt, intersect those witnesses with that item's answer keys and save them separately from within-run prior exposure. Later source support cannot alter the child snapshot or earlier attempt evidence. Summary exposure counts include either source or within-run exposure. Seeing a gloss alone does not prove knowledge of its Danish association.

## Browser continuity and acceptance

Persist the exact follow-up endpoint identity and serialized creation request before sending, under the same owner and URL-bound child UUID. A failed storage write sends no mutation. An uncertain response replays the same request; refresh must not create another child or renew an expired owner before checking continuity. Keep the source URL recoverable. Reuse existing item support, attempt, advance and retry behavior, with dynamic item counts and explicit review labeling.

Acceptance covers deterministic selected-target coverage and semantic variation, finite lexical size, source exclusion, completed-source eligibility, ownership, creation conflicts/concurrency/rollback, frozen and nested exposure, first/latest outcome distinctions, source history preservation, refresh and committed lost-response recovery. Verify keyboard selection, focus, both themes, narrow/enlarged reflow, all historical archives and existing practice/standalone journeys. Independent review and repository/packaged-browser gates precede backed-up preview promotion and Git delivery.

## Remaining boundaries

Automatic suggestions, broader scheduling (BL-011), new grammar/vocabulary, specialist teaching approval, accounts, cross-device recovery, audio, media and mobile packaging remain separate work. User review of the existing flow remains BL-035. Implementation evidence and any changes to these defaults must be recorded before completion.

## Observed delivery — 7 November 2026

Source `8eac007` completed independent engine/API/web review and all five [source CI jobs](https://github.com/liveweird/astrid/actions/runs/42388919244). All 130 Linux browser journeys passed without retries or flaky results. Local checks passed 85 engine tests plus the same fresh Arabic-locale suite, 54 server tests including real PostgreSQL, 108 frontend tests, unchanged coverage floors, strict dependency verification, lint, API drift, retained review exports and deployment checks. All 130 native Chromium/WebKit journeys passed without retries; the exact packaged image passed the repository vulnerability policy.

Review corrected an avoidable adjacent-target ordering defect and a partial-browser-storage failure that could move the latest-run pointer before saving its creation request. Count-aware seeded ordering and atomic recovery persistence now have regressions; the latter also covers original and explicit-new practice creation. Integration tests caught and corrected missing cross-run exposure in the in-memory repository. A clean packaged build also verified the corrected typed recall fixture. Independent static and rendered review closed all findings.

An isolated upgrade preserved exact original-practice/context response bytes, every pre-existing row and all three published evaluators' read/replay/fresh-submit behavior. Owned app and PostgreSQL restarts retained exact source/child state, source exposure and attempt replay while the main services stayed unchanged. Browser acceptance includes single lexical, four-item grammar and full eight-target runs, both directions, frozen source evidence, source return, committed/uncommitted creation loss, manual retry, owner rotation, keyboard focus and both themes at enlarged text.

The verified image `sha256:c38eec06f937f7f9981601806b4d9f26ed90462bb73218342a9c1f391420afeb` now serves the private main preview. Before promotion, a 178,172-byte mode-0600 PostgreSQL backup was captured and subsequently verified readable. Promotion preserved immutable evidence for 1,318 exercises, 307 attempts, 44 sessions, 352 items and 126 attempt contexts, the same PostgreSQL process/mounts, schema versions 1/2/3 and all three evaluators. All 22 main Chromium/WebKit smoke journeys passed without retries. Ownership-guarded cleanup removed only the temporary QA containers, volume and network; the main processes/mounts remained unchanged.

Private build hashes, logs, screenshots, backup and verification manifests remain ignored under `.local/targeted-follow-up/`. This completion record accompanies the authorized focused Git delivery. BL-039 is complete; BL-035 remains user review of pacing, clarity, selection and repetition.
