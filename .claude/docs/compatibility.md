# Compatibility: revisions, frozen evidence, golden fixtures

Astrid stores what a learner saw and answered, and promises that anything it ever published still replays and regrades
exactly as it did. Behaviour that changes is a **new revision**; old revisions stay executable. This file is binding for
any change in `engine/`, `server/.../exercises/`, `server/.../practice/` or stored payload shapes. Long-form record:
[learning engine](../../docs/knowledge/learning-engine.md), [writing/reading contract](../../docs/knowledge/writing-reading-contract.md)
("freeze before editing" procedure and rollback boundary), [vocabulary assessment](../../docs/knowledge/vocabulary-assessment.md),
[practice sessions](../../docs/knowledge/practice-sessions.md), [runtime recovery](../../docs/knowledge/runtime-recovery.md).

## Revision vocabulary

Three strings travel together; a **revision pair** is `(contentRevision, generatorRevision)` and maps to one evaluation revision
(`PilotEngine.evaluationRevision`). Constants live in `engine/.../PilotContent.kt`, the supported list in
`PilotEngine.SUPPORTED_REVISION_PAIRS`.

| Pair | Evaluation | Formats | Status |
| --- | --- | --- | --- |
| `pilot-1` / `generator-1` | `evaluation-1` | E001–E003 | published, frozen |
| `pilot-2` / `generator-2` | `evaluation-2` | + E004–E006 | published, frozen |
| `pilot-3` / `generator-3` | `evaluation-3` | + E007–E008 (vocabulary) | published, frozen; legacy guided plans and follow-up-1/2 stay pinned here |
| `pilot-4` / `generator-4` | `evaluation-4` | + E009 (writing), E010 (reading) | published, frozen |
| `pilot-5` / `generator-5` | `evaluation-5` | + E011 (Danish word choice), E012/E013 (all/spare word tiles) | published, frozen |
| `pilot-6` / `generator-6` | `evaluation-6` | + E014 (four-pair vocabulary matching) | published, frozen |
| `pilot-7` / `generator-7` | `evaluation-7` | + E015 (primary present-verb/supporting-article gaps) | published, frozen; guided-gap plans stay pinned here |
| `pilot-8` / `generator-8` | `evaluation-8` | + E016–E019 (scoped subject-word and stored-present selection/recall) | published and frozen; three first-step plans pin this pair |
| `pilot-9` / `generator-9` | `evaluation-9` | + E020/E021 (singular inanimate subject-reference selection/recall) | published and frozen; four-item S04 plan pins this pair |
| `pilot-10` / `generator-10` | `evaluation-10` | + E022/E023 (named present forms), E024/E025 (subject-first assembly) | published and frozen; S05 and S06 pin this pair |
| `pilot-11` / `generator-11` | `evaluation-11` | E022/E023 add scoped possession tasks | published and frozen; S07 pins this pair |
| `pilot-12` / `generator-12` | `evaluation-12` | E022/E023 add scoped name, residence and language tasks | published and frozen; S09 pins this pair |
| `pilot-13` / `generator-13` | `evaluation-13` | + E026/E027 bare infinitive after supplied ability modal | published and frozen; S10 pins this pair |
| `pilot-14` / `generator-14` | `evaluation-14` | + E028 contextual modal meaning and E025 modal construction | published and frozen; S11 pins this pair |
| `pilot-15` / `generator-15` | `evaluation-15` | + E029 supplied core with final time/place constituent | published and frozen; S12 pins this pair |
| `pilot-16` / `generator-16` | `evaluation-16` | + E030 supplied neutral positive yes/no question | published and frozen; S13 pins this pair |
| `pilot-17` / `generator-17` | `evaluation-17` | + E031–E039 scoped work/study vocabulary operations | published; revision-one P1 plans retained; matching-layout exception below |
| `pilot-18` / `generator-18` | `evaluation-18` | same formats; stable matching shuffle | CURRENT, published and frozen; five revision-two P1 plans pin this pair |

