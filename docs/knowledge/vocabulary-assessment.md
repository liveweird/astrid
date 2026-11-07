# Vocabulary Assessment Contract

## Authorization and scope

**7 November 2026 — User-confirmed:** “Go for it” authorizes BL-036: Danish-to-English meaning recognition and English-to-Danish introduced-headword recall for existing L014 `bog`, L015 `avis`, and L018 `brev`. The technical choices below implement that scope. Mixed sessions remain later BL-009 work. The 23-entry inventory, 99 licensed sentences and draft teaching-review status are unchanged.

Use vocabulary-specific senses `L014-S01` (book), `L015-S01` (newspaper), and `L018-S01` (letter, a written message). Keep existing grammar glosses unchanged; the explicit vocabulary cue is `letter (written message)`. This is introduced-word recall, not unrestricted translation.

## Targets and prompts

| Format | Lexical task | Prompt and response |
| --- | --- | --- |
| E007 · Recognize the meaning | `meaning`, `da-en` | Danish headword; three seeded, shuffled English choices: `book`, `newspaper`, `letter (written message)` |
| E008 · Recall the Danish headword | `singular-headword`, `en-da` | English cue; type the introduced Danish singular headword without an article |

Grammar formats retain `targetSkillId`; lexical formats omit it and expose `lexicalTask {facet, direction}` in the catalog. Generated vocabulary exercises expose `lexicalTarget {entryId, senseId, facet, direction}`. Generation derives one target from a deterministic three-entry pool; callers do not select a lexical answer. Reject a supplied grammar target for a lexical format.

New optional prompt fields are `cue`, `cueLanguage` and `answerLanguage`. Recognition uses `da`/`en`; recall uses `en`/`da`, with input label “Danish singular headword.” Sentence fragments and punctuation are empty for vocabulary. Public target metadata contains no answer gloss/headword. Ordinary instructions/context/hints must not reveal the requested meaning or headword; the collapsed optional lesson may teach both and records sticky support. English choices have English language markup. Danish character assistance remains input support rather than a linguistic hint.

Canonical sentence evidence is absent for vocabulary. A separate private lexical inspector source contains entry/sense, headword, gloss and article; no fake sentence, construction or grammatical role is created.

## Evaluation

Retain primary target status and linguistic status; add lexical-only `linguisticScope` (`english-choice` or `danish-headword`) and `responseConstraintStatus` (`met` or `not-met`). All published grammar results omit these fields.

For a newspaper recall task:

| Response | Target / language / constraint | Meaning |
| --- | --- | --- |
| `avis` | met / acceptable / met | Requested headword |
| `bog` | not-met / acceptable / met | Another introduced Danish noun |
| `en avis` | met / acceptable / not-met | Headword identified; noun-only instruction missed |
| `et avis` | met / known-issue / not-met | Headword identified; article gender mismatch |
| `en bog` | not-met / acceptable / not-met | Different noun, with an extra article |
| Unrecognized word or synonym | not-met / unknown / met | Outside the bounded headword diagnosis; no invalid-Danish claim |

Recognize exact `en`/`et` plus any existing seed noun for bounded phrase diagnostics. Other phrases remain unknown and fail the one-word constraint. Recognition accepts the correct supplied meaning, distinguishes other supplied choices, and marks non-choice answers unknown with a failed choice constraint. Vocabulary results contain no completed sentence.

Keep raw input and NFC/whitespace-normalized input case-preserving. For these two formats only, compare using `lowercase(Locale.ROOT)`; `Avis` and `NEWSPAPER` can match. Preserve Danish letters and accents; no fuzzy matching or transliteration. Earlier formats retain their original case behavior.

English feedback identifies a recognized/unrecognized English choice. Recall identifies a recognized Danish headword. Display a failed response constraint separately; green “Good work” requires target met, acceptable language and the constraint met. A known word with an extra article needs explicit acknowledgment and corrective feedback.

## Revisions and persistence

Publish `pilot-3/generator-3/evaluation-3`; retain exact supported pairs 1/1/1 and 2/2/2 with their original format sets. Mixed pairs remain unsupported. Preserve old request defaults, IDs, prompts, fingerprint algorithms and immutable result replay. Legacy optional metadata remains absent. New fingerprints include lexical target, prompt and inspector metadata.

The forward migration only drops `NOT NULL` from `exercises.target_skill_id`; no old row is rewritten. Persist lexical metadata in the exercise snapshot and regenerate it for verification. A pre-change binary cannot safely read new prompt/target fields or nullable grammar targets. Rollback requires a compatible reader and explicit retirement policy; preserve stored evidence and the nullable schema.

## Acceptance and evidence

Preserve the original 335 pilot-1 fixtures/provenance. Before changing engine code, froze 952 pilot-2 generation/evaluation cases and 978 fingerprints from both published revision pairs using unchanged release binaries at source baseline `86a9e07`; new provenance identifies generated compatibility evidence, not linguistic approval. These fixtures must not be refreshed to fit new behavior.

Verify both vocabulary directions across all three senses, deterministic no-repeat target cycles, varied choice orders, exact metadata, normalization, grading distinctions and support leakage. Exercise real PostgreSQL migration/readback/replay, old-instance submissions, supported-pair boundaries, browser language markup, keyboard entry, hint evidence, accessibility, narrow enlarged layouts and uncertain-attempt recovery. Independent review and all repository gates precede delivery. Implementation is in progress.
