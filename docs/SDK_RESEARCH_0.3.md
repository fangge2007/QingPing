# 清屏 0.3：按广告类型与 SDK 家族核对源码

核对日期：2026-10-22。通过 GitHub 连接器检索并读取固定提交的完整源文件。本页记录实际源码证据与安全实施条件；订阅中的匹配成功记录不等于清屏在真实设备通过验收，也不保证这些资源名在 SDK 后续版本继续存在。

## 共同来源及许可

本轮用于数据的来源统一为 [ganlinte/GKD-subscription 固定提交 4746d192b4c57a4b62ed92a7f5deaf79e64fa811](https://github.com/ganlinte/GKD-subscription/tree/4746d192b4c57a4b62ed92a7f5deaf79e64fa811)，[LICENSE 为 MIT](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/LICENSE)，版权声明为 Copyright (c) 2024 ganlinte。APK 已有 `assets/third_party_notices.txt` 保留完整 MIT 正文；本轮只参考资源名、明确广告文案及页面约束事实，不复制 GKD 客户端执行器或高级选择器。

GitHub 全局检索还命中了其他订阅、自动化脚本及反编译资源仓库。未以其授权不清的实现作为移植来源，也未把点击广告、获取奖励的脚本流程纳入清屏。下表每项均重新读取上述 MIT 仓库的固定文件核实。

## 0.3.0 已实现的五个条件资源名

下表五个 ID 已进入 `AdControlPolicy`，通过 Android 适配层与现有三项开屏 SDK ID 配合工作。实现采用精确名称比较，未导入整份第三方订阅。这里的“已实现”指代码路径，真实 APP 覆盖仍待设备验收。

| SDK / 控件 | 固定源码事实 | 清屏 0.3.0 的条件 |
| --- | --- | --- |
| 字节 / 穿山甲 `tt_reward_full_count_down_after_close` | [曲奇云盘规则第 11–15、33–36 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.quqi.quqioffice.ts#L11-L36) 在 SDK Activity 中匹配该 ImageView；[天府通第 10–23 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.chinarainbow.tft.ts#L10-L23) 同时证明 `com.byted.pangle.m` 命名空间；[七猫第 87–110 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.kmxs.reader.ts#L87-L110) 将其描述为视频广告 X，且该组默认关闭、明确提示可能误触。 | 精确资源名 + `com.bytedance.sdk.openadsdk.stub.activity.Stub_Standard_Portrait_Activity` + 独立广告证据。等待节点出现、可见且 enabled 后才动作。不能只见到倒计时容器就点击；不能自动确认放弃奖励或退出 APP。 |
| 快手 `ksad_auto_close_btn` | [喵趣漫画第 79–90 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.paokeji.yiqu.ts#L79-L90) 在“快手 SDK”子规则明确匹配该资源名。其外层 [第 28–36 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.paokeji.yiqu.ts#L28-L36) 是多个 SDK 与宿主页面的集合，并非只有快手 Activity。 | 精确资源名 + 独立广告证据 + 小型关闭控件；不能将 `ksad_` 前缀中任意控件视为关闭。若严格限定 SDK Activity，覆盖面会小于原规则；原源码不能证明所有 Kwad Activity 都暴露该 ID。 |
| JAD 家族 `jad_feed_close` | [下厨房第 19–26 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.xiachufang.ts#L19-L26) 在菜谱详情页卡片广告匹配可见节点；[彩云天气第 36–45 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.nowcasting.activity.ts#L36-L45) 在另一宿主页面使用相同资源名。 | 精确资源名 + 独立广告证据 + 小型关闭控件。允许卡片中央位置，仍禁止全屏父容器。这里从资源名和跨宿主使用推断 SDK 家族，不能据此声称覆盖其全部 SDK 产品。 |
| 美数 `ms_activity_sdk_interstitial_cacel` | [PU 口袋校园第 90–95 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.xyhui.ts#L90-L95) 明确记录美数广告和 `com.meishu.sdk.activity.SdkInterstitialActivity`。注意实际 ID 的 `cacel` 拼写。 | 精确资源名 + 该完整 SDK Activity + 独立广告证据。不能扩大为任意 `cancel`、`close`。 |
| Anythink / TopOn 家族 `anythink_myoffer_btn_close_id` | [小组件盒子第 32–37 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/io.iftech.android.box.ts#L32-L37) 匹配 `com.anythink.basead.ui.ATPortraitTranslucentActivity` 的此 ID；[趣智校园第 13–20 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.klcxkj.zqxy.ts#L13-L20) 在另一个宿主命名空间使用同 ID。 | 精确资源名 + 该完整 SDK Activity + 独立广告证据。上游部分规则把广告来源称作“百青藤”；资源名和 Activity 本身只证明 Anythink 家族，不把聚合供应来源名称直接当 SDK 归属。 |

所有候选还必须经过：用户已开启的来源 APP、当前前台包与窗口复核、可见且 enabled、非输入子树、节点及父级边界检查、动作频率限制。资源 ID 的 Android 命名空间可以是宿主包或 SDK 包；资源名相同也不允许跨前台包执行。

“独立广告证据”独立于新增候选资源名：适配层需从可见文本、描述或另一个原有已核实开屏控件获得广告上下文，不能因发现这五个候选 ID 本身就把该页的广告上下文设为真。如果节点本身已有“关闭广告”等明确语义，会走明确广告文案路径；用户配置的有效精确规则也保留其独立授权路径。SDK Activity 与资源名组合提供额外约束，但不把下载、购买、领取奖励、反馈、不感兴趣等动作纳入关闭白名单。模拟点击继续由来源 APP 设置控制，只对已核实控件或明确授权的精确规则回退。

## 持续观察与候选范围

0.3.0 在用户选定的来源 APP 位于前台、屏幕解锁且保护授权有效时持续观察。进入时约每 250 毫秒检查，默认密集期 15 秒；有精确规则时密集期可延至规则的 30 秒上限。之后约每 1.5 秒重扫，窗口和内容事件可提前触发；不是 15 秒后停止检测，也不是只在已发现广告时才继续轮询。每棵树仍限制 700 个节点、24 层；停止保护、撤销授权、离开选定 APP 或锁屏后不继续这条轮询链。持续观察会增加前台耗电，实际耗电和厂商后台稳定性尚需设备测量。

观察持续不等于所有候选都永久有效。普通“跳过” / “Skip”与业务按钮难以区分，仍只在进入后的前 15 秒结合广告上下文和边缘位置处理；15 秒后仅明确广告文案、已核实 SDK 控件和用户显式启用的 `persistent` 精确规则可继续产生候选。旧精确规则默认仍受原 3–30 秒进入窗口限制，旧单 ID 设置也保留 15 秒范围。

广告轮次在完整、无广告证据及关闭控件的清屏状态持续至少 500 毫秒后重置。每轮最多接受 4 次点击，同一个控件先最多接受两次快速尝试，间隔至少 10 秒后可补试最后一次。多个阶段共用预算；没有稳定清屏就不重置失效控件的预算，也不因单个按钮变化提前计数。只有动作后的两次完整清屏观察才确认“跳过”。广告之间若没有能观察到的清屏间隔，仍可能被视为同一轮。

跳转返回使用单独的较窄证据：普通静态“广告”标识不足以启用返回，需当前观察到摇动提示或原有三项开屏 SDK 控件，并且来源与指定目标匹配、未临时放行。它仍是跳转后返回，不是跳转前阻断。

## 倒计时、中央弹窗和连续广告

1. **倒计时后才可关闭**：SDK 可能稍后才插入关闭 ImageView，或先显示 disabled 控件。原来 15 秒入口窗口与成功后停止本轮，会漏掉较晚出现的关闭按钮。新版持续低频观察已选前台 APP，disabled / 锁定文案节点只提供“仍存在”证据，不触发点击，也不当作控件已消失。
2. **长期使用中的弹窗 / 视频**：开屏与局部广告不是一个会话。MIT 订阅的 [全局开屏规则第 8–18 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/globalGroups.ts#L8-L18) 限定 10 秒和最多两次；[番茄小说卡片广告、视频广告规则](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.dragon.read.ts#L128-L281) 是独立的页面规则。新版保留入口扫描，通过持续低频观察及内容 / 窗口事件处理后续广告，清屏后允许新轮次；每轮动作预算仍有上限。
3. **中央明确广告关闭**：[番茄小说第 181–186 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.dragon.read.ts#L181-L186) 真实使用“关闭此广告”“关闭此条广告”第二阶段按钮。新版补齐后者等精确广告文案，允许小型中央控件；普通“关闭”仍不足以执行。
4. **点击后仍未退出广告**：前一段视频关闭后可能出现落地页或下一层关闭按钮。用新证据、新会话或受限阶段跟进处理，点击成功回调本身不计数；不把不同按钮变化当作整个广告消失。持续存在且点击无效的同一候选不能通过重启会话无限重试。
5. **后期轮询**：事件触发与约 1.5 秒低频轮询搭配，等待广告加载和 enabled 变化；每次树遍历和每轮动作受限。可见但被安全策略剪掉的子树，以及 `getChild()` 返回空节点，均标记观察不完整，不用于成功确认。设备测试需覆盖少事件 / 无事件变化，不能仅凭引擎 JUnit 声称 Android 适配全部通过。

## 本轮不泛化的路径

- `tt_reward_full_count_down_after`：上游出现过该 ID，但它可能是倒计时后容器；没有本页充分动作语义，不能按名称相近就加入关闭白名单。
- `tt_item_tv`、`不感兴趣`、`反馈`：上游可通过有前置步骤的多段规则关闭广告，但这些控件本身不是广告关闭按钮，且可能提交反馈；本轮不作为全局动作。
- `close`、`iv_close`、`icon_close`、`ad_close` 的宽泛正则或包含匹配：宿主普通弹窗、账户、支付流程也会复用。个别上游 APP 规则中的结构选择器和页面条件不能抹掉后泛化。
- 腾讯 / 百度广告：当前阅读的 MIT 规则常依赖 Activity、兄弟节点数量、具体层级和无 ID 图片节点。例如 [七猫第 114–145 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.kmxs.reader.ts#L114-L145) 的两组默认关闭并提示误触。没有稳定明确关闭语义时继续使用来源限定精确规则，不全局猜测小图标坐标。
- 奖励视频没有可访问关闭控件、强制看完、全画布渲染：增加 ID 表不能产生原本不存在的动作。不点击广告正文、下载、购买、支付或领取按钮，也不把退出整个 APP 作为跳过成功。
- 摇一摇：无障碍会话改进可更及时发现关闭控件，无法替代目标进程的传感器 Hook。系统设备方向权限引导与已配置目标跳转后返回仍按原能力边界保留。

## 建议验证矩阵

- 开屏关闭成功后，在同一 APP 等待 20 秒再出现弹窗；应能新建广告会话。
- 先出现明确广告证据，35 秒或更晚才出现 enabled 的关闭控件；应继续低频观察，不提前点击倒计时。
- 两段关闭控件及不同视频广告连续出现；每段动作需结果确认，单一失效控件需停止重试。
- 中央“关闭此条广告”与中央普通“关闭”同时分别验证，只有明确广告语义可自动执行。
- 各新 ID 的无广告文本页面、错误 Activity、disabled、大父容器、前台切包及取消手势回调均不得动作或计数。
- 原开屏、来源统计、明确目标跳转返回、停止授权以及后台恢复提示必须保留回归验证。

上面是源码分析及测试建议。实际实现与验证结果以本版本 `VERIFICATION.md` 和 APK 内行为为准；当前没有据此完成真实 APP 或华为 / vivo 真机验收。