Guided practice plans (`practice-1`, `present-verbs-1`) and follow-ups (`follow-up-1/2`) carry their own plan revisions and remain
pinned to pair three. The separately selected `verb-article-gaps-1` and `follow-up-3` plans pin pair seven literally.
A CURRENT constant must never silently upgrade a plan; moving it requires a new plan revision with its own validation.
The three `module-one-singular-1`, `module-one-groups-1` and `module-one-er-1` plans pin pair
eight, with explicit required-teaching, completion and exposure policy metadata. Their scoped
targets do not enter an older follow-up planner. The four-item `module-one-things-1` plan pins
pair nine and reuses unchanged first-step completion/exposure policies.
The six-item `module-one-present-1` and `module-one-order-1` plans pin pair ten.
S06 has its own completion and exposure policies for order corrections and partial word banks.
The four-item `module-one-possession-1` plan pins pair eleven. Its stored-form practice reuses
the existing named-form completion and exposure policies.
The four-item `module-one-reading-words-1` plan pins the existing pair-five E011 choices.
Its `module-one-reading-word-exposure-1` policy records meaning confirmation on correct choices.
The six-item `module-one-personal-1` plan pins pair twelve. The four-item
`module-one-ability-1` plan pins pair thirteen and reuses the named-form completion and exposure policies.
The two-item `module-one-volition-1`, `module-one-obligation-1` and
`module-one-permission-1` plans pin pair fourteen. Separate versioned modal-round policies
record meaning selection and supplied word order.
The four-item `module-one-circumstances-1` plan pins pair fifteen and retains the existing
order outcome and exposure policies.
The four-item `module-one-questions-1` plan pins pair sixteen and retains those same
order policies; ja/nej remain introduced support rather than assessed vocabulary.
The five `vocabulary-p1-s1-1` … `vocabulary-p1-s5-1` plans pin pair seventeen literally.
The five `vocabulary-p1-s1-2` … `vocabulary-p1-s5-2` plans pin pair eighteen and are advertised
as current. Both generations use unchanged `vocabulary-p1-outcome-1` /
`vocabulary-p1-exposure-1` policies and no existing follow-up planner entry. Source metadata,
typed known alternatives, matching correspondences, structured facts, occurrence orders and
teaching/exposure inputs remain compatibility inputs.
Gradle/npm package versions, `/api/v1` and the app version are separate numbers.

### Pair-seventeen matching exception

The released P1 matching shuffle consumed a data-class hash containing enum identity. Its
raw matching layout and matching-dependent plan/review bytes vary across JVM initialization
histories. A universal deterministic raw seventeen golden would misrepresent that behavior;
the attempted freeze remained an ignored rehearsal and created no permanent fixture.

Pair eighteen uses stable lexical entry/sense hashing. For old stored seventeen E033 rows,
normal regeneration runs first; only fingerprint mismatch permits reconstruction from the
stored public board. The candidate must reproduce both the original complete fingerprint
and the original public presentation. Never overwrite a stored fingerprint or regenerate
its private practice plan. Actual released-board fixtures pin source and SHA, and preserve
the original lexical slot/exposure associations. The
[repair contract](../../docs/knowledge/matching-restart-compatibility.md) owns this narrow
exception and pending-first-grade/restart acceptance. Existing one–sixteen goldens remain immutable.

## What is stored, and what that implies

- `exercises` stores the revision pair, `presentation_payload` (the exact JSON the learner got) and a `reproduction_fingerprint`
  (SHA-256 over every generated field; `ExerciseMapping.reproductionFingerprint` has a literal branch per pair and **errors on an unknown pair**).
- `attempts` stores `request_payload`, `raw_answer` (**raw learner input is preserved verbatim**), `response_payload` and the evaluation revision.
- An attempt replay returns the stored `response_payload` without regrading, even after an exercise's pair stops being supported
  (`ApplicationHardeningTest`). A *new* grade regenerates the exercise (`regenerateVerified`) and answers `409` if the fingerprint drifted.
