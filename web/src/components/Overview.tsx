"use client";
import Link from "next/link";
import { useData, type Scope } from "@/lib/hooks";
import { date, recordTitle, Row } from "@/lib/catalog";
import { Badge, Contours, Empty, Icon, Loading, Notice } from "./ui";
export default function Overview({
  scope,
  revision,
}: {
  scope: Scope;
  revision: number;
}) {
  const { data, error, loading } = useData(scope.ops + "/overview", revision);
  if (loading) return <Loading />;
  if (error) return <Notice error>{error}</Notice>;
  if (!data) return null;
  const count = data.counts || {};
  return (
    <>
      <section className="hero-band">
        <div>
          <span className="eyebrow">YOUR REGION, IN FOCUS</span>
          <h2>
            {count.awaiting_review
              ? `${count.awaiting_review} decisions. A clearer response.`
              : "Ready for the next response."}
          </h2>
          <p>
            {count.awaiting_review
              ? "Review the people, reports and requests that need your attention."
              : "Bring your people and resources together. Start with a mission brief."}
          </p>
        </div>
        <Link href="/missions" className="button">
          Open missions
          <Icon name="arrow" />
        </Link>
        <Contours />
      </section>
      <div className="metrics">
        {[
          ["Active missions", count.active_missions, "missions"],
          ["Staged missions", count.staged_missions, "clock"],
          ["Checked in", count.checked_in, "people"],
          ["Awaiting review", count.awaiting_review, "file"],
        ].map(([name, value, icon]) => (
          <div className="metric" key={name}>
            <span className="metric-label">
              {name}
              <Icon name={String(icon)} size={18} />
            </span>
            <strong>{value ?? 0}</strong>
          </div>
        ))}
      </div>
      {data.dispatch?.paused && (
        <Notice error>
          Dispatch is paused for this region. Review dispatch controls before
          assigning responders.
        </Notice>
      )}
      <div className="split">
        <div className="stack">
          <section className="panel">
            <div className="panel-head">
              <h2>Needs your attention</h2>
              <span className="eyebrow">Decision queue</span>
            </div>
            {data.lanes?.filter((l: any) => l.count > 0).length ? (
              data.lanes
                .filter((l: any) => l.count > 0)
                .map((l: any) => (
                  <Link
                    href={
                      "/" +
                      String(l.destination).toLowerCase().replace(/ /g, "-")
                    }
                    key={l.key}
                    className="queue-row"
                  >
                    <span className="queue-icon">
                      <Icon name="file" />
                    </span>
                    <span>
                      <strong>{l.action}</strong>
                      <small>
                        {l.items?.[0]
                          ? recordTitle(l.items[0])
                          : "Review open items"}
                      </small>
                    </span>
                    <span className="queue-count">{l.count}</span>
                    <Icon name="chevron" size={16} />
                  </Link>
                ))
            ) : (
              <Empty
                title="You’re up to date"
                body="New applications, field reports and training submissions will appear here for review."
              />
            )}
          </section>
          <section className="panel">
            <div className="panel-head">
              <h2>Current missions</h2>
              <Link className="text-button" href="/missions">
                View all →
              </Link>
            </div>
            {data.missions?.length ? (
              <div className="table-wrap" tabIndex={0} role="region" aria-label="Scrollable records table">
                <table>
                  <thead>
                    <tr>
                      <th>Mission</th>
                      <th>Team</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.missions.map((r: Row) => (
                      <tr key={r.id}>
                        <td>
                          <Link href={"/missions?record=" + r.id}>
                            <strong>{recordTitle(r)}</strong>
                            <small>{r.payload.meeting_point}</small>
                          </Link>
                        </td>
                        <td>
                          {r.payload.assigned_count || 0} / {r.payload.capacity}
                        </td>
                        <td>
                          <Badge status={r.payload.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <Empty
                title="No missions underway"
                body="Draft a mission and invite the right people to take part."
                action={
                  <Link className="button" href="/missions?create=1">
                    Plan a mission
                  </Link>
                }
              />
            )}
          </section>
        </div>
        <div className="stack">
          <section className="panel">
            <div className="panel-head">
              <h2>Regional readiness</h2>
              <Icon name="sentinel" />
            </div>
            <div className="panel-body">
              <span className="eyebrow">Sentinel</span>
              <h3 className="section-title">See what’s ahead.</h3>
              <p>
                Review weather evidence alongside your team’s preparation needs.
              </p>
              <Link className="button" href="/sentinel">
                Open radar
                <Icon name="arrow" size={17} />
              </Link>
            </div>
          </section>
          <section className="panel">
            <div className="panel-head">
              <h2>Recent activity</h2>
              <Icon name="clock" />
            </div>
            <div className="panel-body">
              {data.activity?.length ? (
                <div className="timeline">
                  {data.activity.map((r: Row) => (
                    <article key={r.id}>
                      <strong>{recordTitle(r)}</strong>
                      <p>{r.payload.record_kind?.toLowerCase()} updated</p>
                      <small>{date(r.updatedAt)}</small>
                    </article>
                  ))}
                </div>
              ) : (
                <p>Recorded workspace changes will appear here.</p>
              )}
              <Link href="/activity" className="text-button">
                View audit trail →
              </Link>
            </div>
          </section>
        </div>
      </div>
      <p className="page-intro">
        <small>Updated {date(data.as_of)}</small>
      </p>
    </>
  );
}
