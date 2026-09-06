# Pixel Fold installation verification

Verified on 2026-09-06 using the connected Pixel Fold. Both original projects were rebuilt with `:androidApp:assembleDebug --rerun-tasks --no-build-cache`, so no prior task output or build-cache artifact was reused. The website was also rebuilt successfully in its original folder.

| App | Installed version | APK SHA-256 | Installed APK matches local build |
| --- | --- | --- | --- |
| Volunteer | 1.1.0 (2) | `c87955f17abf10354c577e0bd09c76451c0f67e5ec9a692fe7932234741fb616` | Yes |
| Admin | 1.1.0 (2) | `d0a8b5d9c6202e95162433c7e07b77e59b08e28404948452f593e3216695d2d8` | Yes |

The installed package files were hashed on the device and matched the APKs under each original project's `androidApp/build/outputs/apk/debug` directory. Installation used `adb install -r`; no uninstall or data clearing was performed. Both applications were restarted and the redesigned screens were inspected on the device.

The cause of the old UI was a source-location mismatch: the redesign was staged for verification while Android Studio still built the original source. The original source has now been replaced with the reviewed redesign, and Android version 1.1.0 / code 2 distinguishes it from the old 1.0 build. Use `tools/Run-Android.ps1` from either app to build, install and verify the matching APK in future.

The previously supplied emulator screenshots complement these physical-device captures. This installation verification does not establish backend connectivity; the apps remain explicit local demonstration workspaces as described in `MIGRATION.md`.