- `wireJson` has `ignoreUnknownKeys = false`: an older binary cannot read payloads written with new fields. Adding a revision or an optional
  field is therefore a one-way door for the *binary*; rollback needs a compatible reader (see the rollback notes in the contracts above).

## Frozen evidence — immutable

`*/src/test/resources/golden/` holds compatibility evidence generated from the **unchanged published code**. Each fixture has a
`.provenance` file (baseline commit, revisions, row count, `fixtureSha256`, `reviewState=compatibility-only-not-linguistic-approval`).

| Fixture | Guard | Freezes |
| --- | --- | --- |
| `engine/.../pilot-1-generator-1.tsv` (335 rows) | `PilotCompatibilityGoldenTest` | pilot-1 generation cycles, boundaries, grading |
| `engine/.../pilot-2-generator-2.tsv` (952) | `PilotTwoCompatibilityGoldenTest` | pilot-2 generation and evaluation |
| `engine/.../pilot-3-generator-3-evaluation-3.tsv` (1,320) | `PilotThreeCompatibilityGoldenTest` (SHA-256 pinned) | pilot-3 formats, hashes, grading |
| `server/.../pilot-3-generator-3-wire.tsv` (1,806) | `PilotThreeWireCompatibilityTest` (SHA-256 pinned) | public wire, inspector and fingerprint for pilot-3 |
| `engine/.../pilot-4-generator-4-evaluation-4.tsv` (1,614) | `PilotFourCompatibilityGoldenTest` (SHA-256 pinned) | all ten formats, grading facets, raw/Unicode cases and boundaries |
| `server/.../pilot-4-generator-4-wire.tsv` (2,187) | `PilotFourWireCompatibilityTest` (SHA-256 pinned) | pair-four public wire, inspector and reproduction fingerprints |
| `engine/.../pilot-5-generator-5-evaluation-5.tsv` (2,074) | `PilotFiveCompatibilityGoldenTest` (SHA-256 pinned) | all thirteen formats, grading facets, raw/Unicode/boundary cases and byte-exact teaching review |
| `server/.../pilot-5-generator-5-wire.tsv` (2,811) | `PilotFiveWireCompatibilityTest` (SHA-256 pinned) | pair-five public wire, inspector and reproduction fingerprints |
| `engine/.../pilot-6-generator-6-evaluation-6.tsv` (2,365) | `PilotSixCompatibilityGoldenTest` (SHA-256 pinned) | inherited cycles, bounded matching generation, 209 partial bijections, ordered answers, raw/Unicode/boundary grading and byte-exact review |
| `server/.../pilot-6-generator-6-wire.tsv` (2,852) | `PilotSixWireCompatibilityTest` (SHA-256 pinned) | pair-six public wire, private source and reproduction fingerprints |
| `engine/.../pilot-7-generator-7-evaluation-7.tsv` (3,675) | `PilotSevenCompatibilityGoldenTest` (SHA-256 pinned) | inherited formats and all 92 gap clauses with nine semantic states, thirteen ordered arrays, malformed/raw input and byte-exact review |
| `server/.../pilot-7-generator-7-wire.tsv` (3,134) | `PilotSevenWireCompatibilityTest` (SHA-256 pinned) | pair-seven public/private gap wire, attempt results and reproduction fingerprints |
| `engine/.../guided-gap-practice-0.8.0.tsv` (21) | `GuidedGapPracticeCompatibilityTest` (SHA-256 pinned) | published guided-gap plans, seed boundaries, slot outcomes/exposure, first/latest unknowns and recursive reviews |
| `server/.../guided-gap-practice-0.8.0-wire.tsv` (22) | `GuidedGapPracticeWireCompatibilityTest` (SHA-256 pinned) | initial/supported/attempted/completed guided wire, private plans, existing profile, request serialization and recursive review lineage |
| `engine/.../pilot-8-generator-8-evaluation-8.tsv` (2,674) | `PilotEightCompatibilityGoldenTest` (SHA-256 pinned) | all nineteen format cycles/boundaries, all thirty first-step tasks, real private answer keys, grading boundaries and byte-exact review |
| `server/.../pilot-8-generator-8-wire.tsv` (3,518) | `PilotEightWireCompatibilityTest` (SHA-256 pinned) | pair-eight public/private wire, grading/replay, nested fingerprint values and ordering |
| `engine/.../module-one-practice-0.9.0.tsv` (23) | `ModuleOnePracticeCompatibilityTest` (SHA-256 pinned) | three first-step plans, teaching/policy/scoped outcomes, exposure, first/latest unknowns and completion eligibility |
| `server/.../module-one-practice-0.9.0-wire.tsv` (100) | `ModuleOnePracticeWireCompatibilityTest` (SHA-256 pinned) | initial/supported/attempted/completed first-step wire, saved request/plan/context, eligibility and no review fallback |
| `engine/.../pilot-9-generator-9-evaluation-9.tsv` (2,742) | `PilotNineCompatibilityGoldenTest` (SHA-256 pinned) | all 21 format cycles, inanimate-reference grading and private keys, byte-exact review |
| `server/.../pilot-9-generator-9-wire.tsv` (3,578) | `PilotNineWireCompatibilityTest` (SHA-256 pinned) | pair-nine public and inspector wire, attempts and reproduction fingerprints |
| `engine/.../module-one-things-practice-0.10.0.tsv` (13) | `ModuleOneThingsPracticeCompatibilityTest` (SHA-256 pinned) | S04 teaching, policies, scoped evidence and latest-attempt eligibility |
| `server/.../module-one-things-practice-0.10.0-wire.tsv` (34) | `ModuleOneThingsPracticeWireCompatibilityTest` (SHA-256 pinned) | S04 lifecycle wire, saved requests, plans, replay and exposure |
| `engine/.../pilot-10-generator-10-evaluation-10.tsv` (2,930) | `PilotTenCompatibilityGoldenTest` (SHA-256 pinned) | all 25 format cycles, S05/S06 private keys and grading boundaries, byte-exact review |
| `server/.../pilot-10-generator-10-wire.tsv` (3,746) | `PilotTenWireCompatibilityTest` (SHA-256 pinned) | pair-ten public and inspector wire, attempts and reproduction fingerprints |
| `engine/.../present-order-practice-0.11.0.tsv` (23) | `PresentOrderPracticeCompatibilityTest` (SHA-256 pinned) | S05/S06 teaching, plans, scoped outcomes, eligibility and exposure |
| `server/.../present-order-practice-0.11.0-wire.tsv` (67) | `PresentOrderPracticeWireCompatibilityTest` (SHA-256 pinned) | S05/S06 saved requests, private plan/context, public lifecycle and replay |
| `engine/.../pilot-16-generator-16-evaluation-16.tsv` (3,459) | `PilotSixteenCompatibilityGoldenTest` (SHA-256 pinned) | all thirty format cycles and boundaries, actual private question keys, inherited raw/Unicode grading cases, all E029/E030 permutations and byte-exact review |
| `server/.../pilot-16-generator-16-wire.tsv` (4,193) | `PilotSixteenWireCompatibilityTest` (SHA-256 pinned) | pair-sixteen public/private wire, inspector, fingerprints, malformed-attempt persistence boundary, grades and exact replay |
| `engine/.../question-practice-0.18.0.tsv` (55) | `QuestionPracticeCompatibilityTest` (SHA-256 pinned) | S13 four-item plan teaching/support, policies, scoped outcomes, ordered exposure, eligibility and replayable evaluations |
| `server/.../question-practice-0.18.0-wire.tsv` (37) | `QuestionPracticeWireCompatibilityTest` (SHA-256 pinned) | S13 selected profile, saved request/private plan/context, public lifecycle, support, eligibility and exact replay wire |
| `engine/.../pilot-18-generator-18-evaluation-18.tsv` (3,591) | `PilotEighteenCompatibilityGoldenTest` (SHA-256 pinned) | all 39 format cycles/boundaries, all 23 P1 tasks, real private source/keys, accepted/reviewed tiles, matching partial bijections, grading and byte-exact review |
| `server/.../pilot-18-generator-18-wire.tsv` (5,862) | `PilotEighteenWireCompatibilityTest` (SHA-256 pinned) | pair-eighteen public/inspector wire, fingerprints, grading, replay and private-metadata mutations |
| `engine/.../vocabulary-p1-practice-0.20.1.tsv` (261) | `VocabularyP1RevisionTwoPracticeCompatibilityTest` (SHA-256 pinned) | five literal revision-two plans, teaching, outcomes, ordered exposure, real sequential retry contexts and completion policies |
| `server/.../vocabulary-p1-practice-0.20.1-wire.tsv` (221) | `VocabularyP1RevisionTwoPracticeWireCompatibilityTest` (SHA-256 pinned) | revision-two requests/private plans, public lifecycle, support, matching outcomes, exposure, eligibility and exact replay |
| `server/.../published-fingerprints.tsv` (978) | `PublishedFingerprintCompatibilityTest` (SHA-256 pinned) | original reproduction fingerprints of pilot-1/2 |

