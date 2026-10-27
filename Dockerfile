# Stage 1: Build application using Maven and JDK 17
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build application source code
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Lightweight JRE 17 runtime for production
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Run as non-root user for security
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copy built jar from builder stage
COPY --from=builder /app/target/nptel-manager-*.jar app.jar

# Environment defaults
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
