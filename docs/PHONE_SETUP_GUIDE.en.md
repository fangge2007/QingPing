# Phone setup and troubleshooting by brand

[Back to the project](../README.en.md) · [中文](PHONE_SETUP_GUIDE.md)

Applies to QingPing 0.4.2. Official documentation checked on **2026-10-23**.

The old 0.4.1 debug APK uses a different signing certificate. Read the [export and migration guide](RELEASE_BUILD.en.md) before installing the new release APK.

This guide covers different apps, phone brands, and Android systems. The brand table provides setup references, not a certified device list. These brands have not all been tested on physical devices. Regional, carrier, and OS variants may use different menus and service recovery policies.

QingPing requires an **Android 8.0 or later APK environment with access to Android Accessibility services**. Installing the APK, enabling the system switch, connecting the service, and successfully handling an ad are separate conditions. Extra permissions cannot guarantee a skip when no readable, actionable dismissal control exists.

The app currently displays Chinese UI text. English labels below explain the corresponding Chinese controls; they do not imply that the app itself has an English interface.

## 1. Seven setup steps for every brand

### 1. Check the system and install the APK

Search Settings for “About phone” or “Android version” and check the Android 8.0 minimum. Download the APK from this repository's Releases and follow the system installation prompts.

If asked, allow installation only for the browser or file manager used for this download; you can revoke that permission afterward. QingPing does not require Developer options, USB debugging, Root, or disabling device security scanning.

Systems that cannot run APKs cannot directly install this version. Installation inside a compatibility container also does not establish access to the host system's Accessibility services; see the Huawei rows.

To upgrade, install the newer APK signed with the same key over the existing app. Do not uninstall first: uninstalling or clearing app data removes local settings, exact rules, and statistics.

### 2. Read the disclosure and select one source app

Open QingPing, read the data-use disclosure, and consent if you understand it. Start by enabling just one app in “应用” (Apps).

The source app is the app showing the ad. Under its “配置规则” (Configure rules), check “识别并点击广告跳过 / 关闭控件” (Identify and click ad skip/close controls). Saving a rule alone does not enable the source app.

Leave simulated taps off while testing normal control clicks. There is no need to enable every app at once.

### 3. Enable QingPing's Accessibility service manually

In “保护” (Protection), tap “前往开启无障碍” (Open Accessibility settings). Select QingPing, read the system prompt, and enable its service manually. Settings searches such as “Accessibility,” “Installed services,” or “Downloaded apps” can also help.

Some installation environments on Android 13 and later restrict sensitive settings for sideloaded apps. If “Restricted setting” appears, verify the APK source and purpose before deciding whether to allow it. Google's reference path is Settings → Apps → QingPing → More → Allow restricted settings; manufacturers may change or omit it. [Android: restricted settings](https://support.google.com/android/answer/12623953?hl=en-GB)

If the service is absent or enabling it is prohibited, follow the device's restrictions. Another app's Accessibility service, device administrator access, or “Display over other apps” is not a required substitute.

### 4. Enable protection and verify the three runtime states

Return to QingPing, enable “启用保护” (Enable protection), and open “设置 → 运行检查” (Settings → Runtime check). Confirm:

- System Accessibility is enabled.
- QingPing's service is actually connected.
- Global protection is on, and the source app is individually enabled.

An enabled system switch with a disconnected service cannot handle ads. Manually switch QingPing's Accessibility service off and on, then check again.

### 5. Adjust background settings only if disconnections recur

Try normal use first. If screen locking, cleanup, or rebooting repeatedly interrupts the service, consult the brand table. “设置 → 开机与后台运行” (Settings → Startup and background operation) links to the battery optimization list and QingPing's app information.

Allow background activity, autostart, or a cleanup exception for QingPing only as needed. A recent-app lock may help where provided. Other apps do not all need unrestricted background access.

Relaxing restrictions can increase battery consumption and cannot guarantee permanent service operation. Keep normal power management for other apps and verify results in Runtime check and the actual ad page.

### 6. Test both ads and ordinary pages

With the screen on and unlocked, reopen the selected app in normal full-screen mode. Check whether an available ad skip/close button works and whether the app returns to ordinary content.

Also check regular navigation, business dialogs, and video playback for unwanted dismissals. Add source apps one at a time after validation; counters alone are insufficient evidence.

### 7. Recheck after updates, rebooting, or changing profiles

