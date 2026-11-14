# P3 Places and Journeys: Complete Script Draft

**Latest status — 14 November 2026:** the [activation and delivery contract](vocabulary-places-journeys-activation.md)
records released app0.23.0/pair20, six profiles/23 activities and cumulative-review-3. The
scripts below preserve the original authoring proposals and source-reviewed draft; their
pre-selection statements are historical, not the current runtime status. Specialist teaching
approval remains pending. Partial matching submissions are accepted incomplete boards;
duplicate endpoints and out-of-bank IDs remain malformed.

## Historical authoring status, authority and proposals

This is an **authoring draft for independent source/content and Danish-specialist review**. It expands the
six reviewed P3 outlines in [places/journeys placement and source packet](vocabulary-places-journeys-packet.md); it is not a runtime
contract, teacher approval, release selection or claim that the current engine supports every activity.
At authoring time P3 activation was unselected; the later runtime contract supersedes that
status and adopts the bounded choices below. S14 remains paused, and P1 release work was separate. This draft
allocates no stable lexical/sense IDs, activity IDs, profile revisions, generator/evaluator versions,
learning pair, app version or persistence policy.

The source packet remains authoritative for **21 placements: 5 reused and 16 new**, introduced in batches
of **4 + 4 + 4 + 3 + 3 + 3**. Review appearances do not create placements. The six scripts retain exactly
**23 activities** (`4 + 4 + 4 + 3 + 4 + 4`).

The following are explicit **authoring proposals**, not selected runtime decisions:

1. Resolve Session 3's home contrast as one paired closed choice. The two choices are the correct mapping
   (`hjemme` = static at home; `hjem` = homeward destination) and its exact reversal.
2. Resolve Session 5's station contrast the same way. The choices are the correct mapping
   (`til stationen` = destination; `på stationen` = static location) and its exact reversal.
   These activities assess bounded meaning contrasts, not productive preposition selection.
3. Give each spatial fact a content-level typed role: **destination** for a movement endpoint and
   **static location** for where a state/activity holds. A reading question owns the requested role and
   exact fact. This is more precise than a generic “place” label, but the future runtime representation
   remains open.
4. Keep “Bicycle display,” “Vehicle A,” and “Vehicle B” as English presentation labels outside the
   Danish reading text. Keep the Session 5 car scene and Session 6 rail scene as required English
   context; neither adds a Danish vehicle phrase.

No new dictionary wording, corpus sentence or source claim is introduced. All Danish below comes from
the existing P3 ledger, allowed readings/frames or expressly named support. Unsupported constructions
are described in English rather than rendered as new Danish examples.

## Exposure and response conventions

Every session is independently enterable. It visibly teaches its own batch and visibly reviews every
Danish alternative or prerequisite used in that session. It assumes no prior session, P1 completion,
browser history, global learning or global exposure.

The backticked labels are **descriptive authoring-audit labels only**, not stable runtime keys:

- `surface:` — exact visible Danish text;
- `meaning:` — an explicit Danish-to-English selected-meaning mapping;
- `bundle:` — a whole article+noun or supplied location/destination teaching unit;
- `form:` — a visibly explained infinitive/present relation;
- `frame:` — the exact bounded construction/order shown and translated;
- `fact:` — the complete proposition or scene relation;
- `spatial:` — the fact's explicit destination or static-location classification;
- `support:` — visible prerequisite/presentation material with no P3 target ownership.

English-only distractors expose no Danish counterpart. A Danish choice/tile exposes its surface; a
shuffled matching prompt does not expose the correct bilingual correspondence until a correct mapping
or rendered solution confirms it. A finite reading form does not expose its unseen infinitive. Answer
reveal/retry is possible exposure, never proof of learning or independent recall.

Every bilingual matching board uses the established **English-left / Danish-right** `meaning/en-da`
direction. Danish→English and English→Danish closed choices retain their separately stated directions.

For a closed bank, unknown/duplicate option IDs or out-of-bank responses are malformed after ownership
checks, not learner-language judgments. Displayed introduced alternatives are known and off-target when
they miss the requested fact or meaning. Unintroduced forms, synonyms, constructions and unsupported
complete tile orders remain linguistically **unknown**, even when a response-shape failure can be decided.
“Unknown” never means “bad Danish.”

## Session 1 — Two ways to travel

