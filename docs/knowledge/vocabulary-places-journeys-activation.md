# P3 Places and Journeys Activation

## Selection and scope

**13 November 2026 — User-selected:** “OK, sounds like a plan, go” selects the recommended
P3 implementation: six short sessions and 23 activities, new introduced-material eligibility
in cumulative mixed review, independent review, focused commits/merge/push, app **0.23.0**,
an annotated GitHub release and backed-up app-only private-preview promotion. The
[complete scripts](vocabulary-places-journeys-scripts.md) and
[placement packet](vocabulary-places-journeys-packet.md) supply the bounded language.
Danish remains a linguistic draft awaiting specialist teaching review (BL-022).

Vocabulary library is a proposed subsequent milestone, not selected by this implementation.
S14 remains paused. No other packet, account, mastery/scheduler, free sentence generation,
audio/media, arbitrary route or productive preposition/definiteness rule is included.

## Sessions and assessment boundaries

| Session | Activities | Situation |
| --- | --- | --- |
| 1 | 4 | Two ways to travel: car identification, bicycle and cycling now |
| 2 | 4 | Walking to the station and home |
| 3 | 4 | Residence in a city, working at home and a labelled library |
| 4 | 3 | A ticket and labelled bus/train displays |
| 5 | 4 | Waiting at the station and vehicular travel home |
| 6 | 4 | An English-supported rail change and walking to the pharmacy |

Each session is independently enterable and visibly teaches/reviews every required Danish
surface, selected meaning, exact frame, known alternative and support fact before assessment.
Save introduction acknowledgment before the first grade. The 21 placements are five reused
and sixteen new placements; review appearances do not add records or outcomes.

The implementation assigns lexical records **L064–L079**, retaining each selected sense,
article/form bundle, source reference and use restriction. Together with the published 63
records this gives **79**. The packet adds no grammar skill IDs. Seven isolated format entries
**E045–E051** bring the advertised catalogue to **51**:

- E045: introduced Danish→English meaning selection.
- E046: English→Danish introduced article+noun bundle selection.
- E047: English-left/Danish-right matching with a separate meaning outcome per pair.
- E048: one-word selected vehicular present-form recall, G004 / `p3-kore-present`.
- E049: all-block supplied subject-first order, G005 / `p3-subject-first-supplied`.
- E050: explicit P3 reading facts, without a lexical or grammar primary outcome.
- E051: bounded spatial meaning selection, G016, with distinct home-direction,
  home-paired-contrast and station-paired-contrast scopes.

Matching assesses only the selected mappings. Reading owns one named fact and role, including
subject, object, action, destination or static location. Direction and static position remain
separate typed meanings. E051 contrasts have one whole-response outcome; they do not earn
separate recall, morphology or preposition outcomes for their displayed words.
Supplied tile constituents remain intact and do not establish conjugation, article recall,
productive definiteness, productive prepositions or S14 fronting.

Keep `kører` in an explicit English car-travel situation and `skifter` in an explicit English
rail-change situation. Generic “goes” or “changes” cues do not isolate those selected senses.
English display labels/scenes must not inherit Danish markup. Store and fingerprint their
meaning-bearing context alongside the named subject/action/spatial facts.

## Presentation and structured source contract

Retain current text/order/matching answer transports. Closed choices submit the exact supplied
string, not a new option-ID protocol. Append optional prompt fields, omitted when absent:

```text
passageLabels: [{ sentenceIndex: integer, text: string }]
choiceMappings: [{ choice: string, mappings: [{ danish: string, english: string }] }]
```

English passage labels have unique, in-range sentence indices and render outside the Danish
sentence markup. Each paired contrast has one `choiceMappings` entry per supplied choice,
two rows in authored order and separate Danish/English spans; submit its exact `choice`.
Both the correct and reversed mapping are public options; correctness stays private.
The exact submitted whole-choice strings are `hjemme = static at home; hjem = homeward destination`
and `hjemme = homeward destination; hjem = static at home` for the home contrast;
`til stationen = destination; på stationen = static location` and
`til stationen = static location; på stationen = destination` for the station contrast.
The associated mapping rows render the same two Danish surfaces/English classifications separately.
Car/rail scenes use the existing English `presentation.context`, retained in standalone,
practice and cumulative presentation. Recall separates its English instruction from `at køre`.

