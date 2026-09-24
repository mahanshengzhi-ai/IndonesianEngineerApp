#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res"
KOTLIN = ROOT / "app" / "src" / "main" / "java"

TEXT_VIEW_FIXED_HEIGHT = re.compile(
    r"<TextView\\b[^>]*android:layout_height=\"[0-9]+dp\"",
    re.IGNORECASE | re.DOTALL,
)
HORIZONTAL_ROW_TEXT = re.compile(
    r"<LinearLayout\\b[^>]*android:orientation=\"horizontal\"[^>]*>.*?<TextView",
    re.IGNORECASE | re.DOTALL,
)
EMOJI = re.compile(
    r"[\\U0001F300-\\U0001FAFF\\u2600-\\u27BF]",
)

problems: list[str] = []

for path in RES.rglob("*.xml"):
    text = path.read_text("utf-8", errors="ignore")

    if TEXT_VIEW_FIXED_HEIGHT.search(text):
        problems.append(f"FIXED_TEXTVIEW_HEIGHT: {path.relative_to(ROOT)}")

    if EMOJI.search(text):
        problems.append(f"EMOJI_IN_XML: {path.relative_to(ROOT)}")

menu = RES / "menu" / "bottom_nav.xml"
if menu.exists():
    menu_text = menu.read_text("utf-8", errors="ignore")
    if menu_text.count("<item ") != 5:
        problems.append("BOTTOM_NAV_ITEM_COUNT != 5")
    if EMOJI.search(menu_text):
        problems.append("EMOJI_IN_BOTTOM_NAV")
    for required in ("@drawable/ic_home", "@drawable/ic_book", "@drawable/ic_check",
                     "@drawable/ic_translate", "@drawable/ic_person"):
        if required not in menu_text:
            problems.append(f"MISSING_VECTOR_NAV_ICON: {required}")

activity_layout = RES / "layout" / "activity_main.xml"
if activity_layout.exists():
    text = activity_layout.read_text("utf-8", errors="ignore")
    player = text.find('android:id="@+id/playerContainer"')
    bottom = text.find('android:id="@+id/bottomNavigation"')
    if player == -1 or bottom == -1:
        problems.append("PLAYER_OR_BOTTOM_NAV_MISSING")
    elif player > bottom:
        problems.append("PLAYER_MUST_BE_ABOVE_BOTTOM_NAV")

for path in KOTLIN.rglob("*.kt"):
    text = path.read_text("utf-8", errors="ignore")
    if "android.widget.TextView" in text and "setHeight(" in text:
        problems.append(f"TEXTVIEW_SETHEIGHT_CALL: {path.relative_to(ROOT)}")

main_activity = KOTLIN / "com" / "mahanshengzhi" / "indonesianengineer" / "MainActivity.kt"
if main_activity.exists():
    line_count = len(main_activity.read_text("utf-8", errors="ignore").splitlines())
    if line_count > 500:
        problems.append(f"MAIN_ACTIVITY_TOO_LARGE: {line_count}")

if problems:
    print("UI STATIC PROBLEMS:")
    for problem in problems:
        print("-", problem)
    raise SystemExit(1)

print("UI_STATIC_CHECK = PASS")
print("BOTTOM_NAV = 5")
print("FIXED_TEXTVIEW_HEIGHT = 0")
print("EMOJI_NAV = 0")
