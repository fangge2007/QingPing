# 实现说明：当前主线架构

[项目入口](../README.md) · [权限与数据边界](PERMISSIONS_AND_LIMITS.md) · [验证工具](VERIFICATION_TOOLING.md)

本页维护执行链路、状态及代码职责。安装教程见项目介绍和手机指南；授权、隐私、系统恢复与分发限制统一在权限页维护。旧版本行为以对应 Git 标签为准。

## 数据与动作链路

1. `AppStore` 提供已同意用途、保护总开关、来源、跳转目标及逐应用/逐规则手势授权。UI、规则导入、持久配置和服务共用 `PackagePolicy` 的关键包名限制。
2. `AccessibilityServiceInfo.packageNames` 动态配置所选来源及其明确目标；没有有效选择或保护停用时使用清屏自身包名，避免 null/空数组重新成为全应用事件范围。它控制事件交付，不是读屏 API 的权限隔离。
3. 服务在读取实际活动根节点后核对前台、授权、锁屏和关键包名；仅检查选中来源的节点。SDK 完整 ID 与精确规则的查询结果也重新校验，不能绕过正常遍历的输入/祖先限制。
4. `inspect` 将有限节点转换为候选快照。`AdDecisionEngine` 只处理纯值，不持有 Android 节点；返回动作决策与页面确认结果。
5. Android 适配层在点击、手势及全局返回前复核当前条件。请求被系统受理后，仍需后续页面观测才能写入统计。

## 有界扫描

| 项目 | 当前约束 |
| --- | --- |
| 常规节点遍历 | 每次最多 700 个节点、24 层 |
| 优先 ID 查询 | 最多 8 个 ID，每个最多 8 个命中，命中路径重新校验 |
| 动作子树核验 | 最多 40 个节点；不可读取的子节点使动作拒绝 |
| 点击祖先 | 最多向上寻找 2 层，窗口根不作为普通点击目标 |
| 输入与危险业务 | 密码/可编辑子树跳过；危险标签、描述、资源名及危险点击祖先否决动作 |
| 轮询 | 入口默认 15 秒内约 250ms，之后约 1500ms；事件可加速下一次有界观察 |

来源内置目录、已核实 SDK 条件与精确规则优先；通用标签仍需广告语义及位置尺寸约束。目录的实际范围与能力边界在 [README](../README.md) 说明，本页不维护另一份应用数量或体验承诺。

## 状态机与统计确认

- 引擎先产生 offered 决策，适配层报告是否 accepted；受理不等于成功关闭。
- 点击后至少经过 200ms，并在两次完整页面观察中确认普通内容、候选与广告证据均消失；两次缺失观察间隔至少 180ms。读取不完整不能证明消失。
- 请求确认窗口为 2.5 秒。每轮广告最多 4 次受理点击、8 次请求；每控件最多 3 次受理，后续重试另受冷却时间限制。
- 广告间普通内容稳定观察至少 500ms 后，才重新允许下一轮，避免重复计数。
- 返回只针对一个明确配置目标，依赖短时广告证据；目标实际打开后请求返回，后续来源恢复才计数。临时放行影响返回，不影响广告关闭。

统计属于页面变化推断。真实页面效果、误触与耗电需要独立设备记录，见[真机验收模板](DEVICE_ACCEPTANCE_TEMPLATE.md)。

## 精确规则与手势

`ExactRule` 只支持 `qingping-rules-v1` 的字面值选择器；全部非空条件同时匹配，没有 viewId 时还需完整 Activity。默认进入窗口 15 秒，可设 3–30 秒，持续匹配需逐规则显式开启。不执行远程订阅、选择器语言或脚本。

手势需逐应用授权，精确规则还需 `allowGesture`。普通点击失败，或受理后 2.5 秒未确认时，才可能对重新核实的小型控件使用中心点手势。已受理点击的后备手势还要求窗口与边界保持一致；不是固定屏幕坐标猜测。

设置变更、锁屏、断连等会重置引擎并增加 generation。旧手势回调不能恢复已撤销的动作。`onCancelled` 释放在途状态并报告未受理，不增加成功统计。

## 代码地图

| 入口 | 职责 |
| --- | --- |
| [QingPingAccessibilityService](../app/src/main/java/cn/qingping/app/QingPingAccessibilityService.java) | 系统事件、节点读取、动作前复核、异步回调与资源释放 |
| [AdDecisionEngine](../app/src/main/java/cn/qingping/app/engine/AdDecisionEngine.java) | 纯值状态机、预算、确认、轮次与跳转证据 |
| [AdControlPolicy](../app/src/main/java/cn/qingping/app/engine/AdControlPolicy.java) / [PackagePolicy](../app/src/main/java/cn/qingping/app/engine/PackagePolicy.java) | 危险动作、上下文与共用关键包名约束 |
| [ExactRule](../app/src/main/java/cn/qingping/app/rules/ExactRule.java) | 有界 JSON、规则验证、字面值匹配 |
| [MainActivity](../app/src/main/java/cn/qingping/app/MainActivity.java) / [UsageGuideActivity](../app/src/main/java/cn/qingping/app/UsageGuideActivity.java) | 用途同意、配置、统计及只读离线教程 |
| [验证入口](VERIFICATION_TOOLING.md) | 统一任务、Robolectric、覆盖率门槛与机器证据 |

## 构建与发布

Java/SDK 配置及命令见[验证工具](VERIFICATION_TOOLING.md)，签名和迁移见[发布构建](RELEASE_BUILD.md)，引用与许可见[来源清单](REFERENCES.md)。本页不重复这些操作步骤或历史测试数字。
