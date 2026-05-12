# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 – Build
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy POM first (layer cache: only re-download deps when pom changes)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copy source and build fat JAR
COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 – Runtime (minimal image)
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="CSE112 - Alamein International University"
LABEL description="Cairo Smart Transportation Optimization System"
LABEL version="1.0.0"

WORKDIR /app

# Copy the fat JAR from the builder stage
COPY --from=builder /app/target/cairo-transport-system.jar ./cairo-transport-system.jar

# Default: run the full auto demo (non-interactive)
# Override with `docker run ... interactive` to get the menu
ENTRYPOINT ["java", "-jar", "cairo-transport-system.jar"]
CMD ["--auto"]