After updating Android, QingPing, or a target app, check runtime status. Reconnect Accessibility if the switch is on but the service is disconnected. Diagnose again if a rule stops matching.

Check after rebooting and first unlocking the device. “开机后提醒检查状态” (Remind me to check after boot) is optional. Android 13 and later also require notification permission and an enabled notification channel for reminders; refusing notifications does not stop an already connected service from identifying ads.

## 2. Brand and OS reference table

**Evidence labels:** “Official reference” means the linked documentation describes a feature for the specified device or version. It does not mean that the manufacturer endorses QingPing or that every model has the same menus. “Check on device” means a family-wide official path was not confirmed in this review. Complete the seven steps above before applying a row.

Use QingPing's button to reach Accessibility first. Background changes below address service disconnections. If Settings search finds nothing, consult the exact model's help or manufacturer rather than guessing hidden menus.

| Brand / system family | Procedure and Settings search terms | Notes and evidence |
| --- | --- | --- |
| **Huawei: EMUI / HarmonyOS environments directly running Android APKs** | Search Accessibility for installed/downloaded services. Search “App launch”; for QingPing, disable automatic management and allow auto-launch, secondary launch, and background activity as needed. Use a recent-app lock if provided. | **Official reference limited to HarmonyOS 4.X and earlier.** Huawei documents third-party Accessibility switches being disabled after cleanup/reboot, with reconnection, task-locking, and manual launch-management steps. Do not copy this path to every newer HarmonyOS version. [Huawei Accessibility disconnections](https://consumer.huawei.com/cn/support/content/zh-cn00410039/) |
| **Huawei: HarmonyOS 5+ / native HarmonyOS / compatibility containers** | First establish whether the APK can install and whether the system can list and connect QingPing's Android Accessibility service. Installing inside ZhuoyiTong or another container does not establish access to native or outside-container apps. | **Official installation reference; QingPing behavior in this combination remains unverified.** Huawei says support for some APK installations depends on ZhuoyiTong. This project provides no native HarmonyOS HAP and has not validated cross-container Accessibility. If the required APK environment or Accessibility is unavailable, this version is unsuitable. [Huawei HarmonyOS 5+ installation](https://consumer.huawei.com/cn/support/content/zh-cn16061787/) |
| **HONOR: MagicOS / Magic UI** | Open Accessibility through QingPing. Search “App launch”; switch QingPing to manual management and allow auto-launch, secondary launch, and background activity as needed. Lock its recent task if available. | **Official reference lists MagicOS 7/8 and some Magic UI versions.** Check newer and regional variants on device; Huawei and HONOR menus are not interchangeable across all versions. [HONOR background app guidance](https://www.honor.com/cn/support/content/zh-cn00426500/) |
| **Xiaomi / Redmi / POCO: HyperOS / MIUI** | Search “Background autostart” or “Autostart.” A model-specific reference is Settings → Apps → Permissions → Background autostart. Search app battery settings for background limits or “No restrictions”; relax only QingPing if needed. | **Official autostart reference; exact battery and Accessibility submenus require device checks.** The source covers REDMI 13C, not every POCO, regional model, or HyperOS release. [Xiaomi background autostart](https://www.mi.com/global/support/faq/details/KA-497677/) |
| **OPPO: ColorOS** | Open Accessibility through QingPing. Search “Battery usage,” “Background activity,” and “Auto launch.” OPPO documents Settings → Battery for app consumption on ColorOS 14+; inspect QingPing's app information for available background controls. | **Official battery entry reference; current background permission path needs device checking.** Do not assume task-lock gestures from ColorOS 3/5 apply to newer versions. Use a Lock option only if present. [OPPO app battery usage](https://support.oppo.com/en/answer/?aid=2171043) |
| **OnePlus: China ColorOS / international OxygenOS** | Follow the actual OS, not just the phone brand. The OxygenOS 14 manual documents Settings → Apps → Auto launch. Search battery/app settings for background activity, app battery management, or battery optimization. | **Official OxygenOS 14 autostart reference; check other versions on device.** Older international OxygenOS menus should not be assumed on China models. [OnePlus OxygenOS 14 manual](https://service.oneplus.com/content/dam/support/user-manuals/common/OxygenOS_14.0_User_Manual.pdf) |
| **realme: realme UI** | Open Accessibility through QingPing. Search “App battery management,” “Background activity,” or “Auto launch.” The official FAQ references Settings → Battery → App battery management → QingPing; allow background activity/autolaunch as needed. | **Official reference.** Check the actual options on newer, lighter, and regional editions. QingPing does not need blanket permission to launch unrelated apps. [realme FAQ: background settings for missed notifications](https://www.realme.com/global/support/faq) |
| **vivo / iQOO: OriginOS / Funtouch OS** | Search “Autostart,” then “Background power consumption” or “High background power consumption.” Allow QingPing as needed; use a recent-app lock if available. Reach Accessibility through QingPing or Settings search. | **Official support reference with older menu examples.** It lists Apps and permissions → Permission management → Permissions → Autostart, and Battery → Background power consumption management. Check newer OriginOS and international Funtouch OS menus on device. [vivo background settings](https://kefu.vivo.com.cn/robot/imgmsgData/799ea11757694d8686c8003e5864c190/index_1.html) |
| **Samsung: One UI** | Search Accessibility for installed apps. For background limits, use Settings → Device care / Battery and device care → Battery → Background usage limits. Remove QingPing from sleeping/deep-sleep lists or add it to Never auto sleeping apps as needed. | **Official reference.** Sleep lists and individual app battery restrictions may be separate controls. Names vary by One UI version; do not disable power saving globally. [Samsung background usage limits](https://www.samsung.com/uk/support/mobile-devices/how-to-optimise-the-battery-life-on-a-galaxy-smartphone/) |
| **Google Pixel / AOSP-like systems** | Use QingPing's Accessibility link. Search “App battery usage.” Pixel's reference is Settings → Apps → See all apps → QingPing → App battery usage → Allow background usage; first check that background use is allowed. | **Official Pixel reference; check other AOSP derivatives on device.** Start with normal optimization and assess an individual exception only for recurring disconnections. Do not assume every AOSP build has autostart management. [Pixel battery optimization](https://support.google.com/pixelphone/answer/7015477?hl=en-AS) |
| **Motorola / moto** | Search “Manage background apps” or “Battery optimization.” Where offered, Settings → Battery → Manage background apps can allow QingPing using “Always allow.” On other versions, inspect its app battery page. | **Official reference explicitly says availability varies by model.** Do not apply this path to all China Lenovo ZUI devices. [Motorola background apps](https://en-us.support.motorola.com/app/answers/detail/a_id/159435/~/tips-to-extend-the-battery-life) |
| **Nothing / CMF: Nothing OS** | Open Accessibility through QingPing. Long-press its icon → App info, then look for Battery / App battery usage / Manage battery usage. If Restricted, restore normal Optimised behavior and test first. | **Official reference; exact names still depend on Nothing OS version.** An Android-like interface does not establish that a particular Unrestricted submenu must exist. [Nothing apps not running](https://support.nothing.tech/hc/en-us/articles/16770258207377-What-do-I-do-if-apps-won-t-run) |
| **Meizu: Flyme** | Open Accessibility through QingPing. Search app launch management, autostart, background management, or battery optimization. Apply any provided QingPing background/cleanup exception as needed, then check connection. | **Official reference confirms a launch-management entry only; detailed device procedures remain unverified.** Flyme's account policy mentions Settings → Apps → App launch management. This does not establish all Flyme versions or service survival behavior. [Flyme launch-management reference](https://xj-platform.flyme.com/fms/policy/nocache/53d68bc26b185466b866ba21695a1ccd.html) |
| **Lenovo: ZUI / Legion; historical ZUK** | Search Accessibility, autostart, background management, battery optimization, or app management. Inspect QingPing's app information. Use cleanup exclusions/task locking only where provided. | **Check on device.** No single official path covering historical and current phone/tablet versions was confirmed. Older devices must satisfy Android 8.0; ZUI and Motorola should not be treated as one menu system. |
| **ZTE / nubia / REDMAGIC: MyOS / nubia UI / REDMAGIC OS, etc.** | Use QingPing's Accessibility link. Search autostart, background operation, battery optimization, or app management. If failure occurs only during games, inspect game-mode background restrictions and compare with an ordinary full-screen app. | **Check on device.** Related brands need not share menus. Do not disable game mode indiscriminately. Use the exact model's official manual; the [ZTE support portal](https://www.ztedevices.com/cn/support/) is for model lookup, not evidence that a particular menu was verified. |
| **Transsion: TECNO / Infinix / itel; HiOS / XOS, etc.** | Search “Auto-start,” “Background,” and “Battery optimization.” Where Phone Master is present, a reference path is Phone Master → Toolbox → Auto-start Management; allow QingPing as needed. Check battery controls on the device. | **Official service-provider reference from Carlcare for some devices; check regional/lighter editions.** Not every itel or Android Go device necessarily provides the same tool or usable Accessibility service. [Carlcare autostart management](https://www.carlcare.com/ke/tips-detail/speed-up-slow-tecno-phone/) |
| **Sony: Xperia** | Open Accessibility through QingPing. A recent Xperia example is Settings → Apps → QingPing → App battery usage → Allow background usage → Unrestricted, assessed only if disconnections occur. On older systems search power-saving exceptions or STAMINA. | **Official Xperia 10 VII reference.** STAMINA and background restrictions may affect operation; other Xperia models may differ. [Sony STAMINA app exceptions](https://helpguide.sony.net/mobile/xperia-10m7/v1/en/contents/stamina_mode.html) |
| **ASUS / ROG: ZenUI / ROG UI** | Open Accessibility through QingPing. ASUS documents Settings → Battery → Auto-start Manager; allow QingPing at boot as needed. Older systems may use PowerMaster. | **Official reference notes model differences.** Also inspect app battery policy and gaming environments; an autostart switch cannot guarantee Accessibility connection. [ASUS battery and auto-start management](https://www.asus.com/uk/support/faq/1009547/) |
| **Other Android brands / carrier builds / custom ROMs** | Open system Accessibility through QingPing. Search “Accessibility,” “Downloaded apps,” “Installed services,” “Restricted settings,” “Battery optimization,” “Background,” or “Auto-start.” Inspect the options available in QingPing's app information. | **Check on device.** QingPing cannot override ROM, enterprise, parental-management, or stripped-down-system restrictions on Accessibility. Follow the exact ROM/device's official guidance. |

## 3. Choose a procedure by symptom

### Skipping suddenly fails in every app

1. Check actual service connection, global protection, and source-app switches in Runtime check.
2. If disconnected, manually reconnect Accessibility. Check whether cleanup, Force stop, or an OS update just occurred.
3. Use brand-specific background settings only for recurring disconnections. If connected, investigate control matching next.

### A skip/close button is visible, but nothing happens

1. Check that the button is enabled, the screen is unlocked, and the selected app is foreground/full-screen.
2. Run the 30-second diagnostic below to see whether the system exposes a readable candidate. Visible text may be an image, canvas, or part of a video.
3. Consider per-app simulated taps only when the dismissal control is clearly matched but a normal click fails. Custom exact rules also need their own authorization.
4. This version has no screenshot/OCR fallback or arbitrary-coordinate tapping. Ad content, download, and reward-claim buttons are not substitutes for a dismissal control.

### The button appears after a countdown, or the ad appears later

QingPing does not accelerate countdowns or bypass required viewing time. Wait for the actual dismissal control to become usable. Generic Skip matching has a time limit after entering the source app; it does not apply unconditionally to every later page.

Exact rules default to 15 seconds after app entry, configurable to 3–30 seconds. After confirming a rule matches only an ad dismissal control, “持续匹配” (Continuous matching) can cover later dialogs. A stable return to ordinary content allows a subsequent ad episode to be handled; ineffective attempts are limited rather than repeated indefinitely.

Closing rewarded ads early may forfeit rewards. Pause the source app's protection before deliberately watching for a reward, then restore it afterward.

### Shaking or rotating the phone opens another app

If the device provides a per-app “Device orientation,” “Motion and orientation,” or similar control, assess it for the **app showing the ad**. Consider a splash-only restriction only where offered. QingPing cannot add this system feature or disable another app's motion sensors itself.

Ordinary Android Physical activity permission should not be treated as a universal accelerometer-blocking switch. System orientation restrictions can also affect legitimate motion features, so verify after changing them.

QingPing separately supports returning after an ad-related jump: configure one specific target for a source app. Only after the target opens in the qualifying recent ad context does QingPing attempt Back. It cannot prevent the first launch or cover arbitrary targets. Use Settings → “允许跳转 10 分钟” (Allow jumps for 10 minutes) for intentional visits, then “结束放行” (End allowance). Ad skipping remains enabled.

### App clones, work profiles, private spaces, or other users fail

Test an ordinary app in the main profile first. Then confirm the profile/container, actual package name, and Accessibility authorization used by the target app. Manually adding a package only saves the source; it does not bypass profile or container isolation.

QingPing does not guarantee access across all isolated spaces. Follow enterprise policies where authorization is prohibited. Do not delete profiles or clear their data as trial-and-error fixes.

### Landscape, floating windows, split screen, foldables, or games fail

Compare with normal full-screen mode, default display scaling, and the usual orientation. Windows, control bounds, occlusion, and exposed nodes may change. If full-screen works, validate other modes separately; one successful mode does not establish all-mode compatibility.

## 4. Full 30-second diagnostic and exact-rule procedure

1. Enable global protection, the source app, and ad skipping; confirm the service is connected.
2. In Apps → target app → “精确关闭规则与诊断” (Exact dismissal rules and diagnostics), tap “开始 30 秒按钮诊断” (Start 30-second button diagnostic).
3. Within 30 seconds, switch to the target app and show the ad in the unlocked foreground.
4. Return and tap “刷新诊断结果” (Refresh results). Inspect the page, scan completeness, ad evidence, and candidates.
5. Only if a candidate is the actual ad dismissal control, use “用此候选创建规则” (Create a rule from this candidate), review it, and save.
6. All nonempty conditions must match exactly together. Without a control ID, a complete activity class is required. Countdown text from one second may not match another second.
7. Start with normal clicks inside the entry window; enable Continuous matching only where later ads need it.
8. Custom-rule simulated taps require **both the rule's “允许此规则模拟点击” and the source app's “允许对内置或精确规则控件模拟点击”**. Neither is forcibly enabled by default.
9. Recheck ads and ordinary pages after saving. Pause the source app and delete a rule that causes unwanted dismissal. Recheck after target-app updates.

Diagnostics retain at most 20 candidates temporarily in memory; the last result expires after 2 minutes. A candidate is not proof of an ad and is not an exhaustive control list. A rule cannot create a missing system-exposed control.

Simulated taps are restricted to clearly matched controls within their allowed scope. They cannot force clicks anywhere on an image; dangerous-action, window-change, and bounds-consistency checks can still reject execution.

## 5. Restart, upgrade, data, and permission notes

- **Recent-task dismissal, cleanup, and Force stop differ.** Manufacturer cleanup may end the service. After Force stop, manually reopen QingPing and check its connection and Accessibility switch.
- **Notifications are optional.** They only remind you to check after boot/update. Allowing notifications does not guarantee service survival; rejecting them does not prevent a connected service from handling ads.
- **No extra blanket permissions are required.** Ad skipping does not require network, screenshots, overlays, VPN, Root, camera, microphone, or contacts in this version.
- **Enable only necessary apps.** Do not indiscriminately protect launchers, keyboards, payment apps, installers, or permission screens, or apply generic Close rules to them.
- **Backup scope is limited.** Settings → local ad dismissal rules exports QingPing JSON, up to 100 rules / 256KB. GKD subscriptions and other scripts are not directly compatible. This export excludes global protection, per-app configuration, and statistics.
- **Review imports first.** Matching rule identifiers are merged/replaced. Imports do not enable source apps or their simulated-tap switches. Statistics CSV is a report and cannot restore historical counters.
- **Statistics infer UI outcomes.** Requesting a click is not success. Counts may differ from perceived results and do not represent blocked network requests; verify the actual page.
- **Pause first after unwanted actions.** Pause global protection, disable the source app, or revoke disclosure consent in Settings. Review the rule before resuming; broader permissions do not solve unsafe matching.

## 6. Information to include in a useful issue

First consult the offline, searchable “完整使用教程与问题处理” (Full guide and troubleshooting) inside the app. Search its Chinese terms such as “点不动” (click does not work), “后台” (background), “倒计时” (countdown), or “误触” (unwanted action).

When reporting a problem, include:

- Brand, complete model, regional variant, OS version, and Android version where shown.
- QingPing version, target-app version and package name, and profile/clone/container type.
- The three Runtime check states and whether a restart, cleanup, or OS update just occurred.
- Ad type/timing, whether its dismissal button is visible and enabled, and whether manually clicking works.
- Reviewed diagnostic candidates and rule scope, including Continuous matching and both simulated-tap authorizations.

Remove personal information before sharing. Accounts, passwords, payment information, and complete chat pages are unnecessary. This guide supports further device checks; it does not claim compatibility with every brand, OS release, app, or ad.
