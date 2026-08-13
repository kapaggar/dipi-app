# DIPI-web memory map

Built 2026-08-13 from custom DIPI code only (Drupal 7 core and contrib excluded).
Graphify graph: `graphify-out/graph.html` (284 nodes, 310 edges, 55 communities).
AST cannot see Drupal `.module` files natively; those were staged as `.php` for extraction.

This map is the working model for a **centre-staff Android client**. It is not a student-apply app.

---

## 1. What this repo is

A Drupal 7 LAMP application that runs Vipassana centre registration worldwide.

Custom product code lives in:

| Path | Role |
|------|------|
| `sites/all/modules/dh_manageapp/` | Registrar desk (the product) |
| `sites/all/modules/dh_atportal/` | Assistant Teacher self-service |
| `sites/all/modules/dh_patrika/` | Physical newsletter |
| `sites/all/modules/dipi_api/` | Machine API (Services) |
| `sites/all/modules/find_people/` | Admin user search |
| `sites/all/modules/user_created_by/` | Who created a Drupal user |
| `sites/all/themes/tweme/` | Bootstrap skin |
| `cron-*.php`, `status-trigger.php`, `unfinalized.php` | CLI |

Everything else is stock Drupal 7 or contrib (Views, Services, CTools, Mailgun module).

---

## 2. Runtime shape

```
Browser registrar UI          AT portal UI           External machines
 (DataTables + Drupal forms)   (/at-portal)           (mobile / IVR / SMS / Mitra)
           \                         |                         |
            \                        |                         |
             +-------- Drupal 7 bootstrap + session/cookie ----+
                                 |
                    dh_* tables in MySQL
                                 |
          Mailgun | 360Dialog | S3 | RabbitMQ | SMS vendors | pdftk
```

There is **no registrar REST**. The desk UI is HTML pages plus a handful of JSON callbacks. The REST layer (`/api`, `/zeroday`, `/ivr`, `/sms`, `/mitra`) was built for other clients, not for the registrar.

---

## 3. Tenancy and identity

- Every operational row is **centre-scoped** (`a_center`, `c_center`, `l_center`).
- Staff access is `users` + `dh_user_center` (uid ↔ centre). `_manageapp_check_access()` counts that mapping.
- Gender split is a first-class permission: `access male` / `access female`.
- AT login username is `t_code.t_gender` (example `DHI001.M`), stored in both `dh_teacher` and `users`.
- Application confirmation number is generated on Confirm: `{N|O|S}{M|F}` + per-course sequence (`generate_conf_no()` via MySQL `nextval1`).
- Status labels are **not hardcoded forever**. Canonical values live in `dh_type_detail` (`COURSE-SYSTEM-STATUS`, `COURSE-STATUS`, `COURSE-LC-STATUS`). Code still has string literals (`Received`, `WaitList`, `R-ATReview`) in many branches.

Drupal Services auth for `/api`:

1. `POST /api/user/login` → session + token
2. Subsequent `POST /api/dipi/{action}/...`
3. AT extra: Sodium sealed box `username|||password` on `check-cred`

Role **APP API** already exists: `get all centres`, `get all courses`, `post application`, `get application status`, `at portal access`.

---

## 4. Applicant record (the atomic object)

Split across five tables, always keyed by `a_id`:

| Table | Prefix | Contents |
|-------|--------|----------|
| `dh_applicant` | `a_` | Identity, contact, status, centre, course, conf no, type, old/new, flags |
| `dh_applicant_course` | `ac_` | Course-count history (10d/20d/30d/…), first/last course |
| `dh_applicant_extra` | `ae_` | Medical, ID, pregnancy, parents (teen/child) — NPI |
| `dh_applicant_lc` | `al_` | Long-course eligibility + Reco/Area AT + approvals |
| `dh_applicant_attended` | `aa_` | Day-0: room, cell, laundry, valuables, group, seat, left |

After finalize, attended students are copied to `dh_student` / `dh_student_course` / `dh_student_course_input`. That is the permanent person record. Applications remain historical.

