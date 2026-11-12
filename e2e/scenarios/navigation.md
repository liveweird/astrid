# Learner navigation: lessons, review, recovery and safe routes

- **Spec**: [tests/navigation.spec.ts](../tests/navigation.spec.ts)
- **Actors**: an anonymous learner using the lessons home, review anchor and practice workspace
- **Owns** (exclusive server-side state): the passive, unknown-route and reflow scenarios own no
  server state; each start/resume scenario owns its development session, practice session and attempt

## Scenario: home and review navigation stay passive and expose the bounded learning catalogue

1. The learner opens `/` while every `/api/` request is recorded.
   - *Expected*: **Learn Danish** is the page heading. **Module 1: First steps** retains fifteen lesson starts; **A day of work and study**
     adds five separately named P1 starts, for exactly twenty lesson starts total. The **Review** section
     retains exactly three fixed practice starts whose copy limits their scope to familiar words,
     sentence order, verb forms and articles.
2. They inspect the navigation and follow **Review**.
   - *Expected*: **Lessons** is initially active, **Engine playground** is secondary under
     **Developer tools** and links to `/playground`; Review changes the URL to `/#review`, becomes
     active and focuses the Review heading.
3. The request record is inspected.
   - *Expected*: neither the initial home nor review-anchor navigation made any learning API request.

## Scenario: home starts a ${entry.kind} through the saved creation handoff

Runs for the first fixed lesson (**Talking about one person**) and one bounded existing review
(**Mixed practice**).

1. The learner opens the relevant home section and presses its named Start button.
   - *Expected*: before the creation request leaves the browser, local storage already contains a
     creation outbox entry whose serialized body is byte-for-byte identical to that request.
2. The creation succeeds.
   - *Expected*: `/practice?session=<id>` opens the appropriate existing runner state; the URL,
     request and server state carry the same session identity and the selected literal focus/plan.

## Scenario: home resume preserves an outstanding attempt identity and replays its exact body

1. The learner starts **Mixed practice**, answers the first part incorrectly, and the server commits
   the attempt before its browser response is aborted.
   - *Expected*: the runner reports an uncertain operation and retains its recovery record.
2. They follow **Lessons** and press **Resume saved practice**.
   - *Expected*: the home describes the saved run, then returns to the exact `?session=` identity.
3. Recovery completes.
   - *Expected*: the attempt is sent twice with byte-identical bodies and the same `attemptId`, while
     the server still records exactly one attempt for the item.

## Scenario: an unknown route is safe and returns to Lessons without starting learning

1. The learner opens an unknown URL while every `/api/` request is recorded.
   - *Expected*: **Page not found** renders inside the normal shell and developer tools stay available.
2. They press **Go to Lessons**.
   - *Expected*: `/`, the **Learn Danish** page heading and **Module 1: First steps** render, with no learning API request made.

## Scenario: learner navigation reflows at 320px and 200% text in ${theme} mode

Runs once with a light preference and once with a dark preference.

1. At 320×720 and 200% text, the learner opens the lessons home.
   - *Expected*: the selected scheme is applied, root text is 32px, axe reports no WCAG A/AA
     violations and the document has no horizontal overflow.
2. They open the mobile navigation drawer.
   - *Expected*: its expanded state is exposed; **Lessons**, **Review**, **Engine playground** and
     footer **Changelog** are visible, **Developer tools** labels the secondary
     destination, the unread marker remains visible, and the open drawer also passes axe/reflow.
3. They follow **Review** from the drawer.
   - *Expected*: `/#review` focuses a visible, unclipped Review heading below the fixed header; the
     focused review section still has zero axe violations and no horizontal overflow.

## Not covered here (and why)

- Exercise grading, full practice recovery variants and all twenty-three chooser profiles remain in the
  existing practice, recovery, topic-practice, module-one and P1 vocabulary journeys. This file covers only the new
  learner-navigation handoffs and does not duplicate their matrices.
