"use client";
import { UserFacingError } from "@/lib/friendly-errors";
import { useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { Badge, Button, Drawer, Empty, Notice } from "./ui";
import { Form, opts, reason } from "./Forms";

export default function AccountAccess({
  scope,
  revision,
  privacy = false,
}: {
  scope: Scope;
  revision: number;
  privacy?: boolean;
}) {
  const joins = useData(privacy ? null : "/api/me/join-requests", revision),
    partnerships = useData(privacy ? null : "/api/me/partnerships", revision);
  const [action, setAction] = useState(""),
    [notice, setNotice] = useState(""),
    [error, setError] = useState("");
  async function cancel(id: string) {
    try {
      await api("/api/me/join-requests/" + id, {}, "DELETE");
      scope.refresh();
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  return (
    <>
      {error && <Notice error>{error}</Notice>}
      {notice && <Notice>{notice}</Notice>}
      {privacy ? (
        <section className="panel panel-body">
          <h2>Consent & contact</h2>
          <p>
            Manage optional participation or stop recruitment from an
            organisation.
          </p>
          <div className="action-list">
            <Button onClick={() => setAction("consent")}>
              Record consent preference
            </Button>
            <Button onClick={() => setAction("block")}>
              Block recruitment
            </Button>
          </div>
        </section>
      ) : (
        <>
          <section className="panel">
            <div className="panel-head">
              <h2>Your membership requests</h2>
            </div>
            {joins.error && <Notice error>{joins.error}</Notice>}
            {joins.data?.length ? (
              joins.data.map((j: any) => (
                <div className="queue-row" key={j.id}>
                  <span>
                    <strong>
                      {j.organization?.displayName || "Membership request"}
                    </strong>
                    <small>{j.message}</small>
                  </span>
                  <Badge status={j.status} />
                  {j.status === "PENDING" && (
                    <Button onClick={() => cancel(j.id)}>Withdraw</Button>
                  )}
                </div>
              ))
            ) : (
              <Empty title="No outstanding requests" />
            )}
          </section>
          <section className="panel">
            <div className="panel-head">
              <h2>Organisation partnerships</h2>
              <Button onClick={() => setAction("partner")}>
                Apply for a workspace
              </Button>
            </div>
            {partnerships.data?.map((p: any) => (
              <div className="queue-row" key={p.id}>
                <span>
                  <strong>{p.name}</strong>
                  <small>{p.domain}</small>
                </span>
                <Badge status={p.status} />
              </div>
            ))}
            {!partnerships.data?.length && (
              <p className="panel-body">
                Apply to coordinate on behalf of an organisation. A platform
                owner reviews the request.
              </p>
            )}
          </section>
          {scope.org && (
            <section className="panel panel-body">
              <h2>Leave this workspace</h2>
              <p>
                Your personal account stays available. Administrators must
                complete a handover before leaving.
              </p>
              <Button onClick={() => setAction("leave")}>
                Review departure
              </Button>
            </section>
          )}
        </>
      )}
      {action && (
        <Drawer
          title={
            action === "partner"
              ? "Organisation partnership"
              : action === "consent"
                ? "Consent preference"
                : action === "block"
                  ? "Block recruitment"
                  : "Leave workspace"
          }
          onClose={() => setAction("")}
        >
          <Form
            fields={
              action === "partner"
                ? [
                    {
                      key: "name",
                      label: "Organisation name",
                      min: 2,
                      max: 160,
                      full: true,
                    },
                    { key: "domain", label: "Organisation domain", full: true },
                    {
                      key: "message",
                      label: "Describe your coordination needs",
                      type: "textarea",
                      min: 10,
                      max: 1000,
                      full: true,
                    },
                  ]
                : action === "consent"
                  ? [
                      {
                        key: "purpose",
                        label: "Purpose",
                        options: opts([
                          "DIRECTORY",
                          "PUBLIC_PROFILE",
                          "UPDATES",
                          "SENSING",
                          "LOCATION",
                          "RELAY",
                        ]),
                      },
                      {
                        key: "policy_version",
                        label: "Policy version",
                        min: 1,
                        max: 80,
                        full: true,
                      },
                      {
                        key: "granted",
                        label: "I consent to this purpose",
                        type: "checkbox",
                        full: true,
                      },
                    ]
                  : action === "block"
                    ? [
                        {
                          key: "organization_id",
                          label: "Organisation reference",
                          full: true,
                        },
                        reason,
                      ]
                    : [
                        {
                          key: "confirmation",
                          label: "Type LEAVE to confirm",
                          full: true,
                        },
                      ]
            }
            submit="Confirm choice"
            onSubmit={async (v) => {
              if (action === "partner") await api("/api/me/partnerships", v);
              if (action === "consent") await api("/api/me/consents", v);
              if (action === "block")
                await api("/api/me/recruitment-blocks", v);
              if (action === "leave") {
                if (v.confirmation !== "LEAVE")
                  throw new UserFacingError("Type LEAVE to continue.");
                await api("/api/organizations/" + scope.org + "/leave", {});
              }
              setAction("");
              setNotice("Your choice has been recorded.");
              scope.refresh();
            }}
          />
        </Drawer>
      )}
    </>
  );
}
