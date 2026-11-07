# Runtime Database Recovery

## Reproduced gap — 7 November 2026

A separate loopback-only Docker QA project warmed the application pool, created a session/exercise and saved an attempt, then paused only its own PostgreSQL container. A second attempt received no HTTP response within a 35.003-second client observation window. Resuming PostgreSQL restored readiness; exact same-ID replay returned retry 2, and the original retry-1 response remained byte-identical. The user's normal Compose preview and database were not paused.

The pool's configured 10-second maximum acquisition wait does not bound a statement after a connection is acquired. The browser previously had no request deadline, so this case left the visible exercise busy. No attempt-integrity failure was found.

## Working implementation decision

Use a 15-second deadline per browser API request, including consumption of the JSON response body. This is an assistant implementation choice for the authorized private pilot, slightly beyond the current pool-acquisition budget. It adds no automatic retries or new product setting.

On an incomplete successful response or transport timeout, retain the existing uncertain-result flow and exact attempt identity/payload. A timeout cannot establish whether a write committed. A received HTTP error keeps its known status even if its error body fails; 401/409 recovery and uncertain 5xx handling remain distinct. Clear the request timer after every outcome.

The deadline applies to each request, not the complete three-request bootstrap. JavaScript execution and timers can be suspended in a background/frozen browser; this is not a hard wall-clock guarantee for suspended tabs. [AbortController](https://developer.mozilla.org/en-US/docs/Web/API/AbortController/abort) can abort both fetch and response-body consumption.

Readiness must separately preserve caller cancellation: return `false` for ordinary database failure, rethrow `CancellationException`. This leaves the readiness HTTP success/503 contract intact.

## Server boundary and limits

The browser deadline bounds visible waiting; it does not prove server rollback, connection release or removal of held locks. Prolonged database stalls can retain server work and pool connections, and readiness calls can remain pending. A future server deadline requires independent evidence of transport termination, cleanup completion and pool recovery.

The [pinned R2DBC pool acquisition option](https://github.com/r2dbc/r2dbc-pool/blob/v1.0.2.RELEASE/README.md#supported-connectionfactory-discovery-options) covers acquisition only. The pinned PostgreSQL driver's statement-timeout setting becomes a PostgreSQL backend setting; it cannot fire while the backend process is paused. Local inspection of pinned Exposed 1.6 also found transaction cleanup in `NonCancellable`, so wrapping a transaction in a coroutine timeout alone does not establish a timely response during a paused-backend cleanup. Startup connection retries remain a separate policy documented in the [hardening delivery](hardening-delivery.md).

## Acceptance and evidence

Verify stalled headers, stalled success/error bodies, timer cleanup, preserved status classification and exact attempt replay with controlled timers. A browser regression must let the real server commit before withholding the response until the deadline; the paused-database experiment can instead stall during session lookup before the write.

The implemented client passed 29 frontend tests, lint/build and E2E type checking. Coverage ratcheted to floors of 89% statements, 82% branches, 89% functions and 91% lines. All 11 packaged browser journeys passed, including the commit-before-timeout case with byte-identical request/response replay and retry number 1.

A real Chromium run against the final recovery image then paused only the owned QA database after warming the pool and storing an original attempt. The page left its busy state and offered exact-attempt recovery after **15.531 seconds**, with answer and generation controls frozen and hint evidence preserved. PostgreSQL was unpaused in cleanup. The pending ID replayed with retry 2; another same-ID replay returned identical bytes, the original retry-1 response remained unchanged, readiness recovered and a fresh exercise could be generated. This is observed foreground-browser evidence, not a universal timing guarantee.

All 26 server tests passed, including the real-PostgreSQL readiness test that observes cancellation from inside the cancelled child rather than inferring it from the child job's state. Independent code review found no remaining actionable issue in the deadline or readiness changes. Owned-resource cleanup and final Git/CI evidence are recorded after observing them.

## Additional continuity checks

- **Image upgrade:** in one browser context with routing interception disabled, normal navigation after replacing the QA app fetched the new hashed entry module and semantic foreground. The HttpOnly development session stayed unchanged, the original attempt remained byte-identical, HTML carried `no-cache` and API responses carried `no-store`.
- **Real session expiry:** expiring only that browser's database session produced a real 401 and the explicit reconnect action. Reconnection rotated the HttpOnly cookie; the new session received 404 for the old owner's exercise and successfully stored its own retry-1 answer. This controlled database expiry was not a 24-hour soak.
- **Idle resume:** a browser kept the same exercise open for 661 seconds with no application API requests. Only the QA app's health polling was disabled for this experiment. Its container/session stayed unchanged; resuming replaced the idle physical database connection and saved the intended retry-1 answer. The observed local submission took 74 ms; this is continuity evidence, not a latency guarantee.
- **Recovery contrast:** the new real-outage state exposed light-mode orange text at 3.61:1. A darker semantic foreground now measures 4.86:1 against the same background. The final 11-journey packaged run includes Axe checks of this uncertain state in both themes with no exclusions and passed completely.
- **Complete pilot traversal:** an independent Chromium check submitted all 290 eligible format–sentence pairs and one wraparound per format: 293 submissions with distinct exercise/attempt identities. Each target and linguistic-acceptability result passed, every new attempt was retry 1, support transitions remained correct, and no sentence repeated within its format before the complete cycle. The answer oracle used the five taught present forms, eight authored noun genders and pronoun identities. This validates the published pilot inventory and browser/API continuity; it does not establish teaching suitability or learning effectiveness. See the [variation audit](pilot-variation-audit.md) for the uneven distribution of verb families.
- **Actual process restarts:** only the labelled QA PostgreSQL container was restarted first, then only its app container. The untouched counterpart retained its process/start identity in each phase. All three formats retained byte-identical public exercise snapshots, saved responses and exact same-ID/same-payload replay; fresh attempts on those original exercises advanced to retries 2 and 3. The same HttpOnly session survived, and the visible exercise opened before both restarts still submitted as retry 1 afterward. Ten saved responses were checked. Readiness returned within the bounded observation windows (655 ms and 2,653 ms locally); these timings are observations, not availability guarantees. Neither phase tested destructive volume loss or backup restoration.

## Browser portability acceptance

The same eleven journeys passed in both Chromium and WebKit on macOS. Native keyboard traversal uses macOS WebKit's Option-Tab policy; Linux continues to use Tab. This is rendering-engine coverage, not testing on a physical iPhone or a packaged mobile application.

The first [Linux branch run](https://github.com/liveweird/astrid/actions/runs/42329407025) passed the server, frontend, dependency and Kubernetes jobs. Its packaged browser job exposed a WebKit-only failure selecting a format after an accessibility scan: 20 journeys passed, one accessibility journey passed on retry and the other failed. Investigation and a clean Linux matrix remain prerequisites for merging this browser configuration into master. Preserve the accessibility scans and real control interaction while diagnosing the failure.

The [second branch run](https://github.com/liveweird/astrid/actions/runs/42330987616) passed both accessibility journeys but failed the ordinary pointer-format journey (21 passed). Trace review identified a shared trigger: clicking or tabbing into the expanding accordion scrolls its temporary clipped container. When expansion removes clipping, the Select reference can move outside the viewport while its portaled dropdown becomes hidden. This is observed automation/transition behavior, not established evidence of a human pointer defect. Tests now wait for the named panel's actual `overflow-y: visible` endpoint before interacting inside it; no sleep, forced click or longer timeout is used.

In a digest-pinned official Playwright 1.64.1 Ubuntu Noble/amd64 container, five pointer journeys and six light/dark accessibility journeys then passed with retries disabled. A passing trace confirms the panel was unclipped before input scrolling and actual pointer dispatch selected both formats. The container uses Node 26, while final CI uses pinned Node 24, so this does not replace branch CI. A separately observed intermittent Chromium Enter-key selection failure remains under investigation before final acceptance.
