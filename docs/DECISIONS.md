# Decision ledger

Distilled record of the dated plans and specs (2026-08-13 → 2026-08-31) that shipped
versions 1.0.0 → 1.30.5. The full documents live in git history under `docs/plans/`
and `docs/specs/`. Ruling labels (C1–C10, R1–R9, S1–S4, T1–T8, the six T7 P-items)
are preserved because code comments cite them.

## Transport & protocol

- **08-13 · Native staff client on Drupal 7 `dh_manageapp` only.** Not WebView, not
  student-apply, not the APP `/api` (`dipi_api` has a `get-app-detail` IDOR — C9:
  never design against it). Backend PHP is immutable; no `/staff/*` on the live host.
- **08-13 · P0 validation (C1–C10) PASSED.** C1: no registrar REST exists —
  `/search-app/{cid}` is an HTML page. C2: status write is `GET /change-status/{app_id}`
  with `s`,`l`,`c` returning `{status, msg, confno, newstatus}` — the app uses this
  raw route (revised choice), always `l=0`. The proposed `/staff/*` façade API was
  abandoned: the live desk is Drupal HTML, not Services login. Do not assume
  `POST /api/user/login`, `GET /staff/session`, `POST /search-app`, or a hardcoded
  Dhamma Giri centre. Default host is live `https://dipi.vridhamma.org`.
- **08-13 · Auth = real staff Drupal accounts + `dh_user_center`** (C8); never APP
  API keys. Tenancy is server-side. Centres type a base URL — no baked hostname.
- **08-13 · C5/C6: `/app-update-attended` is not a boolean write** — with `a!=false`
  and `today <= c_start` it requires room section `s` and room `r`. V1 therefore
  shipped no attendance write; resolved by the 08-16 allocation-sync amendment below.
- **08-31 · NEVER send `r` on sheet GETs.** Presence of the `r` param triggers
  server-side bulk seat auto-allocation. Global constraint on every export/report
  fetch (`SheetTransport`, clarification PDF, Day-11, Course report).
- **08-27 · The desk-module list and dashboard matrix are scraped from
  `GET /centre/{cid}`** (`dh_manage_centre()`); links are server-gated by
  `drupal_valid_path()` — a missing link means no permission, render what is present.

## Product rules & privacy

