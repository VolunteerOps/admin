"use client";
import { useState } from "react";
import Link from "next/link";
import { api, errorMessage } from "@/lib/api";
import { usePagedData, useData, type Scope } from "@/lib/hooks";
import { date, type Row } from "@/lib/catalog";
import { Badge, Button, Drawer, Empty, Loading, Notice } from "./ui";
import { Form, reason } from "./Forms";
export default function Preparedness({
  scope,
  revision,
}: {
  scope: Scope;
  revision: number;
}) {
  const feed = usePagedData(scope.ops + "/records?kind=CAMPAIGN", revision),
    [creating, setCreating] = useState(false),
    [selected, setSelected] = useState<Row | null>(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false),
    [archiving, setArchiving] = useState(false);
  const [pendingTask, setPendingTask] = useState<{
    id: string;
    completed: boolean;
  } | null>(null);
  const [filter, setFilter] = useState("CURRENT");
  const bulletins = useData(scope.ops + "/records?kind=BULLETIN", revision);
  const items = (feed.data?.items || []) as Row[];
  const matching = items.filter(
    (r) =>
      filter === "ALL" ||
      (filter === "ARCHIVED"
        ? r.payload.status === "ARCHIVED"
        : r.payload.status !== "ARCHIVED"),
  );
  const refreshed = selected ? items.find((r) => r.id === selected.id) : null;
  const current = selected
    ? refreshed && refreshed.version >= selected.version
      ? refreshed
      : selected
    : null;
  async function task(id: string, completed: boolean) {
    if (!current || busy) return;
    setBusy(true);
    setPendingTask({ id, completed });
    setError("");
    try {
      const r = await api(scope.ops + "/commands", {
        action: "UPDATE_CAMPAIGN_TASK",
        id: current.id,
        expected_version: current.version,
        task_id: id,
        completed,
      });
      setSelected(r);
      scope.refresh();
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setBusy(false);
      setPendingTask(null);
    }
  }
  return (
    <>
      <div className="preparedness-intro">
        <div>
          <span className="eyebrow">SMALL STEPS. STRONGER READINESS.</span>
          <h2>Prepare before the response.</h2>
          <p>
            Give each preparation a clear owner, a deadline and a shared
            checklist.
          </p>
        </div>
        <Button variant="primary" icon="plus" onClick={() => setCreating(true)}>
          New campaign
        </Button>
      </div>
      <div className="toolbar">
        <label>
          Show
          <select
            aria-label="Campaign filter"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
          >
            <option value="CURRENT">Current campaigns</option>
            <option value="ARCHIVED">Archived campaigns</option>
            <option value="ALL">All campaigns</option>
          </select>
        </label>
        <Link href="/sentinel" className="text-button">
          Review Sentinel evidence →
        </Link>
      </div>
      {(error || feed.error) && <Notice error>{error || feed.error}</Notice>}
      {feed.loading && !feed.data ? (
        <Loading />
      ) : (
        <div className="campaign-grid">
          {matching.map((r) => {
            const tasks = r.payload.tasks || [],
              done = tasks.filter((t: any) => t.completed).length;
            return (
              <article className="campaign-card" key={r.id}>
                <div className="campaign-banner">
                  <Badge status={r.payload.status} />
                  <span>{date(r.payload.due_at)}</span>
                </div>
                <div className="campaign-body">
                  <span className="eyebrow">PREPAREDNESS CAMPAIGN</span>
                  <h2>{r.payload.title}</h2>
                  <p>{r.payload.description}</p>
                  <div className="campaign-meta">
                    <span>Owner: {r.payload.owner}</span>
                    <span>
                      {done} / {tasks.length} ready
                    </span>
                  </div>
                  <progress
                    max={Math.max(tasks.length, 1)}
                    value={done}
                    aria-label="Campaign progress"
                  />
                  <ul>
                    {tasks.slice(0, 3).map((t: any) => (
                      <li key={t.id}>
                        <span className={t.completed ? "task-done" : ""}>
                          {t.completed ? "✓" : "○"}
                        </span>
                        {t.title}
                      </li>
                    ))}
                  </ul>
                  <Button
                    onClick={() => {
                      setSelected(r);
                      setArchiving(false);
                    }}
                  >
                    Review checklist
                  </Button>
                </div>
              </article>
            );
          })}
        </div>
      )}
      {!feed.loading && !matching.length && (
        <Empty
          title="Your next preparation starts here"
          body="Create a campaign for supply checks, community outreach or team readiness."
        />
      )}
      {feed.hasMore && (
        <Button onClick={() => feed.loadMore()}>Load more campaigns</Button>
      )}
      {creating && (
        <Drawer
          title="New preparedness campaign"
          onClose={() => setCreating(false)}
        >
          <Form
            submit="Create campaign"
            fields={[
              {
                key: "title",
                label: "Campaign name",
                min: 3,
                max: 160,
                full: true,
              },
              {
                key: "description",
                label: "What are we preparing for?",
                type: "textarea",
                min: 10,
                max: 3000,
                full: true,
              },
              {
                key: "owner",
                label: "Responsible person or team",
                min: 2,
                max: 120,
              },
              { key: "due_at", label: "Target date", type: "datetime-local" },
              {
                key: "bulletin_id",
                label: "Related regional bulletin",
                type: "select",
                required: false,
                options: (bulletins.data?.items || []).map((b: Row) => ({
                  value: b.id,
                  label: b.payload.title,
                })),
                hint: "Optional. Link guidance your team should review.",
              },
              {
                key: "tasks",
                label: "Checklist — one task per line",
                type: "textarea",
                min: 3,
                max: 9000,
                full: true,
              },
            ]}
            onSubmit={async (v, key) => {
              await api(
                scope.ops + "/commands",
                {
                  action: "CREATE_CAMPAIGN",
                  ...v,
                  due_at: new Date(v.due_at).toISOString(),
                  tasks: String(v.tasks)
                    .split("\n")
                    .map((t) => t.trim())
                    .filter(Boolean),
                },
                "POST",
                undefined,
                key,
              );
              setCreating(false);
              scope.refresh();
            }}
          />
        </Drawer>
      )}
      {current && (
        <Drawer
          title={current.payload.title}
          onClose={() => {
            setSelected(null);
            setError("");
          }}
        >
          <Badge status={current.payload.status} />
          <p>{current.payload.description}</p>
          <p>
            <strong>{current.payload.owner}</strong> · Due{" "}
            {date(current.payload.due_at)}
          </p>
          {error && <Notice error>{error}</Notice>}
          {current.payload.bulletin_id && (
            <p>
              Linked guidance:{" "}
              {bulletins.data?.items?.find(
                (b: Row) => b.id === current.payload.bulletin_id,
              )?.payload.title || "Regional bulletin"}
              . Open regional bulletins below to review it.
            </p>
          )}
          <div className="campaign-tasks">
            {current.payload.tasks?.map((t: any) => (
              <label className="check" key={t.id}>
                <input
                  type="checkbox"
                  checked={
                    pendingTask && pendingTask.id === t.id
                      ? pendingTask.completed
                      : t.completed
                  }
                  disabled={busy || current.payload.status === "ARCHIVED"}
                  onChange={(e) => void task(t.id, e.target.checked)}
                />
                <span>
                  {t.title}
                  {t.updated_at && <small>Updated {date(t.updated_at)}</small>}
                </span>
              </label>
            ))}
          </div>
          <p>
            These are team preparations, not official hazard warnings. Review
            published guidance alongside local evidence.
          </p>
          <div className="action-list">
            <Link className="button" href="/bulletins">
              Open regional bulletins
            </Link>
            {current.payload.status !== "ARCHIVED" && (
              <Button onClick={() => setArchiving(!archiving)}>
                Archive campaign
              </Button>
            )}
          </div>
          {archiving && (
            <Form
              fields={[reason]}
              submit="Confirm archive"
              onSubmit={async (v, key) => {
                await api(
                  scope.ops + "/commands",
                  {
                    action: "ARCHIVE_CAMPAIGN",
                    id: current.id,
                    expected_version: current.version,
                    ...v,
                  },
                  "POST",
                  undefined,
                  key,
                );
                setSelected(null);
                scope.refresh();
              }}
            />
          )}
        </Drawer>
      )}
    </>
  );
}
