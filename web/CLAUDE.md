# Frontend (`web/`)

Vite + React 19 + TypeScript 6 + Mantine 9 SPA for Astrid, a Danish reading/writing learning engine: **Lessons** (`/`, Review at `/#review`), the **engine playground** (`/playground`), **guided
practice** (`/practice`) and the lazy **changelog** (`/changelog`). Server rules and the feature template: `.claude/docs/conventions.md`; test rules:
`.claude/docs/testing.md`; compatibility of what the UI sends and stores: `.claude/docs/compatibility.md`. Look at Flow's `web/CLAUDE.md` before inventing a
building block (Astrid is a much smaller app: no auth screens, tables, forms libraries or i18n).

## Commands (run from `web/`, install with `npm ci --legacy-peer-deps`)

- `npm run dev` — Vite on **127.0.0.1:5180** (`strictPort`; avoids Lettuce 5173, Toadie 5174, Covenant 5175, Flow 5176). Proxies `/api` to `http://127.0.0.1:8088`
  and rewrites the browser `Origin` from the dev origin to 8088 so the server's origin allowlist passes. Needs the server with `ASTRID_DEVELOPMENT_MODE=true` (`run-stack` skill).
- `npm run build` — `tsc -b && vite build` to `web/dist`; the Docker image bakes it in and Ktor serves it (`WEB_STATIC_DIR`) single-origin.
- `npm run gen:api` — `openapi-typescript` over `../server/src/main/resources/openapi/documentation.yaml` → `src/api/schema.ts`. **Commit the regenerated file in the same change as any spec edit.**
  `npm run check:api` regenerates to stdout and `diff`s against the committed file (never overwrites). `npm run lint:api` Spectral-lints the spec and `api-guidelines/examples/conformant.yaml`
  through `spectral.astrid.yaml` with `--fail-severity=warn` (fix the spec; a new `off` needs an inapplicability comment).
- `npm run lint` (`eslint . --max-warnings 0`), `npm run knip`, `npm test`, `npm run test:coverage` — zero-findings gates, details below. `scripts/check.sh` runs them all.

## Layout (`src/`)

- `App.tsx` — the shell and **React Router** routes; `main.tsx` — providers; `theme.ts` + `theme.module.css` + `index.css` — all styling.
- `pages/playground/` (`Playground.tsx` and its pieces, `useCatalogQuery.ts`, `usePlaygroundFocus.ts`), `pages/practice/` (`Practice.tsx` shell, `ActivePractice`, `PracticeChooser`,
  `SupportSections`, `PracticeMessages`, pure `practiceOperations.ts` / `practiceProfiles.ts`, `practiceRecovery.ts` browser outbox, `useInitialPracticeRecovery`, `usePracticeFocusQueue`),
  `pages/Changelog.tsx` (lazy).
- `components/` (shared UI: `ExercisePrompt`, `MatchingPrompt`, `FeedbackPanel`, `PracticeSummary`, `FormatSelect`, `InspectorPanel`, `VersionStamp`, `BrandMark`, `MarkdownView`,
  `ChangelogErrorBoundary`), `hooks/` (`useChangelogSeen`), `api/` (`client.ts` transport, `models.ts`, generated `schema.ts`), `changelog/` (`version.ts`, `entries.ts`), `test/` (shared test scaffolding).
- Default exports for components/pages, named exports elsewhere; **relative imports only** (no path aliases); a folder per page, no catch-all `utils/`. Tests are colocated
  (`Foo.test.tsx` beside `Foo.tsx`). Domain logic stays on the server/engine — the UI renders what the API says and never grades.
