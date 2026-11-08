# The changelog page, the build stamp and the what's-new indicator

- **Spec**: [tests/changelog.spec.ts](../tests/changelog.spec.ts)
- **Actors**: an anonymous learner; a second tab of the same browser context; one test also runs a
  guided practice session
- **Owns** (exclusive server-side state): the changelog itself is a build-time artifact and the
  seen-state is device `localStorage` (`astrid.changelog`), so the changelog scenarios touch no
  server state. "visiting release notes preserves…" creates one practice session, exercise rows and
  one attempt under its own development session

## Scenario: the real build stamp opens release notes and clears the current-version indicator

1. The learner opens the playground on a fresh context.
   - *Expected*: the "Build version" stamp reads `vX.Y.Z · <commit> · YYYY-MM-DD HH:MM` (real
     committed build metadata); when `E2E_EXPECTED_COMMIT` / `E2E_EXPECTED_COMMIT_TIME` are set the
     commit and time must equal them. The stamp links to `/changelog` and the "What's new" indicator
     is visible.
2. They click the stamp.
   - *Expected*: the **Changelog** page opens; its first version equals the stamp's version, its
     first date is `YYYY-MM-DD`, the indicator is gone and `localStorage` holds that version as
     `seenVersion`.
3. They follow the **Engine playground** link back.
   - *Expected*: the playground shows and the indicator stays cleared.

## Scenario: direct changelog navigation works without learning requests when seen storage is ${storage}

Runs with `storage` = `corrupt` (the stored value is `not-json`) and `unavailable` (reading and
writing storage throws `SecurityError`).

1. The learner opens `/changelog` directly while recording every `/api/` request.
   - *Expected*: the Changelog heading, a version and a list render; axe reports zero violations and
     no horizontal overflow.
2. They reload.
   - *Expected*: the page renders again, and no `/api/` request was ever made (the page needs no
     development session).

## Scenario: reading the changelog in another tab immediately clears the original indicator

1. The learner's stored `seenVersion` is `0.0.0`; they open the playground.
   - *Expected*: the "What's new" indicator shows.
2. In a second tab of the same context they open the changelog.
   - *Expected*: the indicator disappears in the second tab and, without any reload, in the first
     tab too.

## Scenario: changelog and compact build stamp reflow at enlarged text in ${theme}

Runs once in light and once in dark (emulated colour scheme).

1. At 320×720 with text enlarged to 200%, the learner opens the changelog.
   - *Expected*: zero axe violations and no horizontal overflow.
2. They open the navigation drawer with **Toggle navigation**.
   - *Expected*: the "Build version" stamp is visible; scan clean. They close the drawer.
3. They widen the window to 1200×500 and press **Collapse navigation**.
   - *Expected*: the compact stamp link shows only `vX.Y.Z` and carries the full build text in its
     `title`; once the "Expand navigation" tooltip is fully opaque the page scans clean.
4. They press **Expand navigation**.
   - *Expected*: the full stamp returns; with the "Collapse navigation" tooltip settled the page
     scans clean.

## Scenario: visiting release notes preserves an uncertain practice answer and its owner

1. The learner starts **Mixed practice**.
   - *Expected*: the first exercise shows with **Check answer**.
2. They choose a wrong answer and check, while the first attempt request is committed by the server
   and then aborted (`connectionfailed`).
   - *Expected*: an alert reports the uncertain result.
3. They open **Changelog** and come back via **Practice**.
   - *Expected*: the same `?session=` practice session reopens and the saved operation is replayed
     with the **identical request body** (two attempt requests, equal bodies); no new development
     session was requested.
4. They read the session through the API.
   - *Expected*: same owner session as before, exactly one recorded attempt on the first item.

## Scenario: an ${failure} changelog chunk keeps navigation available and can reload safely

Runs with `failure` = `interrupted` (first chunk request aborted) and `HTTP 503` (first chunk
request answered `503`).

1. The first request for the lazily loaded Changelog script fails; the learner opens `/changelog`.
   - *Expected*: an alert mentions the Changelog; the navigation and its **Practice** link stay
     visible and the "What's new" indicator is still shown.
2. They press **Reload changelog**.
   - *Expected*: the Changelog heading renders, the alert is gone, the indicator clears, the script
     was requested exactly twice and no `/api/` request was made.

## Not covered here (and why)

- **The exact entry list, release-body rendering, and the version pin
  (`CHANGELOG[0].version === APP_VERSION`)** — pinned by the unit suites
  `web/src/changelog/entries.test.ts`, `web/src/pages/Changelog.test.tsx` and
  `web/src/components/VersionStamp.test.tsx`; this spec only asserts the real built stamp agrees with
  the page.
- **The error boundary's rendering of a failed changelog** — `web/src/components/ChangelogErrorBoundary.test.tsx`;
  the browser scenario above covers the real failed script load and reload.
