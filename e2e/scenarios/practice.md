# Guided practice sessions (`/practice`)

- **Spec**: [tests/practice.spec.ts](../tests/practice.spec.ts)
- **Actors**: an anonymous learner starting **Mixed practice** (`reading-basics`, eight parts); some
  scenarios add a second tab or clear the cookie to expire the development session
- **Owns** (exclusive server-side state): development-session-scoped practice sessions, exercises
  and attempts per test; device `localStorage` (`astrid.practice.v1.*` run records and outbox)

"Wrong answer" below means a deterministic wrong choice or text for the current exercise
(`wrongAnswer` in `support/practice.ts`); feedback for it reads "Target to revisit". Practice is not
a pass gate and the page says so.

## Scenario: practice keeps saved feedback on refresh and completes without a pass gate

1. The learner starts a run, answers the first part wrongly and checks.
   - *Expected*: the feedback status holds focus and shows "Target to revisit".
2. They reload.
   - *Expected*: the feedback is restored with the chosen answer still pressed; exactly one attempt
     was written to the server.
3. For all eight parts they answer wrongly (the first is already done) and press **Continue**.
   - *Expected*: each **Continue** shows "Part N of 8"; after the eighth, the "Practice complete"
     heading holds focus and the text says it is not a mastery judgment; eight attempts written.
4. They reload.
   - *Expected*: the completed summary shows again with no new writes.

## Scenario: practice replays a committed lost attempt after refresh with the exact saved identity

1. The first attempt request is committed by the server and then aborted. The learner starts a run,
   answers wrongly and checks.
   - *Expected*: an alert shows; **Continue** is absent; the answer choices, **Session introduction**
     and **Learn this first** are disabled.
2. They reload.
   - *Expected*: the saved attempt is replayed and the feedback status shows; two requests were sent
     with identical bodies; the server holds one attempt on the first item.

## Scenario: practice unlocks after an uncertain answer is definitively stale

1. The first attempt request is aborted before reaching the server. The learner answers wrongly.
   - *Expected*: an alert says "Your answer is not confirmed".
2. Meanwhile the answer is saved through the API and the item is advanced (another client wins).
   They press **Retry saved operation**.
   - *Expected*: the page moves to "Part 2 of 8"; the focused alert says the answer was not
     recorded and no longer says it is unconfirmed; **Start new practice** and **Session
     introduction** are enabled; the server holds exactly one attempt on item 0.

## Scenario: practice offers explicit recovery when authentication expires before a saved retry

1. The first attempt request is aborted. The learner answers wrongly; an alert shows.
   - *Expected*: an uncertain-result alert.
2. Their cookies are cleared and they press **Retry saved operation**.
   - *Expected*: a focused alert says the session expired or is unavailable; **Start new practice**
     is enabled.
3. They press it and start a new run.
   - *Expected*: a new practice session (different `?session=`) opens at **Check answer**.

## Scenario: practice restores acknowledged support and an uncertain Continue without skipping

1. The first support request is committed and then aborted. The learner presses **Learn this first**.
   - *Expected*: an alert shows.
2. They reload.
   - *Expected*: the "Learn this first" region is visible — the committed support was restored.
3. They answer wrongly and check, then press **Continue** while the first advance request is
   committed and aborted.
   - *Expected*: an alert and the feedback show; **Try again** is disabled.
4. They reload.
   - *Expected*: "Part 2 of 8" shows; the server cursor is 1 and item 0's first-attempt context
     records the lesson as opened.

## Scenario: practice cannot start a competing run while an answer response is in flight

1. The attempt response is held after the server has committed it. The learner answers wrongly and
   checks.
   - *Expected*: **Start new practice** is disabled and the URL still has the original session id.
2. The response is released.
   - *Expected*: the feedback status shows; the server holds one attempt on the first item.

## Scenario: practice does not send a mutation when browser recovery storage fails

1. Writes to `astrid.practice.*` `localStorage` keys throw `QuotaExceededError`. The learner opens
   `/practice` and starts Mixed practice.
   - *Expected*: the page says the "operation was not sent"; no `POST /api/v1/practice-sessions` was
     issued. (The recovery record must be saved before any mutation may leave the browser.)

## Scenario: practice recovers the exact creation after a ${committed ? "committed" : "uncommitted"} lost response

Runs with the first creation request aborted before the server saw it (`uncommitted`) and after the
server created the session (`committed`).

1. The learner starts Mixed practice.
   - *Expected*: **Start new practice** is visible (the creation outcome is unknown) and the URL
     already has a session id.
2. They reload.
   - *Expected*: the same session id opens at **Check answer**; the creation was sent twice with
     identical bodies; the server state has cursor 0 and no attempts.

## Scenario: practice does not replay a pending creation under a renewed cookie

1. Every creation request is aborted. The learner starts Mixed practice.
   - *Expected*: **Start new practice** is visible after exactly one creation request.
2. Cookies are cleared, a new development session is opened through the API, and the learner reloads.
   - *Expected*: an alert says the session expired, differs or is unavailable; still one creation
     request in total; the page has sent exactly one development-session request (the initial one
     before the run started), so the reload does not renew the cookie itself; the pending session id
     answers `404` to the new owner.

## Scenario: practice explains a stale answer from another tab and restores the current item

1. The learner opens the same run in a second tab. In the first tab they answer wrongly, check and
   press **Continue**.
   - *Expected*: the first tab shows "Part 2 of 8".
2. In the second tab they answer the old exercise and check.
   - *Expected*: an alert mentions another tab and the tab shows "Part 2 of 8"; the server holds
     only the first tab's one attempt on item 0.

## Scenario: practice advances with native keyboard focus in a short enlarged-text window

1. At 768×360 with text at 200% the learner starts a run and uses only the keyboard (Tab, or Alt+Tab
   in WebKit on macOS; at most 60 presses per control).
   - *Expected*: they can reach and activate the wrong answer, **Check answer** and **Continue** in
     turn; after checking the feedback status holds focus.
2. After **Continue**.
   - *Expected*: "Part 2 of 8" shows and the new part's `h2` holds focus below the fixed header and
     inside the 360px viewport; no horizontal overflow.

## Scenario: practice support, feedback and summary reflow accessibly in ${theme} mode

Runs in light and dark (emulated colour scheme).

1. The learner starts a run, then the window shrinks to 390×844 with text at 200%.
   - *Expected*: zero axe violations.
2. They open and close **Session introduction**.
   - *Expected*: the "Session introduction" region is visible once opened.
3. They answer wrongly and check.
   - *Expected*: **Continue** is enabled and fully opaque; feedback badges are not clipped; the page
     scans clean without horizontal overflow.
4. The run is completed through the API and the learner reloads with enlarged text.
   - *Expected*: "Practice complete" shows, scans clean and has no horizontal overflow.
