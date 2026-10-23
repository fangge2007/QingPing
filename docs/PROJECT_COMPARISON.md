# 可参考的 GitHub 项目：实际源码比较

核对日期：2026-10-23。通过 GitHub 连接器读取仓库元信息、固定提交、源码与许可。本页补充原有 GKD、ganlinte 订阅及 AdClose 分析；比较代码设计，不以星数、README 宣传或其他设备数据推断清屏覆盖率。未将下列项目的客户端实现复制进清屏。

## 项目与许可

| 项目 | 本次固定提交 | 许可核对 | 主要价值 |
| --- | --- | --- | --- |
| [开屏跳过 / Android-Touch-Helper](https://github.com/zfdang/Android-Touch-Helper) | `d896dc13954cdeaf7f6a71e6201cc01f9e751b77`，提交日期 2026-10-19 | [MIT，Copyright (c) 2020 Zhengfa Dang](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/LICENSE) | 无障碍关闭、拆分文字、可点击父级、事件队列和解锁恢复，适合优先研究。 |
| [Ad Skipper](https://github.com/madeye/ad-skipper) | `2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5` | [根 LICENSE 为 MIT](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/LICENSE)，模型与依赖另行核对 | 节点不足时按顺序尝试 OCR / 图像检测；详细实际代码证据及本版取舍见 [VISUAL_DETECTION_RESEARCH.md](VISUAL_DETECTION_RESEARCH.md)。 |
| [SkipAds](https://github.com/KonghuanSmart/SkipAds) | `2ad5afa4594cbfc210fa2459d6e4b9b29529db0c`，提交日期 2022-07-20 | 该提交非截断递归文件树未见 LICENSE / COPYING / NOTICE，GitHub 元信息 `license=null`；只能称公开源码参考，不能据此认为允许复制。 | 简单无障碍方案的对照：可理解按包名规则、白名单及状态页，但有明显需要避免的旧实现。 |

前两项是本轮新核对的有许可项目，均使用普通 Android 无障碍路线。第三项帮助判断“某项目有源码”与“代码质量、复用授权、真实效果已证明”之间的区别。AdClose 的目标进程传感器 Hook 仍属另一条依赖 Xposed 的技术路线，不混为无 Root 功能。

## 开屏跳过：值得采用的具体思路

本次主要读取 `TouchHelperServiceImpl.java`、`SkipAdRules.java`、无障碍服务测试和 Manifest，均使用上表固定提交。

| 实际代码 | 优点 | 对清屏的具体意义及安全条件 |
| --- | --- | --- |
| [SkipAdRules.java 第 77–125 行](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/SkipAdRules.java#L77-L125)：分隔符归一化、组合多个子节点短文字。适配层 [第 845–889 行](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/TouchHelperServiceImpl.java#L845-L889) 限定 2–5 个子节点及容器大小。 | 能发现节点分别显示“跳”“过”，或者文字与计时被拆成多个节点的控件。清屏旧单节点正则会漏掉这一类。 | 本轮据此独立设计小型可点击容器的短叶子文字合并；只认明确广告跳过 / 关闭语义，检查同包、可见、非输入、无下载支付等危险子节点。不把原项目宽泛包含匹配直接变成清屏全局规则。最终实现与验证以清屏源码、`VERIFICATION.md` 为准。 |
| [TouchHelperServiceImpl.java 第 969–1005、1019–1052 行](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/TouchHelperServiceImpl.java#L969-L1052)：标签点击失败后寻找可点击祖先，然后再考虑中心手势。 | 解释了“按钮文字可见却点不动”的常见原因：监听器位于父容器。 | 清屏保留自己的至多两级父节点检查、较小几何范围、危险子树拒绝和逐 APP 模拟点击授权。不能直接把祖先搜索放宽到全屏容器，也不因第一次 ACTION_CLICK 返回 true 就判断关闭成功。 |
| [同文件第 550–604 行](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/TouchHelperServiceImpl.java#L550-L604)：相同来源事件去重、最多三个待处理内容子树、丢弃后补整窗扫描。 | 避免高频无障碍事件让陈旧树排队，导致真正新出现的关闭按钮迟迟没被检查。 | 这是后续性能优化方向。清屏本版采用主线程任务合并、单树 700 节点 / 24 层及后期约 1.5 秒低频观察；未移植该工作线程和局部队列。要引入局部扫描，必须保留完整清屏观察供结果确认。 |
| [同文件第 1231–1279 行](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/TouchHelperServiceImpl.java#L1231-L1279)：解锁后立即再检查，并延迟约一秒复查热启动页面。 | 部分 SDK 在 APP 恢复后才挂载开屏广告；只靠首次窗口事件会漏检。 | 清屏已加入锁屏停止与用户解锁后重扫；仍需华为 / vivo 的真实解锁、清后台和系统重绑测试。不能把任何项目的事件接收写法当作厂商一定恢复的保证。 |
| [服务测试第 315–581、602–854、878–1028、1090–1150 行](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/test/java/com/zfdang/touchhelper/TouchHelperServiceImplTest.java)：队列、切包、失效手势、旧会话、解锁与拆分文字测试。 | 不只测纯匹配函数，也测试无障碍适配层与生命周期。 | 值得继续扩展清屏的适配层测试。当前纯 Java JUnit 与已编译 fixture 不替代这些 Android 行为的设备验收。 |

该项目也有需要保留判断的地方：[规则字段匹配](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/SkipAdRules.java#L163-L186) 可以按坐标、ID、描述、文字之一命中，而清屏精确规则对所有已配置字段同时匹配；[位置点击路线](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/TouchHelperServiceImpl.java#L450-L480) 会按保存位置模拟点击，清屏不引入盲点坐标。原项目 [按配置时长停止开屏过程](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/java/com/zfdang/touchhelper/TouchHelperServiceImpl.java#L1294-L1311)，不能原样解决用户长时间使用后出现弹窗的问题。

上游 [Manifest](https://github.com/zfdang/Android-Touch-Helper/blob/d896dc13954cdeaf7f6a71e6201cc01f9e751b77/app/src/main/AndroidManifest.xml) 未声明联网权限，应用清单通过针对性查询实现。这里核对的是声明事实，不沿用 README 中“绝无隐私问题”等绝对保证。清屏继续保留自己的最小权限、输入子树排除和短期诊断策略。

## SkipAds：可读，但不宜直接迁入

实际读取 [SkipAdsService.java](https://github.com/KonghuanSmart/SkipAds/blob/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c/app/src/main/java/com/konghuan/skipads/service/SkipAdsService.java)、[KeepAliveService.java](https://github.com/KonghuanSmart/SkipAds/blob/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c/app/src/main/java/com/konghuan/skipads/service/KeepAliveService.java) 和 [Manifest](https://github.com/KonghuanSmart/SkipAds/blob/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c/app/src/main/AndroidManifest.xml)。

- 有来源包名规则和白名单，结构简单，适合理解最基础的本地设置组织。清屏已有逐 APP 授权、规则和分来源统计，继续保留。
- 服务 [第 72–82、88–111 行](https://github.com/KonghuanSmart/SkipAds/blob/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c/app/src/main/java/com/konghuan/skipads/service/SkipAdsService.java#L72-L111) 主要处理 Activity 窗口状态事件；配置了点击事件但代码只进入窗口状态分支，没有内容变化扫描与迟到关闭观察，不能作为解决应用内连续广告的充分方案。
- 有规则时 [第 103–109 行](https://github.com/KonghuanSmart/SkipAds/blob/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c/app/src/main/java/com/konghuan/skipads/service/SkipAdsService.java#L103-L109) 在延迟查找控件前就增加次数；文字路线也未用后续界面确认结果。这种计数不能迁入清屏，用户要求的统计需以结果观察为依据。
- 文字匹配 [第 111–142 行](https://github.com/KonghuanSmart/SkipAds/blob/2ad5afa4594cbfc210fa2459d6e4b9b29529db0c/app/src/main/java/com/konghuan/skipads/service/SkipAdsService.java#L111-L142) 直接查含“跳过”的文本并请求点击，没有清屏目前的严格广告语义、位置、危险控件和当前窗口再校验组合。
- `KeepAliveService` 使用前台通知，而 Manifest 声明其类型为 `location`。这只能证明代码尝试维持服务，不能证明华为 / vivo 清后台后仍活着，也不代表该旧配置适合今天的 Android。清屏不为保活增加无关位置用途，也不把前台服务当作无障碍授权恢复保证。
- 未找到明确许可证，因此只研究公开行为和缺陷，不复制其实现、资源或规则数据；不把“GitHub 可见”视为获得重用授权。

## 对用户当前问题的结论

最值得参考的现成工程是 **开屏跳过的节点适配和测试设计**，以及 **Ad Skipper 的可选本地图像后备架构**。这两者解决不同漏检路径：前者帮助“有节点但文字拆开 / 点击层不对”，后者研究“关闭文字只在图片中”。清屏当前继续优先完善前者；截图、OCR、YOLO、VLM 本版均未接入，具体原因和下一步见 [图像研究](VISUAL_DETECTION_RESEARCH.md)。

新增五个 SDK 条件资源名的依据另见 [SDK_RESEARCH_0.3.md](SDK_RESEARCH_0.3.md)，原有 GKD / AdClose 分析见 [GITHUB_RESEARCH.md](GITHUB_RESEARCH.md)。这些项目都不能替目标广告创造原本不存在的跳过控件；摇一摇系统传感器控制也不能从普通无障碍项目中直接获得。
