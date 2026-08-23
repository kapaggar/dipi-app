#!/usr/bin/env python3
"""Walk every 1.19.0 desk screen on the Pixel C and screenshot."""
from __future__ import annotations

import time

from tour import back, find, shot, swipe, tap, tap_xy, texts

step = 0


def snap(slug: str) -> None:
    global step
    step += 1
    shot(f"{step:02d}-{slug}")


def open_and_back(label: str, slug: str, wait: float = 1.4) -> None:
    if not tap(label):
        snap(f"miss-{slug}")
        return
    time.sleep(wait)
    snap(slug)
    # Prefer an on-screen Back / ← Centre if present
    if find("Back") and tap("Back"):
        time.sleep(0.6)
        return
    if find("← Centre") and tap("← Centre"):
        time.sleep(0.6)
        return
    back()
    time.sleep(0.6)


def main() -> None:
    snap("centre-as-landed")
    # Scroll up to the top of Centre (upcoming + lotus)
    swipe(1280, 400, 1280, 1500, 350)
    time.sleep(0.5)
    snap("centre-top")
    print("CENTRE TEXTS:", texts())

    # Centre desk tiles + ops row
    for label, slug, wait in [
        ("Centre Settings", "centre-edit-form", 2.0),
        ("Manage Courses", "manage-courses", 2.0),
        ("Advanced Search", "advanced-search", 1.5),
        ("Daily Activity", "daily-activity", 2.0),
        ("SMS Report", "sms-report", 2.0),
        ("Course Report", "course-report", 3.0),
        ("Bulk Mail", "bulk-mail-placeholder", 1.0),
        ("Letters", "letters", 2.0),
        ("Centre settings", "centre-ops-rooms", 1.5),
        ("Settings", "app-settings", 1.0),
    ]:
        # Course report / sheets may overlay — close first
        if find("Close") or find("✕") or find("Done"):
            tap("Close") or tap("✕") or tap("Done")
            time.sleep(0.4)
        open_and_back(label, slug, wait)
        snap(f"back-after-{slug}")

    # Close any leftover sheet overlay
    if find("Close"):
        tap("Close")
        time.sleep(0.4)

    # Pick the first older course (upcoming may be empty / offscreen)
    swipe(1280, 400, 1280, 1400, 350)
    time.sleep(0.4)
    snap("centre-before-course")
    course = find("Dhamma Sudha / 10 Day / 2026 / 5th-Aug to 16th-Aug")
    if course:
        tap_xy(course["cx"], course["cy"])
        time.sleep(2.5)
    else:
        # tap first course-looking row
        for t in texts():
            if "Dhamma Sudha" in t and "Day" in t:
                tap(t)
                time.sleep(2.5)
                break
    snap("desk-or-hub")
    print("DESK TEXTS:", texts())

    # Tablet rail sections
    for label, slug in [
        ("Board", "desk-board"),
        ("Check-in", "desk-checkin"),
        ("Audit", "desk-audit"),
        ("Calling", "desk-calling"),
        ("Rooms", "desk-rooms"),
        ("Applications", "desk-applications"),
    ]:
        if tap(label, contains=True) or tap(label.upper()) or tap(label.upper(), contains=True):
            time.sleep(1.0)
            snap(slug)
        else:
            snap(f"miss-{slug}")

    # Board exports if visible
    for label, slug in [
        ("Day 0 list", "sheet-day0-list"),
        ("Day 0 summary", "sheet-day0-summary"),
        ("Course summary report", "sheet-day11"),
        ("Seating plan", "sheet-seating"),
    ]:
        if find(label):
            tap(label)
            time.sleep(2.0)
            snap(slug)
            if find("Close"):
                tap("Close")
            else:
                back()
            time.sleep(0.6)

    # Open first applicant on Applications if any name-like row
    snap("desk-before-card")
    print("APP TEXTS:", texts())

    snap("tour-end")
    print("done", step, "shots")


if __name__ == "__main__":
    main()
