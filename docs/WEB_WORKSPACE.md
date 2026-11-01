> **Legacy browser cleanup - 1 November 2026:** `webApp/` and its Compose build/deployment helpers have been removed. Commands below targeting that module are historical. Use `npm run dev --prefix web` or `bash scripts/build-web.sh` for the current admin portal. Shared/native Kotlin modules are retained.

# ResQOps web workspace

> Superseded for the browser on 1 November 2026: the default web presentation is now React/Next.js in `web/`. See [the migration inventory](NEXT_PORTAL_MIGRATION.md) and [run instructions](../web/README.md). The mapping below remains a record of the shared/native workflows; Compose layout and browser build instructions are historical. Email/password authentication replaces mandatory MFA.

The Android, browser (JS and Wasm), desktop and iOS entry points now call the same authenticated `App`. The live workspace is the only operational interface. The former local/practice navigation, seeded repository, preview entry points and snapshot persistence adapters have been removed. Existing local snapshots are not uploaded or treated as operational data.

## Layout and identity

- At 1000dp the application uses a persistent 224dp navy sidebar, workspace and region selectors, and a flexible content area. Smaller windows use a menu with the same destinations.
- Record pages use searchable, status-filtered lists. Search and status filters explicitly apply to loaded records; pagination remains available. Wide content areas show a 410dp review panel; smaller ones show a dialog.
- Creation forms open on demand. Mission transitions, bulletin publication/withdrawal, moderation and credential revocation require confirmation. Server-side versions and idempotency keys remain authoritative.
- The account journey has a separate brand panel on desktop. Account controls are divided into Profile, Privacy, Workspaces, Administration and Account security.
- The ResQOps palette, Oswald headings, Overlock body type, supplied icons and onboarding artwork remain shared with Android.
- A pending operational request locks navigation until it is confirmed or the user stops retrying. A retry uses the original key and payload. Stopping a retry does not claim to cancel a server-side change.

The pen.dev canvas contains `Resqops Web / …` frames for all 24 workspace destinations and a desktop account-entry frame. Canvas: `C:/Users/admin/.pencil/documents/54bc5dce-fde6-4561-822c-5e6cec8ffd86/pencil-welcome.pen`. PNG exports are in `designs/resqops-web`. Illustrative canvas data is not included in runtime screens.

## Screen and action map

`operations` below means `/api/organizations/{org}/regions/{region}/operations`. Every request uses the current Firebase identity; the server checks membership, scope, assurance and version.

| Destination | Actions / backend capability |
| --- | --- |
| Overview | `GET operations/metrics`; links to missions, applications, reports, hours and resources |
| Missions | `GET operations/records?kind=MISSION`; `CREATE_MISSION`, `TRANSITION_MISSION`, `MESSAGE`, `REPORT`; draft date/capacity/coordinate validation |
| People | Organization membership pagination, email invitations scoped to the selected region, versioned membership suspension (organization administrator capability) |
| Applications | `APPLICATION` records; `ASSIGN` checks the mission, current application, responder availability, qualifications, capacity and selected inventory atomically |
| Assignments | `ASSIGNMENT` records; own `CHECK_IN` and `COMPLETE_ASSIGNMENT` |
| Reports | `REPORT` records; `TRIAGE_REPORT`, reason and optional related report |
| Inbox | `MESSAGE` records; reply in the mission thread and `MODERATE_MESSAGE` |
| Bulletins | `BULLETIN` records; new `PUBLISH_BULLETIN` and `WITHDRAW_BULLETIN`, priority, regional audience and versioned withdrawal |
| Deployment map | Mission coordinates on the existing map widget and accessible record list; mission details and actions |
| Sentinel | Existing regional forecast/hazard evidence, provenance and role-aware assistant endpoints |
| Resources | `INVENTORY`; `CREATE_RESOURCE`, `ADJUST_STOCK`, `TRANSFER_STOCK`; reservation occurs during assignment |
| Stock history | `STOCK_LEDGER` with opening counts, adjustments, transfers, reservations, releases and consumption |
| Training | `COURSE`; `CREATE_COURSE`, `PUBLISH_COURSE`, `ENROLL_COURSE` |
| Learning | `ENROLLMENT`; own lesson submissions and coordinator training review |
| Qualifications | `CREDENTIAL`; verification through training review, expiry and `REVOKE_CREDENTIAL` |
| Hours | `HOURS`; `APPROVE_HOURS`, with a different coordinator required for approval |
| Insights | Bounded metrics, interval validation and authenticated, capability-checked CSV export |
| Activity | Region-scoped `EVENT` records |
| Security | New `GET operations/dispatch`, `SET_DISPATCH`; paginated organization audit for authorized administrators |
| Settings | Existing recruitment, units, domain proof and partnership controls |
| Support | `SUPPORT`, `REPLY_SUPPORT`, `TRIAGE_SUPPORT` |
| Profile | Existing personal profile, privacy, invitations, discovery, MFA, account/session and export endpoints |
| Team Pro | Existing sponsorship list and versioned seat changes |
| Pro | Existing subscription controller, entitlement status and available purchase/management flow |

