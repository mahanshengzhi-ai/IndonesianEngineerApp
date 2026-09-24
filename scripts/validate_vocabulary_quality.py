#!/usr/bin/env python3
from __future__ import annotations

import csv
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
CJK = re.compile(r"[\u3400-\u9fff]")

BAD_TEMPLATES = (
    "pekerjaan pekerjaan",
    "gambar gambar",
    "pengukuran ukuran",
    "pengukuran catatan",
    "pengukuran rencana",
    "pengukuran spesifikasi",
    "pengukuran kualitas",
    "lokasi lokasi",
)


def read_rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f, delimiter="\t"))


def main() -> None:
    files = [ASSETS / f"words_{i:02d}.tsv" for i in range(1, 12)]
    missing = [str(p) for p in files if not p.exists()]
    if missing:
        raise SystemExit(f"MISSING_WORD_FILES={missing}")

    rows: list[dict[str, str]] = []
    for path in files:
        parsed = read_rows(path)
        for row in parsed:
            row["_file"] = str(path.relative_to(ROOT))
        rows.extend(parsed)

    required = ("id", "indonesian", "chinese", "category", "scene")
    missing_required = [
        row["id"]
        for row in rows
        if any(not row.get(field, "").strip() for field in required)
    ]

    ids = [row["id"] for row in rows]
    ind = [row["indonesian"].strip() for row in rows]
    zh = [row["chinese"].strip() for row in rows]

    duplicate_ids = len(ids) - len(set(ids))
    duplicate_indonesian = len(ind) - len(set(ind))
    duplicate_chinese = len(zh) - len(set(zh))

    mixed_language = [
        row["id"] for row in rows
        if CJK.search(row["indonesian"])
    ]

    bad_result = [
        row["id"] for row in rows
        if row["chinese"].endswith("结果")
        and not row["chinese"].startswith(
            ("检查", "检验", "检测", "试验", "测试", "测量", "验收", "审核", "审查", "翻译")
        )
    ]

    bad_position = [
        row["id"] for row in rows
        if row["chinese"].endswith("位置")
        and row["indonesian"].lower().startswith("posisi ")
    ]

    bad_templates = [
        row["id"] for row in rows
        if any(token in row["indonesian"].lower() for token in BAD_TEMPLATES)
    ]

    schedule_mismatch = [
        row["id"] for row in rows
        if row["chinese"].endswith("计划")
        and row["indonesian"].lower().startswith("jadwal ")
    ]

    old_waterpass = [
        row["id"] for row in rows
        if "alat ukur nivo" in row["indonesian"].lower()
    ]

    suspicious_empty = [
        row["id"] for row in rows
        if not row["indonesian"].strip() or not row["chinese"].strip()
    ]


    def grouped(values):
        groups = {}
        for index, value in enumerate(values):
            groups.setdefault(value, []).append(rows[index]["id"])
        return {k: v for k, v in groups.items() if len(v) > 1}

    duplicate_ind_groups = grouped(ind)
    duplicate_zh_groups = grouped(zh)
    print(f"VOCABULARY_COUNT = {len(rows)}")
    print(f"UNIQUE_CHINESE_MEANINGS = {len(set(zh))}")
    print(f"DUPLICATE_IDS = {duplicate_ids}")
    print(f"DUPLICATE_INDONESIAN = {duplicate_indonesian}")
    print(f"DUPLICATE_CHINESE = {duplicate_chinese}")
    print(f"CHINESE_IN_INDONESIAN = {len(mixed_language)}")
    print(f"BAD_RESULT_PHRASES = {len(bad_result)}")
    print(f"POSITION_NOT_CANONICAL = {len(bad_position)}")
    print(f"BAD_TEMPLATE_PHRASES = {len(bad_templates)}")
    print(f"PLAN_TRANSLATION_JADWAL = {len(schedule_mismatch)}")
    print(f"OLD_WATERPASS_TERM = {len(old_waterpass)}")
    print(f"MISSING_REQUIRED_FIELDS = {len(missing_required)}")
    print(f"EMPTY_LEARNING_FIELDS = {len(suspicious_empty)}")
    if duplicate_ind_groups:
        print("DUPLICATE_INDONESIAN_DETAILS =")
        for key, ids in list(duplicate_ind_groups.items())[:20]:
            print(f"  {key} -> {ids}")
    if duplicate_zh_groups:
        print("DUPLICATE_CHINESE_DETAILS =")
        for key, ids in list(duplicate_zh_groups.items())[:20]:
            print(f"  {key} -> {ids}")
    if bad_templates:
        print("BAD_TEMPLATE_DETAILS =")
        for row in rows:
            if row["id"] in set(bad_templates):
                print(f"  {row['id']} -> {row['indonesian']} / {row['chinese']}")

    problems = []
    if len(rows) < 3020:
        problems.append(f"VOCABULARY_COUNT < 3020 ({len(rows)})")
    if duplicate_ids:
        problems.append("DUPLICATE_IDS > 0")
    if duplicate_indonesian:
        problems.append("DUPLICATE_INDONESIAN > 0")
    if duplicate_chinese:
        problems.append("DUPLICATE_CHINESE > 0")
    if mixed_language:
        problems.append("CHINESE_IN_INDONESIAN > 0")
    if bad_result:
        problems.append("BAD_RESULT_PHRASES > 0")
    if bad_position:
        problems.append("POSITION_NOT_CANONICAL > 0")
    if bad_templates:
        problems.append("BAD_TEMPLATE_PHRASES > 0")
    if schedule_mismatch:
        problems.append("PLAN_TRANSLATION_JADWAL > 0")
    if old_waterpass:
        problems.append("OLD_WATERPASS_TERM > 0")
    if missing_required:
        problems.append("MISSING_REQUIRED_FIELDS > 0")
    if suspicious_empty:
        problems.append("EMPTY_LEARNING_FIELDS > 0")

    if problems:
        print("QUALITY_PROBLEMS:")
        for problem in problems:
            print("-", problem)
        raise SystemExit(1)

    print("VOCABULARY_QUALITY = PASS")


if __name__ == "__main__":
    main()
