# Astrid Visual Identity

## Confirmed family design requirement

**6 November 2026 — Follow the sibling application pattern.** The user requires Astrid to share the UI style and layout used across Lettuce, Toadie, Covenant, Astrolabe, and Flow, while having its own main color and unique logo. Reuse the family design system and application shell as the starting point. Place learning-specific interactions within that familiar layout; a separate visual identity does not call for an unrelated interface redesign.

Keep the shared typography, spacing, surfaces, controls, navigation conventions, light/dark treatment, and accessibility behavior. Exercise-specific components can introduce new interaction patterns while retaining that visual language.

## Preferred color direction

The user proposed red as the most suitable main color, drawing on the Danish flag. Treat red as the preferred direction; the exact palette and shades have not been selected.

Follow the sibling mechanism: a named `astrid` palette with light/dark shades and dedicated accent tokens. Use restrained red accents for primary actions, active navigation, and focus on the shared neutral surfaces. Select shades through contrast checks for their actual roles rather than copying another brand's shade indices unchanged.

The sibling apps commonly use red for blocking errors. Astrid needs an explicit semantic-color mapping because its proposed brand also uses red. Distinguish primary actions, incorrect answers, destructive actions, and system failures through wording, icons, component treatment, and color where useful. A wrong answer is a learning outcome. The final feedback palette remains open; no alternative error hue has been selected.

## Confirmed logo brief

Astrid must have a unique logo connected to Denmark or a Danish girl representing Astrid. The user explicitly links the character option to Astrid being a female name. Neither a final concept nor an asset has been selected.

Candidate directions for later exploration:

- A compact Denmark-related symbol, such as an original mark built around a flag-cross motif.
- A stylized Astrid character portrait with a clear Denmark-related cue.
- A combined character and geometric Danish motif, if it remains readable at small sizes.

These are proposals within the user's brief. No particular age, appearance, illustration style, or mascot behavior is implied by the character option. A portrait should carry its Danish connection through an intentional visual cue rather than an assumed appearance.

## Proposed asset and validation criteria

Use an original, simple mark compatible with the sibling logos' visual weight and placement. Plan a scalable vector master, favicon/app-icon treatment, light/dark versions where required, and a monochrome version. The mark should remain recognizable in the compact navigation rail and at small icon sizes.

Review typography and shell consistency alongside palette contrast, visible focus, reduced motion, and distinguishable answer/error states. Preserve accessible names and semantic controls when applying the new brand.

Production media remains a later phase. This brief records requirements and exploration work; it does not introduce application code or generate final artwork.

## Local reference evidence

Read-only inspection of `web/src/theme.ts` in the sibling checkouts confirmed custom brand palettes: Lettuce green, Toadie amber/brown, Covenant violet, Astrolabe cyan, and Flow blue. The inspected themes share bundled Inter typography and related Mantine conventions. Flow's `CLAUDE.md` and `web/CLAUDE.md` also describe its distinct logo, shared shell, theme tokens, and accessibility checks.

Sources are the local repositories under `~/Sources/{lettuce,toadie,covenant,astrolabe,flow}`. This was a source/configuration inspection, not a rendered visual comparison or a change to those projects.

Palette and logo questions are tracked as Q18 and Q19 in [Open questions](open-questions.md); planning is tracked as BL-021 in the [backlog](../../BACKLOG.md).
