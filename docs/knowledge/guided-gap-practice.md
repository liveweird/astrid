# Guided Multiple-gap Practice

## Authorization and scope

**9 November 2026 — Confirmed:** the user's “Go for it” selects BL-050 after the recommendation
to bring multiple gaps into a bounded guided session: an introduction and worked example,
separate verb/article evidence, summaries and targeted follow-up, with existing saved sessions,
editable retries and exact connection-loss recovery preserved. Deliver a reviewed, tested,
versioned release under the standing focused commit/merge/push and backed-up private-preview
promotion workflow. App **0.8.0** is the implementation version default.

The existing 24 lexical entries and licensed sentences suffice. Full Module 1 activation,
additional exercise families, new language, scheduling and mastery claims remain separate work.
The earlier overnight cutoff belonged to the completed 0.7.0 milestone; it does not establish a
new overnight work period or shutdown deadline here. Danish material remains a teaching draft.

## Compatibility and design boundary

Keep `pilot-7/generator-7/evaluation-7` unchanged. Add an explicitly selected practice profile
and plan revision; preserve `practice-1`, `present-verbs-1` and `follow-up-1/2` on pair three.
Persist all new plan/target/exposure metadata in immutable snapshots and omit new optional
fields from older payloads. No SQL migration is expected within the existing one-to-eight-item
bounds. Freeze pair-seven compatibility evidence from unchanged published source before
production edits, in a separate commit.

E015 always assesses a primary present verb and a supporting article. Summary rows must use
the corresponding slot results, while session exercise/attempt totals count each response
once. Missing or wrong articles cannot turn a correct primary verb into a verb failure.
Retain the whole-response result separately: a partly correct response is not overall success.

Follow-up must assess only the selected target(s). Existing E001 can test verb selection;
E002 supplies an article in writing. If used for article follow-up, explicitly explain that
operation transition and keep article selection and recall evidence distinct. Do not silently
use E015 for a single-target review, since it would assess the unselected target as well.

## Plan and target identities

Implementation defaults: add **Verbs and articles together**, focus `verb-article-gaps`, plan
`verb-article-gaps-1`, with four E015 items pinned literally to pair seven. Deterministically
rotate `have/læse/spise/være` and select one distinct licensed clause for each, retaining its
original generation coordinates. Do not include intransitive `sove` or new vocabulary.

| Assessment | Skill / mode | Role / slot | Review key |
| --- | --- | --- | --- |
| Present verb selection | G004 / selection | primary / g0 | `grammar:G004:selection` |
| Indefinite article selection | G006 / selection | support / g1 | `grammar:G006:selection` |
| Follow-up verb selection (E001) | G004 / selection | primary / absent | `grammar:G004:selection` |
| Written indefinite article recall (E002) | G006 / recall | primary / absent | `grammar:G006:recall` |

`follow-up-3` contains four items, alternating E001/E002 when both targets are selected.
Explain **Articles will be supplied in writing** before creation. Accept only targets assessed
in an owned completed new-profile or revision-three review source; G006 selection maps to G006
recall, and recall remains available in descendant reviews. Reject empty/duplicate/unassessed
targets, unsupported source profiles and incompatible revision pairs. Exclude every immediate
source sentence and earlier child sentence. Keep source outcomes and lineage immutable.
Legacy `grammar:G006` and earlier review behavior retain their meanings.

## Snapshot and summary contract

Append `PracticeAssessment(slotId?, targetDescriptor, assessmentRole, answerExposureKeys)` to
practice items/source items as an empty-default list. Preserve the legacy primary descriptor.
Private immutable item payloads store all fields; public descriptors expose only slot, label,
skill, mode and role. Add nullable, default-omitted `introductionExposureKeys` to the plan snapshot
and optional summary `assessmentRole`. Older empty/null metadata uses the exact legacy path.

The summary consumes saved per-slot `GapResult` values and verifies slot/skill/role identity.
It projects each new assessment separately, retaining first/latest results, retries, support
and missing-slot uncertainty; whole-run counters still count each exercise/answer once. A
correct verb with a wrong/missing article can fully meet the verb row without meeting the
whole exercise. Single-target review items use their saved whole-attempt result. Read saved
plans/results without regeneration or regrading. Active runs withhold target summary rows.

## Teaching support and exposure

Use a bounded optional introduction with an English explanation, Danish morphology and the
existing worked example `Jeg læser en bog.`, then explain the separate primary/supporting
roles. Preserve `bodyLanguage` through mapping/rendering. Save support before displaying it;
keep the existing support focus and sticky evidence behavior.

The introduction's explicit exposure list covers only rendered material:
`verb-infinitive:L011`, `verb-present:L011`, `article:L014`, `headword:L014-S01`,
`grammar:G004`, `grammar:G006`. A new explicit list is authoritative; null retains the legacy
union-of-lessons behavior. Each assessment's answer keys identify its actual verb form or
noun article. Supplied prompt choices are possible exposure, not proof of association.
Generic hints disclose concepts; lessons disclose their actual forms.

One shared pure feedback-exposure helper serves PostgreSQL, the fake store and child source
snapshots. E015 correct **and wrong** feedback reveal that slot's expected form; missing-slot
feedback does not. Release a form key only after a saved nonmissing result for that slot.
New E001/E002 review feedback releases its answer form only after a target-met result, since
wrong feedback does not disclose the correction. Preserve legacy filtering. Filter frozen
within-run/source exposure to each assessment's answer keys for its summary count, keeping
support flags unchanged. Later support cannot rewrite old contexts or child ancestry.

