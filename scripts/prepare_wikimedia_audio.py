#!/usr/bin/env python3
from __future__ import annotations

import csv
import hashlib
import os
import re
import subprocess
import tempfile
import time
import unicodedata
import wave
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.parse import unquote

import requests

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
API_URL = "https://commons.wikimedia.org/w/api.php"
CATEGORY = "Category:Lingua Libre pronunciation-ind"
UA = "IndonesianEngineerApp/0.4 (open-source pronunciation audio build; contact: mahanshengzhi-ai)"
MAX_AUDIO = 3704
DOWNLOAD_WORKERS = 4


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


def get_json(session: requests.Session, params: dict[str, str | int]) -> dict:
    delay = 2.0
    for attempt in range(9):
        try:
            response = session.get(API_URL, params=params, timeout=90)
            if response.status_code in (429, 500, 502, 503, 504):
                retry_after = response.headers.get("Retry-After")
                wait = float(retry_after) if retry_after else delay
                print(f"HTTP {response.status_code}; retrying in {wait:.1f}s")
                time.sleep(min(wait, 45.0))
                delay = min(delay * 1.8, 45.0)
                continue
            response.raise_for_status()
            return response.json()
        except (requests.RequestException, ValueError) as exc:
            if attempt == 8:
                raise
            print(f"request error: {exc}; retrying in {delay:.1f}s")
            time.sleep(delay)
            delay = min(delay * 1.8, 45.0)
    raise RuntimeError("request retry loop exhausted")


def list_category_files(session: requests.Session) -> list[str]:
    titles: list[str] = []
    cont: dict[str, str] = {}

    while True:
        params = {
            "action": "query",
            "format": "json",
            "list": "categorymembers",
            "cmtitle": CATEGORY,
            "cmnamespace": "6",
            "cmtype": "file",
            "cmlimit": 500,
            **cont,
        }
        data = get_json(session, params)
        titles.extend(
            item["title"]
            for item in data.get("query", {}).get("categorymembers", [])
            if item.get("title", "").lower().endswith(".wav")
        )
        if "continue" not in data:
            break
        cont = data["continue"]
        time.sleep(2.0)

    return titles


def transcription_candidates(title: str) -> list[str]:
    name = unquote(title.removeprefix("File:"))
    stem = Path(name).stem
    prefix = "LL-Q9240 (ind)-"
    if not stem.startswith(prefix):
        return []

    body = stem[len(prefix):]
    parts = body.split("-")
    candidates = [body]
    for index in range(1, len(parts)):
        candidates.append("-".join(parts[index:]))

    output: list[str] = []
    for candidate in candidates:
        output.append(candidate)
        output.append(candidate.replace("_", " "))
    return output


def choose_matches(titles: list[str], desired: list[str]) -> dict[str, str]:
    wanted = {normalize(text): text for text in desired}
    found: dict[str, str] = {}

    for title in titles:
        for candidate in transcription_candidates(title):
            key = normalize(candidate)
            if key in wanted and key not in found:
                found[key] = title
                break

    return found


def metadata_for_titles(
    session: requests.Session,
    titles: list[str],
) -> dict[str, dict[str, str]]:
    output: dict[str, dict[str, str]] = {}
    for start in range(0, len(titles), 50):
        batch = titles[start:start + 50]
        data = get_json(
            session,
            {
                "action": "query",
                "format": "json",
                "prop": "imageinfo",
                "iiprop": "url|extmetadata",
                "titles": "|".join(batch),
            },
        )
        pages = data.get("query", {}).get("pages", {})
        for page in pages.values():
            title = page.get("title", "")
            infos = page.get("imageinfo", [])
            if not infos:
                continue
            info = infos[0]
            meta = info.get("extmetadata", {})
            license_name = meta.get("LicenseShortName", {}).get("value", "")
            clean = re.sub(r"<[^>]+>", "", license_name).upper()
            if "CC0" not in clean and "CC-ZERO" not in clean:
                continue
            url = info.get("url", "")
            if url:
                output[title] = {
                    "url": url,
                    "license": "CC0",
                }
        time.sleep(0.5)
    return output


