# Planning prompt — DIPI Staff as a platform-independent Java desktop client

> **Planning already ran (2026-09-05).** The three deliverables exist:
>
> - spec — `docs/superpowers/specs/2026-09-05-java-desktop-design.md`
> - plan — `docs/superpowers/plans/2026-09-05-java-desktop.md`
> - implement handover — `docs/handovers/2026-09-05-java-desktop-handover.md`
>
> Paste **the handover** into the implement session. Paste everything
> below this rule only if the owner wants a **re-plan**. Do not write
> production code in a planning session.

> Original brief: paste everything below the rule into a fresh session
> whose workspace can see `/Users/wizops/DIPI/dipi-app` (and, read-only,
> `/Users/wizops/DIPI/dipi-web`). It asks for a **spec, a phase-wise plan,
> and an implement-phase handover**. Do not write production code.

---

You are planning a **second build** of the DIPI Staff registrar-desk client:
a **Java** application that runs on Linux, Windows, and macOS without an
Android runtime. The working product today is the Android tablet app at
`/Users/wizops/DIPI/dipi-app`, shipping **`1.42.0` / `versionCode` 69** on
`main`, delivered on a Pixel C. That app stays. This session designs its
desktop twin. It does **not** implement it.

The owner asked for Java specifically so the same binary family can run on
any JVM host. Kotlin Compose Multiplatform, a browser rewrite, and an
Electron/WebView shell are out of scope unless the owner later overrides
that. JavaFX is the recommended UI toolkit to propose; Swing and SWT are
the comparison cases. Do not silently pick one.

**Your job in this session is planning only.** Do not scaffold a project,
do not write production Java, do not edit the Android app, do not bump a
version, do not commit, do not open issues, do not invoke
`executing-plans` or `subagent-driven-development`. The output is three
markdown files. Implementation is a later session that will execute the
handover you write.

Announce at start: you are using the **brainstorming** skill, then
**writing-plans**, and you will stop at the handover.

## Skills you must load before you write

These are already installed on this machine (2026-09-05). Read each
`SKILL.md` and follow it, with the overrides in this prompt winning when
they conflict.

| Skill | Path | Use for |
|---|---|---|
| brainstorming | `~/.claude/skills/brainstorming/SKILL.md` (or `~/.agents/skills/brainstorming`) | approaches, sectioned design, spec self-review |
| writing-plans | `~/.agents/skills/writing-plans/SKILL.md` | phase-wise implementation plan |
| markdown-mermaid-writing | `~/.claude/skills/markdown-mermaid-writing/SKILL.md` | architecture / sequence / phase diagrams in the spec and plan |
| karpathy-guidelines | `~/.claude/skills/karpathy-guidelines/SKILL.md` | no speculative architecture, surface assumptions, verifiable success |
| javafx-architecture-frameworks | `~/.agents/skills/javafx-architecture-frameworks/SKILL.md` | MVVM vs MVP vs MVCI, DI, navigation shell — **if** JavaFX is the chosen approach |
| javafx-project-starter | `~/.agents/skills/javafx-project-starter/SKILL.md` | module/Gradle-or-Maven shape, JDK floor |
| javafx-testing-packaging-distribution | `~/.agents/skills/javafx-testing-packaging-distribution/SKILL.md` | JUnit, TestFX, `jpackage` per OS |
| javafx-desktop-shell-integration | `~/.agents/skills/javafx-desktop-shell-integration/SKILL.md` | file open, print, OS keychain, desktop integration |
| javafx-media-webview-3d | `~/.agents/skills/javafx-media-webview-3d/SKILL.md` | **WebView hardening only** (JS off, no cookie leak). Ignore 3D/media. |

**Overrides (this prompt wins):**

- Brainstorming says "ask one question at a time" and "commit the spec".
  **Ask the blocking questions in one message first** (house style of this
  repo). **Do not commit.** Write files; stop.
- Brainstorming's terminal state is invoking writing-plans, then
  implementation. **Stop after writing-plans and the handover.** Do not
  offer to start coding.
- `writing-plans` bite-size (2–5 minute TDD steps) is for a single
  subsystem. This port is several verticals. Write **one plan file per
  phase** (or one file with clearly separated phase plans), each phase
  independently shippable. Do not produce a 400-step monolith.
- The JavaFX skills have low install counts. Treat them as checklists, not
  gospel. Product rules in `AGENTS.md` beat any skill default.
- Do **not** run `create-github-issues-feature-from-implementation-plan`
  in this session. The handover may say the implement phase may do that
  later.

If a listed skill is missing, install it with
`npx skills add <owner/repo@skill> -g -y` and continue. Do not invent a
skill.

