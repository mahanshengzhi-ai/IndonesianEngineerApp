# 印尼语工程员学习｜第一阶段项目审计

审计日期：2026-09-24  
仓库：mahanshengzhi-ai/IndonesianEngineerApp  
分支：main  
审计基线 HEAD：0a4f75293489e6fc19769ce189a8afacd157d769

## 1. 仓库现状

GitHub 当前可访问，默认分支为 `main`，当前仓库只有一个分支记录。仓库近期提交较密集，已经历过 Android 基础、学习页面、进度、翻译、音频管线和 CI 多轮迭代。

当前项目不是空仓库，也不是需要从零推倒重做的项目。已有价值主要集中在：

- Android/Kotlin 基础工程配置
- 单 MainActivity 导航骨架
- SharedPreferences 学习进度存储
- ML Kit Translation
- Media3 ExoPlayer
- TSV 本地学习数据
- 真实真人录音的 Wikimedia/Lingua Libre 构建脚本
- 音频 SHA-256 索引体系
- GitHub Actions 内容/Android/音频检查流程

## 2. 当前目录结构

当前实际使用的主体结构为：

```
.github/
  workflows/
    android-build.yml
    audio-audit.yml

app/
  build.gradle.kts
  proguard-rules.pro
  src/main/
    AndroidManifest.xml
    assets/
      letters.tsv
      scenes.tsv
      sentences.tsv
      tts_index.tsv
      words_01.tsv ... words_11.tsv
    java/com/mahanshengzhi/indonesianengineer/
      MainActivity.kt
      audio/
      data/
      model/
      translation/
      ui/
    res/
      drawable/
      layout/
      menu/
      values/
  src/test/

docs/
  AUDIO_SOURCES.md
  PROJECT_AUDIT.md

scripts/
  prepare_wikimedia_audio.py
  validate_content.py
  validate_ui.py

根目录：
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  README.md
```

历史上曾存在 `ui/pages/` 和 `UiKit.kt` 两套 UI 体系，但当前工作树已经通过后续 cleanup/restore 提交收敛到 `ui/` 单一路径；当前 MainActivity 直接引用 `com.mahanshengzhi.indonesianengineer.ui.*`。

## 3. 技术配置核对

当前 `app/build.gradle.kts`：

- Kotlin Android
- Android Views
- compileSdk = 36
- targetSdk = 36
- minSdk = 26
- Java 17 / Kotlin JVM 17
- AndroidX
- Material Components
- RecyclerView
- Media3 ExoPlayer 1.11.1
- ML Kit Translation 17.0.3
- SharedPreferences 由 ProgressStore 使用

确认没有 Jetpack Compose 依赖。

当前只有一个 `MainActivity.kt`，没有发现第二个 MainActivity。

注意：仓库当前没有提交 Gradle Wrapper（`gradlew` / `gradle/wrapper/gradle-wrapper.properties` 均不存在）。GitHub Actions 目前通过下载 Gradle 8.13 来构建。这个基础工程问题需要在后续基础框架阶段补齐，不能留到最终发布才处理。

## 4. 内容资产核对

实际读取 11 个词库文件：

- 总词汇：3600
- words_01.tsv ~ words_10.tsv：每份 325 条数据
- words_11.tsv：350 条数据
- ID 重复：0
- 印尼语字段混入中文：0
- 重复的印尼语音频 key：0

其他内容：

- 核心句型：40
- 场景：10
- 场景对话行：70
- 每个场景当前均有 7 个 turn
- 字母：26

### 内容质量风险

数量是合格的，但不能把“3600”直接当成“3600 条已经审核通过的高质量工程印尼语”。

检查中发现至少 25 条命中明显模板化/组合式规则，例如：

- `pekerjaan pekerjaan konstruksi`
- `pemeriksaan pekerjaan konstruksi`
- `pengukuran pekerjaan konstruksi`
- `pemasangan pekerjaan konstruksi`
- `lokasi pekerjaan konstruksi`

其中一部分可能是可以解释的工程词组，但整体存在明显模板化批量生成痕迹，因此必须在第三阶段做逐条语义复核和重建，不能为了维持数量继续扩写同类伪词。

本阶段没有直接删除 3600 条数据，原因是目前无法在不破坏有效条目的前提下做可靠的逐条人工语义裁决；保留原始 TSV 作为第三阶段重建输入，避免不可逆误删。

## 5. 语音系统审计

### 保留

`scripts/prepare_wikimedia_audio.py` 是当前有价值的真实音频方案：

- 来源方向：Wikimedia Commons / Lingua Libre
- 只选择允许再分发的许可证
- 支持完整短语真人录音
- 无完整录音时才尝试使用可再分发真人单词录音拼接
- 输出 24kHz / Mono
- 统一音量约 0.88
- 使用原始印尼语文本做 SHA-256
- 输出 `tts_index.tsv`
- 输出 `AUDIO_ATTRIBUTION.tsv`
- 输出单一 `tts_audio.m4a`
- 没有真实录音就不伪造“有声音”