**Placement ownership:** P3-01 `en bil` (reuse), P3-02 `en cykel` (new), P3-03
`cykle → cykler` (new), P3-04 `nu` (reuse). **Outcome:** understand a labelled car fact, a bicycle
display and who is travelling by bicycle now.

### Direct-entry teaching script

1. Show `en bil = a car`; keep `en` with the count noun. Show possession support `har = has`, and
   explain that Anna and Nora are names.
   Exposure: `surface:en bil`, `meaning:en bil=a car`, `bundle:en bil=count`, `surface:har`,
   `support:har=has`, `support:Anna=name`, `support:Nora=name`.
2. Show an English-labelled **Bicycle display**, then `en cykel = a bicycle`; keep `en` with the
   count noun. Review supplied identification `Det er … = This is …`.
   Exposure: `surface:en cykel`, `meaning:en cykel=a bicycle`, `bundle:en cykel=count`,
   `surface:Det er`, `support:Det er=This is`, `support:Bicycle display=English label`.
3. Show an English bicycle-travel scene and `at cykle → cykler`: “`at cykle` means **to travel by
   bicycle**. `cykler` means **travels/is travelling by bicycle**.” It is not taught as generic “go.”
   Exposure: `surface:at cykle`, `surface:cykler`, `meaning:cykle=travel by bicycle`,
   `meaning:cykler=travels/is travelling by bicycle`, `form:cykle→cykler`.
4. Show `nu = now`, the current time, as one final time constituent in the cycling clause.
   Exposure: `surface:nu`, `meaning:nu=now`, `bundle:nu=final time constituent`.
5. Show the exact labelled reading:

   > Anna har en bil.  
   > [Bicycle display:] Det er en cykel.  
   > Nora cykler nu.

   Give the exact translation: **“Anna has a car. [Bicycle display:] This is a bicycle. Nora is
   travelling by bicycle now.”**
   Exposure: `surface:Anna har en bil.`, `surface:Det er en cykel.`,
   `surface:Nora cykler nu.`, `frame:Anna|har|en bil`, `frame:Det er|en cykel`,
   `frame:Nora|cykler|nu`, `fact:Anna|has|a car`, `fact:display|identifies|a bicycle`,
   `fact:Nora|travels by bicycle|now`.

### Activities and exact grading material

| No. | Activity, exact bank and primary ownership | Exact answer | Reviewed known off-targets and unknowns |
| --- | --- | --- | --- |
| 1.1 | Danish→English recognition. Cue `en cykel`; choices `a bicycle / a car / a station`. Primary target: P3-02 selected bundle meaning. | `a bicycle`. | `a car` is a reviewed known meaning but off-target. `a station` is English-only and exposes no Danish. Out-of-bank input is malformed. |
| 1.2 | Three-pair `meaning/en-da` matching. English left: `a car`, `a bicycle`, `travels by bicycle`. Danish right: `en bil`, `en cykel`, `cykler`. Atomic primary targets: P3-01, P3-02 and the present surface of P3-03. | The three literal English→Danish pairs. | A complete wrong cross-pairing is known and off-target pair by pair. Partial one-to-one selections are accepted and graded: unmatched rows are unknown and the complete-board constraint is not met. Duplicate endpoints or out-of-bank IDs are malformed. |
| 1.3 | Explicit-subject reading. Cue “Who is travelling by bicycle now?” Choices `Nora / Anna`. Primary target: subject of `fact:Nora|travels by bicycle|now`. | `Nora`. | Anna is a known subject of the car-possession fact and is off-target. The bicycle display has no person referent. |
| 1.4 | All-word tiles. Cue “Write **Nora is travelling by bicycle now**. Begin with Nora.” Tiles `[Nora] [cykler] [nu]`; period supplied. Primary target: exact subject-first cycling frame/order. | `Nora cykler nu.` | No different complete known answer is formable. Incomplete/duplicate use fails shape; any other complete order is language-unknown/off-target, not automatically ungrammatical. |

**Session boundary:** Recognition/matching do not establish production. The session does not teach a
driving frame for `en bil`, an arbitrary vehicle substitution or any later vehicular verb.

## Session 2 — Walking to the station and home

**Placement ownership:** P3-05 `en station`, P3-06 `gå → går`, P3-07 destination chunk
`til stationen`, P3-08 directional `hjem` (all new). **Outcome:** distinguish a station destination
from homeward direction and assemble one exact walking proposition.