Plus `PracticeWireCompatibilityTest` (legacy plan payload and practice item/response stay byte-stable when new optional fields are absent).
Pair four was frozen from unchanged baseline `e37749c` in commit `c43c8f7` before pair-five production edits.
Pair five was frozen from unchanged baseline `06dc714` in commit `ac2ed71` before pair-six production edits.
Pair six was frozen from unchanged baseline `6d1c0fe` in commit `889a9b4` before pair-seven production edits.
Pair seven was frozen from unchanged baseline `8138855` before guided multiple-gap production edits;
it remained CURRENT through 0.8.0 with unchanged generation and grading. New practice plans carry separate revisions.
Published 0.8.0 guided practice was additionally frozen from unchanged baseline `0dfc00a`
before the selected first-steps implementation changed shared target/support/completion models.
Its canonical projection lists legacy fields explicitly; adding omitted defaults must not change
an archive through a data class's object representation.
Pair eight and the three 0.9.0 first-step plans were frozen from released source `8913dea`,
without production changes. Their projections include the real private answer key as well as
inspector/task metadata. The test-source review adapter checks the literal retained-eight snapshot and
hashes actual retained review bytes. The retained renderer was added before CURRENT moved
in `394c4c1`; the review hash, fixtures and provenance are unchanged.
Pair nine and its S04 plan were frozen from released source `3d87b4d` before adding S05–S06.
Pair ten and its S05/S06 plans were frozen from released source `8934fb1` before adding S07.
Pair eleven and S07–S08 were frozen from released source `b82ad36` before S09.
Pair twelve and S09 were frozen from released source `f0c9568` in `156c86e` before S10.
The four pair-twelve/personal-practice archives contain 3,140 engine, 40 plan, 3,866 wire
and 34 practice-wire rows. They pin private keys, exact review bytes and lifecycle exposure.

