# Practice: lost answers queued from two tabs

- **Spec**: [tests/practice-multitab.spec.ts](../tests/practice-multitab.spec.ts)
- **Actors**: one learner with the same practice run open in two tabs of one browser context
- **Owns** (exclusive server-side state): one development-session-scoped **Mixed practice**
  session with two attempts on its first item; device `localStorage` outbox entries
  (`astrid.practice.v1.outbox.<id>.attempt.*`)

## Scenario: practice drains two tabs' immutable lost answers in enqueue order without skipping

The first attempt id is forced to `ffffffff-ffff-4fff-8fff-ffffffffffff` in tab A and the second to
`00000000-0000-4000-8000-000000000000` in tab B, so ids do not follow lexical order.

1. The learner starts **Mixed practice** (first item is a vocabulary recognition, E007) and opens
   the same URL in a second tab.
   - *Expected*: both tabs show **Check answer**; the item has two wrong choices.
2. Every attempt request in both tabs is aborted before reaching the server. Tab A checks wrong
   choice one, then tab B checks wrong choice two.
   - *Expected*: both tabs show "Your answer is not confirmed", have **Continue** absent, the
     answer choices and **Session introduction** disabled; the server still records no attempts.
3. They inspect the saved outbox.
   - *Expected*: two entries, in enqueue order (tab A's id first, `queuedAt` increasing); each stores
     the exact request body (attempt id, exercise id and text answer). Each tab's one lost wire
     request carried the oldest queued body, so tab B's first request replayed tab A's entry.
4. Tab A's network is restored but the second request is held at the server gate; tab A presses
   **Retry saved operation**.
   - *Expected*: the first body is sent, then the second, byte-identical to what was queued; while
     the second is held **Continue** is absent, the retry button is disabled and the server shows
     one attempt.
5. The held request is released.
   - *Expected*: **Continue** enables; the outbox is empty; the server has two attempts with ids in
     enqueue order; the first answer is choice one, the last answer choice two; the cursor is still 0.
6. Tab B's network is restored and it presses **Retry saved operation**, then reloads.
   - *Expected*: only the first queued body is re-sent (the server already stored it); **Continue**
     enables; the attempt count stays 2; after reload no further request is made.
7. Each tab presses **Continue**.
   - *Expected*: both reach "Part 2 of 8"; the server cursor is 1 and the first item still has two
     attempts.
