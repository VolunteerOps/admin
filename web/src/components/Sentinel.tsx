"use client";
import { UserFacingError } from "@/lib/friendly-errors";
import RequestForecast from "./RequestForecast";
import ForecastEvidence from "./ForecastEvidence";
import WeatherChat, {weatherSuggestions} from "./WeatherChat";
import { useEffect, useState } from "react";
import PlanEditor from "./PlanEditor";
import Specialised from "./Specialised";
import AgentRuns from "./AgentRuns";
import RadarMap from "./RadarMap";
import OperationsMap from "./OperationsMap";
import { HarnessSteps } from "./Harness";
import Link from "next/link";
import { api, errorMessage } from "@/lib/api";
import { useData, usePagedData, type Scope } from "@/lib/hooks";
import { date, label, recordTitle, type Row } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Loading, Notice } from "./ui";
import { Form, opts, reason, type Field } from "./Forms";
export default function Sentinel({
  page,
  scope,
  revision,
}: {
  page: string;
  scope: Scope;
  revision: number;
}) {
  if (page === "assistance")
    return <Assistance scope={scope} revision={revision} />;
  if (page === "specialised")
    return <Specialised scope={scope} revision={revision} />;
  if (page === "agent-runs")
    return <AgentRuns scope={scope} revision={revision} />;
  if (page === "deployment-map" || page === "operations-map")
    return <OperationsMap scope={scope} revision={revision} />;
  if(page === "weather-analysis")return <><Sources scope={scope} revision={revision}/><Planning scope={scope} revision={revision} weather/></>;
  if (["resource-planning"].includes(page))
    return (
      <Planning
        scope={scope}
        revision={revision}
        weather={page === "weather-analysis"}
      />
    );
  return (
    <Radar
      scope={scope}
      revision={revision}
      deployments={page === "deployment-map"}
    />
  );
}
function Radar({scope,revision}:{scope:Scope;revision:number;deployments:boolean}) {
 const [ask,setAsk]=useState(false);
 useEffect(()=>{setAsk(new URLSearchParams(window.location.search).has("ask"));},[]);
 return <div className="sentinel-reference"><div className="sentinel-intro"><div><span className="eyebrow">A SHARED PICTURE. A CLEARER NEXT STEP.</span><h2>See what needs your attention.</h2><p>Review the source. Understand the local context. Prepare your next move.</p></div><div className="action-list"><Link className="button" href="/campaigns">Preparedness campaigns</Link><Button variant="primary" icon="sentinel" onClick={()=>setAsk(!ask)} aria-expanded={ask} aria-controls="sentinel-conversation">Ask Sentinel</Button></div></div><RadarMap scope={scope}/>{ask&&<div id="sentinel-conversation"><WeatherChat scope={scope}/></div>}<div className="sentinel-source-link"><span>Forecasts indicate possible conditions. Field reports and local checks establish what is happening on the ground.</span><Link className="text-button" href="/weather-analysis">Sources & coverage →</Link></div></div>;
}
function Sources({scope,revision}:{scope:Scope;revision:number}) {
 const forecast=useData(scope.sentinel+'/forecasts/latest',revision),readiness=useData(scope.sentinel+'/readiness',revision);
 const [open,setOpen]=useState(false),[job,setJob]=useState<any>(null),[error,setError]=useState('');
 useEffect(()=>{if(!job?.job_id||['DONE','SUCCEEDED','COMPLETED','DEAD','EXPIRED'].includes(job.state))return;
 const controller=new AbortController();const timer=setInterval(()=>{api(scope.sentinel+'/jobs/'+job.job_id,undefined,'GET',controller.signal).then(r=>{setJob({...r,job_id:job.job_id});if(['DONE','SUCCEEDED','COMPLETED'].includes(r.state))scope.refresh();}).catch(e=>{if(!controller.signal.aborted)setError(errorMessage(e));});},3000);return()=>{clearInterval(timer);controller.abort();};},[job?.job_id,job?.state,scope.sentinel]);
 return <><div className="toolbar"><p>Inspect the evidence behind Sentinel’s preparedness findings.</p><Button icon="refresh" onClick={()=>setOpen(true)}>Request latest forecast</Button></div>{(error||forecast.error||readiness.error)&&<Notice error>{error||forecast.error||readiness.error}</Notice>}{job&&<Notice>{['DONE','SUCCEEDED','COMPLETED'].includes(job.state)?'Your forecast is ready.':['DEAD','EXPIRED'].includes(job.state)?'The forecast could not be retrieved. Check source availability and try again.':'Your forecast is being prepared.'}</Notice>}<div className="source-panels"><section className="panel"><div className="panel-head"><h2>Forecast evidence</h2></div><div className="panel-body">{forecast.loading&&!forecast.data?<Loading/>:forecast.data?<><ForecastEvidence cells={forecast.data.cells||[]}/><Evidence value={forecast.data}/></>:<Empty title="No forecast loaded" body="Request the latest available forecast for this region."/>}</div></section><section className="panel"><div className="panel-head"><h2>Source availability & coverage</h2></div><div className="panel-body"><p>Missing inputs remain visible so your team can judge the limits of an assessment.</p><Evidence value={readiness.data}/><Link className="button" href="/sentinel">Ask Sentinel on the radar</Link></div></section></div>{open&&<Drawer title="Request a forecast" onClose={()=>setOpen(false)}><RequestForecast scope={scope} onRequested={r=>{setJob(r);setOpen(false);}}/></Drawer>}</>;
}
export function Evidence({ value, depth = 0 }: { value: any; depth?: number }) {
  if (value == null) return null;
  if (typeof value !== "object")
    return (
      <span>
        {typeof value === "boolean" ? (value ? "Yes" : "No") : String(value)}
      </span>
    );
  if (depth > 4)
    return <small>Further detail retained in the source record.</small>;
  if (Array.isArray(value))
    return (
      <div>
        {value.slice(0, 50).map((v, i) => (
          <div className="detail-line" key={i}>
            <Evidence value={v} depth={depth + 1} />
          </div>
        ))}
        {value.length > 50 && (
          <p>
            {value.length - 50} additional entries retained in this assessment.
          </p>
        )}
      </div>
    );
  return (
    <div>
      {Object.entries(value)
        .filter(
          ([k]) =>
            ![
              "fingerprint",
              "token",
              "question",
              "points",
              "cells",
              "snapshot",
            ].includes(k),
        )
        .map(([k, v]) => (
          <div
            key={k}
            className={
              typeof v === "object" ? "evidence-section" : "detail-line"
            }
          >
            {typeof v === "object" ? (
              <details>
                <summary>{label(k)}</summary>
                <Evidence value={v} depth={depth + 1} />
              </details>
            ) : (
              <>
                <span>{["error_code","last_error","error_message","failure_reason"].includes(k) ? "What happened" : label(k)}</span>
                <strong>
                  <Evidence value={["error_code","last_error","error_message","failure_reason"].includes(k) ? errorMessage({code:String(v)}) : v} depth={depth + 1} />
                </strong>
              </>
            )}
          </div>
        ))}
    </div>
  );
}
function Planning({
  scope,
  revision,
  weather,
}: {
  scope: Scope;
  revision: number;
  weather: boolean;
}) {
  const kind = weather ? "weather" : "resources";
  const [rev, setRev] = useState(0),
    [open, setOpen] = useState(false),
    [selected, setSelected] = useState<Row | null>(null),
    [action, setAction] = useState("");
  const history = usePagedData(
    scope.sentinel + "/planning/" + kind,
    revision + rev,
  );
  const missions = useData(
    weather ? null : scope.ops + "/records?kind=MISSION",
    revision,
  );
  const weatherHistory = useData(
    weather ? null : scope.sentinel + "/planning/weather",
    revision,
  );
  const [recommendation, setRecommendation] = useState<any>(null);
  const [context, setContext] = useState({ mission: "", assessment: "" });
  useEffect(() => {
    setSelected(null);
    const query = new URLSearchParams(window.location.search);
    setContext({
      mission: query.get("mission") || "",
      assessment: query.get("assessment") || "",
    });
    setOpen(!weather && (query.has("mission") || query.has("assessment")));
    setAction("");
  }, [scope.sentinel, weather]);
  useEffect(() => {
    setRecommendation(
      selected?.payload.recommendation
        ? structuredClone(selected.payload.recommendation)
        : null,
    );
  }, [selected]);
  const fields: Field[] = weather
    ? [
        {
          key: "question",
          label: "Preparedness question",
          type: "textarea",
          min: 3,
          max: 2000,
          full: true,
        },
        {
          key: "init_time",
          label: "Forecast publication date and time",
          hint: "Use the time shown on a published forecast. For automatic selection, use Ask Sentinel or request the latest forecast in Radar.",
          type: "datetime-local",
          full: true,
        },
        { key: "from", label: "From", type: "datetime-local" },
        { key: "to", label: "To", type: "datetime-local" },
        {
          key: "include_station_detail",
          label: "Include station temperature detail",
          type: "checkbox",
          full: true,
        },
        {
          key: "mode",
          label: "Run mode",
          value: "EXECUTE",
          options: [
            { value: "EXECUTE", label: "Execute - gather evidence and draft" },
            { value: "PLAN_FIRST", label: "Plan first - record the agent plan before evidence" },
          ],
          full: true,
        },      ]
    : [
        {
          key: "mission_id",
          value: context.mission,
          label: "Mission",
          options: (missions.data?.items || []).map((r: Row) => ({
            value: r.id,
            label: recordTitle(r),
          })),
          full: true,
        },
        {
          key: "question",
          label: "Planning brief",
          type: "textarea",
          min: 3,
          max: 2000,
          full: true,
        },
        {
          key: "source_assessment_id",
          value: context.assessment,
          label: "Weather assessment (optional)",
          required: false,
          options: (weatherHistory.data?.items || []).map((r: Row) => ({
            value: r.id,
            label: date(r.updatedAt) + " · " + r.payload.status,
          })),
          full: true,
        },
        {
          key: "mode",
          label: "Run mode",
          value: "EXECUTE",
          options: [
            { value: "EXECUTE", label: "Execute - gather evidence and draft" },
            { value: "PLAN_FIRST", label: "Plan first - record the agent plan before evidence" },
          ],
          full: true,
        },      ];
  return (
    <>
      {weather && <div className="section-intro"><h2>Saved assessments</h2><p>Review previous findings or run a detailed assessment. For a guided conversation, open Ask Sentinel on the Disaster radar.</p></div>}
      <div className="toolbar">
        <p className="page-intro">
          {weather
            ? "Inspect forecast evidence, uncertainty and what it means for preparation."
            : "Review staffing and equipment together before approving allocations."}
        </p>
        <Button variant="primary" icon="plus" onClick={() => setOpen(true)}>
          {weather ? "New assessment" : "Create proposal"}
        </Button>
      </div>
      {history.error && <Notice error>{history.error}</Notice>}
      {history.loading ? (
        <Loading />
      ) : (
        <section className="panel">
          {history.data?.items?.length ? (
            <div className="table-wrap" tabIndex={0} role="region" aria-label="Scrollable records table">
              <table>
                <thead>
                  <tr>
                    <th>{weather ? "Assessment" : "Proposal"}</th>
                    <th>Model</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th />
                  </tr>
                </thead>
                <tbody>
                  {history.data.items.map((r: Row) => (
                    <tr key={r.id}>
                      <td>
                        <button
                          className="table-link"
                          onClick={() => setSelected(r)}
                        >
                          <strong>{recordTitle(r)}</strong>
                          <small>
                            {r.payload.narrative?.summary?.slice(0, 120) ||
                              r.payload.recommendation?.summary?.slice(
                                0,
                                120,
                              ) ||
                              errorMessage({code:r.payload.error_code,message:r.payload.message}) ||
                              r.payload.request?.question}
                          </small>
                        </button>
                      </td>
                      <td>{r.payload.model || "Pending"}</td>
                      <td>
                        <Badge status={r.payload.status} />
                      </td>
                      <td>{date(r.updatedAt)}</td>
                      <td>
                        <Button
                          icon="chevron"
                          aria-label="Review assessment"
                          onClick={() => setSelected(r)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <Empty
              title={
                weather
                  ? "Build an evidence-led assessment"
                  : "A thoughtful plan starts here"
              }
              body={
                weather
                  ? "Ask a question above or prepare today?s briefing. Detailed assessments are available when you need a specific forecast window."
                  : "Select a mission. The assistant checks eligible volunteers, available stock and existing commitments."
              }
              action={
                <Button onClick={() => setOpen(true)}>
                  {weather ? "New assessment" : "Create proposal"}
                </Button>
              }
            />
          )}
        </section>
      )}
      {history.hasMore && (
        <Button onClick={history.loadMore}>Load more assessments</Button>
      )}
      {open && (
        <Drawer
          title={
            weather
              ? "Weather preparedness assessment"
              : "Resource allocation proposal"
          }
          onClose={() => setOpen(false)}
        >
          <p>
            {weather
              ? "Choose a specific forecast and period for a detailed review. For help choosing dates, close this panel and ask Sentinel in your own words."
              : "Recommendations require your review. Creating a proposal does not reserve people or supplies."}
          </p>
          <Form
            fields={fields}
            submit={weather ? "Analyze forecast" : "Generate proposal"}
            onSubmit={async (v, key) => {
              if (
                weather &&
                (Date.parse(v.to) <= Date.parse(v.from) ||
                  Date.parse(v.to) - Date.parse(v.from) > 48 * 3600000)
              )
                throw new UserFacingError("Choose an end time after the start, with no more than two days between them. Use Ask Sentinel for a broader question.");
              if(weather && Date.parse(v.from)<Date.parse(v.init_time)) throw new UserFacingError("The analysis starts before this forecast was issued. Choose a later start date, or use Ask Sentinel to explore earlier days.");
              if(weather && Date.parse(v.init_time)>Date.now()) throw new UserFacingError("This forecast has not been issued yet. Choose an earlier forecast time.");
              if(weather && [v.init_time,v.from,v.to].some(t=>Date.parse(t)%3600000)) throw new UserFacingError("The minutes must be 00, for example 18:00 rather than 18:35. Choose a published forecast, or use Ask Sentinel to select one automatically.");
              const r = await api(
                scope.sentinel + "/planning/" + kind,
                v,
                "POST",
                undefined,
                key,
              );
              setOpen(false);
              setRev((v) => v + 1);
              setSelected(r);
            }}
          />
        </Drawer>
      )}
      {selected && (
        <Drawer
          title={recordTitle(selected)}
          onClose={() => {
            setSelected(null);
            setAction("");
          }}
        >
          <Badge status={selected.payload.status} />
          <h3 className="section-title">
            {weather ? "Preparedness finding" : "Recommended allocation"}
          </h3>
          <p className="detail-prose">
            {selected.payload.narrative?.summary ||
              selected.payload.recommendation?.summary ||
              errorMessage({code:selected.payload.error_code,message:selected.payload.message})}
          </p>
          <HarnessSteps harness={selected.payload.harness} />
          {weather ? (
            <>
              <Evidence value={selected.payload.narrative} />
              <h3 className="section-title">Source & limitations</h3>
              <Evidence value={selected.payload.source} />
              {selected.payload.sources && <Evidence value={selected.payload.sources} />}
              <Evidence value={selected.payload.evidence} />
              {selected.payload.evidence?.thresholds && (
                <p>
                  Regional thresholds: heat ≥{" "}
                  {selected.payload.evidence.thresholds.heat_c} °C · wind ≥{" "}
                  {selected.payload.evidence.thresholds.wind_mps} m/s · rain ≥{" "}
                  {selected.payload.evidence.thresholds.hourly_rain_mm} mm/h
                </p>
              )}
              <Notice>
                Weather indicators are not a validated local flood-impact
                estimate.
              </Notice>
              {selected.payload.evidence?.points?.length > 0 && <Link
                href={"/resource-planning?assessment=" + selected.id}
                className="button primary"
              >
                Plan resources from this evidence
              </Link>}
            </>
          ) : (
            <>
              <Evidence value={selected.payload.recommendation} />
              <h3 className="section-title">Verified planning context</h3>
              <Evidence value={selected.payload.snapshot} />
              <Evidence value={selected.payload.weather_evidence} />
              {selected.payload.status === "DRAFT" && (
                <div className="action-list">
                  {["approve", "edit", "reject"].map((a) => (
                    <Button
                      key={a}
                      onClick={() => setAction(a)}
                      variant={a === "approve" ? "primary" : ""}
                    >
                      {label(a)} proposal
                    </Button>
                  ))}
                </div>
              )}
              {action && (
                <Form
                  fields={[
                    reason,
                    ...(action === "approve"
                      ? [
                          {
                            key: "route_reviewed",
                            label: "I reviewed access routes",
                            type: "checkbox",
                          },
                          {
                            key: "conditions_reviewed",
                            label: "I reviewed current field conditions",
                            type: "checkbox",
                          },
                        ]
                      : []),
                  ]}
                  submit={label(action) + " proposal"}
                  onSubmit={async (v, key) => {
                    if (
                      action === "approve" &&
                      (!v.route_reviewed || !v.conditions_reviewed)
                    )
                      throw new UserFacingError(
                        "Review routes and current conditions before approving.",
                      );
                    await api(
                      scope.sentinel +
                        "/planning/resources/" +
                        selected.id +
                        "/" +
                        action,
                      {
                        ...v,
                        expected_version: selected.version,
                        ...(action === "edit" ? { recommendation } : {}),
                      },
                      "POST",
                      undefined,
                      key,
                    );
                    setSelected(null);
                    setAction("");
                    setRev((v) => v + 1);
                    scope.refresh();
                  }}
                >
                  {action === "edit" && recommendation && (
                    <PlanEditor
                      value={recommendation}
                      onChange={setRecommendation}
                      snapshot={selected.payload.snapshot}
                    />
                  )}
                </Form>
              )}
            </>
          )}
          <p>
            <small>
              Model: {selected.payload.model || "Not available"} · Created{" "}
              {date(selected.updatedAt)} · Version {selected.version}
            </small>
          </p>
        </Drawer>
      )}
    </>
  );
}
function Assistance({ scope, revision }: { scope: Scope; revision: number }) {
  const [status, setStatus] = useState(""),
    [open, setOpen] = useState(false),
    [selected, setSelected] = useState<Row | null>(null),
    [rev, setRev] = useState(0);
  const list = usePagedData(
    scope.safety + "/cases" + (status ? "?status=" + status : ""),
    revision + rev,
  );
  const detail = useData(
    selected ? scope.safety + "/cases/" + selected.id : null,
    revision + rev,
  );
  const roster = useData(
    "/api/organizations/" + scope.org + "/memberships",
    revision,
  );
  const done = () => {
    setOpen(false);
    setSelected(null);
    setRev((v) => v + 1);
  };
  return (
    <>
      <div className="toolbar">
        <select
          aria-label="Case status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
        >
          <option value="">All cases</option>
          {["OPEN", "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED"].map((s) => (
            <option key={s} value={s}>
              {label(s)}
            </option>
          ))}
        </select>
        <Button variant="primary" icon="plus" onClick={() => setOpen(true)}>
          Record assistance request
        </Button>
      </div>
      {list.error && <Notice error>{list.error}</Notice>}
      {list.loading ? (
        <Loading />
      ) : (
        <section className="panel">
          {list.data?.items?.length ? (
            list.data.items.map((r: Row) => (
              <button
                className="queue-row case-button"
                key={r.id}
                onClick={() => setSelected(r)}
              >
                <span>
                  <strong>{r.payload.summary}</strong>
                  <small>
                    {r.payload.source} · Response due{" "}
                    {date(r.payload.response_due_at)}
                  </small>
                </span>
                <Badge status={r.payload.severity} />
                <Badge status={r.payload.status} />
              </button>
            ))
          ) : (
            <Empty
              title="No assistance cases"
              body="Capture a request, assign responsibility and retain the response timeline."
            />
          )}
        </section>
      )}
      {list.hasMore && <Button onClick={list.loadMore}>Load more cases</Button>}
      {open && (
        <Drawer
          title="Record assistance request"
          onClose={() => setOpen(false)}
        >
          <Form
            fields={[
              {
                key: "summary",
                label: "What assistance is needed?",
                type: "textarea",
                min: 10,
                max: 3000,
                full: true,
              },
              {
                key: "severity",
                label: "Priority",
                options: opts(["URGENT", "HIGH", "STANDARD"]),
              },
              {
                key: "source",
                label: "Received through",
                options: opts(["IN_APP", "PHONE", "RADIO"]),
                value: "PHONE",
              },
              {
                key: "affected_user_id",
                label: "Affected member (optional)",
                required: false,
                options: (roster.data || []).map((m: any) => ({
                  value: String(m.userId),
                  label: (m.user?.fullName || "Member " + m.userId),
                })),
                full: true,
              },
            ]}
            submit="Record request"
            onSubmit={async (v, key) => {
              await api(
                scope.safety + "/cases",
                {
                  ...v,
                  ...(v.affected_user_id
                    ? { affected_user_id: Number(v.affected_user_id) }
                    : {}),
                },
                "POST",
                undefined,
                key,
              );
              done();
            }}
          />
        </Drawer>
      )}
      {selected && (
        <Drawer title="Assistance case" onClose={() => setSelected(null)}>
          <Badge status={selected.payload.status} />
          <p>{selected.payload.summary}</p>
          <Evidence value={selected.payload} />
          <h3 className="section-title">Response timeline</h3>
          <div className="timeline">
            {detail.data?.items?.map((r: Row) => (
              <article key={r.id}>
                <strong>{label(r.payload.action)}</strong>
                <p>{r.payload.reason}</p>
                <small>{date(r.updatedAt)}</small>
              </article>
            ))}
          </div>
          {selected.payload.status !== "RESOLVED" && (
            <Form
              fields={[
                {
                  key: "action",
                  label: "Next action",
                  options: opts(
                    selected.payload.status === "OPEN"
                      ? ["ACKNOWLEDGE", "ESCALATE"]
                      : selected.payload.status === "ACKNOWLEDGED"
                        ? ["START_RESPONSE", "HANDOVER", "RESOLVE", "ESCALATE"]
                        : ["HANDOVER", "RESOLVE", "ESCALATE"],
                  ),
                },
                reason,
                {
                  key: "coordinator_id",
                  label: "Receiving coordinator (handover only)",
                  required: false,
                  options: (roster.data || [])
                    .filter((m: any) =>
                      ["ORG_ADMIN", "COORDINATOR"].includes(m.role),
                    )
                    .map((m: any) => ({
                      value: String(m.userId),
                      label: (m.user?.fullName || "Member " + m.userId),
                    })),
                },
              ]}
              submit="Save decision"
              onSubmit={async (v, key) => {
                if (v.action === "HANDOVER" && !v.coordinator_id)
                  throw new UserFacingError("Choose the receiving coordinator.");
                const { coordinator_id, ...values } = v;
                await api(
                  scope.safety + "/cases/" + selected.id + "/decisions",
                  {
                    ...values,
                    expected_version: (detail.data?.case || selected).version,
                    ...(v.action === "HANDOVER"
                      ? { coordinator_id: Number(coordinator_id) }
                      : {}),
                  },
                  "POST",
                  undefined,
                  key,
                );
                done();
              }}
            />
          )}
        </Drawer>
      )}
    </>
  );
}
