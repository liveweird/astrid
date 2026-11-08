# Multiple-gap exercises

## Authorization and scope

**9 November 2026 — Confirmed:** “Go for it” accepts the recommended next milestone: multiple-gap
exercises (#10), initially two gaps, an explicit grammar focus, related choices for each gap,
per-gap feedback, editable retries and exact replay after connection loss. Reuse the current
Danish vocabulary and licensed sentence structures in the standalone playground. Deliver app
**0.7.0** under the standing focused commit/merge/push, independent review and proper release
workflow, including exact-source CI, annotated tag/GitHub Release and backed-up app-only preview
promotion. Guided integration is the following proposal and is not selected by this milestone.

**Confirmed timing constraint:** continue authorized work until **07:30 Europe/Warsaw on
9 November 2026 (06:30 UTC)**, leaving the laptop ready for the user's 08:00 shutdown. Do not
initiate OS shutdown. Reserve time for checks, safe completion records and removal of owned QA
resources. If release gates cannot finish in time, retain the existing preview and record the
verified state and remaining work. Related hardening is within scope; other backlog features
need a separate selection.

## Assessment boundary

Implementation decision within the authorized scope: the 99 existing licensed clauses each contain one finite
verb, and 92 contain an article. Start with those 92 complemented clauses, using G004 present
verb as the primary target and G006 article as explicitly labeled supporting grammar. Preserve
separate evidence for each slot; an article error must not make the primary verb target fail.
Overall success also requires the supporting answer and requested response to be satisfied.
The UI must name both roles. This avoids silently introducing new sentence constructions or
a two-sentence task to manufacture repeated instances of one grammar topic.

## Selected content and presentation

E015 is **Choose a present verb and article**, with `targetSkillId="G004"` and supporting IDs
`G001/G002/G003/G005/G006`. Both gaps have visible roles: **Present verb — Primary target** and
**Indefinite article — Supporting grammar**. Eligible sentences are exactly the existing 92:
56 with `have`, 21 with `læse`, seven with `spise`, eight identifying `Det er …` sentences.
Do not add `magasin` or expand any existing sentence/lexical pool.

Examples use only existing licensed clauses: `Jeg [læse/læser] [en/et] bog.`,
`Jeg [have/har] [en/et] brev.`, `Det [være/er] [en/et] hus.`. This tests finite present versus
infinitive, not tense selection. Article choices use the governed noun's stored gender.

A deterministic 92-sentence cycle visits all eligible clauses before repetition. Independently
shuffle each gap's two related forms with distinct salts, then assign local opaque `c0/c1`
choice IDs. Slot IDs are positional: `g0` verb, `g1` article. A choice's identity is the
composite `(slotId, choiceId)`; `c0` in both slots is valid and can denote different forms.
No expected IDs or correct-answer mapping appear in the public prompt.

## Engine and wire models

Append optional nullable fields; wire additions use `@EncodeDefault(NEVER)` to retain old bytes.

| Location | Addition |
| --- | --- |
| Prompt | `gapPrompt: GapPrompt?`, containing exactly three `segments: List<String>` and two `slots: List<GapSlot>`; interleave segment 0, slot 0, segment 1, slot 1, segment 2 |
| Slot | `{id,label,targetSkillId,assessmentRole,choices:[{id,text}]}`; each has exactly two choices |
| Answer | `Answer.Gaps` / `NormalizedAnswer.Gaps` with `selections: List<GapSelection>`; each selection is `{slotId,choiceId}` |
| Evaluation | `gapResults: List<GapResult>?` — two rows in displayed slot order |
| Inspector | `gapSource: GapSourceDetails?` — correct IDs and lexical/form provenance, key-guarded only |

Use `GapAssessmentRole.PRIMARY("primary")` / `SUPPORT("support")`. `g0` is G004/primary;
`g1` is G006/support. `GapChoice` has `id,text`. Existing prompt `before`, `after`, `choices`,
`blocks` and `punctuation` are empty; segments carry the actual sentence and punctuation.
`cueLanguage` and `answerLanguage` are `da`; lexical/reading targets are absent.

Public DTOs are `GapPromptDto`, `GapSlotDto`, `GapChoiceDto`, `GapSelectionDto`, `GapResultDto`.
The answer discriminator is `AttemptAnswerDto.Gaps`, `@SerialName("gaps")`:
`{kind:"gaps", selections:[{slotId:"g1",choiceId:"c1"},{slotId:"g0",choiceId:"c0"}]}`.

`GapResult` contains `slotId`, selected `choiceId` (nullable/absent if missing), `targetSkillId`,
`assessmentRole`, `targetStatus`, `responseConstraintStatus`, `linguisticStatus`, `feedback`.
Only `targetStatus` is renamed to wire `targetSatisfaction`. Add scope `DANISH_GAPS("danish-gaps")`.
Meaning is not assessed: omit `meaningSatisfaction` from the whole attempt and individual rows.

Private `GapSourceDetails(slots: List<GapSlotSourceDetails>)` rows have `slotId`,
`expectedChoiceId`, `lexicalId`, `expectedForm`, `alternativeForm`. For g0, lexical ID names the
verb; for g1, it names the governed noun. Server private DTOs are `GapSourceDto` and
`GapSlotSourceDto`. Preserve existing inspector canonical sentence/ID/construction/lexical IDs.
`AnswerKey.Gaps(expectedBySlotId: Map<String,String>)` is derived from those source rows;
`MultipleGapExerciseSource(sentence,answerKey,blockRoles=emptyMap())` holds the private key.

## Grading and recovery

| Selected slot form | Slot target | Slot constraint | Slot language |
| --- | --- | --- | --- |
| Correct supplied form | met | met | acceptable |
| Wrong supplied form | not-met | met | known-issue |
| Missing | unevaluable | not-met | unknown |

Whole target satisfaction is only g0's status. Whole constraint is met only when both slots are
answered. Whole language is known-issue if either selected form is wrong; otherwise unknown if
incomplete, acceptable when complete. These rules cover all nine missing/correct/wrong pairs.
Primary correct/support wrong therefore means target met, language known-issue, constraint met;
primary correct/support missing means target met, language unknown, constraint not-met.

“Good work” requires target met, acceptable language and constraint met. Do not require a meaning
field for this scope or convert support mistakes into G004 failures. Feedback uses existing
`INFINITIVE_USED` and `ARTICLE_GENDER_MISMATCH` diagnoses and explicitly names each role.

For complete valid selections, `completedSentence` reproduces the learner's selected words,
including diagnosed errors. Partial/malformed responses use an empty wire string. Never fill
missing answers or replace selected wrong forms with canonical corrections.

Allow zero, one or two valid selections in the API; enable UI checking after one. A slot may
occur only once; each choice must belong to its scoped slot. Freeze controls while pending,
uncertain or displaying feedback. **Try again** retains both selections, unlocks both, and
focuses the gap task. Both choices can be changed/cleared, including previously correct ones.
New checking uses a new attempt ID; **Check the same attempt** resends the exact saved ID/body.
Keep supplied array order in raw and normalized evidence. Grading ignores that order; changing
it under the same saved attempt ID is 409. No sorting or rebuilding of uncertain requests.

Support is sticky for the exercise. Later attempts show **Retry after feedback**; retained
choices are not new independent evidence. Reset selections/local prompt state on a new exercise
ID. Use native keyboard controls, English chrome and Danish content language tags; respect
feedback focus, both themes, and 320px/200% text including each card's text-range bounds.
Existing playground recovery requires keeping the page open; durable reload recovery is outside
this milestone.

## API, persistence and compatibility

Preserve existing headers/authentication, strict decoding and generic validation. The store
returns an owned stored attempt before regeneration; fresh grading regenerates/verifies the
owned exercise before gap-specific semantic validation. Syntactically valid outsider requests
return uniform 404, including invalid selections. Invalid owned gap kinds/IDs/duplicate slots/
excess selections return 400 without reserving attempt identity or a retry number. Strict JSON
syntax/field errors return 400 after authentication, independently of resource existence.

Malformed pure-engine answers conservatively return unevaluable rows, unknown language,
constraint not-met and no completed sentence; retain raw input without manufacturing success.
The existing transaction and JSON payload stores suffice: no migration/store-interface expansion.

OpenAPI closes E015/G004/pair-seven/GapPrompt and slot/skill/role correlations, per-row status/
constraint/language/choice-presence, and gapResults/danish-gaps/evaluation-seven. Gap attempts
omit meaning fields; all older shapes omit gap fields. Practice schemas stay unchanged. Accept
all nine semantic selections and thirteen ordered arrays; reject duplicated slots even with
different choice IDs. Four full-assignment branches cover all partials using the existing outer
composed `allOf` pattern (this validator ignores `anyOf` attached directly to a typed array).
Response uniqueness/order by slot property is also checked in generation/HTTP tests.
Cite API-SEC-003, API-ERR-005/006, API-IDEM-001, API-DOC-001/002, API-CONF-001/002 in review.

Add `pilot-7` / `generator-7` / `evaluation-7` and explicit retained-six snapshot/constants/
format and evaluator branches. Request defaults remain pair one; guided plans/follow-ups remain
pair three. Add a literal seven fingerprint branch over unchanged six framing plus every new
segment/slot/role/choice/order/optional presence/private source field. Unknown pairs still fail.
All six older pairs remain executable. Retained-six review must keep its 24 records, 99 clauses,
matching inventory and L024 “E011 and E014 only” wording. Add `renderPilotSixMarkdown`,
`--pilot-six`, `exportPilotSixReview`; append gap inventory only to seven.

Pair six was frozen from unchanged `6d1c0fe` in separate commit `889a9b4` before production edits:
engine 2,365 rows (`c027bf9d370a0e2974f02120cca98e953820daf9b8c8e5699bc29c563b8636f8`),
server 2,852 rows (`8cc606452d0eea26d39476a6b58d04b7c7aaef4b65fe16292583cea5a0ab59df`).
Its exact report SHA is `f6e95d88a629b162745e93b4952f87baca43624a82f12efca407d3ec0c61bab7`.
All golden/provenance bytes and applied migrations remain immutable.

## Verification and release

Prove the complete 92-clause cycle, fixed examples, boundary seeds/indices, varying independent
choice positions, nine outcome states and all thirteen ordered arrays. Cover owner/malformed/
retry matrices, replay before regeneration, fresh-grade drift rejection, every nested fingerprint
mutation, PostgreSQL cross-pool ordered raw-answer/result readback, and actual app restart.
Retain all six older golden/wire/review guards and practice payloads.

Verify primary-correct/support-wrong and support-missing remain non-success with no meaning badge;
keyboard/focus, changing/clearing both slots, sticky support, exercise reset, committed/uncommitted
response loss, both themes, Axe and local text clipping. Run the complete Docker-backed gate,
both-browser matrix, current/retained review exports, exact-image runtime/vulnerability checks,
owned-stack upgrade/restart, readable private backup and app-only promotion preserving all
immutable histories and PostgreSQL process/mounts. App version/changelog/OpenAPI version agree
in one commit. Publish annotated v0.7.0 and authored notes only at exact-master green CI; read
remote release/tag state back. Bare 0.6.0 cannot read new gap payloads; rollback after new writes
requires a compatible reader. Record observed delivery below after execution and remove only
owned QA resources by the cutoff.

## Pre-release verification — 9 November 2026

The complete local gate passed with 140 engine tests (also under the fresh Arabic-locale task),
128 server tests including PostgreSQL integration, and 174 frontend tests. Engine coverage is
98.009% lines / 84.145% branches; server coverage is 98.072% / 80.420%. Raise the server floors
to 98/80; engine and frontend floors remain unchanged. Frontend coverage is 94.98% statements,
88.67% branches, 97.70% functions and 97.67% lines.

Independent engine/API/UI reviews found no remaining actionable defects. Review corrections
added independent licensed-form oracles across all 92 clauses, all nine semantic states and
thirteen ordered arrays, validated all nine real HTTP outcome bodies through OpenAPI, and
aligned visible clear labels with accessible names. Sixteen focused Chromium/WebKit journeys
passed without retries, including both response-loss boundaries and both themes at 320px/200%.
An independent rendered review of representative working-image states found no actionable
visual defects. These checks do not constitute Danish teaching approval.

Current review SHA-256 is `0a1caa4c367e8408ac8e35cbffa0249b524493d5c4122c8361be8081b4a3d0d2`;
retained-six review remains byte-identical. Final clean-image matrix, exact-master CI,
release/tag readback, app-only promotion, actual backup restore, live smoke and owned cleanup
remain release gates. The backup will be restored only into a newly created database on the
owned QA PostgreSQL container, compared against immutable main-preview records, and removed
before promotion. Main PostgreSQL is neither restored nor restarted.

The first clean candidate, `371939f`, exposed a real layout regression in the complete native
matrix: 282 journeys passed, but both browsers measured 862px document width in an 844×390
window at 32px text when reopening the E015 chooser. Its dropdown stayed inside the viewport;
the underlying gap grid still used two columns because its breakpoint followed window width,
while enlarged navigation left only 264px of content. The correction uses the existing playground
container query to stack gap cards at 30rem of available content and permits long role labels
to wrap. It preserves desktop columns and does not conceal overflow. Recheck the unchanged
landscape regression and complete matrix before release; do not publish this first candidate.
