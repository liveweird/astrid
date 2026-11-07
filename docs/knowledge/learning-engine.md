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

**7 November 2026 — User-authorized:** “OK, proceed” selects the recommended learner-controlled follow-up run. BL-039 tracks implementation; BL-035 remains user review, and broader progression and scheduling remain BL-011. Selection, size and persistence details are implementation defaults to document before coding.

Let the learner select existing grammar or vocabulary targets from the summary and start a new, explicitly labeled review session. For example, article practice could reuse eligible article supply/repair formats with varied licensed sentences. Keep recognition and headword production distinct. Suggestions may draw on recorded outcomes, with support, response constraints, unknown diagnoses and possible earlier exposure shown separately; exact suggestion rules remain open.

Reuse current content and graders, preserve the source run and its frozen results, and persist the new run's selected targets, lineage, revisions and plan for reliable resume/replay. Earlier source-run teaching and answers are possible exposure, so review answers must not be presented as independent knowledge or mastery. Define the exposure contract, selection/prerequisite rules, run size and finite-pool repetition policy before implementation. This proposal adds no new curriculum, accounts, automatic schedule or proficiency threshold.

The [follow-up contract](targeted-follow-up.md) records the selected implementation defaults: one item per lexical target, additional distinct grammar sentences, no preselected targets or automatic suggestions, and frozen source-linked exposure.
