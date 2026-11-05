import Link from "next/link";
import { Fragment } from "react";
import { displayText, resourceName } from "@/lib/display-text";
export type PlanningReferences = { people: Record<string,string>; resources: Record<string,string>; missions?: Record<string,string> };
export function ReferenceText({text,references}:{text:unknown;references:PlanningReferences}) {
 const value=displayText(text);
 // Numeric quantities remain numbers. Only explicitly identified people and known UUIDs are linked.
 const personPattern=/(?:volunteers?|responders?|users?|user_ids?|excluded ineligible|IDs?)\s*[:#(]?\s*\d+(?:\s*,\s*\d+)*\)?/i;
 const parts=value.split(new RegExp('('+personPattern.source+'|[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})','gi'));
 return <>{parts.map((part,i)=>references.resources[part]?<Link key={i} href={'/resources?record='+encodeURIComponent(part)}>{references.resources[part]}</Link>:references.missions?.[part]?<Link key={i} href={'/missions?record='+encodeURIComponent(part)}>{references.missions[part]}</Link>:personPattern.test(part)?<Fragment key={i}>{part.replace(/,\s*/g,', ').split(/(\d+)/).map((token,j)=>references.people[token]?<Link key={j} href={'/people?user='+token}>{references.people[token]}</Link>:token)}</Fragment>:<Fragment key={i}>{part}</Fragment>)}</>;
}
export function planningReferences(snapshot:any,roster:any[],inventory:any[]):PlanningReferences {
 return {people:Object.fromEntries([...(snapshot?.candidates||[]).map((p:any)=>[String(p.user_id),p.display_name||'Volunteer']),...roster.map(p=>[String(p.userId),p.user?.fullName||'Volunteer'])]),resources:Object.fromEntries([...(snapshot?.resources||[]).map((r:any)=>[r.id,resourceName(r)]),...inventory.map(r=>[r.id,resourceName(r.payload)])]),missions:snapshot?.mission?{[snapshot.mission.id]:snapshot.mission.title}:{}};
}
export function PlanningContext({value,references}:{value:any;references:PlanningReferences}) {
 if(!value)return null;
 return <div className="stack">
 {value.mission && <p><Link href={'/missions?record='+encodeURIComponent(value.mission.id)}>{displayText(value.mission.title)}</Link> · {value.mission.assigned_count} assigned of {value.mission.capacity} needed</p>}
 {!!value.candidates?.length && <ul>{value.candidates.map((person:any)=><li key={person.user_id}><Link href={'/people?user='+person.user_id}>{references.people[String(person.user_id)]||'Volunteer profile'}</Link> · {person.eligible?'Eligible':'Needs review'}{person.missing_skills?.length?' · Missing: '+person.missing_skills.map((s:string)=>s.toLowerCase().replaceAll('_',' ')).join(', '):''}</li>)}</ul>}
 {!!value.resources?.length && <ul>{value.resources.map((resource:any)=><li key={resource.id}><Link href={'/resources?record='+encodeURIComponent(resource.id)}>{references.resources[resource.id]||resourceName(resource)}</Link> · {resource.available} available{resource.location?' · '+displayText(resource.location):''}</li>)}</ul>}
 {!!value.limitations?.length && <ul>{value.limitations.map((line:string,i:number)=><li key={i}><ReferenceText text={line} references={references}/></li>)}</ul>}
 </div>;
}
export default function PlanningDetails({value,references}:{value:any;references:PlanningReferences}) {
 if(!value)return null;
 return <div className="stack">
 {(value.allocations||[]).map((allocation:any)=><section className="panel panel-body" key={allocation.user_id}>
  <h4><Link href={'/people?user='+allocation.user_id}>{references.people[String(allocation.user_id)]||'Volunteer profile'}</Link></h4>
  <p><ReferenceText text={allocation.reason} references={references}/></p>
  <ul>{(allocation.supplies||[]).map((s:any)=><li key={s.resource_id}><Link href={'/resources?record='+encodeURIComponent(s.resource_id)}>{references.resources[s.resource_id]||'Resource details'}</Link> — {s.quantity}<p><ReferenceText text={s.reason} references={references}/></p></li>)}</ul>
 </section>)}
 {['shortages','decisions'].map(key=>!!value[key]?.length&&<section key={key}><h4 className="section-title">{key==='shortages'?'Shortages to resolve':'Coordinator decisions'}</h4><ul>{value[key].map((line:string,i:number)=><li key={i}><ReferenceText text={line} references={references}/></li>)}</ul></section>)}
 </div>;
}
