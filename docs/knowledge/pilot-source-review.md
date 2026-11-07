# Pilot Linguistic Source Review

## Status — 7 November 2026

A bounded independent agent review checked the 23 active lexical records, their forms/genders, selected meanings, licensed verb frames and 99 canonical sentence combinations against primary references. It found no actionable active-content error. This is a source review of a small draft, not Danish teaching approval, corpus-wide naturalness evidence or proof of learning effectiveness.

English glosses, sense boundaries and generation allowlists are Astrid-authored proposals. External morphological matches do not establish English equivalence, semantic identity or permission to generate every combination. The [COR crosswalk](cor-pilot-crosswalk.md) remains a candidate mapping, with no import or runtime dependency.

## Claim-specific references

| Claim | Evidence and qualification |
| --- | --- |
| Human subject pronouns and fixed identifying `det` | [Sproget.dk pronouns](https://sproget.dk/typiske-problemer/pronomener-stedord/); the pilot's `Det er …` is a bounded construction, not a general reference rule |
| Lexical uppercase `I` for plural addressees | [Retskrivningsordbogen §13(1)](https://ro.dsn.dk/?paragraf=13&type=rulesearch); preserve case during normalization |
| Numeral `én/ét` versus unstressed article `en/et` | [RO §5(1)](https://ro.dsn.dk/?paragraf=5&type=rulesearch) and [DDO en](https://ordnet.dk/ddo/ordbog/en); numeral accent is optional, so absence of an accent alone cannot identify the use |
| Subject, object and predicative are distinct functions | [Sproget.dk sentence constituents](https://sproget.dk/typiske-problemer/saetningsled/) |
| A fronted object/predicative can satisfy V2 while missing the subject-first exercise target | [Aarhus University field-model discussion](https://tildeweb.au.dk/au132769/papers/joer00b.pdf); fronting can require discourse context and does not make every permutation neutral everyday phrasing |

The explicit article instruction justifies the pilot's treatment of an accented matching numeral as linguistically acceptable but off target. It does not imply that an unaccented numeral is impossible. Plural subjects with a singular object can also admit collective/distributive readings; that ambiguity alone is not a grammatical defect.

## Dictionary locator audit

The original metadata cites broad headword pages. Review recovered these entry identities or human-readable meaning locators; not every page exposed a safe numeric identifier. Meaning numbers below are editorial locators observed on the review date, not guaranteed stable sense IDs.

| Pilot record | Observed DDO locator and bounded sense |
| --- | --- |
| L006 `I` | `I4`, entry `11022728`, meaning 1 subject use; RO §13 supplies the orthographic rule |
| L009 `have` | `have2`, entry `11020037`, meaning 1 possession/availability |
| L010 `være` | entry `12007519`, meaning 2, especially fixed identifying 2.c |
| L011 `læse` | headword page, meaning 1 written material; numeric entry ID not recovered |
| L012 `spise` | `spise2`, entry `11049250`, meaning 1 eating |
| L013 `sove` | numeric route `11048849`, meaning 1 intransitive sleep |
| L014 `bog` | `bog1`, entry `11005503`, meanings 1–2 reading publication; exclude `bog2` |
| L015 `avis` | numeric route `11002960`, meaning 1 newspaper; exclude publisher/organization 1.a |
| L016 `bil` | headword page, principal vehicle meaning; numeric entry ID not recovered |
| L017 `hund` | `hund1`, entry `11021628`, meaning 1 animal; exclude pejorative person and money homonym |
| L018 `brev` | headword page, meaning 1 written letter/message; numeric entry ID not recovered |
| L019 `hus` | headword page, meaning 1 building, including dwelling 1.a; numeric entry ID not recovered |
| L020 `bord` | entry `11005762`, meaning 1 furniture; exclude nautical plank meaning |
| L021 `æble` | entry `12007914`, meaning 1 fruit; exclude tree 1.a and apple-shaped object 2 |
| L022/L023 `en/et` | article `en3`, meaning 1, with `et` inflection; distinguish numeral `en1` entry `11011264` |

Resolve entries through the [DDO headword search](https://ordnet.dk/ddo/ordbog) and use its [citation guidance](https://ordnet.dk/ddo/om-ordbogen/citering/). The reviewer did not verify stable `def_id` values for these selected meanings; incidental IDs in search URLs must not be adopted as sense anchors. Several recovered routes had unrelated search slugs, so those slugs are not canonical citations.

## Remaining work

- Attach the claim-specific RO references to the local review metadata and preserve the review date. This is provenance maintenance, not a learner-content change.
- Adopt precise entry/sense citations only after verifying the actual destination; retain human locators when machine identifiers are unavailable.
- Obtain Danish teaching review of naturalness, target isolation and instructional effectiveness before changing the draft review state.
- Keep resource reuse assessment separate. No dictionary definitions or example sentences were imported during this audit; published access and reuse terms are documented in the [resource assessment](danish-resource-assessment.md).