- **08-13 · No client-side access control, ever.** Send the request; render the
  server response verbatim (including refusals like "Please Edit application and
  choose Area teacher before approving!"). No client status engine, no transitions
  invented in Kotlin.
- **08-13 · Never offer or send `Approved`.** C4: `s=Approved` / `s=R-ATReview` is an
  LC path. 08-31 T3 hardened this: `confirmStatus` / `changeStatusFor` /
  `StaffRepository.changeStatus` no-op with an error snack on `Approved`
  (ignore-case), `StatusWrite.query` refuses it so it cannot reach GET params, and `flushOutbox`
  fails any queued `Approved` row from an older install without a request.
- **08-13 · NPI is never persisted or logged**: `aadhar`, `pancard`, `passport`,
  `voterid`, `ae_*`. List reads whitelist public `a_*` fields (C7 — never copy the
  `search.inc` SELECT wholesale).
- **2026-08-16 · NPI display amendment (owner decision).** ID documents
  (Aadhaar/PAN/Voter ID/Passport) and health disclosures MAY be displayed on-screen
  for desk-side verification, but must never be persisted (no Room/DataStore/DTO
  fields) or logged — in-memory session map only (`SensitiveInfo`, wiped on
  course change / logout / erase-all).
- **2026-08-16 · Allocation-sync amendment (owner decision).** Room-allocation sync
  via the desk's existing update form (`POST /app-update-attended/{id}` with the
  dialog's own fields), bulk and user-initiated, IS allowed — the client still never
  sends a status, never `Approved`, never NPI; backend PHP stays immutable.
  `RoomAllocSyncTest.paramsNeverCarryAStatus` pins this; `l`/`v` are posted empty.
- **2026-09-02 · Course-ops persistence amendment (owner decision), verbatim:**
  Course-ops application data (roll + `/application-view` answers, health
  included) MAY be persisted device-local so the hall reads offline across
  restarts. Encrypted at rest in the course-ops store's own
  EncryptedSharedPreferences file; keyed to the resolved running course id;
  wiped when the resolved course changes, on Erase-all, and on logout of the
  account. Never logged (redacting `toString()` on every model); never in Room;
  never in plain DataStore. This amendment applies ONLY to the course-ops store
  — the desk-mode `SensitiveInfo` rules are unchanged.
- **08-13 · Letters are a black box.** `_change_status` may send one; the sheet shows
  "The server may send the applicant a letter for this change." No body preview,
  no letter admin UI.
- **08-13 · Conf-number contract (C3):** `{N|O|S}{M|F}{seq}` minted server-side by
  `generate_conf_no`; Expected→Confirmed does not remint. `nf/of/nm/om/sm/sf`
  prefixes drive the gender + new/old filters (`ConfPrefix`) throughout the desk.

## Centre dashboard

- **08-27 · `CourseMatrix` is additive, not a replacement.** `CourseSummary` and
  `courseCountsLine()` stay (CourseHub uses them). The matrix carries the desk's
  10-cell row contract from `course_summary()`: label, NM, OM, male total, SM,
  spacer, NF, OF, female total, SF. Derived totals are ignored and recomputed.
  The status set comes from `dh_type_detail` and **must never be hardcoded** —
  parse whatever rows are present, in page order.
- **08-27 · Zero renders as `·` (middot), never `0`** — matching the desk's blank
  cells.
- **08-30 · The 60% ceiling correction (gate-review).** `weight(0.6f, fill=false)`
  beside a `1f` sibling is a **37.5%** share (0.6/1.6), not 60%. Shipped fix:
  `BoxWithConstraints` measures `belowHeader`, upcoming is capped
  `heightIn(max = belowHeader * 0.6f)`, lower pane takes `weight(1f)`. Upcoming has
  **no scroll** — safe only because the desk serves at most 4 upcoming courses
  (`upcoming_courses()` `limit 4`) and cards are fixed-height.
- **08-30 · Fixed-height course cards**: exactly four rows, always —
  `Received`, `Confirmed + Expected` (summed via `MatrixRow.plus`), `Cancelled`,
  `Total` (with `+N sevak` suffix). Absent statuses render as empty `·` rows.
  `CourseMatrix.cardRows` carries this; `highlights`/`HIGHLIGHT_LABELS` are kept.
- **08-27 · `OLDER_COURSE_LIMIT = 3`**, applied in `StaffRepository` (both mock and
  live branches), not in the parser — the parser stays a faithful page reader.
- **08-27 · Desk tiles recede**: transparent fill, hairline border, 0 elevation,
  foreground label. `DeskTileAction` enum (CentreOps, AppSettings, AdvancedSearch)
  replaces fragile title-string dispatch; native tiles lead. The native Centre
  Settings tile **replaced** the `centre/{cid}/edit` web-form link.
- **08-30 · Older courses render on the same two-column grid as upcoming** (equal
  widths); the lower pane stacks older-above-desk instead of a side-by-side split.
- **08-27 desk polish S1 · Dropped strings**: the "· from your account ·" clause in
  the centre header and the older-courses sub-line.

## Design system & v4 conformance

- **Colours only via `Industry`/`LocalDipi` tokens** — five user-selectable skins;
  no inline hex in screens. Fixed-for-all-skins hexes live in the theme file.
- **08-28 R6 · Fixed severity pair** on the `hard` token: `#A33A34` light /
  `#E0796F` dark. `statusColors()` untouched. Dark mode = the Steel-night ramp
  (`bg #14171A`, `fg #E4E6E9`, `tint #1D2D3D`, muted `#9BA1A8`).
- **08-28 R9 · Design authority** (1 px = 1 dp, font px = sp): the design file wins
  on look and measurement, existing code wins on architecture. Today that is
  `docs/design/DIPI-Staff.dc.html` + `docs/DESIGN.md`; the earlier `version-2/…4/`
  bundles were consolidated there on 2026-09-02 and live in git history.
- **Touch targets ≥ 48 dp** on every new control.
- **08-26 S1 · Sync banners are two independent strips** (offline / queued) — an
  online device with queued rows must never claim offline; four-row truth table
  pinned in `SyncBannersTest`. **S1.5:** Retry runs the same outbox flush, always
  attempts the send, no client reachability gate. 08-28 R7 added
  `lastSyncAttemptAt` ("last try HH:MM", not persisted).
- **08-26 S2 · Today skeleton**: eight rows with a deterministic width cycle
  `0.52 0.66 0.44 0.60 0.72 0.48 0.58 0.64` so screenshot tests stay stable.
- **08-26 S3.4 · `centreOpsEffect(prefs)`** derives the centre-settings RESULT
  sentence in `:core:model` (room + seating unconditional; laundry/valuables/groups
  append; groups-off adds the Main Dhamma Hall sentence). S3.5 moved `DeskKicker`
  into `:core:ui`. S4 dropped the unused `material3-window-size-class` — adaptive
  runs off `screenWidthDp` (≥600 tablet list-detail, ≥1100 full desk shell).
- **08-28 R8 · Login IME behaviour via `WindowInsets.isImeVisible`** swapping tall
  and compact card arrangements; no new screen. `LoginLotusRelief` stays — the skin
  photos were deleted at 1.15.0 and the photo hero is on the do-not-re-propose list.
- **08-31 T7 · Six measured drifts fixed**: top-bar tracking `0.2.sp`, rail 190 dp,
  check-in sidebar 296 dp, Board kicker split (`SHEETS & EXPORTS` + muted
  `RARELY URGENT`), queued-strip pad 24 with last-try on the count's row, radius
  ramp 8/6/5. Its NOT-list stands: top-bar pad 20, clock 13 sp, lotus 54, Board
  100/58/40 densification, strip heights 38/56, `cardRows` shape, skin photos.

## Desk (Board / Check-in / Rooms / Applications)

- **08-27 S3 · Chrome de-duplication**: `crumbLine` drops the centre-name label
  (joins `dates · dayChip` only) at 17 sp; the Board's 40 sp centre-name heading
  was deleted.
- **08-28 R1 · The check-in scan bug was ViewModel lifetime, not DataStore.**
  `deskScan` is plain `DeskUiState`; `pickCourse` resets it to `""` (and nothing
  else — gender/filter persistence is deliberate). The "restored-query strip" is
  never built.
- **08-28 R3 · Applications/Rooms stay out of the Board's NEXT rows** — "counts are
  inventory, not a queue".
- **08-28/08-31 · Board = 12 exports on three shelves** (`ROLL SHEETS` /
  `DESK SLIPS` / `FOR THE TEAM`, 3×4) **plus Day-11 as a full-width 40 dp
  fourth-line row** (T1), never a 13th shelf cell. The chip fires
  `onExport("Course summary report")` → `SheetExport.Day11Report` →
  `GET /report-day11/{cid}/{courseId}`. The dashed `GAP — NOT IN 1.22.0` badge is
  never drawn. R2's original withhold went stale when `0ce3342` cherry-picked the
  transport at 1.27.0; the chip shipped at 1.28.0.
- **08-30 room-layout-reach S4 · Bounded-container class of bug**: the check-in
  room-picker dialog body gets `verticalScroll` (registrars could not allocate past
  room ~27 of 73); header and CANCEL/CHECK IN stay fixed outside the scroll. Third
  instance of the class — a reaches-the-last-room test pins it.
- **08-31 T4 · Course Report tile fetches the CSV**: `DeskTileSpec.sheet: String?`,
  `DeskViewModel.openCourseReport()` resolves `SheetExport.CourseReport` with
  `centre.id` and `course?.id ?: 0` (centre-scoped — works with no open course).
  Bulk Mail stays a desk-site chip on `onLater`.
- **08-31 T6 · Applicant desk history ported from `feat/desk-gap` via `git show`
  only** (branch never merged): `ApplicantDeskHistory` (do not reuse the name
  `ApplicantHistory`), `HtmlForms`/`HtmlTables`, three GETs + clarification PDF
  (`GET /show-clarification/{appId}/{clarId}` into `cacheDir/sheets`, no `r`),
  rendered on phone `CardScreen` and tablet `ApplicationsPane`.

## Room layout & sync

- **08-27 S4 · Columns are the editable dimension; rows follow** (`ceil(N/C)`).
  `RoomLayout` keyed `gender|section` (`Mbk`, `Fbk`, `Guest`), `DEFAULT_COLUMNS=4`,
  `MIN=1`, `MAX=12`. Device-local display preference inside serialized
  `CentreOpsPrefs` — no server field, wiped by erase-all.
- **08-30 S1 · Stage, preview, then commit**: stepper taps reflow local state only;
  persistence happens on **SAVE ROOM LAYOUT** (disabled when clean; leaving
  discards). S2/S3: `RoomsPane` groups per gender+section, stacked full-width, and
  honours `layout.columnsFor(gender, section)` from the same DataStore source.
- **Check-in/Rooms merge web-assigned rooms** parsed from `#table-attending` on
  `GET /zero-day/{cid}/{courseId}`; centre room config is read-only from
  `GET /centre/{cid}/acco-handler`.
- **08-31 T5 · One check-in state, not two**: `ZeroDayDraft` deleted; phone Zero Day
  reads/writes the same `Map<ApplicantId, CheckInRecord>` through per-card mirrors of
  the tablet functions (`setZeroDaySeat`, `toggleZeroDayLaundry/Valuables`, `pickRoom`,
  `markAttended` with the same `deskSaveSnack` no-room guard). Laundry/valuables are
  double-fire-safe toggles, still not posted as free text. A successful phone mark
  enters `RoomAllocSync.pending`.

## Status vocabulary

- **08-31 T3 · `ApplicantStatus.deriveStatuses(select, roster)`**: prefer the
  non-empty parsed `#edit-app-status` worklist select, else roster keys, then
  `mergeChoices` (which strips `Approved`); `SHEET_CHOICES` is the empty fallback.
  No status engine — vocabulary is data, transitions are the server's.

## Phone flows

- **08-31 T2 · Phone Card gets ID + health** via `state.sensitiveById[card.id]`,
  as new phone-native `LocalDipi` blocks (dark-aware) — the tablet
  `IdVerificationBlock`/`HealthPanel` `Industry.accent100` blocks are deliberately
  not reused. `openCard` fires the health snack once per **new** card id, matching
  `selectDeskApp`. Nothing persisted or logged.
- **Phone hub overflow reaches every export** through `hubSheetLabel`, including
  Day-11; its keys are never widened to Page exports.

## Dead code & retirements — do not re-propose

- **2026-08-30 (owner instruction) · Retired from the app**: **Manage Courses**,
  **Daily Activity**, **SMS Report** — still reachable on the desk site; recorded in
  the do-not-re-propose list, now the Shipped delta ledger inside `docs/DESIGN.md`
  (all earlier SHIPPED-DELTA files are consolidated there).
- **08-31 T8 · Also retired**: **Letters** (added to the 08-30 retired table) and
  **CentreEditScreen** (the native `CentreOpsScreen` is the centre-settings
  surface). Swept when unused: `DeskSectionPlaceholder`, `ComingScreen`,
  `ZeroDayDraft` leftovers, stale R2 / "13th chip" comments.
- **`feat/desk-gap` served as a read-only donor via `git show`**; its
  `DeskSearchFields` / `POST /search-app`, retired screens and `docs/qa-1.19.0/` were
  explicitly not ported. On 2026-09-01 the branch was close-out merged (`-s ours`,
  `63848e1`) and deleted — its history stays reachable from `main`, its content never
  re-entered the tree.
- **Skin photos** (~158 KB) deleted at 1.15.0; the sign-in photo hero is an owner
  decision about APK size, not a conformance defect.
- `docs/LIVE-DESK.md` (the merged HAR + page inventory + server memory map)
  documents the live server, which still serves retired routes — it stays accurate.

- **2026-09-02 (r2, owner feedback with screenshots):** hall orientation follows
  the live web page, not frame 2c — teacher at the bottom, letters as columns,
  numbers ascending away from the dhamma seat. `CELL / PAGODA` renamed
  **CHOWKY / CHAIR** (pagoda cells = separate building, future feature).
  Unseated sevaks hidden from the list (cushion seating), tally unchanged.
  66dp cells, two-line ellipsized names. Portrait (<1000dp) stacks the rail
  below the grid.
- **2026-09-02 · Course ops buffers its own data (owner directive):** on entry
  the app fetches the locked course's worklist itself (the id mapping starves
  without it), then pulls every allocated application at ≤4 concurrent into
  the encrypted course store, with a visible attempt-level progress strip;
  misses snack and retry on the next entry. This adds the pre-existing
  worklist GET to course ops' allowed reads (owner-directed amendment of the
  design's two-GET wording).
