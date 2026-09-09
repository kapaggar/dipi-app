# v7 vs `main` — screen test map and cherry-pick groups

Owner test + apply guide. **Do not treat this as a commit list.** Nothing below has been cherry-picked, merged, committed, or pushed.

## What the git graph actually is

| Ref | Commit | SemVer |
| --- | --- | --- |
| `main`, `origin/main`, `feat/dipi-v7` HEAD | `64db74e` — *1.46.1 Room Chart age once at top-right* | **1.46.1 / 95** |
| Working tree on `feat/dipi-v7` (dirty) | no new commits | **1.48.0 / 99** |

```
git log --oneline main..feat/dipi-v7   # empty
git log --oneline feat/dipi-v7..main   # empty
git diff --stat main...feat/dipi-v7    # empty
```

`feat/dipi-v7` is **the same commit as `main`**. The product delta is the **uncommitted working tree** versus `64db74e`.

Tracked delta: **59 files, +1941 / −759**. Untracked: **~439 paths**, most of them the handover snapshot under `docs/handovers/dipi-v7-implementation-2026-09-09/` (a frozen copy of the whole tree — not a patch to apply).

There is **no commit-by-commit cherry-pick**. Apply later as **logical file groups** (`git add -p` / split commits). Suggested groups are at the end.

### Version layers sitting in the dirty tree (never shipped on `main`)

`main` is **1.46.1**. These are **not** on `main`:

| Claimed | Code | What the owner would notice |
| --- | ---: | --- |
| 1.46.2 | 96 | Room Chart tall occupied names **17sp Medium** |
| 1.46.3 | 97 | Audit **Open pins that applicant** on Applications (no more every row → first in-scope card) |
| 1.47.0 | 98 | Compatible v7: health Yes/No, Left out of arrival/call rounds, Audit related IDs + Back, Room Chart Jump/Next, Steel night, honest freshness, report presets fill-only, Fit/Readable **screen-only** |
| 1.48.0 | 99 | Responsive pin/wrap, repeatable room reveal, dark card contrast, chit/slip print heights, **photo review default off** |

If they want one tablet version after a partial apply, bump SemVer themselves. Do not blindly take `1.48.0` / 99 unless the whole tree ships.

### Hard rules (unchanged; verify on every pick)

- Never send `?r=` on sheet GETs (bulk seat auto-allocation).
- Never send status `Approved`.
- Teacher-list GET mutates the server — fetch **once per course entry**, never refetch on filter / width / theme.
- Course ops stays read-only. No attendance engine. Server messages verbatim.
- NPI stays in-memory (`SensitiveInfo`) only.

---

## Screen / rail blocks

**22 groups.** Theme and freshness are listed as surfaces because they change what every other screen looks like.

---

### 1. Centre dashboard

- **What changed**
  - Course report, App Settings, and Centre Settings sit on **one tablet row** and wrap on a phone.
  - Course-report tile still carries NEW; older-course 2×2 grid on wide layouts is unchanged.
- **Key files**
  - `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreScreen.kt`
  - `app/src/test/kotlin/org/dhamma/dipi/staff/CentreScreenWideTest.kt`
- **Pixel C**
  - Open the centre. Confirm the three actions share one row in landscape; rotate or shrink and confirm they wrap instead of overflowing.
- **Grouping:** **optional** UI once theme cluster is present.
- **Risk without siblings:** compile fail if `ThemeIndustry` is missing. No transport change.

---

### 2. Centre Settings (hall cards)

- **What changed**
  - Male hall and Female hall are **separate cards** (columns × depth still the same staged prefs).
  - Chowky/chair rail copy stays below.
- **Key files**
  - `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CentreOpsScreen.kt`
- **Pixel C**
  - Centre → Centre Settings. Confirm two hall cards; change one gender’s columns and confirm the other is untouched.
- **Grouping:** **optional** (small wrap). Needs theme alias.
- **Risk without siblings:** none beyond theme. Hall geometry rules unchanged.

---

