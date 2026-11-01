import ts from '../web/node_modules/typescript/lib/typescript.js';
import {readdirSync,readFileSync,writeFileSync} from 'node:fs';import path from 'node:path';
process.chdir((await import('node:url')).fileURLToPath(new URL('../',import.meta.url)));
const errors=[];const validations=[];
function walk(dir){for(const d of readdirSync(dir,{withFileTypes:true})){if(d.name==='generated')continue;const p=path.join(dir,d.name);if(d.isDirectory())walk(p);else if(p.endsWith('.ts')){const src=ts.createSourceFile(p,readFileSync(p,'utf8'),ts.ScriptTarget.Latest,true);const lit=n=>n&&(ts.isStringLiteral(n)||ts.isNoSubstitutionTemplateLiteral(n))?n.text:null;function visit(n){let code,message;if(ts.isNewExpression(n)&&n.expression.getText(src)==='ApiError'){code=lit(n.arguments?.[1]);message=lit(n.arguments?.[2]);}if(ts.isCallExpression(n)&&/^ApiError\./.test(n.expression.getText(src))){const method=n.expression.getText(src).split('.')[1];const defaults={badRequest:'validation_failed',unauthorized:'unauthorized',forbidden:'forbidden',notFound:'not_found',conflict:'conflict',internal:'internal_error'};code=lit(n.arguments[method==='badRequest'?2:1])||defaults[method];message=lit(n.arguments[0]);}if(code)errors.push({code,message,file:p.replaceAll('\\','/'),line:src.getLineAndCharacterOfPosition(n.getStart()).line+1});if(ts.isPropertyAssignment(n)&&n.name.getText(src)==='message'&&lit(n.initializer))validations.push({message:lit(n.initializer),file:p.replaceAll('\\','/'),line:src.getLineAndCharacterOfPosition(n.getStart()).line+1});ts.forEachChild(n,visit);}visit(src);}}}
walk('../backend/src');writeFileSync('docs/PORTAL_ERROR_AUDIT.json',JSON.stringify({errors,validationMessages:validations},null,2)+'\n');const inventory={};for(const e of errors)(inventory[e.code]??=[]).push(e.file);
for(const code of Object.keys(inventory))inventory[code]=[...new Set(inventory[code])];
writeFileSync('docs/PORTAL_ERROR_INVENTORY.json',JSON.stringify(inventory,null,2)+'\n');
console.log(`${new Set(errors.map(x=>x.code)).size} error codes across ${errors.length} backend error sites.`);

const {friendlyErrors}=await import('../web/src/lib/friendly-errors.ts');
const unmapped=Object.keys(inventory).filter(code=>code!=='validation_failed'&&!friendlyErrors[code]);
if(unmapped.length)throw Error('Missing plain-language messages: '+unmapped.join(', '));
await import('./update-error-map.mjs');