- **2026-09-02 · `( View )` / `( PDF )` link remnants** are stripped from names
  at parse time (`LINK_REMNANT`, v5 T5) — every worklist-fed surface at once.

## Parked / decision-gated items

1. **Server-side Advanced Search (`POST /search-app`)** — gated on HAR
   re-verification of the live desk plus explicit owner sign-off. Never assume it.
2. **Real photo upload** — no live desk route exists; exposing one is a product /
   owner decision (backend immutable). Client flow exists mock-only.
3. **Centre-screen page-metric drift** (24 dp padding / 23 sp header / 12 dp grid
   gap vs frame 1a) — needs an owner on-device look before any change; `cardRows`
   itself is an accepted owner decision, not drift.
4. **Pre-existing `observeJob` leak on `openSearchResult`** — a stale collector can
   write the previous course's rows; recommended as its own follow-up (session
   finding, never specced).

## Post-spec fixes (2026-09-01, no spec — session decisions)

- **1.30.4 · `statusChoices` never wiped**: `ensureWorklist` routes `loadStatuses()`
  through `mergeChoices`, so an offline cold start keeps `SHEET_CHOICES` and the
  mock path stays Approved-stripped.
- **1.30.5 · History sections close again**: `ApplicantDeskHistory.expanded` is
  open/closed state, separate from "fetched". Tap = toggle (`toggled` /
  `tapNeedsFetch` in `:core:model`); closing keeps cached rows, reopening costs no
  second request; only a never-loaded or failed section fetches.

