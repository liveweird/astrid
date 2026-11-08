# Practice: teaching support revealed only after saving

- **Spec**: [tests/practice-support.spec.ts](../tests/practice-support.spec.ts)
- **Actors**: an anonymous learner in a **Mixed practice** run
- **Owns** (exclusive server-side state): one development-session-scoped practice session per
  instance, with one support event recorded on its first item

## Scenario: practice reveals ${kind} after saving and keeps it readable at ${viewport.width}×${viewport.height} with 200% text${retry ? " after saved-operation retry" : ""}

Instances: `kind` is `introduction`, `lesson` or `hint`; `viewport` is 390×844 or 768×360; the
`retry` variant runs only at 768×360. That is nine instances: six plain and three with a lost
response.

1. At the viewport size the learner starts a practice run and enlarges text to 200%.
   - *Expected*: **Check answer** shows; the expected teaching body is read from the API.
2. They press the control (**Session introduction**, **Learn this first** or **Show hint**). The
   support request is committed by the server, then held.
   - *Expected*: the server already marks the support (`introductionOpened`, `lessonOpened` or
     `hintOpened`) as opened, but the body text is **not** shown yet — it appears only after the
     response arrives.
3. Plain variant: the response is released. Retry variant: the response is aborted instead.
   - *Expected (retry)*: an alert shows and the body is still hidden; **Retry saved operation** is
     pressed.
4. The body appears.
   - *Expected*: the matching `data-practice-focus` target holds focus, sits below the fixed header
     and inside the viewport; the body text is visible and intersects the usable area below the
     header; for the introduction and lesson the region has `overflow-y: visible` and its button is
     `aria-expanded`; no horizontal overflow.
5. Request bookkeeping.
   - *Expected*: plain, one support request; retry, two requests with identical bodies.
