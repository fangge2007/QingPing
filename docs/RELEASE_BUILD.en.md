# Release builds, signing and migration

[简体中文](RELEASE_BUILD.md)

Starting with 0.4.2, the main distributed APK uses the `release` build type, `debuggable=false`, and a dedicated RSA 3072 signing key. The two optional fixture APKs remain debug builds.

## Migration from the 0.4.1 debug APK

The old debug certificate and the new release certificate are different. Android rejects a direct update between these identities.

1. Export exact rules as JSON and, if needed, statistics as CSV. Record selected apps, gesture switches and jump targets separately.
2. Save exports outside the app directory. CSV is for viewing and cannot restore statistics; JSON contains exact rules rather than a complete settings backup.
3. Uninstall the debug APK and install `qingping-0.4.2-release.apk`. Uninstalling deletes local settings and statistics.
4. Consent again, enable accessibility, select apps, preview and import rules, and verify behavior per app.

Future releases must retain the same release key and increase versionCode to support signed updates. A lost key cannot be replaced while retaining the same installation identity.

## Build

Use Java 17, Android SDK 36, Build Tools 35.0.0 and the Gradle Wrapper. Project code is MIT licensed; third-party materials retain their own notices.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:lintDebug
```

Set these environment variables for release signing:

| Variable | Purpose |
| --- | --- |
| `QINGPING_KEYSTORE` | Absolute PKCS12/JKS path |
| `QINGPING_STORE_PASSWORD` | Keystore password |
| `QINGPING_KEY_ALIAS` | Alias; defaults to `qingping` |
| `QINGPING_KEY_PASSWORD` | Private key password |

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Windows users can run `scripts/build-release.ps1`. Missing credentials fail release signing; there is no debug-key fallback. Never commit signing credentials or upload them as CI artifacts. Maintainer credentials reside in the excluded local `.signing/` directory; back up the complete directory privately to controlled offline storage. Independent builders use their own key and create a different installation identity.

```sh
apksigner verify --verbose --print-certs qingping-0.4.2-release.apk
aapt dump badging qingping-0.4.2-release.apk
```

The release certificate fingerprint and final results are recorded in [VERIFICATION.md, Chinese](VERIFICATION.md). A device-side `run-as` check is still needed; manifest inspection is not a physical-device acceptance test.

## Automation limits

GitHub Actions builds the debug app and fixtures, runs logic and Robolectric service tests, runs Lint, preserves JaCoCo reports, and checks that the release manifest is not debuggable. CI has no release private key.

Robolectric simulates Android APIs and system call boundaries on a host. It cannot certify actual accessibility authorization, OEM process management, real ads, physical gesture hits or power consumption. Follow the [device test plan, Chinese](DEVICE_TEST_PLAN.md) for those checks.
