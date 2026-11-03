import {test} from 'node:test';
import assert from 'node:assert/strict';
import {mkdtempSync,mkdirSync,writeFileSync,readFileSync,existsSync,rmSync,chmodSync} from 'node:fs';
import {tmpdir} from 'node:os';
import path from 'node:path';
import {syncBusiness} from './sync-business.mjs';

function fixture(t){
  const root=mkdtempSync(path.join(tmpdir(),'resq-business-sync-'));
  t.after(()=>{
    assert.equal(path.dirname(root),path.resolve(tmpdir()));
    assert.ok(path.basename(root).startsWith('resq-business-sync-'));
    rmSync(root,{recursive:true,force:true});
  });
  const source=path.join(root,'business/build/dist/js/productionLibrary');
  const target=path.join(root,'web/src/generated/business');
  mkdirSync(source,{recursive:true});
  writeFileSync(path.join(source,'admin-business.mjs'),'export const version = 1;');
  writeFileSync(path.join(source,'admin-business.d.mts'),'export declare const version: number;');
  writeFileSync(path.join(source,'package.json'),'{}');
  return {root,target};
}

for(const platform of ['linux','darwin','win32'])test(`sync launches the wrapper correctly on ${platform} and copies business artifacts`,t=>{
  const {root,target}=fixture(t);
  syncBusiness({platform,projectRoot:root,run(command,args,options){
    assert.equal(command,platform==='win32'?'cmd.exe':'bash');
    if(platform==='win32')assert.deepEqual(args.slice(0,3),['/d','/c','.\\gradlew.bat']);
    else assert.equal(args[0],'gradlew');
    assert.ok(args.includes(':business:jsBrowserProductionLibraryDistribution'));
    assert.ok(args.includes('-PwebOnly=true'));
    assert.equal(options.cwd,root);assert.equal(options.stdio,'inherit');assert.equal(options.shell,false);
    return {status:0};
  }});
  assert.equal(readFileSync(path.join(target,'admin-business.mjs'),'utf8'),'export const version = 1;');
  assert.ok(existsSync(path.join(target,'admin-business.d.mts')));
  assert.equal(existsSync(path.join(target,'package.json')),false);
});

for(const [name,result,pattern,exitCode] of [
  ['permission error',{status:null,error:Object.assign(new Error('spawn EACCES'),{code:'EACCES'})},/Could not start.*EACCES/],
  ['missing executable',{status:null,error:Object.assign(new Error('spawn ENOENT'),{code:'ENOENT'})},/Could not start.*ENOENT/],
  ['termination',{status:null,signal:'SIGTERM'},/stopped by SIGTERM/],
  ['build failure',{status:7},/failed with exit code 7/,7],
])test(`sync reports ${name} and leaves existing generated files intact`,t=>{
  const {root,target}=fixture(t);mkdirSync(target,{recursive:true});
  writeFileSync(path.join(target,'admin-business.mjs'),'previous artifact');
  assert.throws(()=>syncBusiness({projectRoot:root,run:()=>result}),error=>{
    assert.match(error.message,pattern);
    if(exitCode)assert.equal(error.exitCode,exitCode);
    return true;
  });
  assert.equal(readFileSync(path.join(target,'admin-business.mjs'),'utf8'),'previous artifact');
});

test('Unix sync runs a wrapper without execute permission',{skip:process.platform==='win32'},t=>{
  const {root,target}=fixture(t);
  const wrapper=path.join(root,'gradlew');
  writeFileSync(wrapper,'#!/usr/bin/env bash\nexit 0\n');chmodSync(wrapper,0o644);
  syncBusiness({projectRoot:root});
  assert.ok(existsSync(path.join(target,'admin-business.mjs')));
});
