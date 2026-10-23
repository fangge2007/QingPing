# GitHub 同类项目分析与清屏 0.2.0 / 0.3.0 改动

核对日期：2026-10-22。目标设备为没有 Root/LSPosed 的华为、vivo。分析基于固定提交的实际源码，而非项目介绍中的覆盖率宣称。

0.3.0 按用户反馈继续读取同一 MIT 订阅固定提交的实际源码，新增五个有条件的 SDK 关闭资源名，分别覆盖字节、快手、JAD、美数及 Anythink 家族；原页面条件、独立广告证据要求、上游误触警告和准确源码链接见 [SDK_RESEARCH_0.3.md](SDK_RESEARCH_0.3.md)。该文同时记录新版持续前台约 1.5 秒低频观察、普通“跳过”的 15 秒范围、显式持续精确规则和连续广告预算。下表保留 0.2.0 的来源记录，不将其旧入口限制当作新版行为。

## 已阅读的代码

| 代码 | 发现 | 清屏 0.2.0 采取的改动 |
| --- | --- | --- |
| [GKD GkdAction.kt](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/gkd-app/src/androidMain/kotlin/li/gkd/app/data/GkdAction.kt) | `Click` 先尝试控件点击，不可点击或失败后通过 `ClickCenter` 模拟节点中心点击 | 独立实现控件点击与可选手势回退；仅用户允许的精确规则或已核实 SDK 控件可回退 |
| [GKD A11yRuleEngine.kt](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/gkd-app/src/androidMain/kotlin/li/gkd/app/a11y/A11yRuleEngine.kt#L246-L258) | APP 变化或动作后的短期重扫，以及规则所需的强制查询 | 对已选择 APP 默认15秒、每约250毫秒有界重扫，解决异步挂载却无新事件时的漏检；不照搬无限扫描 |
| [MIT 订阅的 UC 规则](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.UCMobile.ts) | 资源 ID 可以属于 SDK 命名空间，例如 `com.byted.pangle:id/tt_splash_skip_btn` | 精确规则按来源包限定，允许合法的第三方 SDK 资源命名空间，解除旧版必须同包前缀的错误假设 |
| [MIT 订阅的 NGA 规则](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/gov.pianzong.androidnga.ts#L14-L18) | 常见资源名包含 `ksad_splash_circle_skip_view`、`ksad_skip_view_skip` | 与字节跳过资源名一同作为已核实候选；仍检查来源选择、时间窗口、可见性、尺寸及动作权限 |
| [AdClose DisableShakeAd.java](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/app/src/main/java/com/close/hook/ads/hook/common/DisableShakeAd.java) | 在目标进程 Hook 加速度计/陀螺仪注册 | 不迁入普通 APK；无 Root 环境没有对应进程注入能力，不能宣称已阻止传感器 |

GKD 客户端的 [LICENSE](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/LICENSE) 为 GPLv3；本轮参考公开行为与 Android API，自行实现清屏的代码，没有复制其执行器或选择器。资源名来源订阅的 [MIT 许可](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/LICENSE) 已保留在 APK 的 `assets/third_party_notices.txt`。AdClose 的许可和传感器模块分析见 [SENSOR_RESEARCH.md](SENSOR_RESEARCH.md)，本轮没有复制其实现。

## 旧版为什么漏检

旧版主要依赖窗口/内容事件；首次检查没有候选便不再轮询。SDK 异步显示控件但不发有效事件时，清屏看不到新控件。8秒入口窗口、右上区域、过于严格的同包资源 ID、只支持 `ACTION_CLICK`，也会排除可关闭的真实广告。广告标识只存在图片中时，文本广告特征可能不可读。

0.2.0 扩展了窗口与候选条件，支持精确页面/ID/描述规则、短时诊断、导入前预览，以及小控件中心手势。通用“关闭”不会自动执行；完全不暴露节点的画布仍不会盲点。动作最多接受两次，结果确认后停扫；识别到广告、手势提交成功及传感器回调都不直接充当拦截次数。

## 对无 Root 华为、vivo 的实际意义

这些改动能扩大“有可访问关闭控件”的适配范围，并帮助用户为未覆盖页面建立本地规则。它们不会关闭其他 APP 的运动传感器，不保证快速摇晃时抢在跳转之前关闭广告，也不会把不同版本、不同厂商的表现视为已验证。

本轮未接入自动下载规则、脚本执行、Shizuku、VPN 或 Root。清屏 JSON 与 GKD 的高级选择器语法不同；导入不支持的格式会报错，不会默默忽略条件。真实识别率与华为/vivo 后台持续运行仍需按具体系统版本做设备测试。
