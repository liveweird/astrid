# Navigation: drawer, rail and keyboard order

- **Spec**: [tests/navigation.spec.ts](../tests/navigation.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/`), using the keyboard
- **Owns** (exclusive server-side state): device `localStorage` only (the rail preference
  `astrid.viewSettings.appShell.navCollapsed`); the page load also generates one exercise under the
  test's own development session

Tab traversal uses Alt+Tab in WebKit on macOS (Safari's default focus policy) and Tab elsewhere.

## Scenario: mobile navigation opens and closes with native keyboard behavior

1. At 390×844 the learner opens the playground.
   - *Expected*: **Toggle navigation** controls `primary-navigation`, is `aria-expanded=false`, and
     the navigation and its **Engine playground** link are hidden.
2. They Tab to the toggle and press Enter.
   - *Expected*: `aria-expanded=true` and the navigation is visible.
3. They Tab to **Engine playground** and press Enter.
   - *Expected*: the drawer closes (`aria-expanded=false`), keyboard focus lands on `#main-content`,
     and the navigation and link are hidden again.

## Scenario: desktop navigation remains in native keyboard order

1. At 1280×900 the learner opens the playground.
   - *Expected*: the navigation is visible without a drawer.
2. They Tab until they reach **Engine playground** (at most 12 presses).
   - *Expected*: the link receives focus in the natural tab order.

## Scenario: persisted rail preference survives a mobile drawer and viewport changes

1. With the rail stored as collapsed, the learner opens the playground at 390×844 and opens the
   drawer from the keyboard.
   - *Expected*: the drawer is open and the **Engine playground** link is visible (the collapsed
     rail does not shrink the mobile drawer).
2. They enlarge the window to 1280×900.
   - *Expected*: the navigation settles at 64px wide (the rail).
3. They Tab to the **Expand navigation** button.
   - *Expected*: it is focused, `aria-expanded=false`, controls `primary-navigation`, and
     `localStorage` still says the rail is collapsed.
4. They shrink the window back to 390×844.
   - *Expected*: the burger is `aria-expanded=false` and the navigation and its link are hidden.
