"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { api } from "@/lib/api";
import { useData, type Scope } from "@/lib/hooks";
import { date } from "@/lib/catalog";
import { Badge, Button, Notice } from "./ui";

export type RadarPin = {
  id: string; type: string; incident_type?: string; category: string; status: string;
  title: string; latitude: number; longitude: number; severity: string;
  source: string; source_url?: string | null; observed_at: string; detail: string;
  verification?: string; route?: { label: string; href: string };
};
const GROUPS: Record<string, { label: string; types: string[]; categories?: string[] }> = {
  prediction: { label: "Predictions", types: [], categories: ["PREDICTION"] },
  flood: { label: "Flood & severe rain", types: ["FLOOD", "FLOOD_WATCH", "SEVERE_RAIN"] },
  heatwind: { label: "Heat & wind", types: ["HEAT", "STRONG_WIND"] },
  earthquake: { label: "Earthquake", types: ["EARTHQUAKE"] },
  landslide: { label: "Land & mud slides", types: ["LANDSLIDE"] },
  drought: { label: "Drought / famine / El Niño", types: ["DROUGHT", "FAMINE", "EL_NINO"] },
  artificial: { label: "Artificial disasters", types: [], categories: ["ARTIFICIAL"] },
  sensors: { label: "Sensors", types: ["GAUGE_SENSOR"], categories: ["SENSOR"] },
};
function groupOf(pin: RadarPin): string {
  for (const [key, group] of Object.entries(GROUPS)) {
    if (group.categories?.includes(pin.category) && key !== "sensors") return key;
    if (group.types.includes(pin.type)) return key;
  }
  return pin.category === "SENSOR" ? "sensors" : "prediction";
}
export default function RadarMap({ scope }: { scope: Scope }) {
  const [rev, setRev] = useState(0);
  const radar = useData(scope.sentinel + "/radar", rev);
  const pins: RadarPin[] = useMemo(() => (radar.data?.pins || []).filter((p: RadarPin) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude)), [radar.data]);
  const [enabled, setEnabled] = useState<Record<string, boolean>>(Object.fromEntries(Object.keys(GROUPS).map((k) => [k, true])));
  const [selected, setSelected] = useState<RadarPin | null>(null);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [error, setError] = useState("");
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markers = useRef<mapboxgl.Marker[]>([]);
  const visible = useMemo(() => pins.filter((p) => enabled[groupOf(p)]), [pins, enabled]);
  useEffect(() => { const timer = setInterval(() => setRev((v) => v + 1), 60000); return () => clearInterval(timer); }, []);
  useEffect(() => {
    let disposed = false;
    const controller = new AbortController();
    fetch("/sentinel/map-widget-config", { signal: controller.signal, cache: "no-store" })
      .then((r) => r.json())
      .then((config) => {
        if (disposed || !config.token || !mapboxgl.supported() || !container.current) return;
        const instance = new mapboxgl.Map({
          container: container.current, accessToken: config.token,
          style: "mapbox://styles/mapbox/dark-v11", center: [36.82, -1.28], zoom: 7, attributionControl: true,
        });
        instance.addControl(new mapboxgl.NavigationControl(), "top-right");
        mapRef.current = instance;
      })
      .catch(() => setError("The live map could not start. Check the map configuration."));
    return () => { disposed = true; controller.abort(); mapRef.current?.remove(); mapRef.current = null; };
  }, []);
  useEffect(() => {
    const instance = mapRef.current;
    if (!instance) return;
    markers.current.forEach((m) => m.remove());
    markers.current = [];
    for (const pin of visible) {
      const element = document.createElement("button");
      element.type = "button";
      element.className = `radar-pin radar-pin-${groupOf(pin)} severity-${String(pin.severity).toLowerCase()}`;
      element.title = pin.title;
      element.setAttribute("aria-label", pin.title);
      element.addEventListener("click", () => setSelected(pin));
      markers.current.push(new mapboxgl.Marker({ element }).setLngLat([pin.longitude, pin.latitude]).addTo(instance));
    }
    if (visible.length && !selected) {
      const bounds = new mapboxgl.LngLatBounds();
      visible.forEach((p) => bounds.extend([p.longitude, p.latitude]));
      instance.fitBounds(bounds, { padding: 70, maxZoom: 10, duration: 600 });
    }
  }, [visible, selected]);
  const center = visible[0];
  async function ingest() {
    setBusy(true); setError(""); setNotice("");
    try {
      const result = await api(scope.sentinel + "/incidents/ingest", {}, "POST", undefined, crypto.randomUUID());
      setNotice(`Incident feeds checked. ${result.created} new reports added; every new item stays REPORTED until a coordinator verifies it.`);
      setRev((v) => v + 1);
    } catch (e: any) { setError(e?.message || "Ingestion failed"); }
    finally { setBusy(false); }
  }
  async function decide(decision: "verify" | "dismiss") {
    if (!selected) return;
    setBusy(true); setError("");
    try {
      await api(scope.sentinel + "/incidents/" + selected.id + "/" + decision, { reason }, "POST", undefined, crypto.randomUUID());
      setNotice(decision === "verify" ? "Incident verified." : "Incident dismissed.");
      setReason(""); setRev((v) => v + 1);
    } catch (e: any) { setError(e?.message || "Decision failed"); }
    finally { setBusy(false); }
  }
  const isIncident = selected && !selected.id.startsWith("forecast-") && !selected.id.startsWith("usgs-") && !selected.id.startsWith("gauge-");
  return (
    <div>
      <div className="radar-hero">
        <span className="radar-eyebrow">Early warning intelligence</span>
        <h2>Real-Time Multi-Hazard Radar</h2>
        <p>Predicted and ongoing floods, severe rain, drought and famine signals, El Niño, earthquakes, landslides and artificial disasters — from forecast models, official feeds, verified-source reports and ground sensors.</p>
        <div className="radar-hero-chips">
          <span>{pins.length} mapped hazards</span>
          <span>{pins.filter((p) => p.category === "ARTIFICIAL").length} artificial reports</span>
          <span>Live · updated {radar.data?.generated_at ? date(radar.data.generated_at) : "…"}</span>
        </div>
      </div>
      <div className="radar-layers" role="group" aria-label="Hazard layers">
        {Object.entries(GROUPS).map(([key, group]) => (
          <button key={key} type="button" className={enabled[key] ? "radar-chip active" : "radar-chip"}
            aria-pressed={!!enabled[key]} onClick={() => setEnabled((state) => ({ ...state, [key]: !state[key] }))}>
            {group.label} · {pins.filter((p) => groupOf(p) === key).length}
          </button>
        ))}
        <Button disabled={busy} onClick={ingest}>Ingest incident feeds</Button>
      </div>
      {error && <Notice error>{error}</Notice>}
      {notice && <Notice>{notice}</Notice>}
      {radar.error && <Notice error>{radar.error}</Notice>}
      <div className="radar-map-wrap">
        <div ref={container} className="radar-map" aria-label="Multi-hazard radar map" />
        {center && (
          <div className="radar-sweep" style={{ left: "55%", top: "45%" }} aria-hidden>
            <span /><span /><span />
          </div>
        )}
        <div className="radar-legend">
          <span><i className="dot prediction" />Prediction</span>
          <span><i className="dot ongoing" />Ongoing / reported</span>
          <span><i className="dot artificial" />Artificial</span>
          <span><i className="dot sensor" />Sensor</span>
        </div>
      </div>
      <p className="radar-disclaimer">{radar.data?.disclaimer || "Pins show predictions, reported observations and recorded sensors — not impact estimates."}</p>
      {selected && (
        <section className="panel radar-detail" aria-label="Hazard detail">
          <div className="panel-head">
            <h3>{selected.title}</h3>
            <Badge status={selected.status} />
          </div>
          <dl className="radar-facts">
            <div><dt>Type</dt><dd>{selected.type.replace(/_/g, " ")}</dd></div>
            <div><dt>Severity</dt><dd>{selected.severity}</dd></div>
            <div><dt>Coordinates</dt><dd>{Number(selected.latitude).toFixed(4)}, {Number(selected.longitude).toFixed(4)}</dd></div>
            <div><dt>When</dt><dd>{date(selected.observed_at)}</dd></div>
            <div><dt>Source</dt><dd>{selected.source_url ? <a href={selected.source_url} target="_blank" rel="noreferrer">{selected.source}</a> : selected.source}</dd></div>
            {!!selected.verification && <div><dt>Verification</dt><dd>{selected.verification}</dd></div>}
          </dl>
          <p>{selected.detail}</p>
          <div className="action-list">
            {selected.route && <Link className="button primary" href={selected.route.href}>{selected.route.label}</Link>}
            <Link className="button" href="/weather-analysis">Weather analysis</Link>
            <Link className="button" href="/agent-runs">Agent runs</Link>
          </div>
          {isIncident && selected.status === "REPORTED" && (
            <div className="form-grid">
              <label className="full">Verification / dismissal reason
                <textarea value={reason} onChange={(e) => setReason(e.target.value)} maxLength={500} placeholder="What did you check?" />
              </label>
              <div className="form-actions">
                <Button variant="primary" disabled={busy || reason.trim().length < 3} onClick={() => decide("verify")}>Verify incident</Button>
                <Button disabled={busy || reason.trim().length < 3} onClick={() => decide("dismiss")}>Dismiss</Button>
              </div>
            </div>
          )}
        </section>
      )}
      <section className="panel">
        <div className="panel-head"><h3>Mapped hazards</h3><span>{visible.length} shown</span></div>
        {visible.length ? visible.map((pin) => (
          <button key={pin.id} type="button" className={"queue-row radar-row" + (selected?.id === pin.id ? " selected" : "")} onClick={() => setSelected(pin)}>
            <span><strong>{pin.title}</strong><small>{pin.type.replace(/_/g, " ")} · {Number(pin.latitude).toFixed(3)}, {Number(pin.longitude).toFixed(3)} · {pin.source}</small></span>
            <Badge status={pin.severity} />
          </button>
        )) : <p className="panel-body">No hazards in the enabled layers. Request a forecast, ingest incident feeds, or record gauge readings.</p>}
      </section>
    </div>
  );
}
