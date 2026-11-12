# Application Versioning and Changelog

## Authorized scope and provenance

**7 November 2026 — User-authorized:** Port Flow's app versioning functionality, including its changelog. Reuse the inspected donor `4e255e966804c5750c8fe0f1928a1461c17cc96f` while keeping Flow read-only. Adapt its version stamp, lazy Markdown changelog, device-local seen state, build metadata and consistency checks to Astrid's existing shell. The existing MIT notice covers the adapted components.

## Version and release-content conventions

Implementation default: `web/src/changelog/version.ts` holds the user-facing `APP_VERSION`; the newest authored entry in `entries.ts` must match it, as must OpenAPI `info.version`. Start this feature at `0.2.0` and record the already delivered private engine as the `0.1.0` baseline, both dated 7 November 2026. This is a newly established application history, not a claim that earlier tagged releases existed. Gradle/npm package versions, `/api/v1` and content/generator/evaluator revision IDs remain separate.

Adapt Flow's release convention: each user-visible feature gets a minor bump and each user-visible fix a patch bump, with its own newest-first entry. Documentation-, test-, CI- and dependency-only changes may retain the app version. Keep published version entries immutable; author notes about user behavior rather than generating them from commit subjects. Use unique semantic versions and valid descending ISO dates. Astrid currently serves English-speaking learners, so release bodies are English; Flow's Polish UI/localization framework is not needed.

## Build identity and presentation

Display `v<version> · <commit> · <commit time>` in the expanded navigation footer; the rail shows the version with full detail available on demand. Link the stamp and a Changelog navigation item to `/changelog`. Load Markdown, entries and the page lazily so ordinary practice does not pull release content into its initial bundle or create a learning session when opening the changelog directly. Preserve the shared shell, red accent, responsive layout, keyboard semantics and both themes. Port Flow's route-error-boundary pattern around the lazy page: a rejected chunk must leave the shell/navigation available and offer a full reload, which resets the cached lazy-import failure.

Vite accepts `GIT_SHA` and `GIT_COMMIT_TIME` overrides and otherwise reads Git locally, marking uncommitted local work with `+dirty`. Docker follows Flow's builder-only Git metadata pattern, computing identity explicitly to avoid treating its partial worktree as dirty. Build from a regular clone containing its `.git` directory; archive-only and linked-worktree contexts need a separately prepared Git context. Keep Git and tooling out of the runtime image. The timestamp is Git's committer-local ISO value, displayed to minutes without timezone reinterpretation. CI and packaged browser checks must prove real metadata rather than silently accepting an unknown production stamp.

Store `{seenVersion}` under `astrid.changelog`. Use guarded storage and a reactive external store, so reading the current changelog clears its indicator immediately; corrupt or unavailable storage must not prevent use of the app. This device state is separate from learning evidence and recovery records.

## WebKit preload compatibility decision

