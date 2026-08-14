# CLAUDE.md

DIPI Staff Android (`org.dhamma.dipi.staff`).

Governing spec: `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` (wins over older architecture/Grok prompts).

Vertical 1: login → course → today worklist → public card → `/change-status` → photo review → day summary → settings.

No client tenancy/gender gating. No attended API. Fixed `BuildConfig.BASE_URL`. Mock server on in debug.

See `AGENTS.md` and `docs/TODO-SERVER.md`.
