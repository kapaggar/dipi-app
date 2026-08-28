# Version-3 conformance spec — sync banners, loading skeleton, centre settings

**Status:** proposed, 2026-08-26
**Parent design:** `version-3/project/DIPI Staff.dc.html` (2,004 lines) — the
governing visual contract. This document does not replace it; it fills the
three places where the design leaves behaviour undefined, and records the
token mapping the design hard-coded in steel.

**Audit that produced this:** the shipped app on `main` (1.18.0 / `versionCode`
29) implements all eight items of `version-3/DELTA.md`. What follows is the
residue — three conformance gaps plus one dependency cleanup.

## Why the design needs supplementing

The design canvas is a static prototype drawn in the steel skin. Three things
it shows cannot be read off the markup:

1. It draws a **Retry** control but never says what Retry does.
2. It draws a loading skeleton with data-bound bar widths (`{{ s.w }}`) but
   ships no data.
3. It draws a centre-settings **RESULT** block bound to `{{ optEffect }}` but
   never gives the derivation.

Everything else below is a direct transcription of design values.

---

## S1 — Phone sync banners

### S1.1 The defect being fixed

`app/.../ui/DipiAppUi.kt:131` renders one strip:

```kotlin
if (state.offline || state.queuedCount > 0) {
    Text(text = stringResource(R.string.offline_banner, state.queuedCount), ...)
}
```

against `R.string.offline_banner` =
`"◍ Offline — showing cached list · %1$d changes waiting to sync"`.

Because the condition ORs the two states but the string asserts both, an
**online** device with queued changes tells the registrar it is offline. The
desk rail is unaffected — `deskSyncLine()` already distinguishes the cases.

### S1.2 Required behaviour

Two independent strips, stacked in this order, directly under the TEST MODE
strip and above the screen body:

| `offline` | `queued` | Strips rendered |
|---|---|---|
| false | 0 | none |
| true | 0 | offline only |
| false | > 0 | queued only |
| true | > 0 | offline, then queued |

### S1.3 Offline strip

Transcribed from the design's `3 · Today — offline, queued, test mode strip`:

- Background `neutral200`, bottom hairline `neutral300`
- Padding 16dp horizontal, 7dp vertical
- Text 12sp, colour `neutral700`
- Copy, verbatim: `◍ Offline — showing cached list`

### S1.4 Queued strip

- Background `accent100`, bottom hairline `accent300`
- Padding 16dp horizontal, 9dp vertical
- Count rendered in `DipiMono` at 11sp; remaining text 12.5sp; colour `accent800`
- Retry label pushed to the end: 11.5sp, letter-spacing `.06em`, uppercase,
  colour `accent700`
- Copy: `{n} change waiting to sync` / `{n} changes waiting to sync`
  (Android `plurals`, so the singular is not "1 changes")

### S1.5 Retry semantics — **decision, not in the design**

Tapping Retry runs the *same* outbox flush the app already runs automatically
when connectivity returns (`DeskViewModel.flush()`, today private and called
only from the connectivity collector at `DeskViewModel.kt:278`).

