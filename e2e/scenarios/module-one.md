# First Module 1 sessions

- **Spec**: [tests/module-one.spec.ts](../tests/module-one.spec.ts)
- **Actors**: an anonymous beginner and a same-owner API client
- **Owns**: a fresh development session and its exercises, attempts, practice runs and browser storage per scenario

## Scenario: first steps teach before assessment and retain scoped outcomes for ${profile.focus}

1. Choose each of the four first-step profiles directly, with no previous-course assumption.
   - Expected: the three six-item profiles remain pinned to pair eight; the four-item one-thing profile pins pair nine. Saved introduction is required and assessment controls stay hidden until it is opened.
2. Attempt submission through the API before the introduction, then open the introduction through the UI.
   - Expected: the early submission is rejected without an attempt; confirmed support opens the assessment.
   - Expected: direct-entry foundation explains words and subject roles. The retained first three profiles supply Danish letters and an annotated Danish statement; S04 instead supplies the noun bundles, their genders, reference forms and the distinction from identification. Every exercise uses a readable target label rather than a grammar ID.
3. Complete every activity using independent literal expectations: the retained profiles use their authored English cues, while S04 shows Danish `en bog` or `et brev` and supplies meaning and gender in the English context.
   - Expected: the S04 cue and all Danish answer controls carry Danish language tags; correct saved answers include the introduction exposure, selection and recall remain distinct, and the profile's exact item budget is preserved.
4. Reload the completed run.
   - Expected: summary outcomes remain exact; no unsupported targeted-review controls appear.
5. Open repeat and the next-session chooser, including S04 → S05. Cancel each.
   - Expected: the appropriate profile is selected without a new creation request, and the completed source run is retained.

## Scenario: first steps reflow teaching and scoped summaries for ${profile.focus} in ${theme} at ${viewport.width}

1. Open each profile in light and dark themes, at narrow portrait and short landscape viewports, with 200% text.
2. Inspect the required-introduction state, saved teaching, exercise feedback and completed scoped summary.
   - Expected: Axe finds no configured WCAG violations, the page has no horizontal overflow, Danish content keeps its language tag, and teaching/summary text stays inside its own paragraph or column.

## Scenario: named present forms teach three verbs and retain selection and recall

1. Enter Using present verb forms directly and save its required introduction.
   - Expected: the six named infinitive and present forms are exposed as Danish teaching before assessment.
2. Complete the three supplied-choice and three typed-form activities from their literal sentence frames.
   - Expected: every answer is graded as the named present form and the summary retains separate three-item selection and recall rows.
3. Open the next-session chooser.
   - Expected: Building subject-first statements is preselected and the completed session remains in the URL.

## Scenario: subject-first assembly checks constituents and partial word tiles with saved order recovery

1. Enter Building subject-first statements directly, save the required teaching and complete the three constituent-bank activities using the keyboard.
   - Expected: the English proposition is visible, every supplied constituent is required, and the canonical subject-first order succeeds.
2. On the first individual-word activity, check one selected word and reload the saved outcome.
   - Expected: the ordered block identity is stored, the precise missing-words constraint permits continuation, feedback identifies the response constraint, and the result survives reload.
3. Retry the saved partial selection, edit the retained tile and complete all three individual-word activities.
   - Expected: constituent-order and word-order have separate three-item summary rows. **Saying what someone has** is offered as the explicit next session. The completed state passes Axe at 320 px and 200% text.

## Scenario: possession with har requires teaching and retains literal first and latest outcomes

1. Enter **Saying what someone has** directly and attempt its current exercise through the API before opening the required introduction.
   - Expected: the four-item pair-eleven session has prerequisites G001–G005, rejects assessment before teaching, and exposes no saved attempt.
2. Open the introduction and inspect the teaching and first activity.
   - Expected: the introduction explains the shared present form across subjects. `at have → har`, the worked possession statement and supplied Danish answers carry Danish language metadata. The readable target is **Present possession with har**.
