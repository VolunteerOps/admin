# Assisted workflows and portal fixes — 1 November 2026

This release implements the seven requested portal improvements. The Google Cloud billing and WeatherNext bucket grants are still pending; the integration paths are configured now so a new request can use them after provisioning, without another feature deployment.

## User journeys

| Area | Entry point and behavior |
| --- | --- |
| Sentinel / Weather analysis | Three clickable example questions, natural-language follow-ups, and **Prepare today's briefing**. Muse Wonder calls controlled tools to retrieve and calculate evidence, then explains preparation priorities, sources, missing inputs and next questions. |
| Daily briefing | Automatically compares archived forecasts from the previous three days with forecasts for the next seven. Both BigQuery and GCS are attempted; inaccessible sources are shown explicitly. No forecast cycle or coordinates need to be entered. |
| Detailed assessment | Remains available for specialists. Date validation explains whether the forecast was issued after the requested period, whether dates are reversed, or whether minutes need to be removed. |
| Missions / Plan a mission | Muse Spark collects information one question at a time, incorporates extra notes and corrections, searches real places and asks for location confirmation. The backend validates the draft against the existing mission schema. Only **Confirm and create mission** creates it; chat cannot dispatch volunteers. The original form remains available. |
| Administration / Add region | Search a city or district and country, choose a Mapbox result, review coverage, and give a reason. Name, bounding box and initial screening policy are filled automatically. Advanced settings remain available. |
| Resource planning | Existing Spark proposals and human approval remain. A weather conversation with usable forecast evidence can feed the existing weather-to-resource planning journey. Freshness, region and policy are checked again before approval. |
| Other operational decisions | Assistance, communications, equipment, governance and membership changes retain explicit commands and review. These decisions are not silently inferred or submitted by the assistants. Their validation and service errors use the shared plain-language error map. |

Region defaults are ResQ screening defaults (35°C heat, 15 m/s wind, 20 mm hourly rain, 12-hour forecast age), **not calibrated local warning thresholds**. Country-wide selections may be too broad for the bounded regional weather tools; select an operational city/district in that case.

## Fixed root causes

- **People / Radar crashes:** the API serializes snake_case keys, while several views expected camelCase. API responses now retain the actual wire keys and expose non-enumerable aliases. This preserves strict outbound command schemas. Radar also reads the actual hazard `{ observations, ... }` envelope instead of treating it as an array.
- **Repeated header loads and region setup flash:** the portal shell now persists across Next.js navigation. Requests share a per-user, bounded in-memory cache, with in-flight deduplication and background refresh after freshness expires. Region setup waits for membership, region loading and selection to settle. Manual Refresh invalidates cached data; account changes clear it. Unauthorized responses discard protected cached results.
- **Dark text on dark surfaces:** theme variables are applied at the document root and the app explicitly inherits the theme foreground/background.
- **Two scrollbars:** drawers and the mobile navigation lock document scrolling while open and restore the previous state when closed, including nested locks.
- **Unhelpful errors:** [PORTAL_ERROR_MAP.md](PORTAL_ERROR_MAP.md) records the mapped messages and [PORTAL_ERROR_INVENTORY.json](PORTAL_ERROR_INVENTORY.json) records backend code origins. Unknown failures use safe next-step messages rather than exposing SQL, stack traces or raw error codes. Section failures preserve navigation and offer recovery.
- **Long assessment requests:** the Next.js rewrite proxy allows four minutes instead of its default 30 seconds, covering the backend's three-minute weather workflow deadline.

## Weather implementation and limits

Muse cannot submit arbitrary SQL, bucket paths, URLs or shell commands. The backend validates dates, region permissions and tool arguments. BigQuery performs partition-filtered, polygon-overlap queries with fixed columns; the GCS reader uses the WeatherNext statistics Zarr store, selects the region/time window before computation and counts bytes read. Deterministic services calculate rainfall, heat and wind indicators. Stored assessments retain source IDs, issued/retrieval times, coverage, assumptions and missing evidence.

The broad tool accepts the previous 31 days or next 15 days, up to a 14-day span per turn, split into at most 48-hour requests at the past/future boundary. One broad search is allowed per turn. The UI supports follow-up questions. Historical WeatherNext forecasts describe **what was predicted**, not what rain gauges measured. Neither source provides an independent seasonal ENSO outlook; a month-long/El Niño question must identify that missing source rather than extrapolate a claim from short-range forecasts. Local flood impacts also require local hydrological and exposure evidence.

BigQuery and GCS are two access paths to the same forecast model. Agreement between them is not independent confirmation. The model receives up to eight grid cells ranked by hourly rainfall with explicit selection labels, not invented regional percentiles.

## Provisioning state

The staging API has `WEATHERNEXT_ACCESS_ENABLED=true`, `WEATHERNEXT_GCS_ACCESS_ENABLED=true`, a **2,000,000,000,000-byte cap per BigQuery query** and a 1,000,000,000-byte total GCS allowance divided across the requested windows. The BigQuery cap is per query, not a total daily briefing budget. These replace the earlier disabled/100 MB configuration at the user's request. Requests still have finite row, time, memory and concurrency bounds.

The runtime identity remains `resq-auth-staging@resq-25be9.iam.gserviceaccount.com`, with billing project `resq-25be9` and linked dataset `weathernext_3`. Google billing and bucket access must be activated for this runtime identity, not only the coordinator's Google account. Once activated, submit a new briefing. Completed BigQuery jobs are reused; recoverable billing/access failures can be retried under a new bounded job ID on a subsequent request. There is no automatic recurring paid weather schedule.

A dedicated `resq-forecast` worker is deployed for the existing queued Radar refresh action. It shares only database and Google access needed for that work, not the Meta model key. Conversational weather queries run through the API directly.

## Verification

- Frontend production build and TypeScript check; 91 browser checks passed, including desktop/mobile routes, actual snake_case fixtures, People/Radar, region setup, drawer scrolling, dark mode, navigation caching, chat and explicit mission creation.
- 34 targeted backend tests passed for weather windows, query bounds, job recovery, model tools, source contracts and planning evidence.
- Four Python tests passed using actual synthetic Zarr arrays, covering reads/calculations, source metadata, byte/row bounds and invalid windows.
- Live Muse Wonder 1.0 and Muse Spark 1.3 calls returned structured results and used their required controlled tools with synthetic context. Live Mapbox geocoding returned valid Mombasa matches and bounds.
- Browser screenshots use explicit test fixtures. No real missions, allocations or invitations were created during verification. Full live BigQuery/GCS weather retrieval remains unverified until the user's external provisioning is complete.

## Staging release

- Admin: `a413c686-16a3-4c3a-bfb6-658a247e085a` - SUCCESS.
- API: `ae142cf3-b255-4817-9d7c-042c2d385253` - SUCCESS.
- Forecast worker: `d267aa9a-6683-4f5d-bd32-8c6d96e02cc6` - SUCCESS; queue-backed health check passed.
- Portal: https://resq-admin-staging.up.railway.app
- Final six guided-flow regressions passed again after the wording, local-time display, cache-bound and proxy-timeout refinements.
- Live Mapbox region preview: one rendered map canvas, no fallback, no failed Mapbox responses.
- Backend runbook: `backend/docs/assistant-weather-runtime.md`.


Deployed smoke check: People, Radar and Weather analysis returned HTTP 200 and rendered their expected headings with isolated browser fixtures. All three weather suggestions and the daily briefing button were present; no browser exceptions occurred. The live API health endpoint returned HTTP 200.