### Direct-entry teaching script

1. Show `en station = a station`; keep `en` with the count noun. Review `Det er … = This is …` and
   explain that Anna and Nora are names.
   Exposure: `surface:en station`, `meaning:en station=a station`, `bundle:en station=count`,
   `surface:Det er`, `support:Det er=This is`, `support:Anna=name`, `support:Nora=name`.
2. Show a person walking and `at gå → går`: “`at gå` means **to walk/go on foot**. `går` means
   **walks/goes on foot** in the exact destination frames taught here.”
   Exposure: `surface:at gå`, `surface:går`, `meaning:gå=walk/go on foot`,
   `meaning:går=walks/goes on foot`, `form:gå→går`.
3. Show `til stationen = to the station` as one complete **destination** chunk. The definite-looking
   noun occurs only inside this chunk; do not split or generalize it.
   Exposure: `surface:til stationen`, `meaning:til stationen=to the station`,
   `bundle:til stationen=destination chunk`, `spatial:til stationen=destination`.
4. Show directional `hjem = home/homeward`. Explain in English: “Here this is a movement destination,
   not a statement that someone is already at home.”
   Exposure: `surface:hjem`, `meaning:hjem=home/homeward`, `spatial:hjem=destination/direction`.
5. Show and translate the exact reading:

   > Det er en station.  
   > Anna går til stationen.  
   > Nora går hjem.

   Exact translation: **“This is a station. Anna walks to the station. Nora walks home.”**
   Exposure: `surface:Det er en station.`, `surface:Anna går til stationen.`,
   `surface:Nora går hjem.`, `frame:Det er|en station`, `frame:Anna|går|til stationen`,
   `frame:Nora|går|hjem`, `fact:display|identifies|a station`,
   `fact:Anna|walks|to the station`, `spatial:Anna fact=destination`,
   `fact:Nora|walks|homeward`, `spatial:Nora fact=destination/direction`.

### Activities and exact grading material

| No. | Activity, exact bank and primary ownership | Exact answer | Reviewed known off-targets and unknowns |
| --- | --- | --- | --- |
| 2.1 | Four-pair `meaning/en-da` matching. English left: `a station`, `walks/goes on foot`, `to the station`, `home/homeward`. Danish right: `en station`, `går`, `til stationen`, `hjem`. Atomic primary targets: P3-05, the present surface of P3-06, P3-07 and P3-08. | The four literal English→Danish pairs. | Wrong cross-pairings are known/off-target pair by pair. Partial one-to-one selections are accepted and graded: unmatched rows are unknown and the complete-board constraint is not met. Duplicate endpoints or out-of-bank IDs are malformed. The station noun bundle and destination chunk remain distinct targets. |
| 2.2 | Explicit-destination reading. Cue “Where is Anna going?” Choices `to the station / home`. Primary target: destination of `fact:Anna|walks|to the station`. | `to the station`. | `home` is Nora's known destination and is off-target for Anna. This is a destination role, not generic static place. |
| 2.3 | Closed spatial-meaning choice. Cue `Nora går hjem.` Choices `destination/direction / static location`. Primary target: typed meaning `spatial:hjem=destination/direction`. | `destination/direction`. | `static location` is a known English category but off-target here; it exposes no Danish counterpart. |
| 2.4 | All-word/block tiles. Cue “Write **Anna walks to the station**. Begin with Anna.” Blocks `[Anna] [går] [til stationen]`; period supplied. Primary target: exact subject-first walking/destination frame. | `Anna går til stationen.` | The destination stays one block. Incomplete/duplicate use fails shape; any other complete order is language-unknown/off-target. No standalone definite noun or productive preposition target is derived. |

**Session boundary:** The complete destination proposition is taught before assembly. This session does
not teach productive `til + noun`, definite morphology, arbitrary destinations or a static station phrase.

## Session 3 — City, home and library

**Placement ownership:** P3-09 static `hjemme` (reuse), P3-10 `bo → bor` (reuse), P3-11
`i Aarhus/i Odense` (reuse), P3-12 `et bibliotek` (new). **Outcome:** identify residence, a static
home location and a labelled library, then distinguish static home from homeward destination.

### Direct-entry teaching script

