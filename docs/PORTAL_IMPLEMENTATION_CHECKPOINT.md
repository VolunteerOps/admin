> **Current portal release - 1 November 2026:** Guided weather and mission conversations, automatic briefing, region search, plain-language errors, navigation caching and crash/theme/scroll fixes are documented in [PORTAL_ASSISTED_WORKFLOWS.md](PORTAL_ASSISTED_WORKFLOWS.md). Google billing and GCS authorization remain external prerequisites; integration paths are enabled.

> **Superseded web implementation — 1 November 2026:** The browser UI has been replaced with React/Next.js, shared Kotlin policies and Android-derived Oswald/Overlock styling. Required MFA has been removed while role checks and recent password authentication remain. See [NEXT_PORTAL_MIGRATION.md](NEXT_PORTAL_MIGRATION.md) for the current screen inventory, deployment and verification. The record below describes the earlier Compose web iteration.

# ResQ portal implementation checkpoint — 30 October 2026

This is an implementation record, not a production-readiness certificate. Screenshots under `designs/portal-v2/browser` use explicit local fixtures. No operational records were seeded into Railway.

## Implemented in this working tree

- Seven navigation areas, contextual tabs, responsive layouts, Raleway/Open Sans, supplied ResQ light/dark tokens and subtle contour artwork.
- Scoped coordination queue with current counts, mission dossier, server-side search/status filters and Mapbox evidence selection without camera reset on every refresh.
- Regional policy editing, forecast-job controls and source configuration/observed progress; polling continues until a terminal state. Source timestamps are not represented as worker heartbeats.
- Muse Spark `muse-spark-1.3`: bounded read tools for mission requirements, applicants, credentials, commitments and stock. Durable proposals, expiry, audited revisions/rejection, recomputation, recent-MFA approval and atomic existing assignment commands.
- Resource proposals can retain a scoped weather assessment through a fifth evidence tool. Assessment version, forecast age, regional bounds and policy are rechecked before approval. Interrupted model runs become visible failures after five minutes without automatically repeating a provider call; history is newest first.
- Muse Wonder `muse-wonder-1.0`: bounded WeatherNext queries, deterministic weather indicators, source IDs, assumptions and missing evidence. Optional separate 0.05° station-head temperature/dew-point query. No local flood-impact estimate is invented.
- Assistance intake, deduplication into an open case, severity/deadlines, acknowledgement, response progress, escalation record, coordinator handover, resolution and private timeline. No Pro requirement. Recording a case does not send a movement order or guarantee notification delivery.
- Additive canonical privacy migrations for agent records and assistance records; contracts regenerated. Both migrations were subsequently applied to Railway staging during sign-in recovery, with full schema verification. See `ADMIN_SIGNIN_RECOVERY.md`.

## Verification recorded

