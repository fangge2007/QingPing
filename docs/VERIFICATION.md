# 清屏 0.4.1 验证记录

日期：2026-10-23（Asia/Shanghai）。本轮按用户要求加入面向不同 APP、Android 系统与手机型号的通用使用教程及故障应对。没有修改识别引擎、动作策略、权限或同意版本，没有执行真机或安卓模拟器验收。

## 最终验证结果

最终源码执行 `scripts/verify.ps1`，`BUILD SUCCESSFUL in 40s`；122 个任务，15 个执行、107 个最新。完整本地日志为 `verification-0.4.1-final.log`。先前 0.4.0 的验证记录另存为 `VERIFICATION_0.4.0.md`。

| 项目 | 结果 |
| --- | --- |
| 清屏 `:app:assembleDebug` | 通过 |
| 两个测试 APP 构建 | `:fixture:assembleDebug` / `:fixturetarget:assembleDebug` 均通过 |
| `:app:testDebugUnitTest` | 现有 74 项全通过：决策引擎 36、控件策略 7、开屏目录 9、稳定控件身份 3、精确规则 19；失败、错误、跳过均为 0 |
| 清屏 `:app:lintDebug` | 通过；最终 XML 中 0 问题 |
| fixture `:fixture:lintDebug` | 通过；0 错误、11 警告，未隐藏或禁用 Lint |
| 清屏 APK 签名 | `apksigner verify --verbose --print-certs` 通过，Signature Scheme v2 |
| 与 0.4.0 签名一致性 | 新旧 APK 的证书 SHA-256 相同；支持原签名覆盖升级，未执行设备覆盖安装验收 |
| APK 包及版本 | `cn.qingping.app`，versionCode=5，versionName=0.4.1；minSdk=26、targetSdk=36 |
| 权限检查 | 仍仅声明 `RECEIVE_BOOT_COMPLETED` 与 `POST_NOTIFICATIONS`；无障碍绑定权限位于服务组件 |
| 教程组件 | 打包清单包含 `UsageGuideActivity`，`android:exported=false` |
| 用途同意 | 继续使用 `consent_version=4`；已完成当前同意的用户不因本次教程更新重新同意 |
| 安卓模拟器 / 真机 | 未执行；未验证实际 UI 交互、不同品牌后台恢复、跨型号广告覆盖率或耗电 |

签名证书 SHA-256：

`a475dd3aa88e7a2b8bd702c5a795193574d74ad3d1ae039cb868f9117af2926a`

最终 APK：186562 字节，SHA-256：

`7dda9655896084305df790ddb975afd00415c44a10ad2cf5104e50468d292858`

fixture 的 11 条警告为 CustomSplashScreen 1、DataExtractionRules 1、MissingApplicationIcon 1、SetTextI18n 8，与上一版相同。本地 SDK 另输出无法初始化 `C:\.android` 指标目录的环境提示，不影响本次编译或 Lint 完成。

## 教程与导航源码核对

- 用途同意前、保护首页、应用列表页和设置页均有教程入口。没有在未保存规则对话框内开启教程，避免跳转后遗留旧弹窗。
- 17 个主题 ID 唯一；正文没有单 APP 或品牌专章。涵盖首次使用、小范围验证、升级、分身与窗口、可见按钮失效、倒计时、连续与迟到广告、视频、摇一摇、后台、诊断、规则、备份、统计、误触、隐私及限制。
- 可离线全文搜索主题标题、摘要与正文；搜索结果自动展开，非搜索模式逐节展开/收起，正文可选择复制。展开时只更新当前卡片，不重建列表。
- 自定义保存搜索词、展开主题与滚动位置；从系统设置回来只刷新运行状态。主界面通过 `onNewIntent` 消费并移除内部标签参数，复用主界面时仍选中对应页面，未同意时继续展示用途说明。
- 教程只读，不自动开启保护、无障碍、APP 或手势开关。所有导航指向现有设置，没有新增截图、联网或传感器控制能力。
- 对照当前源码核对了双重手势授权、3–30 秒规则范围、逐条持续匹配、动态倒计时精确条件失配、30 秒诊断与 2 分钟失效、本地 JSON 合并、统计误差和卸载删除等说明。

上述为编译、静态检查与源码审查结论。既有 74 项测试覆盖纯 Java 识别与规则逻辑，不属于新增教程的 Android 交互测试。尚未在设备上验证搜索输入、大字号布局、旋转恢复、系统设置往返与返回栈。

## 交付

运行 `scripts/package.ps1` 生成 `artifacts/qingping-0.4.1-debug.apk`、`qingping-0.4.1-source.zip` 和测试 APK，旧版本保留。APK 与已核对构建产物一致；源码 ZIP 包含两个教程类，排除工具、缓存、签名密钥和构建日志，校验值保存在 `artifacts/SHA256SUMS.txt`。

本版本是教程补充，不代表所有手机与真实 APP 已完成适配验收。历史 0.4.0 的算法修复与研究证据见归档记录；本次范围见 `RELEASE_0.4.1.md`。
