# Danish Resource Assessment

## Scope and evidence date

Primary-source audit completed 7 November 2026 for BL-005. No dataset was downloaded or imported. This records available evidence and a future enrichment proposal; Astrid's 23 authored entries and internal sense identities remain authoritative.

## Candidate resources

| Resource | Verified coverage and access | Reuse evidence and limits |
| --- | --- | --- |
| COR₁ and Retskrivningsordbogen | Current COR₁ is 1.5.1.0, published 16 December 2025, corresponding to RO 5.1 (2025). TSV and documented HTML/JSON/CSV lookup provide lemma/form IDs, morphology and normalization flags. No systematic learner senses or English glosses. [Release](https://ordregister.dk/), [format](https://ordregister.dk/doc/COR-README.html), [lookup service](https://ordregister.dk/doc/COR.Webservices.html). | Official COR documentation states open availability and use for any purpose; the historical-release page explicitly identifies CC0. Archive or confirm the exact named terms accompanying the current release before an import. Separately, RO XML requires a contract and has a standalone-dictionary-product restriction. [COR terms](https://ordregister.dk/doc/COR.html), [historical terms](https://ordregister.dk/doc/Gamle-udgaver.html), [RO XML terms](https://ro.dsn.dk/ro-elektronisk-og-som-trykt-bog/). |
| COR.EXT | Version 1.0 adds over 25,000 DDO lemmas outside RO, with DDO EntryIDs, short Danish homograph glosses, word classes and forms. No English gloss field; completeness and update cadence are not established. [Resource](https://ordregister.dk/doc/COR.EXT.html), [specification](https://ordregister.dk/doc/COR-EXT-spec.html). | Resource page explicitly identifies CC0. Inclusion does not establish official spelling status. |
| COR.SEM | Version 1.0 covers about 34,000 lemmas and 42,000 senses linked to COR/DDO identities, with semantic relations, topics and frames. Some DDO senses are merged or omitted. English frame labels are not learner translations. [Resource](https://ordregister.dk/doc/COR.SEM.html), [sense policy](https://ordregister.dk/doc/COR-SEM-beskrivelse.html), [fields](https://ordregister.dk/files/COR.SEM_1.0_specifikation.html). | COR.SEM explicitly identifies CC0. The companion COR.SEM.EXT, containing definitions/examples, has separate CC BY-NC-ND terms and must not be treated as the same resource. [Companion terms](https://ordregister.dk/doc/COR.SEM.EXT.html). |
| Den Danske Ordbog | Continuously maintained monolingual dictionary; latest listed update was 26 June 2026. Useful forms, meanings, usage and valency evidence. No public bulk/API contract or EntryID stability promise was located in this audit. [About](https://ordnet.dk/ddo/om-ordbogen/), [updates](https://ordnet.dk/ddo/nyt-i-ordbogen/). | Free web access is distinct from data reuse. Published terms reserve rights, allow isolated private digital copies and require prior agreement for other copying. Current practice remains bounded source consultation and links, without content import. [Published terms](https://ordnet.dk/copyright). |

## Proposed enrichment boundary

Prefer a COR₁-first morphology audit before considering COR.EXT fallback. Preserve Astrid's `L…` and authored sense IDs; store external assertions separately with exact resource version, external identity, feature bundle, normalization state, source/terms URL, retrieval date and mapping-review state. English glosses, pedagogical sense boundaries, countability, valency allowlists and generation permissions remain authored and reviewed.

COR identifiers have stable denotation but can be retired. Upgrade tooling must inspect official moves, merges and replacements rather than treating an external resource row as permanently unchanged. [Identity specification](https://ordregister.dk/files/COR-spec_v1.1.pdf).

The current release uses normalization values `U/N/K`, whereas parts of the README still describe `0/1`. A future parser needs release-specific fixtures. Proposed generation policy: activate reviewed `N` forms, keep `K` pending review, and exclude `U` from normative generation; this remains a proposal. [Release notes](https://ordregister.dk/), [small live example](https://ordregister.dk/lemma/COR/json/hest).

## Open points

Confirm current-release licence attachment, exact 23-entry identity mappings, COR.EXT/COR.SEM update guarantees and an appropriate English-gloss provenance strategy. COR.SEM can assist mapping but does not establish equivalent learner senses. No external import, runtime dictionary dependency or teacher-approved status follows from this assessment.