1. Show `at bo → bor`: “`at bo` means **to live/reside**. `bor` means **lives/resides** in the
   supplied city frame.” Explain that Anna and Nora are names.
   Exposure: `surface:at bo`, `surface:bor`, `meaning:bo=live/reside`,
   `meaning:bor=lives/resides`, `form:bo→bor`, `support:Anna=name`, `support:Nora=name`.
2. Show both supplied static city chunks: `i Aarhus = in Aarhus` and `i Odense = in Odense`.
   They license only the named-city residence frame here.
   Exposure: `surface:i Aarhus`, `surface:i Odense`, `meaning:i Aarhus=in Aarhus`,
   `meaning:i Odense=in Odense`, `bundle:i Aarhus/i Odense=static named-city chunks`,
   `spatial:i Aarhus=static location`, `spatial:i Odense=static location`.
3. Show static `hjemme = at home`. Review `arbejder = works/is working`. Explain that the complete
   supported use says where someone works; it does not describe movement.
   Exposure: `surface:hjemme`, `meaning:hjemme=at home`, `spatial:hjemme=static location`,
   `surface:arbejder`, `meaning:arbejder=works/is working`.
4. Because the contrast activity uses it, visibly review directional `hjem = home/homeward` from a
   movement scene. State the contrast plainly: `hjemme` is static; `hjem` is homeward destination.
   Exposure: `surface:hjem`, `meaning:hjem=home/homeward`,
   `spatial:hjem=destination/direction`, `support:hjemme≠hjem contrast`.
5. Show `et bibliotek = a library`; keep `et` with the count noun. Review supplied
   `Det er … = This is …` only for the exact identification.
   Exposure: `surface:et bibliotek`, `meaning:et bibliotek=a library`,
   `bundle:et bibliotek=count`, `surface:Det er`, `support:Det er=This is`.
6. Show and translate the reading:

   > Anna bor i Odense.  
   > Nora arbejder hjemme.  
   > Det er et bibliotek.

   Exact translation: **“Anna lives in Odense. Nora works at home. This is a library.”**
   Exposure: `surface:Anna bor i Odense.`, `surface:Nora arbejder hjemme.`,
   `surface:Det er et bibliotek.`, `frame:Anna|bor|i Odense`,
   `frame:Nora|arbejder|hjemme`, `frame:Det er|et bibliotek`,
   `fact:Anna|lives|in Odense`, `spatial:Anna residence=static location`,
   `fact:Nora|works|at home`, `spatial:Nora work=static location`,
   `fact:display|identifies|a library`.

### Activities and exact grading material

| No. | Activity, exact bank and primary ownership | Exact answer | Reviewed known off-targets and unknowns |
| --- | --- | --- | --- |
| 3.1 | Four-pair `meaning/en-da` matching. English left: `lives/resides`, `in Odense`, `at home`, `a library`. Danish right: `bor`, `i Odense`, `hjemme`, `et bibliotek`. Atomic primary targets: the present surface of P3-10, P3-11's Odense chunk, P3-09 and P3-12. | The four literal English→Danish pairs. | Complete wrong cross-pairings are known/off-target pair by pair. Partial one-to-one selections are accepted and graded: unmatched rows are unknown and the complete-board constraint is not met. Duplicate endpoints or out-of-bank IDs are malformed. |
| 3.2 | Explicit static-location reading. Cue “Where does Anna live?” Choices `in Odense / at home / at the station`. Primary target: static residence location in `fact:Anna|lives|in Odense`. | `in Odense`. | `at home` is a reviewed Danish meaning tied to Nora's separate fact and is known/off-target. `at the station` is English-only; it exposes no future Danish surface. |
| 3.3 | Explicit static-location reading. Cue “Where does Nora work?” Choices `at home / in Odense`. Primary target: static work location in `fact:Nora|works|at home`. | `at home`. | `in Odense` is a known static chunk in Anna's separate residence fact and is off-target. |
| 3.4 | **Proposed paired closed contrast.** Cue “Choose the exact taught mapping.” Choice A: `hjemme` = `static at home`; `hjem` = `homeward destination`. Choice B reverses those two English classifications. Primary target: the bounded two-form spatial contrast. | Choice A, the correct mapping. | Choice B uses both known displayed surfaces/meanings but reverses them, so the complete response is known and off-target. Out-of-bank input is malformed. No free substitution or productive morphology is assessed. |

**Session boundary:** City chunks and `hjemme` are static meanings; `hjem` is directional. The library is
identified only. The session does not license arbitrary city/building phrases, library destinations or a
new residence frame beyond the supplied named-city use.

