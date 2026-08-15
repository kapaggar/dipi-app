# DIPI Staff

Native centre-staff client for the DIPI registrar desk (`dh_manageapp`). Package `org.dhamma.dipi.staff`.

**Shipped:** **1.4.1** (`versionCode` 10), branch `feat/vertical-1`.

**Start here:** [AGENTS.md](AGENTS.md) (current assumptions) and [docs/LIVE-DESK-HAR.md](docs/LIVE-DESK-HAR.md).

Product rules (no client ACL, no `/staff` status façade, no attendance write, fixed URL) still come from [docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md](docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md). That prompt’s **transport section is stale**: live Drupal does not implement `/staff/*` or Services login. The app scrapes the existing desk.

**Design:** [docs/DIPI Staff.dc.html](docs/DIPI%20Staff.dc.html) — visual source of truth.  
**Historical mock contract:** [docs/openapi-staff.yaml](docs/openapi-staff.yaml) (fixtures only).  
**Live host is immutable:** do not add PHP; see [docs/TODO-SERVER.md](docs/TODO-SERVER.md).

## Run

```bash
# local.properties must contain sdk.dir=…
# Live Drupal (https://dipi.vridhamma.org) is the default.
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :core:model:test :core:network:testDebugUnitTest :core:audit:test
```

Fixtures only if you opt in:

```bash
./gradlew :app:assembleDebug -Pdipi.useMock=true
```

Override host (debug only):

```properties
# gradle.properties or -P
dipi.baseUrl=https://your-dipi-host.example
```

Release `BuildConfig.BASE_URL` is `https://dipi.vridhamma.org`. There is **no URL field** on login; the centre comes from Drupal `dh_user_center` after sign-in.

## Mock vs real

| Build | `USE_MOCK` | Host |
|---|---|---|
| debug (default) | **false** | `https://dipi.vridhamma.org` (desk HTML) |
| debug `-Pdipi.useMock=true` | true | in-process MockWebServer (`/staff/*` fixtures) |
| release | false | `https://dipi.vridhamma.org` |

Mock `/change-status`: Rakesh Iyer is refused with the Area-teacher message; other Confirmed writes mint `NF129`. Login password `bad` returns `Unrecognized username or password.`

Settings: **Remember me**, **Simulate offline**, **Erase all local data** (factory reset), **Log out**.

## What it is / is not

Staff desk for centre registrars: find an applicant, read the card, change a status. Centre comes from the signed-in user’s mapping, not a hardcoded name.  
Not student-apply, not a WebView, not AT/SMS/WhatsApp/IVR, not `/api` APP API, not attendance writes. Photo upload is not exposed on the live desk.

## Layout

```
:app                 Hilt app, repository, ViewModel, chrome
:core:model          ids, status, worklist filter, UserCentreMap (mock names)
:core:network        Retrofit + desk HTML parser + mock dispatcher
:core:database       Room + SQLCipher (one course + outbox)
:core:datastore      Encrypted prefs (cookie/CSRF/remember-me) + DataStore
:core:ui             tokens, badge, row, chips
:core:audit          client rules (never block)
:feature:auth        login + Remember me
:feature:course      course list + Settings entry
:feature:applicants  today, card, status sheet
:feature:photos      photo review (mock)
:feature:summary     day summary (read-only)
:feature:settings    theme, offline, logout, factory reset
```
