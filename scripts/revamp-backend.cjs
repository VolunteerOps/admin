const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../backend');
const write = (name, value) => fs.writeFileSync(path.join(root, name), value);
const edit = (name, before, after) => {
  const file = path.join(root, name), value = fs.readFileSync(file, 'utf8');
  if (value.includes(after)) return;
  if (!value.includes(before)) throw Error('Patch context not found: ' + name);
  fs.writeFileSync(file, value.replace(before, after));
};
write('src/modules/operations/workspace-policy.ts', `import type { Prisma } from '../../generated/prisma/client.js';
import { ApiError } from '../../core/errors.js';
import type { listSchema } from './operations.schema.js';
import type { z } from 'zod';

export const reviewLanes = [
 {key:'reports',kind:'REPORT',statuses:['ACTION_REQUIRED','OPEN','REVIEWING'],destination:'Reports',action:'Review field report'},
 {key:'applications',kind:'APPLICATION',statuses:['APPLIED','WAITLISTED'],destination:'Applications',action:'Review application'},
 {key:'hours',kind:'HOURS',statuses:['PENDING'],destination:'Hours',action:'Approve contribution'},
 {key:'training',kind:'ENROLLMENT',statuses:['AWAITING_REVIEW'],destination:'Learning',action:'Verify training'},
 {key:'support',kind:'SUPPORT',statuses:['OPEN','IN_PROGRESS'],destination:'Support',action:'Follow up case'},
] as const;
export function requireCoordinator(role:string) {
 if(!['ORG_ADMIN','COORDINATOR'].includes(role)) throw ApiError.forbidden('Coordinator membership required','CAPABILITY_REQUIRED');
}
export function statusWhere(statuses:readonly string[]):Prisma.OperationRecordWhereInput {
 return {OR:statuses.map(status=>({payload:{path:['status'],equals:status}}))};
}
export function recordWhere(org:string,region:string,userId:number,role:string,input:z.infer<typeof listSchema>):Prisma.OperationRecordWhereInput {
 const personal=['APPLICATION','ASSIGNMENT','HOURS','SUPPORT','ENROLLMENT','CREDENTIAL'].includes(input.kind)&&role==='VOLUNTEER';
 const filters:Prisma.OperationRecordWhereInput[]=[];
 if(input.status)filters.push(statusWhere([input.status]));
 if(input.q)filters.push({OR:['title','body','brief','mission_title','skill','location'].map(field=>({payload:{path:[field],string_contains:input.q,mode:'insensitive'}}))});
 // Keep scope outside OR groups so neither text nor status can broaden access.
 return {organizationId:org,regionId:region,kind:input.kind,sequence:{gt:input.after},...(personal?{ownerId:userId}:{}),...(input.parent_id?{parentId:input.parent_id}:{}),...(filters.length?{AND:filters}:{})};
}
`);
write('src/modules/operations/workspace.service.ts', `import { withRegion } from '../sentinel/sentinel.context.js';
import type { Actor } from '../organizations/context.js';
import { reviewLanes, requireCoordinator, statusWhere } from './workspace-policy.js';

/** A bounded decision queue. Counts are current scoped state, never fabricated trends. */
export async function coordinationOverview(actor:Actor,organizationId:string,regionId:string) {
 return withRegion(actor,organizationId,regionId,false,async tx=>{
  const membership=await tx.organizationMembership.findUniqueOrThrow({where:{organizationId_userId:{organizationId,userId:actor.userId}}});
  requireCoordinator(membership.role);
  const scope={organizationId,regionId};
  const lanes=await Promise.all(reviewLanes.map(async lane=>{
   const where={...scope,kind:lane.kind,...statusWhere(lane.statuses)};
   const [count,items]=await Promise.all([
    tx.operationRecord.count({where}),
    tx.operationRecord.findMany({where,orderBy:{sequence:'asc'},take:4}),
   ]);
   return {key:lane.key,count,destination:lane.destination,action:lane.action,items};
  }));
  const missionWhere={...scope,kind:'MISSION',...statusWhere(['STAGED','ACTIVE','PAUSED'])};
  const [missions,active,staged,checkedIn,dispatch,events]=await Promise.all([
   tx.operationRecord.findMany({where:missionWhere,orderBy:{sequence:'desc'},take:8}),
   tx.operationRecord.count({where:{...scope,kind:'MISSION',...statusWhere(['ACTIVE'])}}),
   tx.operationRecord.count({where:{...scope,kind:'MISSION',...statusWhere(['STAGED'])}}),
   tx.operationRecord.count({where:{...scope,kind:'ASSIGNMENT',...statusWhere(['CHECKED_IN'])}}),
   tx.operationRecord.findFirst({where:{...scope,kind:'DISPATCH_CONTROL'}}),
   tx.operationRecord.findMany({where:{...scope,kind:'EVENT'},orderBy:{sequence:'desc'},take:6}),
  ]);
  // Filter event targets using the same RLS-visible set as other operational reads.
  const ids=events.map(e=>(e.payload as Record<string,unknown>).record_id).filter((v):v is string=>typeof v==='string');
  const targets=await tx.operationRecord.findMany({where:{...scope,id:{in:ids}},select:{id:true}});
  const allowed=new Set(targets.map(r=>r.id));
  return {as_of:new Date().toISOString(),counts:{active_missions:active,staged_missions:staged,checked_in:checkedIn,awaiting_review:lanes.reduce((sum,lane)=>sum+lane.count,0)},lanes,missions,activity:events.filter(e=>allowed.has((e.payload as Record<string,unknown>).record_id as string)),dispatch:dispatch?.payload??{paused:false}};
 });
}
`);
edit('src/modules/operations/operations.schema.ts', 'parent_id:scopeId.optional(),q:', "parent_id:scopeId.optional(),status:z.string().regex(/^[A-Z_]{2,40}$/).optional(),q:");
edit('src/modules/operations/operations.service.ts', "import {assertDispatchAllowed}", "import {recordWhere} from './workspace-policy.js';\nimport {assertDispatchAllowed}");
const serviceFile=path.join(root,'src/modules/operations/operations.service.ts');
let service=fs.readFileSync(serviceFile,'utf8');
const old=service.split(/\r?\n/).find(line=>line.includes('const items=await tx.operationRecord.findMany')&&line.includes('input.q'));
if(old)service=service.replace(old,"  const items=await tx.operationRecord.findMany({where:recordWhere(org,region,actor.userId,membership,input),orderBy:{sequence:'asc'},take:101});");
service=service.replace("  const personal=['APPLICATION','ASSIGNMENT','HOURS','SUPPORT','ENROLLMENT','CREDENTIAL'].includes(input.kind)&&membership==='VOLUNTEER';",'');
fs.writeFileSync(serviceFile,service);
edit('src/modules/operations/operations.controller.ts', "import {operationalMetrics", "import {coordinationOverview} from './workspace.service.js';\nimport {operationalMetrics");
edit('src/modules/operations/operations.controller.ts', "router.get('/dispatch'", "router.get('/overview',ah(async(req,res)=>res.json(serialize(await coordinationOverview(requestActor(req),scopeId.parse(req.params.orgId),scopeId.parse(req.params.regionId))))));\nrouter.get('/dispatch'");
write('tests/workspace-policy.test.ts', `import {test} from 'node:test';
import assert from 'node:assert/strict';
import {listSchema} from '../src/modules/operations/operations.schema.js';
import {recordWhere,requireCoordinator,reviewLanes} from '../src/modules/operations/workspace-policy.js';
test('search and status preserve organization, region, owner and mission scope',()=>{
 const input=listSchema.parse({kind:'APPLICATION',q:'water',status:'APPLIED',parent_id:'00000000-0000-4000-8000-000000000003'});
 const where=recordWhere('org-a','region-a',7,'VOLUNTEER',input);
 assert.equal(where.organizationId,'org-a');assert.equal(where.regionId,'region-a');assert.equal(where.ownerId,7);assert.equal(where.parentId,input.parent_id);
 assert.equal((where.AND as unknown[]).length,2);
 assert.equal(recordWhere('org-a','region-a',7,'COORDINATOR',input).ownerId,undefined);
});
test('operational queue is coordinator-only and excludes completed records',()=>{
 for(const role of ['VOLUNTEER','PLATFORM_ADMIN',''])assert.throws(()=>requireCoordinator(role),{code:'CAPABILITY_REQUIRED'});
 for(const role of ['ORG_ADMIN','COORDINATOR'])assert.doesNotThrow(()=>requireCoordinator(role));
 for(const lane of reviewLanes)for(const status of lane.statuses)assert.ok(!['COMPLETED','RESOLVED','APPROVED','ACCEPTED'].includes(status));
});
test('list filters reject arbitrary fields and malformed statuses',()=>{
 for(const change of [{status:'ACTIVE OR 1=1'},{status:'x'.repeat(100)},{ownerId:2},{organizationId:'other'},{q:'x'},{q:'x'.repeat(101)}])assert.equal(listSchema.safeParse({kind:'MISSION',...change}).success,false);
 assert.equal(listSchema.safeParse({kind:'MISSION',status:'ACTIVE',q:'Water'}).success,true);
});
`);
console.log('Updated scoped coordination overview, search and status contracts.');
