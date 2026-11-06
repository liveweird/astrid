# Learning Engine Proposals

The following design ideas support the [confirmed product requirements](product-direction.md). They remain proposals; no architecture, framework, or algorithm has been selected.

The [beginner slice specification](beginner-slice-spec.md) now makes the first three exercise formats, generation boundaries, normalization, feedback, and attempt handling concrete enough to review. The broader ideas below remain design direction rather than implemented behavior.

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

Before implementation, manually test a connected subset containing sentence structures, vocabulary, exercise variants, valid answers, likely mistakes, and feedback. Include unseen examples and delayed practice when evaluating learners.

Keep learning rules and content independent of web screens, account for phone input, and isolate Danish-specific rules so future languages can be added. Decisions about storage, mobile technology, scheduling algorithms, generation methods, and grading remain open.
