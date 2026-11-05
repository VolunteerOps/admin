import { api, identity } from "./api";
import { useEffect, useState } from "react";
export function useDraftSyncNotice(key: string) {
  const [notice,setNotice]=useState("");
  useEffect(()=>{
    setNotice("");
    const failed=(event:Event)=>{
      const detail=(event as CustomEvent).detail;if(detail?.key!==key)return;
      setNotice(detail.conflict?"Another device changed this conversation. Your local copy is safe; reload to review the saved copy before continuing on another device.":"This conversation could not sync. Keep this tab open or save a local copy before leaving.");
    };
    window.addEventListener("resq-draft-error",failed);return()=>window.removeEventListener("resq-draft-error",failed);
  },[key]);
  return notice;
}
export type ServerDraft = { key: string; payload: Record<string, any> | null; version: number; updated_at?: string };
async function path(key: string) {
  const safe = /^[\w:./-]{1,300}$/.test(key) ? key : "form:" + Array.from(
    new Uint8Array(await crypto.subtle.digest("SHA-256", new TextEncoder().encode(key))),
    b => b.toString(16).padStart(2, "0"),
  ).join("");
  return "/api/me/drafts/" + encodeURIComponent(safe);
}
const reads = new Map<string, { until: number; pending: Promise<ServerDraft | null> }>();
const timers = new Map<string, ReturnType<typeof setTimeout>>();
const versions = new Map<string, number>();
const queues = new Map<string, Promise<unknown>>();
const conflicted = new Set<string>();
function enqueue(owner: string, task: () => Promise<void>) {
  const next = (queues.get(owner) ?? Promise.resolve()).catch(() => {}).then(task);
  queues.set(owner, next);
  void next.finally(() => { if (queues.get(owner) === next) queues.delete(owner); }).catch(() => {});
  return next;
}
async function ownerKey(key: string) {
  const uid = (await identity()).currentUser?.uid;
  return uid ? uid + "|" + key : null;
}
export async function fetchServerDraft(key: string): Promise<ServerDraft | null> {
  if (!key) return null;
  const owner = await ownerKey(key).catch(() => null);
  if (!owner) return null;
  const previous = reads.get(owner);
  if (previous && previous.until > Date.now()) return previous.pending;
  const entry = { until: Date.now() + 30_000, pending: Promise.resolve(null) as Promise<ServerDraft | null> };
  entry.pending = path(key).then(p => api(p)).then(draft => {
    versions.set(owner, draft?.version ?? 0);
    return draft?.payload ? draft as ServerDraft : null;
  }).catch(() => null);
  reads.set(owner, entry);
  return entry.pending;
}
export function syncServerDraft(key: string, payload: Record<string, any>) {
  if (!key) return;
  const snapshot = structuredClone(payload);
  void ownerKey(key).then(owner => {
    if (!owner) return;
    const existing = timers.get(owner);
    if (existing) clearTimeout(existing);
    timers.set(owner, setTimeout(() => {
      timers.delete(owner);
      void enqueue(owner, async () => {
      try {
        if (conflicted.has(owner) || await ownerKey(key) !== owner) return;
        if (!versions.has(owner)) await fetchServerDraft(key);
        const endpoint = await path(key);
        if (await ownerKey(key) !== owner || !versions.has(owner)) return;
        const draft = await api(endpoint, { payload: snapshot, base_version: versions.get(owner) }, "PUT");
        versions.set(owner, draft.version);
        reads.set(owner, { until: Date.now() + 30_000, pending: Promise.resolve(draft) });
      } catch (error: any) {
        if (error?.code === "DRAFT_CONFLICT") conflicted.add(owner);
        window.dispatchEvent(new CustomEvent("resq-draft-error", { detail: { key, conflict: error?.code === "DRAFT_CONFLICT" } }));
      }
      });
    }, 800));
  }).catch(() => {});
}
export async function deleteServerDraft(key: string) {
  if (!key) return;
  const owner = await ownerKey(key).catch(() => null);
  if (!owner) return;
  const timer = timers.get(owner);
  if (timer) clearTimeout(timer);
  timers.delete(owner);
  await enqueue(owner, async () => {
    const endpoint = await path(key);
    if (await ownerKey(key) !== owner) return;
    try {
      await api(endpoint, undefined, "DELETE");
      versions.set(owner, 0); conflicted.delete(owner);
      reads.set(owner, { until: Date.now() + 30_000, pending: Promise.resolve(null) });
    } catch { /* Keep the known version when deletion fails. */ }
  });
}
