# Review the Engine Playground

## Purpose

This proposed walkthrough helps evaluate the engine before expanding content or producing media. Open `http://localhost:8088`. It is a private developer preview, with fourteen entries and draft Danish content rather than a complete beginner course.

## Try the fourteen entries

1. **Present form:** open “Learn this first,” then choose the finite present form. The alternatives deliberately use the same verb. Check the answer, inspect feedback, retry, then advance. Opening the lesson or hint records supported practice; that evidence stays attached to retries of the same exercise.
2. **Article:** expand “Generation controls” and select “Supply the indefinite article.” Type `en` or `et`. Try the wrong gender, then retry with the matching form. A matching accented numeral such as `én` has a different target interpretation from the requested unstressed article; feedback should distinguish that from a gender error.
3. **Order:** select “Arrange a subject-first statement.” Choose whole constituents, including intact noun phrases. Try a subject-first answer, then a fronted object where available. A grammatical, context-dependent fronted statement can miss the requested subject-first target.

4. **Present recall:** select “Supply the present form.” Type one word for the named lemma. Try `æ`, `ø` and `å` buttons with a selection or caret; they assist input and do not count as hints. Opening the lesson reveals the introduced form and records support.
5. **Verb repair:** select “Repair the verb form.” Replace only the marked infinitive, retaining the named verb. A different verb receives acceptable-but-off-target feedback only when the resulting sentence is already in the licensed inventory; other unfamiliar writing stays unknown.
6. **Article repair:** select “Repair the indefinite article.” Replace the marked incorrect article with the noun's unstressed `en` or `et`. The accented numeral distinction still applies.

7. **Meaning recognition:** select “Recognize the meaning.” Match the Danish headword with an English choice. All three alternatives are related introduced meanings. Feedback describes an English choice rather than calling it Danish.
8. **Headword recall:** select “Recall the Danish headword.” Type `bog`, `avis` or `brev` for the specified English meaning. Try another known noun, then an extra article: `en avis` should acknowledge the identified word while requesting a noun-only answer; `et avis` also receives a stored-gender diagnosis. Unrecognized words or synonyms remain outside the bounded assessment, not invalid Danish. Optional teaching still records support.

9. **Sentence writing:** select “Write a Danish sentence.” Produce a whole statement from the English meaning. Compare a correct answer, a grammatical statement with another meaning, a meaning-preserving fronted answer, a known form/article mistake and unsupported writing. Check the separate order, meaning, language and shape facets. Opening the optional language reference records support; its Danish bodies should receive Danish pronunciation from assistive technology.
10. **Short reading:** select “Read a short text.” Read all three facts, then match both the participant and the action in the English question. Each readable noun appears once; all choices are related. Try a distractor referring to the other participant/action, then retry. Optional support records a reveal; this is supported comprehension of explicitly stated facts, not a proficiency judgment.

11. **Danish word choice:** select “Choose the Danish word.” Match an English meaning to one of four related article+noun bundles. Compare a wrong meaning with the correct choice; both are supplied Danish, while only one matches the cue. Optional support explains all four.
12. **All-word tiles:** select “Build a Danish sentence.” Translate the English cue with individual word tiles, including a separate article. Compare subject-first and licensed object-first orders, then omit a required tile; feedback should distinguish target, meaning and response shape.
13. **Spare-word tiles:** select “Choose the present form with word tiles.” Build the sentence while leaving its infinitive unused. Try the infinitive instead, both forms together and a finite fronted statement. A correct finite verb with fronting meets the verb target but misses the requested response order.

14. **Word matching:** select “Match English and Danish words.” Choose an English meaning, then its Danish article+noun phrase. Check one pair, a mixed board and all four correct pairs. Compare per-pair feedback with the whole-board status. **Try again** keeps every pair editable; later attempts are labeled **Retry after feedback**. Use Enter/Space on the buttons, remove a match and regenerate with an unfinished selection. The new board should start with no selection. Check that complete feedback remains readable at 320px with 200% text.

Use Tab/Enter throughout and try the narrow layout and both themes. If a result cannot be confirmed after a connection problem, “Check the same attempt” safely replays the original submission.

## Try guided practice

