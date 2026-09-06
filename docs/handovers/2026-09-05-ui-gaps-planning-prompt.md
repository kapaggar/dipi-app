# Planning prompt — four UI gaps over shipped code (paste into a fresh session)

> Paste everything below the rule into a fresh session at
> `/Users/wizops/DIPI/dipi-app`. It asks for a **plan**, not code.

---

You are picking up the DIPI Staff Android app (`org.dhamma.dipi.staff`) at
`/Users/wizops/DIPI/dipi-app`. Baseline: **`1.38.0` / `versionCode` 63** on
`main`, clean tree, full suite green.

**Your job in this session is to produce an implementation plan. Do not write
production code, do not edit a test, do not bump a version.** The output is one
plan file plus the three owner questions asked out loud. Implementation is a
separate session that will execute your plan.

## Read first, in this precedence

1. `AGENTS.md` — Hard rules and Current assumptions. These win over everything,
   including anything below.
2. `docs/DESIGN.md` — visual authority and the shipped-delta ledger. Read
   § "Sheets v5" and § "Reach v5 UI".
3. `docs/LIVE-DESK.md` Part 4 — sheet routes and markup skeletons.
4. `docs/DECISIONS.md` — owner rulings already settled.
5. `docs/handovers/2026-09-05-ui-gaps-handover.md` — **the source of truth for
   this pass.** It contains the four milestones, the file-ownership map, the
   never-touch list, the blocking questions and the verify recipe. Read it in
   full before planning anything.
6. `CLAUDE.md` — project shape and the test-command rule.

Every claim in the handover was verified against the tree on 2026-09-05
(`DESK_ACTION_PLACEHOLDER`, `hubSheetLabel`, the `SheetViewerPane` mount at
`DipiAppUi.kt:658`, `RollRow.backrest`, `SeatKind` vs the `SeatGrid`
prefix filters, the `Bulk Mail` tile at `DeskTiles.kt:55`). Re-verify anything
you intend to change, but you are not starting from an unchecked document.

## The four milestones

| # | Gap | Version | Nature |
|---|---|---|---|
| M1 | Five hub tiles (Day 0 List, Seating Plan, Student Chit, Checking Slip, Teachers List) fetch sheets the phone cannot draw — `SheetViewerPane` is mounted only inside the ≥1100dp `DeskHost` | `1.39.0` / 64 | routing, maybe responsive redesign |
| M2 | `backrest` is parsed, persisted, aggregated, and never shown per student | `1.40.0` / 65 | the one real feature hole |
| M3 | `SeatKind` is a second implementation of the CW/CH/floor rule that `SeatGrid` re-derives from the seat string | `1.40.1` / 66 | internal hygiene, no user-visible change |
| M4 | `Bulk Mail` tile is a label with no transport behind it | none yet | **decision-gated — do not design or build it** |

Order matters: M1 → M2 → M3. Each must be independently shippable.

## Scope fence

No new endpoint, no new query parameter, no new write protocol, no backend
change. `/Users/wizops/DIPI/dipi-web` is read-only reference; the PHP is
immutable. Every milestone is UI over transport and parsers that already ship
and already have tests. If your plan adds a `@GET` to `StaffApi.kt`, it has left
the scope — say so and stop rather than widening.

The handover's **Never touch** section is binding: the `conf`/`seating` sort
allowlist, the absolute ban on `r` on any sheet GET, no `Approved`, no NPI
persistence or logging, WebView hardening does not relax because the screen got
smaller, server messages verbatim, retired surfaces stay retired.

## Ask the three blocking questions before you plan around them

The handover's § Open questions are genuinely blocking and two of them decide
how big a milestone is. Put them to the owner **at the top of this session, in
one message, before you write the plan**:

1. **(M1)** Does the phone get the full v5 sheet chrome — (a) same chrome with
   horizontal table scroll, (b) reduced chrome (print + close only), or (c) a
   phone-specific default column set per sheet? This decides whether M1 is a
   mount-point move or a responsive redesign, and therefore one slice or three.
2. **(M2)** Is there a backrest treatment in `docs/DESIGN.md` /
   `DIPI-Staff.dc.html`? If not, the owner picks the glyph and its position,
   given `SeatW = 76.dp` and a monochrome 5i print.
3. **(M4)** Retire the Bulk Mail tile the way Letters was retired, or relabel it
   honestly as a desk-site link? There is no third option — building it crosses
   AGENTS.md hard rule 14 (bridge rule).

Do not silently pick a default on any of these. Write the plan for the branches
you can plan unconditionally, mark the conditional parts, and hold the rest
until the answers land.

