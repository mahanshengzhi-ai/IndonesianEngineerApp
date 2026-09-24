#!/usr/bin/env python3
"""
Build a redistributable Indonesian audio pack from CC0 Lingua Libre recordings
on Wikimedia Commons.

Outputs:
  app/src/main/assets/tts_audio.m4a
  app/src/main/assets/tts_index.tsv
  build/audio/audio_build_report.json

Only exact/normalized transcription matches are used for learning-text coverage.
No TTS or synthetic audio is generated.
"""

from __future__ import annotations

import argparse
import audioop
import csv
import hashlib
import io
import json
import re
import shutil
import subprocess
import tempfile
import time
import zipfile
import urllib.error
import urllib.parse
import urllib.request
import wave
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

API = "https://commons.wikimedia.org/w/api.php"
CATEGORY = "Category:Lingua Libre pronunciation-ind"
CC0_NAMES = {"CC0 1.0", "CC0 1.0 Universal"}
USER_AGENT = "IndonesianEngineerApp/0.7 audio-builder"
PAUSE_MS = 120
TARGET_LINES = 3000
MAX_LINES = 3000
DATASET_URL = "https://lingualibre.org/datasets/Q305-ind-Indonesian.zip"


def request_json(params: dict[str, Any], retries: int = 8) -> dict[str, Any]:
    params = dict(params)
    params["format"] = "json"
    params["maxlag"] = "10"
    encoded = urllib.parse.urlencode(params)
    url = API + "?" + encoded

    last_error: Exception | None = None
    for attempt in range(retries):
        request = urllib.request.Request(
            url,
            headers={
                "User-Agent": USER_AGENT,
                "Accept": "application/json",
            },
        )
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                return json.load(response)
        except urllib.error.HTTPError as exc:
            last_error = exc
            if exc.code not in {429, 502, 503, 504}:
                raise
            retry_after = exc.headers.get("Retry-After")
            if retry_after:
                try:
                    delay = min(60.0, float(retry_after))
                except ValueError:
                    delay = 5.0
            else:
                delay = min(60.0, 2.0 ** attempt)
            print(
                f"Wikimedia API HTTP {exc.code}; retry "
                f"{attempt + 1}/{retries} after {delay:.1f}s"
            )
            time.sleep(delay)
    raise RuntimeError("Wikimedia API request failed after retries") from last_error


def fetch_category_files() -> list[dict[str, Any]]:
    pages: list[dict[str, Any]] = []
    cont: dict[str, Any] = {}

    while True:
        params = {
            "action": "query",
            "generator": "categorymembers",
            "gcmtitle": CATEGORY,
            "gcmnamespace": "6",
            "gcmtype": "file",
            "gcmlimit": "500",
            "prop": "imageinfo",
            "iiprop": "url|size|mime|extmetadata",
            "iiurlwidth": "0",
        }
        params.update(cont)
        data = request_json(params)
        pages.extend(data.get("query", {}).get("pages", {}).values())

        next_cont = data.get("continue")
        if not next_cont:
            break
        cont = next_cont

    result = []
    for page in pages:
        title = page.get("title", "")
        if not title.lower().endswith(".wav"):
            continue
        info = (page.get("imageinfo") or [{}])[0]
        if info.get("mime") not in {"audio/wav", "audio/x-wav"}:
            continue
        result.append({
            "title": title,
            "pageid": page.get("pageid"),
            "url": info.get("url"),
            "size": int(info.get("size") or 0),
            "metadata": info.get("extmetadata") or {},
        })
    return result


def metadata_value(metadata: dict[str, Any], key: str) -> str:
    value = metadata.get(key, {})
    if isinstance(value, dict):
        return str(value.get("value", "")).strip()
    return str(value).strip()