## Course ops — assistant-teacher mode (2026-09-02)

- **Owner: device PIN exit gate.** No PIN existed in the app (the design assumed
  one); a 4-digit device PIN is set when course ops is enabled — salted SHA-256
  in the store's own EncryptedSharedPreferences file `dipi_course_ops`,
  constant-time check, survives logout, wiped by Erase-all. In course ops the
  PIN gates the Settings door itself, which also covers Logout/Erase — logout
  clears the mode key, so an ungated logout would bypass the gate. Raw digits
  never persisted (not even saved-instance state) and never logged.
- **Owner: hall grid is registrar-configured device-locally** (per gender,
  rows × seats-per-row, clamps 1..26/1..20) in Centre Settings ("Hall chart",
  stage-then-SAVE like the room chart; placed below the RESULT card — beside
  the Room chart broke the phone fold, ruling ratified at gate review). The
  server's `cs_seat_config` INI is exposed on no readable endpoint. Seat labels
  place students; data beats config — labels beyond the grid EXTEND it (capped
  at 2× so a garbage label lands visibly in UNSEATED, never dropped, and never
  stretches the plan); duplicate labels: first keeps the seat, later ones land
  in UNSEATED. Empty CW-/CH- slots are unknowable client-side and are not drawn.
- **Owner: course ops is read-only** — no attendance marking (would be the
  mode's first write; revisit as its own project), no notes, no edit, no export.
- **One roll fetch per entry, never polled**: `GET /teacher-list/{cid}/{courseId}`
  runs a server-side DELETE (`zeroize_new_course_data`) on every request. Seat
  order derives client-side from labels; `?seating=1` exists but is unused.
- **The teacher-list Comments column is never parsed or stored** — it is an
  unlabelled concatenation of health text. Flags derive from `/application-view`
  fields only: `HLTH MED INTOX TECH PREG MONK` in that order; empty and `-`
  un-flag; PREG is gender-gated; Pregnancy renders `N/A` for male applicants.
- **Applicant-id mapping ruling**: teacher-list markup carries no ids
  (`zero-day.inc:1040-1048`); the zero-day merge's seat field is a seating-aid
  type, not a hall label, so the seat join is impossible. Rows map by band
  gender + unique normalized worklist name, disambiguated by room then age;
  residual ambiguity → no id, the row stays, tap answers "Not on the worklist
  yet". Mapping uses only already-cached desk data — the two-GET rule holds.
- **`/application-view` parsing is allowlist-structural**: only the header,
  `Personal` (its eight labels allowlisted individually), `Course History`
  (server tile order `10-Day Teen STP Special TSC 20-Day 30-Day 45-Day 60-Day
  Service`) and `Health` (labels verbatim) are ever read. Identification,
  Emergency, Contact, Background, Languages, Other, Children/Teen and Long
  Course sections never reach a row regex; the NPI sweep test plants decoys in
  every skipped section and enumerates model fields reflectively with a count
  pin so growth forces re-verification.
- **The mode is byte-identical off**: DESK-mode behavior is pinned by
  `centreStillStartsInDesk`; in course ops the desk rail, queued strip and
  every desk destination never compose.

## Version milestones (from the documents' own headers)

1.18.0/29 pre-v3-audit baseline · 1.20.0/31 v3 conformance · 1.21.0/33 centre
dashboard · 1.22.0/35 desk polish · 1.23.0/36 v4 design pass · 1.24.0/37 centre
trim · 1.25.0/40 room layout + reach · 1.27.0/42 Day-11 transport ·
1.28.0/43 T1+T3 · 1.29.0/44 T2 · 1.29.1/45 T4+T7 · 1.30.0/46 T5 · 1.30.1/47 T6+T8
· now shipping 1.30.5/51.
