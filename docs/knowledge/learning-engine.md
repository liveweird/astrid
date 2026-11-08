# Learning Engine Proposals

The following design ideas support the [confirmed product requirements](product-direction.md). The broader-course ideas remain proposals; the authorized pilot uses the architecture, deterministic generation and bounded grading choices in [implementation decisions](implementation-decisions.md).

The [beginner slice specification](beginner-slice-spec.md) now makes the first three exercise formats, generation boundaries, normalization, feedback, and attempt handling concrete enough to review. The [private playground delivery](playground-delivery.md) implements that bounded subset. The broader ideas below remain design direction rather than a claim of full-course behavior.

## Learning targets and progression

Give each exercise an explicit target, its prerequisites, and the knowledge assumed by its surrounding material. A tense exercise should use familiar vocabulary and enough context to establish which tense is appropriate.

Use focused exercises to isolate a skill, then combined exercises to practise several learned skills together. Similar answer choices should require the intended knowledge while preserving a fair, unambiguous task.

A proposed learning loop is: brief explanation, guided practice, independent recall, explanatory feedback, and later review. Track reading recognition and written production separately rather than treating all correct responses as equivalent evidence.

## Structured sentence generation

Represent a sentence's intended meaning, grammatical components, required word forms, ordering constraints, and prerequisite knowledge. Include phrases and clauses as building blocks.

Vary subjects, objects, time references, modifiers, or sentence types only where the construction permits. Grammar alone does not ensure natural meaning: vocabulary also needs usage information, compatible constructions, and semantic restrictions.

Begin with reviewed examples and constrained sentence families. Expand their range deliberately. AI-assisted drafting is a possibility; unchecked generation and grading have not been accepted as the basis of the engine.

## Exercise candidates

| Format | Intended practice |
| --- | --- |
| Choose between related forms | Distinguish tense, number, or another grammatical feature |
| Complete a gap | Recall the required word or form |
| Order sentence components | Apply word-order rules |
| Transform a sentence | Change tense, number, or sentence type |
| Correct an error | Identify and repair a grammatical problem |
| Read and answer | Interpret a short passage |
| Write under clear constraints | Produce a sentence using target knowledge |

Choose formats for their learning purpose and suitability to the learner's stage. Presentational variety can supplement differences in the mental task.

## Variability and difficulty

Repeat skills across changing examples. Vary vocabulary, context, structure, task direction, and available support while controlling prerequisites and difficulty. Avoid interpreting memorization of an identical prompt as mastery of a general rule.

For one tense target, a possible sequence is form selection, typed completion, sentence transformation, error correction, and constrained writing. Vocabulary and context can change across that sequence.

## Evaluation and feedback

Define valid alternative answers and policies for spelling, punctuation, capitalization, Danish letters, and partial correctness. Prompts must provide sufficient context for fair evaluation.

Feedback should explain the relevant error and support another attempt. Record evidence about vocabulary, form choice, and word order separately where possible, but retain uncertainty when an answer does not reveal the cause of a mistake.

## Validation and architecture questions

An earlier suggested trial size was 40–60 vocabulary entries, three grammar concepts, and four or five exercise formats. These numbers are illustrative, not agreed scope, and do not replace the broader language map.

The current draft instead defines a deliberately small, connected test space: six foundational skill records, three construction families, 23 vocabulary entries, and three exercise formats. Its allowlists yield 99 abstract sentence combinations. This establishes a bounded engine test fixture rather than sufficient variety for a full course.

The authorized pilot has completed implementation and software checks. For future expansion, manually review a connected subset containing sentence structures, vocabulary, exercise variants, valid answers, likely mistakes, and feedback before activating it. Include unseen examples and delayed practice when evaluating learners; those learner studies remain outstanding.

Keep learning rules and content independent of web screens, account for phone input, and isolate Danish-specific rules so future languages can be added. The pilot uses typed content, persisted PostgreSQL attempts and explicit grading; storage beyond that subset, mobile distribution, scheduling and broader generation/grading remain open.

## Vocabulary assessment implementation

The user-authorized [vocabulary contract](vocabulary-assessment.md) adds meaning recognition and introduced-headword recall for three existing senses, bringing the catalog to eight entries. Lexical targets are distinct from grammar skills, and response constraints are assessed separately from identifying a headword. The initial three-format description above is historical; [delivery evidence](vocabulary-delivery.md) tracks the current iteration. Short practice sessions were subsequently authorized as BL-009; broader progression remains a proposal.

## Short practice sessions: delivered milestone

**7 November 2026 — Initially proposed, subsequently user-authorized (“go with that”).** The engine now supports eight exercise entries across grammar and vocabulary. Next, connect those capabilities into a bounded practice flow using the existing content and graders.

