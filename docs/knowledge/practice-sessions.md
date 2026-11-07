# Short Practice Session Contract

## Scope and working defaults

**7 November 2026 — User-authorized:** “go with that” approves BL-009: optional introduction, varied current exercises, feedback/retries, per-target summary and reliable resume within a valid development session. The following size/focus/transaction choices are implementation defaults, not separately user-selected curriculum policy. Keep standalone playground `/` unchanged; add Practice at `/practice`. Retain sibling layout, Inter typography, red theme and existing prompt/feedback components. New grammar/vocabulary, adaptive review, accounts, media and mastery claims remain outside scope.

One focus, `reading-basics`, titled **Words and simple statements**; plan revision `practice-1`. Pin its exercises to `pilot-3/generator-3/evaluation-3`, independently of any future catalog default. No published generator, grader, fingerprint, AttemptRequest or AttemptResponse changes.

A/B/C are E007's three selected senses at seed/index 0/1/2. Persist this eight-item plan:

| Position | Format and target |
| --- | --- |
| 0 | E007 meaning recognition A |
| 1 | E003 G005 ordering, a `læse`/L011 sentence containing C |
| 2 | E007 meaning recognition B |
| 3 | E008 headword recall A |
| 4 | E007 meaning recognition C |
| 5 | E008 headword recall B |
| 6 | E006 G006 article repair, a `læse`/L011 sentence containing A |
| 7 | E008 headword recall C |

Scan existing grammar indices 0..<99 deterministically for the required licensed noun/verb; retain exact coordinates. The noun sequence A/C/B/A/C/B/A/C avoids adjacent repetition; recognition precedes corresponding recall. All three senses are assessed both ways plus G005/G006; G001–G004 are supplied prerequisites/support, not assessed outcomes. Reuse/deduplicate exact existing introduction title/body pairs; no new teaching rules.

Choose a fresh cryptographic nonnegative 31-bit seed once when creating a browser run; persist it in the immutable creation request. Recovery reuses that seed, while a new run can vary noun order and licensed sentences. Cursor 0..7 selects the current item; 8 means completed. Continue requires at least one saved attempt of any outcome. No skip, pass gate, auto-advance or score-to-mastery conversion. Retries are optional; completion records attempted-and-continued items, not successful learning.

## Pure engine models

Add new practice files; leave published exercise generation/evaluation untouched. Publish these signatures early for server integration:

- `PracticePlanner(engine: PilotEngine = PilotEngine()).plan(focusId, planRevision, seed): PracticePlan`.
- `PracticePlan`: focusId, planRevision, seed, title, prerequisiteSkillIds, introduction, items; pinned exercise revisions.
- `PracticePlanItem`: position, exercise (`GeneratedExercise`), answerExposureKeys, promptExposureKeys, lessonExposureKeys, hintExposureKeys, feedbackExposureKeys. The server never directly serializes this engine object.
- `PracticeAttemptContext`: introductionOpened, lessonOpened, hintOpened, priorExposure (list of position/source/keys).
- A pure summary calculator consumes typed item/attempt observations and never grades answers. Engine/server owners coordinate exact observation signatures before shared integration.

Knowledge keys: `gloss:L014-S01`, `headword:L014-S01`, `article:L014`, `grammar:G005`, etc. E007 answer keys identify its gloss; E008 its headword; E003 G005; E006 the article for its noun. Every E007 prompt exposes all three gloss choices plus its own headword. E008 exposes the English cue. Grammar blocks/context expose noun, article and supplied English meaning. Lessons and feedback have conservative possible-exposure keys for content they can reveal. Generic lexical/article hints record support without answer keys; the ordering hint exposes its grammar rule. Danish sentence feedback exposes its headword/article, not the English gloss supplied by the earlier prompt. Headword feedback can reveal the target noun’s stored article. Displayed glosses do not prove knowledge of their headword association.

At each attempt, intersect current answer keys with earlier released prompts/support/feedback. Exclude the current prompt itself; include prior feedback on the same item for retries. Deduplicate by position/source/key, not by retry. Record support before revealing it. Label evidence **possible earlier exposure**, not learned/independent/mastered. Conservative marking after a lost response is intentional. Freeze each attempt's context; later support cannot rewrite earlier evidence.

