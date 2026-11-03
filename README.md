# VolunteerOps administrator app

A coordination workspace with a React/Next.js web frontend and Kotlin/Compose native applications. The interface uses Oswald headings, Overlock body text, navy and blue, and readable light content surfaces. Both presentations consume shared Kotlin business rules.

Responsive navigation exposes missions, people, field reports, communications, resources, training, qualifications, analytics, security, support and account tools. Workspace membership and region scope are enforced by the backend.

The default web application is now [web/](web/README.md). Desktop browsers use persistent navigation, contextual review drawers and backend operations. The local practice workspace has been removed. See the [React migration inventory and verification](docs/NEXT_PORTAL_MIGRATION.md).

`webApp/`, the retired Compose browser application, has been removed. Keep `web/` for the admin portal, `business/` for shared Kotlin policies, and `shared/` plus `androidApp/` for the native app. The sibling `../web/` is the separate marketing website deployed as `resq-web`.

## Run and verify

Web: `npm ci --prefix web`, then `npm run dev --prefix web`. Open http://127.0.0.1:4173. Use `npm run business:sync --prefix web` after changing shared Kotlin rules.

Use JDK 21, Android SDK 36 and the checked-in Gradle wrapper:

```powershell
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :shared:compileCommonMainKotlinMetadata :shared:jvmTest
./gradlew :shared:compileKotlinJs :shared:compileKotlinWasmJs
```

The CI Kotlin checks run without the Android SDK:

```sh
./gradlew :shared:jsNodeTest :business:jvmTest -PwebOnly=true --no-daemon --max-workers=1
```

After changing Kotlin/JS dependencies or removing a Gradle module, regenerate the Kotlin lockfile with `./gradlew kotlinUpgradeYarnLock -PwebOnly=true`, rerun those checks, and commit `kotlin-js-store/yarn.lock` with the dependency changes. Keep CI's lockfile mismatch check enabled. This lockfile is separate from the Next.js portal's `web/package-lock.json`.

Open `iosApp` in Xcode on macOS for Apple builds. Android debug artifacts are under `androidApp/build/outputs/apk/debug`. See [verification](docs/VERIFICATION.md) for completed checks and limits.

Shared tests cover identity, onboarding, API authorization failures and export handling. Backend tests cover lifecycle transitions, dispatch pause policy, bulletin validation and scoped operations. Operational data is loaded from the backend; there is no seeded fallback after a network error.

## Installing the redesign on Android

Current Android source declares **1.3.0 (version code 4)**. Earlier installation records describe older debug builds and are historical evidence. Verify the installed package against the actual build before claiming a device update; retain the existing signing identity and local data.

```powershell
./tools/Run-Android.ps1
# If multiple devices are connected:
./tools/Run-Android.ps1 -DeviceSerial YOUR_DEVICE_SERIAL
# To explicitly bypass build-cache reuse:
./tools/Run-Android.ps1 -Rebuild
```

The script builds this project, installs its exact debug APK, verifies the installed version, and restarts the app. It does not uninstall the app, erase device data, or delete global Gradle caches. Android Settings > Apps > App info also shows the version. Use the `androidApp` run configuration for this project in Android Studio.

## Production roadmap and Railway

The [seven-stage ResQ roadmap](../backend/docs/resq-production-implementation-roadmap.md) is the current cross-platform implementation guide. Follow [Railway integration](docs/RAILWAY_INTEGRATION.md) for this app and the [shared deployment runbook](../backend/docs/railway-production-deployment-runbook.md) for infrastructure. The required coordinator hostname is `https://www.admin.resq-app.co.ke`.

The workspace uses Firebase identity, the scoped operations API, Sentinel evidence map and the existing subscription controller. Regional bulletins and dispatch controls require the matching backend release. See the screen/action map for precise capabilities and boundaries. This source change does not deploy Railway services.
