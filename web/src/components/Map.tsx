"use client";
import { useEffect, useRef, useState } from "react";
import mapboxgl from "mapbox-gl";
import type * as GeoJSON from "geojson";
import "mapbox-gl/dist/mapbox-gl.css";
import { resqMapStyle } from "@/lib/resqMapStyle";
export type Point = {
  id: string;
  longitude: number;
  latitude: number;
  title: string;
  kind: string;
};
export default function MapView({
  points,
  scope,
  onSelect,
  coverage,
}: {
  points: Point[];
  coverage?: {west:number;east:number;south:number;north:number};
  scope: string;
  onSelect?: (id: string) => void;
}) {
  const container = useRef<HTMLDivElement>(null),
    map = useRef<mapboxgl.Map | null>(null),
    latest = useRef(points),
    select = useRef(onSelect);
  const [error, setError] = useState("");
  latest.current = points;
  select.current = onSelect;
  useEffect(() => {
    let disposed = false;
    const controller = new AbortController();
    fetch("/sentinel/map-widget-config", {
      signal: controller.signal,
      cache: "no-store",
    })
      .then((r) => {
        if (!r.ok) throw Error();
        return r.json();
      })
      .then((c) => {
        if (disposed) return;
        if (!c.token || !mapboxgl.supported()) throw Error();
        const instance = new mapboxgl.Map({
          container: container.current!,
          accessToken: c.token,
          style: structuredClone(resqMapStyle),
          center: [36.82, -1.28],
          zoom: 8,
          attributionControl: true,
        });
        map.current = instance;
        instance.addControl(new mapboxgl.NavigationControl(), "top-right");
        instance.on("load", () => {
          instance.addSource("operations", {
            type: "geojson",
            data: collection(latest.current),
          });
          instance.addLayer({
            id: "operations",
            type: "circle",
            source: "operations",
            paint: {
              "circle-radius": 7,
              "circle-color": [
                "match",
                ["get", "kind"],
                "hazard",
                "#bb6635",
                "mission",
                "#2563eb",
                "#52745f",
              ],
              "circle-stroke-color": "#fff",
              "circle-stroke-width": 2,
            },
          });
          const located=latest.current.filter(p=>Number.isFinite(p.longitude)&&Number.isFinite(p.latitude));
          if (coverage) {
            const east=coverage.east<coverage.west?coverage.east+360:coverage.east;
            instance.addSource('coverage',{type:'geojson',data:{type:'Feature',properties:{},geometry:{type:'Polygon',coordinates:[[[coverage.west,coverage.south],[east,coverage.south],[east,coverage.north],[coverage.west,coverage.north],[coverage.west,coverage.south]]]}}});
            instance.addLayer({id:'coverage-fill',type:'fill',source:'coverage',paint:{'fill-color':'#2563eb','fill-opacity':.12}});
            instance.addLayer({id:'coverage-line',type:'line',source:'coverage',paint:{'line-color':'#2563eb','line-width':2}});
            instance.fitBounds([[coverage.west,coverage.south],[east,coverage.north]],{padding:40,duration:0});
          } else if (located.length) {
            const b = new mapboxgl.LngLatBounds();
            located.forEach((p) => b.extend([p.longitude, p.latitude]));
            instance.fitBounds(b, { padding: 70, maxZoom: 12, duration: 0 });
          }
        });
        instance.on("click", "operations", (e) => {
          const f = e.features?.[0] as unknown as GeoJSON.Feature | undefined;
          if (f?.geometry.type === "Point") {
            new mapboxgl.Popup()
              .setLngLat(f.geometry.coordinates as [number, number])
              .setText(String(f.properties?.title || "Evidence"))
              .addTo(instance);
            select.current?.(String(f.properties?.id));
          }
        });
        instance.on("error", () =>
          setError(
            "Some map data could not load. The evidence list remains available.",
          ),
        );
      })
      .catch(() => {
        if (!disposed)
          setError(
            "Map unavailable. You can still review every location in the evidence list.",
          );
      });
    return () => {
      disposed = true;
      controller.abort();
      map.current?.remove();
      map.current = null;
    };
  }, [scope]);
  useEffect(() => {
    const source = map.current?.getSource("operations") as
      mapboxgl.GeoJSONSource | undefined;
    source?.setData(collection(points));
  }, [points]);
  return (
    <div className="map-panel">
      <div
        ref={container}
        className="map-canvas"
        aria-label="Regional evidence map"
      />
      {error && (
        <div className="map-unavailable">
          <div>
            <h3>Regional evidence</h3>
            <p>{error}</p>
          </div>
        </div>
      )}
      <div className="map-legend">
        {coverage ? <span>Selected operating coverage</span> : <><span>
          <i className="dot" />
          Mission
        </span>
        <span>
          <i className="dot gold" />
          Reported hazard
        </span>
        <span>{points.length} locations</span></>}
      </div>
    </div>
  );
}
function collection(points: Point[]): GeoJSON.FeatureCollection {
  return {
    type: "FeatureCollection",
    features: points
      .filter(
        (p) => Number.isFinite(p.longitude) && Number.isFinite(p.latitude),
      )
      .map((p) => ({
        type: "Feature",
        properties: { id: p.id, title: p.title, kind: p.kind },
        geometry: { type: "Point", coordinates: [p.longitude, p.latitude] },
      })),
  };
}
