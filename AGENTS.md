# AGENTS.md

Guidance for Claude Code, Cursor, Codex, Fable, Grok.

## What this is

Centre-staff Android client for DIPI registrar desk. Package: `org.dhamma.dipi.staff`.

**Governing spec:** `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md`  
It **supersedes** `docs/00-architecture.md` and `docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md` on conflicts.

Server reference (read-only): `/Users/wizops/DIPI/dipi-web` module `dh_manageapp`.

## Hard rules

1. No access control in the app. Send the request; render the server response verbatim.
2. No status engine in Kotlin. Display and send strings only.
3. Never send status `Approved`.
4. Status write = existing `/change-status/{id}?s=&l=&c=` with `l=0`.
5. No attendance writes in v1.
6. Never parse HTML. Never use APP API / `get-app-detail`.
7. No NPI columns in Room or logs (`ae_*`, Aadhaar, PAN, passport, voter id).
8. Server URL is `BuildConfig.BASE_URL` — no login URL field.
9. Design file `docs/DIPI Staff.dc.html` wins every visual argument.
10. Do not commit `local.properties`, keystores, or real student data.

## Commands

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

JDK 17+, `sdk.dir` in `local.properties`.