## Transport, browser and release acceptance

Extend the existing profile/create/follow-up and practice-response schemas; add no endpoints.
Correlate exact focus/revision/pair, slot/skill/mode/role and supported exercise formats.
Keep public answer keys private, existing guard/status ordering, owner binding, transaction
lock order, creation replay before planning and attempt replay before regeneration.
New optional stored/wire metadata uses omitted defaults, retaining historical bytes.

Reuse the gap prompt and feedback. Allow checking after one choice, retain selections on
editable retry, focus the gap board and clear only on a new exercise. Restore ordered choices
from saved answers and every pending recovery path. Persist the original identity and serialized
body before sending; replay those exact bytes after reload or response loss, without sorting.
Existing owner-expiry, storage-failure, chooser cancellation and multi-tab rules still apply.

Acceptance includes all nine missing/correct/wrong slot combinations; first/latest counts;
per-assessment exposure; all selected review subsets and chains; deterministic source exclusion;
auth/ownership/identity conflicts; PostgreSQL round-trip/rollback; actual owned app restart;
committed/uncommitted response loss; both themes, keyboard/focus, Danish language tags and
320px/200% plus short landscape reflow. Preserve all seven exercise archives, applied migration
bytes and legacy plan/request/summary/context wire evidence.

Run Docker-backed `./scripts/check.sh`, full native and exact-master Linux browser suites,
review exports, exact-image isolation/vulnerability checks and independent implementation and
rendered reviews. Release annotated `v0.8.0` and authored GitHub notes only at verified master;
perform a private readable backup and actual isolated restore before app-only preview promotion.
Preserve old immutable rows and main PostgreSQL process/mounts, then remove only owned QA.
New strict JSON fields require a compatible rollback reader even without a SQL migration.
Verification and observed delivery remain pending.

## Compatibility capture — 9 November 2026

Before production edits, commit `3635b5f` froze pair seven from unchanged published baseline
`8138855`. The CREATE_NEW exports wrote 3,675 engine rows
(`fe25485c618f3906ad25a8986779e45e58a8c9b0f910a69eb73651278db75393`) and 3,134 wire rows
(`20d5e590db0c7f1c26e8a8ebfdf93f9dc9b6f5df3dee88e6020e9b2754bf4a51`). The current review
remains `0a1caa4c367e8408ac8e35cbffa0249b524493d5c4122c8361be8081b4a3d0d2`.
The complete Docker-backed local gate passed before the freeze commit. Two new archive-helper
issues were corrected without changing captured bytes: an exporter was split to meet the
existing complexity limit, and the guard counts valid ordered arrays separately from deliberately
malformed cases. No production behavior or earlier evidence changed.

An owned QA stack starts from the exact released 0.7.0 image. Private baseline evidence includes
all seven evaluator reads/results/replay, original mixed practice, active verb practice,
completed verb summaries, both legacy follow-up revisions and immutable attempt contexts.
Upgrade/restart comparison against these snapshots remains a delivery gate. Main preview
services and their database have not been changed by this capture.

## Pre-release verification — 9 November 2026

Current software checks pass 159 engine tests (also in a fresh Arabic-locale JVM), 134 server
tests including PostgreSQL, and 185 frontend tests. Measured coverage: engine 98.008% lines /
84.860% branches; server 98.095% / 80.355%; frontend 95.06% statements / 89.11% branches /
97.73% functions / 97.73% lines. Engine/server floors remain unchanged; frontend floors rise
to 95/89/97/97. All seven review exports run, and pair-seven review bytes remain identical.

Independent engine, API and UI reviews have no remaining actionable findings after corrections:

- Follow-up source validation now matches the exact exercise/slot/skill/mode/role matrix,
  rejecting forged or mismatched frozen descriptors rather than inventing source assessment.
- Response schemas close plan/revision/pair, nested legacy/new item metadata, E015 assessment
  presence/uniqueness and four-item child bounds. Negative response mutations guard those rules.
- Guided feedback receives its current exercise and displays both slot cards. Summary grids
  auto-size to available card width, fixing internal overlap at enlarged portrait/landscape sizes.
- Support readback preserves fresh choices and edited retries when the exercise and saved last
  attempt are unchanged; a concurrent grade or advance still restores authoritative state.
- Successful automatic replay clears uncertainty; a late follow-up owner read cannot persist
  an abandoned child after unmount/navigation. Regressions cover both paths.

All 28 focused native Chromium/WebKit journeys pass without retries, including per-slot cards,
select-then-support, editable retry support, partial outcomes, selected-only reviews/chains,
committed/uncommitted answer/creation/follow-up loss, both themes, keyboard and 320px/844px
layouts at 200% text. Tests now check text-range bounds inside individual paragraphs/columns,
since Axe and document overflow alone missed internal overlap. Independent visual review sampled
eight corrected views from 36 captures and closed the reported defects; earlier captures remain
private evidence. The isolated working upgrade preserves exact old wire/replay for all seven
evaluators, original practice, verb summaries and both legacy review revisions.

These are working-candidate checks, not release/teaching approval. Final full repository gate,
clean committed-image full browser matrix, exact-master CI, runtime/scan/restart proof, tag and
GitHub Release, actual backup restore, app-only promotion, live smoke and owned cleanup remain
delivery gates. Record their observed results below only after execution.
