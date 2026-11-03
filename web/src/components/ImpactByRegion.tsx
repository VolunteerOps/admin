"use client";
import { useData, type Scope } from "@/lib/hooks";
import { Loading, Notice } from "./ui";
export default function ImpactByRegion({
  scope,
  range,
  revision,
}: {
  scope: Scope;
  range: { from: string; to: string };
  revision: number;
}) {
  const regions = useData(
    `/api/organizations/${scope.org}/sentinel/regions`,
    revision,
  );
  return (
    <section className="panel">
      <div className="panel-head">
        <div>
          <h2>Volunteer hours by region</h2>
          <p>Recorded contributions and reviewed hours are shown separately.</p>
        </div>
      </div>
      <div className="panel-body">
        {regions.error && <Notice error>{regions.error}</Notice>}
        {regions.loading && !regions.data ? (
          <Loading />
        ) : (
          regions.data?.map((region: any) => (
            <RegionHours
              key={region.id}
              org={scope.org}
              region={region}
              range={range}
              revision={revision}
            />
          ))
        )}
        <small>
          Only regions your account can access are included. Counts use records
          created in the selected interval.
        </small>
      </div>
    </section>
  );
}
function RegionHours({
  org,
  region,
  range,
  revision,
}: {
  org: string;
  region: any;
  range: { from: string; to: string };
  revision: number;
}) {
  const result = useData(
    `/api/organizations/${org}/regions/${region.id}/operations/metrics?` +
      new URLSearchParams(range),
    revision,
  );
  const recorded = Number(result.data?.recorded_hours || 0),
    approved = Number(result.data?.approved_hours || 0),
    max = Math.max(1, recorded, approved);
  return (
    <div className="region-hours">
      <div>
        <strong>{region.name}</strong>
        <span>
          {result.loading && !result.data
            ? "Loading…"
            : result.error
              ? "Unavailable"
              : `${approved} approved / ${recorded} recorded`}
        </span>
      </div>
      {result.error ? (
        <Notice error>{result.error}</Notice>
      ) : (
        <div
          className="region-hour-tracks"
          aria-label={`${region.name}: ${approved} approved hours, ${recorded} recorded hours`}
        >
          <span style={{ width: (recorded / max) * 100 + "%" }} />
          <span style={{ width: (approved / max) * 100 + "%" }} />
        </div>
      )}
    </div>
  );
}
