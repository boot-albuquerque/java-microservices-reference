# Build stage — uses eclipse-temurin JDK (stable multi-arch) + Gradle Wrapper
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY gradlew ./
COPY gradle/ ./gradle/
COPY settings.gradle build.gradle ./
COPY gradle.properties* ./
RUN chmod +x ./gradlew

COPY payment-service/build.gradle ./payment-service/
COPY notification-service/build.gradle ./notification-service/

RUN ./gradlew :notification-service:dependencies --no-daemon > /dev/null 2>&1 || true

COPY payment-service/src/main/avro/ ./payment-service/src/main/avro/
COPY notification-service/ ./notification-service/

RUN ./gradlew :notification-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /otel && \
    wget -q -O /otel/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar
COPY --from=build /app/notification-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-javaagent:/otel/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
