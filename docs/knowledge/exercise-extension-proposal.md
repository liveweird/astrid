# Exercise Extension Proposal

## Status and boundary

**7 November 2026 — User-authorized iteration, implemented and verified.** Following positive playground feedback, the user selected typed finite-form recall followed by guided single-word repair. The contract below records implementation decisions for that work; vocabulary assessment remains a proposal. The existing [beginner slice](beginner-slice-spec.md) supplies 23 lexical entries and G001–G006. Only G004/G005/G006 are directly assessed; G001/G002/G003 support those tasks.

Implement typed finite-form recall first and bounded error repair second. Vocabulary recognition/recall remains a separately reviewed target expansion. These options address the [product requirements](product-direction.md) using existing engine and interface primitives. They establish neither teaching approval nor learning effectiveness.

## Three bounded options

| Priority | Proposed task and original seed-only example | Boundary |
| --- | --- | --- |
| 1 | **Finite-form supply:** “Type the finite present form of læse: `Vi ___ et brev.`” Expected: `læser`. | G004 with the named verb introduced; vocabulary, clause order and article supplied. Changes selection into recall, but belongs to the supply operation already used for articles. |
| 2 | **Single-target repair:** “This present statement contains one verb-form error. Keep the same verb; type its replacement: `Hun læse en avis.`” Expected: `læser`. Article variant: “Replace the incorrect unstressed article: `Vi har en brev.`” Expected: `et`. | Assess G004 or G006 separately. Collect one replacement word, initially avoiding whole-sentence grading. Generate a licensed canonical sentence, then present one controlled corruption; do not license the corrupted sentence as valid content. |
| 3 | **Meaning recognition, then headword recall:** Show `avis`; choose **book / newspaper / letter (written message)**. Reverse task: “Type the Danish singular headword introduced for ‘newspaper’, without its article.” Expected: `avis`. | Assess L015’s selected meaning in different reading/writing directions. Use the existing readable-object trio L014/L015/L018: `bog/avis/brev`. |

The third option needs a lexical target contract before implementation. The [seed](danish-vocabulary-seed.md) provides curated English glosses and semantic allowlists, but these remain draft teaching content without Danish-teacher approval.

Repair introduces recognition of an existing error, but naming its category remains guided repair, not unrestricted diagnosis. Selection, supply, order, repair, comprehension and composition are different operations. Shuffling, decorative changes or changing input widgets alone should not inflate the format count. These options add neither passage comprehension nor composition.

The readable-object trio keeps alternatives compatible with reading. Dog, car and house are not interchangeable examples of that semantic neighborhood. Standalone noun tasks also avoid an article cue eliminating `brev` before meaning is considered.

## Support and target leakage

Current [generation](../../engine/src/main/kotlin/app/astrid/engine/PilotEngine.kt) always supplies English pronoun meanings and, where present, noun meanings through `contextFor`. Its introductions also reveal exact present forms and article–noun–gloss mappings. This supports grammar isolation but cannot be reused unchanged for vocabulary assessment.

| Task | Ordinary support | Answer-revealing support |
| --- | --- | --- |
| Finite recall/repair | Named lemma, pronoun/noun meanings, correct surrounding structure | Exact finite form: optional recorded reveal |
| Article repair | Noun meaning and remaining sentence | Correct article–noun phrase: optional recorded reveal |
| Meaning recognition | Danish headword and selected-sense scope | Target English gloss: absent from ordinary context |
| Headword recall | English cue and requested response form | Danish headword/article phrase: absent from ordinary context |

Review instructions, context, examples, labels, introductions and hints together. Choices necessarily contain the recognition answer; surrounding support must not identify it. Record optional reveals and retries as supported practice using established behavior. “No support opened” does not establish independent mastery.

## Target identity and grading

Draft lexical targets should distinguish **L entry, selected sense, assessed meaning/form facet, and reading recognition versus writing recall**. G IDs continue to identify grammar targets. This is an authoring distinction, not a selected API or universal ontology.

G006 already depends on entry-specific gender. One correct article supplies narrow evidence about that noun and task, not all noun meanings, spelling or general gender mastery.

Keep primary-target satisfaction, linguistic acceptability and response constraints distinct:

