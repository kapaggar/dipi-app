# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

**Now shipping:** **2.3.3** (`versionCode` 106). Behaviour per release from 2.0.0 is in `CHANGELOG.md`; design measurements per release are the shipped-delta ledger in `docs/DESIGN.md`; owner rulings are in `docs/DECISIONS.md`. Current binding facts: allocated room + not Left is checked in and room punctuation is ignored (`Mbk- 51` == `Mbk 51`); Room Chart inventory is `GET /centre/{cid}/acco-handler` and Hall Settings come from `GET /centre/{cid}/edit`, both read-only; sevak prefixes `sm`/`sf` count as Old with gender from the second letter (`ConfPrefix`); the desk rail starts at 840dp with 14sp/48dp floors; only Cancelled/Left/Rejected/Regret/Duplicate leave the roll and WaitList is held; photo review, correction and ML Kit scan are off unless built with `-Pdipi.photoReview=true` (`docs/PHOTO-BUILDS.md`); WhatsApp automation is off by default (`docs/WHATSAPP.md`); keep-alive is the unchanged 20-minute pair.

Governing product rules live in `AGENTS.md` Hard rules (no client ACL, no `Approved`, no attendance write, server messages verbatim, bridge rule).  
**Transport (this file + `AGENTS.md` win):** the live desk is Drupal HTML, not Services login and not `/staff/*`. Backend PHP is immutable.

Vertical 1 loop: login → centre (from `dh_user_center`) → upcoming courses → today worklist (`var dataset`) → public card → `GET /change-status` → settings (remember me / erase all local data). Photo review is native correction plus a full-form desk update (`GET`+`POST /app/{id}/edit`); Course ops stays read-only.

Vertical 2 desk: one course, six rail sections (`DeskSection`: 0 Day Board, Applications, Audit, Calling, Check-in, Rooms & seats) plus the `DeskScreen` phone routes. 0 Day Board = the live desk's **3×3** exports through `SheetTransport` (Course summary in-grid; Valuable list off the Board since 1.37.2; Course report moved to the centre dashboard in v5 T3; Male/Female PDFs removed in 1.37.1); HTML sheets render in the hardened WebView under an injected stylesheet with an allowlisted `conf`/`seating` sort; Check-in/Rooms merge web-assigned rooms parsed from `#table-attending` on `GET /zero-day/{cid}/{courseId}`; centre room config is read-only from `GET /centre/{cid}/acco-handler`; `nf`/`om`/`sm` confirmation prefixes drive the gender + new/old filters (`ConfPrefix`). Status vocabulary comes from the worklist `edit-app-status` select with roster fallback (T3).

**Tests:** full green suite is `./gradlew :core:model:test :core:audit:test :core:network:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest`. Never `./gradlew test` at the root — it drags in `:app:testReleaseUnitTest`, where every Robolectric Compose test dies (`ui-test-manifest` is `debugImplementation`, so `ComponentActivity` will not resolve).

**Do not assume:** `POST /api/user/login`, `GET /staff/session`, `POST /search-app`, or a hardcoded Dhamma Giri centre.

**NPI display amendment (owner decision 2026-08-16):** ID documents (Aadhaar/PAN/Voter ID/Passport) and health disclosures MAY be displayed on-screen for desk-side verification, but must never be persisted (no Room/DataStore/DTO fields) or logged — in-memory session map only (`SensitiveInfo`).
**Allocation sync amendment (owner decision 2026-08-16):** room-allocation sync via the desk's existing update form (`POST /app-update-attended/{id}` with the dialog's own fields), bulk and user-initiated, IS allowed — the client still never sends a status, never `Approved`, never NPI; backend PHP stays immutable.
**Checked-in amendment (owner 2026-09-16):** allocated room + not Left ⇒ checked in. Room format is ignored (`Mbk- 51` == `Mbk 51`). Room Chart uses acco-handler inventory plus Hall Settings from `GET /centre/{cid}/edit` (read-only Main Plan: columns, chowky, direction). Centre Settings displays that plan; SAVE HALL LAYOUT is the local override only when the desk omits a field (depth always local).
**Workflow:** implementation runs as a dynamic multi-agent workflow — parallel scoped workers (strict file ownership, scoped tests) plus an integrator that runs the full suite, bumps SemVer, builds the slim release, and installs on the Pixel C.
Centre settings are global (Centre screen), no longer a desk section.

See `AGENTS.md` (current assumptions), `docs/LIVE-DESK.md` and `docs/DESIGN.md`.

**Java desktop twin (planned, sibling repo — not this codebase):** a JavaFX 21 desktop port of the staff desk is specced and planned in `docs/java-desktop/2026-09-05-java-desktop-design.md` + `docs/java-desktop/2026-09-05-java-desktop-plan.md`; to start it, paste `docs/java-desktop/2026-09-05-java-desktop-handover.md` into a fresh session. It reuses the same live-desk transport rules; nothing in this repo changes for it.

SemVer: bump `versionName` + `versionCode` on every shippable change (MAJOR/MINOR/PATCH). After a major (and any tablet-facing minor), install the debug APK on the Pixel C over Wi-Fi ADB (`10.0.0.144:5555`). Details in `AGENTS.md`.
