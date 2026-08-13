# DIPI Centre-Staff Android Client — Grok Build Prompt

Copy everything below the line into Grok (chat first for design, then Grok Build / Android Studio agent for implementation). Do not skip Phase 0.

This prompt is the product/domain spec. It was written from a full read of custom `dipi-web` (`dh_manageapp`) plus a Graphify memory map. If you have a local clone, prefer it over guessing from GitHub.

---

You are a senior Android + Drupal 7 integration engineer.

**Build a centre-staff Android client for the DIPI registrar desk.**

This is **not**:

- a student-apply app (`dipi-applicant` is a different product)
- a WebView of `dipi.vridhamma.org`
- a clone of DataTables / 50-field edit forms
- an AT portal, SMS gateway, WhatsApp bot, IVR, Mitra, or Patrika client
- a wrapper of `/api` (that API is apply + AT + catalogues; role **APP API** can IDOR `get-app-detail`)

This **is** a native, faster, user-oriented client of **`dh_manageapp`** — the main registrar application.

Work in this order. Do not skip.

1. Install and **read** the skills in Phase 0.
2. Read **only** the desk source listed in “Read these files”. Do not tour bridges.
3. Write Phase 1 (`docs/00-architecture.md`) and stop for approval.
4. Implement Vertical 1 (Today’s applications) against OpenAPI + mock.
5. Add a Drupal façade **only** for endpoints that do not already return JSON.

If a skill and this prompt conflict: **this prompt wins on product/domain/scope**; the skill wins on Kotlin/Compose/Android mechanics.

---

## Phase 0 — Skills before Kotlin

Install these **before** design or code.

### Official Android skills (Google)

Repo: https://github.com/android/skills  
Docs: https://developer.android.com/tools/agents/android-skills

```bash
android skills add --skill=jetpack-compose --project=.
android skills add --skill=navigation-3 --project=.
android skills add --skill=edge-to-edge --project=.
android skills add --skill=android-intent-security --project=.
android skills add --skill=testing-setup --project=.
```

Add `camerax` **only when** you start confirmation-number scan (Vertical 2). Do not add it in Vertical 1.

| Skill | When |
|---|---|
| `jetpack-compose` | Every screen |
| `navigation-3` | App graph, course/applicant deep links |
| `edge-to-edge` | Phone + tablet chrome |
| `android-intent-security` | Share PDF, file pick |
| `testing-setup` | Unit + Compose UI + instrumentation |

### Chris Banes Kotlin + Compose

```bash
npx skills add chrisbanes/skills
```

Read, in this order:

1. `using-chrisbanes-skills`
2. `to-plan`
3. `kotlin-api-design` — `CentreId`, `CourseId`, `ApplicantId`, `ConfNo`, `ApplicantStatus`
4. `kotlin-concurrency-and-flow` — repository, one-shot events, outbox
5. `compose-state-and-effects`
6. `compose-component-design` — applicant row, status chip
7. `compose-performance` — applicant lists
8. `compose-ui-testing-patterns` — login → list → status change
9. `kotlin-control-flow` — `when` on sealed status

Optional: `npx skills add aldefy/compose-skill` (Material 3 receipts), `npx skills add dpconde/claude-android-skill` (UI → domain → data, Hilt).

Open each relevant `SKILL.md` before writing that layer. After planning, write `SKILLS-USED.md`.

---

## Goal (cut scope hard)

### Vertical 1 — must ship (Today’s applications)

The daily registrar loop. Nothing else.

- Log in as a **real Drupal centre-staff user**
- Pick centre (`dh_user_center`) + current/upcoming unfinalized course
- Worklist of applicants for that course, filterable by status
- Search by name / conf no / phone / email
- Open a **public card** (identity + status + contact). No medical/ID (`ae_*`) in v1
- Change status through the **existing PHP path** (letter may fire — black box)
- See new confirmation number when Confirm mints one
- Mark attended / not attended

### Vertical 2 — later (do not implement until Vertical 1 is demoable)

Day-0 arrival list, conf-no lookup, room/cell/laundry/group/seat, seating views. Tablet-first. Offline-tolerant.

### Explicitly out (desktop / other products)

- Letter template admin, bulk mail, finalize, centre settings, room inventory editor
- AT schedule admin, AT portal, LC review UI
- Referrals admin, Patrika, student-apply, IVR, SMS keywords, Mitra, WhatsApp internals
- Full 50-field application editor (add/edit form in `application.inc`)

If a Vertical 1 action **must** send a letter or run waitlist logic, call the existing desk function. Do **not** open those modules to “understand” them.

