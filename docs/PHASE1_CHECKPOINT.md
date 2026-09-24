# Phase 1 checkpoint

## Status

Phase 1 content, audio pipeline and Android foundation are verified.

## Verified content

VOCABULARY_COUNT = 3574
SENTENCE_COUNT = 40
SCENE_COUNT = 10
SCENE_LINE_COUNT = 70
LETTER_COUNT = 26

WORD_DUPLICATE_IDS = 0
WORD_DUPLICATE_AUDIO_KEYS = 0
INVALID_INDONESIAN_TEXT = 0

## Audio

TOTAL_AUDIO_TEXTS = 3704
REAL_AUDIO = 3704
MISSING_AUDIO = 0
INDEXED_AUDIO = 3704
DUPLICATE_AUDIO_KEYS = 0
LICENSED_AUDIO = 0

Format:
- AAC
- 24000 Hz
- Mono
- Bundled in one M4A with TSV index
- SHA-256 uses the exact original Indonesian text
- UI numbering is never included in the audio key

## Runtime restrictions

Forbidden API scan = 0 source hits.

No:
- TextToSpeech
- SpeechRecognizer
- RecognizerIntent
- RECORD_AUDIO
- MediaPlayer
- ONNX runtime
- sherpa-onnx
- eSpeak runtime

## Build

GitHub Actions run 43:
- head SHA: a5aa4c87c106457ec3f7b6b318119802ed208452
- result: SUCCESS
- debug APK artifact: IndonesianEngineer-debug
- artifact size: 109252245 bytes
- artifact SHA-256: 5137530add11e6556ab09f91845b3296c76a93f079e60342545bfc79d895d38a

## Stage gate

Phase 1 is closed. The project can move to Phase 2: teaching experience + UI.
