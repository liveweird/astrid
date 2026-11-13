# Cumulative Mixed Review

## Selection and scope

**12 November 2026 — User-authorized:** “OK, sounds like a plan - go for it” selects
BL-066 after the recommendation to combine previously introduced grammar and vocabulary
across lessons. Implement, independently review, verify and deliver under the standing
focused commit/merge/push and versioned release/private-preview workflow. App **0.21.0**
and plan **cumulative-review-1** are implementation assignments, separate from learning
the existing learning pairs, whose content, generation and grading remain unchanged.

Existing fixed Mixed practice, other Review sets, source-linked follow-ups and lessons
retain their contracts. The Vocabulary library and S14 remain later milestones. Food/shopping activation was
subsequently selected separately; its versioned extension is recorded below. Accounts, cross-device history, spaced scheduling, mastery thresholds,
new grammar and runtime AI generation are outside this release. Danish teaching review
remains BL-022; software verification does not confer teaching approval.

## Selected food/shopping extension — 12 November 2026

The user subsequently selected [P2 activation](vocabulary-food-shopping-activation.md)
under BL-070. App **0.22.0** advertises **cumulative-review-2**, admitting the six P2
lesson families after saved teaching, for twenty-six eligible source families. The new
version retains the eight-item cap, empty/short pools, fixed-lifetime owner boundary,
atomic creation and frozen replay. Each P2 item keeps pair nineteen and its own completion
and exposure policies; other source items keep their original graders.

Retained **cumulative-review-1** keeps its original twenty-family eligibility, including
new explicit version-one requests. Later P2 teaching cannot change a saved selection or
expand that old contract. Its published engine and synthetic wire archives were frozen
before production changes. Actual upgrade/restart and release observations belong in
the P2 activation record after verification; this section records the selected contract.

## Eligibility and ownership

Use only the authenticated current development session's stored evidence. Its existing
fixed 24-hour lifetime applies; browser storage alone never proves ownership or restores
another session's history. Explain this scope in the learner UI.

The first version pools material from the **twenty taught lesson profiles**: fifteen
Module 1 sessions and five work/study sessions. A saved teaching introduction is required;
completion, a correct answer and a mastery score are not. Active source lessons can
contribute after teaching is saved. Merely creating a run, seeing choices, supplying a
correct answer or opening a skill ID does not unlock the rest of that skill's vocabulary.

The three older Review sets and targeted follow-ups do not grant candidate eligibility.
Their introductions do not consistently teach every generated item; broader admission
requires a reviewed coverage map. Actual released prompts and support remain exposure
evidence rather than an assertion of learning.

Keep candidates bounded to the supplied items and material of eligible source lessons.
Deduplicate repeated lessons and semantic task/operation identities. Do not promote an
entire grammar skill, or sample arbitrary later vocabulary because a target ID matches.
Sources retain explicit revisions and eligibility witnesses. Legacy P1 revision-one
material maps by stable semantic task identity to pair eighteen; it does not reuse an
old index and assume that an expanded pool has identical meaning or support.

`sourceLessonCount` counts distinct eligible lesson focuses with usable candidates,
normalizing retained/current P1 revisions together. Reduce repeated source runs to a
finite candidate/witness set; do not forget older teaching through an arbitrary last-N
history cutoff. Frozen exposure keeps relevant witnesses rather than copying raw history.

## Selection and variability

New focus **cumulative-review** selects at most eight distinct eligible activities.
An empty pool offers a lesson and creates no run. A pool smaller than eight produces a
shorter run with its actual count. Never pad a run with duplicate tasks or untaught items.

Balance available source lessons, primary targets and operations, using seeded ordering
with stable scalar identities. Prefer eligible examples not recently released and avoid
adjacent repeated formats/targets when alternatives exist. Preserve an explicit semantic
identity so shuffling choices alone does not count as a new example. The finite existing
pools limit novelty: fixed work/study texts can recur, and the UI must not promise every
run contains new sentences. No broader linguistic generation is selected.

