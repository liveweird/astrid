# Project Backlog

Work planning for the Danish learning app. Updated 6 November 2026. Current scope is discovery and documentation; application implementation has not been authorized.

Statuses follow the [working agreement](docs/knowledge/working-agreement.md). The ordering below is an initial proposed sequence; it does not imply the user has approved the assistant's design proposals.

## [next]

- **BL-001 — Clarify initial learning scope.** Complete beginners and general everyday Danish are confirmed. Define observable reading/writing outcomes and grammar/sentence-coverage boundaries through the first slice design, recording remaining uncertainties in [Product direction](docs/knowledge/product-direction.md) and [Open questions](docs/knowledge/open-questions.md). The [first engine milestone](docs/knowledge/first-engine-milestone.md) proposes a short language/engine design pass followed by an engine playground. No further broad questionnaire is needed before drafting; application implementation awaits authorization.

## [todo]

- **BL-002 — Map the grammar curriculum.** Inventory teachable skills, prerequisite relationships, and a provisional logical teaching order for the confirmed grammar requirement. Record examples, language-source references, and review needs. Refine scope using BL-001; coordinate with BL-003.
- **BL-003 — Map sentence constructions.** Catalogue phrase and clause families, components, ordering constraints, valid transformations, exceptions, and coverage status. Complete when the agreed scope is represented and linked to grammar topics. Coordinate with BL-002.
- **BL-004 — Define the teaching vocabulary subset.** Establish selection criteria, meanings, grammatical forms, construction compatibility, English equivalents, and source fields. Complete when vocabulary supports useful variation for the selected topic and construction coverage; use BL-002, BL-003, and BL-005.
- **BL-005 — Assess language resources.** Evaluate morphology, frequency, meanings, English equivalents, and usage sources, including COR. Record coverage gaps, resource versions, and specific reuse terms. Complete with a source recommendation and unresolved data needs.
- **BL-006 — Specify exercise families.** Turn candidate formats into worked examples with explicit learning targets, prerequisites, related alternatives, and variation rules. Complete with a reviewed-on-paper exercise catalogue; exact formats remain proposals.
- **BL-007 — Define answer evaluation and feedback.** Resolve answer-equivalence, error-tolerance, partial-credit, ambiguity, and diagnostic-feedback policies. Complete with examples of accepted, rejected, and uncertain answers and explanatory feedback. Depends on concrete examples from BL-006.
- **BL-008 — Design a manual engine validation subset.** Connect selected grammar, constructions, vocabulary, exercise variants, grading, and feedback. Agree scope, acceptance criteria, review needs, and learning evidence before implementation. The [first engine milestone](docs/knowledge/first-engine-milestone.md) proposes related-form selection, gap completion, and sentence ordering in a connected slice; these formats are not yet selected. Uses BL-002 through BL-007; trial sizes remain undecided.
- **BL-020 — Agree the Flow reuse boundary.** Review the [stack assessment](docs/knowledge/flow-stack-assessment.md), decide which remaining foundation components and rules to keep/adapt/omit, and record a donor manifest with provenance and required validation. Kubernetes and Docker Compose are confirmed: include adapted manifests, local-stack configuration, and their validation in the reuse plan. Resolve account, hosting, and API-contract implications. Complete with recorded adoption decisions; the assessment alone authorizes no code port. Use the confirmed online/web-reuse mobile preference.
- **BL-021 — Specify Astrid branding within the shared UI family.** Preserve the sibling style/layout and reuse the theme/shell conventions. Develop the preferred red palette and distinct feedback semantics; explore a unique Denmark-related or Danish Astrid character logo. Complete when the palette, logo concept, asset requirements, and validation criteria are recorded. Exact visual choices remain open in Q18 and Q19. The [visual identity brief](docs/knowledge/visual-identity.md) is recorded; production assets remain in the later media phase unless the user brings them forward.

## [new]

- **BL-009 — Explore session and progression design.** Assess guided instruction, independent practice, mixed practice, learner control, and session shape. Complete when options and a proposed learning flow are documented.
- **BL-010 — Compare generation and grading approaches.** Examine reviewed templates, structural generation, and possible AI assistance against accuracy, naturalness, variety, and diagnostic needs. Complete with tradeoffs and a recommendation; no method is selected.
- **BL-011 — Explore progress and review strategy.** Consider recognition versus production, mastery evidence, spaced practice, and uncertainty. Complete with a proposed model and evaluation criteria; no scheduling algorithm is selected.

