#!/usr/bin/env python3
from __future__ import annotations

import asyncio
import csv
import hashlib
import os
import shutil
import subprocess
import shlex
from dataclasses import dataclass
from pathlib import Path

import edge_tts
from mutagen.mp3 import MP3

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
WORK = ROOT / "build" / "generated_audio"
CLIPS = WORK / "clips"
VOICE = os.environ.get("INDONESIAN_VOICE", "id-ID-ArdiNeural")
RATE = os.environ.get("INDONESIAN_RATE", "-12%")
VOLUME = os.environ.get("INDONESIAN_VOLUME", "+0%")
PITCH = os.environ.get("INDONESIAN_PITCH", "+0Hz")
PAUSE_MS = 120
CONCURRENCY = int(os.environ.get("AUDIO_CONCURRENCY", "8"))
RETRY_COUNT = int(os.environ.get("AUDIO_RETRY_COUNT", "5"))

@dataclass(frozen=True)
class AudioText:
    text: str
    source: str

def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))

def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()

def collect_texts() -> list[AudioText]:
    result: list[AudioText] = []
    seen: set[str] = set()

    def add(text: str, source: str) -> None:
        normalized = text.strip()
        if not normalized or normalized in seen:
            return
        seen.add(normalized)
        result.append(AudioText(normalized, source))

    for i in range(1, 12):
        path = ASSETS / f"words_{i:02d}.tsv"
        for row in read_tsv(path):
            add(row.get("indonesian", ""), str(path.relative_to(ROOT)))

    for row in read_tsv(ASSETS / "sentences.tsv"):
        add(row.get("indonesian", ""), "sentences.tsv")

    for row in read_tsv(ASSETS / "scenes.tsv"):
        add(row.get("indonesian", ""), "scenes.tsv")

    for row in read_tsv(ASSETS / "letters.tsv"):
        add(row.get("example", ""), "letters.tsv")

    return result

async def synthesize_one(item: AudioText, semaphore: asyncio.Semaphore) -> Path:
    key = sha256(item.text)
    output = CLIPS / f"{key}.mp3"
    if output.exists() and output.stat().st_size > 1024:
        return output

    async with semaphore:
        last_error: Exception | None = None
        for attempt in range(1, RETRY_COUNT + 1):
            temp = output.with_suffix(".tmp.mp3")
            try:
                communicate = edge_tts.Communicate(
                    text=item.text,
                    voice=VOICE,
                    rate=RATE,
                    volume=VOLUME,
                    pitch=PITCH,
                )
                await communicate.save(str(temp))
                if temp.stat().st_size <= 1024:
                    raise RuntimeError("generated audio is empty")
                temp.replace(output)
                return output
            except Exception as exc:
                last_error = exc
                temp.unlink(missing_ok=True)
                await asyncio.sleep(min(2.0 * attempt, 10.0))
        raise RuntimeError(f"failed after {RETRY_COUNT} attempts: {item.text}") from last_error

async def synthesize_all(items: list[AudioText]) -> list[Path]:
    CLIPS.mkdir(parents=True, exist_ok=True)
    semaphore = asyncio.Semaphore(CONCURRENCY)
    results: list[Path | None] = [None] * len(items)
    completed = 0
    lock = asyncio.Lock()

    async def run(index: int, item: AudioText):
        nonlocal completed
        path = await synthesize_one(item, semaphore)
        results[index] = path
        async with lock:
            completed += 1
            if completed % 50 == 0 or completed == len(items):
                print(f"AUDIO_GENERATED = {completed}/{len(items)}", flush=True)

    await asyncio.gather(*(run(i, item) for i, item in enumerate(items)))
    return [p for p in results if p is not None]

def run(cmd: list[str]) -> None:
    print("RUN:", " ".join(cmd), flush=True)
    subprocess.run(cmd, check=True)

def main() -> None:
    if shutil.which("ffmpeg") is None:
        raise SystemExit("ffmpeg is required")
    if shutil.which("ffprobe") is None:
        raise SystemExit("ffprobe is required")

    items = collect_texts()
    print(f"TOTAL_AUDIO_TEXTS = {len(items)}", flush=True)

    asyncio.run(synthesize_all(items))

    silence = WORK / "silence.mp3"
    run([
        "ffmpeg", "-y",
        "-f", "lavfi",
        "-i", "anullsrc=channel_layout=mono:sample_rate=24000",
        "-t", f"{PAUSE_MS / 1000:.3f}",
        "-ar", "24000",
        "-ac", "1",
        "-c:a", "libmp3lame",
        "-b:a", "48k",
        str(silence),
    ])

    concat_list = WORK / "concat.txt"
    index_rows: list[tuple[str, int, int, str, str, str]] = []
    current_ms = 0
    list_lines: list[str] = []

    for item in items:
        path = CLIPS / f"{sha256(item.text)}.mp3"
        duration_ms = round(MP3(path).info.length * 1000)
        start_ms = current_ms
        index_rows.append((
            sha256(item.text),
            start_ms,
            duration_ms,
            item.text,
            item.source,
            "BUILD_TIME_VOICE_UNVERIFIED",
        ))
        list_lines.append(f"file {shlex.quote(path.as_posix())}")
        list_lines.append(f"file {shlex.quote(silence.as_posix())}")
        current_ms += duration_ms + PAUSE_MS

    concat_list.write_text("\n".join(list_lines) + "\n", encoding="utf-8")

    output = ASSETS / "tts_audio.m4a"
    run([
        "ffmpeg", "-y",
        "-f", "concat", "-safe", "0",
        "-i", str(concat_list),
        "-ar", "24000",
        "-ac", "1",
        "-c:a", "aac",
        "-b:a", "64k",
        "-movflags", "+faststart",
        str(output),
    ])

    index_path = ASSETS / "tts_index.tsv"
    with index_path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="\t", lineterminator="\n")
        writer.writerow(["sha256", "start_ms", "duration_ms", "original_text", "source_file", "license"])
        writer.writerows(index_rows)

    print(f"REAL_AUDIO = {len(index_rows)}", flush=True)
    print(f"MISSING_AUDIO = {len(items) - len(index_rows)}", flush=True)
    print(f"INDEXED_AUDIO = {len(index_rows)}", flush=True)
    print(f"DUPLICATE_AUDIO_KEYS = {len(index_rows) - len({row[0] for row in index_rows})}", flush=True)
    print(f"AUDIO_BYTES = {output.stat().st_size}", flush=True)

if __name__ == "__main__":
    main()
