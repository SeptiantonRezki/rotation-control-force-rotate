# Rotation Control: Force Rotate

A native Kotlin/XML Android app for device-wide screen rotation. It targets API 36 and supports API 26+. Open this directory in Android Studio or build with `./gradlew :app:assembleDebug`.

See [REQUIREMENTS.md](REQUIREMENTS.md) for the implementation-aligned product requirements. It supersedes technical assumptions in the original SRS that conflict with Android platform behavior or later product decisions.

## Release materials

- [Play Store listing copy and asset checklist](PLAY_STORE_LISTING.md)
- [Play Console readiness](PLAY_CONSOLE_READINESS.md)
- [Release signing record](docs/release-signing.md) — public certificate metadata only; signing passwords and the private upload key remain local and ignored by Git.
- [Privacy policy source](docs/privacy-policy.html); public URL: <https://septiantonrezki.github.io/rotation-control-force-rotate/privacy-policy.html>
- Play Store assets and emulator screenshots are under [`play-store-assets/`](play-store-assets/).

## First run

1. Grant **Modify system settings** so the app can change device rotation.
2. Allow notifications on Android 13+ so the foreground service can show its persistent status and shortcut.
3. Choose one of the nine global rotation modes on Control. The selected mode is saved and restored when the service runs again.
4. In Settings, optionally enable **Pause rotation during phone calls**. After the in-app disclosure, grant Phone state access; the feature reads call state only, not phone numbers or call history.

Rotation preferences and the latest ten choices are stored in device-protected SharedPreferences. When the service is enabled, the boot receiver attempts to restore the saved mode after startup.

## Current interface

The app uses a dark Ethereal Glassmorphism theme without an animated orb. A compact two-item bottom navigation switches between Control and Settings. Control presents the nine global rotation modes; Settings contains rotation access, auto-start, optional call bypass, privacy controls, and app information. One adaptive banner view is reused across pages, and each page has a native ad slot. Ads remain in page content rather than in a fixed area above the navigation bar.

The former per-app profile picker, installed-app visibility permission, and AccessibilityService have been removed. The app now applies one rotation mode globally.

## Platform limits

Android does not expose a `screenOrientation` property on `WindowManager.LayoutParams` overlays. This app uses the user-approved `WRITE_SETTINGS` capability to set `ACCELEROMETER_ROTATION` and `USER_ROTATION`; it does not create an invisible overlay. Android, individual apps, or an OEM can constrain the resulting orientation, so the app cannot guarantee overrides on every app, lock screen, notification shade, or device.

The Google Play ads SDK and its libraries mean the requested <3.5 MB package target cannot be guaranteed. Verify final release APK/AAB size and 16 KB alignment after a release build.

## Release signing

The upload keystore is deliberately excluded from version control. See `docs/release-signing.md` for its public certificate fingerprints and signed release build commands. Back up `app/rotation-control-upload.jks` and `keystore.properties` together in a private encrypted vault; do not commit either file.

Production AdMob identifiers are configured from the SRS. The app uses banner and native ads only, with UMP consent requested before ads load. It does not use interstitial or app-open ads.
