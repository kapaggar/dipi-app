# DIPI Staff — complete Vertical 1 against `dipi-web` (no mock)

**Date:** 2026-08-15
**App repo (write here):** `/Users/wizops/DIPI/dipi-app`
**Backend source (read-only):** `/Users/wizops/DIPI/dipi-web`
**Module of record:** `sites/all/modules/dh_manageapp`
**Design (visual source of truth):** `docs/DIPI Staff.dc.html`
**Current ship:** 1.4.1 (`versionCode` 10), branch `feat/vertical-1`
**Package:** `org.dhamma.dipi.staff`

This prompt **replaces** the mock-first / `/staff/*` plan in `docs/DIPI-STAFF-IMPLEMENTATION-PROMPT-GROK-4.6.md` for all remaining work. Product rules from that file and `AGENTS.md` still hold. Transport does not: there is no `/staff` JSON layer and no Services login in `dipi-web`.

---

## 0. Mission

Finish the registrar desk Android client so **debug and release both talk only to the existing Drupal 7 desk**, as implemented in `dipi-web`. Remove the mock stack. Do not add PHP. Do not invent routes.

One user, short sessions: sign in → pick upcoming course → find an applicant → read the card → change a status. Phone stacked; tablet `width >= 600.dp` is list–detail (Today left, card right).

**Done when:**

1. `-Pdipi.useMock` is gone. `USE_MOCK` is gone. MockWebServer never starts.
2. Every read/write the app performs is a real `dh_manageapp` menu path (see §3), parsed from HTML/JSON the PHP already emits.
3. Unit tests cover parsers with **checked-in HTML/JSON fixtures** derived from `dipi-web` (not live traffic, not MockWebServer).
4. Photo upload is not offered (it is not a desk endpoint).
5. SemVer bumped (MINOR if user-visible, else PATCH) in `app/build.gradle.kts`.
6. `./gradlew :app:testDebugUnitTest :core:network:testDebugUnitTest :core:model:test :core:audit:test :app:assembleDebug` is green.

---

## 1. Ground rules (non-negotiable)

1. **`dipi-web` is immutable.** Read it. Do not edit it. Do not add `/staff/*`, Services resources, or new PHP.
2. **No access control in the app.** Send the request. Render the server response verbatim. Drupal already gates on `access manageapp`, `change status`, and `dh_user_center`.
3. **No status engine in Kotlin.** Display and send status strings. `_change_status` / `_update_status` / `dh_send_letter` decide transitions, conf-no, letters.
4. **Never send `s=Approved` or `s=R-ATReview`.** Those are LC paths in `_change_status()` (`dh_manageapp.module` ~1456). The sheet must not offer them.
5. **NPI never persists.** `dh_manageapp_search_results()` embeds `aadhar`, `passport`, `voterid`, `pancard` in `var dataset`. Drop those keys in the parser. No Room columns, no logs, no UI.
6. **No APP API / `get-app-detail`.** Desk HTML + `var dataset` + `drupal_json_output` only.
7. **No attendance write.** Do not call `app-update-attended/%app_id`. Day summary is read-only from the cached worklist.
8. **No credentials, cookies, CSRF tokens, or student records in the repo or in this prompt’s output.**
9. **Do not log into a live host to “discover” the API.** The contract is the PHP. `docs/LIVE-DESK-HAR.md` is a secondary check, not a license to crawl production.
10. **Design file wins every visual argument.** `docs/DIPI Staff.dc.html`.
11. English UI, strings in resources. SemVer + `versionCode` on every shippable change.

---

## 2. What already exists (do not rebuild)

Vertical 1 screens are already wired in `DipiAppUi` via `DeskScreen`:

```
Login → Courses → Today ⇄ Card
                 ↳ Day summary (read-only)
                 ↳ Photo review (UI only; live upload must go away)
                 ↳ Settings (theme, simulate offline, logout, factory reset)
Card → Change-status sheet
```

Live path is already the default (`USE_MOCK=false`). `StaffRepository` already:

