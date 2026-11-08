# Layout: 320px reflow and short windows

- **Spec**: [tests/layout.spec.ts](../tests/layout.spec.ts)
- **Actors**: an anonymous learner on the engine playground (`/`)
- **Owns** (exclusive server-side state): development-session-scoped generated exercises and graded
  attempts in this test's own browser context

## Scenario: all pilot formats and recovery states reflow at 320px with 200% text

1. At 320×900 with root text enlarged to 200% from first paint, the learner opens the playground.
   - *Expected*: **Check answer** shows, the root font size is 32px, no page error was thrown, and
     the **Toggle navigation** and **Use dark theme** header buttons lie fully inside the header and
     the 320px width.
2. They press Tab (Alt+Tab in WebKit on macOS).
   - *Expected*: the "Skip to learning exercise" link was off-screen before and is now focused and
     in the viewport; a second Tab moves on.
3. They open and close the navigation drawer.
   - *Expected*: the navigation is visible while open; the exercise has no horizontal overflow and
     no clipped badge/accordion/button labels.
4. They open **Learn this first**, **Show hint**, then answer and check.
   - *Expected*: no horizontal overflow in the lesson, hint and feedback states; the feedback state
     also has no clipped labels.
5. They visit **Supply the indefinite article**, **Arrange a subject-first statement**, then the
   remaining pilot formats.
   - *Expected*: each format change moves focus to the exercise heading; no horizontal overflow in
     each; the remaining pilot formats (from the fourth on) also have no clipped labels.
6. They choose **Recall the Danish headword** and enter the headword with the wrong article.
   - *Expected*: "One-word response required" feedback has no overflow, clipped labels or clipped
     badge text (measured against the badge's rounded shape).
7. A mocked `409 revision_conflict` answers the next exercise request while they choose
   **Choose the present form**.
   - *Expected*: **Reload engine** shows without horizontal overflow or clipped labels; still no
     page errors.

## Scenario: format navigation stays inside short ${viewport.width}x${viewport.height} windows at ${fontSize}px

Runs for each of two windows (844×390 and 320×400) at each of two text sizes (16px, and 200% text
= 32px): four instances.

1. The learner opens the playground at that window size, enlarges text if the size is 32px and opens
   the generation controls.
   - *Expected*: the root font size equals the size under test.
2. For each of the ten pilot formats they select it if needed, then open the Exercise type list.
   - *Expected*: the current format is the selected option and the highlighted option is fully
     visible inside the list and window.
3. They press Arrow Down once per format.
   - *Expected*: the highlighted option stays visible every time and the document scroll position is
     unchanged.
4. They check overflow and press Escape.
   - *Expected*: no horizontal overflow; the list closes (`aria-expanded` false).
