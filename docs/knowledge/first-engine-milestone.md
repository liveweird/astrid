# First Learning Engine Milestone

## Readiness recommendation

The product direction is sufficient to begin concrete language and engine design. The grammar inventory, sentence-construction catalogue, and vocabulary subset are still requirements rather than authored resources. Propose one short design pass to make a small end-to-end engine milestone reviewable, then begin implementation once the user authorizes application code.

The user asked whether more information is needed or building should begin. This is a readiness discussion; the earlier planning-only constraint remains in effect. The milestone below is a proposal.

## Confirmed initial scope

The user answered both initial scope questions on 6 November 2026:

- Initial learner level: complete beginners learning through English.
- Vocabulary and context priority: general everyday Danish.

These are confirmed requirements. No further broad product questionnaire is needed to draft the language map and the first exercise slice. Specific outcome, grammar-boundary, grading, and linguistic-review choices can be resolved through that design work. See Q01 and Q02 in [Open questions](open-questions.md).

## Proposed design pass

1. Draft the broad grammar topic map and prerequisite relationships, alongside sentence-construction families. Track advanced coverage without requiring every rule to be finished before implementation.
2. Choose a connected beginner subset and enough compatible everyday vocabulary to generate varied, useful sentences. Define an observable reading/writing outcome for the subset.
3. Specify three initial exercise formats: related-form selection, gap completion, and sentence ordering. For each, document the target skill, prerequisites, valid variations, accepted answers, likely mistakes, and explanatory feedback.
4. Define the first content records, exercise/attempt contract, generation constraints, and tests. Record the remaining Flow-stack adoption choices in the reuse manifest.

These activities develop BL-002 through BL-008 and BL-020; they do not replace the broader language-foundation work.

## Proposed first implementation

Build an engine playground inside the shared sibling application layout. It should let a reviewer choose a grammar target and exercise type, generate multiple variations, answer them, see feedback, and inspect the linguistic structure and generation details through a development-only view. Learner-facing text should remain clear and free of internal engine details.

Use the preferred red branding direction with provisional styling; the final logo and palette remain later visual work. Retain the confirmed Docker Compose and Kubernetes scope and port the smallest useful Flow foundation. A pure Kotlin engine, Ktor API, PostgreSQL, and React/Mantine interface remain the recommended stack, pending adoption decisions.

## Proposed completion criteria

- Each exercise identifies its intended skill and required knowledge.
- Variations remain within the supported grammar and vocabulary constraints.
- Related alternatives test the intended distinction, and context makes the task fair.
- Grading follows explicit policies and accepts the documented alternatives.
- Feedback explains the relevant rule or mistake.
- A reported exercise can be reproduced using its recorded content/generator version and generation inputs.
- Reviewed language examples and automated engine/API/UI checks support the slice.
- Retrying a recorded answer does not create a duplicate attempt.

Numeric content targets, sampling requirements, and the linguistic review process still need definition. Demonstrating software behavior is separate from establishing that learners retain and transfer the knowledge.

## Decisions that can follow

Final logo artwork, exact red shades, production Kubernetes provider, public onboarding, long-term review scheduling, gamification, and mobile packaging can be scheduled after the first engine behavior is concrete. Decide any of these earlier if they become an actual dependency; none is a reason to leave the language model unspecified.
