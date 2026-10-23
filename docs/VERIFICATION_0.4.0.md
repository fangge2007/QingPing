# 清屏 0.4.0 验证记录

日期：2026-10-23（Asia/Shanghai）。本轮针对用户报告“华为上 B站及其它开屏可见跳过按钮但未关闭”，完成代码修复与本地 Debug 交付验证。没有真机复现，未发布商店。

## 最终验证结果

最终源码执行 `scripts/verify.ps1`，`BUILD SUCCESSFUL in 40s`；122 个任务，15 个执行、107 个最新。本地完整日志 `verification-0.4.0-final.log`。上一版记录已保留为 `VERIFICATION_0.3.0.md`。

| 项目 | 结果 |
| --- | --- |
| 清屏 `:app:assembleDebug` | 通过 |
| `:fixture:assembleDebug` / `:fixturetarget:assembleDebug` | 均通过 |
| `:app:testDebugUnitTest` | 74 项全通过：决策引擎 36、控件策略 7、开屏目录 9、稳定控件身份 3、精确规则 19；失败/错误/跳过均 0 |
| 清屏 `:app:lintDebug` | 通过；最终 XML 无 issue，0 错误、0 警告 |
| fixture `:fixture:lintDebug` | 通过；0 错误、11 警告，未隐藏或禁用 Lint |
| 最终清屏 APK 签名 | `apksigner verify --verbose --print-certs` 通过，Signature Scheme v2 |
| 与 0.3.0 签名一致性 | 新旧 APK 证书 SHA-256 相同，支持覆盖升级；数据库和偏好未换结构；未执行设备覆盖安装验收 |
| APK 包及版本 | `cn.qingping.app`，versionCode=4，versionName=0.4.0；minSdk=26、targetSdk=36 |
| 最终 APK 权限检查 | 仅 `RECEIVE_BOOT_COMPLETED` 与 `POST_NOTIFICATIONS`；无障碍绑定权限在服务组件上 |
| 无障碍资源检查 | flags=0x52，包含报告 ID、非重要视图、交互窗口；手势/取树能力开启，`isAccessibilityTool=false` |
| 安卓模拟器 / 真机 | 未执行；未测量跨品牌覆盖率、B站真实版本识别率、误触、耗电或后台恢复 |

最终新旧签名证书 SHA-256：

`a475dd3aa88e7a2b8bd702c5a795193574d74ad3d1ae039cb868f9117af2926a`

fixture 的 11 条警告：CustomSplashScreen 1、DataExtractionRules 1、MissingApplicationIcon 1、SetTextI18n 8。它是专用本地测试 APP，保留测试布局和动态状态文案；主 APP 无 Lint 问题。

## 本轮增加的逻辑覆盖

- B站主版两个完整 ID、HD 及其它已核实来源；不额外依赖可读广告徽标。严格来源与完整命名空间，错误来源/后缀、跳过片头/教程/登录/下载等拒绝。
- B站同 APP 后期开屏、其它新增目录 30 秒范围、描述标签、B站入口空标签后备及其它来源/后期空标签拒绝、直接查询目录隔离，以及匹配后仍等真正清屏才统计。
- 倒计时竖线/中点分隔符与业务文字排除。
- 已受理点击在 2.5 秒未确认后允许引擎请求升级，但不是成功计数；没有历史、仍在 pending、错误 key 或 reset 后不升级。
- 同来源、同窗口及同位置的稳定身份；窗口替换、布局移动、其它包和无效尺寸不满足超时手势升级。
- 原有连续广告、稳定清屏恢复、每控件及每轮请求预算、回调代次、跳转精确目标、静态横幅不触发返回，以及严格 JSON/persistent 回归。

## Android 适配层源码审查

- 两份 XML 与连接时设置均请求 `FLAG_INCLUDE_NOT_IMPORTANT_VIEWS`；这是公开配置，不是强行取得不公开节点，也没有加入隐藏 API、Shizuku 或虚假辅助残障用途。
- 先对最多 8 个内置/已配置完整 ID 查询，每个至多接入 8 个结果。查询后以元数据先核验完整祖先路径的输入、来源、窗口和 24 层深度，再读取非输入路径标签；不因快速查询越过输入隔离。常规遍历仍限 700 节点/24 层。
- 不再因聚合“下载”或长描述剪掉整棵子树；只排除该节点的候选/证据，继续找独立安全子按钮。小型危险可点击父节点及其后代不制造动作证据。
- 普通点击与手势前仍复核当前来源/窗口、节点刷新、可见/可用、小几何和安全子树。手势进一步检查任意尺寸可点祖先；不可点击叶子不能把触摸交给危险下载父容器。
- 超时升级还需原语义点击的窗口与 bounds 一致、既有 APP 手势授权及精确规则单独授权；不自动开启模拟点击。系统拒绝手势发生异常时会清理进行中状态，避免本来源卡住；不把回调或提交当广告消失。

这些是源码审查与纯 Java 测试结论，没有运行 Android 适配层端到端验收。

## 测试 APP 与设备边界

新编译场景 `aggregate`（聚合下载/超长父描述）、`dangerousparent`、`notimportant`、`noopclick`（语义点击无效而触摸才关闭）、`dangerousgesture`，以及原连续/延迟/多窗口/拆分文字场景均已构建。fixture 不加载商业广告 SDK；未在模拟器或手机执行，不能说危险计数已实测为零。

当前 SDK 未安装 emulator/system image；此前 adb 在此桌面沙箱初始化目录受限，未获得设备验收结果。本轮没有把桌面构建或上游项目快照当作用户的手机验证。Android 8.0+ 是 API 范围，不是全部品牌、系统和 APP 的覆盖承诺；独立系统不支持 Android APK 时也不能据此运行。

步骤见 `DEVICE_TEST_PLAN.md` 与 `RELEASE_0.4.0.md`。原分来源统计、签名、后台/开机恢复说明及系统方向权限指南保留；没有 Root、联网、截图、OCR、VPN、模型或悬浮窗保活。

## 交付

运行 `scripts/package.ps1` 生成 `artifacts/qingping-0.4.0-debug.apk`、源码 ZIP 和测试 APK，旧版本保留。`SHA256SUMS.txt` 提供校验；源码包排除工具、缓存、密钥和构建日志。实际 GitHub 来源见 `BILIBILI_RESEARCH_0.4.md`、`SPLASH_CATALOG_RESEARCH.md`，MIT 声明保留在 APK。
