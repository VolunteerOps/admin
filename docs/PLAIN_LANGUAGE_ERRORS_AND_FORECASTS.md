# Plain-language errors and automatic forecast selection

## What was missed

The previous error map translated known API codes but allowed unknown messages and Zod custom validation details to pass through. The exact screenshot message, “Forecast initialization must be on a UTC hour”, arrived inside a `validation_failed.details` entry. Forecast background-job errors were also rendered directly.

## Current behavior

- API errors are translated once at the API boundary. Custom validation details use reviewed patterns, field labels and numeric bounds. Unknown backend wording is never echoed, including unrecognized error codes or malformed provider responses.
- Locally authored `UserFacingError` messages are a separate, explicit path for reviewed form instructions. This preserves useful instructions such as cancelling password confirmation without reopening the raw-provider-text bypass.
- Firebase sign-in, email-link expiry, account changes, network errors, permissions, conflicts, storage, stock, missions, training, memberships, billing, forecast jobs and assistant failures use the shared translator.
- Messages state the problem and the next action. A failed request does not claim that nothing changed when that cannot be established; the user is told to check the latest record before retrying.
- The source audit identifies **136 static backend error codes across 330 error sites**. Every identified code is mapped, with `validation_failed` handled by field-specific translation. Provider and unknown runtime failures also have safe fallbacks. See [PORTAL_ERROR_MAP.md](PORTAL_ERROR_MAP.md) and [PORTAL_ERROR_AUDIT.json](PORTAL_ERROR_AUDIT.json).
- Run `npm run audit:errors --prefix web` with the sibling backend checkout present to regenerate the inventory and map. It fails if a newly identified backend code has no mapping.

## Request a forecast

The default form asks only **How far ahead?** with **Next 24 hours** and **Next 48 hours**. **Get latest forecast** calls the authenticated `POST .../sentinel/regions/:regionId/forecasts/refresh-latest` route.

The backend checks coordinator access before querying, then asks BigQuery for the newest published forecast that intersects the region and contains the requested ending hour. It uses an initialization-time partition filter, fixed source table, parameterized geometry and time values, the configured byte ceiling and a bounded lookup timeout. It does not guess a publication time by rounding the current clock. The selected run must meet the region's freshness policy and published forecast horizon. Requested hours start after the current time rather than loading the first day of an old run.

After lookup, the existing forecast queue is used. Permissions, regional policy and map coverage are checked again before accepting the job. The selected publication time is returned for the progress message. The service does not hold a database transaction open during the provider lookup.

**Choose a specific forecast** remains an optional specialist path, with publication-time and hours-after-publication labels. Invalid minutes are caught before submission and are also translated if returned by an older API. The explanation is: “The minutes must be 00, for example 18:00 rather than 18:35. Choose a published forecast, or use the latest forecast option.”

Google billing and runtime WeatherNext access remain external prerequisites. If access is unavailable, the form explains that setup is needed; it does not invent a forecast timestamp or quietly show old evidence. No live weather query was used for this release's verification.

## Verification

Twenty targeted backend tests cover lookup SQL, published coverage, upcoming-hour selection, stale/missing forecasts, malformed-minute validation, permissions/billing error codes, cancellation and existing provider behavior. Backend and test TypeScript checks passed. API contracts regenerated and verified (112 routes).

Browser regressions cover the automatic form, exact screenshot input, legacy API validation details, inaccessible weather data, raw-error suppression and cancellation of password confirmation. All **96 browser tests passed**, along with the 20 targeted backend tests, frontend/backend TypeScript checks and API contract checks. Screenshots of the default form and invalid-minute explanation were visually reviewed.

Staging deployments succeeded:

- Admin: `2d710c77-f532-4105-bd0e-fe8025a4986f`
- API: `48e9ce07-927d-4c36-a49f-84108204f991`
- Forecast worker: `5cb04f65-0bd3-4d7b-907c-f5f0dc382b6b`

The deployed portal smoke check used isolated browser fixtures: HTTP 200, default 24-hour selection, no timestamp field, a visible Get latest forecast button and no browser exceptions. The live API health endpoint returned HTTP 200. No operational records or real provider queries were created by these checks.