## Read first, in this precedence

1. `AGENTS.md` — Hard rules and Current assumptions. These win over
   everything, including anything below and any JavaFX skill.
2. `docs/handovers/2026-09-05-java-desktop-source-inventory.md` — **the
   verified inventory of the Android app.** Every destination, live HTTP
   path, parser, store, retirement, and Android-only substitute is there.
   Re-verify claims you will copy; you are not starting from an unchecked
   document.
3. `docs/DESIGN.md` — visual authority and the shipped-delta ledger
   through 1.42.0. Read § Course ops, § Sheets v5, § Reach v5 UI, and the
   1.39.0–1.42.0 deltas.
4. `docs/LIVE-DESK.md` — transport, PHP inventory, sheet skeletons.
5. `docs/DECISIONS.md` — owner rulings. Ignore the stale footer version
   line (it still says 1.30.5).
6. `docs/design/DIPI-Staff.dc.html` — open in a browser. 1 px = 1 dp.
7. Course-ops specs: `docs/specs/2026-09-02-course-ops-2a-mode-spec.md`
   through `2d`.
8. `core/network/.../StaffApi.kt`, `core/model/.../SheetExport.kt`,
   `feature/course/.../DeskTiles.kt` — live surface as code.
9. `CLAUDE.md` — current ship line.

`README.md` is stale (headers 1.4.1). Do not plan from it.

`/Users/wizops/DIPI/dipi-web` is **read-only** reference. Backend PHP is
immutable. No `/staff/*` on the live host.

## What you are designing

A Java desktop client that is **behaviourally equivalent** to the shipped
Android desk for a registrar (and, in a later phase, an assistant teacher
in Course ops), running on Linux, Windows, and macOS.

It is a **clean-room port of behaviour**, not a rewrite of product rules
and not a Kotlin library wrapped in a Java window. The Android tree is
the specification. The Java tree will be a sibling (recommended path
`/Users/wizops/DIPI/dipi-desktop` — Q1). Do not add a `:desktop` module
inside the Android Gradle build unless the owner picks that in Q1.

Success means: a staff member who already uses the Pixel C app can sit at
a Linux or Windows machine, sign in to the same live desk, and complete
the same day-0 work without learning a new protocol. Visual fidelity
follows `DIPI-Staff.dc.html` at a default window of **1280×900**. Wider
windows may grow; they must not invent a third information architecture.

## Scope fence

- **In:** planning artifacts only (the three files below).
- **In the future implement phase (describe, do not build):** Java
  desktop client, live Drupal HTML transport, parsers, encrypted local
  stores, design tokens, sheets viewer, print, `jpackage` (or equivalent)
  installers.
- **Out of this session and out of the first Java ship unless the owner
  expands:** new endpoints, new query parameters, `/staff/*` on the live
  host, PHP changes, server-side Advanced Search, real photo upload,
  Group seating, Cell list, Course-ops attendance writes, Bulk Mail,
  Letters, Manage Courses, Daily Activity, SMS Report, Male/Female course
  PDFs, skin photographs, a status engine, client ACL, any `r` on a sheet
  GET, cookie handoff to the system browser, NPI in a database or log.

If a phase in your plan would add a new live URL or a new query name,
you have left the fence — say so and stop that phase rather than widening.

## Ask the blocking questions before you plan around them

Put these to the owner **at the top of this session, in one message,
before you write the spec**. Do not silently pick a default. Plan the
unconditional parts; mark the rest conditional.

1. **(Repo)** New sibling git repo at `/Users/wizops/DIPI/dipi-desktop`,
   or a new top-level Gradle/Maven build inside `dipi-app`? Recommended:
   sibling repo, so the tablet SemVer and Pixel C install path stay
   untouched.
2. **(Toolkit)** JavaFX 21+, Swing, or SWT? Recommended: JavaFX 21+ on
   JDK 21 LTS (WebView for HTML sheets, Print API, `jpackage`, CSS that
   can carry the Steel/Paper/Blossom/Pond/Still tokens). Swing is the
   conservative alternative; SWT ties the app to Eclipse bits.
3. **(Parity target for v1 of the desktop client)** (a) Vertical 1 only
   (login → centre → worklist → card → change-status → settings), then
   stop and ship; (b) Vertical 1 + the six-section desk + sheets; (c)
   full 1.42.0 including Course ops, PIN, hall, 5i print. Recommended:
   (b) as desktop 1.0, Course ops as desktop 1.1. Phone-only chrome
   (Today skeleton, hub overflow) does not need a third desktop IA.
