# 可复现验证入口

[English](VERIFICATION_TOOLING.en.md)

本地与 GitHub Actions 共用根 Gradle `verify` 任务；任务清单只维护一处。

Linux/macOS、Git Bash 或 WSL，准备 Java 17 与 Android SDK 36 后运行：

```bash
bash scripts/verify.sh
# 已缓存依赖时，可附加 --offline
bash scripts/verify.sh --offline
```

Windows PowerShell：

```powershell
.\scripts\verify.ps1
```

也可直接运行 `./gradlew verify`。首次下载构建依赖需要网络；脚本不自动同意 SDK 许可。Java/SDK 路径按常规 `JAVA_HOME`、`ANDROID_HOME` 配置，PowerShell 入口也识别项目已有的便携工具链。

统一任务包含：主应用和夹具调试构建、JUnit/Robolectric、服务覆盖率门槛、主应用和广告夹具 Lint，以及 release 清单不可调试检查。无须维护者签名私钥；这不生成正式发布签名。

## 机器生成的证据

成功后生成：

- `app/build/reports/verification/evidence.json`：当前测试总数、通过/失败/错误/跳过数、服务执行数、JaCoCo 计数与百分比、release 清单检查及 CI 提交号。
- `app/build/reports/verification/summary.md`：便于人工阅读的同源摘要。

CI 直接运行 Bash 入口，将摘要写入 Actions Summary，并将 JSON、JUnit XML、HTML、Lint 和 JaCoCo 报告保存为 `android-check-reports` 附件。测试缺失、服务测试未执行、覆盖率证据缺失或清单检查不成立会失败；脚本/Gradle 的非零退出码会使 CI 失败。

不把“74 项”或“96 项”写死为自动证据。报告按当次结果计算，通过数与跳过数分开；旧发布标签的数字只代表该标签的历史快照。逐类覆盖率门槛见下表，唯一配置在 Gradle 中维护。

## 真机证据另行记录

Robolectric、静态清单及主机测试均不能证明真实 OEM 行为、手势命中或耗电。机器报告始终将 `physicalDeviceAcceptanceEstablished` 标记为 false。使用[中文真机模板](DEVICE_ACCEPTANCE_TEMPLATE.md)记录实际设备结果；未执行项不能预填通过。APK 签名及 `run-as` 需按[发布构建说明](RELEASE_BUILD.md)单独核验。

## SDK 配置与 UI 门槛

`gradle/android-sdk.properties` 是 compile/target SDK、Build Tools 与 CI 命令行工具版本的统一入口，三个 Android 模块与 CI 共用。CI 固定 SDK 安装 Action 的提交，按配置安装缺失包；不再只检查 runner 是否预装某个 aapt。自动安装使用 runner 已有的 SDK 许可，不静默接受新的许可条款。

Activity 测试操作实际按钮、对话框、搜索框和生命周期，覆盖用途同意、保护总开关、应用选择、规则配置/导入预览、统计确认与教程搜索/展开。它们使用 Robolectric，仍不代表真机触摸、截图外观或 OEM 验收。

覆盖率约束现在保护七个类，唯一配置位于 `app/build.gradle` 的 `coverageGates`：

| 类 | 行下限 | 分支下限 |
| --- | --- | --- |
| QingPingAccessibilityService | 65% | 35% |
| MainActivity | 70% | 50% |
| UsageGuideActivity | 90% | 85% |
| AdDecisionEngine | 95% | 75% |
| AdControlPolicy | 95% | 80% |
| PackagePolicy | 95% | 95% |
| ExactRule | 90% | 70% |

`jacocoCriticalCoverageVerification` 执行全部门槛，旧的服务门槛任务名保留为兼容入口。JSON 与 Actions Summary 会列出逐类实测值、门槛、两个 Activity 的测试执行数及 SDK 配置；缺少任一 Activity 测试或关键类证据会失败。门槛仍是回归下限，不把未覆盖代码视为已经验证。
