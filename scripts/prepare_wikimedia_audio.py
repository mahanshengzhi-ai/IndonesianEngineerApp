#!/usr/bin/env python3
from __future__ import annotations

import csv
import hashlib
import os
import re
import shutil
import subprocess
import tempfile
import unicodedata
import zipfile
from pathlib import Path
from urllib.parse import unquote

import requests

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
DATASET_URL = "https://lingualibre.org/datasets/Q305-ind-Indonesian.zip"
UA = "IndonesianEngineerApp/0.3 (LinguaLibre dataset audio build; contact: mahanshengzhi-ai)"
MAX_AUDIO = 3704


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFC", text or "")
    text = text.replace("_", " ")
    return re.sub(r"\s+", " ", text.strip()).lower()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def desired_texts() -> list[str]:
    texts: list[str] = []
    for i in range(1, 12):
        for row in read_tsv(ASSETS / f"words_{i:02d}.tsv"):
            if row.get("indonesian"):
                texts.append(row["indonesian"])
    for name in ("sentences.tsv", "scenes.tsv"):
        for row in read_tsv(ASSETS / name):
            if row.get("indonesian"):
                texts.append(row["indonesian"])
    for row in read_tsv(ASSETS / "letters.tsv"):
        if row.get("example"):
            texts.append(row["example"])

    unique: dict[str, str] = {}
    for text in texts:
        unique.setdefault(normalize(text), text)
    return list(unique.values())


def dataset_path() -> Path:
    cache_dir = Path(os.environ.get("AUDIO_CACHE_DIR", "/tmp/indonesian-engineer-audio-cache"))
    cache_dir.mkdir(parents=True, exist_ok=True)
    return cache_dir / "Q305-ind-Indonesian.zip"


def download_dataset(path: Path) -> None:
    if path.exists() and path.stat().st_size > 1_000_000 and zipfile.is_zipfile(path):
        return

    path.unlink(missing_ok=True)
    tmp = path.with_suffix(".part")
    subprocess.run(
        [
            "curl", "-fL",
            "--retry", "6",
            "--retry-delay", "3",
            "--connect-timeout", "30",
            "--max-time", "600",
            "-A", UA,
            "-o", str(tmp),
            DATASET_URL,
        ],
        check=True,
    )
    if not zipfile.is_zipfile(tmp):
        with tmp.open("rb") as f:
            head = f.read(32)
        raise RuntimeError(
            "LinguaLibre dataset download is not a ZIP; "
            f"size={tmp.stat().st_size}, first_bytes={head!r}"
        )
    tmp.replace(path)


def transcription_candidates(filename: str) -> list[str]:
    name = unquote(Path(filename).name)
    stem = Path(name).stem
    prefix = "LL-Q9240 (ind)-"
    if not stem.startswith(prefix):
        return []

    body = stem[len(prefix):]
    parts = body.split("-")
    candidates: list[str] = []

    # Exact body first, then progressively remove recorder/speaker prefixes.
    candidates.append(body)
    for index in range(1, len(parts)):
        candidates.append("-".join(parts[index:]))

    # Archive builders may normalize spaces to underscores.
    result = []
    for candidate in candidates:
        result.append(candidate)
        result.append(candidate.replace("_", " "))
    return result


def find_audio_files(root: Path, desired: list[str]) -> dict[str, Path]:
    wanted = {normalize(text): text for text in desired}
    found: dict[str, Path] = {}

    for path in root.rglob("*"):
        if not path.is_file() or path.suffix.lower() not in {".wav", ".ogg", ".mp3"}:
            continue

        for candidate in transcription_candidates(path.name):
            key = normalize(candidate)
            if key in wanted and key not in found:
                found[key] = path
                break

    return found


def duration_ms(path: Path) -> int:
    result = subprocess.run(
        [
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            str(path),
        ],
        capture_output=True, text=True, check=True,
    )
    return max(1, int(float(result.stdout.strip()) * 1000))


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    audio_output = ASSETS / "tts_audio.m4a"
    index_output = ASSETS / "tts_index.tsv"
    audio_output.unlink(missing_ok=True)
    index_output.unlink(missing_ok=True)

    desired = desired_texts()
    archive = dataset_path()
    download_dataset(archive)

    with tempfile.TemporaryDirectory(prefix="lingualibre-ind-") as tmp:
        extracted = Path(tmp) / "dataset"
        extracted.mkdir()
        with zipfile.ZipFile(archive) as zf:
            zf.extractall(extracted)

        matches = find_audio_files(extracted, desired)
        print("GENERATED_TEXTS =", len(desired))
        print("DATASET_ARCHIVE_BYTES =", archive.stat().st_size)
        print("EXACT_MATCHES =", len(matches))

        if not matches:
            index_output.write_text(
                "sha256\tstart_ms\tduration_ms\toriginal_text\tsource_file\tlicense\n",
                encoding="utf-8",
            )
            raise SystemExit("No exact Lingua Libre matches found")

        selected = []
        wanted_by_key = {normalize(text): text for text in desired}
        for key, source in matches.items():
            selected.append((wanted_by_key[key], source))
            if len(selected) >= MAX_AUDIO:
                break

        normalized_files: list[Path] = []
        index_rows = []
        cursor_ms = 0

        for number, (text, source) in enumerate(selected):
            normalized = Path(tmp) / f"{number:04d}_norm.wav"

            subprocess.run(
                [
                    "ffmpeg", "-y", "-loglevel", "error",
                    "-i", str(source),
                    "-ac", "1", "-ar", "24000",
                    "-af", "apad=pad_dur=0.12",
                    str(normalized),
                ],
                check=True,
            )

            duration = duration_ms(normalized)
            key = hashlib.sha256(text.encode("utf-8")).hexdigest()
            normalized_files.append(normalized)
            index_rows.append([
                key,
                cursor_ms,
                duration,
                text,
                source.name,
                "CC0 / Lingua Libre pronunciation dataset; verify individual source page",
            ])
            cursor_ms += duration

        concat = Path(tmp) / "concat.txt"
        concat.write_text(
            "".join(
                "file '" + str(path).replace("'", "'\\''") + "'\n"
                for path in normalized_files
            ),
            encoding="utf-8",
        )

        subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error",
                "-f", "concat", "-safe", "0",
                "-i", str(concat),
                "-c:a", "aac", "-b:a", "96k",
                "-ar", "24000", "-ac", "1",
                str(audio_output),
            ],
            check=True,
        )

        with index_output.open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter="\t", lineterminator="\n")
            writer.writerow([
                "sha256", "start_ms", "duration_ms",
                "original_text", "source_file", "license",
            ])
            writer.writerows(index_rows)

        print("INDEXED_AUDIO =", len(index_rows))
        print("MISSING_AUDIO =", len(desired) - len(index_rows))
        print("AUDIO_FILE_SHA256 =", sha256_file(audio_output))


if __name__ == "__main__":
    main()
