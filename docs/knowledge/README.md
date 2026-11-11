# Project Knowledge Base

This is the repository record of the Danish learning app's requirements, design proposals, research, and unresolved questions. The private engine playground is implemented; language content remains a draft awaiting Danish teaching review. Records below distinguish delivered behavior from future proposals.

## Contents

- [Product direction](product-direction.md): confirmed scope, intended advantages, and product questions.
- [Learning engine](learning-engine.md): proposed exercise model, variability, evaluation, and progress tracking.
- [Language foundations](language-foundations.md): requirements for the grammar curriculum, sentence structures, and vocabulary database.
- [Research and sources](research-and-sources.md): reference material, potential reusable data, and validation needs.
- [Working agreement](working-agreement.md): standing documentation rule, decision conventions, and backlog statuses.
- [Assumptions and principles](assumptions-and-principles.md): working assumptions, confirmed product tenets, and proposed design principles.
- [Open questions](open-questions.md): central register of unresolved questions and other open points.
- [Flow stack assessment](flow-stack-assessment.md): evaluated donor stack, reuse boundaries, recommendations, and gaps.
- [Visual identity](visual-identity.md): shared sibling UI style/layout, preferred red accent, and Denmark/Astrid logo brief.
- [First engine milestone](first-engine-milestone.md): readiness recommendation, proposed design pass, engine playground, and completion criteria.
- [Danish grammar map](danish-grammar-map.md): 58 draft skills in eight proposed phases with prerequisite links.
- [Seven-module grammar course blueprint](grammar-course-blueprint.md): the user's 28 topic units, each with multiple short sessions, mapped to existing skills with source checks and reconciliation proposals.
- [Seven-module vocabulary course blueprint](vocabulary-course-blueprint.md): the user's 35 synchronized thematic units, four design principles, proposed lesson mix and evidence/authoring qualifications.
- [Thematic vocabulary expansion](vocabulary-expansion-activation.md): selected five-session work/study authoring milestone, activation contract, overnight queue and stopping boundaries.
- [Work/study vocabulary packet](vocabulary-work-study-packet.md): eighteen selected placements, five complete session scripts and 23 authored exercise slots for the selected first-unit implementation.
- [Food/shopping vocabulary outline](vocabulary-food-shopping-packet.md): draft 21-placement ledger, six session outlines, functional purchase/request language and direct-entry review; no runtime activation.
- [Places/journey vocabulary outline](vocabulary-places-journeys-packet.md): draft 21-placement ledger, six session outlines, explicit static/directional limits and source/homograph checks; no runtime activation.
- [Module 1 course slice](module-one-course-slice.md): authorized short-session authoring plan, prerequisite reconciliation, learning evidence and dependency-ordered engine gaps. The [language pack](module-one-language-pack.md) and [worked exercises](module-one-worked-exercises.md) supply bounded source-checked material and grading cases.
- [First Module 1 activation contract](module-one-activation.md): selected implementation of three six-item beginner sessions, explicit teaching/reference exposure, scoped pronoun/present-form evidence and compatibility acceptance; [0.9.0 runtime delivery](module-one-activation.md#observed-runtime-delivery--10-november-2026) is verified.
- [Danish sentence catalogue](danish-sentence-catalogue.md): 30 phrase/clause constructions and generation constraints.
- [Danish vocabulary seed](danish-vocabulary-seed.md): the original 23-entry seed, choice-only magazine and supplied first-step teacher additions, grammatical forms, senses, and revision-specific allowlists.
- [Beginner slice specification](beginner-slice-spec.md): connected targets, three exercise formats, grading examples, variation rules, and content/API responsibilities.
- [Project backlog](../../BACKLOG.md): separate work plan using `[next]`, `[todo]`, `[new]`, `[parked]`, and `[blocked]`.

## Status conventions

**Confirmed requirements** come directly from the user's stated intentions. **Proposals** are ideas discussed with the assistant that have not been adopted as decisions. **Open questions** need further investigation or a product decision. Examples illustrate possibilities; they do not establish curriculum coverage or implementation commitments.

Update the relevant notes as planning continues. Promote a proposal to a confirmed decision only when the discussion supports that change, and preserve the reason for consequential decisions. Keep externally sourced linguistic facts separate from proposed teaching choices.

The user has established continuous documentation as a standing project rule. Capture assumptions, requirements, constraints, open points, questions, decisions, rules, conventions, principles, and tenets as they arise. Follow the [working agreement](working-agreement.md) for record locations and maintenance.

## Current implementation

The [backlog](../../BACKLOG.md) owns work status and priority. The first grammar map and construction catalogue are drafted, with a connected beginner seed and worked exercise specification. The authorized engine playground is delivered. Its scope and operational boundaries are recorded in [implementation decisions](implementation-decisions.md), with the concrete [Flow reuse manifest](flow-reuse-manifest.md). Linguistic teaching review remains separate from software validation.

The broad language map and the small validation subset serve different purposes. A complete language implementation is not a prerequisite for validating the engine.

- [Playground delivery](playground-delivery.md): observed baseline implementation and validation evidence.
- [Pilot hardening plan](pilot-hardening-plan.md): authorized overnight reliability, content-integrity and accessibility work.

- [Danish resource assessment](danish-resource-assessment.md): primary evidence for morphology, sense identity, access and published reuse terms; no imported data.

- [Proposed COR pilot crosswalk](cor-pilot-crosswalk.md): record-level candidate morphology identities and homograph safeguards; not an adopted import.
- [Pilot source review](pilot-source-review.md): bounded linguistic source checks, precise locator gaps and qualifications.
- [Hardening delivery](hardening-delivery.md): observed compatibility, transaction, accessibility and local Kubernetes evidence.
- [Foundation source review](foundation-source-review.md): consolidated later-map corrections, coverage boundaries and source limitations.
- [Pilot review guide](pilot-review-guide.md): a short proposed walkthrough for evaluating the engine before expanding it.
- [Danish teaching review handoff](pilot-teaching-review.md): proposed specialist packet, full-inventory review, issue records and immutable-revision safeguards.
- [Runtime database recovery](runtime-recovery.md): observed paused-database behavior, bounded browser waiting and exact attempt recovery.
- [Server outage boundary](server-outage-boundary.md): proposed local/remote cleanup invariants, pinned-contract findings and future failure-matrix acceptance.
- [Static delivery](static-delivery.md): measured uncompressed assets, Flow plugin reuse and transport/browser acceptance.
- [Text resizing and reflow](text-resize-reflow.md): measured enlarged-text defects, corrections and regression evidence.
- [Keyboard navigation and selection](keyboard-navigation.md): confirmed navigation/Enter defects, bounded corrections and acceptance requirements.
- [Unicode input boundary](unicode-input-boundary.md): reproduced malformed-surrogate replay gap, transport validation and length policy.
- [Foundation coverage crosswalk](foundation-coverage.md): proposed links for all 58 skills and 30 constructions, distinguishing pilot targets, support, later overlays and gaps.
- [Pilot variation inventory](pilot-variation-audit.md): measured sentence, format and target distributions without changing published generation.
- [Frequency and usage resources](frequency-and-usage-resources.md): candidate ranking/corpus evidence, edition differences and published reuse conditions.
- [Bilingual lexical candidates](bilingual-resources.md): English-sense resources, mapping limits and data/tool licence distinctions.
- [Exercise extension proposal](exercise-extension-proposal.md): user-selected recall and repair contract, support and compatibility boundaries; vocabulary assessment remains proposed.
- [Beginner outcome references](beginner-outcome-reference.md): official framework reference points and proposed reading/writing evidence, without selecting a CEFR level or exam pathway.

- [Recall and guided repair delivery](recall-repair-delivery.md): selected operations, compatibility safeguards and observed verification for BL-034.

- [Vocabulary assessment contract](vocabulary-assessment.md): authorized recognition/headword-recall targets, feedback facets, revision and migration safeguards, and acceptance evidence.

- [Vocabulary delivery](vocabulary-delivery.md): observed compatibility, migration, inspector, grading and browser verification for BL-036.

- [Short practice sessions](practice-sessions.md): authorized BL-009 plan, possible-exposure evidence, owner-bound recovery and transaction acceptance.

- [Guided practice delivery](practice-delivery.md): local verification, independent review corrections and rollout evidence for BL-009.

- [Practice variation inventory](practice-variation-audit.md): independently checked semantic/presentation counts, finite bounds and reproducible frozen method.

- [Practice continuity follow-up](practice-continuity.md): readable support, real shared-outbox scenarios and actual process-restart evidence.

- [Targeted follow-up practice](targeted-follow-up.md): authorized learner selection, finite review plans, frozen source exposure and recovery acceptance.

- [Application versioning](app-versioning.md): Flow-derived version stamp, lazy changelog, build identity and release conventions.

- [Topic-based practice](topic-practice.md): authorized mixed/verb focus selection, operation-specific outcomes, precise exposure and saved-run continuity.

- [Exercise family expansion](exercise-family-expansion.md): selected sentence writing and short reading comprehension, with error diagnosis and transformations retained as later proposals.

- [Writing and reading implementation contract](writing-reading-contract.md): authorized standalone sentence production and short-text comprehension, bounded grading and retained revision safeguards.

- [Word choice and sentence tiles](word-choice-tiles-contract.md): authorized BL-047 formats, revision-five grading/wire contract, compatibility freeze and 0.5.0 release acceptance.

- [Vocabulary matching grid](vocabulary-matching-contract.md): delivered four-pair E014, per-pair evidence, whole-board retry contract and verified 0.6.0 release.

- [Multiple-gap exercises](multiple-gap-contract.md): delivered two-gap 0.7.0 release, primary/supporting grammar evidence, compatibility, actual backup restore and verified preview promotion.

- [Guided multiple-gap practice](guided-gap-practice.md): delivered 0.8.0 four-item session, separate slot evidence, selected-only reviews and verified compatibility/promotion.

- [Inanimate-reference activation](inanimate-reference-activation.md): selected S04 subject-reference session, supplied gender, scoped evidence and compatibility/release acceptance.

- [Present forms and sentence assembly](module-one-course-slice.md#following-package--s05-and-s06): S05–S06 activity cards, bounded grading and exposure, with [verified 0.11.0 delivery](module-one-course-slice.md#observed-delivery-for-s05-and-s06).
