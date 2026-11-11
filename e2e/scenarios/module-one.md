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
   - Expected: selection and recall have separate two-item rows. First outcomes retain the selected and typed marker errors plus the unknown response, latest outcomes retain corrected answers and the capitalization issue, and the seven-answer/three-retry totals survive reload. **Expressing an intention** is offered next.

## Scenario: ${round.focus} teaches contextual meaning then supplied modal order

### Volition round

1. Enter **Expressing an intention** directly and try the first answer before opening the required introduction.
   - Expected: the pair-fourteen two-item round requires G001–G005, blocks the early answer, and saves no attempt. The items expose one volition-meaning target followed by supplied-modal order.
2. Open the introduction and inspect `at ville → vil` and `Jeg vil læse en bog.`.
   - Expected: both carry Danish language metadata. The modal reading stays limited to intention in the given context, and the first target reads **Intention in context**.
3. For `Hun vil læse et brev.`, choose **An external requirement.**, inspect the known wrong interpretation, retry, and choose **A personal wish or intention.**.
   - Expected: the Danish statement is marked Danish and all three related meaning choices are marked English. The wrong supplied choice may continue under the modal-round policy, while the retry records the correct contextual meaning.
4. In the supplied construction, place `vi vil læse avis en`.
   - Expected: subject, finite modal and nonfinite verb order meet G005 while the complete requested order constraint is unmet and the Danish is unknown. The policy permits continuation. Clear the retained word tiles and retry with `vi vil læse en avis`.
5. Inspect the two primary summary rows, open **Understanding a requirement**, cancel the chooser, reload and inspect the same summary.
   - Expected: contextual **Meaning selection** and supplied-modal order stay separate, first/latest evidence and four-answer/two-retry totals survive, and the next round is explicitly selected without starting it.

### Obligation round

1. Enter **Understanding a requirement**, open its required teaching, and inspect `at skulle → skal` and `Du skal læse et brev.`.
   - Expected: each exact Danish teaching element has Danish language metadata, and the first target reads **Requirement in context**.
2. For `De skal læse en bog.`, choose **Permission to do it.**, then retry with **An external requirement.**.
   - Expected: wrong and correct contextual meanings remain saved as first and latest outcomes using English choices.
3. In the supplied construction, place only `jeg skal læse` and check it.
   - Expected: the incomplete response is unevaluable with `REQUIRED_WORDS_MISSING`. The modal-round policy permits continuation. Clear the retained word tiles and retry with `jeg skal læse en avis`.
4. Inspect the two primary summary rows, open **Understanding permission**, cancel, reload and inspect the same summary.
   - Expected: four answers and two retries survive, with distinct contextual-meaning and supplied-order evidence.

### Permission round

1. Enter **Understanding permission**, open its required teaching, and inspect `at måtte → må` and `Han må læse en avis.`.
   - Expected: each exact Danish teaching element has Danish language metadata, and the first target reads **Permission in context**.
2. For `Du må læse et brev.`, choose **A personal wish or intention.**, then retry with **Permission to do it.**.
   - Expected: the supplied English interpretations retain wrong and corrected contextual-meaning evidence.
3. Build `hun må læse en bog` with all five supplied word occurrences.
   - Expected: the canonical order meets the supplied-modal-order target and requested response constraint.
4. Inspect and reload the completed summary.
   - Expected: the two primary results and three-answer/one-retry totals survive reload. **Adding when and where** is explicitly selected without starting it.

## Scenario: final circumstances retain clause-core and requested-end evidence

1. Enter **Adding when and where** directly and try an empty order before opening the required introduction.
   - Expected: the pair-fifteen four-item run exposes the expanded Module 1 prerequisites G001–G005, blocks assessment before teaching, records no attempt and keeps every item under G016 / **One final circumstance — constituent order**.
2. Open the introduction and inspect the clause-core rule, separate **Today** → `i dag`, **Now** → `nu`, and **In Aarhus** → `i Aarhus` forms, the English word-unit explanation and the three worked statements.
   - Expected: each introduced form and worked Danish statement has Danish language metadata. The teaching says that the expression meaning today contains two written words and fills one constituent, limits the rule to this round, and the target badge reads **Time or place at the end**.
3. Inspect the four exact cards for `Jeg arbejder.`, `Hun læser et brev.`, `Vi arbejder.` and `De læser en avis.`.
   - Expected: each original Danish clause is the Danish cue; its English translation and the English circumstance gloss today, now or in Aarhus stay in context. Subject, finite verb, optional intact object and intact circumstance are separate Danish sentence parts. Public word-tile metadata is absent, `i dag`, `i Aarhus`, `et brev` and `en avis` are not split, and **Check answer** remains disabled until all supplied constituents are chosen.
4. On the first card, put the circumstance last but swap the clause core, then retry canonically.
   - Expected: the target is met, while meaning and Danish remain unknown and the requested-order constraint is not met with `REQUESTED_ORDER_CONSTRAINT`. The corrected response is fully met.
5. On the second card, put the circumstance before the clause core, then retry canonically.
   - Expected: target and constraint are both not met, meaning and Danish remain unknown, and retrying saves the fully met latest outcome.
