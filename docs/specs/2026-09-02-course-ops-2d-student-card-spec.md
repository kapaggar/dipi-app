# 2d — Student card + prefetch + flags — spec

**Status:** approved for build, 2026-09-02. **Baseline:** Wave 1 merged (2a+2b).
**Design:** frame 2d + `docs/DESIGN.md` § Course ops (2d + corrections).
Server contract from `dh_manageapp/inc/search.inc:1963-2198`.

## Governing amendment (owner, 2026-09-02) — record verbatim in DECISIONS.md

Course-ops application data (roll + `/application-view` answers, health
included) MAY be persisted device-local so the hall reads offline across
restarts. Encrypted at rest in the course-ops store's own
EncryptedSharedPreferences file; keyed to the resolved running course id;
wiped when the resolved course changes, on Erase-all, and on logout of the
account. Never logged (redacting `toString()` on every model); never in Room;
never in plain DataStore. This amendment applies ONLY to the course-ops store
— the desk-mode `SensitiveInfo` rules are unchanged.

## S1 — Transport + parser

`StaffApi`: `@GET("/application-view/{id}") suspend fun applicationView(@Path
"id") id: Int): Response<ResponseBody>` — path param only. Full themed Drupal
page; parse with the `av-sec`/`av-row` structure (`<h3>` section titles,
`av-label`/`av-val` spans).

`core/network/ApplicationViewParser.kt` extracts ONLY:
- Header: `<h2>{name} ({conf})</h2>` (conf optional), `av-status` line
  (`{status} · {course}`), photo presence (`show-photo/` img).
- `Personal` §: the eight labelled rows verbatim (Gender, Date of Birth, Age,
  Nationality, Old / New, Monk / Nun, A-List, Applied On); `-` stays `-`.
- `Course History` §: ten counts in SERVER order (`10-Day Teen STP Special TSC
  20-Day 30-Day 45-Day 60-Day Service`) + First Course, Last Course, Practice
  Details verbatim.
- `Health` §: six rows with labels verbatim — Physical, Mental, Medication,
  Intoxicants, Other Techniques, Pregnancy (`Yes`/`No`/`Yes - … (N months)`).
**The parser NEVER reads** Contact, Identification, Background, Emergency
Contact, Languages, Other, Children/Teen, Long Course Details, or the four
lazy `Loading...` sections — assert structurally (see tests). Model
`ApplicationCard` (core/model): non-serializable in-memory type with redacting
`toString()`; a parallel `@Serializable` snapshot DTO lives INSIDE
`core/datastore/CourseOpsStore` for the encrypted cache only.

## S2 — Applicant-id mapping + prefetch

Prefetch on entry to course ops after the roll parse: for every row with an
applicant id, `applicationView(id)` at ≤4 concurrent (reuse the
`BoundedLoader` semaphore pattern or a simple `Semaphore(4)` in the repo);
results land in `CourseOpsStore` (memory map + encrypted write-behind).
Progress is silent; flags appear on rows as answers land. Failures stay
un-flagged and retry ONLY on next course-ops entry (never a hot loop). If 2b's
BLOCKED-check found no per-row applicant id in the teacher-list markup, the
ruling made there governs the mapping; implement per that ruling.

## S3 — Flags (derived, never judged)

Pure fn in core/model: `flagsFor(card: ApplicationCard, gender): List<String>`
— `HLTH` (Physical or Mental non-empty and != "-"), `MED`, `INTOX`
(Intoxicants), `TECH` (Other Techniques), `PREG` (Pregnancy starts "Yes"),
`MONK` (Personal Monk/Nun == "Yes"). Order fixed as listed. A flag is a 22dp
neutral pill — never a colour code (no severity hex anywhere near these).

## S4 — Screen (`:feature:teacher/StudentCardScreen.kt`)

Per frame 2d and the corrections:
- Header 60dp: back ‹ 44dp; name 24sp + status chip (`OLD · OM7` style from
  roll seniority + conf when present); placement line `room · seat · group ·
  AT-code`; `‹ ›` 48dp pair walking the CURRENT group in roll order, stopping
  at ends (disable state: 38% alpha — no drawn spec, note as deviation).
- Left 404dp fixed: photo 132×158 via `PhotoLoader` (placeholder text when
  absent); personal table rows 22.5dp (keys/values verbatim from parser); ten
  50dp history tiles in SERVER order, non-zero `accent100`/`accent300`/
  `accent700`, zero `#FAFAFB`/`#E7E7EA`/neutral400 — zeros stay; history meta
  rows (First/Last/Practice, values verbatim).
- Right column scrolls: kicker + caption; one card per Health row IN ORDER,
  question label verbatim; `YES` tag + body at Roboto-equivalent 14.5sp/1.5,
  NEVER truncated; flagged rows tinted `accent100` on `accent300` (NOT the
  frame's off-ramp `#F7FBFF`), 2dp left rule accent500; empty rows `NO` tag,
  no body, still shown; Pregnancy for gender M renders tag `N/A`.
- Read-only: no edit, no note, no share, no export.
- Offline: cards render from the store; a card never fetched shows an honest
  "Not cached — connect once to fetch" body with the offline strip.

## Tests

- `ApplicationViewParserTest`: fixture with ALL sections present incl.
  realistic Identification/Emergency/Contact NPI — assert extracted fields
  verbatim AND the load-bearing negative: no Aadhaar/PAN/passport/emergency/
  address/mobile string from the fixture appears anywhere in the parsed model
  (serialize the model's fields and grep). Pregnancy `Yes - 4 (months)` parse;
  `-` passthrough; missing photo.
- `CourseOpsStoreTest` (core/datastore): write→read round-trip; wipe on course
  change; wipe on erase-all; survives... process restart is implicit (file);
  raw health text never appears in `toString()` of anything.
- `FlagsTest` (core/model): each flag's trigger + MONK + PREG gender N/A + the
  never-empty-means-flag rule (`-` and blank both un-flag).
- `StudentCardScreenTest` (app): answers render full-width never-truncated
  (unclipped bounds grow with text), empty question still shown with NO tag,
  ‹ › walk group order and stop at ends, left column fixed while right
  scrolls, N/A pregnancy for males.
Never touched: desk-mode `SensitiveInfo` tests and wipe points; `CardScreen`
(phone desk card) untouched.
