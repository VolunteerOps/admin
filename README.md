# VolunteerOps administrator app

A Kotlin and Compose Multiplatform coordination workspace for Android, iOS, desktop, JavaScript and Wasm. The interface uses Oswald headings, Overlock body text, supplied vector icons, a navy command header and readable light content surfaces.

Phone navigation provides Overview, People, Missions, Messages and More. Wide screens use a rail. Incident, roster, mission, inventory, skill, communications, map, analytics, security, settings and help journeys have focused drilldowns and validated local mutations.

This build opens an explicitly labeled **practice workspace**. Actions persist on this device; they do not contact responders or an organisation. Live login, remote delivery, real geographic tracking and backend security enforcement are not connected. See [coverage and integration boundaries](docs/MIGRATION.md).

## Run and verify

Use JDK 17, Android SDK 36 and the checked-in Gradle wrapper:

```powershell
./gradlew :androidApp:assembleDebug
./gradlew :desktopApp:run
./gradlew :shared:compileCommonMainKotlinMetadata :shared:jvmTest
./gradlew :shared:compileKotlinJs :shared:compileKotlinWasmJs
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Open `iosApp` in Xcode on macOS for Apple builds. Android debug artifacts are under `androidApp/build/outputs/apk/debug`. See [verification](docs/VERIFICATION.md) for completed checks and limits.

Repository tests cover capacity, duplicate assignment, mission completion/reconciliation, lifecycle transitions, resource allocation, pause/release behavior, skill renaming, frozen broadcast recipients, Unicode persistence, corrupted snapshots and audit identity/storage errors. State stores no login credentials; Android backup is disabled. Local workspace data is not an operational system of record.

## Installing the redesign on Android

The redesigned Android build is **1.1.0 (version code 2)**. Its application ID remains unchanged, so a normal update replaces the previous app while retaining local data.

```powershell
./tools/Run-Android.ps1
# If multiple devices are connected:
./tools/Run-Android.ps1 -DeviceSerial YOUR_DEVICE_SERIAL
# To explicitly bypass build-cache reuse:
./tools/Run-Android.ps1 -Rebuild
```

The script builds this project, installs its exact debug APK, verifies the installed version, and restarts the app. It does not uninstall the app, erase device data, or delete global Gradle caches. Android Settings > Apps > App info also shows the version. Use the `androidApp` run configuration for this project in Android Studio.
