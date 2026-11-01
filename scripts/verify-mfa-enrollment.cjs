/* Browser regression: real Firebase SDK, synthetic responses; no live credentials or enrollment. */
const {createRequire}=require('node:module');
const {chromium}=createRequire(require('node:path').resolve(__dirname,'../../backend/package.json'))('playwright');
const assert=require('node:assert/strict');
const now=Math.floor(Date.now()/1000);
const jwt=[{alg:'none'},{sub:'test-coordinator',user_id:'test-coordinator',aud:'resq-test',iss:'https://securetoken.google.com/resq-test',iat:now,auth_time:now,exp:now+3600,firebase:{sign_in_provider:'password'}},'signature'].map((x,i)=>i===2?x:Buffer.from(JSON.stringify(x)).toString('base64url')).join('.');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'msedge'});
 try{
  for(const scenario of ['invalid-retry','expired','unknown','disabled']){
   const context=await browser.newContext({viewport:{width:1100,height:1200}});
   await context.addInitScript(({jwt})=>{
    localStorage.setItem('volunteerops_admin_onboarding_v1','1\n');
    sessionStorage.setItem('firebase:authUser:test-api-key:resqops',JSON.stringify({uid:'test-coordinator',email:'coordinator@example.test',emailVerified:true,isAnonymous:false,providerData:[],stsTokenManager:{refreshToken:'test-refresh',accessToken:jwt,expirationTime:Date.now()+3600000},apiKey:'test-api-key',appName:'resqops'}));
   },{jwt});
   let starts=0,finalizations=0,enrolled=false,privileged=0;
   await context.route('**/*',async route=>{
    const request=route.request(),url=new URL(request.url());
    if(url.hostname==='127.0.0.1')return route.continue();
    if(url.pathname.startsWith('/api/organizations/')||url.pathname.startsWith('/api/platform/'))privileged++;
    let body={},status=200;
    const fail=message=>{status=400;body={error:{code:400,message}};};
    if(url.pathname.endsWith('/mfaEnrollment:start')){
     starts++;assert.equal(request.postDataJSON().idToken,jwt);
     if(scenario==='disabled')fail('OPERATION_NOT_ALLOWED');
     else body={totpSessionInfo:{sharedSecretKey:'JBSWY3DPEHPK3PXP',hashingAlgorithm:'SHA1',verificationCodeLength:6,periodSec:30,finalizeEnrollmentTime:new Date(Date.now()+600000).toISOString(),sessionInfo:'fixture-session-'+starts}};
    }else if(url.pathname.endsWith('/mfaEnrollment:finalize')){
     finalizations++;
     const payload=request.postDataJSON();
     assert.equal(payload.idToken,jwt);
     assert.deepEqual(payload.totpVerificationInfo,{sessionInfo:'fixture-session-'+starts,verificationCode:'123456'});
     if(scenario==='invalid-retry'&&finalizations===1)fail('INVALID_CODE');
     else if(scenario==='expired')fail('INVALID_SESSION_INFO');
     else if(scenario==='unknown')fail('UNEXPECTED_PROVIDER_FAILURE : PRIVATE_PROVIDER_PAYLOAD');
     else {enrolled=true;body={idToken:jwt,refreshToken:'test-refresh'};}
    }else if(url.hostname==='identitytoolkit.googleapis.com')body={users:[{localId:'test-coordinator',email:'coordinator@example.test',emailVerified:true,providerUserInfo:[],mfaInfo:enrolled?[{mfaEnrollmentId:'fixture-totp',displayName:'ResQ authenticator',enrolledAt:new Date().toISOString(),totpInfo:{}}]:[]}]};
    else if(url.hostname==='securetoken.googleapis.com')body={access_token:jwt,id_token:jwt,refresh_token:'test-refresh',expires_in:'3600',token_type:'Bearer',user_id:'test-coordinator',project_id:'resq-test'};
    else if(url.pathname==='/sentinel/config')body={firebase:{apiKey:'test-api-key',projectId:'resq-test',authDomain:'resq-test.firebaseapp.com',appId:'1:123:web:test'}};
    else if(['/api/auth/me','/api/auth/firebase/bootstrap'].includes(url.pathname))body={user:{id:1,fullName:'Test Coordinator'},memberships:[],platform_access:'DENIED'};
    else if(url.pathname==='/api/me/home')body={profile:{version:1,displayName:'Test Coordinator',region:'Nairobi',skills:[],privateDetails:{}},directory:{version:1}};
    else if(url.pathname==='/api/me/invitations')body=[];
    else if(url.pathname==='/api/me/workspaces')body={items:[]};
    else return route.abort();
    await route.fulfill({status,contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify(body)});
   });
   const page=await context.newPage();await page.goto('http://127.0.0.1:4173/');
   await page.getByRole('button',{name:'Account and security',exact:true}).click({timeout:60000,force:true});
   const security=page.getByRole('button',{name:'Account security',exact:true});await security.waitFor();await page.waitForTimeout(500);await security.click({force:true});
   const start=page.getByRole('button',{name:'Set up authenticator',exact:true});await start.waitFor();await page.waitForTimeout(350);await start.click({force:true});
   if(scenario==='disabled'){
    await page.getByText("This sign-in method is not enabled. Use your organisation's approved method or contact your administrator.",{exact:true}).waitFor();
    assert.equal(await page.getByRole('button',{name:'Confirm authenticator enrollment',exact:true}).count(),0);
    assert.equal(finalizations,0);
   }else{
    const confirm=page.getByRole('button',{name:'Confirm authenticator enrollment',exact:true});await confirm.waitFor();
    await page.waitForTimeout(350);
    await confirm.click({force:true});await page.waitForTimeout(350);
    assert.equal(finalizations,0, 'Empty code must not submit enrollment');
    const enterCode=async()=>{const input=page.getByRole('textbox').nth(1);await input.click({force:true});await page.keyboard.insertText('123 456');await page.waitForTimeout(350);await confirm.click({force:true});};
    await enterCode();
    const expected=scenario==='invalid-retry'?'That verification code is invalid or expired. Enter a current code and try again.':scenario==='expired'?'Authenticator setup has expired. Sign in again, start a new setup and replace the old authenticator entry.':'This request could not be completed. Reference: auth/unexpected-provider-failure.';
    await page.getByText(expected,{exact:true}).waitFor({timeout:15000});
    assert.equal(await page.getByRole('button',{name:'Sign out / switch account',exact:true}).count(),1,'Failed enrollment must keep the account signed in');
    assert.equal(await page.getByRole('button',{name:'Sign in',exact:true}).count(),0);
    assert.equal(await page.getByText(/PRIVATE_PROVIDER_PAYLOAD/).count(),0);
    if(scenario==='invalid-retry'){
     await enterCode();
     await page.getByText('Authenticator enrolled. Sign in again and enter its code.',{exact:true}).waitFor();
     assert.equal(finalizations,2);assert.equal(starts,1);assert.equal(enrolled,true);
     assert.equal(await page.getByRole('button',{name:'Sign in',exact:true}).count(),1);
     assert.equal(await page.getByRole('button',{name:'Confirm authenticator enrollment',exact:true}).count(),0);
    }else{
     await page.getByRole('button',{name:'Restart authenticator setup',exact:true}).click({force:true});
     await page.getByText('Add this setup key to your authenticator app, then enter its current six-digit code.',{exact:true}).waitFor();
     assert.equal(starts,2);
    }
   }
   assert.equal(privileged,0);console.log('PASS MFA enrollment',scenario);await context.close();
  }
 }finally{await browser.close();}
})().catch(e=>{console.error(e.stack);process.exitCode=1;});