### 3. 0 Day Board

- **What changed**
  - Tiles tell the truth: **On the roll** = students + sevaks; Checked in is **% of eligible arrivals** (Left excluded); Still to call uses the call **round** (Left held out); Needs attention = findings · applicants.
  - Each 3×3 export cell shows a **format caption** (HTML / Excel / native / PDF).
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/BoardPane.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskScope.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskDerive.kt`
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/SheetPresentation.kt`
- **Pixel C**
  - Open a course with some Left. Board “Checked in” and “still to arrive” should ignore Left. Tap Course summary / Day 0 / chits and confirm captions.
- **Grouping:** **atomic** with Check-in + Calling populations (`DeskScope` + `DeskDerive`). Format captions need `SheetPresentation`.
- **Risk without siblings:** Board numbers disagree with Check-in / Calling. 3×3 cell **order** is unchanged; Valuable stays off the Board.

---

### 4. Applications

- **What changed**
  - **1.46.3:** Open from Audit **pins that person**. Gender / New-Old / status chips no longer snap the card to the first in-scope row.
  - **1.47:** “Opened from Audit” + **Back** returns to the finding (or “This finding is no longer present”).
  - **1.48:** Status chips wrap; list/detail stack on a narrow pane.
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/ApplicationsPane.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskDerive.kt` (`deskSelectedApplicant`, `deskApplicationList`)
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`
  - `app/src/test/kotlin/org/dhamma/dipi/staff/AuditNavigationContextTest.kt`
- **Pixel C**
  - Audit → a `shared_email_unrelated` (or any multi-person) finding → Open the **second** name. Confirm that card, not the first. Tap Back. Change Male/Female while pinned — the opened person stays.
- **Grouping:** **atomic** with Audit + `DeskViewModel` pin/context. Do not ship Applications pin UI without `deskSelectedApplicant`.
- **Risk without siblings:** Open looks like it works and still shows the wrong card (the 1.46.1 bug).

---

### 5. Audit

- **What changed**
  - Heading stays **pinned**; evidence list scrolls underneath.
  - Related people are **identified matches** with their **own Open** (from `relatedApplicantIds` — conf dup, shared phone, name+DOB, shared mobile, unrelated shared email).
  - Narrow width stacks list over detail.
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/AuditPane.kt`
  - `core/audit/src/main/kotlin/org/dhamma/dipi/staff/audit/ClientAudit.kt`
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/Models.kt` (`AuditFlag.relatedApplicantIds`)
  - `core/audit/src/test/kotlin/org/dhamma/dipi/staff/audit/AuditRelatedApplicantsTest.kt`
- **Pixel C**
  - Open a duplicate/shared finding. Confirm every listed person has Open, and each Open is a different applicant. Scroll evidence; heading stays.
- **Grouping:** **atomic** with Applications pin + `ClientAudit` related IDs + merge that keeps related IDs when client/server flags combine.
- **Risk without siblings:** UI Open buttons with empty related IDs; or related IDs with no pin (wrong card). Merge change is required if any path still merges client+server flags.

---

### 6. Calling

- **What changed**
  - **Left is held out of this call round.** Status is not rewritten — they appear only under **All**.
  - Controls wrap; primary actions are taller (48dp).
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/CallingPane.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskScope.kt` (`deskCallRound`, `deskCallHeldOutCount`)
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskDerive.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskWidgets.kt`
- **Pixel C**
  - Mark someone Left (or use a course that has Left). Calling “To call” / batch should omit them; All still lists them. WhatsApp/status write paths unchanged — do not send a test student message.
- **Grouping:** **atomic** with Board + Check-in populations.
- **Risk without siblings:** Board “still to call” ≠ Calling list.

---

### 7. Check-in

