# Danish Vocabulary Seed

## Scope and review status

This is the 23-entry draft seed implemented in the private [beginner engine slice](beginner-slice-spec.md), not the final teaching vocabulary. It supports G001–G006 in the [grammar map](danish-grammar-map.md) and C001–C003 in the [sentence catalogue](danish-sentence-catalogue.md). The initial learner is an English-speaking complete beginner; contexts are general everyday Danish.

The seed balances four common-gender and four neuter count nouns to exercise article selection, plus five verbs, eight pronoun uses, and two article entries. This balance is an engineering fixture choice, not a frequency claim or a course syllabus. Selection is based on useful combinations and contrasting forms; no corpus frequency ranking has been performed.

Dictionary headings and grammatical sources have been checked; English glosses, semantic categories, examples, and the allowed-combination sets below are authored draft content. No Danish teacher has reviewed or approved this seed. Later content expansion needs broader topics, usage evidence, and linguistic review.

## Entry conventions

The L identifiers are stable content identifiers; word spelling alone is not identity. Each entry has a selected meaning and word class. Store future meanings and homonyms separately. An English gloss is a contextual teaching aid, not a guarantee of word-for-word equivalence in all sentences.

Active forms are those the pilot may generate. Reference forms help future data design but remain inactive until their grammar is introduced. A form can have multiple grammatical analyses; identity includes its feature bundle, not just its written surface.

## Pronouns and articles

| ID | Danish | Selected English use | Pilot restrictions |
| --- | --- | --- | --- |
| L001 | jeg | I | Human speaker; subject in C001/C002. |
| L002 | du | you, one addressee | Singular informal addressee; subject in C001/C002. |
| L003 | han | he | A person whose pronoun is established by the prompt; subject in C001/C002. |
| L004 | hun | she | A person whose pronoun is established by the prompt; subject in C001/C002. |
| L005 | vi | we | Human group including speaker; subject in C001/C002. |
| L006 | I | you, multiple addressees | Uppercase `I` is part of this pronoun's spelling; subject in C001/C002. |
| L007 | de | they, plural | Plural people established in context. Formal `De` and other uses are outside the pilot. |
| L008 | det | it/that in an identifying statement | Fixed subject of C003, `Det er …`; do not substitute it into C001/C002 or treat this as a general rule for pronoun reference. |
| L022 | en | a/an, common-gender singular | Unstressed indefinite article before a licensed common-gender count noun. Numeral `en/én` is a different use; its accent is optional. |
| L023 | et | a/an, neuter singular | Unstressed indefinite article before a licensed neuter count noun. Numeral `et/ét` is a different use; its accent is optional. |