6. Save the canonical third answer, reload it, retry the restored constituent order, and finish the fourth card.
   - Expected: raw block identity and visible order survive reload. The summary has one four-item G016 constituent-order row, seven attempts, three retries, distinct first target/constraint evidence and four fully met latest outcomes.
7. Reload the completed run.
   - Expected: teaching confirmation and summary are unchanged, accessibility/reflow checks still pass, and **Asking yes or no questions** is explicitly selected without starting it.

## Scenario: a final-circumstance order replays byte-identically after committed response loss

1. Open the required teaching, arrange the first final-circumstance card canonically and lose the attempt response after server commit.
   - Expected: recovery remains available and retains the exact ordered block IDs.
2. Reload and let the saved operation replay.
   - Expected: the second request is byte-identical, one attempt is stored, the restored answer is fully met and continuation is available.

## Scenario: final-circumstance teaching feedback and summary reflow in ${theme} at 320

1. Open **Adding when and where** in the selected colour scheme at 320 px with 200% text, then open its required introduction.
   - Expected: the final-circumstance teaching has no horizontal overflow, clipped teaching text or configured Axe violations.
2. Complete the first constituent order and inspect its confirmed feedback before completing the remaining cards.
   - Expected: the complete-bank controls and feedback remain readable and accessible at the bounded viewport.
3. Inspect the completed four-item summary.
   - Expected: the G016 constituent-order result, counts and terminal actions reflow without horizontal overflow, clipped summary text or configured Axe violations.

## Scenario: neutral yes/no questions retain verb-first and subject-second evidence

1. Enter **Asking yes or no questions** directly and try an empty order before opening the required introduction.
   - Expected: the pair-sixteen four-item run exposes G001–G005 prerequisites, blocks assessment before teaching, records no attempt and keeps every item under G018 / **Neutral positive yes/no question — constituent order**.
2. Open the introduction and inspect `finite verb | subject | supplied remainder`, both statement-to-question transformations, the no-`do` explanation, and separate Danish forms `I`, `i`, `i Aarhus`, `ja`, and `nej` with their English meanings.
   - Expected: every Danish form, statement and question has Danish language metadata. Short answers are teaching support only; there is no answer selection or lexical score. The target badge reads **Neutral yes/no question order** and the boundary excludes modal, negative, echo and sentence-initial modal-particle questions.
3. Inspect the four exact positive Danish propositions, reference contexts and English question glosses.
   - Expected: each Danish proposition is the Danish cue. Its reference context and complete English question stay in English context, without repeating `ja` or `nej`. The banks contain finite verb, subject and optional intact object/location complement. Public word-tile metadata is absent, the question mark is supplied, and **Check answer** stays disabled until every constituent is chosen.
4. For the newspaper and work cards, submit subject before finite verb, inspect the saved result, then retry canonically.
   - Expected: the requested target and response constraint are not met, Danish and meaning remain unknown, `REQUESTED_ORDER_CONSTRAINT` is saved, and the complete previous order remains visible on retry before correction.
5. Build and save `Bor I i Aarhus?`, reload, and inspect the restored order before retrying it.
   - Expected: `Bor` is capitalized at position one, lexical subject `I` remains uppercase at position two, `i Aarhus` remains one lowercase-preposition constituent, raw block IDs and the complete retry order survive, and the supplied question mark remains visible.
6. Complete the possession question and inspect the summary.
   - Expected: one four-item G018 **Question order** row retains two wrong first responses, four fully met latest responses, seven answers and three retries. The completed run has no S14 continuation.
7. Reload the terminal summary.
   - Expected: teaching confirmation and summary remain exact and accessibility/reflow checks still pass.

## Scenario: a neutral-question order replays byte-identically after committed response loss

1. Open the required teaching, arrange the first yes/no question canonically and lose the attempt response after server commit.
   - Expected: recovery retains the exact constituent order and question task identity.
2. Reload and let the saved operation replay.
   - Expected: the second request is byte-identical, one attempt is stored, the restored question is fully met and continuation is available.

## Scenario: neutral-question teaching feedback and summary reflow in ${theme} at 320

1. Open **Asking yes or no questions** in the selected colour scheme at 320 px with 200% text, then open its required introduction.
   - Expected: question teaching has no horizontal overflow, clipped text or configured Axe violations.
2. Complete the first constituent order and inspect its confirmed feedback before completing the remaining cards.
   - Expected: complete-bank controls, uppercase `I`, lowercase `i`, supplied punctuation and feedback remain readable and accessible.
3. Inspect the completed four-item summary.
   - Expected: the G018 **Question order** result, counts and terminal actions reflow without horizontal overflow, clipped summary text or configured Axe violations.

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

1. Open the chooser with eighteen profiles, including 15 Module 1 sessions, in both themes, narrow portrait and short landscape, at 200% text.
   - Expected: its first-step grouping and every profile remain accessible, without page/text overflow or configured Axe violations.
2. Focus the group-pronoun radio and select it with Space.
   - Expected: the choice is checked and Start practice is enabled; no session is created automatically.

## Not covered here

Linguistic teaching approval and learning efficacy remain separate from software verification.
Domain grading, revision/fingerprint mutation matrices and database serialization/locking are covered by engine/server tests.
