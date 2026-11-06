# Assumptions and Principles

## Working assumptions to validate

- **A01 — Initial proficiency, resolved 6 November 2026:** The user confirmed complete beginners as the initial learner level, with general everyday Danish as the content priority. These are now requirements in [Product direction](product-direction.md); they are no longer working assumptions. A specific learner age group was not selected.
- **A02 — Language variety:** Standard contemporary written Danish is a proposed starting scope. Register, colloquial variants, and exceptional constructions remain unresolved in Q03.
- **A03 — Generation strategy:** Reviewed sentence families and constrained variation may provide a manageable initial approach. Validate this against naturalness, variety, and the desired range of constructions; no generation method has been selected.
- **A04 — Trial size:** The earlier suggestion of 40–60 vocabulary entries, three grammar concepts, and four or five exercise formats was illustrative. It is not an agreed milestone or content limit.
- **A05 — Reusable data:** COR may support word identities and inflection data, but its suitability for the selected vocabulary and other required information still needs assessment.
- **A06 — Donor suitability:** Static inspection suggests Flow's generic foundation can support Astrid. Runtime fit, hosting cost, chosen components, and workload sizing remain to be validated. See the [Flow assessment](flow-stack-assessment.md).

Question IDs refer to [Open questions](open-questions.md).

## Confirmed product tenets

These restate the user's requirements from the planning discussion on 6 November 2026. See [Product direction](product-direction.md) for their scope.

- **T01 — Purposeful exercises:** Each exercise tests a specific topic or skill.
- **T02 — Meaningful alternatives:** Closely related alternatives should require the intended grammatical distinction rather than elimination of unrelated words.
- **T03 — Structural language knowledge:** The app should represent sentence components and construct varied sentences.
- **T04 — Grammar and vocabulary together:** Grammar, including word order, tense usage, and language-specific forms, is a substantial part of learning.
- **T05 — Varied activities:** Provide multiple exercise types to reduce repetitive practice.
- **T06 — Connected language foundations:** Establish a logical grammar breakdown, a thorough sentence-structure description, and sufficient teaching vocabulary.
- **T07 — Reuse existing foundations:** Prefer reusing Flow's proven code, tooling, and conventions where they fit Astrid, while evaluating the boundaries before adoption.
- **T08 — Shared UI family with distinct branding:** Follow the sibling apps' UI style and layout while giving Astrid its own main color and unique logo. Red is the user's preferred color direction; the logo must connect to Denmark or a Danish Astrid character. See [Visual identity](visual-identity.md).

## Proposed design principles

- **P01 — Fair difficulty:** Context should make a task answerable, and incidental unknown material should not obscure the intended target.
- **P02 — Natural language:** Generated sentences should be grammatical, meaningful, and appropriate to their context.
- **P03 — Evidence of transfer:** Test learned knowledge in unseen examples; identical prompt recall alone should not establish mastery.
- **P04 — Distinct capabilities:** Treat reading recognition and written production as different evidence about learning.
- **P05 — Useful feedback:** Explain relevant mistakes, accept valid alternatives, and preserve uncertainty about error causes.
- **P06 — Controlled variation:** Repeat skills while changing examples and keeping prerequisites and difficulty deliberate.
- **P07 — Platform independence:** Keep learning behavior and content reusable across web and future mobile interfaces.
- **P08 — Language extensibility:** Make Danish-specific rules identifiable without requiring a universal language system before the first release.
- **P09 — Independent learning logic:** Keep sentence generation, exercise construction, and answer evaluation independent of HTTP, persistence, and UI frameworks. A pure Kotlin/JVM engine is a candidate given the online/web-reuse preference; multiplatform compilation is optional future work.
- **P10 — Domain-specific correctness:** Treat linguistic validity, answer fairness, reproducibility, and content provenance as first-class quality checks alongside software coverage and static analysis.

These principles are assistant proposals. Adoption and concrete acceptance criteria remain open in Q11.
