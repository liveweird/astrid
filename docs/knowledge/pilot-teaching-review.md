# Pilot Danish Teaching Review Handoff

## Status and boundary

This is a **proposed review process**, not a user-approved rubric, an arranged specialist
engagement, or evidence that a specialist has reviewed or approved the content. It covers the
private beginner preview at app **0.14.0**, content `pilot-12`, generator `generator-12`
and evaluation `evaluation-12`. Its language state remains `draft-not-teacher-reviewed`.
See the [course slice](module-one-course-slice.md#s09-names-homes-and-languages).

The current bounded inventory is 32 lexical records, 109 licensed catalogue clauses and
twenty-five standalone exercise formats. Nine guided profiles cover first steps through
names, homes and languages. The proposed review covers their teaching and visible feedback.

Software and source checks cover identifiers, deterministic generation, references, frozen
compatibility and bounded inventories. They cannot decide whether Danish is natural, explanations
are suitable for complete beginners, feedback is helpful, or learners understand and transfer the
material. See the [beginner slice](beginner-slice-spec.md#initial-draft-review-and-checks),
[beginner outcome reference](beginner-outcome-reference.md#what-the-pilot-currently-demonstrates)
and [first-step activation contract](module-one-activation.md).

## Prepare the current review packet

- From the repository root, run `./gradlew :engine:exportPilotReview`. The current output is
  `engine/build/review/pilot-12.md`. Confirm content `pilot-12`, generator
  `generator-12` and review state `draft-not-teacher-reviewed`. Record the exact source commit,
  application version and command result with the review.
- Give the reviewer that export together with the [vocabulary seed](danish-vocabulary-seed.md),
  [Module 1 language pack](module-one-language-pack.md), [beginner-slice specification](beginner-slice-spec.md),
  [first-step activation contract](module-one-activation.md), [variation audit](pilot-variation-audit.md),
  [first-step finite inventory](practice-variation-audit.md#first-step-finite-inventory--10-november-2026)
  and [live review guide](pilot-review-guide.md). The export is an inventory and reproducible
  source-review surface; the contracts explain intended senses, teaching, scope and behavior.
- Review the matching private preview as part of the packet. The export includes the complete
  lexical and clause inventories, deterministic prompt samples, the fuller format-specific
  inventories listed below, and the mandatory profile teaching. Inspect the matching live app
  for displayed instructions, evaluation results, feedback, saved exposure, retries and summaries.
- Capture every finding with its exact format, revision pair, seed/index or practice-session URL
  and position, visible context and answer. For a UI finding also record which introduction,
  lesson or hint had been opened, the displayed feedback, latest saved result, `canAdvance`
  behavior and summary state.

### What the current export covers

| Review surface | Current exported coverage |
| --- | --- |
| Lexicon and clauses | All 32 lexical records and all 109 licensed catalogue clauses |
| Reproducible examples | Two seed-47 prompt summaries for each E001–E025 |
| E009 writing | 99 complete cue/reference/hint units |
| E010 reading | 24 complete passages, questions, choice sets and private answers |
| E011 word choice | All four selected meanings, choices and answers |
| E012/E013 tiles | 99 units per format, including licensed orders and distractors |
| E014 matching | Four boards and their private correspondences |
| E015 two gaps | 92 units with displayed slots and private expected choices |
| First-step profiles | Nine full required introductions and pinned policy metadata. Review each profile's feedback live. |
| E016–E019 | All 30 standalone authored tasks: 7 E016, 7 E017, 8 E018 and 8 E019 |
| E020/E021 | Four inanimate-reference tasks, two selection and two recall, including antecedent/sense/context metadata; inspect per-item lessons and hints live |
| E022/E023 | Sixteen named-present tasks, including four possession and six personal-statement tasks, with scoped form targets and exact teaching exposure |
| E024/E025 | Six subject-first ordering tasks covering whole constituents and individual words |

For standalone first-step traversal at fixed seed `47`, use indices `0..6` for each
E016/E017 with G003, and `0..7` for each E018/E019 with G004. Each range traverses its
complete finite task cycle; record the resulting authored task and visible teaching unit.
Profile traversal is separate and still requires the sequence coverage below.

The three pair-eight profiles each plan six activities. Across seeds they reach 24 distinct authored task
identities: eight in one-person practice, six in group practice and ten in `er` practice. Six of
the thirty standalone identities are not selected by those plans. Review both surfaces: all
thirty tasks remain independently generatable, while the profile review must cover every one of
the 24 reachable identities and each complete ordered sequence (four one-person sequences, one
group sequence and four `er` sequences). These counts identify authored operations and contexts,
not thirty different Danish meanings or evidence of unseen transfer.

S04 adds four authored operation/context identities: book and letter selection, then separate
book and letter recall. Traverse indices `0..1` for each E020/E021 with G038 at seed 47,
and separately inspect each profile ordering. These are two introduced forms with repeated
reference contexts, not four new meanings or unseen transfer.

At seed 47, indices `0..7` traverse each pair-twelve E022/E023 cycle and `0..2` traverse
each E024/E025 cycle. In the guided sequence, check the six regular present items in S05,
the six ordering items in S06, the four possession items in S07 and the six personal-statement
items in S09. Check the separate names, residence and language examples and diagnoses.

## Specialist checklist

For each lexical entry, judge the selected Danish form and sense, English gloss, gender/article
where applicable, usage constraint and suitability for an English-speaking complete beginner.
Flag misleading exclusions, ambiguous reference—especially pronouns—and any introduction or
gloss likely to encourage the wrong generalization.

For each of the 109 clauses, judge grammaticality, idiomatic naturalness in the stated everyday
sense, semantic plausibility and whether the context adequately establishes pronoun reference,
fixed identifying `Det er …`, names and bare occupation complements. Check that allowlists stay
suitably narrow without implying excluded Danish is ungrammatical.

For each format, inspect instruction, context, prompt, cue/gloss, choices or blocks, required or
optional introduction, lesson, hint, expected response, accepted alternatives and visible
feedback as one teaching unit. Review correct, known-wrong, off-target, response-shape and
unsupported answers rather than only the answer key. In particular:

- **E001–E003:** finite-present recognition, unstressed indefinite article, and requested
  subject-first order must remain isolated without teaching the infinitive, numeral `en/én` or
  `et/ét`, or valid Danish fronting as globally wrong.
- **E004–E006:** named-verb one-word recall, single-word verb repair and article repair must keep
  their narrow response and licensed-alternative diagnoses.
- **E007–E008:** review the selected meaning relationships, noun-only recall, optional answer
  disclosure and separate headword/response-constraint feedback.
- **E009–E010:** inspect every writing cue/reference/hint and every passage/question/distractor;
  check pronoun number/reference, meaning preservation, fronted alternatives and participant/action
  binding. Supported reading or writing is not a proficiency judgment.
- **E011–E014:** review the four selected senses, article+noun bundles, optional mappings, tile
  references, licensed orders, distractors and per-pair matching feedback. Incomplete banks and
  finite/infinitive selection must not collapse meaning, grammar and response shape.
- **E015:** review all verb/article combinations and per-slot feedback, including one correct and
  one wrong or missing slot. Primary verb and supporting article outcomes must remain separate.
- **E016–E025:** apply the Module 1 checklist below in addition to reviewing the standalone entry.

Across all formats, ask whether prerequisites are introduced in a sensible order, whether answer
disclosure is intentional and recorded as supported practice, whether English meanings and hints
are proportionate, and whether feedback separates **target not met**, **known form/shape issue**,
**linguistically acceptable**, and **unknown**. A revealed answer, passing automated check or
completed activity is not independent performance or mastery.

## Module 1: First steps review checklist

Review each of the eight profiles both by direct entry and in the recommended sequence. For every
profile and reachable task:

- Confirm the complete required introduction appears and is saved before assessment. Check the
  full foundation teaching: written word versus complete statement, `æ/ø/å` entry, roles in
  `Jeg er Anna.`, subject/pronoun terminology, sentence meaning and standalone capitalization.
  For groups also check `vi/I/de/du`, plural reference and supplied `sover`; for `er` also check
  noun/article bundles, `at være → er`, fixed `det`, bare `lærer`, names and what is not assessed.
- Read the actual learner-facing instruction, English situation, cue, Danish frame, gloss,
  choices/input label, lesson and hint together. Check that speaker, addressee or referent is
  explicit; E016 selection and E017 recall use deliberately changed English situations; capital
  `I` is taught without misdiagnosing lowercase `i`; and `de`/`De` feedback does not invent
  formal-address meaning or a wrong person.
- For E018/E019, inspect book, letter, four teacher and two identity frames across standalone
  tasks. In the profiles, cover both book/letter selection parity, both identity selections,
  book/letter recall and all four rotating teacher recalls. Confirm only `er` is assessed and the
  supplied article, noun, subject, name or profession is not silently scored. Assess whether the
  intentional book/letter context overlap between E018 selection and E019 recall, followed by the
  identity/teacher context and operation transition, forms a clear and useful teaching sequence.
- For E020/E021, inspect the same explicit individual book/letter antecedent, supplied article,
  meaning and gender, requested subject reference and standalone lowercase form. Keep referential
  `det` separate from identifying `Det er …`, and exclude object/plural/demonstrative/dummy
  interpretations. Check direct-entry foundation teaching without implying reference-form practice
  proves complete-clause analysis, gender recall or subject/object discrimination. Both sense
  and visible-form exposure must be recorded without claiming sense knowledge from spelling.
- For E022/E023, check the named verb, supplied context and lowercase finite form. Check the
  regular pattern in S05 and the stored possession form in S07. For E024/E025, check the
  transition from constituents to words and the separation of requested placement, missing
  words and linguistic judgment.
- For S08, review the printed-magazine meaning and the three familiar reading-material senses.
  Check each complete article+noun choice and the meaning-selection label. Review the
  difference between an exposed choice and a confirmed cue-to-meaning mapping.
- Exercise correct, related-wrong, casing-only, multiword, blank and unsupported responses.
  Inspect the actual UI feedback and completed sentence, not only export metadata. Unknown latest
  answers must keep support/retry available and block Continue; a revision-defined known
  form/shape issue may continue without becoming a success. A newer unknown must supersede an
  earlier eligible result.
- On the summary, check each scope and operation separately, with first and latest outcomes,
  earlier teaching/support exposure and the planned activity count. A completed session records
  its eligible activities after required teaching.
- Check **Repeat this session** and the suggested **Next** chooser boundary. The next profile must
  not be created silently. First-step profiles have no target-selection review and must not fall
  back to an older follow-up planner; leaving or cancelling must retain the completed run.

The profile export records full required introductions, exposure keys, scopes and completion
policy IDs, and the authored-task table records contexts and teaching metadata. The live UI/API
state is still required to review saved teaching enforcement, actual feedback, first/latest
summary prose, continuation eligibility, recovery and repeat/next behavior.

## Record issues and outcomes

Use stable IDs such as `DTR-001`. Each record should include application/source version, export
revisions, lexical/clause/task/format IDs, seed/index or session position, visible reproducer,
affected field, severity (`blocking`, `substantive`, or `editorial`), teaching rationale, proposed
correction, reviewer, date and status (`open`, `needs-discussion`, `accepted-for-correction`,
`rejected`, or `verified`). Severity organizes follow-up; it is not a learner threshold.

Approval must be explicit and scoped. Record separately whether the reviewer accepts the lexical
inventory, clause inventory, each format's teaching treatment, each first-step profile's required
teaching and reachable tasks, and any exceptions. Silence, corrected software, source checking or
“no engineering defect” is not teaching approval. Specialist review also does not establish CEFR
level, comprehensibility, transfer, retention or efficacy; those require appropriately designed
learner evidence.

## Corrections, revisions and retained history

Published instances must remain readable, replayable and regradable under their retained revision
semantics. Never delete a snapshot, plan, review export or evaluator branch that stored data still
references, and never edit, regenerate or reformat a frozen fixture or provenance file. In
particular, the original 335 `pilot-1/generator-1` outputs and provenance remain immutable, as do
every later published golden/provenance file. This compatibility requirement does not define how
long user data must be retained; user-data retention policy is outside this handoff.

Apply an accepted content, prompt, teaching, support or sampling correction only in a new authored
content/generator or plan revision as applicable. A grading change also needs a new evaluation
revision; the old evaluator remains executable for its published pair. Preserve original attempt
responses and replay semantics, add new evidence rather than altering old fixtures, regenerate the
new review packet, link issues to new reproducers, and obtain explicit specialist verification
before changing review state. Follow the binding [compatibility policy](../../.claude/docs/compatibility.md)
and the relevant implementation contract.

## Historical extension packets and boundaries

Retained exports are historical review surfaces, not the current packet and not evidence of
approval. Use `exportPilotEightReview` (`pilot-8-retained.md`), `exportPilotSevenReview`, `exportPilotSixReview`, `exportPilotFiveReview`,
`exportPilotFourReview`, `exportPilotThreeReview`, `exportPilotTwoReview` and
`exportLegacyPilotReview` for `pilot-7.md` through `pilot-1.md` when reviewing what a published
revision actually contained. Do not infer later fields or tasks into those packets.

- Pair four introduced E009 sentence writing over the retained 99-clause pool and E010 reading
  over 24 three-fact units. Its review boundary includes complete cues, references, hints,
  passages, questions, distractors and bounded feedback.
- Pair five introduced E011 word choice and E012/E013 tiles, including the choice-only printed
  magazine sense, selected meanings, article+noun alternatives, tile references, licensed orders
  and distractors.
- Pair six introduced E014 four-pair bilingual matching; review individual correspondences,
  incomplete/duplicate interaction states and per-pair versus whole-board feedback.
- Pair seven introduced E015 present-verb/supporting-article gaps over 92 units; review each slot
  independently and the combined response. The separate guided-gap profile remains literally
  pinned to pair seven.
- Pair eight introduced E016–E019, ten additional C004 clauses, L025 and the three first-step
  plans described above. Older plans and follow-ups remain pinned to their published pairs.

Historical exports, compatibility archives and automated source review preserve evidence; none
of them changes `draft-not-teacher-reviewed` into specialist approval.
