# Accessibility sweeps (axe, WCAG 2 A/AA)

- **Spec**: [tests/accessibility.spec.ts](../tests/accessibility.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/`) with a development session
- **Owns** (exclusive server-side state): development-session-scoped generated exercises and
  graded attempts in this test's own browser context; nothing shared with other tests

Every scan is axe with the `wcag2a`, `wcag2aa`, `wcag21a` and `wcag21aa` tags and must report zero
violations.

## Scenario: pilot formats, feedback and recovery states have no WCAG A/AA violations

1. The learner opens the playground.
   - *Expected*: the "Danish engine playground" heading shows; the first exercise scans clean.
2. They pick the first answer choice and press **Check answer**.
   - *Expected*: a feedback status appears and scans clean.
3. They switch (via the Exercise type combobox, typing the title, checking its exact highlighted match and pressing Enter)
   to **Supply the indefinite article**, then **Arrange a subject-first statement**.
   - *Expected*: each format scans clean.
4. For **Supply the present form**, **Repair the verb form** and **Repair the indefinite article**
   they open the format, scan, type "unknown" into its text field and check.
   - *Expected*: the unfamiliar-writing feedback status scans clean for each.
5. The next exercise request is answered with a mocked `409 revision_conflict`
   (`application/problem+json`) while they choose **Choose the present form**.
   - *Expected*: a **Reload engine** button appears and the recovery state scans clean.
6. They press **Reload engine**, pick an answer, and the attempt request is aborted
   (`connectionfailed`) when they check.
   - *Expected*: "The result is not confirmed" shows and scans clean.

## Scenario: pilot formats, feedback and recovery states have no WCAG A/AA violations in dark mode

1. The browser emulates a dark colour scheme, then the learner repeats every step of the previous
   scenario (formats, feedback, revision conflict, uncertain result).
   - *Expected*: all scans stay clean in dark.
2. They check the document.
   - *Expected*: `<html data-mantine-color-scheme="dark">` — the emulated preference was honoured.

## Scenario: open ${title} format list remains accessible with enlarged text

The test is instantiated once for each of the thirteen playground format titles. Every instance uses a fresh
browser context and performs the complete list, keyboard, scroll and accessibility checks for its `title`.

1. At a 320px-wide viewport with root text enlarged to 200% (computed font size 32px), the learner
   opens the playground and the generation controls.
   - *Expected*: the page renders; the root font size is exactly 32px.
2. They select the instance's format (if it is not already current), then open the Exercise type listbox.
   - *Expected*: the list is visible, the combobox is `aria-expanded`, the current format is the
     selected option and the active descendant; every option's left and right edge stays inside
     0–320px.
3. They press Arrow Down once per format.
   - *Expected*: the highlighted option stays visible inside the listbox and the viewport each time,
     and the document scroll position does not change.
4. They scan with the list open, then press Escape.
   - *Expected*: zero axe violations; the list closes.

## Scenario: vocabulary prompts and constrained feedback have no WCAG A/AA violations in ${theme} mode

The test runs once with `theme` = light and once with dark (emulated colour scheme).

1. The learner opens the playground and chooses **Recognize the meaning** (seed 47, index 0).
   - *Expected*: the prompt scans clean.
2. They pick the first answer choice and check.
   - *Expected*: the feedback status scans clean.
3. They choose **Recall the Danish headword** and scan, then type the target headword with the wrong
   indefinite article ("et" for *brev*, "en" otherwise).
   - *Expected*: the feedback shows "One-word response required" and scans clean.
4. They press **Try again**, type "unfamiliar" and check.
   - *Expected*: the unfamiliar-writing feedback scans clean.

Format options are matched by their exact accessible titles: the original present-form task and the new present-form tile task share a prefix. The list sweeps include all thirteen entries.
