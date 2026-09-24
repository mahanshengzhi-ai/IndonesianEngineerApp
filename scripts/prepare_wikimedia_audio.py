#!/usr/bin/env python3
from __future__ import annotations

import csv
import hashlib
import os
import re
import struct
import subprocess
import tempfile
import time
import unicodedata
import wave
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.parse import quote, unquote

import requests

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
API_URL = "https://commons.wikimedia.org/w/api.php"
CATEGORY = "Category:Lingua Libre pronunciation-ind"
UA = "IndonesianEngineerApp/0.8 (redistributable human-recorded audio build; contactable user agent)"
MAX_AUDIO = 4000
DOWNLOAD_WORKERS = 8
CACHE_DIR = Path(os.environ.get("AUDIO_CACHE_DIR", "/tmp/indonesian-engineer-audio-cache"))

TOKEN_RE = re.compile(r"[A-Za-zÀ-ÿ]+(?:'[A-Za-zÀ-ÿ]+)?", re.UNICODE)
LICENSE_RE = re.compile(r"<[^>]+>")

ALLOWED_LICENSES = (
    "CC0",
    "CC-ZERO",
    "CC BY 2.0",
    "CC BY 3.0",
    "CC BY 4.0",
    "CC-BY 2.0",
    "CC-BY 3.0",
    "CC-BY 4.0",
    "CC BY-SA 2.0",
    "CC BY-SA 3.0",
    "CC BY-SA 4.0",
    "CC-BY-SA 2.0",
    "CC-BY-SA 3.0",
    "CC-BY-SA 4.0",
    "PUBLIC DOMAIN",
)

DENIED_LICENSE_MARKERS = (
    "NC",
    "NONCOMMERCIAL",
    "ND",
    "NODERIVATIVES",
    "NO DERIVATIVES",
)


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFC", text or "")
    text = text.replace("_", " ")
    return re.sub(r"\s+", " ", text.strip()).lower()


def clean_html(text: str) -> str:
    return re.sub(r"\s+", " ", LICENSE_RE.sub("", text or "")).strip()


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
    for attempt in range(10):
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
            if attempt == 9:
                raise
            print(f"request error: {exc}; retrying in {delay:.1f}s")
            time.sleep(delay)
            delay = min(delay * 1.8, 45.0)
    raise RuntimeError("request retry loop exhausted")



def list_category_metadata(
    session: requests.Session,
    desired_keys: set[str],
    desired_tokens: set[str],
) -> dict[str, dict[str, str]]:
    """
    Fetch category members together with image metadata in one API query family.
    We keep only recordings whose transcript is relevant to the requested text
    or to a word token needed by a stitched phrase.
    """
    output: dict[str, dict[str, str]] = {}
    cont: dict[str, str] = {}

    while True:
        params: dict[str, str | int] = {
            "action": "query",
            "format": "json",
            "generator": "categorymembers",
            "gcmtitle": CATEGORY,
            "gcmnamespace": "6",
            "gcmtype": "file",
            "gcmlimit": 500,
            "prop": "imageinfo",
            "iiprop": "url|extmetadata",
            **cont,
        }
        data = get_json(session, params)
        pages = data.get("query", {}).get("pages", {})

        for page in pages.values():
            title = page.get("title", "")
            if not title.lower().endswith(".wav"):
                continue

            candidate_keys = {
                normalize(candidate)
                for candidate in transcription_candidates(title)
                if normalize(candidate)
            }
            if not (candidate_keys & desired_keys) and not (candidate_keys & desired_tokens):
                continue

            infos = page.get("imageinfo", [])
            if not infos:
                continue

            info = infos[0]
            meta = info.get("extmetadata", {})
            license_name = clean_html(
                meta.get("LicenseShortName", {}).get("value", "")
            )
            if not license_allowed(license_name):
                continue

            url = info.get("url", "")
            if not url:
                continue

            source_url = (
                f"https://commons.wikimedia.org/wiki/{quote(title.replace(' ', '_'))}"
            )
            artist = clean_html(
                meta.get("Artist", {}).get("value", "")
                or meta.get("Credit", {}).get("value", "")
                or "Unknown"
            )
            output[title] = {
                "url": url,
                "license": license_name,
                "artist": artist,
                "source_url": source_url,
            }

        if "continue" not in data:
            break
        cont = data["continue"]
        time.sleep(0.8)

    return output


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