Propose a short session (around eight items as an illustrative starting point): a clear focus, an optional introduction to its words/rule, a deterministic sequence mixing suitable recognition, recall, ordering and repair operations, explanatory feedback/retries, and an end summary. Select items by target coverage and prerequisites rather than mechanically showing every format once. Keep manual engine controls available for development inspection.

Report results separately by grammar target and lexical sense/direction. Distinguish first submissions, retries, opened lessons/hints, unknown diagnoses and missed response constraints. A headword identified with an extra article must not become an unqualified success. Earlier session items can reveal answers for later items; retain that exposure as session context separately from the existing per-exercise hint flag. Describe these as practice outcomes, not independent mastery or long-term retention.

Retain the planned item order, exercise revisions and attempt references so refreshing/resuming within a still-valid development session does not silently generate a new sequence, duplicate grading, or lose exact uncertain-attempt replay. Define expired-session recovery explicitly; cross-device accounts are outside this milestone. Avoid adjacent repetition where the finite inventory permits it, while acknowledging the three-word vocabulary pool.

Before implementation, specify session size/focus choices, prerequisite/support sequencing, completion rules, persistence/resume and summary aggregation. Acceptance should cover reproducible ordering, target coverage, exposure/support evidence, partial/unknown outcomes, reload and lost-response recovery, accessible keyboard progress and summary, and unchanged standalone exercises. BL-009 owns this authorized implementation; BL-035 remains user review of the expanded playground. New content, later grammar, adaptive scheduling, streaks and mastery thresholds are separate decisions.

The [implementation contract](practice-sessions.md) now specifies the eight-item `practice-1` plan, optional support, frozen first-attempt context, per-target first/latest summaries and owner-bound browser recovery. These are implementation defaults within the authorized current-content scope; the [delivery record](practice-delivery.md) records passing source/browser gates and the upgraded private preview.

## Targeted follow-up practice

**7 November 2026 — User-authorized:** “OK, proceed” selects the recommended learner-controlled follow-up run. BL-039 tracks implementation; BL-035 remains user review, and broader progression and scheduling remain BL-011. Selection, size and persistence details are implementation defaults recorded in the contract below.

Let the learner select existing grammar or vocabulary targets from the summary and start a new, explicitly labeled review session. For example, article practice could reuse eligible article supply/repair formats with varied licensed sentences. Keep recognition and headword production distinct. Support, response constraints, unknown diagnoses and possible earlier exposure remain visible. No automatic suggestions are included; any future outcome-based recommendation policy belongs to BL-011.

Reuse current content and graders, preserve the source run and its frozen results, and persist the new run's selected targets, lineage, revisions and plan for reliable resume/replay. Earlier source-run teaching and answers are possible exposure, so review answers must not be presented as independent knowledge or mastery. The contract defines exposure, selection/prerequisites, run size and finite-pool repetition. This milestone adds no new curriculum, accounts, automatic schedule or proficiency threshold.

The [follow-up contract](targeted-follow-up.md) records the selected implementation defaults: one item per lexical target, additional distinct grammar sentences, no preselected targets or automatic suggestions, and frozen source-linked exposure.

## Topic-based practice

**7 November 2026 — User-authorized:** “OK, go for it” selects BL-041: choose an existing learning focus before starting a fresh guided run. Start with present verb forms: selection (E001), typed recall (E004) and guided repair (E005) already assess G004 in the standalone playground, while delivered `practice-1` and `follow-up-1` assess order/articles and selected vocabulary. G004 currently acts as supplied prerequisite knowledge in those runs.

The wider chooser proposal includes verb forms, sentence order, articles and vocabulary, reusing existing content, teaching support and graders. The authorized first scope is Mixed practice plus a complete verb-focused run and source-linked follow-up; other profiles are deferred. Preserve the original mixed plan and all saved runs; a new choice must not silently replace an active or uncertain run.

Before implementation, specify profile IDs/revisions, eligibility, size, ordering, introduction prerequisites, source review selection and verb-specific possible-exposure keys. Keep the grammar target distinct from how it was assessed: choosing a form, recalling it and repairing one should remain distinguishable in summaries rather than being treated as equivalent evidence. Retain first/latest results, support, response constraints, uncertainty and exact owner-bound recovery. Define these rules without changing published generation or grading.

This recommendation advances the engine with existing language material. New tenses, later grammar, broad vocabulary expansion, scheduling and mastery policy remain separate decisions. BL-035 user review and BL-022 Danish teaching review remain open; neither is presumed complete by this proposal.

The [topic-practice contract](topic-practice.md) specifies the selected first scope, six-item verb plan, assessment modes, feedback-aware exposure, version-two follow-ups and chooser continuity.


