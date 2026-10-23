# 0.4：华为哔哩哔哩开屏漏检与 GitHub 实际规则核对

核对日期：2026-10-23。用户报告 0.3.0 在无 Root 华为手机的哔哩哔哩开屏无法跳过。本页只记录已读取的上游规则和执行器事实、可实施的修正及证据边界；没有该手机的节点快照或真机复现，不能将下面任一漏检路径直接确认为用户手机的唯一原因。

## B 站规则不是只搜“跳过”

| 已读固定文件 | 实际做法 | 对清屏的意义 |
| --- | --- | --- |
| [ganlinte/GKD-subscription，4746d192，B 站文件第 7–26 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bili.ts#L7-L26) | 来源包 `tv.danmaku.bili`，`TextView`；完整资源 ID 分别为 `tv.danmaku.bili:id/count_down` 与 `tv.danmaku.bili:id/skip`，同时文字以“跳过”开头。开屏组没有 Activity 白名单，启用快速查询、10 秒匹配时间、每次切 APP 最多一次动作。 | `count_down` 不能仅凭普通通用 `*_skip` 命名猜出；可将完整 ID + 来源包作为 B 站内置识别事实。没有要求页面额外显示“广告”徽标，清屏若对每个按钮都硬性要求独立广告文案，会漏掉一些已有可靠来源 ID 的开屏控件。 |
| [Lin-arm/GKD_subscription，2a715456，B 站文件第 7–29 行](https://github.com/Lin-arm/GKD_subscription/blob/2a71545641335ca1e37fa3c57452a295c8b01c46/src/apps/tv.danmaku.bili.ts#L7-L29) | 当前社区规则按 B 站 `vid=count_down/skip` 且 `visibleToUser=true` 匹配，不要求 TextView 或跳过文字，`matchRoot=true`。注释移除了 10 秒 `matchTime`：从桌面小组件进入视频后返回首页也可能出现开屏；允许两次动作，防连续两次开屏。 | 说明只认文字、只扫局部事件节点、一次成功后结束、仅入口时间窗，都会形成真实适配漏洞。上游没有为普通文字“跳过”做全局无条件点击；其注释指出模糊文字包含匹配会误触搜索框。清屏可独立实现来源与完整 ID 约束、完整根树复查、后期开屏与连续广告观察。 |

ganlinte 的 `main` 在本次核对时仍是 `4746d192b4c57a4b62ed92a7f5deaf79e64fa811`，提交日期 2026-09-24；B 站文件 blob 为 `45e9afe32a0d11c5d088ddf2e5fa720785d7f0d0`。因此本次不是从该仓库得到比 0.3 研究所用提交更新的 B 站规则。Lin 固定提交 `2a71545641335ca1e37fa3c57452a295c8b01c46` 的提交日期为 2026-10-23，B 站文件 blob 为 `0b47134993aa9a3f09001c10cc5a68e415706ec7`。

两份 B 站开屏组均没有把 Activity 锁死在 `HotSplashActivity`。Lin 文件其它组确有 `.ui.splash.ad.page.HotSplashActivity`，但那是更新提示、分享稿件弹窗；不能把其它组的页面条件冒充开屏规则已经证明的限制。清屏应使用明确来源 + 完整资源 ID + 小尺寸安全控件等自己的条件，不将任意 APP 的 `skip/count_down` 当成广告。

Lin 注释引用的 [AIsouler/GKD_subscription issue #1280](https://github.com/AIsouler/GKD_subscription/issues/1280) 已直接读取：2025-10-20 的报告描述“侧边返回退出 B 站而不杀进程，一分钟后重开”，订阅 v320 跳过第一条但漏掉第二条，并附两份 MainActivityV2 快照。这是连续两条场景的真实上游报告，不是清屏真机复现；本次没有下载或读取其附件中的完整节点树。

## 按钮存在却点不动：上游动作有后备

已读 [GKD `GkdAction.kt`，固定提交 fe6736fb](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/gkd-app/src/androidMain/kotlin/li/gkd/app/data/GkdAction.kt)。其默认 `Click` 在 `node.isClickable` 时先请求 `ACTION_CLICK`；不可点击或该动作返回 false，则进入 `ClickCenter`。后者先使用已就绪的特权 `tap`，没有特权时退回无障碍 `dispatchGesture`，手势时长用系统 `ViewConfiguration.getTapTimeout()`。所以“使用节点范围中心手势”本身不要求 Root 或 Shizuku。

该执行器在 `ACTION_CLICK=true` 时立即返回，不检查广告是否消失。这个动作结果不适合直接作为清屏的成功统计。清屏应继续区分“请求已受理”与“控件消失/页面改变后的确认”，在允许模拟点击时才考虑经过重新校验的小型关闭控件手势，不使用固定盲点坐标。真机还需验证系统是否接受该手势，以及弹窗遮挡、控件延迟、动作返回成功但界面不变等路径。

## 不能把 GKD 的节点可见性能力全部归到普通无障碍

已读 [GKD `a11y_info.xml`](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/gkd-android/src/main/res/xml/a11y_info.xml)：它请求报告资源 ID、包括不重要视图、检索交互窗口、取窗口内容、手势；`isAccessibilityTool` 引用构建资源。其普通 [A11yService](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/gkd-app/src/androidMain/kotlin/li/gkd/app/service/A11yService.kt) 的节点来源包括 `rootInActiveWindow` 与 `windows`。

另外，[`UiAutomationServiceInfo.kt` 第 25–27 行](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/gkd-app/src/androidMain/kotlin/li/gkd/app/priv/UiAutomationServiceInfo.kt#L25-L27) 的特权 UiAutomation 路线，在 Android 14+ 通过隐藏 API 设置 `setAccessibilityTool(true)`。这与普通 `dispatchGesture` 是不同问题。不能由“B 站选择器没有写 Shizuku”推导“所有手机上的节点一定向普通无障碍公开”，也不能给清屏伪造辅助残障用途或无提示调用隐藏接口来承诺穿透限制。有关平台条件应结合当前服务配置、系统版本与节点诊断再判断。

还直接读取了 [GKD issue #1387](https://github.com/gkd-kit/gkd/issues/1387)：报告者使用 Sui/Shizuku 自动化模式、关闭普通无障碍后，包含 bilibili 的多款 APP 无法跳过；[issue #1417](https://github.com/gkd-kit/gkd/issues/1417) 则报告另一 APP 普通无障碍可跳、启用 Shizuku 优化后反而漏检。这些仅是用户报告，未据此认定根因，但足以说明“再加一个特权开关”不应被当作无须验证的全覆盖解法。

## 许可核对与实现约束

- ganlinte 固定提交的 [LICENSE](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/LICENSE) 为 MIT，Copyright (c) 2024 ganlinte。清屏 `assets/third_party_notices.txt` 已保留完整声明。其两个 B 站完整资源 ID 可按该来源标注，自己的匹配与执行安全策略独立实现。
- Lin 固定提交根 `LICENSE` 返回 404；本次读取其完整、非截断递归树，未发现 LICENSE、COPYING 或 NOTICE 文件。其 [README](https://github.com/Lin-arm/GKD_subscription/blob/2a71545641335ca1e37fa3c57452a295c8b01c46/README.md) 有学习交流与传播限制声明。故不把它视作 MIT 数据源，不复制其完整规则、选择器组合或规则库进入清屏；本页只对比可公开核实的资源名与失效情形，使用已有 MIT 来源支持的事实独立设计。
- GKD 客户端 [LICENSE](https://github.com/gkd-kit/gkd/blob/fe6736fb083ba90d7bace124e30b61ff6e5701f2/LICENSE) 为 GPLv3。本页研究执行路径，未将其选择器引擎、隐藏 API 工具或动作执行器复制到清屏。

## 本轮应优先实现与验证

1. 为 B 站加来源包与完整资源 ID 的内置适配；检查可见、enabled、尺寸、屏内位置和危险子树。对有非跳过业务文案的同 ID 控件拒绝；无文字变体若允许，必须同时满足严格来源 ID 和几何条件。该无文字范围是清屏自己的审慎扩展，MIT 旧规则本身仍要求“跳过”前缀，不能声称旧规则已经直接验证空标签。
2. 同一来源 ID 的匹配不能被缺少额外“广告”字样一票否决；同时保留广告下载、支付、登录、分享等危险动作拒绝。该豁免只给已核实的明确来源控件，不能给通用 `close/skip`。
3. 重新核对完整根树、异步节点更新和持续会话；为超过入口窗口才出现、返回首页才出现、连续两次出现建立测试。原有结果确认与每 APP 统计继续保留。
4. 对节点点击失败与点击返回 true 但不消失分别诊断；只有用户已允许且重新校验通过时使用无障碍节点中心手势。单元测试和 fixture 能验证代码分支，不能代替用户华为的真实广告验收。

本页不证明所有版本 B 站、全部华为系统、任何未命名 APP 都暴露这些资源 ID。没有可访问控件或广告根本无跳过功能时，不以点击推广、退出整个 APP 或把自然倒计时结束计为自动成功代替适配。

## 0.4.0 已实施的范围

`BuiltinSplashRules` 已加入 B站主版两 ID 与 HD：完整来源、完整 ID、短跳过语义及小型边缘控件；仅两版 B站在进入前 15 秒另允许文字/描述同时为空的受限后备，有非跳过业务文案仍拒绝。可读短跳过的 B站已核实 ID 支持同 APP 后期开屏；不按其它弹窗组猜 Activity。该空标签范围是清屏独立设计的审慎扩展，非 MIT 主版旧规则的原有条件。

服务已请求公开的非重要视图 flag、对来源目录做有上限的优先 ID 查询、修复危险聚合描述剪整树，并在既有逐 APP 手势授权下提供语义点击超时后的稳定控件手势升级。没有复制上游选择器执行器或依赖特权的节点路线。最终本地构建与逻辑测试见 `VERIFICATION.md`，实际 B站与跨 ROM 验收仍待执行。