4. **(Window)** Default 1280×900 matching the design file, user-
   resizable with a floor, or free-form desktop layout? Recommended:
   1280×900 default, minimum ~1100×700 so the desk rail still fits,
   tokens unchanged.
5. **(Packaging)** `jpackage` installers per OS (deb/rpm, msi/exe, dmg)
   as a required last phase, or "run from a fat JAR" is enough for the
   first desk trial?
6. **(Secrets)** OS credential stores (libsecret / DPAPI / Keychain) vs
   a Java KeyStore file under the user config dir? Remember-me stores a
   password today; that fact must not get weaker on the desktop.
7. **(Course ops on a shared PC)** Keep the device PIN as the Settings
   gate, drop Course ops from desktop v1, or gate the whole app with the
   OS user account? PIN-as-on-tablet is the conservative copy.
8. **(HTML sheets)** JavaFX WebView with the same five hardening
   constraints (JS off, no cookie injection, in-memory HTML, injected
   stylesheet, no `r`), or native Java renderers per sheet? Recommended:
   WebView for `Page` exports, native Java for Day 0 summary / Course
   report / hall — matching the Android split.

Do not invent a ninth question that reopens a settled owner ruling
(Approved, NPI, bridge rule, `r`, retired tiles).

## Approaches you must present

After the questions (or in parallel for the parts that do not depend on
them), present **2–3 approaches** with trade-offs and a recommendation,
covering at least:

- UI toolkit (Q2).
- Architecture: MVVM (closest to `DeskViewModel`) vs MVCI vs a single
  god-controller. Recommend a `DeskViewModel`-shaped facade so the
  Android inventory maps 1:1.
- HTTP: Java `HttpClient` + a first-party cookie jar vs OkHttp on the
  JVM. Either is fine; the cookie jar must persist the full `SESS` set
  and never leak into the system browser or the sheet WebView.
- Local DB: SQLite (JDBC) + SQLCipher vs a file-backed outbox only.
  Recommend the smallest store that still survives a restart for the
  outbox and one-course cache.
- Test oracle: replay the existing parser fixtures from
  `core/network/src/test` as Java golden tests. Do not re-invent
  `var dataset` shapes.

YAGNI: one window, one process, one live host. No plugin system, no
multi-centre dashboard, no offline-first sync protocol beyond the
existing outbox.

## What the three deliverables must contain

Write these three files and nothing else. Paths are binding.

### 1. Spec — `docs/superpowers/specs/2026-09-05-java-desktop-design.md`

Follow brainstorming's spec self-review (no TBD, no contradictions, no
ambiguous requirements). Include, with Mermaid where a diagram is
clearer than prose:

- Product statement and non-goals.
- Chosen (or branched) answers to Q1–Q8.
- Information architecture: screens mapped 1:1 to `DeskScreen` /
  `DeskSection` / centre tiles. Call out phone-only surfaces that the
  desktop will **not** grow (Today skeleton, hub overflow as a third IA).
- Transport: every live path from the inventory, method + query allowlist.
  A table that fails the spec if a new query name appears.
- Parser contract: which Android parser tests become Java golden tests.
- Persistence and NPI rules, including wipe conditions.
- Design tokens and default window. No new skins.
- Error policy: server text verbatim; snack / dialog mapping.
- Hardening: sheet WebView, cookie isolation, no `r`, no `Approved`.
- Packaging and supported OS list.
- SemVer for the desktop line (start at `0.1.0` or `1.0.0` — pick one
  and say why; do not reuse Android `versionCode` 69).
- Test strategy: JUnit 5 for parsers/transport; UI tests only where
  they pin a hard rule (Approved blocked, sheet GET query names, NPI
  field absence).
- Risks: teacher-list GET mutates the server; WebView cookie leak;
  KeyStore portability; print stack per OS; Pixel C remains the
  Android source of truth so the two clients can drift.

### 2. Phase-wise plan — `docs/superpowers/plans/2026-09-05-java-desktop.md`

Use the `writing-plans` header (Goal, Architecture, Tech Stack, Spec
path, Global Constraints). Then decompose into **independently
shippable phases**. Suggested shape (reorder only with a reason):

| Phase | Working software at the end |
|---|---|
| P0 | Empty Java window, HTTP client, cookie jar, login-form scrape tests, `SheetRouteSafety` equivalent, mock dispatcher. No registrar UI. |
| P1 | Vertical 1: login, centre (matrix + 4 older + 4 tiles), worklist, card, change-status, settings (remember me / erase-all). |
| P2 | Desk shell + six sections + sheets (3×3 Board, hardened viewer, Course report on centre, allocation sync). |
| P3 | Course ops (only if Q3 = c or as 1.1). PIN, teacher list, hall, student card, 5i print. |
| P4 | `jpackage` (or the Q5 answer) for Linux, Windows, macOS + a one-page runbook. |

