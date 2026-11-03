import { api } from "./api";

/**
 * Server-synced drafts: the same draft key resolves on web, mobile and
 * desktop for the signed-in user. Local storage remains the offline
 * fallback; the server copy wins when it is newer.
 */
export type ServerDraft = {
  key: string;
  payload: Record<string, any> | null;
  version: number;
  updated_at?: string;
};
const path = (key: string) => "/api/me/drafts/" + encodeURIComponent(key);

export async function fetchServerDraft(key: string): Promise<ServerDraft | null> {
  try {
    const draft = await api(path(key));
    return draft?.payload ? (draft as ServerDraft) : null;
  } catch {
    return null;
  }
}
const timers = new Map<string, ReturnType<typeof setTimeout>>();
export function syncServerDraft(key: string, payload: Record<string, any>) {
  const existing = timers.get(key);
  if (existing) clearTimeout(existing);
  timers.set(
    key,
    setTimeout(() => {
      timers.delete(key);
      void api(path(key), { payload, base_version: 0 }, "PUT").catch(() => {});
    }, 800),
  );
}
export async function deleteServerDraft(key: string) {
  const existing = timers.get(key);
  if (existing) clearTimeout(existing);
  timers.delete(key);
  try {
    await api(path(key), undefined, "DELETE");
  } catch {
    /* Draft cleanup is best-effort. */
  }
}