Android 运行时：

- `AudioIndex` 对原始文本计算 SHA-256
- `AudioPlayer` 使用 Media3 ExoPlayer 播放 `asset:///tts_audio.m4a`
- UI 在索引不存在时显示“暂无内置语音”
- 没有 TextToSpeech / SpeechRecognizer / 麦克风运行链路

### 当前真实资源状态

当前 Git 仓库内：

- `tts_index.tsv`：只有表头，0 条索引
- `tts_audio.m4a`：当前未提交
- `AUDIO_ATTRIBUTION.tsv`：当前未提交

这意味着当前仓库源码具备真实音频构建能力，但当前 Git 工作树本身并没有携带可播放音频包。后续必须通过第七阶段真实音频构建和 CI 产物验证解决，不能在审计中把“脚本能生成”写成“当前 APK 已有完整语音”。

### 删除

已删除：

`scripts/generate_audio.py`

该脚本使用 `edge_tts` 和 `id-ID-ArdiNeural` 生成合成语音，与本项目最终要求的“真实、可再分发真人录音”原则冲突，且当前正式 Android CI 已不再依赖它，因此第一阶段应彻底移除，避免未来误用。

## 6. 禁止项检查

对当前 Android 源码、Manifest、Gradle 配置和现行脚本进行检查：

- SpeechRecognizer：0
- RecognizerIntent：0
- RECORD_AUDIO：0
- TextToSpeech API：0
- MediaPlayer：0
- sherpa-onnx：0
- ONNX runtime：0
- eSpeak runtime：0
- Jetpack Compose：0
- 同声传译：0
- 实时同传：0
- 麦克风功能：0

说明：

项目文件名中保留 `tts_index.tsv`、`tts_audio.m4a` 是既定离线音频索引/资源命名，不代表运行时 TTS。它们不能作为“使用 TTS API”的依据。

## 7. UI / 导航审计

确认：

- 一级导航只有：首页 / 学习 / 练习 / 翻译 / 我的
- BottomNavigation 为 5 项
- 当前 MainActivity 只有一个
- 播放器容器位于 BottomNavigation 上方
- 返回键已有“子页 → 首页；首页第一次提示，再次退出”的基础逻辑
- 当前没有旧的连续监听/麦克风入口
- UI 已从历史的重复 `ui/pages` 架构收敛到 `ui`

当前视觉基础已使用项目目标中的绿色系和暖色背景，但第八阶段仍需要统一打磨，不在第一阶段扩写功能。

## 8. CI 现状

当前 GitHub Actions：

- `.github/workflows/android-build.yml`
- `.github/workflows/audio-audit.yml`

Android build 流程已经包含：

- JDK 17
- 音频工具准备
- Wikimedia 真人录音构建
- 内容与禁止 API 检查
- Release APK 构建
- apksigner
- zipalign
- APK / 音频产物上传

当前通过 GitHub 连接器无法获得 HEAD 对应的 push workflow check 状态，返回为空，因此本审计不虚报“当前 HEAD 已构建成功”。

此前历史提交中的 CI 成功记录只作为历史证据，不作为当前 HEAD 的成功证明。

## 9. 第一阶段清理结果

已完成：

1. 保留有效 Android 基础、学习数据、进度、翻译、Media3、真实音频构建脚本、CI。
2. 删除旧的 Edge TTS 合成脚本。
3. 删除已经与当前状态不一致的旧 `PHASE1_CHECKPOINT.md`，避免把过期的“3704 条已完成语音”等数字继续当成现状。
4. 新建本审计文档作为当前事实基准。
5. 确认当前 Android 源码没有旧同传/麦克风/语音识别运行链路。
6. 确认当前不存在多个 MainActivity。
7. 确认当前 UI 已经没有历史 `ui/pages` 重复体系。
8. 确认 11 个词库文件、40 句型、10 场景、26 字母文件均存在。

## 10. 尚未在第一阶段解决、留给后续阶段的问题

- 词汇库存在明显模板化/伪数据风险，需要第三阶段重建。
- 当前仓库没有 Gradle Wrapper。
- 当前 Git 工作树没有实际打包音频资源，`tts_index.tsv` 为空。
- 当前 Release APK 尚未以当前 HEAD 的真实构建产物作为验收依据。
- 第九阶段仍需把禁止项扫描、APK 内部资源检查、音频索引/文件完整性检查进一步收紧。

## 11. 阶段结论

第一阶段的目标不是“功能全部做完”，而是把项目从混杂历史实现的状态收敛到一个可继续开发的基线。

当前基线可以继续进入下一阶段，但第三阶段的内容重建和第七阶段的真实音频资源验证必须保留为明确门槛，不能因为已有数量和脚本就宣称完成。

第一阶段清理提交：

- 删除旧 Edge TTS 生成器：ea7b611e5defba832f9b571e84c3e099c05d760e
- 删除旧阶段检查记录：cd5dd4de71b6db261578032e5486cfc8fb829bb4