- logs in by parsing `GET /user/login` (fallback `GET /` / `GET /centre`) and POSTing the form
- loads courses from `GET /centre/{cid}` HTML
- loads the worklist from `GET /search-course/{cid}/{courseId}?s=&t=&g=&d=a` + `SearchPageParser`
- writes status with `GET /change-status/{id}?s=&l=0&c=`
- keep-alives with `GET /services/session/token` + `GET /centre`
- logs out with `GET /user/logout`

Your job is to **delete the mock fork**, **finish the live parsers against PHP**, and **close the gaps in §5**.

---

## 3. Backend contract (from `dipi-web`, not from fixtures)

Read these files before changing Kotlin:

| Need | PHP | Path | Notes |
|---|---|---|---|
| Menu + access | `dh_manageapp.module` `dh_manageapp_menu()` | — | Permission strings are the ACL. Client does not copy them. |
| Login | Drupal user module | `GET /user/login` (200) or login block; `POST` to parsed `action` with `name`, `pass`, `form_build_id`, `form_id` (`user_login` or `user_login_block`), `op=Log in` | Wipe cookies first. 403/404 HTML may still contain the form (`Response.html()`). |
| Centre mapping | `dh_user_center` (`uc_user`, `uc_center`, `uc_deleted=0`) | `GET /centre` → 302 `/centre/{cid}` | Do not hardcode a centre name. Parse all mapped centres if a select is present. |
| Centre dashboard | `inc/centre.inc` / center dashboard | `GET /centre/{cid}` | Upcoming course links `/course/{cid}/{courseId}` and `/search-course/{cid}/{courseId}?s=…` |
| Course page | `inc/course.inc` `dh_course_dashboard()` | `GET /course/{cid}/{courseId}` | Not required for v1 list. “View Applications” is the search-course URL. |
| Worklist | `inc/search.inc` `_search_course` → `dh_manageapp_search_results()` | `GET /search-course/{cid}/{courseId}` | **GET only.** Do not `POST /search-app`. |
| Dataset | `inc/search.inc` ~`$rs[…]` then `var dataset = …` | inline JS | Public keys to map: `aid`, `name`, `app_status`, `confno`, `gender`, `o_n`, `type`, `city`, `state`, `country`, `dob`, `age`, phones, email, `photo`, course counts, `first_course`, `last_course`, `app_created`, `alist`. **Drop** `aadhar`, `passport`, `voterid`, `pancard`. |
| Status write | `_change_status()` | `GET /change-status/{app_id}?s=&l=&c=` | Access: `change status`. Always `l=0`. Response is `drupal_json_output`: `{status, msg, confno, newstatus}`. Parse JSON even if the body is wrapped. |
| Keep-alive | Services | `GET /services/session/token` | CSRF string. Plus `GET /centre` to refresh `SESS`. |
| Logout | Drupal user | `GET /user/logout` | Then clear cookie jar, Room, outbox. Remember-me stays unless factory reset. |


### 3.1 Unauthenticated login wall (public pages only)

An operator-supplied public-page note (2026-08-15) confirms the anonymous Drupal wall. It did **not** authenticate and does **not** describe post-login screens. Use it only for login-parser edge cases. Internal layout still comes from `dh_manageapp` + the existing Compose screens.

| GET | Anonymous result | App implication |
|---|---|---|
| `/` | 403, login block may be in the body | Already handled: `Response.html()` on errorBody |
| `/user/login` | 200, title “User account \| Dīpi”; fields Username *, Password *, Log in; tab to `/user/password` | Prefer this first (already the live login path) |
| `/centre/{cid}` | 404 with the same login form embedded | Treat 404 HTML as a possible login page, not a hard failure, until a session exists |
| `/user/password` | 200, “Username or e-mail address” + “E-mail new password” | **Do not implement.** Sending recovery email is a mutation. Login screen has no forgot-password. |

Do not crawl the live host to fill gaps. Do not add password recovery.

Query params on `search-course` (from `_search_course` / HAR, confirm in `search.inc`):

