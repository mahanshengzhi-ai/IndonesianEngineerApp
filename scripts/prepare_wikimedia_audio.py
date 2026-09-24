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
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path
from urllib.parse import quote, unquote

import requests

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
API_URL = "https://commons.wikimedia.org/w/api.php"
CATEGORY = "Category:Lingua Libre pronunciation-ind"
UA = "IndonesianEngineerApp/0.6 (redistributable human-recorded audio build)"
MAX_AUDIO = 3730
DOWNLOAD_WORKERS = 6
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
    return re.sub(r"s+", " ", text.strip()).lower()


def clean_html(text: str) -> str:
    return re.sub(r"s+", " ", LICENSE_RE.sub("", text or "")).strip()


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="	"))


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


def list_category_files(session: requests.Session) -> list[str]:
    titles: list[str] = []
    cont: dict[str, str] = {}
    while True:
        data = get_json(
            session,
            {
                "action": "query",
                "format": "json",
                "list": "categorymembers",
                "cmtitle": CATEGORY,
                "cmnamespace": "6",
                "cmtype": "file",
                "cmlimit": 500,
                **cont,
            },
        )
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
        for page in data.get("query", {}).get("pages", {}).values():
            title = page.get("title", "")
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

            source_url = (
                meta.get("LicenseUrl", {}).get("value")
                or f"https://commons.wikimedia.org/wiki/{quote(title.replace(' ', '_'))}"
            )
            artist = clean_html(
                meta.get("Artist", {}).get("value", "")
                or meta.get("Credit", {}).get("value", "")
                or "Unknown"
            )
            url = info.get("url", "")
            if url:
                output[title] = {
                    "url": url,
                    "license": license_name,
                    "artist": artist,
                    "source_url": source_url,
                }
        time.sleep(0.4)
    return output


def tokenize_phrase(text: str) -> list[str]:
    return [normalize(token) for token in TOKEN_RE.findall(text)]


def cache_path_for_title(title: str) -> Path:
    source_dir = CACHE_DIR / "wav_sources"
    source_dir.mkdir(parents=True, exist_ok=True)
    return source_dir / (hashlib.sha1(title.encode("utf-8")).hexdigest() + ".wav")


def download_and_normalize(
    session_factory,
    title: str,
    meta: dict[str, str],
    out_dir: Path,
) -> tuple[str, Path]:
    cached = cache_path_for_title(title)
    if cached.exists() and cached.stat().st_size > 1000:
        return title, cached

    session = session_factory()
    raw = out_dir / (hashlib.sha1((title + ".source").encode("utf-8")).hexdigest() + ".bin")
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
            return title, normalized
        except (requests.RequestException, subprocess.CalledProcessError) as exc:
            raw.unlink(missing_ok=True)
            if attempt == 9:
                raise RuntimeError(f"audio failed for {title}: {exc}")
            time.sleep(delay)
            delay = min(delay * 1.8, 30.0)

    raise RuntimeError(f"download retry exhausted for {title}")


def duration_ms(path: Path) -> int:
    result = subprocess.run(
        [
            "ffprobe", "-v", "error",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            str(path),
        ],
        capture_output=True,
        text=True,
        check=True,
    )
    return max(1, int(float(result.stdout.strip()) * 1000))