Regeneration retains each source exercise's literal learning pair and matches reviewed
task, sense or canonical sentence identities plus full private/support constraints.
If a same-pair variant cannot preserve those semantics, reuse the verified original
recipe with a fresh exercise UUID. Source indices are not portable across pools.
P1 seventeen-to-eighteen is the sole existing exception: validate authored-task/support
equivalence and rebuild matching associations for the stable board. Preserve assessment,
support, completion and per-operation exposure semantics.

**Superseded design:** initially rebasing every task to pair eighteen appeared sufficient.
Inspection found that retained present-form tasks have narrower known-alternative sets;
pair eighteen would change unknown answers into known wrong answers and alter continuation.
Preserving source graders prevents that change. A cumulative run is consequently composite:
its three top-level revision strings are the literal marker `per-item`, and every public
item has `revisions` containing its actual content/generator/evaluation triple. These
markers are not an engine pair and must never enter engine dispatch or fingerprinting.
Existing practice items omit the new optional field; existing run headers keep their meaning.

## Persistence, support and results

Reuse the existing practice creation and answer outboxes. The browser saves the UUID,
owner and exact serialized request before sending. New creation uses the unchanged
`POST /api/v1/practice-sessions` shape with the new exact focus/plan pair.

Within one owning-session lock and transaction, replay an existing creation identity
before querying history. Otherwise read eligibility, select activities and save the
complete plan, exercises, source references, selection-policy revision and relevant
cross-run exposure snapshot atomically. A later history change cannot replan a saved run.
An empty pool returns a useful 409 without inserting a partial run or orphan exercises.

Persist optional per-item completion/exposure policies, including an explicit policy
object when its source policy values are null. Resolve them consistently for summaries,
advance, attempt hooks, feedback exposure and both store implementations. New fields are
omitted from old payloads; all published bytes and evaluator branches remain executable.
No applied migration or golden fixture may change.

The strict stored JSON reader makes an older 0.20.1 binary unsuitable for direct rollback
after new cumulative metadata is written, despite the unchanged schema and learning pair.
Rollback requires a compatible reader; restoring a backup is a separate explicit data-loss
decision, not an automatic release step.

The cumulative introduction reuses selected source teaching sections, deduplicated and
marked with the correct content language; save it before assessment. Available lesson
and hint support remains explicit. Introduction exposure keys come from those actual
source introductions rather than assuming lesson/hint keys were shown there. Active
source prompt exposure includes only positions already released at or before its cursor.
Summary rows retain scope, operation, lexical sense,
reading role and matching-pair distinctions. Preserve first/latest/unknown outcomes and
possible prior exposure separately; recognition is not written recall or mastery.
Existing single-source follow-up planners do not accept the new cumulative plan.

## Learner flow and API

Lessons/Review browsing remains passive. An explicit **Prepare mixed review** action
establishes/reuses the development session and freshly reads authenticated
`GET /api/v1/practice-review-context`. It returns `ownerSessionId`, `sessionExpiresAt`,
`focusId`, `planRevision`, `title`, `itemCount` (0–8) and `sourceLessonCount`.
This GET creates no exercises or practice run. Do not cache eligibility across owners.

The preparation count is advisory; creation rechecks history authoritatively. Revalidate
the owner before saving a new creation. Preserve resume/start-new/cancel choices and any
existing uncertain outbox. Reuse the runner's URL binding, support, retry and exact replay.
Repeat returns to fresh cumulative preparation rather than silently starting a fixed set.

## Acceptance and delivery evidence

Required checks cover empty/short/full pools, taught versus merely created sources,
active lessons, repeated/legacy sources, owner isolation/expiry, stable selection at
seed boundaries, finite novelty and explicit scoped summaries. Verify per-item policy
selection, first/latest unknowns, source-exposure provenance and no answer-key leakage.

Fake/Postgres contracts must agree. Prove atomic rollback, concurrent owner serialization,
creation replay before changed history and actual restart recovery with pending first
grades and saved attempts. API reviews cite the shared stable rule IDs; new operations,
statuses and closed focus/plan correlations enter the authoritative OpenAPI and generated
client together. Check header/auth/ownership ordering and negative request matrices.

