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

FROM eclipse-temurin:24.0.2_12-jdk-noble@sha256:dacac8e9a0df0d2bd24e702b4431132875c249930b70555ebd7ca285b5bee684 AS server
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties *gradle.lockfile ./
COPY gradle/ gradle/
COPY config/ config/
COPY engine/ engine/
COPY server/ server/
RUN ./gradlew :server:installDist --no-daemon --dependency-verification strict

FROM eclipse-temurin:24.0.2_12-jre-noble@sha256:b416d02335e702b0403ff280de9475a3348e29382285969c9d4e17862ce632e7 AS runtime
RUN apt-get update && apt-get upgrade -y \
    && apt-get install -y --no-install-recommends curl \
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