Form groups in `application.inc` (the registrar add/edit screen):

1. Personal (name, gender, DOB, phones, address, old/new, Student/Sevak, A-list, monk)
2. Work / education / department
3. ID + nationality + languages (3 + discourse)
4. Medical / emergency / internal note / friend-family
5. Course history counts (if old student)
6. LC screening (if LC course)
7. Teen/child extras (if teen/child course)
8. Day-0 allotment (section, room, cell, laundry, group, left)

Cannot add/edit if `c_finalized = 1`.

---

## 5. Status machine

System statuses (from `dh_type_detail` + code):

```
Received
  → Clarification → ClarificationResponse
  → Confirmed  → Expected → ReConfirmation → Attended
                                            ↘ Left
  → Cancelled | Rejected | Errors | WaitList | Duplicate | Custom | Deceased
```

Long course extra:

```
R-ATReview → A-ATReview → Received (approved)
           ↘ Rejected-R-AT
A-ATReview → Rejected-A-AT
R-ATTransfer → (registrar picks Reco AT) → R-ATReview
```

**Write path for a status change (desk):**

`GET/POST /change-status/{app_id}?s=&l=&c=` → `_change_status()` → `_update_status()` and/or `update_status_external()` → optional `dh_send_letter()`.

`update_status_external()` is the **god function** of the lifecycle:

- On `Received`: `set_referral()`, `dh_course_status_check()`, maybe force `WaitList`
- On Confirm: `generate_conf_no()` unless already Expected→Confirmed
- Rebuilds application PDF
- Syncs course Full/Waitlist from centre INI (`cs_course_config`)
- Duplicate is excluded from capacity counts (2026 change)

Letters fire on most non-LC status changes. Confirm/Expected attach the application PDF from S3.

Graphify confirms this: `update_status_external()`, `_update_status()`, `_change_status()`, `dh_atportal_review_form_submit()`, `_process_lc_application()` sit in one community (“Status Transition Core”).

---

## 6. HTTP surface a native client must replace

### 6.1 Drupal pages (HTML, session cookie)

Centre home: `centre/{cid}`

Courses: `manage-course/{cid}`, `course/{cid}/{course}`, `course/handler/{cid}` (DataTables Editor CRUD on `dh_course`)

Applicants: `search-app/{cid}`, `app/add/{cid}/{course}`, `app/{id}/edit`, `app/{id}/delete`, `change-status/{id}`, `app-courses/{id}`, `app-activity/{id}`, `app-clarifications/{id}`, `show-application/{id}`, `show-photo/{id}`

Day-0: `zero-day/{cid}/{course}`, `teacher-list`, `manager-list`, `cell-list`, `laundry-list`, `valuable-list`, `seating`, `group-seating`, `day0-list`, `student-chit`, `checking-slip`, `report-day11`

Letters: `letters/{cid}`, `letter-fields/{cid}`

AT schedule: `at-schedule/{cid}`, `assign-teacher/{cid}/{course}`, `at-schedule/change-{status,type,group}/{atappid}`

Referrals: `referral/{cid}`, `center-referral/{cid}`

Comms: `centre/{cid}/bulk-mail-schedule`, `centre/{cid}/sms-report`, `centre/{cid}/course-report`

Dashboards: `center-dashboard-page`, `vri-management`

Webhooks (permission `access webhook`): `wa-hook`, `wa-hook-bulk`, `webhook/mailgun`, `webhook3/mailgun`

### 6.2 JSON / AJAX already on the desk (not REST)

| URL | Job |
|-----|-----|
| `/change-status/{app_id}` | Status + letter |
| `/course/handler/{cid}` | DataTables Editor on courses |
| `/user-mapping/handler` | User↔centre mapping |
| `/letter-fields/handler/{cid}` | Custom merge fields |
| `/centre/{cid}/acco-handler` | Room inventory |
| `/autocomplete/get-{country,state,city,teacher}` | Typeahead |
| `/get-location-from-pincode` | Zip lookup |
| `/get-courses/{cid}` | Transfer target list |
| `/app-student-count-finalize/{course}` | Finalize preview counts |
| `/app-update-attended/{app}` | Toggle attended |
| `/move-to-course/{app}/{course}/{cid}` | Transfer |