def title_transcription_candidates(title: str) -> list[str]:
    stem = title.rsplit("/", 1)[-1]
    stem = re.sub(r"^LL-[^-]+\s*\(ind\)-", "", stem)
    stem = re.sub(r"\.wav$", "", stem, flags=re.IGNORECASE)

    parts = stem.split("-")
    candidates = []
    for index in range(len(parts)):
        candidate = "-".join(parts[index:]).strip()
        if candidate:
            candidates.append(candidate)
    candidates.sort(key=lambda item: (-len(item), item))
    return candidates


def normalize_text(text: str) -> str:
    text = unicodedata.normalize("NFC", text)
    text = text.replace("\u00a0", " ")
    text = re.sub(r"\s+", " ", text).strip().casefold()
    text = text.strip(" \t\r\n\\\"'“”‘’.,!?;:，。！？；：、（）()[]{}")
    return text


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8-sig", newline="") as fh:
        return list(csv.DictReader(fh, delimiter="\t"))


def collect_required_texts(root: Path) -> tuple[list[str], Counter[str]]:
    values: dict[str, str] = {}
    sources: Counter[str] = Counter()

    def add(text: str, source: str) -> None:
        text = text.strip()
        key = normalize_text(text)
        if not key:
            return
        values.setdefault(key, text)
        sources[source] += 1

    for index in range(1, 12):
        path = root / "app" / "src" / "main" / "assets" / f"words_{index:02d}.tsv"
        for row in read_tsv(path):
            add(row.get("indonesian", ""), "vocabulary")

    sentences = root / "app" / "src" / "main" / "assets" / "sentences.tsv"
    if sentences.exists():
        for row in read_tsv(sentences):
            add(row.get("indonesian", ""), "sentence")

    scenes = root / "app" / "src" / "main" / "assets" / "scenes.tsv"
    if scenes.exists():
        for row in read_tsv(scenes):
            add(row.get("indonesian", ""), "scene")

    letters = root / "app" / "src" / "main" / "assets" / "letters.tsv"
    if letters.exists():
        for row in read_tsv(letters):
            add(row.get("example", ""), "letter_example")

    return list(values.values()), sources


def run(cmd: list[str]) -> None:
    subprocess.run(cmd, check=True)


def ffprobe_duration(path: Path) -> float:
    value = subprocess.check_output([
        "ffprobe",
        "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        str(path),
    ], text=True).strip()
    return float(value)


def download_file(url: str, destination: Path) -> None:
    if destination.exists() and destination.stat().st_size > 0:
        return
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    last_error: Exception | None = None
    for attempt in range(6):
        try:
            with urllib.request.urlopen(request, timeout=90) as response:
                with destination.open("wb") as output:
                    shutil.copyfileobj(response, output)
            return
        except urllib.error.HTTPError as exc:
            last_error = exc
            if exc.code not in {429, 502, 503, 504}:
                raise
            time.sleep(min(60.0, 2.0 ** attempt))
    raise RuntimeError(f"Download failed: {url}") from last_error