- Routes (`App.tsx`): `/` Lessons, `/#review` its three existing extra-practice profiles, `/playground` Playground, `/practice` (`PracticeRoute`), `/changelog` (`React.lazy` + `ChangelogErrorBoundary` + `Suspense`), `*` a page-not-found state linking to Lessons. New routes go above `*`.
  Lessons is passive: browsing reads a guarded saved-run pointer without bootstrapping a development session or replaying an outbox. An explicit start validates the profile, saves the existing serialized creation operation and navigates to the runner; the runner owns sending/recovery. Review is not cumulative across studied lessons. No vocabulary-library route exists yet.
  `/practice?session=<id>` is the URL-bound run: `Practice` binds it with `navigate(…, { replace: true })`; a pushed navigation to `/practice` remounts it so recovery restarts as a full page load would, while its own
  `replace` keeps the mounted run. `useRouteChangeFocus` moves focus to `#main-content` on a pushed navigation only (replace/Back leave focus alone).

## Data layer: TanStack Query is deliberately narrow

`main.tsx` creates one `QueryClient` with `{ retry: false, refetchOnWindowFocus: false, staleTime: Infinity }` (commented there): the practice recovery protocol owns every retry, a focus refetch would
race the cross-tab owner checks, and revisions only change with a deploy (a 409 already forces a fresh read — `Playground` removes the cached catalog on a revision-changed 409).

- **Queries only for session-scoped reads that are plain GETs:** `GET /catalog` (`catalogQuery`, key `["catalog"]`, `enabled` once the development session exists) and `GET /practice-profiles`
  (`practiceProfilesQuery`, key `["practice-profiles"]`). Both wait for `startDevelopmentSession()` first (the cookie must exist).
- **No `useMutation`, and no query for `GET /practice-sessions/{id}`.** Every POST and `loadPracticeSession` stay on the handwritten client. Mutations here are not "send and invalidate": each carries
  a client-generated identity (`attemptId`, `practiceSessionId`) and its **exact serialized body**, saved in the browser outbox *before* sending and replayed byte for byte after a lost response, refresh
  or reload (`practiceRecovery.ts`, `practiceOperations.ts`). A mutation library's retry/cache semantics would re-implement or fight that outbox. Do not wrap them.
- Tests get the same defaults from `renderWithProviders` (below).

## Transport (`api/client.ts`)

- `readJson<T>` is the one fetch wrapper: same-origin credentials, a **15 s deadline** covering the response body (`AbortController`), JSON parse. Every new call goes through it; every mutation sends
  `X-Astrid-Client: playground` and `Content-Type: application/json` (`serializedJsonInit`), practice mutations also `X-Astrid-Practice-Owner`, the inspector `X-Astrid-Inspector-Key`.
- **`UncertainResultError`** = "the server may have committed": a network failure, abort/timeout, a truncated/unparseable body, and (for attempts) any `5xx`. The UI keeps the same request available
  ("Check the same attempt", "Retry saved operation") and never regenerates identity. **`ApiError(status, message)`** = a definite answer; branch on `status`, and render **only** its message through
  the page's mappers (`practiceError`) — never raw `Error.message` from a failed `fetch`.
- Error bodies are `application/problem+json` (`type`, `title`, `status`, `detail`, `instance`, `code`); the client reads `detail` (and a legacy `message` if present) regardless of content type. The
  server's status ordering is a contract (403/415 before 401, 404 when development mode is off): `.claude/docs/security.md`.
- Types come from generated `schema.ts` through `api/models.ts`; hand-write only what the generator cannot express (e.g. the `AttemptAnswer` discriminated union). Keep `schema.ts` out of lint/knip/coverage
  (already configured). Avoid heavyweight client generators: `openapi-typescript` (types only) + hand-written fetch is intentional. `openapi-typescript` installs with `--legacy-peer-deps` because it
  declares a TypeScript 5 peer; the generated output is compatible with TS 6.
- A new endpoint: spec first (`conventions.md`), `gen:api`, a function in `client.ts`, a `models.ts` alias, a `client.test.ts` case for its error mapping.

## Recovery rules (do not regress; e2e `recovery.spec.ts`, `practice-multitab.spec.ts` guard them)

