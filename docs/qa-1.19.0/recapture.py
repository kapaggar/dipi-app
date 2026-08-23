#!/usr/bin/env python3
"""Recapture every screen with file-pull screenshots (exec-out is corrupt on Pixel C)."""
from __future__ import annotations

import sys
import time

from tour import back, find, shot, swipe, tap, tap_xy, texts

sys.stdout.reconfigure(line_buffering=True)

OUT_PREFIX = "r"


def snap(slug: str) -> None:
    shot(f"{OUT_PREFIX}-{slug}")


def wait(s: float = 1.0) -> None:
    time.sleep(s)


def to_centre() -> None:
    for _ in range(6):
        ts = texts()
        if "Upcoming courses" in ts or "Centre desk" in ts:
            return
        back()
        wait(0.5)


def scroll_tiles() -> None:
    for _ in range(5):
        if find("Daily Activity") or find("Letters"):
            return
        swipe(1280, 1450, 1280, 500, 320)
        wait(0.35)


def open_back(label: str, slug: str, settle: float = 1.8) -> None:
    scroll_tiles()
    if not tap(label):
        snap(f"miss-{slug}")
        return
    wait(settle)
    snap(slug)
    if find("Back"):
        tap("Back")
    elif find("← Centre"):
        tap("← Centre")
    else:
        back()
    wait(0.5)


def main() -> None:
    to_centre()
    swipe(1280, 400, 1280, 1500, 300)
    wait(0.4)
    snap("01-centre-top")

    scroll_tiles()
    snap("02-centre-tiles")

    open_back("Centre Settings", "03-centre-edit", 2.2)
    open_back("Manage Courses", "04-manage-courses", 2.2)
    open_back("Advanced Search", "05-advanced-search", 1.5)
    open_back("Daily Activity", "06-daily-activity", 2.2)
    open_back("SMS Report", "07-sms-report", 2.2)
    # Course report may open a system viewer — grab then back twice
    scroll_tiles()
    if tap("Course Report"):
        wait(3.0)
        snap("08-course-report")
        back()
        wait(0.6)
        if find("Course Report") is None and find("Upcoming courses") is None:
            back()
            wait(0.5)
    open_back("Bulk Mail", "09-bulk-mail", 1.0)
    open_back("Letters", "10-letters", 2.0)
    if tap("Centre settings"):
        wait(1.2)
        snap("11-centre-ops")
        if find("Back"):
            tap("Back")
        else:
            back()
        wait(0.5)
    scroll_tiles()
    if tap("Settings"):
        wait(1.0)
        snap("12-settings")
        back()
        wait(0.5)

    # Upcoming course → desk
    swipe(1280, 400, 1280, 1500, 300)
    wait(0.4)
    n = find("Dhamma Sudha / 10 Day / 2026 / 19th-Aug to 30th-Aug")
    if n:
        tap_xy(n["cx"], n["cy"])
        wait(2.6)
    snap("13-desk-board")

    for label, slug in [
        ("Check-in", "14-checkin"),
        ("Audit", "15-audit"),
        ("Calling", "16-calling"),
        ("Rooms", "17-rooms"),
        ("Applications", "18-applications"),
        ("Board", "19-board-again"),
    ]:
        if tap(label, contains=True):
            wait(1.0)
            snap(slug)

    # HTML sheets only (no PDF/XLS handoff)
    if tap("Board"):
        wait(0.6)
    for label, slug in [
        ("Day 0 list", "20-day0-list"),
        ("Day 0 summary", "21-day0-summary"),
        ("Teacher list", "22-teacher-list"),
        ("Seating plan", "23-seating"),
    ]:
        if tap("Board"):
            wait(0.3)
        if tap(label):
            wait(2.2)
            snap(slug)
            back()
            wait(0.6)

    if tap("Applications"):
        wait(1.0)
    snap("24-applications-detail")
    # first likely name
    for t in texts():
        if t.count(" ") == 1 and t[0].isupper() and len(t) < 32 and "Dhamma" not in t:
            if tap(t):
                wait(1.2)
                snap("25-applicant-card")
                break

    snap("26-end")
    print("recapture complete")


if __name__ == "__main__":
    main()
