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
| `pilot-3` / `generator-3` | `evaluation-3` | + E007–E008 (vocabulary) | published, frozen; guided practice and follow-ups stay pinned here |
| `pilot-4` / `generator-4` | `evaluation-4` | + E009 (writing), E010 (reading) | published, frozen |
| `pilot-5` / `generator-5` | `evaluation-5` | + E011 (Danish word choice), E012/E013 (all/spare word tiles) | CURRENT (catalog, new playground exercises) |

Guided practice plans (`practice-1`, `present-verbs-1`) and follow-ups (`follow-up-1`, …) carry their own plan revisions and are
pinned to pair three in `PracticePlanner` and `PracticeFollowUpPlanner`. A CURRENT constant must never silently upgrade them; moving them is a
new plan revision with its own validation. Gradle/npm package versions, `/api/v1` and the app version are separate numbers.

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
| `server/.../published-fingerprints.tsv` (978) | `PublishedFingerprintCompatibilityTest` (SHA-256 pinned) | original reproduction fingerprints of pilot-1/2 |

Plus `PracticeWireCompatibilityTest` (legacy plan payload and practice item/response stay byte-stable when new optional fields are absent).
Pair four was frozen from unchanged baseline `e37749c` in commit `c43c8f7` before pair-five production edits.
Pair five has behavioural tests and must itself be frozen before a future revision changes it.

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

## Export tasks

| Task | Output |
| --- | --- |
| `:engine:exportPilotReview` | `engine/build/review/pilot-5.md` (current; run in CI and uploaded as `server-reports`) |
| `:engine:exportPilotFourReview` / `exportPilotThreeReview` / `exportPilotTwoReview` / `exportLegacyPilotReview` | `pilot-4.md` / `pilot-3.md` / `pilot-2.md` / `pilot-1.md` |
| `:engine:exportPilotFourCompatibility` / `:server:exportPilotFourWireCompatibility` | write **new** pair-four evidence and provenance only (refuse overwrite) |
| `:server:exportPilotThreeWireCompatibility` | writes a **new** wire fixture + provenance (refuses to overwrite) |

Review exports are deterministic evidence for teaching review; they are not approval. The production engine does no file I/O.
