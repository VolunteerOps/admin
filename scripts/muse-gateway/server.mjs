import {createServer} from 'node:http';
import {createHmac, timingSafeEqual} from 'node:crypto';
import {readFileSync} from 'node:fs';
import {pathToFileURL} from 'node:url';

const path = '/v1/chat/completions';
const models = new Set(['muse-spark-1.3', 'muse-wonder-1.0']);
export function signature(secret, timestamp, nonce, body) {
  return createHmac('sha256', secret).update(`POST\n${path}\n${timestamp}\n${nonce}\n`).update(body).digest('hex');
}
async function readBounded(stream, limit) {
  const chunks = []; let size = 0;
  for await (const chunk of stream) {
    size += chunk.length;
    if (size > limit) throw new Error('TOO_LARGE');
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}

/** Only the backend can call this relay. It executes no tools and accepts no destination URL. */
export function createGateway({key, secret, fetcher = fetch, now = Date.now, concurrency = 3, perMinute = 40, log = () => {}}) {
  if (!key || !secret || secret.length < 43) throw new Error('Gateway credentials are missing or too short');
  const nonces = new Map(); let active = 0, windowStart = now(), requests = 0;
  const server = createServer(async (req, res) => {
    const send = (status, value) => {
      if (res.destroyed) return;
      res.writeHead(status, {'Content-Type':'application/json', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff'});
      res.end(JSON.stringify(value));
    };
    const fail = (status, code) => send(status, {error:{code, message:'The temporary assistant connection could not complete this request.'}});
    if (req.method === 'GET' && req.url === '/health') return send(200, {status:'ready'});
    if (req.method !== 'POST' || req.url !== path) return fail(404, 'NOT_FOUND');
    const timestamp = req.headers['x-resq-timestamp'], nonce = req.headers['x-resq-nonce'], supplied = req.headers['x-resq-signature'];
    // Reject untrusted traffic before reading a request body or calling Meta.
    if (typeof timestamp !== 'string' || !/^\d{13}$/.test(timestamp) || Math.abs(now()-Number(timestamp)) > 60000 ||
        typeof nonce !== 'string' || !/^[a-f0-9]{32}$/.test(nonce) || typeof supplied !== 'string' || !/^[a-f0-9]{64}$/.test(supplied)) return fail(401, 'GATEWAY_UNAUTHORIZED');
    if (req.headers['content-type']?.split(';')[0] !== 'application/json' || req.headers['content-encoding']) return fail(415, 'GATEWAY_CONTENT_TYPE');
    if (Number(req.headers['content-length']) > 524288) return fail(413, 'GATEWAY_BODY_TOO_LARGE');
    let body;
    try { body = await readBounded(req, 524288); } catch { return fail(413, 'GATEWAY_BODY_TOO_LARGE'); }
    const expected = signature(secret, timestamp, nonce, body);
    if (!timingSafeEqual(Buffer.from(expected,'hex'), Buffer.from(supplied,'hex'))) return fail(401, 'GATEWAY_UNAUTHORIZED');
    for (const [id, at] of nonces) if (now()-at > 120000) nonces.delete(id);
    if (nonces.has(nonce)) return fail(409, 'GATEWAY_REPLAY');
    if (now()-windowStart >= 60000) { windowStart=now(); requests=0; }
    if (active >= concurrency || requests >= perMinute) return fail(429, 'GATEWAY_BUSY');
    nonces.set(nonce,now()); requests++;
    let payload;
    try { payload = JSON.parse(body); } catch { return fail(400, 'GATEWAY_INVALID_REQUEST'); }
    const allowed = new Set(['model','messages','max_completion_tokens','stream','tools','tool_choice','response_format']);
    if (!payload || typeof payload !== 'object' || Array.isArray(payload) || Object.keys(payload).some(k=>!allowed.has(k)) ||
        !models.has(payload.model) || (payload.stream !== undefined && payload.stream !== false) ||
        !Number.isInteger(payload.max_completion_tokens) || payload.max_completion_tokens < 1 || payload.max_completion_tokens > 4096 ||
        !Array.isArray(payload.messages) || payload.messages.length < 1 || payload.messages.length > 200 ||
        (payload.tools !== undefined && (!Array.isArray(payload.tools) || payload.tools.length > 32))) return fail(400, 'GATEWAY_INVALID_REQUEST');
    active++;
    const started=now(), controller=new AbortController();
    res.on('close',()=>{if (!res.writableFinished) controller.abort();});
    try {
      const upstream = await fetcher('https://api.meta.ai/v1/chat/completions', {
        method:'POST',redirect:'error', signal:AbortSignal.any([controller.signal,AbortSignal.timeout(28000)]),
        headers:{'Content-Type':'application/json',Authorization:`Bearer ${key}`}, body,
      });
      const bytes = upstream.body ? await readBounded(upstream.body, 262144) : Buffer.from('{}');
      const data = JSON.parse(bytes.toString('utf8'));
      const requestId=upstream.headers.get('x-request-id');
      if (requestId && /^[\w-]{1,100}$/.test(requestId)) res.setHeader('x-request-id',requestId);
      if (upstream.ok) send(200,data);
      else fail(upstream.status, ['model_not_found','content_policy_violation'].includes(data.error?.code) ? data.error.code : 'GATEWAY_PROVIDER_REJECTED');
      log({event:'MUSE_GATEWAY_REQUEST',model:payload.model,status:upstream.status,duration_ms:now()-started});
    } catch {
      fail(502,'GATEWAY_UNAVAILABLE');
      log({event:'MUSE_GATEWAY_REQUEST',model:payload.model,status:502,duration_ms:now()-started});
    } finally { active--; }
  });
  server.requestTimeout=15000; server.headersTimeout=10000; server.keepAliveTimeout=5000; server.maxConnections=32;
  return server;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  // This ACL-protected file stays outside the repository. Its path, not its contents, appears in process arguments.
  const config=JSON.parse(readFileSync(process.argv[2],'utf8').replace(/^\uFEFF/,''));
  const server=createGateway({key:config.key,secret:config.secret,log:entry=>console.log(JSON.stringify({...entry,at:new Date().toISOString()}))});
  server.listen(config.port ?? 4187,'127.0.0.1',()=>console.log(JSON.stringify({event:'MUSE_GATEWAY_READY',port:config.port ?? 4187})));
  for (const signal of ['SIGINT','SIGTERM']) process.on(signal,()=>server.close(()=>process.exit(0)));
}
