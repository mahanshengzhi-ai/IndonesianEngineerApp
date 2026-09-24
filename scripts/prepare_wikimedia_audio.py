#!/usr/bin/env python3
from __future__ import annotations

import csv
import hashlib
import re
import subprocess
import tempfile
import os
import shutil
import time
import unicodedata
from pathlib import Path
from urllib.parse import unquote

import requests

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
CATEGORY = "Category:Lingua Libre pronunciation-ind"
API = "https://commons.wikimedia.org/w/api.php"
UA = "IndonesianEngineerApp/0.1 (open-source audio build)"
MAX_AUDIO = 800

def normalize(text: str) -> str:
    text = unicodedata.normalize("NFC", text or "")
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
            key = "indonesian"
            if row.get(key):
                texts.append(row[key])
    for row in read_tsv(ASSETS / "letters.tsv"):
        if row.get("example"):
            texts.append(row["example"])
    unique: dict[str, str] = {}
    for text in texts:
        unique.setdefault(normalize(text), text)
    return list(unique.values())

def get_json(session: requests.Session, params: dict) -> dict:
    for attempt in range(7):
        response = session.get(API, params=params, timeout=60)
        if response.status_code != 429:
            response.raise_for_status()
            return response.json()
        retry_after = response.headers.get("Retry-After")
        delay = float(retry_after) if retry_after else min(30.0, 2.0 ** attempt)
        print("Wikimedia API rate limited; retrying in", delay, "seconds")
        time.sleep(delay)
    raise RuntimeError("Wikimedia API remained rate-limited after retries")

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
            "cmlimit": 200,
            **cont,
        }
        data = get_json(session, params)
        titles.extend(item["title"] for item in data.get("query", {}).get("categorymembers", []))
        if "continue" not in data:
            break
        time.sleep(1.5)
        cont = {
            "cmcontinue": data["continue"]["cmcontinue"],
            "continue": data["continue"]["continue"],
        }
    return titles

def transcription_from_title(title: str) -> str:
    name = title.removeprefix("File:")
    match = re.match(r"^.+?-(.+)\.wav$", name, re.IGNORECASE)
    if not match:
        return ""
    return unquote(match.group(1))

def choose_matches(all_titles: list[str], desired: list[str]) -> dict[str, str]:
    wanted = {normalize(text): text for text in desired}
    found: dict[str, str] = {}
    for title in all_titles:
        if not title.lower().endswith(".wav"):
            continue
        transcription = transcription_from_title(title)
        key = normalize(transcription)
        if key in wanted and key not in found:
            found[key] = title
    return found

def metadata_for_titles(session: requests.Session, titles: list[str]) -> dict[str, dict]:
    result: dict[str, dict] = {}
    for start in range(0, len(titles), 50):
        batch = titles[start:start + 50]
        data = get_json(session, {
            "action": "query",
            "format": "json",
            "prop": "imageinfo",
            "iiprop": "url|extmetadata",
            "titles": "|".join(batch),
        })
        for page in data.get("query", {}).get("pages", {}).values():
            title = page.get("title")
            info = (page.get("imageinfo") or [{}])[0]
            meta = info.get("extmetadata") or {}
            license_name = (meta.get("LicenseShortName") or {}).get("value", "")
            if "CC0" in re.sub(r"<[^>]+>", "", license_name).upper():
                result[title] = {
                    "url": info.get("url", ""),
                    "license": license_name,
                    "description": (meta.get("ImageDescription") or {}).get("value", ""),
                }
    return result

def download_file(session: requests.Session, url: str, out: Path) -> None:
    cache_dir = Path(os.environ.get("AUDIO_CACHE_DIR", ""))
    if str(cache_dir):
        cache_dir.mkdir(parents=True, exist_ok=True)
        cache_file = cache_dir / (hashlib.sha256(url.encode("utf-8")).hexdigest() + ".wav")
        if cache_file.exists():
            shutil.copy2(cache_file, out)
            return
    with session.get(url, timeout=120, stream=True) as response:
        response.raise_for_status()
        with out.open("wb") as f:
            for chunk in response.iter_content(1024 * 256):
                if chunk:
                    f.write(chunk)
    if str(cache_dir):
        shutil.copy2(out, cache_file)

def duration_ms(path: Path) -> int:
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration",
         "-of", "default=noprint_wrappers=1:nokey=1", str(path)],
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
    for path in (ASSETS / "tts_audio.m4a", ASSETS / "tts_index.tsv"):
        path.unlink(missing_ok=True)

    session = requests.Session()
    session.headers["User-Agent"] = UA

    desired = desired_texts()
    all_titles = list_category_files(session)
    matches = choose_matches(all_titles, desired)
    meta = metadata_for_titles(session, list(matches.values()))

    selected = []
    for key, title in matches.items():
        if title not in meta:
            continue
        selected.append((meta[title]["url"], desired[[normalize(x) for x in desired].index(key)], title, meta[title]))
        if len(selected) >= MAX_AUDIO:
            break

    if not selected:
        print("No CC0 exact matches found; keeping audio index empty")
        (ASSETS / "tts_index.tsv").write_text(
            "sha256\tstart_ms\tduration_ms\toriginal_text\tsource_file\tlicense\n",
            encoding="utf-8",
        )
        return

    with tempfile.TemporaryDirectory(prefix="indonesian-audio-") as tmp:
        tmpdir = Path(tmp)
        normalized_files: list[Path] = []
        index_rows = []
        cursor_ms = 0

        for n, (url, text, source_title, license_meta) in enumerate(selected):
            raw = tmpdir / f"{n:04d}.wav"
            normalized = tmpdir / f"{n:04d}_norm.wav"
            download_file(session, url, raw)

            subprocess.run(
                [
                    "ffmpeg", "-y", "-loglevel", "error",
                    "-i", str(raw),
                    "-ac", "1", "-ar", "24000",
                    "-af", "apad=pad_dur=0.12",
                    str(normalized),
                ],
                check=True,
            )

            dur = duration_ms(normalized)
            key = hashlib.sha256(text.encode("utf-8")).hexdigest()
            normalized_files.append(normalized)
            index_rows.append([
                key, cursor_ms, dur, text, source_title, license_meta["license"]
            ])
            cursor_ms += dur

        concat = tmpdir / "concat.txt"
        concat.write_text(
            "".join(f"file '{p.as_posix().replace(chr(39), chr(39)+chr(92)+chr(39)+chr(39))}'\n"
                    for p in normalized_files),
            encoding="utf-8",
        )

        subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error",
                "-f", "concat", "-safe", "0",
                "-i", str(concat),
                "-c:a", "aac", "-b:a", "96k",
                "-ar", "24000", "-ac", "1",
                str(ASSETS / "tts_audio.m4a"),
            ],
            check=True,
        )

        with (ASSETS / "tts_index.tsv").open("w", encoding="utf-8", newline="") as f:
            writer = csv.writer(f, delimiter="\t", lineterminator="\n")
            writer.writerow(["sha256","start_ms","duration_ms","original_text","source_file","license"])
            writer.writerows(index_rows)

        print("GENERATED_TEXTS =", len(desired))
        print("INDEXED_AUDIO =", len(index_rows))
        print("AUDIO_FILE_SHA256 =", sha256_file(ASSETS / "tts_audio.m4a"))

if __name__ == "__main__":
    main()
