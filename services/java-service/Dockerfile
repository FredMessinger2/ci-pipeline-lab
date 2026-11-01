# Stage 1: build with Maven + JDK 21.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer caches until pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src src
RUN mvn -q -B package

# Stage 2: runtime is JRE-only — no Maven, no source.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/java-service.jar app.jar

EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
