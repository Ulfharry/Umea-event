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

## Vem vakar över vakten

Workern vakar över UVEN. Om Workern själv slutar köra — avpublicerad, kastar fel varje varv,
KV-bindningen borttagen — blir den bara tyst. Och tystnad från en övervakare ser exakt ut som
att allt är bra. Det är den farligaste sortens fel.

Därför skriver Workern ett **hjärtslag** till KV vid varje körning, och exponerar det på
`GET /heartbeat`. GitHub-workflowet läser det och faller om tidsstämpeln blir gammal
(över 20 minuter = fyra missade körningar).

De två systemen täcker alltså varandra:

| Vad som går sönder | Vem larmar | Hur snabbt |
|---|---|---|
| UVEN ligger nere | Workern | ~5 min |
| Workern har slutat köra | GitHub-backstopen | inom några timmar |
| Båda ligger nere | GitHub-backstopen mäter även API:t direkt | inom några timmar |

GitHubs opålitliga takt duger här: en trasig *övervakare* är mycket ovanligare än ett
avbrott, och "upptäckt inom några timmar" slår "aldrig".

För att koppla in den, sätt en repository-variabel i GitHub
(Settings → Secrets and variables → Actions → Variables):

```
UPTIME_WORKER_URL = https://uven-uptime.<din-subdomän>.workers.dev
```

Är den inte satt hoppar steget över sig själv med en varning, så resten av övervakningen
fungerar ändå.

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

| Anrop | Gör | Kräver hemlighet |
|---|---|---|
| `GET /heartbeat` | När kördes den senast? En KV-läsning, inga mätningar. | nej |
| `GET /?probe=only&secret=…` | Kör mätningarna, svarar med JSON. Skickar och skriver inget. | ja |
| `GET /?secret=…` | Full körning inklusive mail och tillstånd — som cron-jobbet. | ja |
| `GET /?send=test&secret=…` | Skickar ett testlarm. | ja |
| `npx wrangler tail` | Följer loggarna live. | — |

Allt utom `/heartbeat` kräver hemligheten. Lämnad öppen vore mätvägen ett gratis sätt för vem
som helst att bränna kontots dagliga request-budget — vilket skulle stoppa cron-jobbet och
slå ut övervakningen.

## Mailpolicy

Mail skickas vid **övergångar**, inte vid varje mätning: en gång när det går ner, en gång när
det kommer upp, plus en påminnelse i timmen så länge det är nere. Ett avbrott kostar alltså
en handfull mail, inte ett var femte minut.
