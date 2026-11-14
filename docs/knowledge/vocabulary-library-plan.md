# Vocabulary Library: Browse-and-Revise Milestone

Status: browse-and-revise milestone selected on 14 November 2026 following the user’s “vocabulary library looks awesome” response to the two-stage recommendation. The assistant proceeds with the recommended first stage: all-entry reference browsing, the saved-lesson-teaching filter and untracked reference use. This interpretation was stated before implementation; recognition/recall practice remains stage two, and the [backlog](../../BACKLOG.md) retains BL-067 until its accepted practice scope is delivered. Baseline: app 0.23.0, source `6080df921240cb27b68bd59a7bfa76d3b471f19c`.

## Selected first-stage scope

This reference-only milestone is a partial precursor, not completion or closure of BL-067. The accepted direction still includes owner-introduced material and populated recognition/recall practice by situation. Those requirements remain open unless the user explicitly revises them. The selected first stage offers all 79 reference records; later practice retains the accepted introduced-content scope.

Provide reference cards for all 79 existing lexical entry records, grouped by their course situations, with selected meanings, actually taught forms/article bundles/fixed chunks, use restrictions, reviewed examples and links to existing lessons. An optional current-owner filter should be labelled **In saved lesson teaching**. Do not promise 79 senses, 79 independently practisable words, mastery or new entry-specific exercises.

Initial library reading would be untracked reference exposure. Opening, searching or expanding a card would create no development session/run, support acknowledgment, attempt, summary change or cumulative admission. Explain the limit alongside exposure information. Recording reference reveals later needs its own event/replay contract; never disguise browsing as opening a lesson or backfill frozen attempt contexts.

## Existing data and required crosswalk

The engine's `PilotReviewMetadata.kt:3` entry-level lexical review has no sense ID; `PilotContentSnapshot.kt:374` retains a deliberately restricted four-noun assessment pool. Neither can be exposed as a complete learner library unchanged. Introduce a pure, separately versioned read projection and a small explicit crosswalk, independent of all generators/graders and published snapshots.

Each entry needs stable entry identity and situation membership; selected uses need exact sense identity, English meaning, active forms/bundles/chunks, restrictions, source/draft-review status, example references including required scene and relevant lesson references. Preserve existing identities instead of inferring a sense from spelling.

Acceptance examples:

- Request-only L009-S02 `have` must not inherit possession's active `har`.
- Referential L008-S02 `det` differs from fixed identification `Det er`.
- Directional `hjem` and static `hjemme` stay separate.
- `til stationen`/`på stationen` and pharmacy destinations stay complete chunks; this library supplies no productive preposition/definiteness rule.
- L032 city surfaces and the exact rail permission `skifter i Aarhus` remain scoped; vehicular `kører` retains its car context and transport `tog` its selected sense.
- Article records stay distinct from numeral uses; inactive historical past/plural reference forms remain outside the active-form display.

Use authored examples. Identify untranslated/missing editorial examples explicitly; do not synthesize sentences by combining glosses.

## Teaching evidence and ownership

Map authenticated saved introduction keys to precise entry/sense/form claims with plan-revision witnesses. P1/P2/P3/Module 1 have different key vocabularies: extracting an L-number from an arbitrary exposure key would overclaim. A visible finite form does not imply exposure to its infinitive; a displayed distractor or grammar skill ID does not mean a whole entry was taught. Missing evidence means **not recorded in saved lesson teaching**, not unknown to the learner.

An owner-context projection should read saved teaching witnesses directly. Do not invoke cumulative planning, regenerate exercises or load full attempt histories to render a badge. Preserve current fixed 24-hour ownership, expiry and isolation; no global indefinitely cached personal query. Refresh personal context explicitly and clear it on expiry/401 or owner changes.

## Implementation contract: API and UI

Selected read-only endpoints: a finite unpaged `/api/v1/vocabulary-entries` envelope with library/content revisions and safe explicit DTOs; a separately authenticated `/api/v1/vocabulary-context` for the current owner's teaching claims. For truly passive browsing, the reference endpoint needs a narrowly documented development-mode/no-owner exception, ordinary host/origin guards, no learner history and no private task keys/accepted answers/matching solutions. This narrow exception requires independent API/security review and changes no other endpoint’s authentication. No migration is currently indicated.

Add `/vocabulary`, a navigation item, four situation filters, local labelled search over supplied forms/headwords/English meanings, expandable cards and passive lesson links. Keep Danish/English markup separate, preserve case-sensitive presentation such as pronoun `I`, and preserve the existing navigation/focus/mobile/reflow conventions. Do not invent `/practice?focus=` deep links; the runner currently binds actual session IDs. Explicit starts must reuse existing creation/outbox behavior.

Donors inspected by the architect: Flow's `web/src/components/EmptyState.tsx:9` and Lettuce's `web/src/components/ListToolbar.tsx:36` are suitable small building blocks. Flow's persisted pagination/filter machinery is unnecessary for 79 finite local entries. Port useful components without adding i18n/auth/table frameworks.

## Work packages and acceptance