| param | meaning |
|---|---|
| `s` | status; empty = all; comma-separated allowed |
| `t` | old student: `1` / `0` / empty |
| `g` | `M` / `F` / empty |
| `d` | `a` applicant (v1 default), `s` student DB |
| `at` | `a` Student, `s` Sevak |

**Do not call (v1):**

- `POST /api/user/login`, `POST /api/user/logout`
- any `/staff/*`
- `POST /search-app/{cid}`
- `GET /get-courses/{cid}` (optional, transfer-course permission; not needed if dashboard parse works)
- `app-update-attended/%app_id`
- `app/%app_id/edit`, add-application, assign-teacher, seating, zero-day, letters, AT, referral, SMS, bulk-mail, PDFs
- photo upload (no desk endpoint)

---

## 4. Delete the mock

Remove or gut so nothing can turn it back on:

- `core/network/.../DipiMockDispatcher.kt`
- `core/network/.../MockFixtures.kt`
- `NetworkModule` MockWebServer provider and `useMock` host swap
- Gradle `-Pdipi.useMock` / `BuildConfig.USE_MOCK`
- `DrupalAuthApi.login` / `logout` Services calls (keep `csrfToken()`)
- `StaffApi` `/staff/session`, `/staff/centres/…`, `/staff/courses/…/applicants`, `/staff/applicants/{id}`, `/staff/meta/statuses`, `/staff/courses/{id}/photo-review`, `POST /staff/applicants/{id}/photo`
- `StaffApi.changeStatus` POST duplicate if live is GET-only (`changeStatusGet` is the desk call)
- every `if (useMock)` in `StaffRepository`
- `UserCentreMap` (mock username → centre name)
- mock-only unit tests that hit MockWebServer; replace with parser tests
- README / AGENTS / TODO-SERVER language that tells people to assemble with `-Pdipi.useMock=true`

`docs/openapi-staff.yaml` stays as a historical mock contract. Do not implement it.

Photo review screen: keep the layout if the design still shows it, but **remove the upload button and mock suggestion chips** when there is no desk photo-review endpoint. Show photos from `dataset.photo` (`show-photo/{aid}`) as read-only, or hide the ◎ entry if there is nothing to show. Do not leave a control that claims to upload.

---

## 5. Gaps to close (this is the remaining implementation)

### 5.1 Course dates and “starts in N days”

Live `loadCourses` currently sets `start`/`end` to `""`. Parse dates from the centre dashboard HTML (course row text / links in `inc/centre.inc` or the upcoming table). `CoursesScreen` must use `LocalDate.now()`, not the hardcoded `2026-08-13`.

### 5.2 Worklist completeness

`SearchPageParser` must map every **public** `$rs` key the card and Today row need:

- identity: `aid`, `name` (split display only; do not invent NPI)
- status / conf: `app_status`, `confno`
- meta: `gender`, `o_n` (old/new), `type` (Student/Sevak), `age`, `dob`, `city`, `state`, `country`
- contact: mobile / home / email field names as emitted in `$rs` (read `search.inc`, do not guess)
- history: `first_course`, `last_course`, `course_10d` / `course_stp` / `course_seva` / other count keys
- `photo` URL (prefix host from `BuildConfig.BASE_URL` if relative)
- `app_created`, monk/robed if present, `alist`

Local search (`q`) filters the cached list by name, conf no, phone, email. Server `s=` is for status chips when you re-fetch; empty `s` is All.

Chip counts: derive from the unfiltered cached dataset (All + each `app_status` present). Do not invent a `/staff/meta/statuses` call. Never add Approved.

### 5.3 Applicant card

Live `loadCard` is cache-only. That is correct (no `GET /staff/applicants/{id}`). After a status write, re-fetch `search-course` for that course (or at least re-parse the row) so conf-no / status come from the server. Optimistic echo is allowed; server wins.

Card facts already in UI: location, mobile, email, home phone, DOB, application date, old-student history. Fill them from dataset keys. Still no ID numbers, medical, street address, edit, or attended toggle.

