# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

**Now shipping:** **1.35.0** (`versionCode` 57) on `main` — Vertical 2 desk, **Sheets v5** (injected sheet stylesheet, native Day 0 summary, native centre Course report, eleven-chip Board, rebalanced Rooms) and **Course ops** (Teacher list / Seating plan with the teacher at the bottom / read-only student card; device-PIN gated; worklist + applications buffered on entry). See `docs/DECISIONS.md`.

Governing product rules live in `AGENTS.md` Hard rules (no client ACL, no `Approved`, no attendance write, server messages verbatim, bridge rule).  
**Transport (this file + `AGENTS.md` win):** the live desk is Drupal HTML, not Services login and not `/staff/*`. Backend PHP is immutable.

Vertical 1 loop: login → centre (from `dh_user_center`) → upcoming courses → today worklist (`var dataset`) → public card → `GET /change-status` → settings (remember me / erase all local data). Photo review/upload is mock-only.

Vertical 2 desk: one course, six rail sections (`DeskSection`: Board, Applications, Audit, Calling, Check-in, Rooms & seats) plus the `DeskScreen` phone routes. Board = the live desk's 11 shelf exports through `SheetTransport` plus Day-11 on the fourth-line row (Course report moved to the centre dashboard in v5 T3); HTML sheets render in the hardened WebView under an injected stylesheet with an allowlisted `conf`/`seating` sort; Check-in/Rooms merge web-assigned rooms parsed from `#table-attending` on `GET /zero-day/{cid}/{courseId}`; centre room config is read-only from `GET /centre/{cid}/acco-handler`; `nf`/`om`/`sm` confirmation prefixes drive the gender + new/old filters (`ConfPrefix`). Status vocabulary comes from the worklist `edit-app-status` select with roster fallback (T3).

**Tests:** full green suite is `./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest`. Never `./gradlew test` at the root — it drags in `:app:testReleaseUnitTest`, where every Robolectric Compose test dies (`ui-test-manifest` is `debugImplementation`, so `ComponentActivity` will not resolve).

**Do not assume:** `POST /api/user/login`, `GET /staff/session`, `POST /search-app`, or a hardcoded Dhamma Giri centre.

**NPI display amendment (owner decision 2026-08-16):** ID documents (Aadhaar/PAN/Voter ID/Passport) and health disclosures MAY be displayed on-screen for desk-side verification, but must never be persisted (no Room/DataStore/DTO fields) or logged — in-memory session map only (`SensitiveInfo`).
**Allocation sync amendment (owner decision 2026-08-16):** room-allocation sync via the desk's existing update form (`POST /app-update-attended/{id}` with the dialog's own fields), bulk and user-initiated, IS allowed — the client still never sends a status, never `Approved`, never NPI; backend PHP stays immutable.
**Workflow:** implementation runs as a dynamic multi-agent workflow — parallel scoped workers (strict file ownership, scoped tests) plus an integrator that runs the full suite, bumps SemVer, builds the slim release, and installs on the Pixel C.
Centre settings are global (Centre screen), no longer a desk section.

See `AGENTS.md` (current assumptions), `docs/LIVE-DESK.md` and `docs/DESIGN.md`.

SemVer: bump `versionName` + `versionCode` on every shippable change (MAJOR/MINOR/PATCH). After a major (and any tablet-facing minor), install the debug APK on the Pixel C over Wi-Fi ADB (`10.0.0.144:5555`). Details in `AGENTS.md`.
