> **Legacy browser cleanup - 1 November 2026:** `webApp/` and its Compose build/deployment helpers have been removed. Commands below targeting that module are historical. Use `npm run dev --prefix web` or `bash scripts/build-web.sh` for the current admin portal. Shared/native Kotlin modules are retained.

> **Current deployment — 1 November 2026:** The default web app is now React/Next.js in `web/`, consuming shared Kotlin rules from `business/`. The repository Dockerfile builds Next.js; native Compose remains in `shared/`. Staging is https://resq-admin-staging.up.railway.app. See [NEXT_PORTAL_MIGRATION.md](NEXT_PORTAL_MIGRATION.md), [../web/README.md](../web/README.md) and [ADMIN_WEB_DNS.md](ADMIN_WEB_DNS.md). The October architecture notes below are historical, not the current deployment boundary.

# Railway deployment and backend integration: coordinator app

Updated 1 October 2026. Railway is the deployment target. This guide replaces the former hosting guide; creating this document did not deploy an app, change credentials, migrate data, or remove any cloud service.

Follow the [seven-stage ResQ roadmap](../../backend/docs/resq-production-implementation-roadmap.md) for feature implementation and the [Railway production runbook](../../backend/docs/railway-production-deployment-runbook.md) for infrastructure. This document covers this repository's build and integration details. The [data migration and recovery guide](../../backend/docs/railway-data-migration-and-recovery.md) governs any later source-database cutover.

## 1. Understand the current boundary

This is Kotlin/Compose Multiplatform, with Android, iOS, desktop, JS and Wasm targets. It is not a Next.js project. The operational screens still instantiate `AdminRepository`, a persistent local practice workspace. Firebase and membership interfaces in `IdentityGateway.kt` have unconfigured defaults. There is no admin RevenueCat integration yet.

Web Sentinel embeds the backend `/sentinel` portal; native Sentinel opens it in the system browser. This separate surface does not complete native login, operational synchronization, billing, background sensing, or the full engine. Follow the roadmap before describing this as production operations software.

## 2. Prepare the deployable web artifact

Use the checked-in Gradle wrapper and a pinned JDK/toolchain in CI. The current wrapper can bootstrap a JDK, but a repeatable multi-stage container or CI image is preferable to relying on whatever Java the host happens to provide. The `-PwebOnly=true` option excludes Android/desktop modules and native compiler targets.

The following Bash commands show the existing production JS build; execute them in the admin repository after setting the two public URLs:

```bash
node scripts/configure-sentinel.mjs --check
bash ./gradlew :webApp:jsBrowserDistribution -PwebOnly=true --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8'
node scripts/configure-sentinel.mjs
node scripts/verify-web-bundle.mjs
```

The output is `webApp/build/dist/js/productionExecutable`. Retain the entire distribution, including JavaScript, WebAssembly and Compose resources. Record its source commit and checksums. Do not upload `.env`, credentials, source maps, Gradle caches, Android build directories or SDKs as public assets.

The existing `scripts/render-build.sh` contains this build flow, public-setting validation, JDK bootstrap and memory limits. Its historical filename and the hard-coded old-provider origins remain source cleanup tasks in the roadmap. No scripts were renamed by this documentation change. Do not reference a nonexistent `railway-build.sh` as if it were implemented.

## 3. Serve it as a Railway service

Create/select a dedicated coordinator service in the chosen Railway project and environment. The repository is `VolunteerOps/admin`. Choose one verified deployment path:

- Build with a pinned multistage Dockerfile, copy the distribution into a small static HTTP server image, and bind to `0.0.0.0:$PORT`. The Dockerfile/server configuration is implementation work, not an existing artifact in this repository.
- Build the artifact in CI, then use Railway's supported static hosting/Railpack configuration for the artifact. Verify the actual output-root and SPA settings for the selected builder. Setting a former provider's static publish directory alone is not a deployable service.

Preserve history-aware app navigation and direct links; return index HTML for actual client routes, but return a genuine missing-file error for unknown `.js`, `.wasm`, font and image assets. Serve WebAssembly with `application/wasm`. Cache content-hashed assets immutably and revalidate HTML/configuration. Add an explicit health route in the selected server, or initially probe `/` and separately verify the application bundle. A successful HTML health check alone does not prove the application starts.

