# Word choice and individual sentence tiles

- **Spec**: [tests/word-choice-tiles.spec.ts](../tests/word-choice-tiles.spec.ts)
- **Actors**: an anonymous playground learner
- **Owns** (exclusive server-side state): exercises and attempts in each test's development session

The new pair-five formats use seed 47. Tile examples use index 5, corresponding to
`Hun læser et brev.`. Outcomes verify draft engine behavior, not teaching approval.

## Scenario: English word recognition supplies four related Danish article bundles and grades meaning separately

1. Generate four successive **Choose the Danish word** examples.
   - *Expected*: four article+noun choices (`en bog`, `en avis`, `et brev`, `et magasin`),
     Danish choice language, English cue language, E011 and no private tile source.
2. Choose another meaning, check, then retry and activate the correct choice with Enter.
   - *Expected*: the wrong choice has target/meaning not met but acceptable supplied Danish;
     the correct choice satisfies all four facets. The wrong choice never shows Good work.

## Scenario: all-word translation supports keyboard removal and checks a complete sentence

1. Choose **Build a Danish sentence**.
   - *Expected*: an English cue, four individual tiles, `use-all`, required count four.
2. Add the subject with Enter, focus its removal button and press Enter.
   - *Expected*: focus returns to that available occurrence.
3. Select the canonical four words and check.
   - *Expected*: target, meaning and response shape met, acceptable language and Good work.

## Scenario: tile translation grades incomplete selections and licensed fronting without accepting a prefix

1. Build the three-word selection without its article and check.
   - *Expected*: constraint not met; target unevaluable and meaning/language unknown.
2. Regenerate the same proposition, build object-first `Et brev læser hun.` and check.
   - *Expected*: meaning and constraint met, acceptable Danish, subject-first target not met.

## Scenario: spare-word translation distinguishes infinitive choice from finite subject-first order

1. Choose **Choose the present form with word tiles** and build with `læse`.
   - *Expected*: five supplied tiles, required count four; meaning/shape met, target not met,
     known language issue.
2. Regenerate and build with `læser`, leaving `læse` unused.
   - *Expected*: all facets satisfied and the spare remains visible.

## Scenario: ${title} preserves supported exact attempt replay after ${committed ? "committed" : "uncommitted"} response loss

Runs for word choice and spare-word tiles, with loss before and after server commit.

1. Open optional support, answer correctly and lose the first attempt response.
   - *Expected*: unconfirmed-result message; answer controls disabled.
2. Choose **Check the same attempt**.
   - *Expected*: identical serialized request bodies, Good work and Try 1 / Supported practice.

## Scenario: ${title} reflows with accessible feedback at 200 percent text in ${theme}

Runs for all three formats in light and dark themes.

1. Generate at 320×844, enlarge text to 200% and scan.
   - *Expected*: zero Axe WCAG 2 A/AA violations and no horizontal overflow.
2. Answer correctly and check.
   - *Expected*: feedback badges remain unclipped; the same accessibility/reflow checks pass.
