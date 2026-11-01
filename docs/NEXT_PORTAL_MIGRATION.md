# React / Next.js portal migration — 1 November 2026

User direction: replace the Compose web presentation with React/Next.js, retain Kotlin business logic, match Android's aesthetic with web-specific layouts, preserve newer workflows, remove required MFA. This supersedes the earlier mandatory-TOTP design. Verified email, membership, role checks, scope, recent authentication for sensitive changes and audit remain enforced.

## Scope and route inventory

| Area | Screens |
| --- | --- |
| Entry | Welcome/sign-in, registration, recovery, email verification, pending admission, invitation inbox |
| Overview | Operational counts, attention queue, current missions, activity |
| Missions | Mission directory/dossier/creation, applications, assignment review, field reports, contributions, deployment map, resource proposals/review |
| People | Roster, member detail/invitation, training builder, training reviews, qualifications |
| Sentinel | Radar/evidence, forecast refresh/jobs, weather analysis/provenance, assistance intake/timeline/decisions |
| Communications | Mission conversations, bulletins/publication/withdrawal |
| Resources | Inventory, stock adjustment/transfer, reservation context, stock history |
| Administration | Regions/policy, dispatch controls, audit, insights/export, sponsorship, organizations/platform governance |
| Account | Profile, privacy, invitations/discovery, email/password recovery, sessions, export/deletion, subscription, support |

## Architecture

`web/` is the Next.js app. React owns DOM, routing, Firebase client integration and presentation. `business/` compiles Kotlin policy/validation/record logic into an ES module consumed by Next.js. Android includes the same Kotlin sources. Existing Compose screens are retained for native targets. The API remains the authorization authority; client validation never grants access.

The Next server forwards only configured backend API paths. Browser tokens use Firebase session persistence; operational requests carry a bearer token, scope and idempotency key. No seeded operational data or public demo authentication is used. Test fixtures stay in browser tests.

## Design

Android-derived navy and blue, restrained papaya accents, bundled Oswald and Overlock, pale canvas, clear control boundaries. Six operational areas plus administration; account links remain secondary. Desktop uses a compact rail, workspace/region header, contextual tabs and detail drawers. Narrow layouts use a menu and stacked detail views. Auth is a desktop split composition with a bounded form, not a stretched phone screen. No timed splash barrier.

## Verification ledger

The new app is deployed at https://resq-admin-staging.up.railway.app and runs locally at http://127.0.0.1:4173. The custom domain still requires the DNS change in [ADMIN_WEB_DNS.md](ADMIN_WEB_DNS.md).

Final frontend deployment `52a84252-5c4a-45b8-b66d-5db9b45b7e4a` reports SUCCESS. After the complete 85-test run, the final chart-spacing adjustment passed two additional desktop/phone checks and screenshot review. The machine-readable ledger is [NEXT_PORTAL_VERIFICATION.json](NEXT_PORTAL_VERIFICATION.json).

- Next production build and TypeScript checks pass. Shared Kotlin production JS generation, four JVM policy tests and Android compilation pass.
- All 85 browser tests passed, including all 32 routes at desktop and phone widths, entry states, real form interactions, backend-schema payload validation, scope/permission failures, password reauthentication, pagination and weather-assessment-to-resource-planning context. Browser records and identity are isolated fixtures, not live operational data.
- Desktop and phone screenshots were inspected under `designs/next-portal`. A separate check rendered the real configured Mapbox style successfully, with no Mapbox HTTP failures.
- Backend authentication regression suites pass (26 tests). Ordinary approved coordinator access uses email/password; sensitive changes retain recent password authentication. `AUTH_REQUIRE_MFA=true` is an explicit opt-in to the former stricter policy, and is not enabled in staging.
- Live HTTP checks confirm the Next app, correct Firebase project/app configuration and Mapbox configuration. Unauthenticated private reads and writes return 401. The frontend bundle audit reports zero known vulnerabilities.

## First administrator and initial setup

The existing, verified Firebase account `mtengo.moses@resq-app.co.ke` has an active ORG_ADMIN membership in the ResQOps workspace and an active TECH_ADMIN platform grant. These were provisioned through an audited operator action and read back from staging. The password was neither inspected nor changed. No MFA factor is enrolled. Sign in with the existing password; no authenticator setup is required.

The workspace deliberately starts without invented regions, responders or missions. Open **Administration → Workspace → Add region** and enter the real area and policy before regional workflows are available. Sensitive changes may ask for the password again; this is recent authentication, not MFA.

## Boundaries of verification

This migration does not certify weather forecasts, local flood-impact predictions or third-party delivery. Existing provider access, query ceilings and ingestion configuration remain as recorded in the WeatherNext documents. No paid weather job, live allocation, outgoing invitation or test operational record was submitted during browser checks. A full login with the owner's private password remains for the owner to perform. Production Railway services were not modified; staging is the review deployment.

The previous Compose web build is retained only as a legacy build path. Android/native screens remain in place. The repository Dockerfile and default web build script now select Next.js; the React app consumes generated Kotlin rules rather than maintaining a second JavaScript implementation of those shared policies.
