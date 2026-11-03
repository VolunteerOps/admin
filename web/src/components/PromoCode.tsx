"use client";
import { useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { Button, Notice } from "./ui";
export default function PromoCode({
  onApplied,
}: {
  onApplied: (value: any) => void;
}) {
  const [code, setCode] = useState(""),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [applied, setApplied] = useState<any>(null);
  return (
    <form
      className="promo-form"
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError("");
        try {
          const result = await api("/api/billing/promotions/preview", { code });
          setApplied(result);
          onApplied(result);
        } catch (e) {
          setError(errorMessage(e));
        } finally {
          setBusy(false);
        }
      }}
    >
      <label htmlFor="promotion-code">Have a promotion code?</label>
      <div className="promo-input">
        <input
          id="promotion-code"
          value={code}
          minLength={4}
          maxLength={40}
          autoComplete="off"
          placeholder="Enter your code"
          onChange={(e) => {
            setCode(e.target.value);
            setApplied(null);
            onApplied(null);
            setError("");
          }}
          required
        />
        <Button type="submit" disabled={busy}>
          {busy ? "Checking…" : "Apply"}
        </Button>
      </div>
      {error && <Notice error>{error}</Notice>}
      {applied && (
        <Notice>
          {applied.percent === 100
            ? `Your code covers ${applied.days} days of Pro. Activate it below.`
            : `${applied.percent}% off. Final eligibility and price are confirmed at checkout.`}
        </Notice>
      )}
    </form>
  );
}
