# ---------- Stage 1: build the JAR ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml first so dependencies are cached as a separate layer
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: slim runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Run as a non-root user
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/target/calculator.jar app.jar
USER app

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
