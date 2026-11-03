import {test,expect} from '@playwright/test';
import {fixtures} from './fixtures';
const answer={id:'answer',payload:{status:'DRAFT',coverage:'NO_FORECAST_EVIDENCE',narrative:{summary:'Check local reports before planning field work.',preparedness:['Confirm conditions with local officers.'],assumptions:[],missing_inputs:[],source_ids:[]},sources:[]}};
const sse=(event:string,data:unknown)=>`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`;
test('weather provider errors do not launch a second run; explicit retry has a fresh key',async({page})=>{
 await fixtures(page);const keys:string[]=[];let jsonCalls=0;
 await page.route('**/conversations/weather',r=>{jsonCalls++;return r.fulfill({json:answer});});
 await page.route('**/conversations/weather/stream',r=>{keys.push(r.request().headers()['idempotency-key']);return r.fulfill({contentType:'text/event-stream',body:keys.length===1?sse('error',{code:'MODEL_UNAVAILABLE'}):sse('result',answer)});});
 await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('How should we prepare for rainfall?');
 const send=page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true});await send.click();
 await expect(page.locator('.weather-chat [role=alert]')).toContainText('Muse is not accepting');expect(jsonCalls).toBe(0);
 await send.click();await expect(page.getByText(answer.payload.narrative.summary)).toBeVisible();expect(keys).toHaveLength(2);expect(keys[0]).not.toBe(keys[1]);expect(jsonCalls).toBe(0);
});
test('missing stream route falls back once using the same request key',async({page})=>{
 await fixtures(page);let streamKey='',jsonKey='';
 await page.route('**/conversations/weather/stream',r=>{streamKey=r.request().headers()['idempotency-key'];return r.fulfill({status:404,json:{error:'NOT_FOUND'}});});
 await page.route('**/conversations/weather',r=>{jsonKey=r.request().headers()['idempotency-key'];return r.fulfill({json:answer});});
 await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('Prepare our team for tomorrow');await page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true}).click();await expect(page.getByText(answer.payload.narrative.summary)).toBeVisible();expect(jsonKey).toBe(streamKey);
});
test('an interrupted stream keeps its key and does not start a parallel run',async({page})=>{
 await fixtures(page);const keys:string[]=[];let jsonCalls=0;
 await page.route('**/conversations/weather',r=>{jsonCalls++;return r.fulfill({json:answer});});
 await page.route('**/conversations/weather/stream',r=>{keys.push(r.request().headers()['idempotency-key']);return r.fulfill({contentType:'text/event-stream',body:keys.length===1?sse('round',{round:1}):sse('result',answer)});});
 await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('Prepare for rainfall');const send=page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true});await send.click();await expect(page.getByText(/connection ended before the answer/)).toBeVisible();await send.click();await expect(page.getByText(answer.payload.narrative.summary)).toBeVisible();expect(keys[0]).toBe(keys[1]);expect(jsonCalls).toBe(0);
});
test('form draft keys are accepted server identifiers and password confirmation is never saved',async({page})=>{
 await fixtures(page);const drafts:{key:string,body:any}[]=[];
 await page.route('**/api/me/drafts/**',r=>{drafts.push({key:decodeURIComponent(new URL(r.request().url()).pathname.split('/drafts/')[1]),body:r.request().postDataJSON()});return r.fulfill({json:{payload:null,version:0}});});
 await page.goto('/settings');await page.getByLabel('Domain',{exact:true}).fill('example.org');await expect.poll(()=>drafts.filter(d=>d.body).length).toBeGreaterThan(0);expect(drafts.every(d=>/^[\w:./-]{1,300}$/.test(d.key))).toBe(true);
 await page.route('**/operations/commands',r=>r.fulfill({status:401,json:{error:'RECENT_AUTH_REQUIRED'}}));await page.goto('/security');await page.getByLabel('Reason for this decision').fill('Review dispatch status');await page.getByRole('button',{name:'Update dispatch control'}).click();const dialog=page.getByRole('dialog');await dialog.getByLabel('Password',{exact:true}).fill('test-private-value');await expect(dialog.getByRole('button',{name:'Save draft',exact:true})).toHaveCount(0);await page.waitForTimeout(1000);
 expect(JSON.stringify(drafts)).not.toContain('test-private-value');expect(drafts.some(d=>/password/i.test(d.key))).toBe(false);expect(await page.evaluate(()=>JSON.stringify(localStorage))).not.toContain('test-private-value');
});
test('reopening weather chat does not repeatedly fetch a failing draft',async({page})=>{
 await fixtures(page);let count=0;await page.route('**/api/me/drafts/weather-chat*',r=>{count++;return r.fulfill({status:500,json:{error:'REQUEST_FAILED'}});});await page.goto('/sentinel?ask=1');await expect(page.getByLabel('Ask about your region')).toBeVisible();const toggle=page.locator('.sentinel-intro').getByRole('button',{name:'Ask Sentinel'});await toggle.click();await toggle.click();await expect(page.getByLabel('Ask about your region')).toBeVisible();expect(count).toBe(1);
});
test('expired sign-in refreshes once and preserves the stream request key',async({page})=>{
 await fixtures(page);const keys:string[]=[];await page.route('**/conversations/weather/stream',r=>{keys.push(r.request().headers()['idempotency-key']);return r.fulfill(keys.length===1?{status:401,json:{error:'INVALID_FIREBASE_IDENTITY'}}:{contentType:'text/event-stream',body:sse('result',answer)});});
 await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('Review our rainfall preparations');await page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true}).click();await expect(page.getByText(answer.payload.narrative.summary)).toBeVisible();expect(keys).toHaveLength(2);expect(keys[0]).toBe(keys[1]);
});
test('revoked sessions do not retry or fall back to another assistant request',async({page})=>{
 await fixtures(page);let count=0;await page.route('**/conversations/weather{,/stream}',r=>{count++;return r.fulfill({status:401,json:{error:'SESSION_REVOKED'}});});await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('Review our rainfall preparations');await page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true}).click();await expect(page.locator('.weather-chat [role=alert]')).toContainText('Sign in again');expect(count).toBe(1);
});
