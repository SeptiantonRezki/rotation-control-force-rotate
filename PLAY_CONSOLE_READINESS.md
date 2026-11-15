# Play Console and release checklist

## Signing

The release build is signed only when all four values are supplied by either `keystore.properties` or the environment. `keystore.properties` takes precedence.

1. Copy `keystore.properties.example` to `keystore.properties` in this project root.
2. Put the upload key file at the path in `storeFile` (relative to the `app` module), then fill in its password and alias.
3. Keep the properties file and key file outside version control and back up the upload key securely.
4. Run `./gradlew :app:bundleRelease` for a Play upload bundle. Without credentials, Gradle warns and produces an unsigned release artifact.

Alternatively, set `ROTATION_RELEASE_STORE_FILE`, `ROTATION_RELEASE_STORE_PASSWORD`, `ROTATION_RELEASE_KEY_ALIAS`, and `ROTATION_RELEASE_KEY_PASSWORD` in the build environment.

An RSA 3072-bit upload key has now been generated locally, and the signed bundle build has completed. Its public certificate fingerprints and backup procedure are in [docs/release-signing.md](docs/release-signing.md). Private key material and passwords are intentionally ignored by Git. This is the Play upload key; enroll in Play App Signing so Google manages a separate app-signing key.

## Restricted permissions and declarations

- **`QUERY_ALL_PACKAGES`:** The in-app disclosure now appears before package discovery. The picker filters the discovered packages to apps with a launcher activity and performs discovery asynchronously. A disclosure does not guarantee permission approval. Declare this restricted permission in Play Console and justify why broad visibility is essential to the core Per-App Rotation feature and why narrower package queries are insufficient. If Google Play does not approve the declaration, remove the permission and redesign the picker around user-selected apps or a narrower allowlist. See [Google Play's package visibility policy](https://support.google.com/googleplay/android-developer/answer/10158779).
- **AccessibilityService:** The user must opt in from Android Accessibility settings after the in-app disclosure. Complete the Accessibility API declaration, explain foreground package detection in the Play listing, and do not mark this app as an accessibility tool: its core purpose is not direct disability support. See [Google Play's Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/16558241).
- **Phone state:** The optional call bypass explains the feature before requesting `READ_PHONE_STATE`. It observes only `IDLE`, `RINGING`, and `OFFHOOK` states; it does not use the phone number or call history. Android requires this permission to register the call-state listener; users can leave this optional feature disabled.
- **`specialUse` foreground service:** The manifest declares the required service subtype and API 34+ permission. Submit the service type and use case in Play Console App content and provide any requested demonstration. Approval is reviewed by Google Play; the manifest alone does not grant approval. See [Android's foreground service type requirements](https://developer.android.com/about/versions/14/changes/fgs-types-required).
- **Ads and consent:** UMP consent information is refreshed on launch. Banner and native requests are held until UMP says ads may be requested; initialization and the ready callback are guarded against duplicate completion. A Privacy options button appears when UMP reports it is required. See [UMP's Android setup guidance](https://developers.google.com/admob/android/privacy).
- **Privacy policy:** Publish the static `docs/` directory through GitHub Pages, then add the resulting public `privacy-policy.html` URL to Play Console and keep the in-app Privacy Policy button pointed at that URL.
- **Notification permission:** Android 13+ requests notification permission at runtime. The service still needs the system's foreground-service notification behavior on all supported devices.

## R8 and Android framework entry points

Release minification and resource shrinking remain enabled. R8 full mode is the AGP 8+ default; manifest components are also retained in `app/proguard-rules.pro`. AdMob and UMP AAR consumer rules remain active. Build and inspect a release artifact after changing SDKs or framework component declarations.
