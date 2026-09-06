# Migration coverage and integration boundaries

## Design handoff

The audit found desktop rows squeezing text to single characters, headers underneath status bars, nonadaptive card grids, emoji navigation, and forms without bounded keyboard-aware scrolling. The redesign gives each role a small primary navigation set, explicit drilldowns, readable typography and real vector icons. Volunteer screens prioritize the next assignment; administrator screens prioritize staffing, incident decisions and reviewed actions.

Phone content stacks with bounded forms and inset-safe navigation. Wide screens use a rail; communications become a two-pane workspace where available. Glass is a portable translucent fill, sheen and fine border, with explicit text status. It does not depend on platform-specific backdrop blur.

## Screen and action coverage

| Original portal area | New client journey |
| --- | --- |
| Volunteer overview | Today, next assignment, contribution metrics, availability, learning and achievements |
| Mission discovery | Search and priority filters, saved missions, skill matching, briefing, reviewed application and withdrawal |
| My missions | Status filters, assignment, device/manual location check-in, dated hours, reports, activity history and local service record |
| Volunteer profile | Contact, emergency contact, availability, languages, self-reported skill add/edit/remove and achievements |
| Volunteer communication | Channel-specific inbox, pinned announcements, read-only alerts and local message composer |
| Training | Category/search catalog, enrollment, ordered lessons, comprehension checks and completion record |
| Notifications/settings | Unread management, mission links, contact channel preferences and reduced motion |
| Admin overview | Derived staffing totals, incident review, response status, inventory allocation/restock and drilldowns |
| Volunteer registry | Search/filter, person creation, dossier, deployment history, reviewed assignment and release |
| Mission control | Create/edit, capacity and coordinator, briefing, planned/active/paused/completed lifecycle and roster |
| Deployment map | Pannable/zoomable response diagram, selectable teams/incidents and guarded resource assignment |
| Skills | Taxonomy editing, demand, personnel counts and dossier lookup |
| Command communications | Targeted message draft, recipient review, severity and immutable local dispatch record |
| Analytics | Derived workspace metrics, labeled sample trends, regional capacity and local calculation assistant |
| Security | Practice dispatch pause, activity audit and explicitly nonenforced account-policy preferences |
| Help/settings | Searchable detailed workflow articles, organization preferences and contact guidance |
| Web | Landing and public information routes, contact form and configured app release gateway |

## Runtime boundary

The clients are durable **local demonstration workspaces**. They do not authenticate organisation accounts, synchronize with each other, deliver messages remotely or dispatch real responders. No arbitrary password is presented as valid authentication. Local lessons and contribution records do not confer accredited medical qualifications. Device location is real when permission and a provider are available; its capture does not imply remote check-in delivery.

State persists through platform storage with versioned snapshots and mutation validation. Local preferences do not activate SMS, email or push gateways. Account-policy controls do not enforce backend authorization. The response map is a diagram, without satellite imagery, geographic routing or live tracking. The analytics assistant calculates local data and does not call an AI service. Application content is English; language choices are contact preferences.

The inspected backend exposes auth, volunteers, events/deployments, skills, reports, channels and notifications. Dedicated applications, saved missions, hours, course progression and certification contracts are absent. Sign-up role validation, deployment ownership and communications-stream authorization/channel isolation require correction before operational integration. Backend source was not changed by this client redesign.

## Remaining release work

Connect and secure role-specific identity/session APIs; implement missing domain endpoints and cross-client synchronization; add conflict/retry and real delivery state; configure an authenticated geographic provider if needed; integrate notification gateways; validate accredited training workflows. Build and exercise iOS on macOS/Xcode, verify physical-device location, conduct accessibility and performance testing, then configure signed store releases. The current debug builds are for design and workflow review.

## Artwork

The splash reference is converted into cached, palette-layered vector paths rendered through Compose Canvas. No bitmap is drawn at runtime. The painted tagline is revealed with an animated bristle mask; the composition preserves the source proportions instead of stretching the people on wide screens. Quantization slightly simplifies source texture, so it is a close vector reproduction rather than a pixel-identical raster. Android launcher artwork uses the caring-hands mark; iOS assets use the supplied role-specific logos.
