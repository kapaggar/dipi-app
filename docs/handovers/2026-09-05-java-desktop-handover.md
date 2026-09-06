# Handover — implement DIPI Staff as a Java desktop client

For the implementing agent. **This session writes Java in a new sibling
repo. It does not re-plan and it does not edit Android production
Kotlin.** Execute **one phase** from the plan. Stop. Do not start the
next phase unless the owner says so.

**Android baseline:** `1.42.0` / `versionCode` 69 on `main` at
`/Users/wizops/DIPI/dipi-app`. Device of record for the existing product
is the Pixel C. You will not install anything on it.

**Desktop baseline (planning recommendation):** sibling repo
`/Users/wizops/DIPI/dipi-desktop`, JavaFX 21 on JDK 21, package
`org.dhamma.dipi.desk`, first registrar-usable ship = **1.0.0** after
P2 (Vertical 1 + six-section desk + sheets). Course ops is P3 / 1.1.0.

If `/Users/wizops/DIPI/dipi-desktop` does not exist, **P0 creates it**.
Do not add a `:desktop` module inside `dipi-app`.

---

## Read first, in this precedence

1. `/Users/wizops/DIPI/dipi-app/AGENTS.md` — Hard rules and Current
   assumptions. These win over everything below, including JavaFX skills.
2. `docs/handovers/2026-09-05-java-desktop-source-inventory.md` — what
   the tablet already does. Re-verify a claim before you copy it.
3. `docs/superpowers/specs/2026-09-05-java-desktop-design.md` — the
   design you are implementing.
4. `docs/superpowers/plans/2026-09-05-java-desktop.md` — phases, slices,
   file ownership, named tests.
5. `docs/DESIGN.md` — tokens and shipped-delta through 1.42.0.
6. `docs/LIVE-DESK.md` — transport and sheet skeletons.
7. `docs/DECISIONS.md` — owner rulings. Ignore the stale footer version
   and the stale `OLDER_COURSE_LIMIT = 3` line; the tree is **4**.
8. `docs/design/DIPI-Staff.dc.html` — open in a browser. 1 px = 1 dp.
9. Course-ops specs under `docs/specs/2026-09-02-course-ops-2*.md` —
   **only if you are on P3**.
10. `core/network/.../StaffApi.kt`, `core/model/.../SheetExport.kt`,
    `feature/course/.../DeskTiles.kt` — live surface as code.

`README.md` in the Android repo still headers 1.4.1. Do not plan or
build from it.

`/Users/wizops/DIPI/dipi-web` is **read-only**. Backend PHP is immutable.

---

## What you are building

A Java desktop twin of the registrar desk: same Drupal HTML protocol,
same hard rules, same tokens, same destinations a staff member uses at
1280×900 on the tablet. Clean-room Java — do not wrap Kotlin, do not
embed the Android APK, do not invent endpoints.

Start at **P0** unless the owner names a later phase and P0–P(n-1)
already exist and are green.

---

## Phase order and file ownership

| Phase | Version | Own these modules | Do not touch |
|---|---|---|---|
| P0 Foundation | `0.1.0` | scaffold all four; model hard-rule types; net HTTP+cookies+login scrape+`SheetRequest`; store wipe stubs; app empty window | registrar screens, `javafx.web`, live login in CI |
| P1 Vertical 1 | `0.2.0` | parsers (centre, worklist), AuthService, Login/Centre/Worklist/Card/Settings views, DesktopHandoff | desk rail, sheets WebView, Course ops |
| P2 Desk + sheets | `1.0.0` | DeskShell, SheetTransport, SheetViewerPane, Board 3×3, five sections, allocation POST, Course report, 12-up/2-up print | Course ops PIN/hall/teacher GET, `jpackage` |
| P3 Course ops | `1.1.0` | CourseOpsStore, PinStore, TeacherList, Hall, Student card, 5i print | Android tree, new URLs |
| P4 Installers | `1.2.0` | `jpackage` + `docs/RUNBOOK.md` | product behaviour |

**Strict file ownership inside a phase:** one worker per module (`model`,
`net`, `store`, `app`) unless the plan slice names a narrower path
(`app` desk chrome vs `app` board). If two slices want the same file,
they are sequential, not parallel.

**Integrator only:** full suite, SemVer bump, desktop DESIGN/DECISIONS
delta. The integrator does not rewrite Android `AGENTS.md` except one
optional pointer line after P0, and only if the owner asks.

Dynamic multi-agent workflow is in scope for **implementation**, not for
this document's author. Cut the current phase into disjoint workers;
do not staff P2 while P0 is unfinished.

---

## Open questions

Planning locked recommended defaults so P0 is unblocked. Confirm or
override before the phase that depends on them.