---

## Bridge rule (non-negotiable)

Treat everything outside `dh_manageapp` desk flows as a **black box**.

| Black box | You may | You may not |
|---|---|---|
| `dh_send_letter()` | Trigger it by going through `_change_status` | Reimplement Mailgun / 360Dialog / SMS / RabbitMQ |
| `update_status_external()` | Let `_change_status` call it | Copy its waitlist / referral / LC branches into Kotlin |
| `/api` `dipi_api` | Reuse **login/token shape only** | Use APP API keys, `get-app-detail`, `post-application`, AT-seva |
| `/zeroday` | Ignore in Vertical 1 | Depend on it for search or lists |
| `dh_atportal` | Ignore | Build LC review or AT login (`t_code.t_gender`) |
| `wa-hook`, `/sms`, `/ivr`, `/mitra` | Ignore | Design screens or clients for them |

Open a bridge **only** if a desk button cannot complete without knowing a return field. Then read that one function and stop.

---

## Read these files (and only these) before designing

Prefer local clone if present: `/Users/wizops/DIPI/dipi-web`  
Also read: `docs/DIPI_MEMORY_MAP.md`, `graphify-out/GRAPH_REPORT.md`  
Upstream: https://github.com/VipassanaTech/dipi-web

| Path | Why |
|---|---|
| `sites/all/modules/dh_manageapp/dh_manageapp.module` | `hook_menu`, `hook_permission`, `_change_status`, `_update_status`, `update_status_external`, `generate_conf_no`, `_manageapp_check_access` |
| `sites/all/modules/dh_manageapp/inc/search.inc` | Daily worklist. **Returns HTML**, not JSON — this is the first forced new API |
| `sites/all/modules/dh_manageapp/inc/application.inc` | Field names / prefixes only. Do not port the form |
| `sites/all/modules/dh_manageapp/inc/course.inc` | Course row, `c_finalized`, `dh_course_status_check` (capacity INI) |
| `sites/all/modules/dh_manageapp/inc/center-dashboard.inc` | Upcoming-course mental model |
| `sites/all/modules/dh_manageapp/js/manageapp.js` | Client-side field rules (old student, mobile) |

Do **not** start by reading `dipi_api.module`, `dh_atportal/`, `letters.inc` internals, `zero-day.inc`, or security dump markdown.

---

## Hard facts (do not “simplify”)

1. **No registrar REST exists.** The desk is HTML + a few JSON callbacks.
2. **Do not wrap HTML.** Do not scrape `search-app`.
3. **Auth = centre-staff Drupal user.** `users` + `dh_user_center`. Never bake APP API credentials into the APK.
4. **Centre tenancy.** Filter `a_center` / `c_center` on the server every time.
5. **Gender is a permission:** `access male` / `access female`. Never show the other gender.
6. **No writes if `c_finalized = 1`.**
7. **Statuses live in `dh_type_detail`** (`COURSE-SYSTEM-STATUS`, `COURSE-STATUS`). Code also uses literals (`Received`, `WaitList`, `Confirmed`, …). Load from server; keep literals as fallback.
8. **All status writes go through existing PHP.** Never `UPDATE dh_applicant.a_status` from the app.
9. **Confirm mints conf no:** `{N\|O\|S}{M\|F}` + per-course sequence (`generate_conf_no` / `nextval1`). Expected→Confirmed does not remint.
10. **On Received**, server may force WaitList and set referral. Client shows whatever status comes back.
11. **Duplicate is excluded** from Full/Waitlist counts (2026).
12. **NPI:** `ae_*` medical/ID. Vertical 1 does not fetch or store them. `FLAG_SECURE` on applicant detail anyway.
13. **`mode_test=1`** reroutes letters. Show a banner if the session reports test mode.

### Applicant object (know the prefixes; v1 only needs `a_*`)

| Table | Prefix | v1 |
|---|---|---|
| `dh_applicant` | `a_` | Yes — public card |
| `dh_applicant_course` | `ac_` | Optional counts on detail |
| `dh_applicant_extra` | `ae_` | **No** |
| `dh_applicant_lc` | `al_` | **No UI.** If status is an LC state, show the chip only |
| `dh_applicant_attended` | `aa_` | Attended flag only in v1 |

### Status machine (display + send these strings; do not reimplement transitions)

```
Received
  → Clarification → ClarificationResponse
  → Confirmed → Expected → ReConfirmation → Attended
                                            ↘ Left
  → Cancelled | Rejected | Errors | WaitList | Duplicate | Custom | Deceased
```

