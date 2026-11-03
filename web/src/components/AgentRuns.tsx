"use client";
import { useState } from "react";
import { api } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { date, label, type Row } from "@/lib/catalog";
import { Badge, Button, Empty, Loading, Notice } from "./ui";
import { HarnessSteps } from "./Harness";

export default function AgentRuns({ scope, revision }: { scope: Scope; revision: number }) {
  const base = scope.sentinel + "/agent/runs";
  const [rev, setRev] = useState(0);
  const [selected, setSelected] = useState<Row | null>(null);
  const [followUp, setFollowUp] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const runs = useData(base, revision + rev);

  async function open(id: string) {
    setBusy(true); setError("");
    try { setSelected(await api(base + "/" + id)); }
    catch (e: any) { setError(e?.message || "Could not load run"); }
    finally { setBusy(false); }
  }
  async function branch() {
    if (!selected) return;
    setBusy(true); setError("");
    try {
      const child = await api(base + "/" + selected.id + "/follow-up", { question: followUp }, "POST", undefined, crypto.randomUUID());
      setNotice("Follow-up run created from this run. It carries the earlier question, summary and notes as context.");
      setFollowUp("");
      setRev((v) => v + 1);
      if (child?.id) setSelected(child);
    } catch (e: any) { setError(e?.message || "Follow-up failed"); }
    finally { setBusy(false); }
  }
  async function cancel(id: string) {
    setBusy(true); setError("");
    try { await api(base + "/" + id + "/cancel", {}, "POST", undefined, crypto.randomUUID()); setRev((v) => v + 1); setNotice("Run cancelled."); }
    catch (e: any) { setError(e?.message || "Cancel failed"); }
    finally { setBusy(false); }
  }
  const p = selected?.payload ?? {};
  return (
    <div>
      <p className="page-intro">
        Every Meta-powered run in this region — resource plans, weather analyses and specialised
        capabilities — with its plan, working notes and tool steps. Branch a follow-up from any
        completed run; the earlier context is carried into the new run.
      </p>
      {error && <Notice error>{error}</Notice>}
      {notice && <Notice>{notice}</Notice>}
      <section className="panel">
        {runs.loading ? <Loading /> : runs.data?.items?.length ? (
          <div className="table-wrap" tabIndex={0} role="region" aria-label="Agent runs">
            <table>
              <thead><tr><th>Run</th><th>Type</th><th>Model</th><th>Plan</th><th>Steps</th><th>Status</th><th>Updated</th><th /></tr></thead>
              <tbody>
                {runs.data.items.map((r: any) => (
                  <tr key={r.id}>
                    <td>
                      <button className="table-link" onClick={() => open(r.id)}>
                        <strong>{r.title || label(r.analysis_type || r.kind)}</strong>
                        <small>{r.summary || (r.parent_run_id ? "Follow-up run" : "")}</small>
                      </button>
                    </td>
                    <td>{label(r.analysis_type || r.kind)}</td>
                    <td>{r.model || "—"}</td>
                    <td>{r.plan_steps || 0}</td>
                    <td>{r.steps || 0}</td>
                    <td><Badge status={r.status} /></td>
                    <td>{date(r.updatedAt)}</td>
                    <td>{r.status === "RUNNING" && <Button disabled={busy} onClick={() => cancel(r.id)}>Cancel</Button>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <Empty title="No agent runs yet" body="Create a resource proposal, weather assessment or specialised analysis and it will appear here with its harness transcript." />
        )}
      </section>
      {selected && (
        <section className="panel" aria-label="Run detail">
          <Badge status={p.status} /> <small>{label(p.analysis_type || selected.kind)} · Model: {p.model || "—"}{p.parent_run_id ? " · Follow-up of an earlier run" : ""}</small>
          <h3 className="section-title">{p.title || "Agent run"}</h3>
          <p className="detail-prose">{p.result?.summary || p.narrative?.summary || p.recommendation?.summary || p.briefing?.body || p.message || ""}</p>
          {p.request?.question && <p><strong>Request:</strong> {p.request.question}</p>}
          {Array.isArray(p.request?.history) && p.request.history.length > 0 && (
            <>
              <h4 className="section-title">Chat history carried into this run</h4>
              <ul>{p.request.history.map((turn: any, i: number) => <li key={i}><strong>{turn.role}:</strong> {String(turn.content).slice(0, 300)}</li>)}</ul>
            </>
          )}
          <HarnessSteps harness={p.harness} />
          <h4 className="section-title">Branch a follow-up</h4>
          <div className="form-grid">
            <label className="full">Follow-up question
              <textarea value={followUp} onChange={(e) => setFollowUp(e.target.value)} maxLength={2000}
                placeholder="Ask the next question; the earlier summary and notes are included as context." />
            </label>
          </div>
          <div className="form-actions">
            <Button variant="primary" disabled={busy || followUp.trim().length < 3} onClick={branch}>Run follow-up</Button>
            {p.status === "RUNNING" && <Button disabled={busy} onClick={() => cancel(selected.id)}>Cancel run</Button>}
          </div>
        </section>
      )}
    </div>
  );
}
