# Cumulative mixed review (`/#review`, `/practice`)

- **Spec**: [tests/cumulative-review.spec.ts](../tests/cumulative-review.spec.ts)
- **Actors**: an anonymous learner in one development browser session; seeded lessons open their
  saved session introduction so the server, rather than the test, determines review eligibility
- **Owns**: development-session-scoped lesson and cumulative practice sessions, exercises and
  attempts; device `localStorage` recovery for the cumulative run

## Scenario: cumulative review stays passive until preparation and explains an empty pool

1. The learner opens `/#review` without interacting with review preparation.
   - *Expected*: Review is visible and no learning API request is sent.
2. They press **Prepare mixed review** with no introduced lesson material.
   - *Expected*: only development-session bootstrap and the fresh review-context GET are sent;
     an accessible empty state says to open and save a lesson’s teaching and no start action is offered.
     The empty state receives focus so keyboard and assistive-technology users hear the result.

## Scenario: cumulative review reports short and full eligible pools and mixes introduced targets

1. The learner prepares an empty review, then one real lesson session records its introduction.
   They prepare again.
   - *Expected*: the card honestly reports a non-zero run shorter than eight.
2. Three more distinct lessons record their introductions and the learner refreshes preparation.
   - *Expected*: the server advertises eight items from multiple source lessons and the visible
     prepared summary receives focus.
3. They start the review, wait for the visible **Session introduction** confirming creation,
   and inspect its persisted server plan.
   - *Expected*: the URL binds the cumulative run, which has eight items and more than one target;
     navigation alone does not imply that the server has committed the run yet.

## Scenario: cumulative review replays an exact saved creation and resumes the URL-bound run

1. Four lesson introductions make a cumulative review available. Its creation commits on the
   server but the first response is lost.
   - *Expected*: the page keeps an alert and the generated session id in the URL.
2. The learner reloads.
   - *Expected*: the saved creation is replayed byte-for-byte and resolves to the same server
     session without generating a new identity. The learner opens and saves **Session
     introduction**, then the first answer control is available.

## Scenario: cumulative prepared and completed views reflow accessibly in ${theme} mode

Runs in light and dark at 320px with 200% text.

1. After four real lesson introductions, the learner prepares a cumulative review.
   - *Expected*: the prepared card has no WCAG A/AA violations or horizontal overflow.
2. The learner opens and saves **Session introduction**; the run is then answered and advanced
   through its real API, then reloaded.
   - *Expected*: the accessible completion summary offers **Prepare another mixed review**, does
     not offer a source-linked selected review, and reflows without horizontal overflow.
3. They press **Prepare another mixed review**.
   - *Expected*: `/#review` opens and requires a fresh explicit preparation.