LC states may appear on a row (`R-ATReview`, `A-ATReview`, `R-ATTransfer`, `Rejected-R-AT`, `Rejected-A-AT`). Show them. Do **not** build Reco/Area review screens. Changing those still goes through `_change_status` if the user has `change status`.

### Existing desk HTTP you may reuse (read the PHP for the real payload)

| URL | Job | v1 |
|---|---|---|
| `POST /api/user/login` then cookie + `X-CSRF-Token` from `/services/session/token` | Auth **shape** | Yes (or mirror on `/staff`) |
| `GET/POST /change-status/{app_id}?s={status}&l={letterId}&c={comment}` | Status + letter. JSON: `{status, msg, confno, newstatus}` | **Yes — preferred write path** |
| `GET/POST /app-update-attended/{app_id}` | Toggle attended | Yes |
| `/autocomplete/get-{country,state,city,teacher}` | Typeahead | Not v1 |
| `/course/handler/{cid}` | DataTables Editor | No (fragile) |
| `/search-app/{cid}` | Worklist | **HTML — do not parse. New JSON required** |
| `/zeroday/*` | Conf-no check-in API | Not v1 (bridge) |

Drupal Services login today:

1. `POST /api/user/login` → `{ sessid, session_name, token, user }`
2. Later requests send the session cookie + `X-CSRF-Token`

### Permissions the app must honor

`access manageapp`, `access centre`, `access course`, `change status`, `access male`, `access female`, `access zero day` (v2), `access application pdf/photos` (later), `access center dashboard`.

**Never flatten to “logged-in user sees all applicants”.**

---

## Phase 1 — Design (mandatory written output)

Write `docs/00-architecture.md`. Then stop.

### 1. API strategy (this prompt already chose for you)

**Hybrid (do not reopen A/B/C as a debate):**

- **Writes:** call existing JSON (`/change-status`, `/app-update-attended`) via a small Android adapter, **or** a 1:1 façade on `/staff` that only calls those PHP functions.
- **Reads that already do not exist as JSON:** add **one** new resource first: course applicant worklist/search.
- **Session:** `GET /staff/session` (or equivalent) returning user, permissions, gender access, centres, `mode_test`.
- No new business rules in PHP. No new status transitions.
- If you cannot write PHP in this session: Android implements `docs/openapi-staff.yaml` + MockWebServer. Mark each mock with `TODO(server): <php function>`.

Forbidden: HTML scrape, WebView, APP API service account.

### 2. Domain model (sealed types, not strings)

- `CentreId`, `CourseId`, `ApplicantId`, `ConfNo`
- `ApplicantStatus` from server list + known literals
- `Gender` + `GenderAccess`
- `ApplicantCard` (public) vs `ApplicantSensitive` (must not be populated in v1)
- `Course` including `c_finalized`, dates, type, male/female open/full flags if the server sends them

### 3. Screens (Vertical 1 only)

Tablet: list + detail. Phone: stacked. `width >= 600.dp` two-pane.

1. Login (server URL + username + password)
2. Centre + course picker (hide finalized)
3. Today — status chips + counts + list
4. Search field on that list
5. Applicant card
6. Change-status sheet: new status, optional comment, checkbox “send letter” (letter id optional; **no letter-body preview** — that is a bridge)
7. Settings: base URL, test-mode banner, logout

Do not draw Day-0 / seating / AT screens in the v1 IA.

### 4. Offline (keep small)

- Cache the **active course worklist** only (encrypted Room / SQLCipher).
- Outbox for `change-status` and `mark-attended`. Server wins. Snackbar on conflict.
- Wipe DB on logout and centre switch.
- No photos on disk.

### 5. Security

- TLS required. Session cookie in Encrypted DataStore. Never log cookies, passwords, or letter text.
- Configurable base URL.
- `FLAG_SECURE` on applicant detail.
- Release builds contain no real student fixtures.

### 6. Risks to name in the plan

Missing search JSON, CSRF, gender leak, finalized writes, letter side effects, Drupal 7 session expiry mid-shift, `_change_status` LC special cases when `s=Approved`.

**Stop and print a 1-page plan.** Do not scaffold the Android project until the human says to start Vertical 1.

---

## Phase 2 — Implement Vertical 1

### Stack (do not bikeshed)

- Kotlin, JDK 17+
- Compose + Material 3
- Navigation 3
- Hilt
- Coroutines + Flow
- Retrofit + OkHttp + kotlinx.serialization
- Encrypted DataStore
- Room + SQLCipher (active course only)
- Coil only if you later show photos (not v1)
- Min SDK 26, target 35+
- Phone + tablet, dark theme, edge-to-edge

