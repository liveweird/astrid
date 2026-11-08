# Guided practice over the HTTP API

- **Spec**: [tests/practice-api.spec.ts](../tests/practice-api.spec.ts)
- **Actors**: an API client (Playwright `request` context, no browser page) holding a development
  session cookie and the `X-Astrid-Client` and `X-Astrid-Practice-Owner` headers
- **Owns** (exclusive server-side state): each test creates its own development session, one
  `reading-basics` / `practice-1` practice session (seed 47) and its attempts and support events

## Scenario: practice completes after eight unsuccessful attempts without skipping or implying mastery

1. The client opens a development session and creates a practice session.
   - *Expected*: `201`; the state has eight items and a cursor at 0.
2. It tries to advance position 0 before any attempt was saved.
   - *Expected*: `409` — the learner cannot skip an item.
3. For each of the eight positions it saves one wrong answer, replays the same attempt, advances
   twice (the duplicate advance is idempotent) and re-posts the creation body.
   - *Expected*: the cursor equals the position and the current exercise matches the item; items do
     not expose a `prompt`; the attempt replay answers `200`; both advances answer `200` and the
     cursor is position + 1; the re-posted creation returns `200` with the same cursor.
4. After the eighth advance.
   - *Expected*: state `completed`, no current exercise, and a summary of eight attempted, eight
     completed, eight attempts and zero retries with eight rows — completion is not a pass gate.

## Scenario: practice freezes first-attempt support and exposure while retaining optional retries

1. The client creates a practice session and saves one wrong attempt on item 0.
   - *Expected*: the item's first-attempt context shows no introduction, lesson or hint opened and
     no prior exposure.
2. It records support events: introduction, lesson for position 0, hint for position 0 (all `200`),
   then saves a second wrong attempt.
   - *Expected*: attempt count 2; the first-attempt context is unchanged; the last-attempt context
     has introduction, lesson and hint opened and a prior exposure from `feedback` at position 0.
3. It records a hint for position 7.
   - *Expected*: `409` — support for a position that is not current is refused.

## Scenario: practice serializes concurrent retries and duplicate advancement and rejects stale new answers

1. The client creates a practice session and submits two different wrong attempts for the current
   exercise in parallel.
   - *Expected*: both answer `200`; the item records exactly two attempts.
2. It sends three advances for position 0 in parallel.
   - *Expected*: all `200`; the cursor is 1 (advanced once).
3. It submits a new attempt (new id) for the already-completed first exercise.
   - *Expected*: `409`.
4. It re-sends one of the earlier attempt bodies unchanged.
   - *Expected*: `200` — an exact replay still returns the stored result.

## Scenario: practice continuity header prevents a pending creation being reassociated after cookie renewal

1. The client opens a development session and records its owner id, prepares a creation body, then
   clears its cookies and opens a new session.
   - *Expected*: the new owner id differs from the old one.
2. It sends the creation body with the old owner id in `X-Astrid-Practice-Owner`.
   - *Expected*: `409`.
3. It reads that practice session id.
   - *Expected*: `404` — nothing was created under the new owner.
