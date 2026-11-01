/* Synthetic browser fixtures only; verifies security access without workspace admission. */
const {createRequire}=require('node:module');
const {chromium}=createRequire(require('node:path').resolve(__dirname,'../../backend/package.json'))('playwright');
const assert=require('node:assert/strict');
const jwt=[{alg:'none'},{sub:'test-coordinator',user_id:'test-coordinator',aud:'resq-test',iss:'https://securetoken.google.com/resq-test',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,firebase:{sign_in_provider:'password'}},'signature'].map((x,i)=>i===2?x:Buffer.from(JSON.stringify(x)).toString('base64url')).join('.');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'msedge'});
 try{
  const context=await browser.newContext({viewport:{width:1000,height:1100}});
  await context.addInitScript(({jwt})=>{
   localStorage.setItem('volunteerops_admin_onboarding_v1','1\n');
   sessionStorage.setItem('firebase:authUser:test-api-key:resqops',JSON.stringify({uid:'test-coordinator',email:'coordinator@example.test',emailVerified:true,isAnonymous:false,providerData:[],stsTokenManager:{refreshToken:'test-refresh',accessToken:jwt,expirationTime:Date.now()+3600000},apiKey:'test-api-key',appName:'resqops'}));
  },{jwt});
  let privilegedRequests=0;
  await context.route('**/*',async route=>{
   const url=new URL(route.request().url());if(url.hostname==='127.0.0.1')return route.continue();
   if(url.pathname.startsWith('/api/organizations/')||url.pathname.startsWith('/api/platform/'))privilegedRequests++;
   let body={};
   if(url.hostname==='identitytoolkit.googleapis.com')body={users:[{localId:'test-coordinator',email:'coordinator@example.test',emailVerified:true,providerUserInfo:[]}]};
   else if(url.hostname==='securetoken.googleapis.com')body={access_token:jwt,id_token:jwt,refresh_token:'test-refresh',expires_in:'3600',token_type:'Bearer',user_id:'test-coordinator',project_id:'resq-test'};
   else if(url.pathname==='/sentinel/config')body={firebase:{apiKey:'test-api-key',projectId:'resq-test',authDomain:'resq-test.firebaseapp.com',appId:'1:123:web:test'}};
   else if(['/api/auth/me','/api/auth/firebase/bootstrap'].includes(url.pathname))body={user:{id:1,fullName:'Test Coordinator'},memberships:[],platform_access:'DENIED'};
   else if(url.pathname==='/api/me/home')body={profile:{version:1,displayName:'Test Coordinator',region:'Nairobi',skills:[],privateDetails:{}},directory:{version:1}};
   else if(url.pathname==='/api/me/invitations')body=[];
   else if(url.pathname==='/api/me/workspaces')body={items:[]};
   else return route.abort();
   await route.fulfill({contentType:'application/json',headers:{'access-control-allow-origin':'*'},body:JSON.stringify(body)});
  });
  const page=await context.newPage();await page.goto('http://127.0.0.1:4173/');
  await page.getByRole('button',{name:'Account and security',exact:true}).click({timeout:60000,force:true});
  const security=page.getByRole('button',{name:'Account security',exact:true});
  await security.waitFor({timeout:20000});await page.waitForTimeout(500);
  await security.click({force:true});
  try{await page.getByRole('button',{name:'Set up authenticator',exact:true}).waitFor({timeout:10000});}
  catch(e){await page.screenshot({path:'.work/pending-account-failure.png'});throw e;}
  assert.equal(privilegedRequests,0);
  await page.getByRole('button',{name:'Back to workspace access',exact:true}).click({force:true});
  await page.getByRole('button',{name:'Check access again',exact:true}).waitFor();
  assert.equal(await page.getByText('Open my workspace',{exact:true}).count(),0);
  console.log('PASS pending account can reach security and return without workspace admission');
 }finally{await browser.close();}
})().catch(e=>{console.error(e.message);process.exitCode=1;});

