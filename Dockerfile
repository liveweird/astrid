# syntax=docker/dockerfile:1
FROM node:24.22.1-alpine@sha256:06d7adf3e0ef61a13f1d28682726366f97a15aa1888c934d3ecec86c33918b53 AS web
RUN apk add --no-cache git
WORKDIR /web
COPY web/package.json web/package-lock.json ./
# openapi-typescript currently declares TypeScript 5; Flow and Astrid use TS 6.
RUN npm ci --legacy-peer-deps
COPY web/ ./
# Flow's build stamp reads Git only in this builder stage. The partial web worktree
# cannot supply a meaningful dirty check, so pass its identity explicitly to Vite.
COPY .git .git
ARG GIT_SHA
ARG GIT_COMMIT_TIME
RUN GIT_SHA="${GIT_SHA:-$(git rev-parse --short HEAD)}" \
    GIT_COMMIT_TIME="${GIT_COMMIT_TIME:-$(git log -1 --format=%cI)}" \
    npm run build

FROM eclipse-temurin:21.0.13_9-jdk-noble@sha256:114cabb73033ca4ce7ceec3deb62396bc1b716282bae051ed3c1d82e38fcab2d AS server
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties *gradle.lockfile ./
COPY gradle/ gradle/
COPY config/ config/
COPY engine/ engine/
COPY server/ server/
RUN ./gradlew :server:installDist --no-daemon --dependency-verification strict

FROM eclipse-temurin:21.0.13_9-jre-noble@sha256:0eda5830ed7b669abc246a8dc520ea8a23cab06f9a8d4b6964109e2f2394e46f AS runtime
# APT can warn yet return success when an index fetch fails. Fail on any such error,
# avoid hiding DNS stalls behind bootstrap retries, and retain signature/TLS verification.
ARG ASTRID_UBUNTU_MIRROR=""
RUN set -eu; \
    if [ -n "$ASTRID_UBUNTU_MIRROR" ]; then \
      [ "$ASTRID_UBUNTU_MIRROR" = "https://mirrors.edge.kernel.org/ubuntu/" ]; \
      [ "$(dpkg --print-architecture)" = amd64 ]; \
      sources=/etc/apt/sources.list.d/ubuntu.sources; \
      [ -f "$sources" ]; \
      expected_sources="$(printf '%s\n' \
        'Types: deb' \
        'URIs: http://archive.ubuntu.com/ubuntu/' \
        'Suites: noble noble-updates noble-backports' \
        'Components: main universe restricted multiverse' \
        'Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg' \
        'Types: deb' \
        'URIs: http://security.ubuntu.com/ubuntu/' \
        'Suites: noble-security' \
        'Components: main universe restricted multiverse' \
        'Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg')"; \
      active_sources="$(sed -e '/^[[:space:]]*#/d' -e '/^[[:space:]]*$/d' "$sources")"; \
      [ "$active_sources" = "$expected_sources" ]; \
      sed -i \
        -e "s|^URIs: http://archive.ubuntu.com/ubuntu/\$|URIs: $ASTRID_UBUNTU_MIRROR|" \
        -e "s|^URIs: http://security.ubuntu.com/ubuntu/\$|URIs: $ASTRID_UBUNTU_MIRROR|" \
        "$sources"; \
      expected_sources="$(printf '%s\n' \
        'Types: deb' \
        "URIs: $ASTRID_UBUNTU_MIRROR" \
        'Suites: noble noble-updates noble-backports' \
        'Components: main universe restricted multiverse' \
        'Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg' \
        'Types: deb' \
        "URIs: $ASTRID_UBUNTU_MIRROR" \
        'Suites: noble-security' \
        'Components: main universe restricted multiverse' \
        'Signed-By: /usr/share/keyrings/ubuntu-archive-keyring.gpg')"; \
      active_sources="$(sed -e '/^[[:space:]]*#/d' -e '/^[[:space:]]*$/d' "$sources")"; \
      [ "$active_sources" = "$expected_sources" ]; \
    fi; \
    apt-get -o Acquire::Retries=0 -o APT::Update::Error-Mode=any update \
    && apt-get -o Acquire::Retries=0 upgrade -y \
    && apt-get -o Acquire::Retries=0 install -y --no-install-recommends curl \
    && dpkg --compare-versions "$(dpkg-query -W -f='${Version}' apt)" ge 2.8.4ubuntu0.1 \
    && dpkg --compare-versions "$(dpkg-query -W -f='${Version}' libapt-pkg6.0t64)" ge 2.8.4ubuntu0.1 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 astrid \
    && useradd --system --uid 10001 --gid astrid --home-dir /app --shell /usr/sbin/nologin astrid
WORKDIR /app
# A distribution preserves Flyway's ServiceLoader descriptors in separate jars.
COPY --from=server --chown=10001:10001 /src/server/build/install/server/ ./
COPY --from=web --chown=10001:10001 /web/dist/ web/
ENV WEB_STATIC_DIR=/app/web ASTRID_DEVELOPMENT_MODE=false ASTRID_BIND_HOST=0.0.0.0
USER 10001
EXPOSE 8088
HEALTHCHECK --interval=15s --timeout=3s --start-period=240s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8088/api/v1/ready >/dev/null || exit 1
ENTRYPOINT ["/app/bin/server"]
