# Review the Engine Playground

## Purpose

This proposed walkthrough helps evaluate the engine before expanding content or producing media. Open `http://localhost:8088`. It is a private developer preview, with six formats and draft Danish content rather than a complete beginner course.

## Try the six formats

1. **Present form:** open “Learn this first,” then choose the finite present form. The alternatives deliberately use the same verb. Check the answer, inspect feedback, retry, then advance. Opening the lesson or hint records supported practice; that evidence stays attached to retries of the same exercise.
2. **Article:** expand “Generation controls” and select “Supply the indefinite article.” Type `en` or `et`. Try the wrong gender, then retry with the matching form. A matching accented numeral such as `én` has a different target interpretation from the requested unstressed article; feedback should distinguish that from a gender error.
3. **Order:** select “Arrange a subject-first statement.” Choose whole constituents, including intact noun phrases. Try a subject-first answer, then a fronted object where available. A grammatical, context-dependent fronted statement can miss the requested subject-first target.

4. **Present recall:** select “Supply the present form.” Type one word for the named lemma. Try `æ`, `ø` and `å` buttons with a selection or caret; they assist input and do not count as hints. Opening the lesson reveals the introduced form and records support.
5. **Verb repair:** select “Repair the verb form.” Replace only the marked infinitive, retaining the named verb. A different verb receives acceptable-but-off-target feedback only when the resulting sentence is already in the licensed inventory; other unfamiliar writing stays unknown.
6. **Article repair:** select “Repair the indefinite article.” Replace the marked incorrect article with the noun's unstressed `en` or `et`. The accented numeral distinction still applies.

Use Tab/Enter throughout and try the narrow layout and both themes. If a result cannot be confirmed after a connection problem, “Check the same attempt” safely replays the original submission.

## Inspect variation

Record the format, seed and index under “Generation controls.” Reusing those coordinates and revisions reproduces the exercise presentation; the API's persisted UUID is a separate identity. Change the index or use “Next exercise” to sample variation. The finite pools contain 99 sentences for present/order and 92 for articles, so long-term variety needs more reviewed content.

For the complete inventory, run `./gradlew :engine:exportPilotReview` and read `engine/build/review/pilot-2.md`.

## Record feedback

Capture the coordinates, answer and expected result when something seems wrong. Separate:

- whether the exercise isolates the intended grammar target;
- whether explanations and feedback are understandable;
- whether Danish meaning, naturalness or accepted variants need correction;
- whether the interaction or variation becomes repetitive.

Use this feedback to choose the next engine work. Danish teaching review and learner trials remain separate from automated software checks; these attempts are practice evidence, with no mastery judgment.

## Initial user review — 7 November 2026

The user tried the playground and described it as simple, promising, and a start in a good direction. They reported nothing they disliked or disagreed with. This is positive feedback on the version tried; no specific defect or requested correction was raised.

Keep the current direction as the basis for the next discussion. The feedback does not establish Danish teaching approval, learning effectiveness or an exhaustive review of every case. In the subsequent message, the user authorized the [recommended extension](exercise-extension-proposal.md): typed finite-form recall followed by guided single-target repair. Q21 is resolved for this iteration; BL-034 tracks delivery.
