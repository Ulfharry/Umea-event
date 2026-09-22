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
# Memory notes for this 512MB-class machine:
#  - MaxRAMPercentage=50 keeps the heap near half of RAM; 75% left too little for everything
#    outside it (metaspace, thread stacks, code cache, Jsoup buffers) and the scrape OOM-killed us.
#  - SerialGC suits a small heap on a single shared vCPU: fewer GC threads, less overhead.
#  - ExitOnOutOfMemoryError is the important one. Without it a heap-exhausted JVM does not die —
#    it survives in a GC spiral, serving nothing while staying "started", so nothing restarts it.
#    We sat in exactly that state for 20+ minutes. Exiting turns a silent outage into a restart.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
