# DIPI Staff

Native centre-staff client for the DIPI registrar desk (`dh_manageapp`). Package `org.dhamma.dipi.staff`.

**Governing spec:** [docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md](docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md)  
That file **supersedes** [docs/00-architecture.md](docs/00-architecture.md) and [docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md](docs/DIPI-STAFF-ANDROID-GROK-PROMPT.md) where they conflict (no client access control, no `/staff` status façade, no attendance write, fixed `BuildConfig.BASE_URL`).

**Design:** [docs/DIPI Staff.dc.html](docs/DIPI%20Staff.dc.html) — visual source of truth.  
**API:** [docs/openapi-staff.yaml](docs/openapi-staff.yaml) v0.2  
**PHP still needed:** [docs/TODO-SERVER.md](docs/TODO-SERVER.md)

## Run

```bash
# local.properties must contain sdk.dir=…
# Live Drupal (https://dipi.vridhamma.org) is the default.
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :core:model:test :core:audit:test
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

Release `BuildConfig.BASE_URL` is `https://dipi.vridhamma.org`. There is **no URL field** on login; the centre comes from Drupal user mapping (`dh_user_center`, e.g. `sudha.user` → Dhamma Sudha).

## Mock vs real

| Build | `USE_MOCK` | Host |
|---|---|---|
| debug (default) | true | in-process MockWebServer (design fixtures) |
| release | false | `https://dipi.vridhamma.org` |

Mock `/change-status`: Rakesh Iyer is refused with the Area-teacher message; other Confirmed writes mint `NF129`. Login password `bad` returns `Unrecognized username or password.`

Settings has **Simulate offline** so you can enqueue a status change and see the queued row + banner.

## What it is / is not

Staff desk for centre registrars: find an applicant, read the card, change a status. The signed-in user's `dh_user_center` mapping picks the centre.  
Not student-apply, not a WebView, not AT/SMS/WhatsApp/IVR, not `/api` APP API, not attendance writes.

## Layout

```
:app                 Hilt app, repository, ViewModel, chrome
:core:model          ids, status, worklist filter, outbox reconciler
:core:network        Retrofit + mock dispatcher/fixtures
:core:database       Room + SQLCipher (one course + outbox)
:core:datastore      Encrypted prefs (cookie/CSRF) + DataStore (theme)
:core:ui             tokens, badge, row, chips
:core:audit          client rules (never block)
:feature:auth        login
:feature:course      course list
:feature:applicants  today, card, status sheet
:feature:photos      photo review
:feature:summary     day summary (read-only)
:feature:settings
```