- Retry is shown whenever `queued > 0`, including while offline.
- Retry **always attempts the send**. It performs no client-side reachability
  check and no pre-flight gating — hard rule 1 ("send the request; render the
  server response verbatim") applies to this control like any other.
- Success and failure both surface through the existing `FlushSnack` path, so
  a failed retry while offline shows the transport's own message. No new error
  UI is introduced.
- Retry is idempotent from the caller's side: `flushOutbox()` walks
  `outbox.pending()`, so rows already sent are no longer pending.

---

## S2 — Today loading skeleton

### S2.1 What ships today

`feature/applicants/.../TodayScreen.kt:90` renders a placeholder:

```kotlin
if (loading && rows.isEmpty()) {
    Column { repeat(6) { Text("········", ...) } }
}
```

### S2.2 Required shape

Eight rows (design: `hint-placeholder-count="8"`), each:

- Row padding 16dp horizontal, 14dp vertical; bottom hairline
- Two stacked bars with 8dp gap:
  - **Line 1:** a name bar, height 14dp, corner radius 2dp, fill `neutral300`;
    plus a right-aligned status bar, width 46dp, height 14dp, radius 2dp,
    fill `neutral200`
  - **Line 2:** a meta bar, height 11dp, width 62% of the row, radius 2dp,
    fill `neutral200`

### S2.3 Bar widths — **decision, not in the design**

The design binds `{{ s.w }}` to absent data. Widths are therefore fixed as a
deterministic cycle of the row's available width, chosen to read as a list of
names of natural varying length:

```
0.52, 0.66, 0.44, 0.60, 0.72, 0.48, 0.58, 0.64
```

Row `i` uses index `i % 8`. Deterministic rather than random so screenshot
tests stay stable.

### S2.4 Token mapping

The design hard-codes `#e0e0e3` and `#eaeaed` because the canvas is drawn in
steel only. In the app these must follow the active skin:

| Design hex | App token | Steel value |
|---|---|---|
| `#e0e0e3` | `Industry.neutral300` | `#D4D4D7` |
| `#eaeaed` | `Industry.neutral200` | `#E7E7EA` |

---

## S3 — Centre settings conformance

### S3.1 What ships today

`feature/course/.../CentreOpsScreen.kt` is functional but is the only desk
surface that imports none of the design system — no kicker, no card, plain
`Text` and a `TextButton("Back")`. Its three toggles carry a label only, and
the effect line is one hard-coded sentence that describes the groups-off case
while ignoring laundry and valuables.

### S3.2 Required structure

Per the design's `CENTRE SETTINGS` pane:

- Heading `Centre settings` (Barlow Condensed) with the sub-line, verbatim:
  `Three switches change what check-in asks for. The line at the bottom shows the result.`
- Three toggle rows, each carrying **title**, **note**, and **state** text
- A `RESULT` block below them: a `DeskKicker` reading `RESULT` over the
  derived effect sentence, drawn on a `deskCard`

### S3.3 Toggle copy — **decision, not in the design**

The design supplies `{{ o.title }}` / `{{ o.note }}` / `{{ o.state }}` with no
data. Notes are written to state what the switch actually gates, verified
against `CheckInPane.kt:660-676`:

| Title | Note | State when on / off |
|---|---|---|
| `Laundry` | `Check-in asks whether laundry was issued.` | `ON` / `OFF` |
| `Valuables` | `Check-in asks whether valuables were deposited.` | `ON` / `OFF` |
| `Groups` | `Check-in assigns a sitting group; Zero Day shows group chips.` | `ON` / `OFF` |

### S3.4 RESULT derivation — **decision, not in the design**

A pure function in `:core:model` (mirroring the `deskSyncLine` precedent), so
it is unit-testable without Robolectric:

```kotlin
fun centreOpsEffect(prefs: CentreOpsPrefs): String
```

**Rules.** Room and seating are always asked (`CheckInPane` renders `ROOM` and
`SEATING` unconditionally). The three switches append to that list:

1. Start with `["room", "seating"]`
2. Append `"laundry"` if `prefs.laundry`
3. Append `"valuables"` if `prefs.valuables`
4. Append `"group"` if `prefs.groups`
5. Sentence one: `"Check-in asks for "` + the list joined with `", "` except
   the final pair joined with `" and "` + `"."`
6. Sentence two, only when `!prefs.groups`, separated by a single space:
   `"Everyone sits in Main Dhamma Hall and Zero Day hides group chips."`

**Worked examples** (these are the test vectors):

| laundry | valuables | groups | Output |
|---|---|---|---|
| true | true | true | `Check-in asks for room, seating, laundry, valuables and group.` |
| true | true | false | `Check-in asks for room, seating, laundry and valuables. Everyone sits in Main Dhamma Hall and Zero Day hides group chips.` |
| false | false | false | `Check-in asks for room and seating. Everyone sits in Main Dhamma Hall and Zero Day hides group chips.` |
| false | true | true | `Check-in asks for room, seating, valuables and group.` |

### S3.5 Enabling move

`DeskKicker` currently lives in `:feature:desk` (`DeskShell.kt:245`), but
`CentreOpsScreen` is in `:feature:course`, which depends only on `:core:ui`.
Rather than couple two feature modules, `DeskKicker` moves down into
`:core:ui` alongside `DeskStyle`/`deskCard`, which every consumer already
depends on. Five files in `:feature:desk` update their import; behaviour is
unchanged.

---

## S4 — Dependency hygiene

`androidx.material3.window` (`material3-window-size-class`) is declared at
`app/build.gradle.kts:93` and never imported. The app's adaptive behaviour runs
off `LocalConfiguration.screenWidthDp` (≥600 → tablet list-detail split;
≥1100 → full desk shell), which already satisfies the design's `1c` tablet
frame. The dependency is removed. No behaviour changes.

---

## Out of scope

- **Sign-in photo hero.** `DELTA.md` §6 specifies a 430dp hero using each
  skin's own lotus photograph. The five `skin_photo_*.jpg` were deliberately
  deleted at 1.15.0 ("~158 KB of skin photos removed") and replaced with
  `LoginLotusRelief`. Reinstating them is an owner decision about APK size,
  not a conformance defect.
- **Photo upload.** The client flow is built; `StaffRepository.uploadPhotos`
  returns `"Photo upload is not exposed on the live desk"` off mock because no
  endpoint exists. Blocked by the immutable-backend rule.
- **`feat/desk-gap` (1.19.0).** Six read-only desk surfaces plus the Day-11
  export, written and unmerged. Landing that branch is a separate integration,
  not part of this plan.

## Versioning

`main` is 1.18.0 / 29. `feat/desk-gap` already claims 1.19.0 / 30. Hard rule 11
forbids two installs sharing a `versionName`, so this work ships as
**1.20.0 / versionCode 31**, leaving 1.19.0 / 30 reserved for the desk-gap
merge. If desk-gap is abandoned, retarget this to 1.19.0 / 30 before shipping.

These are user-visible fixes inside the current vertical, so MINOR is correct,
and the registrar will tap them — an install on the Pixel C is required.
