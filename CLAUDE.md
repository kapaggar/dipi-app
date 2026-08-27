# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

**Now shipping:** Vertical 2 desk **1.20.0** (`versionCode` 31) on `main` — `feat/vertical-1` is merged and gone. Unmerged `feat/desk-gap` carries **1.19.0** (centre tiles, letters, applicant history, desk search — reads only, plus QA screenshots in `docs/qa-1.19.0/`). Default host is live `https://dipi.vridhamma.org`.

Governing product rules: `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` (no client ACL, no `Approved`, no attendance write).  
**Transport (this file + `AGENTS.md` win):** the live desk is Drupal HTML, not Services login and not `/staff/*`. Backend PHP is immutable.

Vertical 1 loop: login → centre (from `dh_user_center`) → upcoming courses → today worklist (`var dataset`) → public card → `GET /change-status` → settings (remember me / erase all local data). Photo review/upload is mock-only.

Vertical 2 desk: one course, six rail sections (`DeskSection`: Board, Applications, Audit, Calling, Check-in, Rooms & seats) plus the `DeskScreen` phone routes. Board = the live desk's 12 sheets/exports through `SheetTransport`; Check-in/Rooms merge web-assigned rooms parsed from `#table-attending` on `GET /zero-day/{cid}/{courseId}`; centre room config is read-only from `GET /centre/{cid}/acco-handler`; `nf`/`om`/`sm` confirmation prefixes drive the gender + new/old filters (`ConfPrefix`).

**Tests:** full green suite is `./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest`. Never `./gradlew test` at the root — it drags in `:app:testReleaseUnitTest`, where every Robolectric Compose test dies (`ui-test-manifest` is `debugImplementation`, so `ComponentActivity` will not resolve).

**Do not assume:** `POST /api/user/login`, `GET /staff/session`, `POST /search-app`, or a hardcoded Dhamma Giri centre.

**NPI display amendment (owner decision 2026-08-16):** ID documents (Aadhaar/PAN/Voter ID/Passport) and health disclosures MAY be displayed on-screen for desk-side verification, but must never be persisted (no Room/DataStore/DTO fields) or logged — in-memory session map only (`SensitiveInfo`).
**Allocation sync amendment (owner decision 2026-08-16):** room-allocation sync via the desk's existing update form (`POST /app-update-attended/{id}` with the dialog's own fields), bulk and user-initiated, IS allowed — the client still never sends a status, never `Approved`, never NPI; backend PHP stays immutable.
**Workflow:** implementation runs as a dynamic multi-agent workflow — parallel scoped workers (strict file ownership, scoped tests) plus an integrator that runs the full suite, bumps SemVer, builds the slim release, and installs on the Pixel C.
Centre settings are global (Centre screen), no longer a desk section.

See `AGENTS.md` (current assumptions) and `docs/LIVE-DESK-HAR.md`.

SemVer: bump `versionName` + `versionCode` on every shippable change (MAJOR/MINOR/PATCH). After a major (and any tablet-facing minor), install the debug APK on the Pixel C over Wi-Fi ADB (`10.0.0.144:5555`). Details in `AGENTS.md`.
