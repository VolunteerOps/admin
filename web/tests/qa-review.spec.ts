import {test,expect} from '@playwright/test';
import {fixtures} from './fixtures';
import {coordinates,publicSourceUrl} from '../src/lib/map-data';

test('map positions reject missing and out-of-range coordinates without hiding zero',()=>{
 expect(coordinates({latitude:0,longitude:0})).toEqual([0,0]);
 for(const p of [{latitude:null,longitude:0},{latitude:91,longitude:0},{latitude:0,longitude:181},{latitude:'',longitude:0},{latitude:true,longitude:0}])expect(coordinates(p)).toBeNull();
 expect(publicSourceUrl('javascript:alert(1)')).toBeNull();
 expect(publicSourceUrl('https://example.org/report')).toBe('https://example.org/report');
});
test('saved form fields actually resume and writes carry the server version',async({page})=>{
 await fixtures(page);let saved:any=null;const versions:number[]=[];
 await page.route('**/api/me/drafts/**',async route=>{
  if(route.request().method()==='PUT'){
   const input=route.request().postDataJSON();versions.push(input.base_version);
   saved={payload:input.payload,version:(saved?.version??0)+1,updated_at:new Date().toISOString()};
  }
  await route.fulfill({json:saved});
 });
 await page.goto('/campaigns');await page.getByRole('button',{name:'New campaign',exact:true}).click();
 await page.getByLabel('Campaign name',{exact:true}).fill('Flood readiness checks');
 await expect.poll(()=>versions.length).toBe(1);
 await page.getByLabel('Campaign name',{exact:true}).fill('Updated readiness checks');
 await expect.poll(()=>versions.length).toBe(2);expect(versions).toEqual([0,1]);
 await page.reload();await page.getByRole('button',{name:'New campaign',exact:true}).click();
 await expect(page.getByRole('button',{name:'Resume draft',exact:true})).toBeVisible();
 await page.getByRole('button',{name:'Resume draft',exact:true}).click();
 await expect(page.getByLabel('Campaign name',{exact:true})).toHaveValue('Updated readiness checks');
 await page.screenshot({path:'test-results/qa-form-resume.png',fullPage:true});
});
test('a draft conflict preserves local edits and explains the recovery',async({page})=>{
 await fixtures(page);
 await page.route('**/api/me/drafts/**',r=>r.request().method()==='PUT'?r.fulfill({status:409,json:{error:'DRAFT_CONFLICT',message:'Changed elsewhere'}}):r.fulfill({json:{version:3,payload:{values:{title:'Other device'},saved_at:new Date().toISOString()}}}));
 await page.goto('/campaigns');await page.getByRole('button',{name:'New campaign',exact:true}).click();
 await page.getByLabel('Campaign name',{exact:true}).fill('My local changes');
 await expect(page.getByText('Another device changed this draft.',{exact:false})).toBeVisible();
 await expect(page.getByLabel('Campaign name',{exact:true})).toHaveValue('My local changes');
});
test('section links retain active state on navigation and narrow screens',async({page})=>{
 await fixtures(page);await page.setViewportSize({width:760,height:1000});await page.goto('/weather-analysis');
 const nav=page.getByRole('navigation',{name:'Section navigation'});
 await expect(nav.locator('[aria-current="page"]')).toHaveCount(1);
 const radar=nav.getByRole('link',{name:'Disaster radar',exact:true});
 await radar.click();await expect(radar).toHaveAttribute('aria-current','page');
 await expect(page.getByRole('heading',{name:'This page couldn’t load'})).toHaveCount(0);
 await page.screenshot({path:'test-results/qa-tabs-narrow.png',fullPage:true});
});
test('incomplete hazard data is disclosed and source links are safe',async({page})=>{
 await fixtures(page);await page.route('**/radar',r=>r.fulfill({json:{generated_at:new Date().toISOString(),unavailable_sources:['Forecasts'],truncated:true,pins:[{id:'reported',type:'BUILDING_COLLAPSE',category:'ARTIFICIAL',status:'REPORTED',title:'Reported collapse',latitude:-1.28,longitude:36.82,severity:'HIGH',detail:'Pending verification',source:'Example',source_url:'javascript:alert(1)',verification:'Not verified by a coordinator'}]}}));
 await page.goto('/operations-map');await expect(page.getByText('The map is incomplete.',{exact:false})).toBeVisible();
 await page.getByRole('button',{name:/Reported collapse/}).click();await expect(page.getByLabel('Map detail')).toContainText('Not verified by a coordinator');
 await expect(page.getByLabel('Map detail')).not.toContainText('impact radius');
 await page.screenshot({path:'test-results/qa-operations-map.png',fullPage:true});
});
test('briefings choose a resource proposal independently of selected analysis history',async({page})=>{
 await fixtures(page);const id='88888888-8888-4888-8888-888888888888';let body:any;
 await page.route('**/planning/resources*',r=>r.fulfill({json:{items:[{id,kind:'RESOURCE_PLAN',version:1,payload:{status:'DRAFT',title:'Reviewed team proposal'}}]}}));
 await page.route('**/specialised/briefings',r=>{body=r.request().postDataJSON();return r.fulfill({json:{id:'brief',payload:{status:'DRAFT',analysis_type:'BRIEFING',briefing:{body:'Coordinator draft'}}}});});
 await page.goto('/specialised');await page.getByLabel('Capability',{exact:true}).selectOption('briefings');
 await page.getByLabel('Source kind',{exact:true}).selectOption('PROPOSAL');
 const run=page.getByRole('button',{name:'Run Audience briefing',exact:true});await expect(run).toBeDisabled();
 await page.getByLabel('Briefing source',{exact:true}).selectOption(id);await run.click();
 await expect.poll(()=>body?.source_id).toBe(id);expect(body.source_kind).toBe('PROPOSAL');
});
test('weather chat keeps its answer visible when cross-device draft sync conflicts',async({page})=>{
 await fixtures(page);
 await page.route('**/api/me/drafts/**',r=>r.request().method()==='PUT'?r.fulfill({status:409,json:{error:'DRAFT_CONFLICT'}}):r.fulfill({json:null}));
 const result={id:'answer',payload:{status:'DRAFT',coverage:'NO_FORECAST_EVIDENCE',sources:[],narrative:{summary:'Confirm conditions with local officers.',preparedness:[],missing_inputs:[],assumptions:[],source_ids:[]}}};
 await page.route('**/conversations/weather/stream',r=>r.fulfill({contentType:'text/event-stream',body:`event: result\ndata: ${JSON.stringify(result)}\n\n`}));
 await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('What should we prepare for today?');
 await page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true}).click();
 await expect(page.getByText('Another device changed this conversation.',{exact:false})).toBeVisible();
 await expect(page.getByText('Confirm conditions with local officers.',{exact:true})).toBeVisible();
});