- **What changed**
  - Tabs: **To arrive / Arrived / Left / All**. Left is excluded from To arrive and Arrived; progress is **of eligible** (roll − Left).
  - Sidebar room free counts use **whole-course occupancy**, not the gender/seniority filter.
  - Long emails wrap; scan/list regions bounded on phone.
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/CheckInPane.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskScope.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DeskDerive.kt`
- **Pixel C**
  - Filter Left — those rows only. Switch gender; room “free” on the sidebar should not jump just because the roster filter changed. Finalized course: still read-only, no allocation sync.
- **Grouping:** **atomic** with Board + Room Chart occupancy (`deskOccupied` / `deskRoomAvailability`).
- **Risk without siblings:** “3 free” on Check-in vs Room Chart disagree. Allocation POST fields must stay the desk dialog’s own (`s,r,g,…`) — this pick does not change that.

---

### 8. Room Chart

- **What changed**
  - **1.46.2 (not on main):** occupied tall cells use **17sp Medium** names. Age stays muted **12sp top-right only** (already on 1.46.1). Old solid / New dashed / Available legend unchanged.
  - **1.47:** block chips, **Jump to room**, **Next occupied**.
  - **1.48:** title, scope, blocks, jump stay **above** the scrolling grid. Jump/Next **bring the cell into view**, including **the same room again**. Valid jump dismisses the keyboard; invalid jump keeps focus. Long sync-refusal lists stay bounded.
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/RoomsPane.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/RoomNavigation.kt`
  - `app/src/test/kotlin/org/dhamma/dipi/staff/RoomNavigationTest.kt`
  - `app/src/test/kotlin/org/dhamma/dipi/staff/RoomsPaneTest.kt`
- **Pixel C**
  - Jump to a known occupied room (e.g. 80). Confirm the cell is on screen and the name is large. Jump to **the same room again**. Next occupied walks occupied rooms. Do not infer occupancy from a room string on a non-arrived / Left record.
- **Grouping:** **atomic** (`RoomsPane` + `RoomNavigation` + occupancy helpers). 17sp is in the same file as Jump — splitting is a surgical edit, not a separate commit today.
- **Risk without siblings:** Jump with no `bringIntoView` looks like a no-op on a long grid. Occupancy without `DeskScope` can count Left as occupying.

---

### 9. Course report

- **What changed**
  - Presets (**This month / This year / Last 12 months**) **fill dates only**. **Run** still starts the request.
  - Date/preset row wraps; table + totals share one horizontal scroll.
  - Teacher names from the CSV (already 1.46.0 on `main`) stay.
- **Key files**
  - `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CourseReportScreen.kt`
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/ReportDateRange.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt` (`applyReportPreset`)
  - `app/src/test/kotlin/org/dhamma/dipi/staff/CourseReportRunTest.kt`
- **Pixel C**
  - Centre → Course report → This month. Dates fill; table does **not** load until Run. Print/share still the last successful report.
- **Grouping:** **atomic** (screen + `ReportDateRange` + ViewModel preset). Safe relative to desk rails.
- **Risk without siblings:** presets that auto-run (do not take UI without `onPreset` = fill-only). Empty-range guidance from 1.38.0 must remain.

---

### 10. Settings

- **What changed**
  - Dark copy states it is **Steel night** (saved light skin is remembered, not used at night).
  - Tablet-mode card tightens on a stacked/narrow pane; Health answers sentence unchanged.
  - Readable caption/secondary colors on mode text.
- **Key files**
  - `feature/settings/src/main/kotlin/org/dhamma/dipi/staff/settings/SettingsScreen.kt`
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/ThemeIndustry.kt`
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/ReadableTokens.kt`
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DipiTheme.kt`
- **Pixel C**
  - Settings → Dark. Cards/text readable (not washed). Flip Light — chosen skin returns. Simulate offline still applies on tap (1.45.4).
- **Grouping:** **atomic** with the theme cluster. Photo capability has **no** Settings runtime toggle (build-time only).
- **Risk without siblings:** Settings says Steel night while screens still paint the old light-skin night ramp.

---

### 11. Course ops — Teacher list

- **What changed**
  - Long teacher names **wrap**. Table has a **finite width** and scrolls horizontally.
  - Theme-aware text (Steel night in dark).
