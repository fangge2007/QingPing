# Physical-device acceptance record template

[简体中文](DEVICE_ACCEPTANCE_TEMPLATE.md) · [Scenario procedures, Chinese](DEVICE_TEST_PLAN.md)

This is a blank template, **not a passing device record**. Copy it into a separate record. Distinguish not run, blocked, failed and passed. A result applies only to the device, OS and app versions recorded here.

## Build and environment

| Field | Observed value |
| --- | --- |
| Record ID, date, timezone, tester alias | To fill |
| QingPing version, versionCode, source commit | To fill |
| Installed APK SHA-256 and certificate SHA-256 | To fill |
| Build type: release or debug | To fill |
| Anonymous device ID, brand and model | To fill; omit IMEI and serial number |
| Android API, OEM/OS version and security patch | To fill |
| Target package/version; cold or warm launch | To fill |
| Resolution, display/font scale, language, orientation | To fill |
| Main profile, clone, work profile or container | To fill |
| Accessibility connection, consent, protection, selected source | To fill |
| Exact rules, persistence, gestures and jump target | To fill; attach a redacted rule export and checksum |
| Background, battery optimization, startup and notification settings | To fill |

Authorize accessibility manually in system settings. Check signing identity before changing build types; uninstalling an incompatible build loses local data. Read the [migration guide](RELEASE_BUILD.en.md). Use non-destructive fixture buttons for dangerous-label tests; do not place real calls, transfer money, delete data or make payments.

## Scenario results

Record actual steps, repetitions, expected/observed behavior, counters before/after and evidence files. All rows start as not run. Explain every not-applicable result.

| ID | Scenario and required behavior | Result | Repetitions, counters, evidence |
| --- | --- | --- | --- |
| A01 | No consent/selected source means zero automatic actions | Not run | To fill |
| A02 | Source/target edits, pausing and revocation narrow event packages; an empty selection never means all packages | Not run | To fill |
| A03 | An accessible, available explicit ad-skip button closes the ad and preserves ordinary content | Not run | To fill |
| A04 | Ordinary Close, tutorial Skip and business controls are untouched | Not run | To fill |
| A05 | Delete, clear, dial, transfer and uninstall fixture labels/descriptions/ancestors are rejected | Not run | To fill |
| A06 | Phone, emergency, permission and keyboard packages cannot be enabled through UI, imports or legacy settings | Not run | To fill |
| A07 | Disabled/countdown/stale controls are not clicked or counted as success | Not run | To fill |
| A08 | Gestures require app/rule authorization; cancellation is not success | Not run | To fill |
| A09 | Revocation, lock, app switch or destruction during a gesture prevents stale callbacks from reviving actions/counts | Not run | To fill |
| A10 | Lock screen, business overlays, inputs and oversized/off-screen controls are protected | Not run | To fill |
| A11 | Only the configured recent ad jump returns; ordinary jumps, missing targets and temporary allowances remain intact | Not run | To fill |
| A12 | Accepted requests without page changes, or incomplete reads, are not counted as dismissal | Not run | To fill |
| A13 | Consecutive/new ads and retries stay bounded without click/back loops | Not run | To fill |
| A14 | Rotation, split screen and scale changes revalidate window and geometry | Not run | To fill |
| A15 | Reboot, recent-task removal and OEM cleanup show accurate connection status and documented recovery | Not run | To fill |
| A16 | A real device rejects `run-as cn.qingping.app` for the release APK; record actual output | Not run | To fill |

Any serious business misclick, critical-package action, continued action after revocation or back loop fails the configuration. Pause use and retain a minimal reproduction. Other passing cases do not cancel such a failure.

## Real-app effectiveness and power

- Total observed ads, ads without available dismissal, visible-but-inaccessible controls, accessible available controls, manually confirmed dismissals and failures: to fill.
- Report both all-observed-ad dismissal share and accessible-available-control handling share with explicit denominators. App counters or system callbacks do not replace page confirmation.
- Ordinary business observations and unintended actions: to fill. Include failures and unsupported cases. Zero events in a small sample is not a zero-misclick guarantee.
- Raw timings from control availability to stable ordinary content, sample count and P50/P95: to fill. Separate countdown waiting and manual intervention.
- Power comparison: use the same device, brightness, network, temperature and action sequence with protection off/on. Record windows, starting/ending charge, app power data, repetitions, background settings and confounders. Battery percentages are coarse observations; mark missing measurements as not measured.

## Evidence and conclusion

- Evidence index, redacted screenshots/recordings and minimal logs: to fill. Remove accounts, notifications, inputs, other-app content and device identifiers. Do not publish full node trees.
- Failed cases, reproductions and workarounds: to fill.
- Reasons for not-run, blocked and not-applicable cases: to fill.
- Conclusion: **Not accepted**. After execution choose scoped pass, fail, blocked or partial, with explicit scope and remaining work.
