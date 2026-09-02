# Applicant desk history port from feat/desk-gap — spec (T6)

**Status:** proposed, 2026-08-31
**Baseline:** `main` @ `0ce3342`, 1.27.0 / versionCode 42. Source branch:
`feat/desk-gap` @ `838dc0a` ("ship unsigned desk-gap reads and Day-11 export at
1.19.0"). Read source files with `git show feat/desk-gap:<path>` — do NOT switch
branches.
**Origin:** owner-directed port. Of the branch's surviving live work, applicant
desk history is the cleanest: no `main` feature competes, no endpoint was
removed, all additions are append-only and defaulted.

**Explicitly OUT of scope (do not port, recorded here so no agent re-proposes):**
server-side Advanced Search (`DeskSearchFields` — its `searchAppForm`/`searchAppSubmit`
endpoints were deliberately deleted from `main` in `0dd0c71`, and `POST /search-app`
is on AGENTS.md's do-not-assume list pending HAR verification — owner decision
required); Manage Courses / Daily Activity / SMS Report / Letters / `CentreEditScreen`
(dead by owner decision 2026-08-30); the branch's wider `hubSheetLabel`
(regresses `main`'s document-only rule, pinned by
`CourseHubScreenTest::hubSheetLabelsAreAllDocumentRoutes`).

## What ships

Three lazily-fetched collapsible sections on the applicant detail — past courses,
desk activity log, clarification letters with per-letter PDF — on **both** the
tablet (`ApplicationsPane` detail) and the phone (`CardScreen`), backed by the
desk's own worklist-expander fragments:

| Fetch | Endpoint | Parser output |
|---|---|---|
| courses | `GET /app-courses/{id}` | `List<ApplicantCourseRow>` |
| activity | `GET /app-activity/{id}` | `List<ApplicantActivityRow>` |
| clarifications | `GET /app-clarifications/{id}` | `List<ApplicantClarificationRow>` (carries `clarId` from the `show-clarification/(\d+)/(\d+)` href) |
| PDF | `@Streaming GET /show-clarification/{appId}/{clarId}` | streamed to `cacheDir/sheets` (wiped on logout / session expiry / erase-all, like every sheet) |

## Port order (each step compiles and tests green before the next)

### 1. `HtmlTables`/`HtmlForms` scraper (prerequisite)

`git show feat/desk-gap:core/network/src/main/kotlin/org/dhamma/dipi/staff/network/HtmlForms.kt`
→ same path on `main`, verbatim. It is self-contained (uses `main`'s existing
`extractElementById` and `SearchPageParser.stripTags`). Do NOT refactor
`CourseReportFormParser.inputValue` to delegate to it — that touches a shipped
path for zero behaviour change.

### 2. Model types

The branch's `core/model/.../DeskReads.kt` carries 7 now-dead types alongside the
4 live ones. Create a NEW file `core/model/src/main/kotlin/org/dhamma/dipi/staff/model/ApplicantHistory.kt`
holding only, copied verbatim from the branch's `DeskReads.kt`:
`ApplicantCourseRow`, `ApplicantActivityRow`, `ApplicantClarificationRow`,
`ApplicantDeskHistory` (fields: `courses?`, `activity?`, `clarifications?`,
`loading: Set<String>`, `errors: Map<String, String>`). Do not create
`DeskReads.kt` on `main`; do not port the dead types (`ManagedCourse`,
`DailyActivity*`, `Sms*Row`, `LetterRow`, `CentreFormSettings`).

### 3. Parser

`git show feat/desk-gap:core/network/src/main/kotlin/org/dhamma/dipi/staff/network/ApplicantHistoryParser.kt`
→ same path, verbatim, plus its test
`core/network/src/test/.../ApplicantHistoryParserTest.kt` from the branch.

### 4. Transport

`core/network/.../StaffApi.kt`: re-apply from the branch the four interface
methods (`appCourses`, `appActivity`, `appClarifications`, `@Streaming
showClarification`) and `SheetTransport.clarificationPdf(...)` (reuses the
existing `save()` → `cacheDir/sheets` path). Copy signatures exactly as `git show
feat/desk-gap:...StaffApi.kt` has them. These are plain GETs — the no-`r`-param
rule applies as on every desk GET.

Mocks: re-apply the branch's `DipiMockDispatcher` routes (`app-courses`,
`app-activity`, `app-clarifications`, `show-clarification`, including the
`FORBIDDEN_CENTRE` 403 arm) and the three `MockFixtures` HTML fixtures
(`appCoursesHtml`, `appActivityHtml`, `appClarificationsHtml`), plus the branch's
`core/network` mock test for the PDF-streams-to-cache and verbatim-403 behaviours
(branch file `DeskReadMockTest.kt` — port only its history/clarification tests;
its `searchApp` tests are out of scope).

