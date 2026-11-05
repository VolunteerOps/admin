/** Repair known legacy UTF-8 punctuation corruption without rewriting names or facts. */
export function displayText(value: unknown): string {
  return String(value ?? "")
    .replace(/â€”|â€“/g, "—").replace(/â€™|â€˜/g, "’")
    .replace(/â€œ|â€\u009d/g, '"').replace(/Â·/g, "·").replace(/Â°C/g, "°C")
    .replace(/\uFFFD/g, "");
}
export function resourceName(resource: any): string {
  return [resource?.vehicle_model || resource?.title || "Resource", resource?.plate_number].filter(Boolean).map(displayText).join(" · ");
}
