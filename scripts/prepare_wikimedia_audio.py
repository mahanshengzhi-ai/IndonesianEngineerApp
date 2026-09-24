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
from urllib.parse import unquote

import requests

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
API_URL = "https://commons.wikimedia.org/w/api.php"
CATEGORY = "Category:Lingua Libre pronunciation-ind"
UA = "IndonesianEngineerApp/0.5 (CC0 human-recording audio build)"
MAX_AUDIO = 3730
DOWNLOAD_WORKERS = 6
CACHE_DIR = Path(os.environ.get("AUDIO_CACHE_DIR", "/tmp/indonesian-engineer-audio-cache"))

TOKEN_RE = re.compile(r"[A-Za-zÀ-ÿ]+(?:'[A-Za-zÀ-ÿ]+)?", re.UNICODE)


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
            license_name = meta.get("LicenseShortName", {}).get("value", "")
            clean = re.sub(r"<[^>]+>", "", license_name).upper()
            if "CC0" not in clean and "CC-ZERO" not in clean:
                continue
            url = info.get("url", "")
            if url:
                output[title] = {"url": url, "license": "CC0"}
        time.sleep(0.4)
    return output


def tokenize_phrase(text: str) -> list[str]:
    return [normalize(token) for token in TOKEN_RE.findall(text)]


def cache_path_for_title(title: str) -> Path:
    CACHE_DIR.mkdir(parents=True, exist_ok=True)
    return CACHE_DIR / (hashlib.sha1(title.encode("utf-8")).hexdigest() + ".wav")


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


def main() -> None:
    ASSETS.mkdir(parents=True, exist_ok=True)
    audio_output = ASSETS / "tts_audio.m4a"
    index_output = ASSETS / "tts_index.tsv"
    audio_output.unlink(missing_ok=True)
    index_output.unlink(missing_ok=True)

    desired = desired_texts()
    print("DESIRED_AUDIO_TEXTS =", len(desired))

    session = requests.Session()
    session.headers["User-Agent"] = UA

    titles = list_category_files(session)
    print("CATEGORY_FILES =", len(titles))

    title_index = build_title_index(titles)

    # Collect candidate CC0 source files needed for exact phrases and word stitching.
    candidate_titles: set[str] = set()
    plans: dict[str, tuple[str, list[str]] | None] = {}

    for text in desired:
        key = normalize(text)
        exact_candidates = title_index.get(key, [])
        if exact_candidates:
            plans[key] = ("exact", exact_candidates)
            candidate_titles.update(exact_candidates)
            continue

        tokens = tokenize_phrase(text)
        if not tokens:
            plans[key] = None
            continue

        sources: list[str] = []
        possible = True
        for token in tokens:
            candidates = title_index.get(token, [])
            if not candidates:
                possible = False
                break
            # Retain up to four candidates per token, metadata filtering happens below.
            sources.append("|".join(candidates))
            candidate_titles.update(candidates)

        plans[key] = ("stitched", sources) if possible else None

    print("PLANNED_TEXTS =", sum(1 for plan in plans.values() if plan))
    print("UNIQUE_CANDIDATE_FILES =", len(candidate_titles))

    metadata = metadata_for_titles(session, sorted(candidate_titles))
    print("CC0_CANDIDATE_FILES =", len(metadata))

    # Resolve each text to an exact CC0 clip or a full sequence of CC0 word clips.
    resolved: dict[str, tuple[str, list[str]]] = {}
    source_titles_needed: set[str] = set()

    for text in desired:
        key = normalize(text)
        plan = plans.get(key)
        if not plan:
            continue

        kind, payload = plan
        if kind == "exact":
            usable = [title for title in payload if title in metadata]
            if usable:
                resolved[key] = ("exact", [usable[0]])
                source_titles_needed.add(usable[0])
            continue

        chosen: list[str] = []
        for candidate_group in payload:
            candidates = candidate_group.split("|")
            usable = next((title for title in candidates if title in metadata), None)
            if usable is None:
                chosen = []
                break
            chosen.append(usable)
            source_titles_needed.add(usable)

        if chosen:
            resolved[key] = ("stitched", chosen)

    print("RESOLVED_AUDIO_TEXTS =", len(resolved))
    print("EXACT_RESOLVED =", sum(1 for kind, _ in resolved.values() if kind == "exact"))
    print("STITCHED_RESOLVED =", sum(1 for kind, _ in resolved.values() if kind == "stitched"))

    with tempfile.TemporaryDirectory(prefix="indonesian-audio-") as temp_dir:
        temp_root = Path(temp_dir)

        def session_factory():
            s = requests.Session()
            s.headers["User-Agent"] = UA
            return s

        # Download each required CC0 source file only once.
        future_map = {}
        with ThreadPoolExecutor(max_workers=DOWNLOAD_WORKERS) as executor:
            for title in sorted(source_titles_needed):
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

            concat_file = phrase_path.with_suffix(".concat.txt")
            concat_entries = []
            for index, title in enumerate(source_titles):
                source_path = normalized_sources[title]
                concat_entries.append(
                    f"file '{str(source_path).replace(chr(39), chr(39)+chr(92)+chr(39)+chr(39))}'\n"
                )
                if index < len(source_titles) - 1:
                    # A short, fixed silence is introduced between human word clips.
                    pass

            concat_file.write_text("".join(concat_entries), encoding="utf-8")

            if kind == "exact":
                subprocess.run(
                    ["cp", str(normalized_sources[source_titles[0]]), str(phrase_path)],
                    check=True,
                )
            else:
                # Concatenate the real human-word recordings, inserting 80 ms silence
                # between words. No synthesis or TTS is used.
                filter_parts = []
                inputs = []
                for idx, title in enumerate(source_titles):
                    path = normalized_sources[title]
                    inputs.extend(["-i", str(path)])
                    filter_parts.append(
                        f"[{idx}:a]apad=pad_dur=0.08,atrim=duration={duration_ms(path)/1000.0:.3f}[a{idx}]"
                    )
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
            source_label = source_titles[0] if kind == "exact" else "STITCHED:" + "|".join(source_titles)
            license_label = (
                "CC0 / Wikimedia Commons Lingua Libre"
                if kind == "exact"
                else "CC0 / Wikimedia Commons Lingua Libre (human-word clips stitched)"
            )
            rows.append([
                sha256(text),
                cursor_ms,
                duration,
                text,
                source_label,
                license_label,
            ])
            cursor_ms += duration
            concat_file.unlink(missing_ok=True)

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

    print("INDEXED_AUDIO =", len(rows))
    print("MISSING_AUDIO =", len(desired) - len(rows))
    print("AUDIO_ASSET_PRESENT =", audio_output.exists())
    print("AUDIO_ASSET_BYTES =", audio_output.stat().st_size if audio_output.exists() else 0)


if __name__ == "__main__":
    main()
