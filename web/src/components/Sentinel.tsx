"use client";
import { UserFacingError } from "@/lib/friendly-errors";
import RequestForecast from "./RequestForecast";
import WeatherChat, {weatherSuggestions} from "./WeatherChat";
import { useEffect, useState } from "react";
import PlanEditor from "./PlanEditor";
import Link from "next/link";
import dynamic from "next/dynamic";
import { api, errorMessage } from "@/lib/api";
import { useData, usePagedData, type Scope } from "@/lib/hooks";
import { date, label, recordTitle, type Row } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Loading, Notice } from "./ui";
import { Form, opts, reason, type Field } from "./Forms";
const MapView = dynamic(() => import("./Map"), {
  ssr: false,
  loading: () => (
    <div className="map-panel">
      <Loading />
    </div>
  ),
});
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
  if (["weather-analysis", "resource-planning"].includes(page))
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
function Radar({
  scope,
  revision,
  deployments,
}: {
  scope: Scope;
  revision: number;
  deployments: boolean;
}) {
  const missions = useData(scope.ops + "/records?kind=MISSION", revision),
    hazards = useData(
      deployments ? null : scope.sentinel + "/hazards",
      revision,
    ),
    forecast = useData(
      deployments ? null : scope.sentinel + "/forecasts/latest",
      revision,
    );
  const [refreshOpen, setRefresh] = useState(false),
    [answer, setAnswer] = useState<any>(null),
    [job, setJob] = useState<any>(null),
    [jobError, setJobError] = useState(""),
    [selected, setSelected] = useState(""),
    [layers, setLayers] = useState({ missions: true, hazards: true });
  const readiness = useData(
    deployments ? null : scope.sentinel + "/readiness",
    revision,
  );
  const [variable, setVariable] = useState("total_precipitation_1hr"),
    [statistic, setStatistic] = useState("mean"),
    [time, setTime] = useState("");
  const variables: Record<string, { name: string; unit: string }> = {
    total_precipitation_1hr: { name: "Hourly rainfall", unit: "mm" },
    temperature_2m: { name: "Temperature", unit: "°C" },
    wind_speed_10m: { name: "Wind speed", unit: "m/s" },
  };
  const times: string[] = Array.from(
    new Set<string>((forecast.data?.cells || []).map((c: any) => c.validTime)),
  ).sort();
  const visibleCells = (forecast.data?.cells || []).filter(
    (c: any) => c.validTime === (times.includes(time) ? time : times[0]),
  );
  useEffect(() => {
    setJob(null);
    setAnswer(null);
    setSelected("");
  }, [scope.sentinel]);
  useEffect(() => {
    if (
      !job?.job_id ||
      ["DONE", "SUCCEEDED", "COMPLETED", "DEAD", "EXPIRED"].includes(job.state)
    )
      return;
    const controller = new AbortController();
    const t = setInterval(
      () =>
        api(
          scope.sentinel + "/jobs/" + job.job_id,
          undefined,
          "GET",
          controller.signal,
        )
          .then((r) => {
            setJob({ ...r, job_id: job.job_id });
            if (["DONE", "SUCCEEDED", "COMPLETED"].includes(r.state))
              scope.refresh();
          })
          .catch((e) => {
            if (!controller.signal.aborted) setJobError(errorMessage(e));
          }),
      3000,
    );
    return () => {
      clearInterval(t);
      controller.abort();
    };
  }, [job?.job_id, job?.state, scope.sentinel]);
  const points = [
    ...visibleCells
      .filter((c: any) => c.values?.[variable]?.[statistic] != null)
      .map((c: any) => ({
        id: `forecast:${c.longitude}:${c.latitude}`,
        title: `${variables[variable].name}: ${Number(c.values[variable][statistic]).toFixed(1)} ${variables[variable].unit}`,
        kind: "forecast",
        latitude: c.latitude,
        longitude: c.longitude,
      })),
    ...(layers.missions
      ? (missions.data?.items || []).map((r: Row) => ({
          id: r.id,
          title: recordTitle(r),
          kind: "mission",
          latitude: r.payload.latitude,
          longitude: r.payload.longitude,
        }))
      : []),
    ...(layers.hazards
      ? (hazards.data?.observations || hazards.data?.items || (Array.isArray(hazards.data) ? hazards.data : [])).map((h: any) => ({
          ...h,
          title: h.title || h.source,
          kind: "hazard",
        }))
      : []),
  ];
  return (
    <>
      <div className="toolbar">
        <div className="toolbar-left">
          <label className="check">
            <input
              type="checkbox"
              checked={layers.missions}
              onChange={(e) =>
                setLayers({ ...layers, missions: e.target.checked })
              }
            />
            Missions
          </label>
          {!deployments && (
            <label className="check">
              <input
                type="checkbox"
                checked={layers.hazards}
                onChange={(e) =>
                  setLayers({ ...layers, hazards: e.target.checked })
                }
              />
              Reported hazards
            </label>
          )}
        </div>
        {!deployments && (
          <div className="action-list"><Link className="button primary" href="/weather-analysis">Ask about preparedness</Link><Button icon="refresh" onClick={() => setRefresh(true)}>Request forecast</Button></div>
        )}
      </div>
      {!deployments && times.length > 0 && (
        <section className="panel panel-body forecast-controls">
          <label>
            Forecast layer
            <select
              value={variable}
              onChange={(e) => setVariable(e.target.value)}
            >
              {Object.entries(variables).map(([id, v]) => (
                <option key={id} value={id}>
                  {v.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Ensemble statistic
            <select
              value={statistic}
              onChange={(e) => setStatistic(e.target.value)}
            >
              {["mean", "p10", "p25", "p50", "p75", "p90"].map((s) => (
                <option key={s}>{s}</option>
              ))}
            </select>
          </label>
          <label>
            Valid time
            <select
              value={times.includes(time) ? time : times[0]}
              onChange={(e) => setTime(e.target.value)}
            >
              {times.map((t) => (
                <option value={t} key={t}>
                  {date(t)}
                </option>
              ))}
            </select>
          </label>
          <small>
            Grid-centre values · {variables[variable].unit}. Percentiles
            describe the forecast ensemble, not local impact probability.
          </small>
        </section>
      )}
      {[
        missions.error,
        hazards.error,
        forecast.error,
        readiness.error,
        jobError,
      ]
        .filter(Boolean)
        .map((e, i) => (
          <Notice error key={i}>
            {e}
          </Notice>
        ))}
      {job && (
        <Notice>
          {["DONE","SUCCEEDED","COMPLETED"].includes(job.state)?"Your forecast is ready.":["DEAD","EXPIRED"].includes(job.state)?"The forecast could not be loaded. Request it again after resolving the problem.":"Your forecast is being prepared."}
          {job.init_time && <span> Published {date(job.init_time)}.</span>}
          {job.last_error && " · " + job.last_error}
        </Notice>
      )}
      <div className={deployments ? "split single" : "split"}>
        <div className="stack">
          <MapView
            scope={scope.sentinel}
            points={points}
            onSelect={setSelected}
          />
          <section className="panel">
            <div className="panel-head">
              <h2>Evidence & locations</h2>
              <span>{points.length} records</span>
            </div>
            {points.length ? (
              points.map((p: any) => (
                <div
                  className={
                    "queue-row " + (selected === p.id ? "selected" : "")
                  }
                  key={p.id}
                >
                  <span>
                    <strong>{p.title}</strong>
                    <small>
                      {label(p.kind)} · {Number(p.latitude).toFixed(3)},{" "}
                      {Number(p.longitude).toFixed(3)}
                    </small>
                  </span>
                  <Badge status={p.kind} />
                </div>
              ))
            ) : (
              <Empty
                title="No mapped evidence"
                body="Mission meeting points and reported hazards appear here when available. No live responder tracking is inferred."
              />
            )}
            {missions.data?.next_cursor && (
              <p className="panel-body">
                More missions exist. Open the mission directory to review
                additional records.
              </p>
            )}
          </section>
        </div>
        {!deployments && (
          <div className="stack">
            <section className="panel">
              <div className="panel-head">
                <h2>Forecast evidence</h2>
              </div>
              <div className="panel-body">
                {forecast.data ? (
                  <Evidence value={forecast.data} />
                ) : (
                  <p>No forecast has been loaded for this region.</p>
                )}
                <Link className="text-button" href="/weather-analysis">
                  Open weather analysis →
                </Link>
                <details>
                  <summary>Provider readiness & coverage</summary>
                  <Evidence value={readiness.data} />
                </details>
              </div>
            </section>
            <section className="panel">
              <div className="panel-head">
                <h2>Ask Sentinel</h2>
              </div>
              <div className="panel-body">
                <Form
                  fields={[
                    {
                      key: "question",
                      label: "What do you need to understand?",
                      type: "textarea",
                      min: 3,
                      max: 2000,
                      full: true,
                    },
                  ]}
                  submit="Review evidence"
                  onSubmit={async (v) =>
                    setAnswer(await api(scope.sentinel + "/agent/chat", v))
                  }
                />
                {answer && (
                  <div className="detail-prose">
                    <h3 className="section-title">Assessment</h3>
                    {answer.answer || answer.summary}
                    <Evidence value={answer.citations || answer.sources} />
                  </div>
                )}
              </div>
            </section>
          </div>
        )}
      </div>
      {refreshOpen && (
        <Drawer title="Request a forecast" onClose={() => setRefresh(false)}>
          <RequestForecast scope={scope} onRequested={result=>{setJob(result);setJobError('');setRefresh(false);}} />
          {job && ["DEAD", "EXPIRED"].includes(job.state) && (
            <Form
              fields={[reason]}
              submit="Retry failed job"
              onSubmit={async (v) =>
                setJob(
                  await api(
                    scope.sentinel + "/jobs/" + job.job_id + "/retry",
                    v,
                  ),
                )
              }
            />
          )}
        </Drawer>
      )}
    </>
  );
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
      ]
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
      ];
  return (
    <>
      {weather && <WeatherChat scope={scope} />} 
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
          {weather ? (
            <>
              <Evidence value={selected.payload.narrative} />
              <h3 className="section-title">Source & limitations</h3>
              <Evidence value={selected.payload.source} />
              {selected.payload.sources && <Evidence value={selected.payload.sources} />}
              <Evidence value={selected.payload.evidence} />
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
