# 图像关闭控件的后续参考：Ad Skipper

核对日期：2026-10-23。已通过 GitHub 连接器读取实际 Kotlin 代码，不以 README 的覆盖率或设备结果代替清屏验收。

参考项目：[madeye/ad-skipper](https://github.com/madeye/ad-skipper)，本次固定提交 `2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5`。[仓库 LICENSE](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/LICENSE) 为 MIT；本轮未复制代码、依赖、模型或本机库，模型和依赖不能仅凭仓库根许可证推断授权。

## 实际读到的设计

- [DetectionPipeline.kt](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/core/src/main/java/com/adskipper/core/detect/DetectionPipeline.kt)：按无障碍节点、OCR、YOLO、VLM 顺序短路，节点命中就不获取截图。图像层另有广告证据 gate；截图不可得则停止像素检测。
- [OcrDetector.kt](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/core/src/main/java/com/adskipper/core/detect/OcrDetector.kt)：通过本地 ML Kit 中文识别获得文字及边框；OCR 客户端创建失败时仅降级该层。它能研究“文字画在图里，但节点树没有按钮标签”的漏检。
- [NodeMatcher.kt](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/core/src/main/java/com/adskipper/core/detect/NodeMatcher.kt)：拒绝覆盖大面积屏幕的命中区域，避免把名称带 skip 的整页容器当按钮；同时提供有节点上限的 SDK 指纹与树大小检查。清屏继续采用自己的宽高限制及安全父节点检查。
- [AdSkipperService.kt](https://github.com/madeye/ad-skipper/blob/2734cb1b3d4c82dc30d909ec6461f5bc3564f7b5/core/src/main/java/com/adskipper/core/service/AdSkipperService.kt#L571-L641)：执行前复核当前根的来源包，图像层依赖广告证据。源码末尾设置开屏轮询扩展值 45 秒、硬上限 90 秒；这比 README 表格更能说明实际代码行为。清屏的后续弹窗采用选定 APP 前台低频重扫，动作预算按广告轮次限制，不直接照搬这些参数。

## 对清屏有价值的方向

最有价值的是**可选的本地 OCR 后备**：保持现有节点规则为第一层，只有缺少可访问关闭节点、有独立广告证据、用户单独启用图像能力时，才在限定频率下识别短广告关闭文案及其小型边框，再复核前台和页面状态。纯图标识别需要独立误触评测集；不能因模型输出某个坐标就立即执行。

这能扩大“关闭文字/图标只存在于图片”的研究范围，但无法创造原本不存在的跳过功能。必须覆盖业务关闭按钮、下载 CTA、启动快照、方向变化、广告倒计时、系统权限窗口和安全截图失败等负例，才能判断是否适合华为/vivo。

## 本版决定

清屏 0.3.0 **未加入截图、OCR、YOLO、VLM 或新联网权限**。现有披露为不截图，当前交付继续按节点、明确广告语义和有条件 SDK 规则处理；图像能力需要新的实现、用途披露、设备验证与性能评估，不能借参考项目的自测数据宣称已完成。

该项目服务在手势提交后即写统计（同文件第 626–628 行），不能直接作为清屏“广告已关闭”的确认标准。清屏继续要求动作后的同源完整观察；同样不迁入不可见悬浮窗制造事件的保活方式，也不将其他 ROM 的保活注释视为华为/vivo 验证结论。
