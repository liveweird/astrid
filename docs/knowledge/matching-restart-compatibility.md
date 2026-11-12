# Vocabulary Matching Across Server Restarts

## Status and observed defect

**12 November 2026 — Compatibility repair within the authorized P1 delivery.** App 0.20.0
was released from `3c22cb645c2bea45c7969e93f7f3088734f67ce0`. Its recorded gates and saved-answer
replay checks passed. Subsequent compatibility-freeze rehearsal exposed a different path:
grading a previously unanswered matching board after another JVM regenerates it.

P1 E033 shuffles rows using a data-class `hashCode()` containing `VocabularyLexicalKind`.
Enum hashes use object identity, so different JVM initialization histories can change
column order, positional ID-to-label associations, private correspondences and fingerprints.
Two ordinary exporter launches happened to agree; the test JVM differed. The first observed
archive difference was E033 at seed 47, index 2. Matching-dependent plans and review text also
changed. Previously answered requests replay stored responses and therefore did not expose
this defect in the original restart check.

This is an implementation error, not new linguistic content. A universal byte-exact raw
pair-seventeen generation or review fixture cannot honestly be captured. The uncommitted
freeze draft and rehearsal evidence are retained locally; no permanent golden was created
or changed. Existing published pair-one through pair-sixteen fixtures remain immutable.

## Repair decisions

The repair is **app 0.20.1**, with new literal
`pilot-18/generator-18/evaluation-18`. Matching uses a stable hash of lexical entry ID,
a unit separator and sense ID, retaining the established mixing/salt arithmetic. Other
shuffles, content, exercise families and assessment policies are unchanged.

Five current P1 plan revisions `vocabulary-p1-s1-2` … `vocabulary-p1-s5-2` pin eighteen.
The five published `-1` plans stay requestable on seventeen, with their original teaching,
completion/exposure policies and frozen assessments. New sessions use `-2`; repeating or
continuing an old run selects the corresponding current profile. Saved run records and
serialized outbox bodies are never rewritten.

The historical seventeen generator remains executable. Its matching layout limitation is
explicit; preservation of a saved board relies on the exact stored evidence below. This
exception must not be presented as cross-JVM seed reproducibility. S14 remains paused; its
tentative next pair must be re-audited after eighteen is consumed by this repair. P2/P3
runtime activation remains unselected.

## Strict recovery of saved boards

Normal verified regeneration runs first. Only a fingerprint mismatch for stored
`pilot-17/generator-17`, format E033, may invoke the narrow pure engine restoration seam.
The input is the server's stored presentation, never a client-supplied board.

The engine validates the original request, authored task, exact label multisets, board
size, unique IDs and original seed/position-derived opaque IDs. It preserves saved column
order and text, binds each saved English label to its authored Danish meaning, and builds
private correspondences in saved-left-column order. Private constructors remain internal.

The server accepts restoration only when both the complete reproduction fingerprint and
the complete mapped public presentation equal the stored originals. Invalid evidence keeps
the existing 409 behavior. It never replaces a fingerprint, presentation or private plan.
Ownership guards, transactional hooks and replay-before-grading ordering remain intact.
The old plan's assessment slots, lexical identities and exposure arrays remain authoritative.

No endpoint or migration is added. OpenAPI keeps closed `-1→17` and `-2→18` correlations
and rejects crossed content/generator pairs. A previously loaded 0.20.0 chooser requires a
page refresh to understand the new catalog revisions; existing runs and pending submissions
remain supported. No client-version negotiation is introduced.

## Required evidence and release boundary

Released-process evidence contains 24 standalone boards across two JVMs and four pending
guided sessions, including original presentations, fingerprints and inspector/plan metadata.
The de-identified test resource pins source SHA and fixture SHA-256; operational credentials
remain outside the repository. Every recovery-path test must select a board whose ordinary
regeneration actually mismatches, rather than accidentally testing the normal fast path.

Acceptance requires independent label-based answer oracles, malformed/tampered-evidence
rejection, actual captured plan/exercise payload rehydration in PostgreSQL, and unchanged
stored bytes. Forked-JVM probes must demonstrate different enum identity hashes while
producing identical pair-eighteen matching outputs.

Candidate browser acceptance also exposed an incomplete API revision allowlist: the catalog
advertised five revision-two P1 profiles, while practice creation accepted only revision one.
The resulting 409 was followed by a misleading recovery GET 404. Preserve all five retained
pairs and add all five current pairs explicitly. An HTTP regression must create every
advertised P1 profile and every retained profile, assert their literal 18/17 revisions and
required teaching, then recover the exact created response. Planner-only coverage cannot
prove the route accepts the advertised contract. This was caught before the 0.20.1 tag or
preview promotion; repeat release gates against the corrected source.

