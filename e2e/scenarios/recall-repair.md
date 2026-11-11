# Verb recall and repair formats

- **Spec**: [tests/recall-repair.spec.ts](../tests/recall-repair.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/playground`)
- **Owns** (exclusive server-side state): development-session-scoped generated exercises and graded
  attempts in each test's own context

Outcome labels such as "Target met" / "Target to revisit" and "Acceptable Danish" / "Known language
issue" / "Language status unknown" describe what the engine reports, not a claim that a response is
approved Danish; all Danish content is a draft awaiting teaching review. Exercises are chosen with
seed 47 and a fixed index.

## Scenario: recalls a named verb with keyboard Danish entry and explicit support evidence

1. The learner chooses **Supply the present form** (index 5).
   - *Expected*: the heading asks them to type the finite present form of *læse*.
2. They type "lXser", select the "X" with Home, Arrow Right and Shift+Arrow Right, then Tab to
   **Insert æ** and press Enter.
   - *Expected*: the field holds "læser", keeps focus and the caret sits after the "æ"
     (selection 2–2).
3. They check.
   - *Expected*: "Target met" and "Acceptable Danish" show, the completed sentence "Hun læser et
     brev." shows, and the try label reads "Try 1 · No support opened".
4. They press **Try again**, open **Learn this first** and check "læser" again.
   - *Expected*: the lesson region mentions *læser*; the result reads "Target met" and
     "Try 2 · Supported practice".

## Scenario: repairs only the named verb and distinguishes licensed alternatives from unknown writing

1. The learner chooses **Repair the verb form** (index 5).
   - *Expected*: "Word to replace" shows.
2. They answer in turn: "har", "unfamiliar", "læse", "læser" (with **Try again** between).
   - *Expected*: "har" → "Target to revisit", "Acceptable Danish" and a note that the task asks
     for the named verb; "unfamiliar" → "Target to revisit", "Language status unknown"; "læse" →
     "Target to revisit", "Known language issue"; "læser" → "Target met", "Acceptable Danish" and
     "Hun læser et brev.".
3. They choose the same format at index 1 and answer "sover".
   - *Expected*: the sentence to repair ends with "sove."; the result is "Target met",
     "Acceptable Danish" and "Du sover.".

## Scenario: repairs an article while distinguishing a numeral from the unstressed target

1. The learner chooses **Repair the indefinite article** (index 0) and answers "et", "én", "en"
   (with **Try again** between).
   - *Expected*: "et" → "Target to revisit", "Known language issue"; "én" → "Target to revisit",
     "Acceptable Danish" and a note that the task asks for the unstressed indefinite article; "en"
     → "Target met", "Acceptable Danish" and "Vi læser en bog.".

## Scenario: freezes typed recall and character entry until an uncertain attempt is replayed

1. The first attempt request is committed by the server and then aborted. The learner chooses
   **Supply the present form** (index 5), types "læser" and checks.
   - *Expected*: "The result is not confirmed" shows; the text field and the **Insert æ**,
     **Insert ø** and **Insert å** buttons are disabled.
2. They press **Check the same attempt**.
   - *Expected*: the feedback status holds focus; two attempt requests with identical bodies were
     sent; the result reads "Try 1 · No support opened".
