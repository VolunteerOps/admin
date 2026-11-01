# VolunteerOps redesign and migration recovery

> Historical client redesign/migration record. The [seven-stage ResQ production roadmap](../../backend/docs/resq-production-implementation-roadmap.md) supersedes this document's project sequencing, hosting and old backend/authentication assumptions. Keep the UI requirements and dated evidence; checked boxes below describe the local-demo milestone, not full production completion. Current hosting guidance is the [Railway production runbook](../../backend/docs/railway-production-deployment-runbook.md).

## Audit and direction

The supplied screenshots match structural defects in the code: unbounded desktop rows squeeze text on phones; Android edge-to-edge content lacks insets; metric cards have no minimum useful width; permanent communications sidebars overwhelm compact screens; modal content does not handle the keyboard. Both apps contain sample repositories with incomplete mutations and misleading authentication, GPS, delivery and telemetry claims. The web's deleted portals remain recoverable from Git HEAD; its working tree already contains a partial landing migration.

The design combines the mood board's blue, navy, sky and white with Oswald headings and Overlock reading text. Glass is reserved for semantic alerts, badges and tags. Supplied vector icons replace emoji. The volunteer experience prioritizes the next assignment and readiness; the admin experience prioritizes staffing and incidents requiring a decision. Status labels carry meaning independently of color. Short, humane wording replaces terminal-style jargon.

## Implementation handoff

- [x] Audit volunteer, admin, web, original portal history and backend capabilities in parallel.
- [x] Inspect mood board, splash reference and Canvas architecture examples.
- [x] Preserve original working changes and stage source with a SHA-256 baseline.
- [x] Volunteer: safe-area adaptive navigation; Today, Explore, Missions, Inbox, Profile; training, notifications and settings drilldowns.
- [x] Volunteer: search, priority/skill filtering, saved missions, application review, assignment details, check-in, dated hours and field reports.
- [x] Volunteer: editable identity/contact/availability/languages/skills, achievements, course progression, scoped messages and persistent preferences.
- [x] Admin: safe-area phone navigation and wide-screen rail; Overview, People, Missions, Messages, More.
- [x] Admin: incident/triage/resource decisions, volunteer dossier and deployment, complete mission creation/edit/lifecycle/capacity validation.
- [x] Admin: skill taxonomy edits and personnel lookup, selectable/pannable deployment diagram, reviewed targeted local broadcasts, analytics and transparent local insights.
- [x] Admin: security/settings controls with accurate scope, complete help article drilldowns.
- [x] Cross-platform durable local state with injected persistence for domain tests; no fabricated network success.
- [x] Real supplied icons and corrected adaptive Android / iOS launcher assets.
- [x] Canvas splash: reference-proportioned layered art, actual caring hands emblem, painted tagline, cached paths and deferred animation reads.
- [x] Web: remove unused portal dependencies/providers, retain contact API/public pages, fix URL history and download gateway, update documentation.
- [x] Verification: Android build, metadata/JS/Wasm compilation, domain tests, web production build; capture phone/tablet screens and test key interactions.
- [x] Apply only reviewed changed files to the original projects after checking baseline hashes; retain before/after evidence.

## Responsive acceptance criteria

Compact layouts start with one readable content column and 16–20dp page margins. Metrics use two columns only when usable; headings and actions stack rather than compete. Secondary chips wrap or scroll horizontally. At wide sizes navigation becomes a rail and bounded content uses multiple columns. All screens respect system bars and the keyboard. Interactive targets are at least 48dp; meaningful body text uses approximately 16/24sp, labels 14/20sp, and headings 28/34sp. Text scaling must preserve reading order and actions. Dialogs scroll within a bounded viewport.

## Service boundaries

The backend has events, deployments, volunteers, field reports, channels, notifications and token auth, but lacks dedicated applications, saved missions, hours, training and certification APIs. Its communications stream lacks authorization/channel isolation; deployment mutations need ownership checks; sign-up accepts elevated roles. AI replies and some maintenance/telemetry responses are fabricated. This work makes the clients honest and fully interactive in durable local demo mode; live operational use requires completing and securing these backend contracts. It must never silently claim remote dispatch, device GPS, official certification, satellite delivery or multi-user synchronization.

## Verification evidence

Record exact commands, outcomes, screenshots and unsupported platform checks in `VERIFICATION.md`. iOS compilation and signing require a macOS/Xcode host. Production store links must remain unavailable until actual releases are configured.

## Delivery status

The redesigned client journeys and landing migration are implemented. Live identity, remote synchronization/delivery, accredited training and geographic map integration remain explicit release work; see `MIGRATION.md`. Android delivery uses version 1.1.0 / code 2 so the redesigned build is distinguishable from the original 1.0 APK.