3. Complete the literal frames `Han ___ et brev`, `Vi ___ en avis`, `Du ___ en bog` and `De ___ et brev`; first choose `have` once and then correct it to `har`.
   - Expected: the introduced infinitive is a known form but does not meet the present-possession target; retrying with `har` replaces the latest outcome while preserving the first outcome.
4. In written recall, submit the unsupported form `havde`, reload, then retry with `har`.
   - Expected: the latest unknown blocks continuation and survives reload without disclosing a solution. Retry clears the field, `har` succeeds, and the summary separates two selection from two recall items with exact first/latest outcomes.
5. Reload the completed summary.
   - Expected: it is retained, passes the configured accessibility and reflow scan, and offers **Words for things you read** as the next session.

## Scenario: reading words retain related meaning choices and per-sense results

1. Enter **Words for things you read** directly and attempt the first item through the API before opening the required introduction.
   - Expected: the four-item retained pair-five session has no grammar prerequisite, rejects assessment before teaching, and records no attempt. Each item has one lexical meaning target and no grammar scope, mode or sub-assessment.
2. Open the introduction and inspect `et magasin`, `en bog`, `en avis` and `et brev`.
   - Expected: the new printed-magazine meaning and the three reviewed reading-word bundles carry Danish language metadata. Every activity shows an English meaning and the same four complete Danish article-and-noun choices.
3. On the printed-magazine item, choose the known but wrong `en bog`, retry, focus `et magasin` and submit it with the keyboard. Complete book, newspaper and written letter in their exact authored order.
   - Expected: the wrong related meaning is acceptable Danish but misses the target and can advance; the retry succeeds without treating the article as a separate assessment. All cues and choices retain their language metadata.
4. Inspect and reload the completed summary.
   - Expected: four exact one-item sense rows are labelled **Meaning selection** rather than written headword recall, retain first/latest outcomes and the five-answer/one-retry totals, and survive reload. **Names, homes and languages** is offered next.

## Scenario: personal present forms alternate choice and recall across six literal statements

1. Enter **Names, homes and languages** directly and attempt the first activity through the API before opening its required introduction.
   - Expected: the six-item pair-twelve session requires G001–G005, rejects assessment before teaching and records no early attempt. Every item uses the personal-introduction present-form scope.
2. Open the introduction and inspect the separate dictionary/present pairs, worked examples and English explanations for naming, residence and language.
   - Expected: `at hedde → hedder`, `Hun hedder Nora.`, `at bo → bor`, `Jeg bor i Odense.`, `at tale → taler` and `Du taler dansk.` carry Danish language metadata. Their meanings remain English teaching, and the target badge is **Present forms for personal details**.
3. Complete the literal cards in round order: `Jeg ___ Anna` then `Du ___ Nora`; `Hun ___ i Aarhus` then `De ___ i Odense`; `Vi ___ dansk` then `I ___ dansk`.
   - Expected: each round alternates an E022 choice and an E023 typed response, the exact English proposition is supplied, and the Danish cue, choices or input retain their language metadata. `hedder`, `bor` and `taler` each succeed in both modes.
4. Inspect and reload the summary.
   - Expected: selection and recall have separate three-item scoped rows with six fully met first/latest outcomes, the completed state survives reload, and **Using a verb after kan** is offered next.

## Scenario: ability after kan separates selection and one-word response outcomes

1. Enter **Using a verb after kan** directly and attempt the first activity through the API before opening its required introduction.
   - Expected: the four-item pair-thirteen session requires G001–G005, rejects assessment before teaching, records no early attempt and keeps all items under the bare-infinitive-after-kan scope.
2. Open the introduction and inspect the supplied modal, target verb, rule and separate examples.
   - Expected: `at kunne → kan`, `at læse → læse efter kan`, `Jeg kan læse et brev.` and `Han kan læse dansk.` carry Danish language metadata. The rule distinguishes bare `læse` from `læser` and `at læse`, and the readable target is **Verb form after kan**.
3. On the first E026 selection choose `at læse`, inspect the known grammar outcome, then retry with `læse`.
   - Expected: the marked infinitive misses the target while satisfying the supplied-choice constraint. Retry succeeds, and the three choices are exactly `læse`, `læser` and `at læse`.