## [parked]

- **BL-012 — Select architecture and begin implementation.** Architecture assessment is permitted during discovery; the [Flow assessment](docs/knowledge/flow-stack-assessment.md) is available. Finalize implementation scope when engine requirements are concrete and the user authorizes application code. Complete with documented technical decisions and an agreed implementation scope; coordinate with BL-020.
- **BL-013 — Build the mobile version.** Revisit after the initial web engine is satisfactory. The user confirmed that online operation and web-interface reuse are acceptable; native UI and offline lessons are not current requirements. Choose packaging/distribution later and retain mobile input and responsive testing requirements.
- **BL-014 — Add listening and speaking.** Explicitly postponed by the user. Revisit when the reading and writing engine is satisfactory and scope is reopened.
- **BL-015 — Produce images, sounds, and other production media.** Revisit once the engine is satisfactory. Linguistic examples and vocabulary needed to validate the engine belong in current foundation work.
- **BL-016 — Add further target languages.** A possible future expansion for English-speaking learners. Revisit after Danish establishes the approach and the user chooses another language.

## [blocked]

No active work is currently blocked. The initial push was completed locally by the user; this session's Git write restriction remains an environment constraint for future Git operations, as recorded in [Git access troubleshooting](docs/knowledge/working-agreement.md#git-access-troubleshooting).

## Completed work

- **6 November 2026 — Contributor guide:** Created root `AGENTS.md` after confirming it did not exist.
- **6 November 2026 — Knowledge base:** Recorded product scope, engine proposals, language foundations, references, and unresolved questions in [docs/knowledge](docs/knowledge/README.md).
- **6 November 2026 — Documentation workflow and backlog:** Added the standing documentation rule, assumptions and principles register, central question list, and this separate backlog. Added scoped documentation instructions in [docs/AGENTS.md](docs/AGENTS.md).
- **6 November 2026 — BL-017, Git initialization:** The user initialized Git and configured the GitHub origin. Verified that Git recognizes the local repository and reports the configured fetch and push remote. Continued delivery is tracked as BL-018.
- **6 November 2026 — BL-018, Initial documentation push:** The user reported completing the push. Verified that local `master` and the local tracking reference `origin/master` both point to `e9521ee` (`Document learning app requirements and work plan`) and that the working tree was clean before this completion update. No live remote query or agent-side Git write was performed. This completion update is a subsequent local documentation change.
- **6 November 2026 — BL-019, Flow stack assessment:** Inspected Flow at `4e255e966804c5750c8fe0f1928a1461c17cc96f`, including independent backend/API, UI, and delivery reviews. Documented technology, infrastructure, design, API, testing, CI/CD, quality rules, reuse recommendations, and gaps. The read-only toolchain-pin check passed; full runtime gates and live GitHub settings were not verified. Recorded the user's online/web-reuse mobile clarification. No application code or Flow files were changed. See the [assessment](docs/knowledge/flow-stack-assessment.md); adoption planning continues in BL-020.
- **6 November 2026 — Infrastructure decision recorded:** The user confirmed retaining Kubernetes and Docker Compose. Updated product requirements, the Flow assessment, Q14, and BL-020; the earlier Kubernetes-deferral suggestion is superseded. Deployment topology and production details remain open.
- **6 November 2026 — Visual identity brief recorded:** Captured the shared sibling UI style/layout requirement, preferred red brand direction, and Denmark-related or Danish Astrid character logo brief. Confirmed the palette mechanism through read-only sibling theme inspection. Final palette and logo work is tracked in BL-021; no assets or application code were generated.
- **6 November 2026 — Readiness proposal and scope clarification:** Proposed a short language/engine design pass followed by an engine playground, with initial completion criteria. The user confirmed complete beginners and general everyday Danish. Recorded those requirements and resolved Q01; exact first-slice outcomes and application-code authorization remain open. See the [milestone proposal](docs/knowledge/first-engine-milestone.md).
