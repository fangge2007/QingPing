# QingPing 0.4.1 — offline help and troubleshooting

[简体中文](RELEASE_0.4.1.md) | **English**

This release adds a searchable offline guide to the Android app. Its 17 expandable topics cover setup, small-scale validation, updates, app selection, profiles and windows, visible buttons that do not respond, countdowns, late and consecutive ads, videos, motion-triggered redirects, background recovery, diagnostics, exact rules, imports, statistics, accidental actions and privacy.

Open the guide from the consent screen, protection home, app list or settings. It displays consent, actual service connection, protection status and the number of selected apps, with shortcuts to existing settings. Reading it does not grant access, enable protection or change rules.

The APK interface and in-app tutorial remain in Chinese. Repository introductions, phone setup guides and reference lists are available as separate Chinese and English documents.

## Installation and upgrade

- Version: 0.4.1; versionCode: 5; Android 8.0 / API 26 or newer with a compatible Android accessibility environment.
- Use the existing signing identity for an in-place upgrade. Do not uninstall or clear data first if you want to retain settings, rules and statistics.
- Consent version remains 4. Users who completed the current disclosure in 0.4.0 do not need to consent again for this tutorial update.
- Detection, action budgets, outcome confirmation, permissions and data storage behavior are unchanged from 0.4.0.

## Files and verification

`qingping-0.4.1-debug.apk` is the debug build for local installation. `qingping-0.4.1-source.zip` contains the publication source and bilingual repository documentation. The two fixture APKs are optional local test apps; they do not load commercial ad SDKs. Verify downloads against `SHA256SUMS.txt`.

The app build, 74 existing unit tests, Lint and APK signature checks passed. Main app Lint reported no issues; the fixture retained 11 existing warnings. The signing certificate matches 0.4.0. The APK SHA-256 is `7dda9655896084305df790ddb975afd00415c44a10ad2cf5104e50468d292858`.

No physical-device or emulator UI acceptance test was performed for this release. The guides do not mean every phone or app has been tested. QingPing cannot force unavailable skip buttons, block every redirect before launch, or universally disable another app's sensors.

See the [English phone guide](PHONE_SETUP_GUIDE.en.md), [English references](REFERENCES.en.md) and [detailed verification record (Chinese)](VERIFICATION.md).
