# Playground recovery: connection, revision and session failures

- **Spec**: [tests/recovery.spec.ts](../tests/recovery.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/playground`)
- **Owns** (exclusive server-side state): development sessions, generated exercises and graded
  attempts in each test's own context; failures are injected in the browser by mocking or aborting
  `fetch` responses, never in the server

## Scenario: reconnects after the initial development session fails

1. The first `POST /api/v1/development-session` is aborted. The learner opens the playground.
   - *Expected*: a **Try connection again** button shows.
2. They press it.
   - *Expected*: the second session request succeeds and **Check answer** appears.

## Scenario: offers a full engine reload after a revision conflict

1. The second exercise-generation request is answered with a mocked `409` `revision_conflict`.
   The learner opens the generation controls, opens the Exercise type list and picks the next format
   (**Supply the indefinite article**) with Arrow Down and Enter.
   - *Expected*: the option was highlighted before selection; an alert holds focus and a
     **Reload engine** button shows.
2. They Tab to it and press Enter.
   - *Expected*: the button received focus on the first Tab; **Check answer** and the "Danish engine
     playground" heading show again.

## Scenario: reconnects an expired existing session before generating again

1. The second exercise-generation request is answered with a mocked `401` `unauthorized`. The
   learner opens the Exercise type list and picks **Supply the indefinite article**.
   - *Expected*: the message "Your learning session expired. Reconnect to continue." shows.
2. They press **Reconnect session**.
   - *Expected*: **Check answer** shows; exactly two development-session requests have been made.
3. They pick **Supply the indefinite article** again.
   - *Expected*: the combobox shows that title.

## Scenario: replays the stored attempt after its committed response exceeds the browser deadline

1. The browser clock is installed. The learner picks the first answer and presses **Check answer**;
   the server commits the attempt but the response is held back.
   - *Expected*: the attempt request reaches the server.
2. The clock jumps 15 seconds.
   - *Expected*: "The result is not confirmed" shows and **Check the same attempt** is enabled.
3. The held response is released (the browser has already aborted that fetch) and the learner
   presses the replay button.
   - *Expected*: a feedback status appears showing "Try 1"; two attempt requests with identical
     bodies were sent; the two server responses are byte-identical and the stored attempt id and
     `retryNumber` 1 match the replayed request.
