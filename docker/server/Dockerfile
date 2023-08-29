#
# conductor:server - Combined Netflix conductor server & UI
#
# ===========================================================================================================
# 0. Builder stage
# ===========================================================================================================
FROM openjdk:11-jdk AS builder
LABEL maintainer="Netflix OSS <conductor@netflix.com>"

# Copy the project onto the builder image
COPY . /conductor

# Build the server
WORKDIR /conductor
RUN ./gradlew build -x test

# ===========================================================================================================
# 1. Bin stage
# ===========================================================================================================

FROM nginx:alpine
RUN apk add openjdk11-jre

LABEL maintainer="Netflix OSS <conductor@netflix.com>"

# Make app folders
RUN mkdir -p /app/config /app/logs /app/libs

# Copy the compiled output to new image
COPY --from=builder /conductor/docker/server/bin /app
COPY --from=builder /conductor/docker/server/config /app/config
COPY --from=builder /conductor/community-server/build/libs/conductor-community-server-*-boot.jar /app/libs

# Copy the files for the server into the app folders
RUN chmod +x /app/startup.sh

HEALTHCHECK --interval=60s --timeout=30s --retries=10 CMD curl -I -XGET http://localhost:8080/health || exit 1

CMD [ "/app/startup.sh" ]
ENTRYPOINT [ "/bin/sh"]