- For finite recall, `læser` succeeds; `læse` has the known infinitive issue. `har` forms a licensed grammatical sentence but misses the named-verb instruction. For E004/E005 only, recognize a different finite verb as acceptable but off target only when its substitution yields an exact existing licensed sentence with the same construction, subject and complement (`NAMED_VERB_REQUIRED`). Other forms remain unknown. Legacy E001 grading remains unchanged.
- For article repair, `et` succeeds; `en` retains the mismatch. `ét` can be grammatical while missing the unstressed-article instruction.
- For headword recall, `bog` is Danish but misses the requested meaning. `en avis` reveals the noun while violating the noun-only instruction. Decide how to report those facets before implementation; do not describe the response as evidence that the noun is unknown.
- An unrecognized synonym is not thereby invalid Danish. “Recall the introduced headword” is narrower than open translation; exact matching must not silently grade the latter.

Preserve raw input, NFC normalization and Danish letter distinctions. No fuzzy or AI grading is proposed.

## Interface and input

Reuse existing text, choice and ordering primitives. Repair needs clear replacement-word wording; meaning choices need accurate language markup because [ExercisePrompt](../../web/src/components/ExercisePrompt.tsx) currently marks choices as Danish. English selections also require a deliberate interpretation of the feedback label “Acceptable Danish.”

Review the “Good work” treatment in [FeedbackPanel](../../web/src/components/FeedbackPanel.tsx) if a future task permits `met` alongside a known non-target issue. This is conditional design work, not a confirmed current defect.

Typed Danish words must support accessible entry of `æ`, `ø` and `å`. Character-entry assistance does not itself reveal a linguistic answer and should not automatically count as a hint. Provide keyboard-accessible character buttons preserving selection/caret and returning focus to the input. Respect disabled input states; insertion alone does not set `hintUsed`. The [delivery record](recall-repair-delivery.md) records observed verification.

## Later transformations

Question transformations require reviewed activation of G018/C010 even when all words already exist. Negation requires G017/C009, a reviewed neutral scope, and the new word `ikke`, outside the seed. Current G005 and catalogue references do not activate these variants. See [foundation coverage](foundation-coverage.md).

## Revision decision before release

Preserve all **335 archived `pilot-1/generator-1` golden cases and provenance unchanged**; add separate fixtures for a new revision. Prompt, support or sampling changes must not silently alter old generation.

**Working implementation decision:** publish `pilot-2/generator-2/evaluation-2`, retaining exact `pilot-1/generator-1/evaluation-1` generation and grading alongside it. Reject mixed revision pairs and new formats on the legacy pair. Preserve legacy request defaults, IDs, fingerprints, archived fixtures and immutable saved results. New IDs include the revision pair; existing formats retain their presentation and grading on the new pair.

Add E004 “Supply the present form” (G004, 99 eligible sentences), E005 “Repair the verb form” (G004, 99), and E006 “Repair the indefinite article” (G006, 92). E005 substitutes only the named verb's infinitive; E006 swaps only `en`/`et`. Corruptions are presentation data, never licensed canonical content. New nullable prompt fields `inputLabel` and `replacementWord` describe the input and displayed erroneous token; absent fields retain legacy rendering. Fingerprints append these fields only for the new pair, preserving legacy hashes byte for byte.

No database migration is required. Saved attempt payloads remain unchanged and replay before revision checks. **Rollback boundary:** the old strict JSON reader cannot read new prompt fields. After new exercises exist, rollback requires a compatible DTO reader (and explicit retirement if new generation is removed); a bare old image does not preserve new exercise reads. Do not delete or rewrite stored evidence.

## Future work and activation decisions

| Owner and exclusive scope | Dependency and acceptance |
| --- | --- |
| Coordinator/content author: relevant knowledge documents and backlog | Record selected scope, target/support matrix and success, off-target and unknown examples; coordinate delivery. |
| Engine developer: `engine/` | After authoring review: deterministic eligibility, bounded grading and controlled corruption; verify eligible items, normalization and leakage. |
| Server developer: `server/` | After contract/revision agreement: preserve session ownership, private grading data, replay and retired-instance conflicts. |
| Web developer: `web/` | After contract agreement, parallel with server: reuse controls; verify language markup, keyboard/input access and support tracking. |
| Independent reviewer/integration owner: review and `e2e/` | After implementation: inspect examples and compatibility evidence; exercise each new operation and failure path. |

Priority is user-confirmed and the bounded revision policy is selected above. Appropriate Danish teaching review remains outstanding: all new examples retain draft status, and agent review does not confer teaching approval. Vocabulary additionally needs agreement on lexical target identity, glosses and treatment of extra articles or alternative words. Learning outcomes and delayed-check criteria remain open; no numerical mastery thresholds, scheduler, media, accounts or mobile packaging are proposed.