Web/browser acceptance includes passive navigation, explicit fresh preparation, empty and
short pools, storage failure before sending, stale owners, lost creation/answer responses,
resume, repeat and summaries. Inspect both themes, keyboard/focus, 320px/200% text and axe
for new states. Run the full Docker-backed gate, existing immutable compatibility guards
and both browser projects. Verify exact-source CI, tag/release, actual isolated backup
restore, backed-up app-only promotion preserving history/database, smoke and owned cleanup
before claiming delivery. Record observed outcomes here when complete.

## Observed delivery

**12 November 2026.** Released app [0.21.0](https://github.com/liveweird/astrid/releases/tag/v0.21.0), source `eab86c6`, after all five
[exact-master CI jobs](https://github.com/liveweird/astrid/actions/runs/43307108048) passed, including **492 packaged-Linux
Chromium/WebKit journeys** without retries or flakies. All **492 native journeys**
also passed against the exact committed candidate. The full Docker-backed local gate
passed **434 engine tests**, the fresh Arabic-locale suite, **293 server tests** and
**298 web tests**, with no skipped tests. Independent architecture, engine/API/UI and
release-operation reviews closed their findings.

Cumulative preparation is explicit and passive browsing stays unchanged. Review draws
from the twenty taught lesson families in the current fixed-lifetime development session,
with up to eight unique activities and honest empty/short pools. Actual released legacy
review material contributes freshness/exposure evidence without becoming an eligibility
source. Results retain scopes, operations, senses and reading/matching distinctions.
The static catalogue remains twenty lessons plus three unchanged fixed Review sets.

An actual candidate upgrade kept six old saved-run responses and two saved attempts
byte-identical. Saved cumulative selection remained unchanged while additional teaching
expanded current eligibility. After a real QA process restart, all eight mixed activities
first-graded and replayed using their actual original evaluation revisions; three old
pending exercises also first-graded at retry one and replayed. The saved private plan,
new exercise rows and all previously captured rows stayed unchanged. A separate six-item
S05 review retained `hedder` as unknown under evaluation ten, rejected continuation,
replayed that answer and accepted the correct second attempt.

The exact image passed the unchanged HIGH/CRITICAL vulnerability policy and runtime
package floors with networking disabled. A **6,458,396-byte**, mode-0600 backup was
actually restored into an isolated owned database and compared before app-only preview
promotion. All six learner tables were exactly equal immediately across promotion;
PostgreSQL process/mounts and Flyway versions 1/2/3 were preserved. All **196 live-preview
checks** passed without retries/flakies after correcting a test synchronization race;
owned QA resources were removed while main
processes/mounts stayed unchanged.

The first live smoke had 195 passes and one test race: a direct state GET began 1 ms
before creation POST. The app then rendered its teaching screen correctly. The test now
waits for that visible creation confirmation before inspecting state; the failed trace
is retained separately and the full corrected smoke passed with retries disabled.
Application code and the published release image did not change.

All 115 pinned compatibility/migration/legacy resources retained their original bytes.
Engine/server floors stayed 98/86 and 98/82; web branch coverage crossed 90%, so its floors
ratcheted to 95/90/98/98. No migration, new linguistic content, new engine pair, account,
scheduler or mastery policy was added. Direct old-binary rollback after cumulative writes
still requires a compatible reader. BL-066 is complete; learner review remains BL-035,
Danish teaching review remains BL-022, P2/P3 stay unactivated and S14 stays paused.

## Version-two delivery — 13 November 2026

App 0.22.0/source `b3fcbb4` delivered the selected six-family P2 extension. The
[P2 delivery evidence](vocabulary-food-shopping-activation.md#observed-delivery--13-november-2026)
records exact-source CI and 530 native/530 Linux journeys, real restart with original
per-item graders, frozen creation replay while eligibility grows, retained version-one
exclusion, backup restoration, promotion and 234 live checks. New preparation admits 26
taught lesson families; retained cumulative-review-1 stays at twenty. These are introduced
material pools within the existing owner lifetime, not mastery or unrestricted generation.
