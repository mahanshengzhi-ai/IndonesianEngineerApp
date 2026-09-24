#!/usr/bin/env python3
"""Validate the generated bundled Indonesian audio pack."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import shutil
import subprocess
from pathlib import Path

MIN_LINES = 3000

def sha256(text: str) -> str:
    return hashlib.sha256(text.encode('utf-8')).hexdigest()

def audio_duration_ms(path: Path) -> int:
    value = subprocess.check_output([
        'ffprobe', '-v', 'error',
        '-show_entries', 'format=duration',
        '-of', 'default=noprint_wrappers=1:nokey=1',
        str(path),
    ], text=True).strip()
    return max(0, round(float(value) * 1000))

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--min-lines', type=int, default=MIN_LINES)
    args = parser.parse_args()

    root = args.root.resolve()
    audio = root / 'app' / 'src' / 'main' / 'assets' / 'tts_audio.m4a'
    index = root / 'app' / 'src' / 'main' / 'assets' / 'tts_index.tsv'

    errors = []
    warnings = []
    if not audio.exists():
        errors.append('AUDIO_FILE_MISSING')
    if not index.exists():
        errors.append('AUDIO_INDEX_MISSING')
    if errors:
        print('\n'.join(errors))
        return 1
    if shutil.which('ffprobe') is None:
        errors.append('FFPROBE_MISSING')
        return 1

    duration_ms = audio_duration_ms(audio)
    rows = []
    with index.open('r', encoding='utf-8-sig', newline='') as fh:
        reader = csv.DictReader(fh, delimiter='\t')
        required = {
            'sha256', 'start_ms', 'duration_ms', 'original_text',
            'source_file', 'source_url', 'license'
        }
        if not required.issubset(set(reader.fieldnames or [])):
            errors.append('INDEX_HEADER_INVALID')
        else:
            rows.extend(reader)

    if len(rows) < args.min_lines:
        errors.append('AUDIO_LINES_TOO_LOW:' + str(len(rows)) + '<' + str(args.min_lines))

    seen_sha = set()
    seen_text = set()
    previous_end = 0
    for row_number, row in enumerate(rows, start=2):
        sha = (row.get('sha256') or '').strip().lower()
        text = row.get('original_text') or ''
        try:
            start = int(row.get('start_ms') or 0)
            duration = int(row.get('duration_ms') or 0)
        except ValueError:
            errors.append('INVALID_NUMERIC_SEGMENT:' + str(row_number))
            continue
        license_name = (row.get('license') or '').strip()
        if len(sha) != 64:
            errors.append('INVALID_SHA:' + str(row_number))
            continue
        if sha256(text) != sha:
            errors.append('SHA_TEXT_MISMATCH:' + str(row_number))
        if sha in seen_sha:
            errors.append('DUPLICATE_SHA:' + str(row_number))
        if text in seen_text:
            errors.append('DUPLICATE_TEXT:' + str(row_number))
        if not text.strip():
            errors.append('EMPTY_TEXT:' + str(row_number))
        if start < 0 or duration <= 0:
            errors.append('INVALID_SEGMENT:' + str(row_number))
        if start < previous_end:
            errors.append('OVERLAPPING_SEGMENT:' + str(row_number))
        if start + duration > duration_ms + 1000:
            errors.append('SEGMENT_OUTSIDE_AUDIO:' + str(row_number))
        if license_name != 'CC0-1.0':
            errors.append('NON_CC0_SOURCE:' + str(row_number))
        seen_sha.add(sha)
        seen_text.add(text)
        previous_end = max(previous_end, start + duration)

    report = {
        'status': 'PASS' if not errors else 'FAIL',
        'audio_lines': len(rows),
        'minimum_audio_lines': args.min_lines,
        'unique_hashes': len(seen_sha),
        'unique_texts': len(seen_text),
        'audio_duration_ms': duration_ms,
        'errors': errors,
        'warnings': warnings,
    }
    report_path = root / 'build' / 'audio' / 'audio_validation_report.json'
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if not errors else 1

if __name__ == '__main__':
    raise SystemExit(main())
