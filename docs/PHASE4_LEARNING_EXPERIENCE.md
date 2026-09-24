# 第四阶段：核心学习体验验收

日期：2026-09-24  
仓库：mahanshengzhi-ai/IndonesianEngineerApp  
阶段：Phase 4

## 本阶段目标

把 App 从“有学习页面”推进为真正以每天 15 分钟为核心节奏的语言学习产品。

核心路径：

`听懂 → 看懂 → 跟读 → 回忆 → 应用`

## 已完成

### 首页

- BAHASA INDONESIA · ENGINEER
- 印尼语工程员学习
- 每天 15 分钟，先听懂，再开口
- 当前日期
- 今日 10 个新词
- 今日 5 个句型
- 今日 1 个场景
- 今日 10 题挑战
- 今日听力次数
- 今日翻译次数
- 已掌握词汇数
- 连续学习天数
- 今日打卡状态
- 继续学习
- 去挑战

首页不再是简单的统计堆叠，而是明确告诉用户“今天要做什么”。

### 学习页

重新整理为四个学习入口：

1. 发音入门
2. 工程词汇
3. 核心句型
4. 真实场景

同时显示今天的学习路径进度。

### 发音

保留 26 个字母与重点发音提示：

- c
- j
- g
- ng
- ny
- sy
- kh
- h
- r
- e

不使用中文谐音作为主要教学方式。

### 工程词汇

新增：

- 中文搜索
- 印尼语搜索
- 分类筛选
- 只看未学会
- 已学会状态
- 音频状态
- 例句显示
- “我会了”本地记录

词汇数据使用第三阶段清理后的 3281 条质量门通过数据。

### 核心句型

每条句型都有：

- 印尼语
- 中文
- 场景
- 解释
- 有效内置音频时的播放入口
- “我会用这句”练习记录

### 真实场景

场景页继续采用 A/B 对话，不退回到词表式展示。

场景详情增加：

- 学习方法提示
- A/B 对话
- 中文解释
- 每句真实音频状态
- 完成整段对话后记录今日场景练习

### 本地学习统计

继续使用 SharedPreferences：

- 每日新词
- 每日句型
- 每日场景
- 每日练习
- 连续学习
- 今日打卡

并增加今日是否已经打卡的可靠状态判断。

## 真实构建验证

第四阶段建立 PR：

PR #5

第一次 Android CI：

结果：FAIL

真实错误：

`MainActivity.kt:168:29 No value passed for parameter 'onLearned'`

原因：

`VocabularyPage.build()` 增加了 `isLearned` 参数后，MainActivity 使用了 Kotlin trailing lambda 与命名参数混用的错误调用方式。

修复：

改为显式：

`onLearned = { ... }`

并显式传入：

`isLearned = progress::isLearned`

第二次 Android CI：

结果：SUCCESS

通过：

- Checkout
- JDK 17
- Android SDK
- Android API 36
- Gradle 8.13
- `testDebugUnitTest`
- `assembleDebug`
- APK artifact upload

产物：

- artifact：`IndonesianEngineer-foundation-debug`
- size：37,042,818 bytes
- SHA-256：`ce69d3dc7bef16a258cf9390c98f655dac8e648474b512d74f8eb29465555395`

同时 Vocabulary Quality Audit：

结果：SUCCESS

## 合并

第四阶段 PR #5 已合并到 `main`。

merge commit：

`b14bc8af95b1e4cceb50f8c1f441085961b4014e`

## 当前阶段边界

本阶段没有提前实现：

- 闪卡完整逻辑
- 10 题挑战完整重构
- 我的页面最终统计重构
- ML Kit 翻译最终体验
- 真人音频资源最终建设
- 第八阶段性能专项
- 第九阶段最终 CI 收口
- 第十阶段 Release APK 验收

这些继续留在对应阶段。

## 验收结论

第四阶段核心学习体验已完成代码实现并通过真实 Android CI 构建验证。

当前 App 的核心入口已经围绕“每天 15 分钟学习”组织，而不是围绕工程管理功能组织。
