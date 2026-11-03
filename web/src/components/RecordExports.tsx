"use client";
import { useState } from "react";
import Link from "next/link";
import { identity, errorMessage, ApiError } from "@/lib/api";
import type { Scope } from "@/lib/hooks";
import { Button, Notice } from "./ui";
export default function RecordExports({
  scope,
  range,
}: {
  scope: Scope;
  range: { from: string; to: string };
}) {
  const [busy, setBusy] = useState(""),
    [error, setError] = useState("");
  async function download(kind: string) {
    setBusy(kind);
    setError("");
    try {
      const token = await (await identity()).currentUser?.getIdToken();
      const response = await fetch(
        scope.ops + "/exports/" + kind + "?" + new URLSearchParams(range),
        { headers: { Authorization: "Bearer " + token }, cache: "no-store" },
      );
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new ApiError(
          response.status,
          body.error || "EXPORT_UNAVAILABLE",
          body.message || "",
        );
      }
      const href = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = href;
      link.download =
        kind === "HOURS" ? "resq-volunteer-hours.csv" : "resq-missions.csv";
      link.click();
      URL.revokeObjectURL(href);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy("");
    }
  }
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Share a clear summary</h2>
      </div>
      <div className="panel-body">
        <p>
          Download records from your selected region and reporting interval.
          Recorded and approved hours stay separate.
        </p>
        <div className="action-list">
          <Button
            icon="download"
            disabled={!!busy}
            onClick={() => void download("HOURS")}
          >
            {busy === "HOURS" ? "Preparing…" : "Export volunteer hours"}
          </Button>
          <Button
            icon="download"
            disabled={!!busy}
            onClick={() => void download("MISSION")}
          >
            {busy === "MISSION" ? "Preparing…" : "Export mission summary"}
          </Button>
          <Link className="text-button" href="/activity">
            Review activity history →
          </Link>
        </div>
        {error && <Notice error>{error}</Notice>}
      </div>
    </section>
  );
}
