# 印尼语语音构建说明

本项目运行时不使用 TTS、SpeechRecognizer、麦克风或神经网络语音模型。

当前音频架构：
- 构建阶段从 Wikimedia Commons 的 Lingua Libre 印尼语发音录音中筛选可再分发的真人录音
- 优先使用与学习文本完全一致的真人录音
- 没有整句录音时，只把许可允许再分发的真人“单词录音”做离线拼接
- 不使用任何 TTS 生成，不在用户手机上生成语音
- 输出统一为 24kHz / Mono / AAC M4A
- APK 运行时仅通过 Media3 ExoPlayer 播放已经打包的音频片段
- tts_index.tsv 使用学习原文 SHA-256 作为查找键
- AUDIO_ATTRIBUTION.tsv 随音频一起生成，记录来源、作者、许可证和来源页

许可证筛选：
- 允许：CC0、Public Domain、CC BY 2.0/3.0/4.0、CC BY-SA 2.0/3.0/4.0
- 排除：CC BY-NC、CC BY-ND 及包含非商业/禁止改编限制的许可证
- APK 不打包无法核验许可证的录音

重要：
- 音频覆盖率不强行补齐到 100%
- 没有对应真实录音时，界面不显示假的“播放”按钮，而显示“暂无内置语音”或直接隐藏
- 用户手机上不会联网生成语音
- 用户手机上不会加载 TTS 模型
- 没有 RECORD_AUDIO 权限
- 没有 SpeechRecognizer / RecognizerIntent
- 没有 ONNX / sherpa-onnx / eSpeak Runtime

当前内容基准：
- 3600 个工程/现场相关词汇
- 40 个核心句型
- 10 个真实场景
- 70 条场景对话行
- 26 个字母发音条目

最终 Release 构建要求：
1. 内容校验通过
2. 禁止 API 扫描为 0
3. Release APK 构建成功
4. APK 签名校验通过
5. APK 16KB 对齐校验通过
6. 音频只包含真实、可核验、可再分发来源的录音