## Replacing practice-only concepts

Local incident cards become field-report triage and related reports; operational work is represented by missions. Local skill labels become course skills and verified, expiring credentials. Local broadcast previews become regional bulletins and mission conversations. Local security toggles become a real regional dispatch pause and existing institution identity policies. Local charts become bounded server-computed metrics. Fake telemetry and compliance assertions are not carried forward.

The regional pause blocks new assignment and mission activation. It intentionally permits check-in, safe completion, reporting, communication, mission cancellation and reconciliation. The control is stored in the existing scoped operation-record model, versioned, idempotent and audited. Mutations serialize under the existing organization transaction lock; no new database table is required.

Bulletins publish in the workspace. They do not claim push, SMS, email delivery or acknowledgement. Emergency dispatch continues to use the organization's established channels. Maps show mission meeting points, not live responder tracking. The map widget now accepts a distinct `mission` point kind and uses a teal marker.

## Validation and deployment

The backend changes are in the sibling `backend` repository, including updated OpenAPI output, dispatch-policy tests and opt-in PostgreSQL/RLS integration scenarios. Deploy that backend before the new UI so the dispatch and bulletin contracts are available. This implementation does not deploy Railway services or migrate production data.

Browser smoke test: build `:webApp:jsBrowserDevelopmentExecutableDistribution`, then run `node scripts/verify-web-workspace.cjs`. To check the optimized bundle, build `:webApp:jsBrowserDistribution` and set `RESQ_WEB_DIST=../webApp/build/dist/js/productionExecutable` before running the script. It uses installed Microsoft Edge through the sibling backend's Playwright dependency, intercepts identity/API traffic with fixtures and performs no live mutations. Screenshots are written to `designs/resqops-web/browser`.

Verified locally on 2026-10-29:

- Optimized JS distribution and Wasm compilation passed.
- Production-bundle browser smoke passed: authenticated entry, all 24 destination routes, browser back/forward, mission review and mobile navigation, with no browser runtime errors. API responses in this check are fixtures, so this does not establish production connectivity or end-to-end mutation correctness.
- All 16 shared JVM tests passed. Shared Android and Android app Kotlin compilation passed.
- Backend application and test TypeScript checks, 16 targeted backend tests, the 97-route OpenAPI contract check and portal build passed.
- Full Android APK assembly is blocked by the existing dependency configuration: `okhttp-android:5.5.0` requires compile SDK 37, while the project uses SDK 36. That dependency/toolchain migration remains separate from this web change.
- PostgreSQL/RLS integration scenarios were added but skipped because no dedicated test database was configured. iOS was not built on this Windows machine.

The exported [pen.dev design specification](../designs/resqops-web/resqops-web-spec.pdf) and [browser screenshots](../designs/resqops-web/browser) accompany this implementation.

Database integration tests require `SENTINEL_TEST_DATABASE_URL` pointing to a dedicated local test database; they must not be pointed at production. Native iOS validation requires macOS.
