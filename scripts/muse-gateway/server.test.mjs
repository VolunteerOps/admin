import {test} from 'node:test';
import assert from 'node:assert/strict';
import {randomBytes} from 'node:crypto';
import {createGateway,signature} from './server.mjs';
const secret='test-gateway-secret-'.repeat(4), key='private-model-key';
const payload={model:'muse-spark-1.3',messages:[{role:'user',content:'Synthetic check'}],max_completion_tokens:128};
async function fixture(t, overrides={}) {
  const calls=[];
  const server=createGateway({key,secret,fetcher:async(url,init)=>{calls.push({url,init});return Response.json({choices:[{message:{content:'Ready'}}]});},...overrides});
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  t.after(()=>new Promise(resolve=>{server.closeAllConnections();server.close(resolve);}));
  const url=`http://127.0.0.1:${server.address().port}`;
  const signed=(body=JSON.stringify(payload),timestamp=String(Date.now()),nonce=randomBytes(16).toString('hex'))=>({method:'POST',headers:{'Content-Type':'application/json','x-resq-timestamp':timestamp,'x-resq-nonce':nonce,'x-resq-signature':signature(secret,timestamp,nonce,body)},body});
  return {calls,url,signed,post:(init)=>fetch(url+'/v1/chat/completions',init)};
}
test('only a signed request reaches the fixed Meta endpoint; secrets and signatures are not forwarded',async t=>{
  const f=await fixture(t);
  assert.equal((await f.post({method:'POST',body:'{}'})).status,401);
  assert.equal(f.calls.length,0);
  const request=f.signed();request.headers.Authorization='Bearer untrusted';
  const response=await f.post(request);assert.equal(response.status,200);
  assert.deepEqual(await response.json(),{choices:[{message:{content:'Ready'}}]});
  assert.equal(f.calls[0].url,'https://api.meta.ai/v1/chat/completions');
  assert.deepEqual(f.calls[0].init.headers,{'Content-Type':'application/json',Authorization:`Bearer ${key}`});
  assert.equal(f.calls[0].init.redirect,'error');
  assert.equal((await f.post(request)).status,409);assert.equal(f.calls.length,1);
});
test('tampered and expired signatures, disallowed models, streaming and destination overrides fail closed',async t=>{
  const f=await fixture(t);
  const tampered=f.signed();tampered.body=JSON.stringify({...payload,max_completion_tokens:200});
  assert.equal((await f.post(tampered)).status,401);
  assert.equal((await f.post(f.signed(undefined,String(Date.now()-120000)))).status,401);
  for (const extra of [{model:'other'},{stream:true},{url:'http://localhost/'},{max_completion_tokens:999999}]) {
    assert.equal((await f.post(f.signed(JSON.stringify({...payload,...extra})))).status,400);
  }
  assert.equal(f.calls.length,0);
});
test('request budgets and body limits apply before invoking Meta',async t=>{
  const f=await fixture(t,{perMinute:1});
  assert.equal((await f.post(f.signed())).status,200);
  assert.equal((await f.post(f.signed())).status,429);
  assert.equal((await f.post(f.signed('x'.repeat(524289)))).status,413);
  assert.equal(f.calls.length,1);
});
test('upstream error bodies and thrown exceptions never expose credentials or prompts',async t=>{
  const f=await fixture(t,{fetcher:async()=>Response.json({error:{code:'model_not_found',message:key}},{status:404})});
  const response=await f.post(f.signed());assert.equal(response.status,404);
  const body=await response.text();assert.ok(body.includes('model_not_found'));assert.ok(!body.includes(key));
  const broken=await fixture(t,{fetcher:async()=>{throw new Error(key);}});
  const unavailable=await broken.post(broken.signed());assert.equal(unavailable.status,502);assert.ok(!(await unavailable.text()).includes(key));
});
test('concurrent calls are capped and capacity is released after a response',async t=>{
  let unblock,entered;
  const started=new Promise(resolve=>{entered=resolve;});
  const waiting=new Promise(resolve=>{unblock=resolve;});
  const f=await fixture(t,{concurrency:1,fetcher:async()=>{entered();await waiting;return Response.json({choices:[]});}});
  const first=f.post(f.signed());await started;
  assert.equal((await f.post(f.signed())).status,429);
  unblock();assert.equal((await first).status,200);
  assert.equal((await f.post(f.signed())).status,200);
});
test('oversized provider replies are rejected without exposing their contents',async t=>{
  const f=await fixture(t,{fetcher:async()=>new Response('x'.repeat(262145))});
  const response=await f.post(f.signed());assert.equal(response.status,502);
  assert.equal((await response.json()).error.code,'GATEWAY_UNAVAILABLE');
});
test('provider refusal keeps its safe classification without exposing the provider message',async t=>{
 const f=await fixture(t,{fetcher:async()=>Response.json({error:{code:'content_policy_violation',message:'private request details'}},{status:400})});
 const response=await f.post(f.signed());assert.equal(response.status,400);
 const body=await response.json();assert.equal(body.error.code,'content_policy_violation');assert.ok(!JSON.stringify(body).includes('private request details'));
});