| # | Default | Blocks |
|---|---|---|
| Q1 Repo = sibling `dipi-desktop` | yes | P0 path |
| Q2 JavaFX 21 + JDK 21 | yes | P0 toolchain |
| Q3 Parity = (b) desk+sheets as 1.0, Course ops as 1.1 | yes | whether P3 is scheduled |
| Q4 1280×900, min 1100×700 | yes | P1 centre layout |
| Q5 `jpackage` required as P4 | yes | P4, not P0 |
| Q6 OS credential store | yes | P1 remember-me; KeyStore file is the documented fallback |
| Q7 PIN-as-on-tablet for Course ops | yes | P3 only |
| Q8 WebView for `Page` exports, native for summary/report/hall | yes | P2 viewer |

Do not invent a ninth question that reopens `Approved`, NPI, the bridge
rule, `r`, or retired tiles.

---

## Never touch

- Android production Kotlin, Gradle versions, or Pixel C install.
- `/Users/wizops/DIPI/dipi-web` and any live PHP.
- New live endpoints or query parameter names.
- `r` on any sheet GET.
- Status `Approved`.
- NPI in a DB, DTO-on-disk, or log (`aadhar`, `passport`, `voterid`,
  `pancard`, `ae_*`).
- Cookie transfer to the system browser (Advanced Search / Add
  Application stay URL-only).
- WebView JavaScript, and cookies inside the sheet viewer.
- Retired surfaces: Manage Courses, Daily Activity, SMS Report, Letters,
  Bulk Mail, Male/Female course PDFs, skin photographs, photo upload as
  live, Group seating, Cell list.
- Bridge-rule family (letter bodies, waitlist, LC, SMS/WhatsApp).
- Mock `/staff/*` as the live default.
- `POST /search-app` (parked).
- Polling `GET /teacher-list`.
- Parsing the Comments column or non-allowlist `/application-view`
  sections.
- FXML for product screens, tray icons, 3D, media, auto-updater.
- Agent attribution in commits (`Co-Authored-By`, Generated-with,
  session URLs).

---

## Skills (already installed on this machine)

Read the `SKILL.md` if you need the checklist. Product rules win.

| Skill | Use |
|---|---|
| `javafx-project-starter` | P0 window only — then stop following its sample package names |
| `javafx-architecture-frameworks` | MVVM facade, not Afterburner unless you must |
| `javafx-ui-layout-navigation` | single-window shell |
| `javafx-fxml-controls-css` | **CSS tokens only**; skip FXML |
| `javafx-forms-validation-preferences` | login + settings |
| `javafx-concurrency-services` | HTTP off the FX thread |
| `javafx-properties-bindings` | facade → view |
| `javafx-media-webview-3d` | WebView hardening only; ignore media/3D |
| `javafx-testing-packaging-distribution` | TestFX + P4 `jpackage` |
| `javafx-desktop-shell-integration` | **do not** add docking/tray |
| `karpathy-guidelines` | no speculative architecture |
| `writing-plans` / `executing-plans` | only after this handover is the brief |

---

## Verify commands

In `dipi-desktop` (you define the modules in P0; names are binding):

```bash
./gradlew :model:test :net:test :store:test :app:test
./gradlew :app:run
```

Do **not** run the Android suite unless you are reading a failing oracle
test. If you do:

```bash
# Android repo only — never from the desktop build
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

Never `./gradlew test` at the Android root.

Live login is owner-gated. Never commit credentials.

### What to check by hand (Linux and Windows; macOS if you package it)

**P1**

- Login error strip is the server's text.
- Centre is `dh_user_center`, four older courses, four tiles, zeros are `·`.
- Erase all clears cookies and remember-me.
- Advanced Search / Add Application open the OS browser without the app
  session.

**P2**

- Rail has six sections; Board is 3×3; no Valuable cell; no course PDFs.
- Day 0 list: JS off, no network cookies on the WebView.
- Course report empty range shows guidance, not a ghost row.
- Logout deletes sheet cache files.
- Allocation refusal renders `msg` verbatim.

**P3**

- One teacher-list GET per entry.
- Hall print does not call `/seating`.
- PIN survives logout and dies on erase-all.

**P4**

- Installed app launches without a system JDK.
- WebView still opens a Day 0 list after `jpackage`.

---

## When you finish a phase

In **`dipi-desktop`**:

- Append the delta to `docs/DESIGN.md`.
- Record owner overrides of Q1–Q8 in `docs/DECISIONS.md` with the date.
- Keep a short `AGENTS.md` that copies the hard rules and the verify
  command.

In **`dipi-app`** (Android):

- Do not bump `1.42.0`.
- Do not rewrite DESIGN/DECISIONS except if the owner asked for a
  one-line pointer to the sibling.

Commits carry no agent attribution.

---

## Do not implement anything that is not in the current phase

If the current phase is P0, you may not draw a centre matrix. If it is
P2, you may not fetch `/teacher-list`. If a slice would add a live URL
or a new query name, stop — you have left the fence.

Paste this file into the implement session. Do not paste the planning
prompt again unless the owner wants a re-plan.