- **Key files**
  - `feature/teacher/src/main/kotlin/org/dhamma/dipi/staff/teacher/TeacherListScreen.kt`
  - `app/src/test/kotlin/org/dhamma/dipi/staff/TeacherListScreenTest.kt`
  - `app/src/test/kotlin/org/dhamma/dipi/staff/CourseOpsResponsiveTest.kt`
- **Pixel C**
  - Course ops → Teacher list. Confirm one fetch on entry; changing gender/width/theme does **not** hit `/teacher-list` again. Scroll sideways; names wrap.
- **Grouping:** **optional** with other course-ops layout files; **atomic** with theme.
- **Risk without siblings:** none for transport if they only take layout. Never add a refetch.

---

### 12. Course ops — Seating plan

- **What changed**
  - Hall + furniture rail stay **teacher-at-bottom**, 66dp cells, CW-A1 at the bottom of the vertical rail.
  - Wide layout can place the rail beside the hall; unseated list is **height-bounded** and scrolls.
  - Header controls wrap / 48dp targets.
- **Key files**
  - `feature/teacher/src/main/kotlin/org/dhamma/dipi/staff/teacher/SeatingPlanScreen.kt`
- **Pixel C**
  - Open seating. Confirm Dhamma seat / teacher row still at the bottom. Open a student; system Back returns to the plan. Do not PRINT from this screen expecting Drupal `/seating` — native print is in-memory only.
- **Grouping:** **optional** layout; keep with student-card nav.
- **Risk without siblings:** theme compile. Geometry must not pick up prototype seven-column halls.

---

### 13. Course ops — Student card

- **What changed**
  - **Phone:** **one** vertical scroll for the loaded card (no nested scroll fight). Tablet: facts / right column scroll independently.
  - Health: six positions; **Yes · as supplied / No · as supplied / Response recorded / Not provided**; male Pregnancy = **Not applicable** (app-authored, not a source answer). History tiles can show **Not provided** when the source cell was absent (`historyCountsPresent`).
- **Key files**
  - `feature/teacher/src/main/kotlin/org/dhamma/dipi/staff/teacher/StudentCardScreen.kt`
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/HealthAnswerPresentation.kt`
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/ApplicationCard.kt`
  - `core/network/src/main/kotlin/org/dhamma/dipi/staff/network/ApplicationViewParser.kt`
  - `core/datastore/src/main/kotlin/org/dhamma/dipi/staff/datastore/CourseOpsStore.kt`
- **Pixel C**
  - Open a card on the tablet, then constrain width / font scale 1.3 if a phone is handy. Confirm one scroll. Check a male Pregnancy row. Confirm Course History teachers still come from the view page (edit-form GET only when omitted — never POST).
- **Grouping:** **atomic** (card + health helpers + parser + encrypted course-ops cache field).
- **Risk without siblings:** badges without `historyCountsPresent` will treat missing counts as `0`. Cache field is additive/nullable — old blobs still decode.

---

### 14. Sheets — viewer chrome (Day 0 list, teacher list, manager list, laundry, valuable, Day 11)

- **What changed**
  - Title / format / actions wrap. Phone column chips still scroll sideways. Pinch-zoom below 600dp kept.
  - **Fit width / Readable** is remembered per export. CSS applies under **`@media screen` only**.
  - Format line comes from `sheetPresentation` (e.g. Day 0: HTML · A4 portrait · Day 0 Contact hidden in print).
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetViewerPane.kt`
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetStylesheet.kt`
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/SheetPresentation.kt`
  - `core/datastore/src/main/kotlin/org/dhamma/dipi/staff/datastore/SessionStore.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`
- **Pixel C**
  - Board → Day 0 list. Toggle Readable. On screen the page may narrow; **Print** must still be full A4 (contact hidden). Confirm the GET URL has no `r`.
- **Grouping:** **atomic** with print CSS (same stylesheet). Session key `sheet_readable` is new and harmless if unused.
- **Risk without siblings:** Readable that also shrinks **print** (the 1.47 defect this exists to prevent).

---

### 15. Sheets — Student chits print

- **What changed**
  - Native PrintManager showed **nine-up** until min-height grew: chits are **63.3 × 69mm minimum**, height can grow. Acceptance: 25 synthetics paginate **12 / 12 / 1**.
  - Screen layout still 12-up anatomy (seat → room → name).
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetStylesheet.kt`
  - `app/src/androidTest/kotlin/org/dhamma/dipi/staff/V7DevicePrintTest.kt` (device only)