def build_title_index(titles: list[str]) -> dict[str, list[str]]:
    result: dict[str, list[str]] = {}
    for title in titles:
        for candidate in transcription_candidates(title):
            key = normalize(candidate)
            if not key:
                continue
            bucket = result.setdefault(key, [])
            if title not in bucket and len(bucket) < 4:
                bucket.append(title)
    return result


def license_allowed(raw_name: str) -> bool:
    clean = clean_html(raw_name).upper()
    if any(marker in clean for marker in DENIED_LICENSE_MARKERS):
        return False
    return any(marker in clean for marker in ALLOWED_LICENSES)


def tokenize_phrase(text: str) -> list[str]:
    return [normalize(token) for token in TOKEN_RE.findall(text)]


def cache_path_for_title(title: str) -> Path:
    source_dir = CACHE_DIR / "wav_sources"
    source_dir.mkdir(parents=True, exist_ok=True)
    return source_dir / (hashlib.sha1(title.encode("utf-8")).hexdigest() + ".wav")


def duration_ms(path: Path) -> int:
    with wave.open(str(path), "rb") as wav:
        frames = wav.getnframes()
        rate = wav.getframerate()
    return max(1, int(frames * 1000 / max(1, rate)))


def download_and_normalize(
    session_factory,
    title: str,
    meta: dict[str, str],
    out_dir: Path,
) -> tuple[str, Path, int]:
    cached = cache_path_for_title(title)
    if cached.exists() and cached.stat().st_size > 1000:
        return title, cached, duration_ms(cached)

    session = session_factory()
    raw = out_dir / (
        hashlib.sha1((title + ".source").encode("utf-8")).hexdigest() + ".bin"
    )
    normalized = cached
    delay = 2.0

    for attempt in range(10):
        try:
            with session.get(
                meta["url"],
                headers={"User-Agent": UA},
                timeout=120,
                stream=True,
            ) as response:
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
                    "-af", "volume=0.88",
                    str(normalized),
                ],
                check=True,
            )
            raw.unlink(missing_ok=True)
            return title, normalized, duration_ms(normalized)
        except (requests.RequestException, subprocess.CalledProcessError) as exc:
            raw.unlink(missing_ok=True)
            if attempt == 9:
                raise RuntimeError(f"audio failed for {title}: {exc}")
            time.sleep(delay)
            delay = min(delay * 1.8, 30.0)

    raise RuntimeError(f"download retry exhausted for {title}")


def resolve_audio_plans(
    desired: list[str],
    title_index: dict[str, list[str]],
    metadata: dict[str, dict[str, str]],
) -> dict[str, tuple[str, list[str]]]:
    resolved: dict[str, tuple[str, list[str]]] = {}

    for text in desired:
        key = normalize(text)
        exact_usable = [
            title for title in title_index.get(key, [])
            if title in metadata
        ]
        if exact_usable:
            resolved[key] = ("exact", [exact_usable[0]])
            continue

        tokens = tokenize_phrase(text)
        if not tokens:
            continue

        chosen: list[str] = []
        for token in tokens:
            usable = next(
                (
                    title for title in title_index.get(token, [])
                    if title in metadata
                ),
                None,
            )
            if usable is None:
                break
            chosen.append(usable)
        else:
            if chosen:
                resolved[key] = ("stitched", chosen)

    return resolved


