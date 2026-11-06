# syntax=docker/dockerfile:1
FROM node:24.22.1-alpine@sha256:06d7adf3e0ef61a13f1d28682726366f97a15aa1888c934d3ecec86c33918b53 AS web
WORKDIR /web
COPY web/package.json web/package-lock.json ./
# openapi-typescript currently declares TypeScript 5; Flow and Astrid use TS 6.
RUN npm ci --legacy-peer-deps
COPY web/ ./
RUN npm run build

FROM eclipse-temurin:21.0.12.1_1-jdk-noble@sha256:9bd188045f522a296ad4fbce642b85524da59ac7c1f2c9a17ea9785b00d5a433 AS server
WORKDIR /src
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties *gradle.lockfile ./
COPY gradle/ gradle/
COPY config/ config/
COPY engine/ engine/
COPY server/ server/
RUN ./gradlew :server:installDist --no-daemon --dependency-verification strict

FROM eclipse-temurin:21.0.12.1_1-jre-noble@sha256:435763596c3be2b929e8bb4282520849804715c3483cfffaa323a7433f2caf71 AS runtime
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
HEALTHCHECK --interval=15s --timeout=3s --start-period=45s --retries=3 \
    CMD curl -fsS http://127.0.0.1:8088/api/v1/ready >/dev/null || exit 1
ENTRYPOINT ["/app/bin/server"]
