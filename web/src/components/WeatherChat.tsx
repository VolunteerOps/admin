"use client";
import {useState} from 'react';
import {api,errorMessage} from '@/lib/api';
import {postStream,partialField} from '@/lib/stream';
import {fetchServerDraft,syncServerDraft,deleteServerDraft} from '@/lib/drafts';
import {useEffect} from 'react';
import type {Scope} from '@/lib/hooks';
import {Button,Notice,Badge} from './ui';
import {Evidence} from './Sentinel';
export const weatherSuggestions=[
 'What should our team prepare for in this region today and over the next seven days?',
 'How do the last three days of forecast rainfall compare with the coming week, and what should we check on the ground?',
 'When could heat or strong winds affect outdoor volunteer work this week?'
];
type Turn={role:'user'|'assistant';content:string};
const conversations=new Map<string,{history:Turn[];answer:any}>();
export default function WeatherChat({scope}:{scope:Scope}){
 const cacheKey=[scope.userId,scope.org,scope.region].join(':');
 const [question,setQuestion]=useState(''),[history,setHistory]=useState<Turn[]>(conversations.get(cacheKey)?.history||[]),[answer,setAnswer]=useState<any>(conversations.get(cacheKey)?.answer||null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const [requestKey,setRequestKey]=useState(()=>crypto.randomUUID());
 const [streamPreview,setStreamPreview]=useState('');
 const serverDraftKey='weather-chat:'+cacheKey;
 useEffect(()=>{let live=true;void fetchServerDraft(serverDraftKey).then(server=>{if(!live||!server?.payload)return;const saved=server.payload as any;if(Array.isArray(saved.history)&&!conversations.get(cacheKey)) {conversations.set(cacheKey,{history:saved.history,answer:saved.answer});setHistory(saved.history);if(saved.answer)setAnswer(saved.answer);}});return()=>{live=false;};},[]);
 async function send(mode:'CHAT'|'DAILY_BRIEF',prompt=question){
  if(busy||prompt.trim().length<3)return;setBusy(true);setError('');
  try{let streamed='';setStreamPreview('Thinking...');
   const record=await postStream(scope.sentinel+'/conversations/weather/stream',{question:prompt,history:history.slice(-16),mode},event=>{
     if(event.type==='token'){streamed+=event.text;const preview=partialField(streamed,'summary');if(preview)setStreamPreview(preview);}
     else if(event.type==='tool_start')setStreamPreview('Checking '+String(event.tool).replace(/_/g,' ')+'...');
     else if(event.type==='round')setStreamPreview('Thinking...');
   }).catch(async streamError=>{if(streamed)throw streamError;const fallback=await api(scope.sentinel+'/conversations/weather',{question:prompt,history:history.slice(-16),mode},'POST',undefined,requestKey);return fallback;});
   setStreamPreview('');
   if(record.payload?.status==='FAILED')throw {code:record.payload.error_code,message:record.payload.message};
   const result=record.payload;const next:Turn[]=[...history,{role:'user',content:prompt},{role:'assistant',content:result.narrative.summary}];
   setHistory(next);setAnswer(result);conversations.set(cacheKey,{history:next,answer:result});syncServerDraft(serverDraftKey,{history:next,answer:result});setQuestion('');setRequestKey(crypto.randomUUID());scope.refresh();
  }catch(e){setError(errorMessage(e));}finally{setBusy(false);}
 }
 return <section className="panel weather-chat" aria-label="Ask Sentinel">
  <div className="panel-head"><div><span className="eyebrow">SENTINEL / PREPAREDNESS</span><h2>What does your team need to know?</h2></div><Button icon="sentinel" variant="primary" disabled={busy} onClick={()=>{void send('DAILY_BRIEF',weatherSuggestions[0]);}}>Prepare today's briefing</Button></div>
  <div className="panel-body">
   <p>Ask in your own words. Sentinel checks available forecasts, compares recent trends and explains what needs attention.</p>
   {!history.length&&<div className="suggestions">{weatherSuggestions.map(q=><button className="suggestion" key={q} onClick={()=>{setQuestion(q);setRequestKey(crypto.randomUUID());}}>{q}</button>)}</div>}
   <div className="conversation" aria-live="polite">{history.slice(-8).map((turn,i)=><div className={'chat-turn '+turn.role} key={i}><strong>{turn.role==='user'?'You':'Sentinel'}</strong><p>{turn.content}</p></div>)}</div>
   {answer&&<div className="briefing"><h3>Preparation priorities</h3><ul>{answer.narrative.preparedness.map((x:string)=><li key={x}>{x}</li>)}</ul>
    {answer.coverage==='NO_FORECAST_EVIDENCE'&&<Notice error>No forecast data could be retrieved. These are general preparation checks, not a weather-based assessment.</Notice>}
    <details><summary>Sources, coverage and limitations</summary><p>Past forecasts describe what was predicted, not what was measured. BigQuery and Cloud Storage hold the same model's output.</p>{answer.sources?.map((s:any,i:number)=><div className="source-entry" key={i}><strong>{s.surface} / {new Date(s.from).toLocaleDateString()} to {new Date(s.to).toLocaleDateString()}</strong><Badge status={s.status==='UNAVAILABLE'?'UNAVAILABLE':'RETRIEVED'}/>{s.status==='UNAVAILABLE'?<p>{errorMessage({code:s.code})}</p>:<Evidence value={s}/>}</div>)}<Evidence value={{assumptions:answer.narrative.assumptions,missing_inputs:answer.narrative.missing_inputs}}/></details>
    {answer.narrative.follow_up&&<p>{answer.narrative.follow_up}</p>}
   </div>}
   {busy&&streamPreview&&<p role="status" className="mission-stream-preview">{streamPreview}</p>}
   {error&&<Notice error>{error}</Notice>}
   <form onSubmit={e=>{e.preventDefault();void send('CHAT');}}><label htmlFor="weather-question">Ask about your region<textarea id="weather-question" value={question} minLength={3} maxLength={4000} required onChange={e=>{setQuestion(e.target.value);setRequestKey(crypto.randomUUID());}} placeholder="For example: what do recent rainfall forecasts mean for our preparations this week?"/></label><div className="form-actions"><small>{busy?'Checking sources and preparing your answer. This may take a few minutes.':'Uses your selected operating region. No deployments are made from chat.'}</small><Button variant="primary" disabled={busy} type="submit">{busy?'Preparing...' :'Ask Sentinel'}</Button></div></form>
  </div>
 </section>;
}
