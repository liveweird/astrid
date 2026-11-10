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

## Delivery status

Selected; implementation and verification are in progress. No release or preview promotion
has yet been performed for S04. Further implementation decisions and observed evidence
will be recorded here before completion is reported.