Railway supports static hosting and requires a reachable listening service/target port. See [static hosting](https://docs.railway.com/guides/static-hosting) and [deployment health checks](https://docs.railway.com/deployments/healthchecks).

## 4. Set public configuration

| Setting | Required value or handling |
| --- | --- |
| `OPERATIONS_API_URL` | Verified public HTTPS API origin, proposed `https://api.resq-app.co.ke`; do not activate an unprovisioned hostname. |
| `SENTINEL_PORTAL_URL` | Same origin followed by `/sentinel`. The current script rejects a different origin or arbitrary path. |
| Browser Firebase configuration | Add through the planned platform adapter; public project configuration only. Never embed service-account credentials. |
| RevenueCat public SDK key | Add through the planned platform adapter; entitlement authority stays on the backend. Never expose a secret REST key. |

Current URL injection writes `operations-api-base-url` and `sentinel-base-url` metadata into the built HTML. Configuration changes therefore require artifact regeneration/redeployment. Production validation must reject a missing value instead of silently using the legacy source default. Native startup must separately configure `BackendConnection`; changing the web metadata does not update installed applications.

Never copy database URLs, database passwords, S3 secret keys, SMTP credentials, Gemini keys or Firebase administrator keys to any client. Browser requests require a public API URL and cannot use a `railway.internal` address.

## 5. Configure the requested domain exactly

The required coordinator URL is **`https://www.admin.resq-app.co.ke`**. `admin.resq-app.co.ke` is a different hostname; optionally register it as an alias with an explicit redirect to the canonical hostname.

1. First verify the service on its generated Railway domain.
2. Add `www.admin.resq-app.co.ke` to this service's custom domains and select the actual listening port.
3. Copy both the **CNAME and TXT ownership-verification records** supplied by Railway into the authoritative DNS zone. Use the exact dashboard values, not guessed targets or IP addresses.
4. Wait for verification and a valid certificate for the exact hostname. If using Cloudflare, start with DNS-only resolution for this nested hostname. An ordinary `*.resq-app.co.ke` certificate does not cover `www.admin.resq-app.co.ke`; proxying requires a certificate explicitly covering the nested name.
5. Add the exact HTTPS origin to backend CORS and portal `frame-ancestors` allowlists. Add Firebase authorized domains and tested action/redirect link configuration. Restrict Mapbox public tokens to the actual map-serving origins.
6. Verify login, logout, CSRF, token refresh, TOTP, invitation links and the embedded portal on Chrome, Firefox and Safari. The full-page portal link remains useful when browser cookie restrictions prevent embedded login. Do not widen CORS or cookie scope indiscriminately to fix an iframe problem.

Railway explains its domain verification, automatic certificate handling and nested Cloudflare hostname constraints in [Working with Domains](https://docs.railway.com/networking/domains/working-with-domains).

## 6. Implement real integration before launch

Use the central roadmap's identity, operations, monetization, Sentinel and agentic stages. In this repository replace the production root's sample repository with authenticated repositories; keep demo mode explicitly isolated. Add organization switching, pending invitations/admission, safe directory and profile-link interactions, real mission/people/messages/settings APIs, RevenueCat paywall/manage/restore flows, and backend-enforced entitlement states. The backend must authorize every organization/region/resource independently of client navigation, requested role or `view=admin`.

Retain real sources, timestamps, missing/stale-data states and uncertainty on Sentinel screens. A premium purchase grants a product capability only; it cannot grant coordinator authority, organization membership or hazard-warning approval.

## 7. Verify and record the release

```powershell
node --test scripts/public-config.test.mjs
./gradlew :shared:jvmTest
```

Then run the production JS build and artifact checks above. Verify actual HTTPS requests and authenticated workflows against staging, including expired sessions, membership removal, denied Pro access, purchase restoration, duplicate invites, tenant isolation, map failure, browser refresh/deep links, large text, keyboard navigation and screen-reader semantics. Confirm missing assets fail clearly rather than return application HTML.

Native builds require separate SDK configuration and signed releases. Current source declares Android version 1.3.0/code 4; historical verification documents describe earlier debug builds. iOS must be built and exercised on macOS/Xcode. Native portal launch is not evidence of native authentication or map parity.

Record the reviewed source commit, artifact digest, Railway project/environment/service and deployment identifiers, configured public origins, actual test results and rollback artifact. Keep the previous deployment recoverable until the production acceptance and rollback checks pass. Retiring old hosting resources is a separate controlled cutover operation.
