import {test,expect} from '@playwright/test';
import {fixtures} from './fixtures';
import {friendlyError,friendlyErrors,validationMessage,UserFacingError} from '../src/lib/friendly-errors';

test('backend validation details cannot bypass the plain-language error map',()=>{
 const text=friendlyError('validation_failed','Request payload validation failed',400,[{path:'',code:'custom',message:'Forecast initialization must be on a UTC hour'}]);
 expect(text).toContain('18:00 rather than 18:35');expect(text).not.toMatch(/initialization|UTC|payload/);
 expect(validationMessage([{path:'mission.capacity',code:'invalid_type',expected:'integer',message:'Expected integer, received float'}])).toContain('whole number');
 expect(validationMessage([{path:'reason',code:'too_small',minimum:10,type:'string',message:'String must contain at least 10 character(s)'}])).toContain('at least 10 characters');
 expect(validationMessage([{path:'internal_database_key',code:'custom',message:'violates constraint private_schema.tenant_id'}])).not.toMatch(/schema|constraint|internal_database_key/);
 expect(validationMessage([{path:'',code:'custom',message:'Exact public DNS domain required'}])).toContain('without https://');
});

test('unknown errors are not echoed, while known errors and local form instructions are useful',()=>{
 expect(()=>friendlyError(500 as any,{} as any)).not.toThrow();expect(()=>validationMessage([null])).not.toThrow();
 for(const status of [0,400,409,422,500])expect(friendlyError('UNRECOGNIZED_PROVIDER_CODE','sentinel_forecast_init_time constraint violation',status)).not.toMatch(/sentinel|constraint|UNRECOGNIZED/);
 expect(friendlyError('PROPOSAL_EXPIRED','')).toContain('Generate a new plan');
 expect(friendlyError('MODEL_UNAVAILABLE','model_not_found')).toContain('Your message is still here');
 expect(friendlyError('META_ACCESS_DENIED','invalid_request_error')).toContain('check the Meta connection');
 expect(friendlyError('auth/expired-action-code','Firebase: expired-action-code')).toContain('Request a new link');
 expect(new UserFacingError('Choose the receiving coordinator.').message).toContain('coordinator who will take over');
 for(const message of Object.values(friendlyErrors))expect(message).not.toMatch(/\b(?:idempotency|UTC|initialization|payload|tenant|schema|constraint|lease|fence|tiling)\b/i);
});

test('forecast requests default to automatic publication selection without a timestamp',async({page})=>{
 const requests:any[]=[];await fixtures(page);
 await page.route('**/forecasts/refresh-latest',async r=>{requests.push(r.request().postDataJSON());await r.fulfill({status:202,json:{job_id:'forecast-automatic',state:'PENDING',init_time:'2026-11-01T06:00:00Z',from_hour:4,to_hour:27}});});
 await page.goto('/weather-analysis');await page.getByRole('button',{name:'Request latest forecast',exact:true}).click();
 await expect(page.getByLabel('How far ahead?')).toHaveValue('24');await expect(page.locator('input[type="datetime-local"]')).toHaveCount(0);await page.screenshot({path:'../designs/next-portal/forecast-automatic.png',fullPage:true});
 await page.getByRole('button',{name:'Get latest forecast',exact:true}).click();await expect(page.getByRole('dialog')).toHaveCount(0);
 expect(requests).toEqual([{hours:24}]);await expect(page.getByText('Your forecast is being prepared.',{exact:false})).toBeVisible();
});

test('specific forecasts explain invalid minutes before sending a request',async({page})=>{
 await fixtures(page);let sent=0;page.on('request',r=>{if(r.url().endsWith('/forecasts/refresh'))sent++;});
 await page.goto('/weather-analysis');await page.getByRole('button',{name:'Request latest forecast',exact:true}).click();await page.getByRole('button',{name:'Choose a specific forecast'}).click();
 await page.getByLabel('Forecast publication date and time',{exact:true}).fill('2026-10-31T18:35');await page.getByRole('button',{name:'Request this forecast'}).click();
 await expect(page.getByText(/The minutes must be 00/)).toBeVisible();expect(sent).toBe(0);await page.screenshot({path:'../designs/next-portal/forecast-plain-error.png',fullPage:true});
});

test('legacy API validation and unavailable automatic lookup show actionable text',async({page})=>{
 await fixtures(page);await page.route('**/forecasts/refresh-latest',r=>r.fulfill({status:503,json:{error:'WEATHER_ACCESS_DENIED',message:'403 accessDenied on project dataset'}}));
 await page.goto('/weather-analysis');await page.getByRole('button',{name:'Request latest forecast',exact:true}).click();await page.getByRole('button',{name:'Get latest forecast'}).click();
 await expect(page.getByText('ResQ cannot read this weather data yet.',{exact:false})).toBeVisible();
 await page.getByRole('button',{name:'Choose a specific forecast'}).click();
 await page.route('**/forecasts/refresh',r=>r.fulfill({status:400,json:{error:'validation_failed',message:'Request payload validation failed',details:[{path:'init_time',message:'Forecast initialization must be on a UTC hour',code:'custom'}]}}));
 await page.getByLabel('Forecast publication date and time',{exact:true}).fill('2026-10-31T18:00');await page.getByRole('button',{name:'Request this forecast'}).click();
 await expect(page.getByText(/The minutes must be 00/)).toBeVisible();await expect(page.getByText('Forecast initialization must be on a UTC hour',{exact:true})).toHaveCount(0);

});