Practice persists under `astrid.practice.v1.*` in `localStorage` (run, latest pointer, outbox per run). An outbox entry stores `{ ownerSessionId, practiceSessionId, kind, identity, body }`; replay sends
`identity` + `body` **unchanged**. Recovery requires the original cookie and storage; otherwise **Start new practice** (old server history stays intact). A confirmed grade is read back, never regraded.
Persistence failures surface as `PracticePersistenceError` and the operation is **not sent**. Related records: `docs/knowledge/runtime-recovery.md`, `practice-continuity.md`.

Standalone matching keeps the pending serialized attempt in memory and resends it exactly; keep the page open for recovery. The board is scoped to the exercise ID, so an unfinished English selection cannot survive regeneration. A confirmed retry retains pairs, unlocks all of them and focuses the board. Matching feedback maps server rows to displayed words; the UI never determines correctness.

## Theme and brand

`src/theme.ts` is the single source (Mantine `createTheme`): the 10-stop **`astrid` red tuple** (Danish flag), `primaryColor: "astrid"`, `primaryShade { light: 7, dark: 6 }`, `autoContrast`, `respectReducedMotion`,
bundled Inter (`@fontsource-variable/inter`, same-origin for the CSP), a reduced `cssVariablesResolver` (AA-safe dimmed/orange text). `index.css` holds the `--astrid-*` surface and navigation tokens for both schemes;
`theme.module.css` the class-level parts. Colour scheme follows the OS (`defaultColorScheme="auto"`) with a header toggle.

- Red is the **brand** (primary actions, active nav, focus) *and* the colour of blocking system failures (red `Alert`s for load/connection errors). **A wrong answer is a learning outcome, not an error:** feedback uses
  teal (`goodWork`, `IconCircleCheck`) and orange (needs another look, `IconAlertTriangle`) with explicit wording and icons, never red, and never colour alone (`docs/knowledge/visual-identity.md`).
  New states go through that vocabulary: red = failure, orange = revisit/unconfirmed, teal = success, gray = neutral/support, `astrid` = identity/target badges.
- Prefer theme `extend`s and `theme.module.css` over per-component CSS. Scheme-dependent colours are `--astrid-*` tokens defined for light, dark and the first-paint `prefers-color-scheme` case in `index.css`; consume the token, don't re-derive hexes in components.
- Brand mark: `components/BrandMark.tsx` picks `public/logo-light.svg` or `logo-dark.svg` from the computed colour scheme
  (decorative `alt=""`; the wordmark names the brand). `index.html` serves the same files as media-matched favicons, with
  `favicon.svg` (a copy of the light logo) as the scheme-agnostic fallback. Edit the light and dark logos together.

## Language, accessibility, keyboard