## API and owner binding

All routes retain cookie authorization, Host/Origin and JSON/client-header guards. New Practice POST routes additionally require `X-Astrid-Practice-Owner` equal to the authenticated development-session UUID. This is a nonsecret continuity binding, not bearer authorization. Keep the existing development-session DTO unchanged.

| Route | Request / result |
| --- | --- |
| GET `/api/v1/practice-context` | Authenticated profile metadata: ownerSessionId, sessionExpiresAt, focusId, title, planRevision, itemCount. Does not renew/create a cookie. |
| POST `/api/v1/practice-sessions` | `{practiceSessionId, focusId, planRevision, seed}`; 201 created / 200 same identity+request resumed / 409 changed request |
| GET `/api/v1/practice-sessions/{id}` | Current owned state; wrong/missing owner record is 404 |
| POST `/api/v1/practice-sessions/{id}/support` | `{kind:"introduction"}` or `{kind:"lesson"|"hint", position}`; monotonic support then state |
| POST `/api/v1/practice-sessions/{id}/advance` | `{position}`; monotonic advancement then state |
| Existing POST `/api/v1/attempts` | Unchanged body and response, including exact request identity and replay |

PracticeSession fields: id, ownerSessionId, focusId, title, planRevision, seed, contentRevision, generatorRevision, evaluationRevision, sessionExpiresAt, state (`active`/`completed`), cursor, itemCount, prerequisiteSkillIds, introduction, introductionOpened, items, currentExercise (nullable), summary.

PracticeItem fields: position, exerciseId, formatId, targetSkillId/lexicalTarget, lessonOpened, hintOpened, attemptIds (retry order), attemptCount, firstAttempt/lastAttempt (nullable), firstAnswer/lastAnswer (nullable, exact saved text or block IDs), firstAttemptContext/lastAttemptContext (nullable). Only currentExercise returns the presentation; no future prompts in descriptors. Existing exercise retrieval remains compatible.

PracticeAttemptContext priorExposure entries have `{position, source:"prompt"|"lesson"|"hint"|"feedback", keys:string[]}`. Introduction and cross-item context do not overwrite Attempt.hintUsed. The Practice view must distinguish item support from session introduction/earlier exposure; an optional FeedbackPanel presentation label may clarify this while retaining standalone labels unchanged.

Summary fields: plannedItems, attemptedItems, completedItems, attemptCount, retryCount, introductionOpened, rows. Rows remain distinct for each G target or `(entryId,senseId,facet,direction)` and include a human-readable label, target metadata, plannedItems, attemptedItems, advancedItems, attemptCount, retryCount, first/latest outcome counts, firstWithSupport, firstWithPriorExposure, lessonOpenedItems, hintOpenedItems. Outcome counts: fullyMet, targetMet, targetNotMet, unevaluable, languageUnknown, languageKnownIssue, responseConstraintNotMet. Fully met requires target met + acceptable language + absent/met response constraint. Keep first outcomes after retries; active responses return an empty rows list, and only completed sessions release summary target labels.

## V3 and transaction invariants

Add only V3 practice tables; do not edit V1/V2 or old payloads. `practice_sessions` stores composite owner/run identity, canonical creation request, immutable mapped plan payload, cursor 0..8, introduction flag and timestamps. `practice_items` links owner/run/position to existing exercise IDs, sticky lesson/hint flags; unique owner/exercise membership. `practice_attempt_context` links existing owner/attempt and practice item to immutable context JSON. Use composite foreign keys for owner isolation. Persist each target label and use the saved exposure/target snapshot for reads and new attempt context; never reconstruct a resumed plan from current planner code. Order saved retries by their persisted retry number, including when timestamps tie.

Every write locks the existing development-session row first. State reads use one consistent transaction (the same lock is sufficient).

- Create: validate and check identity first, then atomically insert run, eight existing StoredExercise rows and item links. Same request returns current state, no new exercises; changed request conflicts.
- Attempt: existing saved-ID/payload replay remains FIRST. For a fresh practice-linked exercise, require its position == cursor; capture context, call the existing evaluator once, and insert attempt/context atomically. Failures/cancellation roll back both. Standalone path remains unchanged. Fresh post-advance IDs conflict; old IDs replay exactly.
- Advance: position < cursor returns current state without increment; position > cursor conflicts; equal requires a saved attempt then increments once. Reaching eight completes once. Distinct concurrent answer IDs are retries; duplicate advances cannot skip an item.
- Support: false→true only. Item positions <= cursor are released (0..7); future support conflicts. Earlier support can affect later observations, never old snapshots. Introduction has no position; its value is frozen in each attempt context.

