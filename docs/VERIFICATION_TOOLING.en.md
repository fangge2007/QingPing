# Reproducible verification

[简体中文](VERIFICATION_TOOLING.md)

Local shells and GitHub Actions share the root Gradle `verify` task, keeping the check graph in one place.

With Java 17 and Android SDK 36 configured, Linux/macOS, Git Bash and WSL users run:

```bash
bash scripts/verify.sh
# Optional when dependencies are already cached:
bash scripts/verify.sh --offline
```

Windows PowerShell:

```powershell
.\scripts\verify.ps1
```

Alternatively run `./gradlew verify`. Initial dependency downloads need network access. Scripts do not automatically accept SDK licenses. Configure `JAVA_HOME` and `ANDROID_HOME`; the PowerShell wrapper also recognizes the existing portable toolchain.

The task builds the debug app and fixtures, runs JUnit/Robolectric, checks service coverage thresholds, runs app/fixture Lint, and validates the non-debuggable release manifest. No maintainer signing key is needed or supplied.

## Generated evidence

- `app/build/reports/verification/evidence.json` records current totals, passes, failures, errors, skips, service executions, JaCoCo counters/percentages, release-manifest checks and the CI source commit.
- `app/build/reports/verification/summary.md` provides the corresponding readable summary.

CI runs the Bash entry point, publishes the summary and retains JSON, JUnit XML, HTML, Lint and JaCoCo reports in `android-check-reports`. Missing tests, service execution or coverage evidence and invalid release-manifest checks fail verification. Nonzero shell/Gradle exits fail CI.

Test counts are read from the current reports rather than fixed at 74 or 96. Passes and skips remain distinct; old release counts describe their tagged snapshot. The service-class 60% line/30% branch thresholds are regression floors, not completeness guarantees.

## Device evidence is separate

Host simulation and static manifests do not establish OEM behavior, physical gesture hits or power use. Machine reports keep `physicalDeviceAcceptanceEstablished` false. Use the [device record template](DEVICE_ACCEPTANCE_TEMPLATE.en.md); do not mark unexecuted cases as passed. Check APK signing and device-side `run-as` separately using the [release guide](RELEASE_BUILD.en.md).