Rules:

1. **Never edit, regenerate, reformat or "fix" a golden/provenance file** (`.gitattributes` keeps LF so bytes are stable). A red golden
   test means the code changed published behaviour: fix the code. The only legitimate golden change is *adding* a new file.
2. A fixture is only ever added by its exporter, which uses `CREATE_NEW` and refuses to overwrite: engine —
   `ASTRID_PILOT_THREE_GOLDEN_DIR=<new dir>` with `PilotThreeCompatibilityGoldenExportTest`; server — `./gradlew :server:exportPilotThreeWireCompatibility`.
3. New optional wire fields are `@EncodeDefault(NEVER)` with a `null`/empty default, so a response for an old revision serializes exactly as before
   (`explicitNulls = false`); assert it with a byte-level test like `PracticeWireCompatibilityTest`.
4. Never reuse a revision string for different behaviour; never delete a pair, snapshot or evaluator branch that stored data may reference.

## Introducing a new revision without changing old behaviour

1. **Freeze first, in its own commit**: from unchanged source, export evidence for the pair about to stop being CURRENT (engine and server),
   with provenance (`baselineCommit`, revisions, `rowCount`, `fixtureSha256`), and pin the SHA-256 in the guarding tests. Cover every
   format cycle, seed/index boundaries (`0`, `Int.MAX_VALUE`, cycle size, `1_000_000`), raw and Unicode inputs and grading cases.
