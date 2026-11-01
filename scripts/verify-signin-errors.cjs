/* Synthetic browser fixtures only; all external requests are intercepted. */
const {createRequire}=require('node:module');
const {chromium}=createRequire(require('node:path').resolve(__dirname,'../../backend/package.json'))('playwright');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'msedge'});
 try{
  for(const scenario of ['unavailable','unconfigured','credentials','mfa']){
   const context=await browser.newContext({viewport:{width:900,height:1100}});
   await context.addInitScript(()=>localStorage.setItem('volunteerops_admin_onboarding_v1','1\n'));
   let loginRequests=0;
   await context.route('**/*',async route=>{
    const url=new URL(route.request().url());
    if(url.hostname==='127.0.0.1')return route.continue();
    if(url.pathname==='/sentinel/config'){
     if(scenario==='unavailable')return route.abort('failed');
     return route.fulfill({contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify({firebase:scenario==='unconfigured'?null:{apiKey:'test-api-key',projectId:'resq-test',authDomain:'resq-test.firebaseapp.com',appId:'1:123:web:test'}})});
    }
    if(url.hostname==='identitytoolkit.googleapis.com'&&url.pathname.endsWith(':signInWithPassword')){
     loginRequests++;
     if(scenario==='mfa')return route.fulfill({status:200,contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify({mfaPendingCredential:'fixture-pending',mfaInfo:[{mfaEnrollmentId:'fixture-totp',displayName:'Authenticator',enrolledAt:'2026-10-01T00:00:00Z',totpInfo:{}}]})});
     return route.fulfill({status:400,contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify({error:{code:400,message:'INVALID_LOGIN_CREDENTIALS'}})});
    }
    if(url.pathname.endsWith('/mfaSignIn:finalize')){
     const request=route.request().postDataJSON();assert.equal(request.mfaPendingCredential,'fixture-pending');assert.equal(request.totpVerificationInfo.verificationCode,'123456');
     return route.fulfill({status:400,contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify({error:{code:400,message:'INVALID_CODE'}})});
    }
    return route.abort();
   });
   const page=await context.newPage();
   await page.goto('http://127.0.0.1:4173/',{waitUntil:'domcontentloaded'});
   await page.getByRole('button',{name:'Log in',exact:true}).click({timeout:60000,force:true});
   const inputs=page.getByRole('textbox');
   for(const [index,value] of [[0,'coordinator@example.test'],[1,'fixture-only-password']]){
    const field=inputs.nth(index);await field.click({force:true});await page.keyboard.insertText(value);await page.waitForTimeout(350);
   }
   await page.getByRole('button',{name:'Log in',exact:true}).click({force:true});
   const expected=scenario==='unavailable'?'The sign-in service is unavailable. Please try again later or contact your administrator.':scenario==='unconfigured'?'Sign-in is not configured for this portal. Contact your administrator.':scenario==='mfa'?"Confirm it's you":'Unable to sign in with those details. Check your email and password, or use Forgot password.';
   try{await page.getByText(expected,{exact:true}).waitFor({timeout:15000});}
   catch(e){console.log(JSON.stringify({scenario,loginRequests,text:await page.locator('body').innerText()}));await page.screenshot({path:'.work/signin-failure.png'});throw e;}
   assert.equal(loginRequests,['credentials','mfa'].includes(scenario)?1:0);
   if(scenario==='mfa'){
    await page.getByRole('textbox').first().click({force:true});await page.keyboard.insertText('123456');await page.waitForTimeout(350);
    await page.getByRole('button',{name:'Verify and continue',exact:true}).click({force:true});
    await page.getByText('That verification code is invalid or expired. Enter a current code and try again.',{exact:true}).waitFor({timeout:15000});
   }
   await page.screenshot({path:'.work/signin-'+scenario+'.png'});
   console.log('PASS',scenario);
   await context.close();
  }
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});

