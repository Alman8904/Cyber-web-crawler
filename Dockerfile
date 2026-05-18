# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Copy maven wrapper and pom first (layer-cached until pom changes)
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Download dependencies (cached separately from source)
RUN ./mvnw dependency:go-offline -q

# Copy source and build, skipping tests (run them in CI separately)
COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy the fat JAR from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Active profile is set via env var at runtime (local / dev / prod)
ENV SPRING_PROFILES_ACTIVE=local

ENTRYPOINT ["java", "-jar", "app.jar"]