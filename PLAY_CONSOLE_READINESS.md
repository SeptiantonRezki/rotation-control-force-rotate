# Play Console and release checklist

## Android API and build

This new Google Play submission targets Android 16 (API 36), as required for new apps and updates submitted since 31 August 2026. The app uses Android Gradle Plugin 8.13.2 and Gradle 8.13. See [Google Play's target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878).

The product is global-only: the manifest no longer requests `QUERY_ALL_PACKAGES` and no longer declares an AccessibilityService. Control offers nine global rotation modes. The former per-app picker, saved per-app profiles, installer detection, and accessibility permission flow have been removed.

## Signing

The release build is signed when all four values are supplied by either `keystore.properties` or the environment. `keystore.properties` takes precedence.

1. Copy `keystore.properties.example` to `keystore.properties` in this project root.
2. Put the upload key file at the path in `storeFile` (relative to the `app` module), then fill in its password and alias.
3. Keep the properties file and key file outside version control and back up the upload key securely.
4. Run `./gradlew :app:bundleRelease` for a Play upload bundle. Without credentials, Gradle warns and produces an unsigned release artifact.

Alternatively, set `ROTATION_RELEASE_STORE_FILE`, `ROTATION_RELEASE_STORE_PASSWORD`, `ROTATION_RELEASE_KEY_ALIAS`, and `ROTATION_RELEASE_KEY_PASSWORD` in the build environment.

An RSA 3072-bit upload key is generated locally. Public certificate fingerprints and backup instructions are in [docs/release-signing.md](docs/release-signing.md). Private key material and passwords are ignored by Git. Enroll in Play App Signing so Google manages a separate app-signing key.

## Permissions, declarations, and consent

- **Global rotation access:** `WRITE_SETTINGS` is used to update Android's `ACCELEROMETER_ROTATION` and `USER_ROTATION` settings after the user grants the system's Modify system settings access.
- **Phone state:** Call bypass is optional. Its in-app disclosure precedes the `READ_PHONE_STATE` request. The service observes call state only and does not use the phone number or call history.
- **`specialUse` foreground service:** The manifest declares a user-requested persistent rotation-control subtype and the API 34+ permission. Complete the Foreground Service declaration in Play Console App content with the service purpose, user impact if interrupted/deferred, and a demonstration video. Google reviews the use case; manifest declaration does not guarantee approval. See [Play's FGS requirements](https://support.google.com/googleplay/android-developer/answer/13392821?hl=en-en).
- **Ads and consent:** The release uses Google Mobile Ads SDK 25.5.0 and UMP 4.0.0. UMP refreshes consent information on launch. Banner and native requests wait until UMP permits ad requests; initialization is guarded against duplicate completion. Privacy options appears when UMP requires it. See [UMP setup](https://developers.google.com/admob/android/privacy).
- **Privacy policy:** The static `docs/` site is deployed at GitHub Pages. Use the public `privacy-policy.html` URL in Play Console and keep the in-app Privacy Policy button pointed to it.
- **Play forms:** Complete Data Safety using the exact Mobile Ads/UMP disclosures, plus content rating, target audience, app access, ads, app category, and any current Play Console app-content declarations before submitting a release.
- **Notification permission:** Android 13+ requests notification permission at runtime so users can see the foreground-service notification.

## Release verification

Release minification and resource shrinking remain enabled. R8 full mode is the AGP 8+ default; manifest components are retained in `app/proguard-rules.pro`. AdMob and UMP consumer rules remain active. Build and inspect the final signed AAB/APK after application changes, verify the upload certificate, and check native 16 KB page-size compatibility.
