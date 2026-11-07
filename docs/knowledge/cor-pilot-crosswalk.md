# Proposed COR Pilot Crosswalk

## Evidence boundary

These are candidate external morphology mappings, checked against the live public COR lookup on 7 November 2026. They are not adopted pilot identities or a data import. Astrid's `L…` records and authored English senses remain canonical. The landing page names COR₁ 1.5.1.0, but individual JSON responses contain no release identifier; a future reproducible import needs a pinned snapshot. [Release](https://ordregister.dk/), [service contract](https://ordregister.dk/doc/COR.Webservices.html).

Every selected active form below returned normalization `N`. Preserve returned case and word class; lookup results can include homographs, other inflections and non-normalized forms. The current `U/N/K` policy differs from older `0/1` examples. [Release-specific notes](https://ordregister.dk/), [older format description](https://ordregister.dk/doc/COR-README.html).

## Candidate mappings

| Astrid | Active form(s) | COR lemma | COR full-form ID(s) | Selection constraint |
| --- | --- | --- | --- | --- |
| L001 | [jeg](https://ordregister.dk/lemma/COR/json/jeg) | COR.01459 | COR.01459.991.01 | Nominative pronoun; exclude the noun homograph |
| L002 | [du](https://ordregister.dk/lemma/COR/json/du) | COR.01979 | COR.01979.991.01 | Nominative pronoun; exclude the verb |
| L003 | [han](https://ordregister.dk/lemma/COR/json/han) | COR.01880 | COR.01880.991.01 | Nominative pronoun; exclude the noun |
| L004 | [hun](https://ordregister.dk/lemma/COR/json/hun) | COR.01051 | COR.01051.991.01 | Nominative pronoun; exclude the noun |
| L005 | [vi](https://ordregister.dk/lemma/COR/json/vi) | COR.01276 | COR.01276.991.01 | Nominative pronoun; exclude verb/noun results |
| L006 | [I](https://ordregister.dk/lemma/COR/json/I) | COR.00705 | COR.00705.991.01 | Exact uppercase nominative pronoun; exclude numeral, noun and lowercase `i` |
| L007 | [de](https://ordregister.dk/lemma/COR/json/de) | COR.01537 | COR.01537.991.01 | Personal “they”; exclude article, formal `De` and the `den` paradigm |
| L008 | [det](https://ordregister.dk/fuldform/COR/json/det) | COR.01831 (`den`) | COR.01831.995.01 | Neuter singular pronoun only; fixed identifying use remains authored |
| L009 | [have / har](https://ordregister.dk/lemma/COR/json/have) | COR.30035 | COR.30035.200.01 / COR.30035.203.01 | Verb infinitive/present; exclude garden noun and forms of `hav`; possession sense remains authored |
| L010 | [være / er](https://ordregister.dk/lemma/COR/json/v%C3%A6re) | COR.30359 | COR.30359.200.01 / COR.30359.203.01 | Verb infinitive/present; identifying use remains authored |
| L011 | [læse / læser](https://ordregister.dk/lemma/COR/json/l%C3%A6se) | COR.30712 | COR.30712.200.01 / COR.30712.203.01 | Verb infinitive/present; exclude noun `læser`; text-reading sense remains authored |
| L012 | [spise / spiser](https://ordregister.dk/lemma/COR/json/spise) | COR.30050 | COR.30050.200.01 / COR.30050.203.01 | Verb infinitive/present; exclude noun forms; food restrictions remain authored |
| L013 | [sove / sover](https://ordregister.dk/lemma/COR/json/sove) | COR.30594 | COR.30594.200.01 / COR.30594.203.01 | Verb infinitive/present; no pilot sense identity supplied |
| L014 | [bog](https://ordregister.dk/lemma/COR/json/bog) | COR.41274 | COR.41274.110.01 | Common indefinite singular; reading-book homograph, not beech fruit |
| L015 | [avis](https://ordregister.dk/lemma/COR/json/avis) | COR.41896 | COR.41896.110.01 | Common indefinite singular; newspaper sense remains authored |
| L016 | [bil](https://ordregister.dk/lemma/COR/json/bil) | COR.42395 | COR.42395.110.01 | Common indefinite singular noun; exclude verb imperative |
| L017 | [hund](https://ordregister.dk/lemma/COR/json/hund) | COR.41457 | COR.41457.110.01 | Common indefinite singular; animal/slang distinction remains authored |
| L018 | [brev](https://ordregister.dk/lemma/COR/json/brev) | COR.43288 | COR.43288.120.01 | Neuter indefinite singular; paper-letter sense remains authored |
| L019 | [hus](https://ordregister.dk/lemma/COR/json/hus) | COR.43962 | COR.43962.120.01 | Neuter indefinite singular noun; exclude verb/other-lemma forms |
| L020 | [bord](https://ordregister.dk/lemma/COR/json/bord) | COR.44636 | COR.44636.120.01 | Neuter indefinite singular furniture homograph; exclude ship-plank/verb records |
| L021 | [æble](https://ordregister.dk/lemma/COR/json/%C3%A6ble) | COR.40648 | COR.40648.120.01 | Neuter indefinite singular; fruit/metaphorical restrictions remain authored |
| L022 | [en](https://ordregister.dk/lemma/COR/json/en) | COR.00831 | COR.00831.915.01 | Common singular indefinite article; exclude numeral and pronoun records |
| L023 | [et](https://ordregister.dk/fuldform/COR/json/et) | COR.00831 (`en`) | COR.00831.916.01 | Neuter singular indefinite article; not a separate COR lemma |

## Interpretation and safeguards

The verb infinitive feature is `vb.inf.akt`; present is `vb.præs.akt`. Pronoun candidates use nominative features except the fixed `det` morphology. Nouns distinguish common `fk` and neuter `itk`, with singular indefinite `sg.ubest`. Articles have explicit article analyses separate from numerals, so a surface-only importer would be incorrect.

L022/L023 are distinct internal form records mapping to one external article lemma. L008 maps morphologically to the `den` paradigm without delegating its selected construction use to COR. These many-to-one relationships are deliberate; do not rewrite internal identities to match an external lemma layout.

COR does not supply Astrid's English glosses, teaching sense boundaries or valency allowlists. Candidate external IDs require mapping review and exact resource/version/provenance fields before adoption. Published IDs retain denotation but can be retired or replaced; upgrades must inspect official changes. [Identity policy](https://ordregister.dk/files/COR-spec_v1.1.pdf). The [resource assessment](danish-resource-assessment.md) records outstanding terms and integration questions.
