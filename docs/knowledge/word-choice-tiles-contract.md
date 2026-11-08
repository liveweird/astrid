# Word choice and sentence tiles

## Authorization and boundary

**8 November 2026 — Confirmed:** “Go for it. Make sure you version things and release them
properly” selects BL-047, following the Module 1 authoring packet. Deliver three standalone
playground formats, independently review and verify them, and merge/push focused commits.
Release as app **0.5.0**, including the authored changelog, annotated `v0.5.0` tag and GitHub
Release at the verified master commit, then promote the private preview. This publication
does not make the application publicly hosted. Language remains a teaching-review draft.

The following bounded implementation decisions operationalize that authorization. Existing
guided practice and follow-ups remain pinned to pair three. Course integration, V2/question/
negation expansion, English sentence tiles, article distractors and matching are later work.
Q27 (typed noun bundles) and Q28 (vocabulary item budget) remain open.

## Formats and language inventory

| Format | Task | Primary target | Response |
| --- | --- | --- | --- |
| E011 | English word meaning → Danish | lexical meaning, `en-da` | one of four supplied article+noun choices |
| E012 | English sentence → Danish, all word tiles | G005 subject-first order | ordered tile IDs, `use-all` |
| E013 | English sentence → Danish, spare infinitive | G004 finite present form | ordered tile IDs, `with-spares` |

E011 uses the explicitly selected printed-reading senses of `en bog`, `en avis`, `et brev`
and new **L024 / L024-S01 `et magasin`**. The fourth item belongs only to the new choice
pool: old E007/E008/E009/E010 pools and all existing sentence allowlists stay unchanged.
E012/E013 reuse the 99 C001–C003 positive propositions. Each occurrence is an individual
word tile; an article is a separate tile. E013 supplies exactly one spare infinitive.

English cues and Danish answers have explicit language tags. Punctuation is separate from
the tile bank. Preserve lexical case (`I`), with sentence-position capitalization at display.
Occurrence IDs `b0`, `b1`, … are assigned after deterministic shuffling and disclose no role
or canonical position. Equivalent repeated occurrences may exchange places; one occurrence
ID cannot be used twice.

## Revisions, wire and private evidence

Freeze published pair four **before production changes, in its own commit**. Add
`pilot-5` / `generator-5` / `evaluation-5`, retaining all four previous pairs and their exact
wire, grading, fingerprints and review exports. Catalog/new playground defaults move to five;
request defaults and existing practice plans do not silently move. App and learning versions
are separate. No database migration is needed for additive JSON payloads.

Prompt additions are trailing nullable `tileMode` (`use-all` or `with-spares`) and
`requiredTileCount` (2 or 4), omitted for old formats. Reuse public `blocks: [{id,text}]`
and `{kind:"order",blockIds:[…]}`; E011 uses the text answer shape. Add linguistic scope
`danish-choice` rather than describing article+noun bundles as headwords.

Private inspector `tileSource` contains `tokens` (`blockId`, `lexicalId`, `form`),
`licensedOrders` (`kind`, `blockIds`) and `distractors` (`blockId`, `rationale`). Forms are
`subject`, `finite-present`, `infinitive`, `article`, `singular-headword`; order kinds are
`subject-first` and `fronted`. Engine keys retain structured identity, not public role labels.
Frame every scalar, nested value, list order and absence in the new reproduction fingerprint;
pin the old pair-four branch to literal revisions. Public responses never include keys,
licensed orders or grammatical token roles. OpenAPI closes pair/format/target/language/mode
correlations; generated TypeScript stays synchronized.

## Grading and transport

All three formats report target, meaning, response-constraint and language facets. Success
requires all applicable facets satisfied and acceptable language. Only complete licensed
responses receive bounded linguistic/meaning judgments; never accept prefixes/subsequences.

| Whole response | Target | Meaning | Constraint | Language |
| --- | --- | --- | --- | --- |
| E011 correct supplied choice | met | met | met | acceptable |
| E011 another supplied choice | not-met | not-met | met | acceptable |
| E012 subject-first canonical | met | met | met | acceptable |
| E012 licensed C002 fronting | not-met | met | met | acceptable |
| E012 missing article | unevaluable | unknown | not-met | unknown |
| E013 canonical, spare unused | met | met | met | acceptable |
| E013 complete infinitive replacement | not-met | met | met | known-issue |
| E013 finite licensed fronting | met | met | not-met | acceptable |
| E013 both verbs included | unevaluable | unknown | not-met | unknown |

