# Production release signing

## Upload key record

The local Android upload key for this project was created on 16 November 2026.

| Field | Value |
| --- | --- |
| Keystore file | `app/rotation-control-upload.jks` (local, ignored by Git) |
| Properties file | `keystore.properties` (project root, local, ignored by Git) |
| Key alias | `rotation-control-upload` |
| Key type | RSA 3072-bit, JKS |
| Certificate subject | `CN=Rotation Control Upload, OU=Android, O=Rotation Control, L=Jakarta, ST=DKI Jakarta, C=ID` |
| Certificate validity | 16 November 2026 – 3 April 2054 |
| Certificate serial | `01A1EBF92CA73506` |
| SHA-1 | `9F:B6:CC:2D:7D:19:B9:A5:20:59:4A:25:E7:E6:7C:9E:0C:DC:05:01` |
| SHA-256 | `CB:9B:65:14:EB:B5:8C:68:CC:3A:79:77:93:76:2A:14:44:A3:01:CD:9D:23:3B:59:BF:A9:59:78:E5:FE:DD:DC` |

The fingerprints identify the public certificate and can be shared with release tooling. Keystore passwords and private key material are intentionally not written here or committed. The local `keystore.properties` file contains the generated passwords and has owner-only file permissions.

## Build a signed release

The release targets Android 16 (API 36), as required for new Google Play submissions since August 31, 2026. It uses Android Gradle Plugin 8.13.2, Gradle 8.13, Google Mobile Ads SDK 25.5.0, and UMP 4.0.0. On Android 16 large displays (smallest width at least 600 dp), the platform ignores per-app orientation restrictions by default; see the [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16).

The project reads signing values from the ignored `keystore.properties` file or from `ROTATION_RELEASE_STORE_FILE`, `ROTATION_RELEASE_STORE_PASSWORD`, `ROTATION_RELEASE_KEY_ALIAS`, and `ROTATION_RELEASE_KEY_PASSWORD` environment variables. With the local files present, run:

```sh
./gradlew :app:bundleRelease
./gradlew :app:assembleRelease
```

The Play upload bundle is written to `app/build/outputs/bundle/release/app-release.aab`; the APK is written to `app/build/outputs/apk/release/app-release.apk`. AAB/APK outputs are build artifacts and are not committed.

## Google Play App Signing and backup

Enroll the app in Google Play App Signing. This private key is the **upload key**, used to authenticate uploads; Google Play generates and protects a separate app-signing key for APKs delivered to users. Store an encrypted backup of both `app/rotation-control-upload.jks` and `keystore.properties` in a private password manager or secure vault. Do not put them in this public source repository, a public release, screenshots, or email. Anyone who obtains the keystore and its passwords can sign uploads as this app.

If the upload key is lost or exposed, request an upload-key reset through Play Console. Do not replace it locally after the first Play upload without coordinating that reset, or future bundle uploads will fail certificate verification.

## Before first production upload

- Confirm the application ID `com.rotationcontrol.force`, package name, version code, and Play listing.
- Enroll in Play App Signing and record the Play-managed app-signing certificate separately from the upload certificate above.
- Upload the signed AAB, complete the restricted-permission declarations, and review the Play Console app signing certificate after processing.
- Keep the upload key fingerprint on file to verify local bundle signatures and any Play Console upload-certificate record.
