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

Live acceptance must grade an originally unanswered old board after upgrade, preserve its
slot associations and retry number 1, replay the exact response, restart again, and save a
new retry as number 2. Complete normal repository/browser/CI/scan gates, verified backup
restore, app-only promotion and owned-QA cleanup before claiming repair delivery. A broader
legacy semantic archive or complete eighteen freeze may follow after this repair is safe.

Rollback after eighteen writes needs an eighteen-compatible reader/grader. Returning to
0.20.0 would strand new work and restore the original defect; preserve data and roll forward
or use a compatible repair image. Danish teaching review remains separately pending BL-022.

## Observed delivery

Implementation and repair acceptance are in progress. The private preview still runs the
published 0.20.0 image until the repair's verified promotion is recorded here.
