# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# Cache dependencies first
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
# Build the jar (tests run in CI, not in the image build)
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Runtime stage ----
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/umea-events-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Honour container memory limits
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
