# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

Native Android client for DIPI **centre staff** (registrars / data-entry). Package: `org.dhamma.dipi.staff`.

Sibling server: `/Users/wizops/DIPI/dipi-web` (`dh_manageapp`).

**Current state:** docs only. Do not scaffold the Gradle project until the human approves Vertical 1 after Fable validates `docs/plans/2026-08-13-p0-fable-validation.md`.

## Non-goals

- Student apply (`dipi-applicant`)
- WebView of the Drupal desk
- AT portal, SMS, WhatsApp, IVR, Mitra, Patrika
- Wrapping `/api` as the registrar API (IDOR on `get-app-detail` via APP API)

## Vertical 1

Login → centre + unfinalized course → applicant worklist/search → public card → `_change_status` → `a_attended` toggle.

## Must follow

- Hybrid API in `docs/00-architecture.md` and `docs/openapi-staff.yaml`
- Bridge rule in `docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md`
- Centre + gender tenancy on every list/detail
- No NPI (`ae_*`, national IDs) in v1 Room or logs
- Status strings from `dh_type_detail`; do not reimplement waitlist / conf-no mint / LC approve in Kotlin

## Stack (when implementing)

Kotlin, Compose + Material 3, Navigation 3, Hilt, Retrofit, Encrypted DataStore, Room + SQLCipher (active course only). Min SDK 26, target 35+.

## Commands (after scaffold)

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Requires JDK 17+ and `local.properties` `sdk.dir`.
