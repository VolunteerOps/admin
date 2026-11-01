const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'../../backend');
const write=(name,value)=>fs.writeFileSync(path.join(root,name),value);
write('portal/evidence-contract.ts',`import {z} from 'zod';
export const payloadSchema=z.object({
 type:z.literal('resq.evidence'),expires_at:z.number().finite(),scope:z.string().max(100).optional(),selected_id:z.string().max(160).optional(),
 points:z.array(z.object({id:z.string().max(160).optional(),longitude:z.number().finite().min(-180).max(180),latitude:z.number().finite().min(-90).max(90),label:z.string().max(300),kind:z.enum(['forecast','hazard','mission'])}).strict()).max(1500),
}).strict();
`);
write('portal/map-widget.ts',`import mapboxgl from 'mapbox-gl';
import 'mapbox-gl/dist/mapbox-gl.css';
import './map-widget.css';
import {payloadSchema} from './evidence-contract.js';
import {resqMapStyle} from './resqMapStyle.js';
import type {z} from 'zod';
let map:mapboxgl.Map|undefined,points:z.infer<typeof payloadSchema>['points']=[],origins:string[]=[];
let lastEvidence=0,scope='',fitted=false,selected='',parentOrigin='';
let popup:mapboxgl.Popup|undefined;
const status=(text:string)=>{document.getElementById('status')!.textContent=text;};
function fit(){if(!map||!points.length)return;const bounds=new mapboxgl.LngLatBounds();points.forEach(p=>bounds.extend([p.longitude,p.latitude]));map.fitBounds(bounds,{padding:55,maxZoom:11,duration:0});fitted=true;}
function paint(){
 if(!map?.getSource('evidence'))return;
 (map.getSource('evidence') as mapboxgl.GeoJSONSource).setData({type:'FeatureCollection',features:points.map(p=>({type:'Feature',properties:{id:p.id??'',label:p.label,kind:p.kind,selected:p.id===selected},geometry:{type:'Point',coordinates:[p.longitude,p.latitude]}}))});
 if(!fitted)fit();
}
function select(id:string,move:boolean){
 const p=points.find(p=>p.id===id);selected=p?.id??'';popup?.remove();popup=undefined;paint();
 if(!p||!map)return;
 popup=new mapboxgl.Popup({closeOnClick:false}).setLngLat([p.longitude,p.latitude]).setText(p.label).addTo(map);
 if(move)map.easeTo({center:[p.longitude,p.latitude],duration:0});
}
window.addEventListener('message',event=>{
 if(event.source!==window.parent||!origins.includes(event.origin))return;
 const parsed=payloadSchema.safeParse(event.data);if(!parsed.success)return;
 parentOrigin=event.origin;
 if(parsed.data.expires_at<=Date.now()){points=[];selected='';popup?.remove();paint();status('Evidence expired. Refresh the radar.');return;}
 if((parsed.data.scope??'')!==scope){scope=parsed.data.scope??'';fitted=false;selected='';popup?.remove();}
 points=parsed.data.points;status('');lastEvidence=Math.min(parsed.data.expires_at,Date.now()+45000);paint();
 const next=parsed.data.selected_id??'';
 if(next!==selected)select(next,true);
});
async function start(){try{
 const response=await fetch('/sentinel/map-widget-config',{credentials:'omit',cache:'no-store'});if(!response.ok)throw Error();const config=await response.json();origins=[location.origin,...config.origins];
 if(!config.token||!mapboxgl.supported()){status('Map unavailable. Switch to the evidence list.');return;}
 map=new mapboxgl.Map({container:'map',accessToken:config.token,style:structuredClone(resqMapStyle),center:[37.8,0.3],zoom:5,attributionControl:true});map.addControl(new mapboxgl.NavigationControl(),'top-right');
 map.on('load',()=>{
  map!.addSource('evidence',{type:'geojson',data:{type:'FeatureCollection',features:[]}});
  map!.addLayer({id:'evidence',type:'circle',source:'evidence',paint:{'circle-radius':['case',['get','selected'],11,6],'circle-color':['match',['get','kind'],'hazard','#b63836','mission','#527453','#28518a'],'circle-stroke-color':['case',['get','selected'],'#e4d076','#ffffff'],'circle-stroke-width':['case',['get','selected'],3,1.5]}});
  paint();status('');
 });
 map.on('error',()=>status('Some map tiles could not load. The evidence list remains available.'));
 map.on('click','evidence',event=>{const f=event.features?.[0];if(f?.geometry.type==='Point'){
  const id=String(f.properties?.id??'');
  if(id){select(id,false);if(parentOrigin)window.parent.postMessage({type:'resq.evidence-selection',id},parentOrigin);}
  else new mapboxgl.Popup().setLngLat(f.geometry.coordinates as [number,number]).setText(String(f.properties?.label??'')).addTo(map!);
 }});
 map.on('mouseenter','evidence',()=>{map!.getCanvas().style.cursor='pointer';});map.on('mouseleave','evidence',()=>{map!.getCanvas().style.cursor='';});
 document.getElementById('fit')!.addEventListener('click',fit);
 const observer=new ResizeObserver(()=>map?.resize());observer.observe(document.body);
 const timer=setInterval(()=>{if(lastEvidence&&Date.now()>lastEvidence){points=[];selected='';popup?.remove();paint();status('Evidence expired. Refresh the radar.');}},5000);
 window.addEventListener('pagehide',()=>{clearInterval(timer);observer.disconnect();popup?.remove();map?.remove();},{once:true});
}catch{status('Map unavailable. Use the evidence list.');}}
void start();
`);
write('portal/map-widget.css',`html,body,#map{margin:0;width:100%;height:100%;font:14px system-ui;background:#f8fafc;color:#043473}#status{position:absolute;left:12px;bottom:34px;max-width:80%;padding:10px 14px;background:white;border:1px solid #8b9091;border-radius:12px;pointer-events:none}#status:empty{display:none}#fit{position:absolute;left:12px;top:12px;min-height:44px;padding:8px 14px;border:1px solid #28518a;border-radius:12px;background:white;color:#043473;font:600 13px system-ui;cursor:pointer}#fit:focus-visible{outline:3px solid #e4d076;outline-offset:2px}.mapboxgl-popup-content{padding:14px;border-radius:12px;color:#043473;max-width:280px}.mapboxgl-popup-close-button{width:28px;height:28px}
`);
const html=path.join(root,'portal/map-widget.html');let content=fs.readFileSync(html,'utf8');if(!content.includes('id="fit"'))content=content.replace('<p id="status"','<button id="fit" type="button">Fit evidence</button><p id="status"');fs.writeFileSync(html,content);
write('tests/evidence-contract.test.ts',`import {test} from 'node:test';import assert from 'node:assert/strict';import {payloadSchema} from '../portal/evidence-contract.js';
test('map evidence bounds and additions preserve legacy senders',()=>{
 const point={longitude:0,latitude:0,label:'Meeting point',kind:'mission'};
 const data={type:'resq.evidence',expires_at:Date.now()+1000,points:[point]};
 assert.equal(payloadSchema.safeParse(data).success,true);
 assert.equal(payloadSchema.safeParse({...data,scope:'region',selected_id:'mission-1',points:[{...point,id:'mission-1'}]}).success,true);
 for(const patch of [{latitude:91},{longitude:Infinity},{label:'x'.repeat(301)},{kind:'invented'}])assert.equal(payloadSchema.safeParse({...data,points:[{...point,...patch}]}).success,false);
 assert.equal(payloadSchema.safeParse({...data,points:Array(1501).fill(point)}).success,false);
});
`);
console.log('Updated authenticated evidence bridge and camera behavior.');
