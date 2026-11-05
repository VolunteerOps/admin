import {test,expect} from '@playwright/test';
import {fixtures} from './fixtures';

test('outlook separates probability ranges from heuristic scores using the API contract',async({page},testInfo)=>{
 await fixtures(page);let duplicateSourceRequests=0;
 await page.route('**/sources',r=>{duplicateSourceRequests++;return r.fulfill({json:{sources:[]}});});
 await page.route('**/predictions',r=>r.fulfill({json:{status:'PARTIAL',generated_at:'2026-11-05T10:00:00Z',
  weather_probability_estimates:{estimates:[{event:'Temperature above the heat threshold',threshold:35,unit:'°C',lower:0.25,upper:0.5,latitude:-1.3,longitude:36.8,valid_at:'2026-11-06T12:00:00Z',forecast_init:'2026-11-05T06:00:00Z'}],total:1,truncated:false,selection:'One grid cell and forecast time.'},
  predictions:[{id:'heat',hazard:'HEAT',kind:'PREDICTED',severity:0.7,valid_from:'2026-11-06T00:00:00Z',valid_to:'2026-11-06T23:00:00Z',latitude:-1.3,longitude:36.8,explanation:['High temperature in the available forecast'],sources:['WEATHERNEXT3'],missing_inputs:['Local validation']}],
  coverage_gaps:['River basin impact model'],sources:[{source:'WEATHERNEXT3',label:'WeatherNext 3',status:'OK',observations:24,note:'Stored forecast'}],capabilities:[]}}));
 await page.goto('/weather-analysis');
 await expect(page.getByText('25–50% estimated range',{exact:true})).toBeVisible();
 await expect(page.getByText('Risk score 70/100',{exact:true})).toBeVisible();
 await expect(page.getByText('The risk score ranks indicators; it is not a probability.',{exact:false})).toBeVisible();
 await expect(page.getByText('Still needed: Local validation',{exact:false})).toBeVisible();
 expect(duplicateSourceRequests).toBe(0);
 await page.screenshot({path:testInfo.outputPath('experimental-outlook.png'),fullPage:true});
});

test('an unavailable engine never shows an all-clear empty state',async({page})=>{
 await fixtures(page);
 await page.route('**/predictions',r=>r.fulfill({status:503,json:{error:'SERVICE_UNAVAILABLE',message:'Source unavailable'}}));
 await page.goto('/weather-analysis');
 await expect(page.getByRole('heading',{name:'Regional preparedness outlook'})).toBeVisible();
 await expect(page.getByText('No conditions flagged in the available evidence',{exact:true})).toHaveCount(0);
 await expect(page.getByText('Probability estimates are not available from the current evidence.',{exact:true})).toHaveCount(0);
});