## The one authorized test change

`CourseHubScreenTest.hubSheetLabelsAreAllDocumentRoutes` (line 165) asserts that
every HTML `Page` export stays `null` in `hubSheetLabel`, commented *"fetch HTML
nothing draws"*. That invariant is exactly what M1 inverts. Per project rule you
do not quietly rewrite a passing test to make new code green: rewrite it only
after Q1 is answered, rewrite it to pin the **new** invariant (every `Page`
export resolves to a viewer the current window size can actually draw), and call
it out explicitly in the commit message. No other existing test may be edited
without asking.

## What the plan must contain

Write it to `docs/plans/2026-09-05-ui-gaps.md`. For each of M1, M2, M3:

- **Slices.** Ordered, each one compiling and green on its own. If Q1 comes back
  as (c), M1 is three slices, not one — say which.
- **Exact files touched**, matched against the handover's ownership map. Flag
  any file two milestones both want; strict single ownership per worker is how
  this repo parallelises.
- **Tests per slice**, named. Which existing tests extend, which cases are new,
  which module they live in. Remember: feature modules have no test source set,
  so every Compose screen is covered by Robolectric tests in `:app`. Tests live
  only in `:app`, `:core:model`, `:core:network`, `:core:datastore`,
  `:core:audit`.
- **The version bump** for that milestone (`versionName` + `versionCode` in
  `app/build.gradle.kts`), bumped once per milestone, before assembling, never
  for a docs-only change.
- **Hand-verification steps** the unit suite cannot see — the handover's
  § "What to check by hand" is the floor, not the ceiling.
- **Rollback shape.** What a bad slice looks like and how it comes out.

Then a short § Risks naming, at minimum: M1 regressing the tablet desk path
(viewer overlaying the rail, `vm.back()` ordering, native hall + 5i print from
the Board), M2 reflowing the 13-column teacher table, and M3 moving the rail
ordering that `SeatGridTest` pins (`CW-A1` nearest the Dhamma seat). For M3 also
state the persistence answer you verified: `CourseOpsStore` builds `Json` with
`ignoreUnknownKeys = true` and decodes inside `runCatching { … }.getOrNull()`,
so retiring `seatKind` needs no migration — pin that with a `CourseOpsStoreTest`
case decoding a fixture that still carries the key.

## Execution model to plan for

Implementation in this repo runs as a dynamic multi-agent workflow: parallel
scoped workers with strict file ownership and scoped tests, plus one integrator
that runs the full suite, bumps SemVer, builds the slim release and installs on
the Pixel C. Shape the plan so it can be cut that way — disjoint file sets per
worker, a named scoped test command per worker, and an explicit integration
step. M1/M2/M3 touch different modules, so they can overlap if and only if the
ownership map stays disjoint; check that and say so.

## Verify commands (for the plan, and for whoever executes it)

```bash
./gradlew :core:model:test :core:audit:test \
  :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest \
  :app:testDebugUnitTest
```

**Never `./gradlew test` or `:app:test` at the root.** It drags in
`:app:testReleaseUnitTest`, where every Robolectric Compose test dies with
"Unable to resolve activity … ComponentActivity" — `androidx.ui.test.manifest`
is `debugImplementation`, so the release variant has no test activity. That
failure is build config, not your code.

Install path (Pixel C over Wi-Fi ADB):

```bash
./gradlew :app:assembleDebug
export ANDROID_HOME=/Users/wizops/Android/Sdk
export PATH="$ANDROID_HOME/platform-tools:$PATH"
adb connect 10.0.0.144:5555
adb -s 10.0.0.144:5555 install -r -d app/build/outputs/apk/debug/app-debug.apk
adb -s 10.0.0.144:5555 shell am start -n org.dhamma.dipi.staff/.MainActivity
```

M1 is a phone feature but must be installed on the Pixel C too, to prove the
tablet desk path is unchanged.

## Done, for this session

1. The three questions asked, in one message, before planning.
2. `docs/plans/2026-09-05-ui-gaps.md` written, with the sections above.
3. A one-paragraph summary naming which parts of the plan are blocked on which
   answer.

No code, no test edits, no version bump. When the answers come back, update the
plan in place and only then start M1.

## When the implementation eventually finishes

Append the delta to `docs/DESIGN.md`, update the version line in `AGENTS.md` and
`CLAUDE.md`, and record the answers to Q1–Q3 in `docs/DECISIONS.md` with the
date — they are owner rulings and the next agent needs to know they were asked
and settled. Commits carry no agent attribution, no `Co-Authored-By`, no
generated-with footer.
