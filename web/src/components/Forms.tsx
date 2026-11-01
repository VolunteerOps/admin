"use client";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Button, Notice } from "./ui";
import { errorMessage } from "@/lib/api";
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
}: {
  fields: Field[];
  onSubmit: (values: Record<string, any>, key: string) => Promise<void>;
  submit?: string;
  children?: React.ReactNode;
  onCancel?: () => void;
}) {
  const [busy, setBusy] = useState(false),
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
      onSubmit={send}
      onChange={() => {
        setChanged(true);
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
        {error && <Notice error>{error}</Notice>}
        <div className="form-actions">
          {onCancel && (
            <Button type="button" onClick={onCancel}>
              Cancel
            </Button>
          )}
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