2. Add the new pair: new `CURRENT_*` constants (the old ones become `RETAINED_*`), a `PilotContentSnapshot`, validation
   (`PilotContentValidator.requireValid`), generator and evaluator dispatch, an `evaluationRevision` branch, `SUPPORTED_REVISION_PAIRS`.
3. Server: a literal branch in `reproductionFingerprint` (new framing; old branches untouched), DTO mapping, and the closed `oneOf`
   correlations (format ↔ target ↔ pair) in `documentation.yaml`; `gen:api`; extend `OpenApiContractTest` accepted/rejected matrices.
4. Keep practice/follow-up planners on their pinned constants unless the plan is itself being revised.
5. Add a review export for the pair (a `PilotReview(...)` entry in the list in `engine/build.gradle.kts`, plus its flag in
   `PilotReviewExportMain`) and keep the older ones.
6. Prove: all frozen goldens and wire tests unchanged and green; retained pairs still create, submit and replay; mutation tests show
   the new fingerprint changes on every nested value/order change.
7. Plan the deploy: back up the database first (`persistence.md`); a migration is needed only if a column shape changes (stored payloads are
   JSON and additive).

Pair thirteen and S10 were frozen from the released 0.15.0 source in `d7724dc`
before S11 production changes. The new archive guards include private ability keys,
all twenty-seven format pools and byte-exact teaching and lifecycle exposure.

Pair fourteen and the three S11 modal rounds were frozen from released 0.16.0
source in `aca3c5d` before S12 production changes. The projection covers complete modal
private inputs, the full previous ability source and all 28 format pools.

Pair fifteen and S12 were frozen from released 0.17.0 source `0f1b322` in
`80fc257` before S13 production changes. The four archives contain 7,611 rows and
pin actual private circumstance keys, review bytes and public/private lifecycle.

Pair sixteen and S13 were frozen from the unchanged released 0.18.0 source
`9bb10ae5633edb192651dc3d6956ff269b2a61eb` before S14 production changes. The four
archives contain 7,744 rows and pin all thirty format pools, actual private neutral-question
keys, review SHA-256 `aca468c27942fc9cfe583ebb772b8cafe580cc18c4a5db0650c9f92f5be6be20`,
the four-item plan's teaching/support/outcome/exposure/eligibility projection, and exact
public/private/saved-payload/replay wire. The P1 pair-seventeen change switches the test-source moving adapters to literal
`pilotSixteenSnapshot` and `renderPilotSixteenMarkdown()` without changing any captured byte.

## Export tasks

