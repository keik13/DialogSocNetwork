FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.11.7_3.3.7 AS builder

WORKDIR /build

COPY build.sbt .
COPY .scalafmt.conf .
COPY project ./project
COPY src ./src

RUN sbt assembly

FROM eclipse-temurin:21-jre-noble
ARG TARGET_JAR=DialogSocNetwork-0.1.0-SNAPSHOT.jar
ENV TARGET_JAR=${TARGET_JAR} \
    PROJECT_DIR="/app" \
    TZ=Europe/Moscow

WORKDIR ${PROJECT_DIR}
COPY docker/start.sh ${PROJECT_DIR}
COPY --from=builder /build/target/${TARGET_JAR} ${PROJECT_DIR}/${TARGET_JAR}
RUN chmod -R +x ${PROJECT_DIR} && \
    adduser -u 1001 --disabled-password appuser && \
    chown -R appuser:appuser ${PROJECT_DIR}
USER appuser

ENTRYPOINT ["./start.sh"]