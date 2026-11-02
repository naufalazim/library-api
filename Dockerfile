# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar ----------
# Multi-arch image: works on Apple Silicon (arm64) and Intel/AMD (amd64)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom.xml first so downloaded dependencies are cached between builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copy the source and build (tests run in CI, not inside the image build)
COPY src src
RUN mvn -B -q package -DskipTests \
 && cp target/*.jar app.jar

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# curl is used by the HEALTHCHECK below
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# Run as a non-root user
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD curl -fs http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]