## Session 4 — Ticket, bus and train

**Placement ownership:** P3-13 `en bus`, P3-14 transport-sense `et tog`, P3-15 `en billet` (all
new). **Outcome:** understand a labelled vehicle display and the exact possession fact about a ticket.

### Direct-entry teaching script

1. Show `en bus = a bus`, `et tog = a train`, and `en billet = a ticket` on separate cards. Keep
   each article+noun bundle intact. Explain in English that the selected `tog` is the train vehicle.
   Exposure: `surface:en bus`, `meaning:en bus=a bus`, `bundle:en bus=count`,
   `surface:et tog`, `meaning:et tog=a train`, `bundle:et tog=count transport vehicle`,
   `surface:en billet`, `meaning:en billet=a ticket`, `bundle:en billet=count`.
2. Review `har = has`, Anna as a name, and `Det er … = This is …`. Introduce English presentation
   labels **Vehicle A** and **Vehicle B**; the labels are not Danish reading text.
   Exposure: `surface:har`, `support:har=has`, `support:Anna=name`, `surface:Det er`,
   `support:Det er=This is`, `support:Vehicle A/B=English labels`.
3. Show the exact labelled reading:

   > Anna har en billet.  
   > [Vehicle A:] Det er en bus.  
   > [Vehicle B:] Det er et tog.

   Exact translation: **“Anna has a ticket. [Vehicle A:] This is a bus. [Vehicle B:] This is a
   train.”**
   Exposure: `surface:Anna har en billet.`, `surface:Det er en bus.`,
   `surface:Det er et tog.`, `frame:Anna|har|en billet`, `frame:Det er|en bus`,
   `frame:Det er|et tog`, `fact:Anna|has|a ticket`, `fact:Vehicle A|identifies|a bus`,
   `fact:Vehicle B|identifies|a train`.

### Activities and exact grading material

| No. | Activity, exact bank and primary ownership | Exact answer | Reviewed known off-targets and unknowns |
| --- | --- | --- | --- |
| 4.1 | Three-pair `meaning/en-da` matching. English left: `a bus`, `a train`, `a ticket`. Danish right: `en bus`, `et tog`, `en billet`. Atomic primary targets: P3-13, P3-14 and P3-15 selected bundle meanings. | The three literal English→Danish pairs. | Wrong cross-pairings are known/off-target pair by pair. Partial one-to-one selections are accepted and graded: unmatched rows are unknown and the complete-board constraint is not met. Duplicate endpoints or out-of-bank IDs are malformed. |
| 4.2 | Explicit possession-object reading. Cue “What does Anna have?” Choices `a ticket / a bus / a train`. Primary target: object of `fact:Anna|has|a ticket`. | `a ticket`. | Bus/train are known displayed vehicles but only identified by separate labels; neither is asserted as Anna's possession. Both are known/off-target. |
| 4.3 | English→Danish closed choice. Cue “Choose the introduced bundle for **a train**.” Choices `en bus / et tog / en billet`. Primary target: selected P3-14 bundle meaning. | `et tog`. | `en bus` and `en billet` are known valid bundles with different selected meanings and are off-target. No typed article recall is assessed. |

**Session boundary:** The labelled display does not say Anna owns a vehicle or that her ticket is valid
for either one. This session adds no means-of-transport, boarding, route or definite-vehicle grammar.

## Session 5 — Waiting at the station

**Placement ownership:** P3-16 vehicular `køre → kører`, P3-17 `vente → venter`, P3-18 static
chunk `på stationen` (all new). **Outcome:** identify where Anna waits, distinguish the static station
chunk from the station destination, and recall the selected vehicular present form from a car-specific cue.

### Direct-entry teaching script

1. Show an English scene: **“Nora is travelling home by car.”** Review `en bil = a car`, then show
   `at køre → kører`: “Here `at køre` means **to travel/drive using a vehicle**. `kører` means
   **travels/drives by vehicle**. The car scene selects this action.” No Danish vehicle phrase is added.
   Exposure: `surface:en bil`, `meaning:en bil=a car`, `support:car-travel scene`,
   `surface:at køre`, `surface:kører`, `meaning:køre=travel/drive by vehicle`,
   `meaning:kører=travels/drives by vehicle`, `form:køre→kører`.