def write_silence(path: Path, milliseconds: int) -> int:
    frames = 24000 * milliseconds // 1000
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(24000)
        zero_block = struct.pack("<h", 0) * 4096
        remaining = frames
        while remaining:
            count = min(remaining, 4096)
            wav.writeframes(zero_block[:count * 2])
            remaining -= count
    return frames * 1000 // 24000


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    audio_output = ASSETS / "tts_audio.m4a"
    index_output = ASSETS / "tts_index.tsv"
    attribution_output = ASSETS / "AUDIO_ATTRIBUTION.tsv"

    audio_output.unlink(missing_ok=True)
    index_output.unlink(missing_ok=True)
    attribution_output.unlink(missing_ok=True)

    desired = desired_texts()
    print("DESIRED_AUDIO_TEXTS =", len(desired))

    session = requests.Session()
    session.headers["User-Agent"] = UA

    desired_keys = {normalize(text) for text in desired}
    desired_tokens = {
        token
        for text in desired
        for token in tokenize_phrase(text)
        if token
    }

    metadata = list_category_metadata(session, desired_keys, desired_tokens)
    print("REDISTRIBUTABLE_RELEVANT_FILES =", len(metadata))

    titles = sorted(metadata)
    title_index = build_title_index(titles)
    print("RELEVANT_CATEGORY_FILES =", len(titles))

    resolved = resolve_audio_plans(desired, title_index, metadata)
    print("RESOLVED_AUDIO_TEXTS =", len(resolved))
    print("EXACT_RESOLVED =", sum(1 for kind, _ in resolved.values() if kind == "exact"))
    print("STITCHED_RESOLVED =", sum(1 for kind, _ in resolved.values() if kind == "stitched"))

    index_rows: list[list[object]] = []
    attribution: dict[str, dict[str, str]] = {}

    if resolved:
        with tempfile.TemporaryDirectory(prefix="indonesian-audio-") as temp_dir:
            temp_root = Path(temp_dir)
            silence_word = temp_root / "silence_80ms.wav"
            silence_phrase = temp_root / "silence_120ms.wav"
            word_gap_ms = write_silence(silence_word, 80)
            phrase_gap_ms = write_silence(silence_phrase, 120)

            def session_factory():
                s = requests.Session()
                s.headers["User-Agent"] = UA
                return s

            source_titles_needed = sorted({
                title
                for _, source_titles in resolved.values()
                for title in source_titles
            })

            future_map = {}
            with ThreadPoolExecutor(max_workers=DOWNLOAD_WORKERS) as executor:
                for title in source_titles_needed:
                    future = executor.submit(
                        download_and_normalize,
                        session_factory,
                        title,
                        metadata[title],
                        temp_root,
                    )
                    future_map[future] = title

                normalized_sources: dict[str, Path] = {}
                source_durations: dict[str, int] = {}
                for future in as_completed(future_map):
                    title, path, duration = future.result()
                    normalized_sources[title] = path
                    source_durations[title] = duration

            concat_file = temp_root / "final.concat.txt"
            cursor_ms = 0
            concat_lines: list[str] = []

            for text in desired[:MAX_AUDIO]:
                key = normalize(text)
                if key not in resolved:
                    continue

                kind, source_titles = resolved[key]
                phrase_start = cursor_ms

                if kind == "exact":
                    title = source_titles[0]
                    concat_lines.append(
                        f"file '{str(normalized_sources[title]).replace(chr(39), chr(39)+chr(92)+chr(39)+chr(39))}'\n"
                    )
                    cursor_ms += source_durations[title]
                    licenses = [metadata[title]["license"]]
                else:
                    licenses = []
                    for idx, title in enumerate(source_titles):
                        concat_lines.append(
                            f"file '{str(normalized_sources[title]).replace(chr(39), chr(39)+chr(92)+chr(39)+chr(39))}'\n"
                        )
                        cursor_ms += source_durations[title]
                        licenses.append(metadata[title]["license"])
                        if idx < len(source_titles) - 1:
                            concat_lines.append(f"file '{silence_word}'\n")
                            cursor_ms += word_gap_ms

                concat_lines.append(f"file '{silence_phrase}'\n")
                cursor_ms += phrase_gap_ms

                source_label = (
                    source_titles[0]
                    if kind == "exact"
                    else "STITCHED:" + "|".join(source_titles)
                )
                index_rows.append([
                    hashlib.sha256(text.encode("utf-8")).hexdigest(),
                    phrase_start,
                    cursor_ms - phrase_start,
                    text,
                    source_label,
                    "; ".join(dict.fromkeys(licenses)),
                ])

                for title in source_titles:
                    attribution[title] = metadata[title]

            concat_file.write_text("".join(concat_lines), encoding="utf-8")

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
        writer.writerows(index_rows)

    with attribution_output.open("w", encoding="utf-8", newline="") as f:
        writer = csv.writer(f, delimiter="\t", lineterminator="\n")
        writer.writerow(["source_file", "artist", "license", "source_url"])
        for title in sorted(attribution):
            meta = attribution[title]
            writer.writerow([
                title,
                meta.get("artist", "Unknown"),
                meta.get("license", ""),
                meta.get("source_url", ""),
            ])

    print("INDEXED_AUDIO =", len(index_rows))
    print("MISSING_AUDIO =", len(desired) - len(index_rows))
    print("AUDIO_ASSET_PRESENT =", audio_output.exists())
    print("AUDIO_ASSET_BYTES =", audio_output.stat().st_size if audio_output.exists() else 0)
    print("ATTRIBUTION_ENTRIES =", len(attribution))


if __name__ == "__main__":
    main()
