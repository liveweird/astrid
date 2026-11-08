# Backlog

Open work for Astrid, by status tag: `[next]`, `[todo]`, `[new]`, `[parked]`, `[blocked]`.
Items an agent adds start as `[new]`; only the owner promotes an item to `[todo]` or `[next]`.
A finished item is removed. Its evidence lives in the topic contract under `docs/knowledge/` (the "Observed delivery"
sections), the release history in `web/src/changelog/entries.ts`, and Git history. Statuses and IDs follow the
[working agreement](docs/knowledge/working-agreement.md); ordering is a proposed sequence, not user approval of the
assistant's design proposals.

## Review and validation

- [next] **BL-035 — Review the expanded playground, guided practice and targeted reviews.** Try `/practice` with **Mixed practice** (eight parts) and **Present verb forms** (six parts), then all ten standalone entries under “Generation controls.” Assess pacing, instruction/feedback clarity, typed Danish input, repair, response constraints, support, recovery, summaries and repetition. Compare sentence-writing order/meaning/language/shape feedback and short-text participant/action questions before choosing the next bounded engine/content iteration. From a completed summary, select a single vocabulary direction and a grammar target for separate follow-up runs; assess selection, source links and possible earlier exposure. Compare the three verb-operation summary cards and select individual operations for four/four/six-part reviews; check cancelling the focus chooser and resuming saved runs. Record reproducible coordinates for corrections. The [review guide](docs/knowledge/pilot-review-guide.md) covers both flows; specialist validation remains BL-022.
- [todo] **BL-022 — Validate and extend the Danish foundation drafts.** Review the [grammar map](docs/knowledge/danish-grammar-map.md), [construction catalogue](docs/knowledge/danish-sentence-catalogue.md), and pilot content for teaching order, linguistic naturalness, valency, accepted variants, and documented coverage gaps. Arrange appropriate Danish teaching review before labeling content specialist-approved; source and independent agent checks do not confer that status. Preserve stable IDs and add regression examples for corrected rules. The source-backed pass and [coverage crosswalk](docs/knowledge/foundation-coverage.md) are complete as drafts; a [proposed specialist handoff](docs/knowledge/pilot-teaching-review.md) specifies the packet, live teaching-unit review, issue records and revision safeguards. No engagement or approval is arranged.
- [todo] **BL-008 — Finalize the engine validation subset.** A [connected draft](docs/knowledge/beginner-slice-spec.md) links six skills, three constructions, 23 vocabulary entries and 99 licensed abstract sentence combinations; the catalog has ten entries, including two directions for three lexical senses, constrained sentence writing and short reading comprehension. Validate naturalness, target isolation, review status, and content/API cases; define sampling and learner-success measures. The draft supplies implementation acceptance examples but does not establish teacher approval or learning efficacy.

## Language content and resources

- [todo] **BL-025 — Resolve resource adoption and corpus gaps.** The [morphology](docs/knowledge/danish-resource-assessment.md), [usage](docs/knowledge/frequency-and-usage-resources.md) and [bilingual](docs/knowledge/bilingual-resources.md) assessments distinguish candidate coverage, editions, terms and mapping limits. Before any import, select the intended use/distribution, clarify conditional rights, pin releases and decide how authored English senses and upstream derivation are reviewed. COR and the [candidate crosswalk](docs/knowledge/cor-pilot-crosswalk.md) remain unadopted.
- [todo] **BL-004 — Define the teaching vocabulary subset.** A [23-entry pilot seed](docs/knowledge/danish-vocabulary-seed.md) records selected meanings, forms, gender, English glosses, and construction compatibility. Expand toward the broader curriculum using usage/frequency evidence and reviewed forms; preserve sources, stable IDs, review state, and versioning. Complete when vocabulary supports useful variation for the selected course coverage; depends on BL-025 and BL-022.

## Exercises and engine

- [todo] **BL-006 — Specify further exercise families.** Choice, supply, ordering, guided repair, lexical recognition/recall, constrained sentence writing and short reading comprehension are delivered. The [expansion proposal](docs/knowledge/exercise-family-expansion.md) recommends error diagnosis/repair next; question transformations remain a later candidate needing grammar activation. Define targets/prerequisites, meaning/structure generation, answer leakage, valid alternatives, unknown cases, response contracts and revision preservation. Complete with worked cases and a reviewable specification for the selected families; diagnosis and transformation remain proposals.
- [todo] **BL-007 — Define answer evaluation and feedback.** The pilot specifies normalization, separate target-satisfaction/linguistic-status facets, and correct/incorrect/valid-but-off-target/unavailable cases. Extend policies to partial credit, later grammar, and freer writing; retain raw input and avoid unsupported grammaticality claims. See [worked cases](docs/knowledge/beginner-slice-spec.md#concrete-worked-cases).
- [new] **BL-042 — Optional additional focus entries and article evidence polish (unprioritized, not authorized).** Remaining scope is fresh-run chooser entries for subject-first sentence order and indefinite articles, plus operation-separated article summaries/reviews. Preserve existing snapshots, target keys and replay if selected. See the [clarification](docs/knowledge/learning-engine.md#correction-to-the-next-step-recommendation).
- [new] **BL-010 — Compare generation and grading approaches.** Examine reviewed templates, structural generation, and possible AI assistance against accuracy, naturalness, variety, and diagnostic needs. Complete with broader-course tradeoffs and a recommendation. The pilot's deterministic typed generation and bounded grading are not reopened.
- [new] **BL-011 — Explore progress and review strategy.** Consider recognition versus production, mastery evidence, spaced practice, and uncertainty. Complete with a proposed model and evaluation criteria; no scheduling algorithm is selected.

## Operations

- [new] **BL-032 — Define bounded server-outage work.** Follow-up to the [observed lifecycle limitation](docs/knowledge/server-outage-boundary.md): choose a fault model and evidence-based request/cleanup/shutdown budgets, then prove lease fencing, eventual remote cleanup, bounded retained work and exact replay before adopting a policy. No server deadline or production SLO is selected.
- [new] **BL-044 — Measure Astrid's memory use under the e2e load, then port Flow's JVM flags.** Flow's `server/build.gradle.kts` sets `applicationDefaultJvmArgs` (a `-Xmx256m` heap cap, C1-only compilation, a class-data archive); none is ported because [Astrid's reuse manifest](docs/knowledge/flow-reuse-manifest.md) says to measure first and the k8s memory limit is 768Mi. Complete with measured RSS and startup under the full `npm --prefix e2e test` run, and either ported flags with the numbers that justify them or a recorded decision to omit.

## Brand

- [todo] **BL-021 — Specify Astrid branding within the shared UI family.** Preserve the sibling style/layout and theme/shell conventions. Develop the preferred red palette and distinct feedback semantics; explore a unique Denmark-related or Danish Astrid character logo. Complete when the palette, logo concept, asset requirements, and validation criteria are recorded (Q18, Q19). The [visual identity brief](docs/knowledge/visual-identity.md) is recorded; production assets remain in the later media phase.

## Later phases

- [parked] **BL-013 — Build the mobile version.** Revisit after the web engine is satisfactory. Online operation and web-interface reuse are acceptable; native UI and offline lessons are not current requirements.
- [parked] **BL-014 — Add listening and speaking.** Explicitly postponed by the user. Revisit when the reading and writing engine is satisfactory and scope is reopened.
- [parked] **BL-015 — Produce images, sounds, and other production media.** Revisit once the engine is satisfactory.
- [parked] **BL-016 — Add further target languages.** Revisit after Danish establishes the approach and the user chooses another language.
