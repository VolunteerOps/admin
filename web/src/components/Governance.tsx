"use client";
import { useState } from "react";
import { api } from "@/lib/api";
import { useData, usePagedData, type Scope } from "@/lib/hooks";
import { label } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Notice } from "./ui";
import { Form, opts, reason, type Field } from "./Forms";
import { Evidence } from "./Sentinel";
export default function Governance({
  scope,
  revision,
}: {
  scope: Scope;
  revision: number;
}) {
  const org = useData("/api/organizations/" + scope.org, revision),
    requests = useData(
      "/api/organizations/" + scope.org + "/join-requests",
      revision,
    ),
    units = useData("/api/organizations/" + scope.org + "/units", revision),
    roster = useData(
      "/api/organizations/" + scope.org + "/memberships",
      revision,
    ),
    directory = usePagedData(
      "/api/organizations/" + scope.org + "/directory",
      revision,
    );
  const [action, setAction] = useState(""),
    [selection, setSelection] = useState<any>(null),
    [notice, setNotice] = useState(""),
    [claim, setClaim] = useState<any>(null);
  let fields: Field[] = [];
  if (action === "unit")
    fields = [
      { key: "name", label: "Unit name", min: 2, max: 120, full: true },
    ];
  if (action === "domain")
    fields = [
      {
        key: "domain",
        label: "Organisation domain",
        hint: "Exact public DNS domain",
        full: true,
      },
      reason,
    ];
  if (action === "recruitment")
    fields = [
      {
        key: "open",
        label: "Publish this workspace for recruitment",
        type: "checkbox",
        full: true,
        value: org.data?.recruitmentOpen,
      },
      reason,
    ];
  if (action === "review")
    fields = [
      {
        key: "decision",
        label: "Decision",
        options: opts(["INVITE", "DECLINE"]),
      },
      reason,
    ];
  if (action === "recruit") fields = [reason];
  if (action === "coordinate")
    fields = [
      {
        key: "partner_organization_id",
        label: "Partner workspace reference",
        full: true,
      },
      {
        key: "lead_organization_id",
        label: "Lead workspace reference",
        value: scope.org,
        full: true,
      },
      {
        key: "resource_reference",
        label: "Shared operation or resource",
        min: 3,
        max: 200,
        full: true,
      },
      {
        key: "expires_in_hours",
        label: "Access duration (hours)",
        type: "number",
        min: 1,
        max: 168,
        step: "1",
        value: 24,
      },
      reason,
    ];
  if (action === "respond")
    fields = [
      { key: "grant_id", label: "Coordination request reference", full: true },
      {
        key: "accept",
        label: "Accept coordination request",
        type: "checkbox",
        full: true,
      },
      reason,
    ];
  if (action === "reserve")
    fields = [
      {
        key: "user_id",
        label: "Responder",
        options: (roster.data || []).map((m: any) => ({
          value: String(m.userId),
          label: m.user.fullName,
        })),
      },
      {
        key: "minutes",
        label: "Reservation duration (minutes)",
        type: "number",
        min: 1,
        max: 1440,
        step: "1",
        value: 60,
      },
      { key: "resource_reference", label: "Operation reference", full: true },
      {
        key: "release",
        label: "Release an existing reservation",
        type: "checkbox",
        full: true,
      },
    ];
  async function submit(v: any) {
    const base = "/api/organizations/" + scope.org;
    if (action === "unit") await api(base + "/units", v);
    if (action === "domain") setClaim(await api(base + "/domains", v));
    if (action === "recruitment")
      await api(
        base + "/recruitment",
        { ...v, expected_version: org.data.version },
        "PUT",
      );
    if (action === "review")
      await api(base + "/join-requests/" + selection.id + "/review", {
        ...v,
        expected_version: selection.version,
      });
    if (action === "recruit")
      await api(base + "/directory/" + selection.id + "/invite", v);
    if (action === "coordinate") await api(base + "/coordination-grants", v);
    if (action === "respond") {
      const { grant_id, ...body } = v;
      await api(base + "/coordination-grants/" + grant_id + "/respond", body);
    }
    if (action === "reserve")
      await api(base + "/responder-reservations", {
        ...v,
        user_id: Number(v.user_id),
      });
    setNotice("Change recorded.");
    setAction("");
    scope.refresh();
  }
  return (
    <>
      {notice && <Notice>{notice}</Notice>}
      {requests.error && <Notice error>{requests.error}</Notice>}
      <div className="split">
        <div className="stack">
          <section className="panel">
            <div className="panel-head">
              <h2>Membership requests</h2>
              <Button onClick={() => setAction("recruitment")}>
                Recruitment settings
              </Button>
            </div>
            {requests.data?.length ? (
              requests.data.map((r: any) => (
                <div className="queue-row" key={r.id}>
                  <span>
                    <strong>
                      {r.user?.fullName ||
                        r.profile?.displayName ||
                        "Membership request"}
                    </strong>
                    <small>{r.message}</small>
                  </span>
                  <Badge status={r.status} />
                  <Button
                    onClick={() => {
                      setSelection(r);
                      setAction("review");
                    }}
                  >
                    Review
                  </Button>
                </div>
              ))
            ) : (
              <Empty
                title="No membership requests"
                body="Publish recruitment or invite people directly from the People page."
              />
            )}
          </section>
          <section className="panel">
            <div className="panel-head">
              <h2>Volunteer discovery</h2>
            </div>
            {directory.data?.items?.length ? (
              directory.data.items.map((p: any) => (
                <div className="queue-row" key={p.id}>
                  <span>
                    <strong>{p.display_name}</strong>
                    <small>
                      {p.region} · {p.biography}
                    </small>
                  </span>
                  <Button
                    onClick={() => {
                      setSelection(p);
                      setAction("recruit");
                    }}
                  >
                    Invite
                  </Button>
                </div>
              ))
            ) : (
              <Empty
                title="No discoverable profiles"
                body="Only volunteers who have opted in appear in the directory."
              />
            )}
          </section>
          <section className="panel panel-body">
            {directory.hasMore && (
              <Button onClick={directory.loadMore}>Load more profiles</Button>
            )}
            <h2>Partner coordination</h2>
            <p>
              Define a lead organisation, a shared operation and an expiry. The
              partner must accept the request.
            </p>
            <div className="action-list">
              <Button onClick={() => setAction("coordinate")}>
                Request coordination
              </Button>
              <Button onClick={() => setAction("respond")}>
                Respond to a request
              </Button>
              <Button onClick={() => setAction("reserve")}>
                Reserve a responder
              </Button>
            </div>
          </section>
        </div>
        <div className="stack">
          <section className="panel">
            <div className="panel-head">
              <h2>Organisation units</h2>
              <Button icon="plus" onClick={() => setAction("unit")}>
                Add unit
              </Button>
            </div>
            {units.data?.length ? (
              units.data.map((u: any) => (
                <div className="queue-row" key={u.id}>
                  <strong>{u.name}</strong>
                </div>
              ))
            ) : (
              <Empty title="No units configured" />
            )}
          </section>
          <section className="panel panel-body">
            <h2>Domain verification</h2>
            <p>Prove control of an organisation domain using a DNS record.</p>
            <Button onClick={() => setAction("domain")}>
              Start domain verification
            </Button>
            {claim && (
              <>
                <Evidence value={claim} />
                <Button
                  onClick={async () => {
                    try {
                      await api(
                        "/api/organizations/" +
                          scope.org +
                          "/domains/" +
                          claim.id +
                          "/verify",
                        {},
                      );
                      setNotice("Domain verification completed.");
                    } catch (e) {
                      setNotice((e as Error).message);
                    }
                  }}
                >
                  Verify DNS record
                </Button>
              </>
            )}
          </section>
        </div>
      </div>
      {action && (
        <Drawer
          title={label(
            {
              unit: "New unit",
              domain: "Verify organisation domain",
              recruitment: "Recruitment settings",
              review: "Review membership",
              recruit: "Invite volunteer",
              coordinate: "Partner coordination",
              respond: "Coordination response",
              reserve: "Responder reservation",
            }[action],
          )}
          onClose={() => setAction("")}
        >
          <Form fields={fields} submit="Save decision" onSubmit={submit} />
        </Drawer>
      )}
    </>
  );
}