| Task | Output |
| --- | --- |
| `:engine:exportPilotReview` | `engine/build/review/pilot-18.md` (current; run in CI and uploaded as `server-reports`) |
| `:engine:exportPilotSeventeenReview` | `engine/build/review/pilot-17-retained.md`; historical matching-dependent layout is not cross-JVM byte-stable |
| `:engine:exportPilotSixteenReview` | `engine/build/review/pilot-16-retained.md` |
| `:engine:exportPilotFifteenReview` | `engine/build/review/pilot-15-retained.md` |
| `:engine:exportPilotFourteenReview` | `engine/build/review/pilot-14-retained.md` |
| `:engine:exportPilotThirteenReview` | `engine/build/review/pilot-13-retained.md` |
| `:engine:exportPilotTwelveReview` | `engine/build/review/pilot-12-retained.md` |
| `:engine:exportPilotElevenReview` | `engine/build/review/pilot-11-retained.md` |
| `:engine:exportPilotTenReview` | `engine/build/review/pilot-10-retained.md` |
| `:engine:exportPilotNineReview` | `engine/build/review/pilot-9-retained.md` |
| `:engine:exportPilotEightReview` | `engine/build/review/pilot-8-retained.md` |
| `:engine:exportPilotSevenReview` / `exportPilotSixReview` / `exportPilotFiveReview` / `exportPilotFourReview` / `exportPilotThreeReview` / `exportPilotTwoReview` / `exportLegacyPilotReview` | `pilot-7.md` / `pilot-6.md` / `pilot-5.md` / `pilot-4.md` / `pilot-3.md` / `pilot-2.md` / `pilot-1.md` |
| `:engine:exportPilotFourCompatibility` / `:server:exportPilotFourWireCompatibility` | write **new** pair-four evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotFiveCompatibility` / `:server:exportPilotFiveWireCompatibility` | write **new** pair-five evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotSixCompatibility` / `:server:exportPilotSixWireCompatibility` | write **new** pair-six evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotSevenCompatibility` / `:server:exportPilotSevenWireCompatibility` | write **new** pair-seven evidence and provenance only (refuse overwrite) |
| `:engine:exportGuidedGapPracticeCompatibility` / `:server:exportGuidedGapPracticeWireCompatibility` | add the published 0.8.0 practice archive and provenance only (refuse overwrite; existing files are immutable) |
| `:engine:exportPilotEightCompatibility` / `:server:exportPilotEightWireCompatibility` | add published pair-eight evidence and provenance only (refuse overwrite) |
| `:engine:exportModuleOnePracticeCompatibility` / `:server:exportModuleOnePracticeWireCompatibility` | add the published 0.9.0 first-step practice archives and provenance only (refuse overwrite) |
| `:engine:exportPilotNineCompatibility` / `:server:exportPilotNineWireCompatibility` | add pair-nine evidence and provenance only (refuse overwrite) |
| `:engine:exportModuleOneThingsPracticeCompatibility` / `:server:exportModuleOneThingsPracticeWireCompatibility` | add published S04 evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotTenCompatibility` / `:server:exportPilotTenWireCompatibility` | add pair-ten evidence and provenance only (refuse overwrite) |
| `:engine:exportPresentOrderPracticeCompatibility` / `:server:exportPresentOrderPracticeWireCompatibility` | add published S05/S06 evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotElevenCompatibility` / `:server:exportPilotElevenWireCompatibility` | add published pair-eleven evidence and provenance only (refuse overwrite) |
| `:engine:exportFollowOnPracticeCompatibility` / `:server:exportFollowOnPracticeWireCompatibility` | add published 0.13.0 S07–S08 plan evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotTwelveCompatibility` / `:server:exportPilotTwelveWireCompatibility` | add pair-twelve evidence and provenance only (refuse overwrite) |
| `:engine:exportPersonalPracticeCompatibility` / `:server:exportPersonalPracticeWireCompatibility` | add published S09 plan evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotThirteenCompatibility` / `:server:exportPilotThirteenWireCompatibility` | add pair-thirteen evidence and provenance only (refuse overwrite) |
| `:engine:exportAbilityPracticeCompatibility` / `:server:exportAbilityPracticeWireCompatibility` | add published S10 plan evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotFourteenCompatibility` / `:server:exportPilotFourteenWireCompatibility` | add pair-fourteen evidence and provenance only (refuse overwrite) |
| `:engine:exportModalRoundPracticeCompatibility` / `:server:exportModalRoundPracticeWireCompatibility` | add published S11 plan evidence and provenance only (refuse overwrite) |
| `:server:exportPilotThreeWireCompatibility` | writes a **new** wire fixture + provenance (refuses to overwrite) |
| `:engine:exportPilotFifteenCompatibility` / `:server:exportPilotFifteenWireCompatibility` | add pair-fifteen evidence and provenance only (refuse overwrite) |
| `:engine:exportCircumstancePracticeCompatibility` / `:server:exportCircumstancePracticeWireCompatibility` | add published S12 plan evidence and provenance only (refuse overwrite) |
| `:engine:exportPilotSixteenCompatibility` / `:server:exportPilotSixteenWireCompatibility` | add pair-sixteen evidence and provenance only (refuse overwrite) |
| `:engine:exportQuestionPracticeCompatibility` / `:server:exportQuestionPracticeWireCompatibility` | add published S13 evidence and provenance only (refuse overwrite) |

