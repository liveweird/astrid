# Pilot Danish Teaching Review Handoff

## Status and boundary

This is a **proposed review process**, not a user-approved rubric, an arranged specialist engagement, or evidence that the content is approved. It covers the existing private beginner pilot only. It does not activate corrections, expand the curriculum, set a pass threshold, make a CEFR commitment, or establish learning efficacy.

Software checks have already exercised IDs, deterministic generation, prerequisite references, and the bounded inventory. Those checks cannot decide whether the Danish is natural, whether the explanations are pedagogically appropriate, or whether learners understand and transfer the material. See the distinction recorded in the [beginner slice](beginner-slice-spec.md#initial-draft-review-and-checks) and [beginner outcome reference](beginner-outcome-reference.md#what-the-pilot-currently-demonstrates).

## Prepare the review packet

- From the repository root, run `./gradlew :engine:exportPilotReview`. Record the command result and identify the revisions printed at the top of `engine/build/review/pilot-5.md`; the current expected identifiers are content `pilot-5` and generator `generator-5`, with review state `draft-not-teacher-reviewed`.
- Give the specialist that generated report together with the [vocabulary seed and choice-only addition](danish-vocabulary-seed.md), [beginner-slice specification](beginner-slice-spec.md), and [variation audit](pilot-variation-audit.md). The export is the item-by-item review surface; the linked documents explain intended senses, constraints, targets, and observed distribution.
- Confirm all 24 lexical records and 99 canonical sentences. The export contains bounded prompt examples, including all four E011 senses and tile examples; it does **not** contain complete instructions, context, lessons, hints or feedback. Include access to the matching private preview and the [live review guide](pilot-review-guide.md). At fixed seed `20261107`, inspect indices 0–98 for E001/E003/E004/E005 and 0–91 for E002/E006 to traverse each grammar cycle. Inspect indices 0–2 for each of E007/E008 to cover their three selected vocabulary senses. Inspect E011 indices 0–3 for all four meanings and E012/E013 indices 0–98 for all tile propositions, including short two-word statements, finite/infinitive selection, incomplete banks and licensed fronting. Capture the visible teaching unit and relevant correct, off-target and unsupported-answer feedback with its revisions/coordinates. Do not approve uninspected prompt contexts from canonical sentences alone. Use the audit’s counts to notice imbalance, not as a teaching-balance target.

## Specialist checklist

For each lexical entry, judge the selected Danish form and sense, English gloss, gender/article where applicable, usage constraint, and suitability for an English-speaking complete beginner. Flag misleading exclusions, ambiguous reference—especially pronouns—and cases where an introduction or gloss would encourage the wrong generalization.

For each of the 99 sentences, judge grammaticality, idiomatic naturalness in the stated everyday sense, semantic plausibility, and whether the prompt supplies enough context for pronoun reference or `Det er …`. Check that the allowlists remain suitably narrow without implying that excluded Danish is ungrammatical.

For each format, inspect instructions, prompt, choices or blocks, introduction, hint, expected response, and feedback as one teaching unit:

- **E001:** Does it isolate finite-present recognition while keeping the lemma, meaning, and surrounding sentence supported?
- **E002:** Does it isolate the introduced noun’s unstressed indefinite article without confusing it with numeral `en/én` or `et/ét`?
- **E003:** Does it clearly request subject-first order while keeping the noun phrase intact and acknowledging grammatical fronting as acceptable Danish but off target?

Across all formats, ask whether G001–G003 support G004–G006 in a sensible prerequisite order; whether answer disclosure is intentional, proportionate, optional and recorded as supported practice; whether examples, English meanings, and hints are accurate and proportionate; and whether feedback distinguishes **target not met** from **linguistically acceptable**. Current lessons and hints intentionally reveal answers; that alone is not a defect or evidence of independent performance. Use the worked off-target cases in the [slice specification](beginner-slice-spec.md#concrete-worked-cases) as explicit review cases. Include the selected [recall/repair extension](exercise-extension-proposal.md): review named-verb constraints, single-word corruptions and the narrow licensed-alternative diagnosis. Include the [vocabulary contract](vocabulary-assessment.md): selected English senses, related choices, noun-only recall, optional disclosure, and separate headword/response-constraint feedback require review.

## Record issues and outcomes

Use stable IDs such as `DTR-001`. Each record should include export revisions, lexical/canonical/format IDs, exact seed and index when applicable, the visible reproducer, affected field, severity (`blocking`, `substantive`, or `editorial`), teaching rationale, proposed correction, reviewer, date, and status (`open`, `needs-discussion`, `accepted-for-correction`, `rejected`, or `verified`). Severity labels organize follow-up; they are not learner-performance thresholds.

Approval must be explicit and scoped: record separately whether the reviewer accepts the lexical inventory, sentence inventory, each format’s teaching treatment, and any exceptions. Silence, corrected software, or “no engineering defect” is not teaching approval.

Published snapshots and all **335 archived `pilot-1/generator-1` outputs and their provenance remain immutable**. Apply accepted corrections to new authored revisions and separate fixtures; preserve original stored attempt responses and replay them before revision checks. Content, prompt, support or sampling changes require appropriate new content/generator revisions. Grading changes also require an evaluation revision and an explicit decision to retire affected old instances or retain their original evaluator; a version number alone is insufficient. Follow the [revision policy](exercise-extension-proposal.md#revision-decision-before-release), regenerate the review packet, link issues to new reproducers, and obtain specialist verification before changing review status. Learning comprehensibility and transfer still require learner trials; specialist approval alone does not establish efficacy.


## Writing and reading extension review

The selected standalone extension adds E009 sentence writing over the same 99 licensed statements and E010 short-text comprehension over 24 three-fact units. Inspect the complete unit: English cue/context, optional language reference, hints, expected sentence and bounded known/unknown/meaning/shape feedback for writing; every passage, question, choice, correct relation and distractor for reading. Check English cue naturalness, pronoun number/reference, lexical senses, fair grammar isolation and accepted fronted alternatives. Three nouns each occur once in reading; participant/action binding must identify exactly one stated answer. Use the [implementation contract](writing-reading-contract.md) for worked cases and the generator's fixed seed/index coordinates. The pilot-four export and live UI support review; earlier published review snapshots remain available under their retained export tasks. Review status remains draft.

## Word-choice and tile extension review

The [pair-five contract](word-choice-tiles-contract.md) adds the choice-only printed-magazine sense and individual-word translation with all/spare tiles. Review the four cues and their related article+noun alternatives for distinct selected senses. Examine all optional mappings as recorded answer-revealing support, and tile references without preassembled sentence order. Check separate meaning, grammar and shape outcomes, especially incomplete selections and E013 finite fronting. The 99 sentence allowlists remain unchanged. Use `exportPilotFourReview` for the retained pre-extension packet; neither packet establishes teaching approval.
