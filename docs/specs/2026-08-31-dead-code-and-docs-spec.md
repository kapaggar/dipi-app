# Dead-code sweep + living-contract refresh — spec (T8)

**Status:** proposed, 2026-08-31
**Baseline:** whatever the T1–T7 waves leave on `main` (this task runs LAST).
**Origin:** repo scan 2026-08-31. Two halves: delete verified-dead code, and
bring the design pipeline's living contract back in sync so the next design pass
does not re-propose decided departures.

## Half A — dead code (all verified zero-consumer on `main` @ `0ce3342`)

Re-verify each with a repo-wide grep before deleting — T1–T7 may have added a
consumer since the scan. Anything that gained a consumer: skip it and say so in
the task report.

| Delete | Where | Note |
|---|---|---|
| `CoursesScreen` alias | `feature/course/.../CentreScreen.kt:814-820` | zero callers incl. tests |
| `ComingScreen` alias | `feature/course/.../DeskActionScreen.kt:53-58` | zero callers |
| `Hairline` composable | `core/ui/.../Components.kt:85-94` | superseded by hairline modifiers |
| `Blueprint.kt` entire file | `core/ui/.../Blueprint.kt` | self-documented as kept-for-reference |
| `OutboxOp`, `OutboxState` | `core/model/.../Models.kt:148-159` | zero references; outbox uses raw strings |
| `ApplicantStatus.fromServer` | `core/model/.../ApplicantStatus.kt:40` | zero callers |
| `ConfNo.looksLikeConf` | `core/model/.../Ids.kt:16` | only `ApplicantStatusTest.kt:50-52` — delete the function AND those test lines (authorized: the test's only subject is the deleted symbol) |
| `nextCursor` | `Models.kt:120` + `Dtos.kt:82` (+ the unused `cursor` param on `StaffApi.applicants` if removable without touching the mock dispatcher's route match) | no pagination exists |
| `Course.typeKey` / `CourseDto.typeKey` | `Models.kt:96`, `Dtos.kt:70` | set only by mock, rendered nowhere |
| `photoUrl` | `SearchPageParser.kt:290-297` builder, `Dtos.kt:129`, `Models.kt:48`, `MockFixtures.kt:362` | parsed and persisted into the Room payload blob with zero readers. Room JSON uses `ignoreUnknownKeys` — old cached payloads keep deserializing. If any parser test asserts it, retarget that assertion to the fields that remain. **Deleting is also NPI-adjacent hygiene: one less applicant-linked URL in the Room blob.** |
| `SearchPageParser.dataset(html, photoHost)` | `SearchPageParser.kt:189` | superseded by `parse()`, zero callers |
| `AccoHandlerParser.rooms()` wrapper | `AccoHandlerParser.kt:45` | test-only; retarget its 8 test call sites to `roomsOrNull(...).orEmpty()` |
| unused strings | `app/src/main/res/values/strings.xml`: `letter_notice`, `status_updated`, `no_photos`, `uploaded_photos` | the last two exist as Kotlin literals in `StaffRepository.kt:477,492` — leave the literals |
| `DipiColors.photoFixed/photoAuto/photoSuggest/photoNone` | `DipiTheme.kt:35-38` (both palettes) | zero consumers |

**Deliberately KEPT (do not delete):**
- `CourseMatrix.highlights` + `HIGHLIGHT_LABELS` — superseded by `cardRows` but
  kept by explicit 2026-08-30 decision (breaking `CourseMatrixTest` for no
  benefit); see `centre-trim-spec.md` §S3.
- `SearchPage.tokens/centres/courses/pathCentreId` — computed-and-discarded on
  the worklist path but load-bearing for the T6 history port's fragment checks
  and asserted by `SearchPageParserTest`; cheap, leave.
- `DeskSectionPlaceholder` (`DeskShell.kt:229-241`) — live default parameter.
- `SearchPage.statuses` — consumed after T3.
- `syncRoomAllocation` (singular) — internal seam of `syncRoomAllocations`.
- Everything in `DipiMockDispatcher`/`MockFixtures` — mock mode is the fixture
  bench; release pins `USE_MOCK=false`.

## Half B — docs

1. **`version-4/uploads/dipi-ui-export/SHIPPED-DELTA.md`** — append a dated
   "Post-v4 owner decisions (2026-08-30/31)" section to the do-not-re-propose
   list: (a) the 416dp two-column lower pane is gone — older courses render on
   the upcoming grid with the desk column stacked beneath
   (`centre-trim-spec.md` §S4); (b) the 460dp/60% upcoming ceiling and
   independent pane scrolls are gone — fixed header + one below-header scroll
   (card-bloat fix, commit `4ba1b3b`); (c) matrix cards render fixed
   `cardRows` with `Confirmed + Expected` summed (§S3); (d) Day-11 chip now
   ships on the design's fourth-line row (T1); (e) whichever T7 items shipped.
2. **`AGENTS.md`** — refresh the "Shipped" line to the post-wave version; add a
   one-liner under assumptions that the status vocabulary now comes from the
   worklist page's own select with roster fallback (T3); note the Day-11 Board
   chip is placed (T1 — the current §9 text says it is not).
3. **`CLAUDE.md`** — same two version/Day-11 corrections, keep it terse.
4. **`docs/CLAUDE-DESIGN-DESK-SCREENS.md`** — sync the Board section (13th chip)
   and anything T7 moved.
5. Confirm `docs/specs/2026-08-28-v4-design-pass-spec.md` R2 correction was made
   by T1; if not, make it here.

## Tests

Full suite green after every deletion batch (one commit per table row-group is
fine; suite at least per batch). No behavioural tests are added — deletions are
pinned by compilation + the retargeted assertions named above.

Never touched (beyond the two explicitly authorized retargets above): every
other test file. If a deletion breaks any other test, that symbol was not dead —
restore it and report, do not edit the test.

## Constraints

No agent trailers; never bare `./gradlew test`; version bump per master plan
(PATCH — no user-visible change unless bundled into a wave's MINOR).