2. Show `at vente → venter`: “`at vente` means **to wait**. `venter` means **waits/is waiting**
   in the exact static station frame taught here.”
   Exposure: `surface:at vente`, `surface:venter`, `meaning:vente=wait`,
   `meaning:venter=waits/is waiting`, `form:vente→venter`.
3. Show `på stationen = at the station` as one complete **static-location** chunk used with
   `venter`. Do not split its definite noun or generalize its preposition.
   Exposure: `surface:på stationen`, `meaning:på stationen=at the station`,
   `bundle:på stationen=static station chunk`, `spatial:på stationen=static location`.
4. Review names, directional `hjem = home/homeward`, destination `til stationen = to the station`,
   and `cykler = travels by bicycle`. State the contrasts: vehicle-specific `kører` versus bicycle
   `cykler`; destination `til stationen` versus static `på stationen`.
   Exposure: `support:Anna=name`, `support:Nora=name`, `surface:hjem`,
   `meaning:hjem=home/homeward`, `spatial:hjem=destination/direction`,
   `surface:til stationen`, `meaning:til stationen=to the station`,
   `spatial:til stationen=destination`, `surface:cykler`,
   `meaning:cykler=travels by bicycle`.
5. Re-state the required English car context and show:

   > Anna venter på stationen.  
   > Nora kører hjem.

   Exact translation: **“Anna waits at the station. Nora travels/drives home by vehicle.”** The
   supplied English situation specifies that Nora is travelling by car; the Danish sentence itself does
   not add a named vehicle phrase.
   Exposure: `surface:Anna venter på stationen.`, `surface:Nora kører hjem.`,
   `frame:Anna|venter|på stationen`, `frame:Nora|kører|hjem`,
   `fact:Anna|waits|at the station`, `spatial:Anna waiting=static location`,
   `fact:Nora|travels by vehicle|homeward`, `spatial:Nora travel=destination/direction`,
   `support:Nora travel vehicle=car`.

### Activities and exact grading material

| No. | Activity, exact bank and primary ownership | Exact answer | Reviewed known off-targets and unknowns |
| --- | --- | --- | --- |
| 5.1 | Three-pair `meaning/en-da` matching. English left: `travels/drives by vehicle`, `waits`, `at the station`. Danish right: `kører`, `venter`, `på stationen`. Atomic primary targets: the present surfaces of P3-16/P3-17 and chunk P3-18. | The three literal English→Danish pairs. | Wrong cross-pairings are known/off-target. Partial one-to-one selections are accepted and graded: unmatched rows are unknown and the complete-board constraint is not met. Duplicate endpoints or out-of-bank IDs are malformed. The English car scene remains required for later recall. |
| 5.2 | Explicit static-location reading. Cue “Where does Anna wait?” Choices `at the station / to the station`. Primary target: static location of `fact:Anna|waits|at the station`. | `at the station`. | `to the station` is a known destination meaning and is off-target for the stated waiting fact. |
| 5.3 | **Proposed paired closed contrast.** Cue “Choose the exact taught mapping.” Choice A: `til stationen` = `destination`; `på stationen` = `static location`. Choice B reverses those classifications. Primary target: the bounded destination/static station contrast. | Choice A, the correct mapping. | Choice B uses two visibly reviewed known chunks but reverses their classifications, so it is known and off-target. Out-of-bank input is malformed. No productive preposition selection is assessed. |
| 5.4 | One-word present-form recall. Scene cue: “The person is travelling home by car. Type the introduced present form of `at køre`.” Primary target: selected vehicular present form. | `kører`. | `køre` is the known infinitive but misses the requested present form. `cykler` is a known present form for bicycle travel and misses the car-selected action. `venter` is a known present form for waiting and misses the action. Any unintroduced form/synonym or extra prose is unknown; extra words fail one-word shape. |

**Session boundary:** The car scene is mandatory; a broad “goes home” cue would not distinguish the
selected action fairly. The station contrast is meaning recognition only. No arbitrary waiting object,
vehicle phrase, preposition rule, definite-noun production or S14 fronting is introduced.

## Session 6 — A change and the pharmacy

**Placement ownership:** P3-19 rail-context `skifte → skifter`, P3-20 `et apotek`, P3-21
destination chunk `til apoteket` (all new). **Outcome:** understand an English-supported rail change,
identify Nora's pharmacy destination, and assemble a first-person version of the taught destination frame.

### Direct-entry teaching script

