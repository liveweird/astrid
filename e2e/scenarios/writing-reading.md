# Sentence writing and short reading formats

- **Spec**: [tests/writing-reading.spec.ts](../tests/writing-reading.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/playground`)
- **Owns** (exclusive server-side state): development-session-scoped generated exercises and graded
  attempts in each test's own context

Writing and reading are the current-revision formats **Write a Danish sentence** and **Read a short
text**. Exercises use seed 47; the writing cases use index 5, whose target response is "Hun læser et
brev." The spec reads the grading response (`targetSatisfaction`, `meaningSatisfaction`,
`linguisticStatus`, `responseConstraintStatus`) as well as the visible feedback. Outcomes describe
engine behaviour for draft content, not teacher approval.

## Scenario: sentence writing accepts a whole response with keyboard Danish insertion and separate meaning evidence

1. The learner chooses **Write a Danish sentence** (index 5).
   - *Expected*: the target skill is `G005`, no reading target, no choices or blocks; the English
     cue is "She reads a letter (written message)." (marked `lang="en"`); the Danish answer is not
     visible; the sentence box is marked `lang="da"`.
2. They type "Hun lXser et brev.", select the "X" by keyboard, Tab to **Insert æ** and press Enter.
   - *Expected*: the box holds "Hun læser et brev.", keeps focus and its caret is at position 6.
3. They check.
   - *Expected*: the grade is target met, meaning met, acceptable Danish for scope `danish-sentence`,
     response constraint met, the completed sentence, no hint used; "Meaning matched" and "Good work"
     show.

## Scenario: writing distinguishes preserved meaning, subject-first order and a valid different statement

For each of three responses (with **Try again** between):

1. The learner enters the response and checks.
   - *Expected*: "Han læser et brev." and "Hun har et brev." → target met, meaning not met
     ("Meaning to revisit"); "Et brev læser hun." → target not met, meaning met ("Meaning
     matched"). All are acceptable Danish with the response constraint met and the completed
     sentence equal to the input; "Good work" never shows.

## Scenario: writing diagnoses bounded form issues and reports unsupported or extra writing without accepting a prefix

For each of five responses (with **Try again** between):

1. The learner enters the response and checks.
   - *Expected*: "Hun læser en brev." → target met, meaning met, known issue; "Hun læse et brev." →
     target not met, meaning met, known issue; "Hun studerer et brev." → target unevaluable,
     meaning unknown, language unknown; "Hun læser et brev. Han sover." and "Hun læser et brev; Han
     sover" → target unevaluable, meaning unknown, language unknown and the response constraint not
     met, with "One statement required" shown.
   - *Expected* throughout: "Good work" and "Supplied choice required" never show; unknown language
     shows a note that it is not a general judgment about Danish.

## Scenario: optional writing reference records support across retries without preassembling the answer

1. The learner chooses **Write a Danish sentence** (index 5) and opens **Learn this first**.
   - *Expected*: the region mentions *læser* but does not contain the full sentence "Hun læser et
     brev.".
2. They enter the sentence and check, press **Try again**, and check it again.
   - *Expected*: both grades report `hintUsed` true with retry numbers 1 and 2, and the status shows
     "Try 1 · Supported practice" and then "Try 2 · Supported practice".

## Scenario: writing replays its exact whole-sentence attempt after ${committed ? "committed" : "uncommitted"} response loss

Runs with the first attempt lost before the server (`uncommitted`) and after the server committed it
(`committed`).

1. The learner chooses **Write a Danish sentence** (index 5), enters the sentence and checks.
   - *Expected*: "The result is not confirmed" shows; the sentence box, the Exercise type combobox and
     **Insert æ/ø/å** are disabled.
2. They press **Check the same attempt**.
   - *Expected*: "Meaning matched" and "Try 1 · No support opened" show; two attempt requests with
     identical bodies were sent.

## Scenario: reading binds the asked participant and action with related English choices

For index 0, 1 and 2 of **Read a short text**:

1. The learner generates the exercise.
   - *Expected*: no target skill, no lexical target, reading facet `participant-action-complement`,
     no `readingSource` field; a three-line passage; the three English meanings as choices; the
     passage group is marked `lang="da"`, the question and the choices `lang="en"`. The expected
     answer is derived from the visible passage and question alone, matching exactly one fact.
2. They choose a wrong choice and check.
   - *Expected*: target not met, acceptable language for scope `english-choice`, constraint met,
     empty completed sentence.
3. They press **Try again**, focus the right choice and press Enter, then check.
   - *Expected*: target met with the same scope; "Recognized English choice" and "Good work" show.

## Scenario: reading preserves supported choice and exact retry when a committed result is lost

1. The first attempt request is committed by the server and then aborted. The learner chooses
   **Read a short text**, opens **Learn this first**, picks the right choice and checks.
   - *Expected*: "The result is not confirmed" shows and every answer choice is disabled.
2. They press **Check the same attempt**.
   - *Expected*: "Good work" and "Try 1 · Supported practice" show; two attempt requests with
     identical bodies were sent.

## Scenario: ${title} and feedback reflow accessibly at enlarged text in ${theme}

Runs for each theme (light, dark) and each of the two formats (**Write a Danish sentence**, **Read a
short text**): four instances.

1. At 320×844 the learner chooses the format (writing at index 5, reading at index 0) and enlarges
   text to 200%.
   - *Expected*: zero axe violations and no horizontal overflow.
2. They answer — writing gets the right sentence plus an extra " Han sover.", reading gets the right
   choice — and check.
   - *Expected*: feedback badges are not clipped (measured against their rounded shape); the page
     scans clean with no horizontal overflow.
