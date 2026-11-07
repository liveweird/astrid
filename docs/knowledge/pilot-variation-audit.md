# Pilot Variation Inventory

## Evidence and scope — 7 November 2026

This is an engineering inventory for `pilot-1` / `generator-1`, not corpus-frequency evidence or a teaching-balance recommendation. A direct parse of the generated `engine/build/review/pilot-1.md` observed **99 distinct canonical IDs and 99 distinct realized sentence surfaces**, with no duplicate surfaces. Regenerate it with `./gradlew :engine:exportPilotReview`.

The report agrees with the authored [seed allowlists](danish-vocabulary-seed.md), [typed content](../../engine/src/main/kotlin/app/astrid/engine/PilotContent.kt) and [format eligibility](../../engine/src/main/kotlin/app/astrid/engine/PilotEngine.kt). No generator, content revision or sampling rule changed during this audit.

## Linguistic inventory versus presentation

| Format | Eligible canonical sentences | Current target |
| --- | ---: | --- |
| E001 | 99 | Select the finite present form from the same verb's infinitive/present pair |
| E002 | 92 | Supply the indefinite article of a noun-bearing sentence |
| E003 | 99 | Order intact constituents into a subject-first statement |

There are **290 eligible format–sentence pairs**, representing **99 underlying sentences**. Seeds and shuffled presentations do not enlarge that linguistic inventory. The seven intransitive sleeping sentences have no article gap and are excluded from E002.

| Construction | Sentences |
| --- | ---: |
| C001 intransitive | 7 |
| C002 transitive | 84 |
| C003 identifying copula | 8 |

## Lexical and target distributions

| Finite present answer in E001 | Count |
| --- | ---: |
| `har` | 56 |
| `læser` | 21 |
| `er` | 8 |
| `spiser` | 7 |
| `sover` | 7 |

E002 is exactly balanced across its full pool: **46 `en` and 46 `et`** answers. Individual nouns are not equally represented: `bog`, `avis`, `brev` and `æble` occur 15 times each; `bil`, `hund`, `hus` and `bord` occur 8 times each. The seven human subject pronouns occur 13 times each, while fixed identifying `det` occurs 8 times.

These counts follow the reviewed combination permissions: possession accepts all eight nouns, reading accepts three, eating accepts one and identification accepts all eight. Uniform sentence coverage therefore gives possession/`har` a larger share than the other verb forms. It does not imply possession is more important for beginners.

## Implications for later authoring — proposals

- Evaluate target-form, lexical and construction coverage separately from prompt count. A larger combination family should not silently determine a future course's teaching mix.
- Keep semantic novelty distinct from format/shuffle novelty. Reusing a sentence across targets can provide deliberate scaffolding, but it is still the same sentence.
- The current finite-form pair tests finite versus infinitive recognition; it is not a three-tense choice task. Later tense alternatives require their grammar, context and forms to be introduced.
- Supplied English meanings and lessons support isolated grammar practice. Current success does not establish independent vocabulary recall, transfer or mastery.
- Broader content and learner feedback are needed to assess long-term repetition. Any future sampling change must preserve old reproduction semantics through explicit revisioning; do not silently reweight `generator-1`.

No balance threshold, scheduler, new format or vocabulary expansion is selected here. Use the [review guide](pilot-review-guide.md) and this inventory to choose the next engine work after feedback.
