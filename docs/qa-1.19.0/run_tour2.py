#!/usr/bin/env python3
"""Second pass: centre tiles (scroll down) + remaining desk actions."""
from __future__ import annotations

import time

from tour import back, find, shot, swipe, tap, tap_xy, texts

step = 40


def snap(slug: str) -> None:
    global step
    step += 1
    shot(f"{step:02d}-{slug}")


def wait(s: float = 1.2) -> None:
    time.sleep(s)


def close_overlay() -> None:
    for label in ("Close", "✕", "Done", "BACK", "Back"):
        if find(label):
            tap(label)
            wait(0.5)
            return
    back()
    wait(0.5)


def open_and_back(label: str, slug: str, wait_s: float = 1.8) -> None:
    if not tap(label):
        snap(f"miss-{slug}")
        return
    wait(wait_s)
    snap(slug)
    close_overlay()
    wait(0.4)


def main() -> None:
    # Back to Centre if we are on the desk
    if find("DESK") or find("Board"):
        # no centre button — keep hitting back
        for _ in range(3):
            if find("Upcoming courses") or find("Centre desk"):
                break
            back()
            wait(0.5)
    snap("pass2-start")
    print("START", texts()[:20])

    # Scroll down until Centre desk tiles appear
    for _ in range(6):
        if find("Daily Activity") or find("Letters") or find("Manage Courses"):
            break
        swipe(1280, 1400, 1280, 500, 350)
        wait(0.4)
    snap("centre-desk-tiles")
    print("TILES", texts())

    for label, slug, w in [
        ("Centre Settings", "centre-edit-form", 2.2),
        ("Manage Courses", "manage-courses", 2.2),
        ("Advanced Search", "advanced-search", 1.6),
        ("Daily Activity", "daily-activity", 2.2),
        ("SMS Report", "sms-report", 2.2),
        ("Course Report", "course-report", 3.5),
        ("Bulk Mail", "bulk-mail-placeholder", 1.0),
        ("Letters", "letters", 2.2),
        ("Settings", "app-settings", 1.2),
    ]:
        # re-scroll tiles into view each time
        for _ in range(4):
            if find(label):
                break
            swipe(1280, 1400, 1280, 500, 300)
            wait(0.3)
        open_and_back(label, slug, w)
        # after back we may be mid-page
        wait(0.3)

    # Open upcoming course (current one) for remaining board sheets
    swipe(1280, 400, 1280, 1500, 350)
    wait(0.4)
    snap("centre-upcoming")
    upcoming = find("Dhamma Sudha / 10 Day / 2026 / 19th-Aug to 30th-Aug")
    if upcoming:
        tap_xy(upcoming["cx"], upcoming["cy"])
        wait(2.8)
    snap("upcoming-desk")
    print("UPCOMING DESK", texts()[:25])

    if tap("Board"):
        wait(0.8)
    snap("board-full")
    # scroll board to last export row
    swipe(1600, 1400, 1600, 600, 350)
    wait(0.4)
    snap("board-exports-scrolled")
    print("BOARD", [t for t in texts() if "list" in t.lower() or "PDF" in t or "report" in t.lower() or "plan" in t.lower() or "chit" in t.lower() or "slip" in t.lower()])

    for label, slug in [
        ("Day 0 list", "sheet-day0-list"),
        ("Day 0 summary", "sheet-day0-summary"),
        ("Student chit", "sheet-student-chit"),
        ("Checking slip", "sheet-checking-slip"),
        ("Teacher list", "sheet-teacher-list"),
        ("Seating plan", "sheet-seating"),
        ("Course report", "sheet-course-report"),
        ("Course summary report", "sheet-day11"),
    ]:
        if not find(label):
            swipe(1600, 1400, 1600, 700, 300)
            wait(0.3)
        if tap(label):
            wait(2.4)
            snap(slug)
            close_overlay()
            wait(0.4)
            if tap("Board"):
                wait(0.4)

    # Applications → first name → history
    if tap("Applications"):
        wait(1.0)
    snap("apps-for-card")
    # tap first applicant name if present
    for t in texts():
        if t and t[0].isupper() and " " in t and t not in {
            "Dhamma Sudha", "Rooms & seats", "SHEETS & EXPORTS · RARELY URGENT",
        } and len(t) < 40 and "Day" not in t:
            # likely a person
            if tap(t):
                wait(1.2)
                snap("applicant-card")
                for sec in ("Prior courses", "Activity", "Clarifications", "Courses", "HISTORY"):
                    if find(sec, contains=True):
                        tap(sec, contains=True)
                        wait(1.4)
                        snap(f"history-{sec.lower().replace(' ', '-')}")
                if tap("EDIT"):
                    wait(2.0)
                    snap("app-edit-page")
                    close_overlay()
                break

    # Check-in mark dialog if possible
    if tap("Check-in"):
        wait(0.8)
    snap("checkin-again")
    if tap("PULL FROM SERVER") or tap("Pull from server"):
        wait(2.5)
        snap("rooms-after-pull-attempt")

    if tap("Rooms") or tap("Rooms & seats", contains=True):
        wait(0.8)
    snap("rooms-again")
    if tap("PULL FROM SERVER"):
        wait(2.5)
        snap("rooms-pulled")

    snap("pass2-end")
    print("done", step)


if __name__ == "__main__":
    main()
