# QingPing 0.4.2: licensing, action scope and release builds

[简体中文](RELEASE_0.4.2.md)

- Adds an MIT LICENSE and includes it in the APK while retaining upstream notices.
- Shares one critical-package policy across UI, rule imports, stored settings and runtime actions. Legacy selections of phone and emergency packages are rejected.
- Defaults accessibility events to QingPing itself; updates the system event filter to authorized sources and explicit jump targets. Pausing or revoking consent narrows the filter.
- Vetoes destructive and telephony labels and resource names, including delete, clear data, dial, transfer and uninstall. Gesture and back boundaries recheck authorization and screen state.
- Adds Android API 28/35 Robolectric service tests, JaCoCo reporting and GitHub Actions.
- Distributes a non-debuggable release APK signed with a dedicated release key. **The old 0.4.1 debug APK cannot be updated directly**; read the [migration guide](RELEASE_BUILD.en.md).

The built-in app catalog is unchanged. Diagnostic candidates can still prefill an exact rule for review. No increased real-ad coverage is claimed. Robolectric does not replace device or emulator UI acceptance; real-world coverage, power consumption and accidental-action rates remain unmeasured. Results are recorded in [VERIFICATION.md, Chinese](VERIFICATION.md).
