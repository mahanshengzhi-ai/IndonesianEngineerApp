# 印尼语工程员学习｜第二阶段基础框架验收

日期：2026-09-24
仓库：mahanshengzhi-ai/IndonesianEngineerApp
目标：完成稳定 Android 原生基础框架，不进入第三阶段内容重建。

## 本阶段完成

- Kotlin + Android Native Views
- compileSdk 36
- targetSdk 36
- minSdk 26
- Java 17 / Kotlin JVM 17
- Material 3 Light 主题
- 单 MainActivity
- 首页 / 学习 / 练习 / 翻译 / 我的五项底部导航
- 子页面返回首页
- 首页双击退出提示：`再按一次退出软件`
- 全局播放器容器固定在底部导航上方
- 播放器不占用底部导航位置
- Activity 状态恢复当前一级导航
- 软键盘使用 `adjustResize`
- 底部导航统一选中/未选中颜色
- 运行时仍不引入麦克风、语音识别、TTS 或 Compose

## 真实构建验证

建立 PR：

- PR #1
- 分支：`stage2-foundation-validation`

基础构建工作流：

`.github/workflows/android-foundation-pr.yml`

验证命令：

`gradle :app:testDebugUnitTest :app:assembleDebug --stacktrace`

### 第一次运行

结果：FAIL

真实 Kotlin 编译错误：

- `MainActivity.kt:293:13 'val' cannot be reassigned`
- `MainActivity.kt:299:13 'val' cannot be reassigned`

原因：两个 `TextView.apply` 初始化块中的 `text` 属性赋值存在 Kotlin 编译器解析歧义。

处理：

- 改为显式创建 `positionLabel` / `durationLabel`
- 使用 `setText(..., TextView.BufferType.NORMAL)`
- 再赋值给对应播放器字段

### 第二次运行

结果：SUCCESS

工作流：

`Android Foundation PR #2`

成功步骤：

- Checkout
- JDK 17
- Android SDK
- Android API 36 / Build Tools
- Gradle 8.13
- `testDebugUnitTest`
- `assembleDebug`
- Debug APK artifact upload

产物：

- artifact：`IndonesianEngineer-foundation-debug`
- size：37,034,405 bytes
- digest：`sha256:9b7bafe71f6119edd519896add39931c9ad4138f4ae0335b0108052e86a9ca92`

## 合并

PR #1 已成功合并到 `main`：

- merge commit：`65cf205e3e3a29e9cbd78aefbf85854cad78912e`

## 阶段结论

第二阶段基础框架已通过真实 GitHub Actions Android 构建验证。

本阶段没有进行：

- 3020+ 词汇质量重建
- 40 句型内容重建
- 10 场景内容重建
- 真人音频资源最终打包
- 第七阶段完整音频覆盖
- 第九阶段最终 APK 资源审计
- 第十阶段 Release 最终验收

这些工作保持在对应阶段，不提前混入当前阶段。
