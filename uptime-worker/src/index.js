import { EmailMessage } from 'cloudflare:email';

/**
 * Uptime monitor for UVEN, on a Cloudflare cron trigger.
 *
 * It replaces a GitHub Actions scheduled workflow that was configured for every 15 minutes and
 * delivered a median of one run every 3.6 hours — no interval under 20 minutes in 99 samples.
 * A two-hour outage on 2026-09-22 fell entirely between two checks and nobody was told.
 * Cloudflare's scheduler is a real one, so the configured interval is the actual interval.
 *
 * Mail goes out on transitions only (up->down, down->up) plus an hourly reminder while down, so
 * an outage costs a handful of messages instead of one every five minutes.
 */

const PROBE_TIMEOUT_MS = 10_000;
const RETRY_DELAY_MS = 5_000;
const REMINDER_INTERVAL_MS = 60 * 60 * 1000;
const STATE_KEY = 'status';
const HEARTBEAT_KEY = 'heartbeat';

/**
 * Health alone is not enough: on some failure modes a JVM answers /actuator/health while the
 * read path is broken, so we also prove a real query and the public site.
 */
const CHECKS = [
  {
    name: 'API health',
    url: 'https://api.uven.se/actuator/health',
    expect: (body) => body.includes('"status":"UP"'),
  },
  { name: 'Events-endpoint', url: 'https://api.uven.se/api/v1/events?size=1' },
  { name: 'Publika sajten', url: 'https://uven.se/' },
];

async function probe(check) {
  try {
    const res = await fetch(check.url, {
      signal: AbortSignal.timeout(PROBE_TIMEOUT_MS),
      headers: { 'User-Agent': 'uven-uptime/1.0' },
      cf: { cacheTtl: 0 },
    });
    if (!res.ok) return `${check.name}: HTTP ${res.status}`;
    if (check.expect && !check.expect(await res.text())) {
      return `${check.name}: HTTP 200 men oväntat svarsinnehåll`;
    }
    return null;
  } catch (e) {
    const why = e?.name === 'TimeoutError'
      ? `svarade inte inom ${PROBE_TIMEOUT_MS / 1000} s`
      : (e?.message ?? String(e));
    return `${check.name}: ${why}`;
  }
}

/** Probe everything; re-probe only what failed, so a single blip doesn't wake anyone. */
async function runChecks() {
  const first = await Promise.all(CHECKS.map(async (c) => ({ check: c, error: await probe(c) })));
  const failed = first.filter((r) => r.error);
  if (failed.length === 0) return [];

  await new Promise((r) => setTimeout(r, RETRY_DELAY_MS));
  const second = await Promise.all(failed.map(async (r) => ({ check: r.check, error: await probe(r.check) })));
  return second.filter((r) => r.error).map((r) => r.error);
}

// ── mail ────────────────────────────────────────────────────────────────────

function utf8Base64(s) {
  const bytes = new TextEncoder().encode(s);
  let bin = '';
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin);
}

/**
 * Hand-rolled RFC 5322 rather than a MIME library: this is the one code path that must work on
 * the day everything else is broken, so it has no dependency that could break it. Body and
 * subject are base64 so Swedish characters survive intact.
 */
function buildMime({ from, fromName, to, subject, body }) {
  const encodedBody = utf8Base64(body).replace(/(.{76})/g, '$1\r\n');
  return [
    `From: ${fromName} <${from}>`,
    `To: ${to}`,
    `Subject: =?UTF-8?B?${utf8Base64(subject)}?=`,
    `Message-ID: <${crypto.randomUUID()}@uven.se>`,
    `Date: ${new Date().toUTCString()}`,
    'MIME-Version: 1.0',
    'Content-Type: text/plain; charset="utf-8"',
    'Content-Transfer-Encoding: base64',
    '',
    encodedBody,
  ].join('\r\n');
}

async function sendAlert(env, subject, body) {
  const from = env.ALERT_FROM;
  const to = env.ALERT_TO;
  const raw = buildMime({ from, fromName: 'UVEN uptime', to, subject, body });
  await env.ALERT_EMAIL.send(new EmailMessage(from, to, raw));
}

// ── state ───────────────────────────────────────────────────────────────────

async function readState(env) {
  const raw = await env.STATE.get(STATE_KEY);
  if (!raw) return { status: 'up', since: Date.now(), lastNotifiedAt: 0 };
  try {
    return JSON.parse(raw);
  } catch {
    return { status: 'up', since: Date.now(), lastNotifiedAt: 0 };
  }
}

const writeState = (env, state) => env.STATE.put(STATE_KEY, JSON.stringify(state));

const minutesSince = (ms) => Math.round((Date.now() - ms) / 60000);

// ── the check itself ────────────────────────────────────────────────────────

