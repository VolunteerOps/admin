import { identity, ApiError } from "./api";

export type StreamEvent = { type: string; [key: string]: any };

/** Extract a partially streamed JSON string field for a live preview. */
export function partialField(accumulated: string, field: string): string {
  const marker = '"' + field + '":"';
  const start = accumulated.indexOf(marker);
  if (start < 0) return "";
  let out = "";
  for (let i = start + marker.length; i < accumulated.length; i++) {
    const ch = accumulated[i];
    if (ch === "\\") {
      const next = accumulated[++i];
      if (next === "n") out += "\n";
      else if (next === "t") out += "\t";
      else if (next) out += next;
    } else if (ch === '"') break;
    else out += ch;
  }
  return out;
}

/**
 * POST with a Server-Sent Events response. Emits round/tool/token events as
 * they arrive and resolves with the final result event. Callers should fall
 * back to the plain JSON endpoint when streaming is unavailable.
 */
export async function postStream(
  path: string,
  body: unknown,
  onEvent: (event: StreamEvent) => void,
  signal?: AbortSignal,
): Promise<any> {
  const auth = await identity();
  const token = await auth.currentUser?.getIdToken();
  const response = await fetch(path, {
    method: "POST",
    cache: "no-store",
    credentials: "omit",
    signal,
    headers: {
      "Content-Type": "application/json",
      Accept: "text/event-stream",
      ...(token ? { Authorization: "Bearer " + token } : {}),
      "Idempotency-Key": crypto.randomUUID(),
    },
    body: JSON.stringify(body),
  });
  if (!response.ok || !response.body || !(response.headers.get("content-type") || "").includes("text/event-stream"))
    throw new ApiError(response.status, "STREAM_UNAVAILABLE", "Streaming is unavailable for this request.");
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  let result: any;
  let failure: any;
  const dispatch = (raw: string) => {
    let event = "message";
    const dataLines: string[] = [];
    for (const line of raw.split("\n")) {
      if (line.startsWith("event:")) event = line.slice(6).trim();
      else if (line.startsWith("data:")) dataLines.push(line.slice(5).trim());
    }
    if (!dataLines.length) return;
    let data: any;
    try { data = JSON.parse(dataLines.join("\n")); } catch { return; }
    if (event === "result") result = data;
    else if (event === "error") failure = data;
    else onEvent({ type: event, ...data });
  };
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const blocks = buffer.split("\n\n");
    buffer = blocks.pop() ?? "";
    for (const block of blocks) dispatch(block);
  }
  if (buffer.trim()) dispatch(buffer);
  if (failure) throw new ApiError(503, failure.code || "AGENT_UNAVAILABLE", failure.message || "The assistant could not finish this request.");
  return result;
}
