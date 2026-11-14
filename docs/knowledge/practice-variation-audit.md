# Practice Variation Inventory

## Scope and method

**7 November 2026 — Observed engineering audit:** Measured the pinned `reading-basics` / `practice-1` planner using the unchanged engine delivered at `c9aa046`. This is a draft-content inventory, not teaching approval or evidence of learning efficacy.

The [frozen Java 21 method](data/PracticeVariationAudit.java) audits 4,096 sequential seeds (`0..4095`) and 4,096 draws from `SplittableRandom(0x415354524944L)`, each `(int) nextLong(2147483648L)`. All 8,192 sampled seeds are distinct. The [raw inventory](data/practice-variation-audit.json) includes per-position targets, nouns, subjects, canonical sentence IDs and invariant counters.

A **semantic plan** retains position, format, target, lexical IDs and clause roles. It excludes request IDs, seed and initial choice/block ordering. A **presentation** uses the ordered engine presentation values, including those initial orderings. The latter comparison is bounded to this compiled Kotlin representation; it does not count CSS themes, screen layouts or future presentation models.

## Results

| Cohort | Seeds | Semantic plans | Presentations |
| --- | ---: | ---: | ---: |
| Sequential | 4,096 | 294 | 4,068 |
| Reproducible pseudorandom | 4,096 | 294 | 4,069 |
| Combined | 8,192 | 294 | 8,075 |

Both grammar positions cover all 21 current reading canonical sentences: seven subjects × three practice nouns. These are part of the existing 99-sentence pilot, not 21 new constructions.

Under the pinned plan and this semantic key, **294 is the complete finite bound**: six permutations of the three senses × seven ordering subjects × seven article-repair subjects. Every remaining semantic field is determined by that tuple. Each cohort covers all 294; presentation coverage remains sampled only. There are 62 presentation values shared between cohorts.

Among the 4,095 adjacent sequential-seed pairs, 13 repeat the exact semantic plan and none repeat the exact presentation. A fresh seed therefore does not guarantee new semantic content. These counts do not establish population probabilities, recommended sampling weights or pedagogical balance.

All eight controlled checks report zero violations: requested seed/identity, pinned revisions, eight-item layout, recognition coverage, recall coverage, reading/noun canonical pairing, noun adjacency and recognition-before-recall. Independent review corrected a seed-echo instrumentation gap before the final rerun; requested seeds also identify any reported violations. No generator, grader, content, archive or runtime API changed.

## Reproduce the frozen measurement

Select the repository's Java 21 toolchain, then run from the repository root:

```sh
./gradlew :engine:classes :server:installDist --dependency-verification strict
practice_audit_dir=$(mktemp -d)
practice_audit_cp="engine/build/classes/kotlin/main:server/build/install/server/lib/kotlin-stdlib-2.4.21.jar"
javac -Xlint:all -encoding UTF-8 --release 21 -cp "$practice_audit_cp" -d "$practice_audit_dir" docs/knowledge/data/PracticeVariationAudit.java
java -cp "$practice_audit_dir:$practice_audit_cp" PracticeVariationAudit "$practice_audit_dir/result.json"
cmp docs/knowledge/data/practice-variation-audit.json "$practice_audit_dir/result.json"
```

The final JSON SHA-256 is `5b7fb9d999bbd612c7f29e42bea840a09edd078fc588020df8d4da5acaa6ede0`. Repeated runs are byte-identical. This archived method is an analysis artifact, not an additional runtime dependency or automatically enforced CI gate.

The broader [pilot inventory](pilot-variation-audit.md) and [session contract](practice-sessions.md) explain the current boundary. Enlarging meaningful variation requires a separately reviewed plan/content change; this audit does not activate more vocabulary, grammar or adaptive scheduling.


## First-step finite inventory — 10 November 2026

**Observed static engineering inventory of released 0.9.0, not a sampled learning study.**
The earlier `practice-1` measurement and its frozen Java/JSON artifacts above are unchanged.
This section inspects the literally pinned pair-eight first-step planner and task definitions;
it does not extend the 294-plan bound to other profiles or count new clause constructions.

A sequence here is the ordered list of `(format, authored task identity)` for a profile.
It excludes seed/index/request identities and initial choice ordering. Direct inspection of
[the planner](../../engine/src/main/kotlin/app/astrid/engine/ModuleOnePracticePlanner.kt)
gives these complete finite bounds:

| Profile | Items/run | Distinct ordered task sequences | Reachable task identities across seeds | Assessed answer forms |
| --- | ---: | ---: | ---: | --- |
| One person | 6 | 4 | 8 | `jeg`, `du`, `han`, `hun` |
| Groups | 6 | 1 | 6 | `vi`, `I`, `de` |
| Present `er` | 6 | 4 | 10 | `er` |

