# Practice follow-up reviews

- **Spec**: [tests/practice-follow-up.spec.ts](../tests/practice-follow-up.spec.ts)
- **Actors**: an anonymous learner who finished a `reading-basics` practice run (the fixture
  completes the source run through the API and seeds the browser's run record so the owner-bound
  `?session=` URL recovers it)
- **Owns** (exclusive server-side state): development-session-scoped source practice session,
  follow-up (child) practice sessions, exercises and attempts in each test's own context; device
  `localStorage` run records. The source session must stay byte-identical wherever noted

## Scenario: follow-up selection keeps recognition and recall distinct and preserves its source

1. On the completed source's summary, the learner has not selected anything.
   - *Expected*: **Start selected review** is disabled and no checkbox is checked.
2. They check a recognition (Danish to English) target and then uncheck it.
   - *Expected*: the button enables, then disables again.
3. They select the recall (English to Danish) target for the same sense and start the review.
   - *Expected*: a new practice session with one item, format E008, whose follow-up links the source
     id and the one selected target key; "Part 1 of 1" and a **Source practice** link to
     `/practice?session=<source>` show, and survive a reload.
4. The child is answered and completed through the API; the learner reloads.
   - *Expected*: "Practice complete" shows; the child's first attempt carries a source exposure for
     that sense's headword; the source session JSON is byte-identical to before.
5. They follow **Source practice**.
   - *Expected*: the source's "Practice complete" summary shows, the URL has the source session id,
     and the source is still unchanged.

## Scenario: follow-up creation recovers exact source and body after ${committed ? "committed" : "uncommitted"} response loss

Runs with the first follow-up creation lost before the server (`uncommitted`) and after the server
committed it (`committed`).

1. From a completed source the learner selects the `G006` target and starts the review; the first
   follow-up request is aborted (after the server saw it, in the committed case).
   - *Expected*: an alert shows; the URL already carries a new session id different from the source.
2. They reload.
   - *Expected*: the review opens at **Check answer**; the creation was sent twice with identical
     bodies, both to the source's `/follow-ups` endpoint; no new development session was requested.
3. They read the child through the API.
   - *Expected*: four items, all for `G006`, in formats E002 and E006, linked to the source; the
     source's run record is still in `localStorage`.

## Scenario: follow-up creation can retry the saved operation before a child state exists

1. The learner selects a recognition target and starts the review; the first request is committed
   by the server and then aborted.
   - *Expected*: an alert shows.
2. They press **Retry saved operation**.
   - *Expected*: the review opens at **Check answer**; two requests with identical bodies were sent.

## Scenario: follow-up selection and source context remain accessible at enlarged text in ${theme}

Runs in light and dark (emulated colour scheme).

1. At 390×844 with text at 200%, the learner is on a completed source summary.
   - *Expected*: zero axe violations and no horizontal overflow.
2. They focus the `G005` target's checkbox and press Space, then Tab to **Start selected review**
   (within 50 presses) and press Enter.
   - *Expected*: the checkbox is checked; the button receives focus; the review opens at
     **Check answer** with focus on the page's `h2`; the page scans clean.
3. The review is completed through the API; the learner reloads and enlarges text again.
   - *Expected*: "Practice complete" shows and scans clean.

## Scenario: all eight summary targets can be selected without merging lexical directions

1. On the completed source summary the learner checks every target row.
   - *Expected*: the summary has eight rows and eight checkboxes are checked.
2. They start the review.
   - *Expected*: "Part 1 of 8"; the child has eight items and its follow-up lists exactly the eight
     target keys (recognition and recall of the same headword remain separate keys).
3. The child is completed through the API and the learner reloads.
   - *Expected*: "Practice complete" shows and the child's summary rows carry the same eight keys.

## Scenario: an uncertain follow-up creation is never reassigned after cookie rotation

1. The learner selects `G005` and starts the review; the creation request is aborted every time.
   - *Expected*: an alert shows; the URL has the pending child's session id.
2. The cookies are cleared, a fresh development session is opened and the learner reloads.
   - *Expected*: an alert says the session expired, differs or is unavailable; the creation request
     was sent only once (no silent replay under the new owner).
3. They read the child and source sessions through the API.
   - *Expected*: both answer `404` for the new owner.
