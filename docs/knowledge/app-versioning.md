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
