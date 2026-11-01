# Onboarding and account entry

> Current authority: the [seven-stage roadmap](../../backend/docs/resq-production-implementation-roadmap.md). This document preserves the client journey design; historical backend assumptions are superseded by the roadmap and current API contracts. Native account providers remain unconfigured until implemented.

## Journeys

Three swipeable illustrated introductions lead to clear account choices. Skip and existing-account login remain available. Introduction completion persists independently of sample workspace data; the story can be replayed from account options.

Volunteer: community welcome → assignment briefing → shared impact. Email/password signup and login, Google and Apple entry points, email verification, password recovery, then location → interests → availability/update preferences.

Admin: response overview → team briefing → handover continuity. Corporate email/password only, email verification, database membership approval, then organisation/team → requested role → region/update preferences. Selecting a role never grants permissions.

Setup previews are explicitly labelled, stored locally, and do not create accounts or alter sample records. Setup choices are optional. Real account access and the sample workspace are separate.

## Research and design decisions

Reviewed Meetup's [onboarding flow](https://mobbin.com/flows/e35f43d2-bbfe-46dc-9c8d-f34cace68d08), [login flow](https://mobbin.com/flows/bf7ee253-b35f-44f3-b13e-eae2378ef6db), and [interest selection](https://mobbin.com/flows/1eecab03-bfde-46ea-907a-1d1836ae2371) through Mobbin. Borrowed people-first storytelling, distinct authentication choices and short progressive personalisation. Artwork, copy and journeys are original to VolunteerOps. The search for recovery references returned a check-inbox screen, not a complete Meetup reset flow; recovery here follows Firebase's action-code model.

Use the existing DESIGN.md: Oswald headings, Overlock interface text, blue actions, restrained teal accents, pale canvas, rounded controls and glass back buttons. Portrait introductions place artwork in the upper portion; wide screens use side-by-side artwork and copy. Scrollable content and a persistent introduction footer accommodate short screens and larger text. Passwords are obscured with an explicit reveal control. Errors are announced and scrolled into view.

## Canvas artwork

Six original authoring images were converted offline into 72-colour compound vector paths per scene. Runtime assets are only `composeResources/files/onboarding_0.tsv` through `onboarding_2.tsv`; no onboarding bitmap is loaded at runtime. The compiler and original references, editable SVGs, vector previews and statistics are in `../../volunteerops-onboarding/`.

`OnboardingArtwork.kt` parses once off the composition dispatcher, publishes immutable paths, uses even-odd fills, preserves the 1000×800 reference aspect ratio and caches drawing geometry. A small contour stroke closes tracing seams. The portrait composition preserves complete figures rather than stretching them. The bounded arrival animation uses a graphics layer; reduced-motion mode displays the final state immediately. This is detailed quantised vector illustration, not a promise of pixel-identical raster texture or measured frame-rate performance.

## Firebase and backend integration boundary

Production target: Railway. Configure the verified public API and portal origins through [Railway integration](RAILWAY_INTEGRATION.md). Existing source defaults still need replacement during implementation; this document does not change them.

Firebase is not configured yet. `IdentityServices.firebase` and `.database` are injectable contracts with explicit unconfigured defaults. Current buttons do not create accounts, send reset messages or authenticate anyone. No passwords or tokens are persisted in onboarding preferences. Google/Apple buttons are volunteer-only provider entry points awaiting SDK configuration.

Before enabling live accounts:

1. Configure Firebase for the supported platforms and register Google/Apple providers for Volunteer only. Implement `FirebaseIdentityProvider` using the SDK, including token refresh, verification, reset actions and sign-out. Keep SDK secrets out of source control.
2. Implement server verification of Firebase ID tokens received over HTTPS. Resolve the verified UID to an active, approved database membership and organisation. Enforce role permissions on every operational endpoint. Client `canEnter` checks are display gates, never server authority.
3. Implement `DatabaseAccessGateway` against confirmed backend routes. The backend has since introduced Firebase verification/session and scoped access foundations. Implement against its current documented contracts rather than the historical JWT/password routes. Do not infer an endpoint contract or production readiness from the deployment origin alone.
4. Wire Firebase reset links/action codes into the app's recovery entry. The UI already supports pasting a reset link or entering an action code and provider-confirmed success. Configure and validate the production action-link domains before enabling this integration.
5. Connect authenticated repositories and an authenticated root destination. The current root remains the explicit sample workspace; successful identity must never be mapped to Sarah's sample records. The ready screen accurately identifies this remaining connection.
6. Publish the organisation's actual terms/privacy details and verify account deletion, recovery, expired/reused codes, revoked memberships and refresh failures end to end.

Reference: [Firebase ID-token verification](https://firebase.google.com/docs/auth/admin/verify-id-tokens), [Firebase Auth REST action contracts](https://firebase.google.com/docs/reference/rest/auth).

## Verification

`OnboardingContractTest` covers email/password validation, preview draft round trips and rejection of unapproved, inactive or non-administrative memberships. Build and visual-check results are recorded in `../../volunteerops-onboarding/VERIFICATION.md`. Production authentication cannot be end-to-end tested until the configuration and server integration above exist.

Sentinel can be opened from the entry screen or Sentinel navigation. See [Railway backend connection](RAILWAY_INTEGRATION.md) for deployment configuration and the distinction between the live portal and unfinished native account providers.
