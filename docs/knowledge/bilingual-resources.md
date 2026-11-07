# English–Danish Lexical Resource Candidates

## Status — 7 November 2026

The current pilot keeps authored English teaching glosses, selected senses and typed Danish forms authoritative. COR remains a **candidate morphology source**, not an adopted runtime dependency. This assessment extends BL-025 beyond morphology; no external dictionary or lexical dataset has been imported into Astrid.

A translation, definition, concept link and inflected form are different assertions. None alone establishes a learner-appropriate equivalent, a verb frame or generation permission. Keep external IDs/revisions as namespaced provenance rather than replacing Astrid's internal identities.

## Candidate comparison

| Candidate | Verified access/model | Proposed role and limitations |
| --- | --- | --- |
| **DanNet with English WordNet links** | [Release v2026-10-02](https://github.com/kuhumcst/DanNet/releases/tag/v2026-10-02), with [RDF/CSV/WN-LMF/DMLex downloads](https://wordnet.dk/dannet/page/downloads) and a [bounded SPARQL service](https://wordnet.dk/dannet/page/sparql). Word→Sense→Synset separates polysemy; English concept links are partial. Releases can retire/redirect IDs and revise cross-resource links. | Strong semantic-candidate source, not a complete beginner bilingual dictionary. Verify function-word coverage explicitly. A shared synset or exact link is evidence to review, not automatic English-gloss approval. Pin release and identifier history. |
| **English Wiktionary Danish entries via Wiktextract/Kaikki** | [Danish index](https://kaikki.org/dictionary/Danish/index.html) reports extraction on 30 October 2026 from the 1 October dump. The postprocessed language file is deprecated; [raw snapshots](https://kaikki.org/dictionary/rawdata.html) or a pinned extractor run are the proposed access paths. Entries include lemma/POS, senses and forms; website-added IDs are not durable identifiers. | Useful English-gloss and homograph candidates. Community sense boundaries, tags and coverage change; preserve dump date, extractor revision/configuration and page/revision provenance where available. Review articles, numerals, pronouns, auxiliaries and case-sensitive `I` explicitly. |
| **Wikidata Lexemes** | [Data model](https://www.mediawiki.org/wiki/Extension:WikibaseLexeme/Data_Model) separates lexemes, forms and senses; [data access](https://www.wikidata.org/wiki/Help:Data_access) includes entity dumps, APIs and WDQS. Danish [`have`](https://www.wikidata.org/wiki/Lexeme:L3828) illustrates external links and separate forms/senses. | Useful identifier/form corroboration where populated. [Asynchronously refreshed statistics](https://www.wikidata.org/wiki/Wikidata:Lexicographical_data/Statistics/Counts_of_various_things_by_language) show uneven sense/translation coverage; lexeme counts are not bilingual sense coverage. Retain entity revision/time and honour service access policies. |
| **FreeDict** | [Downloads](https://freedict.org/downloads/) list `dan-eng` 0.3.1 with 5,097 headwords and `eng-dan` 0.1.0 with 411; the TEI headers are from April 2019. Directions, sense grouping and grammatical annotation are uneven. No durable entry/sense-ID guarantee was found. | Low-priority disagreement/candidate source. A small dictionary may help a bounded lookup but cannot establish systematic coverage or safe automatic form/sense generation. No exhaustive 23-entry conformance check has been performed. |

## Data terms are resource-specific

- **DanNet:** [published downloads/terms](https://wordnet.dk/dannet/page/downloads) distinguish DanNet CC BY-SA 4.0, its OEWN extension CC BY 4.0 and COR integration/COR.SEM CC0. Do not apply a component's terms to the whole combined resource. Attribution, change notices and adaptation/share-alike boundaries require an explicit distribution decision before import.
- **Wiktionary:** follow [Wikimedia's governing content-licensing terms](https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use#7._Licensing_of_Content) and the [entry copyright guidance](https://en.wiktionary.org/wiki/Wiktionary:Copyrights), including CC BY-SA 4.0/GFDL information and applicable exceptions. Quoted/external examples and media can carry separate terms. [Wiktextract's MIT licence](https://github.com/tatuylonen/wiktextract/blob/master/LICENSE) licenses the parser, not dictionary text. Do not infer data rights from the extraction tool.
- **Wikidata:** [structured entity data is CC0](https://www.wikidata.org/wiki/Wikidata:Licensing); site prose/media are separate. Permissive terms do not solve sparse or incorrect semantic mappings.
- **FreeDict:** inspect each direction's TEI header. [`dan-eng`](https://raw.githubusercontent.com/freedict/fd-dictionaries/master/dan-eng/dan-eng.tei) states GPL 3.0-or-later and separately licenses changes since 0.3 under CC BY-SA 3.0-or-later. [`eng-dan`](https://raw.githubusercontent.com/freedict/fd-dictionaries/master/eng-dan/eng-dan.tei) states a whole-dictionary dual GPL 3+/CC BY-SA 3+ grant. These are different scopes; neither is the tooling licence or a CC0 grant.

These are published terms, not legal clearance or a decision to adopt any dataset. No notices, definitions, examples or media were copied into learner content during the assessment.

## Proposed curation workflow

For a small, human-selected curriculum batch, retrieve candidates by lemma/POS and verified external links where present. Expose homographs; record exact, near or rejected alignment to the authored sense. Check active morphology separately, using current normative references and any later-adopted COR release. Preserve resource, release/revision, external ID, assertion type and applicable terms.

Keep English teaching glosses and semantic combination permissions authored and reviewed. Do not automatically activate an external entry, reuse a definition as a learner explanation or infer a valency frame from translation equivalence. The [candidate COR crosswalk](cor-pilot-crosswalk.md) remains separate from semantic alignment.

Track upstream derivation: several resources can repeat the same COR or dictionary assertion. Crosslinks do not establish independent corroboration merely because several websites agree.

## Screened out or unresolved

[PanLex's current licence page](https://panlex.org/license/) states CC BY-NC-SA 4.0 and written permission for commercial use. A current release/download could not be verified. Older CC0 descriptions do not establish the terms of a current edition; this does not imply historical CC0 grants were revoked. Keep it outside the shortlist pending edition-specific provenance.

Still open: pilot-entry coverage measurements, source-specific attribution/adaptation decisions, external-ID migration handling and Danish teaching review. No source promises the stable pedagogical sense boundaries Astrid requires; software and source checks do not establish specialist approval.
