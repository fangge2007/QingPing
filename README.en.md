# QingPing 清屏

[简体中文](README.md) | **English**

QingPing is an Android tool that assists with dismissing ads without Root. When an app selected by the user is in the foreground and the screen is unlocked, QingPing identifies accessible ad-skip or dismiss controls exposed by the system, requests rule-based clicks, and observes the resulting page. It supports available dismiss controls in splash screens, in-app pop-ups, and video ads, as well as returning after a configured ad-related app launch.

**0.4.1 · Android 8.0 / API 26 and later · Native Java · Runs locally**

The APK's current interface and built-in tutorial are in Chinese. English is provided for the repository documentation; this is not an English-localized app build.

[Download releases](https://github.com/fangge2007/QingPing/releases) · [Phone setup guide](docs/PHONE_SETUP_GUIDE.en.md) · [References and licenses](docs/REFERENCES.en.md) · [Verification record, Chinese](docs/VERIFICATION.md)

## Features

| Feature | Actual behavior |
| --- | --- |
| Per-app management | Global protection and each source app have separate switches. No app is handled by default. |
| Assisted ad dismissal | Candidate controls are identified using explicit ad-related labels, full resource IDs, pages, geometry, and context. Countdown controls must become available before an action. |
| Delayed and consecutive ads | Foreground events and low-frequency observation continue to check later pages. Another ad can be handled after ordinary content has stably returned. |
| Exact rules | The source package, page, control ID, literal text, and accessibility description must match all configured conditions. Persistent matching can be enabled separately for each rule. |
| Optional simulated clicks | When an ordinary click fails or is accepted without dismissing the ad, a center gesture may be attempted for a verified control. This is off by default for each app; custom rules also require their own authorization. |
| Button diagnostics | Start a 30-second diagnostic session manually, review candidates, then create a rule. Candidates are held briefly in memory and expire after two minutes. |
| Return after an app launch | One explicit target package can be configured for each source. With recent ad evidence, QingPing attempts to return after the target opens. A temporary 10-minute allowance is available. |
| Rules and statistics | Local JSON import preview, merge, and export. Skips and returns are recorded separately by source and date and can be exported as CSV. |
| Offline tutorial | The app includes 17 searchable, expandable topics covering setup, troubleshooting, accidental actions, background behavior, and data management. |

## How it works

QingPing uses Android's public `AccessibilityService`. It does not inject code into advertising apps, modify their APKs, or alter network traffic.

1. **Limit the scope.** After accepting the purpose disclosure and manually granting accessibility access, the user must still enable protection and select source apps. Checks run only while the screen is unlocked and a selected app is in the foreground.
2. **Read accessible nodes.** The service responds to window and content events and requests non-important views that the system permits it to read. It first queries verified full IDs, then performs a bounded traversal: normally up to 700 nodes and 24 levels per scan. Input fields and their subtrees are excluded.
3. **Combine conditions.** General labels are checked with ad context, position, and size. Built-in and custom rules add source, page, and ID restrictions. All nonempty custom-rule fields must match literally and together; an ordinary business-related “Close” control is not automatically clicked.
4. **Bound the frequency.** On entry, checks normally run about every 250 milliseconds, with a default intensive window of 15 seconds. Later observation runs about every 1.5 seconds. Continued observation does not extend every rule: a generic “Skip” label retains its entry restriction, while exact rules default to 15 seconds, adjustable from 3 to 30 seconds, and can individually allow persistent matching.
5. **Revalidate before acting.** `ACTION_CLICK` is preferred. A node-center gesture is available only for an authorized, unambiguous, safe target. Escalating an accepted click after 2.5 seconds without confirmation also requires the same source, window, and bounds. Unsafe actions, unsafe clickable ancestors, and unavailable buttons prevent an action.
6. **Confirm before counting.** Submitting a request or receiving a gesture callback is not counted as success. Subsequent complete observations must confirm that the ad controls and ad state have disappeared, or that the source app has returned after a launch. Each ad episode permits at most four accepted actions and eight total requests. A stable clear state makes the next episode eligible.

Implementation entry points: [UI](app/src/main/java/cn/qingping/app/MainActivity.java), [accessibility adapter](app/src/main/java/cn/qingping/app/QingPingAccessibilityService.java), [decision engine](app/src/main/java/cn/qingping/app/engine/AdDecisionEngine.java), and [control policy](app/src/main/java/cn/qingping/app/engine/AdControlPolicy.java). Additional parameters are documented in the [implementation notes, Chinese](docs/IMPLEMENTATION_NOTES.md).

### Limits

- Visible does not necessarily mean accessible. If an image, canvas, or protected page provides no readable target, this version has no screenshot, OCR, or fixed-coordinate fallback.
- It does not accelerate videos, bypass countdowns, or create skip buttons that the app does not provide. Closing a rewarded ad early may forfeit its reward.
- Returning happens after the target app has opened. It cannot prevent the first launch or undo a download, submission, or payment.
- An ordinary APK cannot universally disable another app's motion sensors. Some systems offer an orientation permission; adjust it only if it is actually available on the device.
- The Android API range does not mean every model or app has been validated. Background cleanup, app updates, and window changes may affect results.

## References and provenance

The [English source register](docs/REFERENCES.en.md) lists the complete pinned commits, source locations, and license evidence.

| Upstream project | What was examined | Use in QingPing |
| --- | --- | --- |
| [ganlinte/GKD-subscription](https://github.com/ganlinte/GKD-subscription) | Source-isolated rules, SDK resource names, and page conditions | Selected verified rule information is adapted with QingPing's own restrictions. The MIT notice is retained in the APK and [third-party notices](THIRD_PARTY_NOTICES.txt). |
| [gkd-kit/gkd](https://github.com/gkd-kit/gkd) | Action fallback and short-term rescanning | Public behavior and APIs informed an independent implementation. The GPLv3 client executor and selector engine were not copied. |
| [zfdang/Android-Touch-Helper](https://github.com/zfdang/Android-Touch-Helper) | Split labels, click hierarchy, events, and recovery tests | Bounded short-label combination and safety checks were independently implemented; the client was not ported. |
| [madeye/ad-skipper](https://github.com/madeye/ad-skipper) | Local image-recognition architecture when nodes are insufficient | Research only. This version includes no screenshots, OCR, models, or dependencies from the project. |
| [zjyzip/AdClose](https://github.com/zjyzip/AdClose), [pwh-pwh/fuck_shake](https://github.com/pwh-pwh/fuck_shake) | Sensor hooks inside a target process | The limits of the Xposed approach were studied; no module was copied or integrated. |
| Other public projects | Basic dismissal, sensor, and rule approaches | Projects without a clear license were used only to compare public facts. Their code and rules were not copied; see the source register. |

Upstream licenses apply to their respective materials. A reference does not imply upstream endorsement or permission to reuse every project. This repository has not yet selected an overall open-source license for its own code.

## First-time setup for every brand

1. **Check the environment and install.** An Android 8.0+ environment that supports Android APKs and accessibility services is required. If QingPing is already installed, install the update with the same signature. Do not uninstall it or clear its data first.
2. **Read and accept the disclosure.** Read the data-purpose explanation on first launch. Users who already accepted disclosure version 4 in 0.4.0 do not need to accept it again for 0.4.1.
3. **Select source apps.** Enable apps individually under “应用” (Apps), then check the ad-skip option in “配置规则” (Configure rules). If an app has no launcher entry, verify its package name before adding it manually.
4. **Enable accessibility manually.** Open system settings from QingPing and enable QingPing's service. If sideloading triggers restricted settings, verify the source and follow the device's instructions. Do not disable system protection as a whole.
5. **Enable protection and check the connection.** Check three separate states: accessibility is enabled in the system, the service is actually connected, and global protection is on. Unlock the screen, then enter the target app again.
6. **Check background settings when needed.** If disconnections recur, inspect QingPing's battery, background, autostart, or sleeping-app restrictions. Notifications provide an optional boot reminder; they do not replace a connected service.
7. **Validate a few apps first.** Check both ads and ordinary app pages. Leave simulated clicks off by default. Consider the relevant authorization only when an ordinary click fails for a reliable candidate.

## Phone settings and precautions by brand

Complete the seven steps first, then check the relevant brand row. **Menus vary by system, region, and version. Search terms help locate a setting; they do not mean every phone has that exact menu.** Official references, detailed paths, and reboot checks are in the [full English guide](docs/PHONE_SETUP_GUIDE.en.md).

| Brand / system family | What to do on the device | Precautions |
| --- | --- | --- |
| Huawei / EMUI, HarmonyOS | Open accessibility from QingPing. Search for “App launch management” (“应用启动管理”) and “Battery optimization” (“电池优化”) to check QingPing's background restrictions. | First confirm an Android accessibility environment is available. This version has no native HAP. Being able to install an APK in a compatibility container does not establish access to apps outside that container or native HarmonyOS apps. Adjust orientation permission only when the device provides it. |
| HONOR / MagicOS, older EMUI | Enable accessibility. Search for “App launch management,” “Background activity,” and “Battery optimization.” | Do not assume Huawei's paths are identical. Recheck the actual service connection after updates. |
| Xiaomi / Redmi / POCO, HyperOS, MIUI | Enable accessibility. Search for “Background autostart” (“后台自启动”) and “Battery saver” / battery policy (“省电策略”). | Autostart and accessibility are separate settings. Do not grant unrelated permissions to keep an app running. |
| OPPO / OnePlus / realme | Enable accessibility. In app details or battery settings, search for “Allow background activity,” “Autostart,” and “Battery optimization.” | ColorOS, OxygenOS, realme UI, and regional versions differ. Locking an app in Recents does not guarantee permanent operation. |
| vivo / iQOO | Enable accessibility. Search for background power management (“后台耗电管理”) and “Autostart” (“自启动”). | OriginOS and Funtouch OS use different paths. Motion and orientation permissions are system permissions of the target app. |
| Samsung / One UI | Open system accessibility from QingPing and enable it in the local service list. Check the sleeping and deep sleeping app lists in Background usage limits. | If QingPing is restricted, remove the restriction as needed. Battery and notification settings are separate. |
| Google Pixel / AOSP | Open system accessibility from QingPing and enable it in the local service list. If it disconnects unexpectedly, check the app's background battery usage. | Adaptive Battery, restricted settings, and accessibility are different features. |
| Motorola / Nothing | Open accessibility through QingPing. Search app details for battery, background usage, or optimization settings. | Follow the local firmware's menus rather than assuming they exactly match Pixel. |
| Meizu / Flyme | Enable accessibility. Search for background management (“后台管理”), autostart (“自启动”), and battery optimization (“电池优化”). | If a switch is absent, use the general setup process rather than guessing hidden components. |
| Lenovo / ZUI / Legion | Enable accessibility. Search for app management, autostart, background activity, and battery optimization. | Tablets, gaming modes, and older ZUK firmware may differ. Validate landscape and floating-window modes separately. |
| ZTE / nubia / REDMAGIC | Enable accessibility. Search for autostart, background operation, and power saving. | Check the connection after switching gaming spaces, extreme power saving, or cleanup modes. |
| Sony / Xperia | Enable accessibility. If disconnections occur, check the app's battery settings and power-saving mode. | Refer to the device's own documentation for the effects of STAMINA. |
| ASUS / ROG Phone | Enable accessibility. If Auto-start Manager is present, check QingPing there and review its app battery settings. | ZenUI, ROG UI, and gaming-profile entry points differ. |
| TECNO / Infinix / itel | Enable accessibility. Search for “Autostart,” “Battery optimization,” and “Background activity.” | Do not use a guessed exact path across HiOS, XOS, and regional firmware. |
| Other brands, carrier firmware, custom ROMs | Use QingPing's system-settings shortcut. Search for “Accessibility,” “Battery optimization,” and “Background.” | This general route covers brands not listed above. Confirm APK and service support first. Device-policy restrictions on managed devices must be handled by the administrator. |

For every brand: cloned apps, work profiles, and private spaces may isolate services. Validate split-screen, foldable states, landscape, and display scaling separately. After force-stopping QingPing, open it manually again. Notifications, a Recents lock, and autostart do not guarantee that it will remain running permanently.

## Troubleshooting by situation

| Symptom | Suggested order of checks |
| --- | --- |
| All apps suddenly stop working | Check disclosure consent, global protection, source switches, system accessibility, and the actual connection, then screen lock and background restrictions. |
| A button is visible but no action occurs | After the runtime checks, start a 30-second diagnostic session. Verify a candidate before creating a rule. A rule cannot create an inaccessible button. |
| A click is accepted but the ad remains | Consider simulated clicks only for the affected source. A custom rule must also permit its own gesture fallback. |
| A late pop-up is missed | Check the matching window. Enable persistent matching only for a verified control. Changing countdown text may cause a literal condition to stop matching. |
| The countdown is unfinished or no dismiss function exists | Wait until the app provides an available control. Pause protection for that source when watching an ad for a reward. |
| Ordinary content is closed accidentally | Pause immediately, disable the source's simulated clicks, and delete the problematic rule. Verify ordinary pages before enabling protection again. |
| An intentional link causes a return | Clear the source's configured target package, or enable the temporary 10-minute allowance in settings. |
| The count does not increase | Check the page result first. Failed requests, timeouts, and incomplete observations are not successes. Counts do not measure network-level ad blocking. |

## Permissions and privacy

The core feature requires manually authorized accessibility access. Notifications are only for an optional boot reminder. The app does not request internet, overlay, usage-access, or query-all-installed-packages permissions and does not use screenshots, screen recording, VPN, or Root. Settings, rules, and statistics are stored in the app's private directory. Ordinary recognition does not retain whole-page content; diagnostics temporarily retain only a small set of candidates. Uninstalling or clearing app data removes local records. Exported files are managed by the user. See the [full permissions and limitations, Chinese](docs/PERMISSIONS_AND_LIMITS.md).

## Build and verification

Toolchain: JDK 17, Gradle 8.11.1, AGP 8.10.1, and Android SDK 36. Downloading build dependencies requires network access; the installed app does not connect to the internet.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

`scripts/verify.ps1` can use an already prepared portable toolchain to check the test apps as well. `fixture` and `fixturetarget` provide repeatable local scenarios and contain no commercial ad SDK. Main sources are under `app/src/main/java/cn/qingping/app/`.

For 0.4.1, the build, 74 existing unit tests, and signature checks passed. The main app has zero Lint issues; the test apps retain 11 existing warnings. This version has not undergone physical-device or emulator interaction acceptance testing. Coverage across phone brands and power consumption have not been measured. See the [verification record, Chinese](docs/VERIFICATION.md). The supplied package is a debug build with the original signature, allowing an update that retains data. See [permissions and limitations, Chinese](docs/PERMISSIONS_AND_LIMITS.md) for release and distribution restrictions.
