# 开屏专用控件目录的源码核对

核对日期：2026-10-23。通过 GitHub 连接器直接读取 [ganlinte/GKD-subscription 固定提交 `4746d192b4c57a4b62ed92a7f5deaf79e64fa811`](https://github.com/ganlinte/GKD-subscription/tree/4746d192b4c57a4b62ed92a7f5deaf79e64fa811) 的源文件。下述目录只整理上游存在的开屏控件，不以广告 SDK、APP 名称或控件名猜测更多 ID。源码记录不能代替用户华为 / vivo 上的实际版本验收。

## 直接针对哔哩哔哩的发现

[主版文件第 7–26 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bili.ts#L7-L26) 的开屏组明确写了“任意界面切回 APP”，匹配两个完整 ID，且两个都要求 TextView 的文字以“跳过”开头：

```ts
anyMatches: [
  'TextView[id="tv.danmaku.bili:id/count_down"][text^="跳过"]',
  'TextView[id="tv.danmaku.bili:id/skip"][text^="跳过"]',
]
```

上游设置 `matchTime: 10000`、`actionMaximum: 1`、`resetMatch: 'app'`、`fastQuery: true`，没有额外要求“广告”标签或固定 Activity。因此，清屏把普通“跳过”绑定到独立可读广告标签，会漏掉“跳过按钮可读，广告标签只在图片中”的页面。这是可解释的漏检路径；不能从源码推断用户设备一定属于这一种。

[HD 文件第 7–16 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bilibilihd.ts#L7-L16) 则直接匹配 `[id="tv.danmaku.bilibilihd:id/count_down"]`，同为 10 秒、每 APP 轮次最多一次。主版源码要求 `text^="跳过"`，不能把 HD 的较宽条件无差别迁到主版。

## 可供清屏实现的完整来源目录

以下每行都必须先匹配来源包，不能按资源 ID 后缀全局生效。除哔哩哔哩两版外，提供六个可独立核实的 APP 条目；最后一个大麦建议候选保留，不因 ID 命中就放宽无文字动作。

| APP / 来源包 | 完整目标 ID | 上游实际条件与动作 | 精确源码 |
|---|---|---|---|
| 哔哩哔哩 `tv.danmaku.bili` | `tv.danmaku.bili:id/count_down` 或 `tv.danmaku.bili:id/skip` | TextView，`text^="跳过"`；未指定 Activity；未指定 clickCenter | [主版第 7–26 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bili.ts#L7-L26) |
| 哔哩哔哩 HD `tv.danmaku.bilibilihd` | `tv.danmaku.bilibilihd:id/count_down` | 仅完整 ID；未指定文字、Activity、clickCenter | [HD 第 7–16 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/tv.danmaku.bilibilihd.ts#L7-L16) |
| 知乎 `com.zhihu.android` | `com.zhihu.android:id/btn_skip` | TextView；没有文字 / 描述及 Activity 条件；快照注释包含 LauncherActivity、LaunchAdActivity、MainActivity、ContentActivity、MixShortContainerActivity、ContentMixProfileActivity | [知乎第 7–22 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.zhihu.android.ts#L7-L22) |
| 中国电信 `com.ct.client` | `com.ct.client:id/btSkip` | 仅完整 ID；没有 Activity / 文字条件。下组 `tvSkip` 是用户引导，不能混入开屏目录 | [电信第 7–24 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.ct.client.ts#L7-L24) |
| TeraBox `com.dubox.drive` | `com.dubox.drive:id/tv_skip` | TextView；没有 Activity / 文字条件。不采纳另分支 `ivClose`，其需要 `inmobi-ad` 伴随条件 | [TeraBox 第 7–24 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.dubox.drive.ts#L7-L24) |
| 华为视频 `com.huawei.himovie` | `com.huawei.hms.ads.uiengine:id/hiad_skip_text` | 完整 SDK 命名空间，与来源包不同；上游显式 `action: 'clickCenter'`；没有 Activity / 文字条件 | [华为视频第 7–20 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.huawei.himovie.ts#L7-L20) |
| UC 浏览器 `com.UCMobile` | `com.byted.pangle:id/tt_splash_skip_btn` | 仅选明确完整 ID 分支；另有无 ID 的层级 View 分支，本目录不采用 | [UC 第 7–24 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.UCMobile.ts#L7-L24) |
| 大麦 `cn.damai` | `cn.damai:id/homepage_advert_pb` | 上游仅完整 ID；资源名不是明确 skip，清屏应加短“跳过”语义，缺少语义时不动作 | [大麦第 7–16 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/cn.damai.ts#L7-L16) |

这些开屏组都设置了 10 秒匹配窗口、每 APP 轮次最多一次动作；上游规则只是 GKD 的声明数据，不能据此推断华为一定接收 ACTION_CLICK。华为视频的 `clickCenter` 是对“节点可见但控件点击不生效”的具体后备证据，清屏应仍遵守用户授予的模拟点击开关。

### 六个扩展 APP 的上游目标表达式

下面摘录目标表达式，保留上游约束。来源包和开屏组的 10 秒 / 一次 / APP 重置条件见上表源码，不能只截取表达式全局执行。

```ts
// com.zhihu.android
'TextView[id="com.zhihu.android:id/btn_skip"]'

// com.ct.client
'[id="com.ct.client:id/btSkip"]'

// com.dubox.drive：只取明确跳过控件分支
'TextView[id="com.dubox.drive:id/tv_skip"]'

// com.huawei.himovie
{
  matches: '[id="com.huawei.hms.ads.uiengine:id/hiad_skip_text"]',
  action: 'clickCenter',
}

// com.UCMobile：只取明确 ID 分支
'[id="com.byted.pangle:id/tt_splash_skip_btn"]'

// cn.damai
'[id="cn.damai:id/homepage_advert_pb"]'
```

## 清屏建议增加的保护条件

这是清屏自己的设计建议，并非上述源码已验证的页面事实：

- 只在用户已选择的来源包内、进入或切回后的短开屏窗口匹配，使用完整来源包 + 完整 ID，不能扩为 `id/skip`、`btn_skip` 或任意 `close`。
- 按当前节点实际边界判断可见、enabled、足够小；手势只落在该已匹配控件中心，不点击任意屏幕角落，不把广告正文的大父容器作为点击目标。
- 优先再要求短跳过语义，如“跳过”“跳过 5”“5 跳过”“Skip”。这是比上游更窄的条件，可牺牲少量覆盖换取确定性。拒绝包含下载 / 安装 / 领取 / 支付等动作的混合标签。
- ID 与明确短跳过语义共同成立时可自证“开屏跳过按钮”，不要求另一枚可能不可读的“广告” badge。它不应该自动为摇一摇跳转返回提供独立运动触发证据。
- 对无文字控件，只可逐条批准已核实专用 ID；大麦这类非语义 ID 不应无文字自动点击。哔哩哔哩主版上游明确要求文字前缀，默认也应保留。
- 统计仍须等待广告控件消失且页面稳定；动作 API 返回 true 只是接受动作请求，不能直接记成功。不会退出整个 APP 冒充跳过。

## 为什么没有编出微博等 APP 的开屏 ID

已完整读取该提交中的 `com.sina.weibo.ts`、`com.xingin.xhs.ts`、`com.netease.cloudmusic.ts`、`com.baidu.BaiduMap.ts`、`com.autonavi.minimap.ts`、`com.tencent.qqmusic.ts`。它们在此提交没有专用“开屏广告”组：

- [微博](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.sina.weibo.ts) 的 `tv_title_bar_skip` 属于重新登录后的兴趣推荐页，不能当开屏广告 ID。
- [网易云音乐](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.netease.cloudmusic.ts) 的 `skipBannerAd` 属于发现页顶部视频广告，也不是开屏广告 ID。
- [QQ 音乐](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.tencent.qqmusic.ts) 的 `js_close_btn` 属于 WebView 底部弹窗；[百度地图](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.baidu.BaiduMap.ts)、[高德地图](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.autonavi.minimap.ts) 的 close 控件也有各自非开屏用途。
- [小红书](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/apps/com.xingin.xhs.ts) 本文件没有可直接批准的开屏目标 ID。

这不表示这些 APP 没有开屏广告或 GKD 无法处理。该订阅另有 [全局开屏规则第 8–25 行](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/globalGroups.ts#L8-L25)，实际匹配 `[text*="跳过"][text.length<10][visibleToUser=true]`，10 秒窗口、最多两次。它不要求“广告”标签；清屏现有独立标签门槛与这个机制不同。上游还 [把具有专用开屏组的 APP 排除出全局开屏规则](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/src/globalDefaultApps.ts#L158-L175)，以避免通用和专用规则互相抢动作。

## 许可与采用范围

[该固定提交 LICENSE](https://github.com/ganlinte/GKD-subscription/blob/4746d192b4c57a4b62ed92a7f5deaf79e64fa811/LICENSE) 为 MIT，`Copyright (c) 2024 ganlinte`。采用规则事实 / 目标表达式时保留版权和 MIT 正文；项目 `app/src/main/assets/third_party_notices.txt` 已包含完整许可。这里未复制 GKD 客户端执行器、选择器引擎或 GPL 客户端源码，也未引入远程脚本 / 自动订阅执行。上游对当前版本 APP 的快照证据和用户手机的真实页面仍可能不同。
