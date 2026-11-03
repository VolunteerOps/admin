"use client";
import RecordExports from "./RecordExports";
import ImpactByRegion from "./ImpactByRegion";
import { UserFacingError } from "@/lib/friendly-errors";
import RegionSetup from "./RegionSetup";
import { useState } from "react";
import { api, identity } from "@/lib/api";
import { useData, usePagedData, type Scope } from "@/lib/hooks";
import { date, label } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Loading, Notice } from "./ui";
import { Form, opts, reason, type Field } from "./Forms";
import { Evidence } from "./Sentinel";
import PlatformControls from "./PlatformControls";
export default function Administration({
  page,
  scope,
  revision,
  platform = false,
}: {
  page: string;
  scope: Scope;
  revision: number;
  platform?: boolean;
}) {
  if (page === "people") return <People scope={scope} revision={revision} />;
  if (page === "settings")
    return <Settings scope={scope} revision={revision} />;
  if (page === "security")
    return <Dispatch scope={scope} revision={revision} />;
  if (page === "insights")
    return <Insights scope={scope} revision={revision} />;
  if (page === "team-pro")
    return <Sponsorship scope={scope} revision={revision} />;
  return <Platform scope={scope} revision={revision} allowed={platform} />;
}
function People({ scope, revision }: { scope: Scope; revision: number }) {
  const roster = usePagedData(
    "/api/organizations/" + scope.org + "/memberships",
    revision,
    100,
  );
  const { data, error, loading } = roster;
  const invitations = usePagedData(
    "/api/organizations/" + scope.org + "/invitations",
    revision,
    100,
  );
  const [q, setQ] = useState(""),
    [invite, setInvite] = useState(false),
    [member, setMember] = useState<any>(null),
    [notice, setNotice] = useState("");
  const rows = (data || []).filter((m: any) =>
    [(m.user?.fullName || "Member " + m.userId), (m.user?.email || "Contact details unavailable"), m.role]
      .join(" ")
      .toLowerCase()
      .includes(q.toLowerCase()),
  );
  return (
    <>
      <div className="toolbar">
        <div className="search">
          <input
            aria-label="Search members"
            placeholder="Search people, email or role"
            value={q}
            onChange={(e) => setQ(e.target.value)}
          />
        </div>
        <Button icon="plus" variant="primary" onClick={() => setInvite(true)}>
          Invite a person
        </Button>
      </div>
      {error && <Notice error>{error}</Notice>}
      {notice && <Notice>{notice}</Notice>}
      {loading ? (
        <Loading />
      ) : (
        <section className="panel">
          <div className="table-wrap" tabIndex={0} role="region" aria-label="Scrollable records table">
            <table>
              <thead>
                <tr>
                  <th>Person</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Scope</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {rows.map((m: any) => (
                  <tr key={m.id}>
                    <td>
                      <button
                        className="table-link"
                        onClick={() => setMember(m)}
                      >
                        <strong>{(m.user?.fullName || "Member " + m.userId)}</strong>
                        <small>{(m.user?.email || "Contact details unavailable")}</small>
                      </button>
                    </td>
                    <td>{label(m.role)}</td>
                    <td>
                      <Badge status={m.status} />
                    </td>
                    <td>
                      {(m.regionIds || []).length
                        ? `${(m.regionIds || []).length} regions`
                        : "Workspace-wide"}
                    </td>
                    <td>
                      <Button
                        icon="chevron"
                        aria-label={"Review " + (m.user?.fullName || "Member " + m.userId)}
                        onClick={() => setMember(m)}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!rows.length && (
            <Empty
              title="No matching people"
              body="Invite members to build your coordination team."
            />
          )}
        </section>
      )}
      {roster.hasMore && (
        <Button onClick={roster.loadMore}>Load more people</Button>
      )}
      <h2 className="section-title">Pending invitations</h2>
      <section className="panel">
        {(invitations.data || [])
          .filter((i: any) => !i.acceptedAt && !i.revokedAt && !i.declinedAt)
          .map((i: any) => (
            <div className="queue-row" key={i.id}>
              <span>
                <strong>{i.recipientEmail}</strong>
                <small>
                  {label(i.role)} · Expires {date(i.expiresAt)}
                </small>
              </span>
              <Badge status={i.deliveryStatus || "PENDING"} />
              <Button onClick={() => setMember({ ...i, isInvitation: true })}>
                Manage
              </Button>
            </div>
          ))}
        {!invitations.data?.length && <Empty title="No invitations pending" />}
      </section>
      {invitations.hasMore && (
        <Button onClick={invitations.loadMore}>Load more invitations</Button>
      )}
      {invite && (
        <Drawer
          title="Invite to your workspace"
          onClose={() => setInvite(false)}
        >
          <Form
            fields={[
              {
                key: "recipient_email",
                label: "Email address",
                type: "email",
                full: true,
              },
              {
                key: "role",
                label: "Workspace role",
                options: opts(
                  scope.role === "ORG_ADMIN"
                    ? ["VOLUNTEER", "TEAM_LEAD", "COORDINATOR", "ORG_ADMIN"]
                    : ["VOLUNTEER", "COORDINATOR"],
                ),
              },
              {
                key: "expires_in_hours",
                label: "Expires in hours",
                type: "number",
                min: 1,
                max: 168,
                step: "1",
                value: 48,
              },
              reason,
            ]}
            submit="Send invitation"
            onSubmit={async (v) => {
              await api("/api/organizations/" + scope.org + "/invitations", {
                ...v,
                region_ids: [],
              });
              setInvite(false);
              scope.refresh();
              setNotice(
                "Invitation created. The recipient must accept it before membership begins.",
              );
            }}
          />
        </Drawer>
      )}
      {member && (
        <Drawer
          title={
            member.isInvitation ? "Manage invitation" : (member.user?.fullName || "Workspace member")
          }
          onClose={() => setMember(null)}
        >
          <Evidence
            value={
              member.isInvitation
                ? {
                    email: member.recipientEmail,
                    role: member.role,
                    expires: member.expiresAt,
                  }
                : {
                    email: member.user?.email,
                    role: member.role,
                    status: member.status,
                    regions: member.regionIds,
                  }
            }
          />
          <Form
            fields={[
              {
                key: "decision",
                label: "Action",
                options: opts(
                  member.isInvitation
                    ? ["RESEND", "REVOKE"]
                    : ["SUSPENDED", "REMOVED"],
                ),
              },
              reason,
            ]}
            submit="Confirm change"
            onSubmit={async (v) => {
              if (member.isInvitation) {
                const path =
                  "/api/organizations/" +
                  scope.org +
                  "/invitations/" +
                  member.id;
                await api(
                  path + (v.decision === "RESEND" ? "/resend" : ""),
                  { reason: v.reason },
                  v.decision === "RESEND" ? "POST" : "DELETE",
                );
              } else
                await api(
                  "/api/organizations/" +
                    scope.org +
                    "/memberships/" +
                    member.id,
                  {
                    status: v.decision,
                    reason: v.reason,
                    expected_version: member.version,
                  },
                  "PATCH",
                );
              setMember(null);
              scope.refresh();
            }}
          />
        </Drawer>
      )}
    </>
  );
}
function Settings({ scope, revision }: { scope: Scope; revision: number }) {
  const org = useData("/api/organizations/" + scope.org, revision),
    regions = useData(
      "/api/organizations/" + scope.org + "/sentinel/regions",
      revision,
    );
  const [editing, setEditing] = useState<any>(null),
    [notice, setNotice] = useState("");
  return (
    <>
      <div className="split">
        <div className="stack">
          <section className="panel">
            <div className="panel-head">
              <h2>Operating regions</h2>
              <Button icon="plus" onClick={() => setEditing({})}>
                Add region
              </Button>
            </div>
            {regions.error && <Notice error>{regions.error}</Notice>}
            {regions.data?.length ? (
              regions.data.map((r: any) => (
                <div className="queue-row" key={r.id}>
                  <span>
                    <strong>{r.name}</strong>
                    <small>
                      Forecast policy v{r.forecastPolicy?.version || 1}
                    </small>
                  </span>
                  <Button onClick={() => setEditing(r)}>Edit coverage</Button>
                </div>
              ))
            ) : (
              <Empty
                title="Define your first region"
                body="A region connects missions, people and forecast coverage."
                action={
                  <Button variant="primary" onClick={() => setEditing({})}>
                    Add region
                  </Button>
                }
              />
            )}
          </section>
          <section className="panel">
            <div className="panel-head">
              <h2>Organisation domain</h2>
            </div>
            <div className="panel-body">
              <p>
                Register an email domain for review. Claiming a domain does not
                grant membership.
              </p>
              <Form
                fields={[
                  {
                    key: "domain",
                    label: "Domain",
                    hint: "For example: resq-app.co.ke",
                    full: true,
                  },
                  reason,
                ]}
                submit="Register domain"
                onSubmit={async (v) => {
                  await api("/api/organizations/" + scope.org + "/domains", v);
                  setNotice(
                    "Domain claim recorded. Follow the verification instructions in the platform workspace.",
                  );
                }}
              />
            </div>
          </section>
        </div>
        <section className="panel">
          <div className="panel-head">
            <h2>Workspace details</h2>
          </div>
          <div className="panel-body">
            {org.error ? (
              <Notice error>{org.error}</Notice>
            ) : (
              <Evidence value={org.data} />
            )}
            <h3 className="section-title">Sign-in policy</h3>
            <p>
              Email and password. Approved membership determines access to
              operational data.
            </p>
          </div>
        </section>
      </div>
      {notice && <Notice>{notice}</Notice>}
      {editing && (
        <Drawer
          title={editing.id ? "Edit region" : "New operating region"}
          onClose={() => setEditing(null)}
        >
          {!editing.advanced && <RegionSetup scope={scope} editing={editing} done={()=>{setEditing(null);scope.refresh();}} />}
          <Button variant="quiet" onClick={()=>setEditing({...editing,advanced:!editing.advanced})}>{editing.advanced ? "Use place search" : "Advanced settings"}</Button>
          {editing.advanced && <>
          <p>
            Choose geographic coverage and operational thresholds. Changes
            require a reason and are audited.
          </p>
          <Form
            fields={[
              {
                key: "name",
                label: "Region name",
                min: 2,
                max: 120,
                value: editing.name,
                full: true,
              },
              ...["west", "south", "east", "north"].map((k) => ({
                key: k,
                label: label(k) + " boundary",
                type: "number",
                min: ["west", "east"].includes(k) ? -180 : -90,
                max: ["west", "east"].includes(k) ? 180 : 90,
                value: editing.forecastBounds?.[k],
              })),
              {
                key: "heat_c",
                label: "Heat threshold (°C)",
                type: "number",
                min: 25,
                max: 60,
                value: editing.forecastPolicy?.heat_c ?? 35,
              },
              {
                key: "wind_mps",
                label: "Wind threshold (m/s)",
                type: "number",
                min: 5,
                max: 100,
                value: editing.forecastPolicy?.wind_mps ?? 15,
              },
              {
                key: "hourly_rain_mm",
                label: "Hourly rain threshold (mm)",
                type: "number",
                min: 1,
                max: 500,
                value: editing.forecastPolicy?.hourly_rain_mm ?? 20,
              },
              {
                key: "maximum_run_age_hours",
                label: "Maximum forecast age (hours)",
                type: "number",
                min: 1,
                max: 48,
                step: "1",
                value: editing.forecastPolicy?.maximum_run_age_hours ?? 8,
              },
              reason,
            ]}
            submit="Save region"
            onSubmit={async (v) => {
              if (v.south >= v.north) throw new UserFacingError("The northern edge must be farther north than the southern edge. Use place search to fill the coverage automatically.");
              await api(
                "/api/organizations/" +
                  scope.org +
                  "/sentinel/regions/" +
                  (editing.id || crypto.randomUUID()),
                {
                  name: v.name,
                  bounds: {
                    west: v.west,
                    east: v.east,
                    south: v.south,
                    north: v.north,
                  },
                  policy: {
                    version: (editing.forecastPolicy?.version || 0) + 1,
                    heat_c: v.heat_c,
                    wind_mps: v.wind_mps,
                    hourly_rain_mm: v.hourly_rain_mm,
                    maximum_run_age_hours: v.maximum_run_age_hours,
                  },
                  reason: v.reason,
                },
                "PUT",
              );
              setEditing(null);
              scope.refresh();
            }}
          />
          </>}
        </Drawer>
      )}
    </>
  );
}
function Dispatch({ scope, revision }: { scope: Scope; revision: number }) {
  const { data, error, loading } = useData(scope.ops + "/dispatch", revision);
  const [notice, setNotice] = useState("");
  if (loading) return <Loading />;
  return (
    <section className="panel">
      <div className="panel-head">
        <h2>Regional dispatch</h2>
        <Badge status={data?.paused ? "PAUSED" : "ACTIVE"} />
      </div>
      <div className="panel-body">
        {error && <Notice error>{error}</Notice>}
        <p>
          Pause new dispatch actions while conditions are reviewed. Existing
          records and evidence remain available.
        </p>
        {notice && <Notice>{notice}</Notice>}
        <Form
          fields={[
            {
              key: "paused",
              label: "Pause new dispatch actions",
              type: "checkbox",
              value: data?.paused,
              full: true,
            },
            reason,
          ]}
          submit="Update dispatch control"
          onSubmit={async (v, key) => {
            await api(
              scope.ops + "/commands",
              {
                action: "SET_DISPATCH",
                expected_version: data?.version ?? 0,
                ...v,
              },
              "POST",
              undefined,
              key,
            );
            setNotice("Dispatch control updated.");
            scope.refresh();
          }}
        />
      </div>
    </section>
  );
}
function Insights({ scope, revision }: { scope: Scope; revision: number }) {
  const [range, setRange] = useState(() => ({
    from: new Date(Date.now() - 7 * 86400000).toISOString(),
    to: new Date().toISOString(),
  }));
  const { data, error, loading } = useData(
    scope.ops + "/metrics?" + new URLSearchParams(range),
    revision,
  );
  const [notice, setNotice] = useState("");
  return (
    <>
      <section className="panel panel-body">
        <Form
          fields={[
            { key: "from", label: "From", type: "datetime-local", value: new Date(Date.parse(range.from)-new Date(range.from).getTimezoneOffset()*60000).toISOString().slice(0,16) },
            { key: "to", label: "To", type: "datetime-local", value: new Date(Date.parse(range.to)-new Date(range.to).getTimezoneOffset()*60000).toISOString().slice(0,16) },
          ]}
          submit="Apply interval"
          onSubmit={async (v) => {
            if (
              Date.parse(v.to) - Date.parse(v.from) > 31 * 86400000 ||
              Date.parse(v.to) <= Date.parse(v.from)
            )
              throw new UserFacingError("Choose an end date after the start, with no more than 31 days between them.");
            setRange({ from: v.from, to: v.to });
          }}
        />
      </section>
      {error && <Notice error>{error}</Notice>}
      {loading ? (
        <Loading />
      ) : (
        data && (
          <>
            <div className="metrics">
              {[
                ["Completed", data.completed_missions],
                ["Recorded hours", data.recorded_hours],
                  ["Approved hours", data.approved_hours],
                  ["Verified qualifications", data.verified_qualifications],

              ].map(([k, v]) => (
                <div className="metric" key={k}>
                  <span className="metric-label">{k}</span>
                  <strong>{v ?? "?"}</strong>
                  <small>Selected reporting interval</small>
                </div>
              ))}
            </div>
            <section className="panel">
              <div className="panel-head">
                <h2>Mission outcomes</h2>
                <Button
                  icon="download"
                  onClick={async () => {
                    try {
                      const token = await (
                        await identity()
                      ).currentUser?.getIdToken();
                      const r = await fetch(
                        scope.ops +
                          "/metrics/export?" +
                          new URLSearchParams(range),
                        { headers: { Authorization: "Bearer " + token } },
                      );
                      if (!r.ok)
                        throw new UserFacingError("Export is unavailable for this account.");
                      const href = URL.createObjectURL(await r.blob());
                      const a = document.createElement("a");
                      a.href = href;
                      a.download = "resq-operations.csv";
                      a.click();
                      URL.revokeObjectURL(href);
                    } catch (e) {
                      setNotice((e as Error).message);
                    }
                  }}
                >
                  Export CSV
                </Button>
              </div>
              <div className="panel-body">
                <div className="chart">
                  {[
                    ["Total", data.mission_count],
                    ["Active", data.active_missions],
                    ["Complete", data.completed_missions],
                    ["Reports", data.report_count],
                  ].map(([k, v]) => (
                    <div
                      className="chart-bar"
                      key={k}
                      style={{
                        height: Math.max(
                          2,
                          (Number(v) /
                            Math.max(
                              1,
                              data.mission_count,
                              data.report_count,
                            )) *
                            150,
                        ),
                      }}
                    >
                      <span>{v}</span>
                      <small>{k}</small>
                    </div>
                  ))}
                </div>
                <p>
                  Counts are based on records created within the selected
                  interval.
                </p>
              </div>
            </section>
          </>
        )
      )}
      <ImpactByRegion scope={scope} range={range} revision={revision}/><RecordExports scope={scope} range={range}/>{notice && <Notice error>{notice}</Notice>}
    </>
  );
}
function Sponsorship({ scope, revision }: { scope: Scope; revision: number }) {
  const seats = useData(
      "/api/organizations/" + scope.org + "/pro/sponsorships",
      revision,
    ),
    roster = useData(
      "/api/organizations/" + scope.org + "/memberships",
      revision,
    );
  const [notice, setNotice] = useState("");
  return (
    <div className="split">
      <section className="panel">
        <div className="panel-head">
          <h2>Sponsored volunteers</h2>
        </div>
        {seats.error && <Notice error>{seats.error}</Notice>}
        {seats.data?.map((s: any) => (
          <div className="queue-row" key={s.id}>
            <span>
              <strong>
                {roster.data?.find((m: any) => m.userId === s.userId)?.user
                  .fullName || "Volunteer " + s.userId}
              </strong>
              <small>Version {s.version}</small>
            </span>
            <Badge status={s.active ? "ACTIVE" : "INACTIVE"} />
          </div>
        ))}
        {!seats.data?.length && (
          <Empty
            title="No sponsorships yet"
            body="An eligible coordinator can sponsor up to three volunteers."
          />
        )}
      </section>
      <section className="panel panel-body">
        <h2>Manage a seat</h2>
        <p>Only active volunteers in this workspace can receive sponsorship.</p>
        {notice && <Notice>{notice}</Notice>}
        <Form
          fields={[
            {
              key: "user_id",
              label: "Volunteer",
              options: (roster.data || [])
                .filter(
                  (m: any) => m.role === "VOLUNTEER" && m.status === "ACTIVE",
                )
                .map((m: any) => ({
                  value: String(m.userId),
                  label: (m.user?.fullName || "Member " + m.userId),
                })),
              full: true,
            },
            {
              key: "active",
              label: "Enable sponsorship",
              type: "checkbox",
              value: true,
              full: true,
            },
          ]}
          submit="Save sponsorship"
          onSubmit={async (v) => {
            const seat = seats.data?.find(
              (s: any) => s.userId === Number(v.user_id),
            );
            await api(
              "/api/organizations/" + scope.org + "/pro/sponsorships",
              {
                user_id: Number(v.user_id),
                active: v.active,
                expected_version: seat?.version || 0,
              },
              "PUT",
            );
            setNotice("Sponsorship updated.");
            scope.refresh();
          }}
        />
      </section>
    </div>
  );
}
function Platform({
  scope,
  revision,
  allowed,
}: {
  scope: Scope;
  revision: number;
  allowed: boolean;
}) {
  const list = usePagedData(
    allowed ? "/api/platform/organizations" : null,
    revision,
    100,
  );
  const [open, setOpen] = useState(false),
    [org, setOrg] = useState<any>(null),
    [action, setAction] = useState("activate");
  if (!allowed)
    return (
      <Empty
        title="Platform access required"
        body="This area is reserved for approved platform owners. Organisation membership does not grant platform authority."
      />
    );
  return (
    <>
      <div className="toolbar">
        <p>
          Provision and review organisations independently of their operational
          membership.
        </p>
        <Button variant="primary" onClick={() => setOpen(true)} icon="plus">
          Provision workspace
        </Button>
      </div>
      {list.error && <Notice error>{list.error}</Notice>}
      <section className="panel">
        {list.data?.map((o: any) => (
          <div className="queue-row" key={o.id}>
            <span>
              <strong>{o.displayName}</strong>
              <small>{o.slug}</small>
            </span>
            <Badge status={o.status} />
            <Button
              onClick={() => {
                setOrg(o);
                setAction(
                  o.status === "PROVISIONED" || o.status === "SUSPENDED"
                    ? "activate"
                    : o.status === "OFFBOARDING"
                      ? "complete-offboarding"
                      : o.enrollmentReady
                        ? "invitations"
                        : "verification",
                );
              }}
            >
              Review
            </Button>
          </div>
        ))}
        {!list.data?.length && (
          <Empty
            title="Set up the first workspace"
            action={
              <Button onClick={() => setOpen(true)}>Provision workspace</Button>
            }
          />
        )}
      </section>
      {list.hasMore && (
        <Button onClick={list.loadMore}>Load more workspaces</Button>
      )}
      <PlatformControls
        scope={scope}
        revision={revision}
        organizations={list.data || []}
      />
      {open && (
        <Drawer title="Provision a workspace" onClose={() => setOpen(false)}>
          <Form
            fields={[
              {
                key: "display_name",
                label: "Organisation name",
                min: 2,
                max: 160,
                full: true,
              },
              {
                key: "slug",
                label: "Workspace address",
                hint: "Lowercase letters, numbers and hyphens.",
                min: 3,
                max: 63,
                full: true,
              },
              reason,
            ]}
            submit="Provision workspace"
            onSubmit={async (v) => {
              await api("/api/platform/organizations", {
                ...v,
                allowed_providers: ["password"],
                require_mfa: false,
              });
              setOpen(false);
              scope.refresh();
            }}
          />
        </Drawer>
      )}
      {org && (
        <Drawer title={org.displayName} onClose={() => setOrg(null)}>
          <Evidence
            value={{
              status: org.status,
              verified: org.enrollmentReady,
              version: org.version,
            }}
          />
          <div className="action-list">
            {[
              ...(org.status === "PROVISIONED" || org.status === "SUSPENDED"
                ? ["activate"]
                : []),
              ...(org.status === "ACTIVE"
                ? ["verification", "invitations", "suspend"]
                : []),
              ...(org.status === "SUSPENDED" ? ["offboard"] : []),
              ...(org.status === "OFFBOARDING" ? ["complete-offboarding"] : []),
            ].map((a) => (
              <Button key={a} onClick={() => setAction(a)}>
                {label(a)}
              </Button>
            ))}
          </div>
          <Form
            key={action}
            fields={[
              ...(action === "invitations"
                ? [
                    {
                      key: "recipient_email",
                      label: "Administrator email",
                      type: "email",
                      full: true,
                    },
                    {
                      key: "role",
                      label: "Role",
                      options: opts(["ORG_ADMIN", "COORDINATOR", "VOLUNTEER"]),
                      value: "ORG_ADMIN",
                    },
                  ]
                : []),
              ...(["verification", "complete-offboarding"].includes(action)
                ? [
                    {
                      key: "evidence_reference",
                      label: "Review evidence reference",
                      min: 10,
                      full: true,
                    },
                  ]
                : []),
              reason,
            ]}
            submit={label(action)}
            onSubmit={async (v) => {
              await api(
                "/api/platform/organizations/" + org.id + "/" + action,
                {
                  ...v,
                  ...(action === "invitations"
                    ? { region_ids: [], expires_in_hours: 48 }
                    : { expected_version: org.version }),
                },
              );
              setOrg(null);
              scope.refresh();
            }}
          />
        </Drawer>
      )}
    </>
  );
}
