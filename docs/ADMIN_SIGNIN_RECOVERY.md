> **Current status — 1 November 2026:** Sign-in now uses the React/Next.js portal at https://resq-admin-staging.up.railway.app. The verified account `mtengo.moses@resq-app.co.ke` has active ResQOps administrator membership and platform access. Use the existing password; MFA is disabled. Earlier MFA-enrollment instructions below are historical and superseded. See [NEXT_PORTAL_MIGRATION.md](NEXT_PORTAL_MIGRATION.md) for checks and initial region setup.

# Administrator sign-in recovery — 30 October 2026

Requested account: **mtengo.moses@resq-app.co.ke**. This supersedes the earlier address. No account was created for the earlier address.

## Confirmed causes

- The locally served portal at `http://127.0.0.1:4173/` targets `https://resq-api-staging.up.railway.app`.
- Both public identity configuration endpoints returned HTTP 404, Railway `Application not found`, without CORS headers. This fails before Firebase checks a password.
- Railway reported `resq-api` failed/stopped and its PostgreSQL dependency had no deployment.
- API environment has the volunteer Firebase web settings but lacks the separate `FIREBASE_OPS_WEB_*` settings used by the administrator portal.
- The runtime service account can look up Firebase users, but Firebase denied web-app listing and denied the attempted creation of the requested corrected account (`auth/insufficient-permission`). The attempted creation did not succeed.
- The old `admin@volunteerops.org` password exists only in the legacy database seed. It has not been verified as a Firebase login. Do not run that seed: it deletes existing tables' data.

## Local fix verified

`BrowserIdentity.kt` now distinguishes service/configuration failures, invalid credentials, connectivity, provider/domain restrictions, throttling and verification failures. Unknown provider responses are not printed to users. Credential failures do not distinguish unknown accounts from wrong passwords.

Password recovery no longer reports success for arbitrary configuration/network failures, and cancellation is preserved.

JavaScript compilation, webpack and distribution assembly passed. Browser tests with intercepted synthetic requests passed service-unavailable, missing-config, invalid-credential and MFA challenge/invalid-code cases. Configuration failures made zero password requests; the credential case made one intercepted request. No real credentials were submitted in these tests. Repeat with `node scripts/verify-signin-errors.cjs` while the local portal runs on port 4173.

The entry flow now accepts an MFA challenge explicitly and exposes account security before workspace admission. `node scripts/verify-pending-account.cjs` passed: a verified but unassigned identity reaches authenticator setup and returns to pending access without entering a workspace or making privileged API calls.

## External setup in progress

- PostgreSQL staging deployment **SUCCESS**: `e56c5a10-b470-4918-a78d-3cb1ae70dc53`. Existing volume resumed; PostgreSQL 18.6, PostGIS 3.6.4, verified TLS and restricted runtime/migration roles confirmed by the read-only runner `85e150fe-bc74-4452-b474-627f0236aabf`.
- The existing schema had seven users, zero organisations and zero platform grants. Only the two tested additive RLS policies were missing. Migration deployment `858d8955-69e5-4769-93a5-2619ab55c16c` applied both and passed full schema verification at `83008733c091b8a0452428b97e02ad4f482b39f0a30edb8889715fe0d290ec2e`. No seed/reset was used.
- API deployment **SUCCESS**: `bbb5291b-7284-44bb-b68b-dea506dd464a`. `/api/ready` returns HTTP 200, `ok: true`, no blockers.
- Owner supplied Web App ID `1:3158004596:web:745e59611745458905d221`. Configured `FIREBASE_OPS_WEB_*` using this App ID and the existing same-project public API key/auth domain. Both public ops-config endpoints now return HTTP 200 and the correct app/project IDs.
- Added exact staging CORS origins `http://127.0.0.1:4173` and `http://localhost:4173`, preserving existing allowed origins; response header verified for the local portal.
- Owner created `mtengo.moses@resq-app.co.ke` in Firebase Authentication. Lookup confirms UID `evsu3nCEflVsFy724JsjkKruAiV2`, password provider and enabled status. After the owner completed verification, a fresh Firebase Admin lookup confirmed `emailVerified: true`. Password was chosen privately by the owner; it was not retrieved or tested. No initial-password file was created.
- The same lookup returned no enrolled second factors. Authenticator enrollment, a recent TOTP sign-in, immutable Firebase-to-backend identity binding and explicit workspace admission remain necessary. No administrator grant has been inserted and no identity controls bypassed.

The API and database readiness checks now pass, and email verification is confirmed. The owner was asked to open **Account and security → Account security → Set up authenticator**, confirm enrollment, and sign in again using the authenticator code. First-owner bootstrap still requires the existing guarded operator procedure with verified identity binding and a TOTP-authenticated ID token from a sign-in within five minutes; enrollment alone is insufficient. Do not request passwords, authenticator secrets, codes or ID tokens in chat.

The existing `backend/scripts/identity-operator.ts` is the bootstrap entry point. It verifies the Firebase token, requires the separately authorized database owner connection, checks the reviewed numeric user mapping, and invokes `bootstrapPlatformOwner`. The transaction refuses any second bootstrap and records the grant in the audit log. Workspace creation and administrator membership remain separate reviewed operations after this grant.

## Authenticator confirmation follow-up

The owner reported the generic identity error after entering a code and confirming enrollment. A new read-only Firebase lookup confirmed verified/enabled status and no enrolled factors. Project configuration inspection was unsuccessful; the client error alone does not establish a project configuration problem.

The portal unconditionally changed to its signed-out view after every enrollment confirmation, including failed requests. This hid the retry controls. The fix changes that view only on confirmed enrollment success, retains setup after a rejected code, validates six-digit input, and accepts whitespace in pasted codes. Setup material now has a separate ephemeral result with a redacted string representation and a read-only setup-key field; it is cleared from UI state when the account is cleared. Setup must start before confirmation controls appear.

Expired/missing setup sessions have actionable messages. Unmapped Firebase errors expose only a bounded SDK error identifier, never the provider response, so a subsequent live failure can be diagnosed without passwords, codes or tokens. The original provider rejection remains unconfirmed pending the owner's retry.

Validation passed: Kotlin JS compilation, webpack, distribution assembly, all four `scripts/verify-mfa-enrollment.cjs` scenarios (invalid-code retry through success, expired session/restart, unknown provider error without response disclosure, disabled enrollment), all four sign-in error/MFA scenarios, and the pending-account access-boundary test. Browser tests use the real client SDK with intercepted synthetic responses; they do not enroll a live factor. The updated distribution is served on port 4173. The owner was asked to refresh, start fresh enrollment, replace the unfinished authenticator entry with the new key and report success or the specific error reference.
