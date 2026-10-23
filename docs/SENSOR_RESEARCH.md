# 摇一摇广告：GitHub 源码研究与无 Root 边界

核对日期：2026-10-22。研究对象是实际源码，不以 README 的功能宣称代替分析。用户使用无 Root、无 LSPosed 的华为和 vivo，本轮不安装框架、不注入其他 APP，也不新增 Root 模块。

## 结论

补充：不能从“普通 APK 无法跨进程 Hook”推断“所有无 Root 手机均无系统防摇一摇”。华为和 vivo 的部分版本提供用户可手动控制的设备方向权限，本轮已核对来源并在清屏加入设置引导，见 [系统防摇一摇指南](VENDOR_SENSOR_GUIDE.md)。这条路线不需要将 Xposed 代码迁入清屏；系统控制与清屏辅助关闭可配合使用。

AdClose 等项目可以在广告 APP 的进程里阻止传感器注册或回调，依赖 Xposed/LSPosed 提供的运行时 Hook。把这些 Java/Kotlin 文件复制进清屏的普通进程，不会影响其他 APP。无障碍授权与 Shizuku 的 ADB 模式都不会自动赋予这种跨进程 Hook 能力。

无 Root 路线应提高可操作关闭按钮的覆盖：按包名和 Activity 限定规则，匹配控件 ID/说明，必要时在用户明确允许的精确规则中使用手势；没有可操作关闭控件时，只能尝试广告跳转后的返回。**这些方式不关闭其他 APP 的加速度计/陀螺仪，也不能保证摇晃发生前阻止跳转。**

## 实际阅读的项目和固定版本

