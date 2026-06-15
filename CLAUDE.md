# CLAUDE.md

Standing-kontext för Claude Code i detta repo. Läs denna fil innan du gör ändringar.
Detaljerad design finns i `docs/umea-event-platform-arkitektur.md` (lägg arkitektur-
dokumentet där om det inte redan finns) — den är källan till sanning vid konflikt.

---

## Projektöversikt

Backend-API för att samla, publicera, söka och kategorisera restaurang- och nöjesevent
i Umeå (pubquiz, livemusik, DJ-kvällar, standup, m.m.). Tre roller: `USER`, `RESTAURANT`,
`ADMIN`. Tre eventkällor: manuella, återkommande och scrapade (scrapade auto-publiceras
aldrig — de granskas av admin först).

Byggs stegvis, backend först. Milestone 1 är klar (se status nedan).

---

## Teknisk stack — versionsbeslut

| Del | Val | Notering |
|---|---|---|
| Språk | **Java 25 LTS** | Inte 24. 24 är non-LTS och ur support. |
| Ramverk | **Spring Boot 4.0.x** | Inte 3.x. 3.x är EOL/nära EOL i juni 2026. |
| Bygg | Maven (Spring Boot-BOM styr versioner) | |
| DB | PostgreSQL 16 (via `compose.yaml`) | |
| Migrationer | Flyway (`flyway-core` + `flyway-database-postgresql`) | |
| Dokumentation | springdoc-openapi **3.0.x** | v2.8.x är för Boot 3.x — använd inte. |
| Övrigt | Spring Security (M2), JPA/Hibernate, Lombok, Bean Validation, Actuator | |

> Den ursprungliga specen sa Java 24 + Spring Boot 3.x. Det är medvetet ändrat för att
> undvika teknisk skuld. Ändra inte tillbaka utan att fråga.

---

## Bygg, kör, testa

```bash
docker compose up -d        # PostgreSQL på localhost:5432
mvn spring-boot:run         # Flyway migrerar automatiskt vid uppstart
mvn test                    # tester
mvn clean verify            # full byggverifiering före commit
```

Verifiera health: `curl http://localhost:8080/api/v1/categories` → åtta kategorier.
Swagger: `http://localhost:8080/swagger-ui.html` · Health: `/actuator/health`

---

## Arkitekturprinciper (följ dessa)

- **Package-by-feature.** Allt för en feature ligger i samma paket (`category/`,
  `event/`, `venue/` …). `common/` rymmer bara genuint tvärgående kod (security,
  config, exception, validation) — lägg aldrig affärslogik där.
- **Lagerflöde:** Controller (HTTP) → Service (logik + `@Transactional`) → Repository
  (SQL). Inget lager hoppar över ett annat.
- **Exponera aldrig entiteter över API.** Controllers tar emot och returnerar DTO:er.
  DTO:er är records; entiteter använder Lombok.
- **Flyway äger schemat.** `spring.jpa.hibernate.ddl-auto` ska vara `validate` — aldrig
  `update`/`create`. Schemaändringar görs som nya `V{n}__*.sql`-migrationer, befintliga
  migrationer ändras aldrig.
- **Konstruktorinjektion** (via Lombok `@RequiredArgsConstructor`), inte fältinjektion.
- **Mappers:** handskrivna nu. Byt till MapStruct först när antalet entiteter gör det
  motiverat (riktmärke: M4+).
- **All felhantering** går via `common/exception/GlobalExceptionHandler`; alla fel ut ur
  API:t har formatet `ErrorResponse`.
- **Tider:** lagra absoluta tidpunkter som `timestamptz` (UTC). Återkommande regler
  lagras i lokal väggklockstid + IANA-zon (`Europe/Stockholm`) och expanderas till UTC.
  Detta är kritiskt för DST-korrekthet — slarva inte med det i M6.

---

## Domänmodell (kärnbeslut)

Full modell i arkitekturdokumentet. De beslut som styr koden:

- **Ingen separat "Restaurant"-entitet.** Slås ihop till `Venue` (fält `type`:
  RESTAURANT/PUB/BAR/CLUB/OTHER) med en `owner`-relation till `User`. `Organization`
  införs först vid faktiskt behov (kedja med flera lokaler).
- **`Event` (vad) skilt från `EventOccurrence` (när).** Engångsevent = ett Event med en
  Occurrence. Återkommande = ett Event + en `RecurrenceRule` + många genererade
  Occurrences. Allt användaren söker i är Occurrences.
- **Återkommande event = hybrid:** RFC 5545-`RRULE` på serien + ett schemalagt jobb som
  materialiserar Occurrences upp till en rullande horisont. Inte ren beräkning vid läsning.
- **Scrapad data hålls utanför kärnmodellen** i `raw_scraped_event` (status
  `PENDING_REVIEW`). Admin promotar godkända till riktiga Event+Occurrence.
- **Category är en tabell** (inte enum) — seedad med de åtta kategorierna.
- **Eventlivscykel:** `DRAFT → PENDING_REVIEW → PUBLISHED → CANCELLED/ARCHIVED`.

---

## API-konventioner

- Bas-path `/api/v1`, pluraliserade substantiv (`/events`, `/venues`, `/categories`).
- Paginering `?page=&size=`, sortering `?sort=`.
- `GET /api/v1/events` returnerar **occurrences** (varje träff = konkret datum/tid), inte
  Event-rader.
- Behörighet uttrycks per endpoint (`permitAll` / roll / ägare). Publik läsning ser bara
  `PUBLISHED`.
- Validering med `@Valid` på request-DTO:er → 400 med fältfel via GlobalExceptionHandler.

---

## Milstolpestatus

- [x] **M1 — Grundstruktur:** projekt, Postgres/compose, Flyway, OpenAPI, Actuator,
      GlobalExceptionHandler, `Category` + `GET /api/v1/categories`.
- [x] **M2 — Authentication:** `User`, `RefreshToken`, JWT (access+refresh), rollbaserad
      åtkomst, BCrypt/Argon2. **OBS:** Boot 4 → Spring Security 7; använd Security 7-API,
      inte 6-tutorials.
- [x] **M3 — Venues:** `Venue` CRUD, ägarskap, publik listning.
- [x] **M4 — Events:** `Event` + `EventOccurrence` (engångs), statusflöde.
- [x] **M5 — Search & Filter:** occurrence-sökning, Postgres full-text (`tsvector` + GIN).
- [x] **M6 — Recurring:** `RecurrenceRule`, materialiseringsjobb, occurrence-overrides,
      DST-tester.
- [x] **M7 — Scraping:** `raw_scraped_event`, adapter per källa, admin-granskning/promotion.
- [x] **M8 — Admin Dashboard API:** aggregerade endpoints, moderering.

Varje milstolpe ska lämna ett körbart, testat API-snitt — skjut inte tester till slutet.

---

## Att undvika (guardrails)

- Ändra inte stacken tillbaka till Java 24 / Spring Boot 3.x utan att fråga.
- Sätt aldrig `ddl-auto` till annat än `validate`.
- Ändra aldrig en redan körd Flyway-migration; lägg en ny.
- Returnera aldrig JPA-entiteter direkt från en controller.
- Lägg aldrig affärslogik i `common/`.
- Auto-publicera aldrig scrapade event.
- Lägg inte till tunga beroenden (Elasticsearch, MapStruct, PostGIS) förrän ett konkret
  behov finns — motivera i en commit/PR om du gör det.

---

## Innan du committar

Kör `mvn clean verify`. Håll commits avgränsade per milstolpe/feature. Uppdatera
milstolpestatusen ovan när en milstolpe blir klar.
