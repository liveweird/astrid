# Practice Continuity and Support Follow-up

## Scope and confirmed findings

**7 November 2026 — BL-037:** Bounded continuation after the [guided practice delivery](practice-delivery.md), while the user was away. Existing content, formats, plan/grading revisions, persistence schema and public APIs remain unchanged. This work improves support readability and verifies real continuity; it adds no broader course feature.

At 768×360 with 200% text, both browsers focused the exercise after support acknowledgement. The requested introduction ended 393 pixels above the viewport; the item lesson ended 220 pixels above it. Restoring focus to the disclosure control helped narrow layouts, but the short window still placed the first teaching paragraph below the fold.

The final fix focuses the first revealed teaching paragraph, or the hint alert, after durable acknowledgement. Controlled disclosures derive expansion in the same render, with no expansion animation separating layout from focus. Header-aware scroll margins keep the focused text visible. Controls remain accessible and collapsible. Manual retry selects the same support target from its immutable request, while cursor changes/completion retain item/summary focus priority. Request bytes, support evidence and grading are unchanged.

## Targeted browser evidence

Twenty Chromium/WebKit scenarios pass with retries disabled:

- Introduction, lesson and hint at 390×844 and 768×360 with 200% text.
- Held responses prove persistence occurs before teaching text appears.
- Lost committed responses followed by manual retry retain exact request bytes, focus and readable content.
- Two live tabs create separate durable answers in one run, with reverse-lexical UUIDs. Tab B correctly attempts the older shared A entry first while its own B intent remains queued.
- Retry drains A then B before enabling Continue. The server stores exactly two attempts in enqueue order, later cached replay adds no third grade, and duplicate old-position Continue cannot skip an item.

A capitalization-sensitive stale-tab assertion was corrected to recognize both valid intermediate/final message forms without depending on readback timing.

## Actual process-restart evidence

The private protocol verified the exact candidate image and owned project/service/container/volume labels before mutation. It never restarted the main preview or its database. Both Chromium and WebKit passed these phases:

1. Intercept the attempt POST after the controller's initial state read; acquire only the test owner's row lock, forward the POST and verify its transaction waits on that lock. Restart the owned QA app, release the lock and wait for readiness.
2. Reload the browser document and hold automatic recovery. The exact serialized localStorage entry survives unchanged; releasing it sends the same attempt ID/body. The server stores one attempt and one immutable context at retry one, preserving the checked answer, support, plan snapshot and cursor.
3. Continue, capture raw state response bytes, then restart only the owned QA PostgreSQL container with its existing volume. The QA app process remains unchanged. Reload with the same URL/cookie, without renewing authentication: response bytes, seed, item identities, answers, contexts, counters and immutable plan hash match exactly.
4. Saved attempt replay remains exact, duplicate prior-position advance remains idempotent, and a fresh operation on the current item succeeds.

Gate waits are bounded. Final process/mount comparisons confirm the main app and database were untouched. Detailed private protocol/result artifacts remain under ignored `.local/practice-hardening/`.

## Verification and delivery status

The frontend passes 99 tests, lint, build, API drift and unchanged coverage floors. Independent review covered support targeting, immutable replay, real two-tab assumptions, restart ownership guards and proof accuracy. The [variation inventory](practice-variation-audit.md) separately measures meaningful and presentation-only variation, without changing generation.

All 114 local Chromium/WebKit journeys pass in 2.1 minutes with retries disabled. Follow-up CI/promotion remain in progress. Completion evidence will replace this status before BL-037 is closed. The original master delivery passed all five [master CI jobs](https://github.com/liveweird/astrid/actions/runs/42374564854).
