# P2/P3 Activation Readiness

## Status and approach

**Proposed assessment, 12 November 2026.** P2/P3 remain reviewed authoring drafts with
complete proposed session scripts. Runtime activation is unselected, S14 remains paused,
and this assessment allocates no runtime identities, versions or profile revisions. The
released baseline is app 0.20.1/current pair eighteen; published P1 revision-one plans
retain pair seventeen. Danish specialist review remains pending BL-022; that status does
not create another human-approval gate for private draft previews.

The smallest sufficient approach is new bounded packet sources and plans using established
answer transports, controls, exposure records and summaries. Preserve P1's literal content,
policies and generation. It is deliberately packet-specific: task pools, known alternatives,
profile lists and exposure prefixes live in
[VocabularyP1Exercises.kt](../../engine/src/main/kotlin/app/astrid/engine/VocabularyP1Exercises.kt)
and [VocabularyP1PracticePlanner.kt](../../engine/src/main/kotlin/app/astrid/engine/VocabularyP1PracticePlanner.kt).
A general curriculum engine is unnecessary for the next packet.

## Activity coverage

| Proposed activities | Reuse and remaining boundary |
| --- | --- |
| P2 S1: matching, `billig` recognition, food-object reading | Existing interactions fit; adjective source kind is missing. |
| P2 S2: milk recognition, four-pair matching, subject reading, milk tiles | New bounded content fits; “Who drinks water?” needs subject projection. |
| P2 S3: functional matching, attention/handover choices, roll-object reading | Existing controls fit contextual meanings; interjection kind and exact scenes are needed. |
| P2 S4: purchase meaning, last-event reading, purchase tiles | Meaning and intact-bundle tiles fit; last-event assessment needs explicit event-order evidence. |
| P2 S5: contextual `gerne`, wish-frame tiles, gratitude choice | Existing choices/order fit; freeze the exact modal–adverb–infinitive frame and response meanings. |
| P2 S6: quantities, requester reading and two request variants | Complete-script draft proposes two-pair matching and both variants; runtime policy, request sense, subject projection and punctuation support remain open. |
| P3 S1: bicycle recognition/matching, cyclist reading, cycling tiles | Existing operations fit except subject reading. |
| P3 S2: matching, destination reading, direction choice, destination tiles | Existing controls fit; preserve directional semantics in reading/source metadata. |
| P3 S3: matching, static-place readings, home contrast | Matching/place reading fit; script proposes one closed choice between two mappings, with future result ownership still to specify. |
| P3 S4: matching, possession-object reading, Danish train choice | Straightforward bounded content. |
| P3 S5: matching, waiting-place reading, station contrast, present recall | Script proposes a paired-mapping closed choice and local `kører` alternatives; preserve its car scene and define future result ownership. |
| P3 S6: matching, action reading, destination reading, transfer tiles | Needs action/destination projections and retained rail context. |

## Lexical identities and teaching

Retain P2's six reused/fifteen new placements and P3's five reused/sixteen new placements.
Placement counts are not new-entry counts. Request `have` shares the existing lemma but
needs a distinct selected sense from possession `L009-S01`; its implementation identity
remains a future design decision. P1 lexical sources carry entry and sense, but fact lookup
uses entry alone and tile occurrences carry entry/form. Those shortcuts cannot safely
represent multiple uses or compound quantity evidence:
[`placementsById` source lookup](../../engine/src/main/kotlin/app/astrid/engine/VocabularyP1Exercises.kt),
[`VocabularyTileOccurrenceDetails` tile metadata](../../engine/src/main/kotlin/app/astrid/engine/EngineModels.kt).
Future sources should retain sense and construction references explicitly.

The current [kind enum](../../engine/src/main/kotlin/app/astrid/engine/EngineModels.kt)
lacks adjective/interjection kinds. Add bounded source/schema support, without an adjective
agreement or imperative engine. Keep `billig` isolated and `undskyld/værsgo` tied to supplied
scenes. Standalone `tak` is the authored response use; its noun morphology reference does
not create article teaching. Request-only `have` needs a display that does not imply an
active present form: P1's [display helper](../../engine/src/main/kotlin/app/astrid/engine/VocabularyP1Exercises.kt)
assumes infinitive teaching includes present.

## Matching and quantities

Three/four-pair boards, opaque IDs, atomic feedback and per-sense summaries are reusable.
Two quantity pairs fall outside P1's
[`requireValidMatching` validator](../../engine/src/main/kotlin/app/astrid/engine/VocabularyP1Exercises.kt),
[`hasVocabularyP1Outcome` completion policy](../../engine/src/main/kotlin/app/astrid/engine/PracticeCompletionPolicy.kt)
and OpenAPI minimum. The board widget already iterates supplied rows; no new interaction
model is indicated.

