"use client";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Button, Notice } from "./ui";
import { errorMessage } from "@/lib/api";
import { fetchServerDraft, syncServerDraft, deleteServerDraft } from "@/lib/drafts";
export type Field = {
  key: string;
  label: string;
  type?: string;
  required?: boolean;
  min?: number;
  max?: number;
  step?: string;
  options?: { value: string; label: string }[];
  hint?: string;
  value?: any;
  full?: boolean;
};
export function Form({
  fields,
  onSubmit,
  submit = "Save changes",
  children,
  onCancel,
  draftKey,
}: {
  fields: Field[];
  onSubmit: (values: Record<string, any>, key: string) => Promise<void>;
  submit?: string;
  children?: React.ReactNode;
  onCancel?: () => void;
  draftKey?: string;
}) {
  const formRef = useRef<HTMLFormElement>(null);
  const autoDraftKey = typeof window !== "undefined" && !fields.some((f) => f.key === "expected_version") ? window.location.pathname + ":" + submit + ":" + (fields[0]?.key || "form") : "";
  const storageKey = "resq-draft:" + (draftKey || autoDraftKey);
  const [draft, setDraft] = useState<{ values: Record<string, any>; saved_at: string } | null>(null);
  const [epoch, setEpoch] = useState(0);
  const [draftNotice, setDraftNotice] = useState("");
  useEffect(() => {
    if (!storageKey) return;
    try {
      const raw = window.localStorage.getItem(storageKey);
      if (raw) setDraft(JSON.parse(raw));
    } catch { /* Drafts are a convenience; storage may be unavailable. */ }
  }, [storageKey]);
  useEffect(() => {
    if (!draftKey && !autoDraftKey) return;
    let live = true;
    void fetchServerDraft(storageKey.replace("resq-draft:", "")).then((server) => {
      if (!live || !server?.payload) return;
      const serverEntry = { values: (server.payload.values ?? {}) as Record<string, any>, saved_at: server.updated_at || new Date().toISOString() };
      setDraft((local) => (!local || serverEntry.saved_at > local.saved_at ? serverEntry : local));
    });
    return () => { live = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [storageKey]);  function readValues(form: HTMLFormElement) {
    const fd = new FormData(form);
    const values: Record<string, any> = {};
    for (const f of fields) {
      const v = fd.get(f.key);
      if (v !== null && String(v) !== "") values[f.key] = String(v);
    }
    return values;
  }
  function saveDraft(form: HTMLFormElement | null, silent = false) {
    if (!storageKey || !form) return;
    const entry = { values: readValues(form), saved_at: new Date().toISOString() };
    try {
      window.localStorage.setItem(storageKey, JSON.stringify(entry));
      syncServerDraft(storageKey.replace("resq-draft:", ""), entry as unknown as Record<string, any>);
      setDraft(entry);
      if (!silent) setDraftNotice("Draft saved on this device. You can leave and resume later.");
    } catch { if (!silent) setDraftNotice("Drafts are not available in this browser."); }
  }
  function resumeDraft() {
    setEpoch((v) => v + 1);
    setDraftNotice("Draft resumed. Review it before submitting.");
  }
  function discardDraft() {
    try { window.localStorage.removeItem(storageKey); } catch { /* ignore */ }
    void deleteServerDraft(storageKey.replace("resq-draft:", ""));
    setDraft(null);
    setEpoch((v) => v + 1);
    setDraftNotice("Draft discarded.");
  }  const [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const [key, setKey] = useState(() => crypto.randomUUID());
  const [changed, setChanged] = useState(false);
  async function send(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (busy) return;
    const values: Record<string, any> = {};
    const fd = new FormData(e.currentTarget);
    try {
    for (const f of fields) {
      const v = fd.get(f.key);
      if (f.type === "checkbox") values[f.key] = v === "on";
      else if (v !== null && String(v).trim() !== "") {
        values[f.key] =
          f.type === "number"
            ? Number(v)
            : f.type === "datetime-local"
              ? new Date(String(v)).toISOString()
              : f.type === "password"
                ? String(v)
                : String(v).trim();
      }
    }
    } catch { setError("Check the dates and numbers in this form, then try again."); return; }
    setBusy(true);
    setError("");
    try {
      await onSubmit(values, key);
      if (storageKey) { try { window.localStorage.removeItem(storageKey); } catch { /* ignore */ } void deleteServerDraft(storageKey.replace("resq-draft:", "")); setDraft(null); }
      setKey(crypto.randomUUID());
      setChanged(false);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form
      ref={formRef}
      key={epoch}
      onSubmit={send}
      onChange={(e) => {
        setChanged(true);
        saveDraft(e.currentTarget, true);
        setKey(crypto.randomUUID());
      }}
    >
      <fieldset
        disabled={busy}
        style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
      >
        <div className="form-grid">
          {fields.map((f) => (
            <label
              key={f.key}
              className={
                (f.full ? "full " : "") + (f.type === "checkbox" ? "check" : "")
              }
            >
              {f.type !== "checkbox" && f.label}
              {f.type === "textarea" ? (
                <textarea
                  aria-label={f.label}
                  name={f.key}
                  required={f.required !== false}
                  minLength={f.min}
                  maxLength={f.max}
                  defaultValue={f.value}
                />
              ) : f.options ? (
                <SelectField field={f} />
              ) : (
                <input
                  aria-label={f.label}
                  name={f.key}
                  type={f.type || "text"}
                  required={
                    f.type === "checkbox" ? false : f.required !== false
                  }
                  min={f.type === "number" ? f.min : undefined}
                  max={f.type === "number" ? f.max : undefined}
                  minLength={f.type !== "number" ? f.min : undefined}
                  maxLength={f.type !== "number" ? f.max : undefined}
                  step={f.step ?? (f.type === "number" ? "any" : undefined)}
                  defaultValue={f.type === "checkbox" ? undefined : f.value}
                  defaultChecked={f.type === "checkbox" ? !!f.value : undefined}
                />
              )}
              {f.type === "checkbox" && f.label}
              {f.hint && <small>{f.hint}</small>}
            </label>
          ))}
        </div>
        {children}
        {draft && (
          <Notice>
            Draft saved {new Date(draft.saved_at).toLocaleString()} on this device.
            <span className="action-list">
              <Button type="button" onClick={resumeDraft}>Resume draft</Button>
              <Button type="button" onClick={discardDraft}>Discard draft</Button>
            </span>
          </Notice>
        )}
        {draftNotice && <Notice>{draftNotice}</Notice>}        {error && <Notice error>{error}</Notice>}
        <div className="form-actions">
          {onCancel && (
            <Button type="button" onClick={onCancel}>
              Cancel
            </Button>
          )}
          {(draftKey || autoDraftKey) && (<Button type="button" disabled={busy} onClick={() => saveDraft(formRef.current)}>Save draft</Button>)}
          <Button variant="primary" type="submit" disabled={busy}>
            {busy ? "Saving…" : submit}
          </Button>
        </div>
      </fieldset>
    </form>
  );
}
function SelectField({ field: f }: { field: Field }) {
  const [value, setValue] = useState(String(f.value ?? ""));
  const touched = useRef(false);
  useEffect(() => {
    if (!touched.current) setValue(String(f.value ?? ""));
  }, [f.value]);
  return (
    <select
      aria-label={f.label}
      name={f.key}
      required={f.required !== false}
      value={value}
      onChange={(e) => {
        touched.current = true;
        setValue(e.target.value);
      }}
    >
      <option value="" disabled={f.required !== false}>
        Select {f.label.toLowerCase()}
      </option>
      {f.options?.map((o) => (
        <option key={o.value} value={o.value}>
          {o.label}
        </option>
      ))}
    </select>
  );
}
export const reason: Field = {
  key: "reason",
  label: "Reason for this decision",
  type: "textarea",
  min: 10,
  max: 1000,
  full: true,
};
export const opts = (values: string[]) =>
  values.map((value) => ({
    value,
    label: value
      .toLowerCase()
      .replace(/_/g, " ")
      .replace(/^./, (c) => c.toUpperCase()),
  }));