These are the **real registrar API**, just not designed as one.

### 6.3 Drupal Services (existing, wrong shape for registrar)

| Mount | Auth | What it can do |
|-------|------|----------------|
| `/api` | session | Incremental catalogues, `post-application`, app status, AT LC + AT-seva, `check-cred` |
| `/zeroday` | session | Lookup / mark attended / stats **by confirmation number only** |
| `/ivr` | session | Phone-keyed confirm/cancel/status |
| `/sms` | API key | Keyword router |
| `/mitra` | `mitra upakram` | Partner delta sync |

Missing from all of these: search, letter send, edit application, rooms/cells/seating, finalize, bulk mail, referrals, centre settings, course Editor.

---

## 7. Day-0 (physical course start)

Entry: `dh_manage_day_zero()` routes on URL.

Lists generated from `dh_applicant` + `dh_applicant_attended` + seating INI (`cs_seat_config`) + room INI (`dh_center_setting_acco`):

- Teacher list / manager list (optional confirmation number, cell batch)
- Cell list, laundry, valuables
- Hall seating + **group-wise seating** (`aa_group_seat_row/col`, `GROUP<n>-` keys)
- Day-0 arrival list (order by conf no; strip contacts option)
- Student chit / checking slip
- Day-11 report PDF

Mark attended: `dh_app_update_attended` and `/zeroday` `mark-attended-by-conf-num`.

Finalize (`finalize_course`): course end date must be past; attended → `Attended`; snapshot `dh_course_stat`; copy to `dh_student*`; move PDFs/photos; `c_finalized=1`. Reverse is CLI `unfinalized.php`.

---

## 8. Letters and outbound comms

`dh_letter` is per-centre, per-event templates (email subject/body + SMS). Merge via `dh_get_letter()`. Send via `dh_send_letter()`:

- Email: Mailgun (`mailgun_key`, centre from-name / reply-to)
- WhatsApp: 360Dialog templates + optional PDF
- SMS: Textlocal / Bhash / Rudra / SA aggregator
- `mode_test=1` reroutes email to `mode_test_emails`

Bulk mail: search builds a WHERE clause → `dh_bulk_mail` row → `cron-bulk-mail.php` → RabbitMQ.

Inbound: Mailgun webhooks set `Errors` on bounce; WhatsApp `/wa-hook` → RabbitMQ `whatsapp` queue → same keywords as SMS.

---

## 9. AT portal vs registrar AT schedule

Two different jobs:

| | Registrar (`at-schedule.inc`) | AT (`dh_atportal` + `/api`) |
|--|-------------------------------|-----------------------------|
| Who | Centre staff | The teacher |
| Job | Confirm/cancel ATs, type, group, quotas | Apply for seva, review LC apps, edit own profile |
| Quotas | `c_at_m_count` / `c_at_f_count` vs confirmed | Self-apply + status view |
| LC | Sees Reco/Area columns | Reco AT / Area AT review forms |

Address book (`address-book.inc`) is a large AT directory: CT/ACT centres, CAT/ACAT areas, roles, deceased, search.

---

## 10. Tables that matter (46 used in custom SQL)

**Core loop:** `dh_applicant`, `dh_applicant_course`, `dh_applicant_extra`, `dh_applicant_lc`, `dh_applicant_attended`, `dh_applicant_clarification`

**Org:** `dh_center`, `dh_center_setting`, `dh_center_setting_acco`, `dh_user_center`, `dh_course`, `dh_course_teacher`, `dh_course_stat`, `dh_center_course_template`, `dh_lc_admin`

**People after finalize:** `dh_student`, `dh_student_course`, `dh_student_course_input`