1. Content crosswalk: account for every entry, selected use, active surface, example and lesson; mark editorial gaps and retain teacher-review status.
2. Pure engine library projection and version-aware teaching mapper; no changes to existing generation/grading/review bytes.
3. Safe reference and owner-context routes, lightweight store/fake read contract, strict OpenAPI/generated schema and mode/auth/expiry/no-write checks.
4. UI/search/filter/details/navigation with passive browsing, owner-safe states, keyboard/focus, axe, both themes and 320px/200% reflow; preserve uncertain practice operations.
5. Independent content/API/exposure review, immutable compatibility checks, full Docker-backed/browser gates and standing versioned release/backed-up promotion workflow.

The library needs its own projection revision and the next selected minor app version. A reference-only projection does not itself require a new learning pair, evaluator, practice-plan revision or migration.

## Product choices before implementing a precursor

1. May a reference-only partial precursor ship first, leaving BL-067's accepted recognition/recall scope open, or should the first selected library milestone include that practice? New practice needs explicit populated operation pools and a separate versioned plan.
2. Should library reading remain untracked reference use, or must reveal events become recorded exposure? Recorded reveals require precise persistence/replay semantics and still grant no automatic cumulative eligibility.

The user’s positive response to the specific two-stage recommendation selects the smaller first-stage milestone as the working scope. Untracked reference browsing and all-entry visibility are its implementation defaults; neither creates teaching evidence. Future durable reveal events or targeted library practice require a separate selected contract.


## Interface direction for the selected milestone

Reuse the existing shell, bundled Inter and theme tokens: Danish red `#96192d` / dark accent `#b21f36`, light canvas `#f8f9fb`, white surface `#ffffff`, neutral border `#dee2e6`, with the existing dark counterparts. No new brand palette or typeface. Keep one left-aligned reading column within the existing lesson-page width, a labelled search/filter toolbar, a count/status line and expandable vocabulary entries. Show the article/form bundle as the Danish focal text, with the selected English meaning immediately beside or below it. Expanded details carry restrictions, authored examples and passive lesson links; the short reference status explains the teaching-history limit. Use neutral teaching badges rather than mastery colours. Shared sibling empty/search patterns are donor references; no generic dashboard figures or decorative media are needed.

```text
Vocabulary
Search Danish or English      Situation       Saved-teaching filter
Result count                  Teaching-context action/status
[ Danish bundle   English meaning                         expand ]
    taught forms / examples / use notes / related lesson links
```

This design keeps the established sibling appearance and makes Danish content the primary visual element. The reference-only first stage adds no per-entry practice controls.


## Architecture and compatibility decisions

Use pure `VocabularyLibrary` / `VocabularyTeachingMapper` projections with library revision `vocabulary-library-1`; app version is 0.24.0. Leave current learning pair twenty, all stored plan revisions and cumulative-review-3 unchanged. Library-local example IDs expose no raw task identity. Earlier lexical uses without published sense identities receive explicitly documented library crosswalk IDs, rather than pretending those IDs were assessment targets.

The server’s lightweight `PracticeStore.savedVocabularyTeaching` read checks current owner existence/expiry and reads only acknowledged introduction plan payloads in one transaction. It does not load exercise items or attempt histories, invoke cumulative planners, regenerate tasks or acquire mutation locks. The API maps explicit safe DTOs and uses a documented finite unpaged `items` envelope. Only `/vocabulary-entries` has the development-mode-gated owner-free exception; `/vocabulary-context` retains normal authentication. All API no-store and host/origin/forwarded-header checks apply.

Personal context remains fresh local UI state. An explicit read can report no authenticated session without creating one. Clear retained claims on expiry, invalid ownership and stale response races; refresh when returning to a previously loaded page. A lesson link navigates to its directory anchor and performs no learning action. Reference browsing never changes lesson, attempt, support or cumulative-review evidence. Removal of the feature needs no data rollback because no new persisted fields or rows are introduced.


## Implementation inventory and draft validation

The implemented read projection has 79 entries and 81 selected uses: L008 has separate identification/reference uses, and L009 separates possession/request. Existing typed packet/assessment sense IDs are reused; earlier uses without a published sense ID receive an explicit `<entryId>-S01` library crosswalk identity. These lookup IDs create no new assessment target. Form IDs are stable within `vocabulary-library-1`, scoped to a selected sense; public example IDs are library-local. No published learning pair or stored payload changes.

A real-plan matrix checks all 32 lesson families and both P1 plan generations. Exact-key regressions distinguish the personal introduction’s `i Odense`, the P3 rail review’s `i Aarhus`, legacy noun-bundle labels, finite-only review, and request versus possession support. Missing evidence stays absent rather than being inferred from a task example or another city/form. Learner use notes omit internal construction/format/revision identifiers.

Independent content and API/UI reviews identified and corrected false city credit, missing recorded bundles/support, a delayed-reference/context race and lesson-anchor focus. Assembled browser checks additionally found and corrected accessible-name mismatch and enlarged-heading overflow. Visual review corrected double detail padding and rounded multiline badges that could mask text at narrow widths. Final release acceptance will record the exact source/image, complete gates, browser/upgrade/no-write evidence, backup restore, preview promotion and cleanup below; this section is implementation evidence, not a release claim or Danish specialist approval.
