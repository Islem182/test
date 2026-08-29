# Multi-stage Dockerfile for Spring Boot 3 Backend
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "app.jar"]