**Teachers:** `dh_teacher`, `dh_teacher_role`, `dh_teacher_center`, `dh_teacher_area`, `dh_teacher_area_cluster`, `dh_teacher_log`

**Comms:** `dh_letter`, `dh_letter_fields`, `dh_bulk_mail`, `dh_bulk_mail_log`, `dh_bulk_mail_unsubscribe`, `dh_center_sms`, `dh_sms_log`, `dh_ivr_log`

**Referral:** `dh_referral`, `dh_referral_log`

**Geo:** `dh_country`, `dh_state`, `dh_city`, `dh_pin_code`, `dh_languages`

**Config/audit:** `dh_type_detail`, `dh_log`, `dh_json`

**Patrika:** `dh_patrika`, `dh_patrika_payment`, `dh_patrika_note`, `dh_patrika_log`

---

## 11. Permissions a staff app must honor

From `dh_manageapp_permission()` (subset):

`access manageapp`, `access centre`, `access course`, `manage course`, `add/edit/delete application`, `change status`, `transfer course`, `access zero day`, `access male`, `access female`, `manage letters`, `mass mail`, `at scheduling`, `view/manage/delete referral list`, `r-at review`, `a-at review`, `export data`, `view teachers list`, `view sms/course report`, `access application pdf/photos`, `access center dashboard`, `access all centres`

AT portal: `access at portal`, `access at profile`, `access at address book`, `add assistant teacher`, `at portal superadmin`, `approve applications`.

A native client **cannot** flatten this to “logged-in user sees all applicants”.

---

## 12. Integrations

| System | Where | Notes |
|--------|-------|--------|
| Mailgun | `letters.inc` | Transactional + webhooks |
| 360Dialog | `dipi_api.module` | Templates, WAID check, PDF upload |
| AWS S3 | `dana-s3.inc` | Photos + PDFs; creds in `/dhamma/htpasswd/s3-env.ini` |
| RabbitMQ | bulk mail + WhatsApp | `mq_*` Drupal variables |
| pdftk / pdfcpu | `pdf.inc` | Form-fill application + D11 |
| SMS vendors | `send_sms*` | Country-routed |

Test switch: `variable_get('mode_test')`.

---

## 13. Graphify findings (what the graph is good for)

God nodes (highest degree): `_api_handle_sms`, `handle_whatsapp_msg`, `update_status_external`, `_update_status`, `send_sms`, `logit`.

That matches the source: **status change + outbound message** is the system spine. SMS/WhatsApp look more “connected” than search/zero-day because those live in `.inc` files that PHP AST barely cross-links (Drupal `include` is not an import). Isolated communities for `search.inc`, `zero-day.inc`, `course.inc` are an extractor limit, not a modular design.

True architectural fact the graph still shows: **`update_status_external` is the bridge** between AT portal review, registrar `_change_status`, and API `_process_lc_application`.

Health: 4 dangling-endpoint edges after merge; graph is usable.

---

## 14. Implications for an Android registrar client

1. Do not wrap the Drupal pages. They are DataTables Editor + giant forms.
2. Do not pretend `/api` is the registrar API. It is apply + AT + catalogues.
3. The first native vertical that pays off is **today’s course**: incoming Received, status change + letter, search, mark attended. That is `_change_status` + search + day-0 attended.
4. Next: day-0 lists and seating (local, tablet, offline-tolerant).
5. Last: letters admin, bulk mail, finalize, centre settings (desktop is fine).
6. Auth should be Drupal staff session (or a new token issued for that user), **not** the APP API service account (that account can already IDOR `get-app-detail`).
7. Treat medical + ID fields as NPI. No local photo cache without encryption. No logging of letter bodies.

---

## How to refresh this map

```bash
cd dipi-web
graphify update .     # incremental AST; keep graphify-out/cache
open graphify-out/graph.html
```

`.graphifyignore` keeps Drupal core/contrib out. Re-stage `*.module` → `graphify-out/_modules/*.php` if you rebuild from scratch (Graphify does not treat `.module` as PHP).
