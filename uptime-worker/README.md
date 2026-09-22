# uven-uptime

Cloudflare Worker that polls UVEN every five minutes and mails you when it stops answering.

## Varför inte GitHub Actions

Den workflow som fanns var satt till `*/15`. Verkligheten, mätt över 99 körningar:

| | konfigurerat | levererat |
|---|---|---|
| Median-intervall | 15 min | **217 min** |
| Intervall under 20 min | alla | **0 av 99** |
| Största gap | 15 min | **7 tim** |

Ett tvåtimmarsavbrott 2026-09-22 föll helt mellan två mätningar. GitHubs schemaläggare är
"best effort" och stryper täta cron-jobb. Cloudflares är en riktig schemaläggare.

Workflowet finns kvar som backstop, nedskruvat till en gång i timmen — för den dagen den här
Workern själv är trasig.

## Uppsättning

1. **Verifiera mottagaradressen** i Cloudflare → Email Routing → Destination addresses.
   Utan verifiering avvisas `send()`.

2. **Skapa KV-namnrymden** och klistra in id:t i `wrangler.jsonc`:
   ```
   npx wrangler kv namespace create STATE
   ```

3. **Fyll i adresserna** i `wrangler.jsonc` — `ALERT_TO` och `send_email[0].destination_address`
   ska vara samma verifierade adress.

4. **Sätt hemligheten** för manuell körning:
   ```
   npx wrangler secret put TEST_SECRET
   ```

5. **Deploya:**
   ```
   npm install && npx wrangler deploy
   ```

6. **Bevisa att larmet går fram** — det här steget är hela poängen. Den gamla larmvägen hade
   200 gröna körningar och hade aldrig utlösts en enda gång, så ingen visste om den fungerade:
   ```
   curl "https://uven-uptime.<din-subdomän>.workers.dev/?send=test&secret=<TEST_SECRET>"
   ```
   Kommer mailet fram är kedjan hel.

## Användning

| Anrop | Gör |
|---|---|
| `GET /` | Kör mätningarna, svarar med JSON. Skickar inget, skriver inget. |
| `GET /?secret=…` | Full körning inklusive mail och tillstånd — som cron-jobbet. |
| `GET /?send=test&secret=…` | Skickar ett testlarm. |
| `npx wrangler tail` | Följer loggarna live. |

## Mailpolicy

Mail skickas vid **övergångar**, inte vid varje mätning: en gång när det går ner, en gång när
det kommer upp, plus en påminnelse i timmen så länge det är nere. Ett avbrott kostar alltså
en handfull mail, inte ett var femte minut.
