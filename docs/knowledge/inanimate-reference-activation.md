# S04: Referring to One Thing

## Authorization and scope

**10 November 2026 — User selected:** “S04: referring to things with den and det”.
Implement BL-053 from the [reviewed proposal](module-one-course-slice.md#proposed-next-activation--s04).
Deliver app **0.10.0** with a new learning pair **pilot-9 / generator-9 / evaluation-9**,
independent review, normal checks, focused commits merged/pushed, verified release and
backed-up app-only private-preview promotion. These numbers are implementation choices.

The target is **G038 / `inanimate-singular-subject`**, separately assessed through
selection and recall. All content remains a linguistic draft; software verification does
not establish teaching approval. No S05/S06, object/plural/human reference, demonstratives,
dummy subjects, new Danish predicates or complete clauses are activated.

## Teaching and four-item profile

The independent `module-one-things / module-one-things-1` profile is titled **Referring to
one thing**. Required teaching must be saved before assessment, including direct entry.
Explain a word, a noun bundle, grammatical gender, the subject's job and standalone
lowercase spelling without claiming subject/object discrimination from identical forms.

Supply `en bog` (a book, common gender) and `et brev` (a letter, neuter) in every
reference situation. Teach **den** for the same previously mentioned common-gender thing
and **det** for the same previously mentioned neuter thing. Distinguish this reference use
from the identifying `Det er …` already taught in S03. No gender guessing is assessed.

| Activity | Supplied situation | Expected word |
| --- | --- | --- |
| E020 selection | One previously mentioned book; common gender | `den` from `den/det` |
| E020 selection | One previously mentioned letter; neuter | `det` from `den/det` |
| E021 recall | The same book in a separately authored English context | `den` |
| E021 recall | The same letter in another English context | `det` |

Seeds may reorder authored items/banks within the selection-then-recall scaffold. Both
forms occur once in each operation. Wording changes are contextual repetition, not unseen
linguistic transfer. Reuse the existing choice/text controls. S03 offers S04 as its next
profile; S04 offers repeat or an explicit fresh chooser, with no nonexistent S05 link and
no targeted-review fallback.

## Identity, grading and exposure

Reference identities are **L026 / L026-S01** (`den`) and **L008 / L008-S02** (referential `det`), allocated separately from identifying L008; do not broaden its sense
or add things to `humanPronouns`. Private source metadata must bind reference sense,
antecedent sense, singular number, supplied gender, requested subject role, case policy
and exact context variant. Fingerprint every new field; learner responses omit private
keys/analysis. The supplied article is support, not article-recall evidence.

| Response | Bounded result |
| --- | --- |
| Expected lowercase form, including surrounding whitespace | Target met, acceptable, one-word constraint met; raw input retained |
| Other introduced lowercase form | Target not met, acceptable Danish word; explain reference mismatch |
| Matching `Den` / `Det` | Reference evidence retained; known standalone-case issue, constraint not met |
| `det er` | Target unevaluable; known one-word shape failure; no prefix grading |
| `dem` or unsupported word | Target unevaluable, language unknown, no correction disclosed; continuation blocked |
| Choice outside its displayed bank | HTTP validation rejects before attempt persistence |

Reuse `module-one-known-outcome-1` only with identical issue/eligibility meanings. Latest
saved outcomes govern continuation: an unknown retry blocks it even after an eligible
first answer. Keep first/latest counters separate. Corrections expose the standalone word;
unknown feedback exposes none. Use `module-one:reference-sense:L026-S01` / `L008-S02` and
`module-one:visible-form:den` / `det` as separate key namespaces. Track both reference-sense and visible-form exposure,
including identical `det` spelling across identifying/reference senses, without treating
exposure as learning or knowledge. Cross-run experience remains unknown.

## Finite variation boundary

The standalone pool has four authored task identities: book/letter selection and
book/letter recall, with two English context variants per antecedent across operations.
Each format cycles through its two tasks before repeating. The profile preserves the
selection-then-recall scaffold and has two ordered task sequences: even seeds select
book then letter and recall letter then book; odd seeds do the reverse. Choice placement
may vary without creating another linguistic task or unseen transfer. Both expected words
must appear in both choice positions across seeds; task selection must not make the correct
answer always first. Required teaching exposes both forms before assessment.

## Bounded linguistic source check

**10 November 2026 — Source check, not teaching approval:** DDO's pronoun entries
[den², sense 1](https://ordnet.dk/ddo/ordbog/den) and
[det³, sense 1](https://ordnet.dk/ddo/ordbog/det) describe reference to a known or just-mentioned
singular entity, respectively common and neuter gender. Our authored tasks restrict these
uses to the same explicit, individual inanimate book or letter. This is not a rule for every
English “it”: [Dansk Sprognævn's discussion](https://dsn.dk/nyt-fra-sprognaevnet/januar-2024/genus-i-pronomener-i-faste-udtryk/)
explains broader semantic reference and nonreferential/idiomatic uses. Those are excluded.
No dictionary examples, audio or dataset were imported; the short English situations are
original authoring. The source check supports this narrow mapping, not dose, progression,
naturalness of a wider course or learning efficacy.

## Compatibility and verification

Pairs one through eight and their plans remain executable and literally pinned. Existing
goldens/provenance and migrations are immutable. Pair eight and the 0.9.0 profiles already
have frozen generation, private-key, grading, review, fingerprint, wire and lifecycle
evidence; retain the pair-eight review renderer before CURRENT moves. Preserve old shared
sentence pools and complete-clause count (109). No migration is expected for JSON-backed
plans; verify reader/rollback boundaries rather than assuming binary rollback is safe.

Require deterministic finite task coverage, literal grading and exposure oracles, scoped
selection/recall summary rows, latest-unknown blocking, malformed choices, required
teaching, owner isolation, exact lost-response replay and PostgreSQL persistence. Retained
archives must pass unchanged. Closed OpenAPI format/target/revision correlations, generated
client types, inspector mapping and fingerprint mutations must agree.

Run `./scripts/check.sh` before each commit, including Docker tests and coverage floors;
run the full assembled Chromium/WebKit suite with no retries, plus new-state accessibility,
keyboard and narrow/enlarged-text checks. Native WebKit previously failed before page
creation; preserve any new failure and distinguish environment limits from app defects.
Recheck CI's outer image-job headroom as the browser suite grows; preserve full tests,
scans and per-test timeouts. Verify the exact master SHA before tagging/releasing; verify
backup readability and an isolated restore before app-only promotion, preserving existing
rows and the main PostgreSQL process/mounts. Remove only owned QA resources.

## Implementation decisions

- Den's new entry is L026/L026-S01; reference `det` is L008-S02. Identifying L008's
  published metadata, human-pronoun list and old lexical-assessment pools stay unchanged.
- Direct-entry prerequisites are `[G001,G002,G003,G006]`, supplied as support rather than
  locked progression. The two summary keys are
  `grammar:G038:inanimate-singular-subject:selection` and the corresponding `:recall` key.
- The image CI job's outer allowance rises from 35 to 40 minutes, matching the nightly.
  The 372-journey run `42737593818` took 2,004 seconds (96 seconds spare); later unchanged
  run `42773472174` took 1,726. Added S04 browser cases need room for that observed variation.
  Full tests/scans, per-test timeouts, workers, coverage floors and retry rules are retained.
- No column/schema change is planned. Pair-nine payloads need a compatible reader/evaluator;
  rolling the binary back to 0.9.0 after new writes is unsafe. Deploy the server and bundled
  client together because their supported profile catalogs must agree.

## Observed runtime delivery — 10 November 2026

Released app [0.10.0](https://github.com/liveweird/astrid/releases/tag/v0.10.0), source `3d87b4d`, after all five
[exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/42898557511) passed with **388 Linux browser journeys and no retries/flakies**.
The full local gate passed **205 engine tests**, the same **205 in a fresh Arabic locale**,
**165 server tests including PostgreSQL**, and **207 frontend tests**, plus strict dependencies,
API drift/lint, static analysis and deployment rendering. Measured coverage retained the existing
floors: engine 98.14% lines / 85.76% branches, server 98.22% / 81.48%, web 95.12% statements /
89.49% branches / 97.84% functions / 97.85% lines.

All **388 native Chromium/WebKit journeys** passed without retries against immutable image
`sha256:fd083a92d59879883ef1b4a85094664416325d34d5214266ab6986e3e879d2e8`. Independent engine, API and interface reviews were completed and findings
resolved: choice placement no longer predicts the answer, inherited matching/gap attempt schemas
accept evaluation nine, retained recovery cases remain covered, and standalone word labels use
appropriate task/language semantics. Forty source/image-bound views were independently inspected
across both themes and desktop/narrow enlarged layouts, including supplementary summary
card bodies and lower feedback actions. Four narrow card element captures had their upper
title/subtitle region obscured by the sticky header/focused skip link; those regions were not
directly verified in those frames. Desktop titles and automated reflow checks provide separate
evidence. No actionable defect was found in the reviewed regions. Static captures do not prove
interaction, scrolling or teaching approval; the browser journeys provide separate interaction
evidence. These engineering checks do not establish learning efficacy.

The initial `3dcfe9d` candidate completed 384 native journeys with four failures from stale
introduction-wording/profile-count assertions (two cases in both browsers). The test-only
correction was verified in all four affected cases before the successful full run. Failed
logs/traces and the rejected candidate remain preserved separately; no release or preview
promotion used that candidate.

Actual QA upgrade/restart preserved public/private wire, reads, exact attempt replay and fresh
grading for all eight retained evaluators, six saved profiles and the three completed pair-eight
first-step summaries. S04's saved teaching, two scoped rows, case/shape outcomes and latest-unknown
blocking survived the observed restart with exact creation/read/attempt replay. All frozen
archives and the retained-eight review hash remained unchanged; no migration was added.

A private **3,915,363-byte**, mode-0600 backup was restored into an isolated scratch
database and every captured row matched before app-only promotion (development_sessions: 3556, exercises: 19157, attempts: 8999, practice_sessions: 1997, practice_items: 13248, practice_attempt_context: 6126). Main PostgreSQL's
process and mounts and schema versions **1/2/3** were preserved, as were pre-promotion rows.
All **176 live-preview smoke journeys** passed without retries, including S04 and retained flows.
Ownership-checked cleanup removed only the QA containers, volume and network.

Local packaging hit three 10-second Gradle distribution-download read stalls. A 60-second
experiment also stalled and was reverted; the unchanged original Dockerfile and clean tagged
source then built using a controlled host-network build connection. Strict distribution/dependency
verification and the original timeout/retry configuration remained in place. This successful path
does not establish a network root cause; the proposed verified-ZIP fallback was not used.
The main preview serves the exact verified image/source. Private logs, captures, hashes and backup
remain ignored under `.local/s04/`. BL-053 is complete; S05–S06, broader reference, teaching review,
and the separate WebKit/resource investigation remain unselected or open as previously recorded.
