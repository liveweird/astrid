---
name: verify
description: Drive the Astrid SPA end-to-end with Playwright to observe a change working against the local stack. Use after nontrivial frontend/backend changes, before committing.
---

# Verifying changes end-to-end

## Handle

The surface is the SPA in a browser, backed by the real server and PostgreSQL. Two ways to get a stack (see `run-stack`):

- **Assembled stack (preferred for anything that touches the production bundle, lazy routes, headers or recovery):**
  `cp .env.example .env` once, then `docker compose up --build -d`. Drive `http://localhost:8088`.
- **Dev loop (fast UI iteration):** `docker compose up -d postgres`, `POSTGRES_PASSWORD=astrid-local-development ASTRID_DEVELOPMENT_MODE=true ./gradlew :server:run`,
  `cd web && npm run dev`. Drive `http://localhost:5180` (Vite proxies `/api` to 8088 and rewrites the Origin; 5180 is on the default host/origin allowlists).

Check what is already up first: `lsof -nP -iTCP:8088 -iTCP:5180 -iTCP:5438 -sTCP:LISTEN`. Reuse a healthy stack; stray `:server:run` JVMs squat 8088.
Astrid's ports deliberately avoid Lettuce (8080/5173/5432), Toadie (8081/5174/5433), Covenant (8082/5175/5434) and Flow (8084/5176/5435), so confirm WHICH app
answers (`curl -fsS localhost:8088/api/v1/ready`, title "Astrid · Danish learning engine") before concluding anything. The full suite is
`npm --prefix /Users/sebastian/Sources/astrid/e2e test` against the assembled stack; run it for UI/API changes.

No Chrome-extension automation is required: Playwright (Chromium and WebKit) is installed in `e2e/node_modules` (`cd e2e && npx playwright install chromium webkit` if
browsers are missing). Write a scratch script **outside the repo** (the session scratchpad) and import it by absolute path:

```js
import { chromium } from "/Users/sebastian/Sources/astrid/e2e/node_modules/playwright/index.mjs";
// run with: node scratch.mjs   (never from inside e2e/, so a stray file cannot shadow a module)
```

Axe is available the same way (`@axe-core/playwright`, WCAG 2 A/AA tags as in `e2e/tests/accessibility.spec.ts`).

## Drive recipe (gotchas that cost time)

- **There is no login.** The app calls `POST /api/v1/development-session` on first load and the `astrid_session` cookie (HttpOnly, SameSite=Strict, 24 h) is the identity. A new
  browser context is a new learner with no history; practice recovery needs the same context (cookie **and** `localStorage` keys `astrid.practice.v1.*`). To probe recovery, reload the page,
  do not open a new context. Server-side the API is 404 unless `ASTRID_DEVELOPMENT_MODE=true`.
- **Locate by role, label or text** — the app has no `data-testid`. Standalone exercise types live behind the collapsed **Generation controls** button; the format picker is the combobox
  **Exercise type** (open it, `fill` the title, press ArrowDown then Enter; see `e2e/support/generation-controls.ts`, `format-select.ts`). Mantine inputs: prefer
  `getByRole("textbox", { name })` over `getByLabel`.
- **Danish text** is `lang="da"`; the answer fields accept `æ ø å` (the app also offers character buttons; see `unicode-input-boundary.md` in `docs/knowledge/`).
- **Route changes:** `/` playground (unknown paths also show it), `/practice` (a run binds `?session=<id>`), lazy `/changelog`. After clicking a nav link `waitForURL` can pass while the old page is still
  rendered (the chunk loads lazily against the built bundle). `waitFor()` an element unique to the target page before filling. While on `/`, "Engine playground" is a fragment link that only moves focus to `#main-content`.
- **Failures and recovery:** simulate with `page.route("**/api/v1/…")` — `route.abort("connectionfailed")` for a lost response, `route.fulfill({ status: 503, contentType: "application/problem+json", body })`
  for a server error (as `e2e/tests/recovery.spec.ts`). The browser deadline is 15 s; an unconfirmed submission must keep the **same** attempt id/body available ("Check the same attempt").
- **Direct API probes need the guards:** `Host` must be allowed (default `localhost:8088` / `127.0.0.1:8088`), mutations need `X-Astrid-Client: playground` and `Content-Type: application/json`,
  practice mutations also `X-Astrid-Practice-Owner: <session id from /practice-profiles>`. Use a cookie jar (`curl -c jar -b jar`). Don't put cookies or inspector keys in transcripts.
- **WebKit** follows Safari semantics (Option-Tab for link focus); the WebKit failed-preload workaround exists only for the `Changelog-*` chunk of the production build, so reproduce lazy-load failures against `:8088`, not Vite.
- **Screenshots/theme:** the shell follows the OS scheme (`defaultColorScheme="auto"`) plus the header toggle; check both with `page.emulateMedia({ colorScheme })`. The 320 px / 200 % text reflow and short-window navigation cases are in `e2e/tests/layout.spec.ts`; reuse its approach when you change layout.

## Cleanup

Verification writes learner evidence (sessions, exercises, attempts, practice runs) and there is **no delete API**. Do not issue blanket SQL deletes or truncate tables, never touch another
session's rows, and **never run `docker compose down -v`** (it drops the database volume). Prefer a fresh browser context per scenario and leave the rows: they are immutable, session-scoped evidence and
expire from use after 24 h. If you need a clean database, start a throwaway PostgreSQL on another port and point a native server at it (`POSTGRES_JDBC_URL`, `POSTGRES_R2DBC_URL`, `ASTRID_PORT=8089`), then stop only
what you started (check `lsof` for the PID first). Remove scratch scripts, screenshots and traces you created; Playwright output under `e2e/` (`playwright-report/`, `test-results/`) is git-ignored. For a stack you
brought up, `docker compose down` (no `-v`) at the end unless the user was already running it.