Live acceptance must grade an originally unanswered old board after upgrade, preserve its
slot associations and retry number 1, replay the exact response, restart again, and save a
new retry as number 2. Complete normal repository/browser/CI/scan gates, verified backup
restore, app-only promotion and owned-QA cleanup before claiming repair delivery. A broader
legacy semantic archive or complete eighteen freeze may follow after this repair is safe.

Rollback after eighteen writes needs an eighteen-compatible reader/grader. Returning to
0.20.0 would strand new work and restore the original defect; preserve data and roll forward
or use a compatible repair image. Danish teaching review remains separately pending BL-022.

## Observed delivery

**12 November 2026 — Repair delivered.** App [0.20.1](https://github.com/liveweird/astrid/releases/tag/v0.20.1) was released
from annotated tag `v0.20.1`, exact source `9eefcfed708670ce45dbdd9b11adc41f9fe17d25` and verified image `sha256:2a2af0ef46719c8ffd4ae486c01550ad5dcfbe867966cb575dcaf7c2ad65389f`.
The complete Docker-backed gate passed 403 engine tests, the same 403 tests under the
locale-compatibility run, 267 server tests and 285 web tests. All 482 native and 482
packaged-Linux Chromium/WebKit journeys passed without retries or flakies; all five
[exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/43148241073) passed with 482 authenticated-log Linux journeys and no
retries or flakies.

Live recovery graded twelve released standalone and four released guided pair-seventeen
boards at retry 1, replayed their exact responses after an actual candidate restart, and
graded the same original IDs at retry 2. All twelve ordinary pair-seventeen probes produced
actual fingerprint mismatches and therefore exercised the narrow stored-board restoration.
Four fresh pair-eighteen boards remained pending across restart, then regenerated normally,
graded at retry 1 and replayed exactly. Original public presentations, fingerprints,
inspector correspondences, private plans, requests and IDs remained unchanged.

Separately, after actual main-preview promotion, all twelve originally pending captured
main pair-seventeen boards received their first grades at retry 1 under evaluation seventeen;
each planned POST and GET replay returned the exact stored response. That bounded check added
only its twelve predeclared attempts: every prior attempt row and all five other history tables,
exercise IDs, stored presentations and fingerprints remained unchanged. It makes no claim that
the main boards required fingerprint fallback; the twelve observed mismatch/fallback probes
above are the separate owned-QA evidence.

The image passed the HIGH/CRITICAL vulnerability policy and network-disabled runtime package
floors. Before app-only promotion, the actual 6,076,472-byte mode-0600 backup was
restored into an isolated scratch database with all six history tables (attempts 14,033, development sessions 4,884, exercises 25,191, practice attempt context 10,912, practice items 18,870, practice sessions 3,058); the
scratch database was removed. Promotion preserved those rows, Flyway versions 1–3 and the
main PostgreSQL process and mounts. All 186 main-preview browser checks passed without
retries or flakies, and ownership-checked cleanup removed only the matching-repair QA app,
database, volume and network.

This patch release adds no curriculum: current P1 revision-two plans use pair eighteen and
retained revision-one plans remain on pair seventeen. P2/P3 remain unactivated authoring
drafts, S14 remains paused, and Danish teaching review remains pending under BL-022.

## Compatibility archive follow-up

The test-only preservation pass captures published 0.20.1 source `9eefcfe` without changing
its production code or the running preview. Four new immutable archives contain 3,591 engine,
261 revision-two plan, 5,862 server-wire and 221 practice-wire rows. Fresh default-locale and
Arabic-locale exporter JVMs produced byte-identical fixtures and provenances before promotion.
The guards pin the actual source/date/schema, row counts, SHA-256 values and engine review
bytes, with literal coverage of all 39 formats, 23 P1 tasks and five revision-two plans.

Independent review checked the private-source projection, complete three/four-pair matching
cases, actual accepted/reviewed tile orders, sequential retry-feedback contexts and public
non-leakage boundaries. All 105 pre-existing golden/provenance and applied-migration files
kept their hashes. The [compatibility conventions](../../.claude/docs/compatibility.md)
record the archive inventory and stable-ordering rule. This is compatibility evidence only;
linguistic approval, P2/P3 activation and S14 resumption remain separate.

Post-capture `./scripts/check.sh` passed all 420 engine tests, the same 420 locale tests,
282 server tests and 285 web tests, with no skipped JVM tests. Kover measured engine
line/branch coverage at 98.33%/86.04% and server at 98.36%/82.37%; the existing integer
floors remain 98/86 and 98/82. The added exporter/helper refactors passed independent review
without changing any captured bytes or assertion coverage.