For each phase:

- **Slices** that compile and test green alone.
- **Exact proposed packages / files** (Java package names, not Android
  paths). Strict file ownership so a later multi-agent implement pass
  can cut the phase into disjoint workers.
- **Tests per slice**, named, with the Android test they oracle from
  when one exists (`SheetRouteSafetyTest`, `RoomAllocSyncTest`,
  `TeacherListParserTest`, `CourseReportCsvParserTest`,
  `ApplicationViewParserTest`, `StatusWriteTest`, …).
- **Hand-verification** the unit suite cannot see (login against the
  live host is owner-gated; never commit credentials).
- **Rollback shape.**
- **Version bump** for that phase.

Do not include Pixel C `adb` steps in the Java plan. The tablet app is
not this build.

### 3. Implement-phase handover — `docs/handovers/2026-09-05-java-desktop-handover.md`

This is the document the next session pastes. Write it for an agent that
has **not** read this planning prompt. It must contain:

- Baseline (Android 1.42.0 + the agreed desktop repo/toolkit/parity).
- Read-first precedence (same list as above, plus the spec and plan).
- Phase order and file-ownership map.
- Never-touch list (copy from below, plus anything you discovered).
- Open questions that remained unanswered, marked blocking.
- Verify commands for the Java build (you define them; they will not be
  `./gradlew :app:testDebugUnitTest`).
- What to check by hand on Linux and on Windows (macOS if Q5 includes it).
- "When you finish" — where to append DESIGN/DECISIONS equivalents in
  the **desktop** repo; do not rewrite Android `AGENTS.md` except to add
  one line pointing at the sibling if the owner wants that later.
- An explicit **do not implement anything that is not in the current
  phase**.

House style to match: `docs/handovers/2026-09-05-ui-gaps-handover.md`.

## Never touch (planning and, later, implementation)

- The Android production tree, except adding the two planning files and
  this handover. No Kotlin edits, no Gradle bumps, no Pixel C install.
- `/Users/wizops/DIPI/dipi-web` and any live PHP.
- New live endpoints or query parameters.
- `r` on any sheet GET.
- Status `Approved`.
- NPI in a DB, DTO-on-disk, or log.
- Cookie transfer to the system browser (Advanced Search / Add
  Application stay URL-only handoffs).
- WebView JavaScript, and cookies inside the sheet viewer.
- Retired surfaces listed in the inventory.
- Bridge-rule family (letters, waitlist, LC, SMS/WhatsApp bodies).
- Mock `/staff/*` as the live default.
- Agent attribution in any commit the implement phase later makes
  (`Co-Authored-By`, Generated-with, session URLs).

## Global constraints to copy into the spec and every phase plan

- Live protocol is the browser desk. Default host
  `https://dipi.vridhamma.org`.
- Wipe cookies before login. Prefer `GET /user/login`. Persist the full
  cookie jar.
- Centre from `dh_user_center`. No hardcoded centre.
- Worklist = `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` +
  `var dataset`. Never `POST /search-app` unless Q-parked search is
  later unblocked.
- Sheet GET query names = `{conf, seating}` only.
- `GET /teacher-list` is once-per-entry, never polled.
- `/application-view` allowlist only; Comments column never parsed.
- Server messages verbatim.
- Design file wins look and measurement; existing Android code wins
  "what the product does".
- Dark = Steel night ramp. Lotus is a vector. No photo hero.

## Execution model to plan for (do not run it)

The implement phase in this organisation runs as a dynamic multi-agent
workflow: parallel scoped workers with strict file ownership and scoped
tests, plus one integrator that runs the full suite, bumps SemVer, and
builds the distributable. Shape each phase so it can be cut that way —
disjoint file sets, a named test command per worker, an explicit
integration step. Mention that in the handover. Do not staff it now.

## Done, for this session

1. The eight questions asked, in one message, before the spec.
2. `docs/superpowers/specs/2026-09-05-java-desktop-design.md` written
   and self-reviewed.
3. `docs/superpowers/plans/2026-09-05-java-desktop.md` written with
   the writing-plans header and per-phase slices.
4. `docs/handovers/2026-09-05-java-desktop-handover.md` written as the
   paste-ready brief for the implement session.
5. A one-paragraph summary in chat naming which parts of the plan are
   blocked on which unanswered question.

No Java sources, no Gradle/Maven scaffold, no Android edits beyond those
three files, no version bump, no commit, no issues. When the answers
come back, update the spec / plan / handover in place and only then
start P0 in a **new** session that is given the handover.
