# 清屏 0.4.2 验证记录

验证日期：2026-10-24。历史记录：[0.4.1](VERIFICATION_0.4.1.md)、[0.4.0](VERIFICATION_0.4.0.md)。本页区分主机自动化与真实设备结果。

本页下文是 `v0.4.2` 发布标签的验证快照。后续 `main` 增加回调负例及统一验证工具，测试数量与覆盖率以对应提交生成的 `evidence.json` / Actions Summary 为准，使用方法见[可复现验证](VERIFICATION_TOOLING.md)。真机填写[验收记录模板](DEVICE_ACCEPTANCE_TEMPLATE.md)，不能用新增主机测试追认为该标签已通过真机验收。

## 已完成的本地检查

- Java 17、Gradle 8.11.1、AGP 8.10.1、compile/target SDK 36、min SDK 26。
- 主应用 release 构建、两份测试夹具构建通过；主应用 debug/release Lint 均无问题。测试广告夹具保留原有 11 项 Lint 警告。
- 96 项测试通过，0 失败、0 错误：原 74 项纯逻辑测试、2 项包名/危险动作测试、10 项 Android 服务用例在 API 28 与 35 各运行一次（20 项）。
- 服务测试包含系统事件过滤更新/撤销、实际点击边界、锁屏与授权撤销、危险子节点、失效节点、前台变化、旧关键包名配置、完整扫描、手势授权以及跳转返回。仅 Android 系统 IPC 由 Robolectric shadow 替代。
- JaCoCo 服务主类：行 345/505（68.32%），分支 301/834（36.09%）。整个 `QingPingAccessibilityService.java` 含内部类：行 361/538（67.10%），分支 303/846（35.82%）。未覆盖部分继续保留在报告中。
- CI 门槛针对服务主类：行覆盖率至少 60%，分支至少 30%；本地门槛检查通过。这是回归下限，不是完备性证明。
- `apksigner verify --verbose --print-certs` 通过 APK v2 签名校验，单一 RSA 3072 签名者。
- `aapt dump badging` 确认包名 `cn.qingping.app`、versionCode `6`、versionName `0.4.2`，没有 `application-debuggable` 标记；release 清单也没有启用 `debuggable`（缺省为 false）。
- 项目 LICENSE 随 APK 的 `assets/project_license.txt` 分发，上游完整 MIT 声明仍在 `assets/third_party_notices.txt`。

正式 APK SHA-256：

```text
050a943ca084ed7547d4d1230fe02a2d616ca240872149d5d772a93e358a1121
```

发布证书 SHA-256（后续正式更新应保持此身份）：

```text
872d3f574d5498c7bc17f29b5fe18027f8e1848a495439c6d88096716071c178
```

0.4.1 使用不同的调试证书，不能直接覆盖升级。请先阅读[迁移说明](RELEASE_BUILD.md)，不要把签名变更当成可保留全部数据的普通更新。

## 可复现命令与证据

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoServiceCoverageVerification :app:lintDebug :app:processReleaseManifest :fixture:assembleDebug :fixture:lintDebug :fixturetarget:assembleDebug
```

具备私有签名环境变量后，可运行 `:app:assembleRelease :app:lintRelease`；凭据格式见[构建说明](RELEASE_BUILD.md)。源码仓库与 CI 不包含维护者发布私钥。

- CI 配置：[android.yml](../.github/workflows/android.yml)，[运行记录](https://github.com/fangge2007/QingPing/actions/workflows/android.yml)。实际云端状态以对应提交的运行结果为准。
- 主机测试结果：`app/build/test-results/testDebugUnitTest/`。
- HTML 测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`。
- JaCoCo 报告：`app/build/reports/jacoco/jacocoTestReport/`（HTML 与 XML）。
- CI 报告作为 `android-check-reports` 附件保存。

## 尚未完成

没有本版物理手机或安卓模拟器 UI 验收；没有真实 App 版本覆盖矩阵、误触率或电量消耗实测，也没有设备端 `run-as` 检查。Robolectric 即使选择两个 API 版本，仍然是主机模拟，不等于两款手机通过验收。真实无障碍授权、厂商后台限制、窗口与手势命中、连续广告效果仍需按[设备计划](DEVICE_TEST_PLAN.md)核验。

内置目录仍为 8 个来源应用和 9 个精确 ID，另有 3 个开屏 SDK ID 和 5 个条件关闭 ID。本次安全修复与测试增强不能推导出真实覆盖率增长或零误触保证。
