# 参考项目、代码来源与许可记录

[English](REFERENCES.en.md) · [返回项目介绍](../README.md)

本页记录清屏 0.4.1 的第三方来源与实际使用范围，核对日期为 2026-10-23。链接固定到研究时读取的提交，后续上游变更不自动进入清屏。参考项目的功能、规则数量或设备测试结果不代表清屏已达到相同覆盖率。

清屏少量内置控件资源信息来自 **ganlinte/GKD-subscription 的 MIT 规则**；匹配条件、动作执行、风险判断、会话预算和结果确认由清屏实现。其他下列项目用于对照公开行为、Android API 使用方式或技术边界，没有复制其客户端执行器、模块、模型或依赖。

## 来源总表

表中许可只描述对应固定提交所见的文件，不为清屏整体指定许可证，也不自动覆盖上游另行授权的模型、依赖或其他资产。

| 上游项目 | 固定提交与许可证据 | 参考内容 | 清屏采用与未采用的范围 |
| --- | --- | --- | --- |
| [ganlinte/GKD-subscription](https://github.com/ganlinte/GKD-subscription) | [`4746d192`](https://github.com/ganlinte/GKD-subscription/tree/4746d192b4c57a4b62ed92a7f5deaf79e64fa811) · [MIT](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/LICENSE)，Copyright (c) 2024 ganlinte | 明确来源 APP 的开屏 ID、SDK 关闭资源名、广告关闭文案及页面约束。 | **实际沿用部分规则信息并重新表达。** 保留来源包、完整 ID、语义和页面等条件；不导入整份订阅、不执行 GKD 选择器或脚本。完整 MIT 声明随源码和 APK 保留。 |
| [GKD](https://github.com/gkd-kit/gkd) | [`fe6736fb`](https://github.com/gkd-kit/gkd/tree/fe6736fb083ba90d7bace124e30b61ff6e5701f2) · [GNU GPL v3](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/LICENSE) | 节点点击失败后使用节点中心手势、动作后重扫、普通无障碍与特权节点路线的区别。 | 依据公开行为与 Android API 独立实现有条件的节点点击、手势后备与观察。未复制选择器、动作执行器、隐藏 API 工具或特权服务。 |
| [开屏跳过 / Android-Touch-Helper](https://github.com/zfdang/Android-Touch-Helper) | [`d896dc13`](https://github.com/zfdang/Android-Touch-Helper/tree/d896dc13954cdeaf7f6a71e6201cc01f9e751b77) · [MIT](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/LICENSE)，Copyright (c) 2020 Zhengfa Dang | 拆分文字、可点击父节点、事件合并、解锁后复查及服务测试设计。 | 独立实现受尺寸和危险动作约束的短文字合并、父节点检查和解锁重扫。没有复制其实现，未采用保存坐标盲点、宽泛包含匹配或原项目的局部事件队列。 |
| [Ad Skipper](https://github.com/madeye/ad-skipper) | [`2734cb1b`](https://github.com/madeye/ad-skipper/tree/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5) · [根 LICENSE 为 MIT](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/LICENSE)，Copyright (c) 2026 Max Lv | 节点 → OCR → 图像模型的分层检测、广告证据门槛、小型命中区域与动作前来源复核。 | 用于评估未来可选的图像后备方案。本版未集成截图、OCR、YOLO、VLM、模型、本机库或该项目代码；不依据根许可证推断模型和依赖的授权。 |
| [AdClose](https://github.com/zjyzip/AdClose) | [`11f9e554`](https://github.com/zjyzip/AdClose/tree/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1) · [CC BY-NC-SA 4.0](https://github.com/zjyzip/AdClose/blob/11f9e5547ec1e89ad7daf7114d05a0b02aa96de1/LICENSE) | 在目标进程 Hook 加速度计、陀螺仪注册的技术路线及副作用。 | 仅研究能力边界，未复制模块或代码，未把非商业许可材料当作 MIT 使用。本版不依赖 Xposed/LSPosed，不阻断其他 APP 的传感器注册。 |
| [SkipAds](https://github.com/KonghuanSmart/SkipAds) | [`2ad5afa4`](https://github.com/KonghuanSmart/SkipAds/tree/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c) · [完整文件树](https://api.github.com/repos/KonghuanSmart/SkipAds/git/trees/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c?recursive=1) 未见 LICENSE / COPYING / NOTICE | 简单包名规则、白名单、窗口事件处理与提前计数的问题。 | 仅对照公开实现事实与局限，未复制源码、资源或规则。清屏不会在点击请求前计为成功，不因可见源码便推定已获复用许可。 |
| [Lin-arm/GKD_subscription](https://github.com/Lin-arm/GKD_subscription) | [`2a715456`](https://github.com/Lin-arm/GKD_subscription/tree/2a71545641335ca1e37fa3c57452a295c8b01c46) · [完整文件树](https://api.github.com/repos/Lin-arm/GKD_subscription/git/trees/2a71545641335ca1e37fa3c57452a295c8b01c46?recursive=1) 未见 LICENSE / COPYING / NOTICE；[README 有使用及传播声明](https://github.com/Lin-arm/GKD_subscription/blob/2a71545641335ca1e37fa3c57452a295c8b01c46/README.md) | APP 内较晚开屏、连续广告、可见性与搜索框误触等公开适配事实。 | 仅作问题对照，未复制规则库、完整规则或选择器组合。进入清屏的相关资源 ID 均另有上表 ganlinte MIT 来源支持，不将该仓库视作 MIT 数据源。 |
| [pwh-pwh/fuck_shake](https://github.com/pwh-pwh/fuck_shake) | [`34a20bad`](https://github.com/pwh-pwh/fuck_shake/tree/34a20bad084fbc54ce4a776acbf0809defe8d02b) · [MIT](https://github.com/pwh-pwh/fuck_shake/blob/34a20bad084fbc54ce4a776acbf0809defe8d02b/LICENSE)，Copyright (c) 2023 coderpwh | Xposed 替换传感器注册方法，以及未区分传感器类型的影响。 | 仅研究，没有复制或集成模块。普通清屏 APK 不因此获得跨进程 Hook 能力。 |
| [DisableStartActivityOnSensorChanged](https://github.com/ldy19941102/DisableStartActivityOnSensorChanged) | [`a694dd36`](https://github.com/ldy19941102/DisableStartActivityOnSensorChanged/tree/a694dd367cb22c04fe83ddca8b82e7796e7dd57f) · [完整文件树](https://api.github.com/repos/ldy19941102/DisableStartActivityOnSensorChanged/git/trees/a694dd367cb22c04fe83ddca8b82e7796e7dd57f?recursive=1) 未见 LICENSE / COPYING / NOTICE | 动态发现传感器监听器、在限定时间内吞掉回调的另一条 Hook 路线。 | 仅对照公开技术事实，未复制源码、资源或实现。仓库名称不作为其已拦截 Intent 或可供普通 APK 使用的证据。 |
| [Shizuku-API](https://github.com/RikkaApps/Shizuku-API) | [`a27f6e41`](https://github.com/RikkaApps/Shizuku-API/tree/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5) · [MIT](https://github.com/RikkaApps/Shizuku-API/blob/a27f6e4151ba7b39965ca47edb2bf0aeed7102e5/LICENSE)，Copyright (c) 2021 RikkaW | Binder 代理、独立 UserService 与 ADB/shell、root 权限边界。 | 仅用于核对能力边界，未引入依赖或复制代码；清屏不要求 Shizuku，也不将 shell 服务解释为进入其他 APP 进程的注入框架。 |

以上三个“未见许可证”的结论来自对应固定提交的完整、未截断递归文件树，只描述该次核对结果；它们不等于获得复制、修改或再分发授权。

## 实际进入产品的 MIT 规则信息

清屏没有打包第三方规则执行器。以下信息被重新表达为 Java 中的有限本地判断：

| 清屏实现 | 沿用信息与原始出处 | 清屏自己的约束 |
| --- | --- | --- |
| [`BuiltinSplashRules.java`](../app/src/main/java/cn/qingping/app/engine/BuiltinSplashRules.java) | 8 个来源 APP、9 个完整资源 ID。原始文件：[哔哩哔哩](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bili.ts)、[HD](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bilibilihd.ts)、[知乎](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.zhihu.android.ts)、[中国电信](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.ct.client.ts)、[TeraBox](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.dubox.drive.ts)、[华为视频](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.huawei.himovie.ts)、[大麦](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/cn.damai.ts)、[UC](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.UCMobile.ts)。 | 严格隔离来源包和完整 ID，并结合可见性、尺寸、跳过语义、前台窗口和危险动作检查。个别入口空标签后备属于清屏自己的受限扩展，不声称上游已验证该扩展。 |
| [`AdDecisionEngine.java`](../app/src/main/java/cn/qingping/app/engine/AdDecisionEngine.java)、[`AdControlPolicy.java`](../app/src/main/java/cn/qingping/app/engine/AdControlPolicy.java) | 明确开屏 SDK 资源名及五项条件关闭资源名；实际文件与页面条件见 [SDK 来源记录](SDK_RESEARCH_0.3.md)，原有开屏资源信息见 [首次来源记录](GITHUB_RESEARCH.md)。 | 不对任意 `close` / `skip` ID 做全局点击；条件资源名需要独立广告证据，部分还需完整 Activity。模拟点击受逐 APP 和精确规则授权限制，动作请求与关闭结果分开记录。 |

这些条目是来源记录，不是“只支持 8 个 APP”的列表，也不证明对应 APP 的每个版本已通过真机测试。清屏另有通用广告语义和用户精确规则，实际效果取决于目标界面、公开节点与系统限制。

原版权与完整 MIT 许可已保存在 [`app/src/main/assets/third_party_notices.txt`](../app/src/main/assets/third_party_notices.txt)，并作为同名 asset 随 APK 分发。再次分发这些规则信息时，应一并保留该文件中的版权和许可声明。清屏使用自己的规则格式，不直接兼容 GKD 订阅。

## 研究证据与实现边界

- 代码对照与固定文件：[GitHub 初始研究](GITHUB_RESEARCH.md)、[工程比较](PROJECT_COMPARISON.md)、[开屏目录](SPLASH_CATALOG_RESEARCH.md)、[SDK 条件规则](SDK_RESEARCH_0.3.md)。这些历史记录包含旧版行为，当前功能以项目介绍、源码和本版验证记录为准。
- 节点点击及特权路线：[0.4 适配研究](BILIBILI_RESEARCH_0.4.md)。[上游连续广告报告](https://github.com/AIsouler/GKD_subscription/issues/1280) 与 [GKD #1387](https://github.com/gkd-kit/gkd/issues/1387)、[GKD #1417](https://github.com/gkd-kit/gkd/issues/1417) 只用作问题背景，没有把报告、附件或上游测试结果当作清屏验收数据。
- 未采用的能力：[传感器 Hook 研究](SENSOR_RESEARCH.md)、[图像后备研究](VISUAL_DETECTION_RESEARCH.md)。普通无障碍点击不等于阻断传感器，也不能为目标广告创造不存在的关闭按钮。

引用和致谢不表示上游作者参与、审查或认可清屏。本页不以任何上游项目的许可替清屏整体选择开源许可证；清屏本身的使用与再分发条件应以仓库另行明确的授权为准。
