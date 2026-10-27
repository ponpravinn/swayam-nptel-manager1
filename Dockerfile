# Stage 1: Build application using Maven and JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Copy pom and source code
COPY pom.xml .
COPY src ./src

# Build production JAR without running tests
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight JRE 17 runtime for production
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create spring user and data directory with full write permissions for H2 database
RUN addgroup --system spring && adduser --system spring --ingroup spring && \
    mkdir -p /app/data && \
    chown -R spring:spring /app

# Copy built jar from builder stage with correct ownership
COPY --chown=spring:spring --from=builder /app/target/nptel-manager-1.0.0.jar /app/app.jar

USER spring:spring

# Environment defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT:-8080}"]
