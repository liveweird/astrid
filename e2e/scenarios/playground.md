# Engine playground: exercises, keyboard use, replay and theme

- **Spec**: [tests/playground.spec.ts](../tests/playground.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/playground`)
- **Owns** (exclusive server-side state): development-session-scoped generated exercises and graded
  attempts in each test's own browser context; the theme choice is device state

## Scenario: completes each pilot exercise format

1. The learner opens the playground.
   - *Expected*: the "Danish engine playground" heading and **Check answer** show.
2. They open **Learn this first**, pick the first answer choice and check.
   - *Expected*: the lesson region shows its text about the finite present form; a feedback status
     appears reading "Try 1 · Supported practice".
3. They choose **Supply the indefinite article**, type "en" in "Missing word" and check.
   - *Expected*: a feedback status appears.
4. They choose **Arrange a subject-first statement**, click every available sentence part in turn
   and check.
   - *Expected*: a feedback status appears.

## Scenario: replays the exact attempt after an uncertain connection result

1. The first two attempt requests are committed by the server and then aborted.
   The learner opens the playground, picks the first answer, Tabs to **Check answer** and presses
   Enter.
   - *Expected*: "The result is not confirmed" shows, the answer choices are disabled and the alert
     itself holds focus.
2. They Tab once and press Enter on **Check the same attempt**.
   - *Expected*: the replay button was focused; the second response is also lost, so the alert shows
     again and regains focus.
3. They repeat Tab and Enter.
   - *Expected*: the third request succeeds and the feedback status holds focus; exactly three
     attempt requests were sent and all three bodies are identical.

## Scenario: completes, retries and advances an ordering exercise with the keyboard

1. At 390×844 the learner Tabs to **Generation controls**, opens it and types index 16.
   - *Expected*: the controls expand and accept keyboard entry.
2. They Tab backwards to the Exercise type combobox, type "Arrange a subject-first statement",
   press Arrow Down and Enter.
   - *Expected*: the option is highlighted and the combobox shows the title; the instruction heading
     holds focus.
3. They press Enter on "I", "har", "en hund", remove the last block, and place it again, only by
   keyboard.
   - *Expected*: after each Enter, focus moves to the next available part or the new position's
     remove button; "Remove I from position 1" reads "I".
4. They Tab to **Check answer** and press Enter.
   - *Expected*: the feedback status holds focus.
5. They Tab to **Try again**, rebuild the answer, check again, then Tab to **Next exercise**.
   - *Expected*: **Try again** is focused and then **Next exercise**; pressing Enter on it focuses the
     ordering instruction heading of the new exercise.

## Scenario: explicit light theme overrides a dark operating-system preference

1. The browser prefers a dark colour scheme; the learner opens the playground and presses
   **Use light theme**.
   - *Expected*: `<html data-mantine-color-scheme="light">` and a **Use dark theme** button shows.
2. They read the computed styles.
   - *Expected*: header and navbar backgrounds are `rgb(255, 255, 255)`, the main canvas
     `rgb(248, 249, 251)`, cards `rgb(255, 255, 255)`, and the heading text meets 4.5:1 contrast
     against the canvas.