V3 is additive. Back up the main database before migration; preserve old exercise/attempt hashes and database process/mounts. Rollback retains new tables/evidence and suspends Practice. Verify Flyway's handling of a newer applied migration before assuming an older image can start; do not delete schema history or practice evidence. The isolated older-image check starts successfully on V3 with all five histories preserved. This is not permission to leave old Practice clients writing against pre-Practice code: quiesce Practice clients/writes before downgrade, retain V3 evidence, and use a compatible build to resume them.

## Browser recovery and presentation

Use distinct durable localStorage keys for immutable outbox entries. Each stores ownerSessionId, practice ID, operation, exact serialized body and its operation identity (attempt UUID; advance run+position; support kind+position). Persist before sending; if persistence fails, do not send. Preserve each entry’s original enqueue timestamp and serialized body on exact retries; refuse a changed payload under the same operation key. Replay same-kind entries by enqueue chronology, with an identity tie-break for indistinguishable timestamps. Remove only on confirmed success or a definitive rejection explained visibly. Store per-run creation metadata, plus a latest active pointer. Bind an open tab to its run in `/practice?session=<id>` so another tab starting a different run cannot silently redirect it on refresh.

With recovery data, GET practice-context BEFORE any cookie-renewing call. A 401 or different owner requires explicit expired/unavailable recovery, never replay under the new owner. Same-owner missing run plus pending creation may replay the exact creation; the required owner header closes the cookie-rotation race. With no recovery data, ordinary development-session bootstrap is safe. Explicit **Start new practice** abandons the old recovery record by user action, renews authentication as needed, and creates a new UUID; server history remains untouched.

Reconcile pending support, then attempts, then advances, and resume draining the remaining queue after a manual retry succeeds. Do not enable ordinary progress while another queued operation remains. Refresh never assigns an uncertain answer a fresh attempt ID. Confirmed grades require only state readback. Keep answer editing, support, retry and Continue disabled while its attempt is uncertain. Preserve checked answers during an uncertain advance and offer exact operation retry. A matching definitive rejection clears uncertainty and explains unavailable/stale recovery. Allow explicit Start new abandonment when no request is actively running; guard late responses against replacing a newer run. On stale-item 409, refresh and explain that another tab continued; the rejected new answer was not recorded. Raw unsent drafts need not survive refresh; checked/pending answers and progress must survive. Cross-device recovery is not included.

Record support before revealing its body. Keep ordinary prompt context free of optional answer disclosures. Focus the item heading on advance, feedback after grading, recovery alert on failure, and summary heading at completion. Use existing typography/palette, narrow/200% reflow and keyboard patterns. Completed target cards keep first/latest outcomes and support visible without a horizontal results table; retain nonzero diagnosis/constraint counts and explicit fully-met counts. No broad redesign or new runtime dependencies.

## Acceptance and ownership

Engine owns `engine/`: deterministic planner, exposure metadata/context helper, pure summary and tests; publish signatures first. Server owns `server/`: mapped snapshots/DTO/OpenAPI, V3, repository/in-memory parity and transaction/ownership tests. Web owns `web/`: Practice controller/outbox/UI, navigation, generated schema and behavioral tests. Coordinator owns docs, `e2e/`, shared locks/checks and Git. Only the coordinator runs shared Gradle/Docker checks; use early requested lint/compile checkpoints.

Verify deterministic eight-item coverage, all-choice/context exposure, first/retry/late-support snapshots, partial/unknown outcomes, all-wrong completion, atomic create/replay, concurrent attempts/advances, evaluator-failure rollback, expiry/owner-rotation recovery, localStorage failures, refresh after a committed lost response, support replay, keyboard/summary focus, both themes, enlarged/short viewports and exact old standalone behavior. All 335/952/978 archives stay immutable. Independent review and complete repository/browser gates precede delivery.
