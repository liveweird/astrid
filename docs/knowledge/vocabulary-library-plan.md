# Vocabulary Library: Proposed Partial Precursor

Status: proposal only, based on inspected source `e015624884632d54bdc6a0aadbf933cec3601664`. No library implementation is selected by this plan. The [backlog](../../BACKLOG.md) retains BL-067 and its accepted recognition/recall scope.

## Recommended scope

This reference-only candidate is a proposed partial precursor, not completion or closure of BL-067. The accepted direction still includes owner-introduced material and populated recognition/recall practice by situation. Those requirements remain open unless the user explicitly revises them. Offering all 79 reference records is also a proposal, distinct from the accepted introduced-content scope.

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

## Proposed API and UI

Tentative read-only endpoints: a finite unpaged `/api/v1/vocabulary-entries` envelope with library/content revisions and safe explicit DTOs; a separately authenticated `/api/v1/vocabulary-context` for the current owner's teaching claims. For truly passive browsing, the reference endpoint needs a narrowly documented development-mode/no-owner exception, ordinary host/origin guards, no learner history and no private task keys/accepted answers/matching solutions. This is a proposal requiring API/security review, not a global authentication relaxation. No migration is currently indicated.

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

Architect recommendation: start with the smaller reference milestone; keep both choices explicit and await selection.
