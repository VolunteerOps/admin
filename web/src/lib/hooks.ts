"use client";
import { useEffect, useState, useRef } from "react";
import { api, errorMessage } from "./api";
type Entry = { data?: any; time: number; pending?: Promise<any> };
const cache = new Map<string, Entry>();
let cacheIdentity = "";
export function setCacheIdentity(uid: string) {
  if (cacheIdentity !== uid) { cache.clear(); cacheIdentity = uid; }
}
export function invalidateData() { for (const entry of cache.values()) entry.time = 0; }
export function useData(path: string | null, revision = 0) {
  const key = cacheIdentity + ":" + path;
  const [state, setState] = useState<{key:string;data:any;error:string;loading:boolean}>({key,data:cache.get(key)?.data ?? null,error:"",loading:!!path});
  const lastRevision = useRef(revision);
  useEffect(() => {
    let live = true;
    const forced = lastRevision.current !== revision;
    lastRevision.current = revision;
    if (!path) { setState({key,data:null,error:"",loading:false}); return; }
    let entry = cache.get(key);
    if (!entry) {
      if(cache.size>=150){const oldest=[...cache.entries()].find(([,v])=>!v.pending);if(oldest)cache.delete(oldest[0]);}
      entry = {time:0}; cache.set(key,entry);
    }
    const current = entry;
    setState({key,data:current.data ?? null,error:"",loading:current.data == null});
    if (!forced && current.data !== undefined && Date.now()-current.time < 60000) return;
    current.pending ??= api(path).then(data => { current.data=data; current.time=Date.now(); return data; }).finally(()=>{ current.pending=undefined; });
    current.pending.then(data => { if(live) setState({key,data,error:"",loading:false}); }).catch(e=>{
      if(e?.status===401 || e?.status===403) { current.data=undefined; current.time=0; }
      if(live) setState({key,data:current.data ?? null,error:errorMessage(e),loading:false});
    });
    return ()=>{live=false;};
  },[key,path,revision]);
  return state.key === key ? state : {data:cache.get(key)?.data ?? null,error:"",loading:!!path && cache.get(key)?.data===undefined};
}
export type Scope = {
  org: string;
  region: string;
  ops: string;
  sentinel: string;
  safety: string;
  role: string;
  userId: number;
  refresh: () => void;
};

/** Explicit continuation; never silently drop records beyond a provider's page limit. */
export function usePagedData(
  path: string | null,
  revision = 0,
  arrayPageSize = 0,
) {
  const [cursor, setCursor] = useState(""),
    [prior, setPrior] = useState<any[]>([]);
  useEffect(() => {
    setCursor("");
    setPrior([]);
  }, [path, revision]);
  const request = useData(
    path
      ? path +
          (cursor
            ? (path.includes("?") ? "&" : "?") +
              "after=" +
              encodeURIComponent(cursor)
            : "")
      : null,
    revision,
  );
  const current = Array.isArray(request.data)
    ? request.data
    : request.data?.items || [];
  const items = [...prior, ...current];
  const next = Array.isArray(request.data)
    ? arrayPageSize && current.length === arrayPageSize
      ? current.at(-1)?.id
      : null
    : request.data?.next_cursor;
  return {
    ...request,
    data: request.data
      ? Array.isArray(request.data)
        ? items
        : { ...request.data, items }
      : null,
    hasMore: next != null && next !== false,
    loadMore: () => {
      if (next != null && !request.loading) {
        setPrior(items);
        setCursor(String(next));
      }
    },
  };
}
