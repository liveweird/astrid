# Thematic Vocabulary Expansion: Authoring and Activation

## Selection and current boundary

**11 November 2026 — User-confirmed authoring milestone:** after the recommendation to
complete five session scripts and an implementation contract for **P1 — A day of work and
study**, the user said “Go for it” and requested enough useful work for the night until
**07:30 Europe/Warsaw on 12 November**, ahead of an 08:00 laptop shutdown. This is the new
work window; earlier overnight windows are historical. Do not alter power management.

The [selected vocabulary blueprint](vocabulary-course-blueprint.md#selected-expansion-authoring-plan--11-november-2026)
owns the eighteen P1 placements and the sixty-placement three-packet budget. At selection,
runtime app 0.19.0 remained at pair sixteen; the observed delivery below supersedes that
baseline. S14 stays paused. The initial authoring milestone allocated no
stable lexical IDs, changes no published generator or grader, and supplies no Danish
specialist approval.

**Runtime scope subsequently confirmed:** the user answered **“Implement and release the
first unit too”**. Complete P1 after its content/contract review, including the standing
versioned release, exact-source verification and backed-up app-only private-preview delivery.
P2 and P3 remain authoring drafts. This supersedes the initial pending scope clarification.

Retain noun-only recall under its existing contract. Teach count nouns with articles;
mass uses, language nouns and chunks have explicitly different realizations. Q27 remains
open for a future article+noun assessment operation.

## Overnight work queue and stopping points

These are bounded execution packages within the accepted content direction, not a promise
that all packages or a runtime release fit the window. Work through them in dependency order;
independent source checks and code-readiness analysis may proceed alongside authoring.

| Package | Reviewable result | Completion boundary |
| --- | --- | --- |
| 1. P1 complete scripts | Five short sessions: teaching, readings, exact exercise slots, answers, distractors, exposure and feedback cases. | All eighteen placements accounted for; unsupported combinations and pending linguistic decisions explicit. |
| 2. P1 implementation contract | Exact supported operations/pools, prerequisites, saved-plan/exposure policy, compatibility changes and test matrix grounded in current code. | No fictional existing capability; minimal vertical slice and independent review findings reconciled. |
| 3. Source verification | Record-level COR form checks and primary reference consultation for new frame candidates. | Distinguish verified morphology from authored meanings/valency and teacher review. No bulk dataset adoption. |
| 4. P2 food/shopping authoring | Twenty-one selected placements with morphology/senses/frames and six short session outlines. | Recount reuse against current and P1 records; resolve duplicate senses and grammar prerequisites. |
| 5. P3 places/journeys authoring | Twenty-one selected placements with morphology/senses/frames and six short session outlines. | Distinguish location/direction and supplied chunks from productive grammar. |
| 6. Cross-packet readiness review | Exercise coverage, ambiguity, introduced-material eligibility, sentence variety and implementation dependencies across all three packets. | Findings resolved or preserved as precise open decisions; no frequency/mastery claim. |
| 7. Verified delivery and handoff | Focused commits, merge/push, knowledge index/backlog update and exact continuation instructions. | Full `./scripts/check.sh` with Docker before each commit; clean reviewed Git state and observed remote result. |

By **06:30**, prioritize closing active work and its verification. Do not begin a long
unbounded implementation or deployment near the cutoff. By **07:30**, leave the repository
at a safe stopping point with completed and unfinished work recorded. If runtime delivery
is selected, reserve time for exact-source CI/browser checks, a real backup restore,
app-only promotion and ownership-scoped QA cleanup; a partially verified build is not a
release. Documentation-only deliveries do not bump the app version or redeploy the preview.

## Selected P1 implementation contract

The [complete P1 packet](vocabulary-work-study-packet.md) owns the teaching, exact readings,
23 activity slots and authored answers. This section owns implementation policy. Independent
architecture review found substantive gaps in noun-only lexical sources, fixed four-pair
matching, fixed object-query readings, article+noun tile tokenization and guided matching
transport/summary. Reuse the existing UI controls and answer transports while adding bounded
packet sources and projections; do not pretend these generators already support every kind.

### Release and content assignments

The selected first-unit release is **app 0.20.0**, learning pair
**`pilot-17/generator-17/evaluation-17`**. This explicitly supersedes S14's unpublished
0.20.0/pair-seventeen reservation. S14 remains paused, tentatively following at 0.21.0/pair
eighteen, subject to rechecking the actual baseline when resumed. No existing S14 draft is
merged into P1. Keep all eighteen existing profiles literally pinned.

**Subsequent compatibility repair — 12 November 2026:** the
[matching restart repair](matching-restart-compatibility.md) reserves app 0.20.1 and pair
eighteen without new content. It supersedes the tentative S14 pair-eighteen reservation:
S14 remains paused and tentatively follows at 0.21.0/pair nineteen, subject to another
baseline audit. Published P1 revision-one plans remain on seventeen; current revision-two
plans use eighteen.

Audit then allocate `L041` skrive, `L042` computer, `L043` studere, `L044` kursus,
`L045` drikke, `L046` kaffe, `L047` liquid vand, `L048` static hjemme and `L049`
fixed `om morgenen`. Reuse the exact current senses for other P1 rows. The morning expression
is one fixed lexical chunk, like existing `i dag`; P1 never splits it into word-tile
components or licenses productive `om + noun`. The expected lexical inventory is 49 records,
not 49 newly learned words. Add explicit morphology, provenance and draft review metadata;
do not insert mass or chunk entries into count-noun article parsers.

Pair sixteen and S13 are already frozen **from release source `9bb10ae5633edb192651dc3d6956ff269b2a61eb`
in freeze commit `0e98e81`**. Verify archives/provenance and rebind moving test adapters to
literal retained-sixteen snapshot/review access before changing CURRENT. Never recapture
or edit those bytes. Existing format pools, evaluators, fingerprints and saved results stay
executable under their published revisions.

### Operations and primary targets

| Format | P1 slots | Target and answer shape |
| --- | --- | --- |
| E031 — Packet meaning recognition | 1.1–1.3 | One selected lexical meaning, Danish→English supplied text choice. |
| E032 — Packet Danish choice | 2.2 | One selected lexical meaning, English→Danish supplied bundle choice; no article production score. |
| E033 — Packet matching | 2.1, 3.1, 4.1, 5.1 | One board with 4/4/3/3 atomic lexical assessments; existing matching-pair answer shape. |
| E034 — Packet present recall | 3.4, 4.4 | G004, scope `p1-studere-present` or `p1-drikke-present`, mode `recall`; one-word text. |
| E035 — Packet expression recall | 3.5 | Lexical `fixed-chunk/en-da`, introduced `nu`; one-word text in this packet. |
| E036 — Packet mass-word recall | 4.5 | Lexical `mass-headword/en-da`, selected bare liquid `vand`; one-word text. |
| E037 — Packet all-word tiles | 3.3, 5.4, 5.5 | G005 / `p1-subject-first-supplied` / `word-order`; all supplied occurrences. |
| E038 — Packet present-form tiles | 4.3 | G004 / `p1-drikke-present` / `selection`; three selected occurrences, one infinitive spare; subject-first is a response constraint. |
| E039 — Packet fact reading | 1.4, 2.3, 2.4, 3.2, 4.2, 5.2, 5.3 | Explicit-fact reading target; five object questions, one place and one time; supplied English choice. |

These are **nine operations across five UI families**. The spare-form activity tests a finite
form; it must not invent a G005 score. Conversely, an all-word order activity supplies its
verb and creates no independent G004 success. Recognition, matching and production evidence
remain distinct. E007/E008, Q27 and all existing article assessment stay unchanged.

Each candidate freezes a neutral task ID, entry/sense/form references, source frame/fact IDs,
cue, ordered choice bank with rationales, accepted answer/realizations, reviewed errors and
exposure keys. A theme tag creates no valid combination. P1 lexical sources distinguish
count-bundle, verb form, mass word, bare language noun, adverb and fixed chunk; never use an
empty article to force a value into the legacy noun-only source type.

### Reading, matching and tile sources

Every reading question points to exactly one supporting authored fact. Facts record referent,
predicate, object or circumstance, rendered sentence and all visible lexical/form exposures.
Derive the expected answer from that fact and requested role; do not store place/time in an
`answerNounId`. A distractor absent from the passage must not receive the old “appears in
another fact” feedback. Named subjects disambiguate P1's final reading.

Matching uses independently shuffled opaque item IDs and a private correspondence table.
Validate three/four cardinality and unique left/right IDs. Keep each pair's target identity
independent of its displayed index. A public pre-answer descriptor may identify the board
and count; it must not join left and right columns through a private assessment label/key.
Each pair has its own result, exposure and summary row while the board remains one activity
and one attempt.

Tile banks use occurrence IDs plus explicit lexical/form identities, not a universal
article+noun tokenizer. Freeze canonical orders and only these exact grammatical off-target
alternatives: `Nu læser hun en bog` for 3.3 and `I Odense arbejder jeg` for 5.5. Their meaning
and language facets are met/acceptable, while the subject-first constraint fails. This is
bounded diagnostic coverage, not S14 generation or a general fronting lesson. Other
unsupported orders remain unknown, not automatically ungrammatical.

For 4.3, `Nora drikker vand` is fully met; `Nora drikke vand` is the exact reviewed
`INFINITIVE_USED` clause error with meaning/response shape met and G004 not met. All four
tokens, a partial clause or both verb occurrences are unevaluable, meaning/language unknown
and shape not met; finding `drikker` somewhere never awards success. Mass-object fronting
is not licensed in this task. Selection/count/start constraints are independently decidable;
an unsupported order that satisfies them can have shape met while language remains unknown.

### Typed grading and completion defaults

For the four new typed slots, preserve raw input and compare NFC-normalized, outer-whitespace
trimmed isolated words with `Locale.ROOT` case folding. Capitalization is not an assessed
skill in these isolated prompts. No accent removal, transliteration, fuzzy/prefix matching
or acceptance of trailing prose is selected. Internal extra words fail the one-word shape.
Keep legacy normalization literally unchanged.

Isolated `studere`/`drikke` are known acceptable infinitives that miss the requested present
form, rather than malformed sentences. Another introduced word can be known and off-target;
unintroduced synonyms/forms remain unknown. `en vand`/`et vand` fail the selected bare-word
shape and do not establish the selected mass meaning. Do not claim all article-bearing
Danish uses are wrong. A correct headword embedded in arbitrary prose earns no prefix success.

**Implementation clarification, 12 November:** “introduced” means visibly taught or reviewed
within this independently enterable session before the slot, not merely present elsewhere
in P1's inventory. Freeze each typed task's known alternatives from that bounded material.
A displayed count-noun bundle can be recognized as known but fail the one-word shape;
do not derive an unsupported bare noun by removing its article. This preserves honest
unknown outcomes without assuming a learner completed another profile.

Use new literal completion policy `vocabulary-p1-outcome-1`. Require saved teaching and
evaluate the latest saved attempt under the existing owner lock. Known correct or known wrong
shape-valid outcomes may continue; unknown or partial results may not. A matching board must
be a complete bijection even when one selected pair is wrong. The two exact reviewed
fronted alternatives above may continue as known off-target outcomes using a P1-only
subject-first issue-code allowlist; they never count as fully met. A later unknown attempt
supersedes an earlier eligible one. No score threshold or mastery rule is added.

### Profiles, exposure, saved evidence and summaries

Publish five independent profiles `vocabulary-p1-s1` … `vocabulary-p1-s5`, literal plan
revisions `vocabulary-p1-s1-1` … `vocabulary-p1-s5-1`, with 4/4/5/5/5 activities. Each run
always supplies full required teaching and that session's prerequisite review, including
support pronouns, order, possession and names as needed. Repeat/next opens the chooser;
creation remains an explicit saved operation. There is no course-wide unlock, shortened
teaching inferred from browser history or ordinary-start cross-run exposure. Existing
source-linked follow-ups remain unchanged; P1 adds no targeted-review planner.

Use new literal exposure policy `vocabulary-p1-exposure-1`. Freeze meaning, form, bundle,
proposition, order and support keys with each plan. Reading records visible forms and
complete facts. English-only distractors do not expose later Danish counterparts. Matching
prompt exposure records displayed forms; feedback records only mappings actually confirmed
for correct pairs, not every expected mapping after a wrong/partial board. Record answer
reveal/retry as possible exposure, never as proof of learning or independent recall.

Exposure keys describe exact visible surfaces: a finite reading verb does not expose its
infinitive, and an English cue does not reveal its Danish answer. Include every displayed
Danish choice and supplied support form. Wrong feedback releases no unseen expected answer;
only confirmed correct mappings/forms/orders or an actually rendered solution may release
those keys. Keep public matching and tile IDs independent of answer correspondence/order:
assign them after shuffling, then derive the private answer table by occurrence identity.

Extend guided descriptors/observations additively for reading and matching, preserving all
legacy target keys and omitted-default serialization. Matching summary projects by exact
lexical identity and mode, never displayed column or grammar-gap slot IDs. Reading summaries
identify the explicit-fact role. Carry meaning outcomes for P1; fully met requires every
applicable facet. Preserve whole-run activity/attempt/retry counts and separate first/latest,
unknown, correction, support and prior-exposure evidence. Old summary bytes remain unchanged.

No new endpoint/table/migration is expected: V3's eight-activity bound fits each profile.
The server must persist all private plan/source/projection metadata, use explicit DTO mapping,
fingerprint every public/private grading input and preserve exact creation/attempt bodies.
Reject malformed new closed-choice IDs/out-of-bank responses after ownership checks without
persisting an attempt; do not tighten old formats' validation. Apply the existing mutation
guard and transaction order. New payload readers must remain available for old saved work;
rollback after P1 writes needs a compatible reader, not an incompatible older binary.

### Delivery acceptance and ownership

One engine implementer owns shared engine models, content/dispatch, new packet generators,
graders, planners, summaries/policies, review exports and meaningful literal tests. A separate
server implementer owns wire/private payload mapping, validation/fingerprints, OpenAPI and
generated schema plus HTTP/fake/PostgreSQL contracts. The coordinator integrates guided web
matching/state/recovery, learner directory, version/changelog and E2E/scenarios against those
interfaces. Parallel writers own disjoint files; independent reviewers check each substantive
boundary after implementation.

Acceptance covers all 23 slots, every introduction and direct-entry prerequisite, both board
sizes and partial/full outcomes, per-pair exposure, exact fact answers, typed known/unknown/
shape cases, tile alternatives, latest-attempt continuation, fingerprint mutations, security
ordering/private-answer absence, old serialization and sixteen retained revisions. Prove
lost-response replay, actual restart, expiry/cross-tab recovery, keyboard/language markup,
Axe, both themes and 320px/200% reflow. Run the full Docker-backed gate, native and packaged
Linux browser suites, exact-source CI, image/package scans, actual isolated backup restore,
app-only promotion, live smoke and owned-QA cleanup before claiming release delivery.

Danish specialist validation remains BL-022 and must be disclosed as pending. Independent
source/content review is required for this implementation; it does not establish specialist
approval or add a new human-approval gate. Cumulative review and the Vocabulary library
remain separate BL-066/BL-067 work.

## Source verification — 11 November 2026

A fresh independent lookup against the official documented COR JSON service confirmed
all nine P1 candidate lemma records and the exact active form IDs in the blueprint:
`30533` (skrive), `63924` (computer), `32395` (studere), `41200` (kursus),
`30480` (drikke), `43580` (kaffe), `41032` (vand), `10260` (hjemme) and
`43995` (morgen/morgenen). All selected forms returned normalization `N`; gender/form
analyses match the recorded noun/verb/adverb roles. Retrieval initially failed through
the web tool and sandbox DNS; scoped public network reads succeeded. This is a
reference check, not a dataset import or an application network dependency.
[Current COR release](https://ordregister.dk/) ·
[documented lookup service](https://ordregister.dk/doc/COR.Webservices.html)

Consulted DDO's **skrive** sense 1 (text production with optional object), **drikke**
verb sense 1 (beverage intake), **studere** sense 1 (education, with optional subject
of study), **hjemme** adverb sense 1 (static home location), **morgen** (the
`om morgenen` expression), and the separate noun/verb entries for **arbejde**.
These support reference distinctions; they do not certify every authored combination,
optimal teaching order or English cue. In particular, a bare “study” cue must not make
`læse` an allegedly invalid alternative to `studere`; P1 must establish the intended
selected use. No dictionary definitions or corpus sentences are imported.
[skrive](https://ordnet.dk/ddo/ordbog/skrive) ·
[drikke](https://ordnet.dk/ddo/ordbog/drikke) ·
[studere](https://ordnet.dk/ddo/ordbog/studere) ·
[hjemme](https://ordnet.dk/ddo/ordbog/hjemme) ·
[morgen](https://ordnet.dk/ddo/ordbog/morgen) ·
[arbejde](https://ordnet.dk/ddo/ordbog/arbejde)

The COR liquid-water record confirms neuter morphology and its JSON includes the
`væske` disambiguator. The initial extraction omitted that field; an independent
recheck corrected the extraction limitation. The selected English meaning and mass-use
policy remain authored content. Likewise,
bare mass `kaffe/vand` and the full `om morgenen` chunk need explicit teaching/frame
review; an indefinite-form code does not require an indefinite article in every use.

## Observed delivery

The initial execution plan and first source audit passed the complete local gate and were
committed/pushed as `40e1011`; the selected P1 scripts/activation contract followed in
`4ee9c02`. Independent content review resolved ambiguous study/read context, answer-category
leaks, matching-target ambiguity, a pronoun link, an invented specialist gate and freeze
provenance wording. Reviewed P2/P3 ledgers and six-session outlines were separately gated
and pushed in `10a1eeb`; they remain authoring drafts without runtime activation.

**Superseded by the observed runtime delivery below.** P1 implementation had passed
the complete Docker-backed repository gate. Independent engine/API review led to opaque
public IDs, exact visible exposure, session-local typed alternatives, complete direct-entry
support, atomic taught-mapping evidence and legacy summary-field gating. The saved
pre-upgrade S12/S13 sessions and eleven attempts replayed byte for byte on the draft QA
image. Final clean-source browser, CI, release and promotion acceptance was still pending
at that checkpoint; it claimed neither a runtime release nor Danish specialist approval.

### Observed runtime delivery — 12 November 2026

Released app [0.20.0](https://github.com/liveweird/astrid/releases/tag/v0.20.0) at annotated tag `v0.20.0`, exact source
`3c22cb645c2bea45c7969e93f7f3088734f67ce0`, and verified `arm64` image `sha256:469111118540d81a292fabb19f809d4985cabfd993e5a8aba7be8a74b515f9e4`. The private preview now runs
learning pair seventeen with five independently enterable **A day of work and study**
sessions and all 23 authored activity slots. The catalogue contains 49 lexical records,
including the nine new P1 records, across 39 standalone format entries, 20 lessons and
23 practice profiles.

The complete Docker-backed local gate passed: 399 engine tests, the same 399 tests under
the locale-compatibility run, 260 server tests and 279 web tests. Web coverage was
statements 95.31% (1586/1664), branches 89.83% (1344/1496), functions 98.02% (397/405), lines 98% (1325/1352); Kover recorded engine line 9162/9311 and branch
2870/3332, and server line 2183/2220 and branch
757/922. All **482 native** and **482
packaged-Linux** Chromium/WebKit journeys passed against the exact image without retries
or flakies. All five [exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/43129404302) passed, including
**482 CI Linux journeys** with authenticated log verification and no retries
or flakies. The exact image passed the HIGH/CRITICAL vulnerability policy and the
network-disabled runtime package-floor check. Twelve source/image/hash-bound directory,
teaching and matching captures were inspected by the coordinator across both themes, desktop,
and 320px/200% text.

One saved S12/pair-fifteen session and one saved S13/pair-sixteen session, with eleven
attempts between them, replayed byte for byte after the actual candidate restart. All five
new P1 sessions and 23 attempts also replayed exactly after that
restart; raw uppercase and outer-whitespace typed values remained preserved in PostgreSQL
while the selected `Locale.ROOT` case-folded grading accepted them. Before app-only
promotion, a **5,792,929-byte** backup with mode `0600` was restored into an
isolated scratch database, reproducing all six history tables; the scratch database was
then removed. Promotion preserved those rows, Flyway versions 1–3 and the main PostgreSQL
process and mounts. All **186 live-preview browser checks** passed without
retries or flakies.

Cleanup removed only the owned QA app and database containers, volume and network; the
main preview processes and mounts remained unchanged. The temporary loopback GitHub
transport may remain only for the final scoped documentation push and is not part of the
runtime service; no global Git or proxy setting was changed.

All Danish remains a linguistic draft awaiting teaching review under BL-022. P2 and P3
remain reviewed authoring drafts without runtime activation, and S14 remains paused. P1
does not establish specialist approval, broader course activation or learner efficacy.
