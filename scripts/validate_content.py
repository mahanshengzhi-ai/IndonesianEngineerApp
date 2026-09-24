#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
APP = ROOT / "app"

FORBIDDEN = (
    "TextToSpeech",
    "SpeechRecognizer",
    "RecognizerIntent",
    "RECORD_AUDIO",
    "MediaPlayer",
    "sherpa-onnx",
    "onnx",
    "espeak",
)

CJK_PATTERN = re.compile(r"[\u3400-\u4dbf\u4e00-\u9fff]")


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def collect_audio_texts(words, sentences, scenes, letters):
    texts: list[str] = []
    texts.extend(r["indonesian"] for r in words if r.get("indonesian"))
    texts.extend(r["indonesian"] for r in sentences if r.get("indonesian"))
    texts.extend(r["indonesian"] for r in scenes if r.get("indonesian"))
    texts.extend(r["example"] for r in letters if r.get("example"))
    return list(dict.fromkeys(texts))


def count_cjk_in_vocabulary(rows) -> int:
    return sum(1 for row in rows if CJK_PATTERN.search(row.get("indonesian", "")))


TEXT_SCAN_SUFFIXES = {
    ".kt", ".kts", ".java", ".xml", ".gradle", ".properties",
    ".py", ".sh", ".yml", ".yaml", ".json", ".md", ".txt", ".tsv"
}


def scan_forbidden():
    hits = []
    for path in APP.rglob("*"):
        if not path.is_file():
            continue
        if "build" in path.parts:
            continue
        if path.suffix.lower() not in TEXT_SCAN_SUFFIXES:
            continue
        try:
            text = path.read_text("utf-8", errors="ignore")
        except OSError:
            continue
        for token in FORBIDDEN:
            if token.lower() in text.lower():
                hits.append((str(path.relative_to(ROOT)), token))
    return hits


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--strict-audio", action="store_true")
    args = parser.parse_args()

    word_files = [ASSETS / f"words_{i:02d}.tsv" for i in range(1, 12)]
    missing_word_files = [str(p) for p in word_files if not p.exists()]
    if missing_word_files:
        raise SystemExit(f"MISSING_WORD_FILES={missing_word_files}")

    words = [row for p in word_files for row in read_tsv(p)]
    sentences = read_tsv(ASSETS / "sentences.tsv")
    scenes = read_tsv(ASSETS / "scenes.tsv")
    letters = read_tsv(ASSETS / "letters.tsv")

    print(f"VOCABULARY_COUNT = {len(words)}")
    print(f"SENTENCE_COUNT = {len(sentences)}")
    print(f"SCENE_LINE_COUNT = {len(scenes)}")
    print(f"SCENE_COUNT = {len(set(r['scene_id'] for r in scenes))}")
    print(f"LETTER_COUNT = {len(letters)}")

    word_ids = [r["id"] for r in words if r.get("id")]
    indonesian_keys = [r["indonesian"].strip() for r in words if r.get("indonesian")]
    duplicate_word_ids = len(word_ids) - len(set(word_ids))
    duplicate_audio_words = len(indonesian_keys) - len(set(indonesian_keys))
    invalid_indonesian = count_cjk_in_vocabulary(words)

    print(f"WORD_DUPLICATE_IDS = {duplicate_word_ids}")
    print(f"WORD_DUPLICATE_AUDIO_KEYS = {duplicate_audio_words}")
    print(f"INVALID_INDONESIAN_TEXT = {invalid_indonesian}")

    texts = collect_audio_texts(words, sentences, scenes, letters)

    index_path = ASSETS / "tts_index.tsv"
    index: dict[str, dict[str, str]] = {}
    duplicate_audio_keys = 0

    if index_path.exists():
        for row in read_tsv(index_path):
            original_text = row.get("original_text", "").strip()
            key = row.get("sha256", "").strip().lower()
            if not original_text or not key:
                continue
            if key in index:
                duplicate_audio_keys += 1
                continue
            index[key] = row

    missing = [text for text in texts if sha256(text) not in index]
    real = len(texts) - len(missing)

    print(f"TOTAL_AUDIO_TEXTS = {len(texts)}")
    print(f"REAL_AUDIO = {real}")
    print(f"MISSING_AUDIO = {len(missing)}")
    print(f"INDEXED_AUDIO = {len(index)}")
    print(f"DUPLICATE_AUDIO_KEYS = {duplicate_audio_keys}")

    verified_license_markers = (
        "CC0", "CC BY", "CC-BY", "PUBLIC DOMAIN", "WIKIMEDIA"
    )
    licensed = sum(
        1
        for row in index.values()
        if any(marker in row.get("license", "").upper() for marker in verified_license_markers)
    )
    print(f"LICENSED_AUDIO = {licensed}")
    print(f"AUDIO_ASSET_PRESENT = {(ASSETS / 'tts_audio.m4a').exists()}")

    forbidden_hits = scan_forbidden()
    print(f"FORBIDDEN_API_HITS = {len(forbidden_hits)}")
    for path, token in forbidden_hits:
        print(f"FORBIDDEN: {path}: {token}")

    problems: list[str] = []

    if len(words) < 3020:
        problems.append(f"VOCABULARY_COUNT < 3020 ({len(words)})")
    if len(sentences) != 40:
        problems.append(f"SENTENCE_COUNT != 40 ({len(sentences)})")
    if len(set(r["scene_id"] for r in scenes)) != 10:
        problems.append("SCENE_COUNT != 10")

    scene_turn_counts: dict[str, int] = {}
    for row in scenes:
        scene_id = row.get("scene_id", "")
        turn = int(row.get("turn", "0") or 0)
        scene_turn_counts[scene_id] = max(scene_turn_counts.get(scene_id, 0), turn)
    if any(count < 6 for count in scene_turn_counts.values()):
        problems.append("A_SCENE_HAS_FEWER_THAN_6_TURNS")

    if len(letters) != 26:
        problems.append(f"LETTER_COUNT != 26 ({len(letters)})")
    if duplicate_word_ids:
        problems.append("WORD_DUPLICATE_IDS > 0")
    if duplicate_audio_words:
        problems.append("WORD_DUPLICATE_AUDIO_KEYS > 0")
    if invalid_indonesian:
        problems.append("INVALID_INDONESIAN_TEXT > 0")
    if forbidden_hits:
        problems.append("FORBIDDEN_API_HITS > 0")
    if duplicate_audio_keys:
        problems.append("DUPLICATE_AUDIO_KEYS > 0")
    if args.strict_audio and missing:
        problems.append("MISSING_AUDIO > 0 in strict mode")

    if problems:
        print("PROBLEMS:")
        for problem in problems:
            print(f"- {problem}")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