**Proposed script choice:** retain two-pair matching; runtime adoption remains open. Add no filler
to reach three. Ownership is the selected cup/bottle meaning inside its exact
quantity construction, recording beverage and constituent exposure separately. Do not add
two lexical placements or award independent beverage-production success. Specify any
broader compound-meaning target before implementation.

## Reading roles and language

P1 derives answers from structured fact values, but `READING_ROLES` accepts only object/place/time:
[PracticeModels.kt](../../engine/src/main/kotlin/app/astrid/engine/PracticeModels.kt).
An action support fact already exists; action assessment still needs target/schema/summary
support. Subject questions must resolve a named referent or speaker without inventing
lexical entries for names. P2's last-event question needs the supplied purchase-then-payment
relation; sentence order alone is insufficient.

Preserve P3 destination versus static location in typed metadata. **Open choice:** a separate
destination role, or a place role with explicit directional classification. English scenes
can use existing presentation context. Inline “Bicycle display” and “Vehicle A/B” labels
need an English-labelled presentation convention, since
[reading rows inherit Danish markup](../../web/src/components/ExercisePrompt.tsx).
No dialogue engine is required.

## Tiles, morphology and scope

Whole strings already work as occurrence blocks, so `en bolle`, `til stationen`,
`til apoteket` and complete quantities can stay intact. Quantities need composite exposure
provenance instead of one misleading lemma-form key. Internal commas are a small genuine
gap: P1 supplies a trailing period,
[`SentenceOrderPrompt` appends terminal punctuation](../../web/src/components/SentenceOrderPrompt.tsx),
and evaluation space-joins blocks. Supply consistent punctuation metadata for prompt and
feedback; do not introduce a movable comma target.

Article bundles and definite nouns inside chunks do not activate productive morphology.
Supplied modal/order tiles do not independently test modal choice, conjugation or articles.
Direction/static contrasts remain meaning recognition, not productive preposition selection
or S14 fronting.

## Prerequisites and honest variability

Expand each outline's review ledger into visible teaching, translations and exact exposure
keys. Preserve the car cue for `kører`, rail context for `skifter`, possession/request contrast,
bare infinitive after `vil`, exact quantities and both sides of each spatial contrast.
English-only distractors create no Danish exposure. Known alternatives must come from that
session's visible material. Gratitude choices must specify the customer's intended thanking
act, distinguishing the customer’s intended thanks from attention-getting and item handover.

The reviewed outlines specified twenty definite P2 activities plus an unscheduled alternate,
and twenty-three P3 activities with two contrast transports unresolved. The [P2 complete scripts](vocabulary-food-shopping-scripts.md) and
[P3 complete scripts](vocabulary-places-journeys-scripts.md) propose **21 P2 activities** (including the taught bottle request)
and **23 P3 activities** (using closed choices between correct and reversed mappings).
They supply exact banks, translations and direct-entry review. These are authoring proposals;
runtime selection, target representation and persistence remain separate. Matching consistently
uses English-left/Danish-right `meaning/en-da`. Count authored semantic variants separately
from shuffled presentation variants. The bottle request is another semantic variant only
after its own exposure; resemblance to the cup request does not prove unseen transfer.

## Conditional work packages

1. **Content, separately by packet:** complete scripts, banks, contrast shapes, alternate
   scheduling, target ownership and prerequisite ledgers. Accept when every slot has one
   primary assessment, exact answers/known errors and visible support.
2. **Coordinator, then engine:** freeze delivered P1 before changes; implement bounded kinds,
   sense-aware references, reading projections, quantity/punctuation metadata and packet
   policies. Serialize shared-model/planner edits. Require literal coverage, finite variation,
   full direct-entry teaching, honest unknowns and separate operation evidence.
3. **API and UI after interface agreement:** API owns DTOs, fingerprints, validation, OpenAPI,
   generated types and profiles; UI owns rendering, directory and journeys. Extend the
   [`StoredPracticeSession.toResponse` P1-only summary gate](../../server/src/main/kotlin/app/astrid/server/practice/PracticeMapping.kt)
   explicitly. Preserve private answers and guard-before-validation behavior; API-VER-002,
   API-SEC-003, API-DOC-001 and API-CONF-001 apply.
4. **Independent review and verification:** cover every slot, new reading roles, two-pair
   partial/full outcomes, punctuation, composite exposure, latest-attempt continuation,
   fingerprint mutations and retained replay. Complete repository/browser gates, including
   Docker-backed tests, for any future delivery.

No new endpoint or database table is presently indicated. Stored payload additions still
need compatible readers; rollback must preserve them. This assessment used code/document
inspection only. It authorizes no implementation or runtime activation.