A P3 fact retains its complete authored proposition independently of the selected question:
stable fact and referent IDs, display/answer, predicate entry+sense, explicit action answer,
optional complement source and role, exact complement answer, Danish sentence/translation,
construction ID, optional required scene ID and visible exposure keys. Role values distinguish
`object`, `time`, `destination` and `static-location`. Existing `valueText/valueRole` project
the requested answer. Do not place spatial complements in an `objectSource` field.

Append optional `actionText`, `complementSource`, `complementRole`, `constructionId`,
`sceneId` to support-fact source/inspector metadata. Append optional `scene`
(`{ id, text, exposureKeys }`), `contrastId`, and empty-default `spatialMeanings`
(`{ form, lexicalSource, classification }`) to vocabulary task source metadata.
`lexicalSource` reuses the complete existing lexical-source object with entry/sense,
kind, forms, gloss and selected use; it is not a new flattened identity-only structure.
`classification` is the exact `destination` or `static-location` meaning. Pair-twenty fingerprints include every nested value/order and all public
labels/mappings/scenes. Older pair fingerprint branches remain literal and unchanged.

Use stable scalar identities and tie breakers for all shuffles; never hash an enum or a
data class containing one. Pair twenty must inherit stable E033/E042 behavior as well as
its own matching/tiles. Only literal pair seventeen keeps its historical matching exception.

The new lexical allocation is L064 cykel, L065 cykle, L066 station, L067 gå,
L068 til stationen, L069 directional hjem, L070 bibliotek, L071 bus, L072 transport tog,
L073 billet, L074 vehicular køre, L075 vente, L076 static på stationen, L077 rail skifte,
L078 apotek and L079 til apoteket, each with selected sense `-S01`.
Reuse L016-S01, L038-S01, L048-S01, L029-S01 and L032-S01 unchanged. The two city
surfaces belong to L032-S01. The exact new rail-context construction `skifter i Aarhus`
adds one packet-specific use permission for that city sense; it does not broaden residence,
old pools or arbitrary city frames. Show the final first-person pharmacy sentence before
assembly and describe that task as supplied-subject practice, not unseen transfer.

Completion requires saved teaching and a latest known outcome. Complete wrong choices or
boards can advance under the established policy; partial boards and unknown recall/orders
cannot. Present recall accepts `kører`; taught `køre`, `cykler`, `venter` remain known
wrong for distinct stated reasons. Untaught synonyms/forms remain unknown; extra prose
fails one-word shape. Neutral wrong-choice feedback must also cover English-only distractors
without claiming their Danish equivalents were introduced. A wrong whole contrast releases
no independent lexical success for either displayed form.

Expose only actually rendered forms. In particular `venter på stationen` does not expose
an unseen infinitive `vente`. Names/display labels are support IDs, not lexical records.
Reuse existing exposure identities only where semantics match; add packet-specific fact,
spatial, contrast, scene and supplied-construction keys.

## Revisions, persistence and mixed review

New exercises use literal **pilot-20 / generator-20 / evaluation-20**. Six profiles
`vocabulary-p3-s1` … `vocabulary-p3-s6` use their corresponding `-1` plan revisions and
separate versioned P3 completion/exposure policies. Existing P2/P1/grammar plans retain their
literal pairs, policies, private plans and grading behavior. Capture published pair nineteen,
P2 and cumulative-review-2 from unchanged 0.22.0 before any production edits. Existing golden
fixtures/provenance and applied migrations remain immutable.

New **cumulative-review-3** admits six taught P3 source families, for 32 eligible families.
Retained versions one/two keep their original 20/26-family eligibility, including explicit
new requests. All versions keep at most eight unique eligible activities, empty/short pools,
fixed 24-hour owner scope, saved private selections, original per-item evaluators and replay
before any new history query. Composite run headers stay `per-item`, not an engine pair.
A later lesson introduction must not change any saved review or expand an old version.

Reuse the existing creation/support/answer/advance transports and outboxes. No endpoint,
new database table or migration is indicated. New optional DTO/source metadata must omit
its defaults from old payloads. Public mappings exclude private keys; API schema correlations,
closed request validation and guard/ownership ordering remain binding.
Binary rollback after new payloads requires a reader that understands them, even when the
schema does not change. Restoring a backup is not an automatic rollback action.

## Required delivery evidence

- Actual unchanged-source archives and repeatable fresh-JVM generation, grading, private
  source, wire/fingerprint, P2 plan and cumulative-two evidence before production changes.