E013's subject-first order is a response instruction, while finite form is the primary target.
Other complete permutations remain conservatively unknown unless explicitly recognized.
Incomplete/overlong selections from supplied IDs are ordinary grading outcomes. The browser
permits submission of any nonempty selection and never grades it. E003 keeps its old exact-all
validation. Unknown/duplicate IDs, wrong kinds and E011 unlisted choices return 400 only
after ownership lookup; outsiders receive uniform 404. Invalid requests reserve no retry gap.
Engine-owned supplied-choice normalization allows NFC and harmless whitespace, preserves
case-sensitive lexical distinctions, and stores raw input verbatim. Saved replay is read
before regeneration and retains its original attempt ID and serialized body.

## Acceptance and release

Run the full Docker-backed `scripts/check.sh`, both-browser journeys, new Axe/keyboard/
recovery cases, both themes and 320px/200% rendered review. Prove four choice cycles,
all 99 tile propositions, boundaries, repetition, spare handling, raw Unicode, malformed
transport, owner isolation, Postgres readback/replay and fingerprint mutation coverage.
All old golden/provenance bytes and applied migration bytes are immutable.

Use a uniquely owned QA stack for old→new upgrade and actual restart/replay. Before main
promotion create and verify a readable backup. Replace the main app only, preserving its
PostgreSQL process/mounts and immutable histories; remove only ownership-checked QA resources.
Old strict readers cannot read new fields: rollback requires a compatible reader, not the
bare 0.4.1 binary. Publish the tag/release only after exact-master CI passes and verify remote
readback. Record observed results here; tests and source checks do not confer teaching approval.

## Local verification before publication

The revision-four freeze is commit `c43c8f7`: 1,614 engine rows (SHA-256
`5fbe98aadf7489d96678e936bdb7829940795abb1d1bda5ff4a0e62277678053`) and
2,187 server rows (`e421043ca55977c38d0d9e65cb4d1dedff367f0b7d19b9dcd43127344480e5dd`),
exported once from unchanged baseline `e37749c`. All old golden/provenance and migration bytes
remain unchanged. Engine, API and frontend received separate independent reviews. Corrections
covered misleading tile-response badges and snapshot-specific L024 source provenance; follow-up
review found no remaining actionable defects. Added tests cover direct PostgreSQL raw-answer/
request-payload preservation with Unicode whitespace and uniform tile ownership/retry behavior.

The full Docker-backed local gate passes: 117 engine tests plus the fresh Arabic-locale run,
112 server tests and 163 frontend tests. Measured coverage is engine 98.12% lines / 83.94%
branches, server 97.84% / 79.25%, web 94.77% statements / 88.30% branches / 97.66% functions /
97.56% lines. Floors ratchet to engine lines 98, server branches 79 and web branches 88.
Current and retained-four teaching reports generate successfully. The 28 new packaged Chromium/
WebKit journeys pass without retries, including Axe, keyboard, supported exact replay and
320px/200% text. Independent review inspected twelve rendered states in both themes; two
additional E011-success captures complete the visual evidence. The isolated old→new upgrade
preserves exact old wire/history and all four evaluators, including old practice/review runs.

Final clean-commit image, complete 226-journey matrix, exact-master CI, tag/release publication,
backed-up private-preview promotion and ownership-scoped cleanup remain release steps; their
observed results will be recorded below after execution. Private logs and artifacts are ignored
under `.local/word-choice-tiles/`.

The first complete native browser run exposed sixteen strict-locator failures in existing accessibility/layout sweeps: the E001 title is a prefix of E013. Exact accessible-title matching corrects the tests without changing app behavior. The shared format inventory now includes all thirteen entries, extending short-window and enlarged-list navigation coverage to the additions. This test-only correction keeps app 0.5.0 and learning pair five unchanged; a fresh clean-commit candidate and full matrix are required before publication.
