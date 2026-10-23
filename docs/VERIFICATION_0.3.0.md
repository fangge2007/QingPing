# 清屏 0.3.0 验证记录

日期：2026-10-23（Asia/Shanghai）。本地安装 Debug 交付，未发布商店。

## 最终验证结果

最终源代码运行 `scripts/verify.ps1`，输出 `BUILD SUCCESSFUL in 1m 5s`，122 个任务、40 个执行、82 个最新。完整本地日志为项目根目录 `verification-0.3.0.log`。

| 项目 | 结果 |
| --- | --- |
| 清屏 `:app:assembleDebug` | 通过 |
| `:fixture:assembleDebug` / `:fixturetarget:assembleDebug` | 均通过 |
| `:app:testDebugUnitTest` | 60 项全通过：决策引擎 34、控件策略 7、精确规则 19；失败/错误/跳过均 0 |
| 清屏 `:app:lintDebug` | 通过；最终 XML 无 issue，0 错误、0 警告 |
| 测试 APP `:fixture:lintDebug` | 通过；0 错误、8 警告，见下文；没有禁用 Lint |
| 最终清屏 APK 签名 | `apksigner verify --verbose --print-certs` 通过，APK Signature Scheme v2 |
| 与 0.2.0 签名一致性 | 两个 APK 证书 SHA-256 一致，支持覆盖升级；统计数据库及偏好保留原结构；未在设备执行覆盖安装验收 |
| APK 包和版本 | `cn.qingping.app`，versionCode=3，versionName=0.3.0；minSdk=26、targetSdk=36 |
| 最终 APK 权限检查 | 仅 `RECEIVE_BOOT_COMPLETED` 与 `POST_NOTIFICATIONS`；无障碍绑定权限在服务组件上 |
| 无障碍编译资源检查 | `canRetrieveWindowContent=true`、`canPerformGestures=true`、`isAccessibilityTool=false`；窗口/内容/窗口集合/点击事件已包含 |
| 模拟器 / 真机 | 未执行；没有据此宣称真实 APP、华为/vivo 后台恢复或耗电通过 |

最终新旧签名证书 SHA-256：

`a475dd3aa88e7a2b8bd702c5a795193574d74ad3d1ae039cb868f9117af2926a`

fixture 的 8 条 Lint 警告分别是：自定义 Splash 测试 Activity 1 条、Android 12 数据导出配置提示 1 条、未指定应用图标 1 条，以及动态测试文案拼接 5 条。这些位于专用本地测试 APP；主 APP 的 Lint 报告仍为零问题。未隐藏或压低报告级别。

## 逻辑及源码核对范围

- 迟到广告、成功后再次出现、稳定清屏恢复、连续阶段共用动作预算；同控件两次较快接受及至少 10 秒后的最后一次延迟尝试。
- 拒绝请求不计数、同控件拒绝次数及每轮总请求上限；候选变化不能绕过预算。
- 点击后至少 200ms、两次完整观察间隔至少 180ms，且所有匹配关闭控件及广告上下文消失才确认；确认超时、残缺树、阶段更换、仍有其他关闭控件时不确认。
- 重复、陈旧、迟到与跨轮次回调过滤；短暂普通内容和不完整树不恢复次数预算。
- 精确目标跳转、来源恢复确认、无广告/错误目标排除，以及普通静态广告标记不能单独准备返回。
- 五个新增条件 SDK ID、严格 Activity 限定、独立广告证据；危险下载/安装/支付等 CTA；锁定倒计时与普通立即跳过倒计时的区别。
- 普通业务 Skip 不在 15 秒后泛化，明确广告关闭可持续；拆分短叶子文字正例及下载/账户/教程混合负例。
- 精确 JSON 规则严格字段/类型/容量/深度/Unicode 校验、来源及页面约束，以及显式 `persistent` 的缺省兼容、范围与序列化。

另对 Android 适配层进行了源码审查：主动扫描和事件合并；当前来源/窗口、可见性/可用性与小控件边界；危险父子节点拒绝；输入子树排除；缺失子节点或被剪枝子树不当作完整确认；待确认控件同 ID/位置仍可见时不因标签改变而当作消失；锁屏/撤销授权后的代次与任务清理。这些审查不是 Android 运行验收。

## 测试 APP 与真实设备的区别

fixture 已构建 `popup35`、`video`、`chain`、`repeat`、`dialog`、`business`、`businessdialog`、`split`、`splitbusiness`，以及原开屏、延迟、仅触摸图标、真实加速度计和目标跳转场景。视频场景是本地控件倒计时，不播放真实网络视频，也不加载商业 SDK。

当前 SDK 中未安装 emulator/system image；adb 在此桌面沙箱报 `Cannot mkdir '\.android': Permission denied`，配置工作区 Android 用户目录后仍未获得设备清单。没有因这个桌面问题修改手机权限或把它当作 APP 运行失败。真实覆盖率、误触率、耗电、各 ROM 的开机与清后台恢复均未测量。

安装和设备验收步骤见 [DEVICE_TEST_PLAN.md](DEVICE_TEST_PLAN.md)。升级需重新同意版本 3 的持续前台用途；不能自行开启系统无障碍。无 Root 的传感器限制和厂商指南继续按原说明保留。

## 构建与交付

工具链为便携 JDK 17、Gradle 8.11.1、AGP 8.10.1、Android API 36 / Build Tools 35.0.0。`scripts/verify.ps1` 使用 C1 和两个工作线程，避免已知本机 JDK C2 崩溃；没有关闭 Lint。APK 没有第三方运行时依赖、本机模型库、截图、联网、Root、VPN 或悬浮窗。

交付 APK 与源码包位于 `artifacts/`，由 `scripts/package.ps1` 生成；SHA-256 校验见同目录 `SHA256SUMS.txt`。源码包排除便携工具、构建缓存、崩溃日志和签名密钥，保留 MIT 来源说明。GitHub 实际代码研究及未接入的图像后备见 [PROJECT_COMPARISON.md](PROJECT_COMPARISON.md) 和 [VISUAL_DETECTION_RESEARCH.md](VISUAL_DETECTION_RESEARCH.md)。
