# Interface refinements — 1.2.0

Implemented in the original admin project on 6 September 2026. Android versionCode 3; launcher label Admin. iOS display name updated in Info.plist.

## Changes

System-bar backgrounds now belong to the top app bar and bottom navigation surfaces. Touch targets remain clear of system controls. Headers have a bottom shadow. Page eyebrow labels are removed by shared page-title components. Secondary page back buttons and title-row actions use accessible grey glass icon controls. Modal headers have a page-background tint, divider, pinned position and scroll-dependent shadow. Bottom-navigation artwork has bounded, icon-specific activation motion. Volunteer availability uses amber/red as requested; the profile search/add row uses 4:1 weights. Admin has its monochrome header logo, bell and settings actions; the bell opens existing local broadcast and security records. Assigned and unavailable admin statuses have corresponding amber/red treatments.

Design guidance: [DESIGN.md](../DESIGN.md). The reusable Canvas skill is installed at `C:/Users/admin/.codex/skills/compose-canvas-graphics/SKILL.md`; its validator passed.

## Verification

- Both apps: Android assemble, common metadata, desktop entrypoint, JVM shared tests, JS and Wasm compilation passed. 13 volunteer repository tests and 12 admin repository tests passed, plus 2 existing admin template tests.
- Final Android APK built from the original IDE project. Installed in emulator-5554 using install -r. Installed version 1.2.0 / 3 and APK SHA-256 verified against this project.
- APK SHA-256: `d0547974d0809d7b535dac1b23f911916aa47a2d8389d8b945f92a0edb1f523d`.
- Phone-sized Android emulator: 1080×2092 at density 420. Captured the app after dismissing a System UI startup ANR; that host/emulator startup issue is not counted as an application validation pass. Screens below show the recovered app.
- Screens inspected for system-area continuity, readable title/action rows, fixed modal headers and scroll shadows. Volunteer skill row was inspected with both controls visible.
- iOS build and rendering were not verified on Windows. Physical Pixel Fold was disconnected; this release was not installed on it. Existing phone data was not cleared.
- Navigation motion is implemented and compiled; no frame-time performance or reduced-motion device profiling claim is made.

## Screenshots

- [admin-emulator-overview](refinements-1.2.0/admin-emulator-overview.png)
- [admin-emulator-people](refinements-1.2.0/admin-emulator-people.png)
- [admin-notifications-refined](refinements-1.2.0/admin-notifications-refined.png)
- [admin-modal-refined](refinements-1.2.0/admin-modal-refined.png)
- [admin-modal-scrolled](refinements-1.2.0/admin-modal-scrolled.png)

## Run on the phone

Reconnect the device, then use the project's `tools/Run-Android.ps1` or run androidApp from Android Studio. Expect version **1.2.0**, versionCode **3**. Use the original project folder; the APK is `androidApp/build/outputs/apk/debug/androidApp-debug.apk`. No cache deletion or data reset is needed.
