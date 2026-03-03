# syntax=docker/dockerfile:1

# Stage 1: Build
FROM docker.io/gradle:9-jdk21-alpine AS builder

ARG GRADLE_USER_HOME=/home/gradle/.gradle
ARG APP_HOME=/workspace
ARG JAR_NAME=app.jar

WORKDIR ${APP_HOME}

ENV GRADLE_USER_HOME=${GRADLE_USER_HOME} \
    ORG_GRADLE_PROJECT_buildCacheEnabled=true

COPY gradle gradle
COPY gradlew gradlew
COPY settings.gradle.kts build.gradle.kts gradle.properties ./

RUN gradle wrapper

RUN --mount=type=cache,target=${GRADLE_USER_HOME} \
    ./gradlew --no-daemon --no-parallel dependencies || true

COPY src src

RUN --mount=type=cache,target=${GRADLE_USER_HOME} \
    ./gradlew --no-daemon build \
      -x test \
      -x detekt \
      -x ktlintCheck \
      -x ktlintKotlinScriptCheck \
      -x ktlintMainSourceSetCheck \
      -x ktlintTestSourceSetCheck

RUN mkdir -p ${APP_HOME}/dist \
    && cp ${APP_HOME}/build/libs/*.jar ${APP_HOME}/dist/${JAR_NAME} \
    && ls -la ${APP_HOME}/dist


# Stage 2: Runtime
FROM docker.io/eclipse-temurin:21-jre-jammy AS runtime

LABEL org.opencontainers.image.title="MovieNight" \
      org.opencontainers.image.description="MovieNight application" \
      org.opencontainers.image.authors="MovieNight team"

WORKDIR /app

RUN groupadd -g 1000 app \
    && useradd -u 1000 -g app -s /usr/sbin/nologin -M app

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder --chown=app:app /workspace/dist/app.jar /app/app.jar

USER app

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC" \
    SERVER_PORT=8080 \
    PATH="/app:$PATH"

EXPOSE ${SERVER_PORT}

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -fsS http://127.0.0.1:${SERVER_PORT}/actuator/health || exit 1

STOPSIGNAL SIGTERM

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -Dserver.port=${SERVER_PORT} -jar /app/app.jar"]
