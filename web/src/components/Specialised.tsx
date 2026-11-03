"use client";
import { useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { date, label, recordTitle, type Row } from "@/lib/catalog";
import { Badge, Button, Empty, Loading, Notice } from "./ui";
import { HarnessSteps } from "./Harness";

const CAPS = [
  { key: "flood", label: "Flood synthesis", model: "Wonder", needs: "assessment", q: "What flood preparation should we prioritise?" },
  { key: "watch", label: "Rising-water watch", model: "Wonder", needs: "", q: "Prepare a rising-water watch brief for this region." },
  { key: "heat", label: "Heat-health preparation", model: "Spark", needs: "assessment", q: "What heat preparation should we prioritise?" },
  { key: "wind", label: "Wind & access", model: "Spark", needs: "mission", q: "What access assumptions must we re-check?" },
  { key: "multi-region", label: "Multi-region comparison", model: "Wonder", needs: "", q: "Where is preparation attention most needed?" },
  { key: "outcome", label: "Forecast vs outcome", model: "Wonder", needs: "assessment", q: "How did the forecast compare with observations?" },
  { key: "foresight", label: "Credential foresight", model: "Spark", needs: "", q: "Which roles will become unstaffable?" },
  { key: "what-if", label: "What-if scenario", model: "Spark", needs: "mission", q: "How does this scenario change the plan?" },
  { key: "shortage", label: "Shortage options", model: "Spark", needs: "mission", q: "How can we cover these shortages?" },
  { key: "briefings", label: "Audience briefing", model: "Meta", needs: "briefing", q: "" },
  { key: "after-action", label: "After-action draft", model: "Spark", needs: "mission", q: "Draft an after-action summary for this mission." },
] as const;

function Notes({ title, items }: { title: string; items?: string[] }) {
  if (!items?.length) return null;
  return (
    <>
      <h4 className="section-title">{title}</h4>
      <ul>{items.map((item, i) => <li key={i}>{item}</li>)}</ul>
    </>
  );
}

export default function Specialised({ scope, revision }: { scope: Scope; revision: number }) {
  const base = scope.sentinel + "/specialised";
  const [rev, setRev] = useState(0);
  const [tab, setTab] = useState<"run" | "evidence">("run");
  const [cap, setCap] = useState<(typeof CAPS)[number]>(CAPS[0]);
  const [question, setQuestion] = useState<string>(CAPS[0].q);
  const [missionId, setMissionId] = useState("");
  const [assessmentId, setAssessmentId] = useState("");
  const [baseProposalId, setBaseProposalId] = useState("");
  const [horizon, setHorizon] = useState(60);
  const [capacity, setCapacity] = useState("");
  const [skills, setSkills] = useState("");
  const [stockAdj, setStockAdj] = useState("");
  const [resourceId, setResourceId] = useState("");
  const [quantity, setQuantity] = useState(10);
  const [sourceKind, setSourceKind] = useState("ASSESSMENT");
  const [audience, setAudience] = useState("COORDINATOR");
  const [language, setLanguage] = useState("en");
  const [reason, setReason] = useState("");
  const [selected, setSelected] = useState<Row | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const history = useData(base, revision + rev);
  const missions = useData(scope.ops + "/records?kind=MISSION", revision);
  const assessments = useData(scope.sentinel + "/planning/weather", revision);
  const proposals = useData(scope.sentinel + "/planning/resources", revision);
  const inventory = useData(scope.ops + "/records?kind=INVENTORY", revision);
  const evidence = useData(base + "/evidence", revision + rev);
  const tasks = useData(base + "/gap-tasks", revision + rev);

  async function post(path: string, body: any, keyed = true) {
    setBusy(true); setError(""); setNotice("");
    try {
      const result = await api(path, body, "POST", undefined, keyed ? crypto.randomUUID() : undefined);
      setRev((v) => v + 1);
      return result;
    } catch (e: any) {
      setError(e?.message || "Request failed");
      return null;
    } finally { setBusy(false); }
  }
  async function run() {
    const body: any = {};
    if (cap.key === "flood" || cap.key === "heat") { body.question = question; if (assessmentId) body.assessment_id = assessmentId; }
    else if (cap.key === "watch" || cap.key === "multi-region") body.question = question;
    else if (cap.key === "wind" || cap.key === "after-action") { body.mission_id = missionId; body.question = question; }
    else if (cap.key === "outcome") { body.assessment_id = assessmentId; body.question = question; }
    else if (cap.key === "foresight") { body.horizon_days = horizon; body.question = question; }
    else if (cap.key === "what-if") {
      body.mission_id = missionId; body.question = question;
      if (capacity) body.capacity_override = Number(capacity);
      if (skills.trim()) body.required_skills_override = skills.split(",").map((s) => s.trim()).filter(Boolean);
      body.stock_adjustments = stockAdj.split(",").map((part) => {
        const [id, delta] = part.split(":").map((s) => s?.trim());
        return id && delta ? { resource_id: id, delta: Number(delta) } : null;
      }).filter(Boolean);
      if (baseProposalId) body.base_proposal_id = baseProposalId;
    } else if (cap.key === "shortage") {
      body.mission_id = missionId; body.question = question;
      body.requirements = [{ resource_id: resourceId, quantity }];
    } else if (cap.key === "briefings") {
      body.source_kind = sourceKind;
      body.source_id = sourceKind === "ASSESSMENT" ? assessmentId : selected?.id;
      body.audience = audience; body.language = language || "en";
    }
    const result = await post(base + "/" + cap.key, body);
    if (result?.id) setSelected(result);
  }
  const p = selected?.payload ?? {};
  const canRun = !busy && (() => {
    if (cap.key === "wind" || cap.key === "after-action") return !!missionId;
    if (cap.key === "shortage") return !!missionId && !!resourceId;
    if (cap.key === "outcome") return !!assessmentId;
    if (cap.key === "briefings") return !!(sourceKind === "ASSESSMENT" ? assessmentId : selected);
    return true;
  })();

  return (
    <div>
      <p className="page-intro">Meta-powered flood, hazard, scenario and briefing tools. Every result is a draft until a coordinator acts on it.</p>
      <div className="mode-tabs" role="tablist" aria-label="Specialised sections">
        <button type="button" role="tab" aria-selected={tab === "run"} aria-pressed={tab === "run"} onClick={() => setTab("run")}>Analyses</button>
        <button type="button" role="tab" aria-selected={tab === "evidence"} aria-pressed={tab === "evidence"} onClick={() => setTab("evidence")}>Evidence & tasks</button>
      </div>
      {error && <Notice error>{error}</Notice>}
      {notice && <Notice>{notice}</Notice>}
      {tab === "evidence" ? (
        <EvidencePanel base={base} inventory={inventory.data?.items || []} assessments={assessments.data?.items || []}
          evidence={evidence.data?.items || []} tasks={tasks.data?.items || []} busy={busy}
          onSubmit={async (body: any) => { await post(base + "/evidence", body, false); }}
          onWorklist={async (id: string) => { const r = await post(base + "/gap-worklist", { assessment_id: id }, false); if (r) setNotice(`Worklist: ${r.created?.length ?? 0} new tasks, ${r.existing_count ?? 0} already tracked.`); }}
          onTask={async (id: string, body: any) => { await post(base + "/gap-tasks/" + id, body, false); }} />
      ) : (
        <>
          <section className="panel">
            <div className="form-grid">
              <label className="full">Capability
                <select value={cap.key} onChange={(e) => { const next = CAPS.find((c) => c.key === e.target.value)!; setCap(next); setQuestion(next.q); }}>
                  {CAPS.map((c) => <option key={c.key} value={c.key}>{c.label} · {c.model}</option>)}
                </select>
              </label>
              {(cap.needs === "mission") && (
                <label className="full">Mission
                  <select value={missionId} onChange={(e) => setMissionId(e.target.value)}>
                    <option value="">Select a mission…</option>
                    {(missions.data?.items || []).map((r: Row) => <option key={r.id} value={r.id}>{recordTitle(r)}</option>)}
                  </select>
                </label>
              )}
              {(cap.needs === "assessment" || cap.key === "briefings") && (
                <label className="full">{cap.key === "briefings" ? "Source assessment" : "Weather assessment (latest if unselected)"}
                  <select value={assessmentId} onChange={(e) => setAssessmentId(e.target.value)}>
                    <option value="">Select an assessment…</option>
                    {(assessments.data?.items || []).map((r: Row) => <option key={r.id} value={r.id}>{date(r.updatedAt)} · {r.payload.status}</option>)}
                  </select>
                </label>
              )}
              {cap.key === "foresight" && <label>Horizon days<input type="number" min={7} max={180} value={horizon} onChange={(e) => setHorizon(Number(e.target.value))} /></label>}
              {cap.key === "what-if" && <>
                <label>Capacity override<input value={capacity} onChange={(e) => setCapacity(e.target.value)} placeholder="Optional" /></label>
                <label>Skills override<input value={skills} onChange={(e) => setSkills(e.target.value)} placeholder="first-aid, driving" /></label>
                <label className="full">Stock adjustments<input value={stockAdj} onChange={(e) => setStockAdj(e.target.value)} placeholder="resource-id:+5, other-id:-2" /></label>
                <label className="full">Base proposal for diff
                  <select value={baseProposalId} onChange={(e) => setBaseProposalId(e.target.value)}>
                    <option value="">None</option>
                    {(proposals.data?.items || []).map((r: Row) => <option key={r.id} value={r.id}>{date(r.updatedAt)} · {r.payload.status}</option>)}
                  </select>
                </label>
              </>}
              {cap.key === "shortage" && <>
                <label>Stock item
                  <select value={resourceId} onChange={(e) => setResourceId(e.target.value)}>
                    <option value="">Select stock…</option>
                    {(inventory.data?.items || []).map((r: Row) => <option key={r.id} value={r.id}>{recordTitle(r)} · {r.payload.location}</option>)}
                  </select>
                </label>
                <label>Quantity needed<input type="number" min={1} value={quantity} onChange={(e) => setQuantity(Number(e.target.value))} /></label>
              </>}
              {cap.key === "briefings" && <>
                <label>Source kind
                  <select value={sourceKind} onChange={(e) => setSourceKind(e.target.value)}>
                    <option value="ASSESSMENT">Weather assessment</option><option value="PROPOSAL">Resource proposal</option><option value="ANALYSIS">Specialised analysis (select in history)</option>
                  </select>
                </label>
                <label>Audience
                  <select value={audience} onChange={(e) => setAudience(e.target.value)}>
                    <option value="COORDINATOR">Coordinator</option><option value="VOLUNTEER">Volunteer</option><option value="PUBLIC">Public</option>
                  </select>
                </label>
                <label>Language code<input value={language} onChange={(e) => setLanguage(e.target.value)} placeholder="en, sw, fr…" /></label>
              </>}
              {cap.key !== "briefings" && <label className="full">Question / brief<textarea value={question} onChange={(e) => setQuestion(e.target.value)} maxLength={2000} /></label>}
            </div>
            <div className="form-actions">
              <Button variant="primary" disabled={!canRun} onClick={run}>{busy ? "Working…" : "Run " + cap.label}</Button>
              {cap.key === "watch" && <Button disabled={busy} onClick={async () => { const r = await post(base + "/watch/schedule", {}, false); if (r) setNotice("Watch brief scheduled: " + (r.job_id || "")); }}>Schedule hourly watch brief</Button>}
            </div>
          </section>

          {selected && (
            <section className="panel">
              <Badge status={p.status} /> <small>{p.analysis_type} · Model: {p.model || "—"}</small>
              <h3 className="section-title">{p.title || cap.label}</h3>
              <p className="detail-prose">{p.briefing?.body || p.result?.summary || p.recommendation?.summary || p.message || ""}</p>
              <HarnessSteps harness={p.harness} />
              <Notes title="Findings" items={p.result?.findings} />
              <Notes title="Recommendations" items={p.result?.recommendations} />
              <Notes title="Missing inputs" items={p.result?.missing_inputs} />
              <Notes title="Key points" items={p.briefing?.key_points} />
              <Notes title="Limitations" items={p.briefing?.limitations} />
              <Notes title="Shortages" items={p.recommendation?.shortages} />
              <Notes title="Decisions" items={p.recommendation?.decisions} />
              {p.diff && <p>Scenario diff · Added: {(p.diff.added_user_ids || []).join(", ") || "—"} · Removed: {(p.diff.removed_user_ids || []).join(", ") || "—"} · Quantity changes: {(p.diff.quantity_changes || []).length}</p>}
              {p.options && <ul>{p.options.map((o: any) => <li key={o.resource_id}>{o.title || o.resource_id}: short {o.shortfall}, splits {o.split_options?.length || 0}, substitutes {o.substitute_options?.length || 0}, unmet {o.unmet_after_splits}</li>)}</ul>}
              {p.crossings?.length > 0 && <p>Gauges at/above watch: {p.crossings.map((g: any) => g.name || g.gauge_id).join(", ")}</p>}
              {p.findings?.length > 0 && <ul>{p.findings.map((f: any) => <li key={f.skill}>{f.skill}: {f.expiring_count} expiring, earliest {String(f.earliest_expiry).slice(0, 10)}</li>)}</ul>}
              {"flood_impact_estimate" in p && <Notice>No local flood-impact estimate is produced from this evidence.</Notice>}
              {p.status === "DRAFT" && ["BRIEFING", "AFTER_ACTION"].includes(p.analysis_type) && (
                <div className="form-grid">
                  <label className="full">Approval reason<textarea value={reason} onChange={(e) => setReason(e.target.value)} maxLength={1500} /></label>
                  <div className="form-actions">
                    <Button variant="primary" disabled={busy || reason.trim().length < 10}
                      onClick={async () => { const r = await post(base + "/" + selected.id + "/approve", { expected_version: selected.version, reason }); if (r) setSelected(r); }}>Approve for use</Button>
                  </div>
                </div>
              )}
              {p.status === "APPROVED" && p.analysis_type === "BRIEFING" && (
                <div className="form-actions">
                  <input value={language} onChange={(e) => setLanguage(e.target.value)} aria-label="Translation language code" />
                  <Button disabled={busy} onClick={async () => { const r = await post(base + "/briefings/" + selected.id + "/translate", { language }); if (r) setSelected(r); }}>Create translation draft</Button>
                </div>
              )}
            </section>
          )}

          <section className="panel">
            <h3 className="section-title">History</h3>
            {history.loading ? <Loading /> : history.data?.items?.length ? (
              <div className="table-wrap" tabIndex={0} role="region" aria-label="Specialised history">
                <table><thead><tr><th>Analysis</th><th>Type</th><th>Status</th><th>Created</th></tr></thead>
                  <tbody>{history.data.items.map((r: Row) => (
                    <tr key={r.id}><td><button className="table-link" onClick={() => setSelected(r)}><strong>{r.payload.title || label(r.payload.analysis_type)}</strong></button></td>
                      <td>{label(r.payload.analysis_type)}</td><td><Badge status={r.payload.status} /></td><td>{date(r.updatedAt)}</td></tr>))}
                  </tbody></table>
              </div>
            ) : <Empty title="No specialised analyses yet" body="Run a capability above. Results are stored here as drafts." />}
          </section>
        </>
      )}
    </div>
  );
}

function EvidencePanel({ base, inventory, assessments, evidence, tasks, busy, onSubmit, onWorklist, onTask }: any) {
  const [kind, setKind] = useState("GAUGE_READING");
  const [fields, setFields] = useState<any>({ observed_at: new Date().toISOString().slice(0, 19) + "Z" });
  const [assessmentId, setAssessmentId] = useState("");
  const [assignee, setAssignee] = useState("");
  const set = (k: string) => (e: any) => setFields((f: any) => ({ ...f, [k]: e.target.value }));
  async function submit() {
    const body: any = { kind, observed_at: fields.observed_at };
    if (kind === "GAUGE_READING") Object.assign(body, { gauge_id: fields.gauge_id, name: fields.name, river: fields.river || "", level_m: Number(fields.level_m), watch_level_m: Number(fields.watch_level_m), latitude: fields.latitude ? Number(fields.latitude) : null, longitude: fields.longitude ? Number(fields.longitude) : null });
    if (kind === "ROAD_STATUS") Object.assign(body, { name: fields.name, access_type: fields.access_type || "ROAD", status: fields.status || "OPEN", note: fields.note || "" });
    if (kind === "VULNERABILITY_PROFILE") Object.assign(body, { group: fields.group, count: Number(fields.count || 0), needs: String(fields.needs || "").split(",").map((s: string) => s.trim()).filter(Boolean), note: "" });
    if (kind === "FIELD_OBSERVATION") Object.assign(body, { variable: fields.variable || "RAINFALL_MM", value: Number(fields.value), unit: fields.unit || "mm", note: "" });
    if (kind === "SUBSTITUTE_LINK") Object.assign(body, { resource_id: fields.resource_id, substitute_id: fields.substitute_id, note: "" });
    await onSubmit(body);
  }
  return (
    <>
      <section className="panel">
        <h3 className="section-title">Record local evidence</h3>
        <p>Coordinator-recorded inputs for the specialised tools: gauges, access, vulnerability, observations and substitutes.</p>
        <div className="form-grid">
          <label>Kind
            <select value={kind} onChange={(e) => setKind(e.target.value)}>
              <option value="GAUGE_READING">Gauge reading</option><option value="ROAD_STATUS">Access status</option>
              <option value="VULNERABILITY_PROFILE">Vulnerability profile</option><option value="FIELD_OBSERVATION">Field observation</option>
              <option value="SUBSTITUTE_LINK">Substitute link</option>
            </select>
          </label>
          <label>Observed at (ISO UTC)<input value={fields.observed_at} onChange={set("observed_at")} /></label>
          {kind === "GAUGE_READING" && <>
            <label>Gauge ID<input onChange={set("gauge_id")} /></label><label>Name<input onChange={set("name")} /></label>
            <label>River<input onChange={set("river")} /></label><label>Level (m)<input type="number" step="0.01" onChange={set("level_m")} /></label>
            <label>Watch level (m)<input type="number" step="0.01" onChange={set("watch_level_m")} /></label><label>Latitude (radar pin)<input type="number" step="0.0001" onChange={set("latitude")} /></label><label>Longitude (radar pin)<input type="number" step="0.0001" onChange={set("longitude")} /></label>
          </>}
          {kind === "ROAD_STATUS" && <>
            <label>Name<input onChange={set("name")} /></label>
            <label>Type<select onChange={set("access_type")}><option>ROAD</option><option>BRIDGE</option><option>FERRY</option><option>OTHER</option></select></label>
            <label>Status<select onChange={set("status")}><option>OPEN</option><option>RESTRICTED</option><option>CLOSED</option></select></label>
          </>}
          {kind === "VULNERABILITY_PROFILE" && <>
            <label>Group<input onChange={set("group")} placeholder="Older residents" /></label>
            <label>Count<input type="number" onChange={set("count")} /></label>
            <label>Needs (comma separated)<input onChange={set("needs")} /></label>
          </>}
          {kind === "FIELD_OBSERVATION" && <>
            <label>Variable<select onChange={set("variable")}><option>RAINFALL_MM</option><option>TEMPERATURE_C</option><option>WIND_MPS</option><option>RIVER_LEVEL_M</option></select></label>
            <label>Value<input type="number" step="0.1" onChange={set("value")} /></label><label>Unit<input onChange={set("unit")} placeholder="mm" /></label>
          </>}
          {kind === "SUBSTITUTE_LINK" && <>
            <label>Stock item<select onChange={set("resource_id")}><option value="">Select…</option>{inventory.map((r: Row) => <option key={r.id} value={r.id}>{recordTitle(r)}</option>)}</select></label>
            <label>Substitute<select onChange={set("substitute_id")}><option value="">Select…</option>{inventory.map((r: Row) => <option key={r.id} value={r.id}>{recordTitle(r)}</option>)}</select></label>
          </>}
        </div>
        <div className="form-actions"><Button variant="primary" disabled={busy} onClick={submit}>Record evidence</Button></div>
        <ul>{(evidence || []).slice(0, 30).map((e: any) => <li key={e.id}>{label(e.evidence_kind)} · {e.name || e.group || e.variable || ""} · {date(e.observed_at)}</li>)}</ul>
      </section>
      <section className="panel">
        <h3 className="section-title">Evidence-gap worklist</h3>
        <p>Build tracked tasks from an assessment&apos;s missing inputs, then assign and complete them.</p>
        <div className="form-grid">
          <label>Assessment
            <select value={assessmentId} onChange={(e) => setAssessmentId(e.target.value)}>
              <option value="">Select an assessment…</option>
              {assessments.map((r: Row) => <option key={r.id} value={r.id}>{date(r.updatedAt)} · {r.payload.status}</option>)}
            </select>
          </label>
          <label>Assignee member ID (optional)<input value={assignee} onChange={(e) => setAssignee(e.target.value)} /></label>
        </div>
        <div className="form-actions"><Button disabled={busy || !assessmentId} onClick={() => onWorklist(assessmentId)}>Build worklist</Button></div>
        <ul>{(tasks || []).map((t: Row) => (
          <li key={t.id}>{t.payload.title} — {t.payload.status}
            <Button onClick={() => onTask(t.id, { status: t.payload.status === "DONE" ? "OPEN" : "DONE", assignee_user_id: assignee ? Number(assignee) : null })}>
              {t.payload.status === "DONE" ? "Reopen" : "Mark done"}</Button>
          </li>))}
        </ul>
        <p><Link href="/weather-analysis">Open weather analysis</Link></p>
      </section>
    </>
  );
}