Open `http://localhost:8088/practice` and choose **Mixed practice** for eight parts, or **Present verb forms** for six. The verb run alternates choosing a form, written recall and repairing one wrong word, using all five introduced verbs. A saved run resumes directly instead of returning to selection.

1. Open the optional introduction, item lesson or hint. The revealed text should receive focus and stay readable, including in a short window with enlarged text.
2. Give an incorrect answer. Check whether feedback distinguishes the target from response constraints and explains opened support. Retry if useful, or choose **Continue** immediately after the saved result.
3. Refresh midway. The same URL should restore the run, checked answer and support; an unsent draft is not guaranteed to survive refresh. Connection uncertainty offers **Retry saved operation**, retaining the original operation identity.
4. Complete the run and compare first versus latest outcomes. Retries and earlier answer exposure remain visible; completion does not establish mastery. Use **Start new practice** to compare another focus. Opening the chooser and cancelling should preserve the current URL, draft and pending operation; starting another run retains the earlier recovery record.

Assess pacing, exercise transitions, explanation clarity and whether the summary helps decide what to revisit. The [variation audit](practice-variation-audit.md) distinguishes 294 meaningful mixed plans from additional choice/block shuffles; more reviewed language content remains necessary for lasting variety.

## Try targeted follow-up practice

From a completed summary, choose targets with the **Review …** checkboxes, then **Start selected review**. Nothing is preselected. Try one written-headword target: it produces one item, with recognition kept separate. Then return through **Source practice** and choose sentence order or articles: grammar review uses several distinct sentences, and articles alternate supply and repair.

For a completed verb run, compare the three operation cards and select each separately. One or two selected operations produce four parts; all three produce six. Follow-up sentences differ from the immediate source. Incorrect typed feedback repeats the learner's sentence and must not claim to have revealed a correct form. The [topic contract](topic-practice.md) defines these boundaries.

Check whether selecting targets feels useful and whether source exposure is understandable. Refresh a review midway, complete it, and compare its first/latest outcomes with the unchanged source. Reviews retain possible earlier exposure rather than implying independent mastery. The [follow-up contract](targeted-follow-up.md) records the finite vocabulary pool and saved-lineage boundaries.

## Inspect variation

Record the format, seed and index under “Generation controls.” Reusing those coordinates and revisions reproduces the exercise presentation; the API's persisted UUID is a separate identity. Change the index or use “Next exercise” to sample variation. The finite pools contain 99 sentences for present/order and 92 for articles, while E007/E008 each cycle through three selected senses and E011 cycles through four. Matching reuses those four E011 senses with independently shuffled columns; occasional correctly aligned rows are possible. Long-term variety needs more reviewed content.

For the current inventory, run `./gradlew :engine:exportPilotReview` and read `engine/build/review/pilot-6.md`. Retained revision five is available with `:engine:exportPilotFiveReview` at `engine/build/review/pilot-5.md`; revision four is available with `:engine:exportPilotFourReview` at `engine/build/review/pilot-4.md`; revision three with `:engine:exportPilotThreeReview` at `engine/build/review/pilot-3.md`.

## Record feedback

Capture the coordinates, answer and expected result when something seems wrong. Include the application version and source commit from the navigation footer; its link opens the changelog for that build. For guided practice, include the session URL, part number and which support was opened. Separate:

- whether the exercise isolates the intended grammar or vocabulary target;
- whether explanations and feedback are understandable;
- whether Danish meaning, naturalness or accepted variants need correction;
- whether the interaction or variation becomes repetitive.

Use this feedback to choose the next engine work. Danish teaching review and learner trials remain separate from automated software checks; these attempts are practice evidence, with no mastery judgment.

## Initial user review — 7 November 2026

The user tried the playground and described it as simple, promising, and a start in a good direction. They reported nothing they disliked or disagreed with. This is positive feedback on the version tried; no specific defect or requested correction was raised.

Keep the current direction as the basis for the next discussion. The feedback does not establish Danish teaching approval, learning effectiveness or an exhaustive review of every case. In the subsequent message, the user authorized the [recommended extension](exercise-extension-proposal.md): typed finite-form recall followed by guided single-target repair. Q21 is resolved for this iteration; BL-034 tracks delivery.
