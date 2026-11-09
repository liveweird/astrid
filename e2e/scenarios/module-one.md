# First Module 1 sessions

- **Spec**: [tests/module-one.spec.ts](../tests/module-one.spec.ts)
- **Actors**: an anonymous beginner and a same-owner API client
- **Owns**: a fresh development session and its exercises, attempts, practice runs and browser storage per scenario

## Scenario: first steps teach before assessment and retain scoped outcomes for ${profile.focus}

1. Choose each of the three first-step profiles directly, with no previous-course assumption.
   - Expected: six items pin pair eight; the saved introduction is required and assessment controls stay hidden until it is opened.
2. Attempt submission through the API before the introduction, then open the introduction through the UI.
   - Expected: the early submission is rejected without an attempt; confirmed support opens the assessment.
   - Expected: direct-entry foundation explains words/roles, supplies Danish letters and an annotated Danish statement, and the exercise uses a readable target label rather than a grammar ID.
3. Complete every activity using independent literal English-cue/Danish-form expectations.
   - Expected: correct saved answers include the introduction exposure; selection and recall remain distinct, and the profile's exact item budget is preserved.
4. Reload the completed run.
   - Expected: summary outcomes remain exact; no unsupported targeted-review controls appear.
5. Open repeat and, where offered, the next-session chooser; cancel each.
   - Expected: the appropriate profile is selected without a new creation request, and the completed source run is retained.

## Scenario: first steps reflow teaching and scoped summaries for ${profile.focus} in ${theme} at ${viewport.width}

1. Open each profile in light and dark themes, at narrow portrait and short landscape viewports, with 200% text.
2. Inspect the required-introduction state, saved teaching, exercise feedback and completed scoped summary.
   - Expected: Axe finds no configured WCAG violations, the page has no horizontal overflow, and teaching/summary text stays inside its own paragraph or column.

## Scenario: first steps preserve ambiguous ${correction.raw} casing without a wrong-person diagnosis

1. Enter the group-pronoun session directly, read its teaching, and reach typed plural-you or plural-they practice.
2. Supply `i` or `De` instead of the introduced `I` or `de`.
   - Expected: raw input is preserved, the reference target remains unevaluable, and a specific written-form constraint correction is recorded.
3. Continue after the confirmed form correction.
   - Expected: the server permits advance without displaying full success or inventing a wrong-person diagnosis.

## Scenario: first steps let the latest unknown block advance while preserving earlier and shape outcomes

1. Reach typed singular-pronoun recall and save a correct answer.
2. Retry with an unsupported single word.
   - Expected: the latest unknown blocks UI and API advancement despite the earlier success; its raw saved response contains no disclosed solution.
3. Retry with two words in a one-word task, then finish the run.
   - Expected: the known shape failure permits advance while the grammar target stays unevaluable. First/latest outcomes retain that distinction, and the intermediate unknown response remains byte-identical.

## Scenario: first steps confirm required teaching only after ${committed ? "committed" : "uncommitted"} support replay

1. Open a fresh first-step run and lose the first introduction-support response before or after its server commit.
   - Expected: teaching and assessment stay hidden while the result is uncertain.
2. Reload and let the saved operation replay.
   - Expected: request bytes are identical, the same exercise remains current, confirmed teaching opens, and no attempt was manufactured.

## Scenario: first steps replay the exact typed form after ${committed ? "committed" : "uncommitted"} answer loss

1. Reach typed recall and submit the correct introduced form with surrounding spaces.
2. Lose the response before or after commit and reload.
   - Expected: recovery sends the exact identity/body and raw spaces, confirms one saved attempt, and receives server permission to continue.

## Scenario: first steps retain creation identity through ${committed ? "committed" : "uncommitted"} response loss

1. Choose the present-form session and lose its creation response before or after commit.
2. Reload and replay the saved creation.
   - Expected: one run identity and byte-identical request are retained; its introduction is still unopened and no assessment is enabled automatically.

## Scenario: first steps keep an unfinished typed form while lesson and hint support are saved

1. Begin a typed answer, then open the item lesson and hint.
   - Expected: both confirmed support operations retain the unfinished input.
2. Complete and submit the answer.
   - Expected: the saved attempt context records both support events.

## Scenario: first-step chooser keeps all profiles keyboard-accessible in ${theme} at ${viewport.width}

1. Open the six-profile chooser in both themes, narrow portrait and short landscape, at 200% text.
   - Expected: its first-step grouping and every profile remain accessible, without page/text overflow or configured Axe violations.
2. Focus the group-pronoun radio and select it with Space.
   - Expected: the choice is checked and Start practice is enabled; no session is created automatically.

## Not covered here

Linguistic teaching approval and learning efficacy remain separate from software verification.
Domain grading, revision/fingerprint mutation matrices and database serialization/locking are covered by engine/server tests.
