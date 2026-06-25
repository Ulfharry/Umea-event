# UVEN — deploy

Production setup for the UVEN platform (domain **uven.se**). Three things are hosted:

1. **Backend** — Spring Boot (Docker), this repo (`umea-events/`).
2. **Database** — managed PostgreSQL 16 (e.g. **Supabase**).
3. **Web back-office** — static React SPA (`umea-event-web`).

The **mobile app** (`umea-event-mobile`) is not server-hosted — it's built with Expo EAS and
published to the app stores (or shared via Expo).

Suggested domains:
- `api.uven.se` → backend
- `admin.uven.se` → web back-office
- `uven.se` → reserved (future public site / store links)

---

## 0. Prerequisites (one-time)

- [x] Admin self-registration locked (PR #15) and admin bootstrap (PR #17) — **merged**.
- [x] Owner-scoped `/mine` endpoints (PR #16) — merged.
- [ ] Merge this PR (Dockerfile, CORS env binding, deploy docs).
- [ ] Generate a strong `JWT_SECRET` (≥32 chars), e.g. `openssl rand -base64 48`.

---

## 1. Database — Supabase (managed Postgres)

Supabase is a valid host **for the database only** (we use our own JWT auth, not Supabase Auth).

1. Create a Supabase project (pick the EU/Stockholm-nearest region).
2. Project → **Connect** → copy the connection details. **Use the direct/session connection
   (port `5432`), not the transaction pooler (`6543`)** — Flyway migrations and JPA need a
   session connection.
3. Build the JDBC URL (Supabase gives host/db/user/password):
   ```
   DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
   DB_USER=postgres
   DB_PASSWORD=<your-db-password>
   ```
   `sslmode=require` is needed — Supabase enforces TLS.

> Any managed Postgres works the same way (Fly Postgres, Railway Postgres, Neon, RDS …) —
> just point `DB_URL/USER/PASSWORD` at it. On first boot, `FlywayConfig` runs the migrations
> automatically against this database.

---

## 2. Backend — choose a host

The backend needs a JVM/Docker runtime. Options (all work with the included `Dockerfile`):

| Host | Notes |
|---|---|
| **Fly.io** (recommended) | Docker, Stockholm region (`arn`); `fly.toml` is included. Low latency in Sweden. |
| **Railway** | Deploy from GitHub; detects the Dockerfile. Simplest UI; can also host the DB. |
| **Render** | Web Service (Docker) + managed Postgres. Free tiers exist but the free web service sleeps. |

### Backend environment variables (set on the host)
```
DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require
DB_USER=postgres
DB_PASSWORD=<db-password>
JWT_SECRET=<strong-random-string>
CORS_ALLOWED_ORIGINS=https://admin.uven.se
BOOTSTRAP_ADMIN_EMAIL=admin@uven.se
BOOTSTRAP_ADMIN_PASSWORD=<strong-password>
```

### Deploy with Fly.io
```bash
cd umea-events
fly launch --no-deploy        # or: fly apps create uven-api  (fly.toml already present)
fly secrets set DB_URL=... DB_USER=... DB_PASSWORD=... JWT_SECRET=... \
                CORS_ALLOWED_ORIGINS=https://admin.uven.se \
                BOOTSTRAP_ADMIN_EMAIL=admin@uven.se BOOTSTRAP_ADMIN_PASSWORD=...
fly deploy
fly certs add api.uven.se     # then add the shown DNS records at your registrar
```

### First-boot verification
- `GET https://api.uven.se/actuator/health` → `{"status":"UP"}`
- Log in (web admin) with the bootstrap admin → create a venue → run a scrape source → publish an event.
- (Optional) remove `BOOTSTRAP_ADMIN_*` after the account exists.

---

## 3. Web back-office — static SPA

Host `umea-event-web` on Cloudflare Pages / Vercel / Netlify (all free).

- Build command: `npm run build`  → output dir: `dist`
- Build-time env var: `VITE_API_URL=https://api.uven.se`
- Custom domain: `admin.uven.se`

After it's live, ensure the backend's `CORS_ALLOWED_ORIGINS` includes `https://admin.uven.se`.

---

## 4. Mobile app

In `umea-event-mobile`:
```
EXPO_PUBLIC_API_URL=https://api.uven.se
```
Build and submit with EAS (`eas build`, `eas submit`). Native apps don't need CORS.

---

## DNS (at the uven.se registrar)
- `api.uven.se`   → backend host (Fly gives the records via `fly certs add`).
- `admin.uven.se` → static web host (CNAME shown by Cloudflare/Vercel/Netlify).

---

## Notes
- Local dev data (seeded venues, `admin@umea.test`) lives only in the local Docker DB — prod
  starts empty; populate it via the admin portal after first deploy.
- Actuator exposes only `health,info` publicly — keep it that way.
- `spring.flyway.enabled=false` is intentional: migrations run via `FlywayConfig` (programmatic),
  so they execute on first boot against `DB_URL`.
