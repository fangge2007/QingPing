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

不把“74 项”或“96 项”写死为自动证据。报告按当次结果计算，通过数与跳过数分开；旧发布标签的数字只代表该标签的历史快照。服务主类的行 60%、分支 30% 是回归门槛，不是完备性保证。

## 真机证据另行记录

Robolectric、静态清单及主机测试均不能证明真实 OEM 行为、手势命中或耗电。机器报告始终将 `physicalDeviceAcceptanceEstablished` 标记为 false。使用[中文真机模板](DEVICE_ACCEPTANCE_TEMPLATE.md)记录实际设备结果；未执行项不能预填通过。APK 签名及 `run-as` 需按[发布构建说明](RELEASE_BUILD.md)单独核验。
