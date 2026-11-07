# Application Versioning and Changelog

## Authorized scope and provenance

**7 November 2026 — User-authorized:** Port Flow's app versioning functionality, including its changelog. Reuse the inspected donor `4e255e966804c5750c8fe0f1928a1461c17cc96f` while keeping Flow read-only. Adapt its version stamp, lazy Markdown changelog, device-local seen state, build metadata and consistency checks to Astrid's existing shell. The existing MIT notice covers the adapted components.

## Version and release-content conventions

Implementation default: `web/src/changelog/version.ts` holds the user-facing `APP_VERSION`; the newest authored entry in `entries.ts` must match it, as must OpenAPI `info.version`. Start this feature at `0.2.0` and record the already delivered private engine as the `0.1.0` baseline, both dated 7 November 2026. This is a newly established application history, not a claim that earlier tagged releases existed. Gradle/npm package versions, `/api/v1` and content/generator/evaluator revision IDs remain separate.

Adapt Flow's release convention: each user-visible feature gets a minor bump and each user-visible fix a patch bump, with its own newest-first entry. Documentation-, test-, CI- and dependency-only changes may retain the app version. Keep published version entries immutable; author notes about user behavior rather than generating them from commit subjects. Use unique semantic versions and valid descending ISO dates. Astrid currently serves English-speaking learners, so release bodies are English; Flow's Polish UI/localization framework is not needed.

## Build identity and presentation

Display `v<version> · <commit> · <commit time>` in the expanded navigation footer; the rail shows the version with full detail available on demand. Link the stamp and a Changelog navigation item to `/changelog`. Load Markdown, entries and the page lazily so ordinary practice does not pull release content into its initial bundle or create a learning session when opening the changelog directly. Preserve the shared shell, red accent, responsive layout, keyboard semantics and both themes.

Vite accepts `GIT_SHA` and `GIT_COMMIT_TIME` overrides and otherwise reads Git locally, marking uncommitted local work with `+dirty`. Docker follows Flow's builder-only Git metadata pattern, computing identity explicitly to avoid treating its partial worktree as dirty. Keep Git and tooling out of the runtime image. The timestamp is Git's committer-local ISO value, displayed to minutes without timezone reinterpretation. CI and packaged browser checks must prove real metadata rather than silently accepting an unknown production stamp.

Store `{seenVersion}` under `astrid.changelog`. Use guarded storage and a reactive external store, so reading the current changelog clears its indicator immediately; corrupt or unavailable storage must not prevent use of the app. This device state is separate from learning evidence and recovery records.

## Publishing and verification

A version bump is distinct from publishing: this feature does not create an annotated Git tag or GitHub Release automatically. When the user requests publication, follow Flow's explicit-commit sequence: verify the exact master commit and CI, create an immutable annotated `v<version>` tag at that SHA, publish reviewed notes from that tagged entry with `--verify-tag`, then read back the remote tag and release state. Deployment remains a separate verified operation.

Acceptance covers version/entry/OpenAPI consistency, Markdown rendering, stamp identity/time, compact navigation, unseen/read behavior and storage failures, lazy loading, direct-route reload, both themes, enlarged/narrow reflow and unchanged learning/recovery. Preserve all saved data and published exercise revisions, independently review the port, verify the assembled image and deliver focused commits merged/pushed under the standing authorization. BL-040 tracks this work.
