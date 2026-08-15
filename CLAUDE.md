# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

**Now shipping:** Vertical 1 **1.4.1** (`versionCode` 10) on `feat/vertical-1`. Default host is live `https://dipi.vridhamma.org`.

Governing product rules: `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` (no client ACL, no `Approved`, no attendance write).  
**Transport (this file + `AGENTS.md` win):** the live desk is Drupal HTML, not Services login and not `/staff/*`. Backend PHP is immutable.

Vertical 1 loop: login → centre (from `dh_user_center`) → upcoming courses → today worklist (`var dataset`) → public card → `GET /change-status` → settings (remember me / erase all local data). Photo review/upload is mock-only.

**Do not assume:** `POST /api/user/login`, `GET /staff/session`, `POST /search-app`, or a hardcoded Dhamma Giri centre.

See `AGENTS.md` (current assumptions) and `docs/LIVE-DESK-HAR.md`.

SemVer: bump `versionName` + `versionCode` on every shippable change (MAJOR/MINOR/PATCH). After a major (and any tablet-facing minor), install the debug APK on the Pixel C over Wi-Fi ADB (`10.0.0.144:5555`). Details in `AGENTS.md`.
