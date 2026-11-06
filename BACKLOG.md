# Project Backlog

Work planning for the Danish learning app. Updated 6 November 2026. Current scope is discovery and documentation; application implementation has not been authorized.

Statuses follow the [working agreement](docs/knowledge/working-agreement.md). The ordering below is an initial proposed sequence; it does not imply the user has approved the assistant's design proposals.

## [next]

- **BL-001 — Clarify initial learning scope.** Define the first learner group, reading and writing outcomes, starting proficiency, and boundaries of Danish grammar and sentence-variant coverage. Complete when answers and remaining uncertainties are recorded in [Product direction](docs/knowledge/product-direction.md) and [Open questions](docs/knowledge/open-questions.md). This can proceed through further vision discussion; it does not authorize a full curriculum or implementation yet.

## [todo]

- **BL-002 — Map the grammar curriculum.** Inventory teachable skills, prerequisite relationships, and a provisional logical teaching order for the confirmed grammar requirement. Record examples, language-source references, and review needs. Refine scope using BL-001; coordinate with BL-003.
- **BL-003 — Map sentence constructions.** Catalogue phrase and clause families, components, ordering constraints, valid transformations, exceptions, and coverage status. Complete when the agreed scope is represented and linked to grammar topics. Coordinate with BL-002.
- **BL-004 — Define the teaching vocabulary subset.** Establish selection criteria, meanings, grammatical forms, construction compatibility, English equivalents, and source fields. Complete when vocabulary supports useful variation for the selected topic and construction coverage; use BL-002, BL-003, and BL-005.
- **BL-005 — Assess language resources.** Evaluate morphology, frequency, meanings, English equivalents, and usage sources, including COR. Record coverage gaps, resource versions, and specific reuse terms. Complete with a source recommendation and unresolved data needs.
- **BL-006 — Specify exercise families.** Turn candidate formats into worked examples with explicit learning targets, prerequisites, related alternatives, and variation rules. Complete with a reviewed-on-paper exercise catalogue; exact formats remain proposals.
- **BL-007 — Define answer evaluation and feedback.** Resolve answer-equivalence, error-tolerance, partial-credit, ambiguity, and diagnostic-feedback policies. Complete with examples of accepted, rejected, and uncertain answers and explanatory feedback. Depends on concrete examples from BL-006.
- **BL-008 — Design a manual engine validation subset.** Connect selected grammar, constructions, vocabulary, exercise variants, grading, and feedback. Agree scope, acceptance criteria, review needs, and learning evidence before implementation. Uses BL-002 through BL-007; trial sizes remain undecided.

## [new]

- **BL-009 — Explore session and progression design.** Assess guided instruction, independent practice, mixed practice, learner control, and session shape. Complete when options and a proposed learning flow are documented.
- **BL-010 — Compare generation and grading approaches.** Examine reviewed templates, structural generation, and possible AI assistance against accuracy, naturalness, variety, and diagnostic needs. Complete with tradeoffs and a recommendation; no method is selected.
- **BL-011 — Explore progress and review strategy.** Consider recognition versus production, mastery evidence, spaced practice, and uncertainty. Complete with a proposed model and evaluation criteria; no scheduling algorithm is selected.

## [parked]

- **BL-012 — Select architecture and begin implementation.** Revisit when the engine requirements are concrete and the user authorizes application code. Complete with documented technical decisions and an agreed implementation scope.
- **BL-013 — Build the mobile version.** Revisit after the initial web engine is satisfactory. Preserve this future requirement when evaluating interface and architecture options.
- **BL-014 — Add listening and speaking.** Explicitly postponed by the user. Revisit when the reading and writing engine is satisfactory and scope is reopened.
- **BL-015 — Produce images, sounds, and other production media.** Revisit once the engine is satisfactory. Linguistic examples and vocabulary needed to validate the engine belong in current foundation work.
- **BL-016 — Add further target languages.** A possible future expansion for English-speaking learners. Revisit after Danish establishes the approach and the user chooses another language.

## [blocked]

- **BL-018 — Commit and push the project documentation.** Authorized by the user on 6 November 2026 after initializing Git and configuring `origin` as `https://github.com/liveweird/astrid.git`. The repository has no commits and is on `master`. Attempted `git add -- AGENTS.md BACKLOG.md docs`; it failed because this session cannot create `.git/index.lock`. The active policy protects `.git` as read-only and disables sandbox approval requests. Unblock through an effective permission change or by staging, committing, and pushing locally; see [Git access troubleshooting](docs/knowledge/working-agreement.md#git-access-troubleshooting). No permission change has been confirmed. Complete when the documentation commit is present on the intended remote branch. No commit or push has been performed; remote connectivity and authentication remain unverified.

## Completed work

- **6 November 2026 — Contributor guide:** Created root `AGENTS.md` after confirming it did not exist.
- **6 November 2026 — Knowledge base:** Recorded product scope, engine proposals, language foundations, references, and unresolved questions in [docs/knowledge](docs/knowledge/README.md).
- **6 November 2026 — Documentation workflow and backlog:** Added the standing documentation rule, assumptions and principles register, central question list, and this separate backlog. Added scoped documentation instructions in [docs/AGENTS.md](docs/AGENTS.md).
- **6 November 2026 — BL-017, Git initialization:** The user initialized Git and configured the GitHub origin. Verified that Git recognizes the local repository and reports the configured fetch and push remote. Continued delivery is tracked as BL-018.
