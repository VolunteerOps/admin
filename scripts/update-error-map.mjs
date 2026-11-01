import {writeFileSync,readFileSync} from 'node:fs';
import {errorGroups,friendlyErrors} from '../web/src/lib/friendly-errors.ts';
const inventory=JSON.parse(readFileSync('docs/PORTAL_ERROR_INVENTORY.json','utf8'));
const remaining=Object.keys(inventory).filter(k=>!friendlyErrors[k]);
writeFileSync('docs/PORTAL_ERROR_MAP.md',`# Portal error and recovery map — 1 November 2026

The frontend routes API, Firebase, form and assistant errors through one recovery-text layer. Known errors explain the problem and what the officer can do next. Unknown service errors do not expose provider internals, SQL, schema names or stack traces. The inventory in PORTAL_ERROR_INVENTORY.json records the originating backend files.

| Backend error family | What the officer sees |
| --- | --- |
${errorGroups.map(([codes,text])=>'| '+codes.map(c=>'\x60'+c+'\x60').join(', ')+' | '+text+' |').join('\n')}

## Validation detail rules

The API error boundary translates custom validation messages and field paths before any screen sees them. Unknown provider text is never echoed. Only locally authored UserFacingError text can be shown directly.

- Forecast minutes: "The minutes must be 00, for example 18:00 rather than 18:35."
- Mission duration: end after start, with separate shifts for responses longer than 24 hours.
- Website ownership: enter the website address without a protocol or page name.
- Numeric input: distinguish whole numbers, missing values and limits.
- Fields: display reviewed names and numeric bounds; never raw schema paths or database identifiers.

## Other cases

- Firebase credentials: check email/password; no account-existence disclosure for a failed login.
- Invalid forms: identify the affected field and whether it needs more detail, a smaller value or a valid date. The detailed forecast form catches backward/future/subhour dates before a network request.
- HTTP 401/403/404/429/5xx: sign-in, access, missing item, wait, or service-recovery guidance respectively.
- Network failure: check connection and retry without losing the draft.
- Component failure: keep the workspace shell and offer a section retry. The root fallback also has clear recovery navigation.
- Map tiles: retain the evidence list if the map cannot load.
- Confirmation phrases for leaving a workspace or deleting an account remain explicit; these are deliberate safeguards.

Codes handled by the field-validation translator rather than a single message: ${remaining.join(', ')}.

## Scope of assisted and manual workflows

| Workflow | Default experience | Why review remains |
| --- | --- | --- |
| Weather preparedness | Three suggested questions, natural-language conversation and one-click briefing; controlled BigQuery/GCS retrieval | Sources may be missing; a forecast is not an observed flood impact |
| Request a forecast | Latest published forecast is selected automatically for the next 24 or 48 hours. Manual publication-time entry is optional. | The source must actually contain the forecast and cover the requested period |
| Region setup | Search city/district/country, choose match, inspect coverage map, give reason | Screening thresholds are defaults until locally reviewed |
| Mission planning | Spark asks one missing question at a time, incorporates corrections, produces a validated draft | Coordinator explicitly creates the mission; no automatic dispatch |
| Resource planning | Existing assistant checks qualifications, commitments and stock; weather evidence can carry into review | Approval enforces current stock, eligibility, scope and versions |
| Assistance cases | Clear status, handover and resolution actions | Case ownership and operational decisions must remain explicit |
| Stock, hours, qualifications and training | Labelled forms, field validation and actionable errors | These change auditable operational facts; the model does not invent quantities, hours or qualifications |
| Membership, roles, domains, privacy and billing | Guided forms, retained drafts and plain recovery text | Authority, consent and irreversible decisions remain explicit |
`);
