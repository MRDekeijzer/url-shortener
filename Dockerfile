FROM eclipse-temurin:21-jre-jammy AS artifact

ARG JAR_URL

RUN test -n "$JAR_URL" || (echo "JAR_URL build argument is required" && exit 1)

RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN mkdir -p /tmp/dist && \
    curl -fsSL "$JAR_URL" -o /tmp/dist/minurl.jar

FROM eclipse-temurin:21-jre-jammy

ENV LOG_DIR=/app/logs
WORKDIR /app

COPY --from=artifact /tmp/dist/minurl.jar /app/minurl.jar

RUN mkdir -p "${LOG_DIR}"

EXPOSE 7000

ENTRYPOINT ["java", "-jar", "/app/minurl.jar"]
