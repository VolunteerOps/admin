"use client";
import { useState } from "react";
import { api } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { Badge, Button, Drawer, Empty, Notice } from "./ui";
import { Form, opts, reason, type Field } from "./Forms";
import { Evidence } from "./Sentinel";

export default function PlatformControls({
  scope,
  revision,
  organizations,
}: {
  scope: Scope;
  revision: number;
  organizations: any[];
}) {
  const overview = useData("/api/platform/overview", revision),
    applications = useData("/api/platform/partnerships", revision);
  const [action, setAction] = useState(""),
    [selected, setSelected] = useState<any>(null),
    [result, setResult] = useState<any>(null);
  const workspaces = organizations.map((o) => ({
    value: o.id,
    label: o.displayName,
  }));
  let fields: Field[] = [];
  if (action === "review")
    fields = [
      {
        key: "status",
        label: "Decision",
        options: opts(["APPROVED", "DECLINED"]),
      },
      {
        key: "organization_id",
        label: "Provisioned workspace (required for approval)",
        options: workspaces,
        required: false,
        full: true,
      },
      reason,
    ];
  if (action === "grant")
    fields = [
      {
        key: "user_id",
        label: "Verified user number",
        type: "number",
        min: 1,
        step: "1",
        value: selected?.userId,
      },
      {
        key: "active",
        label: "Grant platform owner access",
        type: "checkbox",
        value: selected?.active,
        full: true,
      },
      reason,
    ];
  if (action === "support")
    fields = [
      {
        key: "organization_id",
        label: "Workspace",
        options: workspaces,
        full: true,
      },
      {
        key: "user_id",
        label: "Approved support operator",
        options: (overview.data?.grants || [])
          .filter((g: any) => g.active)
          .map((g: any) => ({
            value: String(g.userId),
            label: "Platform operator " + g.userId,
          })),
      },
      {
        key: "expires_in_minutes",
        label: "Expires in minutes",
        type: "number",
        min: 5,
        max: 60,
        step: "1",
        value: 15,
      },
      reason,
    ];
  if (action === "revoke" || action === "inspect")
    fields = [
      { key: "grant_id", label: "Support access reference", full: true },
      ...(action === "revoke" ? [reason] : []),
    ];
  if (action === "dispute")
    fields = [
      { key: "claim_id", label: "Domain claim reference", full: true },
      reason,
    ];
  async function submit(v: any) {
    let response;
    if (action === "review") {
      if (v.status === "APPROVED" && !v.organization_id)
        throw Error("Select the reviewed workspace first.");
      response = await api(
        "/api/platform/partnerships/" + selected.id + "/review",
        v,
      );
    }
    if (action === "grant") {
      const { user_id, ...body } = v;
      response = await api(
        "/api/platform/users/" + user_id + "/grant",
        body,
        "PUT",
      );
    }
    if (action === "support") {
      const { organization_id, ...body } = v;
      response = await api(
        "/api/platform/organizations/" + organization_id + "/support-grants",
        { ...body, user_id: Number(body.user_id) },
      );
    }
    if (action === "inspect")
      response = await api("/api/platform/support-grants/" + v.grant_id);
    if (action === "revoke")
      response = await api(
        "/api/platform/support-grants/" + v.grant_id,
        { reason: v.reason },
        "DELETE",
      );
    if (action === "dispute")
      response = await api("/api/platform/domains/" + v.claim_id + "/dispute", {
        reason: v.reason,
      });
    setResult(response);
    setAction("");
    scope.refresh();
  }
  return (
    <div className="stack">
      <section className="panel">
        <div className="panel-head">
          <h2>Partnership applications</h2>
        </div>
        {applications.error && <Notice error>{applications.error}</Notice>}
        {applications.data?.length ? (
          applications.data.map((a: any) => (
            <div className="queue-row" key={a.id}>
              <span>
                <strong>{a.name}</strong>
                <small>
                  {a.domain} · {a.message}
                </small>
              </span>
              <Badge status={a.status} />
              {a.status === "PENDING" && (
                <Button
                  onClick={() => {
                    setSelected(a);
                    setAction("review");
                  }}
                >
                  Review
                </Button>
              )}
            </div>
          ))
        ) : (
          <Empty title="No partnership applications" />
        )}
      </section>
      <section className="panel">
        <div className="panel-head">
          <h2>Platform ownership</h2>
          <Button
            onClick={() => {
              setSelected(null);
              setAction("grant");
            }}
          >
            Manage owner
          </Button>
        </div>
        {overview.error && <Notice error>{overview.error}</Notice>}
        {overview.data?.grants?.map((g: any) => (
          <div className="queue-row" key={g.userId}>
            <strong>Operator {g.userId}</strong>
            <Badge status={g.active ? "ACTIVE" : "INACTIVE"} />
            <Button
              onClick={() => {
                setSelected(g);
                setAction("grant");
              }}
            >
              Review access
            </Button>
          </div>
        ))}
      </section>
      <section className="panel panel-body">
        <h2>Time-limited support</h2>
        <p>
          Support grants allow reviewed workspace configuration access. They do
          not confer operational membership.
        </p>
        <div className="action-list">
          <Button onClick={() => setAction("support")}>
            Issue support access
          </Button>
          <Button onClick={() => setAction("inspect")}>
            Inspect support grant
          </Button>
          <Button onClick={() => setAction("revoke")}>
            Revoke support grant
          </Button>
          <Button onClick={() => setAction("dispute")}>
            Record domain dispute
          </Button>
        </div>
        {result && <Evidence value={result} />}
        <details>
          <summary>Service configuration</summary>
          <Evidence value={overview.data?.provider_readiness} />
        </details>
      </section>
      {action && (
        <Drawer
          title={
            action === "review"
              ? "Review partnership"
              : action === "grant"
                ? "Manage platform owner"
                : action === "support"
                  ? "Issue support access"
                  : action === "dispute"
                    ? "Record domain dispute"
                    : "Review support grant"
          }
          onClose={() => setAction("")}
        >
          <Form fields={fields} submit="Confirm decision" onSubmit={submit} />
        </Drawer>
      )}
    </div>
  );
}
