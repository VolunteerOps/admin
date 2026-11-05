/** Reject missing/invalid positions, but preserve valid coordinates on the equator. */
export function coordinates(value: any): [number, number] | null {
  const x = value?.longitude ?? value?.lng ?? value?.center?.[0];
  const y = value?.latitude ?? value?.lat ?? value?.center?.[1];
  if (x == null || y == null || typeof x === "boolean" || typeof y === "boolean" || String(x).trim() === "" || String(y).trim() === "") return null;
  const lon = Number(x), lat = Number(y);
  return Number.isFinite(lon) && Number.isFinite(lat) && Math.abs(lon) <= 180 && Math.abs(lat) <= 90 ? [lon, lat] : null;
}
export function publicSourceUrl(value: unknown): string | null {
  try { const url = new URL(String(value)); return ["http:", "https:"].includes(url.protocol) ? url.href : null; } catch { return null; }
}
