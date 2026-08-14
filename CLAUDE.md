# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

Governing spec: `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` (wins over older architecture/Grok prompts).

Vertical 1: login → course → today worklist → public card → `/change-status` → photo review → day summary → settings.

No client tenancy/gender gating. No attended API. Fixed `BuildConfig.BASE_URL`. Talks to live `https://dipi.vridhamma.org` by default (Services login, `/search-app`, `/get-courses`, `/change-status`). Mock only with `-Pdipi.useMock=true`.

See `AGENTS.md` and `docs/TODO-SERVER.md`.

SemVer: bump `versionName` + `versionCode` on every shippable change (MAJOR/MINOR/PATCH). After a major (and any tablet-facing minor), install the debug APK on the Pixel C over Wi-Fi ADB (`10.0.0.144:5555`). Details in `AGENTS.md`.
