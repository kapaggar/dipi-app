# Four-course dashboard history grid

**Date:** 2026-09-05  
**Release:** 1.42.0 (`versionCode` 69)

## Goal

Show the four newest older courses on the centre dashboard. On the wide dashboard, the existing two-column older-course layout therefore renders a complete 2×2 grid instead of a three-card grid with one empty cell.

## Design

Raise the repository's `OLDER_COURSE_LIMIT` from 3 to 4. Both the live HTML path and mock path already apply this single limit after preserving the server/parser order, so the change exposes the next older course without adding an endpoint, changing parsing, or inventing sort logic.

The wide `CentreScreen` already chunks older courses by its two-column `columns` value and fills incomplete rows with spacers. Four items naturally render as two complete rows, so no Compose layout change is needed. The narrow phone layout continues to show the same items as a vertical list.

This change affects the older-course selector only. It does not change the upcoming-course status matrices, course loading transport, selected-course behavior, or Course ops running-course resolution.

## Validation and release

Update the repository limit test to prove that a five-item server response returns the newest four in original order and excludes the fifth. Keep the shorter-list test to prove lists below the cap pass through. Add a wide Compose assertion that four older course controls form two rows of two equal-width controls.

Ship as the user-visible minor release 1.42.0 (`versionCode` 69), update the shipped ledger, run the full five-module suite, assemble release, install and launch the release APK on the Pixel C, merge and push `main`, then publish GitHub release `v1.42.0` with both `dipi-staff-1.42.0.apk` and `dipi-staff.apk`, marked latest.
