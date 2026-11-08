# E2E scenarios — versioned test-design artifacts

One markdown file per spec file: `scenarios/practice.md` describes `tests/practice.spec.ts`. These are
**design artifacts, not executable tests** — deliberately NOT Gherkin/Cucumber (no step-definition
glue to rot). They exist so a test's *intent* is reviewable, diffable, and writable by anyone —
including someone (or some agent) who will later compile a new scenario into spec code.

**The same-commit rule**: a new or behaviorally changed `test()` lands with its scenario file
updated **in the same commit**. `npm run check:scenarios` (from `e2e/`) enforces the parity in both
directions; a spec without a scenario file is a review failure. There is no separate coverage map:
the scenario files are the record, and this directory plus the checker are the whole index.

## How the suite runs (context for "Owns")

Playwright runs `workers: 1` and `fullyParallel: false` (Chromium, then WebKit) against the assembled
compose stack on `http://127.0.0.1:8088`, so no two tests ever overlap and files need no isolation
between each other. There are no seed accounts and no login: the app opens a **development cookie
session** on first load (`POST /api/v1/development-session`). Every test gets a fresh browser
context, hence a fresh cookie and a fresh learner identity; `request`-only tests get their own API
context the same way. Two kinds of state therefore exist:

- **Server-side, scoped to that test's development session**: generated exercises, graded attempts
  and practice sessions (with their attempts, support events and follow-ups). Nothing deletes them
  and nothing reads another test's rows; the database volume simply accumulates review data
  (never `docker compose down -v` to clean up).
- **Device-level**: browser `localStorage` (recovery outbox, run records, changelog seen-state,
  navigation rail preference). A fresh context starts empty, so it cannot leak between tests.

## Format

```md
# <Journey name — short, human>

- **Spec**: [tests/<name>.spec.ts](../tests/<name>.spec.ts)
- **Actors**: <the anonymous learner, a second tab of the same learner, or an API client>
- **Owns** (exclusive server-side state): <what this file may mutate, per the section above:
  development-session-scoped exercises / attempts / practice sessions, or
  "nothing — read-only" / "device localStorage only" when applicable>

## Scenario: <the test() title, source text VERBATIM>

1. <numbered prose steps: actor → action, in user terms>
   - *Expected*: <the observable consequence asserted at that step>

## Not covered here (and why)

<per-journey deliberate exclusions — only when there are any, and only when the exclusion can be
pointed at the unit or server test that covers it>
```

Rules:

- **The `## Scenario:` heading must equal the `test()` title verbatim** — that is the
  traceability link, greppable in both directions. One `## Scenario` section per `test(` call in
  the file.
- **Template-literal titles keep their placeholders.** Many specs generate tests in a `for` loop
  (per theme, viewport, committed/uncommitted response loss…). The title in the source is a
  template literal, so the heading is that source text exactly as written, `${…}` expressions
  included — for example
  `## Scenario: changelog and compact build stamp reflow at enlarged text in ${theme}`, or
  `…${retry ? " after saved-operation retry" : ""}`. There is **one** heading per `test(` call, not
  one per generated instance; describe the instances in the steps ("in light, then in dark").
  The checker has no skip list and no carve-out: a title it cannot match is a failure to fix, either
  in the scenario or by giving the test a literal title.
- Titles must be string or template literals written directly in `test(...)`; the checker fails a
  `test(` whose title it cannot extract (for example a variable).
- Describe behaviour and what the spec actually asserts, never a claim that Danish content is
  teacher-approved: a passing browser test is not linguistic review.