- **UI chrome is English only, by decision** (no i18n framework; `docs/knowledge/flow-reuse-manifest.md`: Danish is *content*, not chrome). Do not add react-i18next or locale files.
- **Mark every Danish passage `lang="da"`** (and English cues/choices `lang="en"`, or the payload's `cueLanguage`/`answerLanguage`) so screen readers pronounce them; `ExercisePrompt.tsx` is the reference. Danish characters
  (`æ ø å`) have insert buttons; the input boundary is `docs/knowledge/unicode-input-boundary.md`.
- Shell a11y that must stay: a **skip link** (`.skip-link`, "Skip to learning exercise"/"Skip to main content" → `#main-content`, `tabIndex={-1}`), the burger/rail toggles with `aria-expanded` + `aria-controls`,
  `aria-label`s on icon links in rail mode, accessible names that match the visible text (e2e finds elements by role/label/text — **there is no `data-testid`; do not add one**).
- **Focus management** is explicit: after submit/advance/support the next meaningful element gets focus (`useExerciseFocus`, `useRequestedFocus`, `usePracticeFocusQueue`); alerts that need attention
  are focusable (`tabIndex={-1}`) with a ref; feedback is `role="status"`. Respect reflow: 320 px at 200 % text must not clip (`layout.spec.ts`, `docs/knowledge/text-resize-reflow.md`).
- Keyboard rules: `docs/knowledge/keyboard-navigation.md` (ordering is operable by keyboard; Mantine `Select` Enter behaviour; WebKit Option-Tab). Axe (WCAG 2 A/AA) runs in e2e (`accessibility`, `practice`, `practice-follow-up`, `topic-practice`, `writing-reading`, `changelog` specs) — a new
  state needs a scan in one of them.

## Changelog, version stamp, build identity

- `changelog/version.ts` holds `APP_VERSION` (tiny module so the shell can show it without loading entries); `changelog/entries.ts` holds the newest-first English entries rendered by the lazy page through
  `MarkdownView` (react-markdown + GFM). **A release = a new top entry + the bump of that literal + the same `info.version` in `documentation.yaml`** (rules: `conventions.md`, `docs/knowledge/app-versioning.md`);
  `entries.test.ts` and `OpenApiContractTest` fail otherwise. Keep tested phrases in plain text runs (markdown formatting splits text nodes).
- `hooks/useChangelogSeen.ts` is a `useSyncExternalStore` over guarded `localStorage` key `astrid.changelog` (`{ seenVersion }`); unavailable/corrupt storage must never break the app. Reading notes starts no learning session.
- `vite.config.ts` injects `__APP_COMMIT__` (short sha, `+dirty` locally) and `__APP_COMMIT_TIME__` via `define` (types in `src/build-globals.d.ts`); `GIT_SHA`/`GIT_COMMIT_TIME` override git (the Dockerfile and CI set them).
  `components/VersionStamp.tsx` renders `v<version> · <sha> · <time>` in the nav footer (version only in rail mode). The build also omits the lazy `Changelog-*` chunk's own `modulepreload`
  (`build.modulePreload.resolveDependencies`) to dodge a WebKit failed-preload cache bug — keep the chunk name stable and see `app-versioning.md` before touching it.
- Other `localStorage` keys: `astrid.viewSettings.appShell.navCollapsed` (rail). All storage access is guarded by try/catch.

## Quality gates

- **ESLint** (`eslint.config.js`: typescript-eslint, react-hooks, react-refresh, sonarjs recommended; core backstops `complexity` 40, `max-lines-per-function` 260, `max-params` 6 — "today's max" is
  in the comment, backstops not targets). Zero warnings. Tune only in the config with a comment naming the idiom (today `sonarjs/no-nested-conditional` off, `_`-prefixed unused names allowed); an inline
  `eslint-disable` needs a justification. `src/api/schema.ts` is excluded.
- **knip** (`knip.json`): unused files/exports/dependencies fail; test files count as entries, so an export used only by tests is still flagged — un-export it or delete it. `schema.ts` is ignored, `diff` is an allowed binary.
- **TypeScript**: strict, `noUnusedLocals/Parameters`, `verbatimModuleSyntax`, `erasableSyntaxOnly` (no enums/parameter properties).
- **Tests**: Vitest 5 + happy-dom + Testing Library. Use `renderWithProviders` from `src/test/render.tsx` (Mantine with `env="test"` — required or Select/Popover dropdowns stay hidden —, a fresh `QueryClient` with the
  app defaults, a real `BrowserRouter`; set the URL with `window.history.replaceState` first, assert `window.location` after; `strict: true` replays mount effects like `main.tsx`). Shared fixtures:
  `src/test/playgroundFixtures.tsx`, `practiceFixtures.ts`; stub `fetch` per test and restore it. Test names state behaviour.
- **Coverage floors** (`vite.config.ts` `test.coverage.thresholds`): statements 95, branches 89, functions 97, lines 97 (2026-11-09). Ratchet: floor = integer below the measured value, raised when coverage rises, never lowered.
  `main.tsx`, `schema.ts`, tests and `src/test/**` are excluded from the measure.
- `scripts/check.sh` also runs `lint:api`, `check:api`, e2e `lint`/`knip`/`typecheck`; Playwright journeys need the stack (`testing.md`).
