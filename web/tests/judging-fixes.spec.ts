import {test,expect} from '@playwright/test';
import {fixtures,stockRow} from './fixtures';
test('resource proposals use named links and show shortages without opening raw JSON',async({page})=>{
 await fixtures(page);
 const resource={...stockRow,payload:{...stockRow.payload,title:'Toyota Land Cruiser',vehicle_model:'Toyota Land Cruiser',plate_number:'DEMO 001'}};
 await page.route('**/records?kind=INVENTORY',r=>r.fulfill({json:{items:[resource],next_cursor:null}}));
 await page.route('**/planning/resources',r=>r.fulfill({json:{items:[{id:'plan',version:1,payload:{title:'Tana River staffing & transport',status:'RECORDED',demo:true,model:'muse-spark-1.3',snapshot:{candidates:[{user_id:91,display_name:'Amina Wanjiku'}]},recommendation:{summary:'Prepare the team.',allocations:[{user_id:91,reason:'Qualified first aider',supplies:[{resource_id:resource.id,quantity:1,reason:'Transport'}]}],shortages:['Vehicle '+resource.id+' is reserved.'],decisions:[]},harness:{notes:['Eligible volunteers 91; vehicle '+resource.id+' unavailable.']}}}],next_cursor:null}}));
 await page.goto('/resource-planning');await page.getByRole('button',{name:/Tana River staffing/}).click();
 const dialog=page.getByRole('dialog');await expect(dialog.getByRole('link',{name:'Amina Wanjiku'}).first()).toHaveAttribute('href','/people?user=91');
 await expect(dialog.getByRole('link',{name:'Toyota Land Cruiser · DEMO 001'}).first()).toHaveAttribute('href','/resources?record='+resource.id);
 await expect(dialog).toContainText('Shortages to resolve');await expect(dialog).not.toContainText(resource.id);
});
test('resource deep links open details even when the record is outside the first page',async({page})=>{
 await fixtures(page);await page.route('**/records?*',r=>r.fulfill({json:{items:[],next_cursor:null}}));
 await page.goto('/resources?record='+stockRow.id);
 await expect(page.getByRole('dialog')).toBeVisible();await expect(page.getByRole('dialog')).toContainText(stockRow.payload.title);
});
test('known legacy punctuation is repaired in Sentinel responses',async({page})=>{
 await fixtures(page);await page.route('**/conversations/weather/stream',r=>r.fulfill({contentType:'text/event-stream',body:'event: result\ndata: '+JSON.stringify({payload:{narrative:{summary:'River level â€” check locally. Temperature 30Â°C.',preparedness:[],missing_inputs:[],assumptions:[],source_ids:[]},sources:[]}})+'\n\n'}));
 await page.goto('/sentinel?ask=1');await page.getByLabel('Ask about your region').fill('What changed?');await page.locator('.weather-chat').getByRole('button',{name:'Ask Sentinel',exact:true}).click();
 await expect(page.locator('.conversation')).toContainText('River level — check locally. Temperature 30°C.');
});
