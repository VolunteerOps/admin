"use client";
import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import {coordinates as coords} from "@/lib/map-data";
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
  disaster: { label: "Hazards & reports", color: "#f97316" },
  mission: { label: "Missions", color: "#2563eb" },
  report: { label: "Volunteer reports", color: "#22c55e" },
  deployment: { label: "Deployments", color: "#a855f7" },
  resource: { label: "Resources", color: "#eab308" },
};
export default function OperationsMap({ scope, revision }: { scope: Scope; revision: number }) {
  const orgBase = scope.sentinel.split("/regions/")[0];
  const regionId = scope.sentinel.split("/regions/")[1];
  const [rev, setRev] = useState(0);
  const [enabled, setEnabled] = useState<Record<string, boolean>>(Object.fromEntries(Object.keys(CATEGORIES).map((k) => [k, true])));
  const [selected, setSelected] = useState<OpsPin | null>(null);
  const [mapReady,setMapReady]=useState(false);
  const [error, setError] = useState("");
  const [updatedAt, setUpdatedAt] = useState("");
  const regions = useData(orgBase + "/regions", revision);
  const region = (regions.data || []).find((r: any) => r.id === regionId);
  const bounds = region?.forecastBounds;
  const tick = revision + rev;
  const missions = useData(scope.ops + "/records?kind=MISSION", tick);
  const reports = useData(scope.ops + "/records?kind=REPORT", tick);
  const inventory = useData(scope.ops + "/records?kind=INVENTORY", tick);
  const assignments = useData(scope.ops + "/records?kind=ASSIGNMENT", tick);
  const radar = useData(scope.sentinel + "/radar", tick);
  const container = useRef<HTMLDivElement>(null);
  const mapRef = useRef<mapboxgl.Map | null>(null);
  const markers = useRef<mapboxgl.Marker[]>([]);
  useEffect(() => { const timer = setInterval(() => { setRev((v) => v + 1); }, 30000); return () => clearInterval(timer); }, []);

  const pins: OpsPin[] = useMemo(() => {
    const out: OpsPin[] = [];
    const missionById = new Map<string, Row>((missions.data?.items || []).map((r: Row) => [r.id, r]));
    for (const pin of radar.data?.pins || []) {
      out.push({
        id: "radar-" + pin.id, category: pin.category === "PREDICTION" ? "prediction" : "disaster",
        title: pin.title, latitude: pin.latitude, longitude: pin.longitude,
        detail: [pin.verification || pin.status, pin.detail, pin.source].filter(Boolean).join(" | "), severity: pin.severity, href: pin.route?.href || "/sentinel",
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
    for (const resource of inventory.data?.items || []) {
      const c=coords(resource.payload);
      if(c)out.push({id:resource.id,category:"resource",title:[resource.payload.vehicle_model||resource.payload.title,resource.payload.plate_number].filter(Boolean).join(" · "),latitude:c[1],longitude:c[0],detail:resource.payload.location||"",href:"/resources?record="+resource.id});
    }
    return out.filter((p) => coords(p)!==null);
  }, [missions.data, reports.data, assignments.data, radar.data,inventory.data]);
  const visible = useMemo(()=>pins.filter((p) => enabled[p.category]),[pins,enabled]);
  useEffect(()=>{setSelected(null);},[scope.sentinel]);
  useEffect(()=>{setSelected(previous=>previous ? pins.find(p=>p.id===previous.id)??null : null);},[pins]);
  useEffect(()=>{if(radar.data?.generated_at)setUpdatedAt(radar.data.generated_at);},[radar.data]);

  useEffect(() => {
    let disposed = false;
    const controller = new AbortController();
    setError("");setMapReady(false);
    fetch("/sentinel/map-widget-config", { signal: controller.signal, cache: "no-store" })
      .then((r) => r.json())
      .then((config) => {
        if(disposed||!container.current)return;
        if(!config.token||!mapboxgl.supported()){setError("The map is unavailable. You can still review locations in the list below.");return;}
        const instance = new mapboxgl.Map({
          container: container.current, accessToken: config.token,
          style: structuredClone(resqMapStyle) as any, center: [36.82, -1.28], zoom: 6, attributionControl: true,
        });
        instance.addControl(new mapboxgl.NavigationControl(), "top-right");
        instance.on("load", () => {
          if(!disposed){setError("");setMapReady(true);}
        });
        instance.on("zoomend", () => {
          const zoom = instance.getZoom();
          const pitch=zoom>=15?60:zoom>=13?35:0;
          if(Math.abs(instance.getPitch()-pitch)>1)instance.easeTo({pitch,duration:600});
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
      .catch(() => { if (!disposed && !controller.signal.aborted) setError("The map could not load. Refresh this page to try again; locations are still available below."); });
    return () => { disposed = true; controller.abort(); mapRef.current?.remove(); mapRef.current = null; };
  }, []);

  useEffect(() => {
    const instance = mapRef.current;
    if (!instance || !mapReady || !bounds) return;
    const east = bounds.east < bounds.west ? bounds.east + 360 : bounds.east;
    instance.fitBounds([[bounds.west, bounds.south], [east, bounds.north]], { padding: 50, duration: 600 });
    const boundary:any={type:"Feature",properties:{},geometry:{type:"Polygon",coordinates:[[[bounds.west,bounds.south],[east,bounds.south],[east,bounds.north],[bounds.west,bounds.north],[bounds.west,bounds.south]]]}};
    const source=instance.getSource("operating-region") as mapboxgl.GeoJSONSource|undefined;
    if(source)source.setData(boundary);
    else{instance.addSource("operating-region",{type:"geojson",data:boundary});instance.addLayer({id:"operating-region-line",type:"line",source:"operating-region",paint:{"line-color":"#60a5fa","line-width":2}});}
  }, [bounds?.west,bounds?.east,bounds?.south,bounds?.north,mapReady]);

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
  }, [visible,mapReady]);

  useEffect(() => {
    const instance = mapRef.current;
    if (!instance || !mapReady || !selected) return;
    instance.flyTo({ center: [selected.longitude, selected.latitude], zoom: Math.max(instance.getZoom(), 11), duration: 700 });
  }, [selected?.id,mapReady]);

  return (
    <div>
      <div className="ops-map-head">
        <div>
          <span className="radar-eyebrow">Operations map</span>
          <h2>Live operations picture</h2>
          <p>Predictions, reported hazards, missions, volunteer reports and deployments for {region?.name || "this region"}. Zoom in past street level for 3D buildings and precise pinpointing.</p>
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
      {[error,missions.error,reports.error,assignments.error,radar.error].filter(Boolean).map((message,i)=><Notice error key={i}>{message}</Notice>)}
      {!!radar.data?.unavailable_sources?.length&&<Notice error>Some hazard sources could not be checked: {radar.data.unavailable_sources.join(", ")}. The map is incomplete.</Notice>}
      {(missions.data?.next_cursor||reports.data?.next_cursor||assignments.data?.next_cursor||radar.data?.truncated)&&<Notice>The map shows the most recent records. Open the relevant workspace to review older records.</Notice>}
      <div ref={container} className="ops-map" aria-label="Live operations map" style={error ? {display:"none"} : undefined} />
      {selected && (
        <section className="panel radar-detail" aria-label="Map detail">
          <div className="panel-head"><h3>{selected.title}</h3>{selected.severity && <Badge status={selected.severity} />}</div>
          <p>{selected.detail}</p>
          <p>Coordinates: {Number(selected.latitude).toFixed(4)}, {Number(selected.longitude).toFixed(4)} · {label(selected.category)}{selected.category === "disaster" ? " · Location only; no affected-area estimate" : ""}</p>
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
