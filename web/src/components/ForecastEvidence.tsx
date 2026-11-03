"use client";
import { useState } from "react";
import { date } from "@/lib/catalog";

const variables: Record<string, { name: string; unit: string }> = {
  total_precipitation_1hr: { name: "Hourly rainfall", unit: "mm" },
  temperature_2m: { name: "Temperature", unit: "°C" },
  wind_speed_10m: { name: "Wind speed", unit: "m/s" },
};
export default function ForecastEvidence({ cells }: { cells: any[] }) {
  const [variable, setVariable] = useState("total_precipitation_1hr"),
    [statistic, setStatistic] = useState("mean"),
    [time, setTime] = useState("");
  const times = Array.from(
    new Set<string>(
      cells.map((c) => c.validTime || c.valid_time).filter(Boolean),
    ),
  ).sort();
  const chosen = times.includes(time) ? time : times[0];
  const visible = cells.filter(
    (c) =>
      (c.validTime || c.valid_time) === chosen &&
      Number.isFinite(c.values?.[variable]?.[statistic]),
  );
  if (!times.length) return null;
  return (
    <section className="forecast-evidence">
      <div className="forecast-controls">
        <label>
          Forecast layer
          <select
            value={variable}
            onChange={(e) => setVariable(e.target.value)}
          >
            {Object.entries(variables).map(([id, v]) => (
              <option key={id} value={id}>
                {v.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Forecast estimate
          <select
            value={statistic}
            onChange={(e) => setStatistic(e.target.value)}
          >
            {[
              ["mean", "Average"],
              ["p10", "Low estimate (10th percentile)"],
              ["p25", "Lower estimate (25th percentile)"],
              ["p50", "Middle estimate (50th percentile)"],
              ["p75", "Higher estimate (75th percentile)"],
              ["p90", "High estimate (90th percentile)"],
            ].map(([id, name]) => (
              <option key={id} value={id}>
                {name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Forecast time
          <select value={chosen} onChange={(e) => setTime(e.target.value)}>
            {times.map((t) => (
              <option key={t} value={t}>
                {date(t)}
              </option>
            ))}
          </select>
        </label>
      </div>
      <p className="muted">
        These estimates describe possible weather at each forecast location.
        They do not estimate damage or local flood impacts.
      </p>
      {visible.slice(0, 50).map((c) => (
        <div className="queue-row" key={`${c.longitude}:${c.latitude}`}>
          <div>
            <strong>
              {variables[variable].name}:{" "}
              {Number(c.values[variable][statistic]).toFixed(1)}{" "}
              {variables[variable].unit}
            </strong>
            <small>
              Location: {Number(c.latitude).toFixed(3)},{" "}
              {Number(c.longitude).toFixed(3)}
            </small>
          </div>
        </div>
      ))}
      {!visible.length && <p>No values are available for this selection.</p>}
      {visible.length > 50 && (
        <p>
          Showing 50 of {visible.length} forecast locations. The full evidence
          remains available to the assessment tools.
        </p>
      )}
    </section>
  );
}
