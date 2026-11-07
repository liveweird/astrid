# Vocabulary Assessment Delivery

## Scope and behavior

The user authorized BL-036 on 7 November 2026: meaning recognition and introduced Danish headword recall for existing `bog`, `avis`, and `brev`. E007 “Recognize the meaning” presents Danish with three related English choices. E008 “Recall the Danish headword” requests the introduced singular noun for an English cue. `brev` uses the qualified meaning “letter (written message).”

The current catalog has eight entries and separate lexical entry/sense/facet/direction targets. The language inventory remains 23 entries and 99 licensed sentences. Optional lessons record support; the ordinary recall prompt does not expose the requested headword. Case-folded comparison is limited to the two new formats, retaining raw and NFC/whitespace-normalized input. Unknown synonyms/writing receive a bounded unknown diagnosis.

Headword identification and response constraints are distinct: `en avis` identifies the requested headword but misses the noun-only instruction; `et avis` also receives an article-gender diagnosis. English choices receive English-specific feedback. A partially satisfied response does not receive green “Good work.” These are practice records, not mastery or learning-efficacy evidence; linguistic teaching review remains outstanding. Mixed sessions remain later work.

## Published compatibility and migration

Current revisions are `pilot-3/generator-3/evaluation-3`. Both previous exact pairs retain their original format sets, generation, grading, fingerprints and result replay. Kept all 335 original fixtures untouched; froze and verified 952 revision-2 generation/evaluation cases and 978 published fingerprint cases before implementation. The [contract](vocabulary-assessment.md) records their provenance and boundaries.

V2 only drops `NOT NULL` from the grammar-target column. The isolated upgrade began on the actual previous image and V1 schema with exercises/results from both published pairs. After upgrade, all original exercise payloads and fingerprints retained the same aggregate hash, saved reads/replays were byte-exact, and fresh submissions still used their original evaluators. No old row was rewritten.

All six vocabulary format–sense cases were stored and read through real PostgreSQL. The authenticated private inspector returned the matching lexical source without fabricated canonical sentences. After app replacement, all six snapshots, results and replays remained exact, and fresh regenerated attempts succeeded.

Rollback after vocabulary writes requires a compatible reader for nullable grammar IDs and new metadata. Preserve the forward schema and stored evidence; a bare older image is unsafe.

## Verification observed

- Strict JVM/coverage gates passed: 57 engine tests in normal and fresh Arabic-locale JVMs, 47 server tests including real PostgreSQL migration/readback/replay, all frozen cases, retained/current exports and distribution build.
- Frontend lint/build/API drift checks and 62 tests passed, retaining all coverage floors: 94.53% statements, 87.06% branches, 95.31% functions and 96.30% lines.
- Initial complete native and emulated-Linux matrices each passed all 58 journeys without retries. Both include vocabulary directions, known wrong words, extra articles, unknown answers, support tracking, English/Danish markup, uncertain replay, eight-format navigation, accessibility and enlarged layouts.
- Visual review found rounded-corner clipping that ordinary label-overflow checks missed. A text-range/rounded-shape regression failed on the earlier candidate and passed in Chromium and WebKit after the narrow feedback fix. Actual text remains enlarged; desktop badge styling remains unchanged. Both final complete matrices passed all 58 journeys after the correction (native 1.6 minutes; emulated Linux 6.9 minutes), with retries disabled. The emulated run uses a 90-second per-test allowance; CI and assertion deadlines remain unchanged.
- Independent engine/API and web review completed. Fixed the unknown-English-choice badge and lexical review export; added explicit legacy-field omission and complete PostgreSQL snapshot assertions for identified test gaps.
- Toolchain, startup-budget, knowledge, dependency-state and deployment-rendering guards passed. npm audits pass the high-severity threshold; unchanged frontend tooling still has moderate advisories.

## Integration and main-preview acceptance

All five [feature CI jobs](https://github.com/liveweird/astrid/actions/runs/42364486201) passed on `7e4a9a4`, including **58 packaged browser journeys in 4.2 minutes with no retry/flaky result**. The final immutable local image and remote packaged image passed their configured vulnerability scans. Nine focused commits through that head were pushed and fast-forwarded into local `master`.

The verified image serves `http://localhost:8088`. All ten final Chromium/WebKit vocabulary and enlarged-feedback checks passed on the main preview. A private pre-upgrade dump is retained outside version control. Every pre-existing exercise presentation/fingerprint and attempt request/raw answer/result/evaluation hash remained unchanged across V2; the PostgreSQL container, process and mounts were preserved. Previously unsubmitted exercises from both older releases were read, graded under evaluation-1/evaluation-2, and replayed exactly after the upgrade.

Ownership checks preceded removal of the QA app, database volume and network; no vocabulary QA containers remain. Both main services are healthy with zero restarts. BL-036 is complete; feedback on the expanded playground remains BL-035.