- Independent content review of all 23 slots and direct-entry teaching; exact script banks
  and known alternatives, no linguistic approval inferred from software tests.
- Stable scalar-only seeded ordering; fresh processes with deliberately different enum
  initialization histories, full public JSON and private fingerprint comparison.
- Complete authored activity coverage, partial matching and corrected retries, one-word
  recall shape/unknown boundaries, role-sensitive reading and meaning-context mutations.
- Strict OpenAPI generated types/conformance; no cross-family targets or leaked private keys.
- All required repository checks, actual Docker-backed tests, full Chromium/WebKit journeys,
  exact-source packaged Linux CI and unchanged runtime image security checks.
- Real 0.22→0.23 upgrade and actual app restart: stored creation/attempt replay, old pending
  first grades under original evaluators, saved private plans unchanged, new P3/cumulative
  grades and pending first grades, owner isolation and current-policy eligibility boundaries.
- Before main promotion, private custom-format backup and actual isolated restore verification;
  preserve the main PostgreSQL process, mounts, migration checksums and pre-existing rows.
- Focused commits, verified remote master/tag/release identities, live smoke verification and
  ownership-scoped QA cleanup. Remove regenerable scratch outputs after acceptance; retain
  unique recovery/release evidence compactly. No global Docker or volume pruning.

## Current operational baseline

At selection, clean master **844b88a** contains released app 0.22.0. After the user's rebuild,
the main preview uses a fresh Compose PostgreSQL volume (migrations 1/2/3); the former volume
and historical dump files were absent. New smoke-test evidence exists in that fresh database
and must be preserved. Grenadiers is also running and is outside this deployment scope.

An isolated ownership-labelled 0.22.0 QA stack holds ten saved runs: six P2 lessons, S05,
P1 and both retained cumulative versions. Nine current exercises await their first grade,
and one confirmed attempt has an exact replay. Public responses, saved private plans and
six-table hashes are captured privately for the future actual upgrade/restart checks.
This is prepared baseline evidence, not a completed P3 delivery claim.

## Unchanged-baseline compatibility discovery

The engine freeze from published 0.22.0 reproduced `NoSuchElementException` for P2
sessions 2/3/4 at `Int.MAX_VALUE`; a full cumulative-review-2 pool inherits the failure.
The new immutable archives record those released errors explicitly. This is not a P3
regression or an authoring correction. BL-072 tracks a separately versioned old-P2 repair;
P3 must handle its own supported seed range and must not accidentally change old graders,
plans or cumulative-version-two behavior while doing so.

**Bounded new-version decision:** pair nineteen adds seed/index as `Int` before modulo;
overflow can repeat a pool member and make the P2 planner's one-cycle search miss another.
The relevant window includes MAX−3 through MAX (the largest P2 scan index is four).
Only cumulative-review-3 may catch `NoSuchElementException` from the P2 source-plan
variation call, and only when `seed.toLong() + largestP2ScanIndex > Int.MAX_VALUE`.
Use the verified original source item with the source policy; retain its original pair,
seed/index, board IDs, assessments and exposure. Downstream creation still assigns a fresh
exercise UUID. Outside that arithmetic window, rethrow; never catch validation/fingerprint
conflicts, cancellation or arbitrary errors. Successful variation keeps its existing full
semantic-equivalence check. Versions one/two and all published P2 plans keep their old path.

New P3 generation uses widened seed/index addition before modulo. Verify MAX−4 through
MAX for all six P3 profiles and cumulative-three, including reversed source order, exact
fallback recipe/evidence, fresh-UUID pending-first-grade/restart/replay and rejection of
tampered source fingerprints. Older immutable failure rows must still match.

## Observed compatibility preparation — 13 November 2026

All six unchanged-0.22.0 archives were captured and compared across fresh default and
Arabic/Egypt JVMs: 4,991/194/33 engine rows and 7,099/214/25 wire rows. Guarded fixtures
and provenance are new immutable evidence; all 117 earlier protected resources retained
their hashes. Focused engine guards and 17 wire guards passed. The full required check
then passed with 465 engine and 319 server tests (zero failures/skips), unchanged coverage
floors and 318 web tests. Docker-backed tests actually executed with OrbStack's socket
set for Testcontainers. Initial exporter-complexity findings were fixed by splitting helpers;
a first full test run without that socket failed Docker discovery and is retained separately.
No production change, new lesson or version was included in this preparation.