One-person selection always covers its four forms in that order. Recall takes two adjacent
forms from the four-item cyclic rotation: `jeg/du`, `du/han`, `han/hun`, or `hun/jeg`.
Groups always select then recall `vi/I/de` in that order. The `er` profile has two selections
and four recalls: even seeds select book/first-person identity, odd seeds letter/second-person
identity; recall always includes book/letter and two adjacent teacher frames. Parity and
rotation both derive from `seed mod 4`, so they do not multiply into eight sequences.

Across the profiles, the union is **24 task identities and eight expected forms**. Within
`er`, ten task identities cover eight supplied frames, because book/letter occur in both
operations. The four teacher frames occur only in recall; the two identity frames only in
selection. The [standalone task catalogue](../../engine/src/main/kotlin/app/astrid/engine/ModuleOneExercises.kt)
has 30 identities: seven each for E016/E017, eight each for E018/E019. Six standalone identities
are not reached by these plans. Task counts distinguish operations and authored contexts;
they are not counts of different complete Danish sentences or meanings.

Generation maps `(seed,index)` to the finite task list with `floorMod(31*seed + 17*index, size)`.
The planner scans indices `0..31` and chooses the first task matching the requested form/frame.
Choice banks contain the same scope-specific alternatives; the generator preserves or reverses
them using seed/index parity. A different seed or reproduction identity therefore does not by itself guarantee a new
task or bank order, or establish unseen linguistic transfer. Presentation counts are
not measured here. Changed people in English contexts can still request the same Danish form.

All three profiles require saved teaching. It displays their assessed forms before recall;
selection banks and canonical corrections can disclose them again. Group entry supplies the
sleep verb; `er` entry supplies infinitive/present, identifying `det`, and noun/article bundles.
The stored keys record **possible prior exposure**, not proof that a learner read, retained
or independently produced a form. Recognition and recall outcomes remain separate. No new
content, sampling weights, adaptive scheduler or repetition guarantee is introduced by this
audit. Broader meaningful variation requires a separately selected plan/content revision.


## P3 finite inventory — 14 November 2026

Source [`e015624`](https://github.com/liveweird/astrid/tree/e015624884632d54bdc6a0aadbf933cec3601664)
implements six Places and journeys profiles on literal pair twenty. This is a source inventory,
not a claim of learning efficacy or new language generation.

| Quantity | Meaning and boundary |
| --- | --- |
| Six sequences, 4/4/4/3/4/4 activities | Each profile forces its fixed authored subset on every run; the six subsets collectively contain 23 named task IDs. |
| Seven formats, E045–E051 | Meaning selection, matching, recall, supplied order, explicit reading and spatial contrasts remain distinct operations. |
| 17 authored support-fact IDs | Fixed packet propositions; not 17 new globally unique sentences or independently assessed meanings. |
| 21 placements: 16 new and five reused | New records L064–L079 plus selected existing senses/chunks. Review/support appearances add no records. |
| 79 source lexical records overall | Entry inventory, not 79 senses, 79 independently practisable words or a learner's knowledge count. |
| 32 cumulative-three source families | Eligibility ceiling after the current owner's saved teaching; old versions retain 20/26 families. |

The direct generator addresses authored candidates using widened `(seed + index)` modulo its
format pool. The practice planner scans until it finds each named task. A session seed therefore
does not change lesson membership: it controls supplied choice order, matching column order,
tile order and opaque board identities. Different seeds can still produce the same ordering. Shuffling does not synthesize new sentences, meanings,
lexical correspondences or word-order licenses.

Evidence is in `VocabularyP3Exercises.kt` (task/fact pools and scalar shuffles),
`VocabularyP3PracticePlanner.kt` (six fixed sequences), `VocabularyP3ExercisesTest.kt`
(inventory/language/reproduction guards) and `CumulativeReviewPlannerTest.kt` (versioned
admission/semantic identity). The [activation contract](vocabulary-places-journeys-activation.md)
owns the reading, spatial and exposure restrictions.

**Planning implication:** a [reference-library precursor](vocabulary-library-plan.md) improves
content discovery but adds no semantic variety. More within-lesson variety needs separately
reviewed alternative propositions/frames with the same explicit primary targets, selected senses
and context restrictions. Do not substitute arbitrary dictionary words or describe new board
layouts as unseen linguistic transfer. Broadening content or plan behavior requires a separately
selected revision; BL-004 and BL-067 remain open.