def download_one(
    session_factory,
    item: tuple[str, str, dict[str, str]],
    out_dir: Path,
) -> tuple[str, Path, str]:
    text, title, meta = item
    session = session_factory()
    tmp = out_dir / (hashlib.sha1(title.encode("utf-8")).hexdigest() + ".wav")
    raw = tmp.with_suffix(".source")
    delay = 2.0

    for attempt in range(8):
        try:
            with session.get(meta["url"], headers={"User-Agent": UA}, timeout=120, stream=True) as response:
                if response.status_code in (429, 500, 502, 503, 504):
                    retry_after = response.headers.get("Retry-After")
                    wait = float(retry_after) if retry_after else delay
                    time.sleep(min(wait, 30.0))
                    delay = min(delay * 1.8, 30.0)
                    continue
                response.raise_for_status()
                with raw.open("wb") as f:
                    for chunk in response.iter_content(256 * 1024):
                        if chunk:
                            f.write(chunk)

            subprocess.run(
                [
                    "ffmpeg", "-y", "-loglevel", "error",
                    "-i", str(raw),
                    "-ac", "1", "-ar", "24000",
                    "-af", "volume=0.88,apad=pad_dur=0.12",
                    str(tmp),
                ],
                check=True,
            )
            raw.unlink(missing_ok=True)
            return text, tmp, title
        except (requests.RequestException, subprocess.CalledProcessError) as exc:
            if attempt == 7:
                raw.unlink(missing_ok=True)
                raise RuntimeError(f"audio failed for {title}: {exc}")
            time.sleep(delay)
            delay = min(delay * 1.8, 30.0)

    raise RuntimeError(f"audio download retry exhausted for {title}")


def duration_ms(path: Path) -> int:
    with wave.open(str(path), "rb") as wav:
        frames = wav.getnframes()
        rate = wav.getframerate()
    return max(1, int(frames * 1000 / rate))


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    audio_output = ASSETS / "tts_audio.m4a"
    index_output = ASSETS / "tts_index.tsv"
    audio_output.unlink(missing_ok=True)
    index_output.unlink(missing_ok=True)

    desired = desired_texts()
    session = requests.Session()
    session.headers["User-Agent"] = UA

    print("DESIRED_AUDIO_TEXTS =", len(desired))
    titles = list_category_files(session)
    print("CATEGORY_FILES =", len(titles))

    matches = choose_matches(titles, desired)
    print("EXACT_MATCHES =", len(matches))
    if not matches:
        raise SystemExit("No exact CC0 candidate matches found")

    desired_by_key = {normalize(text): text for text in desired}
    exact_items = [
        (desired_by_key[key], title)
        for key, title in matches.items()
        if key in desired_by_key
    ]

    metadata = metadata_for_titles(session, [title for _, title in exact_items])
    licensed_items = [
        (text, title, metadata[title])
        for text, title in exact_items
        if title in metadata
    ]
    print("CC0_MATCHES =", len(licensed_items))

    selected = licensed_items[:MAX_AUDIO]
    selected.sort(key=lambda x: desired.index(x[0]))

    concat_parts: list[Path] = []
    rows: list[tuple[str, int, int, str, str, str]] = []
    cursor_ms = 0

    with tempfile.TemporaryDirectory(prefix="indonesian-audio-") as temp_dir:
        temp_root = Path(temp_dir)

        def session_factory():
            s = requests.Session()
            s.headers["User-Agent"] = UA
            return s

        with ThreadPoolExecutor(max_workers=DOWNLOAD_WORKERS) as executor:
            futures = [
                executor.submit(session_factory and download_one, session_factory, item, temp_root)
                for item in selected
            ]
            ordered_results: dict[int, tuple[str, Path, str]] = {}
            for index, future in enumerate(as_completed(futures)):
                ordered_results[index] = future.result()

        # Re-sort by desired text order so index and concatenation remain deterministic.
        ordered = sorted(ordered_results.values(), key=lambda x: desired.index(x[0]))
        for text, normalized_path, source_title in ordered:
            start = cursor_ms
            duration = duration_ms(normalized_path)
            concat_parts.append(normalized_path)
            rows.append((
                sha256(text),
                start,
                duration,
                text,
                source_title,
                "CC0 / Wikimedia Commons Lingua Libre",
            ))
            cursor_ms += duration

        concat_file = temp_root / "concat.txt"
        concat_file.write_text(
            "".join(
                "file '" + str(path).replace("'", "'\\''") + "'\n"
                for path in concat_parts
            ),
            encoding="utf-8",
        )

        subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error",
                "-f", "concat", "-safe", "0",
                "-i", str(concat_file),
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
        writer.writerows(rows)

    print("INDEXED_AUDIO =", len(rows))
    print("MISSING_AUDIO =", len(desired) - len(rows))
    print("AUDIO_ASSET_PRESENT =", audio_output.exists())
    print("AUDIO_ASSET_BYTES =", audio_output.stat().st_size if audio_output.exists() else 0)


if __name__ == "__main__":
    main()
