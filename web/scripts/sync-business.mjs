import {spawnSync} from 'node:child_process';
import {readdirSync,copyFileSync,mkdirSync} from 'node:fs';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
const root=fileURLToPath(new URL('../../',import.meta.url));
export function syncBusiness({platform=process.platform,run=spawnSync,projectRoot=root}={}) {
  // Git stores gradlew without execute permission. Match CI's explicit Bash invocation.
  const windows=platform==='win32';
  const command=windows?'cmd.exe':'bash';
  const args=[...(windows?['/d','/c','.\\gradlew.bat']:['gradlew']),':business:jsBrowserProductionLibraryDistribution','-PwebOnly=true','--no-daemon','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx1000M','-Pkotlin.compiler.execution.strategy=in-process','--console=plain'];
  const result=run(command,args,{cwd:projectRoot,stdio:'inherit',shell:false});
  if(result.error)throw new Error(`Could not start the Kotlin business build (${result.error.code??'spawn error'}): ${result.error.message}`,{cause:result.error});
  if(result.signal)throw new Error(`Kotlin business build was stopped by ${result.signal}.`);
  if(result.status!==0){
    const error=new Error(`Kotlin business build failed with exit code ${result.status??'unknown'}. See the Gradle output above.`);
    error.exitCode=result.status||1;
    throw error;
  }
  const source=path.join(projectRoot,'business/build/dist/js/productionLibrary'),target=path.join(projectRoot,'web/src/generated/business');
  mkdirSync(target,{recursive:true});
  for(const file of readdirSync(source))if(file.endsWith('.mjs')||file.endsWith('.d.mts'))copyFileSync(path.join(source,file),path.join(target,file));
  console.log('Kotlin business module synchronized.');
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
  try{syncBusiness();}
  catch(error){console.error(error.message);process.exitCode=error.exitCode||1;}
}
