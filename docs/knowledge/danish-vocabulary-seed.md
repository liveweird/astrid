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

**10 November 2026 — Selected S04:** the [activation contract](inanimate-reference-activation.md)
allocates a new entry for `den` and a separate reference sense for existing L008 `det`.

| ID | Lemma and English gloss | Active standalone form | Scope and source |
| --- | --- | --- | --- |
| L026 | den, it (same mentioned common-gender thing) | den | L026-S01, singular subject reference with the antecedent's meaning/article/gender supplied; [DDO den², sense 1](https://ordnet.dk/ddo/ordbog/den). |

L008-S02 is the corresponding neuter reference sense, supported by
[DDO det³, sense 1](https://ordnet.dk/ddo/ordbog/det). Its identifying L008 record stays
unchanged. Neither reference sense enters `humanPronouns` or older lexical/sentence pools.
The current inventory has 26 distinct lexical entries and 109 complete clauses; the separate
S04 source inventory contains two reference senses, not two new lexical entries. The engine
does not expose an exhaustive global sense count. Teaching review remains pending.
