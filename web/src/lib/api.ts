import { friendlyError } from "./friendly-errors";
import { getApps, initializeApp } from "firebase/app";
import {
  getAuth,
  browserSessionPersistence,
  setPersistence,
  type Auth,
} from "firebase/auth";
let init: Promise<Auth> | undefined;
let reauthenticate: (() => Promise<void>) | undefined;
let confirmation: Promise<void> | undefined;
export function setReauthenticationHandler(handler: () => Promise<void>) {
  reauthenticate = handler;
  return () => {
    reauthenticate = undefined;
  };
}
export function identity() {
  return (init ??= (async () => {
    const response = await fetch("/sentinel/config?app=ops", {
      cache: "no-store",
    });
    if (!response.ok)
      throw Error("The sign-in service is unavailable. Please try again.");
    const { firebase } = await response.json();
    if (!firebase?.apiKey || !firebase?.appId)
      throw Error("Sign-in has not been fully connected. Contact your workspace administrator.");
    const auth = getAuth(
      getApps().find((a) => a.name === "resqops-react") ||
        initializeApp(firebase, "resqops-react"),
    );
    await setPersistence(auth, browserSessionPersistence);
    await auth.authStateReady();
    return auth;
  })().catch((e) => {
    init = undefined;
    throw e;
  }));
}
export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
  }
}
export async function api(
  path: string,
  body?: unknown,
  method = body === undefined ? "GET" : "POST",
  signal?: AbortSignal,
  key?: string,
  retried = false,
) {
  const requestKey =
    body === undefined ? undefined : key || crypto.randomUUID();
  const auth = await identity();
  const token = await auth.currentUser?.getIdToken();
  const response = await fetch(path, {
    method,
    cache: "no-store",
    credentials: "omit",
    signal,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: "Bearer " + token } : {}),
      ...(body !== undefined ? { "Idempotency-Key": requestKey! } : {}),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  });
  const content =
    response.status === 204 ? null : await response.json().catch(() => null);
  if (
    !response.ok &&
    content?.error === "RECENT_AUTH_REQUIRED" &&
    reauthenticate &&
    !retried
  ) {
    confirmation ??= reauthenticate().finally(() => {
      confirmation = undefined;
    });
    await confirmation;
    if (signal?.aborted)
      throw new DOMException("Request cancelled", "AbortError");
    return api(path, body, method, signal, requestKey, true);
  }
  if (!response.ok)
    throw new ApiError(
      response.status,
      content?.error || "REQUEST_FAILED",
      response.status === 401
        ? "Your session needs a fresh sign-in."
        : response.status === 403
          ? "Your account does not have access to this action."
          : (content?.error === "validation_failed" ? validationMessage(content.details) : content?.message) ||
            "The request could not be completed. Please retry.",
    );
  return normalizeResponse(content);
}
export function errorMessage(e: unknown): string {
 const error = e as {code?:string;status?:number;message?:string};
 return friendlyError(error?.code || "",error?.message || "",error?.status || 0);
}

/** Keep wire keys for command payloads and expose model aliases consistently across all API reads. */
export function normalizeResponse(value:any):any {
 if(Array.isArray(value)) return value.map(normalizeResponse);
 if(value && typeof value==='object') {
  const result:Record<string,any>={};
  for(const [key,entry] of Object.entries(value)) {
   if(['__proto__','constructor','prototype'].includes(key)) continue;
   const normalized=normalizeResponse(entry);result[key]=normalized;
   const alias=key.replace(/_([a-z])/g,(_,letter)=>letter.toUpperCase());
   if(!(alias in value)) Object.defineProperty(result,alias,{value:normalized,enumerable:false,configurable:true});
  }
  return result;
 }
 return value;
}

function validationMessage(details:any) {
 const names:Record<string,string>={init_time:'forecast time',from:'start time',to:'end time',starts_at:'mission start',ends_at:'mission end',capacity:'number of volunteers',reason:'reason',longitude:'map location',latitude:'map location'};
 if(!Array.isArray(details))return 'Check the information in this form and try again.';
 return details.slice(0,3).map((item:any)=>{
  const key=String(item.path||'').split('.').at(-1)||'details';const field=names[key]||key.replace(/_/g,' ');
  if(item.code==='too_small')return 'Add more detail to '+field+'.';
  if(item.code==='too_big')return 'Reduce the amount or length entered for '+field+'.';
  if(item.code==='invalid_type')return 'Enter a valid '+field+'.';
  if(item.code==='invalid_string')return 'Check the format of '+field+'.';
  return friendlyError('',String(item.message||'Check '+field+'.'));
 }).join(' ');
}