1. Show the required English scene: **“Anna is travelling by train and changes trains/connections in
   Aarhus.”** Review `et tog = a train` and `i Aarhus = in Aarhus`. Then show
   `at skifte → skifter`: “In this rail scene, `skifter` means **changes trains/connections**.”
   The rail context is part of the selected meaning; no Danish vehicle object is added.
   Exposure: `surface:et tog`, `meaning:et tog=a train`, `surface:i Aarhus`,
   `meaning:i Aarhus=in Aarhus`, `support:English rail scene`, `surface:at skifte`,
   `surface:skifter`, `meaning:skifte=change trains/connections`,
   `meaning:skifter=changes trains/connections`, `form:skifte→skifter`.
2. Show `et apotek = a pharmacy`; keep `et` with the count noun. Then show
   `til apoteket = to the pharmacy` as a complete destination chunk. The definite-looking form is
   taught only inside that chunk.
   Exposure: `surface:et apotek`, `meaning:et apotek=a pharmacy`, `bundle:et apotek=count`,
   `surface:til apoteket`, `meaning:til apoteket=to the pharmacy`,
   `bundle:til apoteket=destination chunk`, `spatial:til apoteket=destination`.
3. Review `går = walks/goes on foot`, directional `hjem = home/homeward`,
   `venter på stationen = waits at the station`, `jeg = I`, and Anna/Nora as names. Every Danish
   alternative used in activity 6.2 is now visible.
   Exposure: `surface:går`, `meaning:går=walks/goes on foot`, `surface:hjem`,
   `meaning:hjem=home/homeward`, `spatial:hjem=destination/direction`,
   `surface:venter på stationen`, `meaning:venter på stationen=waits at the station`,
   `spatial:venter på stationen=static location`, `surface:jeg`, `support:jeg=I`,
   `support:Anna=name`, `support:Nora=name`.
4. With the rail scene still visible, show and translate:

   > Anna skifter i Aarhus.  
   > Nora går til apoteket.  
   > Anna går hjem.

   Exact translation: **“Anna changes trains/connections in Aarhus. Nora walks to the pharmacy.
   Anna walks home.”** These are separate facts; no event chronology is asserted.
   Exposure: `surface:Anna skifter i Aarhus.`, `surface:Nora går til apoteket.`,
   `surface:Anna går hjem.`, `frame:Anna|skifter|i Aarhus`,
   `frame:Nora|går|til apoteket`, `frame:Anna|går|hjem`,
   `fact:Anna|changes trains/connections|in Aarhus`, `support:Anna change context=rail`,
   `fact:Nora|walks|to the pharmacy`, `spatial:Nora walk=destination`,
   `fact:Anna|walks|homeward`, `spatial:Anna walk=destination/direction`.
5. Before the final activity, show and translate the exact worked transfer:
   `Jeg går til apoteket.` = **“I walk to the pharmacy.”** Explain that only the supplied subject
   changes; the verb sense and exact destination chunk are already taught.
   Exposure: `surface:Jeg går til apoteket.`, `frame:Jeg|går|til apoteket`,
   `fact:speaker(jeg)|walks|to the pharmacy`, `spatial:speaker walk=destination`.

### Activities and exact grading material

| No. | Activity, exact bank and primary ownership | Exact answer | Reviewed known off-targets and unknowns |
| --- | --- | --- | --- |
| 6.1 | Three-pair `meaning/en-da` matching. English left: `changes trains/connections`, `a pharmacy`, `to the pharmacy`. Danish right: `skifter`, `et apotek`, `til apoteket`. Atomic primary targets: the rail-context present surface of P3-19, P3-20 and P3-21. | The three literal English→Danish pairs. | Wrong cross-pairings are known/off-target pair by pair. Partial one-to-one selections are accepted and graded: unmatched rows are unknown and the complete-board constraint is not met. Duplicate endpoints or out-of-bank IDs are malformed. The rail context remains part of `skifter`'s selected meaning. |
| 6.2 | Explicit-action reading with the rail scene visible. Cue “What does Anna do in Aarhus?” Choices `changes trains/connections / waits at the station / walks home`. Primary target: action in `fact:Anna|changes trains/connections|in Aarhus`. | `changes trains/connections`. | The other two actions have visibly reviewed Danish surfaces. Walking home is another fact about Anna but not what the rail-context sentence says she does in Aarhus; waiting is not asserted. Both are known/off-target. |
| 6.3 | Explicit-destination reading. Cue “Where does Nora walk?” Choices `to the pharmacy / home`. Primary target: destination in `fact:Nora|walks|to the pharmacy`. | `to the pharmacy`. | `home` is Anna's known destination in a separate fact and is off-target for Nora. No chronology or pronoun inference is used. |
| 6.4 | All-word/block tiles. Cue “Write **I walk to the pharmacy**. Begin with the subject.” Blocks `[Jeg] [går] [til apoteket]`; period supplied. Primary target: exact first-person subject-first destination frame/order. | `Jeg går til apoteket.` | No different complete known answer is formable. Incomplete/duplicate use fails shape; any other complete order is language-unknown/off-target. The destination chunk remains intact and creates no preposition/definiteness subscore. |

