# Umeå Events — Backend

Spring Boot 4.0.x · Java 25 LTS · PostgreSQL · Flyway · OpenAPI

Milestone 1: grundstruktur + `GET /api/v1/categories`.

## Förutsättningar

- JDK 25
- Maven 3.9+
- Docker (för PostgreSQL)

## Kör lokalt

```bash
# 1. Starta databasen
docker compose up -d

# 2. Starta appen (Flyway kör V1 + V2 automatiskt vid uppstart)
mvn spring-boot:run
```

## Verifiera

```bash
curl http://localhost:8080/api/v1/categories
```

Ska returnera de åtta seedade kategorierna som JSON.

Övriga endpoints:

- Swagger UI:  http://localhost:8080/swagger-ui.html
- OpenAPI:     http://localhost:8080/v3/api-docs
- Health:      http://localhost:8080/actuator/health

## Tester

```bash
mvn test
```

`CategoryControllerTest` kör utan databas (mockad service). `UmeaEventsApplicationTests`
(context load) kräver en nåbar databas — nästa steg är att byta det mot
Testcontainers så testerna är helt självförsörjande.

## Vill du stå på Java 24 + Spring Boot 3.x istället?

Ändra i `pom.xml`:
- `<parent>`-versionen `4.0.6` → senaste `3.5.x`
- `<java.version>` `25` → `24`
- springdoc `3.0.1` → `2.8.x`

Rekommenderas inte (3.x är EOL och 24 är non-LTS), men koden i denna milestone
är i övrigt identisk.
