import {test, expect} from '@playwright/test';
import {fixtures} from './fixtures';

test('untouched forms do not save drafts; returning to defaults removes the draft', async({page})=>{
  await fixtures(page); let writes=0, deletes=0;
  await page.route('**/api/me/drafts/**', r=>{
    if(r.request().method()==='PUT') { writes++; return r.fulfill({json:{version:writes,payload:r.request().postDataJSON().payload}}); }
    if(r.request().method()==='DELETE')deletes++;
    return r.fulfill({json:null});
  });
  await page.goto('/campaigns'); await page.getByRole('button',{name:'New campaign',exact:true}).click();
  await expect(page.getByRole('button',{name:'Save draft',exact:true})).toBeDisabled();
  await page.getByLabel('Close panel').click();
  await page.getByRole('button',{name:'New campaign',exact:true}).click();
  expect(writes).toBe(0); await expect(page.getByText('Resume draft',{exact:true})).toHaveCount(0);
  await page.getByLabel('Campaign name',{exact:true}).fill('Readiness checks');
  await expect.poll(()=>writes).toBe(1);
  await page.getByLabel('Campaign name',{exact:true}).fill('');
  await expect.poll(()=>deletes).toBe(1);
  await expect(page.getByRole('button',{name:'Save draft',exact:true})).toBeDisabled();
  await expect(page.getByText('Resume draft',{exact:true})).toHaveCount(0);
});

test('sidebar fits a laptop, Pro badge uses papaya and favicon uses the brand logo',async({page})=>{
  await fixtures(page); await page.setViewportSize({width:1366,height:768}); await page.goto('/specialised');
  const nav=page.getByRole('navigation',{name:'Main navigation'});
  await expect(nav).toBeVisible();
  expect(await nav.evaluate(el=>el.scrollHeight<=el.clientHeight)).toBe(true);
  await expect(page.locator('.sidebar-footnote')).toHaveCount(0);
  await expect(page.locator('.sidebar-pro')).toHaveCSS('background-color','rgb(228, 208, 118)');
  await expect(page.locator('link[rel="icon"]')).toHaveAttribute('href','/logo-monochrome.png');
  await expect(page.locator('.page-heading .eyebrow')).toHaveCount(0);
  await expect(page.locator('.page-folder > .tabs')).toHaveCSS('background-color','rgba(0, 0, 0, 0)');
  await expect(page.locator('.page-folder-content')).toHaveCSS('background-color','rgb(255, 255, 255)');
  const secondary=page.getByRole('group',{name:'Specialised sections'});
  await expect(secondary).toHaveCSS('border-radius','12px');
  await page.screenshot({path:test.info().outputPath('workspace-folder-desktop.png'),fullPage:true});
  await page.evaluate(()=>window.scrollTo(0,600));
  await expect(page.locator('.topbar')).toHaveClass(/is-scrolled/);
  expect((await page.locator('.topbar').boundingBox())!.y).toBe(0);
});

for(const width of [1440,1024,768,390])test(`Sentinel chat adapts at ${width}px and preserves unsent text when closed`,async({page})=>{
  await fixtures(page); await page.setViewportSize({width,height:1000}); await page.goto('/sentinel');
  await page.locator('#ask-sentinel-toggle').click();
  const chat=page.getByRole('complementary',{name:'Sentinel assistant'});
  await expect(chat).toBeVisible();
  const bounds=await chat.boundingBox(), map=await page.locator('.sentinel-map-column').boundingBox();
  if(width>=768)expect(bounds!.x).toBeGreaterThanOrEqual(map!.x+map!.width);
  else expect(bounds!.y).toBeGreaterThanOrEqual(map!.y+map!.height);
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
  await page.getByLabel('Ask about your region').fill('What should we prepare for this week?');
  await page.getByLabel('Close Sentinel assistant').click(); await expect(chat).toBeHidden();
  await page.locator('#ask-sentinel-toggle').click();
  await expect(page.getByLabel('Ask about your region')).toHaveValue('What should we prepare for this week?');
  if(width===1440||width===768)await page.screenshot({path:test.info().outputPath(`workspace-sentinel-${width}.png`),fullPage:true});
  if(width===1440){await page.getByLabel('Switch theme').click();await expect(page.locator('.radar-chip.active').first()).toHaveCSS('background-color','rgb(27, 52, 84)');await page.screenshot({path:test.info().outputPath('workspace-sentinel-dark.png'),fullPage:true});}
});