### 5. Repository + ViewModel

`StaffRepository`: port `loadAppCourses` / `loadAppActivity` /
`loadAppClarifications` via the branch's shared `fetchFragment` helper
(login-HTML → unauthorized, non-2xx → verbatim body) and `fetchClarification`.
Fragments carry history text: keep them **in memory only** — no Room, no
DataStore, nothing added to `persist()`.

`DeskViewModel`: port `history: Map<ApplicantId, ApplicantDeskHistory>` into
`DeskUiState` (the branch wraps it in `DeskReadUi` — flatten to a plain state
field instead, the wrapper's other members are dead), plus `expandHistory(id,
key)`, `openClarification(appId, clarId)`, `patchHistory`, and the `clarFetch`
test seam, adapted to `main`'s current state shape. Clarification PDFs resolve
through the existing `resolveSheet` → `openDoc` path so the system viewer works
at both widths.

### 6. UI

`core/ui/.../ApplicantHistorySections.kt` from the branch (the `HistoryBlock`
expander: `▸/▾` titles, loading / verbatim-error / content states, "Open PDF"
with `contentDescription = "Open clarification PDF"`). **Check its color tokens
before compiling in:** if it uses `Industry` tokens, it is safe on the tablet
pane but NOT on the phone card (Industry is not dark-aware) — in that case give
the composable explicit color parameters with `Industry` defaults and pass
`LocalDipi`-derived values from `CardScreen`, mirroring the T2 spec's rule.

Thread through both consumers with defaulted params exactly as the branch does:
`feature/applicants/.../CardScreen.kt` (three defaulted params: the history for
this card, `onExpand: (String) -> Unit`, `onClarification: (Int, Int) -> Unit`)
and `feature/desk/.../ApplicationsPane.kt` (same, threaded into `AppDetail`).
Both files have moved since the merge base — apply the branch's diff hunks by
hand (`git diff 03ccb69..feat/desk-gap -- <file>`), do not `git checkout` the
branch copies (that would revert 8 releases of `main` work).

`DipiAppUi.kt`: pass `state.history[card.id]`, `vm::expandHistory` (curried per
card), `vm::openClarification` at both call sites.

### 7. Screen test

Port `app/src/test/.../ApplicantHistoryScreenTest.kt`
(`expandFetchesAndClarificationOpens`) and adapt to `main`'s test helpers.

## Tests invalidated

None on `main` — every addition is behind a defaulted parameter. The ported
branch tests join the suite. Never touched: `CourseHubScreenTest`,
`AdvancedSearchScreenTest`, every existing `ApplicationsPane`/`CardScreen`
assertion (verify by running the full suite before AND after).

## Constraints

Backend PHP immutable — the four endpoints already exist on the live desk (they
are the desk's own expander fragments); history fragments and PDFs are
display-only (memory / `cacheDir/sheets`), wiped on logout/expiry/erase-all;
no NPI persistence or logging; no `r` param on any GET; tokens rule above;
no agent trailers; never bare `./gradlew test`.

## Versioning note

User-visible MINOR (both sizes gain history sections). Part of its own wave per
the master plan; Pixel C install required (registrar-facing).
