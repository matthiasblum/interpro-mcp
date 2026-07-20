# Multi-stage build for the InterPro MCP server.
# Build context is the repository root,
# so paths below are relative to the repo root, not to deploy/.

# Build stage: compile and package the Spring Boot fat jar
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Resolve dependencies first. This layer is cached unless pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Build the jar. Skip tests (we don't have any yet...)
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Runtime stage: minimal JRE
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# Run as a dedicated non-root user.
RUN groupadd --system --gid 1001 spring \
 && useradd  --system --uid 1001 --gid spring --home-dir /app --shell /usr/sbin/nologin spring

 COPY --from=build /workspace/target/*.jar /app/app.jar

USER spring:spring
EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
