// Run only from manage.ps1 after it restricts the private directory's Windows ACL.
import {readFileSync,writeFileSync,existsSync} from 'node:fs';
import {join} from 'node:path';
import {randomBytes} from 'node:crypto';
import {spawnSync} from 'node:child_process';
import {signature} from './server.mjs';

const [action,directory,railway,url] = process.argv.slice(2);
const file=join(directory,'credentials.json');
const scope=['-p','73690bd6-2d68-4ac7-9330-b5da3ebed8f0','-e','9de4cdd7-abad-4a36-ae1e-7577c06dc2d7','-s','fb8c493f-bcdd-4883-acb3-9b0990b935ea'];
function cli(args,input) {
  const result=spawnSync(railway,args,{input,encoding:'utf8',windowsHide:true,timeout:90000,maxBuffer:4*1024*1024,
    env:{...process.env,RAILWAY_CALLER:'skill:use-railway@1.7.0',RAILWAY_AGENT_SESSION:'muse-workaround-20261104'}});
  // Never print stdout/stderr from secret-bearing CLI commands.
  if(result.error||result.status!==0) throw new Error('Railway operation failed. Check railway whoami and staging access. No credentials were printed.');
  return result.stdout;
}
function config(){return JSON.parse(readFileSync(file,'utf8'));}
function save(value){writeFileSync(file,JSON.stringify(value),{mode:0o600});}
function set(name,value,skip=true){cli(['variable','set',...scope,name,'--stdin',...(skip?['--skip-deploys']:[])],value);}
try {
  if(action==='prepare') {
    const variables=JSON.parse(cli(['variables',...scope,'--json']));
    const key=variables.META_MODEL_API_KEY||variables.MODEL_API_KEY;
    if(!key)throw new Error('The staging API has no readable Meta key.');
    const previous=existsSync(file)?config():{};
    save({...previous,key,secret:previous.secret||randomBytes(32).toString('base64url'),port:4187});
    console.log('Private gateway credentials prepared.');
  } else if(action==='connect'||action==='stage') {
    const parsed=new URL(url);
    if(parsed.protocol!=='https:'||!parsed.hostname.endsWith('.trycloudflare.com')||parsed.pathname!=='/'||parsed.search||parsed.hash||parsed.username||parsed.password)throw new Error('Unexpected temporary tunnel URL.');
    const settings=config();
    const response=await fetch(new URL('/health',url),{signal:AbortSignal.timeout(15000)});
    if(!response.ok||(await response.json()).status!=='ready')throw new Error('The tunnel is not ready.');
    const unauth=await fetch(new URL('/v1/chat/completions',url),{method:'POST',body:'{}',signal:AbortSignal.timeout(15000)});
    if(unauth.status!==401)throw new Error('The public endpoint did not reject an unsigned request.');
    // A correctly signed, intentionally invalid model verifies both tunnel and HMAC without a paid provider call.
    const body=JSON.stringify({model:'gateway-preflight'}),timestamp=String(Date.now()),nonce=randomBytes(16).toString('hex');
    const signed=await fetch(new URL('/v1/chat/completions',url),{method:'POST',body,headers:{'Content-Type':'application/json','x-resq-timestamp':timestamp,'x-resq-nonce':nonce,'x-resq-signature':signature(settings.secret,timestamp,nonce,body)},signal:AbortSignal.timeout(15000)});
    if(signed.status!==400||(await signed.json()).error?.code!=='GATEWAY_INVALID_REQUEST')throw new Error('The signed gateway check failed.');
    set('META_GATEWAY_SECRET',settings.secret);
    set('META_GATEWAY_URL',parsed.origin,action==='stage');
    save({...settings,url:parsed.origin});
    console.log(action==='stage'?'Gateway variables staged without a deployment. Deploy the updated API next.':'Gateway connected. Railway is redeploying the API; allow a few minutes.');
  } else if(action==='disconnect') {
    const variables=JSON.parse(cli(['variables',...scope,'--json']));
    for(const name of ['META_GATEWAY_URL','META_GATEWAY_SECRET'])if(variables[name]!==undefined)set(name,'',name==='META_GATEWAY_URL');
    console.log('Gateway disabled in Railway. The API is redeploying to use Meta directly.');
  } else throw new Error('Unknown gateway command.');
} catch(error) {console.error(error.message?.startsWith('Railway')?error.message:'Gateway setup failed. Check connectivity, local state, and staging configuration; secrets were not printed.');process.exitCode=1;}