The subject-pronoun inventory is supported by [Sproget.dk on pronouns](https://sproget.dk/typiske-problemer/pronomener-stedord/). The special spelling of `I` and lowercase `jer`/`jeres` is discussed in [Sproget.dk's capitalization answer](https://sproget.dk/sprogviden/spoergsmaal-og-svar/den-danske-ordbog/jer-og-jeres-stort-eller-lille-begyndelsesbogstav/). The common/neuter distinction and limits of gender prediction are described by [Dansk Sprognævn](https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00000096/).

Claim-specific orthographic references are [RO §13(1) on uppercase pronouns](https://ro.dsn.dk/?paragraf=13&type=rulesearch) and [RO §5(1) on optional numeral accents](https://ro.dsn.dk/?paragraf=5&type=rulesearch). The [pilot source review](pilot-source-review.md) records bounded meaning locators and remaining provenance work.

## Verbs and selected meanings

| ID | Lemma and selected meaning | Active present | Reference past / past participle | Pilot construction |
| --- | --- | --- | --- | --- |
| L009 | have, possess/have available | har | havde / haft | C002, human subject + count-noun object. Exclude auxiliary and garden-noun uses. [DDO](https://ordnet.dk/ddo/ordbog/have) |
| L010 | være, identify/classify | er | var / været | C003 only, fixed `Det er` + indefinite count-noun complement. Exclude profession, adjective, existential, auxiliary, and locative patterns. [DDO](https://ordnet.dk/ddo/ordbog/v%C3%A6re) |
| L011 | læse, read written text | læser | læste / læst | C002, human subject + one of the licensed written-text objects. Exclude studying a subject, interpreting minds, and machine-reading senses. [DDO](https://ordnet.dk/ddo/ordbog/l%C3%A6se) |
| L012 | spise, eat | spiser | spiste / spist | C002, human subject + apple object. Intransitive eating and other food objects are possible Danish, but not licensed in this pilot. [DDO](https://ordnet.dk/ddo/ordbog/spise) |
| L013 | sove, sleep | sover | sov / sovet | C001, human subject, no object. Other subjects and extended expressions are outside this pilot. [DDO](https://ordnet.dk/ddo/ordbog/sove) |

The infinitive lemma can also appear as a related-form exercise alternative after the finite/infinitive distinction is introduced. Past and participle forms are reference metadata only in this slice. Do not expose them as unexplained distractors or infer tense from an English translation alone.

## Nouns and selected meanings

Only the indefinite singular forms are active in the pilot. Definite/plural forms are reference entries for the next content stage. All eight selected senses are countable; this makes the pilot's article rule deliberately narrower than all Danish noun phrases.

| ID | Lemma and English gloss | Gender | Active phrase | Reference definite singular / plural / definite plural | Semantic category and source |
| --- | --- | --- | --- | --- | --- |
| L014 | bog, book | common | en bog | bogen / bøger / bøgerne | Written text, ownable item; book sense, excluding beech-mast homonym. [DDO](https://ordnet.dk/ddo/ordbog/bog) |
| L015 | avis, newspaper | common | en avis | avisen / aviser / aviserne | Written text, ownable item. [DDO](https://ordnet.dk/ddo/ordbog/avis) |
| L016 | bil, car | common | en bil | bilen / biler / bilerne | Vehicle, ownable item. [DDO](https://ordnet.dk/ddo/ordbog/bil) |
| L017 | hund, dog | common | en hund | hunden / hunde / hundene | Animal, possible possession relation; exclude money-slang homonym. [DDO](https://ordnet.dk/ddo/ordbog/hund) |
| L018 | brev, letter/message on paper | neuter | et brev | brevet / breve / brevene | Written text, ownable item; exclude other specialist senses. [DDO](https://ordnet.dk/ddo/ordbog/brev) |
| L019 | hus, house | neuter | et hus | huset / huse / husene | Building, ownable item. [DDO](https://ordnet.dk/ddo/ordbog/hus) |
| L020 | bord, table | neuter | et bord | bordet / borde / bordene | Furniture, ownable item; exclude ship-plank sense, which has a different plural. [DDO](https://ordnet.dk/ddo/ordbog/bord) |
| L021 | æble, apple fruit | neuter | et æble | æblet / æbler / æblerne | Food, ownable item; exclude tree/metaphorical senses. [DDO](https://ordnet.dk/ddo/ordbog/%C3%A6ble) |

## Allowed combinations for the pilot

These are generation allowlists, not claims that other combinations are ungrammatical in Danish.

| Construction and verb sense | Subjects | Objects or complements |
| --- | --- | --- |
| C001 + L013 sleep | L001–L007 | None. |
| C002 + L009 possess | L001–L007 | L014–L021, using their stored article and indefinite singular form. |
| C002 + L011 read text | L001–L007 | L014, L015, L018. |
| C002 + L012 eat | L001–L007 | L021. |
| C003 + L010 identify | L008 only | L014–L021, in a text-supported identifying context. |

Thus `Hun læser et brev` is licensed, while `Hun læser et bord` is not generated in the pilot's literal-text sense. `Vi spiser et æble` is licensed, while an unrestricted noun substitution such as `Vi spiser en bil` is not. Restricting the generator does not authorize labeling arbitrary outside-pilot user text as invalid Danish.

There are 99 abstract sentence combinations under these allowlists: 7 sleeping, 56 possession, 21 reading, 7 eating, and 8 identifying. This is a finite engineering test space; different task presentations do not make the linguistic inventory infinite. Use the first slice to validate controlled variation, then broaden the content before evaluating long-term repetitiveness.

## Content records and provenance

When serializing this seed, include stable lemma/sense IDs, forms and grammatical features, selected English glosses, countability/gender, verb construction permissions, semantic tags, introduced-skill IDs, source references, content revision, and linguistic review status. Identical surface forms must not merge distinct senses, word classes, or analyses. Keep source/meaning edits versioned so old attempts remain interpretable.

The tables contain compact factual forms and original teaching notes. No dictionary definitions, corpus quotations, images, or audio have been imported. DDO lookup availability does not settle bulk-content reuse terms. Before importing a dataset, record its exact licence and provenance. [COR](https://ordregister.dk/) remains the candidate for reusable morphological identifiers/forms, not an imported dependency or replacement for teaching curation.

## Expansion work

After the pilot, add vocabulary to support questions, negation, locations, daily routines, food, people, time, plural/definite nouns, and adjective agreement. Select actual entries from the curriculum and construction needs, with usage/frequency evidence. Add mass nouns and other determiner patterns deliberately, rather than applying the count-noun article rule universally.

## Pair-five addition

The historical 23-entry table above remains the published pilot seed. BL-047 assigns **L024 / L024-S01** to `et magasin`, selected printed-publication sense, for the new four-choice E011 pool only. Its reference forms are `magasinet / magasiner / magasinerne`; source and excluded senses are in the [Module 1 packet](module-one-language-pack.md#new-authoring-packets). It is not added to the 99 sentence allowlists or old three-sense assessment pools. The [pair-five contract](word-choice-tiles-contract.md) governs activation and review status; teaching review is still pending.

| ID | Lemma and English gloss | Gender | Active phrase | Reference definite singular / plural / definite plural | Scope and source |
| --- | --- | --- | --- | --- | --- |
| L024 | magasin, magazine (printed publication) | neuter | et magasin | magasinet / magasiner / magasinerne | E011 only; printed-periodical sense, excluding broadcast, depot/store and weapon-container senses. [DDO, sense 1](https://ordnet.dk/ddo/ordbog/magasin). |


**8 November 2026 — Matching activation selected:** BL-048 reuses L024-S01 alongside the three
existing reading-material senses for E014 in pair six. The [matching contract](vocabulary-matching-contract.md)
extends its use to choice/matching only; the retained pair-five E011-only metadata above stays historical.
No sentence allowlist or older E007/E008 pool expands, and teaching approval remains pending.

## Pair-eight first-step addition

**10 November 2026 — Implementation delivered at 0.9.0:** BL-052 assigns
**L025 / L025-S01** to the teacher noun in the [first-step contract](module-one-activation.md).
It is introduced with its article and supplied in the narrowly licensed occupational frame;
neither article nor noun recall is assessed by these verb tasks.

| ID | Lemma and English gloss | Gender | Teaching bundle | Reference definite singular / plural / definite plural | Scope and source |
| --- | --- | --- | --- | --- | --- |
| L025 | lærer, teacher | common | en lærer | læreren / lærere / lærerne | Supplied teacher sense in singular C004 `jeg/du/han/hun + er lærer`; exclude the homographic verb and unrelated noun senses. [DDO noun entry](https://ordnet.dk/ddo/ordbog/l%C3%A6rer). |

Pair eight additionally licenses six human identity combinations (`jeg/du/hun` × supplied
`Anna/Nora`) and four singular teacher combinations. Its inventory is 109 licensed complete
clauses: the original 99 plus these ten C004 variants. The new E016–E019 task pools are
separate; existing formats and retained snapshots keep their original generation/parser pools.
The selected senses, private provenance and frame allowlists are versioned; no old L010
permission, E007/E008 pool or published C001–C003 combination is silently broadened.
Names remain supplied literals rather than independently assessed lexical entries. Reference
plural/definite forms above are not activated by the S03 teacher frame. Teaching review remains
open; the source supports compact linguistic facts, not a claim of learner effectiveness.


## Pair-nine inanimate-reference addition

**10 November 2026 — Delivered at 0.10.0:** the [activation contract](inanimate-reference-activation.md)
allocates a new entry for `den` and a separate reference sense for existing L008 `det`.

| ID | Lemma and English gloss | Active standalone form | Scope and source |
| --- | --- | --- | --- |
| L026 | den, it (same mentioned common-gender thing) | den | L026-S01, singular subject reference with the antecedent's meaning/article/gender supplied; [DDO den², sense 1](https://ordnet.dk/ddo/ordbog/den). |

L008-S02 is the corresponding neuter reference sense, supported by
[DDO det³, sense 1](https://ordnet.dk/ddo/ordbog/det). Its identifying L008 record stays
unchanged. Neither reference sense enters `humanPronouns` or older lexical/sentence pools.
Pair nine has 26 distinct lexical entries and 109 complete clauses. The separate
S04 source inventory contains two reference senses, not two new lexical entries. The engine
does not expose an exhaustive global sense count. Teaching review remains pending.

## Pair-ten present-form addition

| ID | Lemma and English gloss | Teaching bundle | Scope and source |
| --- | --- | --- | --- |
| L027 | arbejde, work | at arbejde → arbejder | L027-S01, intransitive human work in the authored S05–S06 tasks. [DDO verb entry, sense 1](https://ordnet.dk/ddo/ordbog/arbejde). |

Pair ten contains 27 lexical entries and the same 109 catalogue sentences. The work frames
are confined to the selected tasks and use a human subject with no object or circumstance.
The six named-form and six subject-first activities use the [course contract](module-one-course-slice.md#following-package--s05-and-s06).

## Pair-twelve personal-statement addition

S09 assigns five new lexical identities in scoped name, residence and language frames.
The three verbs are taught with their infinitive marker and stored present form. The
language noun and location preposition are supplied support.

| ID | Selected meaning | Teaching form | Active scope and source |
| --- | --- | --- | --- |
| L028 | hedde, be called by the stated name | at hedde → hedder | L028-S01, human subject and a supplied fictional name under scoped C004. [DDO, sense 1](https://ordnet.dk/ddo/ordbog/hedde). |
| L029 | bo, live or reside at the stated place | at bo → bor | L029-S01, supplied `i Aarhus/Odense` under scoped C006. [DDO, verb sense 1](https://ordnet.dk/ddo/ordbog/bo). |
| L030 | tale, speak the stated language | at tale → taler | L030-S01, bare supplied `dansk` complement under scoped C002. [DDO, verb sense 1.b](https://ordnet.dk/ddo/ordbog/tale). |
| L031 | dansk, the Danish language | dansk | L031-S01, language noun supplied bare in `tale dansk`. This use has no indefinite article. [DDO, noun sense 1](https://ordnet.dk/ddo/ordbog/dansk). |
| L032 | i, in a named city | i + city | L032-S01, location preposition supplied with Aarhus or Odense in the residence cards. [DDO, preposition sense 1.a](https://ordnet.dk/ddo/ordbog/i). |

Pair twelve contains 32 lexical records and the same 109 catalogue clauses. Its new lexical
permissions are confined to the six authored S09 cards and their worked examples. The earlier
C002 noun-object pool, C004 identities and occupation frames retain their original licensing.
The new entries remain a linguistic draft. The [S09 contract](module-one-course-slice.md#s09-names-homes-and-languages)
records the teaching, assessment and replay boundaries.

## Pair-thirteen ability addition

| ID | Selected meaning | Teaching form | Active scope and source |
| --- | --- | --- | --- |
| L033 | kunne, be able to | at kunne → kan | L033-S01, supplied ability modal in the two authored C014 reading frames. [DDO, ability sense](https://ordnet.dk/ddo/ordbog/11028611). |

The supplied modal is restricted to those reading frames. L011 permits the bare infinitive for reading a letter and written
Danish. L031 adds the supplied language complement in the same frames. Six subject
records receive scoped C014 permissions for the authored cards and worked examples.
Pair thirteen contains 33 lexical records and the same 109 catalogue clauses. Its
[S10 contract](module-one-course-slice.md#s10-bare-infinitive-after-kan) keeps ability
support separate from the assessed reading-verb form. [DDO kunne, ability sense](https://ordnet.dk/ddo/ordbog/11028611) · [Infinitive construction](https://lex.dk/infinitiv)

## Pair-fourteen contextual modal additions

| ID | Selected meaning | Teaching form | Active scope and source |
| --- | --- | --- | --- |
| L034 | ville, personal wish or intention | at ville → vil | L034-S01, supplied C014 intention reading frame. [DDO, sense 1](https://ordnet.dk/ddo/ordbog/ville). |
| L035 | skulle, external requirement | at skulle → skal | L035-S01, supplied C014 assigned-reading frame. [DDO, senses 1–1.a](https://ordnet.dk/ddo/ordbog/skulle). |
| L036 | måtte, permission | at måtte → må | L036-S01, supplied C014 granted-permission frame. [DDO, permission sense 3](https://ordnet.dk/ddo/ordbog/m%C3%A5tte). |

Pair fourteen has 36 lexical records and the same 109 catalogue clauses. L011 additionally
licenses the selected modal count-noun reading frames. Subject permissions cover the exact
six assessed and three worked propositions. Prior lexical records and pools retain their
published interpretation. The [S11 contract](module-one-course-slice.md#s11-contextual-modal-meanings)
records contextual support, meaning selection and supplied-order evidence.

## Pair-fifteen time-expression additions

| ID | Selected meaning | Teaching form | Active scope and source |
| --- | --- | --- | --- |
| L037 | current calendar day | i dag | L037-S01, supplied current-day expression in the authored/worked C007 frames. [DDO, sense 1](https://ordnet.dk/ddo/ordbog/59001379). |
| L038 | at the current time | nu | L038-S01, temporal adverb in the authored/worked C007 frames. [DDO, temporal sense 1](https://ordnet.dk/ddo/ordbog/nu). |

Pair fifteen contains 38 records and the same 109 catalogue clauses. L032 adds supplied
locative support in the work frame. L011 covers the two authored C007 reading propositions
and the separate worked reading example. Scope subject permissions to the exact assessed
and taught frames. Prior records remain literal. The [S12 contract](module-one-course-slice.md#s12-adding-when-and-where)
records the supplied core, constituent units, final placement and exposure boundaries.

## Pair-sixteen positive-question support additions

| ID | Selected meaning | Teaching form | Active scope and source |
| --- | --- | --- | --- |
| L039 | affirmative answer to a positive question | ja | L039-S01, introduced short-answer support only. [DDO ja¹, sense 1](https://ordnet.dk/ddo/ordbog/ja). |
| L040 | negative answer to a positive question | nej | L040-S01, introduced short-answer support only. [DDO nej¹, sense 1](https://ordnet.dk/ddo/ordbog/nej). |

The selected S13 package contains 40 lexical records and the same 109 catalogue
clauses. The new entries enter required teaching and its recorded exposure, not
prompt-supplied component exposure or an independent vocabulary test. Scoped C010
permissions cover the exact reading, work, residence and possession question cards
and their separate worked examples. Existing noun/verb bundles and prior generation
pools retain their published behavior. Negative questions, `jo`, echo questions and
`mon` remain outside the [S13 activation](module-one-course-slice.md#s13-activation--11-november-2026).
Teaching review remains open.

## Pair-seventeen work/study additions

P1 adds nine records, bringing the current inventory to **49** while retaining the same
**109 catalogue clauses**. Its separate scoped facts and task frames do not expand old pools.
The [packet](vocabulary-work-study-packet.md) owns exact sentences, senses and exercise slots;
the [activation contract](vocabulary-expansion-activation.md) owns grading and exposure rules.

| ID | Selected meaning | Teaching surface | Official morphology record |
| --- | --- | --- | --- |
| L041 | write a short text | at skrive → skriver | [COR.30533](https://ordregister.dk/id/COR.30533.json) |
| L042 | computer device | en computer | [COR.63924](https://ordregister.dk/id/COR.63924.json) |
| L043 | study as a learner | at studere → studerer | [COR.32395](https://ordregister.dk/id/COR.32395.json) |
| L044 | course/class programme | et kursus | [COR.41200](https://ordregister.dk/id/COR.41200.json) |
| L045 | drink a beverage | at drikke → drikker | [COR.30480](https://ordregister.dk/id/COR.30480.json) |
| L046 | coffee as beverage/substance | kaffe | [COR.43580](https://ordregister.dk/id/COR.43580.json) |
| L047 | water as liquid | vand | [COR.41032](https://ordregister.dk/id/COR.41032.json) |
| L048 | at home, static location | hjemme | [COR.10260](https://ordregister.dk/id/COR.10260.json) |
| L049 | in the morning, fixed expression | om morgenen | [COR.43995](https://ordregister.dk/id/COR.43995.json), component morgen/morgenen |

These morphology lookups were checked against COR₁ 1.5.1.0 on 11 November 2026; they do not
certify English meanings, valency or authored sentences. Count nouns retain their articles;
coffee/water use only the selected bare mass meanings, and the morning expression remains
one supplied chunk. `skrive` permits the authored letter object, `studere` is intransitive,
and `drikke` permits the two selected beverages. No unrestricted combination, plural/definite
paradigm, noun-only parser, or productive chunk substitution follows. Source/content review
and pending Danish specialist review remain distinct.

## Selected pair-nineteen food/shopping additions

The [P2 activation](vocabulary-food-shopping-activation.md) assigns fourteen new lexical
records and one new sense on existing L009, for the delivered pair-nineteen inventory of **63**
records. [Observed software delivery](vocabulary-food-shopping-activation.md#observed-delivery--13-november-2026)
is separate from pending Danish teaching approval. The packet has 21 placements: six reused and fifteen new uses. Old snapshots,
forms and construction permissions remain literal; P2 task frames do not enlarge the
109-clause catalogue or old generation pools.

| ID | Selected meaning | Teaching surface | Official morphology record |
| --- | --- | --- | --- |
| L050 | bread as the selected food/substance | brød, bare mass use | [COR.42096](https://ordregister.dk/id/COR.42096.json) |
| L051 | inexpensive on a supplied product label | billig, base form only | [COR.15843](https://ordregister.dk/id/COR.15843.json) |
| L052 | milk as the selected beverage/substance | mælk, bare mass use | [COR.40408](https://ordregister.dk/id/COR.40408.json), liquid sense |
| L053 | politely get attention in a shop | Undskyld. | [COR.03908](https://ordregister.dk/id/COR.03908.json), interjection |
| L054 | hand over the stated item | Værsgo. | [COR.03709](https://ordregister.dk/id/COR.03709.json), interjection |
| L055 | bread roll | en bolle | [COR.61066](https://ordregister.dk/id/COR.61066.json), noun rather than verb |
| L056 | retail shop | en butik | [COR.46584](https://ordregister.dk/id/COR.46584.json) |
| L057 | café venue | en café | [COR.77252](https://ordregister.dk/id/COR.77252.json), selected accented spelling |
| L058 | buy the supplied item | at købe → køber | [COR.30649](https://ordregister.dk/id/COR.30649.json) |
| L059 | pay in the stated purchase situation | at betale → betaler | [COR.30300](https://ordregister.dk/id/COR.30300.json) |
| L060 | thank the shopkeeper in the supplied scene | Tak. | [COR.43870](https://ordregister.dk/id/COR.43870.json); response use is authored |
| L061 | willing/polite wish in the exact frame | gerne, inside vil gerne | [COR.10180](https://ordregister.dk/id/COR.10180.json) |
| L062 | cup container/head | en kop | [COR.40370](https://ordregister.dk/id/COR.40370.json) |
| L063 | bottle container/head | en flaske | [COR.41536](https://ordregister.dk/id/COR.41536.json), noun rather than verb |

L009-S02 is bare request-frame `have` in the exact taught `vil gerne have` construction.
It shares lemma L009 with possession but never borrows possession sense L009-S01 or its
active present `har`. The latter is translated support only. Its morphology locator is
[COR.30035](https://ordregister.dk/id/COR.30035.json); the selected sense/frame is authored.

The [placement ledger](vocabulary-food-shopping-packet.md#placement-ledger) preserves exact
form locators, lookup dates, sense exclusions and source limits. COR morphology does not
validate English glosses, pragmatic meanings, count/mass choices or sentence naturalness.
Count nouns retain supplied articles; the bare mass uses remain article-free in these
frames. Cup/bottle matching directly tests their heads. Full quantities stay taught intact
constituents in request tiles, with separate head/beverage exposure and no independent
article, quantity-rule, conjugation or beverage-production credit.

## Selected pair-twenty places/journeys additions

The [P3 activation contract](vocabulary-places-journeys-activation.md) allocates sixteen new
records, for a selected inventory of **79**, with the same **109 catalogue clauses**.
The task-specific frames do not expand earlier pools. Its 21 placements contain five
reused senses plus sixteen new records; teaching review remains pending.

| ID | Selected meaning | Teaching surface | Official morphology record |
| --- | --- | --- | --- |
| L064 | bicycle vehicle | en cykel | [COR.45412](https://ordregister.dk/id/COR.45412.json) |
| L065 | travel by bicycle | at cykle → cykler | [COR.37334](https://ordregister.dk/id/COR.37334.json) |
| L066 | transport station | en station | [COR.69303](https://ordregister.dk/id/COR.69303.json) |
| L067 | walk/go on foot to the supplied destination | at gå → går | [COR.30234](https://ordregister.dk/id/COR.30234.json) |
| L068 | station destination, fixed chunk | til stationen | [COR.00947](https://ordregister.dk/id/COR.00947.json) + [COR.69303](https://ordregister.dk/id/COR.69303.json) |
| L069 | homeward destination, directional adverb | hjem | [COR.10109](https://ordregister.dk/id/COR.10109.json), excludes noun COR.40431 |
| L070 | library place, supplied identification | et bibliotek | [COR.58895](https://ordregister.dk/id/COR.58895.json) |
| L071 | bus vehicle, supplied identification | en bus | [COR.63246](https://ordregister.dk/id/COR.63246.json) |
| L072 | train vehicle, supplied identification | et tog | [COR.51979](https://ordregister.dk/id/COR.51979.json), excludes procession COR.48221 |
| L073 | travel ticket, exact possession frame | en billet | [COR.45414](https://ordregister.dk/id/COR.45414.json) |
| L074 | vehicular travel in an explicit car scene | at køre → kører | [COR.30695](https://ordregister.dk/id/COR.30695.json), excludes noun COR.47027 |
| L075 | wait in the supplied station frame | at vente → venter | [COR.30094](https://ordregister.dk/id/COR.30094.json), excludes phrase COR.04094 |
| L076 | static station location, fixed chunk | på stationen | [COR.00190](https://ordregister.dk/id/COR.00190.json) + [COR.69303](https://ordregister.dk/id/COR.69303.json) |
| L077 | change trains/connections in an explicit rail scene | at skifte → skifter | [COR.30102](https://ordregister.dk/id/COR.30102.json), excludes noun COR.52909 |
| L078 | pharmacy place, supplied identification | et apotek | [COR.98580](https://ordregister.dk/id/COR.98580.json) |
| L079 | pharmacy destination, fixed chunk | til apoteket | [COR.00947](https://ordregister.dk/id/COR.00947.json) + [COR.98580](https://ordregister.dk/id/COR.98580.json) |

Each record uses its own `-S01` selected sense. Reuse L016-S01 (car), L038-S01 (now),
L048-S01 (static home), L029-S01 (residence) and L032-S01 (the two supplied city
chunks) literally. P3 separately licenses the exact rail-context `skifter i Aarhus`
construction for that existing city sense; it does not broaden residence or old pools.

The [placement ledger](vocabulary-places-journeys-packet.md#placement-ledger) owns
precise form locators, 11 November lookup dates, homograph exclusions and use limits.
These references validate morphology only; English senses and authored frames remain
separate reviewed drafts. Preserve noun articles and intact city/destination/location
chunks. No general preposition, definite/plural paradigm, transport-means phrase,
boarding route, object-taking change/wait construction or S14 fronting is introduced.
