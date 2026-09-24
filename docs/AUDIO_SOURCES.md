# 印尼语语音构建说明

本项目运行时绝不使用 TTS、SpeechRecognizer、麦克风或神经网络语音模型。

当前 APK 使用：
- 构建阶段生成印尼语学习音频
- Microsoft Edge TTS 的印尼语神经声线 id-ID-ArdiNeural
- 构建阶段速率约为 -12%
- 输出统一为 24kHz / Mono / AAC M4A
- APK 运行时只通过 Media3 ExoPlayer 播放已经打包好的音频

当前索引：
- 原始学习文本 SHA-256
- start_ms
- duration_ms
- original_text
- source_file
- 资源来源标记

重要：
- 当前这些构建阶段生成的语音没有在项目内标记为第三方授权真人录音
- 不把它们统计成已核验许可证音频
- 用户手机上不会联网生成语音
- 用户手机上不会加载 TTS 模型
- 没有 RECORD_AUDIO 权限
- 没有 SpeechRecognizer / RecognizerIntent
- 没有 ONNX / sherpa-onnx / eSpeak Runtime

阶段 1 最终审计结果：

VOCABULARY_COUNT = 3574
SENTENCE_COUNT = 40
SCENE_COUNT = 10
SCENE_LINE_COUNT = 70
LETTER_COUNT = 26

TOTAL_AUDIO_TEXTS = 3704
REAL_AUDIO = 3704
MISSING_AUDIO = 0
INDEXED_AUDIO = 3704
DUPLICATE_AUDIO_KEYS = 0
LICENSED_AUDIO = 0

AUDIO FORMAT:
codec = AAC
sample_rate = 24000
channels = 1

最新阶段 1 CI：
run = 43
head_sha = a5aa4c87c106457ec3f7b6b318119802ed208452
conclusion = success
debug APK artifact = IndonesianEngineer-debug
artifact SHA-256 = 5137530add11e6556ab09f91845b3296c76a93f079e60342545bfc79d895d38a
