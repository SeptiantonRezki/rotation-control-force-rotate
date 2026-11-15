# Rotation Control: Force Rotate

A native Kotlin/XML Android application targeting API 35 (min API 26). Open this directory in Android Studio or build with `./gradlew :app:assembleDebug`.

See [REQUIREMENTS.md](REQUIREMENTS.md) for the implementation-aligned requirements. It supersedes technical assumptions in the original SRS that conflict with Android platform behavior or the UI/UX decisions made during implementation.

## Release materials

- [Play Store listing copy and asset checklist](PLAY_STORE_LISTING.md)
- [Play Console readiness](PLAY_CONSOLE_READINESS.md)
- [Release signing record](docs/release-signing.md) — public certificate metadata only; signing passwords and the private upload key remain local and ignored by Git.
- [Privacy policy source](docs/privacy-policy.html); intended public URL: <https://septiantonrezki.github.io/rotation-control-force-rotate/privacy-policy.html> after GitHub Pages deployment.
- Play Store assets and emulator screenshots are under [`play-store-assets/`](play-store-assets/).

## First run

1. Grant **Modify system settings** when prompted. This permission is required to change the device rotation settings.
2. Allow notifications on Android 13+ so the foreground service can expose its persistent shortcut.
3. Optional: use **Configure Per-App Profiles** and enable the service after reading the prominent disclosure. The accessibility service observes foreground package changes only; it does not retrieve window content.
4. Optional: turn on **Pause rotation during phone calls** in Settings. After the in-app disclosure, grant Phone state access; the feature reads only call state and never uses the phone number or call history.

Rotation preferences and the latest ten choices are held in device-protected SharedPreferences. On supported devices the boot receiver restores the saved mode when the service was enabled.

## Current interface

The app uses a dark, branded glass-card theme with no animated orb. The bottom navigation is a compact 60 dp bar with all three labels visible. The Per-App page lists launchable apps only; package discovery and icon loading happen asynchronously, and rotation choices use a descriptive dropdown with an active-mode checkmark. One adaptive banner view is reused across pages, with a native ad slot on each page. Both ad types remain in page content rather than in a fixed area above the navigation bar.

## Platform limits

Android does not expose a `screenOrientation` property on `WindowManager.LayoutParams` overlays. This app uses the user-approved `WRITE_SETTINGS` capability to set `ACCELEROMETER_ROTATION` and `USER_ROTATION`; it does not create an invisible overlay. Android or an OEM can still constrain the resulting orientation, and apps/system surfaces with stronger policy may not follow it. Guard Mode currently follows the system sensor rotation policy. Rotation services cannot guarantee overrides on every app, lock screen, notification shade, or device.

The Google Play ads SDK and its native libraries mean the requested <3.5 MB package target cannot be guaranteed. Verify final release APK/AAB size and native 16 KB alignment after a release build. `QUERY_ALL_PACKAGES` is included for the installed-app profile picker; Google Play requires a valid core-functionality justification for that permission.

During a package installer activity, the optional Accessibility service temporarily selects Auto-Rotate by package name and resumes the app-specific or global mode on the next foreground-app event. Installer detection is based on known installer package IDs and a package-installer name heuristic; it cannot identify every OEM installer variant.

## Release signing

The generated local upload keystore is deliberately excluded from version control. See `docs/release-signing.md` for its public certificate fingerprints and the signed release build commands. Back up `app/rotation-control-upload.jks` and `keystore.properties` together in a private encrypted vault; do not commit either file.

Production AdMob identifiers are configured from the supplied SRS. No interstitial or app-open ad unit is used. UMP consent is requested before ads load.
