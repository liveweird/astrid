# Vocabulary matching grid

## Authorization and boundary

**8 November 2026 — Confirmed:** the user's “go for hat” accepts the recommendation to build
word matching (#11) next. Deliver the four-pair grid, tap/keyboard operation, per-pair feedback,
safe retry and historical preservation as app **0.6.0**. Standing focused commit/merge/push,
independent review and verified private-preview delivery apply. The earlier request to version
and release properly carries forward: publish annotated `v0.6.0` and authored GitHub release
notes at the exact CI-verified master source, then promote after a verified backup.

Implementation defaults below are decisions within that scope, not additional user-selected
curriculum policy. The first board has English on the left and Danish on the right. It is
standalone E014; multiple gaps, new grammar/vocabulary, orientation reversal, course integration,
durable playground reload recovery and accounts remain separate work. Danish remains draft
pending teaching review. Guided practice and follow-ups remain pinned to pair three.

## Content and interaction

Reuse the exact four selected senses from E011: L014-S01 `book` ↔ `en bog`, L015-S01
`newspaper` ↔ `en avis`, L018-S01 `letter (written message)` ↔ `et brev`, and L024-S01
`magazine (printed publication)` ↔ `et magasin`. Extend L024's revision-six use to E014 without
changing its retained-five E011-only metadata. Other pools and all 99 sentences stay unchanged.

Shuffle both columns independently with distinct deterministic salts, then assign opaque
`l0`–`l3` and `r0`–`r3` IDs. Independent shuffles may coincidentally align a correct pair;
do not impose derangement. Public item IDs and target-list order disclose no correspondence.

Select an English button, then an available Danish button to form a pair. Show chosen pairs
with removal controls. Native Tab/Shift-Tab and Enter/Space provide keyboard operation; no drag
gesture is required. Enable **Check matches** after at least one pair. The engine grades the
whole selection in one attempt and returns feedback per pair. Immediate grading on each tap
would introduce additional completion/exposure state, so it is outside this bounded design.

Freeze selections while submitting, uncertain or displaying a confirmed result. **Try again**
retains selections, clears feedback and unlocks every pair, including correct ones. New checking
uses a new attempt ID. Uncertain retry resends the saved ID and serialized body exactly. Opening
lesson/hint records sticky support; the lesson may explain all four mappings. Label later
attempts **Retry after feedback** and retain retry counts; carried-forward answers are not fresh
independent successes. Existing standalone recovery requires keeping the page open.

## Model and public/private wire

Add `pilot-6` / `generator-6` / `evaluation-6`, retaining all five previous pairs. Request
defaults stay unchanged; catalog/new playground defaults advance to six. Freeze published five
in its own commit before production edits, including its exact teaching-review output.

All new fields are trailing optional defaults and wire `@EncodeDefault(NEVER)` so old shapes
remain byte-identical:

| Location | Addition |
| --- | --- |
| Exercise | `lexicalTargets: List<LexicalTarget>?` — four meaning/en-da targets in stable sense-ID order, unrelated to either column order |
| Prompt | `matchingColumns: MatchingColumns?` with `left/right: [{id,text}]`, four per column |
| Answer | `{kind:"matching", pairs:[{leftId,rightId}]}` |
| Evaluation/attempt | `matchingResults` — four rows in displayed-left order |
| Private inspector | `matchingSource: {pairs:[{leftId,rightId,lexicalSource}]}` — correct correspondences, guarded by the inspector key |

E014 catalog metadata uses `lexicalTask={facet:"meaning",direction:"en-da"}`. Its singular
lexical target, grammar target and reading target are absent. Prompt `cueLanguage="en"` and
`answerLanguage="da"`; existing before/after/choices/blocks/punctuation are empty. A board is not
a sentence: `completedSentence` is empty. Add linguistic scope `danish-matching`.

Each result row contains `leftId`, selected `rightId` (absent if unmatched), `lexicalTarget`,
`targetSatisfaction`, `meaningSatisfaction`, `responseConstraintStatus`, `linguisticStatus` and
`feedback`. The engine counterpart uses `targetStatus: TargetStatus`; other facets reuse their
existing enums. Never expose expected right IDs for wrong or unmatched rows. The plural target
set is public metadata; per-row target attribution is returned with feedback. No public prompt
contains a solution map or per-item sense IDs.

Engine `Answer.Matching` and `NormalizedAnswer.Matching` retain the supplied pair list unchanged.
Pair-list order does not affect grading but is part of saved request identity: reordering under
the same attempt ID gives 409; a new ID is a new attempt. Never sort/rebuild an uncertain request.

## Grading and validation

Accept a partial one-to-one selection of supplied IDs, including an empty list through the API.
Selections do not need to be correct to be valid requests.

| Pair | Target | Meaning | Constraint | Language |
| --- | --- | --- | --- | --- |
| Correct supplied match | met | met | met | acceptable |
| Wrong supplied match | not-met | not-met | met | acceptable |
| Unmatched left item | unevaluable | unknown | not-met | unknown |

For the whole board, any wrong selected pair makes target/meaning not-met. Otherwise a missing
pair makes target unevaluable and meaning unknown; all four correct makes both met. Response
constraint is met only for four pairs. Language is acceptable for any nonempty valid selection,
unknown for empty: this judges supplied Danish noun phrases, not sentence production or article
recall. Success requires all applicable whole-board facets satisfied. Feedback shows text/icons
and each server-provided row; badges use plural meanings and supplied-phrase wording.

Headers/authentication precede strict request decoding. Decoding obtains the exercise ID; malformed
JSON or field structure returns 400 independently of resource existence. Matching-kind, supplied-ID,
distinctness and pair-count validation then run after owned-exercise lookup; syntactically valid
outsider requests receive uniform 404, including invalid matching selections. Invalid attempts reserve no
attempt ID or retry number. The pure engine handles malformed answers conservatively without
manufacturing pair successes. Existing store serialization, transactional grading and stored
response replay are sufficient: no migration or store-interface expansion is planned.

## Compatibility, verification and release

Pin literal fingerprint branches for five and six. Frame every new scalar, optional presence,
target, column order/item, private lexical field and result metadata used in reproduction.
Retain five's explicit snapshot, 13 formats, 24 lexical records/four senses, provenance wording
and exact review output; add `renderPilotFiveMarkdown`, `--pilot-five` and retained export task.
Update exhaustive sealed-answer branches without changing old behavior. New six review metadata
may name E011/E014; five's metadata remains E011-only.

OpenAPI closes pair/format/target/language/cardinality and answer/result correlations. New
matching fields are absent on old presentations; matching feedback requires matching scope and
evaluation six. Regenerate types, preserve practice schemas and cover API-SEC-003,
API-ERR-005/006, API-IDEM-001 and API-DOC/CONF rules.

Acceptance includes all 209 valid partial bijections and all 24 complete matchings, pair-order
invariance with raw-order preservation, malformed/owner/retry cases, PostgreSQL cross-pool raw
answer and response readback, replay before regeneration, fingerprint mutations and drift
rejection. Retained golden/wire/review evidence and applied migration bytes are immutable.
Prove deterministic variation at seed 0/1/Int.MAX_VALUE and index 0/1/1,000,000.

Run the full Docker-backed repository gate, both-browser matrix, Axe, keyboard/focus, partial
and mixed feedback, editable retry, sticky support, exact committed/uncommitted response-loss
retry, both themes and 320px/200% rendered review. Use an owned QA stack for old→new upgrade and
actual restart/replay. Before main promotion verify a private readable backup, replace only the
app, preserve PostgreSQL process/mounts and immutable histories, then remove only owned QA
resources. Bare 0.5.0 cannot read new matching payloads; rollback after new writes needs a
compatible reader. App version/changelog/OpenAPI version move together; tag/release publication
waits for exact-master CI and remote readback. Observed delivery belongs here after execution.

## Implementation review

Pair five was captured from unchanged `06dc714` and committed separately as `ac2ed71`: engine
2,074 rows and server 2,811 rows, with pinned fixture/provenance hashes. Its retained teaching
report remains byte-exact (`0e47dcf606922fbf20a7d2354e50caa3800f6a4c35459c548196bb899b6060e0`).

Independent review found an unfinished English selection surviving regeneration when positional
IDs were reused. Scope the matching component to the exercise ID; unit rerender coverage and a
packaged regression protect the boundary. The browser regression fails on the earlier image.
API review also tightened left/right ID domains; pair uniqueness is validated after ownership.
Machine-readable answer constraints cover partial bijections; prompt uniqueness by ID is documented
and tested in generation because OpenAPI 3.0 cannot express unique-by-property with variable text.
All three independent reviews closed without remaining actionable findings. The full Docker-backed
repository gate and 14 focused Chromium/WebKit journeys pass, including a red/green regression for
the selection reset. Backend coverage measures engine 98.07% lines / 84.30% branches (branch floor
raised to 84) and server 97.95% / 79.97%; frontend floors remain unchanged. Final packaged, exact-master CI and promotion
evidence will be recorded below after completion.
