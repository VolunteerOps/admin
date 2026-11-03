"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import mapboxgl from "mapbox-gl";
import "mapbox-gl/dist/mapbox-gl.css";
import { useData, type Scope } from "@/lib/hooks";
import { date, label, recordTitle, type Row } from "@/lib/catalog";
import { Badge, Button, Notice } from "./ui";
import { resqMapStyle } from "@/lib/resqMapStyle";

type OpsPin = {
  id: string; category: string; title: string; latitude: number; longitude: number;
  detail: string; severity?: string; href: string;
};
const CATEGORIES: Record<string, { label: string; color: string }> = {
  prediction: { label: "Predictions", color: "#38bdf8" },
  disaster: { label: "Ongoing disasters", color: "#f97316" },
  mission: { label: "Missions", color: "#2563eb" },
  report: { label: "Volunteer reports", color: "#22c55e" },
  deployment: { label: "Deployments", color: "#a855f7" },
  resource: { label: "Resources", color: "#eab308" },
};
function coords(payload: any): [number, number] | null {
  const lon = Number(payload?.longitude ?? payload?.lng ?? payload?.center?.[0]);
  const lat = Number(payload?.latitude ?? payload?.lat ?? payload?.center?.[1]);
  return Number.isFinite(lon) && Number.isFinite(lat) && (lon !== 0 || lat !== 0) ? [lon, lat] : null;
}
function circlePolygon(lon: number, lat: number, radiusKm: number) {
  const points: [number, number][] = [];
  for (let i = 0; i <= 48; i++) {
    const angle = (i / 48) * Math.PI * 2;
    points.push([
      lon + (radiusKm / 111.32) * Math.cos(angle) / Math.max(0.2, Math.cos((lat * Math.PI) / 180)),
      lat + (radiusKm / 110.57) * Math.sin(angle),
    ]);
  }
  return { type: "Feature" as const, properties: {}, geometry: { type: "Polygon" as const, coordinates: [points] } };
}
export default function OperationsMap({ scope, revision }: { scope: Scope; revision: number }) {
  const orgBase = scope.sentinel.split("/regions/")[0];
  const regionId = scope.sentinel.split("/regions/")[1];
  const [rev, setRev] = useState(0);
  const [enabled, setEnabled] = useState<Record<string, boolean>>(Object.fromEntries(Object.keys(CATEGORIES).map((k) => [k, true])));
  const [selected, setSelected] = useState<OpsPin | null>(null);
  const [error, setError] = useState("");
  const [countryName, setCountryName] = useState("");
  const [updatedAt, setUpdatedAt] = useState("");
  const regions = useData(orgBase + "/regions", revision);
  const region = (regions.data || []).find((r: any) => r.id === regionId);
  const bounds = region?.forecastBounds;
  const tick = revision + rev;
  const missions = useData(scope.ops + "/records?kind=MISSION", tick);
  const reports = useData(scope.ops + "/records?kind=REPORT", tick);
  const assignments = useData(scope.ops + "/records?kind=ASSIGNMENT", tick);
  const radar = useData(scope.sentinel + "/radar", tick);
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markers = useRef<mapboxgl.Marker[]>([]);
  const tokenRef = useRef("");
  useEffect(() => { const timer = setInterval(() => { setRev((v) => v + 1); setUpdatedAt(new Date().toISOString()); }, 30000); return () => clearInterval(timer); }, []);

  const pins: OpsPin[] = useMemo(() => {
    const out: OpsPin[] = [];
    const missionById = new Map<string, Row>((missions.data?.items || []).map((r: Row) => [r.id, r]));
    for (const pin of radar.data?.pins || []) {
      out.push({
        id: "radar-" + pin.id, category: pin.category === "PREDICTION" ? "prediction" : "disaster",
        title: pin.title, latitude: pin.latitude, longitude: pin.longitude,
        detail: pin.detail + " (" + pin.source + ")", severity: pin.severity, href: pin.route?.href || "/sentinel",
      });
    }
    for (const mission of missions.data?.items || []) {
      const c = coords(mission.payload);
      if (c) out.push({ id: mission.id, category: "mission", title: recordTitle(mission), latitude: c[1], longitude: c[0], detail: (mission.payload.status || "") + " · " + (mission.payload.meeting_point || ""), href: "/missions" });
    }
    for (const report of reports.data?.items || []) {
      const c = coords(report.payload);
      if (c) out.push({ id: report.id, category: "report", title: recordTitle(report), latitude: c[1], longitude: c[0], detail: report.payload.summary || report.payload.status || "Field report", href: "/reports" });
    }
    for (const assignment of assignments.data?.items || []) {
      const mission = assignment.parentId ? missionById.get(assignment.parentId) : undefined;
      const c = mission ? coords(mission.payload) : coords(assignment.payload);
      if (c) out.push({ id: assignment.id, category: "deployment", title: "Deployment: " + (mission ? recordTitle(mission) : "Assignment"), latitude: c[1], longitude: c[0], detail: assignment.payload.status || "Assigned team", href: "/assignments" });
    }
    return out.filter((p) => Number.isFinite(p.latitude) && Number.isFinite(p.longitude));
  }, [missions.data, reports.data, assignments.data, radar.data]);
  const visible = pins.filter((p) => enabled[p.category]);

  useEffect(() => {
    let disposed = false;
    const controller = new AbortController();
    fetch("/sentinel/map-widget-config", { signal: controller.signal, cache: "no-store" })
      .then((r) => r.json())
      .then((config) => {
        if (disposed || !config.token || !mapboxgl.supported() || !container.current) return;
        tokenRef.current = config.token;
        const instance = new mapboxgl.Map({
          container: container.current, accessToken: config.token,
          style: structuredClone(resqMapStyle) as any, center: [36.82, -1.28], zoom: 6, attributionControl: true,
        });
        instance.addControl(new mapboxgl.NavigationControl(), "top-right");
        instance.on("load", () => {
          instance.addSource("impact", { type: "geojson", data: { type: "FeatureCollection", features: [] } });
          instance.addLayer({ id: "impact-fill", type: "fill", source: "impact", paint: { "fill-color": "#f97316", "fill-opacity": 0.18 } });
          instance.addLayer({ id: "impact-line", type: "line", source: "impact", paint: { "line-color": "#f97316", "line-width": 2 } });
        });
        instance.on("zoomend", () => {
          const zoom = instance.getZoom();
          instance.easeTo({ pitch: zoom >= 15 ? 60 : zoom >= 13 ? 35 : 0, duration: 600 });
          if (zoom >= 15 && !instance.getLayer("resq-3d-buildings")) {
            try {
              instance.addLayer({
                id: "resq-3d-buildings", source: "streets", "source-layer": "building", type: "fill-extrusion", minzoom: 14,
                paint: { "fill-extrusion-color": "#3b4a63", "fill-extrusion-opacity": 0.85, "fill-extrusion-height": ["coalesce", ["get", "render_height"], 8] },
              } as any);
            } catch { /* The configured style has no building layer; 2D pinpointing still applies. */ }
          }
        });
        mapRef.current = instance;
      })
      .catch(() => setError("The operations map could not start. Check the map configuration."));
    return () => { disposed = true; controller.abort(); mapRef.current?.remove(); mapRef.current = null; };
  }, []);

  useEffect(() => {
    const instance = mapRef.current;
    if (!instance || !bounds) return;
    const east = bounds.east < bounds.west ? bounds.east + 360 : bounds.east;
    instance.fitBounds([[bounds.west, bounds.south], [east, bounds.north]], { padding: 50, duration: 600 });
    const centerLon = (bounds.west + east) / 2, centerLat = (bounds.south + bounds.north) / 2;
    (async () => {
      try {
        const geo = await (await fetch(`https://api.mapbox.com/geocoding/v5/mapbox.places/${centerLon},${centerLat}.json?types=country&access_token=${tokenRef.current}`)).json();
        const country = geo.features?.[0];
        if (!country) return;
        setCountryName(country.text || "");
        const code = String(country.properties?.short_code || "").toUpperCase();
        if (!code) return;
        const boundary = await (await fetch(`https://raw.githubusercontent.com/johan/world.geo.json/master/countries/${code}.geo.json`)).json();
        if (!mapRef.current?.getSource("country")) {
          mapRef.current?.addSource("country", { type: "geojson", data: boundary });
          mapRef.current?.addLayer({ id: "country-fill", type: "fill", source: "country", paint: { "fill-color": "#2563eb", "fill-opacity": 0.06 } });
          mapRef.current?.addLayer({ id: "country-line", type: "line", source: "country", paint: { "line-color": "#60a5fa", "line-width": 2 } });
        }
      } catch { /* Country outline is an enhancement; the regional operating bounds remain framed. */ }
    })();
  }, [bounds]);

  useEffect(() => {
    const instance = mapRef.current;
    if (!instance) return;
    markers.current.forEach((m) => m.remove());
    markers.current = [];
    for (const pin of visible) {
      const element = document.createElement("button");
      element.type = "button";
      element.className = "ops-pin";
      element.style.background = CATEGORIES[pin.category]?.color || "#64748b";
      element.title = pin.title;
      element.setAttribute("aria-label", pin.title);
      element.addEventListener("click", () => setSelected(pin));
      markers.current.push(new mapboxgl.Marker({ element }).setLngLat([pin.longitude, pin.latitude]).addTo(instance));
    }
  }, [visible]);

  useEffect(() => {
    const instance = mapRef.current;
    if (!instance || !instance.getSource("impact")) return;
    const source = instance.getSource("impact") as mapboxgl.GeoJSONSource;
    if (!selected) { source.setData({ type: "FeatureCollection", features: [] }); return; }
    const radiusKm = selected.category === "disaster" ? (selected.severity === "SEVERE" ? 25 : selected.severity === "HIGH" ? 12 : 5) : 2;
    source.setData(circlePolygon(selected.longitude, selected.latitude, radiusKm) as any);
    instance.flyTo({ center: [selected.longitude, selected.latitude], zoom: Math.max(instance.getZoom(), 11), duration: 700 });
  }, [selected]);

  return (
    <div>
      <div className="ops-map-head">
        <div>
          <span className="radar-eyebrow">Operations map {countryName ? "· " + countryName : ""}</span>
          <h2>Live operations picture</h2>
          <p>Predictions, ongoing disasters, missions, volunteer reports and deployments for {region?.name || "this region"}. Zoom in past street level for 3D buildings and precise pinpointing.</p>
        </div>
        <span className="live-badge"><i /> LIVE{updatedAt ? " · " + date(updatedAt) : ""}</span>
      </div>
      <div className="radar-layers" role="group" aria-label="Map categories">
        {Object.entries(CATEGORIES).map(([key, cat]) => (
          <button key={key} type="button" className={enabled[key] ? "radar-chip active" : "radar-chip"} aria-pressed={!!enabled[key]}
            onClick={() => setEnabled((state) => ({ ...state, [key]: !state[key] }))}>
            <i className="dot" style={{ background: cat.color }} />{cat.label} · {pins.filter((p) => p.category === key).length}
          </button>
        ))}
        <Button onClick={() => setRev((v) => v + 1)}>Refresh now</Button>
      </div>
      {error && <Notice error>{error}</Notice>}
      <div ref={container} className="ops-map" aria-label="Live operations map" />
      {selected && (
        <section className="panel radar-detail" aria-label="Map detail">
          <div className="panel-head"><h3>{selected.title}</h3>{selected.severity && <Badge status={selected.severity} />}</div>
          <p>{selected.detail}</p>
          <p>Coordinates: {Number(selected.latitude).toFixed(4)}, {Number(selected.longitude).toFixed(4)} · {label(selected.category)}{selected.category === "disaster" ? " · impact radius shown is indicative only" : ""}</p>
          <div className="action-list">
            <Link className="button primary" href={selected.href}>Open in workspace</Link>
            <Link className="button" href="/sentinel">Hazard radar</Link>
            <Link className="button" href="/resource-planning">Resource planning</Link>
          </div>
        </section>
      )}
      <section className="panel">
        <div className="panel-head"><h3>On the map</h3><span>{visible.length} shown · refreshes every 30 seconds</span></div>
        {visible.length ? visible.slice(0, 60).map((pin) => (
          <button key={pin.id} type="button" className={"queue-row radar-row" + (selected?.id === pin.id ? " selected" : "")} onClick={() => setSelected(pin)}>
            <span><strong>{pin.title}</strong><small>{label(pin.category)} · {Number(pin.latitude).toFixed(3)}, {Number(pin.longitude).toFixed(3)}</small></span>
          </button>
        )) : <p className="panel-body">Nothing with coordinates in the enabled categories yet. Missions need a confirmed location; inventory without coordinates is counted in Resources, not pinned.</p>}
      </section>
    </div>
  );
}