def sha256(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def resolve_audio_plans(
    desired: list[str],
    title_index: dict[str, list[str]],
    metadata: dict[str, dict[str, str]],
) -> dict[str, tuple[str, list[str]]]:
    resolved: dict[str, tuple[str, list[str]]] = {}

    for text in desired:
        key = normalize(text)
        exact_candidates = title_index.get(key, [])

        # Prefer one natural human recording of the full phrase.
        exact_usable = [title for title in exact_candidates if title in metadata]
        if exact_usable:
            resolved[key] = ("exact", [exact_usable[0]])
            continue

        # Otherwise stitch real human-recorded word clips. No TTS or synthesis.
        tokens = tokenize_phrase(text)
        if not tokens:
            continue

        chosen: list[str] = []
        possible = True
        for token in tokens:
            candidates = title_index.get(token, [])
            usable = next((title for title in candidates if title in metadata), None)
            if usable is None:
                possible = False
                break
            chosen.append(usable)

        if possible and chosen:
            resolved[key] = ("stitched", chosen)

    return resolved


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

    titles = list_category_files(session)
    print("CATEGORY_FILES =", len(titles))

    title_index = build_title_index(titles)
    candidate_titles: set[str] = set()

    for text in desired:
        key = normalize(text)
        candidate_titles.update(title_index.get(key, []))
        for token in tokenize_phrase(text):
            candidate_titles.update(title_index.get(token, []))

    print("UNIQUE_CANDIDATE_FILES =", len(candidate_titles))
    metadata = metadata_for_titles(session, sorted(candidate_titles))
    print("REDISTRIBUTABLE_CANDIDATE_FILES =", len(metadata))

    resolved = resolve_audio_plans(desired, title_index, metadata)
    print("RESOLVED_AUDIO_TEXTS =", len(resolved))
    print("EXACT_RESOLVED =", sum(1 for kind, _ in resolved.values() if kind == "exact"))
    print("STITCHED_RESOLVED =", sum(1 for kind, _ in resolved.values() if kind == "stitched"))

    if not resolved:
        index_output.write_text(
            "sha256\tstart_ms\tduration_ms\toriginal_text\tsource_file\tlicense\n",
            encoding="utf-8",
        )
        attribution_output.write_text(
            "source_file\tartist\tlicense\tsource_url\n",
            encoding="utf-8",
        )
        print("INDEXED_AUDIO = 0")
        print("MISSING_AUDIO =", len(desired))
        print("AUDIO_ASSET_PRESENT = False")
        return

    with tempfile.TemporaryDirectory(prefix="indonesian-audio-") as temp_dir:
        temp_root = Path(temp_dir)

        def session_factory():
            s = requests.Session()
            s.headers["User-Agent"] = UA
            return s

        source_titles_needed = sorted({
            title
            for kind, source_titles in resolved.values()
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
            for future in as_completed(future_map):
                title, path = future.result()
                normalized_sources[title] = path

        ordered_files: list[Path] = []
        rows = []
        attribution: dict[str, dict[str, str]] = {}
        cursor_ms = 0

        for text in desired[:MAX_AUDIO]:
            key = normalize(text)
            if key not in resolved:
                continue

            kind, source_titles = resolved[key]
            with tempfile.NamedTemporaryFile(
                prefix="phrase-", suffix=".wav", dir=temp_root, delete=False
            ) as phrase_file:
                phrase_path = Path(phrase_file.name)

            if kind == "exact":
                source_path = normalized_sources[source_titles[0]]
                subprocess.run(
                    [
                        "ffmpeg", "-y", "-loglevel", "error",
                        "-i", str(source_path),
                        "-af", "apad=pad_dur=0.12",
                        "-ac", "1", "-ar", "24000",
                        str(phrase_path),
                    ],
                    check=True,
                )
            else:
                inputs = []
                filter_parts = []
                for idx, title in enumerate(source_titles):
                    path = normalized_sources[title]
                    inputs.extend(["-i", str(path)])
                    filter_parts.append(f"[{idx}:a]apad=pad_dur=0.08[a{idx}]")
                graph = ";".join(filter_parts)
                mix_inputs = "".join(f"[a{i}]" for i in range(len(source_titles)))
                graph += f";{mix_inputs}concat=n={len(source_titles)}:v=0:a=1[out]"
                subprocess.run(
                    [
                        "ffmpeg", "-y", "-loglevel", "error",
                        *inputs,
                        "-filter_complex", graph,
                        "-map", "[out]",
                        "-ac", "1", "-ar", "24000",
                        "-af", "volume=0.88,apad=pad_dur=0.12",
                        str(phrase_path),
                    ],
                    check=True,
                )

            duration = duration_ms(phrase_path)
            ordered_files.append(phrase_path)

            if kind == "exact":
                source_label = source_titles[0]
            else:
                source_label = "STITCHED:" + "|".join(source_titles)

            licenses = []
            for title in source_titles:
                meta = metadata[title]
                licenses.append(meta["license"])
                attribution[title] = meta

            rows.append([
                sha256(text),
                cursor_ms,
                duration,
                text,
                source_label,
                "; ".join(dict.fromkeys(licenses)),
            ])
            cursor_ms += duration

        final_concat = temp_root / "final.concat.txt"
        final_concat.write_text(
            "".join(
                f"file '{str(path).replace(chr(39), chr(39)+chr(92)+chr(39)+chr(39))}'\n"
                for path in ordered_files
            ),
            encoding="utf-8",
        )

        subprocess.run(
            [
                "ffmpeg", "-y", "-loglevel", "error",
                "-f", "concat", "-safe", "0",
                "-i", str(final_concat),
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

    print("INDEXED_AUDIO =", len(rows))
    print("MISSING_AUDIO =", len(desired) - len(rows))
    print("AUDIO_ASSET_PRESENT =", audio_output.exists())
    print("AUDIO_ASSET_BYTES =", audio_output.stat().st_size if audio_output.exists() else 0)
    print("ATTRIBUTION_ENTRIES =", len(attribution))


if __name__ == "__main__":
    main()
