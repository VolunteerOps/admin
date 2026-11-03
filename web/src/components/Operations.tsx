"use client";
import { UserFacingError } from "@/lib/friendly-errors";
import MissionChat from "./MissionChat";
import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { date, kinds, label, recordTitle, type Row } from "@/lib/catalog";
import {
  availableStock,
  coverage,
  missionError,
  missionTransitions,
  recordStatuses,
} from "@/lib/business";
import { Badge, Button, Drawer, Empty, Icon, Loading, Notice } from "./ui";
import { Form, opts, reason, type Field } from "./Forms";
const createNames: Record<string, string> = {
  MESSAGE: "New conversation",
  MISSION: "Plan a mission",
  INVENTORY: "Add resource",
  COURSE: "Create course",
  BULLETIN: "Publish bulletin",
  SUPPORT: "Open support case",
};
const baseTitle: Field = {
  key: "title",
  label: "Title",
  min: 3,
  max: 160,
  full: true,
};
export default function Operations({
  page,
  scope,
  revision,
}: {
  page: string;
  scope: Scope;
  revision: number;
}) {
  const kind = kinds[page];
  const [q, setQ] = useState(""),
    [search, setSearch] = useState(""),
    [status, setStatus] = useState(""),
    [cursor, setCursor] = useState(""),
    [prior, setPrior] = useState<Row[]>([]),
    [selected, setSelected] = useState<Row | null>(null),
    [create, setCreate] = useState(false),
    [guided, setGuided] = useState(true),
    [rev, setRev] = useState(0);
  useEffect(() => {
    setStatus("");
    setQ("");
    setSearch("");
    setCursor("");
    setPrior([]);
    setSelected(null);
    setCreate(
      new URLSearchParams(window.location.search).get("create") === "1",
    );
  }, [page, scope.ops]);
  useEffect(() => {
    const t = setTimeout(() => {
      const next = q.trim().length >= 2 ? q.trim() : "";
      if (next === search) return;
      setSearch(next);
      setCursor("");
      setPrior([]);
    }, 300);
    return () => clearTimeout(t);
  }, [q]);
  const query = new URLSearchParams({
    kind: kind || "MISSION",
    ...(status ? { status } : {}),
    ...(search ? { q: search } : {}),
    ...(cursor ? { after: cursor } : {}),
  });
  const { data, error, loading } = useData(
    kind ? scope.ops + "/records?" + query : null,
    revision + rev,
  );
  const rows = [...prior, ...(data?.items || [])];
  useEffect(() => {
    const id = new URLSearchParams(window.location.search).get("record");
    if (id && data?.items)
      setSelected(data.items.find((r: Row) => r.id === id) || null);
  }, [data]);
  const done = () => {
    setSelected(null);
    setCreate(false);
    setCursor("");
    setPrior([]);
    setRev((v) => v + 1);
    scope.refresh();
  };
  if (!kind)
    return (
      <Empty
        title="This page has moved"
        action={
          <Link className="button" href="/overview">
            Return to overview
          </Link>
        }
      />
    );
  return (
    <>
      <div className="toolbar">
        <div className="toolbar-left">
          <div className="search">
            <Icon name="search" size={18} />
            <input
              aria-label="Search records"
              placeholder={
                "Search " +
                (page === "resources" ? "inventory" : page.replace("-", " "))
              }
              value={q}
              onChange={(e) => setQ(e.target.value)}
            />
          </div>
          {recordStatuses(kind).length > 0 && (
            <select
              aria-label="Filter status"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setCursor("");
                setPrior([]);
              }}
            >
              <option value="">All statuses</option>
              {recordStatuses(kind).map((v: string) => (
                <option value={v} key={v}>
                  {label(v)}
                </option>
              ))}
            </select>
          )}
        </div>
        {createNames[kind] && (
          <Button variant="primary" icon="plus" onClick={() => setCreate(true)}>
            {createNames[kind]}
          </Button>
        )}
      </div>
      {error && <Notice error>{error}</Notice>}
      {loading ? (
        <Loading />
      ) : (
        <section className="panel">
          {rows.length ? (
            kind === "COURSE" ? <div className="training-grid">{rows.map((r:Row)=><button key={r.id} className="training-card" onClick={()=>setSelected(r)} aria-label={"Review "+recordTitle(r)}><div className="training-card-top"><Icon name="shield" size={26}/><Badge status={r.payload.status}/></div><span className="eyebrow">{r.payload.skill}</span><h2>{recordTitle(r)}</h2><p>{r.payload.lessons?.length||0} lessons · {r.payload.prerequisites?.length||0} prerequisites</p><div className="progress"><span style={{width:coverage(r.payload.enrolled_count||0,r.payload.capacity||0)+'%'}}/></div><div className="training-card-foot"><small>{r.payload.enrolled_count||0} of {r.payload.capacity} spaces filled</small><Icon name="external"/></div></button>)}</div> :
            <div
              className="table-wrap"
              tabIndex={0}
              role="region"
              aria-label="Scrollable records table"
            >
              <table>
                <thead>
                  <tr>
                    <th>
                      {kind === "INVENTORY"
                        ? "Resource"
                        : kind === "EVENT"
                          ? "Change"
                          : "Details"}
                    </th>
                    <th>
                      {kind === "MISSION"
                        ? "Team"
                        : kind === "INVENTORY"
                          ? "Available"
                          : "Context"}
                    </th>
                    <th>Status</th>
                    <th>Updated</th>
                    <th>
                      <span className="sr-only">Review</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((r: Row) => (
                    <tr key={r.id}>
                      <td>
                        <button
                          className="table-link"
                          onClick={() => setSelected(r)}
                        >
                          <strong>{recordTitle(r)}</strong>
                          <small>
                            {r.payload.meeting_point ||
                              r.payload.location ||
                              r.payload.brief?.slice(0, 100) ||
                              r.payload.body?.slice(0, 100) ||
                              r.payload.reason?.slice(0, 100)}
                          </small>
                        </button>
                      </td>
                      <td>
                        {kind === "MISSION" ? (
                          <>
                            {r.payload.assigned_count || 0} /{" "}
                            {r.payload.capacity}
                            <div className="progress">
                              <span
                                style={{
                                  width:
                                    coverage(
                                      r.payload.assigned_count || 0,
                                      r.payload.capacity || 0,
                                    ) + "%",
                                }}
                              />
                            </div>
                          </>
                        ) : kind === "INVENTORY" ? (
                          <>
                            <strong>
                              {availableStock(
                                r.payload.quantity || 0,
                                r.payload.reserved || 0,
                              )}{" "}
                              {r.payload.unit}
                            </strong>
                            <small>{r.payload.reserved || 0} reserved</small>
                          </>
                        ) : (
                          <span>
                            {r.payload.skill ||
                              (r.payload.submitted_hours &&
                                `${r.payload.submitted_hours} hours`) ||
                              r.payload.record_kind ||
                              r.payload.priority ||
                              "Regional record"}
                          </span>
                        )}
                      </td>
                      <td>
                        <Badge
                          status={
                            r.payload.status ||
                            (kind === "INVENTORY" ? "IN_STOCK" : "RECORDED")
                          }
                        />
                      </td>
                      <td>
                        <small>{date(r.updatedAt)}</small>
                      </td>
                      <td>
                        <Button
                          icon="chevron"
                          variant="quiet"
                          aria-label={"Review " + recordTitle(r)}
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
                search || status
                  ? "No matching records"
                  : `No ${page.replace(/-/g, " ")} yet`
              }
              body={
                search || status
                  ? "Try another search or clear the status filter."
                  : kind === "MISSION"
                    ? "Create a mission brief, choose a meeting point and build your team."
                    : "Records will appear here as your team works in this region."
              }
              action={
                createNames[kind] && (
                  <Button onClick={() => setCreate(true)} icon="plus">
                    {createNames[kind]}
                  </Button>
                )
              }
            />
          )}
          <div className="table-footer">
            <span>{rows.length} records shown</span>
            {data?.next_cursor && (
              <Button
                onClick={() => {
                  setPrior(rows);
                  setCursor(String(data.next_cursor));
                }}
              >
                Load more
              </Button>
            )}
          </div>
        </section>
      )}
      {create && (
        <Drawer title={createNames[kind]} className={kind === "MISSION" && guided ? "mission-chat-drawer" : ""} onClose={() => setCreate(false)}>
          {kind === "MISSION" && <div className="mode-tabs" role="group" aria-label="Mission planning method"><Button onClick={()=>setGuided(true)} aria-pressed={guided}>Plan with assistant</Button><Button onClick={()=>setGuided(false)} aria-pressed={!guided}>Use form</Button></div>}
          {kind === "MISSION" && guided ? <MissionChat scope={scope} done={done}/> : <CreateRecord kind={kind} scope={scope} done={done} />}
        </Drawer>
      )}
      {selected && (
        <Drawer title={recordTitle(selected)} onClose={() => setSelected(null)}>
          <RecordDetail record={selected} scope={scope} done={done} />
        </Drawer>
      )}
    </>
  );
}
export function CreateRecord({
  kind,
  scope,
  done,
}: {
  kind: string;
  scope: Scope;
  done: () => void;
}) {
  const missionOptions = useData(
    kind === "MESSAGE" ? scope.ops + "/records?kind=MISSION" : null,
  );
  const [lessons, setLessons] = useState([
    { title: "", body: "", question: "", choices: ["", ""], answer: 0 },
  ]);
  const missionFields: Field[] = [
    { ...baseTitle, label: "Mission name" },
    {
      key: "brief",
      label: "Mission brief",
      type: "textarea",
      min: 10,
      max: 5000,
      full: true,
    },
    {
      key: "meeting_point",
      label: "Meeting point",
      min: 3,
      max: 500,
      full: true,
    },
    { key: "starts_at", label: "Starts", type: "datetime-local" },
    { key: "ends_at", label: "Ends", type: "datetime-local" },
    {
      key: "capacity",
      label: "Team capacity",
      type: "number",
      min: 1,
      max: 1000,
      step: "1",
      value: 10,
    },
    {
      key: "required_skills",
      label: "Required skills",
      required: false,
      hint: "Separate skills with commas.",
    },
    { key: "latitude", label: "Latitude", type: "number", min: -90, max: 90 },
    {
      key: "longitude",
      label: "Longitude",
      type: "number",
      min: -180,
      max: 180,
    },
  ];
  const fields =
    kind === "MESSAGE"
      ? [
          {
            key: "id",
            label: "Mission",
            options: (missionOptions.data?.items || []).map((r: Row) => ({
              value: r.id,
              label: recordTitle(r),
            })),
            full: true,
          },
          {
            key: "body",
            label: "Message",
            type: "textarea",
            min: 1,
            max: 4000,
            full: true,
          },
        ]
      : kind === "MISSION"
        ? missionFields
        : kind === "INVENTORY"
          ? [
              baseTitle,
              { key: "unit", label: "Unit", value: "items" },
              {
                key: "quantity",
                label: "Initial stock",
                type: "number",
                min: 0,
                max: 1000000,
                step: "1",
              },
              {
                key: "location",
                label: "Storage location",
                min: 3,
                full: true,
              },
            ]
          : kind === "COURSE"
            ? [
                baseTitle,
                {
                  key: "skill",
                  label: "Qualification earned",
                  min: 2,
                  max: 60,
                },
                {
                  key: "capacity",
                  label: "Course capacity",
                  type: "number",
                  min: 1,
                  max: 1000,
                  step: "1",
                  value: 30,
                },
              ]
            : [
                baseTitle,
                {
                  key: "body",
                  label: kind === "BULLETIN" ? "Message" : "How can we help?",
                  type: "textarea",
                  min: 10,
                  max: 4000,
                  full: true,
                },
                ...(kind === "BULLETIN"
                  ? [
                      {
                        key: "priority",
                        label: "Priority",
                        options: opts(["NOTICE", "URGENT"]),
                        value: "NOTICE",
                      },
                    ]
                  : []),
              ];
  return (
    <>
      <p>
        {kind === "MISSION"
          ? "Save a draft first. Review staffing and resources before staging the mission."
          : kind === "BULLETIN"
            ? "Published for members in this region. Delivery through other channels is separate."
            : "Add the details your team needs."}
      </p>
      <Form
        fields={fields}
        submit={kind === "MISSION" ? "Save mission draft" : createNames[kind]}
        onCancel={done}
        onSubmit={async (v, key) => {
          let body: any;
          if (kind === "MISSION") {
            const message = missionError(
              v.title,
              v.brief,
              v.capacity,
              Date.parse(v.starts_at),
              Date.parse(v.ends_at),
              v.latitude,
              v.longitude,
            );
            if (message) throw new UserFacingError(message);
            body = {
              action: "CREATE_MISSION",
              mission: {
                ...v,
                required_skills: (v.required_skills || "")
                  .split(",")
                  .map((s: string) => s.trim())
                  .filter(Boolean),
              },
            };
          } else if (kind === "COURSE") {
            body = {
              action: "CREATE_COURSE",
              ...v,
              lessons,
              prerequisites: [],
            };
          } else
            body = {
              action:
                kind === "MESSAGE"
                  ? "MESSAGE"
                  : kind === "INVENTORY"
                    ? "CREATE_RESOURCE"
                    : kind === "BULLETIN"
                      ? "PUBLISH_BULLETIN"
                      : "SUPPORT",
              ...v,
            };
          await api(scope.ops + "/commands", body, "POST", undefined, key);
          done();
        }}
      >
        {kind === "COURSE" && (
          <>
            <h3 className="section-title">Lessons & assessment</h3>
            {lessons.map((l, i) => (
              <section className="panel panel-body" key={i}>
                <h3>Lesson {i + 1}</h3>
                {lessons.length > 1 && (
                  <Button
                    type="button"
                    onClick={() =>
                      setLessons(lessons.filter((_, j) => j !== i))
                    }
                  >
                    Remove lesson
                  </Button>
                )}
                {["title", "body", "question"].map((k) => (
                  <label key={k}>
                    {label(k)}
                    <textarea
                      required
                      minLength={k === "body" ? 20 : k === "question" ? 10 : 3}
                      value={(l as any)[k]}
                      onChange={(e) =>
                        setLessons(
                          lessons.map((x, j) =>
                            i === j ? { ...x, [k]: e.target.value } : x,
                          ),
                        )
                      }
                    />
                  </label>
                ))}
                {l.choices.map((c, j) => (
                  <label key={j}>
                    Answer option {j + 1}
                    <input
                      required
                      value={c}
                      onChange={(e) =>
                        setLessons(
                          lessons.map((x, n) =>
                            n === i
                              ? {
                                  ...x,
                                  choices: x.choices.map((z, m) =>
                                    m === j ? e.target.value : z,
                                  ),
                                }
                              : x,
                          ),
                        )
                      }
                    />
                  </label>
                ))}
                <label>
                  <span className="action-list">
                    <Button
                      type="button"
                      disabled={l.choices.length >= 5}
                      onClick={() =>
                        setLessons(
                          lessons.map((x, j) =>
                            j === i ? { ...x, choices: [...x.choices, ""] } : x,
                          ),
                        )
                      }
                    >
                      Add answer option
                    </Button>
                    <Button
                      type="button"
                      disabled={l.choices.length <= 2}
                      onClick={() =>
                        setLessons(
                          lessons.map((x, j) =>
                            j === i
                              ? {
                                  ...x,
                                  choices: x.choices.slice(0, -1),
                                  answer: Math.min(
                                    x.answer,
                                    x.choices.length - 2,
                                  ),
                                }
                              : x,
                          ),
                        )
                      }
                    >
                      Remove last option
                    </Button>
                  </span>
                  Correct answer
                  <select
                    value={l.answer}
                    onChange={(e) =>
                      setLessons(
                        lessons.map((x, j) =>
                          j === i
                            ? { ...x, answer: Number(e.target.value) }
                            : x,
                        ),
                      )
                    }
                  >
                    {l.choices.map((_, j) => (
                      <option value={j} key={j}>
                        Option {j + 1}
                      </option>
                    ))}
                  </select>
                </label>
              </section>
            ))}
            <Button
              type="button"
              onClick={() =>
                setLessons([
                  ...lessons,
                  {
                    title: "",
                    body: "",
                    question: "",
                    choices: ["", ""],
                    answer: 0,
                  },
                ])
              }
              disabled={lessons.length >= 20}
              icon="plus"
            >
              Add lesson
            </Button>
          </>
        )}
      </Form>
    </>
  );
}
function RecordDetail({
  record,
  scope,
  done,
}: {
  record: Row;
  scope: Scope;
  done: () => void;
}) {
  const detail = useData(scope.ops + "/records/" + record.id);
  const r: Row = detail.data || record;
  const p = r.payload;
  const [action, setAction] = useState("");
  const related = useData(
    r.kind === "MISSION"
      ? scope.ops + "/records?kind=ASSIGNMENT&parent_id=" + r.id
      : null,
  );
  const stock = useData(
    ["APPLICATION", "INVENTORY"].includes(r.kind)
      ? scope.ops + "/records?kind=INVENTORY"
      : null,
  );
  const [supplies, setSupplies] = useState<Record<string, number>>({});
  const base = { id: r.id, expected_version: r.version };
  const actions: string[] =
    r.kind === "MISSION"
      ? [
          ...missionTransitions(p.status).map((s: string) => "MISSION:" + s),
          "MESSAGE",
          "REPORT",
        ]
      : r.kind === "APPLICATION"
        ? ["ASSIGN"]
        : r.kind === "ASSIGNMENT"
          ? p.status === "ASSIGNED"
            ? ["CHECK_IN", "COMPLETE_ASSIGNMENT"]
            : p.status === "CHECKED_IN"
              ? ["COMPLETE_ASSIGNMENT"]
              : []
          : r.kind === "REPORT"
            ? ["TRIAGE_REPORT"]
            : r.kind === "HOURS"
              ? p.status === "PENDING"
                ? ["APPROVE_HOURS"]
                : []
              : r.kind === "INVENTORY"
                ? ["ADJUST_STOCK", "TRANSFER_STOCK"]
                : r.kind === "COURSE"
                  ? p.status === "DRAFT"
                    ? ["PUBLISH_COURSE"]
                    : []
                  : r.kind === "ENROLLMENT"
                    ? ["REVIEW_TRAINING"]
                    : r.kind === "CREDENTIAL"
                      ? p.status === "VERIFIED"
                        ? ["REVOKE_CREDENTIAL"]
                        : []
                      : r.kind === "BULLETIN"
                        ? p.status === "PUBLISHED"
                          ? ["WITHDRAW_BULLETIN"]
                          : []
                        : r.kind === "SUPPORT"
                          ? ["REPLY_SUPPORT", "TRIAGE_SUPPORT"]
                          : r.kind === "MESSAGE"
                            ? ["MESSAGE", "MODERATE_MESSAGE"]
                            : [];
  let fields: Field[] = [];
  if (
    action.includes("MISSION:") ||
    action === "PUBLISH_COURSE" ||
    action === "CHECK_IN"
  )
    fields = [];
  else if (action === "ASSIGN") fields = [];
  else if (action === "COMPLETE_ASSIGNMENT")
    fields = [
      { key: "hours", label: "Actual hours", type: "number", min: 0, max: 24 },
      {
        key: "note",
        label: "Completion note",
        type: "textarea",
        required: false,
      },
    ];
  else if (action === "APPROVE_HOURS")
    fields = [
      {
        key: "hours",
        label: "Approved hours",
        type: "number",
        min: 0,
        max: 24,
        value: p.submitted_hours,
      },
      reason,
    ];
  else if (action === "ADJUST_STOCK")
    fields = [
      {
        key: "delta",
        label: "Stock change",
        type: "number",
        step: "1",
        hint: "Use a negative value to remove stock.",
      },
      reason,
    ];
  else if (action === "TRANSFER_STOCK")
    fields = [
      {
        key: "target_id",
        label: "Destination stock",
        options: (stock.data?.items || [])
          .filter((s: Row) => s.id !== r.id)
          .map((s: Row) => ({
            value: s.id,
            label: recordTitle(s) + " · " + s.payload.location,
          })),
      },
      { key: "quantity", label: "Quantity", type: "number", min: 1, step: "1" },
      reason,
    ];
  else if (action === "TRIAGE_REPORT")
    fields = [
      {
        key: "status",
        label: "Review decision",
        options: opts(["REVIEWING", "ACTION_REQUIRED", "RESOLVED"]),
      },
      reason,
    ];
  else if (action === "TRIAGE_SUPPORT")
    fields = [
      {
        key: "status",
        label: "Case status",
        options: opts(["OPEN", "IN_PROGRESS", "RESOLVED"]),
      },
      reason,
    ];
  else if (action === "REVIEW_TRAINING")
    fields = [
      { key: "approved", label: "Approve qualification", type: "checkbox" },
      { key: "valid_until", label: "Valid until", type: "datetime-local" },
      reason,
    ];
  else if (
    ["REVOKE_CREDENTIAL", "WITHDRAW_BULLETIN", "MODERATE_MESSAGE"].includes(
      action,
    )
  )
    fields = [reason];
  else
    fields = [
      ...(action === "REPORT" ? [baseTitle] : []),
      {
        key: "body",
        label: "Message",
        type: "textarea",
        min: action === "REPORT" ? 3 : 1,
        max: 4000,
        full: true,
      },
    ];
  return (
    <>
      {detail.error && <Notice error>{detail.error}</Notice>}
      <Badge status={p.status || "RECORDED"} />
      <p className="detail-prose">{p.brief || p.body || p.reason}</p>
      <dl className="detail-grid">
        {Object.entries(p)
          .filter(
            ([k, v]) =>
              [
                "meeting_point",
                "starts_at",
                "ends_at",
                "capacity",
                "assigned_count",
                "location",
                "quantity",
                "reserved",
                "unit",
                "skill",
                "valid_until",
                "submitted_hours",
                "approved_hours",
                "priority",
                "status",
                "delta",
              ].includes(k) && typeof v !== "object",
          )
          .map(([k, v]) => (
            <div key={k}>
              <dt>{label(k)}</dt>
              <dd>
                {k.endsWith("_at") || k === "valid_until" ? date(v) : String(v)}
              </dd>
            </div>
          ))}
      </dl>
      {p.required_skills?.length > 0 && (
        <div>
          <h3 className="section-title">Required qualifications</h3>
          {p.required_skills.map((s: string) => (
            <span className="chip" key={s}>
              {s}
            </span>
          ))}
        </div>
      )}
      {r.kind === "MISSION" && (
        <>
          <h3 className="section-title">Team & preparation</h3>
          <div className="action-list">
            <Link
              className="button"
              href={"/resource-planning?mission=" + r.id}
            >
              Review resource plan
            </Link>
            <Link className="button" href="/applications">
              Review applications
            </Link>
          </div>
          {related.data?.items?.map((a: Row) => (
            <div className="detail-line" key={a.id}>
              <strong>Responder {a.ownerId}</strong>
              <Badge status={a.payload.status} />
            </div>
          ))}
        </>
      )}
      {p.lessons?.map((l: any, i: number) => (
        <details key={i}>
          <summary>{l.title}</summary>
          <p>{l.body}</p>
        </details>
      ))}
      {r.kind === "SUPPORT" &&
        p.replies?.map((reply: any, i: number) => (
          <article className="detail-line" key={i}>
            <p>{reply.body}</p>
            <small>{date(reply.at)}</small>
          </article>
        ))}
      <h3 className="section-title">Actions</h3>
      <div className="action-list">
        {actions.map((a) => (
          <Button
            key={a}
            onClick={() => setAction(a)}
            variant={action === a ? "primary" : ""}
          >
            {a.startsWith("MISSION:")
              ? (
                  {
                    STAGED: "Stage mission",
                    ACTIVE: "Activate mission",
                    PAUSED: "Pause mission",
                    COMPLETED: "Complete mission",
                    CANCELLED: "Cancel mission",
                  } as Record<string, string>
                )[a.split(":")[1]]
              : label(a)}
          </Button>
        ))}
      </div>
      {action && (
        <section className="panel panel-body">
          <h3>
            {action.startsWith("MISSION:")
              ? "Confirm mission change"
              : label(action)}
          </h3>
          <p>Applies to this record at version {r.version}.</p>
          <Form
            key={action}
            fields={fields}
            submit="Confirm change"
            onSubmit={async (v, key) => {
              let body: any = { action, ...base, ...v };
              if (action.startsWith("MISSION:"))
                body = {
                  ...base,
                  action: "TRANSITION_MISSION",
                  status: action.split(":")[1],
                };
              if (action === "ASSIGN")
                body = {
                  ...base,
                  action,
                  user_id: r.ownerId,
                  supplies: Object.entries(supplies)
                    .filter(([, q]) => q > 0)
                    .map(([resource_id, quantity]) => ({
                      resource_id,
                      quantity,
                    })),
                };
              if (action === "MESSAGE")
                body = {
                  action,
                  id: r.kind === "MESSAGE" ? r.parentId : r.id,
                  body: v.body,
                };
              if (action === "REPORT") body = { action, id: r.id, ...v };
              if (action === "TRANSFER_STOCK")
                body.target_version = stock.data.items.find(
                  (s: Row) => s.id === v.target_id,
                )?.version;
              await api(scope.ops + "/commands", body, "POST", undefined, key);
              done();
            }}
          >
            {action === "ASSIGN" && (
              <>
                <p>
                  Assign applicant {r.ownerId}. The backend rechecks
                  qualifications, capacity and commitments.
                </p>
                {stock.data?.items?.map((s: Row) => (
                  <label key={s.id}>
                    {recordTitle(s)} ·{" "}
                    {availableStock(
                      s.payload.quantity || 0,
                      s.payload.reserved || 0,
                    )}{" "}
                    available
                    <input
                      type="number"
                      min="0"
                      max={availableStock(
                        s.payload.quantity || 0,
                        s.payload.reserved || 0,
                      )}
                      step="1"
                      value={supplies[s.id] || 0}
                      onChange={(e) =>
                        setSupplies({
                          ...supplies,
                          [s.id]: Number(e.target.value),
                        })
                      }
                    />
                  </label>
                ))}
              </>
            )}
          </Form>
        </section>
      )}
      <small>
        Updated {date(r.updatedAt)} · Version {r.version}
      </small>
    </>
  );
}
