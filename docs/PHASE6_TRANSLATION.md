# 第六阶段：中文↔印尼语翻译验收

日期：2026-09-24  
阶段：Phase 6  
仓库：mahanshengzhi-ai/IndonesianEngineerApp

## 本阶段目标

把已有 ML Kit 翻译能力从一个简单输入框，整理成真正可以每天使用的翻译工具：

输入 → 方向 → 模型状态 → 翻译 → 复制 → 历史回看

## 技术实现

继续使用 Google ML Kit Translation。

当前项目依赖：

`com.google.mlkit:translate:17.0.3`

ML Kit 当前支持中文 `zh` 与印尼语 `id`，Android Translation 使用按需动态下载模型；模型下载完成后可在设备端继续执行翻译。

本阶段没有引入账号、服务器或自建翻译接口。

## 已完成

### 双向翻译

支持：

- 中文 → 印尼语
- 印尼语 → 中文

方向按钮视觉状态明确，减少误操作。

### 模型状态

页面明确显示：

- 正在准备
- 已就绪 · 可离线翻译
- 未准备好
- 模型下载失败
- 翻译失败

首次使用时准备对应方向的模型。

模型下载成功后，后续可在设备端使用，不需要为每次翻译重新联网。

### 输入体验

- 多行文本输入
- 最大 1000 字
- 实时字数统计
- 清空
- 中文 / 印尼语方向提示
- 工程场景示例提示

### 翻译结果

- 独立结果区域
- 清晰的结果层级
- 一键复制
- 没有结果时不允许假装可以复制

### 最近历史

保存最近 6 条完整记录：

- 翻译方向
- 原文
- 译文

点击历史条目后可以回填原文并恢复方向，同时直接查看之前的译文。

历史仍然使用 SharedPreferences，不需要服务器。

### 今日学习统计

每次翻译成功后：

- 今日翻译次数 +1
- 更新最近 6 条翻译历史

因此“我的”页面能看到真实的今日翻译量。

## 页面布局

本阶段继续沿用前面已经确定的学习 App 视觉：

- 页面宽松留白
- 顶部标题 + 简短说明
- 方向选择放在输入前
- 输入 / 翻译操作 / 结果明确分层
- 历史记录使用简洁卡片
- 主色墨绿色
- 暖橙色只承担辅助操作
- 不使用蓝色后台式大按钮堆叠

翻译页的主要任务是“快速完成沟通”，而不是展示复杂设置。

## 构建验证

PR：#7

第一次 Android CI：

FAIL

真实编译错误：

`TranslatePage.kt:152:13 Unresolved reference 'translateButton'`

以及：

`TranslatePage.kt:325:16 Return type mismatch: expected 'LinearLayout', actual 'MaterialCardView'`

原因：

- Kotlin 初始化按钮时引用自身
- 历史卡片方法声明返回 LinearLayout，但实际返回 MaterialCardView

修复：

- 改用 `lateinit var translateButton: MaterialButton`
- 历史卡片方法返回通用 View

第二次 Android CI：

SUCCESS

通过：

- JDK 17
- Android API 36
- Gradle 8.13
- `testDebugUnitTest`
- `assembleDebug`
- APK artifact upload

同时 Vocabulary Quality Audit：

SUCCESS

Debug APK artifact：

`IndonesianEngineer-foundation-debug`

大小：

37,058,323 bytes

SHA-256：

`4cd5ec7e11b66f7a3d8ac5ada66a92b84ad793cdee95fa1c0582e133130036f8`

## 合并

第六阶段 PR #7 已合并到 `main`。

merge commit：

`264d181acb0da5064552bdec330497fa0fc085f6`

## 当前阶段边界

本阶段没有提前实现：

- 真人印尼语音资源大规模建设
- 性能专项
- 最终 Release 签名
- APK 最终接受测试

## 验收结论

第六阶段已完成。

当前翻译页已经形成：

`双向文字翻译 → 模型状态 → 本地复制 → 最近 6 条历史 → 今日翻译统计`

并通过真实 Android CI。