Review exports are deterministic evidence for teaching review; they are not approval. The production engine does no file I/O.

## Pair-eighteen archive — 12 November 2026

Pair eighteen and the five P1 revision-two plans were captured from unchanged published
0.20.1 source `9eefcfed708670ce45dbdd9b11adc41f9fe17d25`. Two fresh exporter JVM sets,
one under the default locale and one under Arabic (Egypt), produced byte-identical fixture
and provenance files. Four new archives contain 3,591 / 261 / 5,862 / 221 rows; their source,
projection schemas, counts, fixture SHA-256 values and engine review hash are pinned by
the guarding tests. Resources were promoted with collision refusal. All 105 pre-existing
golden/provenance and applied-migration files retained their original hashes.

Deterministic ordering must hash stable scalar identities, such as lexical entry/sense IDs.
Do not use object/enum identity, or a data-class hash containing it, as a reproducible
ordering key. The identity-hash branch is a literal pair-seventeen compatibility exception;
a later pair inheriting P1 matching must not fall through to that legacy behavior. Exercise
new pairs in separate JVMs with different initialization histories before claiming seed
reproducibility. Pair seventeen's real released-board recovery evidence stays separate;
no purported deterministic raw seventeen archive was added. This freeze changes tests and
export tooling only; it neither changes the app version nor activates content.

## Cumulative plans and per-item revisions

Published `cumulative-review-1` was captured from unchanged app 0.21.0 source
`eab86c6ddd44175e2d9b6dde7e2283f30943800e` before P2 production changes. The engine
archive contains 27 rows covering all twenty source families, short/full/empty pools,
boundary seeds, explicit null policies, selections and private per-item evidence. The
25-row server archive captures exact request/private payload roundtrips and synthetic
initial/taught/attempted wire plus saved-attempt context/replay. It is not an assertion
of real-database grading; actual upgrade/restart acceptance is separate. Two fresh JVM
captures under default and Arabic locales were byte-identical. Both archives/provenance
and their SHA guards are immutable; all 115 earlier protected resources stayed unchanged.

`cumulative-review-1` is a composite plan. Its three run-level revision fields are the
literal marker `per-item`; this is **not an engine revision**. Each cumulative public
item supplies `revisions` with its actual content/generator/evaluation triple. Grade and
fingerprint only the stored exercise's literal pair, never the plan's summary markers.
Old plans retain their headers and omit the optional item field.

Preserve source task, private known-alternative, support and assessment semantics when
varying a task. An old index or task ID alone cannot establish cross-pair equivalence.
Keep original pairs for grammar/lexical source lessons; the established P1 seventeen-to-
eighteen matching repair is the narrow exception, with rebuilt board associations.

Optional stored item-policy objects distinguish absence (inherit the session policy)
from a present object whose values are null (use those explicit defaults). Resolve that
distinction consistently in advance, `canAdvance`, hooks, feedback and exposure. Existing
payloads omit all cumulative metadata. New stored fields require a compatible reader for
binary rollback even though no migration or new engine pair is introduced. The selected
contract is [cumulative review](../../docs/knowledge/cumulative-review.md).
