import { api, identity } from "./api";
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
  entry.pending = path(key).then(p => api(p)).then(draft => draft?.payload ? draft as ServerDraft : null).catch(() => null);
  reads.set(owner, entry);
  return entry.pending;
}
export function syncServerDraft(key: string, payload: Record<string, any>) {
  if (!key) return;
  void ownerKey(key).then(owner => {
    if (!owner) return;
    const existing = timers.get(owner);
    if (existing) clearTimeout(existing);
    timers.set(owner, setTimeout(async () => {
      timers.delete(owner);
      try {
        if (await ownerKey(key) !== owner) return;
        const draft = await api(await path(key), { payload, base_version: 0 }, "PUT");
        reads.set(owner, { until: Date.now() + 30_000, pending: Promise.resolve(draft) });
      } catch { /* Preserve local draft; never retry on every render. */ }
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
  reads.set(owner, { until: Date.now() + 30_000, pending: Promise.resolve(null) });
  try { await api(await path(key), undefined, "DELETE"); } catch { /* Best effort. */ }
}