4. On the first E027 typed response enter `at læse`, inspect the result, then retry with `læse`.
   - Expected: the same marker diagnosis precedes generic multiword handling, while the one-word response constraint is now unmet. Retry succeeds.
5. On the final typed response enter unsupported `læste`, reload, then retry with `Læse`.
   - Expected: the unknown answer blocks continuation, discloses no solution and survives reload byte for byte. The capitalized target meets the grammar target with a saved lowercase constraint issue and can continue.
6. Inspect and reload the summary.
   - Expected: selection and recall have separate two-item rows. First outcomes retain the selected and typed marker errors plus the unknown response, latest outcomes retain corrected answers and the capitalization issue, and the seven-answer/three-retry totals survive reload. No unimplemented next session is offered.

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

## Scenario: one-thing recall keeps case evidence while the latest unknown supersedes an eligible answer

1. Enter the one-thing profile directly, save its required teaching and reach neuter `det` recall.
2. Save a correct answer, then retry with capitalized `Det`.
   - Expected: the reference target remains met, the known standalone-case constraint is recorded, the lowercase correction is tagged Danish, continuation remains available, and the narrow dark 200%-text feedback state passes Axe and reflow checks.
3. Retry with unsupported `dem`, inspect the saved response, then reload.
   - Expected: the latest unknown blocks continuation despite the eligible history, discloses no correction, its exact stored response is unchanged after reload, and its narrow enlarged feedback state passes Axe and reflow checks.
4. Retry with `det er`, finish all four cards and inspect the two scoped summary rows.
   - Expected: the one-word shape issue permits continuation without prefix grading; its narrow enlarged feedback passes Axe and reflow checks; selection and recall retain separate two-item first/latest outcomes. S05 can be opened through the explicit next-session chooser.

## Scenario: first steps confirm required teaching only after ${committed ? "committed" : "uncommitted"} support replay

1. Open a fresh first-step run and lose the first introduction-support response before or after its server commit.
   - Expected: teaching and assessment stay hidden while the result is uncertain.
2. Reload and let the saved operation replay.
   - Expected: request bytes are identical, the same exercise remains current, confirmed teaching opens, and no attempt was manufactured.

## Scenario: first steps replay the exact typed form for ${replayProfile.focus} after ${committed ? "committed" : "uncommitted"} answer loss

1. Reach typed recall in the retained pair-eight singular profile or the pair-nine one-thing profile and submit the correct introduced form with surrounding spaces.
2. Lose the response before or after commit and reload.
   - Expected: recovery sends the exact identity/body and raw spaces, confirms one saved attempt, receives server permission to continue, and the final result bytes equal the attempt readback.

## Scenario: first steps retain creation identity through ${committed ? "committed" : "uncommitted"} response loss

1. Choose the present-form session and lose its creation response before or after commit.
2. Reload and replay the saved creation.
   - Expected: one run identity and byte-identical request are retained; its introduction is still unopened and no assessment is enabled automatically.

## Scenario: first steps keep an unfinished typed form while lesson and hint support are saved

1. Begin a typed answer, then open the item lesson and hint.
   - Expected: both confirmed support operations retain the unfinished input; focus moves to the newly opened lesson and then to the hint.
2. Complete and submit the answer.
   - Expected: the saved attempt context records both support events.

## Scenario: first-step chooser keeps all profiles keyboard-accessible in ${theme} at ${viewport.width}

1. Open the chooser with thirteen profiles, including 10 Module 1 sessions, in both themes, narrow portrait and short landscape, at 200% text.
   - Expected: its first-step grouping and every profile remain accessible, without page/text overflow or configured Axe violations.
2. Focus the group-pronoun radio and select it with Space.
   - Expected: the choice is checked and Start practice is enabled; no session is created automatically.

## Not covered here

Linguistic teaching approval and learning efficacy remain separate from software verification.
Domain grading, revision/fingerprint mutation matrices and database serialization/locking are covered by engine/server tests.
