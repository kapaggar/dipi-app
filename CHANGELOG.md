# Changelog

## [2.0.0] - 2026-09-10

### Fixed

- Move desk HTML parsing, row preparation and observed-roll audits off the main thread.
- Reuse worklist parser regexes without changing parsed results.
- Stop the desk minute clock while backgrounded or screen-off; show current time on return.
- Skip the Day 0 summary request for finalized courses.
- Guard delayed processing against session/course changes while retaining Sign-in on session expiry.

### Added

- Record the Pixel C baseline, network and scheduler inventory, ranked findings, implementation, tests and measurement limitations.

Version 2.0.0 / 98 is the owner-selected release number for these changes. The
unpublished 1.46.3 / 97 preparation is superseded. Photo review remains off,
ordinary photos remain available, and the 20-minute keep-alive is unchanged.
Device after-measurements are pending; no tablet speedup or battery saving is claimed.

See the [performance record](docs/plans/2026-09-10-desk-performance.md) and
[full baseline analysis](docs/reports/2026-09-10-android-performance-baseline.md).

[2.0.0]: https://github.com/kapaggar/dipi-app/compare/v1.46.2...v2.0.0
