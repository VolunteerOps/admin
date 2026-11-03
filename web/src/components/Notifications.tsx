"use client";
import { useState, useEffect } from "react";
import Link from "next/link";
import { api, errorMessage } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { date, label } from "@/lib/catalog";
import { Button, Empty, Loading, Notice } from "./ui";
const destinations: Record<string, string> = {
  MISSION: "missions",
  CAMPAIGN: "campaigns",
  APPLICATION: "applications",
  ASSIGNMENT: "assignments",
  HOURS: "hours",
  REPORT: "reports",
  MESSAGE: "inbox",
  COURSE: "training",
  ENROLLMENT: "learning",
  CREDENTIAL: "qualifications",
  INVENTORY: "resources",
  BULLETIN: "bulletins",
  SUPPORT: "support",
};
export default function Notifications({
  scope,
  revision,
}: {
  scope: Scope;
  revision: number;
}) {
  const base = `/api/organizations/${scope.org}/regions/${scope.region}/notifications`;
  const [before, setBefore] = useState(0),
    [unread, setUnread] = useState(false),
    [error, setError] = useState("");
  useEffect(() => {
    setBefore(0);
    setError("");
  }, [base]);
  const feed = useData(base + (before ? "?before=" + before : ""), revision);
  async function read(id: string) {
    try {
      await api(base + "/" + id + "/read", {});
      scope.refresh();
    } catch (e) {
      setError(errorMessage(e));
    }
  }
  const items = (feed.data?.items || []).filter((r: any) => !unread || !r.read);
  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <h2>Workspace updates</h2>
          <p>Activity in your selected operating region.</p>
        </div>
        <label className="check">
          <input
            type="checkbox"
            checked={unread}
            onChange={(e) => setUnread(e.target.checked)}
          />
          Unread only
        </label>
      </div>
      {(error || feed.error) && <Notice error>{error || feed.error}</Notice>}
      {feed.loading && !feed.data ? (
        <Loading />
      ) : items.length ? (
        items.map((r: any) => (
          <article
            key={r.id}
            className={"notification-row " + (!r.read ? "unread" : "")}
          >
            <span className="notification-dot" />
            <div>
              <strong>{label(r.payload.action)}</strong>
              <p>
                {label(r.payload.record_kind)} updated · {date(r.createdAt)}
              </p>
              <Link
                className="text-button"
                href={"/" + (destinations[r.payload.record_kind] || "activity")}
              >
                Review {label(r.payload.record_kind).toLowerCase()} →
              </Link>
            </div>
            {!r.read && (
              <Button onClick={() => void read(r.id)}>Mark as read</Button>
            )}
          </article>
        ))
      ) : (
        <Empty
          title={unread ? "You’re up to date" : "No updates yet"}
          body="Mission, training and preparedness activity will appear here."
        />
      )}
      <div className="panel-body action-list">
        {before > 0 && (
          <Button onClick={() => setBefore(0)}>Latest updates</Button>
        )}
        {feed.data?.next_cursor && (
          <Button onClick={() => setBefore(feed.data.next_cursor)}>
            Older updates
          </Button>
        )}
      </div>
    </section>
  );
}