- **Pixel C**
  - Board → Student chit → Print. Count per printed page (not the on-screen grid). A4, 10mm margins.
- **Grouping:** **atomic** with the stylesheet + viewer (do not split `@media print` from `@media screen`).
- **Risk without siblings:** taking only the 69mm rule without screen/print split can still re-break Readable-vs-print. Never add `?r=`.

---

### 16. Sheets — Checking slips print

- **What changed**
  - Slips **190 × 138mm minimum**, grow with content. Acceptance: five synthetics paginate **2 / 2 / 1**.
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SheetStylesheet.kt`
- **Pixel C**
  - Board → Checking slip → Print. Two stacked slips per page, last page one.
- **Grouping:** **atomic** with chits / stylesheet (same `@media print` block).
- **Risk without siblings:** same as chits.

---

### 17. Sheets — native seating print

- **What changed**
  - **`SeatingPrint.kt` is not in the dirty tree.** A4 landscape, in-memory roll, no `GET /seating`, no `?r=` — still 1.42.1 behavior on `main`.
  - On-screen Course ops hall chrome changed (group 12); print HTML path did not.
- **Key files**
  - (unchanged) `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/SeatingPrint.kt`
- **Pixel C**
  - If they print seating after other picks: one gender per page, Dhamma seat below row 1, occupied CW/CH in the side rail. Regression only.
- **Grouping:** **do not pick** — nothing to apply.
- **Risk:** none.

---

### 18. Day Summary (Board cell)

- **What changed**
  - Colors go through `ThemeIndustry` so dark Day Summary cards are readable. Counts-only native screen; no sheet width control (`supportsReadingWidth = false`).
- **Key files**
  - `feature/desk/src/main/kotlin/org/dhamma/dipi/staff/desk/DaySummaryPane.kt`
- **Pixel C**
  - Board → Day 0 summary in Dark. Text on cards must read; numbers still from `#day-summary` parse.
- **Grouping:** **optional** once theme is in.
- **Risk without siblings:** theme compile only.

---

### 19. Phone Today / public card / Course hub

- **What changed**
  - Compact (default) build **hides** the Photos tile / ◎ / “correct photo” button. `openPhotos()` no-ops. Ordinary applicant **photo display** remains.
  - Enabled build (`-Pdipi.photoReview=true`) keeps today’s photo review.
- **Key files**
  - `feature/course/src/main/kotlin/org/dhamma/dipi/staff/course/CourseHubScreen.kt`
  - `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/TodayScreen.kt`
  - `feature/applicants/src/main/kotlin/org/dhamma/dipi/staff/applicants/CardScreen.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DeskViewModel.kt`
- **Pixel C**
  - Default APK: no Photos entry on hub / Today. Applicant photo still shows on the card. Do not upload a live student photo.
- **Grouping:** **atomic** with the photo flavor cluster. Defaults on `main` today are **always-on ML Kit** (1.45.x). Taking v7 **without** this cluster keeps photos on; taking gradle flavor **without** these hides/guards is a half-build.
- **Risk without siblings:** hidden UI but upload still reachable, or compact APK still bundles ML Kit (~20 MB).

---

### 20. Theme / Steel night (every dark surface)

- **What changed**
  - Dark is **Steel night for every saved light skin**. Light still uses the chosen skin.
  - `ThemeIndustry` is composition-aware (replaces static `Industry` in almost every pane). `deskCard` fill follows theme field color.
