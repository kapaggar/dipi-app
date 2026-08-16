# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

**Now shipping:** Vertical 2 desk **1.9.0** (`versionCode` 18) on `feat/vertical-1`. Default host is live `https://dipi.vridhamma.org`.

Governing product rules: `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` (no client ACL, no `Approved`, no attendance write).  
**Transport (this file + `AGENTS.md` win):** the live desk is Drupal HTML, not Services login and not `/staff/*`. Backend PHP is immutable.

Vertical 1 loop: login → centre (from `dh_user_center`) → upcoming courses → today worklist (`var dataset`) → public card → `GET /change-status` → settings (remember me / erase all local data). Photo review/upload is mock-only.

**Do not assume:** `POST /api/user/login`, `GET /staff/session`, `POST /search-app`, or a hardcoded Dhamma Giri centre.

**NPI display amendment (owner decision 2026-08-16):** ID documents (Aadhaar/PAN/Voter ID/Passport) and health disclosures MAY be displayed on-screen for desk-side verification, but must never be persisted (no Room/DataStore/DTO fields) or logged — in-memory session map only (`SensitiveInfo`).
**Workflow:** implementation runs as a dynamic multi-agent workflow — parallel scoped workers (strict file ownership, scoped tests) plus an integrator that runs the full suite, bumps SemVer, builds the slim release, and installs on the Pixel C.
Centre settings are global (Centre screen), no longer a desk section.

See `AGENTS.md` (current assumptions) and `docs/LIVE-DESK-HAR.md`.

SemVer: bump `versionName` + `versionCode` on every shippable change (MAJOR/MINOR/PATCH). After a major (and any tablet-facing minor), install the debug APK on the Pixel C over Wi-Fi ADB (`10.0.0.144:5555`). Details in `AGENTS.md`.