- Full backend suite: **159 passed, zero failed or skipped**, using isolated PostgreSQL 18/PostGIS. Subsequent polygon-intersection and billing-error corrections passed all 13 targeted provider tests, including new boundary and cost-cap regressions.
- Real database tests cover allocation/stock/responder-reservation rollback, concurrent approval replay, stale versions, changed weather policy, demoted-author RLS, assistance privacy, deduplication, competing acknowledgements and owner-only resolution.
- Both privacy migrations applied and the full schema verified on the disposable loopback database only. Test runtime roles had no ownership or RLS bypass. The cluster uses UTC.
- Backend TypeScript/test types, 108-route contract checks and Mapbox portal bundle passed. The final web Kotlin compilation, webpack bundle and development distribution passed. Webpack was run separately with a 1 GB Node heap after Windows resource exhaustion during the combined build.
- Shared JVM tests and Android/Wasm compilation passed earlier in this continuation. The final inline-confirmation refinement was compiled and exercised on web; native packaging and release checks are still separate gates.
- Final browser fixtures passed desktop/mobile navigation, proposal revision and approval, weather-to-resource handoff, assistance acknowledgement, light/dark themes and phone layout. Approval/decision requests assert expected versions and idempotency keys. Screenshots use synthetic data; no live allocations were made.
- Added accessible names to decision fields. Browser testing found closed modal dialogs remaining in the accessibility tree; the two new workflows now use inline confirmation panels, and post-decision navigation passes. Changing a reason or review attestation clears its confirmation.
- Six targeted agent tests passed after adding the explicit required-tool list and support for a five-tool weather-linked planning turn within the existing eight-call total limit.
- Live Meta tests passed for **both exact model IDs**, authenticated function calls, and full production response schemas with synthetic evidence. Fixed JSON Schema numeric bounds after the provider rejected OpenAPI-style boolean bounds; regression covered.
- Google runtime grant verified: both WeatherNext table metadata calls and query dry runs now succeed.
- WeatherNext dry runs returned 674,398,137,600 bytes (0.1°) and 1,890,404,006,400 bytes (0.05°). These linked VIEW resources do not expose partition/clustering details.
- The first approved 700 GB query returned **zero rows and zero billed bytes** (`WEATHERNEXT_LIVE_VERIFICATION.json`). It matched grid centres; this does not prove forecast unavailability because a small region can overlap a cell without containing its centre.
- Queries now use `geography_polygon`, as specified in the supplied guides. Normalization retains overlapping grid-cell centres within the known grid half-width, including dateline edges. Deterministic job IDs reuse provider jobs on retry.
- Billing-cap failures surface as `FORECAST_QUERY_BUDGET_EXCEEDED` with an operator-facing explanation, without exposing provider SQL or credentials. They are not presented as model reasoning failures.
- The corrected owner-approved query reached BigQuery through the backend identity and was **rejected at the 700 GB cap**: `bytesBilledLimitExceeded`, at least **1,106,204,688,384 bytes** required. See `WEATHERNEXT_POLYGON_VERIFICATION.json`. No populated result was returned; byte-usage fields were absent.
- Following explicit owner authorization, one new query was submitted with a **2 TB (2,000,000,000,000-byte) cap**. Google rejected it with `quotaExceeded` at `unbilled.analysis`: free query-byte quota exceeded. The job is terminal; no results or billed-byte counters were returned. See `WEATHERNEXT_2TB_VERIFICATION.json`. No further query was submitted. Recurring access stays disabled with a **100 MB** query ceiling.
- Both WeatherNext GCS buckets deny the backend identity `storage.objects.list` and direct example-object metadata reads (`storage.objects.get`). No forecast chunks were downloaded. Personal-account access remains untested. Exact external bucket links, access instructions, billing requirements and a proposed Zarr benchmark are documented in `WEATHERNEXT_GCS_ACCESS_AND_COST.md`; observed permissions are in `WEATHERNEXT_GCS_ACCESS_VERIFICATION.json`.
- Railway CLI reauthentication succeeded as Kalama. The global CLI login was observed switching accounts; the corrected verification retained its token only in the running process and pinned the project/environment IDs.

## Verified external configuration

- Railway CLI: `Kalama's Projects`, project `73690bd6-2d68-4ac7-9330-b5da3ebed8f0` (`ResQ / Volunteer Ops`), staging `9de4cdd7-abad-4a36-ae1e-7577c06dc2d7`.
- Meta shared variable now resolves on `resq-api`; backend supports `META_MODEL_API_KEY` with `MODEL_API_KEY` fallback. No browser service receives this secret.
- Google project/billing: `resq-25be9`; dataset `weathernext_3`; US; tables `weathernext_3_0_0_0p1deg` and `weathernext_3_0_0_0p05deg`.
- Runtime identity: `resq-auth-staging@resq-25be9.iam.gserviceaccount.com`. Owner granted dataset Data Viewer and project Job User, verified after grant.
- During sign-in recovery, PostgreSQL staging was restored on its existing volume, the two privacy migrations passed full schema verification, and API deployment `bbb5291b-7284-44bb-b68b-dea506dd464a` reached SUCCESS. Public readiness and ResQOps Firebase configuration return HTTP 200 with correct local CORS. Other services and production rollout remain separate gates. See `ADMIN_SIGNIN_RECOVERY.md`.

## Required remaining work

1. Complete populated WeatherNext verification and select an operating budget before enabling recurring queries. Regional caching, billing telemetry and current-cycle discovery remain necessary.
2. Broaden visual/accessibility review across all forms and roles, including older modal workflows; paginate member choices beyond the first 100 roster entries. Complete final native packaging and release checks.
3. Complete worker heartbeat/supervision beyond observed source/job status. Other hazard adapters, spatial impact models, model activation gates and preparedness campaigns remain incomplete.
4. Complete warning review, supersession/cancellation, safety-check campaigns and durable push/delivery evidence. Assistance escalation currently records a human action; it is not an automatic alerting worker.
5. Android sensor/relay work, physical-device trials, GCS/Zarr ingestion, scientific validation, full cross-platform release, production recovery/load/security exercises and Railway rollout remain roadmap gates.
6. Review the entire seven-stage roadmap for other identity, platform administration, billing and storage gaps. Do not mark stages complete solely on these builds.

The original request remains active. These open items are not replaced by the agent implementation or this document.