**Observed delivery:** BL-041 is complete at `0.3.0`, with two focus choices, six verb items and separate selection/recall/repair evidence. The [delivery record](topic-practice.md#observed-delivery--7-november-2026) includes review corrections, complete browser/CI verification and backed-up preview promotion. Further profiles remain proposals; user and Danish teaching review remain outstanding.


## Proposed next milestone: focused order and article practice

**7 November 2026 — Assistant recommendation, not yet user-selected:** After BL-041, complete focused practice for the remaining grammar already assessed by the pilot. BL-042 proposes a subject-first sentence-order profile using E003/G005 and an indefinite-article profile using E002/E006/G006. Together with present verbs, these would provide three explicit beginner grammar focuses without adding language material or changing published graders.

Keep supplying an article and repairing one distinguishable in new-run summaries and selected reviews. Define profile, assessment and follow-up revisions before implementation; preserve every old target key, stored plan, result and replay. Reuse shared profile/planning/review mechanisms while retaining supported-revision validation and chooser cancellation/owner continuity. Run size, sampling, exact keys and implementation architecture remain design choices, not confirmed requirements.

BL-035 user feedback on the current chooser, pacing and operation summaries should inform this milestone; it is not presumed complete or an automatic implementation blocker. BL-022 teaching review remains outstanding. Vocabulary-only practice should be considered with the finite three-sense assessment pool visible, rather than extending runs through repeated cues. A subsequent new exercise family could use constrained sentence transformations, but its grammar coverage, teaching examples and grading policy require a separate design and user selection. This recommendation activates neither new curriculum nor scheduling, accounts, mobile packaging or media.

Proposed completion: both new grammar focuses can create/resume runs, report their intended operations, start varied source-linked reviews and pass compatibility/recovery/accessibility checks. The current application remains `0.3.0` until implementation is selected and delivered.


### Correction to the next-step recommendation

**7 November 2026 — Verified after the user's “Isn't that done already?”:** E003 sentence ordering, E002 article supply and E006 article repair already exist. Mixed practice includes ordering and article repair; source-linked reviews already offer G005 ordering and G006 alternating article supply/repair. The previous recommendation overstated the missing capability. Its actual incremental scope is fresh-run chooser entries for order/articles and operation-separated article evidence. BL-042 remains optional, unprioritized polish, not a recommended major milestone or authorized implementation.

**Revised assistant proposal:** For a substantive next engine capability, use BL-006 to design a constrained sentence-transformation family, with statement-to-yes/no-question transformation as an initial candidate. Define the required active grammar, sentence structure, teaching examples, permitted transformation, valid alternatives and grading/exposure rules before implementing or publishing new revisions. This is a proposed design direction only; the user has not selected that exercise family or authorized its implementation. BL-035 learner feedback and BL-022 teaching review remain open.


## New exercise families: current recommendation

**7 November 2026 — User interest, implementation not selected:** The user asked to consider more exercise types. The [family expansion proposal](exercise-family-expansion.md) recommends constrained full-sentence writing and short reading comprehension first, then error diagnosis/repair. These explore new operations using existing language material where possible. Question transformations remain a later candidate requiring reviewed grammar activation; this supersedes the earlier question-first recommendation. BL-006 owns the proposed specifications, with grading and validation under BL-007/BL-008. Exact targets, formats, revisions and implementation scope remain open.


**Subsequent user authorization:** “OK, start with these” selects constrained sentence writing and short reading comprehension for standalone implementation under BL-043. The earlier unselected-implementation status is superseded for these two families only; later diagnosis/transformations and guided-practice integration remain separate decisions. The [family record](exercise-family-expansion.md) and working agreement capture scope.


**7 November 2026 — New families delivered:** BL-043 adds standalone E009 whole-sentence writing and E010 participant/action/object reading at app `0.4.0`, with `pilot-4/generator-4/evaluation-4`. Writing separately reports target, meaning, bounded language diagnosis and response shape; reading queries three actual generated facts. The [implementation and delivery record](writing-reading-contract.md#observed-delivery--7-november-2026) covers all 99 writing meanings, 24 reading units and retained revision evidence. This supersedes earlier current-version/catalog statements; existing guided plans remain pinned to revision three. User/teaching review and guided integration remain separate.

## Word choice and sentence tiles selected

**8 November 2026 — User-authorized:** BL-047 adds E011 English→Danish article+noun meaning choice, E012 all-word sentence tiles and E013 tiles with one spare infinitive. The [binding contract](word-choice-tiles-contract.md) separates target, meaning, response shape and language, preserves pair four before editing, and assigns learning pair five / app 0.5.0. Only the printed-magazine sense is new language material, confined to E011; existing sentence pools and guided plans remain unchanged. Observed delivery will be recorded in that contract after verification.


**8 November 2026 — User-authorized:** BL-048 adds E014 four-pair English–Danish matching,
using the existing E011 senses, with per-pair server feedback and whole-board checking. The
[matching contract](vocabulary-matching-contract.md) defines partial grading, editable supported
retries, opaque independent shuffles and learning pair six / app 0.6.0. Pair five is frozen first;
all historical grading and guided-plan revisions remain executable. No new vocabulary is selected.