/** Returns a short line describing what it did, for logs and the manual-trigger response. */
async function check(env) {
  const result = await decide(env);
  // Heartbeat last, and only on a real run. This is what proves the monitor itself is alive;
  // see the /heartbeat route for who reads it.
  await env.STATE.put(HEARTBEAT_KEY, JSON.stringify({ at: new Date().toISOString(), result }));
  return result;
}

async function decide(env) {
  const failures = await runChecks();
  const prev = await readState(env);
  const now = Date.now();

  if (failures.length > 0) {
    const detail = failures.map((f) => `  - ${f}`).join('\n');

    if (prev.status === 'up') {
      await sendAlert(env, 'UVEN: API:t svarar inte',
        `UVEN slutade svara ${new Date(now).toISOString()}.\n\n${detail}\n\n` +
        'Kontrollera: fly status -a uven-api\n' +
        'Nästa mail kommer när det är uppe igen, eller om en timme om det inte är det.');
      await writeState(env, { status: 'down', since: now, lastNotifiedAt: now });
      return `NER — larm skickat (${failures.length} fel)`;
    }

    if (now - prev.lastNotifiedAt >= REMINDER_INTERVAL_MS) {
      await sendAlert(env, 'UVEN: fortfarande nere',
        `UVEN har varit nere i ${minutesSince(prev.since)} minuter.\n\n${detail}`);
      await writeState(env, { ...prev, lastNotifiedAt: now });
      return `NER — påminnelse skickad (${minutesSince(prev.since)} min)`;
    }
    return `NER — redan larmat (${minutesSince(prev.since)} min)`;
  }

  if (prev.status === 'down') {
    await sendAlert(env, 'UVEN: uppe igen',
      `UVEN svarar igen. Avbrottet varade ${minutesSince(prev.since)} minuter.`);
    await writeState(env, { status: 'up', since: now, lastNotifiedAt: now });
    return `UPPE — återställd efter ${minutesSince(prev.since)} min`;
  }

  return 'UPPE';
}

/**
 * Accepts the secret from an X-Uptime-Secret header or, as a fallback, a ?secret= parameter.
 * The header is the one to use: a secret in a query string has to survive URL parsing, and a
 * generated one containing & + # or % silently arrives truncated or altered. Both sides are
 * trimmed, because a value pasted into a prompt often carries whitespace nobody can see.
 *
 * The response is always a flat 404 so a stranger learns nothing, but the reason is logged —
 * run `npx wrangler tail` to see why your own call was refused.
 */
function authorise(request, url, env) {
  const expected = env.TEST_SECRET?.trim();
  if (!expected) {
    console.log('auth: TEST_SECRET is not configured — run `wrangler secret put TEST_SECRET`');
    return false;
  }
  const offered = (request.headers.get('X-Uptime-Secret') ?? url.searchParams.get('secret') ?? '').trim();
  if (!offered) {
    console.log('auth: no secret supplied (use the X-Uptime-Secret header or ?secret=)');
    return false;
  }
  if (offered !== expected) {
    console.log(
      `auth: secret did not match (got ${offered.length} chars, expected ${expected.length})`,
    );
    return false;
  }
  return true;
}

export default {
  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(check(env).then((r) => console.log(`uptime: ${r}`)));
  },

  async fetch(request, env) {
    const url = new URL(request.url);
    const authorised = authorise(request, url, env);

    // Open on purpose, and deliberately cheap: one KV read, no probing, nothing worth abusing.
    // This is the dead man's switch. If this Worker stops running it cannot report that itself,
    // so something outside has to notice the silence — the GitHub backstop reads this and fails
    // when the timestamp goes stale.
    if (url.pathname === '/heartbeat') {
      const raw = await env.STATE.get(HEARTBEAT_KEY);
      const beat = raw ? JSON.parse(raw) : null;
      return Response.json({
        lastRunAt: beat?.at ?? null,
        ageSeconds: beat ? Math.round((Date.now() - Date.parse(beat.at)) / 1000) : null,
        lastResult: beat?.result ?? null,
      });
    }

    // Everything past here probes three sites or sends mail, so it needs the secret. Left open,
    // it would be a free way for anyone to burn the account's daily request budget — which would
    // stop the cron and take out the monitor itself.
    if (!authorised) return new Response('Not found\n', { status: 404 });

    if (url.searchParams.get('send') === 'test') {
      await sendAlert(env, 'UVEN: testlarm',
        'Det här är ett testlarm från uptime-workern. Kommer det fram fungerar larmvägen.');
      return new Response('Testlarm skickat\n');
    }

    if (url.searchParams.get('probe') === 'only') {
      const failures = await runChecks();
      return Response.json(
        { status: failures.length ? 'down' : 'up', failures },
        { status: failures.length ? 503 : 200 },
      );
    }

    return new Response(`${await check(env)}\n`);
  },
};