Package: `org.dhamma.dipi.staff`

```
:app
:core:model
:core:network
:core:database
:core:datastore
:core:ui
:feature:auth
:feature:course
:feature:applicants
```

Do **not** create `:feature:day0` until Vertical 2.

### Network contract (v1 only)

`docs/openapi-staff.yaml`. Android codes to this even if PHP is not written.

```
POST   /staff/user/login
POST   /staff/user/logout
GET    /staff/session
       → user, permissions[], genderAccess, centres[], modeTest

GET    /staff/centres/{cid}/courses?upcoming=1
GET    /staff/courses/{courseId}
       → id, name, start, end, finalized, type, status by gender if cheap

GET    /staff/courses/{courseId}/applicants?status=&q=&gender=&limit=&cursor=
       → THIS IS THE NEW ENDPOINT. Back it with the query in search.inc,
         not with HTML. Server applies centre + gender + “not finalized” rules.

GET    /staff/applicants/{aId}
       → public card only (a_* minus anything you classify as NPI)

GET    /staff/applicants/{aId}/activity     optional; skip if time-boxed

POST   /staff/applicants/{aId}/status
       { status, letterId?, comment? }
       → façade over _change_status / update_status_external
       → return { ok, status, message, confNo, newStatus }

POST   /staff/applicants/{aId}/attended
       { attended: bool }
       → façade over dh_app_update_attended

GET    /staff/meta/statuses
```

Every list/detail is centre-scoped and gender-filtered **on the server**.

Do **not** add Day-0 collection resources, allotment PATCH, seating, or `/staff/zeroday` in this file until Vertical 2.

### UX

- Calm desk. No gamification. No purple AI gradient.
- Stable status colours: Received, Clarification, Confirmed, Expected, WaitList, Attended, Left, Cancelled, other.
- Status sheet copy: **“This may send a letter to the applicant.”**
- English UI; all user strings in `strings.xml`.
- Test-mode banner when `modeTest=true`.
- Offline / outbox banner when pending writes exist.

### Done for Vertical 1

A staff member on a phone or tablet can:

1. Log in as a centre user (mock or real).
2. See today’s course applicant counts by status.
3. Search by name / conf no / phone / email.
4. Change Received → Confirmed (or Clarification) and see the new status (and conf no if minted).
5. Toggle attended and see it stick after refresh.
6. Not see the other gender, other centres, or `ae_*` fields.

Also ship:

- Mock server so the app runs with no DIPI host
- `README.md`: architecture, how to set base URL, how `dh_staff_api` is enabled
- Unit tests: status request mapping, gender filter, outbox
- Compose UI test: login → course → list

---

## Phase 3 — Drupal façade (only if asked or you have write access)

New module `sites/all/modules/dh_staff_api/`:

- `hook_services_resources()` for the v1 routes above
- Search callback: extract the SELECT/WHERE from `search.inc`, return JSON rows
- Status callback: call `_change_status` / `_update_status` / `update_status_external` — **do not copy letter or waitlist logic**
- Attended callback: call `dh_app_update_attended`
- Access: `_manageapp_check_access()` + `access male` / `access female` on every resource
- JSON only
- README with curl examples

Do not modify `update_status_external()` unless you find a bug.  
Do not add AT, SMS, WhatsApp, or zero-day resources here until Vertical 2 needs them.

---

## Implementation rules

- Read PHP before inventing a field name. Prefixes are the schema.
- Do not hardcode centre IDs or production hostnames.
- Do not add student-apply, dana, patrika, WhatsApp, AT-seva, LC review.
- Do not store medical/ID in logs, Crashlytics, or Room.
- Small commits: plan → model → auth → worklist → status → attended.
- When blocked by a missing read API, implement OpenAPI + mock and `TODO(server): search.inc query`.
- When a write already exists (`/change-status`), **use it** instead of inventing a parallel rule engine.

---

## First message you (Grok) should produce

1. Confirm skills installed / read.
2. One page: hybrid API (what you reuse vs the one new search resource).
3. Vertical 1 screen list + mermaid data flow. No Day-0 diagram.
4. OpenAPI skeleton **limited to the v1 routes above**.
5. Implementation sequence, max 6 steps.
6. Ask: **“Approve Vertical 1 and I will scaffold step 1.”**

Only after approval, scaffold `org.dhamma.dipi.staff` and implement Today’s applications.

---

End of prompt.
