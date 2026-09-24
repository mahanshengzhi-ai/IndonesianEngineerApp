# 真人印尼语音资源来源记录

本项目禁止运行时 TTS，只允许把已经取得合法再分发权的真实录音打入 APK。

## 当前研究结论

### Wikimedia Commons / Lingua Libre

Wikimedia Commons 的 Category:Lingua_Libre_pronunciation-ind 页面目前显示约 6,994 个印尼语录音文件。文件页会明确给出录音文本、录音者和许可证；许可证不是统一固定值，因此每一个实际打包文件都必须单独核对。

优先级：
1. CC0 录音：优先，可直接复制、修改、再分发，并适合 APK 内嵌
2. CC BY-SA：只有在确认 APK 分发方式满足署名与 ShareAlike 要求后才可使用
3. 其他许可证：默认不使用

已核对的 CC0 示例：
- Wikimedia Commons：LL-Q9240 (ind)-Sanfilzands (Jan Sapilisan)-suling.wav
- 文本：suling
- 许可证：CC0 1.0 Universal Public Domain Dedication
- 页面明确允许复制、修改、分发以及商业用途

### Mozilla Common Voice Indonesian

Common Voice 当前数据页列出 Indonesian Scripted Speech 27.0，数据集大小约 1.43 GB，标示为 CC0-1.0。

但 2025-10-31 生效的 Common Voice Legal Terms 同时说明数据集通过 Mozilla Data Collective 提供，并要求不要把 Common Voice 数据集全部或部分发布、分发或镜像到其他平台/服务。因此，本项目不会仅因为数据集标为 CC0，就自动把 Common Voice 录音直接嵌入 APK。

它目前只作为：
- 质量研究参考
- 可获取录音覆盖率评估
- 后续法律确认后的候选来源

## 音频验收要求

每一个进入 APK 的音频，都必须记录：
- 原始文本
- SHA-256
- 文件来源
- 原始文件名
- 录音者（若页面提供）
- 许可证
- 是否需要署名
- 是否做过格式转换
- 转换后文件校验值

禁止：
- YouTube
- 短视频
- 影视剧
- 播客
- 未明确授权的网站音频
- 任意运行时 TTS

## 本阶段状态

当前仓库已建立 AudioIndex + Media3 ExoPlayer 架构，但尚未把未经逐条许可核验的外部音频打入 APK。

因此：
- REAL_AUDIO = 0
- MISSING_AUDIO = 当前全部需要音频的文本
- 不允许用 TTS 补洞