**Session boundary:** `skifter` is the rail-change sense only when the English rail situation is present.
The final transfer changes only a supplied subject after the whole proposition is rendered. This session
does not license object-taking change/wait verbs, arbitrary routes, medicine/service language, productive
definiteness or S14 fronting.

## Packet-wide target and exposure audit

| Session | New placement batch | Reused placement owners | Primary activity ownership | Visible support/review that creates no placement |
| --- | --- | --- | --- | --- |
| 1 | P3-02, P3-03 | P3-01, P3-04 | bicycle bundle meaning; three lexical mappings; cyclist subject; exact cycling-clause order | names, `har`, `Det er`, English bicycle-display label |
| 2 | P3-05–P3-08 | None | four mappings; Anna destination; directional `hjem`; exact station-destination order | names, `Det er`, English destination/static explanation |
| 3 | P3-12 | P3-09–P3-11 | four mappings; Anna residence location; Nora work location; paired home contrast | names, `arbejder`, `Det er`, visible review of `hjem` |
| 4 | P3-13–P3-15 | None | three bundle mappings; possession object; Danish train choice | Anna, `har`, `Det er`, English Vehicle A/B labels |
| 5 | P3-16–P3-18 | None | three mappings; waiting location; paired station contrast; vehicular present recall | names, `en bil`, `hjem`, `til stationen`, `cykler`, English car scene |
| 6 | P3-19–P3-21 | None | three mappings; rail action; Nora destination; first-person pharmacy-frame order | `et tog`, `i Aarhus`, `går`, `hjem`, `venter på stationen`, `jeg`, names, English rail scene |

The placement audit remains **5 reuse / 16 new / 21 total**. The 23 activities are
**4/4/4/3/4/4**. Review rows do not alter placement counts. A matching board is one numbered activity
with an atomic selected-meaning result per pair. Each reading owns only its named subject, action,
object, destination or static-location fact. Each tile activity owns only the exact supplied frame/order;
supplied forms/chunks do not silently earn recall, article, definiteness, preposition or S14 scores.

## Review questions and known implementation gaps

Independent review should decide or confirm the following before any runtime contract is selected:

1. Danish specialist review of every original sentence, English translation, cycling/walking/driving
   distinction, `kører hjem`, rail-context `skifter i Aarhus`, location/destination contrast and
   complete-beginner suitability remains pending. This draft is not teacher approval.
2. Confirm the paired closed-choice transports for activities 3.4 and 5.3. Their reversed mapping is an
   honest known off-target; neither activity is productive preposition or morphology assessment.
3. Confirm the content-level `destination` and `static location` roles. A future implementation must
   choose a compatible schema/summary representation without collapsing their meaning distinction.
4. Preserve the car cue for `kører` and the rail cue for `skifter`. Removing either context makes the
   action target under-specified. English presentation labels/scenes need a rendering path that is not
   marked as Danish text.
5. Preserve supplied chunks intact: `til stationen`, `på stationen`, `til apoteket`, `i Aarhus`,
   `i Odense`. Their visible definite nouns/prepositions do not activate productive definite morphology,
   arbitrary place substitution or S14 circumstance fronting.
6. Future runtime design still needs bounded subject/action/destination/static-location projections,
   contrast result ownership, scene metadata, exact exposure, summaries, fingerprints and compatible
   saved-payload readers. No endpoint or table is proposed here.
7. Verify every known-alternative set against the session's visible material. A word known only from
   another session is unknown unless reviewed here; closed-bank transport violations remain malformed,
   not learner-language outcomes.

