# Static Response Compression

## Observed gap

**7 November 2026 — Delivered performance follow-up.** Before this change the deployed build contained six JavaScript/CSS files totaling **769,299 bytes**. Requests advertising `gzip` received those same unencoded bytes. This is the generated asset set, excluding fonts and HTTP framing, rather than a measured page-load time or production performance target.

Flow already installs Ktor's Compression plugin. Reuse the same pinned 3.6.0 module; the existing verification metadata covers it, and strict resolution added only the two compression-module lock entries. See the [reuse boundary](flow-reuse-manifest.md).

## Working implementation choice

Use Ktor's gzip response encoder for JavaScript/CSS under `/assets/` at a minimum size of 1,024 bytes. Keep font/image files, HTML, API data and problem responses outside that policy. Request decompression is not enabled. Preserve the existing static `no-cache`, API `no-store`, content types and security guards.

Ktor supports encoder conditions, media-type and size filters. Its explicit identity encoder emits `Content-Encoding: identity`; HTTP semantics discourages that header value. A small condition using Ktor's header parser therefore suppresses gzip when identity is explicitly preferred, while the plugin performs encoding/ordinary negotiation. This is a narrow compatibility condition, not a replacement HTTP parser. Eligible identity and gzip representations both vary by `Accept-Encoding`. Sources: [Ktor compression](https://ktor.io/docs/server-compression.html), [HTTP content encoding](https://www.rfc-editor.org/rfc/rfc9110.html#section-8.4).

## Acceptance

- Verify actual gzip bytes decode to the original JavaScript/CSS, not merely that a response header exists.
- Check ordinary gzip, absent encoding, explicit identity preference, forbidden gzip, gzip quality below one without explicit identity, and the size threshold.
- Verify MIME, cache and security headers, plus unchanged HTML/API/font/image/error handling.
- Against the packaged app, compare every built JavaScript/CSS file with identity and decoded-gzip responses; record actual transfer-byte counts separately from latency claims.
- Run the existing browser journeys in Chromium and WebKit, independent code review and strict build/CI gates before merging or promoting the follow-up image.

## Observed verification

The full strict JVM gate passed, including all 33 server tests, PostgreSQL integration, detekt, coverage verification and distribution packaging. Four new wire regressions passed, including rejected gzip request bodies with no session cookie issued. Independent code review found no remaining actionable issue.

The packaged QA app delivered the same six JavaScript/CSS bodies in **196,394 bytes** when gzip was accepted, compared with **769,299 bytes** for identity: about **74% less transfer data**. Decoded bodies matched every built file byte for byte; ordinary quality/identity handling, MIME, cache/security headers and excluded resources passed the real Netty probe. All 38 native Chromium/WebKit journeys passed, and the actual local image passed the pinned HIGH/CRITICAL scan under the CI `--ignore-unfixed` policy. All 38 Linux-container journeys also passed without retries. All five [branch CI jobs](https://github.com/liveweird/astrid/actions/runs/42343476526) passed at `e5fbcdf`, including 38 pinned-Node-24 browser journeys without retries. The follow-up was fast-forwarded and pushed to master. The final image serves the private preview, where the same byte probe passed again and both browsers completed all three formats. PostgreSQL retained its original container/process/mounts.

Re-check the narrow identity compatibility condition when upgrading Ktor. The engine's published content, generation and grading revisions remain unchanged. BL-033 records delivery status.
