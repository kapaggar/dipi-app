# Centre trim — fewer desk links, fixed-height course cards

**Status:** proposed, 2026-08-30
**Baseline:** `main` at 1.23.0 / versionCode 36 (merge `e6db06f`), on the Pixel C.
**Origin:** owner feedback 2026-08-30, four items.

## S1 — Remove three desk destinations entirely

Drop **Manage Courses**, **Daily Activity** and **SMS Report** from
`centreDeskTiles` (`feature/course/.../DeskTiles.kt`). The catalogue becomes:

| Tile | Route | Action |
|---|---|---|
| Centre Settings | `centre/{cid}/edit` | `CentreOps` (native) |
| Advanced Search | `search-app/{cid}` | `AdvancedSearch` (native) |
| App Settings | — | `AppSettings` (native) |
| Course Report | `centre/{cid}/course-report` | desk-site chip |
| Bulk Mail | `centre/{cid}/bulk-mail-schedule` | desk-site chip |

Three native tiles, **two** desk-site chips (was five). `DeskTileAction`,
`DeskTileSpec` and the render split (`action != null` → tile,
`== null` → chip) are unchanged — this is a list edit.

**"and their design" —** the removal is recorded where the design pipeline
actually reads it, not by rewriting a delivered handover:

- Add all three to the **do-not-re-propose list** in
  `version-4/uploads/dipi-ui-export/SHIPPED-DELTA.md`. That file is the living
  contract every design pass is handed; it is the mechanism for exactly this.
- Correct the chip count in `version-4/README.md` frame 1a (it says "the five
  desk-site links") to two, with a dated note saying which three went and why.
- Correct the tile list in `docs/CLAUDE-DESIGN-DESK-SCREENS.md:73-74`.

The v4 frame PNGs and `.dc.html` are a delivered historical artifact and are
**not** repainted; the README note plus the do-not-re-propose entry is what
carries forward.

**Not touched:** the desk-site routes still exist on the live Drupal desk, and
`docs/LIVE-DESK-HAR.md` / `DESK-LAYOUT-FOR-ANDROID.md` / `DIPI_MEMORY_MAP.md`
document the *server*, not the app's surface. They stay accurate as-is.

## S2 — No scroll on upcoming courses

The upcoming pane is `weight(0.6f, fill = false).verticalScroll(...)`
(`CentreScreen.kt:121-122`). Remove the `verticalScroll` and its scroll state;
keep `weight(0.6f, fill = false)`.

**Safe because the content is bounded:** the desk serves at most four upcoming
courses (`upcoming_courses()` in `inc/centre.inc` — `limit 4`), rendered two per
row, so the pane holds at most two card rows. With S3 making every card the same
height, the block's height is now deterministic and fits inside the 60% ceiling
on the Pixel C. The narrow (<600dp) path keeps the page-level scroll it already
has — it is the whole page's scroll, not the pane's.

## S3 — Fixed-height course cards

Today `CourseMatrix.highlights` drops all-zero rows, so a card with no Received
row is shorter than one with — cards in the same row of the grid end up ragged.

**Every card renders exactly four rows, always, in this order:**

| Row | Source |
|---|---|
| `Received` | the `Received` status row |
| `Confirmed + Expected` | the `Confirmed` and `Expected` rows **summed** |
| `Cancelled` | the `Cancelled` status row |
| `Total` | `CourseMatrix.total` (unchanged, keeps its `+N sevak` suffix) |

A missing or all-zero row still renders, with `·` in every cell — that is what
makes the heights equal. Zeros stay `·`, never `0` (unchanged rule).

**Model additions** in `core/model/.../CourseMatrix.kt`:

```kotlin
/** Field-wise sum, for rows the desk keeps apart but the desk-hand reads together. */
operator fun MatrixRow.plus(other: MatrixRow?): MatrixRow
```
returning `this` when `other` is null, otherwise a row whose six counts are the
pairwise sums and whose label is the receiver's.

```kotlin
/** The four fixed card rows, in order; absent statuses become empty rows so every card is the same height. */
val CourseMatrix.cardRows: List<MatrixRow>
```
built as `Received` / `(Confirmed + Expected)` labelled `"Confirmed + Expected"` /
`Cancelled`, each falling back to `MatrixRow(label)` when the status is absent.

`highlights` and `HIGHLIGHT_LABELS` are **kept and unchanged** — removing them
would break `CourseMatrixTest`'s existing assertions for no benefit, and they
remain the honest "what the registrar acts on" accessor. `cardRows` is what the
card renders.

## S4 — Older-course buttons match an upcoming card's width

Older courses currently sit in a flexing column beside the 416dp desk column, so
their buttons are noticeably narrower than the upcoming cards above them.

**Decision:** render older courses on the **same two-column grid as upcoming
courses**, so an older button is exactly as wide as an upcoming card — the
"mid way" (half the pane) the feedback asks for. With the cap of three older
courses (`OLDER_COURSE_LIMIT`) that is one full row plus one half-row button.

This requires the older list to span the pane's full width, so the lower pane
stops being a side-by-side split: **older courses stack above the desk column**,
each full-width, with the desk column beneath. The desk column keeps its
three native tiles; with only two desk-site chips left (S1) it is now short
enough that stacking costs no fold.

Row height stays 42dp; the buttons only get wider. The empty-older-courses case
(desk column full width, three tiles across at 52dp) is unchanged.

## Tests this invalidates

- `CentreScreenTest:139-141` assert `Manage Courses` / `Daily Activity` /
  `SMS Report` are displayed. They are the direct subject of S1 — replace with
  assertions that all three are **absent** (`assertDoesNotExist`), so the
  removal is pinned rather than merely untested, and that the surviving five
  render and still fire their callbacks.
- Any assertion pinned to the two-column lower-pane split (S4) or to the
  variable-row card (S3).

Never touched: `centreSettingsRowIsReachableWithoutCourses`, the accommodation
read-only assertions, `OlderCourseLimitTest`, `RoomLayoutTest`, the
`SyncBanners` truth table, parser tests, `CourseMatrixTest`'s existing
`highlights` cases.

## Versioning

User-visible change inside the current vertical → **MINOR: 1.24.0 /
versionCode 37**. `feat/desk-gap` holds 1.19.0/30 — no collision. Registrar
facing → Pixel C install (hard rule 12).
