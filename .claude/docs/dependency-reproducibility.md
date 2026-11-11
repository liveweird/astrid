# Dependency reproducibility

Gradle and npm builds are reproducible and fail closed. Two Gradle controls work together, npm has its own lockfile discipline, and every
runtime image and CI action is pinned. Do not weaken any of them to make a build pass.

## Gradle: locking + verification

- **Locking** (`allprojects { dependencyLocking { lockAllConfigurations(); lockMode = STRICT } }` in the root `build.gradle.kts`): every resolvable
  configuration of `engine` and `server` is pinned in `engine/gradle.lockfile` and `server/gradle.lockfile`. Buildscript classpaths are locked
  separately (`buildscript-gradle.lockfile`, `engine/…`, `server/…`; the root `buildscript { }` and `allprojects { buildscript { } }` blocks activate
  it), and the imported Ktor version catalog (`io.ktor:ktor-version-catalog` in `settings.gradle.kts`) is pinned in `settings-gradle.lockfile`.
  The root project resolves no libraries, so there is **no root `gradle.lockfile`** and no `settingsPluginAudit` (Astrid applies no settings plugin).
  STRICT mode fails on a missing lock state, an extra module or a changed version. The root buildscript carries one advisory constraint
  (`org.freemarker:freemarker:2.3.35`, Kover's reporting dependency); `server` also constrains `scram-client`, `scram-common` and `guava`.
- **Verification** (`gradle/verification-metadata.xml`, `verify-metadata` on, `verify-signatures` off): SHA-256 of every resolved artifact and its POM/module
  metadata. Gradle enables it automatically. Never use `--dependency-verification lenient|off`, add trusted-artifact patterns or exclusions. A missing or
  mismatching checksum is a failure to investigate. The wrapper has its own `distributionSha256Sum`; the JDK comes from `mise.toml` locally and pinned
  Temurin images in CI/Docker (neither is covered by Gradle verification).
- Routine builds enforce both without flags. CI additionally passes `--dependency-verification strict` and fails if `git diff` shows the verification file or any
  `*gradle.lockfile` changed after the build.
- GitHub Actions and container images are outside this mechanism: SHA/digest-pinned and bumped by Dependabot (`ci.md`).

## Regenerating locks (an intentional dependency change)

```sh
cd /Users/sebastian/Sources/astrid
./gradlew :buildEnvironment :engine:buildEnvironment :server:buildEnvironment \
  :dependencies :engine:dependencies :server:dependencies --write-locks
```

Review the diff; never hand-edit a lockfile. Adding a subproject, plugin or task with its own classpath means adding its resolving task here and in the
metadata command below, or its artifacts silently never get a checksum.

## Regenerating verification metadata — only from an empty `GRADLE_USER_HOME`

A warm Gradle home already caches some artifacts, and Gradle records checksums only for what it fetches while `--write-verification-metadata` runs, so a warm
run produces a file that passes locally and then fails the first clean checkout or Docker build. Always start from a fresh directory:

```sh
cd /Users/sebastian/Sources/astrid
export JAVA_HOME=$(mise where java)
export DOCKER_HOST=unix://$HOME/.orbstack/run/docker.sock   # `check` runs the Testcontainers tests
GRADLE_USER_HOME=$(mktemp -d) ./gradlew --write-verification-metadata sha256 \
  :buildEnvironment :engine:buildEnvironment :server:buildEnvironment \
  :dependencies :engine:dependencies :server:dependencies \
  check :engine:exportPilotReview :server:installDist --no-daemon
```

The report tasks resolve every configuration; `check`, `exportPilotReview` and `installDist` then execute what CI and the Dockerfile execute (detekt, Kover, test
runtime, distribution). Validated on this repository: from an empty home this reproduces the committed `verification-metadata.xml` and all six lockfiles byte for byte.
Prove a result from a second empty home before committing:

```sh
GRADLE_USER_HOME=$(mktemp -d) ./gradlew --dependency-verification strict check :server:installDist --no-daemon
```

A clean strict pass is the proof of completeness, not the generation run (it can reuse warm `build/` state). If a round moves a Gradle/Kotlin/Kover plugin, the wrapper cannot run once
the catalog moves ahead of the metadata: bump the wrapper first with the old catalog (taking `distributionSha256Sum` from the published `.sha256`), then the catalog, then regenerate.

## Reviewing a change

1. Lockfile additions/removals/versions match the request. 2. Checksum entries are limited to the expected artifacts. 3. New checksums are established from an independent
publisher source where possible: generation records what Gradle fetched, not that the repository was honest. 4. Keep old entries only while their coordinates are supported; prune in
a focused change. **A changed checksum for an unchanged coordinate is a possible integrity incident**, never routine maintenance: stop and verify with the publisher.

## npm (`web/`, `e2e/`)

- `package.json` pins **exact** versions (no ranges); `package-lock.json` is committed and installs use `npm ci`. Both workspaces are audited in CI
  (`npm audit --audit-level=high`).
- `web` installs with **`--legacy-peer-deps`** (Dockerfile, CI, README): `openapi-typescript@7.13.0` declares a peer `typescript@^5` while the toolchain is TypeScript 6.0.3;
  the generated types are compatible, and type checks plus the `check:api` drift gate remain the real proof. If you re-install from scratch, use the same flag. `e2e/` needs none.
  `overrides` pins `react-is` to the React version.
- `knip` `ignoreDependencies: ["ajv"]` in `web/knip.json` is carried over from Flow, where `ajv` is pinned only for `@stoplight/spectral-cli`'s resolution.
- Node is pinned in `mise.toml` and `.nvmrc` (`check-toolchain.py`); the Dockerfile and CI use the same version.

## CI browser image

`scripts/run-browser-tests.sh` pins the Playwright image by digest and `check-toolchain.py`
requires its package version to match the e2e manifest/lockfile. Update those pins together.
The image's bundled Node does not establish the project runtime: setup-node's exact Linux
executable is mounted read-only and checked in the container and inherited Node workers.
No npm, Gradle, application-image or browser test policy is relaxed by this execution change.
