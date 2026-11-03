"use client";
import { useState } from "react";
import { api, errorMessage } from "@/lib/api";
import { useData } from "@/lib/hooks";
import { date } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Notice } from "./ui";
import { Form, reason } from "./Forms";
export default function Promotions({
  revision,
  refresh,
}: {
  revision: number;
  refresh: () => void;
}) {
  const data = useData("/api/platform/promotions", revision),
    [adding, setAdding] = useState(false),
    [edit, setEdit] = useState<any>(null),
    [error, setError] = useState("");
  return (
    <>
      <section className="panel">
        <div className="panel-head">
          <div>
            <h2>Promotion codes</h2>
            <p>
              Platform administrators manage offers. Existing grants keep their
              original expiry.
            </p>
          </div>
          <Button icon="plus" onClick={() => setAdding(true)}>
            Create code
          </Button>
        </div>
        {(data.error || error) && <Notice error>{data.error || error}</Notice>}
        <div className="panel-body">
          <p>
            100% codes grant time-limited access without a payment. For partial
            discounts, first create the matching code and limits in RevenueCat
            Web Billing; checkout confirms the final discount.
          </p>
        </div>
        {data.data?.map((r: any) => (
          <div className="queue-row" key={r.code}>
            <span>
              <strong>
                {r.code} · {r.payload.percent}% off
              </strong>
              <small>
                {r.payload.percent === 100
                  ? `${r.payload.days} days · ${r.redemptions}/${r.payload.limit} redeemed`
                  : "Paid-checkout redemptions tracked in RevenueCat"}{" "}
                · Ends {date(r.payload.ends_at)}
              </small>
            </span>
            <Badge status={r.payload.enabled ? "ACTIVE" : "DISABLED"} />
            <Button onClick={() => setEdit(r)}>
              {r.payload.enabled ? "Disable" : "Enable"}
            </Button>
          </div>
        ))}
        {data.data?.length === 0 && <Empty title="No promotion codes yet" />}
      </section>
      {(adding || edit) && (
        <Drawer
          title={edit ? "Change promotion availability" : "New promotion code"}
          onClose={() => {
            setAdding(false);
            setEdit(null);
          }}
        >
          <Form
            submit={edit ? "Save availability" : "Create promotion"}
            fields={
              edit
                ? [reason]
                : [
                    { key: "code", label: "Code", min: 4, max: 40 },
                    {
                      key: "percent",
                      label: "Discount (%)",
                      type: "number",
                      min: 1,
                      max: 100,
                    },
                    {
                      key: "days",
                      label: "No-charge access duration (days)",
                      type: "number",
                      min: 1,
                      max: 366,
                    },
                    {
                      key: "limit",
                      label: "Maximum no-charge redemptions",
                      type: "number",
                      min: 1,
                      max: 100000,
                    },
                    {
                      key: "starts_at",
                      label: "Available from",
                      type: "datetime-local",
                    },
                    {
                      key: "ends_at",
                      label: "Expires",
                      type: "datetime-local",
                    },
                    {
                      key: "email",
                      label: "Restrict to this email (optional)",
                      type: "email",
                      required: false,
                    },
                    reason,
                  ]
            }
            onSubmit={async (v) => {
              try {
                const payload = edit
                  ? {
                      code: edit.code,
                      expected_version: edit.version,
                      ...Object.fromEntries(
                        [
                          "percent",
                          "days",
                          "starts_at",
                          "ends_at",
                          "limit",
                          "email",
                        ]
                          .filter((k) => edit.payload[k] !== undefined)
                          .map((k) => [k, edit.payload[k]]),
                      ),
                      enabled: !edit.payload.enabled,
                      reason: v.reason,
                    }
                  : {
                      ...v,
                      email: v.email || undefined,
                      starts_at: new Date(v.starts_at).toISOString(),
                      ends_at: new Date(v.ends_at).toISOString(),
                      enabled: true,
                      expected_version: 0,
                    };
                await api("/api/platform/promotions", payload, "PUT");
                setAdding(false);
                setEdit(null);
                refresh();
              } catch (e) {
                setError(errorMessage(e));
                throw e;
              }
            }}
          />
        </Drawer>
      )}
    </>
  );
}
