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
- Visual review found rounded-corner clipping that ordinary label-overflow checks missed. A text-range/rounded-shape regression failed on the earlier candidate and passed in Chromium and WebKit after the narrow feedback fix. Actual text remains enlarged; desktop badge styling remains unchanged. Final matrices are being repeated for that correction.
- Independent engine/API and web review completed. Fixed the unknown-English-choice badge and lexical review export; added explicit legacy-field omission and complete PostgreSQL snapshot assertions for identified test gaps.
- Toolchain, startup-budget, knowledge, dependency-state and deployment-rendering guards passed. npm audits pass the high-severity threshold; unchanged frontend tooling still has moderate advisories.

Final browser matrices, runtime image scan, GitHub CI, main-preview migration/promotion and ownership-scoped QA cleanup are pending at this checkpoint. BL-036 remains active until delivery acceptance completes.
