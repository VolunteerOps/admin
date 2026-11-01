"use client";
import {useState} from 'react';
import {api} from '@/lib/api';
import type {Scope} from '@/lib/hooks';
import {UserFacingError,friendlyErrors} from '@/lib/friendly-errors';
import {Form} from './Forms';
import {Button} from './ui';

export default function RequestForecast({scope,onRequested}:{scope:Scope;onRequested:(job:any)=>void}) {
 const [manual,setManual]=useState(false);
 return <>
  <p>Get the latest published forecast for your operating region. ResQ finds the publication time for you.</p>
  <div className="action-list">
   <Button aria-pressed={!manual} variant={!manual?'primary':''} onClick={()=>setManual(false)}>Latest available forecast</Button>
   <Button aria-pressed={manual} onClick={()=>setManual(true)}>Choose a specific forecast</Button>
  </div>
  {!manual?<Form key="latest" fields={[{key:'hours',label:'How far ahead?',type:'number',value:24,options:[{value:'24',label:'Next 24 hours'},{value:'48',label:'Next 48 hours'}]}]} submit="Get latest forecast" onSubmit={async(v,key)=>onRequested(await api(scope.sentinel+'/forecasts/refresh-latest',v,'POST',undefined,key))}>
   <small>We check which forecast is available before loading it. If weather data access is unavailable, we will explain what needs to be resolved.</small>
  </Form>:<>
   <p>Use this only when you have the publication time of a specific forecast. Otherwise, choose Latest available forecast.</p>
   <Form key="manual" fields={[
    {key:'init_time',label:'Forecast publication date and time',hint:'Enter the time shown on the published forecast. Times here use your device’s local time.',type:'datetime-local',full:true},
    {key:'from_hour',label:'Start hour after publication',hint:'1 means the first hour after this forecast was published.',type:'number',min:1,max:360,step:'1',value:1},
    {key:'to_hour',label:'End hour after publication',hint:'24 means one day after publication. Request up to 48 hours at a time.',type:'number',min:1,max:360,step:'1',value:24}
   ]} submit="Request this forecast" onSubmit={async(v,key)=>{
    if(Date.parse(v.init_time)%3600000)throw new UserFacingError(friendlyErrors.FORECAST_HOUR_REQUIRED);
    if(Date.parse(v.init_time)>Date.now())throw new UserFacingError(friendlyErrors.FORECAST_CYCLE_FUTURE);
    if(v.to_hour<v.from_hour||v.to_hour-v.from_hour>47)throw new UserFacingError('Choose an end hour after the start hour, covering no more than 48 hours. For one day, use start hour 1 and end hour 24.');
    onRequested(await api(scope.sentinel+'/forecasts/refresh',v,'POST',undefined,key));
   }}/>
  </>}
 </>;
}
