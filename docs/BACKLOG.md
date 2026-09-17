# Backlog

Open findings and pending owner decisions, distilled on 2026-09-17 from the retired
2026-09-13 tablet handovers, the desk 2.2 handover, and the 2.0.0 performance
follow-up. Items those documents raised that later shipped (rail at 840dp, Board
derivations, scope chip, five-status roll exclusion, last-day reconciliation, hall
cards, report presets) are not repeated. Decision-gated items that predate these are
in `DECISIONS.md` under "Parked / decision-gated items". Hard rules in `AGENTS.md`
apply to every item.

## Desk and Course ops UI (observed on Pixel C, 2.1.0)

1. **Centre matrix at 900dp.** Row labels collide with NM/OM totals in portrait. Wrap or abbreviate, or fall back to a single-column upcoming list (`CourseMatrixTable` in `CentreScreen.kt`).
2. **Course ops seating in portrait.** `SeatingPlanScreen.kt` has no compact branch; a 6-wide grid is letterboxed. One hall full-width or a vertical page per gender.
3. **Teacher "who is missing".** A read-only list of Expected worklist rows not on the teacher roll would help the AT. Diff the already-fetched worklist against teacher ids. GET only; never refetch `/teacher-list` to build it.
4. **Overflow sheets.** Confirm Seating Plan opened from the phone-hub overflow still lands on the native hall (`hubSheetLabel("Seating Plan")`). Valuable stays in overflow only, off the Board.
5. **Zero Day "Mark attended" on Expected rows.** Watch item: the button must never write or appear to write attendance. Prefer removing the affordance on the phone screen or routing the tile to Check-in.

## Desk 2.2 questions still with the owner

6. **Mark attended** stays local-only by decision. Label it as local, or approve a server write path (none exists today; Hard rule 5 forbids attendance writes).
7. **Which figure staff read aloud.** Day 0 sheet and app show different derivations (for example `67 → 57` vs `72 → 62`). Both are labelled since 2.2.0; the owner has not picked one.
8. **Room Chart PULL FROM SERVER** and local seating edits: define what a pull does to unsynced local allocations.
9. **Reconciliation window.** Last-day guidance fires on the exact last calendar day or server finalization. An optional after-end-date extension is a product decision. Dates never establish finalization.

## Performance (after 2.0.0 / 98)

Priorities from the 2026-09-11 device follow-up, in order. None is started.

10. Scope the native hall roll, application-card publications and freshness by session, centre and course; retain actual or unknown fetch time.
11. Coalesce identical concurrent worklist and centre requests keyed by session, centre, course and server filter; stop the redundant compact text-query GET.
12. Build course-wide duplicate and audit indexes once per immutable roll; evaluate merging the two audit passes.
13. Measure centre landing, dashboard and config stages; reuse already-fetched dashboard HTML.
14. Retain a Course ops entry job that rejects stale card and progress writes; design pause/resume for unfinished card buffering.
15. Measure image sampling and cancellation, WebView asset loads, cookie and token storage, remaining Compose calculations. Optimize only measured costs.

Owner decisions needed before that work: authorize a separate debuggable profiling APK (route templates, counts, status codes and durations only, no bodies or NPI); state the typical and longest registrar pause so keep-alive survival can be tested (both 20-minute requests stay until then); choose pause versus complete for interrupted Course ops buffering; agree what happens to cached applicants no longer in an authoritative roll while preserving local desk records. `ApplicantDao` still upserts without course-level replacement and `lastSync` is global; never delete local desk work as a cache optimization.

Never trade a new GET, a shorter or duplicated keep-alive, a teacher-list refetch, `?r=`, NPI persistence, a backend change or ML Kit activation for apparent speed.