- **Key files**
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/ThemeIndustry.kt`
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/ReadableTokens.kt`
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DipiTheme.kt`
  - `core/ui/src/main/kotlin/org/dhamma/dipi/staff/ui/theme/DeskStyle.kt`
  - every pane that now `import ThemeIndustry as Industry`
- **Pixel C**
  - Set Appearance to Blossom (or any non-Steel) + Dark. Night must look Steel, not a tinted Blossom night. Day Summary, Settings, student card, Board tiles.
- **Grouping:** **atomic**. This is the compile backbone for nearly every UI file in the tree.
- **Risk without siblings:** cannot apply any pane that already switched the import.

---

### 21. Last-sync freshness (desk chrome)

- **What changed**
  - Missing / unparseable stamp → **Unknown**, not “just now”. Future stamp → absolute time + “Device clock differs”.
- **Key files**
  - `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/FreshnessText.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/ui/DipiAppUi.kt`
  - `core/model/src/test/kotlin/org/dhamma/dipi/staff/model/FreshnessTextTest.kt`
- **Pixel C**
  - Cold start / cached roll with no stamp: chrome says Unknown. After a real refresh, absolute + relative time.
- **Grouping:** **optional / safe solo** (model + one call site).
- **Risk without siblings:** none if both files move together.

---

### 22. Photo review (optional build, default off)

- **What changed**
  - `-Pdipi.photoReview` is **CLI only** (never `local.properties`). Default **false**.
  - Compact: no ML Kit dependency; stub detector; repository **refuses** live `POST /app/{id}/edit` photo write; controller/nav reject review routes.
  - Enabled: existing rotate/crop/scan/export + explicit full-form update. Same package/signing as compact — not side-by-side.
- **Key files**
  - `app/build.gradle.kts`
  - `app/src/photoDisabled/kotlin/org/dhamma/dipi/staff/photos/MlKitPhotoFaceDetect.kt`
  - `app/src/photoEnabled/kotlin/org/dhamma/dipi/staff/photos/MlKitPhotoFaceDetect.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/photos/PhotoScanner.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/photos/PhotoReviewController.kt`
  - `app/src/main/kotlin/org/dhamma/dipi/staff/data/StaffRepository.kt`
  - `docs/PHOTO-BUILDS.md`
- **Pixel C**
  - Install compact 1.48.0: Photos gone, display photos remain, APK ~7 MB. Only build enabled if they want correction; do **not** upload a production photo as a UI test.
- **Grouping:** **optional** product choice; internally **atomic**. SemVer 1.48.0 in gradle is bundled with this — split the version bump if they ship a subset.
- **Risk without siblings:** compile (missing `BuildConfig.PHOTO_REVIEW_ENABLED` or flavor sources). `main` 1.46.1 already has always-on correction; skipping this cluster **keeps** that.

---

## 1. Must-pick together

1. **Theme backbone** — `ThemeIndustry.kt`, `ReadableTokens.kt`, `DipiTheme.kt`, `DeskStyle.kt`, plus every UI file that already imports `ThemeIndustry`. Cannot split a single pane out of the dirty tree without reverting that import.
2. **Audit Open pin + related IDs + Back** — `Models.kt` (`relatedApplicantIds`), `ClientAudit.kt`, `DeskDerive` selection helpers, `DeskViewModel` pin/context, `AuditPane`, `ApplicationsPane`, `DipiAppUi` wiring. This **includes 1.46.3**, which is **not on `main`**.
3. **Desk populations** — `DeskScope.kt` + `DeskDerive` Left/call/room math + `BoardPane` + `CheckInPane` + `CallingPane` (+ Room Chart occupancy). Otherwise Board / Check-in / Calling / rooms lie about each other.
4. **Sheet screen width + print CSS** — `SheetPresentation.kt`, `SheetStylesheet.kt`, `SheetViewerPane.kt`, `SessionStore` / ViewModel width. Chit **69mm** and slip **138mm** live in the same print block. Do not take Readable without `@media screen`.
5. **Student-card health/history** — `HealthAnswerPresentation.kt`, `ApplicationCard.historyCountsPresent`, `ApplicationViewParser`, `CourseOpsStore`, `StudentCardScreen`.
6. **Room Chart Jump/Next + reveal + 17sp** — `RoomNavigation.kt` + `RoomsPane.kt`. **1.46.2 names are not on `main`.**
7. **Photo flavor (if wanted)** — gradle property + sourceSets + both `MlKitPhotoFaceDetect` flavors + scanner move + repository/controller/nav guards + hub/Today/Card hide.
8. **Course-report presets** — `ReportDateRange.kt` + `CourseReportScreen` + `applyReportPreset` (fill only).

## 2. Safe solo picks

Relative to the dirty tree — still need the **theme backbone** if the file already switched imports.

| Pick | Why it is safer |
| --- | --- |
| Freshness (`FreshnessText.kt` + `DipiAppUi` last-sync) | No rail semantics. |
| Centre action `FlowRow` | Layout only. |
| Centre Settings hall card wrap | Layout only. |
| Day Summary `ThemeIndustry` alias | After theme cluster. |
| Native seating print | **Nothing to pick** — already on `main`. |
| Course-report presets | Isolated from desk rails if ViewModel method is included. |
| Teacher-list wrap / finite table | After theme; no refetch. |
| `RoomNavigation.kt` alone | Pure functions — useless on screen until `RoomsPane` calls them. |

There is **no** safe solo for Audit Open without Applications pin. There is **no** safe solo for Fit/Readable without print CSS.

## 3. Do not pick

- **`docs/handovers/dipi-v7-implementation-2026-09-09/`** and the `.zip` / `.sha256` — snapshot of the whole repo at pack time, including `source/` copies of every module. Not an apply set.
- **`docs/plans/photo-correction-handoff/`** and its zip — prior photo handoff, not this delta.
- **`docs/handovers/dipi-v7-execution-log.md`**, **`docs/handovers/dipi-v7-ui-completion-report.md`**, **`docs/superpowers/plans/2026-09-09-dipi-v7-refinement.md`** — process/docs unless they want the paper trail.
- **`V7EvidenceDumpTest.kt`** and generated `files/v7-synthetic-evidence` — evidence, not product.
- **`app/src/androidTest/`** unless they want Pixel C instrumented visual/print. Not required to *run* the app.
- **`AGENTS.md` / `CLAUDE.md` / `docs/DESIGN.md` SemVer banners** — take only if the shipped version matches. Prefer rewriting the banner to whatever they actually apply.
- **`versionName` / `versionCode` 1.48.0 / 99** — take only for a full 1.48 ship. A partial apply needs its own bump (e.g. 1.46.2 or 1.47.0).
- Prototype / excluded policy from the v7 conflict register: identity masking, legend unification, pre-arrival occupancy, new dark policy, removed phone sheet controls — **not in this tree**. Do not pull them from the HTML design.

## 4. Logical patches (dirty tree — no commits on `feat/dipi-v7`)

Propose these as later `git add` groups. Not implemented here.

| Patch | Files |
| --- | --- |
| **A. Theme** | `ThemeIndustry.kt`, `ReadableTokens.kt`, `DipiTheme.kt`, `DeskStyle.kt`; import swaps in Desk/Course/Teacher/Settings/CourseOpsHost; `DarkTokensTest.kt`, `SkinTest.kt`, `ReadableTokensTest.kt` |
| **B. Populations** | `DeskScope.kt`, `DeskDerive.kt` (Left/call/room bits), `BoardPane.kt`, `CheckInPane.kt`, `CallingPane.kt`, `DeskWidgets.kt`; `DeskScopeTest.kt`, `DeskDeriveTest.kt`, `DeskPanesTest.kt` |
| **C. Audit + pin** | `Models.kt`, `ClientAudit.kt`, `AuditPane.kt`, `ApplicationsPane.kt`, ViewModel pin/context, `DipiAppUi` Audit/Applications wiring; `AuditRelatedApplicantsTest.kt`, `AuditNavigationContextTest.kt` |
| **D. Room Chart** | `RoomNavigation.kt`, `RoomsPane.kt`; `RoomNavigationTest.kt`, `RoomsPaneTest.kt` |
| **E. Health / history** | `HealthAnswerPresentation.kt`, `ApplicationCard.kt`, `ApplicationViewParser.kt`, `CourseOpsStore.kt`, `StudentCardScreen.kt` + their tests |
| **F. Course report** | `ReportDateRange.kt`, `CourseReportScreen.kt`, `applyReportPreset`; `CourseReportRunTest.kt` |
| **G. Sheets** | `SheetPresentation.kt`, `SheetStylesheet.kt`, `SheetViewerPane.kt`, `SessionStore` width, ViewModel width; `SheetViewerTest.kt`, `SheetPresentationTest.kt` |
| **H. Course ops layout** | `TeacherListScreen.kt`, `SeatingPlanScreen.kt`, `CourseOpsHost.kt`, remaining `StudentCardScreen` scroll; `CourseOpsResponsiveTest.kt`, `TabletModeTest.kt` |
| **I. Centre** | `CentreScreen.kt`, `CentreOpsScreen.kt`; `CentreScreenWideTest.kt` |
| **J. Freshness** | `FreshnessText.kt`, `DipiAppUi` `freshnessText(...)`; `FreshnessTextTest.kt` |
| **K. Photo (optional)** | `app/build.gradle.kts` (property/sourceSets/ML Kit), both flavor dirs, `PhotoScanner.kt`, `PhotoReviewController.kt`, `StaffRepository.kt` guard, hub/Today/Card/DipiAppUi/ViewModel; `docs/PHOTO-BUILDS.md`; photo tests. **Leave version at 1.46.1 until they choose a ship number.** |
| **L. Docs they may want** | `docs/plans/2026-09-09-v7-ui-completion.md`, this file, `docs/PHOTO-BUILDS.md`. Skip the handover snapshot. |
| **M. Device tests (optional)** | `app/src/androidTest/.../V7DeviceVisualTest.kt`, `V7DevicePrintTest.kt` |

`DeskViewModel.kt` and `DipiAppUi.kt` touch **C, D, F, G, J, K**. Apply those two files once after the clusters they care about, or `git add -p` by function (`openApplicantFromAudit`, `applyReportPreset`, `setSheetScreenWidth`, `PHOTO_REVIEW_ENABLED` guards).

---

## Suggested Pixel C pass (if they install the **whole** dirty tree)

Build compact: `./gradlew :app:assembleDebug` (no `-Pdipi.photoReview`). Install over Wi-Fi ADB. Then:

1. **Audit** — multi-person finding → Open row 2 → confirm card → Back.
2. **Room Chart** — Jump 80 twice; Next occupied; read 17sp name + top-right age.
3. **Check-in / Calling / Board** — Left excluded from arrival and call round; room free counts stable across gender filter.
4. **Sheets** — Day 0 Readable on screen; Print still A4; chits 12-up / slips 2-up; URL has no `r`.
5. **Course ops** — Teacher list wrap, no second `/teacher-list`; student card one scroll + Yes/No health; seating teacher-at-bottom.

Also: Course report preset then Run; Settings Dark = Steel night; no Photos tile on compact.

---

## Return line

- **Report:** `docs/plans/2026-09-09-v7-vs-main-cherrypick.md`
- **SemVer:** `main` / branch tip **1.46.1 / 95** (`64db74e`); dirty v7 **1.48.0 / 99**
- **Screen groups:** **22**
- **Top 5 must-test:** Audit Open pin + related Open + Back; Room Chart Jump/Next + 17sp + repeat reveal; Check-in/Calling/Board Left + room free; Sheets Fit/Readable vs print (chits/slips, no `?r=`); Course ops student card health + phone scroll / Teacher list wrap
