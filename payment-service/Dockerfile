# Build stage — uses eclipse-temurin JDK (stable multi-arch) + Gradle Wrapper
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copy Gradle wrapper + build scripts first (better layer caching)
COPY gradlew ./
COPY gradle/ ./gradle/
COPY settings.gradle build.gradle ./
COPY gradle.properties* ./
RUN chmod +x ./gradlew

# Copy module build files
COPY payment-service/build.gradle ./payment-service/
COPY notification-service/build.gradle ./notification-service/

# Warm dependency cache (non-fatal if offline)
RUN ./gradlew :payment-service:dependencies --no-daemon > /dev/null 2>&1 || true

# Copy remaining sources
COPY payment-service/ ./payment-service/
COPY notification-service/ ./notification-service/

# Build
RUN ./gradlew :payment-service:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN mkdir -p /otel && \
    wget -q -O /otel/opentelemetry-javaagent.jar \
    https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.26.1/opentelemetry-javaagent.jar
COPY --from=build /app/payment-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-javaagent:/otel/opentelemetry-javaagent.jar", "-jar", "/app/app.jar"]