### 5.4 Change-status response

`_change_status` always `drupal_json_output($out)` with `status` (`OK`|`Failed`), `msg`, `confno`, `newstatus`. Parse that JSON from the GET body (Retrofit converter or a small parser that strips any HTML wrapper). Snackbars show `msg` **verbatim**. Success copy may include conf-no only when `confno` is non-empty.

Outbox + optimistic row stay. Flush uses `GET /change-status/{id}?s=&l=0&c=`.

### 5.5 Session / multi-centre

Centre id comes from the `/centre` → `/centre/{cid}` redirect and `dh_user_center`. If the dashboard can list multiple centres, the Courses screen switcher must reload `GET /centre/{cid}` and re-parse courses. Do not use `UserCentreMap`.

### 5.6 Keep-alive and 403

Every 20 minutes: CSRF token + `GET /centre`. 403 or login HTML → Sign in. `Response.html()` must read `errorBody()` on non-2xx.

### 5.7 Day summary

Keep read-only computation from the cached worklist. Seating line stays “not assigned”. Do not call seating or zero-day PHP.

### 5.8 Settings

Keep Remember me, Simulate offline, Log out, Erase all local data. No URL field. Version from `BuildConfig.VERSION_NAME`.

### 5.9 Hardcoded / stub cleanup

- Remove mock “Rakesh Iyer / Area-teacher” fixture behavior (that was MockWebServer).
- Fix any leftover “Dhamma Giri” hardcodes in UI chrome (centre is from the session).
- Test-mode banner only if you still have a real sandbox signal from the desk. Do not fake `modeTest` from mock session JSON.

---

## 6. Tests (no live login)

Replace MockWebServer tests with fixtures:

1. Check in small HTML snippets under `core/network/src/test/resources/` that match the **shape** in `search.inc` (`var dataset = [ … ]`), the login form (`user_login` / `user_login_block`), and a centre dashboard upcoming-course table. Invent synthetic names/phones. **Never** paste real applicant rows or NPI.
2. `SearchPageParserTest` — dataset mapping, NPI keys dropped, login block, course links, dates.
3. Change-status JSON parse — `{"status":"OK","msg":"","confno":"NF129","newstatus":"Confirmed"}` and a Failed `msg`.
4. Keep existing `:core:model` / `:core:audit` tests.
5. Compose tests that do not need a network (Login / Today / Settings) stay.

Do not add an instrumented test that signs into a real host.

---

## 7. Implementation order

1. Read `dh_manageapp.module` menu + `_change_status`, `inc/search.inc` dataset `$rs` keys, `inc/centre.inc` upcoming courses. Write a short comment in `StaffApi` listing only the live methods that remain.
2. Delete mock (§4). App must still compile and the live repository path must be the only path.
3. Finish `SearchPageParser` + course-date parse + NPI drop. Tests green.
4. Wire card/history/photo URL from dataset. Status sheet choices = statuses seen in dataset ∪ design list, minus Approved.
5. Harden change-status JSON parse + outbox flush.
6. Photo review: read-only or hidden. No upload.
7. Docs: README, AGENTS.md, `docs/TODO-SERVER.md` — mock is gone; live desk is the only transport.
8. Bump version. Run the Gradle command in §0.

Small commits. Do not commit `local.properties`, keystores, HAR files with cookies, or real student HTML.

---

## 8. Out of scope (Vertical 2+)

Add application, edit application, Day 0 seating / attended write, assign teacher, AT portal, letters CRUD, referral, SMS, bulk mail, PDFs, laundry/valuables, student chit, checking slip, WebView.

---

## 9. How to work

- Investigate in `/Users/wizops/DIPI/dipi-web` with read-only tools. Quote function names and paths in commit messages.
- Implement only in `/Users/wizops/DIPI/dipi-app`.
- If a PHP function is ambiguous, prefer **not calling it** over guessing a mutating URL.
- When uncertain whether a click/request writes, do not add that call.
