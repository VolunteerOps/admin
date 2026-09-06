# Verification record

## Automated checks

Both client projects passed the following with JDK 17 and Gradle 9.1.0, using locally cached dependencies:

```powershell
./gradlew :androidApp:assembleDebug :desktopApp:compileKotlin :shared:compileCommonMainKotlinMetadata :shared:jvmTest :shared:compileKotlinJs :shared:compileKotlinWasmJs
```

There are 13 volunteer repository tests and 12 administrator repository tests, all passing. Two existing admin template tests also pass. The repository tests exercise state transitions, invalid operations, persistence, corruption and storage failures; they are not screenshot tests.

The public website passes TypeScript checking and `npm run build`. A local headless Chromium walkthrough verified `/`, `/features`, `/solutions`, `/api`, `/integrations`, `/about`, `/press`, `/help`, `/docs`, `/terms` and `/privacy`: all returned 200, with the correct page title and no horizontal page overflow at 360 px. The download dialog opened at 328 px, switched roles, accurately displayed unreleased platforms, and closed with Escape. Visual inspection caught and corrected the supplied MobileIcon sheet's unusual 297 px source height.

## Android visual checks

The emulator was exercised at 360 dp width, 130% text size, and approximately 1067 dp tablet width. Checks covered safe-area navigation, dashboard metrics, volunteer profile and editing, scrollable assignment/check-in details, administrator people/creation, tablet rails and the response diagram. A real soft keyboard was shown in the volunteer profile editor; the form reduced to the remaining viewport and remained scrollable. Visual testing corrected inherited text colors, dialog scroll-position retention and admin system-bar contrast. Final physical-device installation evidence is recorded in `DEVICE_VERIFICATION.md`.

Selected captures are in `screenshots`. Earlier emulator captures predate the final blue selected-container and explicit admin system-bar styling. The splash proof is a browser rendering of the same generated vector data, not an Android frame or a claim of pixel-identical raster reproduction.

## Limits

iOS source is present but has not been compiled or executed: that requires macOS/Xcode. JVM, JS and Wasm compilation does not establish runtime parity on every host/browser. Location permission denial, provider timeout and manual fallback are implemented; physical GPS accuracy needs device testing. No production backend connection, remote delivery, live map, accredited qualification or signed store release was verified. See `MIGRATION.md` for the complete service boundary.
