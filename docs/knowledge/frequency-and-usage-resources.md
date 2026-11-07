# Danish Frequency and Usage Resources

## Status — 7 November 2026

This extends the [morphology/resource assessment](danish-resource-assessment.md) for BL-025. It evaluates candidates for future vocabulary selection and usage checks; no corpus or frequency list is adopted, imported into the application or stored in the repository. Source metadata and published terms were consulted online. Frequency is one selection signal, not a syllabus, sense inventory or naturalness guarantee.

## Candidate comparison

| Resource | Evidence and access | Fit and limits |
| --- | --- | --- |
| **DSL frequent lemmas** | [Official resource](https://korpus.dsl.dk/resources/details/freq-lemmas.html): 30,000-lemma lists with POS and relative frequency aggregated across inflected forms, based on BAKSPEJLET 2024, approximately 1.1 billion tokens from 1983–2024. Plain-text ZIP; no immutable release identifier, stable lemma IDs or documented query API found. | Strongest first candidate for a **ranking prior**, joined by reviewed lemma and POS. It supplies neither English senses nor contexts. Published genre proportions and tagging-error estimates remain unknown. |
| **KorpusDK / CoREST** | [Download details](https://korpus.dsl.dk/resources/details/korpusdk.html) describe over 100 million lemmatized/tagged tokens from written sources around 1990, 2000 and 2010, distributed as shuffled sentences or short excerpts. [CoREST 2021](https://korpus.dsl.dk/corest/) reports approximately 110 million tokens covering roughly 1985–2010, with concordances, collocations and metadata filters. | Candidate for bounded manual usage checks. Edited prose, fiction, diaries and blogs offer different registers; the age and mixture limit claims about present-day everyday writing. No stable result-ID/API contract was found. [Ordnet's separate 56-million-word description](https://ordnet.dk/ddo/) must not be substituted for the expanded edition. |
| **Danish Gigaword** | [Original 2021 paper](https://aclanthology.org/2021.nodalida-main.46/) describes approximately 1.045 billion words with strongly unequal legal, social-media, subtitle, parliamentary, news and historical components. The [current homepage](https://gigaword.dk/) links a [Hugging Face 1.0.0 repack](https://huggingface.co/datasets/danish-foundation-models/danish-gigaword/blob/main/README.md?code=true) with document/source/license fields and several original components omitted. | Possible later domain-stratified comparison, not a ready lemma-frequency list. Raw counts require processing; lemmatization adds a tagger and its errors. Do not transfer original size/composition statistics to the reduced repack or call the undifferentiated corpus “everyday Danish.” No cross-release document-ID stability promise was found. |

## Published reuse conditions

- **DSL frequency resource:** [DSL open-resource terms](https://korpus.dsl.dk/resources/licences/dsl-open.html) allow many uses, including commercial use and modification/redistribution, but exclude **dictionaries in any form** and **other products competing with DSL**. Preserve both exclusions; “open” here does not establish an unrestricted standard licence. Astrid's intended use needs edition/use-specific clarification before product import or redistribution.
- **KorpusDK download:** [Restricted-resource terms](https://korpus.dsl.dk/resources/licences/dsl-restricted.html) require an approved purpose and prohibit commercial use and redistribution, alongside their dictionary/competition conditions. [Ordnet web copyright terms](https://ordnet.dk/copyright) are a separate access/reuse policy. Search access does not establish reusable corpus rights.
- **Gigaword:** the homepage's CC BY 4.0 statement and the repack's per-component licence table must be reconciled for the selected edition. Components name different licences/terms, and removed sources reflect privacy, quality or copyright concerns. Pin the actual repack revision and component manifest before considering reuse; do not apply one homepage label indiscriminately to every source.

These are published conditions and unresolved selection points, not a legal opinion or clearance to import data. No publisher contact was made.

## Proposed next evaluation

Keep the authored pilot vocabulary unchanged. For a future curriculum batch, compare human-selected lemma/POS candidates with a pinned DSL ranking **only after its intended use is clarified**. Retain source edition, retrieval date, fields and terms; compute a local archive checksum if a download is later adopted. Do not use spelling alone to merge homographs or map ranks to English meanings.

Use bounded concordance consultation to investigate an authored phrase, with no stored quotations or production dependency. A future Gigaword experiment should compare separately chosen domains rather than allow corpus size to determine the teaching mix. Human review still decides everyday relevance, selected sense, valency and context.

## Deferred or unverified candidates

No primary Danish resource named `SUBTLEX-DK` was verified during the targeted search. This is not proof of nonexistence; do not rely on that name without an identifiable release and terms. Danish subtitle data inside Gigaword is not itself a validated subtitle-frequency norm.

[CoRal v3](https://huggingface.co/datasets/CoRal-project/coral-v3) offers spoken transcripts/audio with gated access and [custom dataset conditions](https://github.com/alexandrainst/coral/blob/main/DATASET_LICENSE). It is not a frequency list and is outside the present written-reference priority. No audio was obtained; listening/speaking implementation remains postponed.