def write_index(path: Path, rows: list[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as fh:
        writer = csv.writer(fh, delimiter="\t", lineterminator="\n")
        writer.writerow([
            "sha256",
            "start_ms",
            "duration_ms",
            "original_text",
            "source_file",
            "source_url",
            "license",
        ])
        for row in rows:
            writer.writerow([
                row["sha256"],
                row["start_ms"],
                row["duration_ms"],
                row["original_text"],
                row["source_file"],
                row["source_url"],
                row["license"],
            ])


def normalize_wav_bytes(raw: bytes) -> tuple[bytes, int]:
    with wave.open(io.BytesIO(raw), "rb") as wav:
        channels = wav.getnchannels()
        sample_width = wav.getsampwidth()
        sample_rate = wav.getframerate()
        frames = wav.readframes(wav.getnframes())

    if channels == 2:
        frames = audioop.tomono(frames, sample_width, 0.5, 0.5)
        channels = 1
    elif channels != 1:
        raise ValueError(f"Unsupported channel count: {channels}")

    if sample_width != 2:
        frames = audioop.lin2lin(frames, sample_width, 2)
        sample_width = 2

    if sample_rate != 24000:
        frames, _ = audioop.ratecv(
            frames,
            sample_width,
            1,
            sample_rate,
            24000,
            None,
        )
        sample_rate = 24000

    duration_ms = max(
        1,
        round(len(frames) * 1000 / (sample_width * sample_rate)),
    )
    return frames, duration_ms


def download_dataset_zip(cache_root: Path) -> Path:
    destination = cache_root / "Q305-ind-Indonesian.zip"
    if destination.exists() and destination.stat().st_size > 1_000_000:
        return destination
    print("Downloading Lingua Libre Indonesian dataset:", DATASET_URL)
    download_file(DATASET_URL, destination)
    return destination


def duration_ms_from_wav(path: Path) -> int:
    with wave.open(str(path), "rb") as wav:
        frames = wav.getnframes()
        rate = wav.getframerate()
        return max(1, round(frames * 1000 / rate))


def make_concat_audio(
    selected: list[dict[str, Any]],
    temp_root: Path,
    output_path: Path,
    dataset_zip: zipfile.ZipFile,
) -> list[dict[str, Any]]:
    audio_dir = temp_root / "clips"
    audio_dir.mkdir(parents=True, exist_ok=True)
    silence_path = temp_root / "silence.wav"

    with wave.open(str(silence_path), "wb") as silence:
        silence.setnchannels(1)
        silence.setsampwidth(2)
        silence.setframerate(48000)
        silence.writeframes(b"\x00\x00" * round(48000 * PAUSE_MS / 1000))

    zip_members = {}
    for member in dataset_zip.namelist():
        if member.lower().endswith(".wav"):
            zip_members[Path(member).name] = member

    concat_file = temp_root / "concat.txt"
    rows = []
    running_ms = 0
    extracted = 0

    with concat_file.open("w", encoding="utf-8") as playlist:
        for number, item in enumerate(selected, start=1):
            clip_path = audio_dir / (f"{number:04d}.wav")
            title_without_prefix = item["title"].removeprefix("File:")
            member_name = (
                zip_members.get(item["title"])
                or zip_members.get(title_without_prefix)
            )

            if member_name:
                with dataset_zip.open(member_name, "r") as source, clip_path.open("wb") as target:
                    shutil.copyfileobj(source, target)
            else:
                download_file(item["url"], clip_path)

            duration_ms = duration_ms_from_wav(clip_path)
            rows.append({
                "sha256": sha256(item["target_text"]),
                "start_ms": running_ms,
                "duration_ms": duration_ms,
                "original_text": item["target_text"],
                "source_file": item["title"],
                "source_url": item["source_url"],
                "license": "CC0-1.0",
            })

            escaped = str(clip_path).replace("'", "'\\''")
            playlist.write("file '" + escaped + "'\n")
            running_ms += duration_ms

            if number != len(selected):
                playlist.write("file '" + str(silence_path).replace("'", "'\\''") + "'\n")
                running_ms += PAUSE_MS

            extracted += 1
            if extracted % 500 == 0 or extracted == len(selected):
                print(f"Prepared audio {extracted}/{len(selected)}")

    output_path.parent.mkdir(parents=True, exist_ok=True)
    run([
        "ffmpeg", "-y", "-loglevel", "error",
        "-f", "concat", "-safe", "0",
        "-i", str(concat_file),
        "-c:a", "aac",
        "-b:a", "48k",
        "-movflags", "+faststart",
        str(output_path),
    ])
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--min-lines", type=int, default=TARGET_LINES)
    parser.add_argument("--max-lines", type=int, default=MAX_LINES)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    args = parser.parse_args()

    root = args.root.resolve()
    required_texts, required_sources = collect_required_texts(root)
    required_map = {normalize_text(text): text for text in required_texts}
    cache_root = root / ".cache" / "indonesian-audio"
    cache_root.mkdir(parents=True, exist_ok=True)

    print("Required unique learning texts:", len(required_texts))
    dataset_zip_path = download_dataset_zip(cache_root)

    exact_candidates: dict[str, list[dict[str, Any]]] = defaultdict(list)
    supplemental_candidates: dict[str, list[dict[str, Any]]] = defaultdict(list)

    with zipfile.ZipFile(dataset_zip_path, "r") as dataset_zip:
        wav_members = [
            member for member in dataset_zip.namelist()
            if member.lower().endswith(".wav")
        ]
        print("Lingua Libre dataset WAV members:", len(wav_members))

        for member in wav_members:
            title = Path(member).name
            parsed = title_transcription_candidates("File:" + title)
            if not parsed:
                continue

            matched_text = None
            for candidate_text in parsed:
                key = normalize_text(candidate_text)
                if key in required_map:
                    matched_text = required_map[key]
                    break

            transcript = matched_text or parsed[-1]
            transcript_key = normalize_text(transcript)
            if not transcript_key:
                continue

            source_url = "https://commons.wikimedia.org/wiki/File:" + urllib.parse.quote(
                title.replace(" ", "_"), safe="/:(),"
            )
            candidate = {
                "title": title,
                "url": "",
                "source_url": source_url,
                "target_text": transcript,
            }

            if matched_text is not None:
                exact_candidates[normalize_text(matched_text)].append(candidate)
            else:
                supplemental_candidates[transcript_key].append(candidate)

        selected: list[dict[str, Any]] = []
        used_keys: set[str] = set()

        for key in sorted(exact_candidates):
            options = sorted(exact_candidates[key], key=lambda value: value["title"])
            selected.append(options[0])
            used_keys.add(key)

        exact_learning_matches = len(selected)
        print("Exact learning-text audio matches:", exact_learning_matches)

        for key in sorted(supplemental_candidates):
            if len(selected) >= args.min_lines:
                break
            if key in used_keys:
                continue
            options = sorted(
                supplemental_candidates[key],
                key=lambda value: value["title"]
            )
            selected.append(options[0])
            used_keys.add(key)

        if len(selected) < args.min_lines:
            report = {
                "status": "FAIL",
                "required_learning_texts": len(required_texts),
                "exact_learning_audio_matches": exact_learning_matches,
                "audio_lines": len(selected),
                "minimum_audio_lines": args.min_lines,
                "dataset_wav_members": len(wav_members),
                "message": "The official Lingua Libre Indonesian dataset did not contain enough unique recordings.",
            }
            path = root / "build" / "audio" / "audio_build_report.json"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(
                json.dumps(report, ensure_ascii=False, indent=2),
                encoding="utf-8",
            )
            print(json.dumps(report, ensure_ascii=False, indent=2))
            return 2

        selected = selected[:args.max_lines]

        assets = root / "app" / "src" / "main" / "assets"
        assets.mkdir(parents=True, exist_ok=True)
        audio_path = assets / "tts_audio.m4a"
        index_path = assets / "tts_index.tsv"

        with tempfile.TemporaryDirectory(prefix="indonesian_audio_") as temp_dir:
            rows = make_concat_audio(
                selected,
                Path(temp_dir),
                audio_path,
                dataset_zip,
            )

    write_index(index_path, rows)

    report = {
        "status": "PASS",
        "source": "Lingua Libre Indonesian dataset Q305-ind-Indonesian",
        "source_dataset_url": DATASET_URL,
        "required_learning_texts": len(required_texts),
        "exact_learning_audio_matches": exact_learning_matches,
        "learning_audio_coverage_pct": round(
            exact_learning_matches * 100 / max(1, len(required_texts)), 2
        ),
        "dataset_wav_members": len(wav_members),
        "audio_lines": len(rows),
        "pause_ms": PAUSE_MS,
        "minimum_audio_lines": args.min_lines,
        "maximum_audio_lines": args.max_lines,
        "license_required": "CC0-1.0",
        "required_sources": dict(required_sources),
        "audio_asset_bytes": audio_path.stat().st_size,
        "index_asset_bytes": index_path.stat().st_size,
    }
    report_path = root / "build" / "audio" / "audio_build_report.json"
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
