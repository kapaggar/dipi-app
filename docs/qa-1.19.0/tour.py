#!/usr/bin/env python3
"""ADB UI dump + tap-by-text helpers for the Pixel C desk tour."""
from __future__ import annotations

import re
import subprocess
import time
from pathlib import Path

SERIAL = "10.0.0.144:5555"
OUT = Path("/Users/wizops/DIPI/dipi-app/docs/qa-1.19.0")
DUMP = Path("/sdcard/uidump.xml")


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=check,
        capture_output=True,
    )


def shot(name: str) -> Path:
    dest = OUT / f"{name}.png"
    remote = f"/sdcard/{name}.png"
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, str(dest))
    adb("shell", "rm", remote, check=False)
    print(f"  shot {dest.name} ({dest.stat().st_size//1024}k)")
    return dest


def dump() -> str:
    adb("shell", "uiautomator", "dump", str(DUMP))
    adb("pull", str(DUMP), str(OUT / "uidump.xml"))
    return (OUT / "uidump.xml").read_text(errors="replace")


def nodes(xml: str | None = None) -> list[dict]:
    xml = xml or dump()
    out = []
    for m in re.finditer(r"<node [^>]+>", xml):
        tag = m.group(0)
        text = re.search(r'text="([^"]*)"', tag)
        desc = re.search(r'content-desc="([^"]*)"', tag)
        click = re.search(r'clickable="(true|false)"', tag)
        bounds = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', tag)
        t = text.group(1) if text else ""
        d = desc.group(1) if desc else ""
        if not bounds:
            continue
        x1, y1, x2, y2 = map(int, bounds.groups())
        out.append(
            {
                "text": t,
                "desc": d,
                "click": click.group(1) == "true" if click else False,
                "x1": x1,
                "y1": y1,
                "x2": x2,
                "y2": y2,
                "cx": (x1 + x2) // 2,
                "cy": (y1 + y2) // 2,
            }
        )
    return out


def find(label: str, xml: str | None = None, contains: bool = False):
    for n in nodes(xml):
        hay = f"{n['text']} {n['desc']}"
        if contains and label in hay:
            return n
        if n["text"] == label or n["desc"] == label:
            return n
    return None


def tap_xy(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.8)


def tap(label: str, contains: bool = False) -> bool:
    n = find(label, contains=contains)
    if not n:
        print(f"  MISS {label!r}")
        return False
    print(f"  tap {label!r} @ {n['cx']},{n['cy']}")
    tap_xy(n["cx"], n["cy"])
    return True


def swipe(x1: int, y1: int, x2: int, y2: int, ms: int = 400) -> None:
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    time.sleep(0.7)


def back() -> None:
    adb("shell", "input", "keyevent", "4")
    time.sleep(0.8)


def texts() -> list[str]:
    return [n["text"] for n in nodes() if n["text"]]


if __name__ == "__main__":
    print("\n".join(texts()))
