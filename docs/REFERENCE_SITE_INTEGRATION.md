# Reference-site integration map

Source: `D:/Downloads/Volunteer Ops/ResQOps-Complete-Site`. Reference data and browser-only Pro flags are demonstration code, not production authorization.

| Request | Source | ResQOps implementation |
| --- | --- | --- |
| Contours | public/assets/contours.png; entry/pro panels; campaign banners | Reuse exact artwork; replace shared Contours; retain quiet backgrounds behind readable content |
| Navy sidebar | app/globals.css sidebar/nav-item/sidebar-pro | Navy shell, gold active icons, Workspace / Plan & Improve; organization and region stay in top bar |
| Pro | ProExperience.tsx, PromoCode.tsx | Plans, review, promo entry, verified activation, three-step welcome tour; platform-only promo controls |
| Entry | EntryFlow.tsx, entry.css | Splash, three onboarding steps, Firebase login/signup/recovery, persisted four-step account setup; membership approval stays required |
| Plan & Improve | page.tsx field reports/campaigns/training/insights | Existing field reports and training retained; campaigns with milestones/owner/deadline/bulletin; reports distinguish recorded and approved hours |
| Top bar | page.tsx notification-btn/top-avatar | Personal notification feed/read state; avatar menu for settings/profile/support/logout with confirmation |
| Sentinel | page.tsx full-radar/hazard-grid | Existing Mapbox map and all hazard layers; disaster radar includes Ask Sentinel, sources holds forecast evidence; preparedness links to campaigns |
| Tabs | Current NavigationTabs.tsx and shared CSS | Preserve raised curved design, including new routes |

Implementation status: completed locally on 3 November 2026. These changes have not been committed, pushed or deployed to Railway. Deploy the backend migrations and API before the frontend so its new routes are available.

## Added workflows

- `/welcome`, `/onboarding`, `/login`, `/signup`, `/forgot-password`: reference artwork and layouts, with Firebase authentication and recovery.
- `/setup`: four persisted steps, profile updates, existing-workspace requests, invitation acceptance and new-organization applications. Coverage here is a personal preference; it does not grant permissions or change operating boundaries.
- `/pro`, `/pro-guide`: Standard/Pro comparison, upgrade review, validated code entry, confirmed promotional activation and a three-step guide. Subscription state comes from the backend.
- `/campaigns`: regional campaigns, owners, deadlines, linked bulletins, version-checked milestone updates and archival.
- `/notifications`: regional activity, personal read markers, older updates and training/preparedness notification preferences.
- `/insights`: recorded/approved hours, completed missions, valid qualifications, regional comparisons, mission-record and volunteer-hour CSV downloads. Existing Pro export permission checks remain in force.
- `/promotions`: platform-administrator code creation and enable/disable controls. Terms are immutable; disabling a code stops new redemptions without revoking issued grants. The list shows the latest 200 codes.

## Backend changes

Canonical migrations `20261103T1832_portal_experience` and `20261103T1905_promotional_sponsorship` add personal preferences, notification read state, promotion codes/redemptions and narrowly scoped promotional sponsorship access. The Railway migrator applies the new runtime grants after migration verification.

New authenticated routes cover personal preferences, scoped notifications, platform promotions, code preview/redemption, identified-user web checkout and scoped record exports. The generated OpenAPI contract includes 151 routes.

No-charge grants expire, are limited to one redemption per person/code, enforce recipient restrictions and handle competing claims for the final slot atomically. They also support the existing three volunteer sponsorship seats while the sponsor remains eligible. Partial discounts use RevenueCat checkout; neither previewing a code nor viewing the guide grants paid access.

## Verification

- Full backend suite: 200 tests passed against an isolated PostgreSQL 18/PostGIS database. Expanded integration checks then passed for promotional sponsorship, export permissions, audit writes and CSV formula protection.
- Frontend: 126 distinct regression checks covered across the full run and focused reruns. The two full-run failures were stale test selectors/assumptions and were corrected and rerun successfully.
- Backend and frontend type checks, production builds, OpenAPI consistency, migration-chain checks and database verification passed.
- Desktop, dark-mode and 390px layouts checked; all onboarding/setup/tour steps and the upgrade dialog were captured. [Visual previews](../designs/reference-site/README.md) use test fixtures, not operational records.

Live payments and live provider/map results were not exercised in fixture browser checks. Existing provider integrations and the Mapbox style remain in place. No demonstration data or browser-only entitlement flags were introduced into live authorization.

External prerequisite: RevenueCat project projf7905c83 has no web app/payment provider as of this audit. Paid checkout needs a configured public purchase link; 100% promotional access is independently auditable and time-limited. A partial discount alone never grants access.

The user deferred that setup: see [Web billing checklist](WEB_BILLING_SETUP_TODO.md).
