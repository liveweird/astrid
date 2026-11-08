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