| 项目 | 已核对 commit | 许可证 | 实际源码结论 |
| --- | --- | --- | --- |
| [zjyzip/AdClose](https://github.com/zjyzip/AdClose) | `11f9e5547ec1e89ad7daf7114d05a0b02aa96de1` | [CC BY-NC-SA 4.0](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/LICENSE) | 目标进程中 Hook 传感器注册；不能直接迁移到普通 APK |
| [pwh-pwh/fuck_shake](https://github.com/pwh-pwh/fuck_shake) | `34a20bad084fbc54ce4a776acbf0809defe8d02b` | [MIT](https://github.com/pwh-pwh/fuck_shake/blob/34a20bad084fbc54ce4a776acbf0809defe8d02b/LICENSE) | 替换三参数传感器注册方法，处理很宽泛，仍依赖 Xposed |
| [ldy19941102/DisableStartActivityOnSensorChanged](https://github.com/ldy19941102/DisableStartActivityOnSensorChanged) | `a694dd367cb22c04fe83ddca8b82e7796e7dd57f` | 此 commit 未找到 LICENSE/COPYING/NOTICE，GitHub `license=null` | 动态发现监听器实现，前 10 秒吞掉传感器回调；不复制其源码 |
| [RikkaApps/Shizuku-API](https://github.com/RikkaApps/Shizuku-API) | `a27f6e4151ba7b39965ca47edb2bf0aeed7102e5` | [MIT](https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/LICENSE) | Binder 代理与独立 shell/root 服务，不是目标 APP 的方法注入框架 |

以上 commit 均通过 GitHub API 的 commits 接口核对；仓库地址、固定版本、已读文件清单存于 `.research/sensor-evidence.json`。`Xposed-Modules-Repo/com.close.hook.ads` 是发行条目，本分析以 `zjyzip/AdClose` 的源码为依据。

## AdClose 如何处理摇一摇

已读 [DisableShakeAd.java 第 10–20 行](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/app/src/main/java/com/close/hook/ads/hook/common/DisableShakeAd.java#L10-L20)：它给 `SensorManager.registerListener` 安装前置 Hook，检查第二个参数确为 Sensor，遇到加速度计或陀螺仪就提前返回 true，跳过原始注册流程。

这不是“清屏自己停止监听”，而是修改广告 APP 进程里一次方法调用的执行结果。入口 [LibXposedEntry.kt 第 9–24 行](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/app/src/main/java/com/close/hook/ads/hook/LibXposedEntry.kt#L9-L24) 继承 Xposed 模块；[HookLogic.kt 第 124 行](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/app/src/main/java/com/close/hook/ads/hook/HookLogic.kt#L124) 按设置启用该功能；[HookUtil.java](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/app/src/main/java/com/close/hook/ads/hook/util/HookUtil.java) 最终调用 XposedBridge，而不是普通 Android 授权 API。

该实现没有在这段方法里区分“广告监听器”和正常的游戏/姿态/拍摄功能监听器，可能使作用域内正常传感器功能失效；也不能据这段源码承诺覆盖重力/线性加速度、原生 NDK 传感器接口等全部路径。许可证含非商业与相同方式共享要求，不按 MIT 处理。本轮只分析方案，不复制其代码进清屏。

## 两种更小的模块说明了什么

[MIT 项目的 HookEntry.kt 第 16–25 行](https://github.com/pwh-pwh/fuck_shake/blob/34a20bad084fbc54ce4a776acbf0809defe8d02b/app/src/main/java/dev/coderpwh/fuck/hook/HookEntry.kt#L16-L25) 只按方法名和三个参数筛选，并替换为 true。它不检查传感器类型，不能理解为“只阻止广告摇一摇”；四参数等调用的覆盖也不能只凭 README 推定。

另一个项目的 [HookSensorEventListener.java 第 17–32 行](https://github.com/ldy19941102/DisableStartActivityOnSensorChanged/blob/a694dd367cb22c04fe83ddca8b82e7796e7dd57f/app/src/main/java/lin/xposed/hook/item/HookSensorEventListener.java#L17-L32) 设置 10 秒计时；[第 42–70 行](https://github.com/ldy19941102/DisableStartActivityOnSensorChanged/blob/a694dd367cb22c04fe83ddca8b82e7796e7dd57f/app/src/main/java/lin/xposed/hook/item/HookSensorEventListener.java#L42-L70) Hook 类加载，寻找直接实现 SensorEventListener 的类，再吞掉其 onSensorChanged 回调。虽然仓库名提到 startActivity，所读代码实际拦的是传感器回调，未在此处检查广告或拦截启动 Intent。动态发现方式有漏掉继承、已加载类等情况的可能，且未找到可复用许可，所以仅列作研究证据。

这两种方法展示的是特权 Hook 的不同切入点，不是给清屏增加一个权限便能启用的现成能力。

## 无障碍、Shizuku、Root + LSPosed 的差异

| 能力 | 普通 APK + 无障碍 | Shizuku 的 ADB/shell 模式 | 已部署的 Root + LSPosed 环境 |
| --- | --- | --- | --- |
| 读取公开无障碍节点并点击/返回 | 用户授权后可做 | 不是必需 | 不是必需 |
| 阻止目标进程调用 registerListener | 不可通用实现 | 仅安装/授权 Shizuku 不会实现 | 模块注入目标进程后可 Hook，需适配 |
| 用公开运行时权限撤销其他 APP 的普通加速度计读取 | 没有通用权限接口 | 不等于获得厂商逐 APP 传感器控制 | 可用进程 Hook 等手段，存在副作用 |
| 系统 Binder 管理操作 | 受本应用权限约束 | 可在 shell 已有权限范围内使用 | 视 root、SELinux、系统接口和框架而定 |
| 每次重启后自动可用 | 受系统服务恢复与厂商后台管理限制 | 无 Root 启动通常需重启后再次激活 | 取决于已安装框架、模块作用域与兼容性 |
| 本轮清屏是否采用 | 是 | 不增加此依赖 | 不实现，用户设备无此环境 |

这里对 Shizuku 的结论来自源码而非名称判断：[ShizukuBinderWrapper.java 第 30–49 行](https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/api/src/main/java/rikka/shizuku/ShizukuBinderWrapper.java#L30-L49) 将 Binder 事务转发给 Shizuku 服务；其 [开发文档](https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/README.md#differents-of-the-privilege-betweent-adb-and-root) 区分 shell UID 2000 与 root UID 0。独立 UserService 能运行自己的代码，不代表代码进入了广告 APP 的进程。

[Android 传感器文档](https://developer.android.com/develop/sensors-and-location/sensors/sensors_overview) 描述了应用自己注册监听的机制；[应用沙箱说明](https://source.android.com/docs/security/app-sandbox) 说明不同应用运行于隔离进程。依据这些机制，本报告判断：对无 Root 华为/vivo，不能把 BODY_SENSORS、运动健身或高采样率权限误认为可撤销全部低频加速度计的通用开关。厂商若额外提供逐 APP 摇一摇/传感器设置，应按实际机型让用户手动选择，不能预设所有机型都有。

## 适合结合到本轮清屏的部分

1. 继续维护普通无障碍方案，明确“关闭页面/返回页面”和“阻断传感器”是两种能力。
2. 优先增加包名 + Activity + 控件 ID/内容描述的精确关闭规则，覆盖图标关闭、不同位置、延迟出现等实际界面差异。
3. 只有用户主动导入并允许的精确规则，才能在控件不支持点击时尝试节点中心手势；保持边界检查、窗口期限、去抖与结果确认，避免把广告容器当关闭按钮。
4. 将可复现的问题用例加入 fixture；不能把测试页处理成功推广为所有 APP 已适配。
5. 在产品页面如实呈现未适配和不可处理情况。无可访问控件、受保护画面或广告 SDK 内部逻辑不应显示“已成功阻止传感器”。

本轮未复制上述传感器模块代码，也未将 Shizuku/Root/LSPosed 加入普通清屏 APK。研究结论不构成对华为、vivo 所有系统版本的覆盖保证。
