# 发布构建、签名与迁移

[English](RELEASE_BUILD.en.md)

从 0.4.2 起，分发主安装包采用 `release` 构建，`debuggable=false`，使用独立 RSA 3072 发布密钥。两份可选广告测试 APK 仍是调试包，不能与主应用混淆。

## 从 0.4.1 调试版迁移

0.4.1 的调试签名与新的发布签名不同。Android 不允许直接覆盖安装，也没有在两个不同身份之间自动保留数据的迁移通道。

1. 在旧版导出精确规则 JSON，需要保留统计时另导出 CSV，并记录来源开关、模拟点击及跳转目标设置。
2. 确认导出文件已保存到应用目录之外。CSV 仅供查阅，不支持还原统计；JSON 仅包含精确规则，不是全部设置备份。
3. 卸载旧调试版，再安装 `qingping-0.4.2-release.apk`。卸载会删除原设置和本机统计，请先确认上述信息已保存。
4. 重新同意用途、启用无障碍、选择来源应用，预览并导入 JSON，逐应用验证行为。

后续发布必须继续使用同一发布密钥，并增加 versionCode，才能以同签名覆盖升级。丢失密钥及口令后不能冒充同一发布身份。

## 构建

使用 Java 17、Android SDK 36、Build Tools 35.0.0 和 Gradle Wrapper。主源码按根目录 MIT 许可证授权；第三方材料保留各自声明。

调试构建与测试：

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:jacocoTestReport :app:lintDebug
```

发布构建需要通过环境变量提供自己的签名凭据：

| 变量 | 用途 |
| --- | --- |
| `QINGPING_KEYSTORE` | PKCS12/JKS 文件绝对路径 |
| `QINGPING_STORE_PASSWORD` | 密钥库口令 |
| `QINGPING_KEY_ALIAS` | 密钥别名，默认 `qingping` |
| `QINGPING_KEY_PASSWORD` | 私钥口令 |

```sh
./gradlew :app:assembleRelease :app:lintRelease
```

Windows 也可运行 `scripts/build-release.ps1`。缺少凭据时发布签名任务明确失败，不回退到调试密钥。密钥与口令不得提交 Git 或上传为 CI 附件。本机维护凭据位于被排除的 `.signing/`；请将整个目录私下备份到受控离线位置。项目不向其他构建者提供维护者私钥，自行构建会形成自己的安装身份。

```sh
apksigner verify --verbose --print-certs qingping-0.4.2-release.apk
aapt dump badging qingping-0.4.2-release.apk
```

签名指纹及最终验证记录见 [VERIFICATION.md](VERIFICATION.md)。`run-as` 应拒绝正式包；实际设备测试仍需启用 USB 调试并人工执行，不能以静态清单检查冒充设备验收。

## 自动化边界

GitHub Actions 构建调试应用及测试夹具，运行纯逻辑和 Robolectric Android 服务测试、Lint 并保存 JaCoCo 报告；另核对 release 清单未启用调试。CI 不持有发布私钥，不生成冒充正式身份的签名安装包。

Robolectric 在主机模拟 Android API 与系统调用边界，不能代替真实无障碍授权、OEM 进程管理、真实 App 广告、屏幕手势命中或耗电测试。继续按 [设备测试计划](DEVICE_TEST_PLAN.md) 收集证据。
