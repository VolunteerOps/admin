# Android source and installation diagnosis

The original `admin` project still contained the earlier implementation while the redesign was being validated in a separate staging directory. Running the original project therefore correctly built the old UI; clearing IDE or device caches could not fix that source mismatch. Both installed apps were version 1.0 when inspected on the connected Pixel Fold on 2026-09-06.

The reviewed redesign is now delivered to this project, with Android version 1.1.0 / code 2 and the existing `com.example.admin` application ID. The application label is role-specific. Rebuild and run this project's `androidApp`, or use `tools/Run-Android.ps1` to ensure the built APK and installed package match. The script stops immediately on a build or installation failure rather than opening an older build.

A signing-certificate mismatch must be resolved with the appropriate signing key; the script deliberately does not uninstall or erase data as a workaround. Store-signed releases are outside this debug-build workflow.