Packaged testing reproduced a failed lazy script remaining unavailable after a full reload in the installed WebKit browser. A loopback proxy serving a genuine one-time HTTP 503 reproduced the behavior without browser interception; the script was requested only once. Omitting only the changelog entry's own `modulepreload` produced two requests and successful recovery. This observed result matches [WebKit's reported failed-preload cache defect](https://bugs.webkit.org/show_bug.cgi?id=270357); it does not establish which shipping browsers contain an upstream fix.

Use Vite's supported `build.modulePreload.resolveDependencies` callback to omit the dynamically imported `Changelog-*` entry itself for JavaScript hosts. Retain native lazy import, all other dependencies, CSS and initial HTML preloads. Keep the existing error boundary and full-document Reload action. This avoids alternate bundles, retry URL flags or new storage state. The emitted chunk name is part of this narrow workaround; packaged interrupted-download and HTTP-error recovery checks must guard it when the route or bundler changes.

## Publishing and verification

A version bump is distinct from publishing: this feature does not create an annotated Git tag or GitHub Release automatically. When the user requests publication, follow Flow's explicit-commit sequence: verify the exact master commit and CI, create an immutable annotated `v<version>` tag at that SHA, publish reviewed notes from that tagged entry with `--verify-tag`, then read back the remote tag and release state. Deployment remains a separate verified operation.

Acceptance covers version/entry/OpenAPI consistency, Markdown rendering, stamp identity/time, compact navigation, unseen/read behavior and storage failures, lazy loading, direct-route reload, both themes, enlarged/narrow reflow and unchanged learning/recovery. Preserve all saved data and published exercise revisions, independently review the port, verify the assembled image and deliver focused commits merged/pushed under the standing authorization. BL-040 tracks this work.

## Observed delivery — 7 November 2026

Source `8d1e1c3` passed independent build/API/web and rendered review and all five [source CI jobs](https://github.com/liveweird/astrid/actions/runs/42395355408). All 148 Linux browser journeys passed without retries or flaky results. Local verification passed 85 engine tests plus the fresh Arabic-locale suite, 55 server tests, 124 frontend tests with unchanged coverage floors, lint, API drift, strict dependencies, retained review exports and deployment checks. All 148 native Chromium/WebKit journeys passed without retries; the exact packaged image passed the repository vulnerability policy.

Review added the lazy-route error boundary and guarded shell storage access. Real packaged testing then exposed the WebKit preload issue documented above. The corrected image recovered from a genuine one-shot HTTP 503 without browser interception or JavaScript rewriting: two requests, one failure, then successful notes rendering. Both browser engines also passed interrupted-download and HTTP-error recovery, corruption and origin-wide storage denial, immediate cross-tab seen state, compact navigation, real commit/time identity, no learning requests on direct notes, and exact uncertain-practice recovery after visiting notes. Eight rendered light/dark desktop/rail/320px-at-200% states were independently inspected without actionable findings.

The preview now serves `0.2.0`, source `8d1e1c3`, from immutable image `sha256:1ba06d35bf435c112a882fb4b16aaa5e0553334bdb24df5cf3466f8252371bb7`. Runtime inspection found neither Git metadata nor a Git executable. The initial changelog contains the versioning feature and a clearly described `0.1.0` private-engine baseline; no Git tag or GitHub Release was published.

Before promotion, a 238,598-byte mode-0600 PostgreSQL backup was captured and verified readable. The upgrade preserved immutable evidence for 1,585 exercises, 529 attempts, 79 practice sessions, 572 items and 314 attempt contexts, the same database process/mounts, schema versions 1/2/3 and all three evaluators. An isolated original-practice response remained byte-identical. All 24 live Chromium/WebKit smoke journeys passed without retries. Ownership-guarded cleanup removed only the temporary QA containers, volume and network, leaving the main processes/mounts unchanged.

Private build hashes, screenshots, HTTP probes, archive and verification logs remain ignored under `.local/app-versioning/`. This completion record accompanies the authorized focused merge/push. BL-040 is complete; app versioning is separate from new curriculum and broader course work.


**Subsequent release:** BL-041 advances the private preview to `0.3.0` for topic-based practice. Its [delivery record](topic-practice.md#observed-delivery--7-november-2026) supersedes the running-version statement in the historical `0.2.0` evidence above. The published `0.1.0` and `0.2.0` changelog entries are unchanged.


**Subsequent release:** BL-043 advances the private preview to `0.4.0` for standalone sentence writing and short reading comprehension. Its [delivery record](writing-reading-contract.md#observed-delivery--7-november-2026) supersedes the running-version statement for `0.3.0`; published `0.1.0`–`0.3.0` release notes remain unchanged. Learning revisions advance separately to pair four while existing guided plans retain pair three.

## First tag and GitHub Release — 8 November 2026

The user's explicit instruction to version and release BL-047 properly authorized the first annotated tag and [GitHub Release, v0.5.0](https://github.com/liveweird/astrid/releases/tag/v0.5.0). Tag source `b8ff9f9` matched verified master, all five [CI jobs](https://github.com/liveweird/astrid/actions/runs/42523608309) and 250 Linux browser journeys passed without retries/flakies, and remote annotated-tag/source and authored notes were read back. Earlier application entries remain unchanged changelog history; no retroactive tags were invented. App-only private-preview promotion followed a readable backup and preserved history. The [delivery record](word-choice-tiles-contract.md#observed-delivery--8-november-2026) supersedes the earlier running-version statements; learning pair five remains distinct from app 0.5.0.


**Subsequent release — 8 November 2026:** app [0.6.0](https://github.com/liveweird/astrid/releases/tag/v0.6.0), source `7bd0bf2`, is the
next annotated tag/GitHub Release and the current preview. All five [exact-master CI jobs](https://github.com/liveweird/astrid/actions/runs/42530713758),
266 Linux and 266 native browser journeys passed without retries/flakies; a readable backup
preceded app-only promotion. The [matching delivery record](vocabulary-matching-contract.md#observed-delivery--8-november-2026)
supersedes the earlier running-version statement. Published older entries and tags remain unchanged;
learning pair six is separate from app 0.6.0 and guided plans remain at pair three.

**Subsequent release — 9 November 2026:** app [0.7.0](https://github.com/liveweird/astrid/releases/tag/v0.7.0), source `e56ac87`, is the current private
preview. All five [exact-master CI jobs](https://github.com/liveweird/astrid/actions/runs/42537494010), 284 Linux and 284 native browser journeys
passed without retries/flakies. The [multiple-gap delivery record](multiple-gap-contract.md#observed-delivery--9-november-2026) records the actual
isolated backup restore, app-only promotion, 74 live smoke checks and owned cleanup.
Published older entries/tags stay unchanged; learning pair seven and app 0.7.0 are distinct,
and guided plans retain pair three.

**Subsequent release — 9 November 2026:** app [0.8.0](https://github.com/liveweird/astrid/releases/tag/v0.8.0), source `cdb6e06`, is the current private
preview after all five [exact-master CI jobs](https://github.com/liveweird/astrid/actions/runs/42698896226), 312 Linux and 312 native browser journeys
passed without retries/flakies. The [guided-gap delivery](guided-gap-practice.md#observed-delivery--9-november-2026)
records independent reviews, actual restart, isolated backup restore, app-only promotion,
102 live checks and owned cleanup. This supersedes the historical running-version statement
for 0.7.0. Published notes/tags remain immutable; pair seven stays unchanged, new practice plans
pin it explicitly, and old plans/reviews retain pair three.


**Subsequent release — 10 November 2026:** app [0.9.0](https://github.com/liveweird/astrid/releases/tag/v0.9.0),
source `8913dea`, is the current private preview after all five [exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/42722013690),
370 native and 370 Linux browser journeys passed without retries/flakies. The
[first-step delivery record](module-one-activation.md#observed-runtime-delivery--10-november-2026) records independent reviews, exact restart/replay,
actual isolated backup restore, app-only promotion, 160 live checks and owned cleanup.
This supersedes the historical running-version statement for 0.8.0. App version 0.9.0 and
learning pair eight remain distinct; old notes, tags, evaluators and plans are retained.


**Subsequent release — 10 November 2026:** app [0.10.0](https://github.com/liveweird/astrid/releases/tag/v0.10.0), source `3d87b4d`, is the current private preview after all five [exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/42898557511), 388 native and 388 Linux journeys without retries/flakies, actual restart/replay, isolated backup restore and app-only promotion. All 176 live checks and owned cleanup passed. The [S04 delivery](inanimate-reference-activation.md#observed-runtime-delivery--10-november-2026) supersedes earlier running-version statements. Pair nine and the four-item S04 plan are additive; older notes, tags, evaluators and plans remain retained.

**Subsequent release — 10 November 2026:** App 0.11.0, source `8934fb1`, adds named present forms and subject-first assembly. All five exact-source CI jobs and 392 journeys in each of the native and Linux browser suites passed. The private preview passed 106 checks after backup restore verification and app-only promotion. See the [delivery record](module-one-course-slice.md#observed-delivery-for-s05-and-s06) and [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.11.0).

**Subsequent release — 11 November 2026:** App 0.12.0, source `369308a`, adds the four
guided possession activities with `har`. All five exact-source CI jobs passed, with 394
journeys in each of the native and Linux browser suites. The private preview passed
108 checks after backup restore verification and app-only promotion.
[Delivery record](module-one-course-slice.md#observed-s07-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.12.0)

**Subsequent release — 11 November 2026:** App 0.13.0, source `b82ad36`, adds the
reading-material vocabulary bridge. Five exact-source CI jobs and 396 journeys in each
browser suite passed. After backup restoration was verified, app-only promotion preserved
history and the preview passed 110 checks.
[Delivery record](module-one-course-slice.md#observed-s08-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.13.0)

**Subsequent release — 11 November 2026:** App 0.14.0, source `f0c9568`, adds the
personal-statement present-form session. Five exact-source CI jobs and 398 journeys in
each browser suite passed. A verified backup restore preceded app-only promotion, and
the preview passed 112 checks.
[Delivery record](module-one-course-slice.md#observed-s09-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.14.0)

**Subsequent release — 11 November 2026:** App 0.15.0, source `43e9726`, adds
bare-infinitive selection and typing after ability `kan`. Five exact-source CI jobs
and 400 journeys in each browser suite passed. A verified backup restore preceded
app-only promotion, and the preview passed 114 checks.
[Delivery record](module-one-course-slice.md#observed-s10-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.15.0)

**Subsequent release — 11 November 2026:** App 0.16.0, source `9dd787c`, is the current
private preview and adds three short contextual modality rounds. Five exact-source CI jobs and 406
CI journeys passed; the unchanged image also passed 410 packaged-Linux
journeys with the reviewed harness, all without retries or flakies. A verified backup
restore preceded app-only promotion, and the preview passed 120 packaged-Linux checks.
[Delivery record](module-one-course-slice.md#observed-s11-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.16.0)

**Subsequent release — 11 November 2026:** App 0.17.0, source `0f1b322`, is the current
private preview and adds four final time/place activities. Five exact-source CI jobs,
420 Linux and 420 native browser journeys passed without
retries/flakies. Verified backup restoration preceded app-only promotion; all
128 live-preview checks and owned QA cleanup passed.
[Delivery record](module-one-course-slice.md#observed-s12-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.17.0)

**Subsequent release — 11 November 2026:** App 0.18.0, source `9bb10ae`, is the current
private preview and adds four neutral yes/no question activities. Five exact-source CI
jobs, 430 Linux and 430 native browser journeys passed
without retries/flakies. Verified backup restoration preceded app-only promotion;
all 136 live-preview checks and owned QA cleanup passed.
[Delivery record](module-one-course-slice.md#observed-s13-delivery) · [GitHub release](https://github.com/liveweird/astrid/releases/tag/v0.18.0)

**Subsequent repair release — 12 November 2026:** App [0.20.1](https://github.com/liveweird/astrid/releases/tag/v0.20.1), source
`9eefcfe`, repairs matching regeneration across JVM restarts without adding content.
All five [exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/43148241073), 482 native and 482 packaged-Linux journeys,
verified legacy/current restart replay, isolated backup restore, app-only promotion, 186
main-preview checks and owned cleanup passed. The [repair record](matching-restart-compatibility.md#observed-delivery)
supersedes the 0.20.0 running-version statement while preserving its historical evidence,
published notes and pair-seventeen plans.

**Subsequent release — 12 November 2026:** App [0.21.0](https://github.com/liveweird/astrid/releases/tag/v0.21.0), source
`eab86c6`, adds cumulative mixed review across introduced lessons. All five
[exact-source CI jobs](https://github.com/liveweird/astrid/actions/runs/43307108048), 492 native and 492 packaged-Linux journeys,
actual restart/replay, isolated backup restore, app-only promotion and 196 live checks
passed without retries/flakies. [Delivery evidence](cumulative-review.md#observed-delivery)
records preservation and owned cleanup. Existing graders remain pinned per exercise;
`per-item` is a composite-plan marker, not a new engine revision. Prior tags, notes,
lesson plans and learner history remain unchanged.
